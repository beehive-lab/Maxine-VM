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
package com.sun.max.gui;

import java.awt.*;
import java.awt.event.*;
import java.io.*;

import javax.swing.*;
import javax.swing.border.*;

/**
 * A dialog for displaying the details of an exception. Initially the dialog only shows the exception's
 * {@linkplain Throwable#getMessage() message}. It includes a button for expanding the dialog to also show the stack
 * trace. When the stack trace is shown, pressing the same button hides the stack trace.
 *
 * @author Doug Simon
 * @author Aritra Bandyopadhyay
 */
public final class ThrowableDialog extends JDialog {

    /**
     * Creates a dialog to display the details of an exception and makes it visible.
     *
     * @param throwable the exception whose details are being displayed
     * @param owner the {@code Frame} from which the dialog is displayed
     * @param title  the {@code String} to display in the dialog's title bar
     */
    public static void show(Throwable throwable, Frame owner, String title) {
        new ThrowableDialog(throwable, owner, title).setVisible(true);
    }

    /**
     * Creates a dialog to display the details of an exception and makes it visible on the
     * {@linkplain SwingUtilities#invokeLater(Runnable) AWT dispatching thread}.
     *
     * @param throwable the exception whose details are being displayed
     * @param owner the {@code Frame} from which the dialog is displayed
     * @param title the {@code String} to display in the dialog's title bar
     */
    public static void showLater(Throwable throwable, Frame owner, String title) {
        final ThrowableDialog dialog = new ThrowableDialog(throwable, owner, title);
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                dialog.setVisible(true);
            }
        });
    }

    /**
     * Creates a dialog to display the details of an exception. The dialog is not displayed by this method.
     *
     * @param throwable the exception whose details are being displayed
     * @param owner the {@code Frame} from which the dialog is displayed
     * @param title  the {@code String} to display in the dialog's title bar
     */
    private ThrowableDialog(Throwable throwable, Frame owner, String title) {
        super(owner, title, true);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setAlwaysOnTop(true);

        final JToggleButton stackTraceButton = new JToggleButton("Show stack trace");
        final JButton closeButton = new JButton("Close");
        closeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                setVisible(false);
                dispose();
            }
        });

        final JPanel buttonsPanel = new JPanel();
        buttonsPanel.add(closeButton);
        buttonsPanel.add(stackTraceButton);

        final Container mainPane = getContentPane();
        //mainPane.setLayout(new FlowLayout());
        mainPane.setLayout(new BoxLayout(mainPane, BoxLayout.PAGE_AXIS));
        final JLabel message = new JLabel(throwable.getMessage());
        final JPanel messagePanel = new JPanel();
        messagePanel.add(message);
        mainPane.add(messagePanel);
        mainPane.add(buttonsPanel);
        pack();

        final Dimension dialogWithoutStackTracePreferredSize = getPreferredSize();

        final JTextArea stackTraceText = new JTextArea(20, 40);
        throwable.printStackTrace(new PrintWriter(new Writer() {

            @Override
            public void close() throws IOException {
            }

            @Override
            public void flush() throws IOException {
            }

            @Override
            public void write(char[] cbuf, int off, int len) throws IOException {
                stackTraceText.append(new String(cbuf, off, len));
            }
        }));

        final JScrollPane stackTracePanel = new JScrollPane(stackTraceText, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        final Dimension stackTraceTextPreferredSize = stackTraceText.getPreferredSize();
        stackTracePanel.setBorder(new TitledBorder("Stack trace"));
        final Dimension stackTracePanelPreferredSize = new Dimension(
                        stackTraceTextPreferredSize.width + stackTracePanel.getVerticalScrollBar().getPreferredSize().width * 2,
                        stackTraceTextPreferredSize.height + stackTracePanel.getHorizontalScrollBar().getPreferredSize().height);
        stackTracePanel.setPreferredSize(stackTracePanelPreferredSize);
        stackTracePanel.setVisible(false);

        stackTraceButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (stackTraceButton.isSelected()) {
                    stackTraceButton.setText("Hide stack trace");
                    stackTracePanel.setVisible(true);
                    setSize(new Dimension(Math.max(dialogWithoutStackTracePreferredSize.width, stackTracePanelPreferredSize.width),
                                    dialogWithoutStackTracePreferredSize.height + Math.min(1000, stackTracePanelPreferredSize.height)));
                } else {
                    stackTraceButton.setText("Show stack trace");
                    stackTracePanel.setVisible(false);
                    setSize(dialogWithoutStackTracePreferredSize);
                }
                validate();
            }
        });
        mainPane.add(stackTracePanel);

        setSize(dialogWithoutStackTracePreferredSize);
        pack();
        setLocationRelativeTo(owner);
    }

    // Test code

    public static void main(String[] args) {
        try {
            recurse(0);
        } catch (RuntimeException runtimeException) {
            new ThrowableDialog(runtimeException, null, "Runtime Exception");
        }
    }

    static void recurse(int more) {
        if (more > 345) {
            throw new RuntimeException("This is a test. Repeat. This is a test.");
        }
        recurse(more + 1);
    }
}
