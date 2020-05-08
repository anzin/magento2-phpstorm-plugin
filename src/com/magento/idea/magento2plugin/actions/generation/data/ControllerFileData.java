/*
 * Copyright © Magento, Inc. All rights reserved.
 * See COPYING.txt for license details.
 */

package com.magento.idea.magento2plugin.actions.generation.data;

public class ControllerFileData {
    private String actionDirectory;
    private String actionClassName;
    private String controllerModule;
    private String controllerArea;
    private String httpMethodName;
    private String acl;
    private Boolean isInheritClass;
    private String namespace;

    /**
     * Controller data file constructor.
     *
     * @param actionDirectory String
     * @param controllerClassName String
     * @param controllerModule String
     * @param controllerArea String
     * @param httpMethodName String
     * @param acl String
     * @param isInheritClass Boolean
     * @param namespace String
     */
    public ControllerFileData(
            String actionDirectory,
            String controllerClassName,
            String controllerModule,
            String controllerArea,
            String httpMethodName,
            String acl,
            Boolean isInheritClass,
            String namespace
    ) {
        this.actionDirectory = actionDirectory;
        this.actionClassName = controllerClassName;
        this.controllerModule = controllerModule;
        this.controllerArea = controllerArea;
        this.httpMethodName = httpMethodName;
        this.acl = acl;
        this.isInheritClass = isInheritClass;
        this.namespace = namespace;
    }

    /**
     * Get action directory.
     *
     * @return String
     */
    public String getActionDirectory() {
        return actionDirectory;
    }

    /**
     * Get action class name.
     *
     * @return String
     */
    public String getActionClassName() {
        return actionClassName;
    }

    /**
     * Get controller module.
     *
     * @return String
     */
    public String getControllerModule() {
        return controllerModule;
    }

    /**
     * Get namespace.
     *
     * @return String
     */
    public String getNamespace() {
        return namespace;
    }

    /**
     * Get controller area.
     *
     * @return String
     */
    public String getControllerArea() {
        return controllerArea;
    }

    /**
     * Get HTTP method name.
     *
     * @return String
     */
    public String getHttpMethodName() {
        return httpMethodName;
    }

    /**
     * Get ACL.
     *
     * @return String
     */
    public String getAcl() {
        return acl;
    }

    /**
     * Get is inherit class.
     *
     * @return String
     */
    public Boolean getIsInheritClass() {
        return isInheritClass;
    }
}
