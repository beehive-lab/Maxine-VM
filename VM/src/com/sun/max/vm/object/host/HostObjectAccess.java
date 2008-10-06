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
/*VCSID=5ea0acfa-8adf-46d4-b8e8-7e7661aa6e43*/
package com.sun.max.vm.object.host;

import java.lang.reflect.*;
import java.util.*;

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
 * This class implements object access routines for use at prototyping time (i.e. running
 * on a host virtual machine such as HotSpot).
 *
 * @author Bernd Mathiske
 */
@PROTOTYPE_ONLY
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
            final ArrayLayout arrayLayout = (ArrayLayout) hub.specificLayout();
            return arrayLayout.getArraySize(Array.getLength(object));
        }
        if (object instanceof Hybrid) {
            final Hybrid hybrid = (Hybrid) object;
            final HybridLayout hybridLayout = (HybridLayout) hub.specificLayout();
            return hybridLayout.getArraySize(hybrid.length());
        }
        return hub.tupleSize();
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
    private static final ThreadGroup _systemThreadGroup = getSystemThreadGroup();

    /**
     * The main thread running this virtual machine.
     */
    private static Thread _mainThread;

    static {
        final ThreadGroup[] subGroups = new ThreadGroup[_systemThreadGroup.activeGroupCount()];
        final int count = _systemThreadGroup.enumerate(subGroups);
        for (int i = 0; i < count; ++i) {
            final ThreadGroup threadGroup = subGroups[i];
            if (threadGroup.getName().equals("main")) {
                final Thread[] threads = new Thread[threadGroup.activeCount()];
                final int threadCount = threadGroup.enumerate(threads);
                for (int j = 0; j < threadCount; ++j) {
                    final Thread thread = threads[j];
                    if (thread.getName().equals("main")) {
                        _mainThread = thread;
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
        assert _mainThread == null || _mainThread == thread;
        assert thread.getName().equals("main");
        _mainThread = thread;
    }

    /**
     * Accessor for the main thread.
     * @return the main thread
     */
    public static Thread mainThread() {
        return _mainThread;
    }

    /**
     * A object to signal that all references to a particular object should be set to null.
     */
    private static final Object NULL = new Object();

    /**
     * A map used during prototyping time to replace references to a particular object with
     * references to another object during graph reachability.
     */
    private static Map<Object, Object> _objectMap; // TODO: this map and its uses should probably be moved to the prototype

    /**
     * A map used to canonicalize instances of the Maxine value classes.
     */
    private static Map<Value, Value> _valueMap;

    /**
     * A map used to canonicalize instances of @link NameAndTypeConstant.
     */
    private static final Map<NameAndTypeConstant, NameAndTypeConstant> _nameAndTypeMap = new HashMap<NameAndTypeConstant, NameAndTypeConstant>();

    /**
     * This method maps a host object to a target object. For most objects, this method will return the parameter
     * object, but some objects are not portable from the host VM to the target VM and references to them must be
     * updated with references to a different object (perhaps {@code null}).
     *
     * @param object the host object to translate
     * @return a reference to the corresponding object in the target VM
     */
    public static Object hostToTarget(Object object) {
        if (object instanceof String) {
            return ((String) object).intern();
        }
        if (object instanceof Value) {
            // canonicalize all instances of com.sun.max.vm.value.Value
            if (_valueMap == null) {
                _valueMap = new HashMap<Value, Value>();
            }
            final Value iv = (Value) object;

            // '-0' requires a compiler literal that is separated from '0',
            // even though '==' per FPU would return 'true':
            switch (iv.kind().asEnum()) {
                case FLOAT: {
                    if (iv.asFloat() == 0F && Float.floatToRawIntBits(iv.asFloat()) != 0) {
                        return iv;
                    }
                    break;
                }
                case DOUBLE: {
                    if (iv.asDouble() == 0D && Double.doubleToRawLongBits(iv.asDouble()) != 0L) {
                        return iv;
                    }
                    break;
                }
                default: {
                    break;
                }
            }
            final Value rv = _valueMap.get(iv);
            if (rv != null) {
                return rv;
            }
            _valueMap.put(iv, iv);
            return iv;
        }
        if (_objectMap == null) {
            // remap certain objects to certain other objects
            _objectMap = new IdentityHashMap<Object, Object>();

            _objectMap.put(PrototypeClassLoader.PROTOTYPE_CLASS_LOADER, VmClassLoader.VM_CLASS_LOADER);
            _objectMap.put(VmClassLoader.VM_CLASS_LOADER.getParent(), NULL);
            _objectMap.put(_systemThreadGroup, NULL);
            _objectMap.put(_mainThread, _mainThread);
            final ThreadGroup threadGroup = new ThreadGroup("MaxineVM");
            WithoutAccessCheck.setInstanceField(threadGroup, "parent", null);
            _objectMap.put(_mainThread.getThreadGroup(), threadGroup);
            _objectMap.put(threadGroup, threadGroup);
            _objectMap.put(MaxineVM.host(), MaxineVM.target());
            _objectMap.put(WithoutAccessCheck.getStaticField(System.class, "props"), new Properties());
            try {
                _objectMap.put(WithoutAccessCheck.getStaticField(Class.forName("java.lang.ApplicationShutdownHooks"), "hooks"), new IdentityHashMap<Thread, Thread>());
                _objectMap.put(WithoutAccessCheck.getStaticField(Class.forName("java.lang.Shutdown"), "hooks"), new ArrayList<Runnable>());
            } catch (ClassNotFoundException classNotFoundException) {
                ProgramError.unexpected(classNotFoundException);
            }

            HackJDK.fixBufferedInputStream(_objectMap);
        }

        final Object replace = _objectMap.get(object);
        if (replace == NULL) {
            return null;
        }
        if (replace != null) {
            return replace;
        }
        if (object instanceof Thread) {
            return null;
        }
        if (object instanceof ThreadGroup) {
            return null;
        }

        if (object instanceof NameAndTypeConstant) {
            NameAndTypeConstant nameAndType = _nameAndTypeMap.get(object);
            if (nameAndType == null) {
                nameAndType = (NameAndTypeConstant) object;
                _nameAndTypeMap.put(nameAndType, nameAndType);
            }
            return nameAndType;
        }
        return object;
    }
}
