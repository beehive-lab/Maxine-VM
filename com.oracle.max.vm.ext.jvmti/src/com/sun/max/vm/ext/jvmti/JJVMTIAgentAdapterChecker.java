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

import static com.sun.max.vm.ext.jvmti.JVMTICapabilities.E.*;
import static com.sun.max.vm.ext.jvmti.JVMTIConstants.*;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;

import com.sun.max.annotate.*;
import com.sun.max.ide.*;
import com.sun.max.io.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.ext.jvmti.JVMTICapabilities.E;
import com.sun.max.vm.hosted.*;
import com.sun.max.vm.runtime.*;

/**
 * Handles the checking (e.g. capabilities) for agent adapters. The idiom is to subclass this class and call {@code super.method(...)}.
 * Those methods that require a specific capability (as denoted by the {@link JJVMTI_FUNCTION} annotation in {@link JJVMTI}
 * have the check generated automatically in this code. Those that don't, have a null implementation that will be inlined
 * away in the boot image code.
 */
public class JJVMTIAgentAdapterChecker implements JJVMTI {

    static {
        JavaPrototype.registerGeneratedCodeCheckerCallback(new GeneratedCodeCheckerCallback());
    }

    @HOSTED_ONLY
    private static class GeneratedCodeCheckerCallback implements JavaPrototype.GeneratedCodeCheckerCallback {

        public void checkGeneratedCode() {
            boolean updated = checkGeneratedCode(true);
            if (updated) {
                FatalError.unexpected("JJVMTIAgentAdapterChecker is out of sync with JJVMTI, regenerate and refresh in IDE");
            }
        }

        private static class SortableMethod implements Comparable<SortableMethod> {
            final Method method;

            SortableMethod(Method method) {
                this.method = method;
            }

            @Override
            public int compareTo(SortableMethod o) {
                return method.getName().compareTo(o.method.getName());
            }

        }

        boolean checkGeneratedCode(boolean checkOnly) {
            try {
                File base = new File(JavaProject.findWorkspace(), "com.oracle.max.vm.ext.jvmti" + File.separator + "src");
                File outputFile = new File(base, JJVMTIAgentAdapterChecker.class.getName().replace('.', File.separatorChar) + ".java").getAbsoluteFile();
                Writer writer = new StringWriter();
                PrintWriter out = new PrintWriter(writer);
                Method[] methods = JJVMTI.class.getDeclaredMethods();
                SortableMethod[] sortedMethods = new SortableMethod[methods.length];
                for (int i = 0; i < methods.length; i++) {
                    sortedMethods[i] = new SortableMethod(methods[i]);
                }
                Arrays.sort(sortedMethods);
                for (SortableMethod sm : sortedMethods) {
                    Method m = sm.method;
                    Class<?> returnType = m.getReturnType();
                    out.println("    @Override");
                    out.printf("    public %s %s(", getTypeName(m.getReturnType(), m.getGenericReturnType()), m.getName());
                    Class< ? >[] params = m.getParameterTypes();
                    Type[] genericParams = m.getGenericParameterTypes();
                    int argIndex = 0;
                    for (Class param : params) {
                        if (argIndex > 0) {
                            out.print(", ");
                        }
                        out.printf("%s arg%d", getTypeName(param, genericParams[argIndex]), argIndex);
                        argIndex++;
                    }
                    out.println(") {");
                    JJVMTI_FUNCTION capAnnotation = m.getAnnotation(JJVMTI_FUNCTION.class);
                    if (capAnnotation != null) {
                        out.printf("        checkCap(%s);%n", JVMTICapabilities.E.VALUES[capAnnotation.cap().ordinal()]);
                    }
                    if (returnType != void.class) {
                        out.print("        return ");
                        if (returnType == boolean.class) {
                            out.print("false");
                        } else if (returnType == int.class || returnType == long.class) {
                            out.print("0");
                        } else if (returnType == float.class) {
                            out.print("0.0F");
                        } else if (returnType == double.class) {
                            out.print("0.0");
                        } else {
                            out.print("null");
                        }
                        out.println(";");
                    }
                    out.println("    }\n");
                }
                writer.close();
                boolean wouldUpdate = Files.updateGeneratedContent(outputFile, ReadableSource.Static.fromString(writer.toString()),
                                "// START GENERATED CODE", "// END GENERATED CODE", checkOnly);
                return wouldUpdate;
            } catch (Exception exception) {
                FatalError.unexpected("Error while generating source for " + JJVMTIAgentAdapterChecker.class.getName(), exception);
                return false;
            }

        }

        private static String getTypeName(Class<?> returnClass, Type returnType) {
            if (returnType == returnClass) {
                return returnClass.getSimpleName();
            } else {
                ParameterizedType pType = (ParameterizedType) returnType;
                Type[] pTypeArgs = pType.getActualTypeArguments();
                Class<?> pTypeArg0Class = (Class) pTypeArgs[0];
                return returnClass.getSimpleName() + "<" + pTypeArg0Class.getSimpleName() + ">";
            }
        }

    }

    @HOSTED_ONLY
    public static void main(String[] args) {
        boolean checkOnly = args.length > 0 && args[0].equals("-check");
        boolean wouldUpdate = new GeneratedCodeCheckerCallback().checkGeneratedCode(checkOnly);
        if (wouldUpdate) {
            System.out.println("JJVMTIAgentAdapterChecker " + (checkOnly ? "would be" : "was") + " updated");
        }
    }

    protected JVMTI.JavaEnv env;

    private void checkCap(JVMTICapabilities.E cap) {
        if (!env.capabilities.contains(cap)) {
            throw new JJVMTI.JJVMTIException(JVMTI_ERROR_MUST_POSSESS_CAPABILITY);
        }
    }


// START GENERATED CODE
    @Override
    public void addCapabilities(EnumSet<E> arg0) {
    }

    @Override
    public void addToBootstrapClassLoaderSearch(String arg0) {
    }

    @Override
    public void addToSystemClassLoaderSearch(String arg0) {
    }

    @Override
    public void clearBreakpoint(ClassMethodActor arg0, long arg1) {
        checkCap(CAN_GENERATE_BREAKPOINT_EVENTS);
    }

    @Override
    public void clearFieldAccessWatch(FieldActor arg0) {
        checkCap(CAN_GENERATE_FIELD_ACCESS_EVENTS);
    }

    @Override
    public void clearFieldModificationWatch(FieldActor arg0) {
        checkCap(CAN_GENERATE_FIELD_MODIFICATION_EVENTS);
    }

    @Override
    public void disposeEnvironment() {
    }

    @Override
    public void forceEarlyReturnDouble(Thread arg0, double arg1) {
        checkCap(CAN_FORCE_EARLY_RETURN);
    }

    @Override
    public void forceEarlyReturnFloat(Thread arg0, float arg1) {
        checkCap(CAN_FORCE_EARLY_RETURN);
    }

    @Override
    public void forceEarlyReturnInt(Thread arg0, int arg1) {
        checkCap(CAN_FORCE_EARLY_RETURN);
    }

    @Override
    public void forceEarlyReturnLong(Thread arg0, long arg1) {
        checkCap(CAN_FORCE_EARLY_RETURN);
    }

    @Override
    public void forceEarlyReturnObject(Thread arg0, Object arg1) {
        checkCap(CAN_FORCE_EARLY_RETURN);
    }

    @Override
    public void forceEarlyReturnVoid(Thread arg0) {
        checkCap(CAN_FORCE_EARLY_RETURN);
    }

    @Override
    public void forceGarbageCollection() {
    }

    @Override
    public StackInfo[] getAllStackTraces(int arg0) {
        return null;
    }

    @Override
    public Thread[] getAllThreads() {
        return null;
    }

    @Override
    public int getArgumentsSize(ClassMethodActor arg0) {
        return 0;
    }

    @Override
    public int getAvailableProcessors() {
        return 0;
    }

    @Override
    public byte[] getBytecodes(ClassMethodActor arg0) {
        checkCap(CAN_GET_BYTECODES);
        return null;
    }

    @Override
    public EnumSet<E> getCapabilities() {
        return null;
    }

    @Override
    public FieldActor[] getClassFields(ClassActor arg0) {
        return null;
    }

    @Override
    public ClassLoader getClassLoader(ClassActor arg0) {
        return null;
    }

    @Override
    public ClassActor[] getClassLoaderClasses(ClassLoader arg0) {
        return null;
    }

    @Override
    public MethodActor[] getClassMethods(ClassActor arg0) {
        return null;
    }

    @Override
    public int getClassModifiers(ClassActor arg0) {
        return 0;
    }

    @Override
    public String getClassSignature(ClassActor arg0) {
        return null;
    }

    @Override
    public int getClassStatus(ClassActor arg0) {
        return 0;
    }

    @Override
    public ClassVersionInfo getClassVersionNumbers(ClassActor arg0) {
        return null;
    }

    @Override
    public byte[] getConstantPool(ClassActor arg0) {
        checkCap(CAN_GET_CONSTANT_POOL);
        return null;
    }

    @Override
    public Object getCurrentContendedMonitor(Thread arg0) {
        checkCap(CAN_GET_CURRENT_CONTENDED_MONITOR);
        return null;
    }

    @Override
    public Thread getCurrentThread() {
        return null;
    }

    @Override
    public long getCurrentThreadCpuTime() {
        return 0;
    }

    @Override
    public Object getEnvironmentLocalStorage() {
        return null;
    }

    @Override
    public String getErrorName(int arg0) {
        return null;
    }

    @Override
    public ClassActor getFieldDeclaringClass(FieldActor arg0) {
        return null;
    }

    @Override
    public int getFieldModifiers(FieldActor arg0) {
        return 0;
    }

    @Override
    public String getFieldName(FieldActor arg0) {
        return null;
    }

    @Override
    public String getFieldSignature(FieldActor arg0) {
        return null;
    }

    @Override
    public int getFrameCount(Thread arg0) {
        return 0;
    }

    @Override
    public FrameInfo getFrameLocation(Thread arg0, int arg1) {
        return null;
    }

    @Override
    public ClassActor[] getImplementedInterfaces(ClassActor arg0) {
        return null;
    }

    @Override
    public int getJLocationFormat() {
        return 0;
    }

    @Override
    public LineNumberEntry[] getLineNumberTable(ClassMethodActor arg0) {
        checkCap(CAN_GET_LINE_NUMBERS);
        return null;
    }

    @Override
    public ClassActor[] getLoadedClasses() {
        return null;
    }

    @Override
    public double getLocalDouble(Thread arg0, int arg1, int arg2) {
        checkCap(CAN_ACCESS_LOCAL_VARIABLES);
        return 0.0;
    }

    @Override
    public float getLocalFloat(Thread arg0, int arg1, int arg2) {
        checkCap(CAN_ACCESS_LOCAL_VARIABLES);
        return 0.0F;
    }

    @Override
    public int getLocalInt(Thread arg0, int arg1, int arg2) {
        checkCap(CAN_ACCESS_LOCAL_VARIABLES);
        return 0;
    }

    @Override
    public long getLocalLong(Thread arg0, int arg1, int arg2) {
        checkCap(CAN_ACCESS_LOCAL_VARIABLES);
        return 0;
    }

    @Override
    public Object getLocalObject(Thread arg0, int arg1, int arg2) {
        checkCap(CAN_ACCESS_LOCAL_VARIABLES);
        return null;
    }

    @Override
    public LocalVariableEntry[] getLocalVariableTable(ClassMethodActor arg0) {
        checkCap(CAN_ACCESS_LOCAL_VARIABLES);
        return null;
    }

    @Override
    public int getMaxLocals(ClassMethodActor arg0) {
        return 0;
    }

    @Override
    public ClassActor getMethodDeclaringClass(MethodActor arg0) {
        return null;
    }

    @Override
    public String getMethodGenericSignature(MethodActor arg0) {
        return null;
    }

    @Override
    public MethodLocation getMethodLocation(ClassMethodActor arg0) {
        return null;
    }

    @Override
    public int getMethodModifiers(MethodActor arg0) {
        return 0;
    }

    @Override
    public String getMethodName(MethodActor arg0) {
        return null;
    }

    @Override
    public String getMethodSignature(MethodActor arg0) {
        return null;
    }

    @Override
    public int getObjectHashCode(Object arg0) {
        return 0;
    }

    @Override
    public ObjectMonitorUsage getObjectMonitorUsage(Object arg0) {
        checkCap(CAN_GET_MONITOR_INFO);
        return null;
    }

    @Override
    public long getObjectSize(Object arg0) {
        return 0;
    }

    @Override
    public void getOwnedMonitorInfo(Thread arg0) {
        checkCap(CAN_GET_OWNED_MONITOR_INFO);
    }

    @Override
    public MonitorStackDepthInfo[] getOwnedMonitorStackDepthInfo(Thread arg0) {
        checkCap(CAN_GET_OWNED_MONITOR_STACK_DEPTH_INFO);
        return null;
    }

    @Override
    public int getPhase() {
        return 0;
    }

    @Override
    public EnumSet<E> getPotentialCapabilities() {
        return null;
    }

    @Override
    public String getSourceDebugExtension(ClassActor arg0) {
        return null;
    }

    @Override
    public String getSourceFileName(ClassActor arg0) {
        checkCap(CAN_GET_SOURCE_FILE_NAME);
        return null;
    }

    @Override
    public FrameInfo[] getStackTrace(Thread arg0, int arg1, int arg2) {
        return null;
    }

    @Override
    public String[] getSystemProperties() {
        return null;
    }

    @Override
    public String getSystemProperty(String arg0) {
        return null;
    }

    @Override
    public Object getTag(Object arg0) {
        checkCap(CAN_TAG_OBJECTS);
        return null;
    }

    @Override
    public long getThreadCpuTime(Thread arg0) {
        return 0;
    }

    @Override
    public ThreadGroupChildrenInfo getThreadGroupChildren(ThreadGroup arg0) {
        return null;
    }

    @Override
    public ThreadGroupInfo getThreadGroupInfo(ThreadGroup arg0) {
        return null;
    }

    @Override
    public ThreadInfo getThreadInfo(Thread arg0) {
        return null;
    }

    @Override
    public StackInfo[] getThreadListStackTraces(Thread[] arg0, int arg1) {
        return null;
    }

    @Override
    public Object getThreadLocalStorage(Thread arg0) {
        return null;
    }

    @Override
    public int getThreadState(Thread arg0) {
        return 0;
    }

    @Override
    public long getTime() {
        return 0;
    }

    @Override
    public ThreadGroup[] getTopThreadGroups() {
        return null;
    }

    @Override
    public int getVersionNumber() {
        return 0;
    }

    @Override
    public void interruptThread(Thread arg0) {
        checkCap(CAN_SIGNAL_THREAD);
    }

    @Override
    public boolean isArrayClass(ClassActor arg0) {
        return false;
    }

    @Override
    public boolean isFieldSynthetic(FieldActor arg0) {
        checkCap(CAN_GET_SYNTHETIC_ATTRIBUTE);
        return false;
    }

    @Override
    public boolean isInterface(ClassActor arg0) {
        return false;
    }

    @Override
    public boolean isMethodNative(MethodActor arg0) {
        return false;
    }

    @Override
    public boolean isMethodObsolete(MethodActor arg0) {
        return false;
    }

    @Override
    public boolean isMethodSynthetic(MethodActor arg0) {
        checkCap(CAN_GET_SYNTHETIC_ATTRIBUTE);
        return false;
    }

    @Override
    public boolean isModifiableClass(ClassActor arg0) {
        return false;
    }

    @Override
    public void iterateThroughHeap(int arg0, ClassActor arg1, HeapCallbacks arg2, Object arg3) {
        checkCap(CAN_TAG_OBJECTS);
    }

    @Override
    public void notifyFramePop(Thread arg0, int arg1) {
        checkCap(CAN_GENERATE_FRAME_POP_EVENTS);
    }

    @Override
    public void popFrame(Thread arg0) {
        checkCap(CAN_POP_FRAME);
    }

    @Override
    public void redefineClasses(ClassDefinition[] arg0) {
        checkCap(CAN_REDEFINE_CLASSES);
    }

    @Override
    public void relinquishCapabilities(EnumSet<E> arg0) {
    }

    @Override
    public void resumeThread(Thread arg0) {
        checkCap(CAN_SUSPEND);
    }

    @Override
    public int[] resumeThreadList(Thread[] arg0) {
        checkCap(CAN_SUSPEND);
        return null;
    }

    @Override
    public void retransformClasses(ClassActor[] arg0) {
    }

    @Override
    public void runAgentThread(Thread arg0, int arg1) {
    }

    @Override
    public void setBreakpoint(ClassMethodActor arg0, long arg1) {
        checkCap(CAN_GENERATE_BREAKPOINT_EVENTS);
    }

    @Override
    public void setEnvironmentLocalStorage(Object arg0) {
    }

    @Override
    public void setEventNotificationMode(int arg0, int arg1, Thread arg2) {
    }

    @Override
    public void setFieldAccessWatch(FieldActor arg0) {
        checkCap(CAN_GENERATE_FIELD_ACCESS_EVENTS);
    }

    @Override
    public void setFieldModificationWatch(FieldActor arg0) {
        checkCap(CAN_GENERATE_FIELD_MODIFICATION_EVENTS);
    }

    @Override
    public void setLocalDouble(Thread arg0, int arg1, int arg2, double arg3) {
        checkCap(CAN_ACCESS_LOCAL_VARIABLES);
    }

    @Override
    public void setLocalFloat(Thread arg0, int arg1, int arg2, float arg3) {
        checkCap(CAN_ACCESS_LOCAL_VARIABLES);
    }

    @Override
    public void setLocalInt(Thread arg0, int arg1, int arg2, int arg3) {
        checkCap(CAN_ACCESS_LOCAL_VARIABLES);
    }

    @Override
    public void setLocalLong(Thread arg0, int arg1, int arg2, long arg3) {
        checkCap(CAN_ACCESS_LOCAL_VARIABLES);
    }

    @Override
    public void setLocalObject(Thread arg0, int arg1, int arg2, Object arg3) {
        checkCap(CAN_ACCESS_LOCAL_VARIABLES);
    }

    @Override
    public void setNativeMethodPrefix(String arg0) {
        checkCap(CAN_SET_NATIVE_METHOD_PREFIX);
    }

    @Override
    public void setNativeMethodPrefixes(String[] arg0) {
        checkCap(CAN_SET_NATIVE_METHOD_PREFIX);
    }

    @Override
    public void setSystemProperty(String arg0, String arg1) {
    }

    @Override
    public void setTag(Object arg0, Object arg1) {
        checkCap(CAN_TAG_OBJECTS);
    }

    @Override
    public void setThreadLocalStorage(Thread arg0, Object arg1) {
    }

    @Override
    public void setVerboseFlag(int arg0, boolean arg1) {
    }

    @Override
    public void stopThread(Thread arg0, Throwable arg1) {
        checkCap(CAN_SIGNAL_THREAD);
    }

    @Override
    public void suspendThread(Thread arg0) {
        checkCap(CAN_SUSPEND);
    }

    @Override
    public int[] suspendThreadList(Thread[] arg0) {
        checkCap(CAN_SUSPEND);
        return null;
    }

// END GENERATED CODE
}
