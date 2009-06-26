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

import static com.sun.max.vm.VMOptions.register;

import java.io.*;
import java.lang.reflect.*;
import java.security.*;
import java.util.jar.*;

import sun.misc.*;

import com.sun.max.annotate.*;
import com.sun.max.collect.*;
import com.sun.max.program.*;
import com.sun.max.util.*;
import com.sun.max.vm.*;
import com.sun.max.vm.object.TupleAccess;
import com.sun.max.vm.MaxineVM.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.run.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.tele.*;
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
 */
public class JavaRunScheme extends AbstractVMScheme implements RunScheme {

    private static final VMOption versionOption = register(new VMOption(
        "-version", "print product version and exit"), MaxineVM.Phase.STARTING);
    private static final VMOption showVersionOption = register(new VMOption(
        "-showversion", "print product version and continue"), MaxineVM.Phase.STARTING);
    private static final VMOption D64Option = register(new VMOption("-d64",
        "Selects the 64-bit data model if available. Currently ignored."), MaxineVM.Phase.PRISTINE);

    public JavaRunScheme(VMConfiguration vmConfiguration) {
        super(vmConfiguration);
    }

    /**
     * JDK methods that need to be re-executed at startup, e.g. to re-register native methods.
     */
    private StaticMethodActor[] initIDMethods;

    /**
     * At prototyping time, searches the class registry for non-Maxine classes that have methods called
     * "initIDs" with signature "()V". Such methods are typically used in the JDK to initialize JNI
     * identifiers for native code, and need to be reexecuted upon startup.
     */
    @PROTOTYPE_ONLY
    public IterableWithLength<? extends MethodActor> gatherNativeInitializationMethods() {
        final AppendableSequence<StaticMethodActor> methods = new LinkSequence<StaticMethodActor>();
        final String maxinePackagePrefix = new com.sun.max.Package().name();
        for (ClassActor classActor : ClassRegistry.vmClassRegistry()) {
            if (!classActor.name.toString().startsWith(maxinePackagePrefix)) { // non-Maxine class => JDK class
                for (StaticMethodActor method : classActor.localStaticMethodActors()) {
                    if (method.name.equals("initIDs") && (method.descriptor().numberOfParameters() == 0) && method.resultKind() == Kind.VOID) {
                        methods.append(method);
                    }
                }
            }
        }
        initIDMethods = Sequence.Static.toArray(methods, StaticMethodActor.class);
        return methods;
    }

    /**
     * Runs all the native initializer methods gathered at prototyping time.
     */
    public void runNativeInitializationMethods() {
        final AppendableSequence<StaticMethodActor> methods = new LinkSequence<StaticMethodActor>();
        for (StaticMethodActor method : initIDMethods) {
            try {
                final MethodState methodState = method.methodState();
                if (methodState == null || methodState.currentTargetMethod() == null) {
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
        initIDMethods = Sequence.Static.toArray(methods, StaticMethodActor.class);
    }

    /**
     * The initialization method of the Java run scheme runs at both prototyping time and startup.
     * At prototyping time, it gathers the methods needed for native initialization, and at startup
     * it initializes basic VM services.
     */
    @Override
    public void initialize(MaxineVM.Phase phase) {
        switch (phase) {
            case STARTING: {
                MaxineMessenger.initialize();

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
        /* TODO It is not clear that the following should be necessary as ClassLoader.loadClass has an
        * explicit check for a null parent and calls findBootStrapClass in that case. However, class loading
        * is rather complicated and, experimentally, if this code commented out, the VM (re)loads Object.class
        * from rt.jar, which is bad news. The consequence of making VMClassLoader an explicit parent
        * is that the classloader depth differs from Hotspot and we effectively have a non-null boot class loader.
        */
        final ClassLoader systemClassLoader = ClassLoader.getSystemClassLoader();
        // The parent of the system (app) class loader is the extension class loader and
        // we need to set the private "parent" field of it to our VMClassLoader in order to
        // be able to  find classes compiled into the image.
        final FieldActor parentFieldActor = ClassActor.fromJava(ClassLoader.class).findFieldActor(SymbolTable.makeSymbol("parent"));
        // ClassLoader.getParent() has checks we don't want
        final ClassLoader parent = (ClassLoader) TupleAccess.readObject(systemClassLoader, parentFieldActor.offset());
        TupleAccess.writeObject(parent, parentFieldActor.offset(), VmClassLoader.VM_CLASS_LOADER);
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
        MaxineVM.writeInitialVMParams();
        MaxineVM.hostOrTarget().setPhase(MaxineVM.Phase.STARTING);

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
    public void run() {
        boolean error = true;
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

            MaxineVM.host().setPhase(Phase.RUNNING);
            VMConfiguration.hostOrTarget().initializeSchemes(MaxineVM.Phase.RUNNING);

            lookupAndInvokeMain(loadMainClass());
            error = false;

        } catch (ClassNotFoundException classNotFoundException) {
            error = true;
            System.err.println("Could not load main class: " + classNotFoundException);
        } catch (NoClassDefFoundError noClassDefFoundError) {
            error = true;
            System.err.println("Error loading main class: " + noClassDefFoundError);
        } catch (NoSuchMethodException noSuchMethodException) {
            error = true;
            System.err.println("Could not find main method: " + noSuchMethodException);
        } catch (InvocationTargetException invocationTargetException) {
            // This is an application exception: let VmThread.run() handle this.
            // We only catch it here to set the VM exit code to a non-zero value.
            error = true;
            throw Exceptions.cast(RuntimeException.class, invocationTargetException.getCause());
        } catch (IllegalAccessException illegalAccessException) {
            error = true;
            System.err.println("Illegal access trying to invoke main method: " + illegalAccessException);
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
        final Method mainMethod = mainClass.getDeclaredMethod("main", String[].class);
        final int modifiers = mainMethod.getModifiers();
        if ((!Modifier.isPublic(modifiers)) || (!Modifier.isStatic(modifiers)) || (mainMethod.getReturnType() != void.class)) {
            throw new NoSuchMethodException("main");
        }
        AccessController.doPrivileged(new PrivilegedAction<Object>() {
            public Object run() {
                mainMethod.setAccessible(true);
                return null;
            }
        });
        mainMethod.invoke(null, new Object[] {VMOptions.mainClassArguments()});
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
            mainClassName = findMainClassNameInJarFile(jarFile);
            if (mainClassName == null) {
                throw ProgramError.unexpected("could not find main class in jarfile: " + jarFileName);
            }
        }
        return appClassLoader.loadClass(mainClassName);
    }

    private String findMainClassNameInJarFile(JarFile jarFile) throws IOException {
        final Manifest manifest =  jarFile.getManifest();
        if (manifest == null) {
            return null;
        }
        return manifest.getMainAttributes().getValue("Main-Class");
    }

}
