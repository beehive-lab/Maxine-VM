/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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

    public final OpcodeBCIRelocator transform(BytecodeBlock bytecodeBlock) {
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

        final OpcodeBCIRelocator opcodeAddressRelocator = new OpcodeBCIRelocator() {
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
            for (int address = currentOpcodeBCI(); address != currentBCI(); ++address) {
                assembler.appendByte(bytes[address]);
            }
        } else {
            ignoreCurrentInstruction = false;
        }

        opcodeRelocationMap[currentOpcodeBCI()] = currentToAddress;
        currentToAddress = assembler.currentAddress();
    }

    private Label relocatableTarget(int offset) {
        final int target = currentOpcodeBCI() + offset;
        if (relocatableTargets[target] == null) {
            relocatableTargets[target] = assembler.newLabel();
        }
        return relocatableTargets[target];
    }

    private void branch(int offset) {
        final int target = currentOpcodeBCI() + offset;
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
