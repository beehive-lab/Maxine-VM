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

import static com.sun.max.vm.VMOptions.*;

import java.lang.reflect.*;
import java.util.*;

import com.sun.max.*;
import com.sun.max.annotate.*;
import com.sun.max.asm.*;
import com.sun.max.lang.*;
import com.sun.max.program.*;
import com.sun.max.unsafe.*;
import com.sun.max.util.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.code.*;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.jni.*;
import com.sun.max.vm.object.host.*;
import com.sun.max.vm.prototype.*;
import com.sun.max.vm.prototype.BootImage.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.thread.*;
import com.sun.max.vm.type.*;

/**
 * Maxine.
 * @author Paul Caprioli
 */
public final class MaxineVM {

    @PROTOTYPE_ONLY
    private static ThreadLocal<MaxineVM> hostOrTarget = new ThreadLocal<MaxineVM>() {
        @Override
        protected synchronized MaxineVM initialValue() {
            return host();
        }
    };

    public static final String VERSION = "0.2";
    public static final int HARD_EXIT_CODE = -2;

    private static final List<String> MAXINE_CODE_BASE_LIST = new ArrayList<String>();
    private static final String MAXINE_CLASS_PACKAGE_PREFIX = new com.sun.max.Package().name();
    private static final String MAXINE_TEST_CLASS_PACKAGE_PREFIX = "test." + MAXINE_CLASS_PACKAGE_PREFIX;
    private static final String EXTENDED_CODEBASE_PROPERTY = "max.extended.codebase";

    /**
     * The signature of {@link #run(Pointer, Pointer, Word, Word, Word, int, Pointer)}.
     */
    public static final SignatureDescriptor RUN_METHOD_SIGNATURE;

    @PROTOTYPE_ONLY
    private static final Class[] RUN_METHOD_PARAMETER_TYPES;

    @PROTOTYPE_ONLY
    private static final Map<Class, Boolean> PROTOTYPE_CLASSES = new HashMap<Class, Boolean>();

    private static final VMOption HELP_OPTION = register(new VMOption("-help", "Prints this help message."), MaxineVM.Phase.PRISTINE);
    private static final VMOption EA_OPTION = register(new VMOption("-ea", "Enables assertions in user code.  Currently unimplemented."), MaxineVM.Phase.PRISTINE);

    @PROTOTYPE_ONLY
    private static MaxineVM globalHostOrTarget = null;

    /**
     * Assigned twice by the prototype generator. First, assigned to the host VM during
     * {@linkplain Prototype#initializeHost() prototype initialization}. Second, all references to the host MaxineVM
     * object are {@linkplain HostObjectAccess#hostToTarget(Object) swapped} to be references to the target MaxineVM
     * object as the boot image is being generated.
     */
    @CONSTANT
    private static MaxineVM host;

    /**
     * Assigned only once by the prototype generator.
     */
    @CONSTANT
    private static MaxineVM target;

    /**
     * The primordial thread locals.
     *
     * The address of this field is exposed to native code via {@link Header#primordialThreadLocalsOffset}
     * so that it can be initialized by the C substrate. It also enables a debugger attached to the VM to find it.
     */
    private static Pointer primordialThreadLocals;

    private static int exitCode = 0;

    public final VMConfiguration configuration;
    private Phase phase = Phase.PROTOTYPING;


    static {
        MAXINE_CODE_BASE_LIST.add(MAXINE_CLASS_PACKAGE_PREFIX);
        MAXINE_CODE_BASE_LIST.add(MAXINE_TEST_CLASS_PACKAGE_PREFIX);
        final String p = System.getProperty(EXTENDED_CODEBASE_PROPERTY);
        if (p != null) {
            final String[] parts = p.split(",");
            for (int i = 0; i < parts.length; i++) {
                MAXINE_CODE_BASE_LIST.add(parts[i]);
            }
        }

        Method runMethod = null;
        for (Method method : MaxineVM.class.getDeclaredMethods()) {
            if (method.getName().equals("run")) {
                ProgramError.check(runMethod == null, "There must only be one method named \"run\" in " + MaxineVM.class);
                runMethod = method;
            }
        }
        RUN_METHOD_PARAMETER_TYPES = runMethod.getParameterTypes();
        RUN_METHOD_SIGNATURE = SignatureDescriptor.create(runMethod.getReturnType(), runMethod.getParameterTypes());
    }

    public enum Phase {
        /**
         * Running on a host VM in order to construct a target VM or to run tests.
         */
        PROTOTYPING,

        /**
         * Creating the compiled prototype.
         */
        CREATING_COMPILED_PROTOTYPE,

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

    public static void initialize(VMConfiguration hostVMConfiguration, VMConfiguration targetVMConfiguration) {
        host = new MaxineVM(hostVMConfiguration);
        target = new MaxineVM(targetVMConfiguration);
    }

    public static String name() {
        return "Maxine VM";
    }

    public static String description() {
        return "The Maxine Virtual Machine, see <http://kenai.com.projects/maxine>";
    }

    public static void writeInitialVMParams() {
        native_writeMaxMemory(Heap.maxSize().toLong());
        native_writeTotalMemory(Heap.maxSize().toLong());
        // TODO: write a sensible value here, and keep native space up to date
        native_writeFreeMemory(Heap.maxSize().toLong() >> 1);
    }

    @PROTOTYPE_ONLY
    public static boolean isHostInitialized() {
        return host != null;
    }

    public static MaxineVM host() {
        if (host == null) {
            Prototype.initializeHost();
        }
        return host;
    }

    @UNSAFE
    @LOCAL_SUBSTITUTION
    private static MaxineVM host_() {
        return host;
    }

    /**
     * This differs from 'host()' only while running the prototype generator.
     *
     * When prototyping return the VM that is being generated else return the new host VM (that has once been
     * generated as a "target").
     *
     * @return the prototype generator's "target" VM
     */
    @INLINE
    public static MaxineVM target() {
        if (target != null) {
            Prototype.initializeHost();
        }
        return target;
    }

    @UNSAFE
    @LOCAL_SUBSTITUTION
    private static MaxineVM target_() {
        return target;
    }

    @PROTOTYPE_ONLY
    public static void setTarget(MaxineVM vm) {
        target = vm;
    }

    /**
     * Runs a given command that is not expected to throw a checked exception in the context of the target VM
     * configuration.
     */
    public static void usingTarget(Runnable runnable) {
        if (isPrototyping()) {
            final MaxineVM vm = hostOrTarget.get();
            hostOrTarget.set(target());
            runnable.run();
            hostOrTarget.set(vm);
        } else {
            runnable.run();
        }
    }

    /**
     * Runs a given function that is not expected to throw a checked exception in the context of the target VM
     * configuration.
     */
    public static <Result_Type> Result_Type usingTarget(Function<Result_Type> function) {
        if (isPrototyping()) {
            final MaxineVM vm = hostOrTarget.get();
            hostOrTarget.set(target());
            try {
                return function.call();
            } catch (RuntimeException runtimeException) {
                // re-throw runtime exceptions.
                throw runtimeException;
            } catch (Exception exception) {
                throw ProgramError.unexpected(exception);
            } finally {
                hostOrTarget.set(vm);
            }
        }
        try {
            return function.call();
        } catch (RuntimeException runtimeException) {
            // re-throw runtime exceptions.
            throw runtimeException;
        } catch (Exception exception) {
            throw ProgramError.unexpected(exception);
        }
    }

    /**
     * Runs a given function that may throw a checked exception in the context of the target VM configuration.
     */
    public static <Result_Type> Result_Type usingTargetWithException(Function<Result_Type> function) throws Exception {
        if (isPrototyping()) {
            final MaxineVM vm = hostOrTarget.get();
            hostOrTarget.set(target());
            try {
                return function.call();
            } finally {
                hostOrTarget.set(vm);
            }
        }
        return function.call();
    }

    /**
     * Sets the single unique VM context. Subsequent to the call, all calls to {@link #hostOrTarget()} will return
     * the {@code vm} value passed to this method.
     *
     * @param vm the global VM object that will be the answer to all subsequent requests for a hot or target VM context request
     */
    @PROTOTYPE_ONLY
    public static void setGlobalHostOrTarget(MaxineVM vm) {
        globalHostOrTarget = vm;
    }

    /**
     * Gets the current VM context. All {@linkplain VMConfiguration configurable} parts of the VM consult
     * the current VM context via this method.
     * @return
     */
    @UNSAFE
    @FOLD
    public static MaxineVM hostOrTarget() {
        if (isPrototyping()) {
            if (globalHostOrTarget != null) {
                return globalHostOrTarget;
            }
            return hostOrTarget.get();
        }
        return host;
    }

    // Substituted by isPrototyping_()
    @UNSAFE
    public static boolean isPrototyping() {
        return true;
    }

    @UNSAFE
    @LOCAL_SUBSTITUTION
    @FOLD
    public static boolean isPrototyping_() {
        return false;
    }

    /**
     * Determines if this is a {@link BuildLevel#DEBUG debug} build of the VM.
     */
    @UNSAFE
    @FOLD
    public static boolean isDebug() {
        return target().configuration.debugging();
    }

    /**
     * Determines if a given constructor, field or method exists only for prototyping purposes and should not be part of
     * a generated target image.
     */
    @PROTOTYPE_ONLY
    public static boolean isPrototypeOnly(AccessibleObject member) {
        if (member.getAnnotation(PROTOTYPE_ONLY.class) != null) {
            return true;
        }
        return isPrototypeOnly(Classes.getDeclaringClass(member));
    }

    /**
     * Determines if a given class exists only for prototyping purposes and should not be part
     * of a generated target image. A class is determined to be a prototype-only class if any
     * of the following apply:
     *
     * 1. It is annotated with {@link PROTOTYPE_ONLY}.
     * 2. It is nested class in an {@linkplain Class#getEnclosingClass() enclosing} prototype-only class.
     * 3. It is in a {@linkplain MaxPackage#fromClass(Class) Maxine package} that is not a {@linkplain BasePackage base},
     *    {@linkplain AsmPackage assembler}, {@linkplain VMPackage VM} or test package.
     */
    @PROTOTYPE_ONLY
    public static boolean isPrototypeOnly(Class<?> javaClass) {
        final Boolean value = PROTOTYPE_CLASSES.get(javaClass);
        if (value != null) {
            return value.booleanValue();
        }

        if (javaClass.getAnnotation(PROTOTYPE_ONLY.class) != null) {
            PROTOTYPE_CLASSES.put(javaClass, Boolean.TRUE);
            return true;
        }

        final MaxPackage maxPackage = MaxPackage.fromClass(javaClass);
        if (maxPackage != null) {
            if (maxPackage.getClass().getSuperclass() == MaxPackage.class) {
                final boolean isTestPackage = maxPackage.name().startsWith("test.com.sun.max.");
                PROTOTYPE_CLASSES.put(javaClass, !isTestPackage);
                return !isTestPackage;
            }
        }

        final Class<?> enclosingClass = javaClass.getEnclosingClass();
        if (enclosingClass != null) {
            final boolean result = isPrototypeOnly(enclosingClass);
            PROTOTYPE_CLASSES.put(javaClass, Boolean.valueOf(result));
            return result;
        }
        PROTOTYPE_CLASSES.put(javaClass, Boolean.FALSE);
        return false;
    }

    public static boolean isPrimordial() {
        return host().phase == Phase.PRIMORDIAL;
    }

    public static boolean isPristine() {
        return host().phase == Phase.PRISTINE;
    }

    public static boolean isStarting() {
        return host().phase == Phase.STARTING;
    }

    public static boolean isPrimordialOrPristine() {
        final Phase phase = host().phase;
        return phase == Phase.PRIMORDIAL || phase == Phase.PRISTINE;
    }

    public static boolean isRunning() {
        return host().phase() == Phase.RUNNING;
    }

    /**
     * Determines if a given type descriptor denotes a class that is part of the Maxine code base.
     */
    public static boolean isMaxineClass(TypeDescriptor typeDescriptor) {
        final String className = typeDescriptor.toJavaString();
        for (int i = 0; i < MAXINE_CODE_BASE_LIST.size(); i++) {
            final String prefix = MAXINE_CODE_BASE_LIST.get(i);
            if (className.startsWith(prefix)) {
                return true;
            }
        }
        return false;
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

    public static Pointer primordialVmThreadLocals() {
        return primordialThreadLocals;
    }

    /**
     * Used by the inspector only.
     */
    @PROTOTYPE_ONLY
    public static Class[] runMethodParameterTypes() {
        return RUN_METHOD_PARAMETER_TYPES.clone();
    }

    /**
     * Entry point called by the substrate.
     *
     * ATTENTION: this signature must match 'VMRunMethod' in "Native/substrate/maxine.c"
     * ATTENTION: If you change this signature, you must also change the result returned by @linkplain{runMethodParameterTypes}
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
    @C_FUNCTION
    private static int run(Pointer bootHeapRegionStart, Pointer auxiliarySpace, Word nativeOpenDynamicLibrary, Word dlsym, Word dlerror, int argc, Pointer argv) {
        // This one field was not marked by the data prototype for relocation
        // to avoid confusion between "offset zero" and "null".
        // Fix it manually:
        Heap.bootHeapRegion.setStart(bootHeapRegionStart);

        Pointer vmThreadLocals = primordialThreadLocals;

        Safepoint.initializePrimordial(vmThreadLocals);

        Heap.initializeAuxiliarySpace(vmThreadLocals, auxiliarySpace);

        // This must be called first as subsequent actions depend on it to resolve the native symbols
        DynamicLinker.initialize(nativeOpenDynamicLibrary, dlsym, dlerror);

        // Link the critical native methods:
        CriticalNativeMethod.linkAll();

        // Initialize the trap system:
        Trap.initialize();

        Code.initialize();

        ImmortalHeap.initialize();

        JniNativeInterface.initialize();

        VMConfiguration.target().initializeSchemes(MaxineVM.Phase.PRIMORDIAL);

        hostOrTarget().setPhase(MaxineVM.Phase.PRISTINE);

        if (VMOptions.parsePristine(argc, argv)) {
            if (HELP_OPTION.isPresent()) {
                VMOptions.printUsage();
            } else {
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
    @PROTOTYPE_ONLY
    public static void registerImageMethod(ClassMethodActor imageMethod) {
        CompiledPrototype.registerImageMethod(imageMethod);
    }

    /**
     * Request the given method to be statically compiled in the boot image.
     */
    @PROTOTYPE_ONLY
    public static void registerImageInvocationStub(MethodActor imageMethodActorWithInvocationStub) {
        CompiledPrototype.registerImageInvocationStub(imageMethodActorWithInvocationStub);
    }

    @PROTOTYPE_ONLY
    public static void registerCriticalMethod(CriticalMethod criticalEntryPoint) {
        registerImageMethod(criticalEntryPoint.classMethodActor);
    }

    /*
     * Global native functions: these functions implement a thin layer over basic native
     * services that are needed to implement higher-level Java VM services. Note that
     * these native functions *ONLY* work on the target VM, not in prototyping or
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
    public static native void native_exit(int code);

    @C_FUNCTION
    public static native void native_trap_exit(int code, Address address);

    @C_FUNCTION
    public static native void native_writeMaxMemory(long maxMem);

    @C_FUNCTION
    public static native void native_writeTotalMemory(long totalMem);

    @C_FUNCTION
    public static native void native_writeFreeMemory(long freeMem);


    public MaxineVM(VMConfiguration vmConfiguration) {
        configuration = vmConfiguration;
    }

    public Phase phase() {
        return phase;
    }

    public void setPhase(Phase phase) {
        this.phase = phase;
    }

}
