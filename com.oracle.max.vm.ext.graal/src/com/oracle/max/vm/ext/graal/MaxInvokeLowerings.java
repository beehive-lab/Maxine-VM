/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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

import java.util.*;

import com.oracle.graal.api.code.*;
import com.oracle.graal.api.meta.*;
import com.oracle.graal.graph.*;
import com.oracle.graal.nodes.*;
import com.oracle.graal.nodes.calc.*;
import com.oracle.graal.nodes.java.*;
import com.oracle.graal.nodes.java.MethodCallTargetNode.InvokeKind;
import com.oracle.graal.nodes.spi.*;
import com.oracle.graal.snippets.*;
import com.oracle.graal.snippets.Snippet.*;
import com.oracle.graal.snippets.SnippetTemplate.*;
import com.oracle.max.vm.ext.graal.nodes.*;
import com.oracle.max.vm.ext.graal.snippets.*;
import com.sun.max.annotate.*;
import com.sun.max.program.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.runtime.*;


class MaxInvokeLowerings extends SnippetLowerings implements SnippetsInterface {

    private MaxInvokeLowerings(MetaAccessProvider runtime,
                    Assumptions assumptions, TargetDescription target, Map<Class< ? extends Node>, LoweringProvider> lowerings) {
        super(runtime, assumptions, target);
        lowerings.put(InvokeNode.class, new InvokeLowering());
        lowerings.put(InvokeWithExceptionNode.class, new InvokeLowering());
        lowerings.put(UnresolvedMethodNode.class, new UnresolvedMethodLowering(this));
    }

    static void registerLowerings(VMConfiguration config, TargetDescription targetDescription, MetaAccessProvider runtime, Assumptions assumptions,
                    Map<Class< ? extends Node>, LoweringProvider> lowerings) {
        new MaxInvokeLowerings(runtime, assumptions, targetDescription, lowerings);
    }

    private static class InvokeLowering implements LoweringProvider<FixedNode> {

        @Override
        public void lower(FixedNode node, LoweringTool tool) {
            StructuredGraph graph = (StructuredGraph) node.graph();
            Invoke invoke = (Invoke) node;
            if (invoke.callTarget() instanceof MethodCallTargetNode) {
                MethodCallTargetNode callTarget = invoke.methodCallTarget();
                NodeInputList<ValueNode> parameters = callTarget.arguments();
                ValueNode receiver = parameters.size() <= 0 ? null : parameters.get(0);
                if (!callTarget.isStatic() && receiver.kind() == Kind.Object && !receiver.objectStamp().nonNull()) {
                    IsNullNode isNull = graph.unique(new IsNullNode(receiver));
                    graph.addBeforeFixed(node, graph.add(new FixedGuardNode(isNull, DeoptimizationReason.NullCheckException, null, true)));
                }
                JavaType[] signature = MetaUtil.signatureToTypes(callTarget.targetMethod().getSignature(), callTarget.isStatic() ? null : callTarget.targetMethod().getDeclaringClass());
                CallingConvention.Type callType = CallingConvention.Type.JavaCall;

                CallTargetNode loweredCallTarget = null;
                switch (callTarget.invokeKind()) {
                    case Static:
                    case Special:
                        loweredCallTarget = graph.add(new DirectCallTargetNode(parameters, callTarget.returnStamp(), signature, callTarget.targetMethod(), callType));
                        break;
                    case Virtual:
                        /*
                    case Interface:
                        ClassMethodActor method = (ClassMethodActor) callTarget.targetMethod();
                        if (method.getImplementations().length == 0) {
                            // We are calling an abstract method with no implementation, i.e., the closed-world analysis
                            // showed that there is no concrete receiver ever allocated. This must be dead code.
                            graph.addBeforeFixed(node, graph.add(new FixedGuardNode(ConstantNode.forBoolean(true, graph), DeoptimizationReason.UnreachedCode, null, true, invoke.leafGraphId())));
                            return;
                        }
                        int vtableEntryOffset = NumUtil.safeToInt(hubLayout.getArrayElementOffset(method.getVTableIndex()));

                        ReadNode hub = graph.add(new ReadNode(receiver, LocationNode.create(LocationNode.FINAL_LOCATION, Kind.Object, objectLayout.hubOffset(), graph), StampFactory.objectNonNull()));
                        LocationNode entryLoc = LocationNode.create(LocationNode.FINAL_LOCATION, target.wordKind, vtableEntryOffset, graph);
                        ReadNode entry = graph.add(new ReadNode(hub, entryLoc, StampFactory.forKind(target.wordKind)));
                        loweredCallTarget = graph.add(new IndirectCallTargetNode(entry, parameters, callTarget.returnStamp(), signature, callTarget.targetMethod(), callType));

                        graph.addBeforeFixed(node, hub);
                        graph.addAfterFixed(hub, entry);
                        break;
                        */
                    default:
                        ProgramError.unknownCase();
                }
                callTarget.replaceAndDelete(loweredCallTarget);
            }
        }
    }



    private class UnresolvedMethodLowering extends Lowering implements LoweringProvider<UnresolvedMethodNode> {
        protected final ResolvedJavaMethod[] snippets = new ResolvedJavaMethod[InvokeKind.values().length];

        UnresolvedMethodLowering(MaxInvokeLowerings invokeSnippets) {
            super();
            for (InvokeKind invokeKind : InvokeKind.values()) {
                snippets[invokeKind.ordinal()] = invokeSnippets.findSnippet(MaxInvokeLowerings.class,
                                "resolve" + invokeKind.name() + "MethodSnippet");
            }
        }

        @Override
        public void lower(UnresolvedMethodNode node, LoweringTool tool) {
            UnresolvedMethod unresolvedMethod = (UnresolvedMethod) MaxJavaMethod.getRiMethod(node.javaMethod());
            ResolutionGuard.InPool guard = unresolvedMethod.constantPool.makeResolutionGuard(unresolvedMethod.cpi);
            Key key = new Key(snippets[node.invokeKind().ordinal()]);
            Arguments args = new Arguments();
            args.add("guard", guard);
            instantiate(node, key, args);
        }

    }

    @Snippet(inlining = MaxSnippetInliningPolicy.class)
    private static VirtualMethodActor resolveSpecialMethodSnippet(@Parameter("guard") ResolutionGuard.InPool guard) {
        return resolveSpecialMethod(guard);
    }

    @RUNTIME_ENTRY(nonNull = true)
    private static VirtualMethodActor resolveSpecialMethod(ResolutionGuard.InPool guard) {
        return Snippets.resolveSpecialMethod(guard);
    }

    @Snippet(inlining = MaxSnippetInliningPolicy.class)
    private static VirtualMethodActor resolveVirtualMethodSnippet(@Parameter("guard") ResolutionGuard.InPool guard) {
        return resolveVirtualMethod(guard);
    }

    @RUNTIME_ENTRY(nonNull = true)
    private static VirtualMethodActor resolveVirtualMethod(ResolutionGuard.InPool guard) {
        return Snippets.resolveVirtualMethod(guard);
    }

    @Snippet(inlining = MaxSnippetInliningPolicy.class)
    private static InterfaceMethodActor resolveInterfaceMethodSnippet(@Parameter("guard") ResolutionGuard.InPool guard) {
        return resolveInterfaceMethod(guard);
    }

    @RUNTIME_ENTRY(nonNull = true)
    private static InterfaceMethodActor resolveInterfaceMethod(ResolutionGuard.InPool guard) {
        return resolveInterfaceMethod(guard);
    }

    @Snippet(inlining = MaxSnippetInliningPolicy.class)
    private static StaticMethodActor resolveStaticMethodSnippet(@Parameter("guard") ResolutionGuard.InPool guard) {
        return resolveStaticMethod(guard);
    }

    @RUNTIME_ENTRY(nonNull = true)
    private static StaticMethodActor resolveStaticMethod(ResolutionGuard.InPool guard) {
        return Snippets.resolveStaticMethod(guard);
    }


}
