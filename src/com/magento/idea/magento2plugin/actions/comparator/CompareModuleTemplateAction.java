/*
 * Copyright © Magento, Inc. All rights reserved.
 * See COPYING.txt for license details.
 */

package com.magento.idea.magento2plugin.actions.comparator;

import com.intellij.diff.DiffDialogHints;
import com.intellij.diff.DiffManager;
import com.intellij.diff.chains.DiffRequestChain;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiFile;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import com.magento.idea.magento2plugin.MagentoIcons;
import com.magento.idea.magento2plugin.actions.comparator.util.DiffRequestChainUtil;
import com.magento.idea.magento2plugin.indexes.LayoutIndex;
import com.magento.idea.magento2plugin.indexes.ModuleIndex;
import com.magento.idea.magento2plugin.magento.files.LayoutXml;
import com.magento.idea.magento2plugin.magento.packages.Areas;
import com.magento.idea.magento2plugin.project.Settings;
import com.magento.idea.magento2plugin.util.magento.GetModuleNameByDirectoryUtil;
import com.magento.idea.magento2plugin.util.magento.area.AreaResolverUtil;
import org.apache.commons.lang3.StringUtils;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;

public class CompareModuleTemplateAction extends AnAction {

    public static final String ACTION_NAME = "Compare overridden template with the original one";
    public static final String ACTION_DESCRIPTION = "The Magento 2 overridden template comparing";
    private static final String PHTML_EXTENSION = "phtml";
    private static final String DIRECTORY_VIEW = "view";
    private static final String DIRECTORY_TEMPLATES = "templates";
    public static final String PHP_REFERENCE_SEPARATOR = "::";
    protected VirtualFile selectedFile;
    protected VirtualFile originalFile;

    /**
     * Compare template action constructor.
     */
    public CompareModuleTemplateAction() {
        super(ACTION_NAME, ACTION_DESCRIPTION, MagentoIcons.MODULE);
    }

    /**
     * Updates the state of action.
     *
     * @param event AnActionEvent
     */
    @Override
    @SuppressWarnings({"PMD.CyclomaticComplexity", "PMD.NPathComplexity", "PMD.CognitiveComplexity"})
    public void update(final @NotNull AnActionEvent event) {
        setStatus(event, false);
        final Project project = event.getData(PlatformDataKeys.PROJECT);

        if (project != null && !Settings.isEnabled(project)) {
            return;
        }
        final PsiFile psiFile = event.getData(PlatformDataKeys.PSI_FILE);

        if (psiFile == null) {
            return;
        }
        final VirtualFile targetFileCandidate = psiFile.getVirtualFile();

        if (targetFileCandidate == null) {
            return;
        }

        if (!PHTML_EXTENSION.equals(targetFileCandidate.getExtension())) {
            return;
        }
        final Areas templateArea = AreaResolverUtil.getForFileInModule(targetFileCandidate);

        if (templateArea == null) {
            return;
        }

        final String blockName = getBlockNameFromXmlFile(project, psiFile, templateArea);

        if (blockName == null) {
            return;
        }
        final List<XmlTag> block = LayoutIndex.getBlockDeclarations(blockName, project);

        if (block.isEmpty()) {
            return;
        }
        final String blockXmlAttributeValue = block.get(0).getAttributeValue(LayoutXml.XML_ATTRIBUTE_TEMPLATE);

        if (blockXmlAttributeValue == null) {
            return;
        }
        final String[] templateData = blockXmlAttributeValue.split(PHP_REFERENCE_SEPARATOR);

        if(templateData[0].isEmpty() || templateData[1].isEmpty()) {
            return;
        }
        final PsiDirectory originalModuleDirectory =
                new ModuleIndex(project).getModuleDirectoryByModuleName(templateData[0]);

        if (originalModuleDirectory == null) {
            return;
        }
        final String originalFilePath = originalModuleDirectory.getVirtualFile().getPath()
                + "/"
                + DIRECTORY_VIEW
                + "/"
                + templateArea
                + "/"
                + DIRECTORY_TEMPLATES
                + "/"
                + templateData[1];

        final VirtualFile origFileCandidate = VfsUtil.findFile(Path.of(originalFilePath), false);

        if (origFileCandidate == null) {
            return;
        }
        selectedFile = targetFileCandidate;
        originalFile = origFileCandidate;
        this.setStatus(event, true);
    }

    @Override
    public void actionPerformed(final @NotNull AnActionEvent event) {
        final Project project = event.getProject();

        if (project == null || selectedFile == null || originalFile == null) {
            return;
        }
        final DiffRequestChain chain = DiffRequestChainUtil.createMutableChain(
                project,
                selectedFile,
                originalFile
        );

        if (chain == null) {
            return;
        }
        DiffManager.getInstance().showDiff(
                project,
                chain,
                DiffDialogHints.DEFAULT
        );
    }

    private void setStatus(final AnActionEvent event, final boolean status) {
        event.getPresentation().setVisible(status);
        event.getPresentation().setEnabled(status);
    }

    private String getBlockNameFromXmlFile(final Project project, final PsiFile psiFile, final Areas templateArea) {
        final PsiDirectory directory = psiFile.getContainingDirectory();
        final String moduleName = GetModuleNameByDirectoryUtil.execute(directory, project);
        final PsiDirectory originalModuleDirectory =
                new ModuleIndex(project).getModuleDirectoryByModuleName(moduleName);

        if (originalModuleDirectory == null) {
            return null;
        }
        final PsiDirectory directoryView = originalModuleDirectory.findSubdirectory(DIRECTORY_VIEW);

        if (directoryView == null) {
            return null;
        }
        final PsiDirectory directoryArea = directoryView.findSubdirectory(templateArea.toString());

        if (directoryArea == null) {
            return null;
        }
        final PsiDirectory directoryLayout = directoryArea.findSubdirectory(LayoutXml.PARENT_DIR);

        if (directoryLayout == null) {
            return null;
        }
        final String templateName = moduleName + PHP_REFERENCE_SEPARATOR + StringUtils.substringAfter(
                psiFile.getVirtualFile().getPath(),
                DIRECTORY_TEMPLATES + "/"
        );
        final List<XmlTag> results = new ArrayList<XmlTag>(); //NOPMD
        String blockName = null;

        for(final PsiFile xmlFile : directoryLayout.getFiles()) {
            final XmlTag parentTag = ((XmlFile) xmlFile).getRootTag();

            if (parentTag != null) {
                findBlockNameByTemplate(parentTag, results, templateName);

                if (!results.isEmpty()) {
                    blockName = results.get(0).getAttributeValue(LayoutXml.NAME_ATTRIBUTE);
                    break;
                }
            }
        }

        return blockName;
    }

    private void findBlockNameByTemplate(
            @NotNull final XmlTag parentTag,
            final List<XmlTag> results,
            final String templateName
    ) {
        for (final XmlTag childTag: parentTag.getSubTags()) {
            if (LayoutXml.REFERENCE_BLOCK_ATTRIBUTE_TAG_NAME.equals(childTag.getName())) {
                final String blockTemplateName = childTag.getAttributeValue(LayoutXml.XML_ATTRIBUTE_TEMPLATE);

                if (blockTemplateName != null && blockTemplateName.equals(templateName)) {
                    results.add(childTag);
                }
            } else if(childTag.getSubTags().length > 0 ) {
                findBlockNameByTemplate(childTag, results, templateName);
            }
        }
    }
}
