/*
 * Copyright © Magento, Inc. All rights reserved.
 * See COPYING.txt for license details.
 */

package com.magento.idea.magento2plugin.actions.refactoring.php;

import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.psi.util.PsiTreeUtil;
import com.jetbrains.php.lang.psi.elements.MethodReference;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.php.lang.psi.visitors.PhpElementVisitor;
import com.magento.idea.magento2plugin.actions.refactoring.CouldNotRefactorException;
import org.jetbrains.annotations.NotNull;

public class RenameCalledMethodAction implements RefactoringAction {

    final PhpClass element;
    final String targetMethodName;
    final MethodReferenceNameRefactoringResolver refactoringResolver;

    private final PhpElementVisitor refactoringVisitor = new PhpElementVisitor() {

        @Override
        public void visitPhpMethodReference(final MethodReference reference) {

            if (reference == null || reference.getName() == null) {
                return;
            }

            if (!reference.getName().equals(targetMethodName)) {
                return;
            }

            WriteCommandAction.writeCommandAction(
                    reference.getManager().getProject(),
                    reference.getContainingFile()
            ).run(() -> {
                try {
                    reference.handleElementRename(
                            refactoringResolver.resolveMethodName(reference)
                    );
                } catch (CouldNotRefactorException exception) {
                    // something went wrong
                }
            });
        }
    };

    /**
     * RenameCalledMethodAction constructor.
     *
     * @param element PhpClass
     * @param targetMethodName String
     * @param refactoringResolver MethodReferenceNameRefactoringResolver
     */
    public RenameCalledMethodAction(
            final @NotNull PhpClass element,
            final String targetMethodName,
            final MethodReferenceNameRefactoringResolver refactoringResolver
    ) {
        this.element = element;
        this.targetMethodName = targetMethodName;
        this.refactoringResolver = refactoringResolver;
    }

    @Override
    public void refactor() {
        for (final MethodReference methodReference :
                PsiTreeUtil.findChildrenOfType(element, MethodReference.class)) {
            refactoringVisitor.visitPhpMethodReference(methodReference);
        }
    }

    public interface MethodReferenceNameRefactoringResolver {

        String resolveMethodName(final @NotNull MethodReference reference)
                throws CouldNotRefactorException;
    }
}
