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
import javax.swing.plaf.basic.*;

import com.sun.max.ins.*;

/**
 * Component to be used as tabComponent.
 * Contains a JLabel to show the text and a JButton to close the tab it belongs to.
 * <br>
 * Copied from the online Java Tutorial and modified to Maxine Project coding conventions.  2/5/08
 * http://java.sun.com/docs/books/tutorial/uiswing/examples/components/TabComponentsDemoProject/src/components/ButtonTabComponent.java
 *
 * Unfortunately, using this with the VM Inspector's TabbedInspector is a bit
 * tricky; causes there to be children of the TabbedInspector that aren't themselves
 * Inspectors.  The VM Inspector iterator filters them out.
 *
 * @author Michael Van De Vanter
 */
class ButtonTabComponent<Inspector_Type extends Inspector> extends InspectorPanel {
    private final TabbedInspector<Inspector_Type> tabbedInspector;
    private final Inspector_Type inspector;
    private final String toolTipText;

    ButtonTabComponent(Inspection inspection, final TabbedInspector<Inspector_Type> tabbedInspector, Inspector_Type inspector, String toolTipText) {
        //unset default FlowLayout' gaps
        super(inspection, new FlowLayout(FlowLayout.LEFT, 0, 0));
        this.tabbedInspector = tabbedInspector;
        this.inspector = inspector;
        this.toolTipText = toolTipText;
        setOpaque(false);

        final JLabel label = new JLabel() {
            @Override
            public String getText() {
                return ButtonTabComponent.this.inspector.getTextForTitle();
            }
        };
        label.setToolTipText(toolTipText);
        //add more space between the label and the button
        label.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 5));
        final JPopupMenu popupMenu = new JPopupMenu();
        popupMenu.add(new AbstractAction("Close tab") {
            public void actionPerformed(ActionEvent e) {
                tabbedInspector.close(ButtonTabComponent.this.inspector);
            }
        });
        popupMenu.add(new AbstractAction("Close other tabs") {
            public void actionPerformed(ActionEvent e) {
                tabbedInspector.closeOthers(ButtonTabComponent.this.inspector);
            }
        });
        label.setComponentPopupMenu(popupMenu);
        label.addMouseListener(new InspectorMouseClickAdapter(inspector.inspection()) {

            @Override
            public void procedure(MouseEvent mouseEvent) {
                switch(Inspection.mouseButtonWithModifiers(mouseEvent)) {
                    case MouseEvent.BUTTON1:
                        // Default left button behavior; for some reason we have to do it by hand
                        tabbedInspector.setSelected(ButtonTabComponent.this.inspector);
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
            tabbedInspector.close(inspector);
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

