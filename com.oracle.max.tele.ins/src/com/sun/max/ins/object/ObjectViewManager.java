/*
 * Copyright (c) 2009, 2012, Oracle and/or its affiliates. All rights reserved.
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

/**
 * Creates and manages canonical instances of {@link ObjectView} for
 * objects in the heap of the VM.
 * <p>
 * This view manager does not have a public face for creating object views.  Rather,
 * the manager listens for the user to set focus on a particular object, and which point
 * an {@link ObjectView} is created (or merely highlighted if it already exists).
 */
public final class ObjectViewManager extends AbstractMultiViewManager<ObjectView> implements ObjectViewFactory {

    private static final ViewKind VIEW_KIND = ViewKind.OBJECT;
    private static final String SHORT_NAME = "Object";
    private static final String LONG_NAME = "Object View";

    /**
     * Map:   {@link MaxObject} -- > the {@link ObjectView}, if it exists, for the corresponding
     * object in the VM.  Relies on {@link ObjectView}s being canonical.
     */
    private final Map<MaxObject, ObjectView> objectToView = new HashMap<MaxObject, ObjectView>();

    /**
     * Object view constructors for specific tuple-implemented subclasses of {@link MaxObject}s.
     * The most specific class that matches a particular {@link MaxObject} will
     * be used, in an emulation of virtual method dispatch.
     */
    private final Map<Class, Constructor> teleTupleObjectClassToObjectViewConstructor = new HashMap<Class, Constructor>();

    /**
     * Object view constructors for specific array-implemented subclasses of {@link MaxObject}s.
     * The most specific class that matches a particular array component type will
     * be used, in an emulation of virtual method dispatch.
     */
    private final Map<Class, Constructor> arrayComponentClassToObjectViewConstructor = new HashMap<Class, Constructor>();

    private final Constructor defaultArrayViewConstructor;
    private final Constructor defaultTupleViewConstructor;

    private final InspectorAction interactiveMakeViewByAddressAction;
    private final InspectorAction interactiveMakeViewByIDAction;

    private final List<InspectorAction> makeViewActions;

    ObjectViewManager(final Inspection inspection) {
        super(inspection, VIEW_KIND, SHORT_NAME, LONG_NAME);
        Trace.begin(1, tracePrefix() + "initializing");

        // Use this if there is no subclass of array component type is matched, or if the component type is an interface.
        defaultArrayViewConstructor = getConstructor(ArrayView.class);
        // Array views for specific subclasses of component type
        arrayComponentClassToObjectViewConstructor.put(Character.class, getConstructor(CharacterArrayView.class));

        // Use this if there is no object type subclass matched
        defaultTupleViewConstructor = getConstructor(TupleView.class);
        // Tuple views for specific subclasses
        teleTupleObjectClassToObjectViewConstructor.put(TeleDescriptor.class, getConstructor(DescriptorView.class));
        teleTupleObjectClassToObjectViewConstructor.put(TeleEnum.class, getConstructor(EnumView.class));
        teleTupleObjectClassToObjectViewConstructor.put(TeleString.class, getConstructor(StringView.class));
        teleTupleObjectClassToObjectViewConstructor.put(TeleStringConstant.class, getConstructor(StringConstantView.class));
        teleTupleObjectClassToObjectViewConstructor.put(TeleUtf8Constant.class, getConstructor(Utf8ConstantView.class));
        teleTupleObjectClassToObjectViewConstructor.put(TeleHeapRegionInfo.class, getConstructor(HeapRegionInfoView.class));
        focus().addListener(new InspectionFocusAdapter() {

            @Override
            public void heapObjectFocusChanged(MaxObject oldObject, MaxObject newObject) {
                if (newObject != null) {
                    ObjectViewManager.this.makeObjectView(inspection, newObject);
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

    public ObjectView makeView(MaxObject object) {
        focus().setHeapObject(object);
        return objectToView.get(object);
    }

    public InspectorAction makeViewByAddressAction() {
        return interactiveMakeViewByAddressAction;
    }

    public InspectorAction makeViewByIDAction() {
        return interactiveMakeViewByIDAction;
    }

    public InspectorAction makeViewAction(MaxObject object, String actionTitle) {
        return new ViewSpecifiedObjectAction(object, actionTitle);
    }

    @Override
    public void vmProcessTerminated() {
        for (ObjectView objectView : objectViews()) {
            objectView.dispose();
        }
    }

    @Override
    protected List<InspectorAction> makeViewActions() {
        return makeViewActions;
    }

    private ObjectView makeObjectView(Inspection inspection, MaxObject object) {
        ObjectView objectView =  objectToView.get(object);
        if (objectView == null) {
            switch (object.kind()) {
                case HYBRID: {
                    objectView = new HubView(inspection, object);
                    break;
                }
                case TUPLE: {
                    Constructor constructor = lookupViewConstructor(teleTupleObjectClassToObjectViewConstructor, object.getClass());
                    if (constructor == null) {
                        constructor = defaultTupleViewConstructor;
                    }
                    try {
                        objectView = (ObjectView) constructor.newInstance(inspection, object);
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
                    ClassActor componentClassActor = object.classActorForObjectType().componentClassActor();
                    if (componentClassActor.isPrimitiveClassActor()) {
                        final PrimitiveClassActor primitiveClassActor = (PrimitiveClassActor) componentClassActor;
                        componentClassActor = primitiveClassActor.toWrapperClassActor();
                    }
                    Constructor constructor = lookupViewConstructor(arrayComponentClassToObjectViewConstructor, componentClassActor.toJava());
                    if (constructor == null) {
                        constructor = defaultArrayViewConstructor;
                    }
                    try {
                        objectView = (ObjectView) constructor.newInstance(inspection, object);
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
            if (objectView != null) {
                objectToView.put(object, objectView);
                objectView.addViewEventListener(new ViewEventListener() {

                    @Override
                    public void viewClosing(AbstractView view) {
                        final ObjectView objectView = (ObjectView) view;
                        assert objectToView.remove(objectView.object()) != null;
                    }

                });
                super.notifyAddingView(objectView);
            }
        }
        if (objectView != null) {
            objectView.highlight();
        }
        return objectView;
    }

    private Constructor getConstructor(Class clazz) {
        return Classes.getDeclaredConstructor(clazz, Inspection.class, MaxObject.class);
    }

    private Constructor lookupViewConstructor(Map<Class, Constructor> map, Class clazz) {
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

    public boolean isObjectViewObservingObject(long oid) {
        for (MaxObject object : objectToView.keySet()) {
            if (object.reference().makeOID() == oid) {
                return true;
            }
        }
        return false;
    }

    public void resetObjectToViewMapEntry(MaxObject oldObject, MaxObject newObject, ObjectView objectView) {
        objectToView.remove(oldObject);
        objectToView.put(newObject, objectView);
    }

    /**
     * @return all existing instances of {@link ObjectView}, even if hidden or iconic.
     */
    public Set<ObjectView> objectViews() {
        return new HashSet<ObjectView>(objectToView.values());
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
                    final MaxObject object = vm().objects().findObjectAt(address);
                    if (object != null) {
                        focus().setHeapObject(object);
                    } else {
                        gui().errorMessage("object not found at "  + address.to0xHexString());
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
                final MaxObject object = vm().objects().findObjectByOID(oid);
                if (object != null) {
                    focus().setHeapObject(object);
                } else {
                    gui().errorMessage("failed to find heap object for ID: " + input);
                }
            } catch (NumberFormatException numberFormatException) {
                gui().errorMessage("Not an object ID: " + input);
            }
        }
    }

    /**
     * Action:  creates a view for a specific heap object in the VM.
     */
    private final class ViewSpecifiedObjectAction extends InspectorAction {

        private static final String DEFAULT_TITLE = "View object";
        final MaxObject object;

        ViewSpecifiedObjectAction(MaxObject object, String actionTitle) {
            super(inspection(), actionTitle == null ? DEFAULT_TITLE : actionTitle);
            this.object = object;
        }

        @Override
        protected void procedure() {
            focus().setHeapObject(object);
        }
    }

}
