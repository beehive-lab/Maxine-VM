/*
 * Copyright (c) 2009 Sun Microsystems, Inc.  All rights reserved.
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
package com.sun.max.vm.jdk;

import com.sun.max.annotate.*;
import com.sun.max.lang.*;
import com.sun.max.vm.actor.holder.*;


/**
 * This class implements a centralized place for naming specific classes in the JDK that are referenced
 * explicitly in other parts of the VM.
 *
 * @author Ben L. Titzer
 */
public class JDK {

    // Checkstyle: stop field name check
    public static final ClassRef java_lang_ApplicationShutdownHooks  = new ClassRef("java.lang.ApplicationShutdownHooks");
    public static final ClassRef java_lang_Class                     = new ClassRef(Class.class);
    public static final ClassRef java_lang_ClassLoader               = new ClassRef(ClassLoader.class);
    public static final ClassRef java_lang_ClassLoader$NativeLibrary = new ClassRef(ClassLoader.class, "NativeLibrary");
    public static final ClassRef java_lang_Cloneable                 = new ClassRef(Cloneable.class);
    public static final ClassRef java_lang_Compiler                  = new ClassRef(Compiler.class);
    public static final ClassRef java_lang_Object                    = new ClassRef(Object.class);
    public static final ClassRef java_lang_Package                   = new ClassRef(Package.class);
    public static final ClassRef java_lang_ProcessEnvironment        = new ClassRef("java.lang.ProcessEnvironment");
    public static final ClassRef java_lang_Runtime                   = new ClassRef(Runtime.class);
    public static final ClassRef java_lang_Shutdown                  = new ClassRef("java.lang.Shutdown");
    public static final ClassRef java_lang_String                    = new ClassRef(String.class);
    public static final ClassRef java_lang_System                    = new ClassRef(System.class);
    public static final ClassRef java_lang_Thread                    = new ClassRef(Thread.class);
    public static final ClassRef java_lang_Throwable                 = new ClassRef(Throwable.class);

    public static final ClassRef java_lang_ref_Finalizer             = new ClassRef("java.lang.ref.Finalizer");
    public static final ClassRef java_lang_ref_Reference             = new ClassRef(java.lang.ref.Reference.class);
    public static final ClassRef java_lang_ref_ReferenceQueue        = new ClassRef(java.lang.ref.ReferenceQueue.class);

    public static final ClassRef java_io_BufferedInputStream         = new ClassRef(java.io.BufferedInputStream.class);
    public static final ClassRef java_io_Serializable                = new ClassRef(java.io.Serializable.class);
    public static final ClassRef java_io_UnixFileSystem              = new ClassRef("java.io.UnixFileSystem");
    public static final ClassRef java_io_ExpiringCache               = new ClassRef("java.io.ExpiringCache");

    public static final ClassRef java_lang_reflect_Constructor       = new ClassRef(java.lang.reflect.Constructor.class);
    public static final ClassRef java_lang_reflect_Field             = new ClassRef(java.lang.reflect.Field.class);
    public static final ClassRef java_lang_reflect_Method            = new ClassRef(java.lang.reflect.Method.class);
//    public static final ClassRef java_lang_reflect_Proxy             = new ClassRef(java.lang.reflect.Proxy.class);

    public static final ClassRef java_util_EnumMap                   = new ClassRef(java.util.EnumMap.class);

    public static final ClassRef java_util_concurrent_atomic_AtomicReferenceFieldUpdater$AtomicReferenceFieldUpdaterImpl = new ClassRef(java.util.concurrent.atomic.AtomicReferenceFieldUpdater.class, "AtomicReferenceFieldUpdaterImpl");
    public static final ClassRef java_util_concurrent_atomic_AtomicIntegerFieldUpdater$AtomicIntegerFieldUpdaterImpl = new ClassRef(java.util.concurrent.atomic.AtomicIntegerFieldUpdater.class, "AtomicIntegerFieldUpdaterImpl");
    public static final ClassRef java_util_concurrent_atomic_AtomicLongFieldUpdater$CASUpdater = new ClassRef(java.util.concurrent.atomic.AtomicLongFieldUpdater.class, "CASUpdater");
    public static final ClassRef java_util_concurrent_atomic_AtomicLongFieldUpdater$LockedUpdater = new ClassRef(java.util.concurrent.atomic.AtomicLongFieldUpdater.class, "LockedUpdater");

    public static final ClassRef java_util_concurrent_locks_AbstractQueuedSynchronizer = new ClassRef(java.util.concurrent.locks.AbstractQueuedSynchronizer.class);
    public static final ClassRef java_util_concurrent_locks_AbstractQueuedSynchronizer$Node = new ClassRef(java.util.concurrent.locks.AbstractQueuedSynchronizer.class, "Node");

    public static final ClassRef sun_misc_VM                         = new ClassRef(sun.misc.VM.class);
    public static final ClassRef sun_misc_Version                    = new ClassRef(sun.misc.Version.class);
    public static final ClassRef sun_misc_SharedSecrets              = new ClassRef(sun.misc.SharedSecrets.class);

    public static final ClassRef sun_reflect_annotation_AnnotationParser    = new ClassRef(sun.reflect.annotation.AnnotationParser.class);
    public static final ClassRef sun_reflect_Reflection                     = new ClassRef(sun.reflect.Reflection.class);
    public static final ClassRef sun_reflect_ReflectionFactory              = new ClassRef(sun.reflect.ReflectionFactory.class);

    public static final ClassRef sun_security_action_GetPropertyAction      = new ClassRef(sun.security.action.GetPropertyAction.class);

    // Checkstyle: resume field name check

    public static class ClassRef {
        @CONSTANT_WHEN_NOT_ZERO
        private Class _javaClass;
        @CONSTANT_WHEN_NOT_ZERO
        private ClassActor _classActor;

        public ClassRef(Class javaClass) {
            _javaClass = javaClass;
        }

        public ClassRef(Class javaClass, String inner) {
            _javaClass = Classes.forName(javaClass.getName() + "$" + inner);
        }

        public ClassRef(String name) {
            _javaClass = Classes.forName(name);
        }

        @INLINE
        public final Class javaClass() {
            return _javaClass;
        }

        @INLINE
        public final ClassActor classActor() {
            if (_classActor != null) {
                getClassActor();
            }
            return _classActor;
        }

        private void getClassActor() {
            final ClassActor classActor = ClassActor.fromJava(_javaClass);
            // check again that the class actor has not already been set. Some ClassRefs will automatically be
            // updated when their classes are added to the VM class registry
            if (_classActor == null) {
                _classActor = classActor;
            }
            assert _classActor == classActor : "wrong class actor registered with this ClassRef";
        }
    }
}
