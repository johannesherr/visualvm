/*
 * Copyright (c) 2025, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */
package org.graalvm.visualvm.modules.oqlscriptlink;

import java.awt.Component;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JSeparator;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileFilter;
import org.graalvm.visualvm.heapviewer.oql.OQLConsoleView;

/**
 * Enhances the OQL Console by adding buttons for script linking functionality.
 */
public class OQLConsoleEnhancer {

    private static File lastDirectory;

    /**
     * Adds script linking buttons to the OQL Console.
     */
    public static void enhanceOQLConsole(final OQLConsoleView oqlConsole) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                try {
                    JComponent component = oqlConsole.getComponent();
                    JToolBar toolbar = findEditorToolbar(component);

                    if (toolbar != null) {
                        addButtonsToToolbar(toolbar, oqlConsole);
                    }
                } catch (Exception e) {
                    System.err.println("Failed to enhance OQL Console: " + e.getMessage());
                }
            }
        });
    }

    /**
     * Finds the editor toolbar in the OQL Console component hierarchy.
     */
    private static JToolBar findEditorToolbar(Container container) {
        // Navigate through the component hierarchy to find the vertical toolbar
        // Structure: JExtendedSplitPane -> bottom component (EditorView) -> JToolBar

        Component splitPane = findComponentByName(container, "JExtendedSplitPane");
        if (splitPane instanceof Container) {
            // Find the bottom/lower component (EditorView)
            for (Component child : ((Container) splitPane).getComponents()) {
                if (child instanceof Container) {
                    JToolBar toolbar = findToolbar((Container) child);
                    if (toolbar != null && toolbar.getOrientation() == JToolBar.VERTICAL) {
                        return toolbar;
                    }
                }
            }
        }

        return null;
    }

    /**
     * Recursively finds a component by class name.
     */
    private static Component findComponentByName(Container container, String name) {
        if (container.getClass().getSimpleName().equals(name)) {
            return container;
        }

        for (Component child : container.getComponents()) {
            if (child instanceof Container) {
                Component found = findComponentByName((Container) child, name);
                if (found != null) {
                    return found;
                }
            }
        }

        return null;
    }

    /**
     * Recursively finds a JToolBar in the container.
     */
    private static JToolBar findToolbar(Container container) {
        if (container instanceof JToolBar) {
            return (JToolBar) container;
        }

        for (Component child : container.getComponents()) {
            if (child instanceof JToolBar) {
                return (JToolBar) child;
            }
            if (child instanceof Container) {
                JToolBar toolbar = findToolbar((Container) child);
                if (toolbar != null) {
                    return toolbar;
                }
            }
        }

        return null;
    }

    /**
     * Adds the script linking buttons to the toolbar.
     */
    private static void addButtonsToToolbar(JToolBar toolbar, final OQLConsoleView oqlConsole) {
        final OQLScriptLinkManager manager = OQLScriptLinkManager.getInstance(oqlConsole);

        // Add separator
        toolbar.add(new JSeparator());

        // Link to File button
        JButton linkButton = new JButton(new AbstractAction("Link to File...") {
            public void actionPerformed(ActionEvent e) {
                linkToFile(oqlConsole, manager);
            }
        });
        linkButton.setToolTipText("Link OQL Console to an external script file");
        toolbar.add(linkButton);

        // Execute Linked File button
        final JButton executeButton = new JButton(new AbstractAction("Execute Linked File") {
            public void actionPerformed(ActionEvent e) {
                manager.executeLinkedFile();
            }
        });
        executeButton.setToolTipText("Execute the current content of the linked file");
        executeButton.setEnabled(false);
        toolbar.add(executeButton);

        // Auto-run on Save checkbox
        final JCheckBox autoRunCheckbox = new JCheckBox("Auto-run on Save");
        autoRunCheckbox.setToolTipText("Automatically execute the script when the file is saved");
        autoRunCheckbox.setEnabled(false);
        autoRunCheckbox.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                manager.setAutoRunEnabled(autoRunCheckbox.isSelected());
            }
        });
        toolbar.add(autoRunCheckbox);

        // Clear Link button
        final JButton clearButton = new JButton(new AbstractAction("Clear Link") {
            public void actionPerformed(ActionEvent e) {
                manager.clearLink();
                executeButton.setEnabled(false);
                autoRunCheckbox.setEnabled(false);
                autoRunCheckbox.setSelected(false);
                linkButton.setToolTipText("Link OQL Console to an external script file");
            }
        });
        clearButton.setToolTipText("Clear the link to the external script file");
        clearButton.setEnabled(false);
        toolbar.add(clearButton);

        // Update link button to enable/disable other buttons
        linkButton.setAction(new AbstractAction("Link to File...") {
            public void actionPerformed(ActionEvent e) {
                File file = linkToFile(oqlConsole, manager);
                if (file != null) {
                    executeButton.setEnabled(true);
                    autoRunCheckbox.setEnabled(true);
                    clearButton.setEnabled(true);
                    linkButton.setToolTipText("Linked to: " + file.getName());
                }
            }
        });
    }

    /**
     * Shows a file chooser and links to the selected file.
     */
    private static File linkToFile(OQLConsoleView oqlConsole, OQLScriptLinkManager manager) {
        JFileChooser chooser = new JFileChooser();

        // Set initial directory
        if (lastDirectory != null && lastDirectory.exists()) {
            chooser.setCurrentDirectory(lastDirectory);
        }

        // Set file filter
        chooser.setFileFilter(new FileFilter() {
            public boolean accept(File f) {
                return f.isDirectory() || f.getName().endsWith(".oql") || f.getName().endsWith(".js");
            }

            public String getDescription() {
                return "OQL Script Files (*.oql, *.js)";
            }
        });

        chooser.setDialogTitle("Select OQL Script File");

        if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            lastDirectory = file.getParentFile();
            manager.linkToFile(file);
            return file;
        }

        return null;
    }
}
