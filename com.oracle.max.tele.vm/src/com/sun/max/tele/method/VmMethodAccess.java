/*
 * Copyright (c) 2007, 2012, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.tele.method;

import java.lang.reflect.*;
import java.util.*;

import com.sun.max.annotate.*;
import com.sun.max.io.*;
import com.sun.max.program.*;
import com.sun.max.tele.*;
import com.sun.max.tele.field.*;
import com.sun.max.tele.field.VmFieldAccess.InspectedMemberReifier;
import com.sun.max.tele.method.CodeLocation.VmCodeLocationManager;
import com.sun.max.tele.object.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.code.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.tele.*;
import com.sun.max.vm.thread.*;
import com.sun.max.vm.type.*;

/**
 * Singleton collection of access to specific methods in the VM.
 * <p>
 * The {@link INSPECTED} annotation is employed to denote methods that will be accessed remotely.
 * A field of the appropriate {@link TeleMethodAccess} subtype is generated into this file
 * by executing the {@link #main(String[])} method in this class (ensuring that the VM
 * class path contains all the {@code com.sun.max} classes).
 */
public final class VmMethodAccess extends AbstractVmHolder {

    private static final int TRACE_VALUE = 1;

    private static VmMethodAccess vmMethodsAccess;

    public static VmMethodAccess make(TeleVM vm, VmCodeLocationManager codeManager) {
        if (vmMethodsAccess == null) {
            vmMethodsAccess = new VmMethodAccess(vm, codeManager);
        }
        return vmMethodsAccess;
    }

    // Checkstyle: stop field name check

    // START GENERATED CONTENT
    public final TeleStaticMethodAccess Code_codePointerToTargetMethod = new TeleStaticMethodAccess(vm(), Code.class, "codePointerToTargetMethod", SignatureDescriptor.fromJava(TargetMethod.class, Pointer.class));
    public final TeleStaticMethodAccess CodeManager$Inspect_inspectableCodeEvictionCompleted = new TeleStaticMethodAccess(vm(), CodeManager.Inspect.class, "inspectableCodeEvictionCompleted", SignatureDescriptor.fromJava(void.class));
    public final TeleStaticMethodAccess CodeManager$Inspect_inspectableCodeEvictionStarted = new TeleStaticMethodAccess(vm(), CodeManager.Inspect.class, "inspectableCodeEvictionStarted", SignatureDescriptor.fromJava(void.class));
    public final TeleStaticMethodAccess HeapScheme$Inspect_inspectableDecreaseMemoryRequested = new TeleStaticMethodAccess(vm(), HeapScheme.Inspect.class, "inspectableDecreaseMemoryRequested", SignatureDescriptor.fromJava(void.class, Size.class));
    public final TeleStaticMethodAccess HeapScheme$Inspect_inspectableGCCompleted = new TeleStaticMethodAccess(vm(), HeapScheme.Inspect.class, "inspectableGCCompleted", SignatureDescriptor.fromJava(void.class));
    public final TeleStaticMethodAccess HeapScheme$Inspect_inspectableGCReclaiming = new TeleStaticMethodAccess(vm(), HeapScheme.Inspect.class, "inspectableGCReclaiming", SignatureDescriptor.fromJava(void.class));
    public final TeleStaticMethodAccess HeapScheme$Inspect_inspectableGCStarting = new TeleStaticMethodAccess(vm(), HeapScheme.Inspect.class, "inspectableGCStarting", SignatureDescriptor.fromJava(void.class));
    public final TeleStaticMethodAccess HeapScheme$Inspect_inspectableIncreaseMemoryRequested = new TeleStaticMethodAccess(vm(), HeapScheme.Inspect.class, "inspectableIncreaseMemoryRequested", SignatureDescriptor.fromJava(void.class, Size.class));
    public final TeleStaticMethodAccess HeapScheme$Inspect_inspectableObjectRelocated = new TeleStaticMethodAccess(vm(), HeapScheme.Inspect.class, "inspectableObjectRelocated", SignatureDescriptor.fromJava(void.class, Address.class, Address.class));
    public final TeleStaticMethodAccess InspectableCodeInfo_inspectableCodeEvictionCompleted = new TeleStaticMethodAccess(vm(), InspectableCodeInfo.class, "inspectableCodeEvictionCompleted", SignatureDescriptor.fromJava(void.class, CodeRegion.class));
    public final TeleStaticMethodAccess InspectableCodeInfo_inspectableCodeEvictionStarted = new TeleStaticMethodAccess(vm(), InspectableCodeInfo.class, "inspectableCodeEvictionStarted", SignatureDescriptor.fromJava(void.class, CodeRegion.class));
    public final TeleStaticMethodAccess InspectableCompilationInfo_inspectableCompilationCompleted = new TeleStaticMethodAccess(vm(), InspectableCompilationInfo.class, "inspectableCompilationCompleted", SignatureDescriptor.fromJava(void.class, String.class, String.class, String.class, TargetMethod.class));
    public final TeleStaticMethodAccess InspectableCompilationInfo_inspectableCompilationStarted = new TeleStaticMethodAccess(vm(), InspectableCompilationInfo.class, "inspectableCompilationStarted", SignatureDescriptor.fromJava(void.class, String.class, String.class, String.class));
    public final TeleStaticMethodAccess InspectableHeapInfo_inspectableDecreaseMemoryRequested = new TeleStaticMethodAccess(vm(), InspectableHeapInfo.class, "inspectableDecreaseMemoryRequested", SignatureDescriptor.fromJava(void.class, Size.class));
    public final TeleStaticMethodAccess InspectableHeapInfo_inspectableGCAllocating = new TeleStaticMethodAccess(vm(), InspectableHeapInfo.class, "inspectableGCAllocating", SignatureDescriptor.fromJava(void.class, long.class));
    public final TeleStaticMethodAccess InspectableHeapInfo_inspectableGCAnalyzing = new TeleStaticMethodAccess(vm(), InspectableHeapInfo.class, "inspectableGCAnalyzing", SignatureDescriptor.fromJava(void.class, long.class));
    public final TeleStaticMethodAccess InspectableHeapInfo_inspectableGCReclaiming = new TeleStaticMethodAccess(vm(), InspectableHeapInfo.class, "inspectableGCReclaiming", SignatureDescriptor.fromJava(void.class, long.class));
    public final TeleStaticMethodAccess InspectableHeapInfo_inspectableIncreaseMemoryRequested = new TeleStaticMethodAccess(vm(), InspectableHeapInfo.class, "inspectableIncreaseMemoryRequested", SignatureDescriptor.fromJava(void.class, Size.class));
    public final TeleStaticMethodAccess InspectableHeapInfo_inspectableObjectRelocated = new TeleStaticMethodAccess(vm(), InspectableHeapInfo.class, "inspectableObjectRelocated", SignatureDescriptor.fromJava(void.class, Address.class, Address.class));
    public final TeleStaticMethodAccess TargetBreakpoint_findOriginalCode = new TeleStaticMethodAccess(vm(), TargetBreakpoint.class, "findOriginalCode", SignatureDescriptor.fromJava(byte[].class, long.class));
    public final TeleStaticMethodAccess VmThread_detached = new TeleStaticMethodAccess(vm(), VmThread.class, "detached", SignatureDescriptor.fromJava(void.class));
    public final TeleStaticMethodAccess VmThread_run = new TeleStaticMethodAccess(vm(), VmThread.class, "run", SignatureDescriptor.fromJava(void.class, Pointer.class, Pointer.class, Pointer.class));
    // END GENERATED CONTENT

    // Checkstyle: resume field name check

    private final CodeLocation compilationStarted;
    private final CodeLocation compilationCompleted;
    private final CodeLocation gcAllocating;
    private final CodeLocation gcAnalyzing;
    private final CodeLocation gcReclaiming;
    private final CodeLocation vmThreadRunning;
    private final CodeLocation vmThreadDetached;

    private final List<CodeLocation> clientInspectableMethods;

    private VmMethodAccess(TeleVM vm, VmCodeLocationManager codeManager) {
        super(vm);
        // Uncomment to enable verifying that the generated content in this class is up to date when running the inspector
        // updateSource(true);

        // Note that for this to work correctly, the named methods must be compiled into the boot heap.

        compilationStarted = codeManager.createMachineCodeLocation(InspectableCompilationInfo_inspectableCompilationStarted, "Compilation start (internal)");
        compilationCompleted = codeManager.createMachineCodeLocation(InspectableCompilationInfo_inspectableCompilationCompleted, "Compilation complete (internal)");
        gcAllocating = codeManager.createMachineCodeLocation(InspectableHeapInfo_inspectableGCAllocating, "GC completed (internal)");
        gcAnalyzing = codeManager.createMachineCodeLocation(InspectableHeapInfo_inspectableGCAnalyzing, "GC starting (internal)");
        gcReclaiming = codeManager.createMachineCodeLocation(InspectableHeapInfo_inspectableGCReclaiming, "GC reclaiming (internal)");
        vmThreadRunning = codeManager.createMachineCodeLocation(VmThread_run, "VmThread running (internal) ");
        vmThreadDetached = codeManager.createMachineCodeLocation(VmThread_detached, "VmThread detached (internal) ");

        final List<CodeLocation> methods = new ArrayList<CodeLocation>();
        methods.add(codeManager.createMachineCodeLocation(HeapScheme$Inspect_inspectableGCStarting, "Start of GC analyzing"));
        methods.add(codeManager.createMachineCodeLocation(HeapScheme$Inspect_inspectableGCReclaiming, "Start of GC reclaiming"));
        methods.add(codeManager.createMachineCodeLocation(HeapScheme$Inspect_inspectableGCCompleted, "End of GC"));
        methods.add(codeManager.createMachineCodeLocation(HeapScheme$Inspect_inspectableObjectRelocated, "Object relocated"));
        methods.add(codeManager.createMachineCodeLocation(CodeManager$Inspect_inspectableCodeEvictionStarted, "Start of Code Eviction"));
        methods.add(codeManager.createMachineCodeLocation(CodeManager$Inspect_inspectableCodeEvictionCompleted, "End of Code Eviction"));
        clientInspectableMethods = Collections.unmodifiableList(methods);
    }

    /**
     * Identifies methods in the VM that can be offered to clients as convenient
     * stopping places.
     * <br>
     * <strong>Note</strong>: a clear separation is made between methods that are only
     * to be used by clients and those used by internal mechanisms.
     *
     * @return methods suitable for setting client-requested breakpoints.
     */
    public List<CodeLocation> clientInspectableMethods() {
        return clientInspectableMethods;
    }

    /**
     * @return a VM method for internal (non-client) use that is called when a method compilation starts
     */
    public CodeLocation compilationStartedMethodLocation() {
        return compilationStarted;
    }

    /**
     * @return a VM method for internal (non-client) use that is called when a method compilation finishes.
     */
    public CodeLocation compilationCompletedMethodLocation() {
        return compilationCompleted;
    }

    /**
     * @return a VM method for internal (non-client) use that is called whenever a thread enter its run method (equivalent to whenever a new thread is running) .
     */
    public CodeLocation vmThreadRunMethodLocation() {
        return vmThreadRunning;
    }

    /**
     * @return a VM method for internal (non-client) use that is called whenever a VmThread has detached itself from the active list.
     */
    public CodeLocation vmThreadDetachedMethodLocation() {
        return vmThreadDetached;
    }

    /**
     * @return a VM method for internal (non-client) use that is called just after each GC starts,
     * i.e. when it enters the {@link HeapPhase#ANALYZING} phase.
     */
    public CodeLocation gcAnalyzingMethodLocation() {
        return gcAnalyzing;
    }

    /**
     * @return a VM method for internal (non-client) use that is called just after each GC starts
     * reclaiming, i.e. when it enters the {@link HeapPhase#RECLAIMING} phase.
     */
    public CodeLocation gcReclaimingMethodLocation() {
        return gcAnalyzing;
    }

     /**
     * @return a VM method for internal (non-client) use that is called just after each GC end,
     * i.e. when it enters the {@link HeapPhase#ALLOCATING}.
     */
    public CodeLocation gcAllocatingMethodLocation() {
        return gcAllocating;
    }

    /**
     * Gets a representation of a method in the VM matching a particular key, null if not loaded.
     */
    public TeleClassMethodActor findClassMethodActor(MethodKey methodKey) {
        if (vm().tryLock()) {
            try {
                final TeleClassActor teleClassActor = classes().findTeleClassActor(methodKey.holder());
                if (teleClassActor != null) {
                    // the class has been loaded; find a matching method
                    for (TeleClassMethodActor teleClassMethodActor : teleClassActor.getTeleClassMethodActors()) {
                        MethodKey testMethodKey = new MethodKey.MethodActorKey(teleClassMethodActor.methodActor());
                        if (testMethodKey.equals(methodKey)) {
                            return teleClassMethodActor;
                        }
                    }
                }

            } finally {
                vm().unlock();
            }
        }
        return null;
    }

    /**
     * Gets a representation of a method in the VM matching a particular key, null if not loaded.
     */
    public TeleMethodActor findMethodActor(MethodKey methodKey) {
        if (vm().tryLock()) {
            try {
                final TeleClassActor teleClassActor = classes().findTeleClassActor(methodKey.holder());
                if (teleClassActor != null) {
                    // the class has been loaded; find a matching method
                    for (TeleMethodActor teleMethodActor : teleClassActor.getTeleMethodActors()) {
                        MethodKey testMethodKey = new MethodKey.MethodActorKey(teleMethodActor.methodActor());
                        if (testMethodKey.equals(methodKey)) {
                            return teleMethodActor;
                        }
                    }
                }

            } finally {
                vm().unlock();
            }
        }
        return null;
    }

    public static void main(String[] args) {
        Trace.begin(1, "VmMethods updating GENERATED CONTENT");
        Trace.on(1);
        Trace.end(1, "VmMethods updating GENERATED CONTENT");
        updateSource(false);
    }

    private static String javaName(Class c) {
        if (c.isArray()) {
            return javaName(c.getComponentType()) + "[]";
        }
        return c.getSimpleName();
    }

    private static void updateSource(boolean inInspector) {
        final InspectedMemberReifier<Method> methodReifier = new InspectedMemberReifier<Method>() {
            public void reify(Method method, IndentWriter writer) {
                final Class c = method.getDeclaringClass();
                final boolean isStatic = Modifier.isStatic(method.getModifiers());
                final String holder = c.getName().substring(c.getPackage().getName().length() + 1);
                final String name = method.getName();
                String sig = "SignatureDescriptor.fromJava(" + javaName(method.getReturnType()) + ".class";
                for (Class p : method.getParameterTypes()) {
                    sig += ", " + javaName(p) + ".class";
                }
                sig += ")";

                final String inspectorFieldName = holder + (name.charAt(0) == '_' ? name : '_' + name);
                final String inspectorFieldType = "Tele" + (isStatic ? "Static" : "Virtual") + "MethodAccess";
                writer.println("public final " + inspectorFieldType + " " + inspectorFieldName + " = new " + inspectorFieldType + "(vm(), " +
                                holder.replace('$', '.') + ".class, \"" + name + "\", " + sig + ");");
            }
        };
        VmFieldAccess.updateSource(VmMethodAccess.class, Method.class, methodReifier, inInspector);
    }
}
