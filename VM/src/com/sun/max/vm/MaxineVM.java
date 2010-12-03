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
package com.sun.max.vm;

import static com.sun.max.lang.Classes.*;
import static com.sun.max.platform.Platform.*;
import static com.sun.max.vm.VMConfiguration.*;
import static com.sun.max.vm.VMOptions.*;

import java.lang.reflect.*;
import java.util.*;

import com.sun.max.annotate.*;
import com.sun.max.config.*;
import com.sun.max.lang.*;
import com.sun.max.platform.*;
import com.sun.max.program.*;
import com.sun.max.unsafe.*;
import com.sun.max.util.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.c1x.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.hosted.BootImage.Header;
import com.sun.max.vm.hosted.*;
import com.sun.max.vm.jni.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.thread.*;
import com.sun.max.vm.type.*;

/**
 * The global VM context. There is a {@linkplain #vm() single VM context} in existence at any time.
 * The {@linkplain VMConfiguration configuration} for a VM context can be accessed via the
 * {@link #config} field although the preferred mechanism for accessing the configuration for the
 * global VM context is {@linkplain VMConfiguration#vmConfig()}.
 * <p>
 * Other functionality encapsulated in this class includes:
 * <li>The {@link #isHosted()} method that can be used to guard blocks of code that will be omitted from a boot image.</li>
 * <li>The current execution {@linkplain #phase phase} of the VM, denoting what language and VM capabilities
 * have been initialized and are available.</li>
 * <li>Some global native methods used at runtime that don't particularly
 * belong to other classes (e.g. {@link #native_currentTimeMillis()}, {@link #native_exit(int)}, etc).</li>
 * <li>Methods for {@linkplain #registerCriticalMethod(CriticalMethod) registering} methods to be
 * loaded & compiled into the boot image.</li>
 *
 * @author Bernd Mathiske
 * @author Ben L. Titzer
 * @author Doug Simon
 * @author Paul Caprioli
 */
public final class MaxineVM {

    public static final String VERSION = "0.2";
    public static final int HARD_EXIT_CODE = -2;

    /**
     * The set of packages denoting classes for which {@link #isMaxineClass(ClassActor)}
     * and {@link #isMaxineClass(TypeDescriptor)} will return true.
     */
    @HOSTED_ONLY
    private static final Set<String> MAXINE_CODE_BASE_PACKAGES = new HashSet<String>();

    @HOSTED_ONLY
    private static final Map<Class, Boolean> HOSTED_CLASSES = new HashMap<Class, Boolean>();

    @HOSTED_ONLY
    private static final Set<String> KEEP_CLINIT_CLASSES = new HashSet<String>();

    private static final VMOption HELP_OPTION = register(new VMOption("-help", "Prints this help message.") {
        @Override
        protected boolean haltsVM() {
            return true;
        }
        @Override
        public boolean parseValue(Pointer optionValue) {
            VMOptions.printUsage(Category.STANDARD);
            return true;
        }
    }, MaxineVM.Phase.PRISTINE);

    /**
     * The current VM context.
     */
    @CONSTANT
    private static MaxineVM vm;

    /**
     * The {@linkplain VmThreadLocal#ETLA safepoints-enabled} TLA of the primordial thread.
     *
     * The address of this field is exposed to native code via {@link Header#primordialETLAOffset}
     * so that it can be initialized by the C substrate. It also enables a debugger attached to
     * the VM to find it (as it's not part of the thread list).
     */
    private static Pointer primordialETLA;

    private static int exitCode = 0;

    private static long startupTime;

    public enum Phase {
        /**
         * Running on a host VM in order to construct a target VM or to run tests.
         */
        BOOTSTRAPPING,

        /**
         * Creating the compiled boot image.
         */
        COMPILING,

        /**
         * Executing target VM code, but many features do not work yet.
         */
        PRIMORDIAL,

        /**
         * Java thread synchronization initialized and available (but may not do anything yet).
         */
        PRISTINE,

        /**
         * All pure Java language features work; now building the application sandbox.
         */
        STARTING,

        /**
         * Executing application code.
         */
        RUNNING,

        /**
         * VM about to terminate, all non-daemon threads terminated, shutdown hooks run, but {@link VMOperation} thread still live.
         * Last chance to interpose, but be careful what you do. In particular, thread creation is not permitted.
         */
        TERMINATING
    }

    /**
     * An enum for the properties whose values must be obtained from the native environment at runtime. The enum
     * constants in this class are used to read values from the native_properties_t struct defined in
     * Native/substrate/maxine.c returned by {@link MaxineVM#native_properties()}.
     *
     * @author Doug Simon
     */
    public enum NativeProperty {
        USER_NAME,
        USER_HOME,
        USER_DIR;

        /**
         * Gets the value of this property from a given C struct.
         *
         * @param cStruct the value returned by a call to {@link MaxineVM#native_properties()}
         * @return the value of this property in {@code cStruct} converted to a {@link String} value (which may be {@code null})
         */
        public String value(Pointer cStruct) {
            final Pointer cString = cStruct.readWord(ordinal() * Word.size()).asPointer();
            if (cString.isZero()) {
                return null;
            }
            try {
                return CString.utf8ToJava(cString);
            } catch (Utf8Exception utf8Exception) {
                throw FatalError.unexpected("Could not convert C string value of " + this + " to a Java string");
            }
        }
    }

    /**
     * Registers the complete set of packages that (potentially) comprise the boot image being constructed.
     */
    @HOSTED_ONLY
    public static void registerMaxinePackages(List<MaxPackage> packages) {
        for (MaxPackage maxPackage : packages) {
            MAXINE_CODE_BASE_PACKAGES.add(maxPackage.name());
            MAXINE_CODE_BASE_PACKAGES.add("test." + maxPackage.name());
        }
    }

    @HOSTED_ONLY
    public static void registerKeepClassInit(String className) {
        KEEP_CLINIT_CLASSES.add(className);
    }

    @HOSTED_ONLY
    public static boolean keepClassInit(TypeDescriptor classDescriptor) {
        final String className = classDescriptor.toJavaString();
        final boolean result = KEEP_CLINIT_CLASSES.contains(className) ||
            System.getProperty("max.loader.preserveClinitMethods") != null;
        return result;
    }

    public static String name() {
        return "Maxine VM";
    }

    public static String description() {
        return "The Maxine Virtual Machine, see <http://kenai.com/projects/maxine>";
    }

    /**
     * Gets the current VM context.
     */
    @INLINE
    public static MaxineVM vm() {
        return vm;
    }

    /**
     * Gets the current VM context.
     */
    @INLINE
    public static MaxRiRuntime runtime() {
        return vm.runtime;
    }

    /**
     * Initializes or changes the current VM context.
     * This also {@linkplain Platform#set(Platform) sets} the current platform context
     * to {@code vm.configuration.platform}. That is,
     * changing the VM context also changes the platform context.
     *
     * @param vm the new VM context (must not be {@code null})
     * @return the previous VM context
     */
    @HOSTED_ONLY
    public static MaxineVM set(MaxineVM vm) {
        MaxineVM old = MaxineVM.vm;
        Platform.set(platform());
        MaxineVM.vm = vm;
        return old;
    }

    /**
     * Determines if the current execution environment is hosted on another JVM.
     *
     * @return {@code true} if being executed on another JVM, {@code false} if executing the bootstrapped/target VM.
     */
    public static boolean isHosted() {
        return true;
    }

    @LOCAL_SUBSTITUTION
    @FOLD
    public static boolean isHosted_() {
        return false;
    }

    /**
     * Determines if this is a {@link BuildLevel#DEBUG debug} build of the VM.
     * @return {@code true} if this is a debug build
     */
    @FOLD
    public static boolean isDebug() {
        return vm().config.debugging();
    }

    /**
     * Determines if a given constructor, field or method exists only for hosted execution.
     *
     * @param member the member to check
     * @return {@code true} if the member is only valid while executing hosted
     */
    @HOSTED_ONLY
    public static boolean isHostedOnly(AccessibleObject member) {
        return member.getAnnotation(HOSTED_ONLY.class) != null ||
               !Platform.platform().isAcceptedBy(member.getAnnotation(PLATFORM.class)) ||
               isHostedOnly(Classes.getDeclaringClass(member));
    }

    /**
     * Determines if a given class exists only for hosted execution and should not be part
     * of a generated target image.
     *
     * @param javaClass the class to check
     * @return {@code true} if the class is only valid while bootstrapping
     */
    @HOSTED_ONLY
    public static boolean isHostedOnly(Class<?> javaClass) {
        final Boolean value = HOSTED_CLASSES.get(javaClass);
        if (value != null) {
            return value;
        }

        if (javaClass.getAnnotation(HOSTED_ONLY.class) != null) {
            HOSTED_CLASSES.put(javaClass, Boolean.TRUE);
            return true;
        }

        // We really want to apply @HOSTED_ONLY to the MaxPackage class but can't
        // until it's in the VM project
        if (MaxPackage.class.isAssignableFrom(javaClass)) {
            HOSTED_CLASSES.put(javaClass, Boolean.TRUE);
            return true;
        }

        // May want to replace this 'magic' interpretation of ".hosted"
        // with a sentinel class (e.g. HOSTED_ONLY_PACKAGE).
        if (getPackageName(javaClass).endsWith(".hosted")) {
            HOSTED_CLASSES.put(javaClass, Boolean.TRUE);
            return true;
        }

        Class superclass = javaClass.getSuperclass();
        if (superclass != null && isHostedOnly(superclass)) {
            HOSTED_CLASSES.put(javaClass, Boolean.TRUE);
            return true;
        }

        if (!Platform.platform().isAcceptedBy(javaClass.getAnnotation(PLATFORM.class))) {
            HOSTED_CLASSES.put(javaClass, Boolean.TRUE);
            return true;
        }

        final MaxPackage maxPackage = MaxPackage.fromClass(javaClass);
        if (maxPackage != null) {
            if (maxPackage.getClass().getSuperclass() == MaxPackage.class) {
                final boolean isTestPackage = maxPackage.name().startsWith("test.com.sun.max.");
                HOSTED_CLASSES.put(javaClass, !isTestPackage);
                return !isTestPackage;
            } else if (maxPackage.getClass().getAnnotation(HOSTED_ONLY.class) != null) {
                HOSTED_CLASSES.put(javaClass, true);
                return true;
            }

        }

        try {
            final Class<?> enclosingClass = javaClass.getEnclosingClass();
            if (enclosingClass != null) {
                final boolean result = isHostedOnly(enclosingClass);
                HOSTED_CLASSES.put(javaClass, result);
                return result;
            }
        } catch (LinkageError linkageError) {
            ProgramWarning.message("Error trying to get the enclosing class for " + javaClass + ": " + linkageError);
        }
        HOSTED_CLASSES.put(javaClass, Boolean.FALSE);
        return false;
    }

    public static boolean isPrimordial() {
        return vm().phase == Phase.PRIMORDIAL;
    }

    public static boolean isPristine() {
        return vm().phase == Phase.PRISTINE;
    }

    public static boolean isStarting() {
        return vm().phase == Phase.STARTING;
    }

    public static boolean isPrimordialOrPristine() {
        final Phase phase = vm().phase;
        return phase == Phase.PRIMORDIAL || phase == Phase.PRISTINE;
    }

    public static boolean isRunning() {
        return vm().phase == Phase.RUNNING;
    }

    public static long getStartupTime() {
        return startupTime;
    }

    /**
     * Determines if a given class name denotes a class that is part of the Maxine code base.
     */
    public static boolean isMaxineClass(String className) {
        return MAXINE_CODE_BASE_PACKAGES.contains(getPackageName(className));
    }

    /**
     * Determines if a given type descriptor denotes a class that is part of the Maxine code base.
     */
    public static boolean isMaxineClass(TypeDescriptor typeDescriptor) {
        return isMaxineClass(typeDescriptor.toJavaString());
    }

    /**
     * Determines if a given class actor denotes a class that is part of the Maxine code base.
     */
    public static boolean isMaxineClass(ClassActor classActor) {
        return isMaxineClass(classActor.typeDescriptor);
    }

    public static void setExitCode(int code) {
        exitCode = code;
    }

    /**
     * Entry point called by the substrate.
     *
     * ATTENTION: this signature must match 'VMRunMethod' in "Native/substrate/maxine.c"
     *
     * VM startup, initialization and exit code reporting routine running in the primordial native thread.
     *
     * This must work without having established a valid Java 'Thread' or 'VmThread'. Hence, no JNI callbacks are
     * supported in this routine.
     *
     * Also, there is no heap at first. In this early phase, we cannot allocate any objects.
     *
     * @return zero if everything works so far or an exit code if something goes wrong
     */
    @VM_ENTRY_POINT
    public static int run(Pointer bootHeapRegionStart, Word nativeOpenDynamicLibrary, Word dlsym, Word dlerror, Pointer jniEnv, Pointer jmmInterface, int argc, Pointer argv) {
        // This one field was not marked by the data prototype for relocation
        // to avoid confusion between "offset zero" and "null".
        // Fix it manually:
        Heap.bootHeapRegion.setStart(bootHeapRegionStart);

        Pointer tla = primordialETLA;

        Safepoint.setLatchRegister(tla);

        // The primordial thread should never allocate from the heap
        Heap.disableAllocationForCurrentThread();

        // The dynamic linker must be initialized before linking critical native methods
        DynamicLinker.initialize(nativeOpenDynamicLibrary, dlsym, dlerror);

        // Link the critical native methods:
        CriticalNativeMethod.linkAll();

        // Initialize the trap system:
        Trap.initialize();

        ImmortalHeap.initialize();

        NativeInterfaces.initialize(jniEnv, jmmInterface);

        // Perhaps this should be later, after VM has initialized
        startupTime = System.currentTimeMillis();

        vmConfig().initializeSchemes(MaxineVM.Phase.PRIMORDIAL);

        MaxineVM vm = vm();
        vm.phase = Phase.PRISTINE;

        if (VMOptions.parsePristine(argc, argv)) {
            if (!VMOptions.earlyVMExitRequested()) {
                VmThread.createAndRunMainThread();
            }
        }
        return exitCode;
    }

    public static String getExecutablePath() {
        try {
            return CString.utf8ToJava(native_executablePath());
        } catch (Utf8Exception e) {
            throw FatalError.unexpected("Could not convert C string value of executable path to a Java string");
        }
    }

    /**
     * Request the given method to be statically compiled in the boot image.
     */
    @HOSTED_ONLY
    public static void registerImageMethod(ClassMethodActor imageMethod) {
        CompiledPrototype.registerVMEntryPoint(imageMethod);
    }

    /**
     * Request the given method to be statically compiled in the boot image.
     */
    @HOSTED_ONLY
    public static void registerImageInvocationStub(MethodActor imageMethodActorWithInvocationStub) {
        CompiledPrototype.registerImageInvocationStub(imageMethodActorWithInvocationStub);
    }

    @HOSTED_ONLY
    public static void registerCriticalMethod(CriticalMethod criticalEntryPoint) {
        registerImageMethod(criticalEntryPoint.classMethodActor);
    }

    /*
     * Global native functions: these functions implement a thin layer over basic native
     * services that are needed to implement higher-level Java VM services. Note that
     * these native functions *ONLY* work on the target VM, not in bootstrapping or
     * inspecting modes.
     *
     * These service methods cannot block, and cannot use object references.
     */

    @C_FUNCTION
    public static native long native_nanoTime();

    @C_FUNCTION
    public static native long native_currentTimeMillis();

    @C_FUNCTION
    public static native Pointer native_executablePath();

    @C_FUNCTION
    public static native Pointer native_environment();

    /**
     * Gets a pointer to a C struct whose fields are NULL terminated C char arrays. The fields of this struct are read
     * and converted to {@link String} values by {@link NativeProperty#value(Pointer)}. The {@code native_properties_t}
     * struct declaration is in Native/substrate/maxine.c.
     */
    @C_FUNCTION
    public static native Pointer native_properties();

    @C_FUNCTION
    public static native float native_parseFloat(Pointer pointer, float nan);

    @C_FUNCTION
    public static native void native_exit(int code);

    @C_FUNCTION
    public static native void native_trap_exit(int code, Address address);

    @C_FUNCTION
    public static native void core_dump();

    public final MaxRiRuntime runtime;
    public final VMConfiguration config;
    public Phase phase = Phase.BOOTSTRAPPING;
    public final RegisterConfigs registerConfigs;
    public final Stubs stubs;
    public final Safepoint safepoint;
    public final TrapStateAccess trapStateAccess;

    public MaxineVM(VMConfiguration configuration) {
        this.config = configuration;
        this.runtime = new MaxRiRuntime();
        this.registerConfigs = RegisterConfigs.create();
        this.stubs = new Stubs(registerConfigs);
        this.safepoint = Safepoint.create();
        this.trapStateAccess = TrapStateAccess.create();
    }

    public static void reportPristineMemoryFailure(String memoryAreaName, String operation, Size numberOfBytes) {
        Log.println("Error occurred during initialization of VM");
        Log.print("Failed to ");
        Log.print(operation);
        Log.print(' ');
        Log.print(numberOfBytes.toLong());
        Log.print(" bytes of memory for ");
        Log.println(memoryAreaName);
        MaxineVM.native_exit(1);
    }
}
