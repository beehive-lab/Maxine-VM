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
package com.sun.max.tele.interpreter;

import java.util.*;

import com.sun.max.program.*;
import com.sun.max.tele.*;
import com.sun.max.tele.interpreter.ExecutionFrame.*;
import com.sun.max.tele.object.*;
import com.sun.max.tele.reference.*;
import com.sun.max.tele.value.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.bytecode.*;
import com.sun.max.vm.classfile.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.layout.*;
import com.sun.max.vm.prototype.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;

/**
 * The Interpreter's interface to the VM.  Encapsulates all the state of the VM.
 * Can run without VM for testing.
 *
 * @author Athul Acharya
 */
public final class Machine {

    private TeleVM _teleVM;
    private ExecutionThread _currentThread;


    Machine(TeleVM teleVM) {
        final ExecutionThread mainThread = newThread(java.lang.Thread.NORM_PRIORITY, ExecutionThread.ThreadType.NORMAL_THREAD);
        //JavaThreads.initialize(mainThread);
        _teleVM = teleVM;
        activate(mainThread);
    }


    public ExecutionFrame pushFrame(ClassMethodActor method) {
        return _currentThread.pushFrame(method);
    }

    public ExecutionFrame popFrame() {
        return _currentThread.popFrame();
    }

    public void activate(ExecutionThread thread) {
        //active_threads.insertElementAt(this_thread, 0);
        _currentThread = thread;
    }

    public ExecutionThread newThread(int prio, ExecutionThread.ThreadType threadType) {
        return new ExecutionThread(prio, threadType);
    }

    public MethodActor currentMethod() {
        return _currentThread.frame().method();
    }

    public void incrementBCP(int amount) {
        _currentThread.frame().incrementBCP(amount);
    }

    public void setBCP(int bcp) {
        _currentThread.frame().setBCP(bcp);
    }

    public int getBCP() {
        return _currentThread.frame().bcp();
    }

    public Bytecode nextInstruction() {
        final ExecutionFrame currentFrame = _currentThread.frame();
        int bcp;
        byte[] code;

        currentFrame.incrementBCP(1);
        bcp = currentFrame.bcp();
        code = currentFrame.code();
        return Bytecode.from(code[bcp]);
    }

    public void setLocal(int index, Value value) {
        _currentThread.frame().setLocal(index, value);
    }

    public Value getLocal(int index) {
        return _currentThread.frame().getLocal(index);
    }

    public void push(Value value) {
        _currentThread.frame().stack().push(value);
    }

    public Value pop() {
        return _currentThread.frame().stack().pop();
    }

    public Value peek() {
        return _currentThread.frame().stack().peek();
    }

    public Value peek(int n) {
        final Stack<Value> operands = _currentThread.frame().stack();
        return operands.elementAt(operands.size() - n);
    }

    public byte getInlinedParameterByte(int offset) {
        final ExecutionFrame currentFrame = _currentThread.frame();
        final int bcp = currentFrame.bcp();
        final byte[] code = currentFrame.code();

        return code[bcp + offset];
    }

    public short getInlinedParameterShort(int offset) {
        final ExecutionFrame currentFrame = _currentThread.frame();
        final int bcp = currentFrame.bcp();
        final byte[] code = currentFrame.code();

        return (short) (code[bcp + offset] << 8 | code[bcp + offset + 1] & 0xff);
    }

    public int getInlinedParameterInt(int offset) {
        final ExecutionFrame currentFrame = _currentThread.frame();
        final int bcp = currentFrame.bcp();
        final byte[] code = currentFrame.code();

        return code[bcp + offset]             << 24
            | (code[bcp + offset + 1] & 0xFF) << 16
            | (code[bcp + offset + 2] & 0xFF) << 8
            | (code[bcp + offset + 3] & 0xFF);
    }

    //NOTE: this method accesses by absolute index rather than an offset from bcp!
    public int getInlinedParameterIntN(int index) {
        final byte[] code = _currentThread.frame().code();

        return code[index]             << 24
            | (code[index + 1] & 0xFF) << 16
            | (code[index + 2] & 0xFF) << 8
            | (code[index + 3] & 0xFF);
    }

    public Value widenIfNecessary(Value value) {
        if (value instanceof ByteValue || value instanceof ShortValue || value instanceof CharValue || value instanceof BooleanValue) {
            return IntValue.from(value.toInt());
        }

        return value;
    }

    public Value resolveConstantReference(short cpIndex) {
        Value constant = _currentThread.frame().constantPool().valueAt(cpIndex);

        if (constant instanceof ObjectReferenceValue) {
            constant = TeleReferenceValue.from(_teleVM, Reference.fromJava(constant.unboxObject()));
        }

        return widenIfNecessary(constant);
    }

    public static class InterpretedException extends Exception { };

    /**
     * Used to throw runtime exceptions from within the VM.
     * Pushes the exception onto the stack and throws its own
     * RuntimeException, which reaches the handler in
     * Interpreter.interpreterLoop().
     */
    public void throwException(ReferenceValue t) throws InterpretedException {
        push(t);
        throw new InterpretedException();
    }

    public void throwException(Throwable t) throws InterpretedException {
        throwException(ReferenceValue.from(t));
    }

    public boolean handleException(ReferenceValue t) {
        //there was a commented out println here, but now i understand: i am driving a mercedes.
        ExecutionFrame frame = _currentThread.frame();

        while (frame != null) {
            final ExceptionHandlerTable table = frame.new ExceptionHandlerTable();
            final ExceptionHandlerEntry handler = table.findHandler(t.getClassActor(), frame.bcp());

            if (handler != null) {
                setBCP(handler.handlerPosition() - 1); // will get incremented
                push(t);
                return true;
            }

            frame = _currentThread.popFrame();
        }

        //well we're out of options..
        return false;
    }

    public void printStackTrace() {
        ExecutionFrame frame = _currentThread.frame();

        System.err.println("Interpreter stack trace:");

        while (frame != null) {
            System.err.println(frame.method().holder().name() + "." + frame.method().name() + "()");
            frame = frame.previousFrame();
        }
    }

    public int depth() {
        return _currentThread.frame().stack().size();
    }

    public Value getStatic(short cpIndex) {
        final ConstantPool constantPool = _currentThread.frame().constantPool();
        final FieldRefConstant fieldRef = constantPool.fieldAt(cpIndex);

        if (_teleVM != null) {
            final TypeDescriptor holderTypeDescriptor = fieldRef.holder(constantPool);
            final ClassActor holder = holderTypeDescriptor.toClassActor(PrototypeClassLoader.PROTOTYPE_CLASS_LOADER);
            final String fieldName = fieldRef.name(constantPool).toString();
            final Kind kind = fieldRef.type(constantPool).toKind();
            final FieldActor fieldActor = holder.findStaticFieldActor(fieldName);

            final TeleClassActor teleClassActor = _teleVM.teleClassRegistry().findTeleClassActorByType(holderTypeDescriptor);
            final TeleStaticTuple teleStaticTuple = teleClassActor.getTeleStaticTuple();
            final Reference staticTupleReference = teleStaticTuple.reference();

            if (kind == Kind.BOOLEAN) {
                return IntValue.from(staticTupleReference.readBoolean(fieldActor.offset()) ? 1 : 0);
            } else if (kind == Kind.BYTE) {
                return IntValue.from(staticTupleReference.readByte(fieldActor.offset()));
            } else if (kind == Kind.CHAR) {
                return IntValue.from(staticTupleReference.readChar(fieldActor.offset()));
            } else if (kind == Kind.DOUBLE) {
                return DoubleValue.from(staticTupleReference.readDouble(fieldActor.offset()));
            } else if (kind == Kind.FLOAT) {
                return FloatValue.from(staticTupleReference.readFloat(fieldActor.offset()));
            } else if (kind == Kind.INT) {
                return IntValue.from(staticTupleReference.readInt(fieldActor.offset()));
            } else if (kind == Kind.LONG) {
                return LongValue.from(staticTupleReference.readLong(fieldActor.offset()));
            } else if (kind == Kind.REFERENCE) {
                return _teleVM.createReferenceValue(_teleVM.wordToReference(staticTupleReference.readWord(fieldActor.offset())));
            } else if (kind == Kind.SHORT) {
                return IntValue.from(staticTupleReference.readShort(fieldActor.offset()));
            } else if (kind == Kind.WORD) {
                return new WordValue(staticTupleReference.readWord(fieldActor.offset()));
            }
        } else {
            final FieldActor fieldActor = fieldRef.resolve(constantPool, cpIndex);
            return widenIfNecessary(fieldActor.readValue(Reference.fromJava(fieldActor.holder().staticTuple())));
        }

        return null;
    }

    public void putStatic(short cpIndex, Value value) {
        if (_teleVM != null) {
            ProgramError.unexpected("Cannot run putstatic remotely!");
        } else {
            final ConstantPool cp = _currentThread.frame().constantPool();
            final FieldActor fieldActor = cp.fieldAt(cpIndex).resolve(cp, cpIndex);

            fieldActor.writeErasedValue(fieldActor.holder().staticTuple(), value);
        }
    }

    public Value getField(Reference instance, short cpIndex) {
        final ConstantPool constantPool = _currentThread.frame().constantPool();
        final FieldRefConstant fieldRef = constantPool.fieldAt(cpIndex);

        if (instance instanceof TeleReference && !((TeleReference) instance).isLocal()) {
            final Kind kind = fieldRef.type(constantPool).toKind();
            final FieldActor fieldActor = fieldRef.resolve(constantPool, cpIndex);

            if (kind == Kind.BOOLEAN) {
                return IntValue.from(instance.readBoolean(fieldActor.offset()) ? 1 : 0);
            } else if (kind == Kind.BYTE) {
                return IntValue.from(instance.readByte(fieldActor.offset()));
            } else if (kind == Kind.CHAR) {
                return IntValue.from(instance.readChar(fieldActor.offset()));
            } else if (kind == Kind.DOUBLE) {
                return DoubleValue.from(instance.readDouble(fieldActor.offset()));
            } else if (kind == Kind.FLOAT) {
                return FloatValue.from(instance.readFloat(fieldActor.offset()));
            } else if (kind == Kind.INT) {
                return IntValue.from(instance.readInt(fieldActor.offset()));
            } else if (kind == Kind.LONG) {
                return LongValue.from(instance.readLong(fieldActor.offset()));
            } else if (kind == Kind.REFERENCE) {
                return _teleVM.createReferenceValue(_teleVM.wordToReference(instance.readWord(fieldActor.offset())));
            } else if (kind == Kind.SHORT) {
                return IntValue.from(instance.readShort(fieldActor.offset()));
            } else if (kind == Kind.WORD) {
                return new WordValue(instance.readWord(fieldActor.offset()));
            }
        } else {
            return widenIfNecessary(fieldRef.resolve(constantPool, cpIndex).readValue(instance));
        }
        throw ProgramError.unexpected();
    }

    public void putField(Object instance, short cpIndex, Value value) {
        if (instance instanceof TeleReference && !((TeleReference) instance).isLocal()) {
            ProgramError.unexpected("Cannot run putfield remotely!");
        } else {
            final ConstantPool cp = _currentThread.frame().constantPool();
            final FieldActor fieldActor = cp.fieldAt(cpIndex).resolve(cp, cpIndex);

            if (value instanceof TeleReferenceValue) {
                fieldActor.writeErasedValue(instance, TeleReferenceValue.from(_teleVM, makeLocalReference((TeleReference) value.asReference())));
            } else {
                fieldActor.writeErasedValue(instance, value);
            }
        }
    }

    public MethodActor resolveMethod(short cpIndex) {
        final ConstantPool cp = _currentThread.frame().constantPool();
        final MethodRefConstant methodRef = cp.methodAt(cpIndex);
        return methodRef.resolve(cp, cpIndex);
    }

    private Object readRemoteArray(TeleReference remoteArray, int length, TypeDescriptor type) {
        Object localArray = null;
        // TODO: this could probably ask the kind to perform the operation
        if (type == JavaTypeDescriptor.BOOLEAN) {
            final boolean[] array = new boolean[length];

            for (int i = 0; i < length; i++) {
                array[i] = Layout.getBoolean(remoteArray, i);
            }

            localArray = array;
        } else if (type == JavaTypeDescriptor.BYTE) {
            final byte[] array = new byte[length];

            for (int i = 0; i < length; i++) {
                array[i] = Layout.getByte(remoteArray, i);
            }

            localArray = array;
        } else if (type == JavaTypeDescriptor.CHAR) {
            final char[] array =  new char[length];

            for (int i = 0; i < length; i++) {
                array[i] = Layout.getChar(remoteArray, i);
            }

            localArray = array;
        } else if (type == JavaTypeDescriptor.DOUBLE) {
            final double[] array = new double[length];

            for (int i = 0; i < length; i++) {
                array[i] = Layout.getDouble(remoteArray, i);
            }

            localArray = array;
        } else if (type == JavaTypeDescriptor.FLOAT) {
            final float[] array = new float[length];

            for (int i = 0; i < length; i++) {
                array[i] = Layout.getFloat(remoteArray, i);
            }

            localArray = array;
        } else if (type == JavaTypeDescriptor.INT) {
            final int[] array = new int[length];

            for (int i = 0; i < length; i++) {
                array[i] = Layout.getInt(remoteArray, i);
            }

            localArray = array;
        } else if (type == JavaTypeDescriptor.LONG) {
            final long[] array = new long[length];

            for (int i = 0; i < length; i++) {
                array[i] = Layout.getLong(remoteArray, i);
            }

            localArray = array;
        } else if (type == JavaTypeDescriptor.SHORT) {
            final short[] array = new short[length];

            for (int i = 0; i < length; i++) {
                array[i] = Layout.getShort(remoteArray, i);
            }

            localArray = array;
        } else {
            ProgramError.unexpected("readRemoteArray called without a primitive array type");
        }

        return localArray;
    }

    Reference makeLocalReference(TeleReference remoteReference) {
        if (remoteReference.isLocal()) {
            return remoteReference;
        }

        final ClassActor remoteReferenceClassActor = _teleVM.makeClassActorForTypeOf(remoteReference);

        if (remoteReferenceClassActor.typeDescriptor().equals(JavaTypeDescriptor.STRING)) {
            return Reference.fromJava(_teleVM.getString(remoteReference));
        } else if (remoteReferenceClassActor.isArrayClassActor() && remoteReferenceClassActor.componentClassActor().isPrimitiveClassActor()) {
            final int arrayLength = Layout.readArrayLength(remoteReference);
            return Reference.fromJava(readRemoteArray(remoteReference, arrayLength, remoteReferenceClassActor.componentClassActor().typeDescriptor()));
        } else {
            //should put some tracing error message here
            return remoteReference;
        }
    }

    private void invertOperands(Stack<Value> argumentStack, Value[] arguments) {
        for (int i = 0; i < arguments.length; i++) {
            arguments[i] = argumentStack.pop();

            if (arguments[i] instanceof TeleReferenceValue) {
                final TeleReferenceValue inspectorReferenceArgument = (TeleReferenceValue) arguments[i];
                final TeleReference reference = (TeleReference) inspectorReferenceArgument.asReference();
                if (!reference.isLocal()) {
                    arguments[i] = TeleReferenceValue.from(_teleVM, makeLocalReference(reference));
                }
            }
        }
    }

    public void invokeMethod(ClassMethodActor method) throws InterpretedException {
        final ExecutionFrame oldFrame = _currentThread.frame();
        final Stack<Value> argumentStack = new Stack<Value>();
        final Stack<Value> oldOperands = oldFrame.stack();
        int numberOfParameters = method.descriptor().getNumberOfParameters();
        int i;

        if (!method.isStatic()) {
            numberOfParameters++;
        }

        //inverting the operands
        for (i = 0; i < numberOfParameters; i++) {
            argumentStack.push(oldOperands.pop());
        }

        if (method.isNative()) {
            final Value[] arguments = new Value[numberOfParameters];

            System.err.println("Attempting to invoke native method: " + method.format("%r %H.%n(%p)"));
            invertOperands(argumentStack, arguments);

            try {
                push(widenIfNecessary(method.invoke(arguments)));
            } catch (Exception e) {
                System.err.println("Bailing -- couldn't invoke native method");
                e.printStackTrace();
                System.exit(-1);
            }
        } else if (method.isInstanceInitializer()) {
            argumentStack.pop(); // drop the existing receiver
            final Value[] arguments = new Value[--numberOfParameters];

            invertOperands(argumentStack, arguments);
            try {
                final Reference result = Reference.fromJava(method.invokeConstructor(arguments).asObject());
                push(TeleReferenceValue.from(_teleVM, result));
            } catch (Throwable throwable) {
                throwException(throwable);
            }
        } else if (method.codeAttribute() == null || Word.class.isAssignableFrom(method.holder().toJava())) {
            if (method.isStatic()) {
                Problem.unimplemented();
            } else {
                final Value[] arguments = new Value[numberOfParameters];
                invertOperands(argumentStack, arguments);

                try {
                    Value result = method.invoke(arguments);
                    if (result.kind() == Kind.REFERENCE) {
                        result = TeleReferenceValue.from(_teleVM, Reference.fromJava(result.asObject()));
                    }
                    push(widenIfNecessary(result));
                } catch (Throwable throwable) {
                    throwException(throwable);
                }
            }
        } else {
            final ExecutionFrame newFrame = _currentThread.pushFrame(method);
            i = 0;
            while (i < numberOfParameters) {
                final Value argument = argumentStack.pop();
                newFrame.setLocal(i, argument);

                if (argument instanceof DoubleValue || argument instanceof LongValue) {
                    i++;
                    numberOfParameters++;
                }
                i++;
            }
        }
    }

    public ClassActor resolveClassReference(short constantPoolIndex) {
        final ConstantPool constantPool = _currentThread.frame().constantPool();

        return constantPool.classAt(constantPoolIndex).resolve(constantPool, constantPoolIndex);
    }
}
