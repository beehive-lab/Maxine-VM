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
package com.sun.max.ins.object;

import java.lang.reflect.*;
import java.util.*;

import com.sun.max.ins.*;
import com.sun.max.ins.gui.*;
import com.sun.max.ins.util.*;
import com.sun.max.ins.view.*;
import com.sun.max.ins.view.InspectionViews.ViewKind;
import com.sun.max.lang.*;
import com.sun.max.program.*;
import com.sun.max.tele.*;
import com.sun.max.tele.object.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.reference.*;

/**
 * Creates and manages canonical instances of {@link ObjectView} for
 * objects in the heap of the VM.
 * <p>
 * This view manager does not have a public face for creating object views.  Rather,
 * the manager listens for the user to set focus on a particular object, and which point
 * an {@link ObjectView} is created (or merely highlighted if it already exists).
 *
 * @author Michael Van De Vanter
 */
public final class ObjectViewManager extends AbstractMultiViewManager<ObjectView> implements ObjectViewFactory {

    private static final ViewKind VIEW_KIND = ViewKind.OBJECT;
    private static final String SHORT_NAME = "Object";
    private static final String LONG_NAME = "Object Inspector";

    /**
     * Map:   {@link TeleObject} -- > the {@link ObjectView}, if it exists, for the corresponding
     * object in the VM.  Relies on {@link ObjectView}s being canonical.
     */
    private final Map<TeleObject, ObjectView> teleObjectToInspector = new HashMap<TeleObject, ObjectView>();

    /**
     * ObjectInspector constructors for specific tuple-implemented subclasses of {@link TeleObject}s.
     * The most specific class that matches a particular {@link TeleObject} will
     * be used, in an emulation of virtual method dispatch.
     */
    private final Map<Class, Constructor> teleTupleObjectClassToObjectInspectorConstructor = new HashMap<Class, Constructor>();

    /**
     * ObjectInspector constructors for specific array-implemented subclasses of {@link TeleObject}s.
     * The most specific class that matches a particular array component type will
     * be used, in an emulation of virtual method dispatch.
     */
    private final Map<Class, Constructor> arrayComponentClassToObjectInspectorConstructor = new HashMap<Class, Constructor>();

    private final Constructor defaultArrayInspectorConstructor;
    private final Constructor defaultTupleInspectorConstructor;

    private final InspectorAction interactiveMakeViewByAddressAction;
    private final InspectorAction interactiveMakeViewByIDAction;
    private final List<InspectorAction> makeViewActions;

    ObjectViewManager(final Inspection inspection) {
        super(inspection, VIEW_KIND, SHORT_NAME, LONG_NAME);
        Trace.begin(1, tracePrefix() + "initializing");

        // Use this if there is no subclass of array component type is matched, or if the component type is an interface.
        defaultArrayInspectorConstructor = getConstructor(ArrayView.class);
        // Array inspectors for specific subclasses of component type
        arrayComponentClassToObjectInspectorConstructor.put(Character.class, getConstructor(CharacterArrayView.class));

        // Use this if there is no object type subclass matched
        defaultTupleInspectorConstructor = getConstructor(TupleView.class);
        // Tuple inspectors for specific subclasses
        teleTupleObjectClassToObjectInspectorConstructor.put(TeleDescriptor.class, getConstructor(DescriptorView.class));
        teleTupleObjectClassToObjectInspectorConstructor.put(TeleEnum.class, getConstructor(EnumView.class));
        teleTupleObjectClassToObjectInspectorConstructor.put(TeleString.class, getConstructor(StringView.class));
        teleTupleObjectClassToObjectInspectorConstructor.put(TeleStringConstant.class, getConstructor(StringConstantView.class));
        teleTupleObjectClassToObjectInspectorConstructor.put(TeleUtf8Constant.class, getConstructor(Utf8ConstantView.class));

        focus().addListener(new InspectionFocusAdapter() {

            @Override
            public void heapObjectFocusChanged(TeleObject oldTeleObject, TeleObject teleObject) {
                if (teleObject != null) {
                    ObjectViewManager.this.makeObjectInspector(inspection, teleObject);
                }
            }
        });

        interactiveMakeViewByAddressAction = new InteractiveViewObjectByAddressAction();
        interactiveMakeViewByIDAction = new InteractiveViewObjectByIDAction();

        makeViewActions = new ArrayList<InspectorAction>(1);
        makeViewActions.add(interactiveMakeViewByAddressAction);
        makeViewActions.add(interactiveMakeViewByIDAction);
        Trace.end(1, tracePrefix() + "initializing");
    }

    public ObjectView makeView(TeleObject teleObject) {
        return makeObjectInspector(inspection(), teleObject);
    }

    public InspectorAction makeViewByAddressAction() {
        return interactiveMakeViewByAddressAction;
    }

    public InspectorAction makeViewByIDAction() {
        return interactiveMakeViewByIDAction;
    }

    public InspectorAction makeViewAction(TeleObject teleObject, String actionTitle) {
        return new InspectSpecifiedObjectAction(teleObject, actionTitle);
    }

    @Override
    public void vmProcessTerminated() {
        for (ObjectView inspector : inspectors()) {
            inspector.dispose();
        }
    }

    @Override
    protected List<InspectorAction> makeViewActions() {
        return makeViewActions;
    }

    private ObjectView makeObjectInspector(Inspection inspection, TeleObject teleObject) {
        ObjectView objectInspector =  teleObjectToInspector.get(teleObject);
        if (objectInspector == null) {
            switch (teleObject.kind()) {
                case HYBRID: {
                    objectInspector = new HubView(inspection, this, teleObject);
                    break;
                }
                case TUPLE: {
                    Constructor constructor = lookupInspectorConstructor(teleTupleObjectClassToObjectInspectorConstructor, teleObject.getClass());
                    if (constructor == null) {
                        constructor = defaultTupleInspectorConstructor;
                    }
                    try {
                        objectInspector = (ObjectView) constructor.newInstance(inspection, this, teleObject);
                    } catch (InstantiationException e) {
                        throw InspectorError.unexpected(e);
                    } catch (IllegalAccessException e) {
                        throw InspectorError.unexpected(e);
                    } catch (InvocationTargetException e) {
                        throw InspectorError.unexpected(e.getTargetException());
                    }
                    break;
                }
                case ARRAY: {
                    ClassActor componentClassActor = teleObject.classActorForObjectType().componentClassActor();
                    if (componentClassActor.isPrimitiveClassActor()) {
                        final PrimitiveClassActor primitiveClassActor = (PrimitiveClassActor) componentClassActor;
                        componentClassActor = primitiveClassActor.toWrapperClassActor();
                    }
                    Constructor constructor = lookupInspectorConstructor(arrayComponentClassToObjectInspectorConstructor, componentClassActor.toJava());
                    if (constructor == null) {
                        constructor = defaultArrayInspectorConstructor;
                    }
                    try {
                        objectInspector = (ObjectView) constructor.newInstance(inspection, this, teleObject);
                    } catch (InstantiationException e) {
                        throw InspectorError.unexpected();
                    } catch (IllegalAccessException e) {
                        throw InspectorError.unexpected();
                    } catch (InvocationTargetException e) {
                        e.printStackTrace();
                        throw InspectorError.unexpected();
                    }
                    break;
                }
            }
            if (objectInspector != null) {
                teleObjectToInspector.put(teleObject, objectInspector);
                objectInspector.addViewEventListener(new ViewEventListener() {

                    @Override
                    public void viewClosing(AbstractView inspector) {
                        final ObjectView objectInspector = (ObjectView) inspector;
                        assert teleObjectToInspector.remove(objectInspector.teleObject()) != null;
                    }

                });
                super.notifyAddingView(objectInspector);
            }
        }
        if (objectInspector != null) {
            objectInspector.highlight();
        }
        return objectInspector;
    }

    private Constructor getConstructor(Class clazz) {
        return Classes.getDeclaredConstructor(clazz, Inspection.class, ObjectViewManager.class, TeleObject.class);
    }

    private Constructor lookupInspectorConstructor(Map<Class, Constructor> map, Class clazz) {
        Class javaClass = clazz;
        while (javaClass != null) {
            final Constructor constructor = map.get(javaClass);
            if (constructor != null) {
                return constructor;
            }
            javaClass = javaClass.getSuperclass();
        }
        return null;
    }

    public boolean isObjectInspectorObservingObject(long oid) {
        for (TeleObject teleObject : teleObjectToInspector.keySet()) {
            if (teleObject.reference().makeOID() == oid) {
                return true;
            }
        }
        return false;
    }

    public void resetObjectToInspectorMapEntry(TeleObject oldTeleObject, TeleObject newTeleObject, ObjectView objectInspector) {
        teleObjectToInspector.remove(oldTeleObject);
        teleObjectToInspector.put(newTeleObject, objectInspector);
    }

    /**
     * @return all existing instances of {@link ObjectView}, even if hidden or iconic.
     */
    public Set<ObjectView> inspectors() {
        return new HashSet<ObjectView>(teleObjectToInspector.values());
    }

    private final class InteractiveViewObjectByAddressAction extends InspectorAction {

        InteractiveViewObjectByAddressAction() {
            super(inspection(), "View object at address...");
        }

        @Override
        protected void procedure() {
            new AddressInputDialog(inspection(), vm().heap().bootHeapRegion().memoryRegion().start(), "View object at address...", "View") {

                @Override
                public void entered(Address address) {
                    try {
                        final Pointer pointer = address.asPointer();
                        if (vm().isValidOrigin(pointer)) {
                            final Reference objectReference = vm().originToReference(pointer);
                            final TeleObject teleObject = vm().heap().findTeleObject(objectReference);
                            focus().setHeapObject(teleObject);
                        } else {
                            gui().errorMessage("heap object not found at "  + address.to0xHexString());
                        }
                    } catch (MaxVMBusyException maxVMBusyException) {
                        inspection().announceVMBusyFailure(name());
                    }
                }
            };
        }
    }

    private final class InteractiveViewObjectByIDAction extends InspectorAction {

        InteractiveViewObjectByIDAction() {
            super(inspection(), "View object by ID...");
        }

        @Override
        protected void procedure() {
            final String input = gui().inputDialog("View object by ID..", "");
            if (input == null) {
                // User clicked cancel.
                return;
            }
            try {
                final long oid = Long.parseLong(input);
                final TeleObject teleObject = vm().heap().findObjectByOID(oid);
                if (teleObject != null) {
                    focus().setHeapObject(teleObject);
                } else {
                    gui().errorMessage("failed to find heap object for ID: " + input);
                }
            } catch (NumberFormatException numberFormatException) {
                gui().errorMessage("Not an object ID: " + input);
            }
        }
    }

    /**
     * Action:  creates an inspector for a specific heap object in the VM.
     */
    private final class InspectSpecifiedObjectAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "View object";
        final TeleObject teleObject;

        InspectSpecifiedObjectAction(TeleObject teleObject, String actionTitle) {
            super(inspection(), actionTitle == null ? DEFAULT_TITLE : actionTitle);
            this.teleObject = teleObject;
        }

        @Override
        protected void procedure() {
            focus().setHeapObject(teleObject);
        }
    }

}
