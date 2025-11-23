/*
 * Copyright (c) 2025, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */
package org.graalvm.visualvm.modules.oqlscriptlink;

import org.graalvm.visualvm.heapviewer.HeapContext;
import org.graalvm.visualvm.heapviewer.ui.HeapViewerActions;
import org.graalvm.visualvm.heapviewer.ui.HeapViewerFeature;
import org.openide.util.Lookup;
import org.openide.util.lookup.ServiceProvider;

/**
 * Provider that wraps the Threads view and enhances it with copy functionality.
 */
@ServiceProvider(service = HeapViewerFeature.Provider.class, position = 50200)
public class ThreadsViewEnhancerProvider extends HeapViewerFeature.Provider {

    @Override
    public HeapViewerFeature getFeature(HeapContext context, HeapViewerActions actions) {
        // Find the Threads view from other providers
        HeapViewerFeature threadsView = findThreadsView(context, actions);

        if (threadsView != null) {
            // Enhance the Threads view
            ThreadDumpCopyEnhancer.enhanceThreadsView(threadsView);
        }

        // Return null - we don't create a new feature, just enhance the existing one
        return null;
    }

    /**
     * Finds the Threads view from other providers.
     */
    private HeapViewerFeature findThreadsView(HeapContext context, HeapViewerActions actions) {
        for (HeapViewerFeature.Provider provider : Lookup.getDefault().lookupAll(HeapViewerFeature.Provider.class)) {
            // Skip ourselves
            if (provider == this) {
                continue;
            }

            try {
                HeapViewerFeature feature = provider.getFeature(context, actions);

                // Check if it's the JavaThreadsView
                if (feature != null && feature.getClass().getName().contains("JavaThreadsView")) {
                    return feature;
                }
            } catch (Exception e) {
                // Ignore - provider might not create a feature for this context
            }
        }

        return null;
    }
}
