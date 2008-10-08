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
 * Contains a JLabel to show the text and a JButton to close the tab it belongs to
 *
 * Copied from the online Java Tutorial and modified to Maxine coding conventions.  2/5/08
 * http://java.sun.com/docs/books/tutorial/uiswing/examples/components/TabComponentsDemoProject/src/components/ButtonTabComponent.java
 *
 * Unfortunately, using this with the Maxine TabbedInspector is a bit
 * tricky; causes there to be children of the TabbedInspector that aren't themselves
 * Inspectors.  The Inspector iterator filters them out.
 *
 * @author Michael Van De Vanter
 */
public class ButtonTabComponent extends InspectorPanel {
    private final TabbedInspector _tabbedInspector;
    private final Inspector _inspector;
    private final JTabbedPane _tabbedPane;

    public ButtonTabComponent(Inspection inspection, final TabbedInspector tabbedInspector, Inspector inspector, JTabbedPane tabbedPane) {
        //unset default FlowLayout' gaps
        super(inspection, new FlowLayout(FlowLayout.LEFT, 0, 0));
        _tabbedInspector = tabbedInspector;
        _inspector = inspector;
        _tabbedPane = tabbedPane;
        setOpaque(false);

        //make JLabel read titles from JTabbedPane
        final JLabel label = new JLabel() {

            @Override
            public String getText() {
                final int i = _tabbedPane.indexOfTabComponent(ButtonTabComponent.this);
                if (i != -1) {
                    return _tabbedPane.getTitleAt(i);
                }
                return null;
            }
        };

        add(label);
        //add more space between the label and the button
        label.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 5));
        //tab button
        final JButton button = new TabButton();
        add(button);
        //add more space to the top of the component
        setBorder(BorderFactory.createEmptyBorder(2, 0, 0, 0));
    }

    public void refresh(long epoch) {
        // No remote data that needs reading.
    }

    public void redisplay() {
        // No view configurations that we're willing to fuss with.
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
            _tabbedInspector.close(_inspector);
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


