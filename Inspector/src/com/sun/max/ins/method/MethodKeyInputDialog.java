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
package com.sun.max.ins.method;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;

import com.sun.max.ins.*;
import com.sun.max.ins.gui.*;
import com.sun.max.ins.type.*;
import com.sun.max.program.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.actor.member.MethodKey.*;
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
        final JTextComponent _field;
        TypeFieldChooser(String name, JTextComponent field) {
            super(inspection(), name);
            _field = field;
        }

        @Override
        protected void procedure() {
            final TypeDescriptor typeDescriptor = TypeSearchDialog.show(inspection(), "Choose declaring class...", "Select");
            if (typeDescriptor != null) {
                final String javaString = typeDescriptor.toJavaString();
                final String newValue;
                if (_field instanceof JTextArea) {
                    newValue = updateSelectedParameter(_field.getText(), _field.getCaretPosition(), javaString);
                } else {
                    newValue = javaString;
                }
                _field.setText(newValue);
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

    private final MethodKeyMessage _methodKeyMessage = new MethodKeyMessage();

    private final JButton _okButton;
    private final JTextField _holderField;
    private final JTextField _nameField;
    private final JTextField _returnTypeField;
    private final JTextArea _parametersField;
    private DefaultMethodKey _methodKey;

    public void changedUpdate(DocumentEvent e) {
        ProgramError.unexpected();
    }

    public void insertUpdate(DocumentEvent e) {
        _okButton.setEnabled(updateMethodKey());
    }

    public void removeUpdate(DocumentEvent e) {
        _okButton.setEnabled(updateMethodKey());
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

        _methodKeyMessage.setMessage(false, "");

        final String nameString = _nameField.getText();
        if (!nameString.isEmpty()) {
            name = SymbolTable.makeSymbol(nameString);
            if (!ClassfileReader.isValidMethodName(name, true)) {
                name = null;
                _methodKeyMessage.setMessage(true, "Name is not a valid Java identifier");
            }
        }

        final String holderName = _holderField.getText();
        if (!holderName.isEmpty()) {
            try {
                holder = JavaTypeDescriptor.getDescriptorForJavaString(holderName);
            } catch (ClassFormatError e) {
                holder = null;
                _methodKeyMessage.setMessage(true, "Invalid name for declaring class");
            }
        }

        final String returnTypeName = _returnTypeField.getText();
        if (!returnTypeName.isEmpty()) {
            try {
                returnType = JavaTypeDescriptor.getDescriptorForJavaString(returnTypeName).string();
            } catch (ClassFormatError e) {
                returnType = null;
                _methodKeyMessage.setMessage(true, "Invalid name for return type");
            }
        }

        final String parameterTypeNames = _parametersField.getText();
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
                    parameterTypes += JavaTypeDescriptor.getDescriptorForJavaString(n).string();
                } catch (ClassFormatError classFormatError) {
                    parameterTypes = null;
                    _methodKeyMessage.setMessage(true, "Invalid name for parameter " + (i + 1));
                }
            }
        }

        if (holder != null && name != null && returnType != null && parameterTypes != null) {
            _methodKey = new DefaultMethodKey(holder, name, SignatureDescriptor.create("(" + parameterTypes + ")" + returnType));
            _methodKeyMessage.setMessage(false, _methodKey.toString(true));
            return true;
        }
        return false;
    }

    public MethodKeyInputDialog(Inspection inspection, String title) {
        super(inspection, title, true);

        _holderField = new JTextField(30);
        _nameField = new JTextField(30);
        _returnTypeField = new JTextField(30);
        _parametersField = new JTextArea(5, 30);

        final TextLabel holderLabel = new TextLabel(inspection, "Declaring class:");
        final JButton holderButton = new JButton(new TypeFieldChooser("...", _holderField));
        final JLabel nameLabel = new JLabel("Name:");
        final TextLabel returnTypeLabel = new TextLabel(inspection, "Return type:");
        final JButton returnTypeButton = new JButton(new TypeFieldChooser("...", _returnTypeField));

        final JScrollPane parametersPane = new InspectorScrollPane(inspection, _parametersField);
        final TextLabel parametersLabel = new TextLabel(inspection, "Parameters:");
        final JButton parametersButton = new JButton(new TypeFieldChooser("...", _parametersField));

        _okButton = new JButton(new AbstractAction("OK") {
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });
        final JButton cancelButton = new JButton(new AbstractAction("Cancel") {
            public void actionPerformed(ActionEvent e) {
                _methodKey = null;
                dispose();
            }
        });

        final JPanel statusPanel = new InspectorPanel(inspection(), new FlowLayout(FlowLayout.LEFT));
        statusPanel.add(new JLabel("Method key:"));
        statusPanel.add(_methodKeyMessage);
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
                                        addComponent(_holderField).
                                        addComponent(_nameField).
                                        addComponent(_returnTypeField).
                                        addComponent(parametersPane).
                                        addComponent(_okButton)).
                        addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING).
                                        addComponent(holderButton).
                                        addComponent(returnTypeButton).
                                        addComponent(parametersButton).
                                        addComponent(cancelButton)));
        layout.setVerticalGroup(layout.createSequentialGroup().
                        addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING).
                                        addComponent(holderLabel).
                                        addComponent(_holderField).
                                        addComponent(holderButton)).
                        addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING).
                                        addComponent(nameLabel).
                                        addComponent(_nameField)).
                        addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING).
                                        addComponent(returnTypeLabel).
                                        addComponent(_returnTypeField).
                                        addComponent(returnTypeButton)).
                        addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING).
                                        addComponent(parametersLabel).
                                        addComponent(parametersPane).
                                        addComponent(parametersButton)).
                        addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING).
                                        addComponent(_okButton).
                                        addComponent(cancelButton)));



        final JPanel contenPane = new JPanel(new BorderLayout());
        contenPane.add(inputPanel, BorderLayout.CENTER);
        contenPane.add(statusPanel, BorderLayout.SOUTH);

        setContentPane(contenPane);
        pack();
        inspection().moveToMiddle(this);

        _holderField.getDocument().addDocumentListener(this);
        _nameField.getDocument().addDocumentListener(this);
        _returnTypeField.getDocument().addDocumentListener(this);
        _parametersField.getDocument().addDocumentListener(this);

        // Make pressing "Enter" equivalent to pressing the "Select" button.
        getRootPane().setDefaultButton(_okButton);

        // Make pressing "Escape" equivalent to pressing the "Cancel" button.
        getRootPane().registerKeyboardAction(cancelButton.getAction(), KeyStroke.getKeyStroke((char) KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

    /**
     * Gets the method key specified by the user's input to this dialog.
     *
     * @return null if the dialog was canceled or has never been made {@linkplain #setVisible(boolean) visible}
     */
    public MethodKey methodKey() {
        return _methodKey;
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
