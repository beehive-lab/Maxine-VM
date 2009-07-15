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

    /**
     * The bounds of the frame icon in a JFrame and JInternalFrame under the Metal look and feel. These
     * bounds are relative to the frame's coordinate system.
     */
    Rectangle FRAME_ICON_BOUNDS = new Rectangle(1, 1, 20, 20);

    ImageIcon FRAME_ICON = InspectorImageIcon.createDownTriangle(16, 16);

    Inspector inspector();

    void add(InspectorMenuItems inspectorMenuItems);

    InspectorMenu menu();

    void setMenu(InspectorMenu menu);

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
     * <li> Intercepting right mouse clicks on the icon rendered in the top left of a frame's title bar to trigger a
     * frame's {@linkplain InspectorFrame#menu() pop-up menu}. This icon is not guaranteed to exist and be in the
     * expected location under all "look and feel" implementations. For this reason, the inspector
     * {@linkplain MaxineInspector#initializeSwing() forces} the UI to use the Metal look and feel which fulfills this
     * requirement. </li>
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
                if (isPopupTrigger(mouseEvent) && isTitleBarSourceEvent(event)) {
                    final InternalInspectorFrame internalInspectorFrame = getInternalInspectorFrame(mouseEvent.getComponent());
                    if (internalInspectorFrame != null) {
                        final Rectangle frameIconBounds = FRAME_ICON_BOUNDS;
                        final Point mousePoint = mouseEvent.getPoint();
                        if (frameIconBounds.contains(mousePoint)) {
                            final JPopupMenu popupMenu = internalInspectorFrame.menu().popupMenu();
                            popupMenu.show(internalInspectorFrame, frameIconBounds.x + frameIconBounds.width, frameIconBounds.y + frameIconBounds.height);
                        }
                    }
                }
            }
        }

        private static boolean isTitleBarSourceEvent(AWTEvent event) {
            final Object source = event.getSource();
//            System.err.println(source.getClass());
            return source instanceof MetalInternalFrameTitlePane;
        }

        /**
         * Determines if a given mouse event is a trigger for the {@linkplain InspectorFrame#menu() pop-up menu}
         * available in the title bar of an inspector frame.
         */
        private static boolean isPopupTrigger(final MouseEvent mouseEvent) {
            // Either button 1 or the system-dependent button+modifier(s) for triggering pop-up menus is accepted
            return mouseEvent.isPopupTrigger() || (MaxineInspector.mouseButtonWithModifiers(mouseEvent) == MouseEvent.BUTTON1 && mouseEvent.getID() == MouseEvent.MOUSE_RELEASED);
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
