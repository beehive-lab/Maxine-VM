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
package com.sun.max.vm.run.java;

import static com.sun.max.vm.VMOptions.*;

import java.io.*;
import java.lang.instrument.*;
import java.lang.reflect.*;
import java.net.*;
import java.security.*;
import java.util.jar.*;

import sun.misc.*;

import com.sun.max.annotate.*;
import com.sun.max.collect.*;
import com.sun.max.program.*;
import com.sun.max.vm.*;
import com.sun.max.vm.MaxineVM.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.instrument.*;
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
 *
 * @author Bernd Mathiske
 * @author Mick Jordan
 */
public class JavaRunScheme extends AbstractVMScheme implements RunScheme {

    private static final VMOption versionOption = register(new VMOption(
        "-version", "print product version and exit"), MaxineVM.Phase.STARTING);
    private static final VMOption showVersionOption = register(new VMOption(
        "-showversion", "print product version and continue"), MaxineVM.Phase.STARTING);
    private static final VMOption D64Option = register(new VMOption("-d64",
        "Selects the 64-bit data model if available. Currently ignored."), MaxineVM.Phase.PRISTINE);
    private static final AgentVMOption javaagentOption = register(new AgentVMOption(
        "-javaagent", "load Java programming language agent, see java.lang.instrument"), MaxineVM.Phase.STARTING);

    public JavaRunScheme(VMConfiguration vmConfiguration) {
        super(vmConfiguration);
    }

    /**
     * JDK methods that need to be re-executed at startup, e.g. to re-register native methods.
     */
    private StaticMethodActor[] initIDMethods;

    /**
     * While bootstrapping, searches the class registry for non-Maxine classes that have methods called
     * "initIDs" with signature "()V". Such methods are typically used in the JDK to initialize JNI
     * identifiers for native code, and need to be re-executed upon startup.
     */
    @HOSTED_ONLY
    public IterableWithLength<? extends MethodActor> gatherNativeInitializationMethods() {
        final AppendableSequence<StaticMethodActor> methods = new LinkSequence<StaticMethodActor>();
        final String maxinePackagePrefix = new com.sun.max.Package().name();
        for (ClassActor classActor : ClassRegistry.BOOT_CLASS_REGISTRY) {
            if (!classActor.name.toString().startsWith(maxinePackagePrefix)) { // non-Maxine class => JDK class
                for (StaticMethodActor method : classActor.localStaticMethodActors()) {
                    if (method.name.equals("initIDs") && (method.descriptor().numberOfParameters() == 0) && method.resultKind() == Kind.VOID) {
                        methods.append(method);
                    }
                }
            }
        }
        initIDMethods = Sequence.Static.toArray(methods, new StaticMethodActor[methods.length()]);
        return methods;
    }

    /**
     * Runs all the native initializer methods gathered while bootstrapping.
     */
    public void runNativeInitializationMethods() {
        final AppendableSequence<StaticMethodActor> methods = new LinkSequence<StaticMethodActor>();
        for (StaticMethodActor method : initIDMethods) {
            try {
                if (method.currentTargetMethod() == null) {
                    FatalError.unexpected("Native initialization method must be compiled in boot image: " + method);
                }
                method.invoke();
            } catch (UnsatisfiedLinkError unsatisfiedLinkError) {
                // Library not present yet - try again next time:
                methods.append(method);
            } catch (InvocationTargetException invocationTargetException) {
                if (invocationTargetException.getTargetException() instanceof UnsatisfiedLinkError) {
                    // Library not present yet - try again next time:
                    methods.append(method);
                } else {
                    ProgramError.unexpected(invocationTargetException.getTargetException());
                }
            } catch (Throwable throwable) {
                ProgramError.unexpected(throwable);
            }
        }
        initIDMethods = Sequence.Static.toArray(methods, new StaticMethodActor[methods.length()]);
    }

    /**
     * The initialization method of the Java run scheme runs at both bootstrapping and startup.
     * While bootstrapping, it gathers the methods needed for native initialization, and at startup
     * it initializes basic VM services.
     */
    @Override
    public void initialize(MaxineVM.Phase phase) {
        switch (phase) {
            case STARTING: {

                // This hack enables (platform-dependent) tracing before the eventual System properties are set:
                System.setProperty("line.separator", "\n");

                try {
                    ClassActor.fromJava(System.class).findLocalStaticMethodActor("initializeSystemClass").invoke();
                } catch (Throwable throwable) {
                    FatalError.unexpected("error in initializeSystemClass", throwable);
                }

                // Normally, we would have to initialize tracing this late,
                // because 'PrintWriter.<init>()' relies on a system property ("line.separator"), which is accessed during 'initializeSystemClass()'.

                setupClassLoaders();
                break;
            }
            default: {
                break;
            }
        }
    }

    /**
     * Arranges for the standard set of class loaders provided by sun.misc.Launcher to be created for the target environment.
     */
    private void setupClassLoaders() {
        // ClassLoader.getSystemClassLoader calls sun.misc.Launcher.getLauncher()
        // We may need to reinitialize the class loaders in Launcher for the target environment
        resetLauncher(ClassActor.fromJava(sun.misc.Launcher.class));
    }

    /**
     * A hook to reset the state of the Launcher class, if, i.e., it is compiled into the image.
     * @param launcherClassActor
     */
    protected void resetLauncher(ClassActor launcherClassActor) {
    }

    /**
     * Initializes basic features of the VM, including all of the VM schemes and the trap handling mechanism.
     * It also parses some program arguments that were not parsed earlier.
     */
    protected final void initializeBasicFeatures() {
        VMConfiguration.hostOrTarget().initializeSchemes(MaxineVM.Phase.PRISTINE);
        MaxineVM vm = MaxineVM.hostOrTarget();
        vm.phase = MaxineVM.Phase.STARTING;

        // Now we can decode all the other VM arguments using the full language
        if (VMOptions.parseStarting()) {
            VMConfiguration.hostOrTarget().initializeSchemes(MaxineVM.Phase.STARTING);
            SpecialReferenceManager.initialize(MaxineVM.Phase.STARTING);
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

            MaxineVM vm = MaxineVM.host();
            vm.phase = Phase.RUNNING;
            VMConfiguration.hostOrTarget().initializeSchemes(MaxineVM.Phase.RUNNING);

            loadAgents();

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

            VMConfiguration.hostOrTarget().finalizeSchemes(MaxineVM.Phase.RUNNING);
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
                Log.println("could not find main class in jarfile: " + jarFileName);
                return null;
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


    private void loadAgents() throws IOException, ClassNotFoundException, InvocationTargetException, IllegalAccessException, NoSuchMethodException {
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
                    final URL url = new URL("file://" + jarPath);
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
