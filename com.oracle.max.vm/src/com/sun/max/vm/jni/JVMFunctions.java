/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.jni;

import static com.sun.cri.bytecode.Bytecodes.Infopoints.*;
import static com.sun.max.platform.Platform.*;
import static com.sun.max.vm.runtime.VMRegister.*;

import java.lang.reflect.*;
import java.util.*;

import sun.reflect.*;

import com.sun.max.annotate.*;
import com.sun.max.platform.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.stack.*;
import com.sun.max.vm.stack.StackFrameWalker.Cursor;
import com.sun.max.vm.thread.*;
import com.sun.max.vm.type.*;

/**
 * This class implements part of the Maxine version of the JVM_* interface which the VM presents to the JDK's native code.
 * Some of the JVM interface is implemented directly in C in "jvm.c" and
 * some is implemented by upcalls to this Java code, which calls into the Maxine VM.
 */
public class JVMFunctions {

    // Checkstyle: stop method name check

    public static void Unimplemented() {
        throw FatalError.unimplemented();
    }

    static class ClassContext extends SourceFrameVisitor {
        boolean skippingUntilNativeMethod;
        ArrayList<Class> classes = new ArrayList<Class>(20);

        @Override
        public boolean visitSourceFrame(ClassMethodActor method, int bci, boolean trapped, long frameId) {
            if (!skippingUntilNativeMethod) {
                if (method.holder().isReflectionStub() || method.isNative()) {
                    // ignore reflection stubs and native methods (according to JVM_GetClassContext in HotSpot)
                } else {
                    classes.add(method.holder().toJava());
                }
            } else {
                if (method.isNative()) {
                    skippingUntilNativeMethod = false;
                }
            }
            return true;
        }
    }

    public static Class[] GetClassContext() {
        ClassContext classContext = new ClassContext();

        // In Maxine VE there are no native frames, or JNI calls on the stack that need to be ignored
        classContext.skippingUntilNativeMethod = platform().os != OS.MAXVE;

        classContext.walk(null, Pointer.fromLong(here()), getCpuStackPointer(), getCpuFramePointer());
        ArrayList<Class> classes = classContext.classes;
        return classContext.classes.toArray(new Class[classes.size()]);
    }

    static final CriticalMethod javaLangReflectMethodInvoke = new CriticalMethod(Method.class, "invoke",
        SignatureDescriptor.create(Object.class, Object.class, Object[].class));

    static class LatestUserDefinedLoaderVisitor extends RawStackFrameVisitor {
        ClassLoader result;
        @Override
        public boolean visitFrame(Cursor current, Cursor callee) {
            TargetMethod targetMethod = current.targetMethod();
            if (current.isTopFrame() || targetMethod == null || targetMethod.classMethodActor == null || targetMethod.classMethodActor() == javaLangReflectMethodInvoke.classMethodActor) {
                return true;
            }
            final ClassLoader cl = targetMethod.classMethodActor().holder().classLoader;
            if (cl != null && cl != BootClassLoader.BOOT_CLASS_LOADER) {
                result = cl;
                return false;
            }
            return true;
        }
    }

    public static ClassLoader LatestUserDefinedLoader() {
        LatestUserDefinedLoaderVisitor visitor = new LatestUserDefinedLoaderVisitor();
        new VmStackFrameWalker(VmThread.current().tla()).inspect(Pointer.fromLong(here()),
            VMRegister.getCpuStackPointer(),
            VMRegister.getCpuFramePointer(),
            visitor);
        return visitor.result;
    }

    @NEVER_INLINE
    public static Class GetCallerClass(int depth) {
        return Reflection.getCallerClass(depth + 1);
    }

    public static String GetSystemPackage(String name) {
        return BootClassLoader.BOOT_CLASS_LOADER.packageSource(name);
    }

    public static String[] GetSystemPackages() {
        return BootClassLoader.BOOT_CLASS_LOADER.packageNames();
    }

    public static void ArrayCopy(Object src, int srcPos, Object dest, int destPos, int length) {
        System.arraycopy(src, srcPos, dest, destPos, length);
    }

    public static Thread[] GetAllThreads() {
        return VmThreadMap.getThreads(false);
    }

    public static int[] GetThreadStateValues(int javaThreadState) {
        // 1-1
        final int[] result = new int[1];
        result[0] = javaThreadState;
        return result;
    }

    public static String[] GetThreadStateNames(int javaThreadState, int[] threadStateValues) {
        assert threadStateValues.length == 1;
        // 1-1
        final String[] result = new String[1];
        final Thread.State[] ts = Thread.State.values();
        result[0] = ts[javaThreadState].name();
        return result;
    }

    public static Properties InitAgentProperties(Properties props) {
        // sun.jvm.args, sun.jvm.flags, sun.java.command
        props.put("sun.jvm.args", VMOptions.getVmArguments());
        props.put("sun.jvm.flags", "");
        props.put("sun.java.command", VMOptions.mainClassAndArguments());
        return props;
    }

    // Checkstyle: resume method name check
}
