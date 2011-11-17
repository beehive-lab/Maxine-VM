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
package com.sun.max.tele.interpreter;

import java.lang.reflect.*;
import java.util.*;

import com.sun.max.tele.*;
import com.sun.max.tele.object.*;
import com.sun.max.tele.reference.*;
import com.sun.max.tele.util.*;
import com.sun.max.tele.value.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.layout.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;

/**
 * The Interpreter's interface to the VM.  Encapsulates all the state of the VM.
 * Can run without VM for testing.
 */
public final class Machine extends AbstractVmHolder {

    private ExecutionThread currentThread;

    Machine(TeleVM vm) {
        super(vm);
        final ExecutionThread mainThread = newThread(java.lang.Thread.NORM_PRIORITY, ExecutionThread.ThreadType.NORMAL_THREAD);
        //JavaThreads.initialize(mainThread);
        activate(mainThread);
    }

    public ReferenceValue toReferenceValue(Reference reference) {
        if (vm() == null) {
            return ObjectReferenceValue.from(reference.toJava());
        } else {
            return referenceManager().createReferenceValue(reference);
        }
    }

    public ExecutionFrame pushFrame(ClassMethodActor method) {
        return currentThread.pushFrame(method);
    }

    public ExecutionFrame popFrame() {
        return currentThread.popFrame();
    }

    public void activate(ExecutionThread thread) {
        //active_threads.insertElementAt(this_thread, 0);
        currentThread = thread;
    }

    public ExecutionThread newThread(int prio, ExecutionThread.ThreadType threadType) {
        return new ExecutionThread(prio, threadType);
    }

    public MethodActor currentMethod() {
        return currentThread.frame().method();
    }

    public ExecutionThread currentThread() {
        return currentThread;
    }

    public void jump(int offset) {
        currentThread.frame().jump(offset);
    }

    public int readOpcode() {
        return currentThread.frame().readOpcode();
    }

    public void setLocal(int index, Value value) {
        currentThread.frame().setLocal(index, value);
    }

    public Value getLocal(int index) {
        return currentThread.frame().getLocal(index);
    }

    public void push(Value value) {
        currentThread.frame().stack().push(value);
    }

    public Value pop() {
        return currentThread.frame().stack().pop();
    }

    public Value peek() {
        return currentThread.frame().stack().peek();
    }

    public Value peek(int n) {
        final Stack<Value> operands = currentThread.frame().stack();
        return operands.elementAt(operands.size() - n);
    }

    public byte readByte() {
        return currentThread.frame().readByte();
    }

    public short readShort() {
        return currentThread.frame().readShort();
    }

    public int readInt() {
        return currentThread.frame().readInt();
    }

    public void skipBytes(int n) {
        currentThread.frame().skipBytes(n);
    }

    public void alignInstructionPosition() {
        currentThread.frame().alignInstructionPosition();
    }

    public Value widenIfNecessary(Value value) {
        if (value.kind().stackKind == Kind.INT) {
            return IntValue.from(value.toInt());
        }

        return value;
    }

    public Value resolveConstantReference(int cpIndex) {
        Value constant = currentThread.frame().constantPool().valueAt(cpIndex);

        if (constant instanceof ObjectReferenceValue) {
            constant = toReferenceValue(Reference.fromJava(constant.unboxObject()));
        }

        return widenIfNecessary(constant);
    }

    /**
     * Converts a given reference to an object to a {@link Throwable} instance.
     *
     * @param vm the VM to be used if {@code throwableReference} is a reference in a VM's address space
     * @param throwableReference the reference to be converted to a {@code Throwable instance}
     * @return a {@code Throwable instance} converted from {@code throwableReference}
     */
    private static Throwable toThrowable(TeleVM vm, ReferenceValue throwableReference) {
        if (throwableReference instanceof TeleReferenceValue) {
            try {
                return (Throwable) vm.objects().makeTeleObject(throwableReference.asReference()).deepCopy();
            } catch (Exception e1) {
                throw TeleError.unexpected("Could not make a local copy of a remote Throwable", e1);
            }
        } else {
            return (Throwable) throwableReference.asBoxedJavaValue();
        }
    }

    public TeleInterpreterException raiseException(ReferenceValue throwableReference) throws TeleInterpreterException {
        throw new TeleInterpreterException(toThrowable(vm(), throwableReference), this);
    }

    public TeleInterpreterException raiseException(Throwable throwable) throws TeleInterpreterException {
        throw new TeleInterpreterException(throwable, this);
    }

    /**
     * Looks for an exception handler in the current execution scope that handles a given exception. If one is found,
     * the execution context is adjusted appropriately so that execution will resume at the discovered handler.
     * If no handler is found, the execution context is not modified.
     *
     * @param throwableReference the reference value representing the exception to be handled
     * @return {@code true} if an appropriate exception handler was found, {@code false} otherwise
     */
    public boolean handleException(ReferenceValue throwableReference) {
        if (currentThread.handleException(throwableReference.getClassActor())) {
            push(throwableReference);
            return true;
        }
        return false;
    }

    public int depth() {
        return currentThread.frame().stack().size();
    }

    public Value getStatic(int cpIndex) {
        final ConstantPool constantPool = currentThread.frame().constantPool();
        final FieldRefConstant fieldRef = constantPool.fieldAt(cpIndex);
        if (vm() != null) {
            final FieldActor fieldActor = fieldRef.resolve(constantPool, cpIndex);
            final TeleClassActor teleClassActor = classes().findTeleClassActor(fieldActor.holder().typeDescriptor);
            final TeleStaticTuple teleStaticTuple = teleClassActor.getTeleStaticTuple();
            final Reference staticTupleReference = teleStaticTuple.reference();

            switch (fieldActor.kind.asEnum) {
                case BOOLEAN:
                case BYTE:
                case CHAR:
                case SHORT:
                case INT: {
                    final int intValue = fieldActor.kind.readValue(staticTupleReference, fieldActor.offset()).toInt();
                    return IntValue.from(intValue);
                }
                case FLOAT: {
                    return FloatValue.from(staticTupleReference.readFloat(fieldActor.offset()));
                }
                case LONG: {
                    return LongValue.from(staticTupleReference.readLong(fieldActor.offset()));
                }
                case DOUBLE: {
                    return DoubleValue.from(staticTupleReference.readDouble(fieldActor.offset()));
                }
                case WORD: {
                    return new WordValue(staticTupleReference.readWord(fieldActor.offset()));
                }
                case REFERENCE: {
                    final TeleReference reference = referenceManager().makeReference(staticTupleReference.readWord(fieldActor.offset()).asAddress());
                    return referenceManager().createReferenceValue(reference);
                }
            }
        } else {
            final FieldActor fieldActor = fieldRef.resolve(constantPool, cpIndex);
            return widenIfNecessary(fieldActor.readValue(Reference.fromJava(fieldActor.holder().staticTuple())));
        }

        return null;
    }

    public void putStatic(int cpIndex, Value value) {
        if (vm() != null) {
            TeleError.unexpected("Cannot run putstatic remotely!");
        } else {
            final ConstantPool cp = currentThread.frame().constantPool();
            final FieldActor fieldActor = cp.fieldAt(cpIndex).resolve(cp, cpIndex);
            fieldActor.writeValue(fieldActor.holder().staticTuple(), fieldActor.kind.convert(value));
        }
    }

    public Value getField(Reference instance, int cpIndex) throws TeleInterpreterException {
        if (instance.isZero()) {
            raiseException(new NullPointerException());
        }
        final ConstantPool constantPool = currentThread.frame().constantPool();
        final FieldRefConstant fieldRef = constantPool.fieldAt(cpIndex);
        final FieldActor fieldActor = fieldRef.resolve(constantPool, cpIndex);
        final Kind kind = fieldActor.kind;

        if (kind.isExtendedPrimitiveValue()) {
            return widenIfNecessary(fieldActor.readValue(instance));
        } else {
            assert kind.isReference;
            if (instance instanceof TeleReference && !((TeleReference) instance).isLocal()) {
                final TeleReference reference = referenceManager().makeReference(instance.readWord(fieldActor.offset()).asAddress());
                return referenceManager().createReferenceValue(reference);
            } else {
                return fieldActor.readValue(instance);
            }
        }
    }

    public void putField(Object instance, int cpIndex, Value value) {
        if (instance instanceof TeleReference && !((TeleReference) instance).isLocal()) {
            TeleError.unexpected("Cannot run putfield remotely!");
        } else {
            final ConstantPool cp = currentThread.frame().constantPool();
            final FieldActor fieldActor = cp.fieldAt(cpIndex).resolve(cp, cpIndex);

            if (value instanceof TeleReferenceValue) {
                fieldActor.writeValue(instance, TeleReferenceValue.from(vm(), makeLocalReference((TeleReference) value.asReference())));
            } else {
                final Value val = fieldActor.kind.convert(value);
                fieldActor.writeValue(instance, val);
            }
        }
    }

    public MethodActor resolveMethod(int cpIndex) {
        final ConstantPool cp = currentThread.frame().constantPool();
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
            TeleError.unexpected("readRemoteArray called without a primitive array type");
        }

        return localArray;
    }

    Reference makeLocalReference(TeleReference remoteReference) {
        if (remoteReference.isLocal()) {
            return remoteReference;
        }

        final ClassActor remoteReferenceClassActor = classes().makeClassActorForTypeOf(remoteReference);

        if (remoteReferenceClassActor.typeDescriptor.equals(JavaTypeDescriptor.STRING)) {
            return Reference.fromJava(vm().getString(remoteReference));
        } else if (remoteReferenceClassActor.isArrayClass() && remoteReferenceClassActor.componentClassActor().isPrimitiveClassActor()) {
            final int arrayLength = Layout.readArrayLength(remoteReference);
            return Reference.fromJava(readRemoteArray(remoteReference, arrayLength, remoteReferenceClassActor.componentClassActor().typeDescriptor));
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
                    arguments[i] = TeleReferenceValue.from(vm(), makeLocalReference(reference));
                }
            }
        }
    }

    public void invokeMethod(ClassMethodActor method) throws TeleInterpreterException {
        final ExecutionFrame oldFrame = currentThread.frame();
        final Stack<Value> argumentStack = new Stack<Value>();
        final Stack<Value> oldOperands = oldFrame.stack();
        int numberOfParameters = method.descriptor().numberOfParameters();
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
            invertOperands(argumentStack, arguments);
            try {
                push(widenIfNecessary(method.invoke(arguments)));
            } catch (InvocationTargetException e) {
                throw new TeleInterpreterException(e.getCause(), this);
            } catch (IllegalAccessException e) {
                throw new TeleInterpreterException(e, this);
            }
        } else if (method.codeAttribute() == null || Word.class.isAssignableFrom(method.holder().toJava())) {
            final Value[] arguments = new Value[numberOfParameters];
            invertOperands(argumentStack, arguments);

            try {
                Value result = method.invoke(arguments);
                if (result.kind().isReference) {
                    result = toReferenceValue(Reference.fromJava(result.asObject()));
                }
                push(widenIfNecessary(result));
            } catch (InvocationTargetException e) {
                throw new TeleInterpreterException(e.getCause(), this);
            } catch (IllegalAccessException e) {
                throw new TeleInterpreterException(e, this);
            }
        } else {
            final ExecutionFrame newFrame = currentThread.pushFrame(method);
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

    public ClassActor resolveClassReference(int constantPoolIndex) {
        final ConstantPool constantPool = currentThread.frame().constantPool();
        return constantPool.classAt(constantPoolIndex).resolve(constantPool, constantPoolIndex);
    }
}
