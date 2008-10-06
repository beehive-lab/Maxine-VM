/*
 * Copyright (c) 2007 Sun Microsystems, Inc.  All rights reserved.
 *
 * Sun Microsystems, Inc. has intellectual property rights relating to technology embodied in the product
 * that is described in this document. In particular, and without limitation, these intellectual property
 * rights may include one or more of the U.S. patents listed at http://www.sun.com/patents and one or
 * more additional patents or pending patent applications in the U.S. and in other countries.
 *
 * U.S. Government Rights - Commercial software. Government users are subject to the Sun
 * Microsystems, Inc. standard license agreement and applicable provisions of the FAR and its
 * supplements.
 *
 * Use is subject to license terms. Sun, Sun Microsystems, the Sun logo, Java and Solaris are trademarks or
 * registered trademarks of Sun Microsystems, Inc. in the U.S. and other countries. All SPARC trademarks
 * are used under license and are trademarks or registered trademarks of SPARC International, Inc. in the
 * U.S. and other countries.
 *
 * UNIX is a registered trademark in the U.S. and other countries, exclusively licensed through X/Open
 * Company, Ltd.
 */
/*VCSID=991cf16e-ba9e-4a43-adf3-13961a927506*/
package com.sun.max.vm.compiler.cir.transform;

import java.util.*;

import com.sun.max.vm.compiler.cir.*;
import com.sun.max.vm.compiler.cir.variable.*;

/**
 * Deep CIR copying, preserving node sharing.
 * 
 * @author Bernd Mathiske
 */
public final class CirReplication {

    private enum BlockReplicationPolicy {
        NONE, ALL;
    }

    private final BlockReplicationPolicy _blockReplicationPolicy;

    protected CirReplication(BlockReplicationPolicy blockReplicationPolicy) {
        _blockReplicationPolicy = blockReplicationPolicy;
    }

    private CirNode _result = null;

    private class Replication {
        private CirNode _original;

        protected Replication(CirNode original) {
            assert original != null;
            _original = original;
        }

        protected void assign(CirNode replica) {
            assert replica != null;
            _result = replica;
        }
    }

    private final class ArrayValueReplication extends Replication {
        private final CirValue[] _array;
        private final int _index;

        private ArrayValueReplication(CirValue[] array, int index, CirValue originalArgument) {
            super(originalArgument);
            _array = array;
            _index = index;
        }

        @Override
        protected void assign(CirNode argumentReplica) {
            assert argumentReplica != null;
            _array[_index] = (CirValue) argumentReplica;
        }
    }

    private CirValue[] replicateValues(CirValue[] originalValues, LinkedList<Replication> toDo) {
        final CirValue[] valuesReplica = new CirValue[originalValues.length];
        for (int i = 0; i < originalValues.length; i++) {
            if (originalValues[i] != null) {
                toDo.add(new ArrayValueReplication(valuesReplica, i, originalValues[i]));
            }
        }
        return valuesReplica;
    }

    private final class CallProcedureReplication extends Replication {
        private final CirCall _call;

        private CallProcedureReplication(CirCall call, CirValue originalProcedure) {
            super(originalProcedure);
            _call = call;
        }

        @Override
        protected void assign(CirNode procedureReplica) {
            _call.setProcedure((CirValue) procedureReplica, _call.bytecodeLocation());
        }
    }

    private final class ClosureBodyReplication extends Replication {
        private final CirClosure _closure;

        private ClosureBodyReplication(CirClosure closure, CirCall originalBody) {
            super(originalBody);
            _closure = closure;
        }

        @Override
        protected void assign(CirNode bodyReplica) {
            _closure.setBody((CirCall) bodyReplica);
        }
    }

    private final class BlockClosureReplication extends Replication {
        private final CirBlock _block;

        private BlockClosureReplication(CirBlock block, CirClosure originalClosure) {
            super(originalClosure);
            _block = block;
        }

        @Override
        protected void assign(CirNode closureReplica) {
            _block.setClosure((CirClosure) closureReplica);
        }
    }

    private final Map<CirVariable, CirVariable> _variableMap = new IdentityHashMap<CirVariable, CirVariable>();
    private final CirVariableFactory _variableFactory = new CirVariableFactory();

    private CirVariable replicateVariable(CirVariable original) {
        CirVariable replica = _variableMap.get(original);
        if (replica == null) {
            replica = _variableFactory.createFresh(original);
            _variableMap.put(original, replica);
        }
        return replica;
    }

    private CirJavaFrameDescriptor replicateJavaFrameDescriptor(CirJavaFrameDescriptor javaFrameDescriptor, LinkedList<Replication> toDo) {
        if (javaFrameDescriptor == null) {
            return null;
        }
        final CirJavaFrameDescriptor parentReplica = replicateJavaFrameDescriptor(javaFrameDescriptor.parent(), toDo);
        final CirJavaFrameDescriptor replica = new CirJavaFrameDescriptor(parentReplica, javaFrameDescriptor.bytecodeLocation(),
                                                                                 replicateValues(javaFrameDescriptor.locals(), toDo),
                                                                                 replicateValues(javaFrameDescriptor.stackSlots(), toDo));
        return replica;
    }

    private final Map<CirBlock, CirBlock> _blockMap = new IdentityHashMap<CirBlock, CirBlock>();

    private CirNode run(CirNode node) {
        final LinkedList<Replication> toDo = new LinkedList<Replication>();
        Replication replication = new Replication(node);
        while (true) {
            if (replication._original instanceof CirCall) {
                final CirCall originalCall = (CirCall) replication._original;
                final CirCall callReplica = (CirCall) originalCall.clone();
                replication.assign(callReplica);
                callReplica.setArguments(replicateValues(originalCall.arguments(), toDo));
                toDo.add(new CallProcedureReplication(callReplica, originalCall.procedure()));
                if (originalCall.javaFrameDescriptor() != null) {
                    callReplica.setJavaFrameDescriptor(replicateJavaFrameDescriptor(originalCall.javaFrameDescriptor(), toDo));
                }
            } else {
                assert replication._original instanceof CirValue;
                if (replication._original instanceof CirVariable) {
                    replication.assign(replicateVariable((CirVariable) replication._original));
                } else if (replication._original instanceof CirClosure) {
                    final CirClosure originalClosure = (CirClosure) replication._original;
                    final CirClosure closureReplica = (CirClosure) originalClosure.clone();
                    final CirVariable[] originalParameters = originalClosure.parameters();
                    final int numberOfParameters = originalParameters.length;
                    final CirVariable[] parameterReplicas = new CirVariable[numberOfParameters];
                    closureReplica.setParameters(parameterReplicas);
                    for (int i = 0; i < numberOfParameters; i++) {
                        parameterReplicas[i] = replicateVariable(originalParameters[i]);
                        assert parameterReplicas[i] != originalParameters[i];
                    }
                    toDo.add(new ClosureBodyReplication(closureReplica, originalClosure.body()));
                    replication.assign(closureReplica);
                } else if (replication._original instanceof CirBlock) {
                    if (_blockReplicationPolicy == BlockReplicationPolicy.ALL) {
                        final CirBlock originalBlock = (CirBlock) replication._original;
                        CirBlock blockReplica = _blockMap.get(originalBlock);
                        if (blockReplica == null) {
                            blockReplica = (CirBlock) originalBlock.clone();
                            _blockMap.put(originalBlock, blockReplica);
                            blockReplica.reset();
                            toDo.add(new BlockClosureReplication(blockReplica, originalBlock.closure()));
                        }
                        replication.assign(blockReplica);
                    } else {
                        replication.assign(replication._original);
                    }
                } else {
                    replication.assign(replication._original);
                }
            }
            if (toDo.isEmpty()) {
                return _result;
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
