/*
 * Copyright (c) 2009 Sun Microsystems, Inc.  All rights reserved.
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
package com.sun.c1x.alloc;

import java.util.*;

import com.sun.c1x.*;
import com.sun.c1x.ci.CiRegister;
import com.sun.c1x.debug.*;
import com.sun.c1x.lir.*;
import com.sun.c1x.util.*;

/**
 *
 * @author Thomas Wuerthinger
 */
final class MoveResolver {

    private final LinearScan allocator;

    private LIRList insertList;
    private int insertIdx;
    private LIRInsertionBuffer insertionBuffer; // buffer where moves are inserted

    private final List<Interval> mappingFrom;
    private final List<LIROperand> mappingFromOpr;
    private final List<Interval> mappingTo;
    private boolean multipleReadsAllowed;
    private final int[] registerBlocked;
    private final CiRegister.AllocationSet allocatableRegisters;

    private int registerBlocked(int reg) {
        assert reg >= 0 && reg < allocatableRegisters.nofRegs : "out of bounds";
        return registerBlocked[reg];
    }

    private void setRegisterBlocked(int reg, int direction) {
        assert reg >= 0 && reg < allocatableRegisters.nofRegs : "out of bounds";
        assert direction == 1 || direction == -1 : "out of bounds";
        registerBlocked[reg] += direction;
    }

    void setMultipleReadsAllowed() {
        multipleReadsAllowed = true;
    }

    boolean hasMappings() {
        return mappingFrom.size() > 0;
    }

    MoveResolver(LinearScan allocator) {

        this.allocator = allocator;
        this.multipleReadsAllowed = false;
        this.mappingFrom = new ArrayList<Interval>(8);
        this.mappingFromOpr = new ArrayList<LIROperand>(8);
        this.mappingTo = new ArrayList<Interval>(8);
        this.insertIdx = -1;
        this.insertionBuffer = new LIRInsertionBuffer();
        this.allocatableRegisters = allocator.allocatableRegisters;
        this.registerBlocked = new int[allocator.allocatableRegisters.nofRegs];
        assert checkEmpty();
    }

    boolean checkEmpty() {
        assert mappingFrom.size() == 0 && mappingFromOpr.size() == 0 && mappingTo.size() == 0 : "list must be empty before and after processing";
        for (int i = 0; i < allocatableRegisters.nofRegs; i++) {
            assert registerBlocked(i) == 0 : "register map must be empty before and after processing";
        }
        assert !multipleReadsAllowed : "must have default value";
        return true;
    }

    private boolean verifyBeforeResolve() {
        assert mappingFrom.size() == mappingFromOpr.size() : "length must be equal";
        assert mappingFrom.size() == mappingTo.size() : "length must be equal";
        assert insertList != null && insertIdx != -1 : "insert position not set";

        int i;
        int j;
        if (!multipleReadsAllowed) {
            for (i = 0; i < mappingFrom.size(); i++) {
                for (j = i + 1; j < mappingFrom.size(); j++) {
                    assert mappingFrom.get(i) == null || mappingFrom.get(i) != mappingFrom.get(j) : "cannot read from same interval twice";
                }
            }
        }

        for (i = 0; i < mappingTo.size(); i++) {
            for (j = i + 1; j < mappingTo.size(); j++) {
                assert mappingTo.get(i) != mappingTo.get(j) : "cannot write to same interval twice";
            }
        }

        BitMap usedRegs = new BitMap(allocatableRegisters.nofRegs + allocator.maxSpills);
        usedRegs.clearAll();
        if (!multipleReadsAllowed) {
            for (i = 0; i < mappingFrom.size(); i++) {
                Interval it = mappingFrom.get(i);
                if (it != null) {
                    assert !usedRegs.get(it.assignedReg()) : "cannot read from same register twice";
                    usedRegs.set(it.assignedReg());

                    if (it.assignedRegHi() != LinearScan.getAnyreg()) {
                        assert !usedRegs.get(it.assignedRegHi()) : "cannot read from same register twice";
                        usedRegs.set(it.assignedRegHi());
                    }
                }
            }
        }

        usedRegs.clearAll();
        for (i = 0; i < mappingTo.size(); i++) {
            Interval it = mappingTo.get(i);
            assert !usedRegs.get(it.assignedReg()) : "cannot write to same register twice";
            usedRegs.set(it.assignedReg());

            if (it.assignedRegHi() != LinearScan.getAnyreg()) {
                assert !usedRegs.get(it.assignedRegHi()) : "cannot write to same register twice";
                usedRegs.set(it.assignedRegHi());
            }
        }

        usedRegs.clearAll();
        for (i = 0; i < mappingFrom.size(); i++) {
            Interval it = mappingFrom.get(i);
            if (it != null && it.assignedReg() >= allocatableRegisters.nofRegs) {
                usedRegs.set(it.assignedReg());
            }
        }
        for (i = 0; i < mappingTo.size(); i++) {
            Interval it = mappingTo.get(i);
            assert !usedRegs.get(it.assignedReg()) || it.assignedReg() == mappingFrom.get(i).assignedReg() : "stack slots used in mappingFrom must be disjoint to mappingTo";
        }

        return true;
    }

    // mark assignedReg and assignedRegHi of the interval as blocked
    private void blockRegisters(Interval it) {
        int reg = it.assignedReg();
        if (reg < allocatableRegisters.nofRegs) {
            assert multipleReadsAllowed || registerBlocked(reg) == 0 : "register already marked as used";
            setRegisterBlocked(reg, 1);
        }
        reg = it.assignedRegHi();
        if (reg != LinearScan.getAnyreg() && reg < allocatableRegisters.nofRegs) {
            assert multipleReadsAllowed || registerBlocked(reg) == 0 : "register already marked as used";
            setRegisterBlocked(reg, 1);
        }
    }

    // mark assignedReg and assignedRegHi of the interval as unblocked
    private void unblockRegisters(Interval it) {
        int reg = it.assignedReg();
        if (reg < allocatableRegisters.nofRegs) {
            assert registerBlocked(reg) > 0 : "register already marked as unused";
            setRegisterBlocked(reg, -1);
        }
        reg = it.assignedRegHi();
        if (reg != LinearScan.getAnyreg() && reg < allocatableRegisters.nofRegs) {
            assert registerBlocked(reg) > 0 : "register already marked as unused";
            setRegisterBlocked(reg, -1);
        }
    }

    // check if assignedReg and assignedRegHi of the to-interval are not blocked (or only blocked by from)
    private boolean saveToProcessMove(Interval from, Interval to) {
        int fromReg = -1;
        int fromRegHi = -1;
        if (from != null) {
            fromReg = from.assignedReg();
            fromRegHi = from.assignedRegHi();
        }

        int reg = to.assignedReg();
        if (reg < allocatableRegisters.nofRegs) {
            if (registerBlocked(reg) > 1 || (registerBlocked(reg) == 1 && reg != fromReg && reg != fromRegHi)) {
                return false;
            }
        }
        reg = to.assignedRegHi();
        if (reg != LinearScan.getAnyreg() && reg < allocatableRegisters.nofRegs) {
            if (registerBlocked(reg) > 1 || (registerBlocked(reg) == 1 && reg != fromReg && reg != fromRegHi)) {
                return false;
            }
        }

        return true;
    }

    private void createInsertionBuffer(LIRList list) {
        assert !insertionBuffer.initialized() : "overwriting existing buffer";
        insertionBuffer.init(list);
    }

    private void appendInsertionBuffer() {
        if (insertionBuffer.initialized()) {
            insertionBuffer.lirList().append(insertionBuffer);
        }
        assert !insertionBuffer.initialized() : "must be uninitialized now";

        insertList = null;
        insertIdx = -1;
    }

    private void insertMove(Interval fromInterval, Interval toInterval) {
        assert fromInterval.registerNumber() != toInterval.registerNumber() : "from and to interval equal";
        assert fromInterval.type() == toInterval.type() : "move between different types";
        assert insertList != null && insertIdx != -1 : "must setup insert position first";
        assert insertionBuffer.lirList() == insertList : "wrong insertion buffer";

        LIROperand fromOpr = LIROperand.forVariable(fromInterval.registerNumber(), fromInterval.type());
        LIROperand toOpr = LIROperand.forVariable(toInterval.registerNumber(), toInterval.type());

        insertionBuffer.move(insertIdx, fromOpr, toOpr, null);

        // Util.traceLinearScan(4, "MoveResolver: inserted move from register %d (%d, %d) to %d (%d, %d)",
        //                   fromInterval.regNum(), fromInterval.assignedReg(), fromInterval.assignedRegHi(),
        //                   toInterval.regNum(), toInterval.assignedReg(), toInterval.assignedRegHi());
    }

    private void insertMove(LIROperand fromOpr, Interval toInterval) {
        assert fromOpr.kind == toInterval.type() : "move between different types";
        assert insertList != null && insertIdx != -1 : "must setup insert position first";
        assert insertionBuffer.lirList() == insertList : "wrong insertion buffer";

        LIROperand toOpr = LIROperand.forVariable(toInterval.registerNumber(), toInterval.type());
        insertionBuffer.move(insertIdx, fromOpr, toOpr, null);

        if (C1XOptions.TraceLinearScanLevel >= 4) {
            TTY.print("MoveResolver: inserted move from constant %s to %d (%d, %d)", fromOpr, toInterval.registerNumber(), toInterval.assignedReg(), toInterval.assignedRegHi());
        }
    }

    private void resolveMappings() {
        // Util.traceLinearScan(4, "MoveResolver: resolving mappings for Block B%d, index %d", insertList.block() != null ? insertList.block().blockID : -1, insertIdx);
        assert verifyBeforeResolve();

        // Block all registers that are used as input operands of a move.
        // When a register is blocked, no move to this register is emitted.
        // This is necessary for detecting cycles in moves.
        int i;
        for (i = mappingFrom.size() - 1; i >= 0; i--) {
            Interval fromInterval = mappingFrom.get(i);
            if (fromInterval != null) {
                blockRegisters(fromInterval);
            }
        }

        int spillCandidate = -1;
        while (mappingFrom.size() > 0) {
            boolean processedInterval = false;

            for (i = mappingFrom.size() - 1; i >= 0; i--) {
                Interval fromInterval = mappingFrom.get(i);
                Interval toInterval = mappingTo.get(i);

                if (saveToProcessMove(fromInterval, toInterval)) {
                    // this inverval can be processed because target is free
                    if (fromInterval != null) {
                        insertMove(fromInterval, toInterval);
                        unblockRegisters(fromInterval);
                    } else {
                        insertMove(mappingFromOpr.get(i), toInterval);
                    }
                    mappingFrom.remove(i);
                    mappingFromOpr.remove(i);
                    mappingTo.remove(i);

                    processedInterval = true;
                } else if (fromInterval != null && fromInterval.assignedReg() < allocatableRegisters.nofRegs) {
                    // this interval cannot be processed now because target is not free
                    // it starts in a register, so it is a possible candidate for spilling
                    spillCandidate = i;
                }
            }

            if (!processedInterval) {
                // no move could be processed because there is a cycle in the move list
                // (e.g. r1 . r2, r2 . r1), so one interval must be spilled to memory
                assert spillCandidate != -1 : "no interval in register for spilling found";

                // create a new spill interval and assign a stack slot to it
                Interval fromInterval = mappingFrom.get(spillCandidate);
                Interval spillInterval = new Interval(-1);
                spillInterval.setType(fromInterval.type());

                // add a dummy range because real position is difficult to calculate
                // Note: this range is a special case when the integrity of the allocation is checked
                spillInterval.addRange(1, 2);

                // do not allocate a new spill slot for temporary interval, but
                // use spill slot assigned to fromInterval. Otherwise moves from
                // one stack slot to another can happen (not allowed by LIRAssembler
                int spillSlot = fromInterval.canonicalSpillSlot();
                if (spillSlot < 0) {
                    spillSlot = allocator.allocateSpillSlot(allocator.numberOfSpillSlots(spillInterval.type()) == 2);
                    fromInterval.setCanonicalSpillSlot(spillSlot);
                }
                spillInterval.assignReg(spillSlot);
                allocator.appendInterval(spillInterval);

                // Util.traceLinearScan(4, "created new Interval %d for spilling", spillInterval.regNum());

                // insert a move from register to stack and update the mapping
                insertMove(fromInterval, spillInterval);
                mappingFrom.set(spillCandidate, spillInterval);
                unblockRegisters(fromInterval);
            }
        }

        // reset to default value
        multipleReadsAllowed = false;

        // check that all intervals have been processed
        assert checkEmpty();
    }

    void setInsertPosition(LIRList insertList, int insertIdx) {
        // Util.traceLinearScan(4, "MoveResolver: setting insert position to Block B%d, index %d", insertList.block() != null ? insertList.block().blockID : -1, insertIdx);
        assert this.insertList == null && this.insertIdx == -1 : "use moveInsertPosition instead of setInsertPosition when data already set";

        createInsertionBuffer(insertList);
        this.insertList = insertList;
        this.insertIdx = insertIdx;
    }

    void moveInsertPosition(LIRList insertList, int insertIdx) {
        // Util.traceLinearScan(4, "MoveResolver: moving insert position to Block B%d, index %d", (insertList != null && insertList.block() != null) ? insertList.block().blockID : -1, insertIdx);

        if (this.insertList != null && (this.insertList != insertList || this.insertIdx != insertIdx)) {
            // insert position changed . resolve current mappings
            resolveMappings();
        }

        if (this.insertList != insertList) {
            // block changed . append insertionBuffer because it is
            // bound to a specific block and create a new insertionBuffer
            appendInsertionBuffer();
            createInsertionBuffer(insertList);
        }

        this.insertList = insertList;
        this.insertIdx = insertIdx;
    }

    void addMapping(Interval fromInterval, Interval toInterval) {
        // Util.traceLinearScan(4, "MoveResolver: adding mapping from %d (%d, %d) to %d (%d, %d)", fromInterval.registerNumber(), fromInterval.assignedReg(), fromInterval.assignedRegHi(), toInterval.registerNumber(),
        //                 toInterval.assignedReg(), toInterval.assignedRegHi());

        assert fromInterval.type() == toInterval.type();
        mappingFrom.add(fromInterval);
        mappingFromOpr.add(LIROperand.IllegalLocation);
        mappingTo.add(toInterval);
    }

    void addMapping(LIROperand fromOpr, Interval toInterval) {
        if (C1XOptions.TraceLinearScanLevel >= 4) {
            TTY.println("MoveResolver: adding mapping from %s to %d (%d, %d)", fromOpr, toInterval.registerNumber(), toInterval.assignedReg(), toInterval.assignedRegHi());
        }
        assert LIROperand.isConstant(fromOpr) : "only for constants";

        mappingFrom.add(null);
        mappingFromOpr.add(fromOpr);
        mappingTo.add(toInterval);
    }

    void resolveAndAppendMoves() {
        if (hasMappings()) {
            resolveMappings();
        }
        appendInsertionBuffer();
    }
}
