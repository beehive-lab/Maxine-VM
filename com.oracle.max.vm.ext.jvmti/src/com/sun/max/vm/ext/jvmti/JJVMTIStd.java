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

import java.lang.reflect.*;

/**
 * A functionally equivalent <A href="http://docs.oracle.com/javase/6/docs/platform/jvmti/jvmti.html">JVMTI</a> interface
 * but cast in terms of standard Java types that can be called from agents written in Java (for Maxine).
 *
 * Some of the JVMTI functions are redundant in that they essentially replicate existing functionality in the JDK,
 * however, we include them for completeness.
 *
 * A few of the JVMTI functions don't have a Java equivalent, and these are omitted. Raw monitors are
 * unnecessary as agents can use standard Java synchronization mechanisms.
 *
 * Whereas native JVMTI returns errors as the function result, {@link JJVMTIStd} throws a {@link JJVMTIException}.
 *
 * There is an issue with functions that take method ids as arguments in the native interface. These cannot simply
 * be replaced by {@link Method}, since they also apply to {@link Constructor}>. Unfortunately the closest common
 * supertype is {@link Member}, which also includes {@link Field}>. Nevertheless, we use {@link Member} where appropriate
 * and the implementation should perform a runtime check.
 *
 */
public interface JJVMTIStd extends JJVMTICommon {

    /**
     * See <a href="http://docs.oracle.com/javase/6/docs/platform/jvmti/jvmti.html#jvmtiFrameInfo">jvmtiFrameInfo</a>.
     */
    public static class FrameInfoStd extends FrameInfo {
        public final Member method;

        public FrameInfoStd(Member method, int location) {
            super(location);
            this.method = method;
        }
    }

    public static class ClassDefinition {
        public final Class klass;
        public final byte[] classBytes;

        public ClassDefinition(Class klass, byte[] classBytes) {
            this.klass = klass;
            this.classBytes = classBytes;
        }
    }

    public interface EventCallbacksStd extends EventCallbacks {
        void breakpoint(Thread thread, Member method, long location);
        /**
         * There is no CLASS_PREPARE event as Maxine cannot usefully distinguish it from CLASS_LOAD.
          */
        void classLoad(Thread thread, Class klass);
        void methodEntry(Thread thread, Member method);
        void methodExit(Thread thread, Member method, boolean exeception, Object returnValue);
    }

    /*
     * Methods that can be called from the JJVMTI agent. They correspond essentially 1-1 to the JVMTI native
     * interface functions.
     */

    void setBreakpoint(Member method, long location) throws JJVMTIException;
    void clearBreakpoint(Member method, long location) throws JJVMTIException;
    void setFieldAccessWatch(Field field) throws JJVMTIException;
    void clearFieldAccessWatch(Field field) throws JJVMTIException;
    void setFieldModificationWatch(Field field) throws JJVMTIException;
    void clearFieldModificationWatch(Field field) throws JJVMTIException;
    boolean isModifiableClass(Class klass) throws JJVMTIException;
    String getClassSignature(Class klass) throws JJVMTIException;
    int getClassStatus(Class klass) throws JJVMTIException;
    String getSourceFileName(Class klass) throws JJVMTIException;
    int getClassModifiers(Class klass) throws JJVMTIException;
    Method[] getClassMethods(Class klass) throws JJVMTIException;
    Field[] getClassFields(Class klass) throws JJVMTIException;
    Class[] getImplementedInterfaces(Class klass) throws JJVMTIException;
    boolean isInterface(Class klass) throws JJVMTIException;
    boolean isArrayClass(Class klass) throws JJVMTIException;
    ClassLoader getClassLoader(Class klass) throws JJVMTIException;
    String getFieldName(Field field) throws JJVMTIException;
    String getFieldSignature(Field field) throws JJVMTIException;
    Class getFieldDeclaringClass(Field field) throws JJVMTIException;
    int getFieldModifiers(Field field) throws JJVMTIException;
    boolean isFieldSynthetic(Field field) throws JJVMTIException;
    String getMethodName(Member method) throws JJVMTIException;
    String getMethodSignature(Member method) throws JJVMTIException;
    String getMethodGenericSignature(Member method) throws JJVMTIException;
    Class getMethodDeclaringClass(Member method) throws JJVMTIException;
    int getMethodModifiers(Member method) throws JJVMTIException;
    int getMaxLocals(Member method) throws JJVMTIException;
    int getArgumentsSize(Member method) throws JJVMTIException;
    LineNumberEntry[] getLineNumberTable(Member method) throws JJVMTIException;
    MethodLocation getMethodLocation(Member method) throws JJVMTIException;
    LocalVariableEntry[] getLocalVariableTable(Member member) throws JJVMTIException;
    byte[] getBytecodes(Member method, byte[] useThis) throws JJVMTIException;
    boolean isMethodNative(Method method) throws JJVMTIException;
    boolean isMethodSynthetic(Method method) throws JJVMTIException;
    Class[] getLoadedClasses() throws JJVMTIException;
    Class[] getClassLoaderClasses(ClassLoader loader) throws JJVMTIException;
    void redefineClasses(ClassDefinition[] classDefinitions) throws JJVMTIException;
    String getSourceDebugExtension(Class klass) throws JJVMTIException;
    boolean isMethodObsolete(Method method) throws JJVMTIException;
    ClassVersionInfo getClassVersionNumbers(Class klasss, ClassVersionInfo classVersionInfo) throws JJVMTIException;
    int getConstantPool(Class klass, byte[] pool) throws JJVMTIException;
    void retransformClasses(Class[] klasses) throws JJVMTIException;
}
