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

import com.sun.max.ins.*;
import com.sun.max.ins.InspectionSettings.AbstractSaveSettingsListener;
import com.sun.max.ins.InspectionSettings.SaveSettingsEvent;
import com.sun.max.ins.InspectionSettings.SaveSettingsListener;
import com.sun.max.ins.util.*;
import com.sun.max.ins.view.*;
import com.sun.max.ins.view.InspectionViews.ViewKind;
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

    private static final int TRACE_VALUE = 1;
    private static final int MOUSE_TRACE_VALUE = 3;

    private static final String FRAME_SETTINGS_NAME = "windowGeometry";
    private static final String FRAME_X_KEY = "x";
    private static final String FRAME_Y_KEY = "y";
    private static final String FRAME_HEIGHT_KEY = "height";
    private static final String FRAME_WIDTH_KEY = "width";

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
     * The position is recorded in the coordinates of the desktop pane,
     * which is the coordinate system we use to place inspector windows.
     */
    private final class MouseLocationListener implements AWTEventListener {

        public void eventDispatched(AWTEvent awtEvent) {
            final Component source = (Component) awtEvent.getSource();
            if (source != null) {
                // We should only be getting mouse events; others are masked out.
                final MouseEvent mouseEvent = (MouseEvent) awtEvent;
                final Point mouseLocation = mouseEvent.getLocationOnScreen();
                SwingUtilities.convertPointFromScreen(mouseLocation, scrollPane);
                recordMostRecentMouseLocation(mouseLocation);
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
                    InspectorMainFrame.this.inspection.views().memory().makeView(address).highlight();
                    return true;
                }
                if (support.isDataFlavorSupported(InspectorTransferable.MEMORY_REGION_FLAVOR)) {
                    final MaxMemoryRegion memoryRegion = (MaxMemoryRegion) transferable.getTransferData(InspectorTransferable.MEMORY_REGION_FLAVOR);
                    Trace.line(TRACE_VALUE, tracePrefix + "memory region dropped on desktop");
                    InspectorMainFrame.this.inspection.views().memory().makeView(memoryRegion, null).highlight();
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

    /**
     * The scroll pane is the "content" pane for this main frame, and
     * components should be placed relative to its coordinates.
     */
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

        setMinimumSize(inspection.geometry().inspectorMinFrameSize());

        try {
            if (settings.containsKey(saveSettingsListener, FRAME_X_KEY)) {
                // Adjust any negative (off-screen) locations to be on-screen.
                final int x = Math.max(settings.get(saveSettingsListener, FRAME_X_KEY, OptionTypes.INT_TYPE, -1), 0);
                final int y = Math.max(settings.get(saveSettingsListener, FRAME_Y_KEY, OptionTypes.INT_TYPE, -1), 0);
                // Adjust any excessive window size to fit within the screen
                final Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
                final int width = Math.min(settings.get(saveSettingsListener, FRAME_WIDTH_KEY, OptionTypes.INT_TYPE, -1), screenSize.width);
                final int height = Math.min(settings.get(saveSettingsListener, FRAME_HEIGHT_KEY, OptionTypes.INT_TYPE, -1), screenSize.height);
                setPreferredSize(new Dimension(width, height));
                setLocation(x, y);
            } else {
                setPreferredSize(inspection.geometry().inspectorPrefFrameSize());
                setLocation(inspection.geometry().inspectorDefaultFrameLocation());
            }
        } catch (Option.Error optionError) {
            InspectorWarning.message("Inspector Main Frame settings", optionError);
            setPreferredSize(inspection.geometry().inspectorPrefFrameSize());
            setLocation(inspection.geometry().inspectorDefaultFrameLocation());
        }

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent windowevent) {
                InspectorMainFrame.this.inspection.quit();
            }
        });
        desktopPane = new JDesktopPane();
        desktopPane.setOpaque(true);
        if (inspection.vm().inspectionMode() == MaxInspectionMode.IMAGE) {
            desktopPane.setBackground(inspection.style().vmNoProcessBackgroundColor());
        } else {
            desktopPane.setBackground(inspection.style().vmStoppedBackgroundColor(true));
        }
        scrollPane = new InspectorScrollPane(inspection, desktopPane);
        setContentPane(scrollPane);
        menuBar = new InspectorMainMenuBar(actions);
        setJMenuBar(menuBar);

        final InspectionViews views = inspection.views();
        desktopMenu.add(views.activateSingletonViewAction(ViewKind.ALLOCATIONS));
        desktopMenu.add(views.activateSingletonViewAction(ViewKind.BOOT_IMAGE));
        desktopMenu.add(views.activateSingletonViewAction(ViewKind.BREAKPOINTS));
        desktopMenu.add(views.activateSingletonViewAction(ViewKind.CODE_LOCATION));
        desktopMenu.add(views.memory().viewMenu());
        desktopMenu.add(views.memoryBytes().viewMenu());
        desktopMenu.add(views.activateSingletonViewAction(ViewKind.METHODS));
        desktopMenu.add(views.activateSingletonViewAction(ViewKind.NOTEPAD));
        desktopMenu.add(views.objects().viewMenu());
        desktopMenu.add(views.activateSingletonViewAction(ViewKind.REGISTERS));
        desktopMenu.add(views.activateSingletonViewAction(ViewKind.STACK));
        desktopMenu.add(views.activateSingletonViewAction(ViewKind.STACK_FRAME));
        desktopMenu.add(views.activateSingletonViewAction(ViewKind.THREADS));
        desktopMenu.add(views.activateSingletonViewAction(ViewKind.THREAD_LOCALS));
        desktopMenu.add(views.activateSingletonViewAction(ViewKind.WATCHPOINTS));

        desktopPane.addMouseListener(new InspectorMouseClickAdapter(inspection) {
            @Override
            public void procedure(final MouseEvent mouseEvent) {
                if (InspectorMainFrame.this.inspection.gui().getButton(mouseEvent) == MouseEvent.BUTTON3) {
                    desktopMenu.show(mouseEvent.getComponent(), mouseEvent.getX(), mouseEvent.getY());
                }
            }
        });
        unavailableDataTableCellRenderer = new UnavailableDataTableCellRenderer(inspection);

        pack();

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

    public void setLocationRelativeToMouse(Inspector inspector, int diagonalOffset) {
        setLocationRelativeToMouse(inspector, diagonalOffset, diagonalOffset);
    }

    public void moveToMiddle(Component component) {
        final int newX = Math.max(0, (scrollPane.getWidth() - component.getWidth()) / 2);
        final int newY = Math.max(0, (scrollPane.getHeight() - component.getHeight()) / 2);
        component.setLocation(newX, newY);
    }

    public void moveToMiddle(Inspector inspector) {
        moveToMiddle(inspector.getJComponent());
    }


    public InspectorAction moveToMiddleAction(final Inspector inspector) {
        return new InspectorAction(inspection, "Move to center of frame") {

            @Override
            protected void procedure() {
                moveToMiddle(inspector);
            }
        };
    }

    public void moveToFullyVisible(Inspector inspector) {
        final JComponent component = inspector.getJComponent();
        if (!isFullyVisible(component)) {
            int x = component.getX();
            if (x < 0) {
                // Off the pane to the left
                x = 0;
            } else if (x + component.getWidth() > scrollPane.getWidth()) {
                // Off the pane to the right
                x = Math.max(0, scrollPane.getWidth() - component.getWidth());
            }
            int y = component.getY();
            if (y < 0) {
                // Off the top of the pane
                y = 0;
            } else if (y + component.getHeight() > scrollPane.getHeight()) {
                // Off the bottom of the pane
                y = Math.max(0, scrollPane.getHeight() - component.getHeight());
            }
            component.setLocation(x, y);
        }
    }

    public void moveToExposeDefaultMenu(Inspector inspector) {
        final JComponent component = inspector.getJComponent();
        final int x = component.getX();
        final int y = component.getY();
        if (x < 0 || y < 0) {
            component.setLocation(Math.max(x, 0), Math.max(y, 0));
        }
    }

    public void resizeToFit(Inspector inspector) {
        final JComponent component = inspector.getJComponent();
        if (!isFullyVisible(component)) {
            final int x = component.getX();
            final int y = component.getY();
            final int width = component.getWidth();
            final int height = component.getHeight();
            final int newWidth = (Math.min(scrollPane.getWidth(), x + width)) - x;
            final int newHeight = (Math.min(scrollPane.getHeight(), y + height)) - y;
            component.setSize(newWidth, newHeight);
        }
    }

    public InspectorAction resizeToFitAction(final Inspector inspector) {
        return new InspectorAction(inspection, "Resize to fit inside frame") {

            @Override
            protected void procedure() {
                resizeToFit(inspector);
            }
        };
    }

    public void resizeToFill(Inspector inspector) {
        inspector.getJComponent().setBounds(0, 0, scrollPane.getWidth(), scrollPane.getHeight());
    }

    public InspectorAction resizeToFillAction(final Inspector inspector) {
        return new InspectorAction(inspection, "Resize to fill frame") {

            @Override
            protected void procedure() {
                resizeToFill(inspector);
            }
        };
    }

    public void restoreDefaultGeometry(Inspector inspector) {
        final Rectangle defaultFrameGeometry = inspector.defaultGeometry();
        if (defaultFrameGeometry != null) {
            inspector.setGeometry(defaultFrameGeometry);
        } else {
            moveToMiddle(inspector);
        }
    }

    public InspectorAction restoreDefaultGeometryAction(final Inspector inspector) {
        return new InspectorAction(inspection, "Restore size/location to default") {

            @Override
            protected void procedure() {
                restoreDefaultGeometry(inspector);
            }
        };
    }

    public void setLocationRelativeToMouse(JDialog dialog, int diagonalOffset) {
        final Point mouse = mostRecentMouseLocation;
        dialog.setLocation(mouse.x + diagonalOffset, mouse.y + diagonalOffset);
    }

    public Frame frame() {
        return this;
    }

    public InspectorLabel getUnavailableDataTableCellRenderer() {
        return unavailableDataTableCellRenderer;
    }

    /**
     * Set frame location to a point displaced by specified amount from the most recently known mouse position.
     */
    private void setLocationRelativeToMouse(Inspector inspector, int xOffset, int yOffset) {
        final JComponent component = inspector.getJComponent();
        final int width = component.getWidth();
        final int height = component.getHeight();
        final Rectangle paneBounds = scrollPane.getBounds();
        final Point mouse = mostRecentMouseLocation;

        // Try a location down and to the right of the mouse
        Rectangle newBounds = new Rectangle(mouse.x + xOffset, mouse.y + yOffset, width, height);
        if (paneBounds.contains(newBounds)) {
            component.setBounds(newBounds);
        } else {
            // Doesn't fit fully; try a location up and to the left
            newBounds = new Rectangle((mouse.x - xOffset) - width, (mouse.y - yOffset) - height, width, height);
            if (paneBounds.contains(newBounds)) {
                component.setBounds(newBounds);
            } else {
                // Doesn't fit fully there either; give up and go for the middle.
                moveToMiddle(component);
            }
        }
    }

    private boolean isFullyVisible(Component component) {
        return scrollPane.getBounds().contains(component.getBounds());
    }

    /**
     * Records the most recent mouse event of interest.
     *
     * @param point mouse location in absolute screen coordinates.
     */
    private void recordMostRecentMouseLocation(Point point) {
        Trace.line(MOUSE_TRACE_VALUE, InspectorMainFrame.this.tracePrefix + "Mouse@(" + point.x + "," + point.y + ")");
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
                    desktopPane.setBackground(inspection.style().vmRunningBackgroundColor());
                    menuBar.setStateColor(inspection.style().vmRunningBackgroundColor());
                    break;
                case UNKNOWN:
                    desktopPane.setBackground(inspection.style().vmNoProcessBackgroundColor());
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
