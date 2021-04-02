/*
 * Copyright © Magento, Inc. All rights reserved.
 * See COPYING.txt for license details.
 */

package com.magento.idea.magento2plugin.magento.files.actions;

import com.intellij.lang.Language;
import com.jetbrains.php.lang.PhpLanguage;
import com.magento.idea.magento2plugin.magento.files.AbstractPhpFile;
import org.jetbrains.annotations.NotNull;

public final class DeleteActionFile extends AbstractPhpFile {

    public static final String CLASS_NAME = "Delete";
    public static final String HUMAN_READABLE_NAME = "Delete controller class";
    public static final String FILE_EXTENSION = "php";
    public static final String TEMPLATE = "Magento Entity Delete Controller Class";
    private static final String DIRECTORY = "Controller/Adminhtml";
    private final String entityName;

    /**
     * Delete action file constructor.
     *
     * @param moduleName String
     * @param entityName String
     */
    public DeleteActionFile(
            final @NotNull String moduleName,
            final @NotNull String entityName
    ) {
        super(moduleName, CLASS_NAME);
        this.entityName = entityName;
    }

    /**
     * Get Directory path from the module root.
     *
     * @return String
     */
    @Override
    public String getDirectory() {
        return DIRECTORY.concat("/" + entityName);
    }

    @Override
    public String getHumanReadableName() {
        return HUMAN_READABLE_NAME;
    }

    @Override
    public String getFileName() {
        return CLASS_NAME.concat("." + FILE_EXTENSION);
    }

    @Override
    public String getTemplate() {
        return TEMPLATE;
    }

    @Override
    public Language getLanguage() {
        return PhpLanguage.INSTANCE;
    }
}
