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
package com.sun.max.vm.jdk;

import com.sun.max.annotate.*;
import com.sun.max.lang.*;
import com.sun.max.program.*;
import com.sun.max.vm.actor.holder.*;

/**
 * This class implements a centralized place for naming specific classes in the JDK that are referenced
 * explicitly in other parts of the VM.
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
    public static final ClassRef java_lang_Package                   = new ClassRef(java.lang.Package.class);
    public static final ClassRef java_lang_ProcessEnvironment        = new ClassRef("java.lang.ProcessEnvironment");
    public static final ClassRef java_lang_Runtime                   = new ClassRef(Runtime.class);
    public static final ClassRef java_lang_Shutdown                  = new ClassRef("java.lang.Shutdown");
    public static final ClassRef java_lang_String                    = new ClassRef(String.class);
    public static final ClassRef java_lang_System                    = new ClassRef(System.class);
    public static final ClassRef java_lang_Thread                    = new ClassRef(Thread.class);
    public static final ClassRef java_lang_Throwable                 = new ClassRef(Throwable.class);
    public static final ClassRef java_lang_Terminator                = new ClassRef("java.lang.Terminator");

    public static final ClassRef java_lang_ref_Finalizer                   = new ClassRef("java.lang.ref.Finalizer");
    public static final ClassRef java_lang_ref_Finalizer$FinalizerThread   = new ClassRef("java.lang.ref.Finalizer$FinalizerThread");
    public static final ClassRef java_lang_ref_Reference                   = new ClassRef(java.lang.ref.Reference.class);
    public static final ClassRef java_lang_ref_SoftReference               = new ClassRef(java.lang.ref.SoftReference.class);
    public static final ClassRef java_lang_ref_Reference$ReferenceHandler  = new ClassRef(java.lang.ref.Reference.class, "ReferenceHandler");
    public static final ClassRef java_lang_ref_ReferenceQueue              = new ClassRef(java.lang.ref.ReferenceQueue.class);

    public static final ClassRef java_io_Serializable                = new ClassRef(java.io.Serializable.class);
    public static final ClassRef java_io_UnixFileSystem              = new ClassRef("java.io.UnixFileSystem");
    public static final ClassRef java_io_ExpiringCache               = new ClassRef("java.io.ExpiringCache");

    public static final ClassRef java_nio_DirectByteBuffer           = new LazyClassRef("java.nio.DirectByteBuffer");
    public static final ClassRef java_nio_DirectCharBufferS          = new LazyClassRef("java.nio.DirectCharBufferS");
    public static final ClassRef java_nio_DirectCharBufferU          = new LazyClassRef("java.nio.DirectCharBufferU");
    public static final ClassRef java_nio_DirectDoubleBufferS        = new LazyClassRef("java.nio.DirectDoubleBufferS");
    public static final ClassRef java_nio_DirectDoubleBufferU        = new LazyClassRef("java.nio.DirectDoubleBufferU");
    public static final ClassRef java_nio_DirectFloatBufferS         = new LazyClassRef("java.nio.DirectFloatBufferS");
    public static final ClassRef java_nio_DirectFloatBufferU         = new LazyClassRef("java.nio.DirectFloatBufferU");
    public static final ClassRef java_nio_DirectIntBufferS           = new LazyClassRef("java.nio.DirectIntBufferS");
    public static final ClassRef java_nio_DirectIntBufferU           = new LazyClassRef("java.nio.DirectIntBufferU");
    public static final ClassRef java_nio_DirectLongBufferS          = new LazyClassRef("java.nio.DirectLongBufferS");
    public static final ClassRef java_nio_DirectLongBufferU          = new LazyClassRef("java.nio.DirectLongBufferU");
    public static final ClassRef java_nio_DirectShortBufferS         = new LazyClassRef("java.nio.DirectShortBufferS");
    public static final ClassRef java_nio_DirectShortBufferU         = new LazyClassRef("java.nio.DirectShortBufferU");

    public static final ClassRef java_nio_charset_CharsetEncoder     = new LazyClassRef(java.nio.charset.CharsetEncoder.class);

    public static final ClassRef java_lang_reflect_Constructor       = new ClassRef(java.lang.reflect.Constructor.class);
    public static final ClassRef java_lang_reflect_Field             = new ClassRef(java.lang.reflect.Field.class);
    public static final ClassRef java_lang_reflect_Method            = new ClassRef(java.lang.reflect.Method.class);
    public static final ClassRef java_lang_reflect_Proxy             = new LazyClassRef(java.lang.reflect.Proxy.class);

    public static final ClassRef java_util_EnumMap                   = new ClassRef(java.util.EnumMap.class);
    public static final ClassRef java_util_Random                    = new ClassRef(java.util.Random.class);

    public static final ClassRef java_math_BigInteger                = new LazyClassRef(java.math.BigInteger.class);

    public static final ClassRef java_util_concurrent_ConcurrentSkipListSet = new LazyClassRef(java.util.concurrent.ConcurrentSkipListSet.class);
    public static final ClassRef java_util_concurrent_CopyOnWriteArrayList = new LazyClassRef(java.util.concurrent.CopyOnWriteArrayList.class);
    public static final ClassRef java_util_concurrent_ConcurrentHashMap = new LazyClassRef(java.util.concurrent.ConcurrentHashMap.class);
    public static final ClassRef java_util_concurrent_ConcurrentHashMap$HashEntry = new LazyClassRef(java.util.concurrent.ConcurrentHashMap.class, "HashEntry");
    public static final ClassRef java_util_concurrent_SynchronousQueue = new LazyClassRef(java.util.concurrent.SynchronousQueue.class);
    public static final ClassRef java_util_concurrent_SynchronousQueue$TransferStack = new LazyClassRef(java.util.concurrent.SynchronousQueue.class, "TransferStack");
    public static final ClassRef java_util_concurrent_SynchronousQueue$TransferStack$SNode = new LazyClassRef(java.util.concurrent.SynchronousQueue.class, "TransferStack$SNode");
    public static final ClassRef java_util_concurrent_SynchronousQueue$TransferQueue = new LazyClassRef(java.util.concurrent.SynchronousQueue.class, "TransferQueue");
    public static final ClassRef java_util_concurrent_SynchronousQueue$TransferQueue$QNode = new LazyClassRef(java.util.concurrent.SynchronousQueue.class, "TransferQueue$QNode");
    public static final ClassRef java_util_concurrent_ForkJoinPool      = new LazyClassRef("java.util.concurrent.ForkJoinPool");
    public static final ClassRef java_util_concurrent_ForkJoinWorkerThread = new LazyClassRef("java.util.concurrent.ForkJoinWorkerThread");
    public static final ClassRef java_util_concurrent_ForkJoinTask      = new LazyClassRef("java.util.concurrent.ForkJoinTask");

    public static final ClassRef java_util_concurrent_atomic_AtomicBoolean = new LazyClassRef(java.util.concurrent.atomic.AtomicBoolean.class);
    public static final ClassRef java_util_concurrent_atomic_AtomicInteger = new LazyClassRef(java.util.concurrent.atomic.AtomicInteger.class);
    public static final ClassRef java_util_concurrent_atomic_AtomicLong = new LazyClassRef(java.util.concurrent.atomic.AtomicLong.class);
    public static final ClassRef java_util_concurrent_atomic_AtomicReference = new LazyClassRef(java.util.concurrent.atomic.AtomicReference.class);
    public static final ClassRef java_util_concurrent_atomic_AtomicIntegerArray = new LazyClassRef(java.util.concurrent.atomic.AtomicIntegerArray.class);
    public static final ClassRef java_util_concurrent_atomic_AtomicLongArray = new LazyClassRef(java.util.concurrent.atomic.AtomicLongArray.class);
    public static final ClassRef java_util_concurrent_atomic_AtomicReferenceArray = new LazyClassRef(java.util.concurrent.atomic.AtomicReferenceArray.class);
    public static final ClassRef java_util_concurrent_atomic_AtomicReferenceFieldUpdater$AtomicReferenceFieldUpdaterImpl = new LazyClassRef(java.util.concurrent.atomic.AtomicReferenceFieldUpdater.class, "AtomicReferenceFieldUpdaterImpl");
    public static final ClassRef java_util_concurrent_atomic_AtomicIntegerFieldUpdater$AtomicIntegerFieldUpdaterImpl = new LazyClassRef(java.util.concurrent.atomic.AtomicIntegerFieldUpdater.class, "AtomicIntegerFieldUpdaterImpl");
    public static final ClassRef java_util_concurrent_atomic_AtomicLongFieldUpdater$CASUpdater = new LazyClassRef(java.util.concurrent.atomic.AtomicLongFieldUpdater.class, "CASUpdater");
    public static final ClassRef java_util_concurrent_atomic_AtomicLongFieldUpdater$LockedUpdater = new LazyClassRef(java.util.concurrent.atomic.AtomicLongFieldUpdater.class, "LockedUpdater");
    public static final ClassRef java_util_concurrent_atomic_AtomicStampedReference = new LazyClassRef(java.util.concurrent.atomic.AtomicStampedReference.class);
    public static final ClassRef java_util_concurrent_atomic_AtomicMarkableReference = new LazyClassRef(java.util.concurrent.atomic.AtomicMarkableReference.class);

    public static final ClassRef java_util_concurrent_locks_AbstractQueuedSynchronizer = new LazyClassRef(java.util.concurrent.locks.AbstractQueuedSynchronizer.class);
    public static final ClassRef java_util_concurrent_locks_AbstractQueuedSynchronizer$Node = new LazyClassRef(java.util.concurrent.locks.AbstractQueuedSynchronizer.class, "Node");
    public static final ClassRef java_util_concurrent_locks_AbstractQueuedLongSynchronizer = new LazyClassRef(java.util.concurrent.locks.AbstractQueuedLongSynchronizer.class);
    public static final ClassRef java_util_concurrent_locks_AbstractQueuedLongSynchronizer$Node = new LazyClassRef(java.util.concurrent.locks.AbstractQueuedLongSynchronizer.class, "Node");
    public static final ClassRef java_util_concurrent_locks_LockSupport = new LazyClassRef(java.util.concurrent.locks.LockSupport.class);

    public static final ClassRef sun_misc_VM                         = new ClassRef(sun.misc.VM.class);
    public static final ClassRef sun_misc_Version                    = new ClassRef(sun.misc.Version.class);
    public static final ClassRef sun_misc_SharedSecrets              = new ClassRef(sun.misc.SharedSecrets.class);
    public static final ClassRef sun_misc_Unsafe                     = new ClassRef(sun.misc.Unsafe.class);
    public static final ClassRef sun_misc_PerfCounter                = new LazyClassRef("sun.misc.PerfCounter");
    public static final ClassRef sun_util_calendar_ZoneInfo              = new LazyClassRef(sun.util.calendar.ZoneInfo.class);

    public static final ClassRef sun_reflect_annotation_AnnotationParser    = new ClassRef(sun.reflect.annotation.AnnotationParser.class);
    public static final ClassRef sun_reflect_Reflection                     = new ClassRef(sun.reflect.Reflection.class);
    public static final ClassRef sun_reflect_ReflectionFactory              = new ClassRef(sun.reflect.ReflectionFactory.class);
    public static final ClassRef sun_reflect_ConstantPool                   = new ClassRef(sun.reflect.ConstantPool.class);

    public static final ClassRef sun_security_action_GetPropertyAction      = new ClassRef(sun.security.action.GetPropertyAction.class);

    // Checkstyle: resume field name check

    public static class ClassRef {
        @CONSTANT_WHEN_NOT_ZERO
        protected Class javaClass;
        @CONSTANT_WHEN_NOT_ZERO
        protected ClassActor classActor;

        public ClassRef(Class javaClass) {
            this.javaClass = javaClass;
        }

        public ClassRef(Class javaClass, String inner) {
            this.javaClass = Classes.forName(javaClass.getName() + "$" + inner);
        }

        public ClassRef(String name) {
            javaClass = Classes.forName(name);
        }

        public String className() {
            return javaClass.getName();
        }

        public Class javaClass() {
            return javaClass;
        }

        @INLINE
        public final ClassActor classActor() {
            if (classActor == null) {
                getClassActor();
            }
            return classActor;
        }

        public void resolveClassActor() {
            if (javaClass != null) {
                classActor();
            }
        }

        private void getClassActor() {
            final ClassActor classActor = ClassActor.fromJava(javaClass());
            // check again that the class actor has not already been set. Some ClassRefs will automatically be
            // updated when their classes are added to the VM class registry
            if (this.classActor == null) {
                this.classActor = classActor;
            }
            assert this.classActor == classActor : "wrong class actor registered with this ClassRef";
        }
    }

    public static class LazyClassRef extends ClassRef {
        private final String className;

        public LazyClassRef(String className) {
            super((Class) null);
            this.className = className;
        }

        public LazyClassRef(Class javaClass) {
            super((Class) null);
            className = javaClass.getName();
        }

        public LazyClassRef(Class javaClass, String inner) {
            super((Class) null);
            className = javaClass.getName() + "$" + inner;
        }

        @Override
        public String className() {
            return className;
        }

        @Override
        public final Class javaClass() {
            return Classes.forName(className);
        }
    }

    /**
     * Version number of the JDK.
     */
    public static final int JDK_VERSION;

    /**
     * Value for {@linkplain JDK_VERSION} for JDK 6.
     */
    public static final int JDK_6 = 6;

    /**
     * Value for {@linkplain JDK_VERSION} for JDK 7.
     */
    public static final int JDK_7 = 7;

    static {
        String version = System.getProperty("java.specification.version");

        if ("1.6".equals(version)) {
            JDK_VERSION = JDK_6;
        } else if ("1.7".equals(version)) {
            JDK_VERSION = JDK_7;
        } else {
            JDK_VERSION = -1;
            throw ProgramError.unexpected("Unknown java version number: " + version);
        }
    }
}
