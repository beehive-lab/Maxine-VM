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
package com.oracle.max.vm.ext.graal.stubs;

import static com.sun.max.vm.jni.JniHandles.*;
import static com.sun.max.vm.type.ClassRegistry.*;

import java.util.*;

import com.oracle.max.graal.nodes.*;
import com.oracle.max.graal.nodes.calc.*;
import com.oracle.max.vm.ext.graal.nodes.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.jni.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.type.*;

/**
 * Builds the graph that will replace the place-holder for the
 * {@linkplain NativeStubGraphBuilder#nativeFunctionCall native function call}
 * in a native method stub.
 */
class NativeFunctionCallGraphBuilder extends AbstractGraphBuilder {

    private InvokeNode stackHandles;
    private int stackHandleOffset;

    public NativeFunctionCallGraphBuilder(ClassMethodActor nativeMethod) {
        super(nativeMethod);
        setGraph(new StructuredGraph());

        SignatureDescriptor sig = nativeMethod.descriptor();

        List<LocalNode> nativeFunctionCallArgs = createLocals(0, NativeStubGraphBuilder.nativeFunctionCall.descriptor(), true);
        ValueNode function = nativeFunctionCallArgs.get(0);
        ValueNode frame = nativeFunctionCallArgs.get(1);
        ValueNode jniEnv = nativeFunctionCallArgs.get(2);

        List<LocalNode> nativeMethodArgs = createLocals(nativeFunctionCallArgs.size(), sig, nativeMethod.isStatic());

        Iterator<LocalNode> nativeMethodArg = nativeMethodArgs.iterator();

        List<ValueNode> jniArgs = new ArrayList<ValueNode>(nativeMethodArgs.size() + 1);

        stackHandles = append(invoke(Intrinsics_stackAllocate, iconst(stackHandlesSize(sig))));

        // Load the JNI environment variable
        boolean isCFunction = nativeMethod.isCFunction();
        if (!isCFunction) {
            jniArgs.add(jniEnv);

            if (nativeMethod.isStatic()) {
                // Load the class for a static method
                jniArgs.add(handlize(oconst(nativeMethod.holder().toJava())));
            } else {
                // Load the receiver for a non-static method
                jniArgs.add(handlize(nativeMethodArg.next()));
            }
        }

        // Load the remaining parameters, handlizing object parameters
        while (nativeMethodArg.hasNext()) {
            LocalNode arg = nativeMethodArg.next();
            if (arg.kind().isObject()) {
                assert !isCFunction;
                jniArgs.add(handlize(arg));
                stackHandleOffset += Word.size();
            } else {
                jniArgs.add(arg);
            }
        }

        if (!isCFunction) {
            if (stackHandleOffset > 1) {

                // Write the address of the handles array to STACK_HANDLES_ADDRESS_OFFSET
                // to communicate to the GC where the initialized array is.
                append(invoke(Pointer_writeWord, frame, iconst(STACK_HANDLES_ADDRESS_OFFSET), stackHandles));
            }

            // Place a new Java frame anchor and transition into thread_in_native state
            append(invoke(Snippets_nativeCallPrologue, oconst(nativeMethod.nativeFunction)));
        }

        // Invoke the native function
        final Kind resultKind = sig.resultDescriptor().toKind();
        ValueNode[] jniArgsArray = jniArgs.toArray(new ValueNode[jniArgs.size()]);
        NativeCallNode nativeCall = append(graph.add(new NativeCallNode(function, jniArgsArray, WordUtil.ciKind(resultKind, false), nativeMethod)));
        FrameState stateAfter = stateAfterCall(nativeCall, resultKind);
        nativeCall.setStateAfter(stateAfter);

        // Sign extend or zero the upper bits of a return value smaller than an int to
        // preserve the invariant that all such values are represented by an int
        // in the VM. We cannot rely on the native C compiler doing this for us.
        // Alternatively, an object return value must be un-handlized
        // *before* the JNI frame is restored.
        ValueNode retValue = nativeCall;
        switch (resultKind.asEnum) {
            case BOOLEAN:
            case BYTE: {
                retValue = graph.unique(new ConvertNode(ConvertNode.Op.I2B, retValue));
                break;
            }
            case SHORT: {
                retValue = graph.unique(new ConvertNode(ConvertNode.Op.I2S, retValue));
                break;
            }
            case CHAR: {
                retValue = graph.unique(new ConvertNode(ConvertNode.Op.I2C, retValue));
                break;
            }
            case REFERENCE: {
                assert !isCFunction;
                retValue = append(invoke(JniHandle_unhand, retValue));
                break;
            }
            case VOID: {
                retValue = null;
                break;
            }
        }

        append(graph.add(new ReturnNode(retValue)));
    }

    private InvokeNode handlize(ValueNode object) {
        assert object.kind().isObject();
        InvokeNode handle = append(invoke(JniHandles_createStackHandle, stackHandles, iconst(stackHandleOffset), object));
        stackHandleOffset += Word.size();
        return handle;
    }

    static final MethodActor Pointer_writeWord = findMethod(Pointer.class, "writeWord", int.class, Word.class);
    static final MethodActor Snippets_nativeCallPrologue = findMethod(Snippets.class, "nativeCallPrologue", NativeFunction.class);
    static final MethodActor Intrinsics_stackAllocate = findMethod(Intrinsics.class, "stackAllocate", int.class);
    static final MethodActor JniHandle_unhand = findMethod(JniHandle.class, "unhand");
    static final MethodActor JniHandles_createStackHandle = findMethod(JniHandles.class, "createStackHandle", Pointer.class, int.class, Object.class);
}
