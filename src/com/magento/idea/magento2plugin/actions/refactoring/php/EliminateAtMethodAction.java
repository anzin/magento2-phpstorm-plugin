/*
 * Copyright © Magento, Inc. All rights reserved.
 * See COPYING.txt for license details.
 */

package com.magento.idea.magento2plugin.actions.refactoring.php;

import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.util.Pair;
import com.intellij.psi.util.PsiTreeUtil;
import com.jetbrains.php.lang.psi.PhpPsiElementFactory;
import com.jetbrains.php.lang.psi.elements.FieldReference;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.MethodReference;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.php.lang.psi.elements.Statement;
import com.jetbrains.php.lang.psi.elements.Variable;
import com.jetbrains.php.lang.psi.visitors.PhpElementVisitor;
import com.magento.idea.magento2plugin.actions.refactoring.CouldNotRefactorException;
import com.magento.idea.magento2plugin.actions.refactoring.util.PhpRefactoringUtil;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;

public class EliminateAtMethodAction implements RefactoringAction {

    final PhpClass element;

    private final PhpElementVisitor refactoringVisitor = new PhpElementVisitor() {

        private static final String TARGET_METHOD_REGEX = "\\$this->at\\(\\d\\)";

        @Override
        public void visitPhpMethod(final Method method) {

            final String methodText = method.getText();
            final Pattern pattern = Pattern.compile(TARGET_METHOD_REGEX, Pattern.MULTILINE);
            final Matcher matcher = pattern.matcher(methodText);
            final Map<String, List<MockChainData>> mockChainDataMap = new HashMap<>();
            final Map<String, List<Statement>> originalStatements = new HashMap<>();

            while (matcher.find()) {
                final int matchedOffset = matcher.start();
                final Statement statement = PsiTreeUtil.getParentOfType(
                        method.findElementAt(matchedOffset),
                        Statement.class
                );

                if (statement == null) {
                    continue;
                }
                final FieldReference fieldReference =
                        PsiTreeUtil.findChildOfType(statement, FieldReference.class);
                Variable variable = null;

                if (fieldReference == null) {
                    variable = PsiTreeUtil.findChildOfType(statement, Variable.class);

                    if (variable == null) {
                        continue;
                    }
                }
                final String targetField = fieldReference == null
                        ? variable.getText()
                        : fieldReference.getText();
                String targetMethod = null;
                String targetWith = null;
                String targetWillReturn = null;

                List<Statement> statementList = new LinkedList<>();

                if (originalStatements.containsKey(targetField)) {
                    statementList.addAll(originalStatements.get(targetField));
                }
                statementList.add(statement);
                originalStatements.put(targetField, statementList);

                final List<MethodReference> methodReferences =
                        new ArrayList<>(
                                PsiTreeUtil.findChildrenOfType(
                                        statement,
                                        MethodReference.class
                                )
                        );

                for (final MethodReference reference : methodReferences) {
                    if ("method".equals(reference.getName())) {
                        targetMethod = PhpRefactoringUtil.getMethodReferenceArgsAsString(reference);
                    } else if ("with".equals(reference.getName())) {
                        targetWith = PhpRefactoringUtil.getMethodReferenceArgsAsString(reference);
                    } else if ("willReturn".equals(reference.getName())) {
                        targetWillReturn = PhpRefactoringUtil
                                .getMethodReferenceArgsAsString(reference);
                    }
                }

                if (targetMethod == null || targetWillReturn == null) {
                    continue;
                }

                final MockChainData data = new MockChainData(
                        targetMethod,
                        targetWith,
                        targetWillReturn
                );
                List<MockChainData> mockChainDataList;

                if (!mockChainDataMap.containsKey(targetField)) {
                    mockChainDataList = new LinkedList<>();
                } else {
                    mockChainDataList = mockChainDataMap.get(targetField);
                }
                mockChainDataList.add(data);
                mockChainDataMap.put(targetField, mockChainDataList);
            }

            if (!mockChainDataMap.isEmpty()) {
                WriteCommandAction.writeCommandAction(
                        method.getProject(),
                        method.getContainingFile()
                ).run(() -> {
                    for (final Map.Entry<String, List<MockChainData>> entry
                            : mockChainDataMap.entrySet()) {
                        final List<Statement> originalStatementsForField = originalStatements
                                .get(entry.getKey());
                        final String validStatementString =
                                new ValidStatementBuilder(
                                        entry.getKey(),
                                        entry.getValue()
                                ).buildValidStatement();

                        if (validStatementString == null || originalStatementsForField.isEmpty()) {
                            continue;
                        }

                        final Statement lastStatement = originalStatementsForField
                                .get(originalStatementsForField.size() - 1);
                        final Statement validStatement = PhpPsiElementFactory.createStatement(
                                method.getProject(),
                                validStatementString
                        );

                        lastStatement.replace(validStatement);
                        originalStatementsForField.remove(originalStatementsForField.size() - 1);

                        for (final Statement statement : originalStatementsForField) {
                            statement.delete();
                        }
                    }
                });
            }
        }
    };

    /**
     * EliminateAtMethodAction constructor.
     *
     * @param element PhpClass
     */
    public EliminateAtMethodAction(final @NotNull PhpClass element) {
        this.element = element;
    }

    @Override
    public void refactor() {
        for (final Method method : PsiTreeUtil.findChildrenOfType(element, Method.class)) {
            refactoringVisitor.visitPhpMethod(method);
        }
    }

    private static class MockChainData {

        private final String method;
        private final String with;
        private final String willReturn;

        public MockChainData(
                final @NotNull String method,
                final String with,
                final @NotNull String willReturn
        ) {
            this.method = method;
            this.with = with;
            this.willReturn = willReturn;
        }

        public String getMethod() {
            return method;
        }

        public String getWith() {
            return with;
        }

        public String getWillReturn() {
            return willReturn;
        }
    }

    private static class ValidStatementBuilder {

        private final String targetField;
        private final List<MockChainData> mockChainDataList;

        public ValidStatementBuilder(
                final String targetField,
                final List<MockChainData> mockChainDataList
        ) {
            this.targetField = targetField;
            this.mockChainDataList = new LinkedList<>(mockChainDataList);
        }

        /**
         * Build valid statement to eliminate $this->at().
         *
         * @return String
         */
        public String buildValidStatement() {
            if (mockChainDataList.isEmpty()) {
                return null;
            }
            final StringBuilder stringBuilder = new StringBuilder(targetField);

            String method = null;
            final List<String> withArgs = new LinkedList<>();
            final List<String> formattedWithArgs = new LinkedList<>();
            final List<String> returnArgs = new LinkedList<>();

            for (final MockChainData data : mockChainDataList) {
                if (method == null) {
                    method = data.getMethod();
                }
                if (data.getWith() != null) {
                    formattedWithArgs.add(String.format("[%s]", data.getWith()));
                    withArgs.add(data.getWith());
                }
                returnArgs.add(data.getWillReturn());
            }
            stringBuilder.append(String.format("->method(%s)\n", method));

            if (!withArgs.isEmpty()) {
                if (withArgs.size() == 1 && returnArgs.size() == 1) {
                    stringBuilder.append(String.format(
                            "->with(%s)\n",
                            String.join(", ", withArgs)
                    ));
                } else {
                    stringBuilder.append(String.format(
                            "->withConsecutive(%s)\n",
                            String.join(", ", formattedWithArgs)
                    ));
                }
            }
            if (returnArgs.size() == 1) {
                stringBuilder.append(String.format(
                        "->willReturn(%s);\n",
                        String.join(", ", returnArgs)
                ));
            } else {
                stringBuilder.append(String.format(
                        "->willReturnOnConsecutiveCalls(%s);\n",
                        String.join(", ", returnArgs)
                ));
            }

            return stringBuilder.toString();
        }
    }
}
