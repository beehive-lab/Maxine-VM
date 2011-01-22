/*
 * Copyright (c) 2007, 2010, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.ins.method;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;

import com.sun.max.ins.*;
import com.sun.max.ins.gui.*;
import com.sun.max.ins.type.*;
import com.sun.max.ins.util.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.actor.member.MethodKey.DefaultMethodKey;
import com.sun.max.vm.classfile.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.type.*;

/**
 * A dialog for obtaining a {@linkplain MethodKey method key} based on a name, signature and declaring class entered by
 * the user. This dialog implements input validation so that only a valid method key can be specified.
 *
 * @author Doug Simon
 */
public class MethodKeyInputDialog extends InspectorDialog implements DocumentListener {

    /**
     * An action that brings up a dialog for choosing a type available on the inspector's
     * classpath. If a type is selected in the dialog, then an associated field is
     * updated with the selected type in {@linkplain TypeDescriptor#toJavaString() Java source format}.
     */
    class TypeFieldChooser extends InspectorAction {
        final JTextComponent field;
        TypeFieldChooser(String name, JTextComponent field) {
            super(inspection(), name);
            this.field = field;
        }

        @Override
        protected void procedure() {
            final TypeDescriptor typeDescriptor = TypeSearchDialog.show(inspection(), "Choose declaring class...", "Select");
            if (typeDescriptor != null) {
                final String javaString = typeDescriptor.toJavaString();
                final String newValue;
                if (field instanceof JTextArea) {
                    newValue = updateSelectedParameter(field.getText(), field.getCaretPosition(), javaString);
                } else {
                    newValue = javaString;
                }
                field.setText(newValue);
            }
        }

        /**
         * Replaces a selected parameter in a string of parameter types separated by commas.
         *
         * @param parameters a string composed of parameter types separated by commas
         * @param caretPosition an index in {@code parameters} denoting the parameter to be replaced
         * @param replacementParameter the parameter type to replace the parameter in {@code parameters} denoted by
         *            {@code caretPosition}
         * @return the value of {@code parameters} after the replacement has been made
         */
        private String updateSelectedParameter(String parameters, int caretPosition, String replacementParameter) {
            final String before = parameters.substring(0, caretPosition);
            final int start = before.lastIndexOf(',');
            final int end = parameters.indexOf(',', caretPosition);
            if (start < 0) {
                if (end < 0) {
                    return replacementParameter;
                }
                return replacementParameter + parameters.substring(end);
            }
            if (end < 0) {
                return before.substring(0, start + 1) + " " + replacementParameter;
            }
            return before.substring(0, start + 1) + " " + replacementParameter + parameters.substring(end);
        }
    }

    class MethodKeyMessage extends JLabel {
        void clear() {
            setText("");
        }

        void setMessage(boolean isError, String message) {
            if (!isError) {
                super.setText(message);
            } else {
                super.setText("<html><i><font color=\"red\">" + message + "</font></i>");
            }
        }
    }

    private final MethodKeyMessage methodKeyMessage = new MethodKeyMessage();

    private final JButton okButton;
    private final JTextField holderField;
    private final JTextField nameField;
    private final JTextField returnTypeField;
    private final JTextArea parametersField;
    private DefaultMethodKey methodKey;

    public void changedUpdate(DocumentEvent e) {
        InspectorError.unexpected();
    }

    public void insertUpdate(DocumentEvent e) {
        okButton.setEnabled(updateMethodKey());
    }

    public void removeUpdate(DocumentEvent e) {
        okButton.setEnabled(updateMethodKey());
    }

    /**
     * Updates the method key based on the values entered by the user.
     *
     * @return true if the user entered value constitute a valid method key
     */
    private boolean updateMethodKey() {

        TypeDescriptor holder = null;
        Utf8Constant name = null;
        String returnType = null;
        String parameterTypes = null;

        methodKeyMessage.setMessage(false, "");

        final String nameString = nameField.getText();
        if (!nameString.isEmpty()) {
            name = SymbolTable.makeSymbol(nameString);
            if (!ClassfileReader.isValidMethodName(name, true)) {
                name = null;
                methodKeyMessage.setMessage(true, "Name is not a valid Java identifier");
            }
        }

        final String holderName = holderField.getText();
        if (!holderName.isEmpty()) {
            try {
                holder = JavaTypeDescriptor.getDescriptorForJavaString(holderName);
            } catch (ClassFormatError e) {
                holder = null;
                methodKeyMessage.setMessage(true, "Invalid name for declaring class");
            }
        }

        final String returnTypeName = returnTypeField.getText();
        if (!returnTypeName.isEmpty()) {
            try {
                returnType = JavaTypeDescriptor.getDescriptorForJavaString(returnTypeName).string;
            } catch (ClassFormatError e) {
                returnType = null;
                methodKeyMessage.setMessage(true, "Invalid name for return type");
            }
        }

        final String parameterTypeNames = parametersField.getText();
        if (parameterTypeNames.isEmpty()) {
            parameterTypes = "";
        } else {
            final String[] names = parameterTypeNames.split("[\\s]*[,\\s][\\s]*");
            parameterTypes = "";
            for (int i = 0; i != names.length; ++i) {
                try {
                    final String n = names[i];
                    if (n == null || n.isEmpty()) {
                        throw new ClassFormatError();
                    }
                    parameterTypes += JavaTypeDescriptor.getDescriptorForJavaString(n).string;
                } catch (ClassFormatError classFormatError) {
                    parameterTypes = null;
                    methodKeyMessage.setMessage(true, "Invalid name for parameter " + (i + 1));
                }
            }
        }

        if (holder != null && name != null && returnType != null && parameterTypes != null) {
            methodKey = new DefaultMethodKey(holder, name, SignatureDescriptor.create("(" + parameterTypes + ")" + returnType));
            methodKeyMessage.setMessage(false, methodKey.toString(true));
            return true;
        }
        return false;
    }

    public MethodKeyInputDialog(Inspection inspection, String title) {
        super(inspection, title, true);

        holderField = new JTextField(30);
        nameField = new JTextField(30);
        returnTypeField = new JTextField(30);
        parametersField = new JTextArea(5, 30);

        final TextLabel holderLabel = new TextLabel(inspection, "Declaring class:");
        final JButton holderButton = new JButton(new TypeFieldChooser("...", holderField));
        holderButton.setText(null);
        holderButton.setIcon(style().generalFindIcon());
        holderButton.setToolTipText("Find declaring class...");
        final JLabel nameLabel = new JLabel("Name:");
        final TextLabel returnTypeLabel = new TextLabel(inspection, "Return type:");
        final JButton returnTypeButton = new JButton(new TypeFieldChooser("...", returnTypeField));
        returnTypeButton.setText(null);
        returnTypeButton.setIcon(style().generalFindIcon());
        returnTypeButton.setToolTipText("Find return type...");

        final JScrollPane parametersPane = new InspectorScrollPane(inspection, parametersField);
        final TextLabel parametersLabel = new TextLabel(inspection, "Parameters:");
        final JButton parametersButton = new JButton(new TypeFieldChooser("...", parametersField));
        parametersButton.setText(null);
        parametersButton.setIcon(style().generalFindIcon());
        parametersButton.setToolTipText("Find parameters....");

        okButton = new JButton(new AbstractAction("OK") {
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });
        final JButton cancelButton = new JButton(new AbstractAction("Cancel") {
            public void actionPerformed(ActionEvent e) {
                methodKey = null;
                dispose();
            }
        });

        final JPanel statusPanel = new InspectorPanel(inspection, new FlowLayout(FlowLayout.LEFT));
        statusPanel.add(new JLabel("Method key:"));
        statusPanel.add(methodKeyMessage);
        statusPanel.setBorder(BorderFactory.createEtchedBorder());

        final JPanel inputPanel = new InspectorPanel(inspection);
        final GroupLayout layout = new GroupLayout(inputPanel);
        inputPanel.setLayout(layout);
        layout.setAutoCreateContainerGaps(true);
        layout.setAutoCreateGaps(true);
        layout.setHorizontalGroup(layout.createSequentialGroup().
                        addGroup(layout.createParallelGroup(GroupLayout.Alignment.TRAILING).
                                        addComponent(holderLabel).
                                        addComponent(nameLabel).
                                        addComponent(returnTypeLabel).
                                        addComponent(parametersLabel)).
                        addGroup(layout.createParallelGroup(GroupLayout.Alignment.TRAILING).
                                        addComponent(holderField).
                                        addComponent(nameField).
                                        addComponent(returnTypeField).
                                        addComponent(parametersPane).
                                        addComponent(okButton)).
                        addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING).
                                        addComponent(holderButton).
                                        addComponent(returnTypeButton).
                                        addComponent(parametersButton).
                                        addComponent(cancelButton)));
        layout.setVerticalGroup(layout.createSequentialGroup().
                        addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING).
                                        addComponent(holderLabel).
                                        addComponent(holderField).
                                        addComponent(holderButton)).
                        addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING).
                                        addComponent(nameLabel).
                                        addComponent(nameField)).
                        addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING).
                                        addComponent(returnTypeLabel).
                                        addComponent(returnTypeField).
                                        addComponent(returnTypeButton)).
                        addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING).
                                        addComponent(parametersLabel).
                                        addComponent(parametersPane).
                                        addComponent(parametersButton)).
                        addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING).
                                        addComponent(okButton).
                                        addComponent(cancelButton)));

        final JPanel contenPane = new JPanel(new BorderLayout());
        contenPane.add(inputPanel, BorderLayout.CENTER);
        contenPane.add(statusPanel, BorderLayout.SOUTH);

        setContentPane(contenPane);
        pack();
        inspection.gui().moveToMiddle(this);

        holderField.getDocument().addDocumentListener(this);
        nameField.getDocument().addDocumentListener(this);
        returnTypeField.getDocument().addDocumentListener(this);
        parametersField.getDocument().addDocumentListener(this);

        // Make pressing "Enter" equivalent to pressing the "Select" button.
        getRootPane().setDefaultButton(okButton);

        // Make pressing "Escape" equivalent to pressing the "Cancel" button.
        getRootPane().registerKeyboardAction(cancelButton.getAction(), KeyStroke.getKeyStroke((char) KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

    /**
     * Gets the method key specified by the user's input to this dialog.
     *
     * @return null if the dialog was canceled or has never been made {@linkplain #setVisible(boolean) visible}
     */
    public MethodKey methodKey() {
        return methodKey;
    }

    /**
     * Displays a dialog for specifying a method key.
     *
     * @param title
     *                title string for the dialog frame
     * @return the reference to the method key input by the user or null if the user canceled the dialog
     */
    public static MethodKey show(Inspection inspection, String title) {
        final MethodKeyInputDialog dialog = new MethodKeyInputDialog(inspection, title);
        dialog.setVisible(true);
        return dialog.methodKey();
    }
}
