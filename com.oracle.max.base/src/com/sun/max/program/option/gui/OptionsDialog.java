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
package com.sun.max.program.option.gui;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.List;

import javax.swing.*;

import com.sun.max.*;
import com.sun.max.gui.*;
import com.sun.max.lang.*;
import com.sun.max.program.*;
import com.sun.max.program.option.*;

/**
 * A dialog used to present the {@linkplain Option options} defined by a {@link OptionSet}.
 */
public class OptionsDialog extends JDialog {

    public static final int TEXT_FIELD_COLUMNS = 40;

    protected abstract static class GUIOption<T> implements ItemListener {
        protected final Option<T> option;
        protected final JCheckBox guard;
        protected final JLabel label;
        protected JComponent input;

        protected GUIOption(Option<T> opt) {
            this.option = opt;
            guard = new JCheckBox();
            label = new JLabel(option.getName());
            guard.addItemListener(this);
        }

        public JCheckBox getGuard() {
            return guard;
        }

        public JLabel getLabel() {
            return label;
        }

        public JComponent getInputComponent() {
            return input;
        }

        public void itemStateChanged(ItemEvent e) {
            setInputAndLabelEnabled(guard.isSelected());
        }

        protected void setEnabledOfContainedComponents(Container container, boolean enabled) {
            for (Component component : container.getComponents()) {
                component.setEnabled(enabled);
                if (component instanceof Container) {
                    setEnabledOfContainedComponents((Container) component, enabled);
                }
            }
        }

        protected void setInputAndLabelEnabled(boolean enabled) {
            input.setEnabled(enabled);
            setEnabledOfContainedComponents(input, enabled);
            label.setEnabled(enabled);
        }

        public abstract T getValue() throws Option.Error;

        public abstract void setValue(T value);

        public void commitOption() {
            if (guard.isSelected()) {
                option.setValue(getValue());
            }
        }

        public String unparse() {
            return option.getType().unparseValue(getValue());
        }

        public void parse(String str) {
            guard.setEnabled(true);
            setValue(option.getType().parseValue(str));
        }
    }

    protected static class IntegerGUIOption extends GUIOption<Integer> {
        private final JTextField textField;

        protected IntegerGUIOption(Option<Integer> option) {
            super(option);
            textField = new JTextField();
            input = textField;
            textField.setColumns(TEXT_FIELD_COLUMNS);
            setValue(option.getValue());
        }

        @Override
        public void setValue(Integer i) {
            textField.setText(String.valueOf(i));
            guard.setSelected(i != null);
            setInputAndLabelEnabled(i != null);
        }

        @Override
        public Integer getValue() {
            if (guard.isSelected()) {
                return option.getType().parseValue(textField.getText());
            }
            return null;
        }
    }

    protected static class BooleanGUIOption extends GUIOption<Boolean> {
        private final JLabel helpText;
        protected BooleanGUIOption(Option<Boolean> option) {
            super(option);
            helpText = new JLabel("<html>" + option.getHelp() + "</html>");
            input = helpText;
            setValue(option.getValue());
        }

        @Override
        public void commitOption() {
            if (guard.isSelected()) {
                option.setValue(true);
            } else {
                option.setValue(false);
            }
        }

        @Override
        public void setValue(Boolean b) {
            guard.setSelected(b);
            helpText.setEnabled(b);
            setInputAndLabelEnabled(b);
        }

        @Override
        public Boolean getValue() {
            return guard.isSelected();
        }
    }

    protected static class StringGUIOption extends GUIOption<String> {
        private final JTextField textField;

        protected StringGUIOption(Option<String> option) {
            super(option);
            textField = new JTextField(TEXT_FIELD_COLUMNS);
            input = textField;
            setValue(option.getValue());
        }

        @Override
        public void setValue(String i) {
            textField.setText(String.valueOf(i));
            guard.setSelected(i != null);
            setInputAndLabelEnabled(i != null);
        }

        @Override
        public String getValue() {
            if (guard.isSelected()) {
                return textField.getText();
            }
            return null;
        }
    }

    protected static class ListGUIOption extends GUIOption<List<Object>> {
        private final JTextField textField;
        private final JList list;

        protected ListGUIOption(Option<List<Object>> option) {
            super(option);
            if (option.getType() instanceof OptionTypes.EnumListType) {
                textField = null;
                final OptionTypes.EnumListType elt = (OptionTypes.EnumListType) option.getType();
                final OptionTypes.EnumType et = (OptionTypes.EnumType) elt.elementOptionType;
                list = new JList(et.values);
                list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
                list.setVisibleRowCount(Math.min(10, et.values.length));
                list.setLayoutOrientation(JList.VERTICAL);
                final JScrollPane scrollPane = new JScrollPane(list);
                input = scrollPane;
            } else {
                list = null;
                textField = new JTextField(TEXT_FIELD_COLUMNS);
                input = textField;
            }
            setValue(option.getValue());
        }

        @Override
        public void setValue(List<Object> objects) {
            if (objects != null) {
                if (textField == null) {
                    this.list.clearSelection();
                    final int[] selectedIndices = new int[objects.size()];
                    int i = 0;
                    for (Object value : objects) {
                        final Enum enumValue = (Enum) value;
                        selectedIndices[i++] = enumValue.ordinal();
                    }
                    this.list.setSelectedIndices(selectedIndices);
                } else {
                    textField.setText(String.valueOf(option.getType().unparseValue(objects)));
                }
            }
            guard.setSelected(objects != null);
            setInputAndLabelEnabled(objects != null);
        }

        @Override
        public List<Object> getValue() {
            if (guard.isSelected()) {
                if (textField == null) {
                    final List<Object> result = new LinkedList<Object>();
                    for (Object value : this.list.getSelectedValues()) {
                        result.add(value);
                    }
                    return result;
                }
                return option.getType().parseValue(textField.getText());
            }
            return null;
        }
    }

    protected static class URLGUIOption extends GUIOption<URL> {
        private final JTextField textField;

        protected URLGUIOption(Option<URL> option) {
            super(option);
            textField = new JTextField(TEXT_FIELD_COLUMNS);
            input = textField;
            setValue(option.getValue());
        }

        @Override
        public void setValue(URL i) {
            textField.setText(String.valueOf(i));
            guard.setSelected(i != null);
            setInputAndLabelEnabled(i != null);
        }

        @Override
        public URL getValue() {
            if (guard.isSelected()) {
                try {
                    return new URL(textField.getText());
                } catch (MalformedURLException e) {
                    return null;
                }
            }
            return null;
        }
    }

    protected static class EnumGUIOption extends GUIOption<Object> {
        private final JComboBox comboBox;

        protected EnumGUIOption(Option<Object> option) {
            super(option);
            final OptionTypes.EnumType et = (OptionTypes.EnumType) option.getType();
            comboBox = new JComboBox(et.values);
            input = comboBox;
            setValue(option.getValue());
        }

        @Override
        public void setValue(Object value) {
            comboBox.setSelectedItem(value);
            guard.setSelected(true);
        }

        @Override
        public Object getValue() {
            return comboBox.getSelectedItem();
        }
    }

    protected static class FileGUIOption extends GUIOption<File> {
        private final JTextField textField;
        private final JButton fileChooserButton;
        private JFileChooser fileChooser;

        protected FileGUIOption(Option<File> option) {
            super(option);
            textField = new JTextField(TEXT_FIELD_COLUMNS);
            final JPanel inputPanel = new JPanel();
            input = inputPanel;
            fileChooserButton = new JButton(new AbstractAction("Select") {
                public void actionPerformed(ActionEvent e) {
                    if (fileChooser == null) {
                        fileChooser = new JFileChooser(new File("."));
                        fileChooser.setCurrentDirectory(new File(System.getProperty("user.home")));
                        fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
                    }

                    final File file = new File(textField.getText());
                    if (file.exists()) {
                        final File parent = file.getParentFile();
                        if (parent != null && parent.isDirectory()) {
                            fileChooser.setCurrentDirectory(parent);
                        }
                        fileChooser.ensureFileIsVisible(file);
                        fileChooser.setSelectedFile(file);
                    }
                    if (fileChooser.showDialog(fileChooserButton, "Select") == JFileChooser.APPROVE_OPTION) {
                        textField.setText(fileChooser.getSelectedFile().getAbsolutePath());
                    }
                }
            });
            inputPanel.add(textField);
            inputPanel.add(fileChooserButton);
            setValue(option.getValue());
        }

        @Override
        public void setValue(File i) {
            if (i != null) {
                if (fileChooser != null) {
                    fileChooser.setSelectedFile(i);
                }
                textField.setText(i.getAbsolutePath());
            }
            guard.setSelected(i != null);
            setInputAndLabelEnabled(i != null);
        }

        @Override
        public File getValue() {
            return new File(textField.getText());
        }

    }

    protected static class PackageGUIOption extends GUIOption<String> {
        private final JComboBox values;

        protected PackageGUIOption(Option<String> option) {
            super(option);

            final PackageOptionType type = (PackageOptionType) option.getType();

            final Set<String> pkgNames = new TreeSet<String>();
            final String root = type.superPackage;
            new ClassSearch() {
                @Override
                protected boolean visitClass(String className) {
                    if (className.startsWith(root)) {
                        pkgNames.add(Classes.getPackageName(className));
                    }
                    return true;
                }
            }.run(Classpath.fromSystem(), root.replace('.', '/'));

            this.values = new JComboBox(pkgNames.toArray());
            input = this.values;

            // The combo box must be editable as the prepopulated items are just those packages found from the super package
            this.values.setEditable(true);

            setValue(option.getValue());
        }

        @Override
        public String getValue() {
            return option.getType().parseValue(values.getSelectedItem().toString());
        }

        @Override
        public void setValue(String p) {
            if (p != null) {
                values.setSelectedItem(p);
                guard.setSelected(true);
            } else {
                values.setSelectedIndex(-1);
                guard.setSelected(false);
            }
        }
    }

    private static GUIOption createGUIOption(Option<?> option) {
        final Option.Type<?> type = option.getType();
        if (type == OptionTypes.BOOLEAN_TYPE) {
            final Option<Boolean> opt = OptionTypes.BOOLEAN_TYPE.cast(option);
            return new BooleanGUIOption(opt);
        }
        if (type == OptionTypes.INT_TYPE) {
            final Option<Integer> opt = OptionTypes.INT_TYPE.cast(option);
            return new IntegerGUIOption(opt);
        }
        if (type == OptionTypes.STRING_TYPE) {
            final Option<String> opt = OptionTypes.STRING_TYPE.cast(option);
            return new StringGUIOption(opt);
        }
        if (type == OptionTypes.URL_TYPE) {
            final Option<URL> opt = OptionTypes.URL_TYPE.cast(option);
            return new URLGUIOption(opt);
        }
        if (type == OptionTypes.FILE_TYPE) {
            final Option<File> opt = OptionTypes.FILE_TYPE.cast(option);
            return new FileGUIOption(opt);
        }
        if (type instanceof OptionTypes.EnumType) {
            final Option<Object> opt = Utils.cast(option);
            return new EnumGUIOption(opt);
        }
        if (type instanceof OptionTypes.ListType) {
            final Option<List<Object>> opt = Utils.cast(option);
            return new ListGUIOption(opt);
        }
        if (type instanceof PackageOptionType) {
            final Option<String> opt = Utils.cast(option);
            return new PackageGUIOption(opt);
        }
        return null;
    }

    private boolean cancelled;

    public OptionsDialog(JFrame owner, final OptionSet optionSet) {
        super(owner, "Options Dialog", true);

        final LinkedList<GUIOption<?>> guiOptions = new LinkedList<GUIOption<?>>();

        final Container contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());

        // Configure panel of options
        final JPanel optionsPanel = new JPanel();
        optionsPanel.setLayout(new SpringLayout());
        optionsPanel.setBorder(BorderFactory.createTitledBorder(""));

        for (final Option<?> programOption : optionSet.getOptions()) {
            final GUIOption guiOption = createGUIOption(programOption);
            if (guiOption != null) {
                guiOptions.add(guiOption);

                final JCheckBox enabled = guiOption.getGuard();
                final JLabel label = guiOption.getLabel();

                label.setToolTipText(programOption.getHelp());

                optionsPanel.add(enabled);
                optionsPanel.add(label);
                optionsPanel.add(guiOption.getInputComponent());
            }
        }

        SpringUtilities.makeCompactGrid(optionsPanel, guiOptions.size(), 3, 0, 0, 5, 5);
        contentPane.add(optionsPanel, BorderLayout.CENTER);

        // Configure buttons
        final JButton ok = new JButton(new AbstractAction("OK") {
            public void actionPerformed(ActionEvent e) {
                for (GUIOption guiOption : guiOptions) {
                    try {
                        guiOption.commitOption();
                    } catch (Option.Error e1) {
                        showErrorDialog(guiOption, e1);
                        return;
                    }
                }
                OptionsDialog.this.setVisible(false);
            }
        });

        final JButton cancel = new JButton(new AbstractAction("Use Defaults") {
            public void actionPerformed(ActionEvent e) {
                OptionsDialog.this.setVisible(false);
                OptionsDialog.this.cancelled = true;
            }
        });

        final JButton exit = new JButton(new AbstractAction("Cancel") {
            public void actionPerformed(ActionEvent e) {
                System.exit(1);
            }
        });

        ok.setToolTipText("Apply options as configured in this dialog and ignore the command line arguments.");
        cancel.setToolTipText("Ignore options as configured in this dialog and use the command line arguments instead.");
        exit.setToolTipText("Stop running the program [calls System.exit()].");

        final JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.add(ok);
        buttonPanel.add(cancel);
        buttonPanel.add(exit);

        contentPane.add(buttonPanel, BorderLayout.SOUTH);
        pack();

        // Places dialog in the middle of the screen if 'owner == null'
        setLocationRelativeTo(owner);
    }

    private class ErrorDialog extends JDialog {
        ErrorDialog(JDialog owner, GUIOption guiOption, Option.Error error) {
            super(owner, "Option Error", true);
            final JPanel errorPanel = new JPanel();
            errorPanel.setLayout(new SpringLayout());
            errorPanel.setBorder(BorderFactory.createTitledBorder(""));

            final Container contentPane = getContentPane();
            contentPane.setLayout(new BorderLayout());

            // Configure buttons
            final JButton ok = new JButton(new AbstractAction("OK") {
                public void actionPerformed(ActionEvent e) {
                    errorPanel.setVisible(false);
                }
            });
            final JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
            buttonPanel.add(ok);

            final JLabel textPane = new JLabel();
            textPane.setText("Invalid value for " + guiOption.option.getType().getTypeName() + " option \"-" + guiOption.option.getName() + "\"");
            contentPane.add(textPane);

            contentPane.add(buttonPanel, BorderLayout.SOUTH);
            pack();

            // Places dialog in the middle of the screen if 'owner == null'
            setLocationRelativeTo(owner);
        }
    }

    public void showErrorDialog(GUIOption guiOption, Option.Error error) {
        final String typeName = guiOption.option.getType().getTypeName();
        JOptionPane.showMessageDialog(this, "Error in option \"-" + guiOption.option.getName() + "\" of type " + typeName + "\n" + error.getMessage());
//        new ErrorDialog(this, guiOption, error).setVisible(true);
    }

    /**
     * Creates a dialog for displaying a GUI for a given option set.
     *
     * @return true if the user wants to use the options as configured by the dialog, false if the caller of this method
     *         should defer to parsing an argument array instead
     */
    public static boolean show(JFrame owner, OptionSet optionSet) {
        final OptionsDialog programOptionDialog = new OptionsDialog(owner, optionSet);
        programOptionDialog.setVisible(true);
        programOptionDialog.dispose();
        return !programOptionDialog.cancelled;
    }
}
