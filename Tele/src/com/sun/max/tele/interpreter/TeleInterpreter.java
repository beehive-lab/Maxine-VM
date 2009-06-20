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

import java.io.*;
import java.lang.reflect.*;
import java.util.*;

import com.sun.max.lang.*;
import com.sun.max.program.*;
import com.sun.max.tele.*;
import com.sun.max.tele.reference.*;
import com.sun.max.tele.value.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.bytecode.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.compiler.ir.*;
import com.sun.max.vm.interpreter.*;
import com.sun.max.vm.layout.*;
import com.sun.max.vm.prototype.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;

/**
 * Entry points and utility functions for the interpreter.
 *
 * @author Athul Acharya
 * @author Doug Simon
 */
public final class TeleInterpreter extends IrInterpreter<ActorIrMethod> {

    private final TeleVM _teleVM;

    private Machine _machine;
    private Value _returnValue;
    private int _instructionsExecuted;

    public TeleInterpreter(TeleVM teleVM) {
        _teleVM = teleVM;
    }

    @Override
    public Value execute(ActorIrMethod method, Value... arguments) throws InvocationTargetException {
        return run(method.classMethodActor(), arguments);
    }


    /**
     * Creates an interpreter instance and uses it to execute a given method with the given arguments.
     *
     * @param teleVM the remote VM
     * @param classMethodActor the method to be executed
     * @param args the arguments to passed to the method for execution
     * @return the result of the execution
     * @throws TeleInterpreterException if an uncaught exception occurs during execution of the method
     */
    public static Value execute(TeleVM teleVM, ClassMethodActor classMethodActor, Value... args) throws TeleInterpreterException {
        return new TeleInterpreter(teleVM).run(classMethodActor, args);
    }

    /**
     * Creates an interpreter instance and uses it to execute a given method with the given arguments.
     *
     * @param teleVM the remote VM
     * @param declaringClassName the name of the class that declares the method to be executed
     * @param name the name of the method to be executed
     * @param signature the signature of the method to be executed
     * @param args the arguments to passed to the method for execution
     * @return the result of the execution
     * @throws TeleInterpreterException if an uncaught exception occurs during execution of the method
     * @throws NoSuchMethodError if the specified method cannot be found
     */
    public static Value execute(TeleVM teleVM, String declaringClassName, String name, SignatureDescriptor signature, Value... args) throws TeleInterpreterException {
        ClassActor classActor;
        ClassMethodActor classMethodActor;

        classActor = PrototypeClassLoader.PROTOTYPE_CLASS_LOADER.mustMakeClassActor(JavaTypeDescriptor.getDescriptorForJavaString(declaringClassName));
        classMethodActor = classActor.findClassMethodActor(SymbolTable.makeSymbol(name), signature);

        if (classMethodActor == null) {
            throw new NoSuchMethodError(declaringClassName + "." + name + signature);
        }

        return execute(teleVM, classMethodActor, args);
    }

    /**
     * Creates an interpreter instance and uses it to execute a given method with the given arguments.
     *
     * @param teleVM the remote VM
     * @param declaringClass the class that declares the method to be executed
     * @param name the name of the method to be executed
     * @param signature the signature of the method to be executed
     * @param args the arguments to passed to the method for execution
     * @return the result of the execution
     * @throws TeleInterpreterException if an uncaught exception occurs during execution of the method
     */
    public static Value execute(TeleVM teleVM, Class declaringClass, String name, SignatureDescriptor signature, Value... args) throws TeleInterpreterException {
        ClassActor classActor;
        ClassMethodActor classMethodActor;

        classActor = PrototypeClassLoader.PROTOTYPE_CLASS_LOADER.mustMakeClassActor(JavaTypeDescriptor.forJavaClass(declaringClass));
        classMethodActor = classActor.findClassMethodActor(SymbolTable.makeSymbol(name), signature);

        if (classMethodActor == null) {
            throw new NoSuchMethodError(declaringClass.getName() + "." + name + signature);
        }

        return execute(teleVM, classMethodActor, args);
    }

    /**
     * A lazy constructed cache of mappings from opcode positions to disassembled bytecode instructions.
     */
    private Map<MethodActor, Map<Integer, String>> _bytecodeTraces;

    /**
     * The last frame traced in {@link #traceExecution()}.
     */
    private ExecutionFrame _lastTracedFrame;

    /**
     * Traces the current execution point.
     */
    private void traceExecution() {
        if (Trace.hasLevel(2)) {
            final PrintStream stream = Trace.stream();
            final ExecutionFrame frame = _machine.currentThread().frame();

            final int depth = frame.depth();
            if (_lastTracedFrame == null) {
                stream.println("Interpreter: " + Strings.spaces(depth * 2) + "ENTERING: " + frame.method().format("%H.%n(%p)"));
            } else if (_lastTracedFrame != frame) {
                final int lastFrameDepth = _lastTracedFrame.depth();
                if (lastFrameDepth < depth) {
                    stream.println("Interpreter: " + Strings.spaces(depth * 2) + "ENTERING: " + frame.method().format("%H.%n(%p)"));
                } else {
                    stream.println("Interpreter: " + Strings.spaces(lastFrameDepth * 2) + "EXITING: " + _lastTracedFrame.method().format("%H.%n(%p)"));
                }
            }
            if (Trace.hasLevel(3)) {
                if (_bytecodeTraces == null) {
                    _bytecodeTraces = new HashMap<MethodActor, Map<Integer, String>>();
                }
                Map<Integer, String> bcpToTrace = _bytecodeTraces.get(_machine.currentMethod());
                if (bcpToTrace == null) {
                    bcpToTrace = new HashMap<Integer, String>();
                    _bytecodeTraces.put(_machine.currentMethod(), bcpToTrace);
                    final ConstantPool constantPool = frame.constantPool();
                    final BytecodeBlock bytecodeBlock = new BytecodeBlock(frame.code());
                    final String[] instructions = BytecodePrinter.toString(constantPool, bytecodeBlock, "", "\0", 0).split("\0");
                    for (String instruction : instructions) {
                        final int colonIndex = instruction.indexOf(':');
                        assert colonIndex != -1 : "instruction trace does not start with expected '<bcp>:': " + instruction;
                        try {
                            final int bcp = Integer.parseInt(instruction.substring(0, colonIndex));
                            bcpToTrace.put(bcp, instruction);
                        } catch (NumberFormatException numberFormatException) {
                            ProgramWarning.message("instruction trace does not start with expected '<bcp>:': " + instruction);
                        }
                    }
                }

                stream.println("Interpreter: " + Strings.spaces(depth * 2) + bcpToTrace.get(frame.currentOpcodePosition()));
            }
            _lastTracedFrame = frame;
            stream.flush();
        }
    }

    private Value run(ClassMethodActor classMethodActor, Value... arguments) throws TeleInterpreterException {

        _machine = new Machine(_teleVM);
        _machine.pushFrame(classMethodActor);
        int j = 0;
        for (int i = 0; i < arguments.length; i++, j++) {
            _machine.setLocal(j, arguments[i]);
            if (arguments[i].isCategory2()) {
                j++;
            }
        }

        Bytecode code;
        MethodStatus status;
        _instructionsExecuted = 0;

        while (true) {
            code = _machine.readOpcode();

            traceExecution();

            try {
                status = interpret(code);
                if (status == MethodStatus.METHOD_END) {
                    break;
                }
            } catch (TeleInterpreterException executionException) {
                final ReferenceValue throwableReference = executionException.throwableReference();
                final boolean handled = _machine.handleException(throwableReference); //if this succeeds we keep looping
                if (!handled) {
                    throw executionException;
                }
            } catch (Throwable throwable) {
                throw new TeleInterpreterException(throwable, _machine);
            } finally {
                _instructionsExecuted++;
            }
        }

        if (_returnValue instanceof TeleReferenceValue) {
            _returnValue = TeleReferenceValue.from(_teleVM, _machine.makeLocalReference((TeleReference) _returnValue.asReference()));
        }

        final Kind resultKind = classMethodActor.resultKind();
        if (resultKind.toStackKind() == Kind.INT) {
            _returnValue = resultKind.convert(_returnValue);
        }
        return _returnValue;
    }

    private MethodStatus interpret(Bytecode opcode) throws TeleInterpreterException {
        switch (opcode) {
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
                _machine.push(IntValue.from(_machine.readByte()));
                break;

            case SIPUSH:                    // 0x11;
                _machine.push(IntValue.from(_machine.readShort()));
                break;

                /*========================================================================*/

            case LDC: {                     // 0x12;
                final byte cpIndex = _machine.readByte();
                _machine.push(_machine.resolveConstantReference((short) (cpIndex & 0xFF)));
                break;
            }

            case LDC_W:                     // 0x13;
            case LDC2_W: {                  // 0x14;
                final short cpIndex = _machine.readShort();
                _machine.push(_machine.resolveConstantReference(cpIndex));
                break;
            }

            /*========================================================================*/

            case ILOAD:                     // 0x15;
            case LLOAD:                     // 0x16;
            case FLOAD:                     // 0x17;
            case DLOAD:                     // 0x18;
            case ALOAD:                     // 0x19;
                _machine.push(_machine.getLocal(_machine.readByte()));
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

                if (array.isZero()) {
                    _machine.raiseException(new NullPointerException());
                }

                if (Layout.readArrayLength(array) <= index || index < 0) {
                    _machine.raiseException(new ArrayIndexOutOfBoundsException());
                }

                final int val = Layout.getInt(array, index);

                // Push value to operand stack
                _machine.push(IntValue.from(val));
                break;
            }

            case LALOAD: {                  // 0x2F;
                final int index = _machine.pop().asInt();                // Get array index (IntValue)
                final Reference array = _machine.pop().asReference();    // Get the array (ReferenceValue)

                if (array.isZero()) {
                    _machine.raiseException(new NullPointerException());
                }

                if (Layout.readArrayLength(array) <= index || index < 0) {
                    _machine.raiseException(new ArrayIndexOutOfBoundsException());
                }

                final long val = Layout.getLong(array, index);

                // Push value to operand stack
                _machine.push(LongValue.from(val));
                break;
            }

            case FALOAD: {                  // 0x30;
                final int index = _machine.pop().asInt();                // Get array index (IntValue)
                final Reference array = _machine.pop().asReference();    // Get the array (ReferenceValue)

                if (array.isZero()) {
                    _machine.raiseException(new NullPointerException());
                }

                if (Layout.readArrayLength(array) <= index || index < 0) {
                    _machine.raiseException(new ArrayIndexOutOfBoundsException());
                }

                final float val = Layout.getFloat(array, index);

                // Push value to operand stack
                _machine.push(FloatValue.from(val));
                break;
            }

            case DALOAD: {                  // 0x31;
                final int index = _machine.pop().asInt();                // Get array index (IntValue)
                final Reference array = _machine.pop().asReference();    // Get the array (ReferenceValue)

                if (array.isZero()) {
                    _machine.raiseException(new NullPointerException());
                }

                if (Layout.readArrayLength(array) <= index || index < 0) {
                    _machine.raiseException(new ArrayIndexOutOfBoundsException());
                }

                final double val = Layout.getDouble(array, index);

                // Push value to operand stack
                _machine.push(DoubleValue.from(val));
                break;
            }

            case AALOAD: {                  // 0x32;
                final int index = _machine.pop().asInt();                // Get array index (IntValue)
                final Reference array = _machine.pop().asReference();    // Get the array (ReferenceValue)

                if (array.isZero()) {
                    _machine.raiseException(new NullPointerException());
                }

                if (Layout.readArrayLength(array) <= index || index < 0) {
                    _machine.raiseException(new ArrayIndexOutOfBoundsException());
                }

                final Reference val = Layout.getReference(array, index);

                // Push value to operand stack
                _machine.push(_machine.toReferenceValue(val));
                break;
            }

            case BALOAD: {                  // 0x33;
                final int index = _machine.pop().asInt();                  // Get array index (IntValue)
                final Reference array = _machine.pop().asReference();    // Get the array (ReferenceValue)

                if (array.isZero()) {
                    _machine.raiseException(new NullPointerException());
                }

                if (Layout.readArrayLength(array) <= index || index < 0) {
                    _machine.raiseException(new ArrayIndexOutOfBoundsException());
                }

                final IntValue val;

                if (_machine.toReferenceValue(array).getClassActor() == PrimitiveClassActor.BOOLEAN_ARRAY_CLASS_ACTOR) {
                    final boolean booleanVal = Layout.getBoolean(array, index);
                    val = booleanVal ? IntValue.ONE : IntValue.ZERO;
                } else {
                    final byte byteVal = Layout.getByte(array, index);
                    val = IntValue.from(byteVal);
                }

                // Push value to operand stack
                _machine.push(val);
                break;
            }

            case CALOAD: {                   // 0x34;
                final int index = _machine.pop().asInt();                // Get array index (IntValue)
                final Reference array = _machine.pop().asReference();    // Get the array (ReferenceValue)

                if (array.isZero()) {
                    _machine.raiseException(new NullPointerException());
                }

                if (Layout.readArrayLength(array) <= index || index < 0) {
                    _machine.raiseException(new ArrayIndexOutOfBoundsException());
                }

                final char val = Layout.getChar(array, index);

                // Push value to operand stack
                _machine.push(IntValue.from(val));
                break;
            }

            case SALOAD: {                   // 0x35;
                final int index = _machine.pop().asInt();                  // Get array index (IntValue)
                final Reference array = _machine.pop().asReference();    // Get the array (ReferenceValue)

                if (array.isZero()) {
                    _machine.raiseException(new NullPointerException());
                }

                if (Layout.readArrayLength(array) <= index || index < 0) {
                    _machine.raiseException(new ArrayIndexOutOfBoundsException());
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
                _machine.setLocal(_machine.readByte(), _machine.pop());
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

                if (array.isZero()) {
                    _machine.raiseException(new NullPointerException());
                }

                if (Layout.readArrayLength(array) <= index || index < 0) {
                    _machine.raiseException(new ArrayIndexOutOfBoundsException());
                }

                Layout.setInt(array, index, val);
                break;
            }

            case LASTORE: {                 // 0x50;
                final long val = _machine.pop().toLong();                // Get value to store
                final int index = _machine.pop().toInt();                // Get array index
                final Reference array = _machine.pop().asReference();    // Get the array (ReferenceValue)

                if (array.isZero()) {
                    _machine.raiseException(new NullPointerException());
                }

                if (Layout.readArrayLength(array) <= index || index < 0) {
                    _machine.raiseException(new ArrayIndexOutOfBoundsException());
                }

                Layout.setLong(array, index, val);
                break;
            }

            case FASTORE: {                 // 0x51;
                final float val = _machine.pop().toFloat();              // Get value to store
                final int index = _machine.pop().toInt();                // Get array index
                final Reference array = _machine.pop().asReference();    // Get the array (ReferenceValue)

                if (array.isZero()) {
                    _machine.raiseException(new NullPointerException());
                }

                if (Layout.readArrayLength(array) <= index || index < 0) {
                    _machine.raiseException(new ArrayIndexOutOfBoundsException());
                }

                Layout.setFloat(array, index, val);
                break;
            }

            case DASTORE: {                 // 0x52;
                final double val = _machine.pop().toDouble();            // Get value to store
                final int index = _machine.pop().toInt();                // Get array index
                final Reference array = _machine.pop().asReference();    // Get the array (ReferenceValue)

                if (array.isZero()) {
                    _machine.raiseException(new NullPointerException());
                }

                if (Layout.readArrayLength(array) <= index || index < 0) {
                    _machine.raiseException(new ArrayIndexOutOfBoundsException());
                }

                Layout.setDouble(array, index, val);
                break;
            }

            case AASTORE: {                 // 0x53;
                final Reference val = _machine.pop().asReference();      // Get value to store
                final int index = _machine.pop().toInt();                // Get array index
                final Reference array = _machine.pop().asReference();    // Get the array (ReferenceValue)

                if (array.isZero()) {
                    _machine.raiseException(new NullPointerException());
                }

                if (Layout.readArrayLength(array) <= index || index < 0) {
                    _machine.raiseException(new ArrayIndexOutOfBoundsException());
                }

                Layout.setReference(array, index, val);
                break;
            }

            case BASTORE: {                 // 0x54;
                final int val = _machine.pop().toInt();                  // Get value to store
                final int index = _machine.pop().toInt();                // Get array index
                final Reference array = _machine.pop().asReference();    // Get the array (ReferenceValue)

                if (array.isZero()) {
                    _machine.raiseException(new NullPointerException());
                }

                if (Layout.readArrayLength(array) <= index || index < 0) {
                    _machine.raiseException(new ArrayIndexOutOfBoundsException());
                }

                Layout.setByte(array, index, (byte) val);
                break;
            }

            case CASTORE: {                 // 0x55;
                final int val = _machine.pop().toInt();                  // Get value to store
                final int index = _machine.pop().toInt();                // Get array index
                final Reference array = _machine.pop().asReference();    // Get the array (ReferenceValue)

                if (array.isZero()) {
                    _machine.raiseException(new NullPointerException());
                }

                if (Layout.readArrayLength(array) <= index || index < 0) {
                    _machine.raiseException(new ArrayIndexOutOfBoundsException());
                }

                Layout.setChar(array, index, (char) val);
                break;
            }

            case SASTORE: {                 // 0x56;
                final int val = _machine.pop().toInt();                  // Get value to store
                final int index = _machine.pop().toInt();                // Get array index
                final Reference array = _machine.pop().asReference();    // Get the array (ReferenceValue)

                if (array.isZero()) {
                    _machine.raiseException(new NullPointerException());
                }

                if (Layout.readArrayLength(array) <= index || index < 0) {
                    _machine.raiseException(new ArrayIndexOutOfBoundsException());
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
                    _machine.raiseException(new ArithmeticException("Division by zero"));
                }

                _machine.push(IntValue.from(v2 / v1));
                break;
            }

            case LDIV: {                    // 0x6D;
                final long v1 = _machine.pop().asLong();
                final long v2 = _machine.pop().asLong();

                if (v1 == 0L) {
                    _machine.raiseException(new ArithmeticException("Division by zero"));
                }

                _machine.push(LongValue.from(v2 / v1));
                break;
            }

            case FDIV: {                    // 0x6E;
                final float v1 = _machine.pop().asFloat();
                final float v2 = _machine.pop().asFloat();

                if (v1 == 0.0) {
                    _machine.raiseException(new ArithmeticException("Division by zero"));
                }

                _machine.push(FloatValue.from(v2 / v1));
                break;
            }

            case DDIV: {                    // 0x6F;
                final double v1 = _machine.pop().asDouble();
                final double v2 = _machine.pop().asDouble();

                if (v1 == 0.0D) {
                    _machine.raiseException(new ArithmeticException("Division by zero"));
                }

                _machine.push(DoubleValue.from(v2 / v1));
                break;
            }

            /*========================================================================*/

            case IREM: {                    // 0x70;
                final int v1 = _machine.pop().asInt();
                final int v2 = _machine.pop().asInt();

                if (v1 == 0) {
                    _machine.raiseException(new ArithmeticException("Division by zero"));
                }

                _machine.push(IntValue.from(v2 % v1));
                break;
            }

            case LREM: {                    // 0x71;
                final long v1 = _machine.pop().asLong();
                final long v2 = _machine.pop().asLong();

                if (v1 == 0L) {
                    _machine.raiseException(new ArithmeticException("Division by zero"));
                }

                _machine.push(LongValue.from(v2 % v1));
                break;
            }

            case FREM: {                    // 0x72;
                final float v1 = _machine.pop().asFloat();
                final float v2 = _machine.pop().asFloat();

                if (v1 == 0.0) {
                    _machine.raiseException(new ArithmeticException("Division by zero"));
                }

                _machine.push(FloatValue.from(v2 % v1));
                break;
            }

            case DREM: {                    // 0x73;
                final double v1 = _machine.pop().asDouble();
                final double v2 = _machine.pop().asDouble();

                if (v1 == 0.0D) {
                    _machine.raiseException(new ArithmeticException("Division by zero"));
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
                final int index     = _machine.readByte();
                final int increment = _machine.readByte();
                final int value     = _machine.getLocal(index).asInt();

                _machine.setLocal(index, IntValue.from(value + increment));
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
                final short offset = _machine.readShort();
                final int s1 = _machine.pop().asInt();
                if (s1 == 0) {
                    _machine.jump(offset);
                }
                break;
            }

            case IFNE: {                    // 0x9A;
                final short offset = _machine.readShort();
                final int s1 = _machine.pop().asInt();
                if (s1 != 0) {
                    _machine.jump(offset);
                }
                break;
            }

            case IFLT: {                    // 0x9B;
                final short offset = _machine.readShort();
                final int s1 = _machine.pop().asInt();
                if (s1 < 0) {
                    _machine.jump(offset);
                }
                break;
            }

            case IFGE: {                    // 0x9C;
                final short offset = _machine.readShort();
                final int s1 = _machine.pop().asInt();
                if (s1 >= 0) {
                    _machine.jump(offset);
                }
                break;
            }

            case IFGT: {                    // 0x9D;
                final short offset = _machine.readShort();
                final int s1 = _machine.pop().asInt();
                if (s1 > 0) {
                    _machine.jump(offset);
                }
                break;
            }

            case IFLE: {                    // 0x9E;
                final short offset = _machine.readShort();
                final int s1 = _machine.pop().asInt();
                if (s1 <= 0) {
                    _machine.jump(offset);
                }
                break;
            }

            case IF_ICMPEQ: {               // 0x9F;
                final short offset = _machine.readShort();
                final int s1 = _machine.pop().asInt();
                final int s2 = _machine.pop().asInt();
                if (s2 == s1) {
                    _machine.jump(offset);
                }
                break;
            }

            case IF_ICMPNE: {               // 0xA0;
                final short offset = _machine.readShort();
                final int s1 = _machine.pop().asInt();
                final int s2 = _machine.pop().asInt();
                if (s2 != s1) {
                    _machine.jump(offset);
                }
                break;
            }

            case IF_ICMPLT: {               // 0xA1;
                final short offset = _machine.readShort();
                final int s1 = _machine.pop().asInt();
                final int s2 = _machine.pop().asInt();
                if (s2 < s1) {
                    _machine.jump(offset);
                }
                break;
            }

            case IF_ICMPGE: {               // 0xA2;
                final short offset = _machine.readShort();
                final int s1 = _machine.pop().asInt();
                final int s2 = _machine.pop().asInt();
                if (s2 >= s1) {
                    _machine.jump(offset);
                }
                break;
            }

            case IF_ICMPGT: {               // 0xA3;
                final short offset = _machine.readShort();
                final int s1 = _machine.pop().asInt();
                final int s2 = _machine.pop().asInt();
                if (s2 > s1) {
                    _machine.jump(offset);
                }
                break;
            }

            case IF_ICMPLE: {               // 0xA4;
                final short offset = _machine.readShort();
                final int s1 = _machine.pop().asInt();
                final int s2 = _machine.pop().asInt();
                if (s2 <= s1) {
                    _machine.jump(offset);
                }
                break;
            }

            case IF_ACMPEQ: {               // 0xA5;
                final short offset = _machine.readShort();
                final ReferenceValue s1 = (ReferenceValue) _machine.pop();
                final ReferenceValue s2 = (ReferenceValue) _machine.pop();
                if (s2.equals(s1)) {
                    _machine.jump(offset);
                }
                break;
            }

            case IF_ACMPNE: {               // 0xA6;
                final short offset = _machine.readShort();
                final ReferenceValue s1 = (ReferenceValue) _machine.pop();
                final ReferenceValue s2 = (ReferenceValue) _machine.pop();

                if (!s2.equals(s1)) {
                    _machine.jump(offset);
                }
                break;
            }

            /*========================================================================*/

            case GOTO: {                    // 0xA7;
                final short offset = _machine.readShort();
                _machine.jump(offset);
                break;
            }
            case JSR: {                      // 0xA8;
                final short offset = _machine.readShort();
                final int returnPosition = _machine.currentThread().frame().currentBytePosition();
                _machine.push(IntValue.from(returnPosition));
                _machine.jump(offset);
                break;
            }
            case RET: {                     // 0xA9;
                final int index = _machine.readByte();
                final int value = _machine.getLocal(index).asInt();

                _machine.currentThread().frame().setBytecodePosition(value);
                break;
            }

            /*========================================================================*/

            case TABLESWITCH: {             // 0xAA;
                final int index   = _machine.pop().asInt();
                _machine.alignInstructionPosition();
                final int defawlt = _machine.readInt();
                final int low     = _machine.readInt();
                final int high    = _machine.readInt();

                if (index < low || index > high) {
                    _machine.jump(defawlt);
                } else {
                    final int jumpTableIndex = index - low;
                    _machine.skipBytes(jumpTableIndex * 4);
                    final int offset = _machine.readInt();
                    _machine.jump(offset);
                }

                break;
            }

            case LOOKUPSWITCH: {             // 0xAB;
                final int key     = _machine.pop().asInt();
                _machine.alignInstructionPosition();
                final int defawlt = _machine.readInt();
                final int nPairs  = _machine.readInt();

                boolean foundMatch = false;
                for (int i = 0; i < nPairs; i++) {
                    final int value = _machine.readInt();
                    final int offset = _machine.readInt();
                    if (value == key) {
                        _machine.jump(offset);
                        foundMatch = true;
                        break;
                    }
                }

                if (!foundMatch) {
                    _machine.jump(defawlt);
                }
                break;
            }

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
                    _returnValue = result;
                    return MethodStatus.METHOD_END;
                }

                _machine.push(result);
                break;
            }

            case RETURN: {                  // 0xB1;
                final ExecutionFrame frame = _machine.popFrame();
                if (frame == null) {
                    _returnValue = VoidValue.VOID;
                    return MethodStatus.METHOD_END;
                }
                break;
            }

            /*========================================================================*/

            case GETSTATIC: {               // 0xB2;
                final short cpIndex = _machine.readShort();
                try {
                    _machine.push(_machine.getStatic(cpIndex));
                } catch (LinkageError e) {
                    _machine.raiseException(e);
                }
                break;
            }

            case PUTSTATIC: {                // 0xB5;
                final short cpIndex = _machine.readShort();
                try {
                    _machine.putStatic(cpIndex, _machine.pop());
                } catch (LinkageError e) {
                    _machine.raiseException(e);
                }
                break;
            }

            case GETFIELD: {                // 0xB4;
                final Reference instance = _machine.pop().asReference();
                final short cpIndex = _machine.readShort();

                if (instance.isZero()) {
                    _machine.raiseException(new NullPointerException());
                }

                try {
                    _machine.push(_machine.getField(instance, cpIndex));
                } catch (LinkageError e) {
                    _machine.raiseException(e);
                }

                break;
            }

            case PUTFIELD: {                // 0xB5;
                final Value value = _machine.pop();
                final Object instance = _machine.pop().asBoxedJavaValue();
                final short cpIndex = _machine.readShort();

                if (instance == null) {
                    _machine.raiseException(new NullPointerException());
                }

                try {
                    _machine.putField(instance, cpIndex, value);
                } catch (LinkageError e) {
                    _machine.raiseException(e);
                }

                break;
            }

            /*========================================================================*/

            case INVOKEVIRTUAL: {           // 0xB6;
                final short cpIndex = _machine.readShort();

                try {
                    final ClassMethodActor resolveMethod = (ClassMethodActor) _machine.resolveMethod(cpIndex);
                    ClassMethodActor methodActor = resolveMethod;
                    final Value value = _machine.peek(methodActor.descriptor().numberOfParameters() + 1);
                    if (value instanceof ReferenceValue) {
                        final ReferenceValue receiver = (ReferenceValue) value;
                        if (receiver.isZero()) {
                            _machine.raiseException(new NullPointerException());
                        }

                        final ClassActor dynamicClass = receiver.getClassActor();

                        methodActor = dynamicClass.findVirtualMethodActor(methodActor);
                    }

                    if (methodActor == null) {
                        final ReferenceValue receiver = (ReferenceValue) value;
                        final ClassActor dynamicClass = receiver.getClassActor();

                        methodActor = dynamicClass.findVirtualMethodActor(methodActor);
                        _machine.raiseException(new AbstractMethodError());
                    } else if (methodActor.isAbstract()) {
                        _machine.raiseException(new AbstractMethodError());
                    }

                    _machine.invokeMethod(methodActor);
                } catch (LinkageError e) {
                    _machine.raiseException(e);
                }

                break;
            }

            case INVOKESPECIAL: {           // 0xB7;
                final short cpIndex = _machine.readShort();

                try {
                    final ClassMethodActor methodActor = (ClassMethodActor) _machine.resolveMethod(cpIndex);
                    final ReferenceValue receiver = (ReferenceValue) _machine.peek(methodActor.descriptor().numberOfParameters() + 1);

                    if (receiver.isZero()) {
                        _machine.raiseException(new NullPointerException());
                    }

                    _machine.invokeMethod(methodActor);
                } catch (LinkageError e) {
                    _machine.raiseException(e);
                }

                break;
            }

            case INVOKESTATIC: {            // 0xB8;
                final short cpIndex = _machine.readShort();

                try {
                    final ClassMethodActor methodActor = (ClassMethodActor) _machine.resolveMethod(cpIndex);

                    _machine.invokeMethod(methodActor);
                } catch (LinkageError e) {
                    _machine.raiseException(e);
                }

                break;
            }

            case INVOKEINTERFACE: {         // 0xB9;
                final short cpIndex = _machine.readShort();
                _machine.readShort();

                try {
                    final InterfaceMethodActor methodActor = (InterfaceMethodActor) _machine.resolveMethod(cpIndex);
                    final ReferenceValue receiver = (ReferenceValue) _machine.peek(methodActor.descriptor().numberOfParameters() + 1);

                    if (receiver.isZero()) {
                        _machine.raiseException(new NullPointerException());
                    }

                    final ClassActor dynamicClass = receiver.getClassActor();

                    if (!dynamicClass.getAllInterfaceActors().contains((InterfaceActor) methodActor.holder())) {
                        _machine.raiseException(new IncompatibleClassChangeError(dynamicClass + " does not implement " + methodActor.holder()));
                    }

                    final VirtualMethodActor dynamicMethodActor = dynamicClass.findVirtualMethodActor(methodActor);

                    if (dynamicMethodActor == null) {
                        _machine.raiseException(new AbstractMethodError("No such method " + methodActor + " found in " + dynamicClass));
                    } else if (dynamicMethodActor.isAbstract()) {
                        _machine.raiseException(new AbstractMethodError("Method " + dynamicMethodActor + " is abstract in " + dynamicClass));
                    } else if (!dynamicMethodActor.isPublic()) {
                        _machine.raiseException(new IllegalAccessError("Method " + dynamicMethodActor + " is not public in " + dynamicClass));
                    }

                    _machine.invokeMethod(dynamicMethodActor);
                } catch (LinkageError e) {
                    _machine.raiseException(e);
                }

                break;
            }

            /*========================================================================*/

            case XXXUNUSEDXXX:              // 0xBA;
                break;

            /*========================================================================*/

            case NEW: {                      // 0xBB;
                final short cpIndex = _machine.readShort();
                final ClassActor classActor = _machine.resolveClassReference(cpIndex);
                try {
                    _machine.push(ReferenceValue.from(Objects.allocateInstance(classActor.toJava())));
                } catch (InstantiationException instantiationException) {
                    _machine.raiseException(instantiationException);
                }
                break;
            }
            case NEWARRAY: {                // 0xBC;
                final byte arrayType = _machine.readByte();
                final int arraySize  = _machine.pop().asInt();

                if (arraySize < 0) {
                    _machine.raiseException(new NegativeArraySizeException());
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

                break;
            }

            case ANEWARRAY: {               // 0xBB;
                final short cpIndex = _machine.readShort();
                final int arraySize = _machine.pop().asInt();

                final ClassActor classActor = _machine.resolveClassReference(cpIndex);

                if (arraySize < 0) {
                    _machine.raiseException(new NegativeArraySizeException());
                }

                _machine.push(ReferenceValue.from(Array.newInstance(classActor.toJava(), arraySize)));
                break;
            }


            case ARRAYLENGTH: {             // 0xBE;
                final Reference array = _machine.pop().asReference();

                if (array.isZero()) {
                    _machine.raiseException(new NullPointerException());
                }

                _machine.push(IntValue.from(Layout.readArrayLength(array)));
                break;
            }

            /*========================================================================*/

            case ATHROW: {                  // 0xBF;
                final ReferenceValue t = (ReferenceValue) _machine.pop();

                if (t.isZero()) {
                    throw _machine.raiseException(new NullPointerException());
                } else {
                    throw _machine.raiseException(t);
                }
            }

            case CHECKCAST: {               // 0xC0;
                final short cpIndex = _machine.readShort();
                final ClassActor classActor = _machine.resolveClassReference(cpIndex);
                final ReferenceValue object = (ReferenceValue) _machine.pop();

                if (!object.isZero()) {
                    if (!classActor.isAssignableFrom(object.getClassActor())) {
                        final String message = object.getClassActor().toJava() + " is not a subclass of " + classActor;
                        _machine.raiseException(new ClassCastException(message));
                    }
                }

                _machine.push(object);
                break;
            }

            case INSTANCEOF: {              // 0xC1;
                final short cpIndex = _machine.readShort();
                final ClassActor classActor = _machine.resolveClassReference(cpIndex);
                final ReferenceValue object = (ReferenceValue) _machine.pop();

                if (object.isZero() || !classActor.isAssignableFrom(object.getClassActor())) {
                    _machine.push(IntValue.from(0));
                } else {
                    _machine.push(IntValue.from(1));
                }

                break;
            }

            /*========================================================================*/

            case MONITORENTER:              // 0xC2;
            case MONITOREXIT:               // 0xC3;
                _machine.pop();
                break;

            /*========================================================================*/

            case WIDE: {                    // 0xC4;
                final Bytecode nextCode = _machine.readOpcode();
                final short index = _machine.readShort();

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
                        final int increment = _machine.readShort();
                        final int value = _machine.getLocal(index).asInt();

                        _machine.setLocal(index, IntValue.from(value + increment));
                        break;
                    }

                    case RET: {
                        final Value value = _machine.getLocal(index);
                        _machine.currentThread().frame().setBytecodePosition(value.asInt());
                        break;
                    }

                    default:
                        _machine.raiseException(new ClassFormatError("Illegal wide bytecode encountered"));
                }

                break;
            }

            /*========================================================================*/

            case MULTIANEWARRAY: {          // 0xC5;
                final short cpIndex = _machine.readShort();
                final ClassActor arrayClassActor = _machine.resolveClassReference(cpIndex);
                final int lengthsCount = (short) (_machine.readByte() & 0x7F);
                if (lengthsCount < 1) {
                    throw new ClassFormatError("dimensions operand of multianewarray is less than 1");
                }
                final int[] lengths = new int[lengthsCount];

                if (lengthsCount > arrayClassActor.numberOfDimensions()) {
                    throw new IncompatibleClassChangeError(lengthsCount + " is too many dimensions for " + arrayClassActor);
                }

                for (int i = lengthsCount - 1; i >= 0; --i) {
                    lengths[i] = _machine.pop().asInt();
                    if (lengths[i] < 0) {
                        _machine.raiseException(new NegativeArraySizeException());
                    }
                }

                _machine.push(ReferenceValue.from(createMultiDimensionArray(arrayClassActor, 0, lengths)));
                break;
            }

            /*========================================================================*/

            case IFNULL: {                  // 0xC6;
                final int offset = _machine.readShort();
                final Value r = _machine.pop();

                if (r.isZero()) {
                    _machine.jump(offset);
                }

                break;
            }

            case IFNONNULL: {               // 0xC7;
                final int offset = _machine.readShort();
                final Value r = _machine.pop();

                if (!r.isZero()) {
                    _machine.jump(offset);
                }

                break;
            }

            /*========================================================================*/

            case GOTO_W:                    // 0xC8;
                _machine.jump(_machine.readInt());
                break;

            case JSR_W:                     // 0xC9;
                final int offset = _machine.readInt();
                final int returnPosition = _machine.currentThread().frame().currentBytePosition();
                _machine.push(IntValue.from(returnPosition));
                _machine.jump(offset);
                break;

                /*========================================================================*/

            case BREAKPOINT:                // 0xCA;
                break;

                /*========================================================================*/

            default:
                _machine.raiseException(new ClassFormatError("Unsupported bytecode: " + opcode));
        }
        return MethodStatus.METHOD_CONTINUE;
    }

    public static enum MethodStatus {
        METHOD_END,
        METHOD_CONTINUE,
    }

    private static Object createMultiDimensionArray(ClassActor arrayClassActor, int lengthIndex, int[] lengths) {
        final ClassActor componentClassActor = arrayClassActor.componentClassActor();
        assert componentClassActor != null : arrayClassActor + " is not an array class";
        final int length = lengths[lengthIndex];
        assert length >= 0 : "negative array length: " + length;
        final Object result = Array.newInstance(componentClassActor.toJava(), length);
        if (length > 0) {
            final int nextLengthIndex = lengthIndex + 1;
            if (nextLengthIndex < lengths.length) {
                for (int i = 0; i < length; i++) {
                    final Object subArray = createMultiDimensionArray(componentClassActor, nextLengthIndex, lengths);
                    final Object[] array = (Object[]) result;
                    array[i] = subArray;
                }
            }
        }
        return result;
    }
}
