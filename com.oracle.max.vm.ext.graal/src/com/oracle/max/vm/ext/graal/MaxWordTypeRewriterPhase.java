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

import com.oracle.graal.api.meta.*;
import com.oracle.graal.debug.*;
import com.oracle.graal.graph.*;
import com.oracle.graal.nodes.*;
import com.oracle.graal.nodes.calc.*;
import com.oracle.graal.nodes.extended.*;
import com.oracle.graal.nodes.java.*;
import com.oracle.graal.nodes.java.MethodCallTargetNode.InvokeKind;
import com.oracle.graal.nodes.type.*;
import com.oracle.graal.nodes.util.*;
import com.oracle.graal.phases.*;
import com.oracle.graal.phases.util.*;
import com.oracle.max.vm.ext.graal.nodes.*;
import com.sun.cri.ri.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.reference.*;


public abstract class MaxWordTypeRewriterPhase extends Phase {

    private static ResolvedJavaType wordBaseType;
    private static ResolvedJavaType codePointerType;
    private static ResolvedJavaType referenceType;
    private static ResolvedJavaType hubType;
    private static ResolvedJavaType wrappedWordType;

    private final Kind wordKind;
    private MetaAccessProvider metaAccess;

    public MaxWordTypeRewriterPhase(MetaAccessProvider metaAccess, Kind wordKind) {
        this.wordKind = wordKind;
        this.metaAccess = metaAccess;
        if (wordBaseType == null) {
            wordBaseType = metaAccess.lookupJavaType(Word.class);
            referenceType = metaAccess.lookupJavaType(Reference.class);
            hubType = metaAccess.lookupJavaType(Hub.class);
            codePointerType = metaAccess.lookupJavaType(CodePointer.class);
            wrappedWordType = metaAccess.lookupJavaType(WordUtil.WrappedWord.class);
        }
    }


    /**
     * Removes the actual type of {@link Word} subclass instances and converts them to {@link #wordkind}.
     * This can only be done when the object'ness, e.g., method invocation, of the instance is no longer needed.
     */
    public static class KindRewriter extends MaxWordTypeRewriterPhase {
        public KindRewriter(MetaAccessProvider metaAccess, Kind wordKind) {
            super(metaAccess, wordKind);
        }

        @Override
        protected void run(StructuredGraph graph) {
            for (Node n : GraphOrder.forwardGraph(graph)) {
                if (n instanceof ValueNode) {
                    ValueNode vn = (ValueNode) n;
                    if (isWord(vn)) {
                        changeToWord(vn);
                    }
                }
            }
        }

    }

    /**
     * Remove unnecessary checks for {@code null} on {@link Word} subclasses
     * Also remove null checks on access to {@link Hub} and subclasses.
     * It is only called during snippet generation and boot image compilation.
     * These generally arise from inlining a method, where the JVM spec requires
     * an explicit null check to happen.
     */
    public static class MaxNullCheckRewriter extends MaxWordTypeRewriterPhase {

        public MaxNullCheckRewriter(MetaAccessProvider metaAccess, Kind wordKind) {
            super(metaAccess, wordKind);
        }

        @Override
        protected void run(StructuredGraph graph) {
            for (Node n : GraphOrder.forwardGraph(graph)) {
                if (n instanceof ValueNode) {
                    if (isFixedGuardNullCheck(n)) {
                        removeNullCheck(graph, (FixedGuardNode) n);
                    }
                }
            }
        }

        private boolean isFixedGuardNullCheck(Node n) {
            if (n instanceof FixedGuardNode) {
                FixedGuardNode fn = (FixedGuardNode) n;
                return fn.condition() instanceof IsNullNode;
            }
            return false;
        }

        private void removeNullCheck(StructuredGraph graph, FixedGuardNode fixedGuardNode) {
            IsNullNode isNullNode = (IsNullNode) fixedGuardNode.condition();
            ValueNode object = isNullNode.object();
            if (canRemove(object)) {
                if (fixedGuardNode.usages().isNotEmpty()) {
                    checkForPiNode(fixedGuardNode);
                }
                graph.removeFixed(fixedGuardNode);
                // If the IsNullNode is only used by this FixedGuard node we can also delete that.
                // N.B. The previous call will have removed the FixedGuard node from the IsNullNode usages
                if (isNullNode.usages().isEmpty()) {
                    GraphUtil.killWithUnusedFloatingInputs(isNullNode);
                }
            } else {
                ResolvedJavaType objectType = object.objectStamp().type();
                Debug.log("%s: nullCheck not removed on %s", objectType == null ? object : objectType.getName(), graph.method());
            }
        }

        /**
         * Check if it is safe to remove the null check.
         * @param object the object that is the subject of the check
         * @return
         */
        private boolean canRemove(ValueNode object) {
            ResolvedJavaType objectType = object.objectStamp().type();
            // Check for a Word object, a Reference object, or a Hub or a nonNull object (can arise from runtime call rewrites)
            if (object.objectStamp().nonNull() || (objectType != null && (isWordOrReference(objectType) || hubType.isAssignableFrom(objectType)))) {
                return true;
            } else if (object instanceof LoadFieldNode) {
                // field loads from Hub objects are guaranteed non-null
                ResolvedJavaType fieldObjectType = ((LoadFieldNode) object).object().objectStamp().type();
                return hubType.isAssignableFrom(fieldObjectType);
            } else {
                return false;
            }

        }

        public static void checkForPiNode(FixedGuardNode fixedGuardNode) {
            // Likely a PiNode inserted during inlining
            Node usage = getSingleUsage(fixedGuardNode);
            if (usage.getClass() ==  PiNode.class) {
                PiNode pi = (PiNode) usage;
                fixedGuardNode.graph().replaceFloating(pi, pi.object());
            } else {
                assert false;
            }

        }

        private static Node getSingleUsage(Node node) {
            List<Node> usages = node.usages().snapshot();
            assert usages.size() == 1;
            return usages.get(0);
        }

    }

    /**
     * Finds {@link AccessNode} instances that have a chain of {@link MaxUnsafeCastNode}s starting with a
     * {@link Word} type that lead to a node whose type is {@link java.lang.Object}. Such nodes arise from the
     * explicit lowering in Maxine's reference scheme, object access logic. Not only are they redundant but
     * after the {@link Word} type nodes are rewritten as {@code long}, the objectness is lost, which can
     * cause problems with GC refmaps in the LIR stage.
     */
    public static class MaxUnsafeAccessRewriter extends MaxWordTypeRewriterPhase {
        public MaxUnsafeAccessRewriter(MetaAccessProvider metaAccess, Kind wordKind) {
            super(metaAccess, wordKind);
        }

        @Override
        protected void run(StructuredGraph graph) {
            for (Node n : GraphOrder.forwardGraph(graph)) {
                if (n instanceof AccessNode) {
                    ValueNode object = ((AccessNode) n).object();
                    if (object instanceof MaxUnsafeCastNode && object.kind() == Kind.Object) {
                        MaxUnsafeCastNode mn = (MaxUnsafeCastNode) object;
                        ObjectStamp stamp = mn.objectStamp();
                        if (wordBaseType.isAssignableFrom(stamp.type())) {
                            ValueNode endChain = findEndChainOfMaxUnsafeCastNode(mn);
                            if (endChain.kind() == Kind.Object && endChain.objectStamp().type() == MaxResolvedJavaType.getJavaLangObject()) {
                                deleteChain(graph, mn, endChain);
                            }
                        }
                    }

                }
            }
        }

        private ValueNode findEndChainOfMaxUnsafeCastNode(MaxUnsafeCastNode n) {
            MaxUnsafeCastNode nn = n;
            while (nn.object() instanceof MaxUnsafeCastNode) {
                nn = (MaxUnsafeCastNode) nn.object();
            }
            return nn.object();
        }

        private void deleteChain(StructuredGraph graph, MaxUnsafeCastNode n, ValueNode end) {
            if (n.object() != end) {
                deleteChain(graph, (MaxUnsafeCastNode) n.object(), end);
            }
            graph.replaceFloating(n, n.object());
        }

    }

    /**
     * Remove unnecessary {@link MaxUnsafeCastNode} instances.
     */
    public static class MaxUnsafeCastRewriter extends MaxWordTypeRewriterPhase {

        public MaxUnsafeCastRewriter(MetaAccessProvider metaAccess, Kind wordKind) {
            super(metaAccess, wordKind);
        }

        @Override
        protected void run(StructuredGraph graph) {
            for (MaxUnsafeCastNode unsafeCastNode : graph.getNodes().filter(MaxUnsafeCastNode.class).snapshot()) {
                boolean delete = false;
                ValueNode object = unsafeCastNode.object();
                if (!unsafeCastNode.isDeleted()) {
                    Kind kind = unsafeCastNode.kind();
                    if (object.stamp() == unsafeCastNode.stamp()) {
                        delete = true;
                    } else if (kind == Kind.Object && object.kind() == Kind.Object) {
                        ObjectStamp my = unsafeCastNode.objectStamp();
                        ObjectStamp other = object.objectStamp();

                        if (my.type().isAssignableFrom(other.type())) {
                            delete = true;
                        } else if (wordBaseType.isAssignableFrom(my.type()) && wordBaseType.isAssignableFrom(other.type())) {
                            // Word subclasses are freely assignable to one another in Maxine
                            delete = true;
                        }
                    } else if (kind == Kind.Long && object.kind() == Kind.Object && wordBaseType.isAssignableFrom(object.objectStamp().type())) {
                        delete = true;
                    } else if (kind == Kind.Object && object.kind() == Kind.Long && wordBaseType.isAssignableFrom(unsafeCastNode.objectStamp().type())) {
                        delete = true;
                    }
                }
                if (delete) {
                    Debug.log("Replacing %s with %s", unsafeCastNode.stamp(), object.stamp());
                    graph.replaceFloating(unsafeCastNode, object);
                }
            }

        }

    }

    /**
     * Ensures that all non-final methods on {@link Word} subclasses are treated as final (aka {@link InvokeKind.Special}),
     * to ensure that they are inlined by {@link SnippetInstaller}.
     */
    static class MakeWordFinalRewriter extends MaxWordTypeRewriterPhase {

        public MakeWordFinalRewriter(MetaAccessProvider metaAccess, Kind wordKind) {
            super(metaAccess, wordKind);
        }

        @Override
        protected void run(StructuredGraph graph) {
            // make sure all Word method invokes are Special else SnippetInstaller won't inline them
            for (MethodCallTargetNode callTargetNode : graph.getNodes(MethodCallTargetNode.class)) {
                if (callTargetNode.invokeKind() == InvokeKind.Virtual && callTargetNode.arguments().get(0) != null && isWord(callTargetNode.arguments().get(0))) {
                    callTargetNode.setInvokeKind(InvokeKind.Special);
                }
            }
        }
    }

    public boolean isWord(ValueNode node) {
        if (node.stamp() == StampFactory.forWord()) {
            return true;
        }
        if (node.kind() == Kind.Object && node.objectStamp().type() != null) {
            return isWord(node.objectStamp().type());
        }
        return false;
    }

    public static boolean isWord(ResolvedJavaType type) {
        if (type == wrappedWordType) {
            return true;
        }
        RiType riType = ((MaxResolvedJavaType) type).riType;
        if (!(riType instanceof ClassActor)) {
            return false;
        }
        ClassActor actor = (ClassActor) riType;
        return actor.kind == com.sun.max.vm.type.Kind.WORD;
    }

    public static Kind checkWord(JavaType javaType) {
        if (javaType instanceof ResolvedJavaType && isWord((ResolvedJavaType) javaType)) {
            return Kind.Long;
        } else {
            return javaType.getKind();
        }
    }

    public static boolean isWordOrReference(ResolvedJavaType objectType) {
        return objectType == referenceType || objectType == codePointerType || wordBaseType.isAssignableFrom(objectType);
    }

    protected void changeToWord(ValueNode valueNode) {
        if (valueNode.isConstant() && valueNode.asConstant().getKind() == Kind.Object) {
            StructuredGraph graph = valueNode.graph();
            ConstantNode constantNode = (ConstantNode) valueNode;
            WordUtil.WrappedWord wrappedWord = (WordUtil.WrappedWord) constantNode.value.asObject();
            graph.replaceFloating(constantNode, ConstantNode.forConstant(ConstantMap.toGraal(wrappedWord.archConstant()), metaAccess, graph));
        } else {
            assert !(valueNode instanceof ConstantNode) : "boxed Word constants should not appear in a snippet graph: " + valueNode + ", stamp: " + valueNode.stamp();
            valueNode.setStamp(StampFactory.forKind(wordKind));
        }
    }

}
