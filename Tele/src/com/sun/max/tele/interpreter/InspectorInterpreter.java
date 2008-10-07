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

import com.sun.max.tele.*;
import com.sun.max.tele.reference.*;
import com.sun.max.tele.value.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.bytecode.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.layout.*;
import com.sun.max.vm.prototype.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;

/**
 * Entry points and utility functions for the interpreter.
 *
 * @author Athul Acharya
 */
public final class InspectorInterpreter {

    private final TeleVM _teleVM;
    public static final Object _inspectionLock = new Object();

    private final Machine _machine;
    private Value _ret;

    private InspectorInterpreter(TeleVM teleVM, ClassMethodActor classMethodActor, Value...args) {
        _teleVM = teleVM;
        _machine = new Machine(teleVM);
        _machine.pushFrame(classMethodActor);
        int j = 0;
        for (int i = 0; i < args.length; i++, j++) {
            _machine.setLocal(j, args[i]);

            if (args[i] instanceof DoubleValue || args[i] instanceof LongValue) {
                j++;
            }
        }
    }

    public static Value start(TeleVM teleVM, ClassMethodActor classMethodActor, Value... args) {
        InspectorInterpreter i;
        Value ret;

        i = new InspectorInterpreter(teleVM, classMethodActor, args);
        ret = i.run();
        return ret;
    }

    public static Value start(TeleVM teleVM, String className, String methodName, SignatureDescriptor methodDescriptor, Value... args) {
        ClassActor classActor;
        ClassMethodActor classMethodActor;

        classActor = PrototypeClassLoader.PROTOTYPE_CLASS_LOADER.mustMakeClassActor(JavaTypeDescriptor.getDescriptorForJavaString(className));
        classMethodActor = classActor.findClassMethodActor(SymbolTable.makeSymbol(methodName), methodDescriptor);

        if (classMethodActor == null) {
            System.err.println("No such method: " + methodDescriptor + methodName);
            return null;
        }

        return start(teleVM, classMethodActor, args);

    }

    public static Value start(TeleVM teleVM, Class javaType, String methodName, SignatureDescriptor methodDescriptor, Value... args) {
        return start(teleVM, javaType.getName(), methodName, methodDescriptor, args);
    }

    public Value run() {
        Bytecode code;
        MethodStatus status;

        while (true) {
            //Machine.switchThreadIfNecessary();
            code = _machine.nextInstruction();

            //Trace.line(1, _machine.currentMethod().name() + "@" + _machine.getBCP() + " " + code);

            try {
                status = interpret(code);
                if (status == MethodStatus.METHOD_END || status == MethodStatus.METHOD_ABORT) {
                    break;
                } else if (status == MethodStatus.METHOD_EXCEPTION) { //_only_ returned when throwException() is called, which _always_ puts an exception at the top of the stack
                    final ReferenceValue exception = (ReferenceValue) _machine.peek();
                    final boolean handled = _machine.handleException(exception); //if this succeeds we keep looping

                    if (!handled) {
                        if (exception instanceof TeleReferenceValue) {
                            //cant .printStackTrace() this guy
                            System.err.println("Method " + _machine.currentMethod().format("%H.%n(%p)") + " threw unhandled remote exception " + exception.getClassActor().name());
                        } else if (exception instanceof ObjectReferenceValue) {
                            //we get here if a local exception is thrown by interpreted code but not handled
                            final Throwable throwable = (Throwable) exception.asBoxedJavaValue();

                            System.err.println("Exception thrown by interpreted code, but not handled:");
                            throwable.printStackTrace();
                        }

                        _ret = ReferenceValue.NULL;
                        return _ret;
                    }
                }
            } catch (Throwable t) {
                //this is for exceptions thrown by the interpreter only
                System.err.println("Exception thrown by Interpreter:");
                t.printStackTrace();
                _machine.printStackTrace();
                _ret = ReferenceValue.NULL;
                return _ret;
            }
        }

        if (_ret instanceof TeleReferenceValue) {
            _ret = TeleReferenceValue.from(_teleVM, _machine.makeLocalReference((TeleReference) _ret.asReference()));
        }

        return _ret;
    }

    public MethodStatus interpret(Bytecode code) {
        try {
            switch (code) {
                case NOP:                       // 0x00
                    break;

                case ACONST_NULL:               // 0x01
                    _machine.push(ReferenceValue.NULL);
                    break;

                /*========================================================================*/

                case ICONST_M1:                 // 0x02;
                    _machine.push(IntValue.from(-1));
                    break;

                case ICONST_0:                  // 0x03;
                    _machine.push(IntValue.from(0));
                    break;

                case ICONST_1:                  // 0x04;
                    _machine.push(IntValue.from(1));
                    break;

                case ICONST_2:                  // 0x05;
                    _machine.push(IntValue.from(2));
                    break;

                case ICONST_3:                  // 0x06;
                    _machine.push(IntValue.from(3));
                    break;

                case ICONST_4:                  // 0x07;
                    _machine.push(IntValue.from(4));
                    break;

                case ICONST_5:                  // 0x08;
                    _machine.push(IntValue.from(5));
                    break;

                /*========================================================================*/

                case LCONST_0:                  // 0x09;
                    _machine.push(LongValue.from(0));
                    break;

                case LCONST_1:                  // 0x0A;
                    _machine.push(LongValue.from(1));
                    break;

                /*========================================================================*/

                case FCONST_0:                  // 0x0B;
                    _machine.push(FloatValue.from(0));
                    break;

                case FCONST_1:                  // 0x0C;
                    _machine.push(FloatValue.from(1));
                    break;

                case FCONST_2:                  // 0x0D;
                    _machine.push(FloatValue.from(2));
                    break;

                /*========================================================================*/

                case DCONST_0:                  // 0x0E;
                    _machine.push(DoubleValue.from(0));
                    break;

                case DCONST_1:                  // 0x0F;
                    _machine.push(DoubleValue.from(1));
                    break;

                /*========================================================================*/

                case BIPUSH:                    // 0x10;
                    _machine.push(IntValue.from(_machine.getInlinedParameterByte(1)));
                    _machine.incrementBCP(1);
                    break;

                case SIPUSH:                    // 0x11;
                    _machine.push(IntValue.from(_machine.getInlinedParameterShort(1)));
                    _machine.incrementBCP(2);
                    break;

                /*========================================================================*/

                case LDC: {                     // 0x12;
                    final byte cpIndex = _machine.getInlinedParameterByte(1);
                    _machine.push(_machine.resolveConstantReference((short) (cpIndex & 0xFF)));
                    _machine.incrementBCP(1);
                    break;
                }

                case LDC_W:                     // 0x13;
                case LDC2_W: {                  // 0x14;
                    final short cpIndex = _machine.getInlinedParameterShort(1);
                    _machine.push(_machine.resolveConstantReference(cpIndex));
                    _machine.incrementBCP(2);
                    break;
                }

                /*========================================================================*/

                case ILOAD:                     // 0x15;
                case LLOAD:                     // 0x16;
                case FLOAD:                     // 0x17;
                case DLOAD:                     // 0x18;
                case ALOAD:                     // 0x19;
                    _machine.push(_machine.getLocal(_machine.getInlinedParameterByte(1)));
                    _machine.incrementBCP(1);
                    break;

                /*========================================================================*/

                case ILOAD_0:                   // 0x1A;
                    _machine.push(_machine.getLocal(0));
                    break;

                case ILOAD_1:                   // 0x1B;
                    _machine.push(_machine.getLocal(1));
                    break;

                case ILOAD_2:                   // 0x1C;
                    _machine.push(_machine.getLocal(2));
                    break;

                case ILOAD_3:                   // 0x1D;
                    _machine.push(_machine.getLocal(3));
                    break;

                /*========================================================================*/

                case LLOAD_0:                   // 0x1E;
                    _machine.push(_machine.getLocal(0));
                    break;

                case LLOAD_1:                   // 0x1F;
                    _machine.push(_machine.getLocal(1));
                    break;

                case LLOAD_2:                   // 0x20;
                    _machine.push(_machine.getLocal(2));
                    break;

                case LLOAD_3:                   // 0x21;
                    _machine.push(_machine.getLocal(3));
                    break;

                /*========================================================================*/

                case FLOAD_0:                   // 0x22;
                    _machine.push(_machine.getLocal(0));
                    break;

                case FLOAD_1:                   // 0x23;
                    _machine.push(_machine.getLocal(1));
                    break;

                case FLOAD_2:                   // 0x24;
                    _machine.push(_machine.getLocal(2));
                    break;

                case FLOAD_3:                   // 0x25;
                    _machine.push(_machine.getLocal(3));
                    break;

                /*========================================================================*/

                case DLOAD_0:                   // 0x26;
                    _machine.push(_machine.getLocal(0));
                    break;

                case DLOAD_1:                   // 0x27;
                    _machine.push(_machine.getLocal(1));
                    break;

                case DLOAD_2:                   // 0x28;
                    _machine.push(_machine.getLocal(2));
                    break;

                case DLOAD_3:                   // 0x29;
                    _machine.push(_machine.getLocal(3));
                    break;

                /*========================================================================*/

                case ALOAD_0:                   // 0x2A;
                    _machine.push(_machine.getLocal(0));
                    break;

                case ALOAD_1:                   // 0x2B;
                    _machine.push(_machine.getLocal(1));
                    break;

                case ALOAD_2:                   // 0x2C;
                    _machine.push(_machine.getLocal(2));
                    break;

                case ALOAD_3:                   // 0x2D;
                    _machine.push(_machine.getLocal(3));
                    break;

                /*========================================================================*/

                case IALOAD: {                  // 0x2E;
                    final int index = _machine.pop().asInt();                // Get array index (IntValue)
                    final Reference array = _machine.pop().asReference();    // Get the array (ReferenceValue)

                    if (array == null) {
                        _machine.throwException(new NullPointerException());
                    }

                    if (Layout.readArrayLength(array) <= index || index < 0) {
                        _machine.throwException(new ArrayIndexOutOfBoundsException());
                    }

                    final int val = Layout.getInt(array, index);

                    // Push value to operand stack
                    _machine.push(IntValue.from(val));
                    break;
                }

                case LALOAD: {                  // 0x2F;
                    final int index = _machine.pop().asInt();                // Get array index (IntValue)
                    final Reference array = _machine.pop().asReference();    // Get the array (ReferenceValue)

                    if (array == null) {
                        _machine.throwException(new NullPointerException());
                    }

                    if (Layout.readArrayLength(array) <= index || index < 0) {
                        _machine.throwException(new ArrayIndexOutOfBoundsException());
                    }

                    final long val = Layout.getLong(array, index);

                    // Push value to operand stack
                    _machine.push(LongValue.from(val));
                    break;
                }

                case FALOAD: {                  // 0x30;
                    final int index = _machine.pop().asInt();                // Get array index (IntValue)
                    final Reference array = _machine.pop().asReference();    // Get the array (ReferenceValue)

                    if (array == null) {
                        _machine.throwException(new NullPointerException());
                    }

                    if (Layout.readArrayLength(array) <= index || index < 0) {
                        _machine.throwException(new ArrayIndexOutOfBoundsException());
                    }

                    final float val = Layout.getFloat(array, index);

                    // Push value to operand stack
                    _machine.push(FloatValue.from(val));
                    break;
                }

                case DALOAD: {                  // 0x31;
                    final int index = _machine.pop().asInt();                // Get array index (IntValue)
                    final Reference array = _machine.pop().asReference();    // Get the array (ReferenceValue)

                    if (array == null) {
                        _machine.throwException(new NullPointerException());
                    }

                    if (Layout.readArrayLength(array) <= index || index < 0) {
                        _machine.throwException(new ArrayIndexOutOfBoundsException());
                    }

                    final double val = Layout.getDouble(array, index);

                    // Push value to operand stack
                    _machine.push(DoubleValue.from(val));
                    break;
                }

                case AALOAD: {                  // 0x32;
                    final int index = _machine.pop().asInt();                // Get array index (IntValue)
                    final Reference array = _machine.pop().asReference();    // Get the array (ReferenceValue)

                    if (array == null) {
                        _machine.throwException(new NullPointerException());
                    }

                    if (Layout.readArrayLength(array) <= index || index < 0) {
                        _machine.throwException(new ArrayIndexOutOfBoundsException());
                    }

                    final Reference val = Layout.getReference(array, index);

                    // Push value to operand stack
                    _machine.push(_teleVM.createReferenceValue(val));
                    break;
                }

                case BALOAD: {                  // 0x33;
                    final int index = _machine.pop().asInt();                  // Get array index (IntValue)
                    final Reference array = _machine.pop().asReference();    // Get the array (ReferenceValue)

                    if (array == null) {
                        _machine.throwException(new NullPointerException());
                    }

                    if (Layout.readArrayLength(array) <= index || index < 0) {
                        _machine.throwException(new ArrayIndexOutOfBoundsException());
                    }

                    final byte val = Layout.getByte(array, index);

                    // Push value to operand stack
                    _machine.push(IntValue.from(val));
                    break;
                }

                case CALOAD: {                   // 0x34;
                    final int index = _machine.pop().asInt();                // Get array index (IntValue)
                    final Reference array = _machine.pop().asReference();    // Get the array (ReferenceValue)

                    if (array == null) {
                        _machine.throwException(new NullPointerException());
                    }

                    if (Layout.readArrayLength(array) <= index || index < 0) {
                        _machine.throwException(new ArrayIndexOutOfBoundsException());
                    }

                    final char val = Layout.getChar(array, index);

                    // Push value to operand stack
                    _machine.push(IntValue.from(val));
                    break;
                }

                case SALOAD: {                   // 0x35;
                    final int index = _machine.pop().asInt();                  // Get array index (IntValue)
                    final Reference array = _machine.pop().asReference();    // Get the array (ReferenceValue)

                    if (array == null) {
                        _machine.throwException(new NullPointerException());
                    }

                    if (Layout.readArrayLength(array) <= index || index < 0) {
                        _machine.throwException(new ArrayIndexOutOfBoundsException());
                    }

                    final short val = Layout.getShort(array, index);

                    // Push value to operand stack
                    _machine.push(IntValue.from(val));
                    break;
                }

                /*========================================================================*/

                case ISTORE:                    // 0x36;
                case LSTORE:                    // 0x37;
                case FSTORE:                    // 0x38;
                case DSTORE:                    // 0x39;
                case ASTORE:                    // 0x3A;
                    _machine.setLocal(_machine.getInlinedParameterByte(1), _machine.pop());
                    _machine.incrementBCP(1);
                    break;

                /*========================================================================*/

                case ISTORE_0:                  // 0x3B;
                    _machine.setLocal(0, _machine.pop());
                    break;

                case ISTORE_1:                  // 0x3C;
                    _machine.setLocal(1, _machine.pop());
                    break;

                case ISTORE_2:                  // 0x3D;
                    _machine.setLocal(2, _machine.pop());
                    break;

                case ISTORE_3:                  // 0x3E;
                    _machine.setLocal(3, _machine.pop());
                    break;

                /*========================================================================*/

                case LSTORE_0:                  // 0x3F;
                    _machine.setLocal(0, _machine.pop());
                    break;

                case LSTORE_1:                  // 0x40;
                    _machine.setLocal(1, _machine.pop());
                    break;

                case LSTORE_2:                  // 0x41;
                    _machine.setLocal(2, _machine.pop());
                    break;

                case LSTORE_3:                  // 0x42;
                    _machine.setLocal(3, _machine.pop());
                    break;

                /*========================================================================*/

                case FSTORE_0:                  // 0x43;
                    _machine.setLocal(0, _machine.pop());
                    break;

                case FSTORE_1:                  // 0x44;
                    _machine.setLocal(1, _machine.pop());
                    break;

                case FSTORE_2:                  // 0x45;
                    _machine.setLocal(2, _machine.pop());
                    break;

                case FSTORE_3:                  // 0x46;
                    _machine.setLocal(3, _machine.pop());
                    break;

                /*========================================================================*/

                case DSTORE_0:                  // 0x47;
                    _machine.setLocal(0, _machine.pop());
                    break;

                case DSTORE_1:                  // 0x48;
                    _machine.setLocal(1, _machine.pop());
                    break;

                case DSTORE_2:                  // 0x49;
                    _machine.setLocal(2, _machine.pop());
                    break;

                case DSTORE_3:                  // 0x4A;
                    _machine.setLocal(3, _machine.pop());
                    break;

                /*========================================================================*/

                case ASTORE_0:                  // 0x4B;
                    _machine.setLocal(0, _machine.pop());
                    break;

                case ASTORE_1:                  // 0x4C;
                    _machine.setLocal(1, _machine.pop());
                    break;

                case ASTORE_2:                  // 0x4D;
                    _machine.setLocal(2, _machine.pop());
                    break;

                case ASTORE_3:                  // 0x4E;
                    _machine.setLocal(3, _machine.pop());
                    break;

                /*========================================================================*/

                case IASTORE: {                 // 0x4F;
                    final int val = _machine.pop().toInt();                  // Get value to store
                    final int index = _machine.pop().toInt();                // Get array index
                    final Reference array = _machine.pop().asReference();    // Get the array (ReferenceValue)

                    if (array == null) {
                        _machine.throwException(new NullPointerException());
                    }

                    if (Layout.readArrayLength(array) <= index || index < 0) {
                        _machine.throwException(new ArrayIndexOutOfBoundsException());
                    }

                    Layout.setInt(array, index, val);
                    break;
                }

                case LASTORE: {                 // 0x50;
                    final long val = _machine.pop().toLong();                // Get value to store
                    final int index = _machine.pop().toInt();                // Get array index
                    final Reference array = _machine.pop().asReference();    // Get the array (ReferenceValue)

                    if (array == null) {
                        _machine.throwException(new NullPointerException());
                    }

                    if (Layout.readArrayLength(array) <= index || index < 0) {
                        _machine.throwException(new ArrayIndexOutOfBoundsException());
                    }

                    Layout.setLong(array, index, val);
                    break;
                }

                case FASTORE: {                 // 0x51;
                    final float val = _machine.pop().toFloat();              // Get value to store
                    final int index = _machine.pop().toInt();                // Get array index
                    final Reference array = _machine.pop().asReference();    // Get the array (ReferenceValue)

                    if (array == null) {
                        _machine.throwException(new NullPointerException());
                    }

                    if (Layout.readArrayLength(array) <= index || index < 0) {
                        _machine.throwException(new ArrayIndexOutOfBoundsException());
                    }

                    Layout.setFloat(array, index, val);
                    break;
                }

                case DASTORE: {                 // 0x52;
                    final double val = _machine.pop().toDouble();            // Get value to store
                    final int index = _machine.pop().toInt();                // Get array index
                    final Reference array = _machine.pop().asReference();    // Get the array (ReferenceValue)

                    if (array == null) {
                        _machine.throwException(new NullPointerException());
                    }

                    if (Layout.readArrayLength(array) <= index || index < 0) {
                        _machine.throwException(new ArrayIndexOutOfBoundsException());
                    }

                    Layout.setDouble(array, index, val);
                    break;
                }

                case AASTORE: {                 // 0x53;
                    final Reference val = _machine.pop().asReference();      // Get value to store
                    final int index = _machine.pop().toInt();                // Get array index
                    final Reference array = _machine.pop().asReference();    // Get the array (ReferenceValue)

                    if (array == null) {
                        _machine.throwException(new NullPointerException());
                    }

                    if (Layout.readArrayLength(array) <= index || index < 0) {
                        _machine.throwException(new ArrayIndexOutOfBoundsException());
                    }

                    Layout.setReference(array, index, val);
                    break;
                }

                case BASTORE: {                 // 0x54;
                    final int val = _machine.pop().toInt();                  // Get value to store
                    final int index = _machine.pop().toInt();                // Get array index
                    final Reference array = _machine.pop().asReference();    // Get the array (ReferenceValue)

                    if (array == null) {
                        _machine.throwException(new NullPointerException());
                    }

                    if (Layout.readArrayLength(array) <= index || index < 0) {
                        _machine.throwException(new ArrayIndexOutOfBoundsException());
                    }

                    Layout.setByte(array, index, (byte) val);
                    break;
                }

                case CASTORE: {                 // 0x55;
                    final int val = _machine.pop().toInt();                  // Get value to store
                    final int index = _machine.pop().toInt();                // Get array index
                    final Reference array = _machine.pop().asReference();    // Get the array (ReferenceValue)

                    if (array == null) {
                        _machine.throwException(new NullPointerException());
                    }

                    if (Layout.readArrayLength(array) <= index || index < 0) {
                        _machine.throwException(new ArrayIndexOutOfBoundsException());
                    }

                    Layout.setChar(array, index, (char) val);
                    break;
                }

                case SASTORE: {                 // 0x56;
                    final int val = _machine.pop().toInt();                  // Get value to store
                    final int index = _machine.pop().toInt();                // Get array index
                    final Reference array = _machine.pop().asReference();    // Get the array (ReferenceValue)

                    if (array == null) {
                        _machine.throwException(new NullPointerException());
                    }

                    if (Layout.readArrayLength(array) <= index || index < 0) {
                        _machine.throwException(new ArrayIndexOutOfBoundsException());
                    }

                    Layout.setShort(array, index, (short) val);
                    break;
                }

                /*========================================================================*/

                case POP:                       // 0x57;
                    _machine.pop();
                    break;

                case POP2: {                    // 0x58;
                    final Value s1 = _machine.pop();

                    if (!s1.isCategory2()) {
                        _machine.pop();
                    }
                    break;
                }

                case DUP:                       // 0x59;
                    _machine.push(_machine.peek());
                    break;

                case DUP_X1: {                  // 0x5A;
                    final Value s1 = _machine.pop();
                    final Value s2 = _machine.pop();

                    _machine.push(s1);
                    _machine.push(s2);
                    _machine.push(s1);
                    break;
                }

                case DUP_X2: {                  // 0x5B;
                    final Value s1 = _machine.pop();
                    final Value s2 = _machine.pop();

                    if (s2.isCategory2()) {
                        _machine.push(s1);
                        _machine.push(s2);
                        _machine.push(s1);
                    } else {
                        final Value s3 = _machine.pop();

                        _machine.push(s1);
                        _machine.push(s3);
                        _machine.push(s2);
                        _machine.push(s1);
                    }
                    break;
                }

                case DUP2: {                    // 0x5C;
                    final Value s1 = _machine.pop();

                    if (s1.isCategory2()) {
                        _machine.push(s1);
                        _machine.push(s1);
                    } else {
                        final Value s2 = _machine.pop();

                        _machine.push(s2);
                        _machine.push(s1);
                        _machine.push(s2);
                        _machine.push(s1);
                    }
                    break;
                }

                case DUP2_X1: {                 // 0x5D;
                    final Value s1 = _machine.pop();

                    if (s1.isCategory2()) {
                        final Value s2 = _machine.pop();

                        _machine.push(s1);
                        _machine.push(s2);
                        _machine.push(s1);
                    } else {
                        final Value s2 = _machine.pop();
                        final Value s3 = _machine.pop();

                        _machine.push(s2);
                        _machine.push(s1);
                        _machine.push(s3);
                        _machine.push(s2);
                        _machine.push(s1);
                    }
                    break;
                }

                case DUP2_X2: {                 // 0x5E;
                    final Value s1 = _machine.pop();

                    if (s1.isCategory2()) {
                        final Value s2 = _machine.pop();

                        if (s2.isCategory2()) {
                            _machine.push(s1);
                            _machine.push(s2);
                            _machine.push(s1);
                        } else {
                            final Value s3 = _machine.pop();

                            _machine.push(s1);
                            _machine.push(s3);
                            _machine.push(s2);
                            _machine.push(s1);
                        }
                    } else {
                        final Value s2 = _machine.pop();
                        final Value s3 = _machine.pop();

                        if (s3.isCategory2()) {
                            _machine.push(s2);
                            _machine.push(s1);
                            _machine.push(s3);
                            _machine.push(s2);
                            _machine.push(s1);
                        } else {
                            final Value s4 = _machine.pop();

                            _machine.push(s2);
                            _machine.push(s1);
                            _machine.push(s4);
                            _machine.push(s3);
                            _machine.push(s2);
                            _machine.push(s1);
                        }
                    }
                    break;
                }

                case SWAP: {                    // 0x5F;
                    final Value s1 = _machine.pop();
                    final Value s2 = _machine.pop();

                    _machine.push(s1);
                    _machine.push(s2);
                    break;
                }

                /*========================================================================*/

                case IADD: {                    // 0x60;
                    final int v1 = _machine.pop().asInt();
                    final int v2 = _machine.pop().asInt();

                    _machine.push(IntValue.from(v1 + v2));
                    break;
                }

                case LADD: {                    // 0x61;
                    final long v1 = _machine.pop().asLong();
                    final long v2 = _machine.pop().asLong();

                    _machine.push(LongValue.from(v1 + v2));
                    break;
                }

                case FADD: {                    // 0x62;
                    final float v1 = _machine.pop().asFloat();
                    final float v2 = _machine.pop().asFloat();

                    _machine.push(FloatValue.from(v1 + v2));
                    break;
                }

                case DADD: {                    // 0x63;
                    final double v1 = _machine.pop().asDouble();
                    final double v2 = _machine.pop().asDouble();

                    _machine.push(DoubleValue.from(v1 + v2));
                    break;
                }

                /*========================================================================*/

                case ISUB: {                    // 0x64;
                    final int v1 = _machine.pop().asInt();
                    final int v2 = _machine.pop().asInt();

                    _machine.push(IntValue.from(v2 - v1));
                    break;
                }

                case LSUB: {                    // 0x65;
                    final long v1 = _machine.pop().asLong();
                    final long v2 = _machine.pop().asLong();

                    _machine.push(LongValue.from(v2 - v1));
                    break;
                }

                case FSUB: {                    // 0x66;
                    final float v1 = _machine.pop().asFloat();
                    final float v2 = _machine.pop().asFloat();

                    _machine.push(FloatValue.from(v2 - v1));
                    break;
                }

                case DSUB: {                    // 0x67;
                    final double v1 = _machine.pop().asDouble();
                    final double v2 = _machine.pop().asDouble();

                    _machine.push(DoubleValue.from(v2 - v1));
                    break;
                }

                /*========================================================================*/

                case IMUL: {                    // 0x68;
                    final int v1 = _machine.pop().asInt();
                    final int v2 = _machine.pop().asInt();

                    _machine.push(IntValue.from(v1 * v2));
                    break;
                }

                case LMUL: {                    // 0x69;
                    final long v1 = _machine.pop().asLong();
                    final long v2 = _machine.pop().asLong();

                    _machine.push(LongValue.from(v1 * v2));
                    break;
                }

                case FMUL: {                    // 0x6A;
                    final float v1 = _machine.pop().asFloat();
                    final float v2 = _machine.pop().asFloat();

                    _machine.push(FloatValue.from(v1 * v2));
                    break;
                }

                case DMUL: {                    // 0x6B;
                    final double v1 = _machine.pop().asDouble();
                    final double v2 = _machine.pop().asDouble();

                    _machine.push(DoubleValue.from(v1 * v2));
                    break;
                }

                /*========================================================================*/

                case IDIV: {                    // 0x6C;
                    final int v1 = _machine.pop().asInt();
                    final int v2 = _machine.pop().asInt();

                    if (v1 == 0) {
                        _machine.throwException(new ArithmeticException("Division by zero"));
                    }

                    _machine.push(IntValue.from(v2 / v1));
                    break;
                }

                case LDIV: {                    // 0x6D;
                    final long v1 = _machine.pop().asLong();
                    final long v2 = _machine.pop().asLong();

                    if (v1 == 0L) {
                        _machine.throwException(new ArithmeticException("Division by zero"));
                    }

                    _machine.push(LongValue.from(v2 / v1));
                    break;
                }

                case FDIV: {                    // 0x6E;
                    final float v1 = _machine.pop().asFloat();
                    final float v2 = _machine.pop().asFloat();

                    if (v1 == 0.0) {
                        _machine.throwException(new ArithmeticException("Division by zero"));
                    }

                    _machine.push(FloatValue.from(v2 / v1));
                    break;
                }

                case DDIV: {                    // 0x6F;
                    final double v1 = _machine.pop().asDouble();
                    final double v2 = _machine.pop().asDouble();

                    if (v1 == 0.0D) {
                        _machine.throwException(new ArithmeticException("Division by zero"));
                    }

                    _machine.push(DoubleValue.from(v2 / v1));
                    break;
                }

                /*========================================================================*/

                case IREM: {                    // 0x70;
                    final int v1 = _machine.pop().asInt();
                    final int v2 = _machine.pop().asInt();

                    if (v1 == 0) {
                        _machine.throwException(new ArithmeticException("Division by zero"));
                    }

                    _machine.push(IntValue.from(v2 % v1));
                    break;
                }

                case LREM: {                    // 0x71;
                    final long v1 = _machine.pop().asLong();
                    final long v2 = _machine.pop().asLong();

                    if (v1 == 0L) {
                        _machine.throwException(new ArithmeticException("Division by zero"));
                    }

                    _machine.push(LongValue.from(v2 % v1));
                    break;
                }

                case FREM: {                    // 0x72;
                    final float v1 = _machine.pop().asFloat();
                    final float v2 = _machine.pop().asFloat();

                    if (v1 == 0.0) {
                        _machine.throwException(new ArithmeticException("Division by zero"));
                    }

                    _machine.push(FloatValue.from(v2 % v1));
                    break;
                }

                case DREM: {                    // 0x73;
                    final double v1 = _machine.pop().asDouble();
                    final double v2 = _machine.pop().asDouble();

                    if (v1 == 0.0D) {
                        _machine.throwException(new ArithmeticException("Division by zero"));
                    }

                    _machine.push(DoubleValue.from(v2 % v1));
                    break;
                }

                /*========================================================================*/

                case INEG:                      // 0x74;
                    _machine.push(IntValue.from(0 - _machine.pop().asInt()));
                    break;

                case LNEG:                      // 0x75;
                    _machine.push(LongValue.from(0 - _machine.pop().asLong()));
                    break;

                case FNEG:                      // 0x76;
                    _machine.push(FloatValue.from((float) 0.0 - _machine.pop().asFloat()));
                    break;

                case DNEG:                      // 0x77;
                    _machine.push(DoubleValue.from(0.0 - _machine.pop().asDouble()));
                    break;

                /*========================================================================*/

                case ISHL: {                    // 0x78;
                    final int amount = _machine.pop().asInt();
                    final int value  = _machine.pop().asInt();

                    _machine.push(IntValue.from(value << (amount & 0x1F)));
                    break;
                }

                case LSHL: {                    // 0x79;
                    final int amount = _machine.pop().asInt();
                    final long value = _machine.pop().asLong();

                    _machine.push(LongValue.from(value << (amount & 0x3F)));
                    break;
                }

                case ISHR: {                    // 0x7A;
                    final int amount = _machine.pop().asInt();
                    final int value  = _machine.pop().asInt();

                    _machine.push(IntValue.from(value >> (amount & 0x1F)));
                    break;
                }

                case LSHR: {                    // 0x7B;
                    final int amount = _machine.pop().asInt();
                    final long value = _machine.pop().asLong();

                    _machine.push(LongValue.from(value >> (amount & 0x3F)));
                    break;
                }

                case IUSHR: {                   // 0x7C;
                    final int amount = _machine.pop().asInt();
                    final int value  = _machine.pop().asInt();

                    _machine.push(IntValue.from(value >>> (amount & 0x1F)));
                    break;
                }

                case LUSHR: {                   // 0x7D;
                    final int amount = _machine.pop().asInt();
                    final long value = _machine.pop().asLong();

                    _machine.push(LongValue.from(value >>> (amount & 0x3F)));
                    break;
                }

                /*========================================================================*/

                case IAND: {                    // 0x7E;
                    final int s1 = _machine.pop().asInt();
                    final int s2 = _machine.pop().asInt();

                    _machine.push(IntValue.from(s2 & s1));
                    break;
                }

                case LAND: {                    // 0x7F;
                    final long s1 = _machine.pop().asLong();
                    final long s2 = _machine.pop().asLong();

                    _machine.push(LongValue.from(s2 & s1));
                    break;
                }

                case IOR: {                     // 0x80;
                    final int s1 = _machine.pop().asInt();
                    final int s2 = _machine.pop().asInt();

                    _machine.push(IntValue.from(s2 | s1));
                    break;
                }

                case LOR: {                     // 0x81;
                    final long s1 = _machine.pop().asLong();
                    final long s2 = _machine.pop().asLong();

                    _machine.push(LongValue.from(s2 | s1));
                    break;
                }

                case IXOR: {                    // 0x82;
                    final int s1 = _machine.pop().asInt();
                    final int s2 = _machine.pop().asInt();

                    _machine.push(IntValue.from(s2 ^ s1));
                    break;
                }

                case LXOR: {                    // 0x83;
                    final long s1 = _machine.pop().asLong();
                    final long s2 = _machine.pop().asLong();

                    _machine.push(LongValue.from(s2 ^ s1));
                    break;
                }

                /*========================================================================*/

                case IINC: {                    // 0x84;
                    final int index     = _machine.getInlinedParameterByte(1);
                    final int increment = _machine.getInlinedParameterByte(2);
                    final int value     = _machine.getLocal(index).asInt();

                    _machine.setLocal(index, IntValue.from(value + increment));
                    _machine.incrementBCP(2);
                    break;
                }

                /*========================================================================*/

                case I2L: {                     // 0x85;
                    final int value = _machine.pop().asInt();
                    _machine.push(LongValue.from(value));
                    break;
                }

                case I2F: {                     // 0x86;
                    final int value = _machine.pop().asInt();
                    _machine.push(FloatValue.from(value));
                    break;
                }

                case I2D: {                     // 0x87;
                    final int value = _machine.pop().asInt();
                    _machine.push(DoubleValue.from(value));
                    break;
                }

                case L2I: {                     // 0x88;
                    final long value = _machine.pop().asLong();
                    _machine.push(IntValue.from((int) value));
                    break;
                }

                case L2F: {                     // 0x89;
                    final long value = _machine.pop().asLong();
                    _machine.push(FloatValue.from(value));
                    break;
                }

                case L2D: {                     // 0x8A;
                    final long value = _machine.pop().asLong();
                    _machine.push(DoubleValue.from(value));
                    break;
                }

                case F2I: {                     // 0x8B;
                    final float value = _machine.pop().asFloat();
                    _machine.push(IntValue.from((int) value));
                    break;
                }

                case F2L: {                     // 0x8C;
                    final float value = _machine.pop().asFloat();
                    _machine.push(LongValue.from((long) value));
                    break;
                }

                case F2D: {                     // 0x8D;
                    final float value = _machine.pop().asFloat();
                    _machine.push(DoubleValue.from(value));
                    break;
                }

                case D2I: {                     // 0x8E;
                    final double value = _machine.pop().asDouble();
                    _machine.push(IntValue.from((int) value));
                    break;
                }

                case D2L: {                     // 0x8F;
                    final double value = _machine.pop().asDouble();
                    _machine.push(LongValue.from((long) value));
                    break;
                }

                case D2F: {                     // 0x90;
                    final double value = _machine.pop().asDouble();
                    _machine.push(FloatValue.from((float) value));
                    break;
                }

                case I2B: {                     // 0x91;
                    final byte value = _machine.pop().toByte();
                    _machine.push(IntValue.from(value));
                    break;
                }

                case I2C: {                     // 0x92;
                    final char value = _machine.pop().toChar();
                    _machine.push(IntValue.from(value));
                    break;
                }

                case I2S: {                     // 0x93;
                    final short value  = _machine.pop().toShort();
                    _machine.push(IntValue.from(value));
                    break;
                }

                /*========================================================================*/

                case LCMP: {                    // 0x94;
                    final long right  = _machine.pop().asLong();
                    final long left   = _machine.pop().asLong();
                    final int  result = (left < right) ? -1 : (left == right) ? 0 : 1;

                    _machine.push(IntValue.from(result));
                    break;
                }

                case FCMPL:                     // 0x95;
                case FCMPG: {                   // 0x96;
                    final float right  = _machine.pop().asFloat();
                    final float left   = _machine.pop().asFloat();
                    final int   result = (left < right) ? -1 : (left == right) ? 0 : 1;

                    _machine.push(IntValue.from(result));
                    break;
                }

                case DCMPL:                     // 0x97;
                case DCMPG: {                   // 0x98;
                    final double right  = _machine.pop().asDouble();
                    final double left   = _machine.pop().asDouble();
                    final int    result = (left < right) ? -1 : (left == right) ? 0 : 1;

                    _machine.push(IntValue.from(result));
                    break;
                }

                /*========================================================================*/

                case IFEQ: {                    // 0x99;
                    final int s1 = _machine.pop().asInt();

                    if (s1 == 0) {
                        _machine.incrementBCP(_machine.getInlinedParameterShort(1) - 1);
                    } else {
                        _machine.incrementBCP(2);
                    }

                    break;
                }

                case IFNE: {                    // 0x9A;
                    final int s1 = _machine.pop().asInt();

                    if (s1 != 0) {
                        _machine.incrementBCP(_machine.getInlinedParameterShort(1) - 1);
                    } else {
                        _machine.incrementBCP(2);
                    }

                    break;
                }

                case IFLT: {                    // 0x9B;
                    final int s1 = _machine.pop().asInt();

                    if (s1 < 0) {
                        _machine.incrementBCP(_machine.getInlinedParameterShort(1) - 1);
                    } else {
                        _machine.incrementBCP(2);
                    }

                    break;
                }

                case IFGE: {                    // 0x9C;
                    final int s1 = _machine.pop().asInt();

                    if (s1 >= 0) {
                        _machine.incrementBCP(_machine.getInlinedParameterShort(1) - 1);
                    } else {
                        _machine.incrementBCP(2);
                    }

                    break;
                }

                case IFGT: {                    // 0x9D;
                    final int s1 = _machine.pop().asInt();

                    if (s1 > 0) {
                        _machine.incrementBCP(_machine.getInlinedParameterShort(1) - 1);
                    } else {
                        _machine.incrementBCP(2);
                    }

                    break;
                }

                case IFLE: {                    // 0x9E;
                    final int s1 = _machine.pop().asInt();

                    if (s1 <= 0) {
                        _machine.incrementBCP(_machine.getInlinedParameterShort(1) - 1);
                    } else {
                        _machine.incrementBCP(2);
                    }

                    break;
                }

                case IF_ICMPEQ: {               // 0x9F;
                    final int s1 = _machine.pop().asInt();
                    final int s2 = _machine.pop().asInt();

                    if (s2 == s1) {
                        _machine.incrementBCP(_machine.getInlinedParameterShort(1) - 1);
                    } else {
                        _machine.incrementBCP(2);
                    }

                    break;
                }

                case IF_ICMPNE: {               // 0xA0;
                    final int s1 = _machine.pop().asInt();
                    final int s2 = _machine.pop().asInt();

                    if (s2 != s1) {
                        _machine.incrementBCP(_machine.getInlinedParameterShort(1) - 1);
                    } else {
                        _machine.incrementBCP(2);
                    }

                    break;
                }

                case IF_ICMPLT: {               // 0xA1;
                    final int s1 = _machine.pop().asInt();
                    final int s2 = _machine.pop().asInt();

                    if (s2 < s1) {
                        _machine.incrementBCP(_machine.getInlinedParameterShort(1) - 1);
                    } else {
                        _machine.incrementBCP(2);
                    }

                    break;
                }

                case IF_ICMPGE: {               // 0xA2;
                    final int s1 = _machine.pop().asInt();
                    final int s2 = _machine.pop().asInt();

                    if (s2 >= s1) {
                        _machine.incrementBCP(_machine.getInlinedParameterShort(1) - 1);
                    } else {
                        _machine.incrementBCP(2);
                    }

                    break;
                }

                case IF_ICMPGT: {               // 0xA3;
                    final int s1 = _machine.pop().asInt();
                    final int s2 = _machine.pop().asInt();

                    if (s2 > s1) {
                        _machine.incrementBCP(_machine.getInlinedParameterShort(1) - 1);
                    } else {
                        _machine.incrementBCP(2);
                    }

                    break;
                }

                case IF_ICMPLE: {               // 0xA4;
                    final int s1 = _machine.pop().asInt();
                    final int s2 = _machine.pop().asInt();

                    if (s2 <= s1) {
                        _machine.incrementBCP(_machine.getInlinedParameterShort(1) - 1);
                    } else {
                        _machine.incrementBCP(2);
                    }

                    break;
                }

                case IF_ACMPEQ: {               // 0xA5;
                    final ReferenceValue s1 = (ReferenceValue) _machine.pop();
                    final ReferenceValue s2 = (ReferenceValue) _machine.pop();

                    if (s2.equals(s1)) {
                        _machine.incrementBCP(_machine.getInlinedParameterShort(1) - 1);
                    } else {
                        _machine.incrementBCP(2);
                    }

                    break;
                }

                case IF_ACMPNE: {               // 0xA6;
                    final ReferenceValue s1 = (ReferenceValue) _machine.pop();
                    final ReferenceValue s2 = (ReferenceValue) _machine.pop();

                    if (!s2.equals(s1)) {
                        _machine.incrementBCP(_machine.getInlinedParameterShort(1) - 1);
                    } else {
                        _machine.incrementBCP(2);
                    }

                    break;
                }

                /*========================================================================*/

                case GOTO:                      // 0xA7;
                    _machine.incrementBCP(_machine.getInlinedParameterShort(1) - 1);
                    break;

                case JSR:                       // 0xA8;
                    _machine.push(IntValue.from(_machine.getBCP() + 3));
                    _machine.incrementBCP(_machine.getInlinedParameterShort(1) - 1);
                    break;

                case RET: {                     // 0xA9;
                    final int index = _machine.getInlinedParameterByte(1);
                    final int value = _machine.getLocal(index).asInt();

                    _machine.setBCP(value);
                    _machine.incrementBCP(1);
                    break;
                }

                /*========================================================================*/

                case TABLESWITCH: {             // 0xAA;
                    final int index   = _machine.pop().asInt();
                    final int bcp     = (((_machine.getBCP() + 1) / 4) + 1) * 4; //align to next 4-byte boundary
                    final int defawlt = _machine.getInlinedParameterIntN(bcp);
                    final int low     = _machine.getInlinedParameterIntN(bcp + 4);
                    final int high    = _machine.getInlinedParameterIntN(bcp + 8);

                    if (index < low || index > high) {
                        _machine.incrementBCP(defawlt - 1);
                    } else {
                        _machine.incrementBCP(_machine.getInlinedParameterIntN(bcp + 12 + ((index - low) * 4)) - 1);
                    }

                    break;
                }

                case LOOKUPSWITCH:              // 0xAB;
                lookup:
                    {
                        final int key     = _machine.pop().asInt();
                        final int bcp     = (((_machine.getBCP() + 1) / 4) + 1) * 4; //align to next 4-byte boundary
                        final int defawlt = _machine.getInlinedParameterIntN(bcp);
                        final int nPairs  = _machine.getInlinedParameterIntN(bcp + 4);

                        for (int i = 0; i < nPairs; i++) {
                            if (_machine.getInlinedParameterIntN((bcp + 8) + (i * 8)) == key) {
                                _machine.incrementBCP(_machine.getInlinedParameterIntN((bcp + 8) + (i * 8) + 4) - 1);
                                break lookup;
                            }
                        }

                        _machine.incrementBCP(defawlt - 1);
                    }
                    break;

                /*========================================================================*/

                case IRETURN:                   // 0xAC;
                case LRETURN:                   // 0xAD;
                case FRETURN:                   // 0xAE;
                case DRETURN:                   // 0xAF;
                case ARETURN: {                 // 0xB0;
                    final Value result = _machine.pop();
                    final ExecutionFrame frame = _machine.popFrame();

                    //if this was the topmost frame on the stack
                    if (frame == null) {
                        _ret = result;
                        return MethodStatus.METHOD_END;
                    }

                    _machine.push(result);
                    break;
                }

                case RETURN: {                  // 0xB1;
                    final ExecutionFrame frame = _machine.popFrame();

                    //if this was the topmost frame on the stack
                    if (frame == null) {
                        _ret = null;
                        return MethodStatus.METHOD_END;
                    }

                    break;
                }

                /*========================================================================*/

                case GETSTATIC: {               // 0xB2;
                    final short cpIndex = _machine.getInlinedParameterShort(1);

                    try {
                        _machine.push(_machine.getStatic(cpIndex));
                    } catch (LinkageError e) {
                        _machine.throwException(e);
                    }

                    _machine.incrementBCP(2);
                    break;
                }

                case PUTSTATIC: {                // 0xB5;
                    final short cpIndex = _machine.getInlinedParameterShort(1);

                    try {
                        _machine.putStatic(cpIndex, _machine.pop());
                    } catch (LinkageError e) {
                        _machine.throwException(e);
                    }

                    _machine.incrementBCP(2);
                    break;
                }

                case GETFIELD: {                // 0xB4;
                    final Reference instance = _machine.pop().asReference();
                    final short cpIndex = _machine.getInlinedParameterShort(1);

                    if (instance == null) {
                        _machine.throwException(new NullPointerException());
                    }

                    try {
                        _machine.push(_machine.getField(instance, cpIndex));
                    } catch (LinkageError e) {
                        _machine.throwException(e);
                    }

                    _machine.incrementBCP(2);
                    break;
                }

                case PUTFIELD: {                // 0xB5;
                    final Value value = _machine.pop();
                    final Object instance = _machine.pop().asBoxedJavaValue();
                    final short cpIndex = _machine.getInlinedParameterShort(1);

                    if (instance == null) {
                        _machine.throwException(new NullPointerException());
                    }

                    try {
                        _machine.putField(instance, cpIndex, value);
                    } catch (LinkageError e) {
                        _machine.throwException(e);
                    }

                    _machine.incrementBCP(2);
                    break;
                }

                /*========================================================================*/

                case INVOKEVIRTUAL: {           // 0xB6;
                    final short cpIndex = _machine.getInlinedParameterShort(1);

                    try {
                        ClassMethodActor methodActor = (ClassMethodActor) _machine.resolveMethod(cpIndex);
                        final Value value = _machine.peek(methodActor.descriptor().getNumberOfParameters() + 1);
                        if (value instanceof ReferenceValue) {
                            final ReferenceValue receiver = (ReferenceValue) value;
                            if (receiver.isZero()) {
                                _machine.throwException(new NullPointerException());
                            }

                            final ClassActor dynamicClass = receiver.getClassActor();

                            methodActor = dynamicClass.findVirtualMethodActor(methodActor);
                        }
                        _machine.incrementBCP(2);

                        if (methodActor == null) {
                            _machine.throwException(new AbstractMethodError());
                        } else if (methodActor.isAbstract()) {
                            _machine.throwException(new AbstractMethodError());
                        }

                        _machine.invokeMethod(methodActor);
                    } catch (LinkageError e) {
                        _machine.throwException(e);
                    }

                    break;
                }

                case INVOKESPECIAL: {           // 0xB7;
                    final short cpIndex = _machine.getInlinedParameterShort(1);

                    try {
                        final ClassMethodActor methodActor = (ClassMethodActor) _machine.resolveMethod(cpIndex);
                        final ReferenceValue receiver = (ReferenceValue) _machine.peek(methodActor.descriptor().getNumberOfParameters() + 1);

                        if (receiver.isZero()) {
                            _machine.throwException(new NullPointerException());
                        }

                        _machine.incrementBCP(2);
                        _machine.invokeMethod(methodActor);
                    } catch (LinkageError e) {
                        _machine.throwException(e);
                    }

                    break;
                }

                case INVOKESTATIC: {            // 0xB8;
                    final short cpIndex = _machine.getInlinedParameterShort(1);

                    try {
                        final ClassMethodActor methodActor = (ClassMethodActor) _machine.resolveMethod(cpIndex);

                        _machine.incrementBCP(2);
                        _machine.invokeMethod(methodActor);
                    } catch (LinkageError e) {
                        _machine.throwException(e);
                    }

                    break;
                }

                case INVOKEINTERFACE: {         // 0xB9;
                    final short cpIndex = _machine.getInlinedParameterShort(1);

                    try {
                        final InterfaceMethodActor methodActor = (InterfaceMethodActor) _machine.resolveMethod(cpIndex);
                        final ReferenceValue receiver = (ReferenceValue) _machine.peek(methodActor.descriptor().getNumberOfParameters() + 1);

                        if (receiver.isZero()) {
                            _machine.throwException(new NullPointerException());
                        }

                        final ClassActor dynamicClass = receiver.getClassActor();

                        if (!dynamicClass.getAllInterfaceActors().contains((InterfaceActor) methodActor.holder())) {
                            _machine.throwException(new IncompatibleClassChangeError());
                        }

                        _machine.incrementBCP(4);
                        final VirtualMethodActor dynamicMethodActor = dynamicClass.findVirtualMethodActor(methodActor);

                        if (dynamicMethodActor == null) {
                            _machine.throwException(new AbstractMethodError("No such method " + methodActor + " found in " + dynamicClass));
                        } else if (dynamicMethodActor.isAbstract()) {
                            _machine.throwException(new AbstractMethodError("Method " + dynamicMethodActor + " is abstract in " + dynamicClass));
                        } else if (!dynamicMethodActor.isPublic()) {
                            _machine.throwException(new IllegalAccessError("Method " + dynamicMethodActor + " is not public in " + dynamicClass));
                        }

                        _machine.invokeMethod(dynamicMethodActor);
                    } catch (LinkageError e) {
                        _machine.throwException(e);
                    }

                    break;
                }

                /*========================================================================*/

                case XXXUNUSEDXXX:              // 0xBA;
                    break;

                /*========================================================================*/

                case NEW:                       // 0xBB;
                    _machine.push(TeleReferenceValue.from(_teleVM, Reference.fromJava(new Object())));
                    _machine.incrementBCP(2);
                    break;

                case NEWARRAY: {                // 0xBC;
                    final byte arrayType = _machine.getInlinedParameterByte(1);
                    final int arraySize  = _machine.pop().asInt();

                    if (arraySize < 0) {
                        _machine.throwException(new NegativeArraySizeException());
                    }

                    switch (arrayType) {
                        case 4:
                            _machine.push(ReferenceValue.from(new boolean[arraySize]));
                            break;
                        case 5:
                            _machine.push(ReferenceValue.from(new char[arraySize]));
                            break;
                        case 6:
                            _machine.push(ReferenceValue.from(new float[arraySize]));
                            break;
                        case 7:
                            _machine.push(ReferenceValue.from(new double[arraySize]));
                            break;
                        case 8:
                            _machine.push(ReferenceValue.from(new byte[arraySize]));
                            break;
                        case 9:
                            _machine.push(ReferenceValue.from(new short[arraySize]));
                            break;
                        case 10:
                            _machine.push(ReferenceValue.from(new int[arraySize]));
                            break;
                        case 11:
                            _machine.push(ReferenceValue.from(new long[arraySize]));
                            break;
                    }

                    _machine.incrementBCP(1);
                    break;
                }

                case ANEWARRAY: {               // 0xBB;
                    final int arraySize = _machine.pop().asInt();

                    if (arraySize < 0) {
                        _machine.throwException(new NegativeArraySizeException());
                    }

                    _machine.push(ReferenceValue.from(new Object[arraySize]));
                    _machine.incrementBCP(2);
                    break;
                }


                case ARRAYLENGTH: {             // 0xBE;
                    final Reference array = _machine.pop().asReference();

                    if (array == null) {
                        _machine.throwException(new NullPointerException());
                    }

                    _machine.push(IntValue.from(Layout.readArrayLength(array)));
                    break;
                }

                /*========================================================================*/

                case ATHROW: {                  // 0xBF;
                    final ReferenceValue t = (ReferenceValue) _machine.pop();

                    if (t.isZero()) {
                        _machine.throwException(new NullPointerException());
                    } else {
                        _machine.throwException(t);
                    }

                    break;
                }

                case CHECKCAST: {               // 0xC0;
                    final short cpIndex = _machine.getInlinedParameterShort(1);
                    final ClassActor classActor = _machine.resolveClassReference(cpIndex);
                    final ReferenceValue object = (ReferenceValue) _machine.pop();

                    if (!object.isZero()) {
                        if (!classActor.isAssignableFrom(object.getClassActor())) {
                            final String message = object.getClassActor().toJava() + " is not a subclass of " + classActor;
                            _machine.throwException(new ClassCastException(message));
                        }
                    }

                    _machine.push(object);
                    _machine.incrementBCP(2);
                    break;
                }

                case INSTANCEOF: {              // 0xC1;
                    final short cpIndex = _machine.getInlinedParameterShort(1);
                    final ClassActor classActor = _machine.resolveClassReference(cpIndex);
                    final ReferenceValue object = (ReferenceValue) _machine.pop();

                    if (object.isZero() || !classActor.isAssignableFrom(object.getClassActor())) {
                        _machine.push(IntValue.from(0));
                    } else {
                        _machine.push(IntValue.from(1));
                    }

                    _machine.incrementBCP(2);
                    break;
                }

                /*========================================================================*/

                case MONITORENTER:              // 0xC2;
                case MONITOREXIT:               // 0xC3;
                    _machine.pop();
                    break;

                /*========================================================================*/

                case WIDE: {                    // 0xC4;
                    final Bytecode nextCode = _machine.nextInstruction();
                    final short index = _machine.getInlinedParameterShort(1);

                    switch (nextCode) {
                        case ILOAD:
                        case FLOAD:
                        case ALOAD:
                        case LLOAD:
                        case DLOAD:
                            _machine.push(_machine.getLocal(index));
                            break;

                        case ISTORE:
                        case FSTORE:
                        case ASTORE:
                        case LSTORE:
                        case DSTORE:
                            _machine.setLocal(index, _machine.pop());
                            break;

                        case IINC: {
                            _machine.incrementBCP(2);
                            final int increment = _machine.getInlinedParameterShort(1);
                            final int value = _machine.getLocal(index).asInt();

                            _machine.setLocal(index, IntValue.from(value + increment));
                            break;
                        }

                        case RET: {
                            final Value value = _machine.getLocal(index);
                            _machine.setBCP(value.asInt());
                            break;
                        }

                        default:
                            _machine.throwException(new ClassFormatError("Illegal wide bytecode encountered"));
                    }

                    break;
                }

                /*========================================================================*/

                case MULTIANEWARRAY: {          // 0xC5;
                    final short cpIndex = _machine.getInlinedParameterShort(1);
                    final TypeDescriptor type = _machine.resolveClassReference(cpIndex).componentClassActor().typeDescriptor();
                    final int dim = (short) (_machine.getInlinedParameterByte(3) & 0x7F);
                    final int[] dimensions = new int[dim];

                    _machine.incrementBCP(3);

                    for (int i = 0; i < dim; i++) {
                        dimensions[i] = _machine.pop().asInt();

                        if (dimensions[i] < 0) {
                            _machine.throwException(new NegativeArraySizeException());
                        }
                    }

                    _machine.push(ReferenceValue.from(createMultiDimArray(type, dim, dimensions)));
                    break;
                }

                /*========================================================================*/

                case IFNULL: {                  // 0xC6;
                    final Value r = _machine.pop();

                    if (r.isZero()) {
                        _machine.incrementBCP(_machine.getInlinedParameterShort(1) - 1);
                    } else {
                        _machine.incrementBCP(2);
                    }

                    break;
                }

                case IFNONNULL: {               // 0xC7;
                    final Value r = _machine.pop();

                    if (!r.isZero()) {
                        _machine.incrementBCP(_machine.getInlinedParameterShort(1) - 1);
                    } else {
                        _machine.incrementBCP(2);
                    }

                    break;
                }

                /*========================================================================*/

                case GOTO_W:                    // 0xC8;
                    _machine.incrementBCP(_machine.getInlinedParameterInt(1) - 1);
                    break;

                case JSR_W:                     // 0xC9;
                    _machine.push(IntValue.from(_machine.getBCP() + 5));
                    _machine.incrementBCP(_machine.getInlinedParameterInt(1) - 1);
                    break;

                /*========================================================================*/

                case BREAKPOINT:                // 0xCA;
                    break;

                /*========================================================================*/

                default:
                    System.err.println("Unsupported bytecode: " + code);
                    _ret = ReferenceValue.NULL;
                    return MethodStatus.METHOD_ABORT;
            }
        } catch (Machine.InterpretedException e) {
            return MethodStatus.METHOD_EXCEPTION;
        }

        return MethodStatus.METHOD_CONTINUE;
    }

    public static enum MethodStatus {
        METHOD_END,
        METHOD_CONTINUE,
        METHOD_EXCEPTION,
        METHOD_ABORT,
    }

    private static Object createMultiDimArray(TypeDescriptor type, int dim, int[] dimensions) {
        final int dimensionSize = dimensions[dim - 1];
        final Object[] newArray = new Object[dimensionSize];

        if (dim > 1) {
            for (int i = 0; i < dimensionSize; i++) {
                newArray[i] = createMultiDimArray(type, dim - 1, dimensions);
            }
        } else if (dim == 1) {
            if (type == JavaTypeDescriptor.BOOLEAN) {
                return new boolean[dimensionSize];
            } else if (type == JavaTypeDescriptor.BYTE) {
                return new byte[dimensionSize];
            } else if (type == JavaTypeDescriptor.CHAR) {
                return new char[dimensionSize];
            } else if (type == JavaTypeDescriptor.DOUBLE) {
                return new double[dimensionSize];
            } else if (type == JavaTypeDescriptor.FLOAT) {
                return new float[dimensionSize];
            } else if (type == JavaTypeDescriptor.INT) {
                return new int[dimensionSize];
            } else if (type == JavaTypeDescriptor.LONG) {
                return new long[dimensionSize];
            } else if (type == JavaTypeDescriptor.SHORT) {
                return new short[dimensionSize];
            }
        }

        return newArray;
    }

    public Value ret() {
        return _ret;
    }

}
