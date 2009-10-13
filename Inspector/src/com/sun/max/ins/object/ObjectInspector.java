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
import java.util.*;

import javax.swing.*;

import com.sun.max.ins.*;
import com.sun.max.ins.gui.*;
import com.sun.max.ins.gui.TableColumnVisibilityPreferences.*;
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

    private InspectorTable objectHeaderTable;

    protected final ObjectViewPreferences instanceViewPreferences;

    protected ObjectInspector(final Inspection inspection, ObjectInspectorFactory factory, final TeleObject teleObject) {
        super(inspection);
        this.factory = factory;
        this.teleObject = teleObject;
        this.currentObjectOrigin = teleObject().getCurrentOrigin();
        instanceViewPreferences = new ObjectViewPreferences(ObjectViewPreferences.globalPreferences(inspection)) {
            @Override
            protected void setShowHeader(boolean showHeader) {
                reconstructView();
            }
            @Override
            protected void setHideNullArrayElements(boolean hideNullArrayElements) {
                reconstructView();
            }
        };
        instanceViewPreferences.addListener(new TableColumnViewPreferenceListener() {
            public void tableColumnViewPreferencesChanged() {
                reconstructView();
            }
        });
        Trace.line(1, tracePrefix() + " creating for " + getTextForTitle());
    }

    @Override
    public InspectorFrame createFrame() {
        final InspectorFrame frame = super.createFrame();
        gui().setLocationRelativeToMouse(this, inspection().geometry().objectInspectorNewFrameDiagonalOffset());

        final InspectorMenu defaultMenu = frame.makeMenu(MenuKind.DEFAULT_MENU);
        defaultMenu.add(defaultMenuItems(MenuKind.DEFAULT_MENU));
        defaultMenu.addSeparator();
        defaultMenu.add(actions().closeViews(otherObjectInspectorsPredicate, "Close other object inspectors"));
        defaultMenu.add(actions().closeViews(allObjectInspectorsPredicate, "Close all object inspectors"));

        final InspectorMenu memoryMenu = frame.makeMenu(MenuKind.MEMORY_MENU);
        memoryMenu.add(actions().inspectObjectMemoryWords(teleObject, "Inspect this object's memory"));
        if (maxVM().watchpointsEnabled()) {
            memoryMenu.add(actions().setObjectWatchpoint(teleObject, "Watch this object's memory"));
        }
        memoryMenu.add(defaultMenuItems(MenuKind.MEMORY_MENU));
        final JMenuItem viewMemoryRegionsMenuItem = new JMenuItem(actions().viewMemoryRegions());
        viewMemoryRegionsMenuItem.setText("View Memory Regions");
        memoryMenu.add(viewMemoryRegionsMenuItem);

        if (teleObject.getTeleClassMethodActorForObject() != null) {
            frame.makeMenu(MenuKind.OBJECT_MENU);
            frame.makeMenu(MenuKind.CODE_MENU);
        }

        if (teleObject.getTeleClassMethodActorForObject() != null || TeleTargetMethod.class.isAssignableFrom(teleObject.getClass())) {
            frame.makeMenu(MenuKind.DEBUG_MENU);
        }

        frame.makeMenu(MenuKind.VIEW_MENU).add(defaultMenuItems(MenuKind.VIEW_MENU));
        return frame;
    }

    @Override
    protected void createView() {
        final JPanel panel = new InspectorPanel(inspection(), new BorderLayout());
        if (instanceViewPreferences.showHeader()) {
            objectHeaderTable = new ObjectHeaderTable(inspection(), teleObject, instanceViewPreferences);
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
                final ObjectViewPreferences globalPreferences = ObjectViewPreferences.globalPreferences(inspection());
                new TableColumnVisibilityPreferences.ColumnPreferencesDialog<ObjectColumnKind>(inspection(), "View Options", instanceViewPreferences, globalPreferences);
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
}
