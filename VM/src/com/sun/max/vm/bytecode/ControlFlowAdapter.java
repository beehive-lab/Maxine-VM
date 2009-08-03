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
package com.sun.max.vm.bytecode;


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
     */
    private void fork(int offset) {
        fallThrough(currentBytePosition());
        jump(currentOpcodePosition() + offset);
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
        jump(currentOpcodePosition() + offset);
    }

    @Override
    protected void goto_w(int offset) {
        goto_(offset);
    }

    @Override
    protected void tableswitch(int defaultOffset, int lowMatch, int highMatch, int numberOfCases) {
        jump(currentOpcodePosition() + defaultOffset);
        for (int i = 0; i < numberOfCases; i++) {
            final BytecodeScanner scanner = bytecodeScanner();
            jump(currentOpcodePosition() + scanner.readSwitchOffset());
        }
    }

    @Override
    protected void lookupswitch(int defaultOffset, int numberOfCases) {
        jump(currentOpcodePosition() + defaultOffset);
        for (int i = 0; i < numberOfCases; i++) {
            final BytecodeScanner scanner = bytecodeScanner();
            scanner.readSwitchCase(); // ignore case value
            jump(currentOpcodePosition() + scanner.readSwitchOffset());
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
