/*
 * Copyright (c) 2009, 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.ins;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

import javax.swing.*;

import com.sun.cri.ci.*;
import com.sun.max.ins.InspectionSettings.AbstractSaveSettingsListener;
import com.sun.max.ins.InspectionSettings.SaveSettingsEvent;
import com.sun.max.ins.InspectorKeyBindings.KeyBindingMap;
import com.sun.max.ins.gui.*;
import com.sun.max.ins.util.*;
import com.sun.max.program.*;
import com.sun.max.program.option.*;

/**
 * Manages persistent general preferences for inspection sessions.
 */
final class InspectionPreferences extends AbstractSaveSettingsListener {

    private static final int TRACE_VALUE = 2;

    /**
     * Enum for the mechanism to be used when attempting to
     * {@linkplain Inspection#viewSourceExternally(CiCodePos) view} a source file in an external tool.
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

        public static List<ExternalViewerType> VALUES = Arrays.asList(values());
    }

    /**
     * Policies for how long tool tip text remains visible.
     */
    public enum ToolTipDismissDelayPolicy {

        /**
         * Use the default setting.
         */
        DEFAULT("Default delay"),
        /**
         * Double the default tool tip display time.
         */
        EXTENDED("Extended delay"),
        /**
         * Tool tip display does not stop.
         */
        PERSISTENT("Persistent");

        private final String label;

        private ToolTipDismissDelayPolicy(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }

        public static final List<ToolTipDismissDelayPolicy> VALUES = Arrays.asList(values());
    }

    private static final String INSPECTION_SETTINGS_NAME = "prefs";
    private static final String KEY_BINDINGS_PREFERENCE = "keyBindings";
    private static final String DISPLAY_STYLE_PREFERENCE = "displayStyle";
    private static final String TOOLTIP_DELAY_POLICY = "toolTipDelay";
    private static final String INVESTIGATE_WORD_VALUES_PREFERENCE = "investigateWordValues";
    private static final String EXTERNAL_VIEWER_PREFERENCE = "externalViewer";

    private final Inspection inspection;
    private final InspectionSettings settings;
    private final InspectorStyleFactory styleFactory;
    private InspectorStyle style;
    private ToolTipDismissDelayPolicy toolTipDismissDelayPolicy = ToolTipDismissDelayPolicy.DEFAULT;
    private final int defaultToolTipDismissDelay;
    private InspectorGeometry geometry;
    private boolean investigateWordValues = true;

    private ExternalViewerType externalViewerType = ExternalViewerType.SOCKET;
    private final Map<ExternalViewerType, String> externalViewerConfig = new EnumMap<ExternalViewerType, String>(ExternalViewerType.class);

    private final List<InspectorAction> actionsWithKeyBindings = new ArrayList<InspectorAction>();
    private KeyBindingMap keyBindingMap = InspectorKeyBindings.DEFAULT_KEY_BINDINGS;

    /**
     * Creates a new, global instance for managing VM inspection preferences.
     *
     * @param inspection the inspection session state
     * @param settings the manager for settings, already initialized.
     */
    InspectionPreferences(Inspection inspection, InspectionSettings settings) {
        super(INSPECTION_SETTINGS_NAME);
        this.inspection = inspection;
        this.settings = settings;
        this.styleFactory = new InspectorStyleFactory(inspection);
        this.style = styleFactory.defaultStyle();

        // Default socket for EclipseCall is 2341
        externalViewerConfig.put(ExternalViewerType.SOCKET, String.valueOf(2341));

        // TODO (mlvdv) need some way to configure this default in conjunction with style defaults.
        //this.geometry = new InspectorGeometry10Pt();
        this.geometry = new InspectorGeometry12Pt();

        defaultToolTipDismissDelay = ToolTipManager.sharedInstance().getDismissDelay();

        settings.addSaveSettingsListener(this);

        try {
            if (settings.containsKey(this, KEY_BINDINGS_PREFERENCE)) {
                final String keyBindingsName = settings.get(this, KEY_BINDINGS_PREFERENCE, OptionTypes.STRING_TYPE, null);

                final KeyBindingMap keyBindingMap = KeyBindingMap.ALL.get(keyBindingsName);
                if (keyBindingMap != null) {
                    setKeyBindingMap(keyBindingMap);
                } else {
                    InspectorWarning.message(inspection, "Unknown key bindings name ignored: " + keyBindingsName);
                }
            }

            if (settings.containsKey(this, DISPLAY_STYLE_PREFERENCE)) {
                final String displayStyleName = settings.get(this, DISPLAY_STYLE_PREFERENCE, OptionTypes.STRING_TYPE, null);
                InspectorStyle style = styleFactory.findStyle(displayStyleName);
                if (style == null) {
                    style = styleFactory.defaultStyle();
                }
                this.style = style;
            }

            investigateWordValues = settings.get(this, INVESTIGATE_WORD_VALUES_PREFERENCE, OptionTypes.BOOLEAN_TYPE, true);

            setToolTipDismissDelay(settings.get(this, TOOLTIP_DELAY_POLICY, new OptionTypes.EnumType<ToolTipDismissDelayPolicy>(ToolTipDismissDelayPolicy.class), ToolTipDismissDelayPolicy.DEFAULT));

            externalViewerType = settings.get(this, EXTERNAL_VIEWER_PREFERENCE, new OptionTypes.EnumType<ExternalViewerType>(ExternalViewerType.class), ExternalViewerType.SOCKET);
            for (ExternalViewerType externalViewerType : ExternalViewerType.VALUES) {
                final String config = settings.get(this, "externalViewer." + externalViewerType.name(), OptionTypes.STRING_TYPE, null);
                if (config != null) {
                    externalViewerConfig.put(externalViewerType, config);
                }
            }

        } catch (Option.Error optionError) {
            InspectorWarning.message(inspection, optionError);
        }

    }

    /**
     * The current configuration for visual style.
     */
    public InspectorStyle style() {
        return style;
    }

    private void setStyle(InspectorStyle style) {
        this.style = style;
        inspection.updateViewConfiguration();
    }

    private void setToolTipDismissDelay(ToolTipDismissDelayPolicy toolTipDelay) {
        this.toolTipDismissDelayPolicy = toolTipDelay;
        switch(toolTipDelay) {
            case DEFAULT:
                ToolTipManager.sharedInstance().setDismissDelay(defaultToolTipDismissDelay);
                break;
            case EXTENDED:
                ToolTipManager.sharedInstance().setDismissDelay(defaultToolTipDismissDelay * 2);
                break;
            case PERSISTENT:
                ToolTipManager.sharedInstance().setDismissDelay(Integer.MAX_VALUE);
                break;
        }
    }

    /**
     * Default size and layout for windows; overridden by persistent settings from previous sessions.
     */
    public InspectorGeometry geometry() {
        return geometry;
    }

    /**
     * @return Does the Inspector attempt to discover proactively what word values might point to in the VM.
     */
    public boolean investigateWordValues() {
        return investigateWordValues;
    }

    public ExternalViewerType externalViewerType() {
        return externalViewerType;
    }

    public Map<ExternalViewerType, String> externalViewerConfig() {
        return externalViewerConfig;
    }

    public void setExternalViewer(ExternalViewerType externalViewerType) {
        if (this.externalViewerType != externalViewerType) {
            this.externalViewerType = externalViewerType;
            settings.save();
        }
    }

    public void setExternalViewerConfiguration(ExternalViewerType externalViewerType, String config) {
        if (externalViewerConfig.get(externalViewerType) != config) {
            externalViewerConfig.put(externalViewerType, config);
            settings.save();
        }
    }

    /**
     * Informs this inspection of a new action that can operate on this inspection.
     */
    public void registerAction(InspectorAction inspectorAction) {
        final Class<? extends InspectorAction> actionClass = inspectorAction.getClass();
        if (InspectorKeyBindings.KEY_BINDABLE_ACTIONS.contains(actionClass)) {
            actionsWithKeyBindings.add(inspectorAction);
            final KeyStroke keyStroke = keyBindingMap.get(actionClass);
            inspectorAction.putValue(Action.ACCELERATOR_KEY, keyStroke);
        }
    }

    /**
     * Updates the current key binding map for this inspection.
     *
     * @param keyBindingMap a key binding map. If this value differs from the current key
     *            binding map, then the accelerator keys of all the relevant Inspector actions are updated.
     */
    public void setKeyBindingMap(KeyBindingMap keyBindingMap) {
        if (keyBindingMap != this.keyBindingMap) {
            this.keyBindingMap = keyBindingMap;
            for (InspectorAction inspectorAction : actionsWithKeyBindings) {
                final KeyStroke keyStroke = keyBindingMap.get(inspectorAction.getClass());
                Trace.line(TRACE_VALUE, "Binding " + keyStroke + " to " + inspectorAction);
                inspectorAction.putValue(Action.ACCELERATOR_KEY, keyStroke);
            }
        }
    }

    public void saveSettings(SaveSettingsEvent saveSettingsEvent) {
        saveSettingsEvent.save(KEY_BINDINGS_PREFERENCE, keyBindingMap.name());
        saveSettingsEvent.save(DISPLAY_STYLE_PREFERENCE, style().name());
        saveSettingsEvent.save(TOOLTIP_DELAY_POLICY, toolTipDismissDelayPolicy.name());
        saveSettingsEvent.save(INVESTIGATE_WORD_VALUES_PREFERENCE, investigateWordValues);
        saveSettingsEvent.save(EXTERNAL_VIEWER_PREFERENCE, externalViewerType.name());
        for (ExternalViewerType externalViewerType : ExternalViewerType.VALUES) {
            final String config = externalViewerConfig.get(externalViewerType);
            if (config != null) {
                saveSettingsEvent.save(EXTERNAL_VIEWER_PREFERENCE + "." + externalViewerType.name(), config);
            }
        }
    }

    /**
     * Gets a panel for configuring general Inspector preferences..
     */
    JPanel getPanel() {
        final JPanel panel = new InspectorPanel(inspection);
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

        final JPanel interiorPanel = new InspectorPanel(inspection);

        // Add key binding chooser
        final Collection<KeyBindingMap> allKeyBindingMaps = KeyBindingMap.ALL.values();
        final JComboBox keyBindingsComboBox = new InspectorComboBox(inspection, allKeyBindingMaps.toArray());
        keyBindingsComboBox.setSelectedItem(keyBindingMap);
        keyBindingsComboBox.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                setKeyBindingMap((KeyBindingMap) keyBindingsComboBox.getSelectedItem());
            }
        });
        interiorPanel.add(new TextLabel(inspection, "Key Bindings:  "));
        interiorPanel.add(keyBindingsComboBox);

        // Add display style chooser
        final JComboBox uiComboBox = new InspectorComboBox(inspection, styleFactory.allStyles());
        uiComboBox.setSelectedItem(style);
        uiComboBox.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                setStyle((InspectorStyle) uiComboBox.getSelectedItem());
            }
        });
        interiorPanel.add(new TextLabel(inspection, "Display style:  "));
        interiorPanel.add(uiComboBox);

        // Add tool tip policy chooser
        final JComboBox toolTipComboBox = new InspectorComboBox(inspection, ToolTipDismissDelayPolicy.values());
        toolTipComboBox.setSelectedItem(toolTipDismissDelayPolicy);
        toolTipComboBox.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                setToolTipDismissDelay((ToolTipDismissDelayPolicy) toolTipComboBox.getSelectedItem());
            }
        });
        interiorPanel.add(new TextLabel(inspection, "ToolTip dismiss: "));
        interiorPanel.add(toolTipComboBox);

        final JPanel panel = new InspectorPanel(inspection, new BorderLayout());
        panel.add(interiorPanel, BorderLayout.WEST);
        return panel;
    }

    /**
     * @return a GUI panel for setting debugging preferences
     */
    private JPanel getDebugPanel() {

        final InspectorCheckBox wordValueCheckBox =
            new InspectorCheckBox(inspection,
                            "Investigate memory references",
                            "Should displayed memory words be investigated as possible references by reading from the VM",
                            investigateWordValues);
        wordValueCheckBox.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent actionEvent) {
                final InspectorCheckBox checkBox = (InspectorCheckBox) actionEvent.getSource();
                investigateWordValues = checkBox.isSelected();
                inspection.updateViewConfiguration();
            }
        });

        final JPanel panel = new InspectorPanel(inspection, new BorderLayout());

        final JPanel interiorPanel = new InspectorPanel(inspection);
        interiorPanel.add(new TextLabel(inspection, "Debugging:  "));

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

        final JPanel cards = new InspectorPanel(inspection, new CardLayout());
        final JPanel noneCard = new InspectorPanel(inspection);

        final JPanel processCard = new InspectorPanel(inspection);
        processCard.add(new TextLabel(inspection, "Command: "));
        processCard.setToolTipText("The pattern '$file' will be replaced with the full path to the file and '$line' will be replaced with the line number to view.");
        final JTextArea commandTextArea = new JTextArea(2, 30);
        commandTextArea.setLineWrap(true);
        commandTextArea.setWrapStyleWord(true);
        commandTextArea.setText(externalViewerConfig.get(ExternalViewerType.PROCESS));
        commandTextArea.addFocusListener(new FocusAdapter() {

            @Override
            public void focusLost(FocusEvent e) {
                final String command = commandTextArea.getText();
                setExternalViewerConfiguration(ExternalViewerType.PROCESS, command);
            }
        });
        final JScrollPane scrollPane = new InspectorScrollPane(inspection, commandTextArea);
        processCard.add(scrollPane);

        final JPanel socketCard = new InspectorPanel(inspection);
        final JTextField portTextField = new JTextField(6);
        portTextField.setText(externalViewerConfig.get(ExternalViewerType.SOCKET));
        socketCard.add(new TextLabel(inspection, "Port: "));
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
        final JComboBox comboBox = new InspectorComboBox(inspection, values);
        comboBox.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                final ExternalViewerType externalViewerType = (ExternalViewerType) comboBox.getSelectedItem();
                setExternalViewer(externalViewerType);
                final CardLayout cardLayout = (CardLayout) cards.getLayout();
                cardLayout.show(cards, externalViewerType.name());
            }
        });
        comboBox.setSelectedItem(externalViewerType);
        final CardLayout cardLayout = (CardLayout) cards.getLayout();
        cardLayout.show(cards, externalViewerType.name());

        final JPanel comboBoxPanel = new InspectorPanel(inspection);
        comboBoxPanel.add(new TextLabel(inspection, "External File Viewer:  "));
        comboBoxPanel.add(comboBox);

        final JPanel panel = new InspectorPanel(inspection);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.add(comboBoxPanel);
        panel.add(cards);
        return panel;
    }

}
