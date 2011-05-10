/*
 * Copyright (c) 2010, 2010, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.cri.bytecode;

import static com.sun.cri.bytecode.Bytecodes.*;

import java.io.*;

/**
 * A utility to process a bytecode stream to replace certain standard
 * JVM instructions with extended bytecode instructions. The extended
 * instructions are for:
 *
 *<ol>
 * <li>Supporting a Word type hierarchy that is separate from the Object type hierarchy.
 * <li>Supporting low-level, non-Java operations such as reading/writing registers.
 * <li>Other builtins necessary for writing a meta-circular VM.
 * </ol>
 *
 * The transformations made by this utility assume the input code has been verified
 * (or would successfully pass verification).
 *
 * @author Doug Simon
 */
public final class BytecodeIntrinsifier {

    /**
     * Set to true to trace the bytecode processing.
     */
    private static final boolean DEBUG = false;

    /**
     * The interface implemented by a client that submits code to be intrinsified.
     * This interface is comprised of a number of methods for the bytecode instructions
     * that are amenable to intrinsification.
     *
     * @author Doug Simon
     */
    public abstract static class IntrinsifierClient {

        public abstract void invokevirtual(BytecodeIntrinsifier bi, int cpi);
        public abstract void invokestatic(BytecodeIntrinsifier bi, int cpi);
        public abstract void invokespecial(BytecodeIntrinsifier bi, int cpi);
        public abstract void invokeinterface(BytecodeIntrinsifier bi, int cpi);
        public abstract void getstatic(BytecodeIntrinsifier bi, int cpi);
        public abstract void putstatic(BytecodeIntrinsifier bi, int cpi);
        public abstract void getfield(BytecodeIntrinsifier bi, int cpi);
        public abstract void putfield(BytecodeIntrinsifier bi, int cpi);
        public abstract void jnicall(BytecodeIntrinsifier bi, int cpi);
    }

    /**
     * The state of the JVM local variables and operand stack at a given bytecode position.
     */
    static class Frame {

        /**
         * The operand stack depth.
         */
        final int sp;

        /**
         * The local variable state.
         */
        final boolean[] locals;

        /**
         * The operand stack state.
         */
        final boolean[] stack;

        /**
         * Specifies if the basic block denoted by this frame has been {@linkplain BytecodeIntrinsifier#parseBlock(int) parsed}.
         */
        boolean visited;

        /**
         * The next {@code Frame} object in a linked list.
         */
        final Frame next;

        /**
         * The bytecode position of this frame.
         */
        final int bci;

        /**
         * Creates a {@code Frame} object.
         *
         * @param bci the bytecode position
         * @param sp the operand stack depth
         * @param stack the operand stack state
         * @param locals the local variable state
         * @param next the next {@code Frame} in a linked list
         */
        Frame(int bci, int sp, boolean[] stack, boolean[] locals, Frame next) {
            this.bci = bci;
            this.sp = sp;
            this.stack = stack;
            this.locals = locals;
            this.next = next;
        }

        /**
         * Updates the given operand stack and local variable state based on this frame.
         *
         * @param stack the operand stack state modified to match the operand stack state of this frame
         * @param locals the local variable state modified to match the local variable state of this frame
         * @return the stack depth of this frame
         */
        int get(boolean[] stack, boolean[] locals) {
            for (int i = 0; i < sp; ++i) {
                stack[i] = this.stack[i];
            }
            for (int i = sp; i < stack.length; ++i) {
                stack[i] = false;
            }
            for (int i = 0; i < locals.length; ++i) {
                locals[i] = this.locals[i];
            }
            return sp;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(bci).append("[sp=").append(sp);
            if (stack != null) {
                sb.append(" stack=");
                for (int i = 0; i < stack.length; ++i) {
                    sb.append(i < sp ? (stack[i] ? '1' : '0') : '_');
                }
            }
            if (locals != null) {
                sb.append(" locals=");
                for (boolean isWord : locals) {
                    sb.append(isWord ? '1' : '0');
                }
            }
            return sb.append(']').toString();
        }
    }

    /**
     * The work list of basic blocks to be {@linkplain #parseBlock parsed}.
     */
    private Frame todo;

    /**
     * The work list of exception handler entry basic blocks to be {@linkplain #parseBlock parsed}.
     * This list is processed once the {@linkplain #todo primary} work list is completed.
     */
    private Frame todoHandler;

    /**
     * Sentinel value used in {@link #frameMap} to denote that a given
     * instruction ({@code bci}) has been processed. This required as without a
     * pre-pass to establish basic block boundaries, it's possible for
     * {@linkplain #parseBlock(int) block parsing} to cross a block boundary at
     * a (as yet unknown) control flow merge point. There's no need to
     * re-process the instructions after the merge point when the other branch
     * to the merge point is subsequently discovered.
     */
    private static final Frame VISITED = new Frame(-1, 0, null, null, null);
    static {
        VISITED.visited = true;
    }

    /**
     * A map from each {@code bci} to a {@link Frame} object. A entry equal to
     * {@link #VISITED} in this map for a given {@code bci} indicates that the
     * instruction at {@code bci} has already be processed. A {@code null} entry
     * means that the instruction has not yet been processes and not a known
     * basic block entry point. A non-null entry not equals to {@link #VISITED}
     * indicates a known basic block entry point and the value of the
     * {@link Frame} object indicates the frame state to be used when parsing
     * the block.
     */
    private final Frame[] frameMap;

    /**
     * A map from each {@code bci} to the head of a list of {@link Handler} objects denoting the handlers active at that {@code bci}.
     */
    private final Handler[] handlerMap;

    /**
     * The position of the opcode of the instruction currently being processed.
     */
    private int opcodeBci;

    /**
     * The current bytecode position.
     */
    private int bci;

    /**
     * The method being processed.
     */
    private final Object method;

    /**
     * The intrinsification client.
     */
    private final IntrinsifierClient intrinsifier;

    /**
     * Indicates if one or more instructions were replaced with an extended instruction.
     */
    private boolean changed;

    private final byte[] inCode;
    private final byte[] outCode;

    private final boolean[] locals;
    private final boolean[] stack;
    private int sp;

    static class Handler {
        Frame frame;
        final Handler next;
        final int bci;

        Handler(int bci, Handler next) {
            this.bci = bci;
            this.next = next;
        }
    }

    /**
     * Creates an object used to process some given bytecode for the purpose of intrinsification.
     *
     * @param intrinsifier the client side of intrinsification
     * @param method the method being processed. The only use of this value is to call its {@link Object#toString()} method for debug tracing.
     * @param inCode the code to be processed
     * @param outCode the code array in which the processed code is written. If {@code null}, then processing is performed 'in situ' on {@code inCode}
     * @param handlers the exception handler table as an array of triplets (start {@code bci}, end {@code bci}, handler {@code bci}) or
     *            {@code null} if the method has no exception handlers
     * @param maxStack the maximum amount of stack used by {@code inCode}
     * @param locals the initial state of the local variables derived from the signature of the method
     */
    public BytecodeIntrinsifier(IntrinsifierClient intrinsifier, Object method, byte[] inCode, byte[] outCode, int[] handlers, int maxStack, boolean[] locals) {
        this.intrinsifier = intrinsifier;
        this.stack = new boolean[maxStack];
        this.locals = new boolean[locals.length];
        this.inCode = inCode;
        if (outCode == null) {
            this.outCode = inCode;
        } else {
            if (outCode != inCode) {
                assert inCode.length == outCode.length : "outCode.length does not match inCode.length";
                System.arraycopy(inCode, 0, outCode, 0, inCode.length);
            }
            this.outCode = outCode;
        }

        this.method = method;
        this.frameMap = new Frame[inCode.length];
        this.handlerMap = initHandlerMap(inCode, handlers, maxStack, locals);

        Frame entryFrame = new Frame(0, 0, new boolean[maxStack], locals, todo);
        todo = entryFrame;
        frameMap[0] = entryFrame;
    }

    private Handler[] initHandlerMap(byte[] code, int[] handlers, int maxStack, boolean[] locals) {
        if (handlers == null) {
            return null;
        }
        Handler[] handlerMap = new Handler[code.length];
        for (int i = 0; i < handlers.length; i += 3) {
            int start = handlers[i];
            int end = handlers[i + 1];
            int handler = handlers[i + 2];

            Handler previous = null;
            Handler previousExisting = null;
            for (int bci = start; bci < end; ++bci) {
                Handler existing = handlerMap[bci];
                if (previous != null && previous.bci == handler && previous.next == previousExisting) {
                    handlerMap[bci] = previous;
                } else {
                    handlerMap[bci] = new Handler(handler, existing);
                    previous = handlerMap[bci];
                    previousExisting = existing;
                }
            }

        }
        return handlerMap;
    }

    /**
     * Interprets a push of a single-slot value to the stack.
     *
     * @param word specifies if the value pushed is a Word value
     */
    public void push1(boolean word) {
        stack[sp] = word;
        sp++;
    }

    /**
     * Interprets a push of a single-slot, non-Word value to the stack.
     */
    public void push1() {
        stack[sp++] = false;
    }

    /**
     * Interprets a push of a double-slot value to the stack. By definition, the value
     * cannot be a Word value as Word values have the same slot size as Object values
     * which is one slot.
     */
    public void push2() {
        stack[sp++] = false;
        stack[sp++] = false;
    }

    private static final PrintStream debugOut = System.out;

    /**
     * Records a bytecode modification.
     */
    private void recordChange() {
        if (DEBUG) {
            debugOut.println("     ---> " + nameOf(inCode[opcodeBci] & 0xff));
        }
        changed = true;
    }

    /**
     * Interprets a load of a local variable onto the stack.
     *
     * @param index the local variable index
     * @param wordOpcode if the local variable is a Word value, then the load instruction is replaced with this opcode
     */
    public void loadRefOrWord(int index, int wordOpcode) {
        boolean isWord = locals[index];
        stack[sp++] = isWord;
        if (isWord) {
            outCode[opcodeBci] = (byte) wordOpcode;
            recordChange();
        }
    }

    /**
     * Interprets a load of a single-slot, non-Word local variable onto the stack.
     *
     * @param index the local variable index
     */
    public void load1(int index) {
        stack[sp++] = locals[index];
    }

    /**
     * Interprets a load of a double-slot, non-Word local variable onto the stack.
     *
     * @param index the local variable index
     */
    public void load2(int index) {
        stack[sp] = locals[index];
        stack[sp + 1] = locals[index + 1];
        sp += 2;
    }

    /**
     * Interprets a store to a local variable by popping the top value off the stack.
     *
     * @param index the local variable index
     * @param wordOpcode if the local variable is a Word value, then the store instruction is replaced with this opcode
     */
    public void storeRefOrWord(int index, int wordOpcode) {
        boolean isWord = stack[--sp];
        locals[index] = isWord;
        if (isWord) {
            inCode[opcodeBci] = (byte) wordOpcode;
            recordChange();
        }
    }

    /**
     * Interprets a store to a single-slot, non-Word local variable by popping the top value off the stack.
     *
     * @param index the local variable index
     */
    public void store1(int index) {
        locals[index] = stack[--sp];
    }

    /**
     * Interprets a store to a double-slot, non-Word local variable by popping the top value off the stack.
     *
     * @param index the local variable index
     */
    public void store2(int index) {
        locals[index] = stack[sp - 2];
        locals[index + 1] = stack[sp - 1];
        sp -= 2;
    }

    /**
     * Pops a single-slot value off the stack.
     *
     * @return whether the popped value was a Word
     */
    public boolean pop1() {
        return stack[--sp];
    }

    /**
     * Pops n-slots off the stack.
     *
     * @param amount the number of slots to pop
     */
    public void pop(int amount) {
        sp -= amount;
        assert sp >= 0;
    }

    /**
     * Determines if the top value on the stack is a Word.
     */
    public boolean top() {
        return stack[sp - 1];
    }

    /**
     * Reads an unsigned byte from the code stream.
     *
     * @return the unsigned byte value read
     */
    private int readU1() {
        return inCode[bci++] & 0xff;
    }

    /**
     * Reads an unsigned 2-byte value from the code stream.
     *
     * @return the unsigned 2-byte value read
     */
    private int readU2() {
        final int result = Bytes.beU2(inCode, bci);
        bci += 2;
        return result;
    }

    /**
     * Reads an unsigned 1 or 2-byte value from the code stream.
     *
     * @param wide if {@code true}, a 2 byte value is read
     * @return the unsigned 1 or 2 byte value read
     */
    private int readVarIndex(boolean wide) {
        if (wide) {
            return readU2();
        }
        return readU1();
    }

    /**
     * Reads a signed 2-byte value from the code stream.
     *
     * @return the signed 2-byte value read
     */
    private int readS2() {
        final int result = Bytes.beS2(inCode, bci);
        bci += 2;
        return result;
    }

    /**
     * Reads a signed 4-byte value from the code stream.
     *
     * @return the signed 4-byte value read
     */
    private int readS4() {
        final int result = Bytes.beS4(inCode, bci);
        bci += 4;
        return result;
    }

    private void align4() {
        bci = (bci + 3) & ~0x3;
    }

    /**
     * Notifies this object that the current {@code INVOKE...} instruction is being replaced.
     *
     * @param opcode the replacement opcode
     * @param operand the replacement 2-byte operand
     */
    public void intrinsify(int opcode, int operand) {
        int oldOpcode = inCode[opcodeBci] & 0xff;
        assert oldOpcode >= INVOKEVIRTUAL && oldOpcode <= INVOKEINTERFACE;
        assert operand >> 16 == 0 || operand >> 16 == -1 : "operand value cannot be encoded in 16 bits: " + operand;
        outCode[opcodeBci] = (byte) opcode;
        outCode[opcodeBci + 1] = (byte) (operand >> 8);
        outCode[opcodeBci + 2] = (byte) (operand >> 0);
        recordChange();
    }

    /**
     * Performs abstract interpretation of the code with respect to the flow of Word typed values.
     * This interpretation only parses each bytecode instruction at most once (unreachable code
     * is never processed) by following control flow edges. That is, a basic block is not
     * interpreted unless it is reachable along a control flow edge from another basic block
     * that is (transitively) reachable from the entry basic block.
     *
     * It is assumed that the input code verifies. The means that all uses of a variable
     * will match with a definition along all paths to the use and that the type (is a word
     * or not) will be the same along each path.
     *
     * @return {@code true} if one or more instructions were modified
     */
    public boolean run() {
        try {
            if (DEBUG) {
                debugOut.println(method);
            }
            parseBlocks();
            while (todoHandler != null) {
                todo = todoHandler;
                todoHandler = null;
                parseBlocks();
            }
            return changed;
        } catch (Throwable e) {
            int opcode = inCode[opcodeBci] & 0xff;
            Frame errorFrame = new Frame(opcodeBci, sp, stack, locals, null);
            throw (InternalError) new InternalError("Error while preprocessing " + method + " {" + nameOf(opcode) + " @ " + errorFrame + "}").initCause(e);
        }
    }

    /**
     * Parses and processes all the basic blocks on the {@linkplain #todo to do} list.
     */
    public void parseBlocks() {
        while (todo != null) {
            if (DEBUG) {
                debugOut.println("  TODO: " + todoAsString());
            }

            Frame head = todo;
            todo = head.next;
            assert !head.visited;

            sp = head.get(stack, locals);
            head.visited = true;
            this.bci = head.bci;
            parseBlock();
            if (DEBUG) {
                debugOut.println("  ==============");
            }

        }
    }

    /**
     * Parses and processes a single basic block of bytecode.
     *
     * @param ci the entry point to the block
     */
    private void parseBlock() {
        while (true) {
            opcodeBci = bci;

            int opcode = readU1();

            int length = lengthOf(opcode);
            int nextBci = bci + length - 1;
            boolean wide;
            if (opcode == WIDE) {
                opcode = readU1();
                wide = true;
                if (opcode == IINC) {
                    nextBci = opcodeBci + 6;
                } else {
                    // ?LOAD, ?STORE, or RET
                    nextBci = opcodeBci + 4;
                }
            } else {
                wide = false;
            }

            if (DEBUG) {
                debugOut.println("  sp=" + sp + "\t" + opcodeBci + ": " + nameOf(opcode));
                assert nameOf(opcode) != null : opcode;
            }

            switch (opcode) {
                // Checkstyle: stop

                case NOP          :
                case IINC         :
                case GOTO         :
                case RET          :
                case RETURN       :
                case WIDE         :
                case GOTO_W       :
                case JSR_W        :
                case BREAKPOINT   :
                case LALOAD       :
                case DALOAD       :
                case LNEG         :
                case DNEG         :
                case L2D          :
                case D2L          :
                case INEG         :
                case FNEG         :
                case I2F          :
                case F2I          :
                case I2B          :
                case I2C          :
                case I2S          :
                case NEWARRAY     :
                case ANEWARRAY    :
                case ARRAYLENGTH  :
                case CHECKCAST    :
                case INSTANCEOF   : break;
                case ACONST_NULL  :
                case ICONST_M1    :
                case ICONST_0     :
                case ICONST_1     :
                case ICONST_2     :
                case ICONST_3     :
                case ICONST_4     :
                case ICONST_5     :
                case FCONST_0     :
                case FCONST_1     :
                case FCONST_2     :
                case BIPUSH       :
                case SIPUSH       :
                case LDC          :
                case LDC_W        :
                case JSR          :
                case NEW          :
                case I2L          :
                case I2D          :
                case F2L          :
                case F2D          : push1(); break;
                case LDC2_W       :
                case LCONST_0     :
                case LCONST_1     :
                case DCONST_0     :
                case DCONST_1     : push2(); break;
                case POP          :
                case IFEQ         :
                case IFNE         :
                case IFLT         :
                case IFGE         :
                case IFGT         :
                case IFLE         :
                case TABLESWITCH  :
                case LOOKUPSWITCH :
                case IRETURN      :
                case FRETURN      :
                case ATHROW       :
                case MONITORENTER :
                case MONITOREXIT  :
                case IFNULL       :
                case IFNONNULL    :
                case IALOAD       :
                case FALOAD       :
                case AALOAD       :
                case BALOAD       :
                case CALOAD       :
                case SALOAD       :
                case IADD         :
                case FADD         :
                case ISUB         :
                case FSUB         :
                case IMUL         :
                case FMUL         :
                case IDIV         :
                case FDIV         :
                case IREM         :
                case FREM         :
                case ISHL         :
                case ISHR         :
                case IUSHR        :
                case LSHL         :
                case LSHR         :
                case LUSHR        :
                case IAND         :
                case IOR          :
                case IXOR         :
                case L2I          :
                case L2F          :
                case D2I          :
                case D2F          :
                case FCMPL        :
                case FCMPG        : pop1(); break;
                case POP2         :
                case IF_ICMPEQ    :
                case IF_ICMPNE    :
                case IF_ICMPLT    :
                case IF_ICMPGE    :
                case IF_ICMPGT    :
                case IF_ICMPLE    :
                case IF_ACMPEQ    :
                case IF_ACMPNE    :
                case DRETURN      :
                case LADD         :
                case DADD         :
                case LSUB         :
                case DSUB         :
                case LMUL         :
                case DMUL         :
                case LDIV         :
                case DDIV         :
                case LRETURN      :
                case LREM         :
                case DREM         :
                case LAND         :
                case LOR          :
                case LXOR         : pop(2); break;
                case ILOAD        :
                case FLOAD        : load1(readVarIndex(wide)); break;
                case ILOAD_0      :
                case FLOAD_0      : load1(0); break;
                case ILOAD_1      :
                case FLOAD_1      : load1(1); break;
                case ILOAD_2      :
                case FLOAD_2      : load1(2); break;
                case ILOAD_3      :
                case FLOAD_3      : load1(3); break;
                case LLOAD        :
                case DLOAD        : load2(readVarIndex(wide)); break;
                case LLOAD_0      :
                case DLOAD_0      : load2(0); break;
                case LLOAD_1      :
                case DLOAD_1      : load2(1); break;
                case LLOAD_2      :
                case DLOAD_2      : load2(2); break;
                case LLOAD_3      :
                case DLOAD_3      : load2(3); break;
                case ISTORE       :
                case FSTORE       : store1(readVarIndex(wide)); break;
                case ISTORE_0     :
                case FSTORE_0     : store1(0); break;
                case ISTORE_1     :
                case FSTORE_1     : store1(1); break;
                case ISTORE_2     :
                case FSTORE_2     : store1(2); break;
                case ISTORE_3     :
                case FSTORE_3     : store1(3); break;
                case LSTORE       :
                case DSTORE       : store2(readVarIndex(wide)); break;
                case LSTORE_0     :
                case DSTORE_0     : store2(0); break;
                case LSTORE_1     :
                case DSTORE_1     : store2(1); break;
                case LSTORE_2     :
                case DSTORE_2     : store2(2); break;
                case LSTORE_3     :
                case DSTORE_3     : store2(3); break;
                case IASTORE      :
                case FASTORE      :
                case AASTORE      :
                case BASTORE      :
                case CASTORE      :
                case SASTORE      :
                case LCMP         :
                case DCMPL        :
                case DCMPG        : pop(3); break;
                case LASTORE      :
                case DASTORE      : pop(4); break;
                case DUP:
                    push1(top());
                    break;
                case DUP_X1: {
                    boolean value1 = pop1();
                    boolean value2 = pop1();
                    push1(value1);
                    push1(value2);
                    push1(value1);
                    break;
                }
                case DUP_X2: {
                    boolean value1 = pop1();
                    boolean value2 = pop1();
                    boolean value3 = pop1();
                    push1(value1);
                    push1(value3);
                    push1(value2);
                    push1(value1);
                    break;
                }
                case DUP2: {
                    boolean value1 = pop1();
                    boolean value2 = pop1();
                    push1(value2);
                    push1(value1);
                    push1(value2);
                    push1(value1);
                    break;
                }
                case DUP2_X1: {
                    boolean value1 = pop1();
                    boolean value2 = pop1();
                    boolean value3 = pop1();
                    push1(value2);
                    push1(value1);
                    push1(value3);
                    push1(value2);
                    push1(value1);
                    break;
                }

                case DUP2_X2: {
                    boolean value1 = pop1();
                    boolean value2 = pop1();
                    boolean value3 = pop1();
                    boolean value4 = pop1();
                    push1(value2);
                    push1(value1);
                    push1(value4);
                    push1(value3);
                    push1(value2);
                    push1(value1);
                    break;
                }

                case SWAP: {
                    boolean value1 = pop1();
                    boolean value2 = pop1();
                    push1(value1);
                    push1(value2);
                    break;
                }

                case MULTIANEWARRAY: {
                    readU2();
                    int dimensions = readU1();
                    pop(dimensions);
                    push1();
                    break;
                }

                case ARETURN: {
                    if (pop1()) {
                        outCode[opcodeBci] = (byte) WRETURN;
                        recordChange();
                    }
                    break;
                }

                case ALOAD:     loadRefOrWord(readVarIndex(wide), WLOAD); break;
                case ALOAD_0:   loadRefOrWord(0, WLOAD_0); break;
                case ALOAD_1:   loadRefOrWord(1, WLOAD_1); break;
                case ALOAD_2:   loadRefOrWord(2, WLOAD_2); break;
                case ALOAD_3:   loadRefOrWord(3, WLOAD_3); break;

                case ASTORE:    storeRefOrWord(readVarIndex(wide), WSTORE); break;
                case ASTORE_0:  storeRefOrWord(0, WSTORE_0); break;
                case ASTORE_1:  storeRefOrWord(1, WSTORE_1); break;
                case ASTORE_2:  storeRefOrWord(2, WSTORE_2); break;
                case ASTORE_3:  storeRefOrWord(3, WSTORE_3); break;

                case INVOKEVIRTUAL:   intrinsifier.invokevirtual(this, readU2());   break;
                case INVOKESPECIAL:   intrinsifier.invokespecial(this, readU2());   break;
                case INVOKESTATIC:    intrinsifier.invokestatic(this, readU2());    break;
                case INVOKEINTERFACE: intrinsifier.invokeinterface(this, readU2()); break;

                case GETFIELD:        intrinsifier.getfield(this, readU2());        break;
                case PUTFIELD:        intrinsifier.putfield(this, readU2());        break;
                case GETSTATIC:       intrinsifier.getstatic(this, readU2());       break;
                case PUTSTATIC:       intrinsifier.putstatic(this, readU2());       break;

                case JNICALL:         intrinsifier.jnicall(this, readU2());         break;

                default:
                    throw new InternalError("unexpected opcode " + opcode + " [" + nameOf(opcode) + "]");
            }

            // Process exception handlers (if any)
            if (handlerMap != null && canTrap(opcode)) {
                for (Handler handler = handlerMap[opcodeBci]; handler != null; handler = handler.next) {
                    Frame frame = handler.frame;
                    if (frame == null) {
                        frame = frameMap[handler.bci];
                        if (frame == null) {
                            // It's only necessary to initialize the handler frame state
                            // from one control flow in-edge. Given the precondition
                            // about verified code, all variables used within a
                            // handler must have been defined along *all* in-edges.
                            frame = new Frame(handler.bci, 1, new boolean[stack.length], locals.clone(), todoHandler);
                            frameMap[handler.bci] = frame;
                            todoHandler = frame;
                        }
                        handler.frame = frame;
                    } else {
                        assert frameMap[handler.bci] == frame;
                    }
                }
            }

            if (isBlockEnd(opcode) || frameMap[nextBci] != null) {
                if (isBranch(opcode)) {
                    if (length == 3) {
                        merge(opcodeBci + readS2());
                    } else {
                        assert length == 5;
                        merge(opcodeBci + readS4());
                    }
                    if (isConditionalBranch(opcode)) {
                        merge(opcodeBci + length);
                    }
                } else if (opcode == TABLESWITCH) {
                    align4();
                    final int defaultOffset = readS4();
                    final int lowMatch = readS4();
                    final int highMatch = readS4();
                    final int numberOfCases = highMatch - lowMatch + 1;
                    merge(opcodeBci + defaultOffset);
                    for (int i = 0; i < numberOfCases; i++) {
                        merge(opcodeBci + readS4());
                    }
                } else if (opcode == LOOKUPSWITCH) {
                    align4();
                    final int defaultOffset = readS4();
                    final int numberOfCases = readS4();
                    merge(opcodeBci + defaultOffset);
                    for (int i = 0; i < numberOfCases; i++) {
                        readS4();
                        merge(opcodeBci + readS4());
                    }
                }

                return;
            }

            // Mark the next instruction as visited (it may be an as yet unknown control flow merge point)
            frameMap[nextBci] = VISITED;

            bci = nextBci;
        }
    }

    private void merge(int bci) {
        Frame frame = frameMap[bci];
        if (frame == null) {
            frame = new Frame(bci, sp, stack.clone(), locals.clone(), todo);
            frameMap[bci] = frame;
            todo = frame;
        }

        if (!frame.visited) {
            assert isTodo(frame) : frame;
        }
    }

    // DEBUG support
    private boolean isTodo(Frame frame) {
        for (Frame f = todo; f != null; f = f.next) {
            if (f == frame) {
                return true;
            }
        }
        return false;

    }

    // DEBUG support
    private String todoAsString() {
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        for (Frame f = todo; f != null; f = f.next) {
            if (sb.length() != 1) {
                sb.append(", ");
            }
            sb.append(f.bci);
        }
        return sb.append(']').toString();
    }
}
