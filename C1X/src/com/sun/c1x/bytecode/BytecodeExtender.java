/*
 * Copyright (c) 2010 Sun Microsystems, Inc.  All rights reserved.
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
package com.sun.c1x.bytecode;

import static com.sun.c1x.bytecode.Bytecodes.*;

/**
 * A utility to process a bytecode stream to replace certain standard
 * JVM instructions with extended bytecode instructions. The extended
 * instructions are for:
 *
 * 1. Supporting a Word type hierarchy that is separate from the Object type hierarchy.
 * 2. Supporting low-level, non-Java operations such as reading/writing registers.
 * 3. Other builtins necessary for writing a meta-circular VM.
 *
 * The transformations made by this utility assume the input code has been verified
 * (or would successfully pass verification).
 *
 * @author Doug Simon
 */
public final class BytecodeExtender {

    private static final boolean DEBUG = false;

    public abstract static class Intrinsifier {
        public abstract void invokevirtual(BytecodeExtender bce, int cpi);
        public abstract void invokestatic(BytecodeExtender bce, int cpi);
        public abstract void invokespecial(BytecodeExtender bce, int cpi);
        public abstract void invokeinterface(BytecodeExtender bce, int cpi);
        public abstract void getstatic(BytecodeExtender bce, int cpi);
        public abstract void putstatic(BytecodeExtender bce, int cpi);
        public abstract void getfield(BytecodeExtender bce, int cpi);
        public abstract void putfield(BytecodeExtender bce, int cpi);
        public abstract void jnicall(BytecodeExtender bce, int cpi);
    }

    static class Frame {
        final int sp;
        final boolean[] locals;
        final boolean[] stack;
        boolean visited;
        final Frame next;
        final int bci;

        Frame(int bci, int sp, boolean[] stack, boolean[] locals, Frame next) {
            this.bci = bci;
            this.sp = sp;
            this.stack = stack;
            this.locals = locals;
            this.next = next;
        }

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

        boolean matches(int sp, boolean[] stack, boolean[] locals) {
            for (int i = 0; i < sp; ++i) {
                if (stack[i] != this.stack[i]) {
                    return false;
                }
            }
            for (int i = 0; i < locals.length; ++i) {
                if (locals[i] != this.locals[i]) {
                    return false;
                }
            }
            return true;
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

    public int[] extra;

    Frame todo;
    Frame todoHandler;

    /**
     * Sentinel value used in {@link #frameMap} to denote that a given instruction (bci) has been processed.
     * This required as without a pre-pass to establish basic block boundaries, it's possible
     * for {@linkplain #parseBlock(int) block parsing} to cross a block boundary at a
     * (as yet unknown) control flow merge point. There's no need to re-process the instructions
     * after the merge point when the other branch to the merge point is subsequently discovered.
     */
    private static final Frame VISITED = new Frame(-1, 0, null, null, null);
    static {
        VISITED.visited = true;
    }

    /**
     * A map from each bci to a {@link Frame} object. A entry equal to {@link #VISITED} in this map for a given bci
     * indicates that the instruction at bci has already be processed. A {@code null} entry means that
     * the instruction has not yet been processes and not a known basic block entry point. A non-null
     * entry not equals to {@link #VISITED} indicates a known basic block entry point and the value of
     * the {@link Frame} object indicates the frame state to be used when parsing the block.
     */
    private final Frame[] frameMap;

    private final Handler[] handlerMap;

    public int opcodePos;
    public final Object method;
    public final Intrinsifier intrinsifier;

    /**
     * Indicates if one or more instructions were replaced with an extended instruction.
     */
    private boolean changed;

    private final byte[] code;

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
     *
     * @param intrinsifier
     * @param method
     * @param code
     * @param handlers the exception handler table as an array of triplets (start bci, end bci, handler bci) or {@code
     *            null} if the method has not exception handlers
     * @param maxStack
     * @param locals
     */
    public BytecodeExtender(Intrinsifier intrinsifier, Object method, byte[] code, int[] handlers, int maxStack, boolean[] locals) {
        this.intrinsifier = intrinsifier;
        this.stack = new boolean[maxStack];
        this.locals = new boolean[locals.length];
        this.code = code;
        this.method = method;
        this.frameMap = new Frame[code.length];
        this.handlerMap = initHandlerMap(code, handlers, maxStack, locals);

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

    public void push1(boolean word) {
        stack[sp] = word;
        sp++;
    }

    public void push1() {
        stack[sp++] = false;
    }

    public void push2() {
        stack[sp++] = false;
        stack[sp++] = false;
    }

    private void recordChange() {
        if (DEBUG) {
            System.out.println("     ---> " + name(code[opcodePos]));
        }
        changed = true;
    }

    public void loadRefOrWord(int index, int wordOpcode) {
        boolean isWord = locals[index];
        stack[sp++] = isWord;
        if (isWord) {
            code[opcodePos] = (byte) wordOpcode;
            recordChange();
        }
    }

    public void load1(int index) {
        stack[sp++] = locals[index];
    }

    public void load2(int index) {
        stack[sp] = locals[index];
        stack[sp + 1] = locals[index + 1];
        sp += 2;
    }

    public void storeRefOrWord(int index, int wordOpcode) {
        boolean isWord = stack[--sp];
        locals[index] = isWord;
        if (isWord) {
            code[opcodePos] = (byte) wordOpcode;
            recordChange();
        }
    }

    public void store1(int index) {
        locals[index] = stack[--sp];
    }

    public void store2(int index) {
        locals[index] = stack[sp - 2];
        locals[index + 1] = stack[sp - 1];
        sp -= 2;
    }

    public boolean pop1() {
        return stack[--sp];
    }

    public void pop(int amount) {
        sp -= amount;
        assert sp >= 0;
    }

    public boolean top() {
        return stack[sp - 1];
    }

    public boolean lastTop() {
        return stack[sp];
    }

    public boolean local(int index) {
        return locals[index];
    }

    private int readU2(int bci) {
        return (code[bci] & 0xff) << 8 | code[bci + 1] & 0xff;
    }

    private int readU1(int bci, boolean wide) {
        if (wide) {
            return code[bci] & 0xff << 8 | code[bci + 1] & 0xff;
        }
        return code[bci] & 0xff;
    }

    private int readS2(int bci) {
        return code[bci] << 8 | code[bci + 1] & 0xff;
    }

    private int readS4(int bci) {
        return code[bci + 0]          << 24 |
               (code[bci + 1] & 0xff) << 16 |
               (code[bci + 2] & 0xff) << 8  |
               (code[bci + 3] & 0xff);
    }

    private int align4(int bci) {
        final int remainder = bci & 0x3;
        if (remainder != 0) {
            return bci + 4 - remainder;
        }
        return bci;
    }

    public void intrinsify(int opcode, int operand) {
        int oldOpcode = code[opcodePos] & 0xff;
        assert oldOpcode >= INVOKEVIRTUAL && oldOpcode <= INVOKEINTERFACE;
        code[opcodePos] = (byte) opcode;
        code[opcodePos + 1] = (byte) (operand >> 8);
        code[opcodePos + 2] = (byte) (operand >> 0);
        recordChange();
    }

    public boolean run() {
        try {
            if (DEBUG) {
                System.out.println(method);
            }
            parseBlocks();
            while (todoHandler != null) {
                todo = todoHandler;
                todoHandler = null;
                parseBlocks();
            }
            return changed;
        } catch (Throwable e) {
            int opcode = code[opcodePos] & 0xff;
            Frame errorFrame = new Frame(opcodePos, sp, stack, locals, null);
            throw (InternalError) new InternalError("Error while preprocessing " + method + " {" + name(opcode) + " @ " + errorFrame + "}").initCause(e);
        }
    }

    public void parseBlocks() {
        while (todo != null) {
            if (DEBUG) {
                System.out.println("  TODO: " + todoAsString());
            }

            Frame head = todo;
            todo = head.next;
            assert !head.visited;

            sp = head.get(stack, locals);
            head.visited = true;
            parseBlock(head.bci);
            if (DEBUG) {
                System.out.println("  ==============");
            }

        }
    }

    private void parseBlock(int bci) {
        byte[] code = this.code;

        while (true) {
            opcodePos = bci;

            int opcode = code[bci++] & 0xff;

            int length = length(opcode);
            int nextBci = bci + length - 1;
            boolean wide;
            if (opcode == WIDE) {
                opcode = code[bci++] & 0xff;
                wide = true;
                if (opcode == IINC) {
                    nextBci = opcodePos + 6;
                } else {
                    // ?LOAD, ?STORE, or RET
                    nextBci = opcodePos + 4;
                }
            } else {
                wide = false;
            }

            if (DEBUG) {
                System.out.println("  sp=" + sp + "\t" + opcodePos + ": " + name(opcode));
                assert name(opcode) != null : opcode;
            }

            switch (opcode) {
                // Checkstyle: stop

                // NO_EFFECT
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
                // POP1_PUSH1
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

                // PUSH1
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
                // POP1_PUSH2
                case I2L          :
                case I2D          :
                case F2L          :
                case F2D          : push1(); break;

                // PUSH2
                case LDC2_W       :
                case LCONST_0     :
                case LCONST_1     :
                case DCONST_0     :
                case DCONST_1     : push2(); break;

                // POP1
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

                // POP2_
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


                // LOAD1
                case ILOAD        :
                case FLOAD        : load1(readU1(bci, wide)); break;

                // LOAD1_0
                case ILOAD_0      :
                case FLOAD_0      : load1(0); break;

                // LOAD1_1
                case ILOAD_1      :
                case FLOAD_1      : load1(1); break;

                // LOAD1_2
                case ILOAD_2      :
                case FLOAD_2      : load1(2); break;

                // LOAD1_3
                case ILOAD_3      :
                case FLOAD_3      : load1(3); break;

                // LOAD2
                case LLOAD        :
                case DLOAD        : load2(readU1(bci, wide)); break;

                // LOAD2_0
                case LLOAD_0      :
                case DLOAD_0      : load2(0); break;

                // LOAD2_1
                case LLOAD_1      :
                case DLOAD_1      : load2(1); break;

                // LOAD2_2
                case LLOAD_2      :
                case DLOAD_2      : load2(2); break;

                // LOAD2_3
                case LLOAD_3      :
                case DLOAD_3      : load2(3); break;

                // STORE1
                case ISTORE       :
                case FSTORE       : store1(readU1(bci, wide)); break;

                // STORE1_0
                case ISTORE_0     :
                case FSTORE_0     : store1(0); break;

                // STORE1_1
                case ISTORE_1     :
                case FSTORE_1     : store1(0); break;

                // STORE1_2
                case ISTORE_2     :
                case FSTORE_2     : store1(0); break;

                // STORE1_3
                case ISTORE_3     :
                case FSTORE_3     : store1(0); break;

                // STORE2
                case LSTORE       :
                case DSTORE       : store2(readU1(bci, wide)); break;

                // STORE2_0
                case LSTORE_0     :
                case DSTORE_0     : store2(0); break;

                // STORE2_1
                case LSTORE_1     :
                case DSTORE_1     : store2(1); break;

                // STORE2_2
                case LSTORE_2     :
                case DSTORE_2     : store2(2); break;

                // STORE2_3
                case LSTORE_3     :
                case DSTORE_3     : store2(3); break;


                // POP3
                case IASTORE      :
                case FASTORE      :
                case AASTORE      :
                case BASTORE      :
                case CASTORE      :
                case SASTORE      :
                // POP4_PUSH1
                case LCMP         :
                case DCMPL        :
                case DCMPG        : pop(3); break;

                // POP4
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
                    int dimensions = code[bci + 2] & 0xff;
                    pop(dimensions);
                    push1();
                    break;
                }

                case ARETURN: {
                    if (pop1()) {
                        code[opcodePos] = (byte) WRETURN;
                        changed = true;
                        if (DEBUG) {
                            System.out.println("     ---> wreturn");
                        }
                    }
                    break;
                }

                case ALOAD:     loadRefOrWord(readU1(bci, wide), WLOAD); break;
                case ALOAD_0:   loadRefOrWord(0, WLOAD_0); break;
                case ALOAD_1:   loadRefOrWord(0, WLOAD_1); break;
                case ALOAD_2:   loadRefOrWord(0, WLOAD_2); break;
                case ALOAD_3:   loadRefOrWord(0, WLOAD_3); break;

                case ASTORE:    storeRefOrWord(readU1(bci, wide), WSTORE); break;
                case ASTORE_0:  storeRefOrWord(0, WSTORE_0); break;
                case ASTORE_1:  storeRefOrWord(0, WSTORE_1); break;
                case ASTORE_2:  storeRefOrWord(0, WSTORE_2); break;
                case ASTORE_3:  storeRefOrWord(0, WSTORE_3); break;

                case INVOKEVIRTUAL:   intrinsifier.invokevirtual(this, readU2(bci));   break;
                case INVOKESPECIAL:   intrinsifier.invokespecial(this, readU2(bci));   break;
                case INVOKESTATIC:    intrinsifier.invokestatic(this, readU2(bci));    break;
                case INVOKEINTERFACE: intrinsifier.invokeinterface(this, readU2(bci)); break;

                case GETFIELD:        intrinsifier.getfield(this, readU2(bci));        break;
                case PUTFIELD:        intrinsifier.putfield(this, readU2(bci));        break;
                case GETSTATIC:       intrinsifier.getstatic(this, readU2(bci));       break;
                case PUTSTATIC:       intrinsifier.putstatic(this, readU2(bci));       break;

                case JNICALL:         intrinsifier.jnicall(this, readU2(bci));         break;

                default:
                    throw new InternalError("unexpected opcode " + opcode + " [" + name(opcode) + "]");
            }

            if (handlerMap != null && canTrap(opcode)) {
                for (Handler handler = handlerMap[opcodePos]; handler != null; handler = handler.next) {
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
                        merge(opcodePos + readS2(bci));
                    } else {
                        assert length == 5;
                        merge(opcodePos + readS4(bci));
                    }
                    if (isConditionalBranch(opcode)) {
                        merge(opcodePos + length);
                    }
                } else if (opcode == TABLESWITCH) {
                    bci = align4(bci);
                    final int defaultOffset = readS4(bci);
                    bci += 4;
                    final int lowMatch = readS4(bci);
                    bci += 4;
                    final int highMatch = readS4(bci);
                    bci += 4;
                    final int numberOfCases = highMatch - lowMatch + 1;
                    merge(opcodePos + defaultOffset);
                    for (int i = 0; i < numberOfCases; i++) {
                        merge(opcodePos + readS4(bci));
                        bci += 4;
                    }
                } else if (opcode == LOOKUPSWITCH) {
                    bci = align4(bci);
                    final int defaultOffset = readS4(bci);
                    bci += 4;
                    final int numberOfCases = readS4(bci);
                    bci += 4;
                    merge(opcodePos + defaultOffset);
                    for (int i = 0; i < numberOfCases; i++) {
                        readS4(bci);
                        bci += 4;
                        merge(opcodePos + readS4(bci));
                        bci += 4;
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

    private boolean isTodo(Frame frame) {
        for (Frame f = todo; f != null; f = f.next) {
            if (f == frame) {
                return true;
            }
        }
        return false;

    }

    private void addTodo(Frame frame) {

    }

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
