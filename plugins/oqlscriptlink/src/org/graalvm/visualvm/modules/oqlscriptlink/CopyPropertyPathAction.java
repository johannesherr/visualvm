/*
 * Copyright (c) 2025, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */
package org.graalvm.visualvm.modules.oqlscriptlink;

import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
 * Context menu action that copies the property path to clipboard.
 * This allows users to navigate to the same property in OQL Console.
 *
 * Example: If you select foo.bar.items[2].name, it copies "bar.items[2].name"
 * which can be used in OQL as: object.bar.items[2].name
 */
@ServiceProvider(service = HeapViewerNodeAction.Provider.class)
public class CopyPropertyPathAction extends HeapViewerNodeAction.Provider {

    @Override
    public boolean supportsView(HeapContext context, String viewID) {
        // Support all java_* views (Objects, Dominators, GC Roots, etc.)
        return viewID != null && viewID.startsWith("java_");
    }

    @Override
    public HeapViewerNodeAction[] getActions(HeapViewerNode node, HeapContext context,
            HeapViewerActions actions) {

        // Only show action if this node has a parent (i.e., it's a property/field)
        if (node.getParent() != null) {
            String path = buildPropertyPath(node);
            if (path != null && !path.isEmpty()) {
                return new HeapViewerNodeAction[] {
                    new CopyPropertyPathActionImpl(path)
                };
            }
        }

        return new HeapViewerNodeAction[0];
    }

    /**
     * Builds the property path from the node hierarchy.
     */
    private String buildPropertyPath(HeapViewerNode node) {
        List<String> pathParts = new ArrayList<>();
        HeapViewerNode current = node;

        // Traverse up the tree collecting path segments
        while (current != null && current.getParent() != null) {
            String segment = getNodePathSegment(current);
            if (segment != null && !segment.isEmpty()) {
                pathParts.add(segment);
            }
            current = current.getParent();
        }

        // Reverse to get root-to-leaf order
        Collections.reverse(pathParts);

        // Join with dots, but handle array indices specially
        StringBuilder path = new StringBuilder();
        for (int i = 0; i < pathParts.size(); i++) {
            String part = pathParts.get(i);

            // If it's an array index [n], don't add a dot before it
            if (part.startsWith("[")) {
                path.append(part);
            } else {
                if (path.length() > 0) {
                    path.append(".");
                }
                path.append(part);
            }
        }

        return path.toString();
    }

    /**
     * Extracts the path segment for a single node.
     */
    private String getNodePathSegment(HeapViewerNode node) {
        // Get the node's name/display text
        String name = HeapViewerNode.getValue(node, DataType.NAME, null);

        if (name == null || name.isEmpty()) {
            return null;
        }

        // Try to extract field name or array index from the name
        // Names might be like:
        // - "fieldName" (simple field)
        // - "fieldName = value" (field with value)
        // - "[0]" (array element)
        // - "[0] = value" (array element with value)

        // Handle array indices
        if (name.startsWith("[") && name.contains("]")) {
            int endBracket = name.indexOf("]");
            return name.substring(0, endBracket + 1);
        }

        // Handle fields (extract name before '=' if present)
        int equalsIndex = name.indexOf(" = ");
        if (equalsIndex > 0) {
            return name.substring(0, equalsIndex).trim();
        }

        // Handle fields (extract name before ':' if present)
        int colonIndex = name.indexOf(":");
        if (colonIndex > 0) {
            return name.substring(0, colonIndex).trim();
        }

        // Return the whole name, but filter out non-property nodes
        // Skip aggregate nodes like "References", "Fields", etc.
        if (name.equals("References") || name.equals("Fields") ||
            name.equals("Static fields") || name.equals("Instance fields") ||
            name.equals("Items")) {
            return null;
        }

        return name.trim();
    }

    /**
     * The actual action implementation.
     */
    private static class CopyPropertyPathActionImpl extends HeapViewerNodeAction {

        private final String path;

        CopyPropertyPathActionImpl(String path) {
            super("Copy Property Path for OQL", 901);
            this.path = path;
        }

        @Override
        public boolean isMiddleButtonDefault(ActionEvent e) {
            return false;
        }

        @Override
        protected void actionPerformed(ActionEvent e) {
            // Copy to clipboard
            StringSelection selection = new StringSelection(path);
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, selection);

            System.out.println("Copied property path to clipboard: " + path);
        }
    }
}
