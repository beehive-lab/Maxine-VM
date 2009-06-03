/*
 * Copyright (c) 2007 Sun Microsystems, Inc.  All rights reserved.
 *
 * Sun Microsystems, Inc. has intellectual property rights relating to technology embodied in the product
 * that is described in this document. In particular, and without limitation, these intellectual property
 * rights may include one or more of the U.S. patents listed at http://www.sun.com/patents and one or
 * more additional patents or pending patent applications in the U.S. and in other countries.
 *
 * U.S. Government Rights - Commercial software. Government users are subject to the Sun
 * Microsystems, Inc. standard license agreement and applicable provisions of the FAR and its
 * supplements.
 *
 * Use is subject to license terms. Sun, Sun Microsystems, the Sun logo, Java and Solaris are trademarks or
 * registered trademarks of Sun Microsystems, Inc. in the U.S. and other countries. All SPARC trademarks
 * are used under license and are trademarks or registered trademarks of SPARC International, Inc. in the
 * U.S. and other countries.
 *
 * UNIX is a registered trademark in the U.S. and other countries, exclusively licensed through X/Open
 * Company, Ltd.
 */
package com.sun.max.ins.gui;

import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.event.*;

import javax.swing.*;

import com.sun.max.ins.*;
import com.sun.max.ins.Inspection.*;
import com.sun.max.ins.InspectionSettings.*;
import com.sun.max.lang.*;
import com.sun.max.program.*;
import com.sun.max.program.option.*;
import com.sun.max.util.*;


/**
 * The main GUI window for an inspection of a Maxine VM, with related GUI services.
 * Contains multiple instances of {@link Inspector} in a {@link JDesktopPane}.
 *
 * @author Michael Van De Vanter
 *
 */
public final class InspectorMainFrame extends JFrame implements InspectorGUI {

    private final Inspection _inspection;
    private final Cursor _busyCursor = new Cursor(Cursor.WAIT_CURSOR);
    private final JDesktopPane _desktopPane;
    private final JScrollPane _scrollPane;
    private final InspectorMenu _desktopMenu = new InspectorMenu();

    /**
     * Manages saving and restoring the geometry of the main GUI window, accounting
     * for possibly differing screen sizes across sessions, and correcting any off-screen
     * locations.
     *
     * @author Michael Van De Vanter
     */
    private class InspectorMainFrameSaveSettingsListener extends AbstractSaveSettingsListener {

        private static final String FRAME_SETTINGS_NAME = "inspectorMainFrame";
        private static final String FRAME_X_KEY = "frameX";
        private static final String FRAME_Y_KEY = "frameY";
        private static final String FRAME_HEIGHT_KEY = "frameHeight";
        private static final String FRAME_WIDTH_KEY = "frameWidth";

        private final JFrame _frame;

        public InspectorMainFrameSaveSettingsListener(JFrame frame, InspectionSettings settings) {
            super(FRAME_SETTINGS_NAME);
            _frame = frame;
            settings.addSaveSettingsListener(this);
            try {
                if (settings.containsKey(this, FRAME_X_KEY)) {
                    // Adjust any negative (off-screen) locations to be on-screen.
                    final int x = Math.max(settings.get(this, FRAME_X_KEY, OptionTypes.INT_TYPE, -1), 0);
                    final int y = Math.max(settings.get(this, FRAME_Y_KEY, OptionTypes.INT_TYPE, -1), 0);
                    // Adjust any excessive window size to fit within the screen
                    final Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
                    final int width = Math.min(settings.get(this, FRAME_WIDTH_KEY, OptionTypes.INT_TYPE, -1), screenSize.width);
                    final int height = Math.min(settings.get(this, FRAME_HEIGHT_KEY, OptionTypes.INT_TYPE, -1), screenSize.height);
                    _frame.setBounds(x, y, width, height);
                }
            } catch (Option.Error optionError) {
                ProgramWarning.message("Inspector Main Frame settings: " + optionError.getMessage());
            }
        }

        @Override
        public void saveSettings(SaveSettingsEvent saveSettingsEvent) {
            final Rectangle bounds = _frame.getBounds();
            saveSettingsEvent.save(FRAME_X_KEY, bounds.x);
            saveSettingsEvent.save(FRAME_Y_KEY, bounds.y);
            saveSettingsEvent.save(FRAME_WIDTH_KEY, bounds.width);
            saveSettingsEvent.save(FRAME_HEIGHT_KEY, bounds.height);
        }
    }

    /**
     * Creates a new main window frame for the Maxine VM inspection session.
     *
     * @param inspection the inspection's main state: {@link Inspection#actions()} and
     * {@link Inspection#settings()} must already be initialized.
     * @param inspectorName the name to display on the window.
     * @param settings settings manager for the session, already initialized
     * @param actions available for the session, already initialized
     */
    public InspectorMainFrame(Inspection inspection, String inspectorName, InspectionSettings settings, InspectionActions actions) {
        super(inspectorName);
        _inspection = inspection;

        setDefaultLookAndFeelDecorated(true);
        InspectorFrame.TitleBarListener.initialize();

        // Set default geometry; may get overridden by settings when initialized
        setMinimumSize(inspection.geometry().inspectorFrameMinSize());
        setPreferredSize(inspection.geometry().inspectorFramePrefSize());
        setLocation(inspection.geometry().inspectorFrameDefaultLocation());

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent windowevent) {
                _inspection.quit();
            }
        });
        _desktopPane = new JDesktopPane() {

            /**
             * Any component added to the desktop pane is brought to the front.
             */
            @Override
            public Component add(Component component) {
                super.add(component);
                moveToFront(component);
                return component;
            }
        };
        _desktopPane.setOpaque(true);
        _scrollPane = new InspectorScrollPane(_inspection, _desktopPane);
        setContentPane(_scrollPane);
        setJMenuBar(new InspectorMainMenuBar(actions));

        _desktopMenu.add(actions.viewBootImage());
        _desktopMenu.add(actions.viewMemoryRegions());
        _desktopMenu.add(actions.viewThreads());
        _desktopMenu.add(actions.viewVmThreadLocals());
        _desktopMenu.add(actions.viewRegisters());
        _desktopMenu.add(actions.viewStack());
        _desktopMenu.add(actions.viewMethodCode());
        _desktopMenu.add(actions.viewBreakpoints());

        _desktopPane.addMouseListener(new InspectorMouseClickAdapter(_inspection) {

            @Override
            public void procedure(final MouseEvent mouseEvent) {
                if (MaxineInspector.mouseButtonWithModifiers(mouseEvent) == MouseEvent.BUTTON3) {
                    _desktopMenu.popupMenu().show(mouseEvent.getComponent(), mouseEvent.getX(), mouseEvent.getY());
                }
            }
        });
        pack();
        settings.addSaveSettingsListener(new InspectorMainFrameSaveSettingsListener(this, settings));
    }

    public void addInspector(Inspector inspector) {
        final InspectorFrame inspectorFrame = inspector.frame();
        _desktopPane.add((Component) inspectorFrame);
        inspectorFrame.setVisible(true);
        repaint();
    }

    /**
     * Removes and disposes all top-level instances of {@link Inspector} in the GUI display.
     */
    public void removeInspectors(Predicate<Inspector> predicate) {
        for (int i = _desktopPane.getComponentCount() - 1; i >= 0; i--) {
            // Delete backwards so that the indices don't change
            final Component component = _desktopPane.getComponent(i);
            if (component instanceof InspectorFrame) {
                final InspectorFrame inspectorFrame = (InspectorFrame) component;
                final Inspector inspector = inspectorFrame.inspector();
                if (predicate.evaluate(inspector)) {
                    inspector.dispose();
                }
            }
        }
    }

    public Inspector findInspector(Predicate<Inspector> predicate) {
        final int componentCount = _desktopPane.getComponentCount();
        for (int i = 0; i < componentCount; i++) {
            final Component component = _desktopPane.getComponent(i);
            if (component instanceof InspectorFrame) {
                final InspectorFrame inspectorFrame = (InspectorFrame) component;
                final Inspector inspector = inspectorFrame.inspector();
                if (predicate.evaluate(inspector)) {
                    return inspector;
                }
                // This component may contain other InspectorFrames, e.g. if it is related to a tabbed frame.
                if (inspector instanceof InspectorContainer) {
                    final InspectorContainer<? extends Inspector> inspectorContainer = StaticLoophole.cast(inspector);
                    for (Inspector containedInspector : inspectorContainer) {
                        if (predicate.evaluate(containedInspector)) {
                            return containedInspector;
                        }
                    }
                }
            }
        }
        // Legacy hack; probably not relevant now.
        for (Frame frame : Frame.getFrames()) {
            if (frame.isVisible() && frame instanceof InspectorFrame) {
                final InspectorFrame inspectorFrame = (InspectorFrame) frame;
                final Inspector inspector = inspectorFrame.inspector();
                if (predicate.evaluate(inspector)) {
                    return inspector;
                }
            }
        }
        return null;
    }

    public void showTitle(String string) {
        setTitle(string);
        repaint();
    }

    public void showVMState(InspectionState inspectionState) {
        final InspectorMainMenuBar menuBar = (InspectorMainMenuBar) getJMenuBar();
        switch (inspectionState) {
            case STOPPED_IN_GC:
                menuBar.setStateColor(_inspection.style().vmStoppedinGCBackgroundColor());
                break;
            case STOPPED:
                menuBar.setStateColor(_inspection.style().vmStoppedBackgroundColor());
                break;
            case RUNNING:
                menuBar.setStateColor(_inspection.style().vmRunningBackgroundColor());
                break;
            case TERMINATED:
                menuBar.setStateColor(_inspection.style().vmTerminatedBackgroundColor());
                break;
            default:
                ProgramError.unknownCase(inspectionState.toString());
        }
    }

    public void showBusy(boolean busy) {
        if (busy) {
            _desktopPane.setCursor(_busyCursor);
        } else {
            _desktopPane.setCursor(null);
        }
    }

    /**
     * Displays an information message in a modal dialog with specified frame title.
     */
    public void informationMessage(String message, String title) {
        JOptionPane.showMessageDialog(_desktopPane, message, title, JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * Displays an information message in a modal dialog with default frame title.
     */
    public void informationMessage(String message) {
        informationMessage(message, _inspection.currentActionTitle());
    }

    /**
     * Displays an error message in a modal dialog with specified frame title.
     */
    public void errorMessage(String message, String title) {
        JOptionPane.showMessageDialog(_desktopPane, message, title, JOptionPane.ERROR_MESSAGE);
    }

    /**
     * Displays an error message in a modal dialog with default frame title.
     */
    public void errorMessage(String message) {
        errorMessage(message, _inspection.currentActionTitle());
    }

    /**
     * Collects textual input from user.
     *
     * @param message a prompt
     * @param initialValue an initial value
     * @return text typed by user
     */
    public String inputDialog(String message, String initialValue) {
        return JOptionPane.showInputDialog(_desktopPane, message, initialValue);
    }

    public boolean yesNoDialog(String message) {
        return JOptionPane.showConfirmDialog(this, message, _inspection.currentActionTitle(), JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
    }

    /**
     * Displays a message and invites text input from user in a modal dialog.
     *
     * @return text typed by user.
     */
    public String questionMessage(String message) {
        return JOptionPane.showInputDialog(_desktopPane, message, _inspection.currentActionTitle(), JOptionPane.QUESTION_MESSAGE);
    }

    public void postToClipboard(String text) {
        final StringSelection selection = new StringSelection(text);
        getToolkit().getSystemClipboard().setContents(selection, selection);
    }

    public void setLocationRelativeToMouse(Inspector inspector) {
        setLocationRelativeToMouse(inspector, _inspection.geometry().defaultNewFrameXOffset(), _inspection.geometry().defaultNewFrameYOffset());
    }

    /**
     * Set frame location to a point displayed by specified diagonal amount from the most recently known mouse position.
     */
    public void setLocationRelativeToMouse(Inspector inspector, int offset) {
        setLocationRelativeToMouse(inspector, offset, offset);
    }

    public void moveToMiddle(Inspector inspector) {
        final InspectorFrame frame = inspector.frame();
        frame.setLocation(getMiddle(frame));
    }

    public void moveToMiddle(JDialog dialog) {
        final Point middle = getMiddle(dialog);
        middle.translate(getX(), getY());
        dialog.setLocation(middle);
    }

    public Frame frame() {
        return this;
    }

    private Point getMiddle(Component component) {
        final Point point = new Point((getWidth() / 2) - (component.getWidth() / 2), (getHeight() / 2) - (component.getHeight() / 2));
        if (point.y < 0) {
            point.y = 0;
        }
        return point;
    }

    private Point getMiddle(InspectorFrame frame) {
        final Point point = new Point((getWidth() / 2) - (frame.getWidth() / 2), (getHeight() / 2) - (frame.getHeight() / 2));
        if (point.y < 0) {
            point.y = 0;
        }
        return point;
    }

    /**
     * Set frame location to a point displaced by specified amount from the most recently known mouse position.
     */
    private void setLocationRelativeToMouse(Inspector inspector, int xOffset, int yOffset) {
        final Point location = InspectorFrame.TitleBarListener.recentMouseLocationOnScreen();
        location.translate(xOffset, yOffset);
        setLocationOnScreen(inspector, location);
    }

    private void setLocationOnScreen(Inspector inspector, Point locationOnScreen) {
        final Point origin = getContentPane().getLocationOnScreen();
        final Point location = new Point(locationOnScreen.x - origin.x, locationOnScreen.y - origin.y);
        final Rectangle r = getBounds();
        final InspectorFrame frame = inspector.frame();

        if (frame.getWidth() > r.width) {
            frame.setSize(r.width, frame.getHeight());
        }
        if (frame.getHeight() > r.height) {
            frame.setSize(frame.getWidth(), r.height);
        }

        if (location.x <= -frame.getWidth()) {
            location.x = 0;
        } else if (location.x >= r.width) {
            location.x = r.width - frame.getWidth();
        }

        if (location.y < 0) {
            location.y = 0;
        } else if (location.y >= r.height) {
            location.y = r.height - frame.getHeight();
        }
        frame.setLocation(location);
    }

}
