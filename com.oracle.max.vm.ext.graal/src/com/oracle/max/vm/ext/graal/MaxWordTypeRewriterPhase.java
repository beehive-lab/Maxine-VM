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

import com.oracle.graal.api.meta.*;
import com.oracle.graal.debug.*;
import com.oracle.graal.graph.*;
import com.oracle.graal.nodes.*;
import com.oracle.graal.nodes.calc.*;
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
import com.sun.max.vm.reference.*;


public abstract class MaxWordTypeRewriterPhase extends Phase {

    private final Kind wordKind;
    private static ResolvedJavaType wordBaseType;
    private static ResolvedJavaType referenceType;
    private static ResolvedJavaType hubType;

    public MaxWordTypeRewriterPhase(MetaAccessProvider metaAccess, Kind wordKind) {
        this.wordKind = wordKind;
        if (wordBaseType == null) {
            wordBaseType = metaAccess.lookupJavaType(Word.class);
            referenceType = metaAccess.lookupJavaType(Reference.class);
            hubType = metaAccess.lookupJavaType(Hub.class);
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
     * Remove unnecessary checks for {@code null} on {@link Word} subclasses.
     * Also remove null checks on access to {@link Hub} and subclasses.
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
                    } else if (isUncheckedAccess(n)) {
                        removeUnCheckedAccess(graph, (IfNode) n);
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
            // Check for a Word object, a Reference object, or a Hub or a nonNull object (can arise from runtime call rewrites)
            ResolvedJavaType objectType = object.objectStamp().type();
            if (object.objectStamp().nonNull() || (objectType != null && (isWordOrReference(objectType) || hubType.isAssignableFrom(objectType)))) {
                graph.removeFixed(fixedGuardNode);
                GraphUtil.killWithUnusedFloatingInputs(isNullNode);
            }
        }

        private boolean isUncheckedAccess(Node n) {
            if (n instanceof IfNode) {
                IfNode ifNode = (IfNode) n;
                if (ifNode.condition() instanceof IsNullNode) {
                    IsNullNode isNullNode = (IsNullNode) ifNode.condition();
                    ResolvedJavaType objectType = isNullNode.object().objectStamp().type();
                    return hubType.isAssignableFrom(objectType);
                } else {
                    return false;
                }
            } else {
                return false;
            }
        }

        private void removeUnCheckedAccess(StructuredGraph graph, IfNode ifNode) {
            IsNullNode isNullNode = (IsNullNode) ifNode.condition();
            graph.removeSplitPropagate(ifNode, ifNode.falseSuccessor());
            GraphUtil.killWithUnusedFloatingInputs(isNullNode);
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
                    logReplace(unsafeCastNode, object);
                    graph.replaceFloating(unsafeCastNode, object);
                }
            }

        }

        private static final String MAX_UNSAFE_CAST_REPLACE = "MaxUnsafeCastReplace";

        private static void logReplace(final MaxUnsafeCastNode unsafeCastNode, final ValueNode castee) {
            Debug.scope(MAX_UNSAFE_CAST_REPLACE, new Runnable() {

                public void run() {
                    Debug.log("MaxUnsafeCast: replacing %s with %s", unsafeCastNode.stamp(), castee.stamp());
                }
            });

        }
    }

    /**
     * Ensures that all non-final methods on {@link Word} subclasses are treated as final (aka {@link InvokeKind.Special}),
     * to ensure that they are inlined by {@link SnippetInstaller}.
     */
    static class MaxInvokeRewriter extends MaxWordTypeRewriterPhase {

        public MaxInvokeRewriter(MetaAccessProvider metaAccess, Kind wordKind) {
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
        node.inferStamp();
        if (node.stamp() == StampFactory.forWord()) {
            return true;
        }
        if (node.kind() == Kind.Object && node.objectStamp().type() != null) {
            return isWord(node.objectStamp().type());
        }
        return false;
    }

    public static boolean isWord(ResolvedJavaType type) {
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
        return wordBaseType.isAssignableFrom(objectType) || objectType == referenceType;
    }

    protected void changeToWord(ValueNode valueNode) {
        if (valueNode.isConstant() && valueNode.asConstant().getKind() == Kind.Object) {
            assert false;
//            ((StructuredGraph) valueNode.graph()).replaceFloating((ConstantNode) valueNode, newConstant);
        } else {
            assert !(valueNode instanceof ConstantNode) : "boxed Word constants should not appear in a snippet graph: " + valueNode + ", stamp: " + valueNode.stamp();
            valueNode.setStamp(StampFactory.forKind(wordKind));
        }
    }

}
