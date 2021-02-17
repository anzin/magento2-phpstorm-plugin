/*
 * Copyright © Magento, Inc. All rights reserved.
 * See COPYING.txt for license details.
 */

package com.magento.idea.magento2plugin.magento.files.commands;

import com.intellij.lang.Language;
import com.jetbrains.php.lang.PhpLanguage;
import com.magento.idea.magento2plugin.actions.generation.generator.util.NamespaceBuilder;
import com.magento.idea.magento2plugin.magento.files.ModuleFileInterface;
import com.magento.idea.magento2plugin.magento.packages.Package;
import org.jetbrains.annotations.NotNull;

public class DeleteEntityByIdCommandFile implements ModuleFileInterface {
    public static final String CLASS_NAME = "DeleteByIdCommand";
    public static final String FILE_EXTENSION = "php";
    public static final String TEMPLATE = "Magento Delete Entity By Id Command Model";
    private static final String DIRECTORY = "Command";

    /**
     * Get namespace.
     *
     * @param moduleName String
     * @param entityName String
     *
     * @return String
     */
    public static @NotNull String getNamespace(
            final @NotNull String moduleName,
            final @NotNull String entityName
    ) {
        final NamespaceBuilder namespaceBuilder = new NamespaceBuilder(
                moduleName,
                DeleteEntityByIdCommandFile.CLASS_NAME,
                DeleteEntityByIdCommandFile.getDirectory(entityName)
        );

        return namespaceBuilder.getNamespace();
    }

    /**
     * Get class FQN.
     *
     * @param moduleName String
     * @param entityName String
     *
     * @return String
     */
    public static String getClassFqn(
            final @NotNull String moduleName,
            final @NotNull String entityName
    ) {
        return getNamespace(moduleName, entityName)
                .concat(Package.fqnSeparator)
                .concat(CLASS_NAME);
    }

    /**
     * Get directory for delete command file.
     *
     * @param entityName String
     *
     * @return String
     */
    public static String getDirectory(final @NotNull String entityName) {
        return DIRECTORY.concat("/" + entityName);
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
