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
import com.sun.max.program.*;
import com.sun.max.tele.*;
import com.sun.max.tele.object.*;
import com.sun.max.unsafe.*;
import com.sun.max.util.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;

/**
 * An inspector that displays the content of a Maxine low level heap object in the VM.
 *
 * @author Bernd Mathiske
 * @author Michael Van De Vanter
 */
public abstract class ObjectInspector extends Inspector {

    private final ObjectInspectorFactory factory;

    private final TeleObject teleObject;

    /**
     * @return local surrogate for the object being inspected in the VM
     */
    TeleObject teleObject() {
        return teleObject;
    }

    /** The origin is an actual location in memory of the VM;
     * keep a copy for comparison, since it might change via GC.
     */
    private Pointer currentObjectOrigin;

    /**
     * @return The actual location in VM memory where
     * the object resides at present; this may change via GC.
     */
    Pointer currentOrigin() {
        return currentObjectOrigin;
    }

    /**
     * Cache of the most recent update to the frame title; needed
     * in situations where the frame becomes unavailable.
     */
    private String title = null;

    private boolean showHeader;
    private boolean showAddresses;
    private boolean showOffsets;
    private boolean showFieldTypes;
    private boolean showMemoryRegions;
    private boolean hideNullArrayElements;

    private InspectorTable objectHeaderTable;

    protected ObjectInspector(final Inspection inspection, ObjectInspectorFactory factory, final TeleObject teleObject) {
        super(inspection);
        this.factory = factory;
        final ObjectInspectorPreferences globalPreferences = ObjectInspectorPreferences.globalPreferences(inspection);
        this.teleObject = teleObject;
        this.currentObjectOrigin = teleObject().getCurrentOrigin();
        Trace.line(1, tracePrefix() + " creating for " + getTextForTitle());

        showHeader = globalPreferences.showHeader();
        showAddresses = globalPreferences.showAddresses();
        showOffsets = globalPreferences.showOffsets();
        showFieldTypes = globalPreferences.showFieldTypes();
        showMemoryRegions = globalPreferences.showMemoryRegions();
        hideNullArrayElements = globalPreferences.hideNullArrayElements();
    }

    @Override
    public void createFrame(InspectorMenu menu) {
        super.createFrame(menu);
        gui().setLocationRelativeToMouse(this, inspection().geometry().objectInspectorNewFrameDiagonalOffset());
        final InspectorMenu frameMenu = frame().menu();
        frameMenu.addSeparator();
        frameMenu.add(actions().inspectObjectMemoryWords(teleObject, "Inspect object's memory"));
        frameMenu.add(actions().setObjectWatchpoint(teleObject, "Watch object's memory"));
        frameMenu.addSeparator();
        frameMenu.add(actions().closeViews(otherObjectInspectorsPredicate, "Close other object inspectors"));
        frameMenu.add(actions().closeViews(allObjectInspectorsPredicate, "Close all object inspectors"));
    }

    @Override
    protected void createView() {
        final JPanel panel = new InspectorPanel(inspection(), new BorderLayout());
        if (showHeader) {
            objectHeaderTable = new ObjectHeaderTable(this);
            objectHeaderTable.setBorder(style().defaultPaneBottomBorder());
            // Will add without column headers
            panel.add(objectHeaderTable, BorderLayout.NORTH);
        }
        frame().setContentPane(panel);
    }

    @Override
    public final String getTextForTitle() {
        if (teleObject.isLive()) {
            Pointer pointer = teleObject.getCurrentOrigin();
            title = "Object: " + pointer.toHexString() + inspection().nameDisplay().referenceLabelText(teleObject);
            return title;
        }
        // Use the last good title
        return title + " - collected by GC";
    }

    @Override
    public InspectorAction getViewOptionsAction() {
        return new InspectorAction(inspection(), "View Options") {
            @Override
            public void procedure() {
                showViewOptionsDialog(inspection());
            }
        };
    }

    @Override
    public void threadFocusSet(MaxThread oldThread, MaxThread thread) {
        // Object inspector displays are sensitive to the current thread selection.
        refreshView(true);
    }

    @Override
    public void addressFocusChanged(Address oldAddress, Address newAddress) {
        refreshView(true);
    }

    @Override
    public void inspectorGetsWindowFocus() {
        if (teleObject != inspection().focus().heapObject()) {
            inspection().focus().setHeapObject(teleObject);
        }
        super.inspectorGetsWindowFocus();
    }

    @Override
    public void inspectorLosesWindowFocus() {
        if (teleObject == inspection().focus().heapObject()) {
            inspection().focus().setHeapObject(null);
        }
        super.inspectorLosesWindowFocus();
    }

    @Override
    public void inspectorClosing() {
        // don't try to recompute the title, just get the one that's been in use
        Trace.line(1, tracePrefix() + " closing for " + getCurrentTitle());
        if (teleObject == inspection().focus().heapObject()) {
            inspection().focus().setHeapObject(null);
        }
        factory.objectInspectorClosing(this);
        super.inspectorClosing();
    }

    @Override
    public void watchpointSetChanged() {
        refreshView(false);
    }

    @Override
    public void vmProcessTerminated() {
        dispose();
    }

    /**
     * @return whether to display the "Address" column for headers, tuples, and arrays
     */
    boolean showAddresses() {
        return showAddresses;
    }

    /**
     * @return whether to display the "Offset" column for headers, tuples and arrays
     */
    boolean showOffsets() {
        return showOffsets;
    }

    /**
     * @return whether to display the "Type" column for headers and tuples
     */
    boolean showFieldTypes() {
        return showFieldTypes;
    }

    boolean hideNullArrayElements() {
        return hideNullArrayElements;
    }

    /**
     * @return whether to display the "Region" column for headers and tuples
     */
    boolean showMemoryRegions() {
        return showMemoryRegions;
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

    private static final Predicate<Inspector> allObjectInspectorsPredicate = new Predicate<Inspector>() {
        public boolean evaluate(Inspector inspector) {
            return inspector instanceof ObjectInspector;
        }
    };

    private final Predicate<Inspector> otherObjectInspectorsPredicate = new Predicate<Inspector>() {
        public boolean evaluate(Inspector inspector) {
            return inspector instanceof ObjectInspector && inspector != ObjectInspector.this;
        }
    };

    @Override
    protected boolean refreshView(boolean force) {
        final Pointer newOrigin = teleObject.getCurrentOrigin();
        if (!teleObject.isLive()) {
            setWarning();
            updateFrameTitle();
            return false;
        }
        if (!newOrigin.equals(currentObjectOrigin)) {
            // The object has been relocated in memory
            currentObjectOrigin = newOrigin;
            reconstructView();
        } else {
            if (objectHeaderTable != null) {
                objectHeaderTable.refresh(force);
            }
        }
        updateFrameTitle();
        super.refreshView(force);

        return true;
    }

    public void viewConfigurationChanged() {
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
        collectFieldActors(teleObject().classActorForType(), teleObject instanceof TeleStaticTuple, fieldActors);
        return fieldActors;
    }

    private void collectFieldActors(ClassActor classActor, boolean isStatic, TreeSet<FieldActor> fieldActors) {
        if (classActor != null) {
            final FieldActor[] localFieldActors = isStatic ? classActor.localStaticFieldActors() : classActor.localInstanceFieldActors();
            for (FieldActor fieldActor : localFieldActors) {
                fieldActors.add(fieldActor);
            }
            collectFieldActors(classActor.superClassActor, isStatic, fieldActors);
        }
    }

    /**
     * A Panel that displays controls for setting options for object inspection, both for the instance and the global, persistent preferences.
     */
    private final class ViewOptionsPanel extends InspectorPanel {

        public ViewOptionsPanel(Inspection inspection) {
            super(inspection, new BorderLayout());

            final InspectorCheckBox showAddressesCheckBox = new InspectorCheckBox(inspection(), "Addresses", "Display addresses", showAddresses);
            showAddressesCheckBox.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent actionEvent) {
                    showAddresses = showAddressesCheckBox.isSelected();
                    reconstructView();
                }
            });
            final InspectorCheckBox showOffsetsCheckBox = new InspectorCheckBox(inspection(), "Offsets", "Display offsets", showOffsets);
            showOffsetsCheckBox.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent actionEvent) {
                    showOffsets = showOffsetsCheckBox.isSelected();
                    reconstructView();
                }
            });
            final InspectorCheckBox showTypesCheckBox = new InspectorCheckBox(inspection(), "Type", "Display tuple types", showFieldTypes);
            showTypesCheckBox.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent actionEvent) {
                    showFieldTypes = showTypesCheckBox.isSelected();
                    reconstructView();
                }
            });
            final InspectorCheckBox showMemoryRegionsCheckBox = new InspectorCheckBox(inspection(), "Region", "Display memory regions", showMemoryRegions);
            showMemoryRegionsCheckBox.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent actionEvent) {
                    showMemoryRegions = showMemoryRegionsCheckBox.isSelected();
                    reconstructView();
                }
            });
            final InspectorCheckBox showHeaderCheckBox = new InspectorCheckBox(inspection(), "Show Header", "Display Object Header", showHeader);
            showHeaderCheckBox.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent actionEvent) {
                    showHeader = showHeaderCheckBox.isSelected();
                    if (!showHeaderCheckBox.isSelected()) {
                        objectHeaderTable = null;
                    }
                    reconstructView();
                }
            });
            final InspectorCheckBox hideNullArrayElementsCheckBox = new InspectorCheckBox(inspection(), "Hide null array elements", "Hide null array elements", hideNullArrayElements);
            hideNullArrayElementsCheckBox.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent actionEvent) {
                    hideNullArrayElements = hideNullArrayElementsCheckBox.isSelected();
                    reconstructView();
                }
            });

            final JPanel upperContentPanel = new InspectorPanel(inspection());
            upperContentPanel.add(new TextLabel(inspection(), "View Columns:  "));
            upperContentPanel.add(showAddressesCheckBox);
            upperContentPanel.add(showOffsetsCheckBox);
            upperContentPanel.add(showTypesCheckBox);
            upperContentPanel.add(showMemoryRegionsCheckBox);

            final JPanel upperPanel = new InspectorPanel(inspection(), new BorderLayout());
            upperPanel.add(upperContentPanel, BorderLayout.WEST);

            final JPanel lowerContentPanel = new InspectorPanel(inspection());
            lowerContentPanel.add(new TextLabel(inspection(), "View Options:  "));
            lowerContentPanel.add(showHeaderCheckBox);
            lowerContentPanel.add(hideNullArrayElementsCheckBox);

            final JPanel lowerPanel = new InspectorPanel(inspection(), new BorderLayout());
            lowerPanel.add(lowerContentPanel, BorderLayout.WEST);

            add(upperPanel, BorderLayout.NORTH);
            add(lowerPanel, BorderLayout.SOUTH);
        }
    }

    private void showViewOptionsDialog(Inspection inspection) {

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

        final JPanel prefsOptionsPanel = ObjectInspectorPreferences.globalPreferencesPanel(inspection);
        prefsOptionsPanel.setBorder(border);
        prefPanel.add(prefsOptionsPanel);

        SpringUtilities.makeCompactGrid(prefPanel, 2);

        new SimpleDialog(inspection, prefPanel, "Object Inspector Preferences", true);
    }

}
