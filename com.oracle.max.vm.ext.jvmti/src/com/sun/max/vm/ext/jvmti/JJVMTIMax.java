/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.ext.jvmti;

import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.ext.jvmti.JJVMTICommon.*;

/**
 * A functionally equivalent <A href="http://docs.oracle.com/javase/6/docs/platform/jvmti/jvmti.html">JVMTI</a> interface
 * but cast in terms of Maxine Java types that can be called from agents written in Java (for Maxine). This
 * interface is almost identical to {@link JJVMTIStd} but uses Maxine internal actor types, e.g. {@link MethodActor} instead
 * of {@link MethodActor}.
 *
 * Some of the JVMTI functions are redundant in that they essentially replicate existing functionality in the JDK,
 * however, we include them for completeness.
 *
 * A few of the JVMTI functions don't have a Java equivalent, and these are omitted. Raw monitors are
 * unnecessary as agents can use standard Java synchronization mechanisms.
 *
 * Whereas native JVMTI returns errors as the function result, {@link JJVMTIMax} throws a {@link JJVMTIException}.
 */
public interface JJVMTIMax extends JJVMTICommon {

    /**
     * See <a href="http://docs.oracle.com/javase/6/docs/platform/jvmti/jvmti.html#jvmtiFrameInfo">jvmtiFrameInfo</a>.
     */
    public static class FrameInfoMax extends FrameInfo {
        public final MethodActor method;

        public FrameInfoMax(MethodActor method, int location) {
            super(location);
            this.method = method;
        }
    }

    public static class ClassDefinition {
        public final ClassActor klass;
        public final byte[] classBytes;

        public ClassDefinition(ClassActor klass, byte[] classBytes) {
            this.klass = klass;
            this.classBytes = classBytes;
        }
    }

    public interface EventCallbacksMax extends EventCallbacks {
        /*
         * Event callbacks with reflection API arguments.
         */

        void breakpoint(Thread thread, MethodActor method, long location);
        /**
         * There is no CLASS_PREPARE event as Maxine cannot usefully distinguish it from CLASS_LOAD.
          */
        void classLoad(Thread thread, ClassActor klass);
        void methodEntry(Thread thread, MethodActor method);
        void methodExit(Thread thread, MethodActor method, boolean exeception, Object returnValue);
        void fieldAccess(Thread thread, MethodActor method, long location, ClassActor classActor, Object object, FieldActor field);
        void fieldModification(Thread thread, MethodActor method, long location, ClassActor classActor, Object object, FieldActor field, Object newValue);
    }

    /*
     * MethodActors that can be called from the JJVMTI agent. They correspond essentially 1-1 to the JVMTI native
     * interface functions.
     */

    void setBreakpoint(MethodActor method, long location) throws JJVMTIException;
    void clearBreakpoint(MethodActor method, long location) throws JJVMTIException;
    void setFieldAccessWatch(FieldActor field) throws JJVMTIException;
    void clearFieldAccessWatch(FieldActor field) throws JJVMTIException;
    void setFieldModificationWatch(FieldActor field) throws JJVMTIException;
    void clearFieldModificationWatch(FieldActor field) throws JJVMTIException;
    boolean isModifiableClassActor(ClassActor klass) throws JJVMTIException;
    String getClassActorSignature(ClassActor klass) throws JJVMTIException;
    int getClassActorStatus(ClassActor klass) throws JJVMTIException;
    String getSourceFileName(ClassActor klass) throws JJVMTIException;
    int getClassActorModifiers(ClassActor klass) throws JJVMTIException;
    MethodActor[] getClassActorMethodActors(ClassActor klass) throws JJVMTIException;
    FieldActor[] getClassActorFieldActors(ClassActor klass) throws JJVMTIException;
    ClassActor[] getImplementedInterfaces(ClassActor klass) throws JJVMTIException;
    boolean isInterface(ClassActor klass) throws JJVMTIException;
    boolean isArrayClassActor(ClassActor klass) throws JJVMTIException;
    ClassLoader getClassLoader(ClassActor klass) throws JJVMTIException;
    String getFieldActorName(FieldActor field) throws JJVMTIException;
    String getFieldActorSignature(FieldActor field) throws JJVMTIException;
    ClassActor getFieldActorDeclaringClassActor(FieldActor field) throws JJVMTIException;
    int getFieldActorModifiers(FieldActor field) throws JJVMTIException;
    boolean isFieldActorSynthetic(FieldActor field) throws JJVMTIException;
    String getMethodActorName(MethodActor method) throws JJVMTIException;
    String getMethodActorSignature(MethodActor method) throws JJVMTIException;
    String getMethodActorGenericSignature(MethodActor method) throws JJVMTIException;
    ClassActor getMethodActorDeclaringClassActor(MethodActor method) throws JJVMTIException;
    int getMethodActorModifiers(MethodActor method) throws JJVMTIException;
    int getMaxLocals(MethodActor method) throws JJVMTIException;
    int getArgumentsSize(MethodActor method) throws JJVMTIException;
    LineNumberEntry[] getLineNumberTable(MethodActor method) throws JJVMTIException;
    MethodLocation getMethodActorLocation(MethodActor method) throws JJVMTIException;
    LocalVariableEntry[] getLocalVariableTable(MethodActor member) throws JJVMTIException;
    byte[] getBytecodes(MethodActor method, byte[] useThis) throws JJVMTIException;
    boolean isMethodActorNative(MethodActor method) throws JJVMTIException;
    boolean isMethodActorSynthetic(MethodActor method) throws JJVMTIException;
    ClassActor[] getLoadedClasses() throws JJVMTIException;
    ClassActor[] getClassLoaderClasses(ClassLoader loader) throws JJVMTIException;
    void redefineClasses(ClassDefinition[] classDefinitions) throws JJVMTIException;
    String getSourceDebugExtension(ClassActor klass) throws JJVMTIException;
    boolean isMethodActorObsolete(MethodActor method) throws JJVMTIException;
    ClassVersionInfo getClassVersionNumbers(ClassActor klasss, ClassVersionInfo classVersionInfo) throws JJVMTIException;
    int getConstantPool(ClassActor klass, byte[] pool) throws JJVMTIException;
    void retransformClasses(ClassActor[] klasses) throws JJVMTIException;
}
