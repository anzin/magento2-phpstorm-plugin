/*
 * Copyright © Magento, Inc. All rights reserved.
 * See COPYING.txt for license details.
 */

package com.magento.idea.magento2plugin.linemarker.phtml;

import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.codeInsight.daemon.LineMarkerProvider;
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.templateLanguages.OuterLanguageElementImpl;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import com.magento.idea.magento2plugin.indexes.LayoutIndex;
import com.magento.idea.magento2plugin.indexes.ModuleIndex;
import com.magento.idea.magento2plugin.magento.files.LayoutXml;
import com.magento.idea.magento2plugin.magento.packages.Areas;
import com.magento.idea.magento2plugin.project.Settings;
import com.magento.idea.magento2plugin.util.magento.GetModuleNameByDirectoryUtil;
import com.magento.idea.magento2plugin.util.magento.area.AreaResolverUtil;
import org.apache.commons.lang3.StringUtils;
import java.nio.file.Path;
import java.util.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Created by dkvashnin on 11/15/15.
 */
public class OverrideTemplateLineMarkerProvider implements LineMarkerProvider {

    private static final String PHTML_EXTENSION = "phtml";
    private static final String DIRECTORY_VIEW = "view";
    private static final String DIRECTORY_TEMPLATES = "templates";
    private static final String DIRECTORY_DESIGN = "app/design";
    private static final String PHP_REFERENCE_SEPARATOR = "::";
    private static final String PHP_OUTER_TYPE_IN_HTML = "PHP outer type in html";

    @Nullable
    @Override
    public LineMarkerInfo getLineMarkerInfo(final @NotNull PsiElement psiElement) {
        return null;
    }

    @Override
    @SuppressWarnings({
            "PMD.CyclomaticComplexity",
            "PMD.NPathComplexity",
            "PMD.CognitiveComplexity",
            "PMD.AvoidInstantiatingObjectsInLoops",
            "PMD.CollapsibleIfStatements"
    })
    public void collectSlowLineMarkers(
            final @NotNull List<? extends PsiElement> elements,
            final @NotNull Collection<? super LineMarkerInfo<?>> result
    ) {
        if (!elements.isEmpty()) {
            if (!Settings.isEnabled(elements.get(0).getProject())) {
                return;
            }
        }
        final PsiElement psiElement = elements.get(0);
        final IElementType elementType = ((OuterLanguageElementImpl) psiElement).getElementType();

        if(!PHP_OUTER_TYPE_IN_HTML.equals(elementType.toString())) {
           return;
        }
        final PsiFile psiFile = psiElement.getContainingFile();

        if (!PHTML_EXTENSION.equals(psiFile.getVirtualFile().getExtension())) {
            return;
        }
        final Areas templateArea = AreaResolverUtil.getForFileInModule(psiFile.getVirtualFile());

        if (templateArea == null) {
            return;
        }
        final Project project = psiElement.getProject();
        final String blockName = getBlockNameFromXmlFile(project, psiFile, templateArea);

        if (blockName == null) {
            return;
        }
        final List<XmlTag> referenceBlocks = getReferenceBlockDeclarations(project, blockName);
        final List<PsiElement> psiFileCandidates = new ArrayList();

        for (final XmlTag block: referenceBlocks) {
            final String blockXmlAttributeValue = block.getAttributeValue(LayoutXml.XML_ATTRIBUTE_TEMPLATE);

            if (blockXmlAttributeValue == null) {
                continue;
            }
            final String[] templateData = blockXmlAttributeValue.split(PHP_REFERENCE_SEPARATOR);

            if(templateData[0].isEmpty() || templateData[1].isEmpty()) {
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
            psiFileCandidates.add(psiFileCandidate);
        }

        findOverrideInTheme(project, psiFileCandidates, psiFile, templateArea);

        if (psiFileCandidates.isEmpty()) {
            return;
        }
        final String tooltipText = "Compare overridden template with the original one";
        result.add(NavigationGutterIconBuilder
                .create(AllIcons.FileTypes.Html)
                .setTargets(psiFileCandidates)
                .setTooltipText(tooltipText)
                .createLineMarkerInfo(PsiTreeUtil.getDeepestFirst(psiElement))
        );
    }

    private void findOverrideInTheme(
            final Project project,
            final List<PsiElement> psiFileCandidates,
            final PsiFile psiFile,
            final Areas templateArea
    ) {
        final List<String> themeNames = new ModuleIndex(project).getEditableThemeNames();
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
                DIRECTORY_TEMPLATES + "/")
                ;
        String blockName = null;
        final List<XmlTag> results = new ArrayList<>();

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

    private List<XmlTag> getReferenceBlockDeclarations(final Project project, final String referenceBlockName) {
        final List<XmlTag> results = new ArrayList();

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
