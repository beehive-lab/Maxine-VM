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
import java.awt.event.*;
import java.beans.*;

import javax.swing.*;
import javax.swing.event.*;

import com.sun.max.ins.*;

/**
 * A internal frame controlled by an {@linkplain Inspector inspector}.
 *
 * @author Bernd Mathiske
 * @author Doug Simon
 * @author Michael Van De Vanter
 */
public final class InspectorFrame extends JInternalFrame implements Prober {

    private final Inspector inspector;

    private final InspectorMenuBar menuBar;

    /**
     * Creates an internal frame for an Inspector.
     * @param inspector
     * @param menu an optional menu, replaces default inspector menu if non-null
     */
    public InspectorFrame(Inspector inspector, InspectorMenu menu) {
        this.inspector = inspector;

        menuBar = new InspectorMenuBar(inspector.inspection());
        menuBar.add((menu == null) ? inspector.createDefaultMenu() : menu);
        setJMenuBar(menuBar);

        setResizable(true);
        setClosable(true);
        setIconifiable(false);
        setVisible(false);

        addInternalFrameListener(new InternalFrameAdapter() {

            @Override
            public void internalFrameActivated(InternalFrameEvent e) {
                InspectorFrame.this.inspector.inspectorGetsWindowFocus();
            }

            @Override
            public void internalFrameDeactivated(InternalFrameEvent e) {
                InspectorFrame.this.inspector.inspectorLosesWindowFocus();
            }
        });
    }


    public void refresh(boolean force) {
        menuBar.refresh(force);
    }

    public void redisplay() {
    }

    public Inspector inspector() {
        return inspector;
    }

    public InspectorMenu getMenu(String name) {
        return menuBar.findMenu(name);
    }

    public void setSelected() {
        try {
            setSelected(true);
        } catch (PropertyVetoException e) {
        }
    }

    public void flash(Color borderFlashColor) {
        Component pane = getContentPane();
        if (pane instanceof JScrollPane) {
            final JScrollPane scrollPane = (JScrollPane) pane;
            pane = scrollPane.getViewport();
        }
        final Graphics g = pane.getGraphics();
        g.setPaintMode();
        g.setColor(borderFlashColor);
        for (int i = 0; i < 5; i++) {
            g.drawRect(i, i, pane.getWidth() - (i * 2), pane.getHeight() - (i * 2));
        }
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
        }
        g.dispose();
        invalidate();
        repaint();
    }

    @Override
    public void dispose() {
        super.dispose();
        inspector.inspectorClosing();
    }


    private InspectorAction frameClosingAction;
    private InternalFrameListener frameClosingListener;

    public void replaceFrameCloseAction(InspectorAction action) {
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        if (frameClosingAction != null) {
            removeInternalFrameListener(frameClosingListener);
        }
        frameClosingAction = action;
        frameClosingListener = new InternalFrameAdapter() {
            @Override
            public void internalFrameClosing(InternalFrameEvent we) {
                frameClosingAction.perform();
            }
        };
        addInternalFrameListener(frameClosingListener);
    }

    /**
     * Records the last position of the mouse when it was over a component. This is used in positioning newly
     * created internal frames.
     */
    public static final class TitleBarListener implements AWTEventListener {

        private TitleBarListener() {
        }

        private static Point recentMouseLocationOnScreen = new Point(100, 100);

        public static Point recentMouseLocationOnScreen() {
            return recentMouseLocationOnScreen;
        }

        public void eventDispatched(AWTEvent event) {
            if (event instanceof MouseEvent && event.getSource() != null) {
                final MouseEvent mouseEvent = (MouseEvent) event;
                recentMouseLocationOnScreen = getLocationOnScreen(mouseEvent);
            }
        }

        private static Point getLocationOnScreen(MouseEvent mouseEvent) {
            try {
                final Component source = (Component) mouseEvent.getSource();
                final Point eventLocationOnScreen = source.getLocationOnScreen();
                eventLocationOnScreen.translate(mouseEvent.getX(), mouseEvent.getY());
                return eventLocationOnScreen;
            } catch (IllegalComponentStateException e) {
                return new Point(0, 0);
            }

        }

        public static void initialize() {
            Toolkit.getDefaultToolkit().addAWTEventListener(new TitleBarListener(), AWTEvent.MOUSE_EVENT_MASK);
        }
    }


}
