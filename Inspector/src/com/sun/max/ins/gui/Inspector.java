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

import javax.swing.*;

import com.sun.max.ins.*;
import com.sun.max.ins.Inspection.*;
import com.sun.max.ins.InspectionSettings.*;
import com.sun.max.program.*;
import com.sun.max.tele.debug.*;
import com.sun.max.tele.method.*;
import com.sun.max.tele.object.*;
import com.sun.max.vm.stack.*;

/**
 * An inspector combines an aggregation of {@link Prober}s in a frame.
 * It can switch between using an internal or external frame (External frames are deprecated).
 *
 * @author Bernd Mathiske
 * @author Michael Van De Vanter
 */
public abstract class Inspector extends InspectionHolder implements InspectionListener, ViewFocusListener {

    private static final int TRACE_VALUE = 2;

    private InspectorFrame _frame;

    /**
     * @return the window system frame in which the Inspector displays its view
     */
    public InspectorFrame frame() {
        return _frame;
    }

    /**
     * Gets an object that is an adapter between the inspection's persistent {@linkplain Inspection#settings()}
     * and this inspector. If the object's {@link SaveSettingsListener#component()} , then the
     * size and location of this inspector are adjusted according to the settings as well as being
     * persisted any time this inspector is moved or resized.
     */
    public SaveSettingsListener saveSettingsListener() {
        return null;
    }

    /**
     * Creates a settings client for this inspector that causes window geometry to be saved & restored.
     */
    public static SaveSettingsListener createBasicSettingsClient(final Inspector inspector, final String name) {
        return new AbstractSaveSettingsListener(name, null) {
            @Override
            public Component component() {
                return (Component) inspector.frame();
            }
            public void saveSettings(SaveSettingsEvent saveSettingsEvent) {
            }
        };
    }

    /**
     * @return a short string suitable for appearing in the window frame of an inspector.
     */
    public abstract String getTitle();

    /**
     * Enum containing constants denoting whether an inspector should be viewed with an {@linkplain #INTERNAL internal}
     * or {@linkplain #EXTERNAL external} frame.
     */
    public enum Residence {
        INTERNAL, EXTERNAL;
    }

    private Residence _residence;

    public synchronized Residence residence() {
        return _residence;
    }

    /**
     * Set frame location to a point displaced by a default amount from the most recently known mouse position.
     */
    public void setLocationRelativeToMouse() {
        setLocationRelativeToMouse(inspection().geometry().defaultNewFrameXOffset(), inspection().geometry().defaultNewFrameYOffset());
    }

    /**
     * Set frame location to a point displayed by specified diagonal amount from the most recently known mouse position.
     */
    public void setLocationRelativeToMouse(int offset) {
        setLocationRelativeToMouse(offset, offset);
    }

    /**
     * Set frame location to a point displaced by specified amount from the most recently known mouse position.
     */
    public void setLocationRelativeToMouse(int xOffset, int yOffset) {
        final Point location = InspectorFrame.TitleBarListener.recentMouseLocationOnScreen();
        location.translate(xOffset, yOffset);
        _frame.setLocationOnScreen(location);
    }

    public synchronized void setResidence(Residence residence) {
        if (residence == _residence) {
            return;
        }
        Point location = null;
        if (_frame != null) {
            location = _frame.getLocationOnScreen();
            _frame.dispose();
        }
        _residence = residence;
        createFrame(_frame.menu());
        if (location != null) {
            _frame.setLocationOnScreen(location);
        } else {
            setLocationRelativeToMouse();
        }
    }

    public synchronized void toggleResidence() {
        setResidence(Residence.values()[(_residence.ordinal() + 1) % Residence.values().length]);
    }

    protected Inspector(Inspection inspection, Residence residence) {
        super(inspection);
        _residence = residence;
    }

    /**
     * Populates the inspector's frame, already created, with components that make up the Inspector's view.
     * @param epoch current execution age of the {@link teleProcess}.
     */
    protected abstract void createView(long epoch);

    protected void updateSize() {
        final Container contentPane = frame().getContentPane();
        if (contentPane instanceof JScrollPane) {
            final JScrollPane scrollPane = (JScrollPane) contentPane;
            final Dimension size = scrollPane.getViewport().getPreferredSize();
            frame().setMaximumSize(new Dimension(size.width + 40, size.height + 40));
        }
    }

    /**
     * Creates a frame for the inspector, internal or external depending on the residence,
     * calls {@link createView()} to populate it; adds the inspector to the update
     * listeners; makes it all visible.
     *
     * If this inspector has a {@linkplain #saveSettingsListener()}, then its size and location
     * is adjusted according to the {@linkplain Inspection#settings() inspection's settings}.
     *
     * @param menu  optional menu to replace the default frame menu
     */
    protected void createFrame(InspectorMenu menu) {
        switch (_residence) {
            case INTERNAL:
                _frame = new InternalInspectorFrame(this, menu);
                break;
            case EXTERNAL:
                assert menu == null;
                // Any inspector that needs a custom menu shouldn't really be undockable,
                // so it shoudn't ever be put in an external frame.
                _frame = new ExternalInspectorFrame(this);
                break;
        }
        frame().setTitle(getTitle());
        createView(teleProcess().epoch());
        _frame.pack();
        updateSize();
        switch (_residence) {
            case INTERNAL:
                inspection().desktopPane().add((Component) _frame);
                _frame.setVisible(true);
                break;
            case EXTERNAL:
                _frame.setVisible(true);
                _frame.moveToFront();
                break;
        }
        inspection().addInspectionListener(this);
        inspection().focus().addListener(this);

        final SaveSettingsListener saveSettingsListener = saveSettingsListener();
        if (saveSettingsListener != null) {
            inspection().settings().addSaveSettingsListener(saveSettingsListener);
        }
    }

    public synchronized void refreshView(long epoch) {
        _frame.refresh(epoch);
        updateSize();
        _frame.invalidate();
        _frame.repaint();
    }

    protected final synchronized void refreshView() {
        refreshView(teleProcess().epoch());
    }

    /**
     * Rebuilds the "view" component of the inspector, much more
     * expensive than {@link refreshView()}, but necessary when the parameters or
     * configuration of the view changes enough to require creating a new one.
     */
    protected synchronized void reconstructView() {
        createView(teleProcess().epoch());
        frame().pack();
        updateSize();
    }

    public void vmStateChanged(long epoch) {
        refreshView(epoch);
    }

    public void threadSetChanged(long epoch) {
    }

    public void threadStateChanged(TeleNativeThread teleNativeThread) {
    }

    public void breakpointSetChanged() {
    }

    public void codeLocationFocusSet(TeleCodeLocation teleCodeLocation, boolean interactiveForNative) {
    }

    public void threadFocusSet(TeleNativeThread oldTeleNativeThread, TeleNativeThread teleNativeThread) {
    }

    public void stackFrameFocusChanged(StackFrame oldStackFrame, TeleNativeThread threadForStackFrame, StackFrame stackFrame) {
    }

    public void breakpointFocusSet(TeleBreakpoint oldTeleBreakpoint, TeleBreakpoint teleBreakpoint) {
    }

    public void heapObjectFocusChanged(TeleObject oldTeleObject, TeleObject teleObject) {
    }

    public boolean isVisible() {
        return _frame.isVisible();
    }

    public void moveToFront() {
        _frame.moveToFront();
    }

    public void moveToMiddle() {
        _frame.moveToMiddle();
    }

    public boolean isSelected() {
        return _frame.isSelected();
    }
    public void setSelected() {
        _frame.setSelected();
    }

    /**
     * Calls this inspector to the users attention:  move to front, select, and flash.
     */
    public void highlight() {
        moveToFront();
        setSelected();
        _frame.flash();
    }

    /**
     * If not already visible and selected, calls this inspector to the users attention:  move to front, select, and flash.
     */
    public void highlightIfNotVisible() {
        moveToFront();
        if (!isSelected()) {
            setSelected();
            _frame.flash();
        }
    }

    /**
     * Explicitly closes a particular Inspector, but
     * many are closed implicitly by a window system
     * event on the frame.
     */
    public void dispose() {
        _frame.dispose();
    }

    /**
     * Receives notification that the the frame has acquired focus in the window system.
     */
    public void inspectorGetsWindowFocus() {
    }

    /**
     * Receives notification that the the frame has acquired focus in the window system.
     */
    public void inspectorLosesWindowFocus() {
    }

    /**
     * Receives notification that the window system is closing this inspector.
     */
    public void inspectorClosing() {
        inspection().removeInspectionListener(this);
        inspection().focus().removeListener(this);
        final SaveSettingsListener saveSettingsListener = saveSettingsListener();
        if (saveSettingsListener != null) {
            inspection().settings().removeSaveSettingsListener(saveSettingsListener);
        }
    }

    public CloseOthersAction getCloseOtherInspectorsAction() {
        return new CloseOthersAction();
    }

    public final class CloseOthersAction extends InspectorAction {
        private CloseOthersAction() {
            super(inspection(), "Close Other Inspectors");
        }

        @Override
        public void procedure() {
            if (frame() instanceof InternalInspectorFrame) {
                inspection().desktopPane().removeAll();
                inspection().desktopPane().add((InternalInspectorFrame) frame());
                inspection().repaint();
            }
        }
    }

    public RefreshAction getRefreshAction() {
        return new RefreshAction();
    }

    public final class RefreshAction extends InspectorAction {
        private RefreshAction() {
            super(inspection(), "Refresh");
        }

        @Override
        public void procedure() {
            Trace.line(TRACE_VALUE, "Refreshing view: " + Inspector.this);
            refreshView();
        }
    }

    public ToggleResidenceAction createToggleResidenceAction() {
        return new ToggleResidenceAction();
    }

    private String toggleResidenceTitle() {
        if (_residence == Residence.INTERNAL) {
            return "Undock";
        }
        return "Dock";
    }

    public final class ToggleResidenceAction extends InspectorAction {
        private ToggleResidenceAction() {
            super(inspection(), toggleResidenceTitle());
        }

        @Override
        public void procedure() {
            toggleResidence();
        }
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + ":  " + getTitle();
    }

}
