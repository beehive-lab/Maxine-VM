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
package com.sun.max.vm.tele;

import java.io.*;

import com.sun.max.collect.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.actor.member.MethodKey.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.jit.*;
import com.sun.max.vm.type.*;

/**
 * @author Bernd Mathiske
 */
public final class BytecodeBreakpointMessage extends MaxineMessage<BytecodeBreakpointMessage> {

    public enum Action {
        MAKE, DELETE;

        public static final IndexedSequence<Action> VALUES = new ArraySequence<Action>(values());
    }

    private Action action;
    private MethodKey methodKey;

    private int bytecodePosition;

    public BytecodeBreakpointMessage() {
        super(Tag.BYTECODE_BREAKPOINT);
    }

    public static void writeMethodKey(DataOutputStream dataOutputStream, MethodKey methodKey) throws IOException {
        dataOutputStream.writeUTF(methodKey.holder().string);
        dataOutputStream.writeUTF(methodKey.name().string);
        dataOutputStream.writeUTF(methodKey.signature().string);
    }

    public static MethodKey readMethodKey(DataInputStream dataInputStream) throws IOException {
        final TypeDescriptor holder = JavaTypeDescriptor.parseTypeDescriptor(dataInputStream.readUTF());
        final Utf8Constant name = PoolConstantFactory.makeUtf8Constant(dataInputStream.readUTF());
        final SignatureDescriptor signature = SignatureDescriptor.create(dataInputStream.readUTF());
        return new DefaultMethodKey(holder, name, signature);
    }

    @Override
    public void readData(DataInputStream dataInputStream) throws IOException {
        action = Action.VALUES.get(dataInputStream.readInt());
        methodKey = readMethodKey(dataInputStream);
        bytecodePosition = dataInputStream.readInt();
    }

    /**
     * @param bytecodePosition < 0 selects breakpoint at homogeneous method entry point
     */
    public BytecodeBreakpointMessage(Action action, MethodKey methodKey, int bytecodePosition) {
        super(Tag.BYTECODE_BREAKPOINT);
        this.action = action;
        this.methodKey = methodKey;
        this.bytecodePosition = bytecodePosition;
    }

    @Override
    public void writeData(DataOutputStream dataOutputStream) throws IOException {
        dataOutputStream.writeInt(action.ordinal());
        writeMethodKey(dataOutputStream, methodKey);
        dataOutputStream.writeInt(bytecodePosition);
    }

    private static final Bag<MethodKey, Integer, Sequence<Integer>> signatureToBytecodePositions = new SequenceBag<MethodKey, Integer>(SequenceBag.MapType.HASHED);

    public static boolean isActiveFor(ClassMethodActor classMethodActor) {
        final MethodKey methodKey = new MethodActorKey(classMethodActor);
        return !signatureToBytecodePositions.get(methodKey).isEmpty();
    }

    static {
        MaxineMessenger.subscribe(MaxineMessage.Tag.BYTECODE_BREAKPOINT, new MaxineMessage.Receiver<BytecodeBreakpointMessage>() {
            public void consume(BytecodeBreakpointMessage message) {
                switch (message.action) {
                    case MAKE:
                        signatureToBytecodePositions.add(message.methodKey, message.bytecodePosition);
                        break;
                    case DELETE:
                        signatureToBytecodePositions.remove(message.methodKey, message.bytecodePosition);
                        break;
                }
            }
        });
    }

    /**
     * After compiling a method, find out whether byte code breakpoints have been requested and set them if so.
     */
    public static void makeTargetBreakpoints(TargetMethod targetMethod) {
        MaxineMessenger.messenger().flush();
        final MethodKey methodKey = new MethodActorKey(targetMethod.classMethodActor());
        final Sequence<Integer> bytecodePositions = signatureToBytecodePositions.get(methodKey);
        if (!bytecodePositions.isEmpty()) {
            if (targetMethod instanceof JitTargetMethod) {
                final JitTargetMethod jitTargetMethod = (JitTargetMethod) targetMethod;
                for (Integer bytecodePosition : bytecodePositions) {
                    TargetBreakpoint.make(jitTargetMethod.codeStart().plus(jitTargetMethod.targetCodePositionFor(bytecodePosition)));
                }
            } else {
                // TODO: trigger deoptimization
            }
        }
    }
}
