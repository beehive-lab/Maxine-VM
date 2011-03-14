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
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.classfile.*;
import com.sun.max.vm.compiler.c1x.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.hosted.*;
import com.sun.max.vm.jni.*;
import com.sun.max.vm.runtime.*;
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

    public static final String NAME = "Maxine Virtual Machine";
    public static final int MAJOR_VERSION = 1;
    public static final int MINOR_VERSION = 0;
    public static final String VERSION_STRING = Integer.toString(MAJOR_VERSION) + "." + Integer.toString(MINOR_VERSION);
    public static final String HOME_URL = "http://labs.oracle.com/projects/maxine/";
    public static final int HARD_EXIT_CODE = -2;

    /**
     * The set of packages denoting classes for which {@link #isBootImageClass(String)} will return true.
     */
    @HOSTED_ONLY
    private static final Map<String, BootImagePackage> BOOT_IMAGE_CODE_BASE_PACKAGES = new HashMap<String, BootImagePackage>();

    @HOSTED_ONLY
    private static final Map<Class, Boolean> HOSTED_CLASSES = new HashMap<Class, Boolean>();

    @HOSTED_ONLY
    private static final Set<String> KEEP_CLINIT_CLASSES = new HashSet<String>();

    @HOSTED_ONLY
    private static final String[] TEST_PACKAGES = {"test.", "jtt."};

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
     * The exit code returned by the VM process.
     */
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
    public static void registerBootImagePackages(List<BootImagePackage> packages) {
        for (BootImagePackage pkg : packages) {
            BOOT_IMAGE_CODE_BASE_PACKAGES.put(pkg.name(), pkg);
        }
    }

    @HOSTED_ONLY
    public static void registerKeepClassInit(String className) {
        KEEP_CLINIT_CLASSES.add(className);
    }

    /**
     * Global variable determining whether class initializers are to be discarded
     * or preserved by the {@link ClassfileReader}.
     */
    @HOSTED_ONLY
    public static boolean preserveClinitMethods = System.getProperty("max.loader.preserveClinitMethods") != null;

    @HOSTED_ONLY
    public static boolean keepClassInit(TypeDescriptor classDescriptor) {
        final String className = classDescriptor.toJavaString();
        final boolean result = preserveClinitMethods || KEEP_CLINIT_CLASSES.contains(className);
        return result;
    }

    public static String name() {
        return NAME;
    }

    public static String description() {
        return "The " + NAME + " Ver. " + VERSION_STRING + "  <" + HOME_URL + ">";
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
     * Determines if a given class exists only for hosted execution and should not be part of a generated target image.
     * <p>
     * A <i>direct hosted-only class</i> is defined as a class that is:
     * <ul>
     * <li>or annotated with {@link HOSTED_ONLY}
     * <li>or in a package that ends with {@code ".hosted"}
     * <li>or annotated with {@link PLATFORM} that does not match the target platform
     * <li>
     * </ul>
     * <p>
     * A <i>hosted-only</i> class is defined as:
     * <ul>
     * <li>a direct hosted-only class
     * <li>or a subclass of a hosted-only class
     * <li>or a nested class in a hosted-only class
     * <li>or an array of {@code T} where {@code T} is a hosted-only class
     * </ul>
     *
     * @param javaClass the class to check
     * @return {@code true} if the class is hosted-only
     */
    @HOSTED_ONLY
    public static boolean isHostedOnly(Class< ? > javaClass) {
        // We keep a cache of what we know
        final Boolean value = HOSTED_CLASSES.get(javaClass);
        if (value != null) {
            return value;
        }

        boolean result = false; // default assumption
        final String pkgName = getPackageName(javaClass);

        // Direct part of definition

        if (javaClass.getAnnotation(HOSTED_ONLY.class) != null) {
            result = true;
        } else if (pkgName.endsWith(".hosted")) {
            // May want to replace this 'magic' interpretation of ".hosted"
            // with a sentinel class (e.g. HOSTED_ONLY_PACKAGE).
            result = true;
        } else if (!Platform.platform().isAcceptedBy(javaClass.getAnnotation(PLATFORM.class))) {
            result = true;
        } else {

            // Indirect part of definition, cover all the possible cases

            if (javaClass.isArray()) {
                final Class< ? > componentClass = javaClass.getComponentType();
                result = isHostedOnly(componentClass);
            } else {
                final Class superClass = javaClass.getSuperclass();
                if (superClass != null && isHostedOnly(superClass)) {
                    result = true;
                } else {
                    final Class< ? > enclosingClass = getEnclosingClass(javaClass);
                    if (enclosingClass != null && isHostedOnly(enclosingClass)) {
                        result = true;
                    }
                }
            }
        }
        HOSTED_CLASSES.put(javaClass, result);
        //Trace.line(1, "setHostedOnly: " + javaClass.getName() + " " + result);
        return result;
    }

    private static Class<?> getEnclosingClass(Class<?> javaClass) {
        try {
            final Class< ? > enclosingClass = javaClass.getEnclosingClass();
            return enclosingClass;
        } catch (LinkageError linkageError) {
            ProgramWarning.message("Error trying to get the enclosing class for " + javaClass + ": " + linkageError);
        }
        return null;
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
     * Determines if a given class name denotes a class that is specified as part of the boot image.
     * This cannot be based solely on the package name as the package may enumerate
     * a specific set of classes.
     */
    public static boolean isBootImageClass(String className) {
        BootImagePackage pkg = BOOT_IMAGE_CODE_BASE_PACKAGES.get(getPackageName(className));
        if (pkg != null) {
            // check for explicit class
            return pkg.isBootImageClass(className);
        }
        return false;
    }

    public static void setExitCode(int code) {
        exitCode = code;
    }

    /**
     * VM initialization point called by the substrate.
     *
     * ATTENTION: this signature must match 'VMRunMethod' in "Native/substrate/maxine.c"
     *
     * VM startup, initialization and exit code reporting routine running in the VM startup native thread.
     *
     * This must work without having established a valid Java 'Thread' or 'VmThread'. Hence, no JNI callbacks are
     * supported in this routine.
     *
     * Also, there is no heap at first. In this early phase, we cannot allocate any objects.
     *
     * @return 0 indicating initialization succeeded, non-0 if not
     */
    @VM_ENTRY_POINT
    public static int run(Pointer etla, Pointer bootHeapRegionStart, Word nativeOpenDynamicLibrary, Word dlsym, Word dlerror, Pointer jniEnv, Pointer jmmInterface, int argc, Pointer argv) {
        Safepoint.setLatchRegister(etla);

        // This one field was not marked by the data prototype for relocation
        // to avoid confusion between "offset zero" and "null".
        // Fix it manually:
        Heap.bootHeapRegion.setStart(bootHeapRegionStart);

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

        VMOptions.parsePristine(argc, argv);

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

    /**
     * Application-requested exit.
     * @param exitCode code to exit the VM process with.
     * @param halt  true if this is a halt, not an exit.
     */
    public static void exit(int exitCode, boolean halt) {
        // TODO: need to revisit this. Likely, we would want to bring all
        // threads to a safepoint before running the terminating phase.
        vmConfig().initializeSchemes(MaxineVM.Phase.TERMINATING);
        native_exit(exitCode);
    }
}
