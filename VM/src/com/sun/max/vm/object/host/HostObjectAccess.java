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
package com.sun.max.vm.object.host;

import java.lang.reflect.*;
import java.util.*;

import com.sun.c1x.debug.*;
import com.sun.max.annotate.*;
import com.sun.max.program.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.layout.*;
import com.sun.max.vm.object.*;
import com.sun.max.vm.prototype.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;

/**
 * This class implements object access routines for use while bootstrapping.
 *
 * @author Bernd Mathiske
 */
@HOSTED_ONLY
public final class HostObjectAccess {

    private HostObjectAccess() {
    }

    /**
     * Reads the hub of the specified object. The host implementation builds the class actor from the Java class and
     * then gets its dynamic hub.
     *
     * @param object the object for which to get the hub
     * @return the hub for the object
     */
    public static Hub readHub(Object object) {
        if (object instanceof StaticTuple) {
            final StaticTuple staticTuple = (StaticTuple) object;
            return staticTuple.classActor().staticHub();
        }
        return ClassActor.fromJava(object.getClass()).dynamicHub();
    }

    /**
     * Gets the size of the allocation cell for an object. The host implementation gets the hub and uses the hub to get
     * the size, and is thus sensitive to the VM configuration used to create the class actor and hub.
     *
     * @param object the object for which to get the size
     * @return the size of the object in bytes
     */
    public static Size getSize(Object object) {
        return getSize(HostObjectAccess.readHub(object), object);
    }

    /**
     * Gets the size of the allocation cell for an object with a known hub. For class objects, the size is the same for
     * all instances, while for array objects, the size is computed based on the length of the array.
     *
     * @param hub the hub of the object
     * @param object the object itself
     * @return the size of the object in bytes
     */
    public static Size getSize(final Hub hub, Object object) {
        if (object.getClass().isArray()) {
            final ArrayLayout arrayLayout = (ArrayLayout) hub.specificLayout;
            return arrayLayout.getArraySize(Array.getLength(object));
        }
        if (object instanceof Hybrid) {
            final Hybrid hybrid = (Hybrid) object;
            final HybridLayout hybridLayout = (HybridLayout) hub.specificLayout;
            return hybridLayout.getArraySize(hybrid.length());
        }
        return hub.tupleSize;
    }

    /**
     * Gets the length of an array or hybrid.
     *
     * @param object the object for which to get the length
     * @return the length of the array or hybrid (in elements)
     */
    public static int getArrayLength(Object object) {
        if (object.getClass().isArray()) {
            return Array.getLength(object);
        }
        if (object instanceof Hybrid) {
            final Hybrid hybrid = (Hybrid) object;
            return hybrid.length();
        }
        throw ProgramError.unexpected("getArrayLength(tuple) not allowed");
    }

    /**
     * Gets the system thread group.
     *
     * @return the thread group for the entire system
     */
    private static ThreadGroup getSystemThreadGroup() {
        ThreadGroup threadGroup = Thread.currentThread().getThreadGroup();
        while (true) {
            final ThreadGroup parent = threadGroup.getParent();
            if (parent == null) {
                assert threadGroup.getName().equals("system");
                return threadGroup;
            }
            threadGroup = parent;
        }
    }

    /**
     * The system thread group singleton.
     */
    private static final ThreadGroup systemThreadGroup = getSystemThreadGroup();

    /**
     * The main thread running this virtual machine.
     */
    private static Thread mainThread;

    static {
        final ThreadGroup[] subGroups = new ThreadGroup[systemThreadGroup.activeGroupCount()];
        final int count = systemThreadGroup.enumerate(subGroups);
        for (int i = 0; i < count; ++i) {
            final ThreadGroup threadGroup = subGroups[i];
            if (threadGroup.getName().equals("main")) {
                final Thread[] threads = new Thread[threadGroup.activeCount()];
                final int threadCount = threadGroup.enumerate(threads);
                for (int j = 0; j < threadCount; ++j) {
                    final Thread thread = threads[j];
                    if (thread.getName().equals("main")) {
                        mainThread = thread;
                    }
                }
            }
        }
    }

    /**
     * Sets the main thread to the new thread specified.
     * @param thread the main thread
     */
    public static void setMainThread(Thread thread) {
        assert mainThread == null || mainThread == thread;
        assert thread.getName().equals("main");
        mainThread = thread;
    }

    /**
     * Accessor for the main thread.
     * @return the main thread
     */
    public static Thread mainThread() {
        return mainThread;
    }

    /**
     * A object to signal that all references to a particular object should be set to null.
     */
    private static final Object NULL = new Object();

    /**
     * A map used during bootstrapping to replace references to a particular object with
     * references to another object during graph reachability.
     */
    private static Map<Object, Object> objectMap; // TODO: this map and its uses should probably be moved to the prototype

    /**
     * A map used to canonicalize instances of the Maxine value classes.
     */
    private static final Map<Object, Object> valueMap = new HashMap<Object, Object>();

    /**
     * This method maps a host object to a target object. For most objects, this method will return the parameter
     * object, but some objects are not portable from the host VM to the target VM and references to them must be
     * updated with references to a different object (perhaps {@code null}).
     *
     * @param object the host object to translate
     * @return a reference to the corresponding object in the target VM
     */
    public static Object hostToTarget(Object object) {
        if (object instanceof String || object instanceof Value || object instanceof NameAndTypeConstant) {
            // canonicalize all instances of these classes using .equals()
            Object result = valueMap.get(object);
            if (result == null) {
                result = object;
                valueMap.put(object, object);
            }
            return result;
        }
        final Object replace = getObjectReplacement(object);
        if (replace != null) {
            return replace == NULL ? null : replace;
        }
        if (object instanceof Thread || object instanceof ThreadGroup) {
            if (MaxineVM.isMaxineClass(ClassActor.fromJava(object.getClass()))) {
                ProgramError.unexpected("Instance of thread class " + object.getClass().getName() + " will be null in the image");
            }
            return null;
        }
        return object;
    }

    private static Object getObjectReplacement(Object object) {
        if (objectMap == null) {
            // check the object identity map certain objects to certain other objects
            initializeObjectIdentityMap();
        }
        final Object replace = objectMap.get(object);
        return replace;
    }

    private static void initializeObjectIdentityMap() {
        objectMap = new IdentityHashMap<Object, Object>();

        objectMap.put(HostedBootClassLoader.HOSTED_BOOT_CLASS_LOADER, BootClassLoader.BOOT_CLASS_LOADER);
        objectMap.put(BootClassLoader.BOOT_CLASS_LOADER.getParent(), NULL);
        objectMap.put(systemThreadGroup, NULL);
        objectMap.put(mainThread, mainThread);
        objectMap.put(Trace.stream(), Log.out);
        final ThreadGroup threadGroup = new ThreadGroup("main");
        WithoutAccessCheck.setInstanceField(threadGroup, "parent", null);
        objectMap.put(mainThread.getThreadGroup(), threadGroup);
        objectMap.put(threadGroup, threadGroup);
        objectMap.put(MaxineVM.host(), MaxineVM.target());
        objectMap.put(TTY.out, new LogStream(Log.os));
        objectMap.put(WithoutAccessCheck.getStaticField(System.class, "props"), JDKInterceptor.initialSystemProperties);
    }
}
