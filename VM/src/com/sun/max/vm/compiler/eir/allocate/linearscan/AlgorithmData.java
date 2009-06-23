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
package com.sun.max.vm.compiler.eir.allocate.linearscan;

import com.sun.max.collect.*;
import com.sun.max.vm.compiler.eir.*;

/**
 * Data object that the parts of the linear scan register allocator algorithm operate on.
 *
 * @author Thomas Wuerthinger
 */
public final class AlgorithmData {

    private final EirMethodGeneration _generation;
    private Sequence<Interval> _sortedIntervals;
    private final EirRegister[] _integerRegisters;
    private final EirRegister[] _floatingPointRegisters;
    private VerificationRunResult _verificationRunResult;
    private Sequence<EirBlock> _linearScanOrder;
    private Sequence<ParentInterval> _parentIntervals;
    private VariableMapping<Integer, AppendableSequence<Pair<Interval, Interval>>> _splitMoves;

    public AlgorithmData(EirMethodGeneration generation, EirRegister[] integerRegisters, EirRegister[] floatingPointRegisters) {
        _generation = generation;
        _integerRegisters = integerRegisters;
        _splitMoves = new ChainedHashMapping<Integer, AppendableSequence<Pair<Interval, Interval>>>();
        _floatingPointRegisters = floatingPointRegisters;
    }

    public void addSplitMove(int position, Interval a, Interval p) {
        assert position % 2 == 1;

        assert a != p && a.variable() != p.variable();

        final Pair<Interval, Interval> pair = new Pair<Interval, Interval>(a, p);
        if (!_splitMoves.containsKey(position)) {
            _splitMoves.put(position, new LinkSequence<Pair<Interval, Interval>>());
        }

        _splitMoves.get(position).append(pair);
    }

    public Mapping<Integer, AppendableSequence<Pair<Interval, Interval>>> splitMoves() {
        return _splitMoves;
    }

    public Sequence<EirBlock> linearScanOrder() {
        return _linearScanOrder;
    }

    public void setLinearScanOrder(Sequence<EirBlock> blocks) {
        _linearScanOrder = blocks;
    }

    public Sequence<ParentInterval> parentIntervals() {
        return _parentIntervals;
    }

    public void setParentIntervals(Sequence<ParentInterval> parentIntervals) {
        _parentIntervals = parentIntervals;
    }

    public EirRegister[] floatingPointRegisters() {
        return _floatingPointRegisters;
    }

    public VerificationRunResult verificationRunResult() {
        return _verificationRunResult;
    }

    public void setVerificationRunResult(VerificationRunResult result) {
        _verificationRunResult = result;
    }

    public EirMethodGeneration generation() {
        return _generation;
    }

    public EirRegister[] integerRegisters() {
        return _integerRegisters;
    }

    public Sequence<Interval> sortedIntervals() {
        return _sortedIntervals;
    }

    public void setSortedIntervals(Sequence<Interval> intervals) {
        _sortedIntervals = intervals;
    }

    public void clearFloatingPointRegister(EirRegister location) {
        _floatingPointRegisters[location.ordinal()] = null;

    }

    public void clearIntegerRegister(EirRegister location) {
        _integerRegisters[location.ordinal()] = null;

    }

}
