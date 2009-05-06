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
package com.sun.max.vm.verifier;

import static com.sun.max.vm.verifier.types.VerificationType.*;

import java.io.*;

import com.sun.max.lang.*;
import com.sun.max.program.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.bytecode.*;
import com.sun.max.vm.classfile.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.classfile.stackmap.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.verifier.types.*;

/**
 * An implementation of the bytecode verifier specified in 4.11.1 of <a href="http://jcp.org/en/jsr/detail?id=202">JSR
 * 202: Java Class File Specification Update</a>.
 *
 * @author David Liu
 * @author Doug Simon
 */
public class TypeCheckingMethodVerifier extends MethodVerifier {

    public TypeCheckingMethodVerifier(ClassVerifier classVerifier, ClassMethodActor classMethodActor, CodeAttribute codeAttribute) {
        super(classVerifier, classMethodActor, codeAttribute);
        final int codeLength = codeAttribute.code().length;
        _interpreter = new Interpreter();
        _thisObjectType = classVerifier.getObjectType(classActor().typeDescriptor());
        _frame = createInitialFrame(classMethodActor);
        _frameMap = initializeFrameMap(codeAttribute, _frame.copy(), classVerifier);
        _opcodeMap = new Bytecode[codeLength];
        _exceptionHandlerMap = ExceptionHandler.createHandlerMap(codeLength, codeAttribute.exceptionHandlerTable());
    }

    protected Frame createInitialFrame(MethodActor classMethodActor) {
        return new Frame(classMethodActor, this);
    }

    protected final Interpreter _interpreter;

    protected final ObjectType _thisObjectType;

    protected boolean _fallsThrough;

    /**
     * The current frame state derived during the abstract interpretation.
     */
    protected final Frame _frame;

    /**
     * A map from each bytecode position to the {@linkplain Frame frame state} recorded (in a
     * {@link StackMapTable}) at that position. A null entry means that there is no recorded stack map frame at
     * that position.
     */
    protected final Frame[] _frameMap;

    protected final ExceptionHandler[] _exceptionHandlerMap;

    /**
     * A map from each bytecode position to the instruction {@linkplain Bytecode opcode} at that position. A null entry
     * means that an instruction does not start at that position.
     */
    private final Bytecode[] _opcodeMap;

    @Override
    public void verify() {
        if (classVerifier().verbose()) {
            final PrintStream out = Trace.stream();
            out.println();
            out.println("Verifying " + classMethodActor().format("%H.%n(%p)"));

            String prefix = "StackMapTable frames:";
            for (int i = 0; i != _frameMap.length; ++i) {
                final Frame recordedFrame = _frameMap[i];
                if (recordedFrame != null) {
                    if (prefix != null) {
                        out.println(prefix);
                        prefix = null;
                    }
                    out.println(i + ": " + recordedFrame);
                }
            }

            out.println();
            out.println("Interpreting bytecode:");
        }

        verifyBytecodes();
        verifyExceptionHandlers();
        verifyStackMapTable();
    }

    protected Frame[] initializeFrameMap(CodeAttribute codeAttribute, Frame initialFrame, ClassVerifier classVerifier) {
        final StackMapTable stackMapTable = codeAttribute.stackMapTable();
        final Frame[] frameMap = new Frame[codeAttribute.code().length];
        frameMap[0] = initialFrame;

        if (stackMapTable != null) {
            final StackMapFrame[] stackMapFrames = stackMapTable.getFrames(classVerifier);
            int previousFramePosition = -1;
            Frame previousFrame = initialFrame;
            for (int frameIndex = 0; frameIndex != stackMapFrames.length; ++frameIndex) {
                final Frame frame = previousFrame.copy();
                final StackMapFrame stackMapFrame = stackMapFrames[frameIndex];
                stackMapFrame.applyTo(frame);
                final int position = stackMapFrame.getPosition(previousFramePosition);
                try {
                    frameMap[position] = frame;
                } catch (ArrayIndexOutOfBoundsException arrayIndexOutOfBoundsException) {
                    throw verifyError("Invalid bytecode position (" + position + ") in frame " + frameIndex + " of StackMapTable attribute");
                }
                previousFrame = frame;
                previousFramePosition = position;
            }
        }
        return frameMap;
    }

    private void verifyBytecodes() {
        final BytecodeScanner bytecodeScanner = new BytecodeScanner(_interpreter);
        bytecodeScanner.scan(new BytecodeBlock(codeAttribute().code()));
        if (_fallsThrough) {
            throw verifyError("Execution falls off end of method");
        }
    }

    protected void verifyExceptionHandlers() {
        for (ExceptionHandlerEntry info : codeAttribute().exceptionHandlerTable()) {
            verifyExceptionHandler(info);
        }
    }

    protected void verifyExceptionHandler(ExceptionHandlerEntry info) throws VerifyError {
        final int catchTypeIndex = info.catchTypeIndex();
        if (catchTypeIndex != 0) {
            final ObjectType catchType = classVerifier().getObjectType(constantPool().classAt(catchTypeIndex).typeDescriptor());
            verifyIsAssignable(catchType, THROWABLE, "Invalid catch type in exception handler");
        }
        verifyIsValidInstructionPosition(info.handlerPosition(), "handler_pc in exception handler");
        verifyIsValidInstructionPosition(info.startPosition(), "start_pc in exception handler");
        if (info.endPosition() != codeAttribute().code().length) {
            verifyIsValidInstructionPosition(info.endPosition(), "end_pc in exception handler");
        }
        if (info.startPosition() >= info.endPosition()) {
            throw verifyError("Exception handler has a start_pc (" + info.startPosition() + ") not less than end_pc (" + info.endPosition() + ")");
        }
    }

    private void verifyStackMapTable() {
        for (int position = 0; position != _frameMap.length; ++position) {
            final Frame recordedFrame = _frameMap[position];
            if (recordedFrame != null) {
                if (_opcodeMap[position] == null) {
                    throw verifyError("Offset (" +  position + ") in a frame of the StackMapTable attribute does not point to an instruction");
                }
            }
        }
    }

    protected void verifyIsValidInstructionPosition(int position, String positionDescription) {
        try {
            if (_opcodeMap[position] != null) {
                return;
            }
        } catch (ArrayIndexOutOfBoundsException arrayIndexOutOfBoundsException) {
        }
        throw verifyError("Invalid bytecode position " + position + "(" + positionDescription + ")");
    }

    /**
     * Gets the frame at a given bytecode position.
     *
     * @param targetDescription if there is not a recorded frame at {@code position}, then a verification error will be
     *            raised and its detail message will incorporate this description of the location for which a frame
     *            should have existed
     */
    protected Frame frameAt(int position, String targetDescription) {
        try {
            final Frame frame = _frameMap[position];
            if (frame != null) {
                return frame;
            }
        } catch (ArrayIndexOutOfBoundsException arrayIndexOutOfBoundsException) {
        }
        throw verifyError("Missing stackmap frame for bytecode position " + position + " (" + targetDescription + ")");
    }

    @Override
    public int currentOpcodePosition() {
        return _interpreter.currentOpcodePosition();
    }

    /**
     * Called after an opcode has been decoded but before the relevant opcode visitor method is called.
     */
    protected void preInstructionScan() {
        final int currentOpcodePosition = currentOpcodePosition();

        if (!_fallsThrough) {
            final Frame targetFrame = frameAt(currentOpcodePosition, "instruction can only be reached by a branch");
            if (targetFrame != _frame) {
                _frame.reset(targetFrame);
            }
        } else {
            if (currentOpcodePosition < _frameMap.length) {
                final Frame recordedFrame = _frameMap[currentOpcodePosition];
                if (recordedFrame != null) {
                    recordedFrame.mergeFrom(_frame, currentOpcodePosition, -1);
                    _frame.reset(recordedFrame);
                }
            }
        }

        if (classVerifier().verbose()) {
            final PrintStream out = Trace.stream();
            out.println(Strings.indent(_frame.toString(), "    "));
            out.println(currentOpcodePosition + ": " + _interpreter.currentOpcode());
            out.println();
        }

        _opcodeMap[currentOpcodePosition] = _interpreter.currentOpcode();

        for (ExceptionHandler handler = _exceptionHandlerMap[currentOpcodePosition]; handler != null; handler = handler.next()) {
            final int handlerPosition = handler.position();
            final Frame handlerEntryFrame = frameAt(handlerPosition, "exception handler entry point");
            handlerEntryFrame.mergeFrom(_frame, handlerPosition, handler.catchTypeIndex());
        }

        _fallsThrough = true;
    }

    public void push(TypeDescriptor typeDescriptor) {
        _frame.push(getVerificationType(typeDescriptor));
    }

    /**
     * Interprets a type conversion instruction.
     *
     * @param fromType the expected type of the value on top of the stack before the conversion
     * @param toType the type of the value on top of the stack after the conversion
     */
    void performConversion(VerificationType fromType, VerificationType toType) {
        _frame.pop(fromType);
        _frame.push(toType);
    }

    /**
     * Interprets a binary arithmetic instruction.
     *
     * @param type the expected type of the two input values on top of the stack before the operation as well as the
     *            type of the result on top of the stack after the operation
     */
    void performArithmetic(VerificationType type) {
        _frame.pop(type);
        _frame.pop(type);
        _frame.push(type);
    }

    /**
     * Interprets a binary comparison instruction.
     *
     * @param type the expected type of the two input values on top of the stack to be compared before the operation
     */
    void performComparison(VerificationType type) {
        _frame.pop(type);
        _frame.pop(type);
        _frame.push(INTEGER);
    }

    /**
     * Interprets returning from the method.
     */
    void performReturn(VerificationType returnType) {
        final VerificationType declaredReturnType = getVerificationType(classMethodActor().descriptor().resultDescriptor());
        if (declaredReturnType == TOP) {
            if (returnType != TOP) {
                throw verifyError("Invalid return for void method");
            }
        } else {
            final VerificationType returnValue = _frame.pop(returnType);
            verifyIsAssignable(returnValue, declaredReturnType, "Invalid return type");
        }

        _fallsThrough = false;
    }

    /**
     * Verifies that the current type state is {@linkplain Frame#mergeInto(Frame, int) compatible} with the type state
     * at the destination of a control flow instruction.
     *
     * @param position the destination of a control flow instruction
     */
    protected void performBranch(int position) {
        final Frame targetFrame = frameAt(position, "branch target");
        targetFrame.mergeFrom(_frame, position, -1);
    }

    protected void performIfCompareBranch(int offset, VerificationType type) {
        _frame.pop(type);
        _frame.pop(type);
        performBranch(currentOpcodePosition() + offset);
    }

    void performIfBranch(int offset, VerificationType type) {
        _frame.pop(type);
        performBranch(currentOpcodePosition() + offset);
    }

    protected void performStore(VerificationType type, int index) {
        _frame.store(_frame.pop(type), index);
    }

    public void performLoad(VerificationType type, int index) {
        _frame.push(_frame.load(type, index));
    }

    protected void performJsr(int offset) {
        throw verifyError("JSR instruction is not supported");
    }

    protected void performRet(int index) {
        throw verifyError("RET instruction is not supported");
    }

    /**
     * The abstract interpreter that simulates the JVM instructions at the level of types (as opposed to values).
     */
    class Interpreter extends BytecodeVisitor {

        private boolean _constructorInvoked;

        @Override
        public void opcodeDecoded() {
            preInstructionScan();
        }

        @Override
        public void aaload() {
            _frame.pop(INTEGER);
            final VerificationType array = _frame.pop(OBJECT_ARRAY);
            if (array != NULL) {
                final VerificationType element = array.componentType();
                _frame.push(element);
            } else {
                _frame.push(NULL);
            }
        }

        @Override
        public void aastore() {
            _frame.pop(OBJECT);
            _frame.pop(INTEGER); // index
            _frame.pop(OBJECT_ARRAY);
            // The remaining type check is done at runtime, throwing ArrayStoreException if it fails
        }

        @Override
        public void aconst_null() {
            _frame.push(NULL);
        }

        @Override
        public void aload(int index) {
            performLoad(REFERENCE, index);
        }

        @Override
        public void aload_0() {
            aload(0);
        }

        @Override
        public void aload_1() {
            aload(1);
        }

        @Override
        public void aload_2() {
            aload(2);
        }

        @Override
        public void aload_3() {
            aload(3);
        }

        @Override
        public void anewarray(int index) {
            _frame.pop(INTEGER);
            final TypeDescriptor elementDescriptor = constantPool().classAt(index).typeDescriptor();
            try {
                final ObjectType element = (ObjectType) getVerificationType(elementDescriptor);
                _frame.push(getObjectType(JavaTypeDescriptor.getArrayDescriptorForDescriptor(element.typeDescriptor(), 1)));
            } catch (ClassCastException classCastException) {
                throw verifyError("Invalid use of primitive type in ANEWARRAY: " + elementDescriptor);
            }
        }

        @Override
        public void areturn() {
            performReturn(REFERENCE);
        }

        @Override
        public void arraylength() {
            final VerificationType array = _frame.pop(REFERENCE);
            if (array != NULL && !array.isArray()) {
                throw verifyError("Require array type in ARRAYLENGTH");
            }
            _frame.push(INTEGER);
        }

        @Override
        public void astore(int index) {
            performStore(REFERENCE, index);
        }

        @Override
        public void astore_0() {
            astore(0);
        }

        @Override
        public void astore_1() {
            astore(1);
        }

        @Override
        public void astore_2() {
            astore(2);
        }

        @Override
        public void astore_3() {
            astore(3);
        }

        @Override
        public void athrow() {
            _frame.pop(THROWABLE);
            _fallsThrough = false;
        }

        @Override
        public void baload() {
            _frame.pop(INTEGER);
            final VerificationType array = _frame.pop(REFERENCE);
            if (array != BYTE_ARRAY && array != BOOLEAN_ARRAY && array != NULL) {
                throw verifyError("Invalid array type for BALOAD");
            }
            _frame.push(BYTE);
        }

        @Override
        public void bastore() {
            _frame.pop(INTEGER);
            _frame.pop(INTEGER);
            final VerificationType array = _frame.pop(REFERENCE);
            if (array != BYTE_ARRAY && array != BOOLEAN_ARRAY && array != NULL) {
                throw verifyError("Invalid array type for BASTORE");
            }
        }

        @Override
        public void bipush(int operand) {
            _frame.push(INTEGER);
        }

        @Override
        public void breakpoint() {
        }

        @Override
        public void caload() {
            _frame.pop(INTEGER); // index
            _frame.pop(CHAR_ARRAY); // array
            _frame.push(CHAR); // value
        }

        @Override
        public void castore() {
            _frame.pop(CHAR); // value
            _frame.pop(INTEGER); // index
            _frame.pop(CHAR_ARRAY); // array
        }

        @Override
        public void checkcast(int index) {
            _frame.pop(REFERENCE);

            final ClassConstant classConstant = constantPool().classAt(index);
            final TypeDescriptor toType = classConstant.typeDescriptor();
            if (JavaTypeDescriptor.isPrimitive(toType)) {
                throw verifyError("Invalid use of primitive type in CHECKCAST: " + toType);
            }
            _frame.push(getObjectType(toType));
        }

        @Override
        public void d2f() {
            performConversion(DOUBLE, FLOAT);
        }

        @Override
        public void d2i() {
            performConversion(DOUBLE, INTEGER);
        }

        @Override
        public void d2l() {
            performConversion(DOUBLE, LONG);
        }

        @Override
        public void dadd() {
            performArithmetic(DOUBLE);
        }

        @Override
        public void daload() {
            _frame.pop(INTEGER); // index
            _frame.pop(DOUBLE_ARRAY); // array
            _frame.push(DOUBLE); // value
        }

        @Override
        public void dastore() {
            _frame.pop(DOUBLE); // value
            _frame.pop(INTEGER); // index
            _frame.pop(DOUBLE_ARRAY); // array
        }

        @Override
        public void dcmpg() {
            performComparison(DOUBLE);
        }

        @Override
        public void dcmpl() {
            performComparison(DOUBLE);
        }

        @Override
        public void dconst_0() {
            _frame.push(DOUBLE);
        }

        @Override
        public void dconst_1() {
            _frame.push(DOUBLE);
        }

        @Override
        public void ddiv() {
            performArithmetic(DOUBLE);
        }

        @Override
        public void dload(int index) {
            performLoad(DOUBLE, index);
        }

        @Override
        public void dload_0() {
            dload(0);
        }

        @Override
        public void dload_1() {
            dload(1);
        }

        @Override
        public void dload_2() {
            dload(2);
        }

        @Override
        public void dload_3() {
            dload(3);
        }

        @Override
        public void dmul() {
            performArithmetic(DOUBLE);
        }

        @Override
        public void dneg() {
            verifyIsAssignable(DOUBLE, _frame.top(), "Invalid double negation");
        }

        @Override
        public void drem() {
            performArithmetic(DOUBLE);
        }

        @Override
        public void dreturn() {
            performReturn(DOUBLE);
        }

        @Override
        public void dstore(int index) {
            performStore(DOUBLE, index);
        }

        @Override
        public void dstore_0() {
            dstore(0);
        }

        @Override
        public void dstore_1() {
            dstore(1);
        }

        @Override
        public void dstore_2() {
            dstore(2);
        }

        @Override
        public void dstore_3() {
            dstore(3);
        }

        @Override
        public void dsub() {
            performArithmetic(DOUBLE);
        }

        @Override
        public void dup() {
            final VerificationType value = _frame.pop(CATEGORY1);
            _frame.push(value);
            _frame.push(value);
        }

        @Override
        public void dup2() {
            if (!_frame.top().isCategory2()) {
                final VerificationType value1 = _frame.pop(CATEGORY1);
                final VerificationType value2 = _frame.pop(CATEGORY1);
                _frame.push(value2);
                _frame.push(value1);
                _frame.push(value2);
                _frame.push(value1);
            } else {
                final VerificationType value = _frame.pop(CATEGORY2);
                _frame.push(value);
                _frame.push(value);
            }
        }

        @Override
        public void dup2_x1() {
            if (!_frame.top().isCategory2()) {
                final VerificationType value1 = _frame.pop(CATEGORY1);
                final VerificationType value2 = _frame.pop(CATEGORY1);
                final VerificationType value3 = _frame.pop(CATEGORY1);
                _frame.push(value2);
                _frame.push(value1);
                _frame.push(value3);
                _frame.push(value2);
                _frame.push(value1);
            } else {
                final VerificationType value1 = _frame.pop(CATEGORY2);
                final VerificationType value2 = _frame.pop(TOP);
                _frame.push(value1);
                _frame.push(value2);
                _frame.push(value1);
            }
        }

        @Override
        public void dup2_x2() {
            if (!_frame.top().isCategory2()) {
                final VerificationType value1 = _frame.pop(CATEGORY1);
                final VerificationType value2 = _frame.pop(CATEGORY1);
                if (!_frame.top().isCategory2()) {
                    final VerificationType value3 = _frame.pop(CATEGORY1);
                    final VerificationType value4 = _frame.pop(CATEGORY1);
                    _frame.push(value2);
                    _frame.push(value1);
                    _frame.push(value4);
                    _frame.push(value3);
                } else {
                    final VerificationType value3 = _frame.pop(CATEGORY2);
                    _frame.push(value2);
                    _frame.push(value1);
                    _frame.push(value3);
                }
                _frame.push(value2);
                _frame.push(value1);
            } else {
                final VerificationType value1 = _frame.pop(CATEGORY2);
                if (!_frame.top().isCategory2()) {
                    final VerificationType value2 = _frame.pop(CATEGORY1);
                    final VerificationType value3 = _frame.pop(CATEGORY1);
                    _frame.push(value1);
                    _frame.push(value3);
                    _frame.push(value2);
                    _frame.push(value1);
                } else {
                    final VerificationType value2 = _frame.pop(CATEGORY2);
                    _frame.push(value1);
                    _frame.push(value2);
                    _frame.push(value1);
                }
            }
        }

        @Override
        public void dup_x1() {
            final VerificationType value1 = _frame.pop(CATEGORY1);
            final VerificationType value2 = _frame.pop(CATEGORY1);
            _frame.push(value1);
            _frame.push(value2);
            _frame.push(value1);
        }

        @Override
        public void dup_x2() {
            final VerificationType value1 = _frame.pop(CATEGORY1);
            if (!_frame.top().isCategory2()) {
                final VerificationType value2 = _frame.pop(CATEGORY1);
                final VerificationType value3 = _frame.pop(CATEGORY1);
                _frame.push(value1);
                _frame.push(value3);
                _frame.push(value2);
                _frame.push(value1);
            } else {
                final VerificationType value2 = _frame.pop(CATEGORY2);
                _frame.push(value1);
                _frame.push(value2);
                _frame.push(value1);
            }
        }

        @Override
        public void f2d() {
            performConversion(FLOAT, DOUBLE);
        }

        @Override
        public void f2i() {
            performConversion(FLOAT, INTEGER);
        }

        @Override
        public void f2l() {
            performConversion(FLOAT, LONG);
        }

        @Override
        public void fadd() {
            performArithmetic(FLOAT);
        }

        @Override
        public void faload() {
            _frame.pop(INTEGER); // index
            _frame.pop(FLOAT_ARRAY); // array
            _frame.push(FLOAT); // value
        }

        @Override
        public void fastore() {
            _frame.pop(FLOAT); // value
            _frame.pop(INTEGER); // index
            _frame.pop(FLOAT_ARRAY); // array
        }

        @Override
        public void fcmpg() {
            performComparison(FLOAT);
        }

        @Override
        public void fcmpl() {
            performComparison(FLOAT);
        }

        @Override
        public void fconst_0() {
            _frame.push(FLOAT);
        }

        @Override
        public void fconst_1() {
            _frame.push(FLOAT);
        }

        @Override
        public void fconst_2() {
            _frame.push(FLOAT);
        }

        @Override
        public void fdiv() {
            performArithmetic(FLOAT);
        }

        @Override
        public void fload(int index) {
            performLoad(FLOAT, index);
        }

        @Override
        public void fload_0() {
            fload(0);
        }

        @Override
        public void fload_1() {
            fload(1);
        }

        @Override
        public void fload_2() {
            fload(2);
        }

        @Override
        public void fload_3() {
            fload(3);
        }

        @Override
        public void fmul() {
            performArithmetic(FLOAT);
        }

        @Override
        public void fneg() {
            verifyIsAssignable(FLOAT, _frame.top(), "Invalid negation");
        }

        @Override
        public void frem() {
            performArithmetic(FLOAT);
        }

        @Override
        public void freturn() {
            performReturn(FLOAT);
        }

        @Override
        public void fstore(int index) {
            performStore(FLOAT, index);
        }

        @Override
        public void fstore_0() {
            fstore(0);
        }

        @Override
        public void fstore_1() {
            fstore(1);
        }

        @Override
        public void fstore_2() {
            fstore(2);
        }

        @Override
        public void fstore_3() {
            fstore(3);
        }

        @Override
        public void fsub() {
            performArithmetic(FLOAT);
        }

        @Override
        public void getfield(int index) {
            final FieldRefConstant fieldConstant = constantPool().fieldAt(index);
            final TypeDescriptor fieldType = fieldConstant.type(constantPool());
            final VerificationType value = getVerificationType(fieldType);
            final VerificationType object = _frame.pop(getObjectType(fieldConstant.holder(constantPool())));
            protectedFieldAccessCheck(object, fieldConstant, index);
            _frame.push(value);
        }

        @Override
        public void getstatic(int index) {
            final FieldRefConstant fieldConstant = constantPool().fieldAt(index);
            final VerificationType value = getVerificationType(fieldConstant.type(constantPool()));
            _frame.push(value);
        }

        /**
         * Verifies that a non-static field access does not violate {@code protected} access control semantics.
         *
         * @param object the type of the object whose field is being accessed. The caller has already guaranteed that
         *            this type is assignable to the field's holder
         * @param fieldRef the field being accessed
         * @param index the constant pool index of the field reference
         */
        private void protectedFieldAccessCheck(VerificationType object, FieldRefConstant fieldRef, int index) {
            // Accessing a field from the current class is always okay.
            if (object == _thisObjectType) {
                return;
            }

            final TypeDescriptor fieldHolder = fieldRef.holder(constantPool());

            ClassActor superClassActor = classActor().superClassActor();
            while (superClassActor != null) {
                if (superClassActor.typeDescriptor().equals(fieldHolder)) {
                    // Accessing a field from a super class of the current class.
                    final FieldActor fieldActor = fieldRef.resolve(constantPool(), index);
                    if (!fieldActor.isProtected()) {
                        break;
                    } else if (!classActor().packageName().equals(fieldActor.holder().packageName())) {
                        verifyIsAssignable(object, _thisObjectType, "Invalid access of protected field");
                    }

                    // Accessing this field is okay
                    return;
                }
                superClassActor = superClassActor.superClassActor();
            }

            // The field being accessed belongs to a class that isn't a superclass of the current class.
            // The access control check will be performed at run time as part of field resolution.
        }

        @Override
        public void goto_(int offset) {
            performBranch(currentOpcodePosition() + offset);
            _fallsThrough = false;
        }

        @Override
        public void goto_w(int offset) {
            goto_(offset);
        }

        @Override
        public void jsr_w(int offset) {
            jsr(offset);
        }

        @Override
        public void i2b() {
            performConversion(INTEGER, BYTE);
        }

        @Override
        public void i2c() {
            performConversion(INTEGER, CHAR);
        }

        @Override
        public void i2d() {
            performConversion(INTEGER, DOUBLE);
        }

        @Override
        public void i2f() {
            performConversion(INTEGER, FLOAT);
        }

        @Override
        public void i2l() {
            performConversion(INTEGER, LONG);
        }

        @Override
        public void i2s() {
            performConversion(INTEGER, SHORT);
        }

        @Override
        public void iadd() {
            performArithmetic(INTEGER);
        }

        @Override
        public void iaload() {
            _frame.pop(INTEGER); // index
            _frame.pop(INTEGER_ARRAY); // array
            _frame.push(INTEGER); // value
        }

        @Override
        public void iand() {
            performArithmetic(INTEGER);
        }

        @Override
        public void iastore() {
            _frame.pop(INTEGER); // value
            _frame.pop(INTEGER); // index
            _frame.pop(INTEGER_ARRAY); // array
        }

        @Override
        public void iconst_0() {
            _frame.push(INTEGER);
        }

        @Override
        public void iconst_1() {
            _frame.push(INTEGER);
        }

        @Override
        public void iconst_2() {
            _frame.push(INTEGER);
        }

        @Override
        public void iconst_3() {
            _frame.push(INTEGER);
        }

        @Override
        public void iconst_4() {
            _frame.push(INTEGER);
        }

        @Override
        public void iconst_5() {
            _frame.push(INTEGER);
        }

        @Override
        public void iconst_m1() {
            _frame.push(INTEGER);
        }

        @Override
        public void idiv() {
            performArithmetic(INTEGER);
        }

        @Override
        public void if_acmpeq(int offset) {
            performIfCompareBranch(offset, REFERENCE);
        }

        @Override
        public void if_acmpne(int offset) {
            performIfCompareBranch(offset, REFERENCE);
        }

        @Override
        public void if_icmpeq(int offset) {
            performIfCompareBranch(offset, INTEGER);
        }

        @Override
        public void if_icmpge(int offset) {
            performIfCompareBranch(offset, INTEGER);
        }

        @Override
        public void if_icmpgt(int offset) {
            performIfCompareBranch(offset, INTEGER);
        }

        @Override
        public void if_icmple(int offset) {
            performIfCompareBranch(offset, INTEGER);
        }

        @Override
        public void if_icmplt(int offset) {
            performIfCompareBranch(offset, INTEGER);
        }

        @Override
        public void if_icmpne(int offset) {
            performIfCompareBranch(offset, INTEGER);
        }

        @Override
        public void ifeq(int offset) {
            performIfBranch(offset, INTEGER);
        }

        @Override
        public void ifge(int offset) {
            performIfBranch(offset, INTEGER);
        }

        @Override
        public void ifgt(int offset) {
            performIfBranch(offset, INTEGER);
        }

        @Override
        public void ifle(int offset) {
            performIfBranch(offset, INTEGER);
        }

        @Override
        public void iflt(int offset) {
            performIfBranch(offset, INTEGER);
        }

        @Override
        public void ifne(int offset) {
            performIfBranch(offset, INTEGER);
        }

        @Override
        public void ifnonnull(int offset) {
            performIfBranch(offset, REFERENCE);
        }

        @Override
        public void ifnull(int offset) {
            performIfBranch(offset, REFERENCE);
        }

        @Override
        public void iinc(int index, int addend) {
            _frame.load(INTEGER, index);
        }

        @Override
        public void iload(int index) {
            performLoad(INTEGER, index);
        }

        @Override
        public void iload_0() {
            iload(0);
        }

        @Override
        public void iload_1() {
            iload(1);
        }

        @Override
        public void iload_2() {
            iload(2);
        }

        @Override
        public void iload_3() {
            iload(3);
        }

        @Override
        public void imul() {
            performArithmetic(INTEGER);
        }

        @Override
        public void ineg() {
            verifyIsAssignable(INTEGER, _frame.top(), "Invalid negation");
        }

        @Override
        public void instanceof_(int index) {
            constantPool().classAt(index);
            _frame.pop(REFERENCE);
            _frame.push(INTEGER);
        }

        private int popMethodParameters(SignatureDescriptor methodSignature) {
            final int numberOfParameters = methodSignature.numberOfParameters();
            int count = 0;
            for (int n = numberOfParameters - 1; n >= 0; n--) {
                final VerificationType parameter = _frame.pop(getVerificationType(methodSignature.parameterDescriptorAt(n)));
                count += parameter.size();
            }
            return count;
        }

        private void pushMethodResult(SignatureDescriptor methodSignature) {
            final VerificationType returnType = getVerificationType(methodSignature.resultDescriptor());
            if (returnType != TOP) {
                _frame.push(returnType);
            }
        }

        /**
         * Verifies that type of the parameter for {@code this} in a non-static method is correct.
         *
         * @param callee the non-static method being invoked
         * @param thisParameter the parameter for {@code this}
         */
        private void protectedMethodAccessCheck(VerificationType receiver, MethodRefConstant methodRef, int index) {
            // Accessing a method from the current class is always okay.
            if (receiver == _thisObjectType) {
                return;
            }

            final TypeDescriptor methodHolder = methodRef.holder(constantPool());

            ClassActor superClassActor = classActor().superClassActor();
            while (superClassActor != null) {
                if (superClassActor.typeDescriptor().equals(methodHolder)) {
                    // Accessing a method from a super class of the current class.
                    final MethodActor methodActor = methodRef.resolve(constantPool(), index);
                    if (!methodActor.isProtected()) {
                        break;
                    } else if (!classActor().packageName().equals(methodActor.holder().packageName())) {
                        if (receiver.isArray() && methodActor.holder() == ClassRegistry.javaLangObjectActor() && methodActor.name().toString().equals("clone")) {
                            // Special case: arrays pretend to implement public Object clone().
                            break;
                        }
                        verifyIsAssignable(receiver, _thisObjectType, "Invalid access of protected method");
                    }

                    // Accessing this method is okay
                    return;
                }
                superClassActor = superClassActor.superClassActor();
            }

            // The method being accessed belongs to a class that isn't a superclass of the current class.
            // The access control check will be performed at run time as part of method resolution.
        }

        @Override
        public void invokeinterface(int index, int count) {
            final InterfaceMethodRefConstant methodConstant = constantPool().interfaceMethodAt(index);
            final Utf8Constant methodName = methodConstant.name(constantPool());
            if (methodName.toString().startsWith("<")) {
                throw verifyError("Invalid INVOKEINTERFACE on initialization method");
            }

            final SignatureDescriptor methodSignature = methodConstant.signature(constantPool());
            final int actualCount = popMethodParameters(methodSignature) + 1;
            _frame.pop(OBJECT);

            if (actualCount != count) {
                throw verifyError("INVOKEINTERFACE count operand does not match method signature");
            }

            final TypeDescriptor holder = methodConstant.holder(constantPool());
            if (holder.equals(JavaTypeDescriptor.OBJECT)) {
                // This is a case of invokeinterface being used to invoke a virtual method
                // declared in java.lang.Object. While this is perfectly legal, it complicates
                // the compilation or interpretation of invokeinterface. What's more, no sane
                // Java source compiler will produce such code. As such, the instruction is
                // re-written to use invokevirtual instead.
                final byte[] code = bytecodeScanner().bytecodeBlock().code();
                code[bytecodeScanner().currentOpcodePosition()] = (byte) Bytecode.INVOKEVIRTUAL.ordinal();
            }

            pushMethodResult(methodSignature);
        }

        @Override
        public void callnative(int nativeFunctionDescriptorIndex) {
            final SignatureDescriptor nativeFunctionDescriptor = SignatureDescriptor.create(constantPool().utf8At(nativeFunctionDescriptorIndex, "native function descriptor"));
            popMethodParameters(nativeFunctionDescriptor);
            pushMethodResult(nativeFunctionDescriptor);
        }

        @Override
        public void invokespecial(int index) {
            final MethodRefConstant methodConstant = constantPool().methodAt(index);
            final Utf8Constant name = methodConstant.name(constantPool());
            if (name.equals(SymbolTable.CLINIT)) {
                throw verifyError("Cannot invoke <clinit> method");
            }

            final SignatureDescriptor methodSignature = methodConstant.signature(constantPool());
            popMethodParameters(methodSignature);

            if (name.equals(SymbolTable.INIT)) {
                if (methodSignature.resultDescriptor() != JavaTypeDescriptor.VOID) {
                    throw verifyError("<init> must return void");
                }

                final UninitializedType uninitializedObject = (UninitializedType) _frame.pop(UNINITIALIZED);
                final ObjectType initializedObject;

                if (uninitializedObject instanceof UninitializedNewType) {
                    final UninitializedNewType object = (UninitializedNewType) uninitializedObject;
                    initializedObject = getObjectType(getTypeDescriptorFromNewBytecode(object.position()));
                } else {
                    assert uninitializedObject instanceof UninitializedThisType;
                    initializedObject = getObjectType(classActor().typeDescriptor());
                    _constructorInvoked = true;
                }

                _frame.replaceStack(uninitializedObject, initializedObject);
                _frame.replaceLocals(uninitializedObject, initializedObject);

                protectedMethodAccessCheck(initializedObject, methodConstant, index);
            } else {
                final VerificationType object = _frame.pop(getObjectType(methodConstant.holder(constantPool())));
                verifyIsAssignable(object, _thisObjectType, "Invalid use of INVOKESPECIAL");
                pushMethodResult(methodSignature);
            }
        }

        /**
         * Gets the type of object constructed by a {@link Bytecode#NEW} instruction at a given position.
         */
        private TypeDescriptor getTypeDescriptorFromNewBytecode(int position) {
            final byte[] bytecodes = bytecodeScanner().bytecodeBlock().code();
            try {
                final int constantPoolIndex = ((bytecodes[position + 1] & 0xFF) << 8) | (bytecodes[position + 2] & 0xFF);
                return constantPool().classAt(constantPoolIndex).typeDescriptor();
            } catch (ArrayIndexOutOfBoundsException arrayIndexOutOfBoundsException) {
                throw verifyError("Invalid NEW instruction at bytecode position " + position);
            }
        }

        @Override
        public void invokestatic(int index) {
            final MethodRefConstant methodConstant = constantPool().methodAt(index);
            if (methodConstant.name(constantPool()).toString().startsWith("<")) {
                throw verifyError("Invalid INVOKESTATIC on initialization method");
            }
            final SignatureDescriptor methodSignature = methodConstant.signature(constantPool());
            popMethodParameters(methodSignature);
            pushMethodResult(methodSignature);
        }

        @Override
        public void invokevirtual(int index) {
            final MethodRefConstant methodConstant = constantPool().methodAt(index);
            if (methodConstant.name(constantPool()).toString().startsWith("<")) {
                throw verifyError("Invalid INVOKEVIRTUAL on initialization method");
            }

            final SignatureDescriptor methodSignature = methodConstant.signature(constantPool());
            popMethodParameters(methodSignature);
            final ObjectType receiverObject = (ObjectType) _frame.pop(getObjectType(methodConstant.holder(constantPool())));
            protectedMethodAccessCheck(receiverObject, methodConstant, index);
            pushMethodResult(methodSignature);
        }

        @Override
        public void ior() {
            performArithmetic(INTEGER);
        }

        @Override
        public void irem() {
            performArithmetic(INTEGER);
        }

        @Override
        public void ireturn() {
            performReturn(INTEGER);
        }

        @Override
        public void ishl() {
            performArithmetic(INTEGER);
        }

        @Override
        public void ishr() {
            performArithmetic(INTEGER);
        }

        @Override
        public void istore(int index) {
            performStore(INTEGER, index);
        }

        @Override
        public void istore_0() {
            istore(0);
        }

        @Override
        public void istore_1() {
            istore(1);
        }

        @Override
        public void istore_2() {
            istore(2);
        }

        @Override
        public void istore_3() {
            istore(3);
        }

        @Override
        public void isub() {
            performArithmetic(INTEGER);
        }

        @Override
        public void iushr() {
            performArithmetic(INTEGER);
        }

        @Override
        public void ixor() {
            performArithmetic(INTEGER);
        }

        @Override
        public void jsr(int offset) {
            performJsr(offset);
        }

        @Override
        public void l2d() {
            performConversion(LONG, DOUBLE);
        }

        @Override
        public void l2f() {
            performConversion(LONG, FLOAT);
        }

        @Override
        public void l2i() {
            performConversion(LONG, INTEGER);
        }

        @Override
        public void ladd() {
            performArithmetic(LONG);
        }

        @Override
        public void laload() {
            _frame.pop(INTEGER); // index
            _frame.pop(LONG_ARRAY); // array
            _frame.push(LONG); // value
        }

        @Override
        public void land() {
            performArithmetic(LONG);
        }

        @Override
        public void lastore() {
            _frame.pop(LONG); // value
            _frame.pop(INTEGER); // index
            _frame.pop(LONG_ARRAY); // array
        }

        @Override
        public void lcmp() {
            performComparison(LONG);
        }

        @Override
        public void lconst_0() {
            _frame.push(LONG);
        }

        @Override
        public void lconst_1() {
            _frame.push(LONG);
        }

        @Override
        public void ldc(int index) {
            final ConstantPool.Tag tag = constantPool().tagAt(index);
            switch (tag) {
                case INTEGER:
                    _frame.push(INTEGER);
                    break;
                case FLOAT:
                    _frame.push(FLOAT);
                    break;
                case STRING:
                    _frame.push(STRING);
                    break;
                case CLASS:
                    _frame.push(CLASS);
                    break;
                default:
                    throw verifyError("LDC instruction for invalid constant pool type " + tag);
            }
        }

        @Override
        public void ldc2_w(int index) {
            final ConstantPool.Tag tag = constantPool().tagAt(index);
            if (tag.equals(ConstantPool.Tag.LONG)) {
                _frame.push(LONG);
            } else if (tag.equals(ConstantPool.Tag.DOUBLE)) {
                _frame.push(DOUBLE);
            } else {
                throw verifyError("LDC2_W instruction for invalid constant pool type " + tag);
            }
        }

        @Override
        public void ldc_w(int index) {
            ldc(index);
        }

        @Override
        public void ldiv() {
            performArithmetic(LONG);
        }

        @Override
        public void lload(int index) {
            performLoad(LONG, index);
        }

        @Override
        public void lload_0() {
            lload(0);
        }

        @Override
        public void lload_1() {
            lload(1);
        }

        @Override
        public void lload_2() {
            lload(2);
        }

        @Override
        public void lload_3() {
            lload(3);
        }

        @Override
        public void lmul() {
            performArithmetic(LONG);
        }

        @Override
        public void lneg() {
            verifyIsAssignable(LONG, _frame.top(), "Invalid negation");
        }

        @Override
        public void lookupswitch(int defaultOffset, int numberOfCases) {
            _frame.pop(INTEGER);
            performBranch(currentOpcodePosition() + defaultOffset);
            final BytecodeScanner scanner = bytecodeScanner();
            int lastMatch = 0;
            for (int i = 0; i < numberOfCases; i++) {
                final int match = scanner.readSwitchCase();
                final int offset = scanner.readSwitchOffset();
                if (i > 0 && match < lastMatch) {
                    throw verifyError("Unordered lookupswitch (case " + i + " < case " + (i - 1) + ")");
                }
                performBranch(currentOpcodePosition() + offset);
                lastMatch = match;
            }
            _fallsThrough = false;
        }

        @Override
        public void lor() {
            performArithmetic(LONG);
        }

        @Override
        public void lrem() {
            performArithmetic(LONG);
        }

        @Override
        public void lreturn() {
            performReturn(LONG);
        }

        @Override
        public void lshl() {
            _frame.pop(INTEGER);
            _frame.pop(LONG);
            _frame.push(LONG);
        }

        @Override
        public void lshr() {
            _frame.pop(INTEGER);
            _frame.pop(LONG);
            _frame.push(LONG);
        }

        @Override
        public void lstore(int index) {
            performStore(LONG, index);
        }

        @Override
        public void lstore_0() {
            lstore(0);
        }

        @Override
        public void lstore_1() {
            lstore(1);
        }

        @Override
        public void lstore_2() {
            lstore(2);
        }

        @Override
        public void lstore_3() {
            lstore(3);
        }

        @Override
        public void lsub() {
            performArithmetic(LONG);
        }

        @Override
        public void lushr() {
            _frame.pop(INTEGER);
            _frame.pop(LONG);
            _frame.push(LONG);
        }

        @Override
        public void lxor() {
            performArithmetic(LONG);
        }

        @Override
        public void monitorenter() {
            _frame.pop(REFERENCE);
        }

        @Override
        public void monitorexit() {
            _frame.pop(REFERENCE);
        }

        @Override
        public void multianewarray(int index, int dimensions) {
            if (dimensions < 1) {
                throw verifyError("Dimensions in MULTIANEWARRAY operand must be >= 1");
            } else if (dimensions > 255) {
                throw verifyError("Array with too many dimensions");
            }

            for (int i = 0; i < dimensions; i++) {
                _frame.pop(INTEGER);
            }

            final ClassConstant classConstant = constantPool().classAt(index);
            final TypeDescriptor type = classConstant.typeDescriptor();
            if (!JavaTypeDescriptor.isArray(type)) {
                throw verifyError("MULTIANEWARRAY cannot be applied to non-array type " + type);
            }
            if (JavaTypeDescriptor.getArrayDimensions(type) < dimensions) {
                throw verifyError("MULTIANEWARRAY cannot create more dimensions than in the array type " + type);
            }
            _frame.push(getObjectType(type));
        }

        @Override
        public void new_(int index) {
            final UninitializedNewType value = classVerifier().getUninitializedNewType(currentOpcodePosition());
            if (_frame.isTypeOnStack(value)) {
                throw verifyError("Uninitialized type already exists on the stack: " + value);
            }

            if (JavaTypeDescriptor.isArray(constantPool().classAt(index, "array type descriptor").typeDescriptor())) {
                throw verifyError("Invalid use of NEW instruction to create an array");
            }

            _frame.push(value);
            _frame.replaceLocals(value, TOP);
        }

        @Override
        public void newarray(int tag) {
            _frame.pop(INTEGER);
            final VerificationType arrayType = getVerificationType(Kind.fromNewArrayTag(tag).arrayClassActor().typeDescriptor());
            _frame.push(arrayType);
        }

        @Override
        public void nop() {
        }

        @Override
        public void pop() {
            _frame.pop(CATEGORY1);
        }

        @Override
        public void pop2() {
            if (!_frame.top().isCategory2()) {
                _frame.pop(CATEGORY1);
                _frame.pop(CATEGORY1);
            } else {
                _frame.pop(CATEGORY2);
            }
        }

        @Override
        public void putfield(int index) {
            final FieldRefConstant fieldConstant = constantPool().fieldAt(index);

            _frame.pop(getVerificationType(fieldConstant.type(constantPool())));
            final VerificationType expectedObjectType = getObjectType(fieldConstant.holder(constantPool()));
            if (_thisObjectType.equals(expectedObjectType) && UNINITIALIZED_THIS.isAssignableFrom(_frame.top())) {
                _frame.pop(UNINITIALIZED_THIS);
            } else {
                final VerificationType object = _frame.pop(expectedObjectType);
                protectedFieldAccessCheck(object, fieldConstant, index);
            }
        }

        @Override
        public void putstatic(int index) {
            final FieldRefConstant fieldConstant = constantPool().fieldAt(index);
            final VerificationType value = getVerificationType(fieldConstant.type(constantPool()));
            _frame.pop(value);
        }

        @Override
        public void ret(int index) {
            performRet(index);
        }

        @Override
        public void saload() {
            _frame.pop(INTEGER); // index
            _frame.pop(SHORT_ARRAY); // array
            _frame.push(SHORT); // value
        }

        @Override
        public void sastore() {
            _frame.pop(SHORT); // value
            _frame.pop(INTEGER); // index
            _frame.pop(SHORT_ARRAY); // array
        }

        @Override
        public void sipush(int operand) {
            _frame.push(INTEGER);
        }

        @Override
        public void swap() {
            final VerificationType value1 = _frame.pop(CATEGORY1);
            final VerificationType value2 = _frame.pop(CATEGORY1);
            _frame.push(value1);
            _frame.push(value2);
        }

        @Override
        public void tableswitch(int defaultOffset, int lowMatch, int highMatch, int numberOfCases) {
            _frame.pop(INTEGER);
            if (lowMatch > highMatch) {
                throw verifyError("Low match greater than high match in TABLESWITCH: " + lowMatch + " > " + highMatch);
            }
            performBranch(currentOpcodePosition() + defaultOffset);
            final BytecodeScanner scanner = bytecodeScanner();
            for (int i = 0; i < numberOfCases; i++) {
                performBranch(currentOpcodePosition() + scanner.readSwitchOffset());
            }

            _fallsThrough = false;
        }

        @Override
        public void vreturn() {
            performReturn(TOP);

            if (classMethodActor().isInstanceInitializer() && _thisObjectType != OBJECT && !_constructorInvoked) {
                throw verifyError("Constructor must call super() or this()");
            }
        }

        @Override
        public void wide() {
        }
    }
}
