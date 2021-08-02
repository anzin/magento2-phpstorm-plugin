/*
 * Copyright © Magento, Inc. All rights reserved.
 * See COPYING.txt for license details.
 */

package com.magento.idea.magento2plugin.actions.refactoring.util;

import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.psi.PhpPsiElementFactory;
import com.jetbrains.php.lang.psi.elements.ArrayCreationExpression;
import com.jetbrains.php.lang.psi.elements.ClassConstantReference;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.MethodReference;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.php.lang.psi.elements.PhpNamedElement;
import com.jetbrains.php.lang.psi.elements.PhpPsiElement;
import com.magento.idea.magento2plugin.actions.refactoring.CouldNotRefactorException;
import com.magento.idea.magento2plugin.actions.refactoring.php.RenameCalledMethodAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;

public final class PhpUnitRefactoringUtil {

    private final PhpClass phpClass;
    private final RenameCalledMethodAction renameCalledMethodAction;

    /**
     * Refactoring util constructor.
     *
     * @param phpClass PhpClass
     */
    public PhpUnitRefactoringUtil(final @NotNull PhpClass phpClass) {
        this.phpClass = phpClass;
        renameCalledMethodAction = new RenameCalledMethodAction(
                phpClass,
                "setMethods",
                getNameResolver()
        );
    }

    public void fixDeprecatedMethods() {
        renameCalledMethodAction.refactor();
    }

    private RenameCalledMethodAction.MethodReferenceNameRefactoringResolver getNameResolver() {
        return (reference -> {
            final List<PsiElement> parameterList =
                    new ArrayList<>(Arrays.asList(reference.getParameters()));

            if (parameterList.isEmpty()) {
                throw new CouldNotRefactorException();
            }
            final PsiElement parameter = parameterList.listIterator().next();

            if (!(parameter instanceof ArrayCreationExpression)) {
                throw new CouldNotRefactorException();
            }

            final List<String> targetMethods = new LinkedList<>();

            for (final PsiElement value : parameter.getChildren()) {
                if (value instanceof PhpPsiElement && !value.getText().isEmpty()) {
                    targetMethods.add(value.getText().replace("'", ""));
                }
            }

            MethodReference getMockBuilderReference = null;
            int depthCounter = 0;
            MethodReference prevSibling = PsiTreeUtil.getChildOfType(
                    reference,
                    MethodReference.class
            );

            while (prevSibling != null && depthCounter < 5) {
                if ("getMockBuilder".equals(prevSibling.getName())) {
                    getMockBuilderReference = prevSibling;
                    break;
                }
                depthCounter++;
                prevSibling = PsiTreeUtil.getChildOfType(prevSibling, MethodReference.class);
            }

            if (getMockBuilderReference == null) {
                throw new CouldNotRefactorException();
            }
            final PsiElement mockedTypeParameter =
                    Arrays.stream(getMockBuilderReference.getParameters())
                            .iterator().next();

            if (!(mockedTypeParameter instanceof ClassConstantReference)) {
                throw new CouldNotRefactorException();
            }
            final String mockedTypeSignature =
                    ((ClassConstantReference) mockedTypeParameter).getSignature();
            final Pattern fqnPattern = Pattern.compile("(\\\\.*).class");
            final Matcher fqnMatcher = fqnPattern.matcher(mockedTypeSignature);
            String mockedClassFqn = null;

            try {
                if (fqnMatcher.find()) {
                    mockedClassFqn = fqnMatcher.group(1);
                }
            } catch (Exception exception) {
                throw new CouldNotRefactorException();
            }

            if (mockedClassFqn == null) {
                throw new CouldNotRefactorException();
            }
            final PhpIndex phpIndex = PhpIndex.getInstance(reference.getProject());
            final Iterator<PhpClass> classCandidateIterator =
                    new ArrayList<>(phpIndex.getAnyByFQN(mockedClassFqn))
                            .stream().iterator();

            if (!classCandidateIterator.hasNext()) {
                throw new CouldNotRefactorException();
            }
            final PhpClass classCandidate = classCandidateIterator.next();

            if (classCandidate == null) {
                throw new CouldNotRefactorException();
            }
            final List<String> onlyMethodsCandidates = new LinkedList<>();
            final List<String> addMethodsCandidates = new LinkedList<>();

            final List<Method> methods = new ArrayList<>(classCandidate.getMethods());
            final List<String> candidateMethodsNames = methods
                    .stream()
                    .map(PhpNamedElement::getName)
                    .collect(Collectors.toList());

            for (final String method : targetMethods) {
                if (candidateMethodsNames.contains(method)) {
                    onlyMethodsCandidates.add(method);
                } else {
                    addMethodsCandidates.add(method);
                }
            }
            String referenceText = reference.getText();
            String mainReferenceName = null;
            final List<String> mainReferenceArgsCandidates;
            String additionalReferenceName = null;
            final List<String> additionalReferenceArgsCandidates = new LinkedList<>();

            if (!onlyMethodsCandidates.isEmpty()) {
                mainReferenceName = "onlyMethods";
                mainReferenceArgsCandidates = new LinkedList<>(onlyMethodsCandidates);

                if (!addMethodsCandidates.isEmpty()) {
                    additionalReferenceName = "addMethods";
                    additionalReferenceArgsCandidates.addAll(addMethodsCandidates);
                }
            } else {
                mainReferenceName = "addMethods";
                mainReferenceArgsCandidates = new LinkedList<>(addMethodsCandidates);
            }

            final String mainReferenceNameReplacement =
                    mainReferenceName + "([" + mainReferenceArgsCandidates
                            .stream()
                            .map(value -> String.format("'%s'", value))
                            .collect(Collectors.joining(", ")) + "])";

            referenceText = referenceText
                    .replaceAll(
                            "setMethods\\(\\[.*\\]\\)",
                            mainReferenceNameReplacement
                    );

            if (additionalReferenceName != null) {
                final String addMethodsReplacement =
                        "\n->" + additionalReferenceName + "(["
                                + additionalReferenceArgsCandidates
                                .stream()
                                .map(value -> String.format("'%s'", value))
                                .collect(Collectors.joining(", ")) + "])";
                referenceText = referenceText + addMethodsReplacement;
            }

            reference.replace(
                    PhpPsiElementFactory.createMethodReference(
                            phpClass.getProject(),
                            referenceText
                    )
            );

            throw new CouldNotRefactorException();
        });
    }
}
