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
package com.sun.max.vm.bytecode.graft;

import java.util.*;

import com.sun.max.vm.bytecode.*;
import com.sun.max.vm.bytecode.graft.BytecodeAssembler.*;

/**
 * A specialization of a {@linkplain BytecodeVisitor bytecode visitor} that is useful when
 * performing a transformation on some input bytecode. This mechanism takes care of
 * relocating branches if the transformation involves inserting and/or deleting code
 * that changes the distance between a branch and its target.
 *
 * @author Doug Simon
 */
public class BytecodeTransformer extends BytecodeAdapter {

    private final BytecodeAssembler assembler;

    public BytecodeTransformer(BytecodeAssembler assembler) {
        this.assembler = assembler;
    }

    private boolean ignoreCurrentInstruction;

    private int[] opcodeRelocationMap;
    private Label[] relocatableTargets;
    private int currentToAddress;

    protected void ignoreCurrentInstruction() {
        ignoreCurrentInstruction = true;
    }

    public BytecodeAssembler asm() {
        return assembler;
    }

    public final OpcodePositionRelocator transform(BytecodeBlock bytecodeBlock) {
        final int originalCodeLength = bytecodeBlock.code().length;
        opcodeRelocationMap = new int[originalCodeLength];
        relocatableTargets = new Label[originalCodeLength];
        currentToAddress = assembler.currentAddress();

        // Initialize the relocation map so that all addresses are initially illegal
        Arrays.fill(opcodeRelocationMap, -1);

        new BytecodeScanner(this).scan(bytecodeBlock);
        fixup();

        final int endAddress = assembler.currentAddress();
        final int[] opcodeRelocationMap = this.opcodeRelocationMap;

        this.opcodeRelocationMap = null;
        relocatableTargets = null;

        final OpcodePositionRelocator opcodeAddressRelocator = new OpcodePositionRelocator() {
            public int relocate(int address) throws IllegalArgumentException {
                final int relocatedAddress;
                if (address == originalCodeLength) {
                    relocatedAddress = endAddress;
                } else {
                    try {
                        relocatedAddress = opcodeRelocationMap[address];
                    } catch (ArrayIndexOutOfBoundsException e) {
                        throw new IllegalArgumentException();
                    }
                }

                if (relocatedAddress == -1) {
                    throw new IllegalArgumentException();
                }
                return relocatedAddress;
            }

            @Override
            public String toString() {
                final StringBuilder buf = new StringBuilder();
                for (int address = 0; address < originalCodeLength; ++address) {
                    buf.append(address).append(" -> ").append(opcodeRelocationMap[address]).append("\n");
                }
                buf.append(originalCodeLength).append(" -> ").append(endAddress).append("\n");
                return buf.toString();
            }
        };

        return opcodeAddressRelocator;
    }

    private void fixup() {
        for (int target = 0; target != relocatableTargets.length; ++target) {
            final Label relocatableTarget = relocatableTargets[target];
            if (relocatableTarget != null && !relocatableTarget.isBound()) {
                relocatableTarget.bind(opcodeRelocationMap[target]);
            }
        }
    }

    @Override
    protected final void instructionDecoded() {
        if (!ignoreCurrentInstruction) {
            final byte[] bytes = code();
            for (int address = currentOpcodePosition(); address != currentBytePosition(); ++address) {
                assembler.appendByte(bytes[address]);
            }
        } else {
            ignoreCurrentInstruction = false;
        }

        opcodeRelocationMap[currentOpcodePosition()] = currentToAddress;
        currentToAddress = assembler.currentAddress();
    }

    private Label relocatableTarget(int offset) {
        final int target = currentOpcodePosition() + offset;
        if (relocatableTargets[target] == null) {
            relocatableTargets[target] = assembler.newLabel();
        }
        return relocatableTargets[target];
    }

    private void branch(int offset) {
        final int target = currentOpcodePosition() + offset;
        if (offset < 0) {
            final int relocatedTarget = opcodeRelocationMap[target];
            assembler.branch(currentOpcode(), relocatedTarget);
        } else {
            assembler.branch(currentOpcode(), relocatableTarget(offset));
        }
        ignoreCurrentInstruction();
    }

    @Override
    protected void ifeq(int offset) {
        branch(offset);
    }

    @Override
    protected void ifne(int offset) {
        branch(offset);
    }

    @Override
    protected void iflt(int offset) {
        branch(offset);
    }

    @Override
    protected void ifge(int offset) {
        branch(offset);
    }

    @Override
    protected void ifgt(int offset) {
        branch(offset);
    }

    @Override
    protected void ifle(int offset) {
        branch(offset);
    }

    @Override
    protected void if_icmpeq(int offset) {
        branch(offset);
    }

    @Override
    protected void if_icmpne(int offset) {
        branch(offset);
    }

    @Override
    protected void if_icmplt(int offset) {
        branch(offset);
    }

    @Override
    protected void if_icmpge(int offset) {
        branch(offset);
    }

    @Override
    protected void if_icmpgt(int offset) {
        branch(offset);
    }

    @Override
    protected void if_icmple(int offset) {
        branch(offset);
    }

    @Override
    protected void if_acmpeq(int offset) {
        branch(offset);
    }

    @Override
    protected void if_acmpne(int offset) {
        branch(offset);
    }

    @Override
    protected void goto_(int offset) {
        branch(offset);
    }

    @Override
    protected void goto_w(int offset) {
        branch(offset);
    }

    @Override
    protected void jsr_w(int offset) {
        branch(offset);
    }

    @Override
    protected void jsr(int offset) {
        branch(offset);
    }

    @Override
    protected void tableswitch(int defaultOffset, int lowMatch, int highMatch, int numberOfCases) {
        final Label[] relocatableTargets = new Label[numberOfCases];
        for (int i = 0; i != numberOfCases; ++i) {
            relocatableTargets[i] = relocatableTarget(bytecodeScanner().readSwitchOffset());
        }
        assembler.tableswitch(relocatableTarget(defaultOffset), lowMatch, highMatch, relocatableTargets);
        ignoreCurrentInstruction();
    }

    @Override
    protected void lookupswitch(int defaultOffset, int numberOfCases) {
        final Label[] relocatableTargets = new Label[numberOfCases];
        final int[] matches = new int[numberOfCases];
        for (int i = 0; i != numberOfCases; ++i) {
            final BytecodeScanner scanner = bytecodeScanner();
            matches[i] = scanner.readSwitchCase();
            relocatableTargets[i] = relocatableTarget(scanner.readSwitchOffset());
        }
        assembler.lookupswitch(relocatableTarget(defaultOffset), matches, relocatableTargets);
        ignoreCurrentInstruction();
    }

    @Override
    protected void ifnull(int offset) {
        branch(offset);
    }

    @Override
    protected void ifnonnull(int offset) {
        branch(offset);
    }
}
