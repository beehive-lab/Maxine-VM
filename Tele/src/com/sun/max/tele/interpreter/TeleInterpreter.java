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
import com.sun.max.vm.cps.ir.*;
import com.sun.max.vm.cps.ir.interpreter.*;
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

    private final TeleVM teleVM;

    private Machine machine;
    private Value returnValue;
    private int instructionsExecuted;

    public TeleInterpreter(TeleVM teleVM) {
        this.teleVM = teleVM;
    }

    @Override
    public Value execute(IrMethod method, Value... arguments) throws InvocationTargetException {
        return run(method.classMethodActor(), arguments);
    }

    /**
     * Creates an interpreter instance and uses it to execute a given method with the given arguments.
     * Note that arguments must be dynamic types seen by the JavaPrototyper as legitimate VM classes.
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

        classActor = HostedBootClassLoader.HOSTED_BOOT_CLASS_LOADER.mustMakeClassActor(JavaTypeDescriptor.getDescriptorForJavaString(declaringClassName));
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

        classActor = HostedBootClassLoader.HOSTED_BOOT_CLASS_LOADER.mustMakeClassActor(JavaTypeDescriptor.forJavaClass(declaringClass));
        classMethodActor = classActor.findClassMethodActor(SymbolTable.makeSymbol(name), signature);

        if (classMethodActor == null) {
            throw new NoSuchMethodError(declaringClass.getName() + "." + name + signature);
        }

        return execute(teleVM, classMethodActor, args);
    }

    /**
     * A lazy constructed cache of mappings from opcode positions to disassembled bytecode instructions.
     */
    private Map<MethodActor, Map<Integer, String>> bytecodeTraces;

    /**
     * The last frame traced in {@link #traceExecution()}.
     */
    private ExecutionFrame lastTracedFrame;

    /**
     * Traces the current execution point.
     */
    private void traceExecution() {
        if (Trace.hasLevel(2)) {
            final PrintStream stream = Trace.stream();
            final ExecutionFrame frame = machine.currentThread().frame();

            final int depth = frame.depth();
            if (lastTracedFrame == null) {
                stream.println("Interpreter: " + Strings.spaces(depth * 2) + "ENTERING: " + frame.method().format("%H.%n(%p)"));
            } else if (lastTracedFrame != frame) {
                final int lastFrameDepth = lastTracedFrame.depth();
                if (lastFrameDepth < depth) {
                    stream.println("Interpreter: " + Strings.spaces(depth * 2) + "ENTERING: " + frame.method().format("%H.%n(%p)"));
                } else {
                    stream.println("Interpreter: " + Strings.spaces(lastFrameDepth * 2) + "EXITING: " + lastTracedFrame.method().format("%H.%n(%p)"));
                }
            }
            if (Trace.hasLevel(3)) {
                if (bytecodeTraces == null) {
                    bytecodeTraces = new HashMap<MethodActor, Map<Integer, String>>();
                }
                Map<Integer, String> bcpToTrace = bytecodeTraces.get(machine.currentMethod());
                if (bcpToTrace == null) {
                    bcpToTrace = new HashMap<Integer, String>();
                    bytecodeTraces.put(machine.currentMethod(), bcpToTrace);
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
            lastTracedFrame = frame;
            stream.flush();
        }
    }

    private Value run(ClassMethodActor classMethodActor, Value... arguments) throws TeleInterpreterException {

        machine = new Machine(teleVM);
        machine.pushFrame(classMethodActor);
        int j = 0;
        for (int i = 0; i < arguments.length; i++, j++) {
            machine.setLocal(j, arguments[i]);
            if (arguments[i].isCategory2()) {
                j++;
            }
        }

        Bytecode code;
        MethodStatus status;
        instructionsExecuted = 0;

        while (true) {
            code = machine.readOpcode();

            traceExecution();

            try {
                status = interpret(code);
                if (status == MethodStatus.METHOD_END) {
                    break;
                }
            } catch (TeleInterpreterException executionException) {
                final ReferenceValue throwableReference = executionException.throwableReference();
                final boolean handled = machine.handleException(throwableReference); //if this succeeds we keep looping
                if (!handled) {
                    throw executionException;
                }
            } catch (Throwable throwable) {
                throw new TeleInterpreterException(throwable, machine);
            } finally {
                instructionsExecuted++;
            }
        }

        if (returnValue instanceof TeleReferenceValue) {
            returnValue = TeleReferenceValue.from(teleVM, machine.makeLocalReference((TeleReference) returnValue.asReference()));
        }

        final Kind resultKind = classMethodActor.resultKind();
        if (resultKind.toStackKind() == Kind.INT) {
            returnValue = resultKind.convert(returnValue);
        }
        return returnValue;
    }

    private MethodStatus interpret(Bytecode opcode) throws TeleInterpreterException {
        switch (opcode) {
            case NOP:                       // 0x00
                break;

            case ACONST_NULL:               // 0x01
                machine.push(ReferenceValue.NULL);
                break;

                /*========================================================================*/

            case ICONST_M1:                 // 0x02;
                machine.push(IntValue.from(-1));
                break;

            case ICONST_0:                  // 0x03;
                machine.push(IntValue.from(0));
                break;

            case ICONST_1:                  // 0x04;
                machine.push(IntValue.from(1));
                break;

            case ICONST_2:                  // 0x05;
                machine.push(IntValue.from(2));
                break;

            case ICONST_3:                  // 0x06;
                machine.push(IntValue.from(3));
                break;

            case ICONST_4:                  // 0x07;
                machine.push(IntValue.from(4));
                break;

            case ICONST_5:                  // 0x08;
                machine.push(IntValue.from(5));
                break;

                /*========================================================================*/

            case LCONST_0:                  // 0x09;
                machine.push(LongValue.from(0));
                break;

            case LCONST_1:                  // 0x0A;
                machine.push(LongValue.from(1));
                break;

                /*========================================================================*/

            case FCONST_0:                  // 0x0B;
                machine.push(FloatValue.from(0));
                break;

            case FCONST_1:                  // 0x0C;
                machine.push(FloatValue.from(1));
                break;

            case FCONST_2:                  // 0x0D;
                machine.push(FloatValue.from(2));
                break;

                /*========================================================================*/

            case DCONST_0:                  // 0x0E;
                machine.push(DoubleValue.from(0));
                break;

            case DCONST_1:                  // 0x0F;
                machine.push(DoubleValue.from(1));
                break;

                /*========================================================================*/

            case BIPUSH:                    // 0x10;
                machine.push(IntValue.from(machine.readByte()));
                break;

            case SIPUSH:                    // 0x11;
                machine.push(IntValue.from(machine.readShort()));
                break;

                /*========================================================================*/

            case LDC: {                     // 0x12;
                final byte cpIndex = machine.readByte();
                machine.push(machine.resolveConstantReference((short) (cpIndex & 0xFF)));
                break;
            }

            case LDC_W:                     // 0x13;
            case LDC2_W: {                  // 0x14;
                final short cpIndex = machine.readShort();
                machine.push(machine.resolveConstantReference(cpIndex));
                break;
            }

            /*========================================================================*/

            case ILOAD:                     // 0x15;
            case LLOAD:                     // 0x16;
            case FLOAD:                     // 0x17;
            case DLOAD:                     // 0x18;
            case ALOAD:                     // 0x19;
                machine.push(machine.getLocal(machine.readByte()));
                break;

                /*========================================================================*/

            case ILOAD_0:                   // 0x1A;
                machine.push(machine.getLocal(0));
                break;

            case ILOAD_1:                   // 0x1B;
                machine.push(machine.getLocal(1));
                break;

            case ILOAD_2:                   // 0x1C;
                machine.push(machine.getLocal(2));
                break;

            case ILOAD_3:                   // 0x1D;
                machine.push(machine.getLocal(3));
                break;

                /*========================================================================*/

            case LLOAD_0:                   // 0x1E;
                machine.push(machine.getLocal(0));
                break;

            case LLOAD_1:                   // 0x1F;
                machine.push(machine.getLocal(1));
                break;

            case LLOAD_2:                   // 0x20;
                machine.push(machine.getLocal(2));
                break;

            case LLOAD_3:                   // 0x21;
                machine.push(machine.getLocal(3));
                break;

                /*========================================================================*/

            case FLOAD_0:                   // 0x22;
                machine.push(machine.getLocal(0));
                break;

            case FLOAD_1:                   // 0x23;
                machine.push(machine.getLocal(1));
                break;

            case FLOAD_2:                   // 0x24;
                machine.push(machine.getLocal(2));
                break;

            case FLOAD_3:                   // 0x25;
                machine.push(machine.getLocal(3));
                break;

                /*========================================================================*/

            case DLOAD_0:                   // 0x26;
                machine.push(machine.getLocal(0));
                break;

            case DLOAD_1:                   // 0x27;
                machine.push(machine.getLocal(1));
                break;

            case DLOAD_2:                   // 0x28;
                machine.push(machine.getLocal(2));
                break;

            case DLOAD_3:                   // 0x29;
                machine.push(machine.getLocal(3));
                break;

                /*========================================================================*/

            case ALOAD_0:                   // 0x2A;
                machine.push(machine.getLocal(0));
                break;

            case ALOAD_1:                   // 0x2B;
                machine.push(machine.getLocal(1));
                break;

            case ALOAD_2:                   // 0x2C;
                machine.push(machine.getLocal(2));
                break;

            case ALOAD_3:                   // 0x2D;
                machine.push(machine.getLocal(3));
                break;

                /*========================================================================*/

            case IALOAD: {                  // 0x2E;
                final int index = machine.pop().asInt();                // Get array index (IntValue)
                final Reference array = machine.pop().asReference();    // Get the array (ReferenceValue)

                if (array.isZero()) {
                    machine.raiseException(new NullPointerException());
                }

                if (Layout.readArrayLength(array) <= index || index < 0) {
                    machine.raiseException(new ArrayIndexOutOfBoundsException());
                }

                final int val = Layout.getInt(array, index);

                // Push value to operand stack
                machine.push(IntValue.from(val));
                break;
            }

            case LALOAD: {                  // 0x2F;
                final int index = machine.pop().asInt();                // Get array index (IntValue)
                final Reference array = machine.pop().asReference();    // Get the array (ReferenceValue)

                if (array.isZero()) {
                    machine.raiseException(new NullPointerException());
                }

                if (Layout.readArrayLength(array) <= index || index < 0) {
                    machine.raiseException(new ArrayIndexOutOfBoundsException());
                }

                final long val = Layout.getLong(array, index);

                // Push value to operand stack
                machine.push(LongValue.from(val));
                break;
            }

            case FALOAD: {                  // 0x30;
                final int index = machine.pop().asInt();                // Get array index (IntValue)
                final Reference array = machine.pop().asReference();    // Get the array (ReferenceValue)

                if (array.isZero()) {
                    machine.raiseException(new NullPointerException());
                }

                if (Layout.readArrayLength(array) <= index || index < 0) {
                    machine.raiseException(new ArrayIndexOutOfBoundsException());
                }

                final float val = Layout.getFloat(array, index);

                // Push value to operand stack
                machine.push(FloatValue.from(val));
                break;
            }

            case DALOAD: {                  // 0x31;
                final int index = machine.pop().asInt();                // Get array index (IntValue)
                final Reference array = machine.pop().asReference();    // Get the array (ReferenceValue)

                if (array.isZero()) {
                    machine.raiseException(new NullPointerException());
                }

                if (Layout.readArrayLength(array) <= index || index < 0) {
                    machine.raiseException(new ArrayIndexOutOfBoundsException());
                }

                final double val = Layout.getDouble(array, index);

                // Push value to operand stack
                machine.push(DoubleValue.from(val));
                break;
            }

            case AALOAD: {                  // 0x32;
                final int index = machine.pop().asInt();                // Get array index (IntValue)
                final Reference array = machine.pop().asReference();    // Get the array (ReferenceValue)

                if (array.isZero()) {
                    machine.raiseException(new NullPointerException());
                }

                if (Layout.readArrayLength(array) <= index || index < 0) {
                    machine.raiseException(new ArrayIndexOutOfBoundsException());
                }

                final Reference val = Layout.getReference(array, index);

                // Push value to operand stack
                machine.push(machine.toReferenceValue(val));
                break;
            }

            case BALOAD: {                  // 0x33;
                final int index = machine.pop().asInt();                  // Get array index (IntValue)
                final Reference array = machine.pop().asReference();    // Get the array (ReferenceValue)

                if (array.isZero()) {
                    machine.raiseException(new NullPointerException());
                }

                if (Layout.readArrayLength(array) <= index || index < 0) {
                    machine.raiseException(new ArrayIndexOutOfBoundsException());
                }

                final IntValue val;

                if (machine.toReferenceValue(array).getClassActor() == ClassRegistry.BOOLEAN_ARRAY) {
                    final boolean booleanVal = Layout.getBoolean(array, index);
                    val = booleanVal ? IntValue.ONE : IntValue.ZERO;
                } else {
                    final byte byteVal = Layout.getByte(array, index);
                    val = IntValue.from(byteVal);
                }

                // Push value to operand stack
                machine.push(val);
                break;
            }

            case CALOAD: {                   // 0x34;
                final int index = machine.pop().asInt();                // Get array index (IntValue)
                final Reference array = machine.pop().asReference();    // Get the array (ReferenceValue)

                if (array.isZero()) {
                    machine.raiseException(new NullPointerException());
                }

                if (Layout.readArrayLength(array) <= index || index < 0) {
                    machine.raiseException(new ArrayIndexOutOfBoundsException());
                }

                final char val = Layout.getChar(array, index);

                // Push value to operand stack
                machine.push(IntValue.from(val));
                break;
            }

            case SALOAD: {                   // 0x35;
                final int index = machine.pop().asInt();                  // Get array index (IntValue)
                final Reference array = machine.pop().asReference();    // Get the array (ReferenceValue)

                if (array.isZero()) {
                    machine.raiseException(new NullPointerException());
                }

                if (Layout.readArrayLength(array) <= index || index < 0) {
                    machine.raiseException(new ArrayIndexOutOfBoundsException());
                }

                final short val = Layout.getShort(array, index);

                // Push value to operand stack
                machine.push(IntValue.from(val));
                break;
            }

            /*========================================================================*/

            case ISTORE:                    // 0x36;
            case LSTORE:                    // 0x37;
            case FSTORE:                    // 0x38;
            case DSTORE:                    // 0x39;
            case ASTORE:                    // 0x3A;
                machine.setLocal(machine.readByte(), machine.pop());
                break;

                /*========================================================================*/

            case ISTORE_0:                  // 0x3B;
                machine.setLocal(0, machine.pop());
                break;

            case ISTORE_1:                  // 0x3C;
                machine.setLocal(1, machine.pop());
                break;

            case ISTORE_2:                  // 0x3D;
                machine.setLocal(2, machine.pop());
                break;

            case ISTORE_3:                  // 0x3E;
                machine.setLocal(3, machine.pop());
                break;

                /*========================================================================*/

            case LSTORE_0:                  // 0x3F;
                machine.setLocal(0, machine.pop());
                break;

            case LSTORE_1:                  // 0x40;
                machine.setLocal(1, machine.pop());
                break;

            case LSTORE_2:                  // 0x41;
                machine.setLocal(2, machine.pop());
                break;

            case LSTORE_3:                  // 0x42;
                machine.setLocal(3, machine.pop());
                break;

                /*========================================================================*/

            case FSTORE_0:                  // 0x43;
                machine.setLocal(0, machine.pop());
                break;

            case FSTORE_1:                  // 0x44;
                machine.setLocal(1, machine.pop());
                break;

            case FSTORE_2:                  // 0x45;
                machine.setLocal(2, machine.pop());
                break;

            case FSTORE_3:                  // 0x46;
                machine.setLocal(3, machine.pop());
                break;

                /*========================================================================*/

            case DSTORE_0:                  // 0x47;
                machine.setLocal(0, machine.pop());
                break;

            case DSTORE_1:                  // 0x48;
                machine.setLocal(1, machine.pop());
                break;

            case DSTORE_2:                  // 0x49;
                machine.setLocal(2, machine.pop());
                break;

            case DSTORE_3:                  // 0x4A;
                machine.setLocal(3, machine.pop());
                break;

                /*========================================================================*/

            case ASTORE_0:                  // 0x4B;
                machine.setLocal(0, machine.pop());
                break;

            case ASTORE_1:                  // 0x4C;
                machine.setLocal(1, machine.pop());
                break;

            case ASTORE_2:                  // 0x4D;
                machine.setLocal(2, machine.pop());
                break;

            case ASTORE_3:                  // 0x4E;
                machine.setLocal(3, machine.pop());
                break;

                /*========================================================================*/

            case IASTORE: {                 // 0x4F;
                final int val = machine.pop().toInt();                  // Get value to store
                final int index = machine.pop().toInt();                // Get array index
                final Reference array = machine.pop().asReference();    // Get the array (ReferenceValue)

                if (array.isZero()) {
                    machine.raiseException(new NullPointerException());
                }

                if (Layout.readArrayLength(array) <= index || index < 0) {
                    machine.raiseException(new ArrayIndexOutOfBoundsException());
                }

                Layout.setInt(array, index, val);
                break;
            }

            case LASTORE: {                 // 0x50;
                final long val = machine.pop().toLong();                // Get value to store
                final int index = machine.pop().toInt();                // Get array index
                final Reference array = machine.pop().asReference();    // Get the array (ReferenceValue)

                if (array.isZero()) {
                    machine.raiseException(new NullPointerException());
                }

                if (Layout.readArrayLength(array) <= index || index < 0) {
                    machine.raiseException(new ArrayIndexOutOfBoundsException());
                }

                Layout.setLong(array, index, val);
                break;
            }

            case FASTORE: {                 // 0x51;
                final float val = machine.pop().toFloat();              // Get value to store
                final int index = machine.pop().toInt();                // Get array index
                final Reference array = machine.pop().asReference();    // Get the array (ReferenceValue)

                if (array.isZero()) {
                    machine.raiseException(new NullPointerException());
                }

                if (Layout.readArrayLength(array) <= index || index < 0) {
                    machine.raiseException(new ArrayIndexOutOfBoundsException());
                }

                Layout.setFloat(array, index, val);
                break;
            }

            case DASTORE: {                 // 0x52;
                final double val = machine.pop().toDouble();            // Get value to store
                final int index = machine.pop().toInt();                // Get array index
                final Reference array = machine.pop().asReference();    // Get the array (ReferenceValue)

                if (array.isZero()) {
                    machine.raiseException(new NullPointerException());
                }

                if (Layout.readArrayLength(array) <= index || index < 0) {
                    machine.raiseException(new ArrayIndexOutOfBoundsException());
                }

                Layout.setDouble(array, index, val);
                break;
            }

            case AASTORE: {                 // 0x53;
                final Reference val = machine.pop().asReference();      // Get value to store
                final int index = machine.pop().toInt();                // Get array index
                final Reference array = machine.pop().asReference();    // Get the array (ReferenceValue)

                if (array.isZero()) {
                    machine.raiseException(new NullPointerException());
                }

                if (Layout.readArrayLength(array) <= index || index < 0) {
                    machine.raiseException(new ArrayIndexOutOfBoundsException());
                }

                Layout.setReference(array, index, val);
                break;
            }

            case BASTORE: {                 // 0x54;
                final int val = machine.pop().toInt();                  // Get value to store
                final int index = machine.pop().toInt();                // Get array index
                final Reference array = machine.pop().asReference();    // Get the array (ReferenceValue)

                if (array.isZero()) {
                    machine.raiseException(new NullPointerException());
                }

                if (Layout.readArrayLength(array) <= index || index < 0) {
                    machine.raiseException(new ArrayIndexOutOfBoundsException());
                }

                Layout.setByte(array, index, (byte) val);
                break;
            }

            case CASTORE: {                 // 0x55;
                final int val = machine.pop().toInt();                  // Get value to store
                final int index = machine.pop().toInt();                // Get array index
                final Reference array = machine.pop().asReference();    // Get the array (ReferenceValue)

                if (array.isZero()) {
                    machine.raiseException(new NullPointerException());
                }

                if (Layout.readArrayLength(array) <= index || index < 0) {
                    machine.raiseException(new ArrayIndexOutOfBoundsException());
                }

                Layout.setChar(array, index, (char) val);
                break;
            }

            case SASTORE: {                 // 0x56;
                final int val = machine.pop().toInt();                  // Get value to store
                final int index = machine.pop().toInt();                // Get array index
                final Reference array = machine.pop().asReference();    // Get the array (ReferenceValue)

                if (array.isZero()) {
                    machine.raiseException(new NullPointerException());
                }

                if (Layout.readArrayLength(array) <= index || index < 0) {
                    machine.raiseException(new ArrayIndexOutOfBoundsException());
                }

                Layout.setShort(array, index, (short) val);
                break;
            }

            /*========================================================================*/

            case POP:                       // 0x57;
                machine.pop();
                break;

            case POP2: {                    // 0x58;
                final Value s1 = machine.pop();

                if (!s1.isCategory2()) {
                    machine.pop();
                }
                break;
            }

            case DUP:                       // 0x59;
                machine.push(machine.peek());
                break;

            case DUP_X1: {                  // 0x5A;
                final Value s1 = machine.pop();
                final Value s2 = machine.pop();

                machine.push(s1);
                machine.push(s2);
                machine.push(s1);
                break;
            }

            case DUP_X2: {                  // 0x5B;
                final Value s1 = machine.pop();
                final Value s2 = machine.pop();

                if (s2.isCategory2()) {
                    machine.push(s1);
                    machine.push(s2);
                    machine.push(s1);
                } else {
                    final Value s3 = machine.pop();

                    machine.push(s1);
                    machine.push(s3);
                    machine.push(s2);
                    machine.push(s1);
                }
                break;
            }

            case DUP2: {                    // 0x5C;
                final Value s1 = machine.pop();

                if (s1.isCategory2()) {
                    machine.push(s1);
                    machine.push(s1);
                } else {
                    final Value s2 = machine.pop();

                    machine.push(s2);
                    machine.push(s1);
                    machine.push(s2);
                    machine.push(s1);
                }
                break;
            }

            case DUP2_X1: {                 // 0x5D;
                final Value s1 = machine.pop();

                if (s1.isCategory2()) {
                    final Value s2 = machine.pop();

                    machine.push(s1);
                    machine.push(s2);
                    machine.push(s1);
                } else {
                    final Value s2 = machine.pop();
                    final Value s3 = machine.pop();

                    machine.push(s2);
                    machine.push(s1);
                    machine.push(s3);
                    machine.push(s2);
                    machine.push(s1);
                }
                break;
            }

            case DUP2_X2: {                 // 0x5E;
                final Value s1 = machine.pop();

                if (s1.isCategory2()) {
                    final Value s2 = machine.pop();

                    if (s2.isCategory2()) {
                        machine.push(s1);
                        machine.push(s2);
                        machine.push(s1);
                    } else {
                        final Value s3 = machine.pop();

                        machine.push(s1);
                        machine.push(s3);
                        machine.push(s2);
                        machine.push(s1);
                    }
                } else {
                    final Value s2 = machine.pop();
                    final Value s3 = machine.pop();

                    if (s3.isCategory2()) {
                        machine.push(s2);
                        machine.push(s1);
                        machine.push(s3);
                        machine.push(s2);
                        machine.push(s1);
                    } else {
                        final Value s4 = machine.pop();

                        machine.push(s2);
                        machine.push(s1);
                        machine.push(s4);
                        machine.push(s3);
                        machine.push(s2);
                        machine.push(s1);
                    }
                }
                break;
            }

            case SWAP: {                    // 0x5F;
                final Value s1 = machine.pop();
                final Value s2 = machine.pop();

                machine.push(s1);
                machine.push(s2);
                break;
            }

            /*========================================================================*/

            case IADD: {                    // 0x60;
                final int v1 = machine.pop().asInt();
                final int v2 = machine.pop().asInt();

                machine.push(IntValue.from(v1 + v2));
                break;
            }

            case LADD: {                    // 0x61;
                final long v1 = machine.pop().asLong();
                final long v2 = machine.pop().asLong();

                machine.push(LongValue.from(v1 + v2));
                break;
            }

            case FADD: {                    // 0x62;
                final float v1 = machine.pop().asFloat();
                final float v2 = machine.pop().asFloat();

                machine.push(FloatValue.from(v1 + v2));
                break;
            }

            case DADD: {                    // 0x63;
                final double v1 = machine.pop().asDouble();
                final double v2 = machine.pop().asDouble();

                machine.push(DoubleValue.from(v1 + v2));
                break;
            }

            /*========================================================================*/

            case ISUB: {                    // 0x64;
                final int v1 = machine.pop().asInt();
                final int v2 = machine.pop().asInt();

                machine.push(IntValue.from(v2 - v1));
                break;
            }

            case LSUB: {                    // 0x65;
                final long v1 = machine.pop().asLong();
                final long v2 = machine.pop().asLong();

                machine.push(LongValue.from(v2 - v1));
                break;
            }

            case FSUB: {                    // 0x66;
                final float v1 = machine.pop().asFloat();
                final float v2 = machine.pop().asFloat();

                machine.push(FloatValue.from(v2 - v1));
                break;
            }

            case DSUB: {                    // 0x67;
                final double v1 = machine.pop().asDouble();
                final double v2 = machine.pop().asDouble();

                machine.push(DoubleValue.from(v2 - v1));
                break;
            }

            /*========================================================================*/

            case IMUL: {                    // 0x68;
                final int v1 = machine.pop().asInt();
                final int v2 = machine.pop().asInt();

                machine.push(IntValue.from(v1 * v2));
                break;
            }

            case LMUL: {                    // 0x69;
                final long v1 = machine.pop().asLong();
                final long v2 = machine.pop().asLong();

                machine.push(LongValue.from(v1 * v2));
                break;
            }

            case FMUL: {                    // 0x6A;
                final float v1 = machine.pop().asFloat();
                final float v2 = machine.pop().asFloat();

                machine.push(FloatValue.from(v1 * v2));
                break;
            }

            case DMUL: {                    // 0x6B;
                final double v1 = machine.pop().asDouble();
                final double v2 = machine.pop().asDouble();

                machine.push(DoubleValue.from(v1 * v2));
                break;
            }

            /*========================================================================*/

            case IDIV: {                    // 0x6C;
                final int v1 = machine.pop().asInt();
                final int v2 = machine.pop().asInt();

                if (v1 == 0) {
                    machine.raiseException(new ArithmeticException("Division by zero"));
                }

                machine.push(IntValue.from(v2 / v1));
                break;
            }

            case LDIV: {                    // 0x6D;
                final long v1 = machine.pop().asLong();
                final long v2 = machine.pop().asLong();

                if (v1 == 0L) {
                    machine.raiseException(new ArithmeticException("Division by zero"));
                }

                machine.push(LongValue.from(v2 / v1));
                break;
            }

            case FDIV: {                    // 0x6E;
                final float v1 = machine.pop().asFloat();
                final float v2 = machine.pop().asFloat();

                if (v1 == 0.0) {
                    machine.raiseException(new ArithmeticException("Division by zero"));
                }

                machine.push(FloatValue.from(v2 / v1));
                break;
            }

            case DDIV: {                    // 0x6F;
                final double v1 = machine.pop().asDouble();
                final double v2 = machine.pop().asDouble();

                if (v1 == 0.0D) {
                    machine.raiseException(new ArithmeticException("Division by zero"));
                }

                machine.push(DoubleValue.from(v2 / v1));
                break;
            }

            /*========================================================================*/

            case IREM: {                    // 0x70;
                final int v1 = machine.pop().asInt();
                final int v2 = machine.pop().asInt();

                if (v1 == 0) {
                    machine.raiseException(new ArithmeticException("Division by zero"));
                }

                machine.push(IntValue.from(v2 % v1));
                break;
            }

            case LREM: {                    // 0x71;
                final long v1 = machine.pop().asLong();
                final long v2 = machine.pop().asLong();

                if (v1 == 0L) {
                    machine.raiseException(new ArithmeticException("Division by zero"));
                }

                machine.push(LongValue.from(v2 % v1));
                break;
            }

            case FREM: {                    // 0x72;
                final float v1 = machine.pop().asFloat();
                final float v2 = machine.pop().asFloat();

                if (v1 == 0.0) {
                    machine.raiseException(new ArithmeticException("Division by zero"));
                }

                machine.push(FloatValue.from(v2 % v1));
                break;
            }

            case DREM: {                    // 0x73;
                final double v1 = machine.pop().asDouble();
                final double v2 = machine.pop().asDouble();

                if (v1 == 0.0D) {
                    machine.raiseException(new ArithmeticException("Division by zero"));
                }

                machine.push(DoubleValue.from(v2 % v1));
                break;
            }

            /*========================================================================*/

            case INEG:                      // 0x74;
                machine.push(IntValue.from(0 - machine.pop().asInt()));
                break;

            case LNEG:                      // 0x75;
                machine.push(LongValue.from(0 - machine.pop().asLong()));
                break;

            case FNEG:                      // 0x76;
                machine.push(FloatValue.from((float) 0.0 - machine.pop().asFloat()));
                break;

            case DNEG:                      // 0x77;
                machine.push(DoubleValue.from(0.0 - machine.pop().asDouble()));
                break;

                /*========================================================================*/

            case ISHL: {                    // 0x78;
                final int amount = machine.pop().asInt();
                final int value  = machine.pop().asInt();

                machine.push(IntValue.from(value << (amount & 0x1F)));
                break;
            }

            case LSHL: {                    // 0x79;
                final int amount = machine.pop().asInt();
                final long value = machine.pop().asLong();

                machine.push(LongValue.from(value << (amount & 0x3F)));
                break;
            }

            case ISHR: {                    // 0x7A;
                final int amount = machine.pop().asInt();
                final int value  = machine.pop().asInt();

                machine.push(IntValue.from(value >> (amount & 0x1F)));
                break;
            }

            case LSHR: {                    // 0x7B;
                final int amount = machine.pop().asInt();
                final long value = machine.pop().asLong();

                machine.push(LongValue.from(value >> (amount & 0x3F)));
                break;
            }

            case IUSHR: {                   // 0x7C;
                final int amount = machine.pop().asInt();
                final int value  = machine.pop().asInt();

                machine.push(IntValue.from(value >>> (amount & 0x1F)));
                break;
            }

            case LUSHR: {                   // 0x7D;
                final int amount = machine.pop().asInt();
                final long value = machine.pop().asLong();

                machine.push(LongValue.from(value >>> (amount & 0x3F)));
                break;
            }

            /*========================================================================*/

            case IAND: {                    // 0x7E;
                final int s1 = machine.pop().asInt();
                final int s2 = machine.pop().asInt();

                machine.push(IntValue.from(s2 & s1));
                break;
            }

            case LAND: {                    // 0x7F;
                final long s1 = machine.pop().asLong();
                final long s2 = machine.pop().asLong();

                machine.push(LongValue.from(s2 & s1));
                break;
            }

            case IOR: {                     // 0x80;
                final int s1 = machine.pop().asInt();
                final int s2 = machine.pop().asInt();

                machine.push(IntValue.from(s2 | s1));
                break;
            }

            case LOR: {                     // 0x81;
                final long s1 = machine.pop().asLong();
                final long s2 = machine.pop().asLong();

                machine.push(LongValue.from(s2 | s1));
                break;
            }

            case IXOR: {                    // 0x82;
                final int s1 = machine.pop().asInt();
                final int s2 = machine.pop().asInt();

                machine.push(IntValue.from(s2 ^ s1));
                break;
            }

            case LXOR: {                    // 0x83;
                final long s1 = machine.pop().asLong();
                final long s2 = machine.pop().asLong();

                machine.push(LongValue.from(s2 ^ s1));
                break;
            }

            /*========================================================================*/

            case IINC: {                    // 0x84;
                final int index     = machine.readByte();
                final int increment = machine.readByte();
                final int value     = machine.getLocal(index).asInt();

                machine.setLocal(index, IntValue.from(value + increment));
                break;
            }

            /*========================================================================*/

            case I2L: {                     // 0x85;
                final int value = machine.pop().asInt();
                machine.push(LongValue.from(value));
                break;
            }

            case I2F: {                     // 0x86;
                final int value = machine.pop().asInt();
                machine.push(FloatValue.from(value));
                break;
            }

            case I2D: {                     // 0x87;
                final int value = machine.pop().asInt();
                machine.push(DoubleValue.from(value));
                break;
            }

            case L2I: {                     // 0x88;
                final long value = machine.pop().asLong();
                machine.push(IntValue.from((int) value));
                break;
            }

            case L2F: {                     // 0x89;
                final long value = machine.pop().asLong();
                machine.push(FloatValue.from(value));
                break;
            }

            case L2D: {                     // 0x8A;
                final long value = machine.pop().asLong();
                machine.push(DoubleValue.from(value));
                break;
            }

            case F2I: {                     // 0x8B;
                final float value = machine.pop().asFloat();
                machine.push(IntValue.from((int) value));
                break;
            }

            case F2L: {                     // 0x8C;
                final float value = machine.pop().asFloat();
                machine.push(LongValue.from((long) value));
                break;
            }

            case F2D: {                     // 0x8D;
                final float value = machine.pop().asFloat();
                machine.push(DoubleValue.from(value));
                break;
            }

            case D2I: {                     // 0x8E;
                final double value = machine.pop().asDouble();
                machine.push(IntValue.from((int) value));
                break;
            }

            case D2L: {                     // 0x8F;
                final double value = machine.pop().asDouble();
                machine.push(LongValue.from((long) value));
                break;
            }

            case D2F: {                     // 0x90;
                final double value = machine.pop().asDouble();
                machine.push(FloatValue.from((float) value));
                break;
            }

            case I2B: {                     // 0x91;
                final byte value = machine.pop().toByte();
                machine.push(IntValue.from(value));
                break;
            }

            case I2C: {                     // 0x92;
                final char value = machine.pop().toChar();
                machine.push(IntValue.from(value));
                break;
            }

            case I2S: {                     // 0x93;
                final short value  = machine.pop().toShort();
                machine.push(IntValue.from(value));
                break;
            }

            /*========================================================================*/

            case LCMP: {                    // 0x94;
                final long right  = machine.pop().asLong();
                final long left   = machine.pop().asLong();
                final int  result = (left < right) ? -1 : (left == right) ? 0 : 1;

                machine.push(IntValue.from(result));
                break;
            }

            case FCMPL:                     // 0x95;
            case FCMPG: {                   // 0x96;
                final float right  = machine.pop().asFloat();
                final float left   = machine.pop().asFloat();
                final int   result = (left < right) ? -1 : (left == right) ? 0 : 1;

                machine.push(IntValue.from(result));
                break;
            }

            case DCMPL:                     // 0x97;
            case DCMPG: {                   // 0x98;
                final double right  = machine.pop().asDouble();
                final double left   = machine.pop().asDouble();
                final int    result = (left < right) ? -1 : (left == right) ? 0 : 1;

                machine.push(IntValue.from(result));
                break;
            }

            /*========================================================================*/

            case IFEQ: {                    // 0x99;
                final short offset = machine.readShort();
                final int s1 = machine.pop().asInt();
                if (s1 == 0) {
                    machine.jump(offset);
                }
                break;
            }

            case IFNE: {                    // 0x9A;
                final short offset = machine.readShort();
                final int s1 = machine.pop().asInt();
                if (s1 != 0) {
                    machine.jump(offset);
                }
                break;
            }

            case IFLT: {                    // 0x9B;
                final short offset = machine.readShort();
                final int s1 = machine.pop().asInt();
                if (s1 < 0) {
                    machine.jump(offset);
                }
                break;
            }

            case IFGE: {                    // 0x9C;
                final short offset = machine.readShort();
                final int s1 = machine.pop().asInt();
                if (s1 >= 0) {
                    machine.jump(offset);
                }
                break;
            }

            case IFGT: {                    // 0x9D;
                final short offset = machine.readShort();
                final int s1 = machine.pop().asInt();
                if (s1 > 0) {
                    machine.jump(offset);
                }
                break;
            }

            case IFLE: {                    // 0x9E;
                final short offset = machine.readShort();
                final int s1 = machine.pop().asInt();
                if (s1 <= 0) {
                    machine.jump(offset);
                }
                break;
            }

            case IF_ICMPEQ: {               // 0x9F;
                final short offset = machine.readShort();
                final int s1 = machine.pop().asInt();
                final int s2 = machine.pop().asInt();
                if (s2 == s1) {
                    machine.jump(offset);
                }
                break;
            }

            case IF_ICMPNE: {               // 0xA0;
                final short offset = machine.readShort();
                final int s1 = machine.pop().asInt();
                final int s2 = machine.pop().asInt();
                if (s2 != s1) {
                    machine.jump(offset);
                }
                break;
            }

            case IF_ICMPLT: {               // 0xA1;
                final short offset = machine.readShort();
                final int s1 = machine.pop().asInt();
                final int s2 = machine.pop().asInt();
                if (s2 < s1) {
                    machine.jump(offset);
                }
                break;
            }

            case IF_ICMPGE: {               // 0xA2;
                final short offset = machine.readShort();
                final int s1 = machine.pop().asInt();
                final int s2 = machine.pop().asInt();
                if (s2 >= s1) {
                    machine.jump(offset);
                }
                break;
            }

            case IF_ICMPGT: {               // 0xA3;
                final short offset = machine.readShort();
                final int s1 = machine.pop().asInt();
                final int s2 = machine.pop().asInt();
                if (s2 > s1) {
                    machine.jump(offset);
                }
                break;
            }

            case IF_ICMPLE: {               // 0xA4;
                final short offset = machine.readShort();
                final int s1 = machine.pop().asInt();
                final int s2 = machine.pop().asInt();
                if (s2 <= s1) {
                    machine.jump(offset);
                }
                break;
            }

            case IF_ACMPEQ: {               // 0xA5;
                final short offset = machine.readShort();
                final ReferenceValue s1 = (ReferenceValue) machine.pop();
                final ReferenceValue s2 = (ReferenceValue) machine.pop();
                if (s2.equals(s1)) {
                    machine.jump(offset);
                }
                break;
            }

            case IF_ACMPNE: {               // 0xA6;
                final short offset = machine.readShort();
                final ReferenceValue s1 = (ReferenceValue) machine.pop();
                final ReferenceValue s2 = (ReferenceValue) machine.pop();

                if (!s2.equals(s1)) {
                    machine.jump(offset);
                }
                break;
            }

            /*========================================================================*/

            case GOTO: {                    // 0xA7;
                final short offset = machine.readShort();
                machine.jump(offset);
                break;
            }
            case JSR: {                      // 0xA8;
                final short offset = machine.readShort();
                final int returnPosition = machine.currentThread().frame().currentBytePosition();
                machine.push(IntValue.from(returnPosition));
                machine.jump(offset);
                break;
            }
            case RET: {                     // 0xA9;
                final int index = machine.readByte();
                final int value = machine.getLocal(index).asInt();

                machine.currentThread().frame().setBytecodePosition(value);
                break;
            }

            /*========================================================================*/

            case TABLESWITCH: {             // 0xAA;
                final int index   = machine.pop().asInt();
                machine.alignInstructionPosition();
                final int defawlt = machine.readInt();
                final int low     = machine.readInt();
                final int high    = machine.readInt();

                if (index < low || index > high) {
                    machine.jump(defawlt);
                } else {
                    final int jumpTableIndex = index - low;
                    machine.skipBytes(jumpTableIndex * 4);
                    final int offset = machine.readInt();
                    machine.jump(offset);
                }

                break;
            }

            case LOOKUPSWITCH: {             // 0xAB;
                final int key     = machine.pop().asInt();
                machine.alignInstructionPosition();
                final int defawlt = machine.readInt();
                final int nPairs  = machine.readInt();

                boolean foundMatch = false;
                for (int i = 0; i < nPairs; i++) {
                    final int value = machine.readInt();
                    final int offset = machine.readInt();
                    if (value == key) {
                        machine.jump(offset);
                        foundMatch = true;
                        break;
                    }
                }

                if (!foundMatch) {
                    machine.jump(defawlt);
                }
                break;
            }

            /*========================================================================*/

            case IRETURN:                   // 0xAC;
            case LRETURN:                   // 0xAD;
            case FRETURN:                   // 0xAE;
            case DRETURN:                   // 0xAF;
            case ARETURN: {                 // 0xB0;
                final Value result = machine.pop();
                final ExecutionFrame frame = machine.popFrame();

                //if this was the topmost frame on the stack
                if (frame == null) {
                    returnValue = result;
                    return MethodStatus.METHOD_END;
                }

                machine.push(result);
                break;
            }

            case RETURN: {                  // 0xB1;
                final ExecutionFrame frame = machine.popFrame();
                if (frame == null) {
                    returnValue = VoidValue.VOID;
                    return MethodStatus.METHOD_END;
                }
                break;
            }

            /*========================================================================*/

            case GETSTATIC: {               // 0xB2;
                final short cpIndex = machine.readShort();
                try {
                    machine.push(machine.getStatic(cpIndex));
                } catch (LinkageError e) {
                    machine.raiseException(e);
                }
                break;
            }

            case PUTSTATIC: {                // 0xB5;
                final short cpIndex = machine.readShort();
                try {
                    machine.putStatic(cpIndex, machine.pop());
                } catch (LinkageError e) {
                    machine.raiseException(e);
                }
                break;
            }

            case GETFIELD: {                // 0xB4;
                final Reference instance = machine.pop().asReference();
                final short cpIndex = machine.readShort();

                if (instance.isZero()) {
                    machine.raiseException(new NullPointerException());
                }

                try {
                    machine.push(machine.getField(instance, cpIndex));
                } catch (LinkageError e) {
                    machine.raiseException(e);
                }

                break;
            }

            case PUTFIELD: {                // 0xB5;
                final Value value = machine.pop();
                final Object instance = machine.pop().asBoxedJavaValue();
                final short cpIndex = machine.readShort();

                if (instance == null) {
                    machine.raiseException(new NullPointerException());
                }

                try {
                    machine.putField(instance, cpIndex, value);
                } catch (LinkageError e) {
                    machine.raiseException(e);
                }

                break;
            }

            /*========================================================================*/

            case INVOKEVIRTUAL: {           // 0xB6;
                final short cpIndex = machine.readShort();

                try {
                    final ClassMethodActor resolveMethod = (ClassMethodActor) machine.resolveMethod(cpIndex);
                    ClassMethodActor methodActor = resolveMethod;
                    final Value value = machine.peek(methodActor.descriptor().numberOfParameters() + 1);
                    if (value instanceof ReferenceValue) {
                        final ReferenceValue receiver = (ReferenceValue) value;
                        if (receiver.isZero()) {
                            machine.raiseException(new NullPointerException());
                        }

                        final ClassActor dynamicClass = receiver.getClassActor();
                        assert dynamicClass != null;

                        methodActor = dynamicClass.findVirtualMethodActor(methodActor);
                    }

                    if (methodActor == null) {
                        final ReferenceValue receiver = (ReferenceValue) value;
                        final ClassActor dynamicClass = receiver.getClassActor();

                        methodActor = dynamicClass.findVirtualMethodActor(methodActor);
                        machine.raiseException(new AbstractMethodError());
                    } else if (methodActor.isAbstract()) {
                        machine.raiseException(new AbstractMethodError());
                    }

                    machine.invokeMethod(methodActor);
                } catch (LinkageError e) {
                    machine.raiseException(e);
                }

                break;
            }

            case INVOKESPECIAL: {           // 0xB7;
                final short cpIndex = machine.readShort();

                try {
                    final ClassMethodActor methodActor = (ClassMethodActor) machine.resolveMethod(cpIndex);
                    final ReferenceValue receiver = (ReferenceValue) machine.peek(methodActor.descriptor().numberOfParameters() + 1);

                    if (receiver.isZero()) {
                        machine.raiseException(new NullPointerException());
                    }

                    machine.invokeMethod(methodActor);
                } catch (LinkageError e) {
                    machine.raiseException(e);
                }

                break;
            }

            case INVOKESTATIC: {            // 0xB8;
                final short cpIndex = machine.readShort();

                try {
                    final ClassMethodActor methodActor = (ClassMethodActor) machine.resolveMethod(cpIndex);
                    machine.invokeMethod(methodActor);
                } catch (LinkageError e) {
                    machine.raiseException(e);
                }

                break;
            }

            case INVOKEINTERFACE: {         // 0xB9;
                final short cpIndex = machine.readShort();
                machine.readShort();

                try {
                    final InterfaceMethodActor methodActor = (InterfaceMethodActor) machine.resolveMethod(cpIndex);
                    final ReferenceValue receiver = (ReferenceValue) machine.peek(methodActor.descriptor().numberOfParameters() + 1);

                    if (receiver.isZero()) {
                        machine.raiseException(new NullPointerException());
                    }

                    final ClassActor dynamicClass = receiver.getClassActor();
                    assert dynamicClass != null;

                    if (!dynamicClass.getAllInterfaceActors().contains((InterfaceActor) methodActor.holder())) {
                        machine.raiseException(new IncompatibleClassChangeError(dynamicClass + " does not implement " + methodActor.holder()));
                    }

                    final VirtualMethodActor dynamicMethodActor = dynamicClass.findVirtualMethodActor(methodActor);

                    if (dynamicMethodActor == null) {
                        machine.raiseException(new AbstractMethodError("No such method " + methodActor + " found in " + dynamicClass));
                    } else if (dynamicMethodActor.isAbstract()) {
                        machine.raiseException(new AbstractMethodError("Method " + dynamicMethodActor + " is abstract in " + dynamicClass));
                    } else if (!dynamicMethodActor.isPublic()) {
                        machine.raiseException(new IllegalAccessError("Method " + dynamicMethodActor + " is not public in " + dynamicClass));
                    }

                    machine.invokeMethod(dynamicMethodActor);
                } catch (LinkageError e) {
                    machine.raiseException(e);
                }

                break;
            }

            /*========================================================================*/

            case XXXUNUSEDXXX:              // 0xBA;
                break;

            /*========================================================================*/

            case NEW: {                      // 0xBB;
                final short cpIndex = machine.readShort();
                final ClassActor classActor = machine.resolveClassReference(cpIndex);
                try {
                    machine.push(ReferenceValue.from(Objects.allocateInstance(classActor.toJava())));
                } catch (InstantiationException instantiationException) {
                    machine.raiseException(instantiationException);
                }
                break;
            }
            case NEWARRAY: {                // 0xBC;
                final byte arrayType = machine.readByte();
                final int arraySize  = machine.pop().asInt();

                if (arraySize < 0) {
                    machine.raiseException(new NegativeArraySizeException());
                }

                switch (arrayType) {
                    case 4:
                        machine.push(ReferenceValue.from(new boolean[arraySize]));
                        break;
                    case 5:
                        machine.push(ReferenceValue.from(new char[arraySize]));
                        break;
                    case 6:
                        machine.push(ReferenceValue.from(new float[arraySize]));
                        break;
                    case 7:
                        machine.push(ReferenceValue.from(new double[arraySize]));
                        break;
                    case 8:
                        machine.push(ReferenceValue.from(new byte[arraySize]));
                        break;
                    case 9:
                        machine.push(ReferenceValue.from(new short[arraySize]));
                        break;
                    case 10:
                        machine.push(ReferenceValue.from(new int[arraySize]));
                        break;
                    case 11:
                        machine.push(ReferenceValue.from(new long[arraySize]));
                        break;
                }

                break;
            }

            case ANEWARRAY: {               // 0xBB;
                final short cpIndex = machine.readShort();
                final int arraySize = machine.pop().asInt();

                final ClassActor classActor = machine.resolveClassReference(cpIndex);

                if (arraySize < 0) {
                    machine.raiseException(new NegativeArraySizeException());
                }

                machine.push(ReferenceValue.from(Array.newInstance(classActor.toJava(), arraySize)));
                break;
            }

            case ARRAYLENGTH: {             // 0xBE;
                final Reference array = machine.pop().asReference();

                if (array.isZero()) {
                    machine.raiseException(new NullPointerException());
                }

                machine.push(IntValue.from(Layout.readArrayLength(array)));
                break;
            }

            /*========================================================================*/

            case ATHROW: {                  // 0xBF;
                final ReferenceValue t = (ReferenceValue) machine.pop();

                if (t.isZero()) {
                    throw machine.raiseException(new NullPointerException());
                } else {
                    throw machine.raiseException(t);
                }
            }

            case CHECKCAST: {               // 0xC0;
                final short cpIndex = machine.readShort();
                final ClassActor classActor = machine.resolveClassReference(cpIndex);
                final ReferenceValue object = (ReferenceValue) machine.pop();

                if (!object.isZero()) {
                    if (!classActor.isAssignableFrom(object.getClassActor())) {
                        final String message = object.getClassActor().toJava() + " is not a subclass of " + classActor;
                        machine.raiseException(new ClassCastException(message));
                    }
                }

                machine.push(object);
                break;
            }

            case INSTANCEOF: {              // 0xC1;
                final short cpIndex = machine.readShort();
                final ClassActor classActor = machine.resolveClassReference(cpIndex);
                final ReferenceValue object = (ReferenceValue) machine.pop();

                if (object.isZero() || !classActor.isAssignableFrom(object.getClassActor())) {
                    machine.push(IntValue.from(0));
                } else {
                    machine.push(IntValue.from(1));
                }

                break;
            }

            /*========================================================================*/

            case MONITORENTER:              // 0xC2;
            case MONITOREXIT:               // 0xC3;
                machine.pop();
                break;

            /*========================================================================*/

            case WIDE: {                    // 0xC4;
                final Bytecode nextCode = machine.readOpcode();
                final short index = machine.readShort();

                switch (nextCode) {
                    case ILOAD:
                    case FLOAD:
                    case ALOAD:
                    case LLOAD:
                    case DLOAD:
                        machine.push(machine.getLocal(index));
                        break;

                    case ISTORE:
                    case FSTORE:
                    case ASTORE:
                    case LSTORE:
                    case DSTORE:
                        machine.setLocal(index, machine.pop());
                        break;

                    case IINC: {
                        final int increment = machine.readShort();
                        final int value = machine.getLocal(index).asInt();

                        machine.setLocal(index, IntValue.from(value + increment));
                        break;
                    }

                    case RET: {
                        final Value value = machine.getLocal(index);
                        machine.currentThread().frame().setBytecodePosition(value.asInt());
                        break;
                    }

                    default:
                        machine.raiseException(new ClassFormatError("Illegal wide bytecode encountered"));
                }

                break;
            }

            /*========================================================================*/

            case MULTIANEWARRAY: {          // 0xC5;
                final short cpIndex = machine.readShort();
                final ClassActor arrayClassActor = machine.resolveClassReference(cpIndex);
                final int lengthsCount = (short) (machine.readByte() & 0x7F);
                if (lengthsCount < 1) {
                    throw new ClassFormatError("dimensions operand of multianewarray is less than 1");
                }
                final int[] lengths = new int[lengthsCount];

                if (lengthsCount > arrayClassActor.numberOfDimensions()) {
                    throw new IncompatibleClassChangeError(lengthsCount + " is too many dimensions for " + arrayClassActor);
                }

                for (int i = lengthsCount - 1; i >= 0; --i) {
                    lengths[i] = machine.pop().asInt();
                    if (lengths[i] < 0) {
                        machine.raiseException(new NegativeArraySizeException());
                    }
                }

                machine.push(ReferenceValue.from(createMultiDimensionArray(arrayClassActor, 0, lengths)));
                break;
            }

            /*========================================================================*/

            case IFNULL: {                  // 0xC6;
                final int offset = machine.readShort();
                final Value r = machine.pop();

                if (r.isZero()) {
                    machine.jump(offset);
                }

                break;
            }

            case IFNONNULL: {               // 0xC7;
                final int offset = machine.readShort();
                final Value r = machine.pop();

                if (!r.isZero()) {
                    machine.jump(offset);
                }

                break;
            }

            /*========================================================================*/

            case GOTO_W:                    // 0xC8;
                machine.jump(machine.readInt());
                break;

            case JSR_W:                     // 0xC9;
                final int offset = machine.readInt();
                final int returnPosition = machine.currentThread().frame().currentBytePosition();
                machine.push(IntValue.from(returnPosition));
                machine.jump(offset);
                break;

                /*========================================================================*/

            case BREAKPOINT:                // 0xCA;
                break;

                /*========================================================================*/

            default:
                machine.raiseException(new ClassFormatError("Unsupported bytecode: " + opcode));
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
