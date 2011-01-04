/*
 * Copyright (c) 2007, 2010, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.cps.cir.transform;

import java.util.*;

import com.sun.max.vm.cps.cir.*;
import com.sun.max.vm.cps.cir.variable.*;

/**
 * Deep CIR copying, preserving node sharing.
 *
 * @author Bernd Mathiske
 */
public final class CirReplication {

    private enum BlockReplicationPolicy {
        NONE, ALL;
    }

    private final BlockReplicationPolicy blockReplicationPolicy;

    protected CirReplication(BlockReplicationPolicy blockReplicationPolicy) {
        this.blockReplicationPolicy = blockReplicationPolicy;
    }

    private CirNode result;

    private class Replication {
        private CirNode original;

        protected Replication(CirNode original) {
            assert original != null;
            this.original = original;
        }

        protected void assign(CirNode replica) {
            assert replica != null;
            result = replica;
        }
    }

    private final class ArrayValueReplication extends Replication {
        private final CirValue[] array;
        private final int index;

        private ArrayValueReplication(CirValue[] array, int index, CirValue originalArgument) {
            super(originalArgument);
            this.array = array;
            this.index = index;
        }

        @Override
        protected void assign(CirNode argumentReplica) {
            assert argumentReplica != null;
            array[index] = (CirValue) argumentReplica;
        }
    }

    private CirValue[] replicateValues(CirValue[] originalValues, LinkedList<Replication> toDo) {
        if (originalValues.length == 0) {
            return CirCall.NO_ARGUMENTS;
        }
        final CirValue[] valuesReplica = new CirValue[originalValues.length];
        for (int i = 0; i < originalValues.length; i++) {
            if (originalValues[i] != null) {
                toDo.add(new ArrayValueReplication(valuesReplica, i, originalValues[i]));
                valuesReplica[i] = originalValues[i];
            } else {
                assert false;
            }
        }
        return valuesReplica;
    }

    private final class CallProcedureReplication extends Replication {
        private final CirCall call;

        private CallProcedureReplication(CirCall call, CirValue originalProcedure) {
            super(originalProcedure);
            this.call = call;
        }

        @Override
        protected void assign(CirNode procedureReplica) {
            call.setProcedure((CirValue) procedureReplica);
        }
    }

    private final class ClosureBodyReplication extends Replication {
        private final CirClosure closure;

        private ClosureBodyReplication(CirClosure closure, CirCall originalBody) {
            super(originalBody);
            this.closure = closure;
        }

        @Override
        protected void assign(CirNode bodyReplica) {
            closure.setBody((CirCall) bodyReplica);
        }
    }

    private final class BlockClosureReplication extends Replication {
        private final CirBlock block;

        private BlockClosureReplication(CirBlock block, CirClosure originalClosure) {
            super(originalClosure);
            this.block = block;
        }

        @Override
        protected void assign(CirNode closureReplica) {
            this.block.setClosure((CirClosure) closureReplica);
        }
    }

    private final Map<CirVariable, CirVariable> variableMap = new IdentityHashMap<CirVariable, CirVariable>();
    private final CirVariableFactory variableFactory = new CirVariableFactory();

    private CirVariable replicateVariable(CirVariable original) {
        CirVariable replica = variableMap.get(original);
        if (replica == null) {
            replica = variableFactory.createFresh(original);
            variableMap.put(original, replica);
        }
        return replica;
    }

    private CirJavaFrameDescriptor replicateJavaFrameDescriptor(CirJavaFrameDescriptor javaFrameDescriptor, LinkedList<Replication> toDo) {
        if (javaFrameDescriptor == null) {
            return null;
        }
        final CirJavaFrameDescriptor parentReplica = replicateJavaFrameDescriptor(javaFrameDescriptor.parent(), toDo);
        final CirJavaFrameDescriptor replica = new CirJavaFrameDescriptor(parentReplica, javaFrameDescriptor.classMethodActor, javaFrameDescriptor.bytecodePosition,
                                                                                 replicateValues(javaFrameDescriptor.locals, toDo),
                                                                                 replicateValues(javaFrameDescriptor.stackSlots, toDo));
        return replica;
    }

    private final Map<CirBlock, CirBlock> blockMap = new IdentityHashMap<CirBlock, CirBlock>();

    private void fixme(CirNode node) {
        Replication replication = new Replication(node);
        final CirClosure originalClosure = (CirClosure) replication.original;
        final CirClosure closureReplica = (CirClosure) replication.original.clone();
        final CirVariable[] originalParameters = originalClosure.parameters();
        final int numberOfParameters = originalParameters.length;
        final CirVariable[] parameterReplicas = CirClosure.newParameters(numberOfParameters);
        closureReplica.setParameters(parameterReplicas);
    }

    private CirNode run(CirNode node) {
        final LinkedList<Replication> toDo = new LinkedList<Replication>();
        Replication replication = new Replication(node);
        while (true) {
            if (replication.original instanceof CirCall) {
                final CirCall originalCall = (CirCall) replication.original;
                final CirCall callReplica = (CirCall) originalCall.clone();
                replication.assign(callReplica);
                callReplica.setArguments(replicateValues(originalCall.arguments(), toDo));
                toDo.add(new CallProcedureReplication(callReplica, originalCall.procedure()));
                if (originalCall.javaFrameDescriptor() != null) {
                    callReplica.setJavaFrameDescriptor(replicateJavaFrameDescriptor(originalCall.javaFrameDescriptor(), toDo));
                }
            } else {
                assert replication.original instanceof CirValue;
                if (replication.original instanceof CirVariable) {
                    replication.assign(replicateVariable((CirVariable) replication.original));
                } else if (replication.original instanceof CirClosure) {
                    final CirClosure originalClosure = (CirClosure) replication.original;
                    final CirClosure closureReplica = (CirClosure) originalClosure.clone();
                    final CirVariable[] originalParameters = originalClosure.parameters();
                    final int numberOfParameters = originalParameters.length;
                    final CirVariable[] parameterReplicas = CirClosure.newParameters(numberOfParameters);
                    closureReplica.setParameters(parameterReplicas);
                    for (int i = 0; i < numberOfParameters; i++) {
                        parameterReplicas[i] = replicateVariable(originalParameters[i]);
                        assert parameterReplicas[i] != originalParameters[i];
                    }
                    toDo.add(new ClosureBodyReplication(closureReplica, originalClosure.body()));
                    replication.assign(closureReplica);
                } else if (replication.original instanceof CirBlock) {
                    if (blockReplicationPolicy == BlockReplicationPolicy.ALL) {
                        final CirBlock originalBlock = (CirBlock) replication.original;
                        CirBlock blockReplica = blockMap.get(originalBlock);
                        if (blockReplica == null) {
                            blockReplica = (CirBlock) originalBlock.clone();
                            blockMap.put(originalBlock, blockReplica);
                            blockReplica.reset();
                            toDo.add(new BlockClosureReplication(blockReplica, originalBlock.closure()));
                        }
                        replication.assign(blockReplica);
                    } else {
                        replication.assign(replication.original);
                    }
                } else {
                    replication.assign(replication.original);
                }
            }
            if (toDo.isEmpty()) {
                return result;
            }
            replication = toDo.removeFirst();
        }
    }

    public static CirClosure apply(final CirClosure node) {
        final CirReplication replication = new CirReplication(BlockReplicationPolicy.ALL);
        return (CirClosure) replication.run(node);
    }

    /**
     * Replicates the closure of a {@link #CirBlock}. The replication stops copying once another
     * block is encountered which is the desired semantics when inlining blocks.
     */
    public static CirClosure replicateLocalClosure(CirClosure closure) {
        final CirReplication replication = new CirReplication(BlockReplicationPolicy.NONE);
        return (CirClosure) replication.run(closure);
    }
}
