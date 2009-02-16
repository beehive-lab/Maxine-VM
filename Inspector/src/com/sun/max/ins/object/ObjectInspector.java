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

import com.sun.max.ins.*;
import com.sun.max.ins.InspectionSettings.*;
import com.sun.max.ins.gui.*;
import com.sun.max.ins.memory.*;
import com.sun.max.ins.memory.MemoryInspector.*;
import com.sun.max.ins.memory.MemoryWordInspector.*;
import com.sun.max.program.*;
import com.sun.max.program.option.*;
import com.sun.max.tele.*;
import com.sun.max.tele.debug.*;
import com.sun.max.tele.object.*;
import com.sun.max.tele.object.TeleObject.*;
import com.sun.max.unsafe.*;
import com.sun.max.util.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;

/**
 * An inspector that displays the content of a Maxine low level heap object in the {@link TeleVM}.
 *
 * @author Bernd Mathiske
 * @author Michael Van De Vanter
 */
public abstract class ObjectInspector extends Inspector implements MemoryInspectable, MemoryWordInspectable {

    private static ObjectInspectorPreferences _globalPreferences;

    public static synchronized ObjectInspectorPreferences globalPreferences(Inspection inspection) {
        if (_globalPreferences == null) {
            _globalPreferences = new ObjectInspectorPreferences(inspection);
        }
        return _globalPreferences;
    }

    // Preferences

    private static final String SHOW_HEADER_PREFERENCE = "showHeader";
    private static final String SHOW_ADDRESSES_PREFERENCE = "showAddresses";
    private static final String SHOW_OFFSETS_PREFERENCE = "showOffsets";
    private static final String SHOW_FIELD_TYPES_PREFERENCE = "showFieldTypes";
    private static final String SHOW_MEMORY_REGIONS_PREFERENCE = "showMemoryRegions";
    private static final String HIDE_NULL_ARRAY_ELEMENTS_PREFERENCE = "hideNullArrayElements";

    public static class ObjectInspectorPreferences {
        private final Inspection _inspection;
        boolean _showHeader;
        boolean _showAddresses;
        boolean _showOffsets;
        boolean _showFieldTypes;
        boolean _showMemoryRegions;
        boolean _hideNullArrayElements;

        ObjectInspectorPreferences(Inspection inspection) {
            _inspection = inspection;
            final InspectionSettings settings = inspection.settings();
            final SaveSettingsListener saveSettingsListener = new AbstractSaveSettingsListener("objectInspectorPrefs", null) {
                public void saveSettings(SaveSettingsEvent saveSettingsEvent) {
                    saveSettingsEvent.save(SHOW_HEADER_PREFERENCE, _showHeader);
                    saveSettingsEvent.save(SHOW_ADDRESSES_PREFERENCE, _showAddresses);
                    saveSettingsEvent.save(SHOW_OFFSETS_PREFERENCE, _showOffsets);
                    saveSettingsEvent.save(SHOW_FIELD_TYPES_PREFERENCE, _showFieldTypes);
                    saveSettingsEvent.save(SHOW_MEMORY_REGIONS_PREFERENCE,  _showMemoryRegions);
                    saveSettingsEvent.save(HIDE_NULL_ARRAY_ELEMENTS_PREFERENCE,  _hideNullArrayElements);
                }
            };
            settings.addSaveSettingsListener(saveSettingsListener);

            _showHeader = settings.get(saveSettingsListener, SHOW_HEADER_PREFERENCE, OptionTypes.BOOLEAN_TYPE, true);
            _showAddresses = settings.get(saveSettingsListener, SHOW_ADDRESSES_PREFERENCE, OptionTypes.BOOLEAN_TYPE, false);
            _showOffsets = settings.get(saveSettingsListener, SHOW_OFFSETS_PREFERENCE, OptionTypes.BOOLEAN_TYPE, true);
            _showFieldTypes = settings.get(saveSettingsListener, SHOW_FIELD_TYPES_PREFERENCE, OptionTypes.BOOLEAN_TYPE, true);
            _showMemoryRegions = settings.get(saveSettingsListener, SHOW_MEMORY_REGIONS_PREFERENCE, OptionTypes.BOOLEAN_TYPE, false);
            _hideNullArrayElements = settings.get(saveSettingsListener, HIDE_NULL_ARRAY_ELEMENTS_PREFERENCE, OptionTypes.BOOLEAN_TYPE, false);
        }

        /**
         * @return a GUI panel for setting preferences
         */
        public JPanel getPanel() {
            final JCheckBox alwaysShowHeaderCheckBox =
                new InspectorCheckBox(_inspection, "Header", "Should new Object Inspectors initially display the header?", _showHeader);
            final JCheckBox alwaysShowAddressesCheckBox =
                new InspectorCheckBox(_inspection, "Addresses", "Should new Object Inspectors initially display addresses?", _showAddresses);
            final JCheckBox alwaysShowOffsetsCheckBox =
                new InspectorCheckBox(_inspection, "Offsets", "Should new Object Inspectors initially display offsets?", _showOffsets);
            final JCheckBox alwaysShowTupleTypeCheckBox =
                new InspectorCheckBox(_inspection, "Type", "Should new Object Inspectors initially display types?", _showFieldTypes);
            final JCheckBox alwaysShowMemoryRegionCheckBox =
                new InspectorCheckBox(_inspection, "Region", "Should new Object Inspectors initially display memory regions?", _showMemoryRegions);
            final JCheckBox hideNullArrayElementsCheckBox =
                new InspectorCheckBox(_inspection, "Hide null array elements", "Should new Object Inspectors initially hide null elements in arrays?", _hideNullArrayElements);

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
                        _showFieldTypes = alwaysShowTupleTypeCheckBox.isSelected();
                    } else if (source == alwaysShowMemoryRegionCheckBox) {
                        _showMemoryRegions = alwaysShowMemoryRegionCheckBox.isSelected();
                    } else if (source == hideNullArrayElementsCheckBox) {
                        _hideNullArrayElements = hideNullArrayElementsCheckBox.isSelected();
                    }
                    _inspection.settings().save();
                }
            };
            alwaysShowHeaderCheckBox.addItemListener(itemListener);
            alwaysShowAddressesCheckBox.addItemListener(itemListener);
            alwaysShowOffsetsCheckBox.addItemListener(itemListener);
            alwaysShowTupleTypeCheckBox.addItemListener(itemListener);
            alwaysShowMemoryRegionCheckBox.addItemListener(itemListener);
            hideNullArrayElementsCheckBox.addItemListener(itemListener);

            final JPanel upperContentPanel = new InspectorPanel(_inspection);
            upperContentPanel.add(new TextLabel(_inspection, "Columns:  "));
            upperContentPanel.add(alwaysShowHeaderCheckBox);
            upperContentPanel.add(alwaysShowAddressesCheckBox);
            upperContentPanel.add(alwaysShowOffsetsCheckBox);
            upperContentPanel.add(alwaysShowTupleTypeCheckBox);
            upperContentPanel.add(alwaysShowMemoryRegionCheckBox);

            final JPanel upperPanel = new InspectorPanel(_inspection, new BorderLayout());
            upperPanel.add(upperContentPanel, BorderLayout.WEST);

            final JPanel lowerContentPanel = new InspectorPanel(_inspection);
            lowerContentPanel.add(new TextLabel(_inspection, "Options:  "));
            lowerContentPanel.add(hideNullArrayElementsCheckBox);

            final JPanel lowerPanel = new InspectorPanel(_inspection, new BorderLayout());
            lowerPanel.add(lowerContentPanel, BorderLayout.WEST);

            final JPanel panel = new InspectorPanel(_inspection, new BorderLayout());
            panel.add(upperPanel, BorderLayout.NORTH);
            panel.add(lowerPanel, BorderLayout.SOUTH);

            return panel;
        }

        void showDialog() {
            new ObjectInspectorPreferencesDialog(_inspection);
        }

        private final class ObjectInspectorPreferencesDialog extends InspectorDialog {

            ObjectInspectorPreferencesDialog(Inspection inspection) {
                super(inspection, "Object Inspector Preferences", false);

                final JPanel dialogPanel = new InspectorPanel(inspection, new BorderLayout());

                final JPanel buttonPanel = new InspectorPanel(inspection);
                buttonPanel.add(new JButton(new InspectorAction(inspection(), "Close") {
                    @Override
                    protected void procedure() {
                        dispose();
                    }
                }));

                dialogPanel.add(getPanel(), BorderLayout.NORTH);
                dialogPanel.add(buttonPanel, BorderLayout.SOUTH);

                setContentPane(dialogPanel);
                pack();
                inspection().moveToMiddle(this);
                setVisible(true);
            }
        }

    }

    private final ObjectInspectorFactory _factory;

    private final TeleObject _teleObject;

    /**
     * @return local surrogate for the object being inspected in the {@link TeleVM}
     */
    TeleObject teleObject() {
        return _teleObject;
    }

    /** The origin is an actual location in memory of the {@link TeleVM};
     * keep a copy for comparison, since it might change via GC.
     */
    private Pointer _currentObjectOrigin;

    /**
     * @return The actual location in {@link TeleVM} memory where
     * the object resides at present; this may change via GC.
     */
    Pointer currentOrigin() {
        return _currentObjectOrigin;
    }

    private final JCheckBoxMenuItem _showHeaderMenuCheckBox;
    private final JCheckBoxMenuItem _showAddressesMenuCheckBox;
    private final JCheckBoxMenuItem _showOffsetsMenuCheckBox;
    private final JCheckBoxMenuItem _showTypesMenuCheckBox;
    private final JCheckBoxMenuItem _showMemoryRegionsMenuCheckBox;
    private final JCheckBoxMenuItem _hideNullArrayElementsMenuCheckBox;

    private InspectorTable _objectHeaderTable;

    protected ObjectInspector(final Inspection inspection, ObjectInspectorFactory factory, Residence residence, final TeleObject teleObject) {
        super(inspection, residence);
        _factory = factory;
        final ObjectInspectorPreferences preferences = globalPreferences(inspection);
        _teleObject = teleObject;
        _currentObjectOrigin = teleObject().getCurrentOrigin();
        Trace.line(1, tracePrefix() + " creating for " + getTextForTitle());
        _showHeaderMenuCheckBox = new JCheckBoxMenuItem("Display Object Header", preferences._showHeader);
        _showHeaderMenuCheckBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                if (!_showHeaderMenuCheckBox.getState()) {
                    _objectHeaderTable = null;
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
        _showTypesMenuCheckBox = new JCheckBoxMenuItem("Display tuple types", preferences._showFieldTypes);
        _showTypesMenuCheckBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                reconstructView();
            }
        });
        _showMemoryRegionsMenuCheckBox = new JCheckBoxMenuItem("Display memory regions", preferences._showMemoryRegions);
        _showMemoryRegionsMenuCheckBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                reconstructView();
            }
        });
        _hideNullArrayElementsMenuCheckBox = new JCheckBoxMenuItem("Hide null array elements", preferences._hideNullArrayElements);
        _hideNullArrayElementsMenuCheckBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                reconstructView();
            }
        });
    }

    @Override
    public void createFrame(InspectorMenu menu) {
        super.createFrame(menu);
        setLocationRelativeToMouse(inspection().geometry().objectInspectorNewFrameDiagonalOffset());
        frame().menu().addSeparator();
        frame().menu().add(_showHeaderMenuCheckBox);
        frame().menu().add(_showAddressesMenuCheckBox);
        frame().menu().add(_showOffsetsMenuCheckBox);
        frame().menu().add(_showTypesMenuCheckBox);
        frame().menu().add(_showMemoryRegionsMenuCheckBox);
        if (!(_teleObject.getObjectKind() == ObjectKind.TUPLE)) {
            // both arrays and hybrids have array elements
            frame().menu().add(_hideNullArrayElementsMenuCheckBox);
        }
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
        final JPanel panel = new InspectorPanel(inspection(), new BorderLayout());
        if (_showHeaderMenuCheckBox.getState()) {
            _objectHeaderTable = new ObjectHeaderTable(this);
            _objectHeaderTable.setBorder(style().defaultPaneBottomBorder());
            // Will add without column headers
            panel.add(_objectHeaderTable, BorderLayout.NORTH);
        }
        frame().setContentPane(panel);
    }

    @Override
    public final String getTextForTitle() {
        return _teleObject.getCurrentOrigin().toHexString() + inspection().nameDisplay().referenceLabelText(_teleObject);
    }

    @Override
    public void threadFocusSet(TeleNativeThread oldTeleNativeThread, TeleNativeThread teleNativeThread) {
        // Object inspector displays are sensitive to the current thread selection.
        refreshView(teleVM().epoch(), true);
    }

    @Override
    public void addressFocusChanged(Address oldAddress, Address newAddress) {
        refreshView(teleVM().epoch(), true);
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
        _factory.objectInspectorClosing(this);
        super.inspectorClosing();
    }

     /**
     * @return whether to display the "Address" column for headers, tuples, and arrays
     */
    boolean showAddresses() {
        return _showAddressesMenuCheckBox.getState();
    }

    /**
     * @return whether to display the "Offset" column for headers, tuples and arrays
     */
    boolean showOffsets() {
        return _showOffsetsMenuCheckBox.getState();
    }

    /**
     * @return whether to display the "Type" column for headers and tuples
     */
    boolean showTypes() {
        return _showTypesMenuCheckBox.getState();
    }

    boolean hideNullArrayElements() {
        return _hideNullArrayElementsMenuCheckBox.getState();
    }

    /**
     * @return whether to display the "Region" column for headers and tuples
     */
    boolean showMemoryRegions() {
        return _showMemoryRegionsMenuCheckBox.getState();
    }


    /**
     * @return how many columns are currently being displayed
     */
    int numberOfTupleColumns() {
        int result = 2; // always show field name and value
        if (showAddresses()) {
            result++;
        }
        if (showOffsets()) {
            result++;
        }
        if (showTypes()) {
            result++;
        }
        if (showMemoryRegions()) {
            result++;
        }
        return result;
    }

    /**
     * @return how many columns are currently being displayed
     */
    int numberOfArrayColumns() {
        int result = 2;
        if (showAddresses()) {
            result++;
        }
        if (showOffsets()) {
            result++;
        }
        if (showMemoryRegions()) {
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

    @Override
    public synchronized void refreshView(long epoch, boolean force) {
        final Pointer newOrigin = _teleObject.getCurrentOrigin();
        if (!newOrigin.equals(_currentObjectOrigin)) {
            // The object has been relocated in memory
            _currentObjectOrigin = newOrigin;
            reconstructView();
        } else {
            if (_objectHeaderTable != null) {
                _objectHeaderTable.refresh(epoch, force);
            }
        }
        super.refreshView(epoch, force);
    }

    public void viewConfigurationChanged(long epoch) {
        reconstructView();
    }

    /**
     * Gets the fields for either a tuple or hybrid object, static fields in the special case of a {@link StaticTuple} object.
     *
     * @return a {@FieldActor} for every field in the object, sorted by offset.
     */
    protected Collection<FieldActor> getFieldActors() {
        final TreeSet<FieldActor> fieldActors = new TreeSet<FieldActor>(new Comparator<FieldActor>() {
            public int compare(FieldActor a, FieldActor b) {
                final Integer aOffset = a.offset();
                return aOffset.compareTo(b.offset());
            }
        });
        collectFieldActors(teleObject().classActorForType(), _teleObject instanceof TeleStaticTuple, fieldActors);
        return fieldActors;
    }

    private void collectFieldActors(ClassActor classActor, boolean isStatic, TreeSet<FieldActor> fieldActors) {
        if (classActor != null) {
            final FieldActor[] localFieldActors = isStatic ? classActor.localStaticFieldActors() : classActor.localInstanceFieldActors();
            for (FieldActor fieldActor : localFieldActors) {
                fieldActors.add(fieldActor);
            }
            collectFieldActors(classActor.superClassActor(), isStatic, fieldActors);
        }
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
