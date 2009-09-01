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

import javax.swing.*;
import javax.swing.plaf.metal.*;

import com.sun.max.ins.*;

/**
 * A frame controlled by an {@linkplain Inspector inspector}.
 *
 * @author Bernd Mathiske
 * @author Doug Simon
 * @author Michael Van De Vanter
 */
public interface InspectorFrame extends Prober {

    Inspector inspector();

    InspectorMenu getMenu(String menuName);

    void setVisible(boolean visible);

    /**
     * @return whether the frame that holds the Inspector can be seen on the screen.
     */
    boolean isShowing();

    void moveToFront();

    void setTitle(String title);

    String getTitle();

    Container getContentPane();

    Component getGlassPane();

    void setContentPane(Container container);

    boolean isSelected();

    void setSelected();

    void flash(Color borderFlashColor);

    void invalidate();

    void repaint();

    void pack();

    Dimension getSize();

    void setPreferredSize(Dimension dimension);

    Rectangle getBounds();

    void setBounds(Rectangle bounds);

    Point getLocationOnScreen();

    int getWidth();

    int getHeight();

    void setLocation(int x, int y);

    void setLocation(Point location);

    void setSize(int width, int height);

    void setMaximumSize(Dimension size);

    void replaceFrameCloseAction(InspectorAction action);

    void removeAll();

    void dispose();

    /**
     * This listener exists for two primary purposes:
     * <ul>
     * <li>Recording the last position of the mouse when it was over a component. This is used in positioning newly
     * created internal frames.</li>
     *
     * </ul>
     */
    public final class TitleBarListener implements AWTEventListener {

        private TitleBarListener() {
        }

        private InternalInspectorFrame getInternalInspectorFrame(Component component) {
            Component c = component;
            while (!(c == null || c instanceof InternalInspectorFrame)) {
                c = c.getParent();
            }
            return (InternalInspectorFrame) c;
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

        private static boolean isTitleBarSourceEvent(AWTEvent event) {
            final Object source = event.getSource();
//            System.err.println(source.getClass());
            return source instanceof MetalInternalFrameTitlePane;
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

    public static final class Static {

        private Static() {
        }

        public static void flash(InspectorFrame inspectorFrame, Color borderFlashColor) {
            Component pane = inspectorFrame.getContentPane();
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
            inspectorFrame.invalidate();
            inspectorFrame.repaint();
        }
    }

}
