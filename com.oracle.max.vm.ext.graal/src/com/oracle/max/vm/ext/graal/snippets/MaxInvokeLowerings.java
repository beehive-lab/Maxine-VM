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
package com.oracle.max.vm.ext.graal.snippets;

import java.util.*;

import com.oracle.graal.api.code.*;
import com.oracle.graal.api.meta.*;
import com.oracle.graal.graph.*;
import com.oracle.graal.nodes.*;
import com.oracle.graal.nodes.java.*;
import com.oracle.graal.nodes.java.MethodCallTargetNode.InvokeKind;
import com.oracle.graal.nodes.spi.*;
import com.oracle.graal.replacements.*;
import com.oracle.graal.replacements.SnippetTemplate.*;
import com.oracle.max.vm.ext.graal.*;
import com.oracle.max.vm.ext.graal.nodes.*;
import com.oracle.max.vm.ext.graal.phases.*;
import com.sun.max.annotate.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.runtime.Snippets;

/**
 * Maxine attempts to do as much lowering as possible with {@link Snippet snippets} to leverage the meta-circularity and
 * share code between compilers (write-once). Method invocation is one place where this cannot be done completely as
 * there is no way to express the actual invocation in a snippet that would not cause a cycle (amongst other issues).
 * However, the computation of the method code address is handled by a snippet, by inserting a {@link MethodAddressNode}
 * node before the {@code InvokeNode}, which is subsequently lowered using a snippet. The value returned by the snippet
 * is fed into the {@link IndirectCallTargetNode} that is the lowering for the incoming {@link MethodCallTargetNode}.
 * Therefore the code here would not need to changed if a different implementation was chosen for the virtual method
 * table.
 *
 * Unresolved methods are handled in a similar fashion, by inserting a node that is lowered by a snippet to do the
 * resolution. This is actually done in {@link MaxGraphBuilderPhase#unresolvedInvoke}.
 */
public class MaxInvokeLowerings extends SnippetLowerings {

    @HOSTED_ONLY
    public MaxInvokeLowerings(CodeCacheProvider runtime, Replacements replacements, TargetDescription target,
                    Map<Class< ? extends Node>, LoweringProvider> lowerings) {
        super(runtime, replacements, target);
    }

    @Override
    @HOSTED_ONLY
    public void registerLowerings(CodeCacheProvider runtime, Replacements replacements, TargetDescription targetDescription,
                    Map<Class< ? extends Node>, LoweringProvider> lowerings) {
        lowerings.put(InvokeNode.class, new InvokeLowering());
        lowerings.put(InvokeWithExceptionNode.class, new InvokeLowering());
        lowerings.put(ResolveMethodNode.class, new UnresolvedMethodLowering(this));
        lowerings.put(MethodAddressNode.class, new MethodAddressLowering(this));
    }

    private class InvokeLowering implements LoweringProvider<FixedNode> {

        @Override
        public void lower(FixedNode node, LoweringTool tool) {
            StructuredGraph graph = node.graph();
            Invoke invoke = (Invoke) node;
            if (invoke.callTarget() instanceof MethodCallTargetNode) {
                MethodCallTargetNode callTarget = (MethodCallTargetNode) invoke.callTarget();
                NodeInputList<ValueNode> parameters = callTarget.arguments();
                ValueNode receiver = parameters.size() <= 0 ? null : parameters.get(0);
                // For Virtual/Interface calls an explicit null check is not needed as loading the
                // address from the vtable/itable acts as an implicit null check.
                JavaType[] signature = MetaUtil.signatureToTypes(callTarget.targetJavaMethod().getSignature(),
                                callTarget.isStatic() ? null : callTarget.targetJavaMethod().getDeclaringClass());
                CallingConvention.Type callType = CallingConvention.Type.JavaCall;

                CallTargetNode loweredCallTarget = null;
                switch (callTarget.invokeKind()) {
                    case Static:
                    case Special:
                        if (callTarget instanceof UnresolvedMethodCallTargetNode) {
                            // Insert a MethodAddressNode that will compute the address
                            MethodAddressNode entry = new MethodAddressNode(callTarget.invokeKind(),
                                            ((UnresolvedMethodCallTargetNode) callTarget).resolvedMethodActor(), receiver);
                            graph.addBeforeFixed(node, graph.add(entry));

                            loweredCallTarget = graph.add(new IndirectCallTargetNode(entry, parameters, callTarget.returnStamp(),
                                            signature, callTarget.targetJavaMethod(), callType));
                        } else {
                            loweredCallTarget = graph.add(new DirectCallTargetNode(parameters, callTarget.returnStamp(),
                                            signature, callTarget.targetMethod(), callType));
                        }
                        break;
                    case Virtual:
                    case Interface:
                        ValueNode methodActor;
                        if (callTarget instanceof UnresolvedMethodCallTargetNode) {
                            methodActor = ((UnresolvedMethodCallTargetNode) callTarget).resolvedMethodActor();
                        } else {
                            methodActor = ConstantNode.forObject(MaxResolvedJavaMethod.getRiResolvedMethod(callTarget.targetMethod()), runtime, graph);
                        }
                        // Insert a MethodAddressNode that will compute the address from the vtable/itable
                        MethodAddressNode entry = new MethodAddressNode(callTarget.invokeKind(), methodActor, receiver);
                        graph.addBeforeFixed(node, graph.add(entry));
                        loweredCallTarget = graph.add(new IndirectCallTargetNode(entry, parameters, callTarget.returnStamp(),
                                        signature, callTarget.targetJavaMethod(), callType));
                        break;
                }
                callTarget.replaceAndDelete(loweredCallTarget);
            }
        }
    }

    /**
     * Lowers a {@link MethodAddressNode} using a snippet that returns the entry point of the method.
     */
    private class MethodAddressLowering extends Lowering implements LoweringProvider<MethodAddressNode> {
        protected final SnippetInfo[] snippets = new SnippetInfo[InvokeKind.values().length];

        MethodAddressLowering(MaxInvokeLowerings invokeSnippets) {
            super();
            for (InvokeKind invokeKind : InvokeKind.values()) {
                snippets[invokeKind.ordinal()] = invokeSnippets.snippet(MaxInvokeLowerings.class,
                                "addressFor" + invokeKind.name() + "MethodSnippet");
            }
        }

        @Override
        public void lower(MethodAddressNode node, LoweringTool tool) {
            Arguments args = new Arguments(snippets[node.invokeKind().ordinal()]);
            if (node.invokeKind() == InvokeKind.Interface || node.invokeKind() == InvokeKind.Virtual) {
                args.add("receiver", node.receiver());
            }
            args.add("methodActor", node.methodActor());
            instantiate(node, args);
        }

    }

    @Snippet(inlining = MaxSnippetInliningPolicy.class)
    private static com.sun.max.unsafe.Address addressForSpecialMethodSnippet(VirtualMethodActor methodActor) {
        return getAddressForSpecialMethodSnippet(methodActor);
    }

    @SNIPPET_SLOWPATH
    private static com.sun.max.unsafe.Address getAddressForSpecialMethodSnippet(VirtualMethodActor methodActor) {
        return Snippets.makeEntrypoint(methodActor, com.sun.max.vm.compiler.CallEntryPoint.OPTIMIZED_ENTRY_POINT);
    }

    @Snippet(inlining = MaxSnippetInliningPolicy.class)
    private static com.sun.max.unsafe.Address addressForStaticMethodSnippet(StaticMethodActor methodActor) {
        return getAddressForStaticMethodSnippet(methodActor);
    }

    @SNIPPET_SLOWPATH
    private static com.sun.max.unsafe.Address getAddressForStaticMethodSnippet(StaticMethodActor methodActor) {
        Snippets.makeHolderInitialized(methodActor);
        return Snippets.makeEntrypoint(methodActor, com.sun.max.vm.compiler.CallEntryPoint.OPTIMIZED_ENTRY_POINT);
    }

    @Snippet(inlining = MaxSnippetInliningPolicy.class)
    private static com.sun.max.unsafe.Address addressForVirtualMethodSnippet(Object receiver, VirtualMethodActor methodActor) {
        return Snippets.selectNonPrivateVirtualMethod(receiver, methodActor).asAddress();
    }

    @Snippet(inlining = MaxSnippetInliningPolicy.class)
    private static com.sun.max.unsafe.Address addressForInterfaceMethodSnippet(Object receiver, InterfaceMethodActor methodActor) {
        return Snippets.selectInterfaceMethod(receiver, methodActor).asAddress();
    }

    private class UnresolvedMethodLowering extends Lowering implements LoweringProvider<ResolveMethodNode> {
        protected final SnippetInfo[] snippets = new SnippetInfo[InvokeKind.values().length];

        UnresolvedMethodLowering(MaxInvokeLowerings invokeSnippets) {
            super();
            for (InvokeKind invokeKind : InvokeKind.values()) {
                snippets[invokeKind.ordinal()] = invokeSnippets.snippet(MaxInvokeLowerings.class,
                                "resolve" + invokeKind.name() + "MethodSnippet");
            }
        }

        @Override
        public void lower(ResolveMethodNode node, LoweringTool tool) {
            UnresolvedMethod unresolvedMethod = (UnresolvedMethod) MaxJavaMethod.getRiMethod(node.javaMethod());
            ResolutionGuard.InPool guard = unresolvedMethod.constantPool.makeResolutionGuard(unresolvedMethod.cpi);
            Arguments args = new Arguments(snippets[node.invokeKind().ordinal()]);
            args.add("guard", guard);
            instantiate(node, args);
        }

    }

    @Snippet(inlining = MaxSnippetInliningPolicy.class)
    private static VirtualMethodActor resolveSpecialMethodSnippet(ResolutionGuard.InPool guard) {
        return resolveSpecialMethod(guard);
    }

    @SNIPPET_SLOWPATH(nonNull = true)
    private static VirtualMethodActor resolveSpecialMethod(ResolutionGuard.InPool guard) {
        return Snippets.resolveSpecialMethod(guard);
    }

    @Snippet(inlining = MaxSnippetInliningPolicy.class)
    private static VirtualMethodActor resolveVirtualMethodSnippet(ResolutionGuard.InPool guard) {
        return resolveVirtualMethod(guard);
    }

    @SNIPPET_SLOWPATH(nonNull = true)
    private static VirtualMethodActor resolveVirtualMethod(ResolutionGuard.InPool guard) {
        return Snippets.resolveVirtualMethod(guard);
    }

    @Snippet(inlining = MaxSnippetInliningPolicy.class)
    private static InterfaceMethodActor resolveInterfaceMethodSnippet(ResolutionGuard.InPool guard) {
        return resolveInterfaceMethod(guard);
    }

    @SNIPPET_SLOWPATH(nonNull = true)
    private static InterfaceMethodActor resolveInterfaceMethod(ResolutionGuard.InPool guard) {
        return Snippets.resolveInterfaceMethod(guard);
    }

    @Snippet(inlining = MaxSnippetInliningPolicy.class)
    private static StaticMethodActor resolveStaticMethodSnippet(ResolutionGuard.InPool guard) {
        return resolveStaticMethod(guard);
    }

    @SNIPPET_SLOWPATH(nonNull = true)
    private static StaticMethodActor resolveStaticMethod(ResolutionGuard.InPool guard) {
        return Snippets.resolveStaticMethod(guard);
    }

}
