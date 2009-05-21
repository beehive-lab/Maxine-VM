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
import java.beans.*;

import javax.swing.*;
import javax.swing.event.*;

import com.sun.max.ins.*;

/**
 * An internal frame with convenience methods for positioning and a default menu.
 *
 * @author Bernd Mathiske
 * @author Michael Van De Vanter
 */
public final class InternalInspectorFrame extends JInternalFrame implements InspectorFrame {

    private final Inspector _inspector;

    public Inspector inspector() {
        return _inspector;
    }

    public void moveToMiddle() {
        _inspector.inspection().moveToMiddle(this);
    }

    private InspectorMenu _menu;

    public InspectorMenu menu() {
        return _menu;
    }

    public void setMenu(InspectorMenu menu) {
        _menu = menu;
    }

    public void add(InspectorMenuItems inspectorMenuItems) {
        menu().add(inspectorMenuItems);
    }

    public Inspection inspection() {
        return _inspector.inspection();
    }

    public void refresh(long epoch, boolean force) {
        menu().refresh(epoch, force);
    }

    public void redisplay() {
    }

    public Container asContainer() {
        return this;
    }

    /**
     * Creates an internal frame for an Inspector.
     * @param inspector
     * @param menu an optional menu, replaces default inspector menu if non-null
     */
    public InternalInspectorFrame(Inspector inspector, InspectorMenu menu) {
        _inspector = inspector;
        _menu = menu;

        setResizable(true);
        setClosable(true);
        setIconifiable(false);
        setVisible(false);

        if (_menu == null) {
            _menu = new InspectorMenu(inspector);
        }
        setFrameIcon(FRAME_ICON);

        addInternalFrameListener(new InternalFrameAdapter() {

            @Override
            public void internalFrameActivated(InternalFrameEvent e) {
                _inspector.inspectorGetsWindowFocus();
            }

            @Override
            public void internalFrameDeactivated(InternalFrameEvent e) {
                _inspector.inspectorLosesWindowFocus();
            }
        });
    }

    public void setLocationOnScreen(Point locationOnScreen) {
        final Point origin = _inspector.inspection().getContentPane().getLocationOnScreen();
        final Point location = new Point(locationOnScreen.x - origin.x, locationOnScreen.y - origin.y);

        final Rectangle r = _inspector.inspection().getVisibleBounds();
        if (getWidth() > r.width) {
            setSize(r.width, getHeight());
        }
        if (getHeight() > r.height) {
            setSize(getWidth(), r.height);
        }

        if (location.x <= -getWidth()) {
            location.x = 0;
        } else if (location.x >= r.width) {
            location.x = r.width - getWidth();
        }

        if (location.y < 0) {
            location.y = 0;
        } else if (location.y >= r.height) {
            location.y = r.height - getHeight();
        }

        setLocation(location);
    }

    public void setSelected() {
        try {
            setSelected(true);
        } catch (PropertyVetoException e) {
        }
    }

    public void flash(Color borderFlashColor) {
        InspectorFrame.Static.flash(this, borderFlashColor);
    }

    @Override
    public void dispose() {
        super.dispose();
        _inspector.inspectorClosing();
    }

    private InspectorAction _frameClosingAction;
    private InternalFrameListener _frameClosingListener;

    public void replaceFrameCloseAction(InspectorAction action) {
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        if (_frameClosingAction != null) {
            removeInternalFrameListener(_frameClosingListener);
        }
        _frameClosingAction = action;
        _frameClosingListener = new InternalFrameAdapter() {
            @Override
            public void internalFrameClosing(InternalFrameEvent we) {
                _frameClosingAction.perform();
            }
        };
        addInternalFrameListener(_frameClosingListener);
    }

}
