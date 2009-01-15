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
package com.sun.max.vm.compiler.dir.transform;

import com.sun.max.collect.*;
import com.sun.max.memory.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.compiler.dir.*;
import com.sun.max.vm.compiler.snippet.*;
import com.sun.max.vm.grip.*;
import com.sun.max.vm.jni.*;
import com.sun.max.vm.monitor.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.thread.*;
import com.sun.max.vm.value.*;

/**
 * @author Bernd Mathiske
 */
public final class MethodTrace {

    private static VMSizeOption _methodTraceOption = new VMSizeOption("-XX:MethodTraceThreshold=", Size.fromLong(Long.MAX_VALUE),
                    "The method tracing threshold.", MaxineVM.Phase.PRISTINE);

    private MethodTrace() {
    }

    private static boolean _isEnabled;

    public static boolean isEnabled() {
        return _isEnabled;
    }

    public static void enable() {
        _isEnabled = true;
    }

    public static void enter(String methodName) {
        final Address a = VmThreadLocal.METHOD_TRACE_COUNT.getVariableWord().asAddress();
        if (!a.isBitSet(0)) {
            VmThreadLocal.METHOD_TRACE_COUNT.setVariableWord(Address.fromInt(1));
            final long count = a.unsignedShiftedRight(1).toLong();
            if (count >= _methodTraceOption.getValue().toLong()) {
                final boolean lockDisabledSafepoints = Log.lock();
                Log.print(VmThread.currentVmThreadLocals());
                Log.print(" - ");
                Log.print(count);
                Log.print(" enter: ");
                Log.println(methodName);
                Log.unlock(lockDisabledSafepoints);
            }
            VmThreadLocal.METHOD_TRACE_COUNT.setVariableWord(a.plus(2));
        }
    }

    public static void leave(String methodName) {
        final Address a = VmThreadLocal.METHOD_TRACE_COUNT.getVariableWord().asAddress();
        if (!a.isBitSet(0)) {
            VmThreadLocal.METHOD_TRACE_COUNT.setVariableWord(Address.fromInt(1));
            final long count = a.unsignedShiftedRight(1).toLong();
            if (count >= _methodTraceOption.getValue().toLong()) {
                final boolean lockDisabledSafepoints = Log.lock();
                Log.print(VmThread.currentVmThreadLocals());
                Log.print(" - ");
                Log.print(count);
                Log.print(" leave: ");
                Log.println(methodName);
                Log.unlock(lockDisabledSafepoints);
            }
            VmThreadLocal.METHOD_TRACE_COUNT.setVariableWord(a.plus(2));
        }
    }

    public static void throwFrom(String methodName) {
        final Address a = VmThreadLocal.METHOD_TRACE_COUNT.getVariableWord().asAddress();
        if (!a.isBitSet(0)) {
            VmThreadLocal.METHOD_TRACE_COUNT.setVariableWord(Address.fromInt(1));
            final long count = a.unsignedShiftedRight(1).toLong();
            if (count >= _methodTraceOption.getValue().toLong()) {
                final boolean lockDisabledSafepoints = Log.lock();
                Log.print(VmThread.currentVmThreadLocals());
                Log.print(" - ");
                Log.print(count);
                Log.print(" throwFrom: ");
                Log.println(methodName);
                Log.unlock(lockDisabledSafepoints);
            }
            VmThreadLocal.METHOD_TRACE_COUNT.setVariableWord(a.plus(2));
        }
    }

    private static DirMethodCall createCall(String name, DirConstant callerName) {
        final CriticalMethod callee = new CriticalMethod(MethodTrace.class, name, CallEntryPoint.OPTIMIZED_ENTRY_POINT);
        return new DirMethodCall(null, new DirMethodValue(callee.classMethodActor()), new DirValue[]{callerName}, null, false, null);
    }

    private static final IdentityHashSet<Class> _excludedClasses = new LinkedIdentityHashSet<Class>(new Class[]{
        MethodTrace.class,
        Log.class,
        MaxineVM.class,
        BootMemory.class,
        String.class,
        Math.class,
        CString.class,
        Reference.class,
        Grip.class,
        StringBuilder.class,
        Enum.class,
        VmThread.class,
        VmThreadLocal.class,
        Safepoint.class,
        VmThreadMap.class,
        JniHandles.class
    });

    private static final IdentityHashSet<String> _excludedClassNames = new LinkedIdentityHashSet<String>(new String[]{
        "java.lang.AbstractStringBuilder",
    });

    public static void instrument(DirMethod dirMethod) {
        final ClassMethodActor method = dirMethod.classMethodActor();
        final ClassActor holder = method.holder();
        if (!MethodTrace.isEnabled() ||
                        Snippet.class.isAssignableFrom(holder.toJava()) ||
                        method.isInline() ||
                        method.isDeclaredNeverInline() ||
                        method.isNative() ||
                        method.isDeclaredFoldable() ||
                        method.isCFunction() ||
                        method.isTemplate() ||
                        _excludedClasses.contains(holder.toJava()) ||
                        _excludedClassNames.contains(holder.name().toString()) ||
                Word.class.isAssignableFrom(holder.toJava()) ||
                MonitorScheme.class.isAssignableFrom(holder.toJava())) {
            return;
        }
        final DirConstant callerName = new DirConstant(ReferenceValue.from(method.format("%H.%n(%p)")));
        dirMethod.blocks().first().instructions().prepend(createCall("enter", callerName));
        for (DirBlock block : dirMethod.blocks()) {
            final VariableSequence<DirInstruction> instructions = block.instructions();
            if (instructions.last() instanceof DirReturn) {
                instructions.insert(instructions.length() - 1, createCall("leave", callerName));
            } else if (instructions.last() instanceof DirThrow) {
                instructions.insert(instructions.length() - 1, createCall("throwFrom", callerName));
            }
        }
    }

    public static void mark1() {
    }

    public static void mark2() {
    }

    public static void mark3() {
    }

    public static void mark4() {
    }

    public static void mark5() {
    }

    public static void mark6() {
    }

    public static void mark7() {
    }

    public static void mark8() {
    }

    public static void mark9() {
    }

    public static void mark10() {
    }
}
