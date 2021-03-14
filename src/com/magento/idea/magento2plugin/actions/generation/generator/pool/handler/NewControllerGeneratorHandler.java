/*
 * Copyright © Magento, Inc. All rights reserved.
 * See COPYING.txt for license details.
 */

package com.magento.idea.magento2plugin.actions.generation.generator.pool.handler;

import com.magento.idea.magento2plugin.actions.generation.data.NewActionEntityControllerFileData;
import com.magento.idea.magento2plugin.actions.generation.data.converter.DataObjectConverter;
import com.magento.idea.magento2plugin.actions.generation.data.dialog.GenerationContextData;
import com.magento.idea.magento2plugin.actions.generation.generator.NewActionEntityControllerFileGenerator;
import com.magento.idea.magento2plugin.actions.generation.generator.pool.GeneratorHandler;
import com.magento.idea.magento2plugin.actions.generation.generator.pool.GeneratorRunnerValidator;
import org.jetbrains.annotations.NotNull;

public class NewControllerGeneratorHandler extends GeneratorHandler {

    /**
     * New controller generator handler.
     *
     * @param contextData GenerationContextData
     * @param dataObjectConverter DataObjectConverter
     */
    public NewControllerGeneratorHandler(
            final @NotNull GenerationContextData contextData,
            final @NotNull DataObjectConverter dataObjectConverter
    ) {
        this(contextData, dataObjectConverter, null);
    }

    /**
     * New controller generator handler.
     *
     * @param contextData GenerationContextData
     * @param dataObjectConverter DataObjectConverter
     * @param runnerValidator GeneratorRunnerValidator
     */
    public NewControllerGeneratorHandler(
            final @NotNull GenerationContextData contextData,
            final @NotNull DataObjectConverter dataObjectConverter,
            final GeneratorRunnerValidator runnerValidator
    ) {
        super(contextData, dataObjectConverter, runnerValidator);
    }

    @Override
    public void generate() {
        new NewActionEntityControllerFileGenerator(
                (NewActionEntityControllerFileData) getDataObjectConverter(),
                getProject()
        ).generate(getContextData().getActionName(), true);
    }
}
