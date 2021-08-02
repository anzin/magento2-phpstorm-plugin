/*
 * Copyright © Magento, Inc. All rights reserved.
 * See COPYING.txt for license details.
 */

package com.magento.idea.magento2plugin.actions.refactoring.util;

import com.intellij.psi.PsiElement;
import com.jetbrains.php.lang.psi.elements.MethodReference;
import java.util.LinkedList;
import java.util.List;
import org.jetbrains.annotations.NotNull;

public final class PhpRefactoringUtil {

    private PhpRefactoringUtil() {
    }

    /**
     * Get method arguments as string.
     *
     * @param reference MethodReference
     *
     * @return String
     */
    public static String getMethodReferenceArgsAsString(final @NotNull MethodReference reference) {
        final List<String> args = new LinkedList<>();

        for (final PsiElement argument : reference.getParameters()) {
            args.add(argument.getText());
        }

        return String.join(", ", args);
    }
}
