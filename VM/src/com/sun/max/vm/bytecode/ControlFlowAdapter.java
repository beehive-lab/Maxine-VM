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
package com.sun.max.vm.bytecode;

import com.sun.cri.bytecode.*;

/**
 * Byte code visitor that focuses on control flow changing opcodes.
 *
 * @author Bernd Mathiske
 */
public abstract class ControlFlowAdapter extends BytecodeAdapter {

    /**
     * The {@linkplain #currentOpcode() current opcode} has an explicit control transfer branch to a given address.
     *
     * @param address
     *                the jump target address
     */
    public abstract void jump(int address);

    /**
     * The {@linkplain #currentOpcode() current opcode} has a fall-through flow of control to its lexical successor.
     *
     * @param address
     *                the address of the current opcode's lexical successor in the bytecode stream
     */
    public abstract void fallThrough(int address);

    /**
     * The {@linkplain #currentOpcode() current opcode} terminates a basic block.
     */
    public abstract void terminate();

    /**
     * Control flow is forking into two branches: one starts at the current address
     * and the other at the given offset from it.
     * @param offset the offset of the taken branch
     */
    private void fork(int offset) {
        fallThrough(currentBCI());
        jump(currentOpcodeBCI() + offset);
    }

    @Override
    protected void ifeq(int offset) {
        fork(offset);
    }

    @Override
    protected void ifne(int offset) {
        fork(offset);
    }

    @Override
    protected void iflt(int offset) {
        fork(offset);
    }

    @Override
    protected void ifge(int offset) {
        fork(offset);
    }

    @Override
    protected void ifgt(int offset) {
        fork(offset);
    }

    @Override
    protected void ifle(int offset) {
        fork(offset);
    }

    @Override
    protected void if_icmpeq(int offset) {
        fork(offset);
    }

    @Override
    protected void if_icmpne(int offset) {
        fork(offset);
    }

    @Override
    protected void if_icmplt(int offset) {
        fork(offset);
    }

    @Override
    protected void if_icmpge(int offset) {
        fork(offset);
    }

    @Override
    protected void if_icmpgt(int offset) {
        fork(offset);
    }

    @Override
    protected void if_icmple(int offset) {
        fork(offset);
    }

    @Override
    protected void if_acmpeq(int offset) {
        fork(offset);
    }

    @Override
    protected void if_acmpne(int offset) {
        fork(offset);
    }

    @Override
    protected void goto_(int offset) {
        jump(currentOpcodeBCI() + offset);
    }

    @Override
    protected void goto_w(int offset) {
        goto_(offset);
    }

    @Override
    protected void tableswitch(int defaultOffset, int lowMatch, int highMatch, int numberOfCases) {
        jump(currentOpcodeBCI() + defaultOffset);
        for (int i = 0; i < numberOfCases; i++) {
            final BytecodeScanner scanner = bytecodeScanner();
            jump(currentOpcodeBCI() + scanner.readSwitchOffset());
        }
    }

    @Override
    protected void lookupswitch(int defaultOffset, int numberOfCases) {
        jump(currentOpcodeBCI() + defaultOffset);
        for (int i = 0; i < numberOfCases; i++) {
            final BytecodeScanner scanner = bytecodeScanner();
            scanner.readSwitchCase(); // ignore case value
            jump(currentOpcodeBCI() + scanner.readSwitchOffset());
        }
    }

    @Override
    protected void ireturn() {
        terminate();
    }

    @Override
    protected void lreturn() {
        terminate();
    }

    @Override
    protected void freturn() {
        terminate();
    }

    @Override
    protected void dreturn() {
        terminate();
    }

    @Override
    protected void areturn() {
        terminate();
    }

    @Override
    protected boolean extension(int opcode, boolean isWide) {
        if (opcode == Bytecodes.WRETURN) {
            terminate();
        }
        return false;
    }

    @Override
    protected void vreturn() {
        terminate();
    }

    @Override
    protected void athrow() {
        terminate();
    }

    @Override
    protected void ifnull(int offset) {
        fork(offset);
    }

    @Override
    protected void ifnonnull(int offset) {
        fork(offset);
    }

}
