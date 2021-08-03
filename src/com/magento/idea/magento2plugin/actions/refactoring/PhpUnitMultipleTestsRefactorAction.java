/*
 * Copyright © Magento, Inc. All rights reserved.
 * See COPYING.txt for license details.
 */

package com.magento.idea.magento2plugin.actions.refactoring;

import com.intellij.ide.IdeView;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDirectory;
import com.magento.idea.magento2plugin.MagentoIcons;
import com.magento.idea.magento2plugin.actions.refactoring.dialog.RefactorMultiplePhpUnitTestsDialog;
import com.magento.idea.magento2plugin.project.Settings;
import org.jetbrains.annotations.NotNull;

public class PhpUnitMultipleTestsRefactorAction extends AnAction {

    public static final String ACTION_NAME = "Refactor PHPUnit Tests In Directory";
    public static final String ACTION_DESCRIPTION =
            "Refactor PHPUnit classes by include list to be compatible with the PHPUnit v9.3.0";
    private PsiDirectory directory;

    /**
     * PHPUnit class refactoring action constructor.
     */
    public PhpUnitMultipleTestsRefactorAction() {
        super(ACTION_NAME, ACTION_DESCRIPTION, MagentoIcons.MODULE);
    }

    @Override
    public void update(final @NotNull AnActionEvent event) {
        setIsAvailableForEvent(event, false);
        final Project project = event.getProject();
        final DataContext dataContext = event.getDataContext();
        final IdeView view = LangDataKeys.IDE_VIEW.getData(dataContext);

        if (view == null) {
            return;
        }
        final PsiDirectory directory = view.getOrChooseDirectory();

        if (project == null || !Settings.isEnabled(project) || directory == null) {
            return;
        }
        this.directory = directory;
        setIsAvailableForEvent(event, true);
    }

    @Override
    public void actionPerformed(final @NotNull AnActionEvent event) {
        if (event.getProject() == null || directory == null) {
            return;
        }
        RefactorMultiplePhpUnitTestsDialog.open(event.getProject(), directory);
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
