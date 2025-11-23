/*
 * Copyright (c) 2025, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */
package org.graalvm.visualvm.modules.oqlscriptlink;

import org.openide.modules.ModuleInstall;
import org.openide.util.Lookup;
import org.graalvm.visualvm.heapviewer.HeapContext;
import org.graalvm.visualvm.heapviewer.ui.HeapViewerActions;
import org.graalvm.visualvm.heapviewer.ui.HeapViewerFeature;
import org.graalvm.visualvm.heapviewer.oql.OQLConsoleView;
import javax.swing.SwingUtilities;
import java.util.Collection;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;

/**
 * Manages the OQL Script Link module lifecycle.
 */
public class Installer extends ModuleInstall {

    @Override
    public void restored() {
        // The enhancement will be done when OQL Console instances are created
        // We need to hook into the HeapViewerFeature.Provider to detect when
        // OQL Console views are created

        System.out.println("OQL Script Link plugin loaded");
    }

    @Override
    public void uninstalled() {
        System.out.println("OQL Script Link plugin unloaded");
    }

    /**
     * Enhances an OQL Console view with script linking functionality.
     */
    public static void enhanceOQLConsoleIfNeeded(Object feature) {
        if (feature instanceof OQLConsoleView) {
            final OQLConsoleView oqlConsole = (OQLConsoleView) feature;

            // Delay enhancement to ensure the UI is fully constructed
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    // Delay again to ensure all components are created
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            OQLConsoleEnhancer.enhanceOQLConsole(oqlConsole);
                        }
                    });
                }
            });
        }
    }
}
