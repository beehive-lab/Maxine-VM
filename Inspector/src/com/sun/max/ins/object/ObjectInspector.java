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
package com.sun.max.ins.object;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

import javax.swing.*;

import com.sun.max.collect.*;
import com.sun.max.gui.*;
import com.sun.max.ins.*;
import com.sun.max.ins.InspectionSettings.*;
import com.sun.max.ins.gui.*;
import com.sun.max.ins.memory.*;
import com.sun.max.ins.memory.MemoryInspector.*;
import com.sun.max.ins.memory.MemoryWordInspector.*;
import com.sun.max.ins.type.*;
import com.sun.max.ins.value.*;
import com.sun.max.program.*;
import com.sun.max.program.option.*;
import com.sun.max.tele.*;
import com.sun.max.tele.object.*;
import com.sun.max.unsafe.*;
import com.sun.max.util.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;

/**
 * An inspector that displays the content of a Maxine low level heap object in the {@link TeleVM}.
 *
 * @author Bernd Mathiske
 * @author Michael Van De Vanter
 */
public abstract class ObjectInspector<ObjectInspector_Type extends ObjectInspector> extends UniqueInspector<ObjectInspector_Type> implements MemoryInspectable, MemoryWordInspectable {

    private static Manager _manager;

    private static Preferences _globalPreferences;

    public static synchronized Preferences globalPreferences(Inspection inspection) {
        return _globalPreferences;
    }

    /**
     * Singleton manager;  no visible presence or direct user interaction at this time.
     *
     * @author Michael Van De Vanter
     */
    public static final class Manager extends InspectionHolder {

        private Manager(Inspection inspection) {
            super(inspection);
            _globalPreferences = new Preferences(inspection);
        }

        public static void make(Inspection inspection) {
            if (_manager == null) {
                Trace.begin(1, "[ObjectInspector] initializing manager");
                _manager = new Manager(inspection);
                inspection.focus().addListener(new InspectionFocusAdapter() {

                    @Override
                    public void heapObjectFocusChanged(TeleObject oldTeleObject, TeleObject teleObject) {
                        if (teleObject != null) {
                            ObjectInspector.make(_manager.inspection(), teleObject);
                        }
                    }
                });
                Trace.end(1, "[ObjectInspector] initializing manager");
            }
        }

    }

    private static final IdentityHashSet<ObjectInspector> _objectInspectors = new IdentityHashSet<ObjectInspector>();

    /**
     * Displays and highlights a kind of object inspector appropriate to the object reference.
     * @return A possibly new inspector for the object
     */
    private static void make(Inspection inspection, TeleObject teleObject) {
        final boolean isArray = teleObject instanceof TeleArrayObject;
        final boolean isTuple = teleObject instanceof TeleTupleObject;
        final Class<? extends ObjectInspector> inspectorClass = isArray ? ArrayInspector.class : isTuple ? TupleInspector.class : HubInspector.class;
        final UniqueInspector.Key<? extends ObjectInspector> key = UniqueInspector.Key.create(inspection, inspectorClass, teleObject.reference());
        ObjectInspector objectInspector = UniqueInspector.find(inspection, key);
        if (objectInspector == null) {
            if (isArray) {
                objectInspector = new ArrayInspector(inspection, Residence.INTERNAL, teleObject);
            } else if (isTuple) {
                objectInspector = new TupleInspector(inspection, Residence.INTERNAL, teleObject);
            } else {
                if (!(teleObject instanceof TeleHybridObject)) {
                    assert teleObject instanceof TeleHybridObject;
                }
                objectInspector = new HubInspector(inspection, Residence.INTERNAL, teleObject);
            }
        }
        objectInspector.highlight();
    }

    // Preferences

    public static class Preferences {
        private final Inspection _inspection;
        boolean _showHeader;
        boolean _showAddresses;
        boolean _showOffsets;
        boolean _showFieldType;
        boolean _showMemoryRegion;

        Preferences(Inspection inspection) {
            _inspection = inspection;
            final InspectionSettings settings = inspection.settings();
            final SaveSettingsListener saveSettingsListener = new AbstractSaveSettingsListener("objectInspectorPrefs", null) {
                public void saveSettings(SaveSettingsEvent saveSettingsEvent) {
                    saveSettingsEvent.save("showHeader", _showHeader);
                    saveSettingsEvent.save("showAddresses", _showAddresses);
                    saveSettingsEvent.save("showOffsets", _showOffsets);
                    saveSettingsEvent.save("showFieldType", _showFieldType);
                    saveSettingsEvent.save("showMemoryRegion",  _showMemoryRegion);
                }
            };
            settings.addSaveSettingsListener(saveSettingsListener);

            _showHeader = settings.get(saveSettingsListener, "showHeader", OptionTypes.BOOLEAN_TYPE, true);
            _showAddresses = settings.get(saveSettingsListener, "showAddresses", OptionTypes.BOOLEAN_TYPE, false);
            _showOffsets = settings.get(saveSettingsListener, "showOffsets", OptionTypes.BOOLEAN_TYPE, true);
            _showFieldType = settings.get(saveSettingsListener, "showFieldType", OptionTypes.BOOLEAN_TYPE, true);
            _showMemoryRegion = settings.get(saveSettingsListener, "showMemoryRegion", OptionTypes.BOOLEAN_TYPE, false);
        }

        /**
         * @return a GUI panel for setting preferences
         */
        public JPanel getPanel() {
            final JCheckBox alwaysShowHeaderCheckBox = new JCheckBox("Header");
            alwaysShowHeaderCheckBox.setOpaque(true);
            alwaysShowHeaderCheckBox.setBackground(_inspection.style().defaultBackgroundColor());
            alwaysShowHeaderCheckBox.setToolTipText("Show new Object Inspectors initially display the header?");
            alwaysShowHeaderCheckBox.setSelected(_showHeader);

            final JCheckBox alwaysShowAddressesCheckBox = new JCheckBox("Addresses");
            alwaysShowAddressesCheckBox.setOpaque(true);
            alwaysShowAddressesCheckBox.setBackground(_inspection.style().defaultBackgroundColor());
            alwaysShowAddressesCheckBox.setToolTipText("Display addresses?");
            alwaysShowAddressesCheckBox.setSelected(_showAddresses);

            final JCheckBox alwaysShowOffsetsCheckBox = new JCheckBox("Offsets");
            alwaysShowOffsetsCheckBox.setOpaque(true);
            alwaysShowOffsetsCheckBox.setBackground(_inspection.style().defaultBackgroundColor());
            alwaysShowOffsetsCheckBox.setToolTipText("Display offsets?");
            alwaysShowOffsetsCheckBox.setSelected(_showOffsets);

            final JCheckBox alwaysShowTupleTypeCheckBox = new JCheckBox("Type");
            alwaysShowTupleTypeCheckBox.setOpaque(true);
            alwaysShowTupleTypeCheckBox.setBackground(_inspection.style().defaultBackgroundColor());
            alwaysShowTupleTypeCheckBox.setToolTipText("Display types in tuples?");
            alwaysShowTupleTypeCheckBox.setSelected(_showFieldType);

            final JCheckBox alwaysShowMemoryRegionCheckBox = new JCheckBox("Region");
            alwaysShowMemoryRegionCheckBox.setOpaque(true);
            alwaysShowMemoryRegionCheckBox.setBackground(_inspection.style().defaultBackgroundColor());
            alwaysShowMemoryRegionCheckBox.setToolTipText("Display memory region in tuples?");
            alwaysShowMemoryRegionCheckBox.setSelected(_showMemoryRegion);

            final ItemListener itemListener = new ItemListener() {
                public void itemStateChanged(ItemEvent e) {
                    final Object source = e.getItemSelectable();
                    if (source == alwaysShowHeaderCheckBox) {
                        _showHeader = alwaysShowHeaderCheckBox.isSelected();
                    } else if (source == alwaysShowAddressesCheckBox) {
                        _showAddresses = alwaysShowAddressesCheckBox.isSelected();
                    } else if (source == alwaysShowOffsetsCheckBox) {
                        _showOffsets = alwaysShowOffsetsCheckBox.isSelected();
                    } else if (source == alwaysShowTupleTypeCheckBox) {
                        _showFieldType = alwaysShowTupleTypeCheckBox.isSelected();
                    } else if (source == alwaysShowMemoryRegionCheckBox) {
                        _showMemoryRegion = alwaysShowMemoryRegionCheckBox.isSelected();
                    }
                    _inspection.settings().save();
                }
            };
            alwaysShowHeaderCheckBox.addItemListener(itemListener);
            alwaysShowAddressesCheckBox.addItemListener(itemListener);
            alwaysShowOffsetsCheckBox.addItemListener(itemListener);
            alwaysShowTupleTypeCheckBox.addItemListener(itemListener);
            alwaysShowMemoryRegionCheckBox.addItemListener(itemListener);

            final JPanel panel = new JPanel(new BorderLayout());
            panel.setOpaque(true);
            panel.setBackground(_inspection.style().defaultBackgroundColor());

            final JPanel content = new JPanel();
            content.add(new TextLabel(_inspection, "Show:  "));
            content.add(alwaysShowHeaderCheckBox);
            content.add(alwaysShowAddressesCheckBox);
            content.add(alwaysShowOffsetsCheckBox);
            content.add(alwaysShowTupleTypeCheckBox);
            content.add(alwaysShowMemoryRegionCheckBox);

            panel.add(content, BorderLayout.WEST);
            return panel;
        }

        void showDialog() {
            new Dialog(_inspection);
        }

        private final class Dialog extends InspectorDialog {

            Dialog(Inspection inspection) {
                super(inspection, "Object Inspector Preferences", false);

                final JPanel dialogPanel = new JPanel();
                dialogPanel.setLayout(new BorderLayout());
                dialogPanel.setOpaque(true);
                dialogPanel.setBackground(style().defaultBackgroundColor());

                final JPanel buttons = new JPanel();
                buttons.setOpaque(true);
                buttons.setBackground(style().defaultBackgroundColor());
                buttons.add(new JButton(new InspectorAction(inspection(), "Close") {
                    @Override
                    protected void procedure() {
                        dispose();
                    }
                }));

                dialogPanel.add(getPanel(), BorderLayout.NORTH);
                dialogPanel.add(buttons, BorderLayout.SOUTH);

                setContentPane(dialogPanel);
                pack();
                inspection().moveToMiddle(this);
                setVisible(true);
            }
        }

    }


    private final TeleObject _teleObject;

    /**
     * @return local surrogate for the object being inspected in the {@link TeleVM}
     */
    public TeleObject teleObject() {
        return _teleObject;
    }

    /** The origin is an actual location in memory of the {@link TeleVM};
     * keep a copy for comparison, since it might change via GC.
     */
    private Pointer _currentObjectOrigin;

    private final JCheckBoxMenuItem _showHeaderMenuCheckBox;
    private final JCheckBoxMenuItem _showAddressesMenuCheckBox;
    private final JCheckBoxMenuItem _showOffsetsMenuCheckBox;
    private final JCheckBoxMenuItem _showTypeMenuCheckBox;
    private final JCheckBoxMenuItem _showMemoryRegionMenuCheckBox;

    private ObjectHeaderInspector _objectHeaderInspector;

    protected ObjectInspector(final Inspection inspection, Residence residence, final TeleObject teleObject) {
        super(inspection, residence, teleObject.reference());
        final Preferences preferences = globalPreferences(inspection);
        _teleObject = teleObject;
        _currentObjectOrigin = teleObject().getCurrentOrigin();
        Trace.line(1, tracePrefix() + " creating for " + getTextForTitle());
        _showHeaderMenuCheckBox = new JCheckBoxMenuItem("Display Object Header", preferences._showHeader);
        _showHeaderMenuCheckBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                if (!_showHeaderMenuCheckBox.getState()) {
                    _objectHeaderInspector = null;
                }
                reconstructView();
            }
        });
        _showAddressesMenuCheckBox = new JCheckBoxMenuItem("Display addresses", preferences._showAddresses);
        _showAddressesMenuCheckBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                reconstructView();
            }
        });
        _showOffsetsMenuCheckBox = new JCheckBoxMenuItem("Display offsets", preferences._showOffsets);
        _showOffsetsMenuCheckBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                reconstructView();
            }
        });
        _showTypeMenuCheckBox = new JCheckBoxMenuItem("Display tuple types", preferences._showFieldType);
        _showTypeMenuCheckBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                reconstructView();
            }
        });
        _showMemoryRegionMenuCheckBox = new JCheckBoxMenuItem("Display memory region", preferences._showMemoryRegion);
        _showMemoryRegionMenuCheckBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                reconstructView();
            }
        });
        _objectInspectors.add(this);
    }

    @Override
    public void createFrame(InspectorMenu menu) {
        super.createFrame(menu);
        setLocationRelativeToMouse(inspection().geometry().objectInspectorNewFrameDiagonalOffset());
        frame().menu().addSeparator();
        frame().menu().add(_showHeaderMenuCheckBox);
        frame().menu().add(_showAddressesMenuCheckBox);
        frame().menu().add(_showOffsetsMenuCheckBox);
        frame().menu().add(_showTypeMenuCheckBox);
        frame().menu().add(_showMemoryRegionMenuCheckBox);
        frame().menu().add(new InspectorAction(inspection(), "Object Display Prefs..") {
            @Override
            public void procedure() {
                globalPreferences(inspection()).showDialog();
            }
        });
        frame().menu().addSeparator();
        frame().menu().add(inspection().getDeleteInspectorsAction(_otherObjectInspectorsPredicate, "Close other object inspectors"));
        frame().menu().add(inspection().getDeleteInspectorsAction(_allObjectInspectorsPredicate, "Close all object inspectors"));
    }

    @Override
    protected synchronized void createView(long epoch) {
        final JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.setOpaque(true);
        panel.setBackground(style().defaultBackgroundColor());
        if (_showHeaderMenuCheckBox.getState()) {
            _objectHeaderInspector = new ObjectHeaderInspector(inspection(), teleObject(), this, valueLabels());
            panel.add(_objectHeaderInspector, BorderLayout.NORTH);
        }
        frame().setContentPane(panel);
    }

    @Override
    public final String getTextForTitle() {
        return _teleObject.getCurrentOrigin().toHexString() + inspection().nameDisplay().referenceLabelText(_teleObject);
    }

    @Override
    public void inspectorGetsWindowFocus() {
        if (_teleObject != inspection().focus().heapObject()) {
            inspection().focus().setHeapObject(_teleObject);
        }
        super.inspectorGetsWindowFocus();
    }

    @Override
    public void inspectorLosesWindowFocus() {
        if (_teleObject == inspection().focus().heapObject()) {
            inspection().focus().setHeapObject(null);
        }
        super.inspectorLosesWindowFocus();
    }

    @Override
    public void inspectorClosing() {
        // don't try to recompute the title, just get the one that's been in use
        Trace.line(1, tracePrefix() + " closing for " + getCurrentTitle());
        if (_teleObject == inspection().focus().heapObject()) {
            inspection().focus().setHeapObject(null);
        }
        super.inspectorClosing();
    }

     /**
     * @return whether to display the "Address" column for headers, tuples, and arrays
     */
    public boolean showAddresses() {
        return _showAddressesMenuCheckBox.getState();
    }

    /**
     * @return whether to display the "Offset" column for headers, tuples and arrays
     */
    public boolean showOffsets() {
        return _showOffsetsMenuCheckBox.getState();
    }

    /**
     * @return whether to display the "Type" column for headers and tuples
     */
    public boolean showType() {
        return _showTypeMenuCheckBox.getState();
    }

    /**
     * @return whether to display the "Region" column for headers and tuples
     */
    public boolean showMemoryRegion() {
        return _showMemoryRegionMenuCheckBox.getState();
    }


    /**
     * @return how many columns are currently being displayed
     */
    public int numberOfTupleColumns() {
        int result = 2; // always show field name and value
        if (showAddresses()) {
            result++;
        }
        if (showOffsets()) {
            result++;
        }
        if (showType()) {
            result++;
        }
        if (showMemoryRegion()) {
            result++;
        }
        return result;
    }

    /**
     * @return how many columns are currently being displayed
     */
    public int numberOfArrayColumns() {
        int result = 2;
        if (showAddresses()) {
            result++;
        }
        if (showOffsets()) {
            result++;
        }
        if (showMemoryRegion()) {
            result++;
        }
        return result;
    }

    private static final Predicate<Inspector> _allObjectInspectorsPredicate = new Predicate<Inspector>() {
        public boolean evaluate(Inspector inspector) {
            return inspector instanceof ObjectInspector;
        }
    };

    private final Predicate<Inspector> _otherObjectInspectorsPredicate = new Predicate<Inspector>() {
        public boolean evaluate(Inspector inspector) {
            return inspector instanceof ObjectInspector && inspector != ObjectInspector.this;
        }
    };

    protected abstract AppendableSequence<ValueLabel> valueLabels();

    @Override
    public synchronized void refreshView(long epoch, boolean force) {
        if (isShowing()) {
            final Pointer newOrigin = _teleObject.getCurrentOrigin();
            if (!newOrigin.equals(_currentObjectOrigin)) {
                // The object has been relocated in memory
                _currentObjectOrigin = newOrigin;
                reconstructView();
            } else {
                if (_objectHeaderInspector != null) {
                    _objectHeaderInspector.refresh(epoch, force);
                }
                for (ValueLabel valueLabel : valueLabels()) {
                    valueLabel.refresh(epoch, force);
                }
            }
            super.refreshView(epoch, force);
        }
    }


    /**
     * Adds to the {@link JPanel} a row of labels for each field defined the {@link ClassActor}, and
     * adds the {@link ValueLabel} on the row to a collection used to update the values on refresh.
     */
    private void displayFields(JPanel fieldsPanel, ClassActor classActor, AppendableSequence<ValueLabel> valueLabels) {
        if (classActor == null) {
            return;
        }
        if (!(_teleObject instanceof TeleStaticTuple)) {
            displayFields(fieldsPanel, classActor.superClassActor(), valueLabels);
        }
        final FieldActor[] fieldActors = _teleObject instanceof TeleStaticTuple ? classActor.localStaticFieldActors() : classActor.localInstanceFieldActors();
        Arrays.sort(fieldActors, new Comparator<FieldActor>() {
            public int compare(FieldActor a, FieldActor b) {
                final Integer aOffset = a.offset();
                return aOffset.compareTo(b.offset());
            }
        });
        for (final FieldActor fieldActor : fieldActors) {
            if (showAddresses()) {
                fieldsPanel.add(new LocationLabel.AsAddressWithOffset(inspection(), fieldActor.offset(), _currentObjectOrigin));  // Field address
            }
            if (showOffsets()) {
                fieldsPanel.add(new LocationLabel.AsOffset(inspection(), fieldActor.offset(), _currentObjectOrigin));                           // Field position
            }
            if (showType()) {
                fieldsPanel.add(new ClassActorLabel(inspection(), fieldActor.descriptor()));                                                              // Field type
            }
            fieldsPanel.add(new FieldActorLabel(inspection(), fieldActor));                                                                                     // Field name

            ValueLabel valueLabel;
            if (fieldActor.kind() == Kind.REFERENCE) {
                valueLabel = new WordValueLabel(inspection(), WordValueLabel.ValueMode.REFERENCE) {
                    @Override
                    public Value fetchValue() {
                        return _teleObject.readFieldValue(fieldActor);
                    }
                };
            } else if (fieldActor.kind() == Kind.WORD) {
                valueLabel = new WordValueLabel(inspection(), WordValueLabel.ValueMode.WORD) {
                    @Override
                    public Value fetchValue() {
                        return _teleObject.readFieldValue(fieldActor);
                    }
                };
            } else {
                if (_teleObject instanceof TeleActor && fieldActor.name().toString().equals("_flags")) {
                    final TeleActor teleActor = (TeleActor) _teleObject;
                    valueLabel =  new ActorFlagsValueLabel(inspection(), teleActor);
                } else {
                    valueLabel = new PrimitiveValueLabel(inspection(), fieldActor.kind()) {
                        @Override
                        public Value fetchValue() {
                            return _teleObject.readFieldValue(fieldActor);
                        }
                    };
                }
            }
            valueLabels.append(valueLabel);
            fieldsPanel.add(valueLabel);                                                                                                                                     // Field value

            if (showMemoryRegion()) {
                final ValueLabel memoryRegionValueLabel = new MemoryRegionValueLabel(inspection()) {
                    @Override
                    public Value fetchValue() {
                        return _teleObject.readFieldValue(fieldActor);
                    }
                };
                valueLabels.append(memoryRegionValueLabel);
                fieldsPanel.add(memoryRegionValueLabel);
            }
        }
    }

    protected JPanel createFieldsPanel(AppendableSequence<ValueLabel> valueLabels) {
        final JPanel fieldsPanel = new JPanel(new SpringLayout());
        fieldsPanel.setBorder(BorderFactory.createMatteBorder(3, 0, 0, 0, style().defaultBorderColor()));
        fieldsPanel.setOpaque(true);
        fieldsPanel.setBackground(style().defaultBackgroundColor());
        displayFields(fieldsPanel, teleObject().classActorForType(), valueLabels);
        SpringUtilities.makeCompactGrid(fieldsPanel, fieldsPanel.getComponentCount() / numberOfTupleColumns(), numberOfTupleColumns(), 0, 0, 0, 0);
        return fieldsPanel;
    }

    protected JPanel createArrayPanel(AppendableSequence<ValueLabel> valueLabels, final Kind kind, int startOffset, int startIndex, int length, String indexPrefix,
                                      WordValueLabel.ValueMode wordValueMode) {
        final int size = kind.size();
        final JPanel panel = new JPanel(new SpringLayout());
        panel.setOpaque(true);
        panel.setBackground(style().defaultBackgroundColor());
        for (int i = 0; i < length; i++) {
            final int index = startIndex + i;
            if (showAddresses()) {
                panel.add(new LocationLabel.AsAddressWithOffset(inspection(), startOffset + (i * size), _currentObjectOrigin));
            }
            if (showOffsets()) {
                panel.add(new LocationLabel.AsOffset(inspection(), startOffset + (i * size), _currentObjectOrigin));
            }
            panel.add(new LocationLabel.AsIndex(inspection(), indexPrefix, i, startOffset + (i * size), _currentObjectOrigin));
            if (kind == Kind.REFERENCE) {
                valueLabels.append(new WordValueLabel(inspection(), WordValueLabel.ValueMode.REFERENCE) {
                    @Override
                    public Value fetchValue() {
                        return teleVM().getElementValue(kind, _teleObject.reference(), index);
                    }
                });
            } else if (kind == Kind.WORD) {
                valueLabels.append(new WordValueLabel(inspection(), wordValueMode) {
                    @Override
                    public Value fetchValue() {
                        return teleVM().getElementValue(kind, _teleObject.reference(), index);
                    }
                });
            } else {
                valueLabels.append(new PrimitiveValueLabel(inspection(), kind) {
                    @Override
                    public Value fetchValue() {
                        return teleVM().getElementValue(kind, _teleObject.reference(), index);
                    }
                });
            }
            panel.add(valueLabels.last());
            if (showMemoryRegion()) {
                final ValueLabel memoryRegionValueLabel = new MemoryRegionValueLabel(inspection()) {
                    @Override
                    public Value fetchValue() {
                        return teleVM().getElementValue(kind, _teleObject.reference(), index);
                    }
                };
                valueLabels.append(memoryRegionValueLabel);
                panel.add(memoryRegionValueLabel);
            }
        }
        SpringUtilities.makeCompactGrid(panel, numberOfArrayColumns());
        panel.setBorder(BorderFactory.createMatteBorder(3, 0, 0, 0, style().defaultBorderColor()));
        return panel;
    }

    public InspectorAction getMemoryInspectorAction() {
        return new InspectorAction(inspection(), "Inspect Memory") {
            @Override
            protected void procedure() {
                makeMemoryInspector();
            }
        };
    }

    public void makeMemoryInspector() {
        MemoryInspector.create(inspection(), _teleObject);
    }

    public InspectorAction getMemoryWordInspectorAction() {
        return new InspectorAction(inspection(), "Inspect Memory Words") {
            @Override
            protected void procedure() {
                makeMemoryWordInspector();
            }
        };
    }

    public void makeMemoryWordInspector() {
        MemoryWordInspector.create(inspection(), _teleObject);
    }

    @Override
    public void vmProcessTerminated() {
        dispose();
    }

}
