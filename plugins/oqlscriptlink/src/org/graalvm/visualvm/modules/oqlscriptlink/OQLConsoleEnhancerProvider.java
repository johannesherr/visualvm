/*
 * Copyright (c) 2025, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */
package org.graalvm.visualvm.modules.oqlscriptlink;

import org.graalvm.visualvm.heapviewer.HeapContext;
import org.graalvm.visualvm.heapviewer.oql.OQLConsoleView;
import org.graalvm.visualvm.heapviewer.ui.HeapViewerActions;
import org.graalvm.visualvm.heapviewer.ui.HeapViewerFeature;
import org.openide.util.Lookup;
import org.openide.util.lookup.ServiceProvider;

/**
 * Provider that wraps the OQL Console and enhances it with script linking functionality.
 */
@ServiceProvider(service = HeapViewerFeature.Provider.class, position = 50100)
public class OQLConsoleEnhancerProvider extends HeapViewerFeature.Provider {

    @Override
    public HeapViewerFeature getFeature(HeapContext context, HeapViewerActions actions) {
        // Find the OQL Console provider
        OQLConsoleView oqlConsole = findOQLConsole(context, actions);

        if (oqlConsole != null) {
            // Enhance the OQL Console
            Installer.enhanceOQLConsoleIfNeeded(oqlConsole);
        }

        // Return null - we don't create a new feature, just enhance the existing one
        return null;
    }

    /**
     * Finds the OQL Console view from other providers.
     */
    private OQLConsoleView findOQLConsole(HeapContext context, HeapViewerActions actions) {
        for (HeapViewerFeature.Provider provider : Lookup.getDefault().lookupAll(HeapViewerFeature.Provider.class)) {
            // Skip ourselves
            if (provider == this) {
                continue;
            }

            try {
                HeapViewerFeature feature = provider.getFeature(context, actions);
                if (feature instanceof OQLConsoleView) {
                    return (OQLConsoleView) feature;
                }
            } catch (Exception e) {
                // Ignore - provider might not create a feature for this context
            }
        }

        return null;
    }
}
