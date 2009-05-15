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
import javax.swing.border.*;

import com.sun.max.gui.*;
import com.sun.max.ins.*;
import com.sun.max.ins.gui.*;
import com.sun.max.ins.memory.*;
import com.sun.max.program.*;
import com.sun.max.tele.*;
import com.sun.max.tele.debug.*;
import com.sun.max.tele.object.*;
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
public abstract class ObjectInspector extends Inspector {

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

    private boolean _showHeader;
    private boolean _showAddresses;
    private boolean _showOffsets;
    private boolean _showFieldTypes;
    private boolean _showMemoryRegions;
    private boolean _hideNullArrayElements;

    private InspectorTable _objectHeaderTable;

    protected ObjectInspector(final Inspection inspection, ObjectInspectorFactory factory, final TeleObject teleObject) {
        super(inspection);
        _factory = factory;
        final ObjectInspectorPreferences preferences = ObjectInspectorPreferences.globalPreferences(inspection);
        _teleObject = teleObject;
        _currentObjectOrigin = teleObject().getCurrentOrigin();
        Trace.line(1, tracePrefix() + " creating for " + getTextForTitle());

        _showHeader = preferences.showHeader();
        _showAddresses = preferences.showAddresses();
        _showOffsets = preferences.showOffsets();
        _showFieldTypes = preferences.showFieldTypes();
        _showMemoryRegions = preferences.showMemoryRegions();
        _hideNullArrayElements = preferences.hideNullArrayElements();
    }

    @Override
    public void createFrame(InspectorMenu menu) {
        super.createFrame(menu);
        setLocationRelativeToMouse(inspection().geometry().objectInspectorNewFrameDiagonalOffset());
        frame().menu().addSeparator();
        frame().menu().add(new InspectorAction(inspection(), "Inspect Memory") {
            @Override
            protected void procedure() {
                MemoryInspector.create(inspection(), _teleObject).highlight();
            }
        });
        frame().menu().add(new InspectorAction(inspection(), "Inspect Memory Words") {
            @Override
            protected void procedure() {
                MemoryWordInspector.create(inspection(), _teleObject).highlight();
            }
        });
        frame().menu().addSeparator();
        frame().menu().add(inspection().getDeleteInspectorsAction(_otherObjectInspectorsPredicate, "Close other object inspectors"));
        frame().menu().add(inspection().getDeleteInspectorsAction(_allObjectInspectorsPredicate, "Close all object inspectors"));
    }

    @Override
    protected synchronized void createView(long epoch) {
        final JPanel panel = new InspectorPanel(inspection(), new BorderLayout());
        if (_showHeader) {
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
    public InspectorAction getViewOptionsAction() {
        return new InspectorAction(inspection(), "View Options") {
            @Override
            public void procedure() {
                new ObjectInspectorPreferencesDialog(inspection());
            }
        };
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
        return _showAddresses;
    }

    /**
     * @return whether to display the "Offset" column for headers, tuples and arrays
     */
    boolean showOffsets() {
        return _showOffsets;
    }

    /**
     * @return whether to display the "Type" column for headers and tuples
     */
    boolean showFieldTypes() {
        return _showFieldTypes;
    }

    boolean hideNullArrayElements() {
        return _hideNullArrayElements;
    }

    /**
     * @return whether to display the "Region" column for headers and tuples
     */
    boolean showMemoryRegions() {
        return _showMemoryRegions;
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
        if (showFieldTypes()) {
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
    protected synchronized void refreshView(long epoch, boolean force) {
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

    /**
     * A Panel that displays controls for setting options for object inspection, both for the instance and the global, persistent preferences.
     */
    private final class ViewOptionsPanel extends InspectorPanel {

        public ViewOptionsPanel(Inspection inspection) {
            super(inspection, new BorderLayout());

            final InspectorCheckBox showHeaderCheckBox = new InspectorCheckBox(inspection(), "Header", "Display Object Header", _showHeader);
            showHeaderCheckBox.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent actionEvent) {
                    _showHeader = showHeaderCheckBox.isSelected();
                    if (!showHeaderCheckBox.isSelected()) {
                        _objectHeaderTable = null;
                    }
                    reconstructView();
                }
            });
            final InspectorCheckBox showAddressesCheckBox = new InspectorCheckBox(inspection(), "Addresses", "Display addresses", _showAddresses);
            showAddressesCheckBox.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent actionEvent) {
                    _showAddresses = showAddressesCheckBox.isSelected();
                    reconstructView();
                }
            });
            final InspectorCheckBox showOffsetsCheckBox = new InspectorCheckBox(inspection(), "Offsets", "Display offsets", _showOffsets);
            showOffsetsCheckBox.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent actionEvent) {
                    _showOffsets = showOffsetsCheckBox.isSelected();
                    reconstructView();
                }
            });
            final InspectorCheckBox showTypesCheckBox = new InspectorCheckBox(inspection(), "Type", "Display tuple types", _showFieldTypes);
            showTypesCheckBox.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent actionEvent) {
                    _showFieldTypes = showTypesCheckBox.isSelected();
                    reconstructView();
                }
            });
            final InspectorCheckBox showMemoryRegionsCheckBox = new InspectorCheckBox(inspection(), "Region", "Display memory regions", _showMemoryRegions);
            showMemoryRegionsCheckBox.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent actionEvent) {
                    _showMemoryRegions = showMemoryRegionsCheckBox.isSelected();
                    reconstructView();
                }
            });
            final InspectorCheckBox hideNullArrayElementsCheckBox = new InspectorCheckBox(inspection(), "Hide null array elements", "Hide null array elements", _hideNullArrayElements);
            hideNullArrayElementsCheckBox.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent actionEvent) {
                    _hideNullArrayElements = hideNullArrayElementsCheckBox.isSelected();
                    reconstructView();
                }
            });

            final JPanel upperContentPanel = new InspectorPanel(inspection());
            upperContentPanel.add(new TextLabel(inspection(), "View:  "));
            upperContentPanel.add(showHeaderCheckBox);
            upperContentPanel.add(showAddressesCheckBox);
            upperContentPanel.add(showOffsetsCheckBox);
            upperContentPanel.add(showTypesCheckBox);
            upperContentPanel.add(showMemoryRegionsCheckBox);

            final JPanel upperPanel = new InspectorPanel(inspection(), new BorderLayout());
            upperPanel.add(upperContentPanel, BorderLayout.WEST);

            final JPanel lowerContentPanel = new InspectorPanel(inspection());
            lowerContentPanel.add(new TextLabel(inspection(), "Options:  "));
            lowerContentPanel.add(hideNullArrayElementsCheckBox);

            final JPanel lowerPanel = new InspectorPanel(inspection(), new BorderLayout());
            lowerPanel.add(lowerContentPanel, BorderLayout.WEST);

            add(upperPanel, BorderLayout.NORTH);
            add(lowerPanel, BorderLayout.SOUTH);
        }
    }

    /**
     * A model dialog that allows object inspection options to be set:  both for the instance being inspected and global, persistent preferences.
     */
    private final class ObjectInspectorPreferencesDialog extends InspectorDialog {

        ObjectInspectorPreferencesDialog(Inspection inspection) {
            super(inspection, "Object Inspector Preferences", false);

            final JPanel dialogPanel = new InspectorPanel(inspection, new BorderLayout());

            final JPanel prefPanel = new InspectorPanel(inspection, new SpringLayout());

            final Border border = BorderFactory.createLineBorder(Color.black);

            final JPanel thisLabelPanel = new InspectorPanel(inspection, new BorderLayout());
            thisLabelPanel.setBorder(border);
            thisLabelPanel.add(new TextLabel(inspection, "This Object"), BorderLayout.WEST);
            prefPanel.add(thisLabelPanel);

            final JPanel thisOptionsPanel = new ViewOptionsPanel(inspection);
            thisOptionsPanel.setBorder(border);
            prefPanel.add(thisOptionsPanel);

            final JPanel prefslLabelPanel = new InspectorPanel(inspection, new BorderLayout());
            prefslLabelPanel.setBorder(border);
            prefslLabelPanel.add(new TextLabel(inspection, "Preferences"), BorderLayout.WEST);
            prefPanel.add(prefslLabelPanel);

            final JPanel prefsOptionsPanel = ObjectInspectorPreferences.globalPreferences(inspection).getPanel();
            prefsOptionsPanel.setBorder(border);
            prefPanel.add(prefsOptionsPanel);

            SpringUtilities.makeCompactGrid(prefPanel, 2);

            final JPanel buttonPanel = new InspectorPanel(inspection);
            buttonPanel.add(new JButton(new InspectorAction(inspection(), "Close") {
                @Override
                protected void procedure() {
                    dispose();
                }
            }));

            dialogPanel.add(prefPanel, BorderLayout.CENTER);
            dialogPanel.add(buttonPanel, BorderLayout.SOUTH);
            setContentPane(dialogPanel);
            pack();
            inspection().moveToMiddle(this);
            setVisible(true);
        }
    }

    @Override
    public void vmProcessTerminated() {
        dispose();
    }

}
