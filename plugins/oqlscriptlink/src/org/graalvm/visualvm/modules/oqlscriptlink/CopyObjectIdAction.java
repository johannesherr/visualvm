/*
 * Copyright (c) 2025, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */
package org.graalvm.visualvm.modules.oqlscriptlink;

import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import org.graalvm.visualvm.heapviewer.HeapContext;
import org.graalvm.visualvm.heapviewer.model.DataType;
import org.graalvm.visualvm.heapviewer.model.HeapViewerNode;
import org.graalvm.visualvm.heapviewer.ui.HeapViewerActions;
import org.graalvm.visualvm.heapviewer.ui.HeapViewerNodeAction;
import org.graalvm.visualvm.lib.jfluid.heap.Heap;
import org.graalvm.visualvm.lib.jfluid.heap.Instance;
import org.openide.util.lookup.ServiceProvider;

/**
 * Context menu action that copies an object's ID (memory address) to clipboard.
 * This allows users to use heap.findObject(id) in OQL Console.
 */
@ServiceProvider(service = HeapViewerNodeAction.Provider.class)
public class CopyObjectIdAction extends HeapViewerNodeAction.Provider {

    @Override
    public boolean supportsView(HeapContext context, String viewID) {
        // Support all java_* views (Objects, Dominators, GC Roots, etc.)
        return viewID != null && viewID.startsWith("java_");
    }

    @Override
    public HeapViewerNodeAction[] getActions(HeapViewerNode node, HeapContext context,
            HeapViewerActions actions) {

        Heap heap = context.getFragment().getHeap();
        Instance instance = HeapViewerNode.getValue(node, DataType.INSTANCE, heap);

        if (instance != null) {
            return new HeapViewerNodeAction[] {
                new CopyObjectIdActionImpl(instance)
            };
        }

        return new HeapViewerNodeAction[0];
    }

    /**
     * The actual action implementation.
     */
    private static class CopyObjectIdActionImpl extends HeapViewerNodeAction {

        private final Instance instance;

        CopyObjectIdActionImpl(Instance instance) {
            super("Copy Object ID for OQL", 900);
            this.instance = instance;
        }

        @Override
        public boolean isMiddleButtonDefault(ActionEvent e) {
            return false;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            long objectId = instance.getInstanceId();
            String idString = "0x" + Long.toHexString(objectId);

            // Copy to clipboard
            StringSelection selection = new StringSelection(idString);
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, selection);

            System.out.println("Copied object ID to clipboard: " + idString);
        }
    }
}
