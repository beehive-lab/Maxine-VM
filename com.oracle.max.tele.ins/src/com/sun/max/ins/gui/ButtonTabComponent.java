/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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
import java.awt.event.*;

import javax.swing.*;
import javax.swing.plaf.basic.*;

import com.sun.max.ins.*;

/**
 * Component to be used as tabComponent.
 * Contains a JLabel to show the text and a JButton to close the tab it belongs to.
 * <br>
 * Copied from the online Java Tutorial and modified to Maxine Project coding conventions.  2/5/08
 * http://java.sun.com/docs/books/tutorial/uiswing/examples/components/TabComponentsDemoProject/src/components/ButtonTabComponent.java
 *
 * Unfortunately, using this with the VM View's TabbedInspector is a bit
 * tricky; causes there to be children of the TabbedView that aren't themselves
 * Views.  The VM view iterator filters them out.
 *
 * @author Michael Van De Vanter
 */
class ButtonTabComponent extends InspectorPanel {
    private final TabbedView tabbedView;
    private final AbstractView view;
    private final String toolTipText;

    ButtonTabComponent(Inspection inspection, final TabbedView tabbedView, AbstractView view, String toolTipText) {
        //unset default FlowLayout' gaps
        super(inspection, new FlowLayout(FlowLayout.LEFT, 0, 0));
        this.tabbedView = tabbedView;
        this.view = view;
        this.toolTipText = toolTipText;
        setOpaque(false);

        final JLabel label = new JLabel() {
            @Override
            public String getText() {
                return ButtonTabComponent.this.view.getTextForTitle();
            }
        };
        label.setToolTipText(toolTipText);
        //add more space between the label and the button
        label.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 5));
        final JPopupMenu popupMenu = new JPopupMenu();
        popupMenu.add(new AbstractAction("Close tab") {
            public void actionPerformed(ActionEvent e) {
                tabbedView.close(ButtonTabComponent.this.view);
            }
        });
        popupMenu.add(new AbstractAction("Close other tabs") {
            public void actionPerformed(ActionEvent e) {
                tabbedView.closeOthers(ButtonTabComponent.this.view);
            }
        });
        label.setComponentPopupMenu(popupMenu);
        label.addMouseListener(new InspectorMouseClickAdapter(view.inspection()) {

            @Override
            public void procedure(MouseEvent mouseEvent) {
                switch(inspection().gui().getButton(mouseEvent)) {
                    case MouseEvent.BUTTON1:
                        // Default left button behavior; for some reason we have to do it by hand
                        tabbedView.setSelected(ButtonTabComponent.this.view);
                        break;
                    case MouseEvent.BUTTON3:
                        popupMenu.show(mouseEvent.getComponent(), mouseEvent.getX(), mouseEvent.getY());
                        break;
                }
                // Pass along the event to the tab component.
                ButtonTabComponent.this.dispatchEvent(mouseEvent);
            }
        });
        add(label);

        //tab button
        final JButton button = new TabButton();
        add(button);
        //add more space to the top of the component
        setBorder(BorderFactory.createEmptyBorder(2, 0, 0, 0));
    }

    private class TabButton extends JButton implements ActionListener {
        public TabButton() {
            final int size = 17;
            setPreferredSize(new Dimension(size, size));
            setToolTipText("close this tab");
            //Make the button looks the same for all Laf's
            setUI(new BasicButtonUI());
            //Make it transparent
            setContentAreaFilled(false);
            //No need to be focusable
            setFocusable(false);
            setBorder(BorderFactory.createEtchedBorder());
            setBorderPainted(false);
            //Making nice rollover effect
            //we use the same listener for all buttons
            addMouseListener(buttonMouseListener);
            setRolloverEnabled(true);
            //Close the proper tab by clicking the button
            addActionListener(this);
        }

        public void actionPerformed(ActionEvent e) {
            tabbedView.close(view);
        }

        //we don't want to update UI for this button
        @Override
        public void updateUI() {
        }

        //paint the cross
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            final Graphics2D g2 = (Graphics2D) g.create();
            //shift the image for pressed buttons
            if (getModel().isPressed()) {
                g2.translate(1, 1);
            }
            g2.setStroke(new BasicStroke(2));
            g2.setColor(InspectorStyle.Black);
            if (getModel().isRollover()) {
                g2.setColor(InspectorStyle.SunBlue1);
            }
            final int delta = 6;
            g2.drawLine(delta, delta, getWidth() - delta - 1, getHeight() - delta - 1);
            g2.drawLine(getWidth() - delta - 1, delta, delta, getHeight() - delta - 1);
            g2.dispose();
        }
    }

    private static final MouseListener buttonMouseListener = new MouseAdapter() {

        @Override
        public void mouseEntered(MouseEvent e) {
            final Component component = e.getComponent();
            if (component instanceof JButton) {
                final JButton button = (JButton) component;
                button.setBorderPainted(true);
            }
        }

        @Override
        public void mouseExited(MouseEvent e) {
            final Component component = e.getComponent();
            if (component instanceof JButton) {
                final JButton button = (JButton) component;
                button.setBorderPainted(false);
            }
        }
    };
}

