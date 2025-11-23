/*
 * Copyright (c) 2025, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */
package org.graalvm.visualvm.modules.oqlscriptlink;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.HashMap;
import java.util.Map;
import javax.swing.SwingUtilities;
import org.graalvm.visualvm.heapviewer.oql.OQLConsoleView;

/**
 * Manages linking OQL Console to external script files with auto-reload on save.
 */
public class OQLScriptLinkManager {

    private static final Map<OQLConsoleView, OQLScriptLinkManager> instances = new HashMap<>();

    private final OQLConsoleView oqlConsole;
    private File linkedFile;
    private WatchService watchService;
    private Thread watchThread;
    private boolean autoRunEnabled;
    private volatile boolean running;

    private OQLScriptLinkManager(OQLConsoleView oqlConsole) {
        this.oqlConsole = oqlConsole;
        this.autoRunEnabled = false;
        this.running = false;
    }

    /**
     * Gets or creates a manager instance for the given OQL Console.
     */
    public static synchronized OQLScriptLinkManager getInstance(OQLConsoleView oqlConsole) {
        return instances.computeIfAbsent(oqlConsole, OQLScriptLinkManager::new);
    }

    /**
     * Removes the manager instance for the given OQL Console.
     */
    public static synchronized void removeInstance(OQLConsoleView oqlConsole) {
        OQLScriptLinkManager manager = instances.remove(oqlConsole);
        if (manager != null) {
            manager.cleanup();
        }
    }

    /**
     * Links to a script file.
     */
    public void linkToFile(File file) {
        if (file == null || !file.exists() || !file.isFile()) {
            return;
        }

        // Stop watching previous file
        stopWatching();

        this.linkedFile = file;

        // Start watching if auto-run is enabled
        if (autoRunEnabled) {
            startWatching();
        }
    }

    /**
     * Gets the currently linked file.
     */
    public File getLinkedFile() {
        return linkedFile;
    }

    /**
     * Clears the file link.
     */
    public void clearLink() {
        stopWatching();
        linkedFile = null;
    }

    /**
     * Sets whether auto-run on save is enabled.
     */
    public void setAutoRunEnabled(boolean enabled) {
        this.autoRunEnabled = enabled;

        if (enabled && linkedFile != null) {
            startWatching();
        } else {
            stopWatching();
        }
    }

    /**
     * Returns whether auto-run on save is enabled.
     */
    public boolean isAutoRunEnabled() {
        return autoRunEnabled;
    }

    /**
     * Executes the linked file content without modifying the editor.
     */
    public void executeLinkedFile() {
        if (linkedFile == null || !linkedFile.exists()) {
            return;
        }

        try {
            // Read file content
            String script = new String(Files.readAllBytes(linkedFile.toPath()), StandardCharsets.UTF_8);

            // Execute using reflection
            executeScript(script);

        } catch (IOException e) {
            showError("Failed to read script file: " + e.getMessage());
        }
    }

    /**
     * Executes a script without modifying the editor content.
     */
    private void executeScript(String script) {
        try {
            // Access private editor field
            Field editorField = OQLConsoleView.class.getDeclaredField("editor");
            editorField.setAccessible(true);
            Object editor = editorField.get(oqlConsole);

            // Access private oqlExecutor field
            Field executorField = OQLConsoleView.class.getDeclaredField("oqlExecutor");
            executorField.setAccessible(true);
            Object oqlExecutor = executorField.get(oqlConsole);

            // Get limit combo value
            Field limitComboField = OQLConsoleView.class.getDeclaredField("limitCombo");
            limitComboField.setAccessible(true);
            Object limitCombo = limitComboField.get(oqlConsole);

            // Get selected limit value
            Method getSelectedItemMethod = limitCombo.getClass().getMethod("getSelectedItem");
            Object limitValue = getSelectedItemMethod.invoke(limitCombo);

            // Get OQLQueryExecutor.runQuery method
            Class<?> executorClass = oqlExecutor.getClass();
            Method runQueryMethod = executorClass.getMethod("runQuery", String.class,
                    boolean.class, boolean.class, int.class);

            // Execute on EDT
            final String scriptToRun = script;
            final Object executorObj = oqlExecutor;
            final int limit = (Integer) limitValue;

            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    try {
                        runQueryMethod.invoke(executorObj, scriptToRun, true, true, limit);
                    } catch (Exception e) {
                        showError("Failed to execute query: " + e.getMessage());
                    }
                }
            });

        } catch (Exception e) {
            showError("Failed to execute script: " + e.getMessage());
        }
    }

    /**
     * Starts watching the linked file for changes.
     */
    private void startWatching() {
        if (linkedFile == null || watchThread != null) {
            return;
        }

        try {
            watchService = FileSystems.getDefault().newWatchService();
            Path dir = linkedFile.toPath().getParent();

            dir.register(watchService, StandardWatchEventKinds.ENTRY_MODIFY);

            running = true;
            watchThread = new Thread(new Runnable() {
                public void run() {
                    watchForChanges();
                }
            }, "OQL-Script-Watcher");
            watchThread.setDaemon(true);
            watchThread.start();

        } catch (IOException e) {
            showError("Failed to start file watcher: " + e.getMessage());
        }
    }

    /**
     * Stops watching the linked file.
     */
    private void stopWatching() {
        running = false;

        if (watchService != null) {
            try {
                watchService.close();
            } catch (IOException e) {
                // Ignore
            }
            watchService = null;
        }

        if (watchThread != null) {
            watchThread.interrupt();
            watchThread = null;
        }
    }

    /**
     * Watch loop that detects file changes.
     */
    private void watchForChanges() {
        while (running) {
            try {
                WatchKey key = watchService.take();

                for (WatchEvent<?> event : key.pollEvents()) {
                    WatchEvent.Kind<?> kind = event.kind();

                    if (kind == StandardWatchEventKinds.OVERFLOW) {
                        continue;
                    }

                    WatchEvent<Path> ev = (WatchEvent<Path>) event;
                    Path filename = ev.context();

                    // Check if it's our file
                    if (linkedFile != null && filename.toString().equals(linkedFile.getName())) {
                        // File was modified - wait a bit for the write to complete
                        Thread.sleep(100);

                        // Execute the script
                        executeLinkedFile();
                    }
                }

                key.reset();

            } catch (InterruptedException e) {
                break;
            } catch (Exception e) {
                // Continue watching
            }
        }
    }

    /**
     * Cleans up resources.
     */
    private void cleanup() {
        stopWatching();
        linkedFile = null;
    }

    /**
     * Shows an error message.
     */
    private void showError(final String message) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                try {
                    Class<?> dialogsClass = Class.forName("org.graalvm.visualvm.lib.profiler.api.ProfilerDialogs");
                    Method displayErrorMethod = dialogsClass.getMethod("displayError", String.class);
                    displayErrorMethod.invoke(null, message);
                } catch (Exception e) {
                    System.err.println("Error: " + message);
                }
            }
        });
    }
}
