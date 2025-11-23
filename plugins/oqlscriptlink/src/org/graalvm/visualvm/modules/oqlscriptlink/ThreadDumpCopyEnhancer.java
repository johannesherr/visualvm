/*
 * Copyright (c) 2025, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */
package org.graalvm.visualvm.modules.oqlscriptlink;

import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.SwingUtilities;
import org.graalvm.visualvm.lib.profiler.api.ProfilerDialogs;
import org.graalvm.visualvm.lib.profiler.api.icons.GeneralIcons;
import org.graalvm.visualvm.lib.profiler.api.icons.Icons;

/**
 * Enhances the Threads view by adding a button to copy cleaned thread dump text.
 * Removes "local variable:" lines that confuse IntelliJ's thread dump analyzer.
 */
public class ThreadDumpCopyEnhancer {

    /**
     * Adds a copy button to the Threads view toolbar.
     */
    public static void enhanceThreadsView(final Object threadsView) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                try {
                    // Access the toolbar
                    Object toolbar = getToolbar(threadsView);
                    if (toolbar == null) {
                        return;
                    }

                    // Add separator
                    addSpace(toolbar, 5);
                    addSeparator(toolbar);
                    addSpace(toolbar, 5);

                    // Create copy button
                    JButton copyButton = new JButton(Icons.getIcon(GeneralIcons.SAVE));
                    copyButton.setToolTipText("Copy cleaned thread dump for IntelliJ (removes local variables)");
                    copyButton.addActionListener(new AbstractAction() {
                        public void actionPerformed(ActionEvent e) {
                            copyCleanedThreadDump(threadsView);
                        }
                    });

                    // Add button to toolbar
                    addButton(toolbar, copyButton);

                } catch (Exception e) {
                    System.err.println("Failed to enhance Threads View: " + e.getMessage());
                }
            }
        });
    }

    /**
     * Copies the cleaned thread dump text to clipboard.
     */
    private static void copyCleanedThreadDump(Object threadsView) {
        try {
            // Get the HTML text from the view
            String htmlText = getThreadDumpHtml(threadsView);

            if (htmlText == null || htmlText.isEmpty()) {
                ProfilerDialogs.displayError("No thread dump text available");
                return;
            }

            // Clean the HTML and remove local variable lines
            String cleanedText = cleanThreadDumpText(htmlText);

            // Copy to clipboard
            StringSelection selection = new StringSelection(cleanedText);
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, selection);

            System.out.println("Cleaned thread dump copied to clipboard");

        } catch (Exception e) {
            ProfilerDialogs.displayError("Failed to copy thread dump: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Extracts the thread dump HTML text from the Threads view.
     */
    private static String getThreadDumpHtml(Object threadsView) throws Exception {
        // Access private htmlView field
        Class<?> viewClass = threadsView.getClass();
        Field htmlViewField = viewClass.getDeclaredField("htmlView");
        htmlViewField.setAccessible(true);
        Object htmlView = htmlViewField.get(threadsView);

        if (htmlView == null) {
            return null;
        }

        // Access private htmlComponent field from HTMLView
        Class<?> htmlViewClass = htmlView.getClass();
        Field htmlComponentField = htmlViewClass.getDeclaredField("htmlComponent");
        htmlComponentField.setAccessible(true);
        Object htmlComponent = htmlComponentField.get(htmlView);

        if (htmlComponent == null) {
            return null;
        }

        // Call getText() on the component
        String text = (String) htmlComponent.getClass().getMethod("getText").invoke(htmlComponent);

        return text;
    }

    /**
     * Cleans the thread dump text by removing HTML tags and local variable lines.
     */
    private static String cleanThreadDumpText(String htmlText) {
        // First, strip HTML tags and decode HTML entities
        String plainText = stripHtml(htmlText);

        // Split into lines and filter out local variable lines
        String[] lines = plainText.split("\r?\n");
        List<String> cleanedLines = new ArrayList<>();

        for (String line : lines) {
            // Skip lines that start with "local variable:" (after trimming)
            String trimmed = line.trim();
            if (!trimmed.startsWith("local ") && !trimmed.isEmpty()) {
                cleanedLines.add(line);
            }
        }

        // Join back into a single string
        StringBuilder result = new StringBuilder();
        for (String line : cleanedLines) {
            result.append(line).append("\n");
        }

        return result.toString();
    }

    /**
     * Strips HTML tags and decodes HTML entities.
     */
    private static String stripHtml(String html) {
        if (html == null) {
            return "";
        }

        // Remove HTML tags
        String text = html.replaceAll("<[^>]*>", "");

        // Decode common HTML entities
        text = text.replace("&lt;", "<");
        text = text.replace("&gt;", ">");
        text = text.replace("&amp;", "&");
        text = text.replace("&nbsp;", " ");
        text = text.replace("&quot;", "\"");
        text = text.replace("&#39;", "'");

        // Normalize whitespace (but preserve line structure)
        text = text.replaceAll("[ \\t]+", " ");

        return text;
    }

    /**
     * Gets the toolbar from the view using reflection.
     */
    private static Object getToolbar(Object view) throws Exception {
        return view.getClass().getMethod("getToolbar").invoke(view);
    }

    /**
     * Adds space to the toolbar.
     */
    private static void addSpace(Object toolbar, int width) throws Exception {
        toolbar.getClass().getMethod("addSpace", int.class).invoke(toolbar, width);
    }

    /**
     * Adds separator to the toolbar.
     */
    private static void addSeparator(Object toolbar) throws Exception {
        toolbar.getClass().getMethod("addSeparator").invoke(toolbar);
    }

    /**
     * Adds button to the toolbar.
     */
    private static void addButton(Object toolbar, JButton button) throws Exception {
        toolbar.getClass().getMethod("add", java.awt.Component.class).invoke(toolbar, button);
    }
}
