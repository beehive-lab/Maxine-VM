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
package com.sun.max.vm.run.java;

import static com.sun.max.vm.MaxineVM.*;
import static com.sun.max.vm.VMConfiguration.*;
import static com.sun.max.vm.VMOptions.*;
import static com.sun.max.vm.type.ClassRegistry.*;

import java.io.*;
import java.lang.instrument.*;
import java.lang.reflect.*;
import java.net.*;
import java.security.*;
import java.util.*;
import java.util.jar.*;

import sun.misc.*;

import com.sun.max.annotate.*;
import com.sun.max.program.*;
import com.sun.max.vm.*;
import com.sun.max.vm.MaxineVM.Phase;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.deopt.*;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.hosted.*;
import com.sun.max.vm.instrument.*;
import com.sun.max.vm.jni.*;
import com.sun.max.vm.jvmti.*;
import com.sun.max.vm.log.*;
import com.sun.max.vm.profilers.sampling.*;
import com.sun.max.vm.run.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.type.*;

/**
 * The normal Java run scheme that starts up the standard JDK services, loads a user
 * class that has been specified on the command line, finds its main method, and
 * runs it with the specified arguments on the command line. This run scheme
 * is intended to provide the same usage as the standard "java" command in
 * a standard JRE.
 *
 * This class incorporates a lot of nasty, delicate JDK hacks that are needed to
 * get the JDK reinitialized to the point that it is ready to run a new program.
 */
public class JavaRunScheme extends AbstractVMScheme implements RunScheme {

    private static final VMOption versionOption = register(new VMOption(
        "-version", "print product version and exit"), MaxineVM.Phase.STARTING);
    private static final VMOption showVersionOption = register(new VMOption(
        "-showversion", "print product version and continue"), MaxineVM.Phase.STARTING);
    private static final VMOption D64Option = register(new VMOption("-d64",
        "Selects the 64-bit data model if available. Currently ignored."), MaxineVM.Phase.PRISTINE);
    private static final JavaAgentVMOption javaagentOption = register(new JavaAgentVMOption(), MaxineVM.Phase.STARTING);
    private static final VMStringOption profOption = register(new VMStringOption(
        "-Xprof", false, null, "run sampling profiler"), MaxineVM.Phase.STARTING);

    /**
     * List of classes to explicitly reinitialise in the {@link MaxineVM.Phase#STARTING} phase.
     * This supports extensions to the boot image.
     */
    private static List<String> reinitClasses = new LinkedList<String>();
    private static boolean profiling;

    @HOSTED_ONLY
    public JavaRunScheme() {
    }

    /**
     * JDK methods that need to be re-executed at startup, e.g. to re-register native methods.
     */
    private StaticMethodActor[] initIDMethods;

    @HOSTED_ONLY
    public static void registerClassForReInit(String className) {
        CompiledPrototype.registerVMEntryPoint(className + ".<clinit>");
        MaxineVM.registerKeepClassInit(className);
        Trace.line(2, "registering "  +  className + " for reinitialization");
        reinitClasses.add(className);
    }

    /**
     * While bootstrapping, searches the class registry for non-Maxine classes that have methods called
     * "initIDs" with signature "()V". Such methods are typically used in the JDK to initialize JNI
     * identifiers for native code, and need to be re-executed upon startup.
     */
    @HOSTED_ONLY
    public List<? extends MethodActor> gatherNativeInitializationMethods() {
        final List<StaticMethodActor> methods = new LinkedList<StaticMethodActor>();
        final String maxinePackagePrefix = "com.sun.max";
        for (ClassActor classActor : BOOT_CLASS_REGISTRY.bootImageClasses()) {
            if (!classActor.name.toString().startsWith(maxinePackagePrefix)) { // non-Maxine class => JDK class
                for (StaticMethodActor method : classActor.localStaticMethodActors()) {
                    if ((method.name.equals("initIDs") || method.name.equals("initNative")) && (method.descriptor().numberOfParameters() == 0) && method.resultKind() == Kind.VOID) {
                        method.makeInvocationStub();
                        methods.add(method);
                    }
                }
            }
        }
        initIDMethods = methods.toArray(new StaticMethodActor[methods.size()]);
        return methods;
    }

    /**
     * Runs all the native initializer methods gathered while bootstrapping.
     */
    public void runNativeInitializationMethods() {
        final List<StaticMethodActor> methods = new LinkedList<StaticMethodActor>();
        for (StaticMethodActor method : initIDMethods) {
            try {
                if (method.currentTargetMethod() == null) {
                    FatalError.unexpected("Native initialization method must be compiled in boot image: " + method);
                }
                method.invoke();
            } catch (UnsatisfiedLinkError unsatisfiedLinkError) {
                // Library not present yet - try again next time:
                methods.add(method);
            } catch (InvocationTargetException invocationTargetException) {
                if (invocationTargetException.getTargetException() instanceof UnsatisfiedLinkError) {
                   // Library not present yet - try again next time:
                    methods.add(method);
                } else {
                    throw ProgramError.unexpected(invocationTargetException.getTargetException());
                }
            } catch (Throwable throwable) {
                throw ProgramError.unexpected(throwable);
            }
        }
        initIDMethods = methods.toArray(new StaticMethodActor[methods.size()]);
    }

    @ALIAS(declaringClass = System.class)
    public static native void initializeSystemClass();

    /**
     * The initialization method of the Java run scheme runs at both bootstrapping and startup.
     * While bootstrapping, it gathers the methods needed for native initialization, and at startup
     * it initializes basic VM services.
     */
    @Override
    public void initialize(MaxineVM.Phase phase) {
        switch (phase) {
            case BOOTSTRAPPING: {
                if (MaxineVM.isHosted()) {
                    // Make sure MaxineVM.exit is available when running the JavaRunScheme.
                    new CriticalMethod(MaxineVM.class, "exit",
                                    SignatureDescriptor.create(void.class, int.class, boolean.class));
                }
                break;
            }
            case STARTING: {

                // This hack enables (platform-dependent) tracing before the eventual System properties are set:
                System.setProperty("line.separator", "\n");

                // Normally, we would have to initialize tracing this late,
                // because 'PrintWriter.<init>()' relies on a system property ("line.separator"), which is accessed during 'initializeSystemClass()'.

                initializeSystemClass();

                // reinitialise any registered classes
                for (String className : reinitClasses) {
                    try {
                        final ClassActor classActor = ClassActor.fromJava(Class.forName(className));
                        classActor.callInitializer();
                    } catch (Exception e) {
                        FatalError.unexpected("Error re-initializing" + className, e);
                    }
                }

                break;
            }

            case RUNNING: {
                // This is always the last scheme to be initialized, so now is the right time
                // to start the profiler if requested.
                final String profValue = profOption.getValue();
                if (profValue != null) {
                    profiling = true;
                    SamplingProfiler.create(profValue);
                }
                break;
            }

            case TERMINATING: {
                JniFunctions.printJniFunctionTimers();
                if (profiling) {
                    SamplingProfiler.terminate();
                }
                break;
            }
            default: {
                break;
            }
        }
    }

    /**
     * Initializes basic features of the VM, including all of the VM schemes and the trap handling mechanism.
     * It also parses some program arguments that were not parsed earlier.
     */
    protected final void initializeBasicFeatures() {
        MaxineVM vm = vm();
        vm.phase = MaxineVM.Phase.STARTING;

        // Now we can decode all the other VM arguments using the full language
        if (VMOptions.parseStarting()) {

            VMLog.checkLogOptions();

            vmConfig().initializeSchemes(MaxineVM.Phase.STARTING);

            if (Heap.ExcessiveGCFrequency != 0) {
                new ExcessiveGCDaemon(Heap.ExcessiveGCFrequency).start();
            }

            if (Deoptimization.DeoptimizeALot != 0 && Deoptimization.UseDeopt) {
                new DeoptimizeALot(Deoptimization.DeoptimizeALot).start();
            }

            // Install the signal handler for dumping threads when SIGHUP is received
            Signal.handle(new Signal("QUIT"), new PrintThreads(false));
        }
    }

    protected boolean parseMain() {
        return VMOptions.parseMain(true);
    }

    /**
     * The run() method is the entrypoint to this run scheme, after the VM has started up.
     * This method initializes the basic features, parses the main program arguments, looks
     * up the user-specified main class, and invokes its main method with the specified
     * command-line arguments
     */
    public void run() throws Throwable {
        boolean error = true;
        String classKindName = "premain";
        try {
            initializeBasicFeatures();
            if (VMOptions.earlyVMExitRequested()) {
                return;
            }

            error = false;

            if (versionOption.isPresent()) {
                sun.misc.Version.print();
                return;
            }
            if (showVersionOption.isPresent()) {
                sun.misc.Version.print();
            }

            if (!parseMain()) {
                return;
            }

            error = true;

            MaxineVM vm = vm();
            vm.phase = Phase.RUNNING;
            vmConfig().initializeSchemes(MaxineVM.Phase.RUNNING);

            /*
             * JVMTI has a VM_START and a VM_INIT event, but the distinction
             * is minimal and only relates to which JVMTI functions may be called.
             * The gating function for dispatching either event this late
             * is that any JNI function can be called from VM_START, e.g, FindClass,
             * which requires that essentially all of the VM machinery is working.
             */
            JVMTI.event(JVMTIEvent.VM_START);
            // VM_INIT must precede the THREAD_START event for the main thread
            JVMTI.event(JVMTIEvent.VM_INIT);

            JVMTI.event(JVMTIEvent.THREAD_START);

            // load -javaagent agents
            loadJavaAgents();

            classKindName = "main";
            Class<?> mainClass = loadMainClass();
            if (mainClass != null) {
                lookupAndInvokeMain(mainClass);
                error = false;
            }

        } catch (ClassNotFoundException classNotFoundException) {
            error = true;
            System.err.println("Could not load " + classKindName + "class: " + classNotFoundException);
        } catch (NoClassDefFoundError noClassDefFoundError) {
            error = true;
            System.err.println("Error loading " + classKindName + "class: " + noClassDefFoundError);
        } catch (NoSuchMethodException noSuchMethodException) {
            error = true;
            System.err.println("Could not find " + classKindName + "method: " + noSuchMethodException);
        } catch (InvocationTargetException invocationTargetException) {
            // This is an application exception: let VmThread.run() handle this.
            // We only catch it here to set the VM exit code to a non-zero value.
            error = true;
            throw invocationTargetException.getCause();
        } catch (IllegalAccessException illegalAccessException) {
            error = true;
            System.err.println("Illegal access trying to invoke " + classKindName + "method: " + illegalAccessException);
        } catch (IOException ioException) {
            error = true;
            System.err.println("error reading jar file: " + ioException);
        } catch (ProgramError programError) {
            error = true;
            Log.print("ProgramError: ");
            Log.println(programError.getMessage());
        } finally {
            if (error) {
                MaxineVM.setExitCode(-1);
            }
        }
    }

    private void lookupAndInvokeMain(Class<?> mainClass) throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {
        final Method mainMethod = lookupMainOrAgentClass(mainClass, "main", String[].class);
        AccessController.doPrivileged(new PrivilegedAction<Object>() {
            public Object run() {
                mainMethod.setAccessible(true);
                return null;
            }
        });
        mainMethod.invoke(null, new Object[] {VMOptions.mainClassArguments()});
    }

    /**
     * Try to locate a given method name and signature that is also public static void in given class.
     * @param mainClass class to search
     * @param methodName name of method
     * @param params parameter types
     * @return the method instance
     * @throws NoSuchMethodException if the method cannot be found
     */
    public static Method lookupMainOrAgentClass(Class<?> mainClass, String methodName, Class<?> ...params) throws NoSuchMethodException {
        final Method mainMethod = mainClass.getDeclaredMethod(methodName, params);
        final int modifiers = mainMethod.getModifiers();
        if ((!Modifier.isPublic(modifiers)) || (!Modifier.isStatic(modifiers)) || (mainMethod.getReturnType() != void.class)) {
            throw new NoSuchMethodException(methodName);
        }
        return mainMethod;
    }

    private Class<?> loadMainClass() throws IOException, ClassNotFoundException {
        final ClassLoader appClassLoader = Launcher.getLauncher().getClassLoader();
        final String jarFileName = VMOptions.jarFile();
        String mainClassName = null;
        if (jarFileName == null) {
            // the main class was specified on the command line
            mainClassName = VMOptions.mainClassName();
        } else {
            // the main class is in the jar file
            final JarFile jarFile = new JarFile(jarFileName);
            mainClassName = findClassAttributeInJarFile(jarFile, "Main-Class");
            if (mainClassName == null) {
                throw new ClassNotFoundException("Could not find main class in jarfile: " + jarFileName);
            }
        }
        return appClassLoader.loadClass(mainClassName);
    }

    /**
     * Searches the manifest in given jar file for given attribute.
     * @param jarFile jar file to search
     * @param classAttribute attribute to search for
     * @return the value of the attribute of null if not found
     * @throws IOException if error reading jar file
     */
    public static String findClassAttributeInJarFile(JarFile jarFile, String classAttribute) throws IOException {
        final Manifest manifest =  jarFile.getManifest();
        if (manifest == null) {
            return null;
        }
        return manifest.getMainAttributes().getValue(classAttribute);
    }

    /**
     * Invoke the given agent method in the given agent class with the given args.
     * @param agentClassName agent class name
     * @param agentMethodName agent method name
     * @param agentArgs agent args
     * @throws ClassNotFoundException if agent class not found
     * @throws NoSuchMethodException if agent method not found
     * @throws InvocationTargetException if invocation failed
     * @throws IllegalAccessException if access is denied
     */
    public static void invokeAgentMethod(URL url, String agentClassName, String agentMethodName, String agentArgs) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        final ClassLoader appClassLoader = Launcher.getLauncher().getClassLoader();
        final Class<?> agentClass = appClassLoader.loadClass(agentClassName);
        Method agentMethod = null;
        Object[] agentInvokeArgs = null;
        try {
            agentMethod = lookupMainOrAgentClass(agentClass, agentMethodName, new Class<?>[] {String.class, Instrumentation.class});
            agentInvokeArgs = new Object[2];
            agentInvokeArgs[1] = InstrumentationManager.createInstrumentation();
        } catch (NoSuchMethodException ex) {
            agentMethod = lookupMainOrAgentClass(agentClass, agentMethodName, new Class<?>[] {String.class});
            agentInvokeArgs = new Object[1];
        }
        agentInvokeArgs[0] = agentArgs;
        InstrumentationManager.registerAgent(url);
        agentMethod.invoke(null, agentInvokeArgs);
    }

    /**
     * The method used to extend the class path of the app class loader with entries specified by an agent.
     * Reflection is used for this as the method used to make the addition depends on the JDK
     * version in use.
     */
    private static final Method addURLToAppClassLoader;
    static {
        Method method;
        try {
            method = Launcher.class.getDeclaredMethod("addURL", URL.class);
        } catch (NoSuchMethodException e) {
            try {
                method = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
            } catch (NoSuchMethodException e2) {
                throw FatalError.unexpected("Cannot find method to extend class path of app class loader");
            }
        }
        method.setAccessible(true);
        addURLToAppClassLoader = method;
    }


    private void loadJavaAgents() throws IOException, ClassNotFoundException, InvocationTargetException, IllegalAccessException, NoSuchMethodException {
        for (int i = 0; i < javaagentOption.count(); i++) {
            final String javaagentOptionString = javaagentOption.getValue(i);
            String jarPath = null;
            String agentArgs = "";
            final int cIndex = javaagentOptionString.indexOf(':');
            if (javaagentOptionString.length() > 1 && cIndex >= 0) {
                final int eIndex = javaagentOptionString.indexOf('=', cIndex);
                if (eIndex > 0) {
                    jarPath = javaagentOptionString.substring(cIndex + 1, eIndex);
                    agentArgs = javaagentOptionString.substring(eIndex + 1);
                } else {
                    jarPath = javaagentOptionString.substring(cIndex + 1);
                }
                JarFile jarFile = null;
                try {
                    jarFile = new JarFile(jarPath);
                    final String preMainClassName = findClassAttributeInJarFile(jarFile, "Premain-Class");
                    jarFile.close();
                    if (preMainClassName == null) {
                        Log.println("could not find premain class in jarfile: " + jarPath);
                    }
                    final URL url = new URL("file://" + new File(jarPath).getAbsolutePath());
                    addURLToAppClassLoader.invoke(Launcher.getLauncher().getClassLoader(), url);
                    invokeAgentMethod(url, preMainClassName, "premain", agentArgs);
                } finally {
                    if (jarFile != null) {
                        jarFile.close();
                    }
                }
            } else {
                throw new IOException("syntax error in -javaagent" + javaagentOptionString);
            }
        }
    }
}
