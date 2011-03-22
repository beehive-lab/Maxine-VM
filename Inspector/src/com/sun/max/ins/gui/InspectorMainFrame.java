/*
 * Copyright (c) 2009, 2011, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.sun.max.ins.gui;

import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.event.*;
import java.io.*;

import javax.swing.*;
import javax.swing.table.*;

import com.sun.max.*;
import com.sun.max.ins.*;
import com.sun.max.ins.InspectionSettings.AbstractSaveSettingsListener;
import com.sun.max.ins.InspectionSettings.SaveSettingsEvent;
import com.sun.max.ins.InspectionSettings.SaveSettingsListener;
import com.sun.max.ins.util.*;
import com.sun.max.program.*;
import com.sun.max.program.option.*;
import com.sun.max.tele.*;
import com.sun.max.tele.object.*;
import com.sun.max.unsafe.*;
import com.sun.max.util.*;

/**
 * The main GUI window for an inspection of a VM, with related GUI services.
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

        /**
         * Returns true iff there is at least one element that is contained in both arrays.
         */
        private boolean containsAny(DataFlavor[] array1, DataFlavor[] array2) {
            for (DataFlavor element1 : array1) {
                for (DataFlavor element2 : array2) {
                    if (element1 == element2) {
                        return true;
                    }
                }
            }
            return false;
        }

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
            if (!containsAny(support.getDataFlavors(),  supportedDropDataFlavors)) {
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
                    final MaxMemoryRegion memoryRegion = (MaxMemoryRegion) transferable.getTransferData(InspectorTransferable.MEMORY_REGION_FLAVOR);
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
                InspectorError.unexpected("Attempt to drop an unsupported data flavor");
            } catch (IOException e) {
                e.printStackTrace();
                InspectorError.unexpected("Unknown drop failure");
            }
            return false;
        }
    }

    private interface MouseButtonMapper {
        int getButton(MouseEvent mouseEvent);
    }

    private final Inspection inspection;
    private final String tracePrefix;
    private final InspectorNameDisplay nameDisplay;
    private final SaveSettingsListener saveSettingsListener;
    private final Cursor busyCursor = new Cursor(Cursor.WAIT_CURSOR);
    private final JDesktopPane desktopPane;
    private final JScrollPane scrollPane;
    private final InspectorMainMenuBar menuBar;
    private final InspectorPopupMenu desktopMenu;
    private final InspectorLabel unavailableDataTableCellRenderer;

    /**
     * Location in absolute screen coordinates of the most recent mouse location of interest.
     */
    private Point mostRecentMouseLocation = DEFAULT_LOCATION;

    /**
     * Platform-specific way to handle mouse clicks.
     */
    private final MouseButtonMapper mouseButtonMapper;

    /**
     * Creates a new main window frame for the VM inspection session.
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
        this.desktopMenu = new InspectorPopupMenu(inspection.vm().entityName() + " Inspector");

        setDefaultLookAndFeelDecorated(true);
        Toolkit.getDefaultToolkit().addAWTEventListener(new MouseLocationListener(), AWTEvent.MOUSE_EVENT_MASK);

        if (inspection.vm().platform().getOS() == MaxPlatform.OS.DARWIN) {
            // For Darwin, make sure alternate mouse buttons get mapped properly
            mouseButtonMapper = new MouseButtonMapper() {
                public int getButton(MouseEvent mouseEvent) {
                    if (mouseEvent.getButton() == MouseEvent.BUTTON1) {
                        if (mouseEvent.isControlDown()) {
                            if (!mouseEvent.isAltDown()) {
                                return MouseEvent.BUTTON3;
                            }
                        } else if (mouseEvent.isAltDown()) {
                            return MouseEvent.BUTTON2;
                        }
                    }
                    return mouseEvent.getButton();
                }
            };
        } else {
            // For all other platforms, use the standard mouse event mapping.
            mouseButtonMapper = new MouseButtonMapper() {
                public int getButton(MouseEvent mouseEvent) {
                    return mouseEvent.getButton();
                }
            };
        }

        // Set default geometry; may get overridden by settings when initialized
        setMinimumSize(inspection.geometry().inspectorMinFrameSize());
        setPreferredSize(inspection.geometry().inspectorPrefFrameSize());
        setLocation(inspection.geometry().inspectorDefaultFrameLocation());

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
        desktopPane.setBackground(inspection.style().vmStoppedBackgroundColor(true));
        scrollPane = new InspectorScrollPane(inspection, desktopPane);
        setContentPane(scrollPane);
        menuBar = new InspectorMainMenuBar(actions);
        setJMenuBar(menuBar);

        desktopMenu.add(actions.viewBootImage());
        desktopMenu.add(actions.viewBreakpoints());
        desktopMenu.add(actions.memoryWordsInspectorsMenu());
        desktopMenu.add(actions.viewMemoryRegions());
        desktopMenu.add(actions.viewMethodCode());
        desktopMenu.add(actions.viewNotepad());
        desktopMenu.add(actions.objectInspectorsMenu());
        desktopMenu.add(actions.viewRegisters());
        desktopMenu.add(actions.viewStack());
        desktopMenu.add(actions.viewStackFrame());
        desktopMenu.add(actions.viewThreads());
        desktopMenu.add(actions.viewVmThreadLocals());
        if (inspection.vm().watchpointManager() != null) {
            desktopMenu.add(actions.viewWatchpoints());
        }

        desktopPane.addMouseListener(new InspectorMouseClickAdapter(inspection) {
            @Override
            public void procedure(final MouseEvent mouseEvent) {
                if (InspectorMainFrame.this.inspection.gui().getButton(mouseEvent) == MouseEvent.BUTTON3) {
                    desktopMenu.show(mouseEvent.getComponent(), mouseEvent.getX(), mouseEvent.getY());
                }
            }
        });
        unavailableDataTableCellRenderer = new UnavailableDataTableCellRenderer(inspection);
        saveSettingsListener = new AbstractSaveSettingsListener(FRAME_SETTINGS_NAME) {
            public void saveSettings(SaveSettingsEvent saveSettingsEvent) {
                final Rectangle geometry = getBounds();
                saveSettingsEvent.save(FRAME_X_KEY, geometry.x);
                saveSettingsEvent.save(FRAME_Y_KEY, geometry.y);
                saveSettingsEvent.save(FRAME_WIDTH_KEY, geometry.width);
                saveSettingsEvent.save(FRAME_HEIGHT_KEY, geometry.height);
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
            InspectorWarning.message("Inspector Main Frame settings", optionError);
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
                    final InspectorContainer<? extends Inspector> inspectorContainer = Utils.cast(inspector);
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

    public int getButton(MouseEvent mouseEvent) {
        return mouseButtonMapper.getButton(mouseEvent);
    }

    public Rectangle setLocationRelativeToMouse(Inspector inspector) {
        return setLocationRelativeToMouse(inspector, inspection.geometry().defaultNewFrameXOffset(), inspection.geometry().defaultNewFrameYOffset());
    }

    public Rectangle setLocationRelativeToMouse(Inspector inspector, int offset) {
        return setLocationRelativeToMouse(inspector, offset, offset);
    }

    public Rectangle moveToMiddle(Inspector inspector) {
        final JComponent component = inspector.getJComponent();
        component.setLocation(getMiddle(component));
        return component.getBounds();
    }

    public Rectangle moveToMiddleIfNotVisble(Inspector inspector) {
        JComponent component = inspector.getJComponent();
        if (!contains(component.getLocation())) {
            moveToMiddle(inspector);
        }
        return component.getBounds();
    }

    public  Rectangle moveToExposeDefaultMenu(Inspector inspector) {
        JComponent component = inspector.getJComponent();
        final Point location = component.getLocation();
        if (location.x < 0 || location.y < 0) {
            component.setLocation(Math.max(location.x, 0), Math.max(location.y, 0));
        }
        return component.getBounds();
    }

    public Rectangle resizeToFit(Inspector inspector) {
        final JComponent component = inspector.getJComponent();
        Rectangle geometry = component.getBounds();
        final int newWidth = (Math.min(getWidth(), geometry.x + geometry.width)) - geometry.x;
        final int newHeight = (Math.min(getHeight(), geometry.y + geometry.height)) - geometry.y;
        component.setBounds(geometry.x, geometry.y, newWidth, newHeight);
        return component.getBounds();
    }

    public Rectangle resizeToFill(Inspector inspector) {
        final JComponent component = inspector.getJComponent();
        component.setBounds(0, 0, getWidth(), getHeight());
        return component.getBounds();
    }

    public Rectangle restoreDefaultGeometry(Inspector inspector) {
        final Rectangle defaultFrameGeometry = inspector.defaultGeometry();
        final JComponent component = inspector.getJComponent();
        if (defaultFrameGeometry != null) {
            component.setBounds(defaultFrameGeometry);
            return defaultFrameGeometry;
        }
        moveToMiddle(inspector);
        return component.getBounds();
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

    public InspectorLabel getUnavailableDataTableCellRenderer() {
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
    private Rectangle setLocationRelativeToMouse(Inspector inspector, int xOffset, int yOffset) {
        final Point location = mostRecentMouseLocation;
        location.translate(xOffset, yOffset);
        return setLocationOnScreen(inspector, location);
    }

    private Rectangle setLocationOnScreen(Inspector inspector, Point locationOnScreen) {
        final Point origin = getContentPane().getLocationOnScreen();
        final Point location = new Point(locationOnScreen.x - origin.x, locationOnScreen.y - origin.y);
        final Rectangle geometry = getBounds();
        final JComponent frame = inspector.getJComponent();

        if (frame.getWidth() > geometry.width) {
            frame.setSize(geometry.width, frame.getHeight());
        }
        if (frame.getHeight() > geometry.height) {
            frame.setSize(frame.getWidth(), geometry.height);
        }

        if (location.x <= -frame.getWidth()) {
            location.x = 0;
        } else if (location.x >= geometry.width) {
            location.x = geometry.width - frame.getWidth();
        }

        if (location.y < 0) {
            location.y = 0;
        } else if (location.y >= geometry.height) {
            location.y = geometry.height - frame.getHeight();
        }
        frame.setLocation(location);
        return frame.getBounds();
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
        final boolean invalidReferenceDetected = !inspection.vm().invalidReferencesLogger().isEmpty();
        if (maxVMState.newerThan(lastRefreshedState)) {
            lastRefreshedState = maxVMState;
            setTitle(inspection.currentInspectionTitle());
            switch (maxVMState.processState()) {
                case STOPPED:
                    if (maxVMState.isInGC()) {
                        menuBar.setStateColor(inspection.style().vmStoppedinGCBackgroundColor(invalidReferenceDetected));
                    } else {
                        desktopPane.setBackground(inspection.style().vmStoppedBackgroundColor(invalidReferenceDetected));
                        menuBar.setStateColor(inspection.style().vmStoppedBackgroundColor(invalidReferenceDetected));
                    }
                    break;
                case RUNNING:
                case UNKNOWN:
                    desktopPane.setBackground(inspection.style().vmRunningBackgroundColor());
                    menuBar.setStateColor(inspection.style().vmRunningBackgroundColor());
                    break;
                case TERMINATED:
                    desktopPane.setBackground(inspection.style().vmTerminatedBackgroundColor());
                    menuBar.setStateColor(inspection.style().vmTerminatedBackgroundColor());
                    break;
                default:
                    InspectorError.unknownCase(maxVMState.processState().toString());
            }
        }
        repaint();
    }

    private final class UnavailableDataTableCellRenderer extends InspectorLabel implements TableCellRenderer, TextSearchable, Prober {

        UnavailableDataTableCellRenderer(Inspection inspection) {
            super(inspection);
            setText(inspection.nameDisplay().unavailableDataShortText());
            setToolTipText(inspection.nameDisplay().unavailableDataLongText());
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            return this;
        }

        @Override
        public String getSearchableText() {
            return null;
        }

        public void redisplay() {
        }

        public void refresh(boolean force) {
        }

    }
}
