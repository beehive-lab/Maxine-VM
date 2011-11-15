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
package com.oracle.max.graal.compiler.phases;

import java.lang.annotation.*;
import java.lang.reflect.*;

import com.oracle.max.graal.compiler.*;
import com.oracle.max.graal.compiler.util.*;
import com.oracle.max.graal.graph.*;
import com.oracle.max.graal.graph.Node.ConstantNodeParameter;
import com.oracle.max.graal.graph.Node.NodeIntrinsic;
import com.oracle.max.graal.nodes.*;
import com.oracle.max.graal.nodes.extended.*;
import com.oracle.max.graal.nodes.java.*;
import com.sun.cri.ci.*;
import com.sun.cri.ri.*;

public class SnippetIntrinsificationPhase extends Phase {

    private final RiRuntime runtime;

    public SnippetIntrinsificationPhase(RiRuntime runtime) {
        this.runtime = runtime;
    }

    @Override
    protected void run(StructuredGraph graph) {
        for (InvokeNode invoke : graph.getNodes(InvokeNode.class)) {
            tryIntrinsify(invoke);
        }
        for (InvokeWithExceptionNode invoke : graph.getNodes(InvokeWithExceptionNode.class)) {
            tryIntrinsify(invoke);
        }
    }

    private void tryIntrinsify(Invoke invoke) {
        if (invoke == null) {
            return;
        }

        RiResolvedMethod target = invoke.callTarget().targetMethod();
        if (Modifier.isStatic(target.accessFlags())) {
            Class< ? > c = target.holder().toJava();
            if ((c != null && !Modifier.isPrivate(target.accessFlags()) && target.holder().isSubtypeOf(runtime.getType(Node.class))) || GraalOptions.Extend) {
                try {
                    Class< ? >[] parameterTypes = toClassArray(target.signature(), target.holder());
                    Method m = c.getMethod(target.name(), parameterTypes);
                    if (m != null) {
                        NodeIntrinsic intrinsic = m.getAnnotation(Node.NodeIntrinsic.class);
                        if (intrinsic != null) {
                            if (intrinsic.value() != NodeIntrinsic.class) {
                                c = intrinsic.value();
                            }
                            int z = 0;
                            Object[] initArgs = new Object[parameterTypes.length];
                            ValueNode[] arguments = InliningUtil.simplifyParameters(invoke);
                            for (Annotation[] annotations : m.getParameterAnnotations()) {
                                Object currentValue = null;
                                for (Annotation a : annotations) {
                                    if (a instanceof ConstantNodeParameter) {
                                        Node n = arguments[z];
                                        assert n instanceof ConstantNode : "must be compile time constant; " + n + " z=" + z + " for " + target;
                                        ConstantNode constantNode = (ConstantNode) n;
                                        currentValue = constantNode.asConstant().asObject();
                                        if (currentValue instanceof Class<?>) {
                                            currentValue = runtime.getType((Class< ? >) currentValue);
                                            parameterTypes[z] = RiResolvedType.class;
                                        }
                                        break;
                                    }
                                }

                                if (currentValue == null) {
                                    currentValue = arguments[z];
                                    parameterTypes[z] = ValueNode.class;
                                    Type type = m.getGenericParameterTypes()[z];
                                    if (type instanceof TypeVariable) {
                                        TypeVariable typeVariable = (TypeVariable) type;
                                        if (typeVariable.getBounds().length == 1) {
                                            Type boundType = typeVariable.getBounds()[0];
                                            if (boundType instanceof Class && ((Class) boundType).getSuperclass() == null) {
                                                // Unbound generic => try boxing elimination
                                                ValueNode node = arguments[z];
                                                if (node.usages().size() == 2) {
                                                    if (node instanceof Invoke) {
                                                        Invoke invokeNode = (Invoke) node;
                                                        MethodCallTargetNode callTarget = invokeNode.callTarget();
                                                        if (BoxingEliminationPhase.isBoxingMethod(runtime, callTarget.targetMethod())) {
                                                            currentValue = callTarget.arguments().get(0);
                                                            if (invokeNode instanceof InvokeWithExceptionNode) {
                                                                // Destroy exception edge & clear stateAfter.
                                                                InvokeWithExceptionNode invokeWithExceptionNode = (InvokeWithExceptionNode) invokeNode;
                                                                invokeWithExceptionNode.killExceptionEdge();
                                                            }
                                                            invokeNode.node().replaceAndDelete(invokeNode.next());
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                initArgs[z] = currentValue;
                                z++;
                            }
                            Constructor<?> constructor = c.getConstructor(parameterTypes);
                            try {
                                ValueNode newInstance = (ValueNode) constructor.newInstance(initArgs);
                                invoke.node().graph().add(newInstance);

                                if (newInstance instanceof StateSplit) {
                                    StateSplit stateSplit = (StateSplit) newInstance;
                                    stateSplit.setStateAfter(invoke.stateAfter());
                                }

                                FixedNode next = invoke.next();

                                if (invoke instanceof InvokeWithExceptionNode) {
                                    // Destroy exception edge.
                                    ((InvokeWithExceptionNode) invoke).killExceptionEdge();
                                }
                                invoke.setNext(null);

                                if (newInstance instanceof FixedWithNextNode) {
                                    FixedWithNextNode fixedWithNextNode = (FixedWithNextNode) newInstance;
                                    fixedWithNextNode.setNext(next);
                                } else {
                                    invoke.node().replaceAtPredecessors(next);
                                }

                                // Replace invoke with new node.
                                invoke.node().replaceAndDelete(newInstance);

                                // Replace with boxing or un-boxing calls if return types to not match, boxing elimination can later take care of it
                                if (newInstance.kind() != CiKind.Object) {
                                    for (Node usage : newInstance.usages().snapshot()) {
                                        if (usage instanceof CheckCastNode) {
                                            CheckCastNode checkCastNode = (CheckCastNode) usage;
                                            for (Node checkCastUsage : checkCastNode.usages().snapshot()) {
                                                if (checkCastUsage instanceof ValueAnchorNode) {
                                                    ValueAnchorNode valueAnchorNode = (ValueAnchorNode) checkCastUsage;
                                                    valueAnchorNode.replaceAndDelete(valueAnchorNode.next());
                                                } else if (checkCastUsage instanceof MethodCallTargetNode) {
                                                    MethodCallTargetNode checkCastCallTarget = (MethodCallTargetNode) checkCastUsage;
                                                    assert BoxingEliminationPhase.isUnboxingMethod(runtime, checkCastCallTarget.targetMethod());
                                                    for (Invoke invokeNode : checkCastCallTarget.invokes()) {
                                                        if (invokeNode instanceof InvokeWithExceptionNode) {
                                                            // Destroy exception edge & clear stateAfter.
                                                            InvokeWithExceptionNode invokeWithExceptionNode = (InvokeWithExceptionNode) invokeNode;
                                                            invokeWithExceptionNode.killExceptionEdge();
                                                        }
                                                        invokeNode.node().replaceAtUsages(newInstance);
                                                        invokeNode.node().replaceAndDelete(invokeNode.next());
                                                    }
                                                    checkCastCallTarget.delete();
                                                } else if (checkCastUsage instanceof FrameState) {
                                                    checkCastUsage.replaceFirstInput(checkCastNode, null);
                                                } else {
                                                    assert false : "unexpected checkcast usage: " + checkCastUsage;
                                                }
                                            }
                                            checkCastNode.delete();
                                        }
                                    }
                                }

                            } catch (IllegalArgumentException e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                            } catch (InstantiationException e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                            } catch (IllegalAccessException e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                            } catch (InvocationTargetException e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                            }
                        }
                    }
                } catch (SecurityException e) {
                    throw new RuntimeException(e);
                } catch (NoSuchMethodException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    public static Class< ? >[] toClassArray(RiSignature signature, RiType accessingClass) {
        int count = signature.argumentCount(false);
        Class< ? >[] result = new Class< ? >[count];
        for (int i = 0; i < result.length; ++i) {
            result[i] = ((RiResolvedType) signature.argumentTypeAt(i, accessingClass)).toJava();
        }
        return result;
    }
}
