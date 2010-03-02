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
import java.io.*;

import javax.swing.*;
import javax.swing.table.*;

import com.sun.max.ins.*;
import com.sun.max.ins.InspectionSettings.*;
import com.sun.max.lang.*;
import com.sun.max.memory.*;
import com.sun.max.program.*;
import com.sun.max.program.option.*;
import com.sun.max.tele.*;
import com.sun.max.tele.object.*;
import com.sun.max.unsafe.*;
import com.sun.max.util.*;

/**
 * The main GUI window for an inspection of a Maxine VM, with related GUI services.
 * Contains multiple instances of {@link Inspector} in a {@link JDesktopPane}.
 *
 * @author Michael Van De Vanter
 */
public final class InspectorMainFrame extends JFrame implements InspectorGUI, Prober {

    private static final int TRACE_VALUE = 2;
    private static final int MOUSE_TRACE_VALUE = 3;

    private static final String FRAME_SETTINGS_NAME = "inspectorMainFrame";
    private static final String FRAME_X_KEY = "frameX";
    private static final String FRAME_Y_KEY = "frameY";
    private static final String FRAME_HEIGHT_KEY = "frameHeight";
    private static final String FRAME_WIDTH_KEY = "frameWidth";

    /**
     * A mouse location to cache when no other location is available.
     */
    private static final Point DEFAULT_LOCATION = new Point(100, 100);

    private static final DataFlavor[] supportedDropDataFlavors =
        new DataFlavor[] {InspectorTransferable.ADDRESS_FLAVOR,
            InspectorTransferable.MEMORY_REGION_FLAVOR,
            InspectorTransferable.TELE_OBJECT_FLAVOR};

    /**
     * Records the last position of the mouse when it was over a component.
     */
    private final class MouseLocationListener implements AWTEventListener {

        public void eventDispatched(AWTEvent awtEvent) {
            final Component source = (Component) awtEvent.getSource();
            if (source != null) {
                mostRecentMouseLocation = DEFAULT_LOCATION;
                // We should only be getting mouse events; others are masked out.
                final MouseEvent mouseEvent = (MouseEvent) awtEvent;
                try {
                    final Point eventLocationOnScreen = source.getLocationOnScreen();
                    eventLocationOnScreen.translate(mouseEvent.getX(), mouseEvent.getY());
                    recordMostRecentMouseLocation(eventLocationOnScreen);
                } catch (IllegalComponentStateException e) {
                }
            }
        }
    }

    /**
     * Support for the desktop pane to act as a Drag and Drop <em>target</em>.
     * Only copy operations are supported.
     *
     * @author Michael Van De Vanter
     */
    private final class MainFrameTransferHandler extends TransferHandler {

        @Override
        public boolean canImport(TransferHandler.TransferSupport support) {
            // Only support drops
            if (!support.isDrop()) {
                return false;
            }
            // Only support copies:
            if ((COPY & support.getSourceDropActions()) == 0) {
                return false;
            }
            // Only support the enumerated data flavors.
            if (!Arrays.containsAny(support.getDataFlavors(),  supportedDropDataFlavors)) {
                return false;
            }

            // Only support drop over the background
            // Location arrives in coordinates of the content pane of the desktop
            TransferHandler.DropLocation dropLocation = support.getDropLocation();
            Component component = desktopPane.getComponentAt(dropLocation.getDropPoint());
            if (component != null && (component instanceof InspectorInternalFrame)) {
                return false;
            }

            support.setShowDropLocation(true);
            support.setDropAction(COPY);
            return true;
        }

        @Override
        public boolean importData(TransferHandler.TransferSupport support) {
            if (!canImport(support)) {
                return false;
            }

            //Remember where the mouse was when dropped; will guide creation of new window if needed.
            final Point dropPoint = support.getDropLocation().getDropPoint();
            final Point eventLocationOnScreen = support.getComponent().getLocationOnScreen();
            eventLocationOnScreen.translate(dropPoint.x, dropPoint.y);
            recordMostRecentMouseLocation(eventLocationOnScreen);

            final Transferable transferable = support.getTransferable();
            try {
                if (support.isDataFlavorSupported(InspectorTransferable.ADDRESS_FLAVOR)) {
                    final Address address = (Address) transferable.getTransferData(InspectorTransferable.ADDRESS_FLAVOR);
                    Trace.line(TRACE_VALUE, tracePrefix + "address dropped on desktop");
                    InspectorMainFrame.this.inspection.actions().inspectMemoryWords(address).perform();
                    return true;
                }
                if (support.isDataFlavorSupported(InspectorTransferable.MEMORY_REGION_FLAVOR)) {
                    final MemoryRegion memoryRegion = (MemoryRegion) transferable.getTransferData(InspectorTransferable.MEMORY_REGION_FLAVOR);
                    Trace.line(TRACE_VALUE, tracePrefix + "memory region dropped on desktop");
                    InspectorMainFrame.this.inspection.actions().inspectMemoryWords(memoryRegion).perform();
                    return true;
                }
                if (support.isDataFlavorSupported(InspectorTransferable.TELE_OBJECT_FLAVOR)) {
                    final TeleObject teleObject = (TeleObject) transferable.getTransferData(InspectorTransferable.TELE_OBJECT_FLAVOR);
                    Trace.line(TRACE_VALUE, tracePrefix + "teleObject dropped on desktop");
                    InspectorMainFrame.this.inspection.focus().setHeapObject(teleObject);
                    return true;
                }

            } catch (UnsupportedFlavorException e) {
                e.printStackTrace();
                ProgramError.unexpected("Attempt to drop an unsupported data flavor");
            } catch (IOException e) {
                e.printStackTrace();
                ProgramError.unexpected("Unknown drop failure");
            }
            return false;
        }
    }

    private final Inspection inspection;
    private final String tracePrefix;
    private final InspectorNameDisplay nameDisplay;
    private final SaveSettingsListener saveSettingsListener;
    private final Cursor busyCursor = new Cursor(Cursor.WAIT_CURSOR);
    private final JDesktopPane desktopPane;
    private final JScrollPane scrollPane;
    private final InspectorMainMenuBar menuBar;
    private final InspectorPopupMenu desktopMenu = new InspectorPopupMenu("Maxine Inspector");
    private final JLabel unavailableDataTableCellRenderer;

    /**
     * Location in absolute screen coordinates of the most recent mouse location of interest.
     */
    private Point mostRecentMouseLocation = DEFAULT_LOCATION;

    /**
     * Creates a new main window frame for the Maxine VM inspection session.
     *
     * @param inspection the inspection's main state: {@link Inspection#actions()} and
     * {@link Inspection#settings()} must already be initialized.
     * @param inspectorName the name to display on the window.
     * @param nameDisplay provides standard naming service
     * @param settings settings manager for the session, already initialized
     * @param actions available for the session, already initialized
     */
    public InspectorMainFrame(Inspection inspection, String inspectorName, InspectorNameDisplay nameDisplay, InspectionSettings settings, InspectionActions actions) {
        super(inspectorName);
        this.tracePrefix = "[" + getClass().getSimpleName() + "] ";
        this.inspection = inspection;
        this.nameDisplay = nameDisplay;

        setDefaultLookAndFeelDecorated(true);
        Toolkit.getDefaultToolkit().addAWTEventListener(new MouseLocationListener(), AWTEvent.MOUSE_EVENT_MASK);

        // Set default geometry; may get overridden by settings when initialized
        setMinimumSize(inspection.geometry().inspectorFrameMinSize());
        setPreferredSize(inspection.geometry().inspectorFramePrefSize());
        setLocation(inspection.geometry().inspectorFrameDefaultLocation());

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent windowevent) {
                InspectorMainFrame.this.inspection.quit();
            }
        });
        desktopPane = new JDesktopPane() {

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
        desktopPane.setOpaque(true);
        scrollPane = new InspectorScrollPane(inspection, desktopPane);
        setContentPane(scrollPane);
        menuBar = new InspectorMainMenuBar(actions);
        setJMenuBar(menuBar);

        desktopMenu.add(actions.viewBootImage());
        desktopMenu.add(actions.viewBreakpoints());
        desktopMenu.add(actions.memoryWordsInspectorsMenu());
        desktopMenu.add(actions.viewMemoryRegions());
        desktopMenu.add(actions.viewMethodCode());
        desktopMenu.add(actions.objectInspectorsMenu());
        desktopMenu.add(actions.viewRegisters());
        desktopMenu.add(actions.viewStack());
        desktopMenu.add(actions.viewThreads());
        desktopMenu.add(actions.viewVmThreadLocals());
        if (inspection.vm().watchpointManager() != null) {
            desktopMenu.add(actions.viewWatchpoints());
        }

        desktopPane.addMouseListener(new InspectorMouseClickAdapter(inspection) {
            @Override
            public void procedure(final MouseEvent mouseEvent) {
                if (Inspection.mouseButtonWithModifiers(mouseEvent) == MouseEvent.BUTTON3) {
                    desktopMenu.show(mouseEvent.getComponent(), mouseEvent.getX(), mouseEvent.getY());
                }
            }
        });
        unavailableDataTableCellRenderer = new UnavailableDataTableCellRenderer(inspection);
        saveSettingsListener = new AbstractSaveSettingsListener(FRAME_SETTINGS_NAME) {
            public void saveSettings(SaveSettingsEvent saveSettingsEvent) {
                final Rectangle bounds = getBounds();
                saveSettingsEvent.save(FRAME_X_KEY, bounds.x);
                saveSettingsEvent.save(FRAME_Y_KEY, bounds.y);
                saveSettingsEvent.save(FRAME_WIDTH_KEY, bounds.width);
                saveSettingsEvent.save(FRAME_HEIGHT_KEY, bounds.height);
            }
        };
        settings.addSaveSettingsListener(saveSettingsListener);

        pack();

        try {
            if (settings.containsKey(saveSettingsListener, FRAME_X_KEY)) {
                // Adjust any negative (off-screen) locations to be on-screen.
                final int x = Math.max(settings.get(saveSettingsListener, FRAME_X_KEY, OptionTypes.INT_TYPE, -1), 0);
                final int y = Math.max(settings.get(saveSettingsListener, FRAME_Y_KEY, OptionTypes.INT_TYPE, -1), 0);
                // Adjust any excessive window size to fit within the screen
                final Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
                final int width = Math.min(settings.get(saveSettingsListener, FRAME_WIDTH_KEY, OptionTypes.INT_TYPE, -1), screenSize.width);
                final int height = Math.min(settings.get(saveSettingsListener, FRAME_HEIGHT_KEY, OptionTypes.INT_TYPE, -1), screenSize.height);
                setBounds(x, y, width, height);
            }
        } catch (Option.Error optionError) {
            ProgramWarning.message("Inspector Main Frame settings: " + optionError.getMessage());
        }

        desktopPane.setTransferHandler(new MainFrameTransferHandler());

    }

    public void addInspector(Inspector inspector) {
        final JComponent component = inspector.getJComponent();
        desktopPane.add(component);
        component.setVisible(true);
        repaint();
    }

    public void removeInspectors(Predicate<Inspector> predicate) {
        for (int i = desktopPane.getComponentCount() - 1; i >= 0; i--) {
            // Delete backwards so that the indices don't change
            final Component component = desktopPane.getComponent(i);
            if (component instanceof InspectorInternalFrame) {
                final InspectorFrame inspectorFrame = (InspectorFrame) component;
                final Inspector inspector = inspectorFrame.inspector();
                if (predicate.evaluate(inspector)) {
                    inspector.dispose();
                }
            }
        }
    }

    public Inspector findInspector(Predicate<Inspector> predicate) {
        final int componentCount = desktopPane.getComponentCount();
        for (int i = 0; i < componentCount; i++) {
            final Component component = desktopPane.getComponent(i);
            if (component instanceof InspectorInternalFrame) {
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
        return null;
    }

    public void showInspectorBusy(boolean busy) {
        if (busy) {
            desktopPane.setCursor(busyCursor);
        } else {
            desktopPane.setCursor(null);
        }
    }

    public void informationMessage(String message, String title) {
        JOptionPane.showMessageDialog(desktopPane, message, title, JOptionPane.INFORMATION_MESSAGE);
    }

    public void informationMessage(String message) {
        informationMessage(message, inspection.currentActionTitle());
    }

    public void errorMessage(String message, String title) {
        JOptionPane.showMessageDialog(desktopPane, message, title, JOptionPane.ERROR_MESSAGE);
    }

    public void errorMessage(String message) {
        errorMessage(message, inspection.currentActionTitle());
    }

    public String inputDialog(String message, String initialValue) {
        return JOptionPane.showInputDialog(desktopPane, message, initialValue);
    }

    public boolean yesNoDialog(String message) {
        return JOptionPane.showConfirmDialog(this, message, inspection.currentActionTitle(), JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
    }

    public String questionMessage(String message) {
        return JOptionPane.showInputDialog(desktopPane, message, inspection.currentActionTitle(), JOptionPane.QUESTION_MESSAGE);
    }

    public void postToClipboard(String text) {
        final StringSelection selection = new StringSelection(text);
        getToolkit().getSystemClipboard().setContents(selection, selection);
    }

    public void setLocationRelativeToMouse(Inspector inspector) {
        setLocationRelativeToMouse(inspector, inspection.geometry().defaultNewFrameXOffset(), inspection.geometry().defaultNewFrameYOffset());
    }

    public void setLocationRelativeToMouse(Inspector inspector, int offset) {
        setLocationRelativeToMouse(inspector, offset, offset);
    }

    public void moveToMiddle(Inspector inspector) {
        final JComponent component = inspector.getJComponent();
        component.setLocation(getMiddle(component));
    }

    public void moveToMiddleIfNotVisble(Inspector inspector) {
        if (!contains(inspector.getJComponent().getLocation())) {
            moveToMiddle(inspector);
        }
    }

    public void setLocationRelativeToMouse(JDialog dialog, int offset) {
        final Point location = mostRecentMouseLocation;
        location.translate(offset, offset);
        dialog.setLocation(location);
    }

    public void moveToMiddle(JDialog dialog) {
        final Point middle = getMiddle(dialog);
        middle.translate(getX(), getY());
        dialog.setLocation(middle);
    }

    public Frame frame() {
        return this;
    }

    public JLabel getUnavailableDataTableCellRenderer() {
        return unavailableDataTableCellRenderer;
    }

    private Point getMiddle(Component component) {
        final Point point = new Point((getWidth() / 2) - (component.getWidth() / 2), (getHeight() / 2) - (component.getHeight() / 2));
        if (point.y < 0) {
            point.y = 0;
        }
        return point;
    }

    private Point getMiddle(InspectorInternalFrame frame) {
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
        final Point location = mostRecentMouseLocation;
        location.translate(xOffset, yOffset);
        setLocationOnScreen(inspector, location);
    }

    private void setLocationOnScreen(Inspector inspector, Point locationOnScreen) {
        final Point origin = getContentPane().getLocationOnScreen();
        final Point location = new Point(locationOnScreen.x - origin.x, locationOnScreen.y - origin.y);
        final Rectangle r = getBounds();
        final JComponent frame = inspector.getJComponent();

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

    /**
     * Records the most recent mouse event of interest.
     *
     * @param point mouse location in absolute screen coordinates.
     */
    private void recordMostRecentMouseLocation(Point point) {
        Trace.line(MOUSE_TRACE_VALUE, InspectorMainFrame.this.tracePrefix + "Recording mouse location=" + point);
        mostRecentMouseLocation = point;
    }

    public void redisplay() {
        refresh(true);
    }

    private MaxVMState lastRefreshedState = null;

    public void refresh(boolean force) {
        final MaxVMState maxVMState = inspection.vm().state();
        if (maxVMState.newerThan(lastRefreshedState)) {
            lastRefreshedState = maxVMState;
            setTitle(inspection.currentInspectionTitle());
            switch (maxVMState.processState()) {
                case STOPPED:
                    if (maxVMState.isInGC()) {
                        menuBar.setStateColor(inspection.style().vmStoppedinGCBackgroundColor());
                    } else {
                        menuBar.setStateColor(inspection.style().vmStoppedBackgroundColor());
                    }
                    break;
                case RUNNING:
                case UNKNOWN:
                    menuBar.setStateColor(inspection.style().vmRunningBackgroundColor());
                    break;
                case TERMINATED:
                    menuBar.setStateColor(inspection.style().vmTerminatedBackgroundColor());
                    break;
                default:
                    ProgramError.unknownCase(maxVMState.processState().toString());
            }
        }
        repaint();
    }

    private final class UnavailableDataTableCellRenderer extends JLabel implements TableCellRenderer, TextSearchable, Prober {

        UnavailableDataTableCellRenderer(Inspection inspection) {
            setText(inspection.nameDisplay().unavailableDataShortText());
            setToolTipText(inspection.nameDisplay().unavailableDataLongText());
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            return this;
        }

        public String getSearchableText() {
            return null;
        }

        public void redisplay() {
        }

        public void refresh(boolean force) {
        }

    }
}
