/*
 * Copyright © Magento, Inc. All rights reserved.
 * See COPYING.txt for license details.
 */

package com.magento.idea.magento2plugin.actions.generation;

import com.intellij.ide.util.PsiNavigationSupport;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.magento.idea.magento2plugin.MagentoIcons;
import java.nio.file.Path;
import org.jetbrains.annotations.NotNull;

public class VirtualOverrideAction extends AnAction {
    private final String actionDescription;
    private final String actionName;

    public VirtualOverrideAction(final String actionName, final String actionDescription) {
        super(OverriddenTemplateAction.ACTION_NAME, OverriddenTemplateAction.DESCRIPTION, MagentoIcons.MODULE);
        this.actionName = actionName;
        this.actionDescription = actionDescription;
    }

    @Override
    public void actionPerformed(final @NotNull AnActionEvent event) {
        final Project project = event.getProject();
        final VirtualFile origFileCandidate = VfsUtil.findFile(Path.of(getDescription()), false);

        if (project == null || origFileCandidate == null) {
            return;
        }

        PsiNavigationSupport.getInstance().createNavigatable(project, origFileCandidate, -1).navigate(false);
    }

    public String getActionName() {
        return this.actionName;
    }

    public String getDescription() {
        return this.actionDescription;
    }
}
