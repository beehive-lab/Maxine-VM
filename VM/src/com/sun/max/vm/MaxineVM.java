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


import java.lang.reflect.*;
import java.util.*;

import com.sun.max.annotate.*;
import com.sun.max.lang.*;
import com.sun.max.program.*;
import com.sun.max.unsafe.*;
import com.sun.max.util.*;
import com.sun.max.vm.actor.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.code.*;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.jni.*;
import com.sun.max.vm.object.host.*;
import com.sun.max.vm.prototype.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.thread.*;
import com.sun.max.vm.type.*;

public final class MaxineVM {

    private static final String _VERSION = "0.1.3-alpha";

    public static String version() {
        return _VERSION;
    }

    public static String name() {
        return "Maxine VM";
    }

    public static final int HARD_EXIT_CODE = -2;

    public enum Phase {
        /**
         * Running on a host VM in order to construct a target VM or to run tests.
         */
        PROTOTYPING,

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
        RUNNING
    }

    private Phase _phase = Phase.PROTOTYPING;
    private final VMConfiguration _configuration;

    public MaxineVM(VMConfiguration vmConfiguration) {
        _configuration = vmConfiguration;
    }

    public static void initialize(VMConfiguration hostVMConfiguration, VMConfiguration targetVMConfiguration) {
        _host = new MaxineVM(hostVMConfiguration);
        _target = new MaxineVM(targetVMConfiguration);
    }

    public Phase phase() {
        return _phase;
    }

    public void setPhase(Phase phase) {
        _phase = phase;
    }

    @INLINE
    public VMConfiguration configuration() {
        return _configuration;
    }

    public static void writeInitialVMParams() {
        native_writeMaxMemory(Heap.maxSize().toLong());
        native_writeTotalMemory(Heap.maxSize().toLong());
        // TODO: write a sensible value here, and keep native space up to date
        native_writeFreeMemory(Heap.maxSize().toLong() / 2);
    }

    /**
     * Assigned twice by the prototype generator. First, assigned to the host VM during
     * {@linkplain Prototype#initializeHost() prototype initialization}. Second, all references to the host MaxineVM
     * object are {@linkplain HostObjectAccess#hostToTarget(Object) swapped} to be references to the target MaxineVM
     * object as the boot image is being generated.
     */
    @CONSTANT
    private static MaxineVM _host;

    @PROTOTYPE_ONLY
    public static boolean isHostInitialized() {
        return _host != null;
    }

    /**
     * @return the host VM that is currently running on the underlying hardware or simulator
     */
    public static MaxineVM host() {
        if (_host == null) {
            Prototype.initializeHost();
        }
        return _host;
    }

    @UNSAFE
    @SURROGATE
    private static MaxineVM host_() {
        return _host;
    }

    /**
     * Assigned only once by the prototype generator.
     */
    @CONSTANT
    private static MaxineVM _target;

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
        if (_target != null) {
            Prototype.initializeHost();
        }
        return _target;
    }

    @UNSAFE
    @SURROGATE
    private static MaxineVM target_() {
        return _target;
    }

    @PROTOTYPE_ONLY
    public static void setTarget(MaxineVM target) {
        _target = target;
    }

    private static ThreadLocal<MaxineVM> _hostOrTarget = new ThreadLocal<MaxineVM>() {
        @Override
        protected synchronized MaxineVM initialValue() {
            return host();
        }
    };

    /**
     * Runs a given command that is not expected to throw a checked exception in the context of the target VM
     * configuration.
     */
    public static void usingTarget(Runnable runnable) {
        final MaxineVM vm = _hostOrTarget.get();
        _hostOrTarget.set(target());
        runnable.run();
        _hostOrTarget.set(vm);
    }

    /**
     * Runs a given function that is not expected to throw a checked exception in the context of the target VM
     * configuration.
     */
    public static <Result_Type> Result_Type usingTarget(Function<Result_Type> function) {
        final MaxineVM vm = _hostOrTarget.get();
        _hostOrTarget.set(target());
        try {
            return function.call();
        } catch (RuntimeException runtimeException) {
            // rethrow runtime exceptions.
            throw runtimeException;
        } catch (Exception exception) {
            throw ProgramError.unexpected(exception);
        } finally {
            _hostOrTarget.set(vm);
        }
    }

    /**
     * Runs a given function that may throw a checked exception in the context of the target VM configuration.
     */
    public static <Result_Type> Result_Type usingTargetWithException(Function<Result_Type> function) throws Exception {
        final MaxineVM vm = _hostOrTarget.get();
        _hostOrTarget.set(target());
        try {
            return function.call();
        } finally {
            _hostOrTarget.set(vm);
        }
    }

    private static MaxineVM _globalHostOrTarget = null;

    /**
     * The MaxineInspector uses this to direct all AWT event threads to use a certain VM without the need to wrap all
     * event listeners in VM.usingTarget().
     */
    public static void setGlobalHostOrTarget(MaxineVM vm) {
        _globalHostOrTarget = vm;
    }

    @UNSAFE
    @FOLD
    public static MaxineVM hostOrTarget() {
        if (_globalHostOrTarget != null) {
            return _globalHostOrTarget;
        }
        return _hostOrTarget.get();
    }

    // Substituted by isPrototyping_()
    @UNSAFE
    public static boolean isPrototyping() {
        return target() != host();
    }

    @UNSAFE
    @SURROGATE
    @FOLD
    public static boolean isPrototyping_() {
        return false;
    }

    /**
     * Determines if a given class, field or method actor exists only for prototyping and purposes should not be part of
     * a generated target image.
     */
    @PROTOTYPE_ONLY
    public static boolean isPrototypeOnly(Actor actor) {
        return actor.getAnnotation(PROTOTYPE_ONLY.class) != null;
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
     * Determines if a given class exists only for prototyping purposes and should not be part of a generated target
     * image.
     */
    @PROTOTYPE_ONLY
    public static boolean isPrototypeOnly(Class<?> javaClass) {
        if (javaClass.getAnnotation(PROTOTYPE_ONLY.class) != null) {
            return true;
        }
        final Class<?> enclosingClass = javaClass.getEnclosingClass();
        if (enclosingClass == null) {
            return false;
        }
        return isPrototypeOnly(enclosingClass);
    }

    public static boolean isPrimordial() {
        return host()._phase == Phase.PRIMORDIAL;
    }

    public static boolean isPristine() {
        return host()._phase == Phase.PRISTINE;
    }

    public static boolean isStarting() {
        return host()._phase == Phase.STARTING;
    }

    public static boolean isPrimordialOrPristine() {
        final Phase phase = host()._phase;
        return phase == Phase.PRIMORDIAL || phase == Phase.PRISTINE;
    }

    public static boolean isRunning() {
        return host().phase() == Phase.RUNNING;
    }

    private static final List<String> _maxineCodeBaseList = new ArrayList<String>();

    private static final String MAXINE_CLASS_PACKAGE_PREFIX = new com.sun.max.Package().name();
    private static final String MAXINE_TEST_CLASS_PACKAGE_PREFIX = "test." + MAXINE_CLASS_PACKAGE_PREFIX;
    private static final String EXTENDED_CODEBASE_PROPERTY = "max.extended.codebase";

    static {
        _maxineCodeBaseList.add(MAXINE_CLASS_PACKAGE_PREFIX);
        _maxineCodeBaseList.add(MAXINE_TEST_CLASS_PACKAGE_PREFIX);
        final String p = System.getProperty(EXTENDED_CODEBASE_PROPERTY);
        if (p != null) {
            final String[] parts = p.split(",");
            for (int i = 0; i < parts.length; i++) {
                _maxineCodeBaseList.add(parts[i]);
            }
        }
    }

    /**
     * Determines if a given type descriptor denotes a class that is part of the Maxine code base.
     */
    public static boolean isMaxineClass(TypeDescriptor typeDescriptor) {
        final String className = typeDescriptor.toJavaString();
        for (int i = 0; i < _maxineCodeBaseList.size(); i++) {
            final String prefix = _maxineCodeBaseList.get(i);
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
        return isMaxineClass(classActor.typeDescriptor());
    }

    private static int _exitCode = 0;

    public static void setExitCode(int exitCode) {
        _exitCode = exitCode;
    }

    private static Pointer _primordialVmThreadLocals;

    public static Pointer primordialVmThreadLocals() {
        return _primordialVmThreadLocals;
    }

    @PROTOTYPE_ONLY
    private static final Class[] _runMethodParameterTypes;
    static {
        Method runMethod = null;
        for (Method method : MaxineVM.class.getDeclaredMethods()) {
            if (method.getName().equals("run")) {
                ProgramError.check(runMethod == null, "There must only be one method named \"run\" in " + MaxineVM.class);
                runMethod = method;
            }
        }
        _runMethodParameterTypes = runMethod.getParameterTypes();
    }

    /**
     * Used by the inspector only.
     */
    @PROTOTYPE_ONLY
    public static  Class[] runMethodParameterTypes() {
        return _runMethodParameterTypes.clone();
    }

    private static final VMOption _helpOption = new VMOption("-help", "Prints this help message.", MaxineVM.Phase.PRISTINE);

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
    private static int run(Pointer primordialVmThreadLocals, Pointer bootHeapRegionStart, Pointer auxiliarySpace, Word nativeOpenDynamicLibrary, Word dlsym, Word dlerror, int argc, Pointer argv) {
        // This one field was not marked by the data prototype for relocation
        // to avoid confusion between "offset zero" and "null".
        // Fix it manually:
        Heap.bootHeapRegion().setStart(bootHeapRegionStart);

        Safepoint.initializePrimordial(primordialVmThreadLocals);

        Heap.initializeAuxiliarySpace(primordialVmThreadLocals, auxiliarySpace);

        // As of here we can write values:
        _primordialVmThreadLocals = primordialVmThreadLocals;

        // This must be called first as subsequent actions depend on it to resolve the native symbols
        DynamicLinker.initialize(nativeOpenDynamicLibrary, dlsym, dlerror);

        // Link the critical native methods:
        CriticalNativeMethod.linkAll();

        // Initialize the trap system:
        Trap.initialize();

        Code.initialize();

        JniNativeInterface.initialize();

        VMConfiguration.target().initializeSchemes(MaxineVM.Phase.PRIMORDIAL);

        hostOrTarget().setPhase(MaxineVM.Phase.PRISTINE);

        if (VMOptions.parsePristine(argc, argv)) {
            if (_helpOption.isPresent()) {
                VMOptions.printUsage();
            } else {
                VmThread.createAndRunMainThread();
            }
        }
        return _exitCode;
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
        registerImageMethod(criticalEntryPoint.classMethodActor());
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
}
