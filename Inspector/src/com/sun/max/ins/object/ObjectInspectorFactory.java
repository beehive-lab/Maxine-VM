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

import java.lang.reflect.*;
import java.util.*;

import com.sun.max.collect.*;
import com.sun.max.ins.*;
import com.sun.max.ins.gui.*;
import com.sun.max.ins.gui.Inspector.*;
import com.sun.max.lang.*;
import com.sun.max.program.*;
import com.sun.max.tele.*;
import com.sun.max.tele.object.*;


/**
 * Creates and manages canonical instances of {@link ObjectInspector} for
 * objects in the heap of the {@link TeleVM}.
 *
 * @author Michael Van De Vanter
 */
public final class ObjectInspectorFactory extends AbstractInspectionHolder {

    private static ObjectInspectorFactory _factory;

    /**
     * Creates the singleton factory that listens for events and will find or create instances
     * of {@link ObjectInspector} as needed.
     */
    public static void make(final Inspection inspection) {
        if (_factory == null) {
            _factory = new ObjectInspectorFactory(inspection);
        }
    }

    /**
     * Map:   {@link TeleObject} -- > the {@link ObjectInspector}, if it exists, for the corresponding
     * object in the {@link TeleVM}.  Relies on {@link ObjectInspector}s being canonical.
     */
    private  final VariableMapping<TeleObject, ObjectInspector> _teleObjectToInspector = HashMapping.createVariableIdentityMapping();

    /**
     * ObjectInspector constructors for specific tuple-implemented classes of tuple and hybrid {@link TeleObject}s.
     * The most specific class that matches a particular {@link TeleObject} will
     * be used, in an emulation of virtual method dispatch.
     */
    private final Map<Class, Constructor> _teleTupleObjectClassToObjectInspectorConstructor = new HashMap<Class, Constructor>();


    private ObjectInspectorFactory(final Inspection inspection) {
        super(inspection);
        Trace.begin(1, tracePrefix() + "initializing");

        // Most general type of tuple-object surrogate, so there will always be a match.
        _teleTupleObjectClassToObjectInspectorConstructor.put(TeleTupleObject.class, getConstructor(TupleInspector.class));

        inspection.focus().addListener(new InspectionFocusAdapter() {

            @Override
            public void heapObjectFocusChanged(TeleObject oldTeleObject, TeleObject teleObject) {
                if (teleObject != null) {
                    _factory.makeObjectInspector(inspection, teleObject);
                }
            }
        });
        Trace.end(1, tracePrefix() + "initializing");
    }

    private void makeObjectInspector(Inspection inspection, TeleObject teleObject) {
        ObjectInspector objectInspector =  _teleObjectToInspector.get(teleObject);
        if (objectInspector == null) {
            switch (teleObject.getObjectKind()) {
                case HYBRID: {
                    objectInspector = new HubInspector(inspection, this, Residence.INTERNAL, teleObject);
                    break;
                }
                case TUPLE: {
                    final Constructor constructor = lookupTupleObjectInspectorConstructor(teleObject);
                    try {
                        objectInspector = (ObjectInspector) constructor.newInstance(inspection, this, Residence.INTERNAL, teleObject);
                    } catch (InstantiationException e) {
                        throw ProgramError.unexpected();
                    } catch (IllegalAccessException e) {
                        throw ProgramError.unexpected();
                    } catch (InvocationTargetException e) {
                        throw ProgramError.unexpected();
                    }
                    break;
                }
                case ARRAY: {
                    objectInspector = new ArrayInspector(inspection, this, Residence.INTERNAL, teleObject);
                    break;
                }
            }
            if (objectInspector != null) {
                _teleObjectToInspector.put(teleObject, objectInspector);
            }
        }
        if (objectInspector != null) {
            objectInspector.highlight();
        }
    }

    private Constructor getConstructor(Class clazz) {
        return Classes.getDeclaredConstructor(clazz, Inspection.class, ObjectInspectorFactory.class, Inspector.Residence.class, TeleObject.class);
    }

    private Constructor lookupTupleObjectInspectorConstructor(TeleObject teleObject) {
        Class javaClass = teleObject.getClass();
        while (javaClass != null) {
            final Constructor constructor = _teleTupleObjectClassToObjectInspectorConstructor.get(javaClass);
            if (constructor != null) {
                return constructor;
            }
            javaClass = javaClass.getSuperclass();
        }
        ProgramError.unexpected(tracePrefix() + " failed to find constructor for class" + javaClass);
        return null;
    }

    void objectInspectorClosing(ObjectInspector objectInspector) {
        _teleObjectToInspector.remove(objectInspector.teleObject());
    }


}
