package com.magento.idea.magento2plugin.actions.refactoring.php;

import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.jetbrains.php.lang.psi.PhpPsiElementFactory;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.ParameterList;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.php.lang.psi.elements.PhpReturnType;
import com.jetbrains.php.lang.psi.resolve.types.PhpType;
import com.jetbrains.php.lang.psi.visitors.PhpElementVisitor;
import com.magento.idea.magento2plugin.magento.packages.MagentoPhpClass;
import com.magento.idea.magento2plugin.util.php.PhpTypeMetadataParserUtil;
import org.jetbrains.annotations.NotNull;

public class AddTypeForClassMethod implements RefactoringAction {

    private final PhpClass phpClass;

    /**
     * @param element PhpClass
     */
    public AddTypeForClassMethod(final @NotNull PhpClass element) {
        this.phpClass = element;
    }

    private final PhpElementVisitor refactoringVisitor = new PhpElementVisitor() {

        public void visitPhpClass(PhpClass phpClass) {
            final Method[] phpClassMethods = phpClass.getOwnMethods();

            WriteCommandAction.writeCommandAction(
                    phpClass.getProject(),
                    phpClass.getContainingFile()
            ).run(() -> {
                for (final Method method : phpClassMethods) {
                    if (!method.getName().equals(MagentoPhpClass.CONSTRUCT_METHOD_NAME)) {


                        if (method.getTypeDeclaration() == null) {
                            String returnType = PhpTypeMetadataParserUtil.getMethodReturnType(method);

                            PsiElement colon = PhpPsiElementFactory.createColon(phpClass.getProject());
                            PsiElement whiteSpace = PhpPsiElementFactory.createWhiteSpace(phpClass.getProject());
                            final PsiElement returnType1 = PhpPsiElementFactory.createReturnType(phpClass.getProject(), "void");

                            final ParameterList paramList = PsiTreeUtil.findChildOfType(
                                    method,
                                    ParameterList.class
                            );

                            PsiElement bracket = null;

                            if (paramList != null) {
                                bracket = paramList.getNextSibling();
                            }

                            if (bracket != null) {
                                colon = method.addAfter(colon, bracket);
                                whiteSpace = method.addAfter(whiteSpace, colon);
                                method.addAfter(returnType1, whiteSpace);
                            }

                            int a = 0;
                        }
                    }
                }
            });
        }
    };

    @Override
    public void refactor() {
        refactoringVisitor.visitPhpClass(phpClass);
    }
}
