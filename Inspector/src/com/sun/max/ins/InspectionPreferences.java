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
package com.sun.max.ins;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

import javax.swing.*;

import com.sun.max.collect.*;
import com.sun.max.ins.InspectionSettings.*;
import com.sun.max.ins.InspectorKeyBindings.*;
import com.sun.max.ins.gui.*;
import com.sun.max.program.*;
import com.sun.max.program.option.*;
import com.sun.max.vm.bytecode.*;


/**
 * Manages persistent general preferences for inspection sessions.
 *
 * @author Michael Van De Vanter
 */
final class InspectionPreferences extends AbstractSaveSettingsListener {

    private static final int TRACE_VALUE = 2;

    /**
     * Enum for the mechanism to be used when attempting to
     * {@linkplain Inspection#viewSourceExternally(BytecodeLocation) view} a source file in an external tool.
     */
    public enum ExternalViewerType {
        /**
         * Specifies that there is no external viewer available.
         */
        NONE,

        /**
         * Specifies that an external viewer is listening on a socket for 'open file' requests. A request is a string of
         * bytes matching this pattern:
         *
         * <pre>
         *     &lt;path to file&gt;|&lt;line number&gt;
         * </pre>
         *
         * For example, the following code generates the bytes of a typical command:
         *
         * <pre>
         * &quot;/maxine/VM/src/com/sun/max/vm/MaxineVM.java|239&quot;.getBytes()
         * </pre>
         */
        SOCKET,

        /**
         * Specifies that an external tool can be launched as a separate process.
         */
        PROCESS;

        public static final IndexedSequence<ExternalViewerType> VALUES = new ArraySequence<ExternalViewerType>(values());
    }

    private static final String INSPECTION_SETTINGS_NAME = "inspection";
    private static final String KEY_BINDINGS_PREFERENCE = "keyBindings";
    private static final String DISPLAY_STYLE_PREFERENCE = "displayStyle";
    private static final String INVESTIGATE_WORD_VALUES_PREFERENCE = "investigateWordValues";
    private static final String EXTERNAL_VIEWER_PREFERENCE = "externalViewer";

    private final Inspection _inspection;
    private final InspectionSettings _settings;
    private final InspectorStyleFactory _styleFactory;
    private InspectorStyle _style;
    private InspectorGeometry _geometry;
    private boolean _investigateWordValues = true;

    private ExternalViewerType _externalViewerType = ExternalViewerType.NONE;
    private final Map<ExternalViewerType, String> _externalViewerConfig = new EnumMap<ExternalViewerType, String>(ExternalViewerType.class);

    private final AppendableSequence<InspectorAction> _actionsWithKeyBindings = new ArrayListSequence<InspectorAction>();
    private KeyBindingMap _keyBindingMap = InspectorKeyBindings.DEFAULT_KEY_BINDINGS;


    /**
     * Creates a new, global instance for managing Maxine VM inspection preferences.
     *
     * @param inspection the inspection session state
     * @param settings the manager for settings, already initialized.
     */
    InspectionPreferences(Inspection inspection, InspectionSettings settings) {
        super(INSPECTION_SETTINGS_NAME);
        _inspection = inspection;
        _settings = settings;
        _styleFactory = new InspectorStyleFactory(inspection);
        _style = _styleFactory.defaultStyle();

        // TODO (mlvdv) need some way to configure this default in conjunction with style defaults.
        //_geometry = new InspectorGeometry10Pt();
        _geometry = new InspectorGeometry12Pt();

        settings.addSaveSettingsListener(this);

        try {
            if (settings.containsKey(this, KEY_BINDINGS_PREFERENCE)) {
                final String keyBindingsName = settings.get(this, KEY_BINDINGS_PREFERENCE, OptionTypes.STRING_TYPE, null);

                final KeyBindingMap keyBindingMap = KeyBindingMap.ALL.get(keyBindingsName);
                if (keyBindingMap != null) {
                    setKeyBindingMap(keyBindingMap);
                } else {
                    ProgramWarning.message("Unknown key bindings name ignored: " + keyBindingsName);
                }
            }

            if (settings.containsKey(this, DISPLAY_STYLE_PREFERENCE)) {
                final String displayStyleName = settings.get(this, DISPLAY_STYLE_PREFERENCE, OptionTypes.STRING_TYPE, null);
                InspectorStyle style = _styleFactory.findStyle(displayStyleName);
                if (style == null) {
                    style = _styleFactory.defaultStyle();
                }
                _style = style;
            }

            _investigateWordValues = settings.get(this, INVESTIGATE_WORD_VALUES_PREFERENCE, OptionTypes.BOOLEAN_TYPE, true);
            _externalViewerType = settings.get(this, EXTERNAL_VIEWER_PREFERENCE, new OptionTypes.EnumType<ExternalViewerType>(ExternalViewerType.class), ExternalViewerType.NONE);
            for (ExternalViewerType externalViewerType : ExternalViewerType.VALUES) {
                final String config = settings.get(this, "externalViewer." + externalViewerType.name(), OptionTypes.STRING_TYPE, null);
                _externalViewerConfig.put(externalViewerType, config);
            }
        } catch (Option.Error optionError) {
            ProgramWarning.message(optionError.getMessage());
        }

    }

    /**
     * The current configuration for visual style.
     */
    public InspectorStyle style() {
        return _style;
    }

    private void setStyle(InspectorStyle style) {
        _style = style;
        _inspection.updateViewConfiguration();
    }

    /**
     * Default size and layout for windows; overridden by persistent settings from previous sessions.
     */
    public InspectorGeometry geometry() {
        return _geometry;
    }

    /**
     * @return Does the Inspector attempt to discover proactively what word values might point to in the VM.
     */
    public boolean investigateWordValues() {
        return _investigateWordValues;
    }

    public ExternalViewerType externalViewerType() {
        return _externalViewerType;
    }

    public Map<ExternalViewerType, String> externalViewerConfig() {
        return _externalViewerConfig;
    }

    public void setExternalViewer(ExternalViewerType externalViewerType) {
        if (_externalViewerType != externalViewerType) {
            _externalViewerType = externalViewerType;
            _settings.save();
        }
    }

    public void setExternalViewerConfiguration(ExternalViewerType externalViewerType, String config) {
        if (_externalViewerConfig.get(externalViewerType) != config) {
            _externalViewerConfig.put(externalViewerType, config);
            _settings.save();
        }
    }

    /**
     * Informs this inspection of a new action that can operate on this inspection.
     */
    public void registerAction(InspectorAction inspectorAction) {
        final Class<? extends InspectorAction> actionClass = inspectorAction.getClass();
        if (InspectorKeyBindings.KEY_BINDABLE_ACTIONS.contains(actionClass)) {
            _actionsWithKeyBindings.append(inspectorAction);
            final KeyStroke keyStroke = _keyBindingMap.get(actionClass);
            inspectorAction.putValue(Action.ACCELERATOR_KEY, keyStroke);
        }
    }

    /**
     * Updates the current key binding map for this inspection.
     *
     * @param keyBindingMap a key binding map. If this value differs from the current key
     *            binding map, then the accelerator keys of all the relevant inspector actions are updated.
     */
    public void setKeyBindingMap(KeyBindingMap keyBindingMap) {
        if (keyBindingMap != _keyBindingMap) {
            _keyBindingMap = keyBindingMap;
            for (InspectorAction inspectorAction : _actionsWithKeyBindings) {
                final KeyStroke keyStroke = keyBindingMap.get(inspectorAction.getClass());
                Trace.line(TRACE_VALUE, "Binding " + keyStroke + " to " + inspectorAction);
                inspectorAction.putValue(Action.ACCELERATOR_KEY, keyStroke);
            }
        }
    }


    public void saveSettings(SaveSettingsEvent saveSettingsEvent) {
        saveSettingsEvent.save(KEY_BINDINGS_PREFERENCE, _keyBindingMap.name());
        saveSettingsEvent.save(DISPLAY_STYLE_PREFERENCE, style().name());
        saveSettingsEvent.save(INVESTIGATE_WORD_VALUES_PREFERENCE, _investigateWordValues);
        saveSettingsEvent.save(EXTERNAL_VIEWER_PREFERENCE, _externalViewerType.name());
        for (ExternalViewerType externalViewerType : ExternalViewerType.VALUES) {
            final String config = _externalViewerConfig.get(externalViewerType);
            if (config != null) {
                saveSettingsEvent.save(EXTERNAL_VIEWER_PREFERENCE + "." + externalViewerType.name(), config);
            }
        }
    }

    /**
     * Gets a panel for configuring general Inspector preferences..
     */
    JPanel getPanel() {
        final JPanel panel = new InspectorPanel(_inspection);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        final JPanel keyBindingsPanel = getUIPanel();
        final JPanel debugPanel = getDebugPanel();
        final JPanel externalViewerPanel = getExternalViewerPanel();

        keyBindingsPanel.setBorder(BorderFactory.createEtchedBorder());
        debugPanel.setBorder(BorderFactory.createEtchedBorder());
        externalViewerPanel.setBorder(BorderFactory.createEtchedBorder());

        panel.add(keyBindingsPanel);
        panel.add(debugPanel);
        panel.add(externalViewerPanel);

        return panel;
    }

    /**
     * Gets a panel for configuring which key binding map is active.
     */
    private JPanel getUIPanel() {

        final JPanel interiorPanel = new InspectorPanel(_inspection);

        // Add key binding chooser
        final Collection<KeyBindingMap> allKeyBindingMaps = KeyBindingMap.ALL.values();
        final JComboBox keyBindingsComboBox = new InspectorComboBox(_inspection, allKeyBindingMaps.toArray());
        keyBindingsComboBox.setSelectedItem(_keyBindingMap);
        keyBindingsComboBox.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                setKeyBindingMap((KeyBindingMap) keyBindingsComboBox.getSelectedItem());
            }
        });
        interiorPanel.add(new TextLabel(_inspection, "Key Bindings:  "));
        interiorPanel.add(keyBindingsComboBox);

        // Add display style chooser
        final JComboBox uiComboBox = new InspectorComboBox(_inspection, _styleFactory.allStyles());
        uiComboBox.setSelectedItem(_style);
        uiComboBox.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                setStyle((InspectorStyle) uiComboBox.getSelectedItem());
            }
        });
        interiorPanel.add(new TextLabel(_inspection, "Display style:  "));
        interiorPanel.add(uiComboBox);

        final JPanel panel = new InspectorPanel(_inspection, new BorderLayout());
        panel.add(interiorPanel, BorderLayout.WEST);
        return panel;
    }

    /**
     * @return a GUI panel for setting debugging preferences
     */
    private JPanel getDebugPanel() {

        final JCheckBox wordValueCheckBox =
            new InspectorCheckBox(_inspection,
                            "Investigate memory references",
                            "Should displayed memory words be investigated as possible references by reading from the VM",
                            _investigateWordValues);
        wordValueCheckBox.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent actionEvent) {
                final JCheckBox checkBox = (JCheckBox) actionEvent.getSource();
                _investigateWordValues = checkBox.isSelected();
                _inspection.updateViewConfiguration();
            }
        });

        final JPanel panel = new InspectorPanel(_inspection, new BorderLayout());

        final JPanel interiorPanel = new InspectorPanel(_inspection);
        interiorPanel.add(new TextLabel(_inspection, "Debugging:  "));

        /*
         * Until there's a good reason for supporting the synchronous debugging mode, it's no longer selectable.
         * This prevents any user confusion as to why the GUI seems to freeze when the VM is running. [Doug]
        interiorPanel.add(synchButton);
        interiorPanel.add(asynchButton);
         */

        interiorPanel.add(wordValueCheckBox);
        panel.add(interiorPanel, BorderLayout.WEST);

        return panel;
    }

    /**
     * Gets a panel for configuring which key binding map is active.
     */
    public JPanel getExternalViewerPanel() {

        final JPanel cards = new InspectorPanel(_inspection, new CardLayout());
        final JPanel noneCard = new InspectorPanel(_inspection);

        final JPanel processCard = new InspectorPanel(_inspection);
        processCard.add(new TextLabel(_inspection, "Command: "));
        processCard.setToolTipText("The pattern '$file' will be replaced with the full path to the file and '$line' will be replaced with the line number to view.");
        final JTextArea commandTextArea = new JTextArea(2, 30);
        commandTextArea.setLineWrap(true);
        commandTextArea.setWrapStyleWord(true);
        commandTextArea.setText(_externalViewerConfig.get(ExternalViewerType.PROCESS));
        commandTextArea.addFocusListener(new FocusAdapter() {

            @Override
            public void focusLost(FocusEvent e) {
                final String command = commandTextArea.getText();
                setExternalViewerConfiguration(ExternalViewerType.PROCESS, command);
            }
        });
        final JScrollPane scrollPane = new InspectorScrollPane(_inspection, commandTextArea);
        processCard.add(scrollPane);

        final JPanel socketCard = new InspectorPanel(_inspection);
        final JTextField portTextField = new JTextField(6);
        portTextField.setText(_externalViewerConfig.get(ExternalViewerType.SOCKET));
        socketCard.add(new TextLabel(_inspection, "Port: "));
        socketCard.add(portTextField);
        portTextField.addFocusListener(new FocusAdapter() {

            @Override
            public void focusLost(FocusEvent e) {
                final String portString = portTextField.getText();
                setExternalViewerConfiguration(ExternalViewerType.SOCKET, portString);
            }
        });

        cards.add(noneCard, ExternalViewerType.NONE.name());
        cards.add(processCard, ExternalViewerType.PROCESS.name());
        cards.add(socketCard, ExternalViewerType.SOCKET.name());

        final ExternalViewerType[] values = ExternalViewerType.values();
        final JComboBox comboBox = new InspectorComboBox(_inspection, values);
        comboBox.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                final ExternalViewerType externalViewerType = (ExternalViewerType) comboBox.getSelectedItem();
                setExternalViewer(externalViewerType);
                final CardLayout cardLayout = (CardLayout) cards.getLayout();
                cardLayout.show(cards, externalViewerType.name());
            }
        });
        comboBox.setSelectedItem(_externalViewerType);
        final CardLayout cardLayout = (CardLayout) cards.getLayout();
        cardLayout.show(cards, _externalViewerType.name());

        final JPanel comboBoxPanel = new InspectorPanel(_inspection);
        comboBoxPanel.add(new TextLabel(_inspection, "External File Viewer:  "));
        comboBoxPanel.add(comboBox);

        final JPanel panel = new InspectorPanel(_inspection);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.add(comboBoxPanel);
        panel.add(cards);
        return panel;
    }

}
