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
package com.oracle.max.vm.ext.jjvmti.agents.apitest;

import static com.sun.max.vm.ext.jvmti.JVMTIConstants.*;
import static com.sun.max.vm.ext.jvmti.JVMTIEvents.*;

import java.lang.reflect.*;
import java.util.*;

import com.oracle.max.vm.ext.jjvmti.agents.util.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.ext.jvmti.*;
import com.sun.max.vm.run.java.*;
import com.sun.max.vm.thread.*;

/**
 * A {@link JJVMTI Java JVMTI agent} that attempts to test as much of the
 * interface as possible. Uses reflection to determine the API details and then constructs
 * a test of each function. It can also be used to execute a specific test.
 *
 * Can be included in the boot image or dynamically loaded as a VM extension.
 * In the former case arguments are passed using the {@code -XX:APITestArgs} command line option;
 * in the latter case as part of the {@code -vmextension} option.
 *
 * Argument syntax: verbose,auto=blacklist,bpt=class.method,function1=args,function2=args,...
 *
 * In order to get meaningful tests, we need to stop the VM at an interesting point before
 * invoking the functions. We do that by setting a breakpoint at a user-specified method.
 * So breakpoints are tested implicitly. If unspecified, we find the main class and set a breakpoint there.
 *
 * {@code auto} and {@code function} are alternates. In the latter case the specific functions are tested
 * with the given arguments; in the former all functions are tested automatically. The optional {@code blacklist} is a
 * colon separated list of functions to ignore in auto mode.
 *
 * Conventions for arguments:
 *
 * <ul>
 * <li>{@code Thread}: # denotes the current thread, otherwise the thread is searched for by name.</li>
 * <li>{@code Thread[]}: {@code Name1/Name2/...}
 * <li>{@code ClassActor}: # denotes the class that declares the method where the breakpoint occurs, else class is searched for by name.</li>
 * <li>{@code MethodActor}: # denotes the method where the breakpoint occurs, else "class.method" is searched for.</li>
 * <li>{@code FieldActor}: name must be specified. Unqualified is refers to a field in this class, else "class.field" is searched for.</li>
 * </ul>
 *
 */
public class APITest extends NullJJVMTICallbacks {
    private static APITest apiTest;
    private static String APITestArgs;
    private static TestThread testThread1;
    private static TestThread testThread2;
    private static DeadThread deadThread;
    private static boolean verbose;
    private static String taggableObject;

    static {
        apiTest = (APITest) JJVMTIAgentAdapter.register(new APITest());
        if (MaxineVM.isHosted()) {
            VMOptions.addFieldOption("-XX:", "APITestArgs", "arguments for API test JJVMTI agent");
        }
    }

    /**
     * Used for {@link JJVMTI#runAgentThread}.
     */
    private static class AgentThread extends Thread {
        AgentThread() {
            super();
            setName("APITestAgent");
        }

        @Override
        public void run() {
            System.out.println("agent thread running");
            while (true) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ex) {

                }
            }
        }
    }

    /**
     * Adds another thread to the mix and used for {@link JJVMTI#interruptThread(Thread)} etc.
     */
    private static class TestThread extends Thread {
        TestThread(int id) {
            super();
            setDaemon(true);
            setName("APITest" + id);
        }

        @Override
        public void run() {
            System.out.printf("%s running%n", getName());
            while (true) {
                try {
                    Thread.sleep(5000);
                    System.out.println("test thread woken");
                } catch (InterruptedException ex) {
                    System.out.println("test thread interrupted");
                }
            }
        }
    }

    /**
     * A thread that dies to test alive status.
      */
    private static class DeadThread extends Thread {
        DeadThread() {
            super();
            setName("APIDead");
        }
    }

    private static class FunctionData {
        boolean enabled;
        Method method;
        boolean isVoid;
        Object[] args;
        Class<?>[] params;

        FunctionData(Method method, Object[] args, boolean enabled) {
            this.method = method;
            this.isVoid = method.getReturnType() == void.class;
            this.params = method.getParameterTypes();
            this.args = args;
            this.enabled = enabled;
        }

        void checkArgs(MethodActor bptMethod) {
            for (int i = 0; i < params.length; i++) {
                Class<?> param = params[i];
                if (param == Thread.class) {
                    if (method.getName().equals("runAgentThread")) {
                        args[i] = new AgentThread();
                    } else {
                        String threadName = (String) args[i];
                        if (threadName == null) {
                            if (method.getName().equals("interruptThread") ||
                                method.getName().equals("suspendThread") || method.getName().equals("resumeThread")) {
                                args[i] = testThread1;
                            } else {
                                args[i] = null; // implies current thread
                            }
                        } else if (threadName.equals(deadThread.getName())) {
                            args[i] = deadThread;
                        } else {
                            // try to find the thread by name
                            args[i] = findThread(threadName);
                        }
                    }
                } else if (param == Thread[].class) {
                    String[] threadNames = (String[]) args[i];
                    Thread[] threads = new Thread[threadNames.length];
                    for (int t = 0; t < threadNames.length; t++) {
                        threads[t] = findThread(threadNames[t]);
                    }
                    args[i] = threads;
                } else if (param == ThreadGroup.class) {
                    String threadGroupName = (String) args[i];
                    if (threadGroupName == null) {
                        args[i] = Thread.currentThread().getThreadGroup();
                    } else {
                        failExit("can't specify thread group by name");
                    }

                } else if (param == ClassActor.class) {
                    String className = (String) args[i];
                    if (className == null) {
                        // use the class of the method at breakpoint
                        args[i] = bptMethod.holder();
                    } else {
                        args[i] = findClass(className);
                    }

                } else if (MethodActor.class.isAssignableFrom(param)) {
                    String qualName = (String) args[i];
                    if (qualName == null) {
                        args[i] = bptMethod;
                    } else {
                        // class.method, no support for overloaded parameters
                        int ix = qualName.lastIndexOf('.');
                        if (ix <= 0) {
                            failExit("bad method syntax");
                        }
                        String methodName = qualName.substring(ix + 1);
                        String className = qualName.substring(0, ix);
                        Method method = null;
                        Class< ? > klass = findClass(className);
                        for (Method m : klass.getDeclaredMethods()) {
                            if (m.getName().equals(methodName)) {
                                method = m;
                                break;
                            }
                        }
                        if (method == null) {
                            failExit("can't find method '" + className + "." + methodName + "'");
                        }
                        args[i] = MethodActor.fromJava(method);
                    }
                } else if (param == FieldActor.class) {
                    String qualName = (String) args[i];
                    String className = null;
                    String fieldName = null;
                    if (qualName == null) {
                        // use this class
                        className = APITest.class.getName();
                        fieldName = "bptData";
                    } else {
                        // [class.]field, no class means this class
                        int ix = qualName.lastIndexOf('.');
                        if (ix <= 0) {
                            className = APITest.class.getName();
                            fieldName = qualName;
                        } else {
                            className = qualName.substring(0, ix);
                            fieldName = qualName.substring(ix + 1);
                        }
                    }
                    Field field = null;
                    Class< ? > klass = findClass(className);
                    for (Field f : klass.getDeclaredFields()) {
                        if (f.getName().equals(fieldName)) {
                            field = f;
                            break;
                        }
                    }
                    if (field == null) {
                        failExit("can't find field '" + className + "." + fieldName + "'");
                    }
                    args[i] = FieldActor.fromJava(field);
                } else if (param == Object.class) {
                    args[i] = taggableObject;
                }
            }
        }

        private Thread findThread(String threadName) {
            Thread[] threads = VmThreadMap.getThreads(false);
            for (Thread thread : threads) {
                if (thread.getName().equals(threadName)) {
                    return thread;
                }
            }
            failExit("can't find thread '" + threadName + "'");
            return null;
        }

        private static Class<?> findClass(String className) {
            try {
                return Class.forName(className);
            } catch (Throwable ex) {
                failExit("can't find class '" + className + "'");
                return null;
            }
        }

        /**
         * Method pairs that should be executed in a given order in auto mode.
         */
        private static final String[][] ORDERED_METHODS = new String[][] {{"setTag", "getTag"},
                                                                          {"suspendThread", "resumeThread"},
                                                                          {"suspendThreadList", "resumeThreadList"},
                                                                          {"setVerboseFlag", "forceGarbageCollection"}};

        static void orderMethods() {
            // for auto-testing we want to execute some functions before others
            for (String[] pair : ORDERED_METHODS) {
                int ix0 = findMethod(pair[0]);
                int ix1 = findMethod(pair[1]);
                if (ix0 > ix1) {
                    // wrong order
                    FunctionData fd1 = functionDataList.get(ix1);
                    functionDataList.set(ix1, functionDataList.get(ix0));
                    functionDataList.set(ix0, fd1);
                }
            }
        }

        static int findMethod(String name) {
            for (int i = 0; i < functionDataList.size(); i++) {
                if (name.equals(functionDataList.get(i).method.getName())) {
                    return i;
                }
            }
            failExit("failed to find method: " + name + " in functionDataList");
            return -1;
        }

    }

    private static class BptData {
        String className;
        String methodName;

        BptData(String className, String methodName) {
            this.className = className;
            this.methodName = methodName;
        }
    }

    private static ArrayList<FunctionData> functionDataList = new ArrayList<FunctionData>();

    private static BptData bptData;

    private static Map<MethodActor, LineNumberEntry[]> lntCache = new HashMap<MethodActor, LineNumberEntry[]>();

    /***
     * VM extension entry point.
     * @param args
     */
    public static void onLoad(String agentArgs) {
        APITestArgs = agentArgs;
        apiTest.onBoot();
    }

    /**
     * Boot image entry point.
     */
    @Override
    public void onBoot() {
        apiTest.setEventNotificationMode(JVMTI_ENABLE, E.VM_INIT, null);
    }

    @Override
    public void vmInit() {
        String bptMethod = null;
        String auto = null;
        if (APITestArgs != null) {
            String[] args = APITestArgs.split(",");
            for (int i = 0; i < args.length; i++) {
                String arg = args[i];
                if (arg.equals("verbose")) {
                    verbose = true;
                } else if (arg.equals("bpt")) {
                    bptMethod = getArgValue(arg, false);
                    if (bptMethod == null) {
                        failExit("usage: bpt=class.method");
                    }
                } else if (arg.startsWith("auto")) {
                    auto = getArgValue(arg, true);
                } else {
                    // everything else is a method=args pair
                    if (!validateFunction(arg)) {
                        failExit("unknown function: " + arg);
                    }
                }
            }
        }

        deadThread = new DeadThread();
        deadThread.start();

        testThread1 = new TestThread(1);
        testThread1.start();
        testThread2 = new TestThread(2);
        testThread2.start();

        taggableObject = new String("Taggable Object");

        if (auto != null) {
            autoGen(auto);
            FunctionData.orderMethods();
        }

        try {
            if (bptMethod == null) {
                bptMethod = JavaRunScheme.getMainClassName() + ".main";
            }
            bptData = tryDecodeBreakpoint(bptMethod);
            apiTest.setEventNotificationMode(JVMTI_ENABLE, E.CLASS_LOAD, null);
            apiTest.addCapabilities(EnumSet.of(JVMTICapabilities.E.CAN_GENERATE_BREAKPOINT_EVENTS));
            apiTest.setEventNotificationMode(JVMTI_ENABLE, E.BREAKPOINT, null);

        } catch (Exception ex) {
            failExit("vmInit: " + ex);
        }

    }

    @Override
    public void vmDeath() {
        System.out.println("VM_DEATH");
    }

    /**
     * Pseudo operation that can be used to sleep the tester between manually specified operations.
     * @param seconds
     */
    private void sleep(int seconds) {
        try {
            Thread.sleep(seconds * 1000);
        } catch (InterruptedException ex) {
        }
    }

    private static String getArgValue(String prefix, boolean nullOk) {
        int ix = prefix.indexOf('=');
        if (ix < 0) {
            return nullOk ? "" : null;
        }
        return prefix.substring(ix + 1);
    }

    @Override
    public void classLoad(Thread thread, ClassActor klass) {
        if (klass.qualifiedName().equals(bptData.className)) {
            MethodActor method = findBptMethod(klass);
            if (method != null) {
                setBreakpoint((ClassMethodActor) method, 0);
                setEventNotificationMode(JVMTI_DISABLE, E.CLASS_LOAD, null);
            } else {
                failExit("can't find method " + bptData.methodName + "in " + bptData.className);
            }
        }
    }

    private MethodActor findBptMethod(ClassActor klass) {
        List<MethodActor> methods = klass.getLocalMethodActors();
        for (MethodActor method : methods) {
            if (method.name().equals(bptData.methodName)) {
                return method;
            }
        }
        return null;
    }

    @Override
    public void breakpoint(Thread thread, MethodActor method, long location) {
        for (FunctionData fd : functionDataList) {
            if (fd.enabled) {
                try {
                    fd.checkArgs(method);
                    printInvoke(fd);
                    Object result = fd.method.invoke(this, fd.args);
                    decodeResult(fd, result);
                } catch (InvocationTargetException ex) {
                    System.out.println(", exception:" + ex.getCause());
                } catch (Throwable ex) {
                    System.out.println(ex);
                }
            }
        }
    }

    private static void printInvoke(FunctionData fd) {
        System.out.print("Invoking " + fd.method.getName() + "(");
        for (int i = 0; i < fd.args.length; i++) {
            Object arg = fd.args[i];
            if (i != 0) {
                System.out.print(", ");
            }
            if (arg instanceof String) {
                System.out.printf("\"%s\"", arg);
            } else {
                System.out.print(arg);
            }
        }
        System.out.print(")");
    }

    private void decodeResult(FunctionData fd, Object result) {
        System.out.print(", ok: ");
        if (fd.isVoid) {
            System.out.println("void result");
            return;
        }
        if (result instanceof Thread) {
            System.out.println(result);
        } else if (result instanceof Thread[]) {
            System.out.println();
            Thread[] threads = (Thread[]) result;
            for (Thread thread : threads) {
                System.out.println(thread);
            }
        } else if (result instanceof ThreadGroup[]) {
            System.out.println();
            ThreadGroup[] threadGroups = (ThreadGroup[]) result;
            for (ThreadGroup threadGroup : threadGroups) {
                System.out.println(threadGroup);
            }
        } else if (result instanceof ThreadInfo) {
            ThreadInfo threadInfo = (ThreadInfo) result;
            System.out.printf("%s %d %b %s %s%n", threadInfo.name, threadInfo.priority, threadInfo.isDaemon,
                            threadInfo.threadGroup, threadInfo.contextClassLoader);
        } else if (result instanceof ThreadGroupInfo) {
            ThreadGroupInfo threadGroupInfo = (ThreadGroupInfo) result;
            System.out.printf("%s %d %b%n", threadGroupInfo.name, threadGroupInfo.maxPriority, threadGroupInfo.isDaemon);
        } else if (result instanceof ThreadGroupChildrenInfo) {
            ThreadGroupChildrenInfo threadGroupChildrenInfo = (ThreadGroupChildrenInfo) result;
            System.out.printf("threads.length %d, groups.length %d%n", threadGroupChildrenInfo.threads.length, threadGroupChildrenInfo.groups.length);
        } else if (result instanceof StackInfo[]) {
            System.out.println();
            StackInfo[] stackInfo = (StackInfo[]) result;
            for (StackInfo si : stackInfo) {
                System.out.printf("%s%n", si.thread);
                for (int i = 0; i < si.frameCount; i++) {
                    FrameInfo fi = si.frameInfo[i];
                    System.out.printf("  %s %d%n", fi.method.format("%H.%n"), mapLocation(fi.method, fi.location));
                }
            }
        } else if (result instanceof FrameInfo[]) {
            System.out.println();
            FrameInfo[] frameInfoArray = (FrameInfo[]) result;
            for (int i = 0; i < frameInfoArray.length; i++) {
                FrameInfo fi = frameInfoArray[i];
                System.out.printf("  %s %d%n", fi.method.format("%H.%n"), mapLocation(fi.method, fi.location));
            }
        } else if (result instanceof FrameInfo) {
            FrameInfo fi = (FrameInfo) result;
            System.out.printf("  %s %d%n", fi.method.format("%H.%n"), mapLocation(fi.method, fi.location));
        } else if (result instanceof EnumSet<?>) {
            @SuppressWarnings("unchecked")
            EnumSet<JVMTICapabilities.E> caps = (EnumSet<JVMTICapabilities.E>) result;
            Iterator<JVMTICapabilities.E> iter = caps.iterator();
            while (iter.hasNext()) {
                JVMTICapabilities.E cap = iter.next();
                System.out.print(cap.name());
                System.out.print(" ");
            }
            System.out.println();
        } else if (result instanceof ClassActor[]) {
            ClassActor[] classes = (ClassActor[]) result;
            System.out.printf("class count %d%n", classes.length);
            if (verbose) {
                for (ClassActor classActor : classes) {
                    System.out.printf("%s%n", classActor.qualifiedName());
                }
            }
        } else if (result instanceof MethodActor[]) {
            MethodActor[] methods = (MethodActor[]) result;
            System.out.println();
            for (MethodActor methodActor : methods) {
                System.out.printf("%s%n", methodActor.format("%H.%n"));
            }
        } else if (result instanceof FieldActor[]) {
            System.out.println();
            FieldActor[] fields = (FieldActor[]) result;
            for (FieldActor fieldActor : fields) {
                System.out.printf("%s%n", fieldActor.format("%H.%n"));
            }
        } else if (result instanceof ClassLoader) {
            System.out.printf("ClassLoader %s%n", result.toString());
        } else if (result instanceof Integer) {
            int ri = (Integer) result;
            if (fd.method.getName().equals("getThreadState")) {
                DecodeHelper.ThreadState.decodePrint(System.out, ri);
                System.out.println();
            } else if (fd.method.getName().equals("getVersionNumber")) {
                DecodeHelper.decodeVersion(System.out, ri);
                System.out.println();
            } else if (fd.method.getName().equals("getClassStatus")) {
                DecodeHelper.ClassStatus.decodePrint(System.out, ri);
                System.out.println();
            } else if (fd.method.getName().equals("getClassModifiers") || fd.method.getName().equals("getMethodModifiers") ||
                            fd.method.getName().equals("getFieldModifiers"))  {
                System.out.println(Actor.flagsString(ri));
            } else {
                System.out.println(ri);
            }
        } else if (result instanceof Long) {
            long ri = (Long) result;
            System.out.println(ri);
        } else if (result instanceof String) {
            String string = (String) result;
            System.out.println(string);
        } else if (result instanceof String[]) {
            String[] strings = (String[]) result;
            System.out.println();
            for (String string : strings) {
                System.out.println(string);
            }
        } else if (result instanceof Boolean) {
            boolean b = (Boolean) result;
            System.out.println(b);
        } else if (result instanceof ClassVersionInfo) {
            ClassVersionInfo v = (ClassVersionInfo) result;
            System.out.printf("%d.%d%n", v.major, v.minor);
        } else if (result instanceof LocalVariableEntry[]) {
            System.out.println();
            LocalVariableEntry[] entries = (LocalVariableEntry[]) result;
            for (LocalVariableEntry entry : entries) {
                System.out.printf("name=%s, sig=%s, slot=%d, loc=%d, length=%d%n", entry.name, entry.signature,
                                entry.slot, entry.location, entry.length);
            }
        } else if (result instanceof LineNumberEntry[]) {
            System.out.println();
            LineNumberEntry[] entries = (LineNumberEntry[]) result;
            for (LineNumberEntry entry : entries) {
                System.out.printf("bci=%d, lineNumber=%d%n", entry.bci, entry.lineNumber);
            }
        } else if (result instanceof MethodLocation) {
            MethodLocation methodLocation = (MethodLocation) result;
            System.out.printf("start=%d, end=%d%n", methodLocation.start, methodLocation.end);
        } else if (result instanceof byte[]) {
            byte[] bytes = (byte[]) result;
            System.out.printf("byte[], length %d%n", bytes.length);
            if (verbose) {
                for (int i = 0; i < bytes.length; i++) {
                    int value = bytes[i] & 0xFF;
                    System.out.printf("%d %c", value, (i + 1) % 20 == 0 ? '\n' : ' ');
                }
                System.out.println();
            }
        } else if (result instanceof int[]) {
            int[] values = (int[]) result;
            for (int i = 0; i < values.length; i++) {
                if (i != 0) {
                    System.out.print(", ");
                }
                System.out.print(values[i]);
            }
            System.out.println();
        } else if (result == null) {
            System.out.println("null");
        } else {
            System.out.print("undecoded result type: " + result.getClass().getName());
            System.out.printf(", toString()=%s%n", result.toString());
        }
    }

    private int mapLocation(MethodActor method, int location) {
        LineNumberEntry[] lneArray = lntCache.get(method);
        if (lneArray == null) {
            try {
                lneArray = getLineNumberTable((ClassMethodActor) method);
                lntCache.put(method, lneArray);
            } catch (JJVMTIException ex) {
                return -1;
            }
        }
        for (LineNumberEntry lne : lneArray) {
            if (lne.bci == location) {
                return lne.lineNumber;
            }
        }
        return -1;
    }

    private BptData tryDecodeBreakpoint(String methodName) {
        int ix = methodName.lastIndexOf('.');
        if (ix < 0) {
            failExit("usage: bpt=class.method");
        }
        return new BptData(methodName.substring(0, ix), methodName.substring(ix + 1));
    }

    private static boolean validateFunction(String arg) {
        String methodName;
        String args;
        int ix = arg.indexOf('=');
        if (ix > 0) {
            methodName = arg.substring(0, ix);
            args = arg.substring(ix + 1);
        } else {
            methodName = arg;
            args = null;
        }

        for (Method m : JJVMTI.class.getDeclaredMethods()) {
            if (m.getName().equals(methodName)) {
                FunctionData fd = convertArgs(m, args, true);
                checkAddNeededCapability(m, true);
                functionDataList.add(fd);
                return true;
            }
        }
        // check for local method
        if (methodName.equals("sleep")) {
            try {
                FunctionData fd = convertArgs(APITest.class.getDeclaredMethod("sleep", int.class), args, true);
                functionDataList.add(fd);
                return true;
            } catch (NoSuchMethodException ex) {
                failExit("can't find APITest.sleep");
            }
        }
        return false;
    }

    private static boolean checkAddNeededCapability(Method m, boolean exit) {
        JJVMTI_FUNCTION capAnnotation = m.getAnnotation(JJVMTI_FUNCTION.class);
        if (capAnnotation != null) {
            // need to add the necessary capability
            try {
                apiTest.addCapabilities(EnumSet.of(capAnnotation.cap()));
                return true;
            } catch (JJVMTIException ex) {
                fail("failed to add necessary capability: " + capAnnotation.cap(), exit);
                return false;
            }
        } else {
            return true;
        }
    }

    private static FunctionData convertArgs(Method m, String functionArgString, boolean fail) {
        String[] functionArgs = functionArgString == null ? new String[0] : functionArgString.split(":");
        Class<?>[] params = m.getParameterTypes();
        if (params.length != functionArgs.length) {
            if (fail) {
                failExit("wrong number of parameters for " + m.getName());
            } else {
                throw new JJVMTI.JJVMTIException(JVMTI_ERROR_NOT_AVAILABLE);
            }
        }
        Object[] objectArgs = new Object[functionArgs.length];

        int i = 0;
        for (Class<?> param : params) {
            final String functionArg = functionArgs[i];
            if (param == int.class) {
                objectArgs[i] = Integer.parseInt(functionArg);
            } else if (param == long.class) {
                objectArgs[i] = Long.parseLong(functionArg);
            } else if (param == boolean.class) {
                objectArgs[i] = Boolean.parseBoolean(functionArg);
            } else if (param == String.class) {
                objectArgs[i] = functionArg;
            } else if (param == Thread.class || param == ClassActor.class ||
                                MethodActor.class.isAssignableFrom(param) ||
                                param == FieldActor.class  || param == ThreadGroup.class) {
                if (functionArg.equals("#")) {
                    objectArgs[i] = null; // take suitable default
                } else {
                    objectArgs[i] = functionArg; // thread, thread group, class, method, field name
                }
            } else if (param == Thread[].class) {
                // syntax: T1/T2/...
                String[] threadNames = functionArg.split("/");
                objectArgs[i] = threadNames;
            } else if (param == Object.class) {
                if (functionArg.equals("#")) {
                    objectArgs[i] = null; // take suitable default
                } else {
                    objectArgs[i] = functionArg;
                }
            } else {
                if (fail) {
                    failExit("unhandled parameter type: " + param);
                } else {
                    throw new JJVMTI.JJVMTIException(JVMTI_ERROR_NOT_AVAILABLE);
                }
            }
            i++;
        }
        FunctionData result = new FunctionData(m, objectArgs, true);
        return result;
    }

    private static final String BLACKLIST = ":disposeEnvironment";

    /**
     * Attempts to test everything!
     */
    private static void autoGen(String autoArg) {
        String[] ignoreList = (autoArg + BLACKLIST).split(":");
        for (Method m : JJVMTI.class.getDeclaredMethods()) {
            boolean ignore = false;
            for (String b : ignoreList) {
                if (b.equals(m.getName())) {
                    ignore = true;
                    break;
                }
            }
            if (ignore) {
                continue;
            }
            FunctionData fd = autoArgs(m);
            if (fd == null || !checkAddNeededCapability(m, false)) {
                System.out.println("auto not on for " + m.getName());
            } else {
                functionDataList.add(fd);
            }
        }
    }

    /**
     * Conjure up some args for a given method.
     *
     * @param m
     */
    private static FunctionData autoArgs(Method m) {
        Class< ? >[] params = m.getParameterTypes();
        Object[] objectArgs = new Object[params.length];
        // special cases
        if (m.getName().equals("setEventNotificationMode")) {
            objectArgs[0] = JVMTI_ENABLE;
            objectArgs[1] = E.VM_DEATH;
        } else if (m.getName().equals("setVerboseFlag")) {
            objectArgs[0] = JVMTI_VERBOSE_GC;
            objectArgs[1] = true;
        } else if (m.getName().equals("getSystemProperty")) {
            objectArgs[0] = "java.vm.name";
        } else {
            for (int i = 0; i < params.length; i++) {
                Class< ? > param = params[i];
                if (param == int.class) {
                    objectArgs[i] = 0;
                } else if (param == long.class) {
                    objectArgs[i] = 0;
                } else if (param == String.class) {
                    objectArgs[i] = "";
                } else if (param == Thread.class || param == ClassActor.class || MethodActor.class.isAssignableFrom(param) ||
                                param == ThreadGroup.class || param == FieldActor.class || param == Object.class) {
                    objectArgs[i] = null; // suitable default
                } else if (param == Thread[].class) {
                    objectArgs[i] = new String[] {"APITest1", "APITest2"};
                } else {
                    return null;
                }
            }
        }
        return new FunctionData(m, objectArgs, true);
    }

    private static void failExit(String message) {
        fail(message, true);
    }

    private static void fail(String message, boolean exit) {
        Log.println(message);
        if (exit) {
            MaxineVM.exit(-1);
        }
    }


}
