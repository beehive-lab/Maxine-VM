/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.max.vm.ext.graal;

import static com.oracle.max.graal.nodes.FrameState.*;
import static com.sun.max.vm.jni.JniHandles.*;
import static com.sun.max.vm.type.ClassRegistry.*;

import java.util.*;

import com.oracle.max.graal.compiler.*;
import com.oracle.max.graal.nodes.*;
import com.oracle.max.graal.nodes.calc.*;
import com.oracle.max.graal.nodes.java.*;
import com.oracle.max.graal.nodes.java.MethodCallTargetNode.InvokeKind;
import com.oracle.max.vm.ext.graal.nodes.*;
import com.oracle.max.vm.ext.maxri.*;
import com.sun.cri.ci.*;
import com.sun.cri.ri.*;
import com.sun.cri.ri.RiType.Representation;
import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.jni.*;
import com.sun.max.vm.jvmti.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.thread.*;
import com.sun.max.vm.type.*;

/**
 * A utility class for generating a native method stub (as a compiler graph) implementing the transition
 * from Java to native code. Most of these transitions are calls to native functions via JNI.
 * However, faster transitions to Maxine specific native code are also supported.
 */
public class NativeStubCompiler {

    public NativeStubCompiler(GraalCompiler graal, ClassMethodActor method) {
        this.method = method;
        this.graph = new StructuredGraph();
        this.graal = graal;
        this.runtime = MaxRuntime.getInstance();
        lastFixedNode = graph.start();
    }

    final ClassMethodActor method;
    final StructuredGraph graph;
    final GraalCompiler graal;
    final MaxRuntime runtime;

    private FixedWithNextNode lastFixedNode;
    private InvokeNode stackHandles;
    private int stackHandleOffset;
    private ValueNode synchronizedObject;

    /**
     * Compiles a native method stub.
     */
    public CiTargetMethod compile() {

        SignatureDescriptor sig = method.descriptor();
        boolean isCFunction = method.isCFunction();

        final TypeDescriptor resultDescriptor = sig.resultDescriptor();
        final Kind resultKind = resultDescriptor.toKind();

        List<LocalNode> inArgs = makeArgs(sig);
        LocalNode receiver = method.isStatic() ? null : inArgs.get(0);
        Iterator<LocalNode> inArg = inArgs.iterator();

        List<ValueNode> outArgs = new ArrayList<ValueNode>(inArgs.size() + 1);
        InvokeNode frame = null;

        InvokeNode currentThread = null;
        InvokeNode jniHandlesTop = null;

        ConstantNode zero = ConstantNode.forIntegerKind(WordUtil.archKind(), 0, graph);

        if (!isCFunction) {

            // Zero out the slot at STACK_HANDLES_ADDRESS_OFFSET
            // so that the GC doesn't scan the object handles array.
            // There must not be a safepoint in the stub before this point.
            frame = append(invoke(VMRegister_getCpuStackPointer));
            append(invoke(Pointer_writeWord, frame, iconst(STACK_HANDLES_ADDRESS_OFFSET), zero));

            if (method.isSynchronized()) {
                acquireMethodLock(receiver);
            }


            traceEntryOrExit(true);

            // Save current JNI frame.
            currentThread = append(invoke(VmThread_current));
            jniHandlesTop = append(invoke(VmThread_jniHandlesTop, currentThread));

            stackHandles = append(invoke(Intrinsics_stackAllocate, iconst(stackHandlesSize(sig))));
            stackHandleOffset = 0;

            // Load the JNI environment variable
            outArgs.add(append(invoke(VmThread_jniEnv)));
            if (method.isStatic()) {
                // Load the class for a static method
                outArgs.add(handlize(oconst(method.holder().toJava())));
            } else {
                // Load the receiver for a non-static method
                outArgs.add(handlize(inArg.next()));
            }
        } else {
            assert method.isStatic();
            assert !method.isSynchronized();
            stackHandles = null;
            stackHandleOffset = 0;
        }

        // Load the remaining parameters, handlizing object parameters
        while (inArg.hasNext()) {
            LocalNode arg = inArg.next();
            if (arg.kind().isObject()) {
                outArgs.add(handlize(arg));
                stackHandleOffset += Word.size();
            } else {
                outArgs.add(arg);
            }
        }

        if (stackHandleOffset > 1) {
            // Write the address of the handles array to STACK_HANDLES_ADDRESS_OFFSET
            // to communicate to the GC where the initialized array is.
            append(invoke(Pointer_writeWord, frame, iconst(STACK_HANDLES_ADDRESS_OFFSET), stackHandles));
        }

        // Link native function
        InvokeNode function = append(invoke(NativeFunction_link, oconst(method.nativeFunction)));

        if (method != JVMTIFunctions.currentJniEnv) {
            append(invoke(isCFunction ?  Snippets_nativeCallPrologue : Snippets_nativeCallPrologueForC, oconst(method.nativeFunction)));
        }

        // Invoke the native function
        ValueNode[] jniArgs = outArgs.toArray(new ValueNode[outArgs.size()]);
        NativeCallNode nativeCall = append(graph.add(new NativeCallNode(function, jniArgs, WordUtil.ciKind(resultKind, true), method)));
        FrameState stateAfter = stateAfterCall(nativeCall, resultKind);
        nativeCall.setStateAfter(stateAfter);

        // Sign extend or zero the upper bits of a return value smaller than an int to
        // preserve the invariant that all such values are represented by an int
        // in the VM. We cannot rely on the native C compiler doing this for us.
        ValueNode retValue = nativeCall;
        switch (sig.returnKind(false)) {
            case Boolean:
            case Byte: {
                retValue = graph.unique(new ConvertNode(ConvertNode.Op.I2B, retValue));
                break;
            }
            case Short: {
                retValue = graph.unique(new ConvertNode(ConvertNode.Op.I2S, retValue));
                break;
            }
            case Char: {
                retValue = graph.unique(new ConvertNode(ConvertNode.Op.I2C, retValue));
                break;
            }
        }

        if (method != JVMTIFunctions.currentJniEnv) {
            append(invoke(isCFunction ?  Snippets_nativeCallEpilogue : Snippets_nativeCallEpilogueForC));
        }

        if (!isCFunction) {

            if (stackHandleOffset > 1) {
                // The object handles array is no longer alive so zero out the slot at sp+OBJECT_HANDLES_BASE_OFFSET
                append(invoke(Pointer_writeWord, frame, iconst(STACK_HANDLES_ADDRESS_OFFSET), zero));
            }

            // Unwrap a reference result from its enclosing JNI handle. This must be done
            // *before* the JNI frame is restored.
            if (resultKind.isReference) {
                append(invoke(JniHandle_unhand, retValue));
            }

            // Restore JNI frame.
            append(invoke(VmThread_resetJniHandlesTop, currentThread, jniHandlesTop));

            traceEntryOrExit(false);

            if (method.isSynchronized()) {
                releaseMethodLock();
            }

            // throw (and clear) any pending exception
            append(invoke(VmThread_throwJniException, currentThread));
        }

        append(graph.add(new ReturnNode(retValue)));

        // Compile and print disassembly.
        graph.verify();
        CiResult result = graal.compileMethod(method, graph);
        System.out.println(runtime.disassemble(result.targetMethod()));

        return result.targetMethod();
    }

    private void acquireMethodLock(ValueNode receiver) {
        if (method.isStatic()) {
            synchronizedObject = oconst(method.holder().toJava());
        } else {
            synchronizedObject = receiver;
        }
        int lockNumber = 0;
        MonitorEnterNode monitorEnter = graph.add(new MonitorEnterNode(synchronizedObject, lockNumber, runtime.sizeOfBasicObjectLock() > 0));
        append(monitorEnter);
        FrameState stateAfter = new FrameState(method, 0, 0, 0, 1, false);
        monitorEnter.setStateAfter(stateAfter);
    }

    private void releaseMethodLock() {
        int lockNumber = 0;
        append(graph.add(new MonitorExitNode(synchronizedObject, lockNumber, runtime.sizeOfBasicObjectLock() > 0)));
    }

    private List<LocalNode> makeArgs(SignatureDescriptor sig) {
        List<LocalNode> args = new ArrayList<LocalNode>(sig.argumentCount(!method.isStatic()));
        int argIndex = 0;
        if (!method.isStatic()) {
            args.add(graph.unique(new LocalNode(CiKind.Object, 0)));
        }
        for (int i = 0; i < sig.numberOfParameters(); i++) {
            final TypeDescriptor parameterDescriptor = sig.parameterDescriptorAt(i);
            CiKind kind = WordUtil.ciKind(parameterDescriptor.toKind(), true);
            args.add(graph.unique(new LocalNode(kind, argIndex)));
            argIndex++;
        }
        return args;
    }

    @NEVER_INLINE
    private static void traceTransition(String name, boolean entry) {
        Log.print("[Thread \"");
        Log.print(VmThread.current().getName());
        if (entry) {
            Log.print(" --> JNI: ");
        } else {
            Log.print(" <-- JNI: ");
        }
        Log.print(name);
        Log.println(']');
    }

    /**
     * Creates a subgraph for tracing the entry or exit of a call to the native method.
     */
    private void traceEntryOrExit(boolean entry) {
        if (JniFunctions.TraceJNI) {
            if (MaxineVM.isHosted()) {
                // Stubs generated while bootstrapping need to test the "-XX:+TraceJNI" VM option
                ValueNode statics = oconst(JNIFunctions_TraceJNI.holder().getEncoding(Representation.StaticFields));
                assert statics != null;
                LoadFieldNode traceJNI = append(graph.add(new LoadFieldNode(statics, JNIFunctions_TraceJNI)));

                EndNode fromTrue = graph.add(new EndNode());
                EndNode fromFalse = graph.add(new EndNode());

                double probabilityOfTrue = 0.1d;
                IfNode ifNode = append(graph.add(new IfNode(graph.unique(new CompareNode(traceJNI, Condition.EQ, zconst(true))), probabilityOfTrue)));
                InvokeNode trace = append(invoke(traceTransition, oconst(method.format("%H.%n(%P)")), zconst(entry)));
                append(fromTrue);
                ifNode.setTrueSuccessor(BeginNode.begin(trace));
                ifNode.setFalseSuccessor(BeginNode.begin(fromFalse));

                MergeNode merge = append(graph.add(new MergeNode()));
                merge.addEnd(fromTrue);
                merge.addEnd(fromFalse);

            } else {
                append(invoke(traceTransition, oconst(method.format("%H.%n(%P)")), zconst(entry)));
            }
        }
    }

    private <T extends FixedNode> T append(T node) {
        if (lastFixedNode != null) {
            lastFixedNode.setNext(node);
        }
        if (node instanceof FixedWithNextNode) {
            lastFixedNode = (FixedWithNextNode) node;
        } else {
            lastFixedNode = null;
        }
        return node;
    }

    private InvokeNode handlize(ValueNode object) {
        assert object.kind().isObject();
        InvokeNode handle = append(invoke(JniHandles_createStackHandle, stackHandles, iconst(stackHandleOffset), object));
        stackHandleOffset += Word.size();
        return handle;
    }

    /**
     * Creates a node for the invocation of a method.
     *
     * @param target the method being called
     * @param args the arguments of the call
     */
    private InvokeNode invoke(MethodActor target, ValueNode... args) {
        InvokeKind invokeKind;
        if (target.isStatic()) {
            invokeKind = InvokeKind.Static;
        } else if (target.holder().isInterface()) {
            invokeKind = InvokeKind.Interface;
        } else {
            VirtualMethodActor vma = (VirtualMethodActor) target;
            if (vma.vTableIndex() >= 0) {
                invokeKind = InvokeKind.Virtual;
            } else {
                invokeKind = InvokeKind.Special;
            }
        }

        RiType returnType = target.descriptor().returnType(method.holder());
        MethodCallTargetNode targetNode = graph.add(new MethodCallTargetNode(invokeKind, target, args, returnType));
        InvokeNode invokeNode = graph.add(new InvokeNode(targetNode, FrameState.BEFORE_BCI));
        FrameState stateAfter = stateAfterCall(invokeNode, target.descriptor().resultKind());
        invokeNode.setStateAfter(stateAfter);
        return invokeNode;
    }

    private FrameState stateAfterCall(ValueNode callNode, Kind resultKind) {
        ValueNode[] locals = {};
        ValueNode[] stack;
        int stackSize;
        if (resultKind == Kind.VOID) {
            stack = new ValueNode[0];
            stackSize = 0;
        } else if (resultKind.isCategory1) {
            stack = new ValueNode[] {callNode};
            stackSize = 1;
        } else {
            stack = new ValueNode[] {callNode, null};
            stackSize = 2;
        }

        List<ValueNode> locks = Collections.emptyList();
        return graph.add(new FrameState(method, AFTER_BCI, locals, stack, stackSize, locks, false));
    }

    /**
     * Creates a constant node for an integer constant.
     */
    ConstantNode iconst(int value) {
        return ConstantNode.forInt(value, graph);
    }

    /**
     * Creates a constant node for a boolean constant.
     */
    ConstantNode zconst(boolean value) {
        return ConstantNode.forBoolean(value, graph);
    }

    /**
     * Creates a constant node for an object constant.
     */
    ConstantNode oconst(Object value) {
        return ConstantNode.forObject(value, runtime, graph);
    }

    static final MethodActor VMRegister_getCpuStackPointer = findMethod(VMRegister.class, "getCpuStackPointer");
    static final MethodActor Pointer_writeWord = findMethod(Pointer.class, "writeWord", int.class, Word.class);
    static final MethodActor VmThread_current = findMethod(VmThread.class, "current");
    static final MethodActor VmThread_jniHandlesTop = findMethod(VmThread.class, "jniHandlesTop");
    static final MethodActor VmThread_resetJniHandlesTop = findMethod(VmThread.class, "resetJniHandlesTop", int.class);
    static final MethodActor VmThread_jniEnv = findMethod(VmThread.class, "jniEnv");
    static final MethodActor VmThread_throwJniException = findMethod(VmThread.class, "throwJniException");
    static final MethodActor Intrinsics_stackAllocate = findMethod(Intrinsics.class, "stackAllocate", int.class);
    static final MethodActor NativeFunction_link = findMethod(NativeFunction.class, "link");
    static final MethodActor Snippets_nativeCallPrologue = findMethod(Snippets.class, "nativeCallPrologue", NativeFunction.class);
    static final MethodActor Snippets_nativeCallPrologueForC = findMethod(Snippets.class, "nativeCallPrologueForC", NativeFunction.class);
    static final MethodActor Snippets_nativeCallEpilogue = findMethod(Snippets.class, "nativeCallEpilogue");
    static final MethodActor Snippets_nativeCallEpilogueForC = findMethod(Snippets.class, "nativeCallEpilogueForC");
    static final MethodActor JniHandle_unhand = findMethod(JniHandle.class, "unhand");
    static final MethodActor Log_println = findMethod(Log.class, "println", String.class);
    static final MethodActor JniHandles_createStackHandle = findMethod(JniHandles.class, "createStackHandle", Pointer.class, int.class, Object.class);
    static final MethodActor traceTransition = findMethod(NativeStubCompiler.class, "traceTransition", String.class, boolean.class);
    static final FieldActor JNIFunctions_TraceJNI = findField(JniFunctions.class, "TraceJNI");
}
