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
/*VCSID=dda86a71-4ae0-4b48-a96e-d1ce11cf1a13*/
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
import com.sun.max.program.option.*;
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

    /**
     * Manages inspection of objects in the {@link TeleVM} heap.
     * Has no visible presence or direct user interaction at this time.
     *
     * @author Michael Van De Vanter
     */
    public static final class Manager extends InspectionHolder {

        private Manager(Inspection inspection) {
            super(inspection);
        }

        public static void make(Inspection inspection) {
            if (_manager == null) {
                _manager = new Manager(inspection);
                inspection.focus().addListener(new InspectionFocusAdapter() {

                    @Override
                    public void heapObjectFocusChanged(TeleObject oldTeleObject, TeleObject teleObject) {
                        if (teleObject != null) {
                            ObjectInspector.make(_manager.inspection(), teleObject);
                        }
                    }
                });
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
        boolean _showFieldPosition;
        boolean _showFieldType;

        Preferences(Inspection inspection) {
            _inspection = inspection;
            final InspectionSettings settings = inspection.settings();
            final SaveSettingsListener saveSettingsListener = new AbstractSaveSettingsListener("objectInspectorPrefs", null) {
                public void saveSettings(SaveSettingsEvent saveSettingsEvent) {
                    saveSettingsEvent.save("showHeader", _showHeader);
                    saveSettingsEvent.save("showFieldPosition", _showFieldPosition);
                    saveSettingsEvent.save("showFieldType", _showFieldType);
                }
            };
            settings.addSaveSettingsListener(saveSettingsListener);

            _showHeader = settings.get(saveSettingsListener, "showHeader", OptionTypes.BOOLEAN_TYPE, true);
            _showHeader = settings.get(saveSettingsListener, "showFieldPosition", OptionTypes.BOOLEAN_TYPE, true);
            _showHeader = settings.get(saveSettingsListener, "showFieldType", OptionTypes.BOOLEAN_TYPE, true);
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

            final JCheckBox alwaysShowPosCheckBox = new JCheckBox("Position");
            alwaysShowPosCheckBox.setOpaque(true);
            alwaysShowPosCheckBox.setBackground(_inspection.style().defaultBackgroundColor());
            alwaysShowPosCheckBox.setToolTipText("Display positions in tuples?");
            alwaysShowPosCheckBox.setSelected(_showFieldPosition);

            final JCheckBox alwaysShowTypeCheckBox = new JCheckBox("Type");
            alwaysShowTypeCheckBox.setOpaque(true);
            alwaysShowTypeCheckBox.setBackground(_inspection.style().defaultBackgroundColor());
            alwaysShowTypeCheckBox.setToolTipText("Display types in tuples?");
            alwaysShowTypeCheckBox.setSelected(_showFieldType);

            final ItemListener itemListener = new ItemListener() {
                public void itemStateChanged(ItemEvent e) {
                    final Object source = e.getItemSelectable();
                    if (source == alwaysShowHeaderCheckBox) {
                        _showHeader = alwaysShowHeaderCheckBox.isSelected();
                    } else if (source == alwaysShowPosCheckBox) {
                        _showFieldPosition = alwaysShowPosCheckBox.isSelected();
                    } else if (source == alwaysShowTypeCheckBox) {
                        _showFieldType = alwaysShowTypeCheckBox.isSelected();
                    }
                    _inspection.settings().save();
                }
            };
            alwaysShowHeaderCheckBox.addItemListener(itemListener);
            alwaysShowPosCheckBox.addItemListener(itemListener);
            alwaysShowTypeCheckBox.addItemListener(itemListener);

            final JPanel panel = new JPanel(new BorderLayout());
            panel.setOpaque(true);
            panel.setBackground(_inspection.style().defaultBackgroundColor());

            final JPanel content = new JPanel();
            content.add(new TextLabel(_inspection, "Show:  "));
            content.add(alwaysShowHeaderCheckBox);
            content.add(alwaysShowPosCheckBox);
            content.add(alwaysShowTypeCheckBox);

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

    private static Preferences _globalPreferences;

    public static synchronized Preferences globalPreferences(Inspection inspection) {
        if (_globalPreferences == null) {
            _globalPreferences = new Preferences(inspection);
        }
        return _globalPreferences;
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
    private final JCheckBoxMenuItem _showPosMenuCheckBox;
    private final JCheckBoxMenuItem _showTypeMenuCheckBox;

    private ObjectHeaderInspector _objectHeaderInspector;

    protected ObjectInspector(final Inspection inspection, Residence residence, final TeleObject teleObject) {
        super(inspection, residence, teleObject.reference());
        final Preferences preferences = globalPreferences(inspection);
        _teleObject = teleObject;
        _currentObjectOrigin = teleObject().getCurrentOrigin();
        _showHeaderMenuCheckBox = new JCheckBoxMenuItem("Display Object Header", preferences._showHeader);
        _showHeaderMenuCheckBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                if (!_showHeaderMenuCheckBox.getState()) {
                    _objectHeaderInspector = null;
                }
                reconstructView();
            }
        });
        _showPosMenuCheckBox = new JCheckBoxMenuItem("Display tuple position", preferences._showFieldPosition);
        _showPosMenuCheckBox.addActionListener(new ActionListener() {
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
        _objectInspectors.add(this);
    }

    @Override
    public void createFrame(InspectorMenu menu) {
        super.createFrame(menu);
        setLocationRelativeToMouse(inspection().geometry().objectInspectorNewFrameDiagonalOffset());
        frame().menu().addSeparator();
        frame().menu().add(_showHeaderMenuCheckBox);
        frame().menu().add(_showPosMenuCheckBox);
        frame().menu().add(_showTypeMenuCheckBox);
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
            _objectHeaderInspector = new ObjectHeaderInspector(inspection(), teleObject(), this);
            panel.add(_objectHeaderInspector, BorderLayout.NORTH);
        }
        frame().setContentPane(panel);
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
        if (_teleObject == inspection().focus().heapObject()) {
            inspection().focus().setHeapObject(null);
        }
        super.inspectorClosing();
    }

    /**
     * @return whether to display the "Position" column for headers and tuples
     */
    public boolean showPos() {
        return _showPosMenuCheckBox.getState();
    }

    /**
     * @return whether to display the "Type" column for headers and tuples
     */
    public boolean showType() {
        return _showTypeMenuCheckBox.getState();
    }

    /**
     * @return how many columns are currently being displayed
     */
    public int numberOfTupleColumns() {
        int result = 2;
        if (showPos()) {
            result++;
        }
        if (showType()) {
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

    protected abstract Sequence<ValueLabel> valueLabels();

    @Override
    public synchronized void refreshView(long epoch) {
        final Pointer newOrigin = _teleObject.getCurrentOrigin();
        if (!newOrigin.equals(_currentObjectOrigin)) {
            // The object has been relocated in memory
            _currentObjectOrigin = newOrigin;
            reconstructView();
        } else {
            if (_objectHeaderInspector != null) {
                _objectHeaderInspector.refresh(epoch);
            }
            for (ValueLabel valueLabel : valueLabels()) {
                valueLabel.refresh(epoch);
            }
        }
        super.refreshView(epoch);
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
            if (showPos()) {
                fieldsPanel.add(new LocationLabel.AsOffset(inspection(), fieldActor.offset(), _currentObjectOrigin));                           // Field Offset
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
                valueLabel = new PrimitiveValueLabel(inspection(), fieldActor.kind()) {
                    @Override
                    public Value fetchValue() {
                        return _teleObject.readFieldValue(fieldActor);
                    }
                };
            }
            valueLabels.append(valueLabel);
            fieldsPanel.add(valueLabel);                                                                                                                                     // Field value
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
        final JPanel panel = new JPanel(new SpringLayout());
        panel.setOpaque(true);
        panel.setBackground(style().defaultBackgroundColor());
        for (int i = 0; i < length; i++) {
            final int index = startIndex + i;
            panel.add(new LocationLabel.AsIndex(inspection(), indexPrefix, i, startOffset + (i * kind.size()), _currentObjectOrigin));
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
        }
        SpringUtilities.makeCompactGrid(panel, 2);
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


}
