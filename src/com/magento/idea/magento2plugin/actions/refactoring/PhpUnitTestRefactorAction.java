/*
 * Copyright © Magento, Inc. All rights reserved.
 * See COPYING.txt for license details.
 */

package com.magento.idea.magento2plugin.actions.refactoring;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDirectory;
import com.jetbrains.php.PhpClassHierarchyUtils;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.psi.elements.ClassReference;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.magento.idea.magento2plugin.MagentoIcons;
import com.magento.idea.magento2plugin.actions.refactoring.util.PhpUnitRefactoringUtil;
import com.magento.idea.magento2plugin.project.Settings;
import com.magento.idea.magento2plugin.util.php.PhpPsiElementsUtil;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;

public class PhpUnitTestRefactorAction extends AnAction {

    public static final String ACTION_NAME = "Refactor PHPUnit Tests";
    public static final String ACTION_DESCRIPTION =
            "Refactor PHPUnit class to be compatible with the PHPUnit v9.3.0";
    private static final String BASE_TEST_CLASS_FQN = "\\PHPUnit\\Framework\\TestCase";
    private PhpClass phpUnitClass;

    /**
     * PHPUnit class refactoring action constructor.
     */
    public PhpUnitTestRefactorAction() {
        super(ACTION_NAME, ACTION_DESCRIPTION, MagentoIcons.MODULE);
    }

    @Override
    public void update(final @NotNull AnActionEvent event) {
        setIsAvailableForEvent(event, false);
        final Project project = event.getProject();
        final PhpClass phpClass = PhpPsiElementsUtil.getPhpClass(event);

        if (project == null || !Settings.isEnabled(project) || phpClass == null) {
            return;
        }
        final List<ClassReference> parentList = phpClass.getExtendsList().getReferenceElements();

        if (parentList.isEmpty()) {
            return;
        }
        PhpIndex phpIndex = PhpIndex.getInstance(project);

        final List<PhpClass> foundClasses =
                new ArrayList<>(phpIndex.getClassesByFQN(BASE_TEST_CLASS_FQN));
        final PhpClass baseTestClass = foundClasses.stream().findFirst().isPresent()
                ? foundClasses.stream().findFirst().get()
                : null;

        boolean isTestClass = false;

        if (baseTestClass != null) {
            isTestClass = PhpClassHierarchyUtils.isSuperClass(baseTestClass, phpClass, false);
        }
        if (phpClass.isAbstract() || !isTestClass) {
            return;
        }
        phpUnitClass = phpClass;
        setIsAvailableForEvent(event, true);
    }

    @Override
    public void actionPerformed(final @NotNull AnActionEvent event) {
        final PsiDirectory directory =
                phpUnitClass.getContainingFile().getContainingDirectory();

        if (event.getProject() == null || directory == null) {
            return;
        }

        new PhpUnitRefactoringUtil(phpUnitClass).fixDeprecatedMethods();
    }

    /**
     * Set is action available for event.
     *
     * @param event AnActionEvent
     * @param isAvailable boolean
     */
    private void setIsAvailableForEvent(
            final @NotNull AnActionEvent event,
            final boolean isAvailable
    ) {
        event.getPresentation().setVisible(isAvailable);
        event.getPresentation().setEnabled(isAvailable);
    }
}
