/*
 * Copyright (c) 2011, 2012, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.max.vm.ext.vma.run.java;

import static com.sun.max.vm.MaxineVM.*;

import java.io.*;
import java.lang.ref.*;
import java.lang.reflect.*;
import java.util.*;

import com.oracle.max.vm.ext.t1x.vma.*;
import com.oracle.max.vm.ext.vma.*;
import com.oracle.max.vm.ext.vma.handlers.store.vmlog.h.*;
import com.oracle.max.vm.ext.vma.options.*;
import com.sun.max.annotate.*;
import com.sun.max.program.ProgramError;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.compiler.RuntimeCompiler.*;
import com.sun.max.vm.compiler.deopt.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.jdk.*;
import com.sun.max.vm.log.*;
import com.sun.max.vm.object.*;
import com.sun.max.vm.run.java.JavaRunScheme;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.thread.VmThread;
import com.sun.max.vm.thread.VmThreadLocal;
import com.sun.max.vm.ti.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.ext.jvmti.*;

/**
 * Variant of {@link JavaRunScheme} that supports the VMA framework.
 *
 */

public class VMAJavaRunScheme extends JavaRunScheme implements JVMTIException.VMAHandler {
    private static class VMTIHandler extends NullVMTIHandler {
        @Override
        public void threadStart(VmThread vmThread) {
            if (advising) {
                adviceHandler.adviseBeforeThreadStarting(VmThread.current());
                enableAdvising();
            }
        }

        @Override
        public void threadEnd(VmThread vmThread) {
            if (advising) {
                disableAdvising();
                adviceHandler.adviseBeforeThreadTerminating(VmThread.current());
            }
        }

        @Override
        public void beginGC() {
            if (VMAJavaRunScheme.isThreadAdvising()) {
                VMAJavaRunScheme.disableAdvising();
                VMAJavaRunScheme.adviceHandler().adviseBeforeGC();
                VMAJavaRunScheme.enableAdvising();
            }
        }

        @Override
        public void endGC() {
            if (VMAJavaRunScheme.isThreadAdvising()) {
                VMAJavaRunScheme.disableAdvising();
                VMAJavaRunScheme.adviceHandler().adviseAfterGC();
                VMAJavaRunScheme.enableAdvising();
            }
        }

    }

    /**
     * A thread local variable that is used to support VM advising, in
     * particular to indicate whether advising is currently enabled.
     */
    public static final VmThreadLocal VM_ADVISING = new VmThreadLocal(
            "VM_ADVISING", false, "For use by VM advising framework");

    /**
     * Set to true when {@link VMAOptions.VMA} is set AND the VM is in a state to start advising.
     */
    private static boolean advising;

    @CONSTANT_WHEN_NOT_ZERO
    private static VMLog vmaVMLog;

    /**
     * If a handler wishes to use {@link VMLogNativeThreadVariableVMA} then this property must be set
     * as it must be included in the boot image.
     */
    public static final String VMA_LOG_PROPERTY = "max.vma.vmlog";

    public static VMLog vmaVMLog() {
        return vmaVMLog;
    }

    /**
     * The build time specified {@link VMAdviceHandler}.
     */
    @CONSTANT_WHEN_NOT_ZERO
    private static VMAdviceHandler adviceHandler;

    /**
     * This property may be specified at boot image time to include a specific handler in the boot image.
     * The preferred approach, however, is to load the handler as a VM extension.
     */
    public static final String VMA_HANDLER_CLASS_PROPERTY = "max.vma.handler";

    public static String getHandlerClassName() {
        String handlerClassName = System.getProperty(VMA_HANDLER_CLASS_PROPERTY);
        if (handlerClassName == null) {
            // not specified for the boot image, loaded as VM extension
        }
        return handlerClassName;
    }

    public static boolean isHandlerClass(Class<? extends VMAdviceHandler> handlerClass) {
        String handlerClassName = getHandlerClassName();
        if (handlerClassName == null) {
            return false;
        } else {
            return handlerClassName.equals(handlerClass.getName());
        }
    }

    /**
     * For dynamically loaded advice handlers.
     * @param handler
     */
    public static void registerAdviceHandler(VMAdviceHandler handler) {
        adviceHandler = handler;
    }

    @Override
    public void initialize(MaxineVM.Phase phase) {
        super.initialize(phase);
        if (MaxineVM.isHosted() && phase == MaxineVM.Phase.BOOTSTRAPPING) {
            VMTI.registerEventHandler(new VMTIHandler());
            JVMTIException.registerVMAHAndler(this);
            String handlerClassName = getHandlerClassName();
            if (System.getProperty(VMA_LOG_PROPERTY) != null) {
                vmaVMLog = new VMLogNativeThreadVariableVMA();
                vmaVMLog.initialize(phase);
            }
            if (handlerClassName != null) {
                try {
                    adviceHandler = (VMAdviceHandler) Class.forName(handlerClassName).newInstance();
                } catch (Throwable ex) {
                    ProgramError.unexpected("failed to instantiate VMA advice handler class: ", ex);
                }
                adviceHandler.initialise(phase);
            }
        }
        if (phase == MaxineVM.Phase.RUNNING) {
            if (VMAOptions.VMA) {
                JDKDeopt.run();
                if (adviceHandler != null) {
                    adviceHandler.initialise(phase);
                    advising = true;
                } else {
                    Log.println("no VMA handler defined");
                    MaxineVM.exit(-1);
                }
            }
        } else if (phase == MaxineVM.Phase.TERMINATING) {
            if (advising) {
                disableAdvising();
                // N.B. daemon threads may still be running and invoking advice.
                // There is nothing we can do about that as they may be in the act
                // of logging so disabling advising for them would be meaningless.
                // This has to be dealt with in the handler.
                adviceHandler.initialise(phase);
            }
        }
    }

    @INLINE
    public static VMAdviceHandler adviceHandler() {
        return adviceHandler;
    }

    /**
     * Unconditionally enables advising for the current thread.
     */
    @INLINE
    public static void enableAdvising() {
        VM_ADVISING.store3(Address.fromLong(1));
    }

    /**
     * Unconditionally disables advising for the current thread.
     */
    @INLINE
    public static void disableAdvising() {
        VM_ADVISING.store3(Word.zero());
    }

    /**
     * Is the VM being run with advising turned on?
     */
    @INLINE
    public static boolean isVMAdvising() {
        return advising;
    }

    /**
     * Is advising enabled for the current thread?
     */
    @INLINE
    private static boolean isThreadAdvising() {
        return VmThread.currentTLA().getWord(VM_ADVISING.index) != Word.zero();
    }

    @Override
    public void exceptionRaised(ClassMethodActor throwingActor, Throwable throwable, int bci, int poppedFrames) {
        if (isThreadAdvising() && isInstrumented(throwingActor)) {
            disableAdvising();
            adviceHandler.adviseBeforeReturnByThrow(bci, throwable, poppedFrames);
            enableAdvising();
        }
    }

    private static boolean isInstrumented(ClassMethodActor classMethodActor) {
        return classMethodActor.currentTargetMethod() instanceof VMAT1XTargetMethod;
    }

    /**
     * Encapsulates all the logic to handle the recompilation and deoptimization of the JDK methods in the boot image,
     * that are subject to advising. Only methods that are capable of baseline compilation are candidates, which
     * excludes any substituted methods that use VM features not supported by the baseline compiler.
     */
    private static class JDKDeopt {

        static void run() {
            VMAOptions.logger.logJdkDeopt("checking boot image JDK classes");
            Collection<ClassActor> bootClassActors = ClassRegistry.BOOT_CLASS_REGISTRY.getClassActors();
            ArrayList<TargetMethod> deoptMethods = new ArrayList<TargetMethod>();
            for (ClassActor classActor : bootClassActors) {
                String className = classActor.qualifiedName();
                if (VMAOptions.instrumentClass(className)) {
                    for (StaticMethodActor staticMethodActor : classActor.localStaticMethodActors()) {
                        checkDeopt(staticMethodActor, deoptMethods);
                    }
                    for (VirtualMethodActor virtualMethodActor : classActor.localVirtualMethodActors()) {
                        checkDeopt(virtualMethodActor, deoptMethods);
                    }
                }
            }

            if (deoptMethods.size() == 0) {
                return;
            }

            VMAOptions.logger.logJdkDeopt("force compile key T1X needed methods");

            // Force the compilation of methods not compiled into the boot image when using C1X that
            // are needed when VMAT1X uses a T1X-compiled JDK, to break meta-circularity.
            // This list was experimentally determined.
            forceCompile(HashMap.class, "hash");
            forceCompile(HashMap.class, "indexFor");
            forceCompile(getClass("java.util.HashMap$Entry"), "<init>");
            forceCompile(ObjectAccess.class, "makeHashCode");
            forceCompile(AbstractList.class, "<init>");
            forceCompile(AbstractCollection.class, "<init>");
            if (JDK.JDK_VERSION == 6) {
                forceCompile(ArrayList.class, "ensureCapacity");
                forceCompile(ArrayList.class, "RangeCheck");
                forceCompile(getClass("java.lang.AbstractStringBuilder"), "ensureCapacity");
            } else {
                forceCompile(ArrayList.class, "ensureCapacityInternal");
                forceCompile(ArrayList.class, "rangeCheck");
                forceCompile(getClass("java.lang.AbstractStringBuilder"), "ensureCapacityInternal");
            }
            forceCompile(Array.class, "newInstance");
            forceCompile(Math.class, "min", SignatureDescriptor.fromJava(int.class, int.class, int.class));
            forceCompile(Math.class, "max", SignatureDescriptor.fromJava(int.class, int.class, int.class));
            forceCompile(ThreadLocal.class, "access$400");
            forceCompile(getClass("java.lang.ThreadLocal$ThreadLocalMap"), "access$000");
            forceCompile(getClass("java.lang.ThreadLocal$ThreadLocalMap"), "setThreshold");
            forceCompile(getClass("java.lang.ThreadLocal$ThreadLocalMap$Entry"), "<init>");
            forceCompile(Enum.class, "ordinal");
            forceCompile(EnumMap.class, "unmaskNull");
            forceCompile(FilterInputStream.class, "<init>");
            forceCompile(Reference.class, "<init>");
            forceCompile(Reference.class, "access$100");
            forceCompile(WeakReference.class, "<init>");
            forceCompile(String.class, "<init>", SignatureDescriptor.fromJava(void.class, int.class, int.class, char[].class));
            forceCompile(String.class, "lastIndexOf", SignatureDescriptor.fromJava(int.class, String.class));
            forceCompile(String.class, "lastIndexOf", SignatureDescriptor.fromJava(int.class, String.class, int.class));
            forceCompile(String.class, "substring", SignatureDescriptor.fromJava(String.class, int.class));
            forceCompile(Arrays.class, "copyOf", SignatureDescriptor.fromJava(char[].class, char[].class, int.class));
            forceCompile(BitSet.class, "wordIndex");
            forceCompile(Class.class, "getEnclosingMethodInfo");
            forceCompile(java.util.regex.Matcher.class, "getTextLength");

            // Compile the methods first, in case of a method used by the compilation system,
            // which would cause runaway recursion.
            Iterator<TargetMethod> iter = deoptMethods.iterator();
            while (iter.hasNext()) {
                TargetMethod tm = iter.next();
                boolean instrument = true;
                try {
                    vm().compilationBroker.compile(tm.classMethodActor, Nature.BASELINE, false, true);
                } catch (Throwable t) {
                    // some failure that (likely) can't easily be expressed in cantBaseline
                    iter.remove();
                    instrument = false;
                }
                VMAOptions.logger.logInstrument(tm.classMethodActor, instrument);
            }

            VMAOptions.logger.logJdkDeopt("start deoptimizing");
            new Deoptimization(deoptMethods).go();
            VMAOptions.logger.logJdkDeopt("done deoptimizing");

        }

        private static Class<?> getClass(String name) {
            try {
                return Class.forName(name);
            } catch (Throwable t) {
                FatalError.unexpected("can't find class " + name);
                return null;
            }
        }

        private static void forceCompile(Class<?> klass, String methodName) {
            forceCompile(klass, methodName, null);
        }

        @NEVER_INLINE
        private static void forceCompile(Class<?> klass, String methodName, SignatureDescriptor sig) {
            ClassMethodActor cma = ClassActor.fromJava(klass).findLocalClassMethodActor(SymbolTable.makeSymbol(methodName), sig);
            assert cma != null;
            cma.makeTargetMethod();
        }

        private static void checkDeopt(ClassMethodActor classMethodActor, ArrayList<TargetMethod> deoptMethods) {
            TargetMethod tm = classMethodActor.currentTargetMethod();
            if (tm != null && !tm.isBaseline()) {
                if (cantBaseline(classMethodActor)) {
                    VMAOptions.logger.logInstrument(tm.classMethodActor, false);
                } else {
                    deoptMethods.add(tm);
                }
            }
        }

        private static boolean cantBaseline(ClassMethodActor classMethodActor) {
            ClassMethodActor compilee = classMethodActor.compilee();
            return Actor.isUnsafe(compilee.flags()) || (compilee.flags() & (Actor.FOLD | Actor.INLINE)) != 0;
        }


    }
}
