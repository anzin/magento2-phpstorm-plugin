/*
 * Copyright © Magento, Inc. All rights reserved.
 * See COPYING.txt for license details.
 */

package com.magento.idea.magento2plugin.actions.generation;

import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.ListPopup;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import com.intellij.ui.popup.PopupFactoryImpl;
import com.intellij.ui.popup.list.PopupListElementRenderer;
import com.intellij.util.ui.GridBag;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import com.magento.idea.magento2plugin.MagentoIcons;
import javax.swing.*;
import java.awt.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import com.magento.idea.magento2plugin.indexes.LayoutIndex;
import com.magento.idea.magento2plugin.indexes.ModuleIndex;
import com.magento.idea.magento2plugin.magento.files.LayoutXml;
import com.magento.idea.magento2plugin.magento.packages.Areas;
import com.magento.idea.magento2plugin.util.magento.GetModuleNameByDirectoryUtil;
import com.magento.idea.magento2plugin.util.magento.area.AreaResolverUtil;
import icons.PhpIcons;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

public class OverriddenTemplateAction extends AnAction {

    public static final String ACTION_NAME = "Compare original template with overridden one";
    public static final String DESCRIPTION = "Overridden templates";
    private static final String PHTML_EXTENSION = "phtml";
    private static final String DIRECTORY_VIEW = "view";
    private static final String DIRECTORY_TEMPLATES = "templates";
    private static final String DIRECTORY_DESIGN = "app/design";
    private static final String PHP_REFERENCE_SEPARATOR = "::";
    private final List<PsiElement> psiFileCandidates = new ArrayList<>();

    public OverriddenTemplateAction() {
        super(ACTION_NAME, DESCRIPTION, MagentoIcons.MODULE);
    }

    @Override
    public void actionPerformed(final @NotNull AnActionEvent event) {
        final DefaultActionGroup defaultActionGroup = new DefaultActionGroup();

        for(final PsiElement psiFileCandidate : this.psiFileCandidates) {
            VirtualFile virtualFile = psiFileCandidate.getContainingFile().getVirtualFile();

            final AnAction action = new VirtualOverrideAction(
                    virtualFile.getName(),
                    virtualFile.getPath()
            );

            defaultActionGroup.addAction(action);
        }

        String popupPlace = ActionPlaces.getPopupPlace(getClass().getSimpleName());
        ListPopup popup = new PopupFactoryImpl.ActionGroupPopup(
                DESCRIPTION,
                defaultActionGroup,
                event.getDataContext(),
                true,
                true,
                false,
                true,
                null,
                -1,
                null,
                popupPlace
        ) {
            @Override
            protected ListCellRenderer<PopupFactoryImpl.ActionItem> getListElementRenderer() {
                return new PopupListElementRenderer<>(this) {
                    private JLabel myInfoLabel;
                    private JLabel myShortcutLabel;

                    @Override
                    protected void createLabel() {
                        super.createLabel();
                            myIconLabel.setBorder(
                                    JBUI.Borders.empty(1, 1, 1, JBUI.CurrentTheme.ActionsList.elementIconGap())
                            );
                            myInfoLabel = new JLabel();
                            myInfoLabel.setBorder(JBUI.Borders.empty(1, UIUtil.DEFAULT_HGAP, 1, 1));
                            myShortcutLabel = new JLabel();
                            myShortcutLabel.setBorder(JBUI.Borders.emptyLeft(UIUtil.DEFAULT_HGAP));
                            myShortcutLabel.setForeground(UIUtil.getContextHelpForeground());
                    }

                    @Override
                    protected JComponent createItemComponent() {
                        createLabel();

                        JPanel panel = new JPanel(new GridBagLayout());
                        GridBag gbc = new GridBag();

                        panel.add(myIconLabel, gbc.next());
                        panel.add(myShortcutLabel, gbc.next());
                        panel.add(myInfoLabel, gbc.next().weightx(1));

                        return layoutComponent(panel);
                    }

                    @Override
                    protected void customizeComponent(
                            @NotNull JList<? extends PopupFactoryImpl.ActionItem> list,
                            @NotNull PopupFactoryImpl.ActionItem actionItem,
                            boolean isSelected
                    ) {
                        Color foreground = isSelected ? UIUtil.getListSelectionForeground(true)
                                : UIUtil.getInactiveTextColor();
                        final VirtualOverrideAction action = (VirtualOverrideAction) actionItem.getAction();
                        myIconLabel.setForeground(foreground);
                        myInfoLabel.setForeground(foreground);
                        myShortcutLabel.setForeground(foreground);
                        myIconLabel.setIcon(PhpIcons.PhpIcon);
                        myInfoLabel.setText(action.getDescription());
                        myShortcutLabel.setText(action.getActionName());
                    }
                };
            }

            @Override
            protected boolean isResizable() {
                return true;
            }
        };

        updatePopupSize(popup);
        popup.showInBestPositionFor(event.getDataContext());
    }

    private static void updatePopupSize(@NotNull ListPopup popup) {
        ApplicationManager.getApplication().invokeLater(() -> {
            popup.getContent().setPreferredSize(new Dimension(500, popup.getContent().getPreferredSize().height));
            popup.getContent().setSize(new Dimension(500, popup.getContent().getPreferredSize().height));
            popup.setSize(popup.getContent().getPreferredSize());
        });
    }

    @Override
    public void update(@NotNull AnActionEvent event) {
        final PsiFile psiFile = event.getData(PlatformDataKeys.PSI_FILE);
        final Project project = event.getData(PlatformDataKeys.PROJECT);
        this.psiFileCandidates.clear();

        if (psiFile == null || project == null) {
            setStatus(event, false);
            return;
        }

        if (!PHTML_EXTENSION.equals(psiFile.getVirtualFile().getExtension())) {
            setStatus(event, false);
            return;
        }
        final Areas templateArea = AreaResolverUtil.getForFileInModule(psiFile.getVirtualFile());

        if (templateArea == null) {
            setStatus(event, false);
            return;
        }

        final String blockName = getBlockNameFromXmlFile(project, psiFile, templateArea);

        if (blockName != null) {
            final List<XmlTag> referenceBlocks = getReferenceBlockDeclarations(project, blockName);

            for (final XmlTag block: referenceBlocks) {
                final String blockXmlAttributeValue = block.getAttributeValue(LayoutXml.XML_ATTRIBUTE_TEMPLATE);

                if (blockXmlAttributeValue == null) {
                    continue;
                }
                final String[] templateData = blockXmlAttributeValue.split(PHP_REFERENCE_SEPARATOR);

                if(templateData[0].isEmpty() || templateData[1].isEmpty()) {
                    setStatus(event, false);
                    return;
                }
                final PsiDirectory originalModuleDirectory =
                        new ModuleIndex(project).getModuleDirectoryByModuleName(templateData[0]);

                if(originalModuleDirectory == null) {
                    continue;
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
                final PsiFile psiFileCandidate = getPsiFileByPath(project, originalFilePath);

                if (psiFileCandidate == null) {
                    continue;
                }
                this.psiFileCandidates.add(psiFileCandidate);
            }
        }
        findOverrideInTheme(project, this.psiFileCandidates, psiFile, templateArea);

        setStatus(event, !this.psiFileCandidates.isEmpty());
    }

    private void setStatus(final AnActionEvent event, final boolean status) {
        event.getPresentation().setVisible(status);
        event.getPresentation().setEnabled(status);
    }

    private void findOverrideInTheme(
            final Project project,
            final java.util.List<PsiElement> psiFileCandidates,
            final PsiFile psiFile,
            final Areas templateArea
    ) {
        final java.util.List<String> themeNames = new ModuleIndex(project).getEditableThemeNames();
        final String moduleName = GetModuleNameByDirectoryUtil.execute(psiFile.getContainingDirectory(), project);

        for (final String themeName : themeNames) {
            final String originalFilePath = project.getBasePath()
                    + "/"
                    + DIRECTORY_DESIGN
                    + "/"
                    + themeName
                    + "/"
                    + moduleName
                    + StringUtils.substringAfter(
                    psiFile.getVirtualFile().getPath(),
                    DIRECTORY_VIEW + "/" + templateArea
            );

            final PsiFile psiFileCandidate = getPsiFileByPath(project, originalFilePath);

            if (psiFileCandidate == null) {
                continue;
            }

            psiFileCandidates.add(psiFileCandidate);
        }
    }

    private PsiFile getPsiFileByPath (final Project project, final String path) {
        final VirtualFile origFileCandidate = VfsUtil.findFile(Path.of(path), false);

        if (origFileCandidate == null) {
            return null;
        }

        return PsiManager.getInstance(project).findFile(origFileCandidate);
    }

    private PsiDirectory originalModuleDirectory(final Project project, final PsiFile psiFile) {
        final PsiDirectory directory = psiFile.getContainingDirectory();

        return new ModuleIndex(project).getModuleDirectoryByModuleName(
                GetModuleNameByDirectoryUtil.execute(directory, project)
        );
    }

    private String getBlockNameFromXmlFile(final Project project, final PsiFile psiFile, final Areas templateArea) {
        final PsiDirectory originalModuleDirectory = originalModuleDirectory(project, psiFile);

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
        final String templateName = GetModuleNameByDirectoryUtil.execute(psiFile.getContainingDirectory(), project)
                + PHP_REFERENCE_SEPARATOR + StringUtils.substringAfter(
                psiFile.getVirtualFile().getPath(),
                DIRECTORY_TEMPLATES + "/");
        String blockName = null;
        final java.util.List<XmlTag> results = new ArrayList<>();

        for(final PsiFile xmlFile : directoryLayout.getFiles()) {
            final XmlTag parentTag = ((XmlFile) xmlFile).getRootTag();

            if (parentTag != null) {
                collectComponentDeclarations(
                        parentTag,
                        results,
                        templateName,
                        LayoutXml.BLOCK_ATTRIBUTE_TAG_NAME,
                        LayoutXml.XML_ATTRIBUTE_TEMPLATE
                );

                if (!results.isEmpty()) {
                    blockName = results.get(0).getAttributeValue(LayoutXml.NAME_ATTRIBUTE);
                    break;
                }
            }
        }

        return blockName;
    }

    private java.util.List<XmlTag> getReferenceBlockDeclarations(
            final Project project,
            final String referenceBlockName
    ) {
        final java.util.List<XmlTag> results = new ArrayList<>();

        for (final XmlFile layoutFile : LayoutIndex.getLayoutFiles(project)) {
            final XmlTag rootTags = layoutFile.getRootTag();

            if (rootTags != null) {
                collectComponentDeclarations(
                        rootTags,
                        results,
                        referenceBlockName,
                        LayoutXml.REFERENCE_BLOCK_ATTRIBUTE_TAG_NAME,
                        LayoutXml.NAME_ATTRIBUTE
                );
            }
        }

        return results;
    }

    private void collectComponentDeclarations(
            final XmlTag parentTag,
            final List<XmlTag> results,
            final String nameValue,
            final String tagName,
            final String attributeValue
    ) {
        for (final XmlTag childTag: parentTag.getSubTags()) {
            if (tagName.equals(childTag.getName())) {
                final String blockAttributeValue = childTag.getAttributeValue(attributeValue);

                if (blockAttributeValue != null && blockAttributeValue.equals(nameValue)) {
                    results.add(childTag);
                }
            } else if(childTag.getSubTags().length > 0 ) {
                collectComponentDeclarations(childTag, results, nameValue, tagName, attributeValue);
            }
        }
    }
}
