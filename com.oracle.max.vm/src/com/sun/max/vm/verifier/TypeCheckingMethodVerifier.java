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
package com.sun.max.vm.verifier;

import static com.sun.cri.bytecode.Bytecodes.*;
import static com.sun.cri.bytecode.Bytecodes.JniOp.*;
import static com.sun.cri.bytecode.Bytecodes.UnsignedComparisons.*;
import static com.sun.max.vm.verifier.types.VerificationType.*;

import com.sun.cri.bytecode.*;
import com.sun.max.lang.*;
import com.sun.max.vm.*;
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
 */
public class TypeCheckingMethodVerifier extends MethodVerifier {

    public TypeCheckingMethodVerifier(ClassVerifier classVerifier, ClassMethodActor classMethodActor, CodeAttribute codeAttribute) {
        super(classVerifier, classMethodActor, codeAttribute);
        final int codeLength = codeAttribute.code().length;
        this.interpreter = new Interpreter();
        this.thisObjectType = classVerifier.getObjectType(classActor().typeDescriptor);
        this.frame = createInitialFrame(classMethodActor);
        this.frameMap = initializeFrameMap(codeAttribute, frame.copy(), classVerifier);
        this.opcodeMap = new boolean[codeLength];
        this.exceptionHandlerMap = ExceptionHandler.createHandlerMap(codeLength, codeAttribute.exceptionHandlerTable());
    }

    protected Frame createInitialFrame(MethodActor classMethodActor) {
        return new Frame(classMethodActor, this);
    }

    protected final Interpreter interpreter;

    protected final VerificationType thisObjectType;

    protected boolean fallsThrough;

    /**
     * The current frame state derived during the abstract interpretation.
     */
    protected final Frame frame;

    /**
     * A map from each BCI to the {@linkplain Frame frame state} recorded (in a
     * {@link StackMapTable}) at that BCI. A null entry means that there is no recorded stack map frame at
     * that BCI.
     */
    protected final Frame[] frameMap;

    protected final ExceptionHandler[] exceptionHandlerMap;

    /**
     * A bit map indicating the BCIs at which an instruction starts.
     */
    private final boolean[] opcodeMap;

    @Override
    public void verify() {
        if (verbose || Verifier.TraceVerifierLevel >= Verifier.TRACE_METHOD) {
            Log.println(classMethodActor().format("[Verifying %H.%n(%p) via type-checking]"));
        }
        if (verbose) {
            Log.println("Input bytecode:");
            CodeAttributePrinter.print(Log.out, codeAttribute());
            Log.println();

            String prefix = "StackMapTable frames:";
            for (int i = 0; i != frameMap.length; ++i) {
                final Frame recordedFrame = frameMap[i];
                if (recordedFrame != null) {
                    if (prefix != null) {
                        Log.println(prefix);
                        prefix = null;
                    }
                    Log.println(i + ": " + recordedFrame);
                }
            }

            Log.println();
            Log.println("Interpreting bytecode:");
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
            int previousFrameBCI = -1;
            Frame previousFrame = initialFrame;
            for (int frameIndex = 0; frameIndex != stackMapFrames.length; ++frameIndex) {
                final Frame frame = previousFrame.copy();
                final StackMapFrame stackMapFrame = stackMapFrames[frameIndex];
                stackMapFrame.applyTo(frame);
                final int bci = stackMapFrame.getBCI(previousFrameBCI);
                try {
                    frameMap[bci] = frame;
                } catch (ArrayIndexOutOfBoundsException arrayIndexOutOfBoundsException) {
                    verifyError("Invalid BCI (" + bci + ") in frame " + frameIndex + " of StackMapTable attribute");
                }
                previousFrame = frame;
                previousFrameBCI = bci;
            }
        }
        return frameMap;
    }

    private void verifyBytecodes() {
        final BytecodeScanner bytecodeScanner = new BytecodeScanner(interpreter);
        bytecodeScanner.scan(new BytecodeBlock(codeAttribute().code()));
        if (fallsThrough) {
            verifyError("Execution falls off end of method");
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
            final VerificationType catchType = classVerifier().getObjectType(constantPool().classAt(catchTypeIndex).typeDescriptor());
            verifyIsAssignable(catchType, THROWABLE, "Invalid catch type in exception handler");
        }
        verifyIsValidInstructionBCI(info.handlerBCI(), "handler_pc in exception handler");
        verifyIsValidInstructionBCI(info.startBCI(), "start_pc in exception handler");
        if (info.endBCI() != codeAttribute().code().length) {
            verifyIsValidInstructionBCI(info.endBCI(), "end_pc in exception handler");
        }
        if (info.startBCI() >= info.endBCI()) {
            verifyError("Exception handler has a start_pc (" + info.startBCI() + ") not less than end_pc (" + info.endBCI() + ")");
        }
    }

    private void verifyStackMapTable() {
        for (int bci = 0; bci != frameMap.length; ++bci) {
            final Frame recordedFrame = frameMap[bci];
            if (recordedFrame != null) {
                if (!opcodeMap[bci]) {
                    verifyError("Offset (" +  bci + ") in a frame of the StackMapTable attribute does not point to an instruction");
                }
            }
        }
    }

    protected void verifyIsValidInstructionBCI(int bci, String bciDescription) {
        try {
            if (opcodeMap[bci]) {
                return;
            }
        } catch (ArrayIndexOutOfBoundsException arrayIndexOutOfBoundsException) {
        }
        verifyError("Invalid BCI " + bci + "(" + bciDescription + ")");
    }

    /**
     * Gets the frame at a given BCI.
     *
     * @param targetDescription if there is not a recorded frame at {@code bci}, then a verification error will be
     *            raised and its detail message will incorporate this description of the location for which a frame
     *            should have existed
     */
    protected Frame frameAt(int bci, String targetDescription) {
        try {
            final Frame frame = frameMap[bci];
            if (frame != null) {
                return frame;
            }
        } catch (ArrayIndexOutOfBoundsException arrayIndexOutOfBoundsException) {
        }
        throw fatalVerifyError("Missing stackmap frame for BCI " + bci + " (" + targetDescription + ")");
    }

    @Override
    public int currentOpcodeBCI() {
        return interpreter.currentOpcodeBCI();
    }

    /**
     * Called after an opcode has been decoded but before the relevant opcode visitor method is called.
     */
    protected void preInstructionScan() {
        final int currentOpcodeBCI = currentOpcodeBCI();

        if (!fallsThrough) {
            final Frame targetFrame = frameAt(currentOpcodeBCI, "instruction can only be reached by a branch");
            if (targetFrame != frame) {
                frame.reset(targetFrame);
            }
        } else {
            if (currentOpcodeBCI < frameMap.length) {
                final Frame recordedFrame = frameMap[currentOpcodeBCI];
                if (recordedFrame != null) {
                    recordedFrame.mergeFrom(frame, currentOpcodeBCI, -1);
                    frame.reset(recordedFrame);
                }
            }
        }

        if (verbose) {
            Log.println(Strings.indent(frame.toString(), "    "));
            Log.println(currentOpcodeBCI + ": " + Bytecodes.nameOf(interpreter.currentOpcode()));
            Log.println();
        }

        opcodeMap[currentOpcodeBCI] = true;

        for (ExceptionHandler handler = exceptionHandlerMap[currentOpcodeBCI]; handler != null; handler = handler.next()) {
            final int handlerBCI = handler.bci();
            final Frame handlerEntryFrame = frameAt(handlerBCI, "exception handler entry point");
            handlerEntryFrame.mergeFrom(frame, handlerBCI, handler.catchTypeIndex());
        }

        fallsThrough = true;
    }

    public void push(TypeDescriptor typeDescriptor) {
        frame.push(getVerificationType(typeDescriptor));
    }

    /**
     * Interprets a type conversion instruction.
     *
     * @param fromType the expected type of the value on top of the stack before the conversion
     * @param toType the type of the value on top of the stack after the conversion
     */
    void performConversion(VerificationType fromType, VerificationType toType) {
        frame.pop(fromType);
        frame.push(toType);
    }

    /**
     * Interprets a binary arithmetic instruction.
     *
     * @param type the expected type of the two input values on top of the stack before the operation as well as the
     *            type of the result on top of the stack after the operation
     */
    void performArithmetic(VerificationType type) {
        frame.pop(type);
        frame.pop(type);
        frame.push(type);
    }

    /**
     * Interprets a binary arithmetic instruction.
     */
    void performArithmetic(VerificationType left, VerificationType right, VerificationType result) {
        frame.pop(right);
        frame.pop(left);
        frame.push(result);
    }

    /**
     * Interprets a binary comparison instruction.
     *
     * @param type the expected type of the two input values on top of the stack to be compared before the operation
     */
    void performComparison(VerificationType type) {
        frame.pop(type);
        frame.pop(type);
        frame.push(INTEGER);
    }

    /**
     * Interprets returning from the method.
     */
    void performReturn(VerificationType returnType) {
        final VerificationType declaredReturnType = getVerificationType(classMethodActor().descriptor().resultDescriptor());
        if (declaredReturnType == TOP) {
            if (returnType != TOP) {
                verifyError("Invalid return for void method");
            }
        } else {
            final VerificationType returnValue = frame.pop(returnType);
            verifyIsAssignable(returnValue, declaredReturnType, "Invalid return type");
        }

        fallsThrough = false;
    }

    /**
     * Verifies that the current type state is {@linkplain Frame#mergeInto(Frame, int) compatible} with the type state
     * at the destination of a control flow instruction.
     *
     * @param bci the destination of a control flow instruction
     */
    protected void performBranch(int bci) {
        final Frame targetFrame = frameAt(bci, "branch target");
        targetFrame.mergeFrom(frame, bci, -1);
    }

    protected void performIfCompareBranch(int offset, VerificationType type) {
        frame.pop(type);
        frame.pop(type);
        performBranch(currentOpcodeBCI() + offset);
    }

    void performIfBranch(int offset, VerificationType type) {
        frame.pop(type);
        performBranch(currentOpcodeBCI() + offset);
    }

    protected void performStore(VerificationType type, int index) {
        frame.store(frame.pop(type), index);
    }

    public void performLoad(VerificationType type, int index) {
        frame.push(frame.load(type, index));
    }

    protected void performJsr(int offset) {
        verifyError("JSR instruction is not supported");
    }

    protected void performRet(int index) {
        verifyError("RET instruction is not supported");
    }

    /**
     * The abstract interpreter that simulates the JVM instructions at the level of types (as opposed to values).
     */
    final class Interpreter extends BytecodeVisitor {

        private boolean constructorInvoked;

        @Override
        public void opcodeDecoded() {
            preInstructionScan();
        }

        @Override
        public void aaload() {
            frame.pop(INTEGER);
            final VerificationType array = frame.pop(OBJECT_ARRAY);
            if (array != NULL) {
                final VerificationType element = array.componentType();
                frame.push(element);
            } else {
                frame.push(NULL);
            }
        }

        @Override
        public void aastore() {
            frame.pop(OBJECT);
            frame.pop(INTEGER); // index
            frame.pop(OBJECT_ARRAY);
            // The remaining type check is done at runtime, throwing ArrayStoreException if it fails
        }

        @Override
        public void aconst_null() {
            frame.push(NULL);
        }

        @Override
        public void aload(int index) {
            performLoad(REFERENCE_OR_WORD, index);
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
            frame.pop(INTEGER);
            final TypeDescriptor elementDescriptor = constantPool().classAt(index).typeDescriptor();
            try {
                final ReferenceOrWordType element = (ReferenceOrWordType) getVerificationType(elementDescriptor);
                frame.push(getObjectType(JavaTypeDescriptor.getArrayDescriptorForDescriptor(element.typeDescriptor(), 1)));
            } catch (ClassCastException classCastException) {
                verifyError("Invalid use of primitive type in ANEWARRAY: " + elementDescriptor);
            }
        }

        @Override
        public void areturn() {
            performReturn(REFERENCE_OR_WORD);
        }

        @Override
        public void arraylength() {
            final VerificationType array = frame.pop(REFERENCE);
            if (array != NULL && !array.isArray()) {
                verifyError("Require array type in ARRAYLENGTH");
            }
            frame.push(INTEGER);
        }

        @Override
        public void astore(int index) {
            performStore(REFERENCE_OR_WORD, index);
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
            frame.pop(THROWABLE);
            fallsThrough = false;
        }

        @Override
        public void baload() {
            frame.pop(INTEGER);
            final VerificationType array = frame.pop(REFERENCE);
            if (array != BYTE_ARRAY && array != BOOLEAN_ARRAY && array != NULL) {
                verifyError("Invalid array type for BALOAD");
            }
            frame.push(BYTE);
        }

        @Override
        public void bastore() {
            frame.pop(INTEGER);
            frame.pop(INTEGER);
            final VerificationType array = frame.pop(REFERENCE);
            if (array != BYTE_ARRAY && array != BOOLEAN_ARRAY && array != NULL) {
                verifyError("Invalid array type for BASTORE");
            }
        }

        @Override
        public void bipush(int operand) {
            frame.push(INTEGER);
        }

        @Override
        public void breakpoint() {
        }

        @Override
        public void caload() {
            frame.pop(INTEGER); // index
            frame.pop(CHAR_ARRAY); // array
            frame.push(CHAR); // value
        }

        @Override
        public void castore() {
            frame.pop(CHAR); // value
            frame.pop(INTEGER); // index
            frame.pop(CHAR_ARRAY); // array
        }

        @Override
        public void checkcast(int index) {
            frame.pop(REFERENCE_OR_WORD);

            final ClassConstant classConstant = constantPool().classAt(index);
            final TypeDescriptor toType = classConstant.typeDescriptor();
            if (JavaTypeDescriptor.isPrimitive(toType)) {
                verifyError("Invalid use of primitive type in CHECKCAST: " + toType);
            }
            frame.push(getObjectType(toType));
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
            frame.pop(INTEGER); // index
            frame.pop(DOUBLE_ARRAY); // array
            frame.push(DOUBLE); // value
        }

        @Override
        public void dastore() {
            frame.pop(DOUBLE); // value
            frame.pop(INTEGER); // index
            frame.pop(DOUBLE_ARRAY); // array
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
            frame.push(DOUBLE);
        }

        @Override
        public void dconst_1() {
            frame.push(DOUBLE);
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
            verifyIsAssignable(DOUBLE, frame.top(), "Invalid double negation");
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
            final VerificationType value = frame.pop(CATEGORY1);
            frame.push(value);
            frame.push(value);
        }

        @Override
        public void dup2() {
            if (!frame.top().isCategory2()) {
                final VerificationType value1 = frame.pop(CATEGORY1);
                final VerificationType value2 = frame.pop(CATEGORY1);
                frame.push(value2);
                frame.push(value1);
                frame.push(value2);
                frame.push(value1);
            } else {
                final VerificationType value = frame.pop(CATEGORY2);
                frame.push(value);
                frame.push(value);
            }
        }

        @Override
        public void dup2_x1() {
            if (!frame.top().isCategory2()) {
                final VerificationType value1 = frame.pop(CATEGORY1);
                final VerificationType value2 = frame.pop(CATEGORY1);
                final VerificationType value3 = frame.pop(CATEGORY1);
                frame.push(value2);
                frame.push(value1);
                frame.push(value3);
                frame.push(value2);
                frame.push(value1);
            } else {
                final VerificationType value1 = frame.pop(CATEGORY2);
                final VerificationType value2 = frame.pop(TOP);
                frame.push(value1);
                frame.push(value2);
                frame.push(value1);
            }
        }

        @Override
        public void dup2_x2() {
            if (!frame.top().isCategory2()) {
                final VerificationType value1 = frame.pop(CATEGORY1);
                final VerificationType value2 = frame.pop(CATEGORY1);
                if (!frame.top().isCategory2()) {
                    final VerificationType value3 = frame.pop(CATEGORY1);
                    final VerificationType value4 = frame.pop(CATEGORY1);
                    frame.push(value2);
                    frame.push(value1);
                    frame.push(value4);
                    frame.push(value3);
                } else {
                    final VerificationType value3 = frame.pop(CATEGORY2);
                    frame.push(value2);
                    frame.push(value1);
                    frame.push(value3);
                }
                frame.push(value2);
                frame.push(value1);
            } else {
                final VerificationType value1 = frame.pop(CATEGORY2);
                if (!frame.top().isCategory2()) {
                    final VerificationType value2 = frame.pop(CATEGORY1);
                    final VerificationType value3 = frame.pop(CATEGORY1);
                    frame.push(value1);
                    frame.push(value3);
                    frame.push(value2);
                    frame.push(value1);
                } else {
                    final VerificationType value2 = frame.pop(CATEGORY2);
                    frame.push(value1);
                    frame.push(value2);
                    frame.push(value1);
                }
            }
        }

        @Override
        public void dup_x1() {
            final VerificationType value1 = frame.pop(CATEGORY1);
            final VerificationType value2 = frame.pop(CATEGORY1);
            frame.push(value1);
            frame.push(value2);
            frame.push(value1);
        }

        @Override
        public void dup_x2() {
            final VerificationType value1 = frame.pop(CATEGORY1);
            if (!frame.top().isCategory2()) {
                final VerificationType value2 = frame.pop(CATEGORY1);
                final VerificationType value3 = frame.pop(CATEGORY1);
                frame.push(value1);
                frame.push(value3);
                frame.push(value2);
                frame.push(value1);
            } else {
                final VerificationType value2 = frame.pop(CATEGORY2);
                frame.push(value1);
                frame.push(value2);
                frame.push(value1);
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
            frame.pop(INTEGER); // index
            frame.pop(FLOAT_ARRAY); // array
            frame.push(FLOAT); // value
        }

        @Override
        public void fastore() {
            frame.pop(FLOAT); // value
            frame.pop(INTEGER); // index
            frame.pop(FLOAT_ARRAY); // array
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
            frame.push(FLOAT);
        }

        @Override
        public void fconst_1() {
            frame.push(FLOAT);
        }

        @Override
        public void fconst_2() {
            frame.push(FLOAT);
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
            verifyIsAssignable(FLOAT, frame.top(), "Invalid negation");
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
            final VerificationType object = frame.pop(getObjectType(fieldConstant.holder(constantPool())));
            protectedFieldAccessCheck(object, fieldConstant, index);
            frame.push(value);
        }

        @Override
        public void getstatic(int index) {
            final FieldRefConstant fieldConstant = constantPool().fieldAt(index);
            final VerificationType value = getVerificationType(fieldConstant.type(constantPool()));
            frame.push(value);
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
            if (object == thisObjectType) {
                return;
            }

            final TypeDescriptor fieldHolder = fieldRef.holder(constantPool());

            ClassActor superClassActor = classActor().superClassActor;
            while (superClassActor != null) {
                if (superClassActor.typeDescriptor.equals(fieldHolder)) {
                    // Accessing a field from a super class of the current class.
                    final FieldActor fieldActor = fieldRef.resolve(constantPool(), index);
                    if (!fieldActor.isProtected()) {
                        break;
                    } else if (!classActor().packageName().equals(fieldActor.holder().packageName())) {
                        verifyIsAssignable(object, thisObjectType, "Invalid access of protected field");
                    }

                    // Accessing this field is okay
                    return;
                }
                superClassActor = superClassActor.superClassActor;
            }

            // The field being accessed belongs to a class that isn't a superclass of the current class.
            // The access control check will be performed at run time as part of field resolution.
        }

        @Override
        public void goto_(int offset) {
            performBranch(currentOpcodeBCI() + offset);
            fallsThrough = false;
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
            frame.pop(INTEGER); // index
            frame.pop(INTEGER_ARRAY); // array
            frame.push(INTEGER); // value
        }

        @Override
        public void iand() {
            performArithmetic(INTEGER);
        }

        @Override
        public void iastore() {
            frame.pop(INTEGER); // value
            frame.pop(INTEGER); // index
            frame.pop(INTEGER_ARRAY); // array
        }

        @Override
        public void iconst_0() {
            frame.push(INTEGER);
        }

        @Override
        public void iconst_1() {
            frame.push(INTEGER);
        }

        @Override
        public void iconst_2() {
            frame.push(INTEGER);
        }

        @Override
        public void iconst_3() {
            frame.push(INTEGER);
        }

        @Override
        public void iconst_4() {
            frame.push(INTEGER);
        }

        @Override
        public void iconst_5() {
            frame.push(INTEGER);
        }

        @Override
        public void iconst_m1() {
            frame.push(INTEGER);
        }

        @Override
        public void idiv() {
            performArithmetic(INTEGER);
        }

        @Override
        public void if_acmpeq(int offset) {
            performIfCompareBranch(offset, REFERENCE_OR_WORD);
        }

        @Override
        public void if_acmpne(int offset) {
            performIfCompareBranch(offset, REFERENCE_OR_WORD);
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
            frame.load(INTEGER, index);
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
            verifyIsAssignable(INTEGER, frame.top(), "Invalid negation");
        }

        @Override
        public void instanceof_(int index) {
            constantPool().classAt(index);
            frame.pop(REFERENCE_OR_WORD);
            frame.push(INTEGER);
        }

        private int popMethodParameters(SignatureDescriptor methodSignature) {
            final int numberOfParameters = methodSignature.numberOfParameters();
            int count = 0;
            for (int n = numberOfParameters - 1; n >= 0; n--) {
                final VerificationType parameter = frame.pop(getVerificationType(methodSignature.parameterDescriptorAt(n)));
                count += parameter.size();
            }
            return count;
        }

        private void pushMethodResult(SignatureDescriptor methodSignature) {
            final VerificationType returnType = getVerificationType(methodSignature.resultDescriptor());
            if (returnType != TOP) {
                frame.push(returnType);
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
            if (receiver == thisObjectType) {
                return;
            }

            final TypeDescriptor methodHolder = methodRef.holder(constantPool());

            ClassActor superClassActor = classActor().superClassActor;
            while (superClassActor != null) {
                if (superClassActor.typeDescriptor.equals(methodHolder)) {
                    // Accessing a method from a super class of the current class.
                    final MethodActor methodActor = methodRef.resolve(constantPool(), index);
                    if (!methodActor.isProtected()) {
                        break;
                    } else if (!classActor().packageName().equals(methodActor.holder().packageName())) {
                        if (receiver.isArray() && methodActor.holder() == ClassRegistry.OBJECT && methodActor.name.toString().equals("clone")) {
                            // Special case: arrays pretend to implement public Object clone().
                            break;
                        }
                        verifyIsAssignable(receiver, thisObjectType, "Invalid access of protected method");
                    }

                    // Accessing this method is okay
                    return;
                }
                superClassActor = superClassActor.superClassActor;
            }

            // The method being accessed belongs to a class that isn't a superclass of the current class.
            // The access control check will be performed at run time as part of method resolution.
        }

        @Override
        public void invokeinterface(int index, int count) {
            final InterfaceMethodRefConstant methodConstant = constantPool().interfaceMethodAt(index);
            final Utf8Constant methodName = methodConstant.name(constantPool());
            if (methodName.toString().startsWith("<")) {
                verifyError("Invalid INVOKEINTERFACE on initialization method");
            }

            final SignatureDescriptor methodSignature = methodConstant.signature(constantPool());
            final int actualCount = popMethodParameters(methodSignature) + 1;
            frame.pop(OBJECT);

            if (actualCount != count) {
                verifyError("INVOKEINTERFACE count operand does not match method signature");
            }

            final TypeDescriptor holder = methodConstant.holder(constantPool());
            if (holder.equals(JavaTypeDescriptor.OBJECT)) {
                // This is a case of invokeinterface being used to invoke a virtual method
                // declared in java.lang.Object. While this is perfectly legal, it complicates
                // the compilation or interpretation of invokeinterface. What's more, no sane
                // Java source compiler will produce such code. As such, the instruction is
                // re-written to use invokevirtual instead.
                final byte[] code = bytecodeScanner().bytecodeBlock().code();
                code[bytecodeScanner().currentOpcodeBCI()] = (byte) Bytecodes.INVOKEVIRTUAL;
            }

            pushMethodResult(methodSignature);
        }

        @Override
        public void jnicall(int nativeFunctionDescriptorIndex) {
            final SignatureDescriptor nativeFunctionDescriptor = SignatureDescriptor.create(constantPool().utf8At(nativeFunctionDescriptorIndex, "native function descriptor"));
            frame.pop(VerificationType.WORD); // the native function address
            popMethodParameters(nativeFunctionDescriptor);
            pushMethodResult(nativeFunctionDescriptor);
        }

        @Override
        public void invokespecial(int index) {
            final MethodRefConstant methodConstant = constantPool().methodAt(index);
            final Utf8Constant name = methodConstant.name(constantPool());
            if (name.equals(SymbolTable.CLINIT)) {
                verifyError("Cannot invoke <clinit> method");
            }

            final SignatureDescriptor methodSignature = methodConstant.signature(constantPool());
            popMethodParameters(methodSignature);

            if (name.equals(SymbolTable.INIT)) {
                if (methodSignature.resultDescriptor() != JavaTypeDescriptor.VOID) {
                    verifyError("<init> must return void");
                }

                final UninitializedType uninitializedObject = (UninitializedType) frame.pop(UNINITIALIZED);
                final VerificationType initializedObject;

                if (uninitializedObject instanceof UninitializedNewType) {
                    final UninitializedNewType object = (UninitializedNewType) uninitializedObject;
                    initializedObject = getObjectType(getTypeDescriptorFromNewBytecode(object.bci()));
                } else {
                    assert uninitializedObject instanceof UninitializedThisType;
                    initializedObject = getObjectType(classActor().typeDescriptor);
                    constructorInvoked = true;
                }

                frame.replaceStack(uninitializedObject, initializedObject);
                frame.replaceLocals(uninitializedObject, initializedObject);

                protectedMethodAccessCheck(initializedObject, methodConstant, index);
            } else {
                final VerificationType object = frame.pop(getObjectType(methodConstant.holder(constantPool())));
                if (object == WORD) {
                    // Virtual dispatch on Word type may have been converted to direct dispatch
                    // as virtual dispatch is not possible on Word types.
                } else {
                    verifyIsAssignable(object, thisObjectType, "Invalid use of INVOKESPECIAL");
                }
                pushMethodResult(methodSignature);
            }
        }

        /**
         * Gets the type of object constructed by a {@link Bytecodes#NEW} instruction at a given BCI.
         */
        private TypeDescriptor getTypeDescriptorFromNewBytecode(int bci) {
            final byte[] bytecodes = bytecodeScanner().bytecodeBlock().code();
            try {
                final int constantPoolIndex = ((bytecodes[bci + 1] & 0xFF) << 8) | (bytecodes[bci + 2] & 0xFF);
                return constantPool().classAt(constantPoolIndex).typeDescriptor();
            } catch (ArrayIndexOutOfBoundsException arrayIndexOutOfBoundsException) {
                throw fatalVerifyError("Invalid NEW instruction at BCI " + bci);
            }
        }

        @Override
        public void invokestatic(int index) {
            final MethodRefConstant methodConstant = constantPool().methodAt(index);
            if (methodConstant.name(constantPool()).toString().startsWith("<")) {
                verifyError("Invalid INVOKESTATIC on initialization method");
            }
            final SignatureDescriptor methodSignature = methodConstant.signature(constantPool());
            popMethodParameters(methodSignature);
            pushMethodResult(methodSignature);
        }

        @Override
        public void invokevirtual(int index) {
            final MethodRefConstant methodConstant = constantPool().methodAt(index);
            if (methodConstant.name(constantPool()).toString().startsWith("<")) {
                verifyError("Invalid INVOKEVIRTUAL on initialization method");
            }

            final SignatureDescriptor methodSignature = methodConstant.signature(constantPool());
            popMethodParameters(methodSignature);
            final VerificationType receiverObject = frame.pop(getObjectType(methodConstant.holder(constantPool())));
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
            frame.pop(INTEGER); // index
            frame.pop(LONG_ARRAY); // array
            frame.push(LONG); // value
        }

        @Override
        public void land() {
            performArithmetic(LONG);
        }

        @Override
        public void lastore() {
            frame.pop(LONG); // value
            frame.pop(INTEGER); // index
            frame.pop(LONG_ARRAY); // array
        }

        @Override
        public void lcmp() {
            performComparison(LONG);
        }

        @Override
        public void lconst_0() {
            frame.push(LONG);
        }

        @Override
        public void lconst_1() {
            frame.push(LONG);
        }

        @Override
        public void ldc(int index) {
            final ConstantPool.Tag tag = constantPool().tagAt(index);
            switch (tag) {
                case INTEGER:
                    frame.push(INTEGER);
                    break;
                case FLOAT:
                    frame.push(FLOAT);
                    break;
                case STRING:
                    frame.push(STRING);
                    break;
                case CLASS:
                    frame.push(CLASS);
                    break;
                default:
                    verifyError("LDC instruction for invalid constant pool type " + tag);
            }
        }

        @Override
        public void ldc2_w(int index) {
            final ConstantPool.Tag tag = constantPool().tagAt(index);
            if (tag.equals(ConstantPool.Tag.LONG)) {
                frame.push(LONG);
            } else if (tag.equals(ConstantPool.Tag.DOUBLE)) {
                frame.push(DOUBLE);
            } else {
                verifyError("LDC2_W instruction for invalid constant pool type " + tag);
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
            verifyIsAssignable(LONG, frame.top(), "Invalid negation");
        }

        @Override
        public void lookupswitch(int defaultOffset, int numberOfCases) {
            frame.pop(INTEGER);
            performBranch(currentOpcodeBCI() + defaultOffset);
            final BytecodeScanner scanner = bytecodeScanner();
            int lastMatch = 0;
            for (int i = 0; i < numberOfCases; i++) {
                final int match = scanner.readSwitchCase();
                final int offset = scanner.readSwitchOffset();
                if (i > 0 && match < lastMatch) {
                    verifyError("Unordered lookupswitch (case " + i + " < case " + (i - 1) + ")");
                }
                performBranch(currentOpcodeBCI() + offset);
                lastMatch = match;
            }
            fallsThrough = false;
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
            frame.pop(INTEGER);
            frame.pop(LONG);
            frame.push(LONG);
        }

        @Override
        public void lshr() {
            frame.pop(INTEGER);
            frame.pop(LONG);
            frame.push(LONG);
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
            frame.pop(INTEGER);
            frame.pop(LONG);
            frame.push(LONG);
        }

        @Override
        public void lxor() {
            performArithmetic(LONG);
        }

        @Override
        public void monitorenter() {
            frame.pop(REFERENCE);
        }

        @Override
        public void monitorexit() {
            frame.pop(REFERENCE);
        }

        @Override
        public void multianewarray(int index, int dimensions) {
            if (dimensions < 1) {
                verifyError("Dimensions in MULTIANEWARRAY operand must be >= 1");
            } else if (dimensions > 255) {
                verifyError("Array with too many dimensions");
            }

            for (int i = 0; i < dimensions; i++) {
                frame.pop(INTEGER);
            }

            final ClassConstant classConstant = constantPool().classAt(index);
            final TypeDescriptor type = classConstant.typeDescriptor();
            if (!JavaTypeDescriptor.isArray(type)) {
                verifyError("MULTIANEWARRAY cannot be applied to non-array type " + type);
            }
            if (JavaTypeDescriptor.getArrayDimensions(type) < dimensions) {
                verifyError("MULTIANEWARRAY cannot create more dimensions than in the array type " + type);
            }
            frame.push(getObjectType(type));
        }

        @Override
        public void new_(int index) {
            final UninitializedNewType value = classVerifier().getUninitializedNewType(currentOpcodeBCI());
            if (frame.isTypeOnStack(value)) {
                verifyError("Uninitialized type already exists on the stack: " + value);
            }

            if (JavaTypeDescriptor.isArray(constantPool().classAt(index, "array type descriptor").typeDescriptor())) {
                verifyError("Invalid use of NEW instruction to create an array");
            }

            frame.push(value);
            frame.replaceLocals(value, TOP);
        }

        @Override
        public void newarray(int tag) {
            frame.pop(INTEGER);
            final VerificationType arrayType = getVerificationType(Kind.fromNewArrayTag(tag).arrayClassActor().typeDescriptor);
            frame.push(arrayType);
        }

        @Override
        public void nop() {
        }

        @Override
        public void pop() {
            frame.pop(CATEGORY1);
        }

        @Override
        public void pop2() {
            if (!frame.top().isCategory2()) {
                frame.pop(CATEGORY1);
                frame.pop(CATEGORY1);
            } else {
                frame.pop(CATEGORY2);
            }
        }

        @Override
        public void putfield(int index) {
            final FieldRefConstant fieldConstant = constantPool().fieldAt(index);

            frame.pop(getVerificationType(fieldConstant.type(constantPool())));
            final VerificationType expectedObjectType = getObjectType(fieldConstant.holder(constantPool()));
            if (thisObjectType.equals(expectedObjectType) && UNINITIALIZED_THIS.isAssignableFrom(frame.top())) {
                frame.pop(UNINITIALIZED_THIS);
            } else {
                final VerificationType object = frame.pop(expectedObjectType);
                protectedFieldAccessCheck(object, fieldConstant, index);
            }
        }

        @Override
        public void putstatic(int index) {
            final FieldRefConstant fieldConstant = constantPool().fieldAt(index);
            final VerificationType value = getVerificationType(fieldConstant.type(constantPool()));
            frame.pop(value);
        }

        @Override
        public void ret(int index) {
            performRet(index);
        }

        @Override
        public void saload() {
            frame.pop(INTEGER); // index
            frame.pop(SHORT_ARRAY); // array
            frame.push(SHORT); // value
        }

        @Override
        public void sastore() {
            frame.pop(SHORT); // value
            frame.pop(INTEGER); // index
            frame.pop(SHORT_ARRAY); // array
        }

        @Override
        public void sipush(int operand) {
            frame.push(INTEGER);
        }

        @Override
        public void swap() {
            final VerificationType value1 = frame.pop(CATEGORY1);
            final VerificationType value2 = frame.pop(CATEGORY1);
            frame.push(value1);
            frame.push(value2);
        }

        @Override
        public void tableswitch(int defaultOffset, int lowMatch, int highMatch, int numberOfCases) {
            frame.pop(INTEGER);
            if (lowMatch > highMatch) {
                verifyError("Low match greater than high match in TABLESWITCH: " + lowMatch + " > " + highMatch);
            }
            performBranch(currentOpcodeBCI() + defaultOffset);
            final BytecodeScanner scanner = bytecodeScanner();
            for (int i = 0; i < numberOfCases; i++) {
                performBranch(currentOpcodeBCI() + scanner.readSwitchOffset());
            }

            fallsThrough = false;
        }

        @Override
        public void vreturn() {
            performReturn(TOP);

            if (classMethodActor().isInstanceInitializer() && thisObjectType != OBJECT && !constructorInvoked) {
                verifyError("Constructor must call super() or this()");
            }
        }

        @Override
        public void wide() {
        }

        @Override
        protected boolean extension(int opcode, boolean isWide) {
            int length = Bytecodes.lengthOf(opcode);
            int operand = 0;
            if (length == 2) {
                operand = isWide ? bytecodeScanner().readUnsigned2() : bytecodeScanner().readUnsigned1();
            } else if (length == 3) {
                assert !isWide;
                operand = bytecodeScanner().readUnsigned2();
            } else {
                assert !isWide;
                assert length == 1;
            }
            switch (opcode) {
                // Checkstyle: stop
                case UNSAFE_CAST: {
                    if (!frame.top().isCategory2()) {
                        frame.pop(CATEGORY1);
                    } else {
                        frame.pop(CATEGORY2);
                    }

                    MethodRefConstant methodRef = constantPool().methodAt(operand);
                    SignatureDescriptor sig = methodRef.signature(constantPool());
                    frame.push(getVerificationType(sig.resultDescriptor()));
                    break;
                }
                case WLOAD: performLoad(WORD, operand); break;
                case WLOAD_0:                performLoad(WORD, 0); break;
                case WLOAD_1:                performLoad(WORD, 1); break;
                case WLOAD_2:                performLoad(WORD, 2); break;
                case WLOAD_3:                performLoad(WORD, 3); break;
                case WSTORE:                 performStore(WORD, operand); break;
                case WSTORE_0:               performStore(WORD, 0); break;
                case WSTORE_1:               performStore(WORD, 1); break;
                case WSTORE_2:               performStore(WORD, 2); break;
                case WSTORE_3:               performStore(WORD, 3); break;
                case WCONST_0:               frame.push(WORD); break;
                case WDIV:                   performArithmetic(WORD); break;
                case WDIVI:                  performArithmetic(WORD, INTEGER, WORD); break;
                case WREM:                   performArithmetic(WORD); break;
                case WREMI:                  performArithmetic(WORD, INTEGER, INTEGER); break;

                case PCMPSWP:
                case PGET:
                case PSET:
                case PREAD:
                case PWRITE: {
                    opcode = opcode | (operand << 8);
                    switch (opcode) {
                        case PREAD_BYTE:             pointerRead(BYTE, false); break;
                        case PREAD_CHAR:             pointerRead(CHAR, false); break;
                        case PREAD_SHORT:            pointerRead(SHORT, false); break;
                        case PREAD_INT:              pointerRead(INTEGER, false); break;
                        case PREAD_FLOAT:            pointerRead(FLOAT, false); break;
                        case PREAD_LONG:             pointerRead(LONG, false); break;
                        case PREAD_DOUBLE:           pointerRead(DOUBLE, false); break;
                        case PREAD_WORD:             pointerRead(WORD, false); break;
                        case PREAD_REFERENCE:        pointerRead(VM_REFERENCE, false); break;
                        case PREAD_BYTE_I:           pointerRead(BYTE, true); break;
                        case PREAD_CHAR_I:           pointerRead(CHAR, true); break;
                        case PREAD_SHORT_I:          pointerRead(SHORT, true); break;
                        case PREAD_INT_I:            pointerRead(INTEGER, true); break;
                        case PREAD_FLOAT_I:          pointerRead(FLOAT, true); break;
                        case PREAD_LONG_I:           pointerRead(LONG, true); break;
                        case PREAD_DOUBLE_I:         pointerRead(DOUBLE, true); break;
                        case PREAD_WORD_I:           pointerRead(WORD, true); break;
                        case PREAD_REFERENCE_I:      pointerRead(VM_REFERENCE, true); break;
                        case PWRITE_BYTE:            pointerWrite(BYTE, false); break;
                        case PWRITE_SHORT:           pointerWrite(SHORT, false); break;
                        case PWRITE_INT:             pointerWrite(INTEGER, false); break;
                        case PWRITE_FLOAT:           pointerWrite(FLOAT, false); break;
                        case PWRITE_LONG:            pointerWrite(LONG, false); break;
                        case PWRITE_DOUBLE:          pointerWrite(DOUBLE, false); break;
                        case PWRITE_WORD:            pointerWrite(WORD, false); break;
                        case PWRITE_REFERENCE:       pointerWrite(VM_REFERENCE, false); break;
                        case PWRITE_BYTE_I:          pointerWrite(BYTE, true); break;
                        case PWRITE_SHORT_I:         pointerWrite(SHORT, true); break;
                        case PWRITE_INT_I:           pointerWrite(INTEGER, true); break;
                        case PWRITE_FLOAT_I:         pointerWrite(FLOAT, true); break;
                        case PWRITE_LONG_I:          pointerWrite(LONG, true); break;
                        case PWRITE_DOUBLE_I:        pointerWrite(DOUBLE, true); break;
                        case PWRITE_WORD_I:          pointerWrite(WORD, true); break;
                        case PWRITE_REFERENCE_I:     pointerWrite(VM_REFERENCE, true); break;
                        case PGET_BYTE:              pointerGet(BYTE); break;
                        case PGET_CHAR:              pointerGet(CHAR); break;
                        case PGET_SHORT:             pointerGet(SHORT); break;
                        case PGET_INT:               pointerGet(INTEGER); break;
                        case PGET_FLOAT:             pointerGet(FLOAT); break;
                        case PGET_LONG:              pointerGet(LONG); break;
                        case PGET_DOUBLE:            pointerGet(DOUBLE); break;
                        case PGET_WORD:              pointerGet(WORD); break;
                        case PGET_REFERENCE:         pointerGet(VM_REFERENCE); break;
                        case PSET_BYTE:              pointerSet(BYTE); break;
                        case PSET_SHORT:             pointerSet(SHORT); break;
                        case PSET_INT:               pointerSet(INTEGER); break;
                        case PSET_FLOAT:             pointerSet(FLOAT); break;
                        case PSET_LONG:              pointerSet(LONG); break;
                        case PSET_DOUBLE:            pointerSet(DOUBLE); break;
                        case PSET_WORD:              pointerSet(WORD); break;
                        case PSET_REFERENCE:         pointerSet(VM_REFERENCE); break;
                        case PCMPSWP_INT:            pointerCompareAndSwap(INTEGER, false); break;
                        case PCMPSWP_WORD:           pointerCompareAndSwap(WORD, false); break;
                        case PCMPSWP_REFERENCE:      pointerCompareAndSwap(VM_REFERENCE, false); break;
                        case PCMPSWP_INT_I:          pointerCompareAndSwap(INTEGER, true); break;
                        case PCMPSWP_WORD_I:         pointerCompareAndSwap(WORD, true); break;
                        case PCMPSWP_REFERENCE_I:    pointerCompareAndSwap(VM_REFERENCE, true); break;
                        default:                     verifyError("Unsupported bytecode: " + Bytecodes.nameOf(opcode));
                    }
                    break;
                }

                case MOV_I2F:                performConversion(INTEGER, FLOAT); break;
                case MOV_F2I:                performConversion(FLOAT, INTEGER); break;
                case MOV_L2D:                performConversion(LONG, DOUBLE); break;
                case MOV_D2L:                performConversion(DOUBLE, LONG); break;

                case UWCMP: {
                    switch (operand) {
                        case ABOVE_EQUAL: performCompare(WORD); break;
                        case ABOVE_THAN:  performCompare(WORD); break;
                        case BELOW_EQUAL: performCompare(WORD); break;
                        case BELOW_THAN:  performCompare(WORD); break;
                        default:          verifyError("Unsupported UWCMP operand: " + operand);
                    }
                    break;
                }
                case UCMP: {
                    switch (operand) {
                        case ABOVE_EQUAL: performCompare(INTEGER); break;
                        case ABOVE_THAN : performCompare(INTEGER); break;
                        case BELOW_EQUAL: performCompare(INTEGER); break;
                        case BELOW_THAN : performCompare(INTEGER); break;
                        default:          verifyError("Unsupported UCMP operand: " + operand);
                    }
                    break;
                }
                case JNICALL: {
                    jnicall(operand);
                    break;
                }
                case JNIOP: {
                    if (!classMethodActor().isNative()) {
                        verifyError("Cannot use " + Bytecodes.nameOf(JNIOP) + " instruction in non-native method " + classMethodActor());
                    }
                    switch (operand) {
                        case LINK: {
                            frame.push(WORD);
                            break;
                        }
                        case J2N:
                        case N2J:
                            break;
                        default:
                            verifyError("Unsupported JNIOP operand: " + operand);
                    }
                    break;
                }
                case INFOPOINT: {
                    opcode = INFOPOINT | ((operand & ~0xff) << 8);
                    if (opcode == HERE) {
                        frame.push(LONG);
                    }
                    break;
                }

                case WRETURN            : performReturn(WORD); break;
                case PAUSE              : break;
                case MEMBAR             : break;
                case BREAKPOINT_TRAP    : break;
                case FLUSHW             : break;
                case LSB                : performConversion(WORD, INTEGER); break;
                case MSB                : performConversion(WORD, INTEGER); break;
                case ALLOCA             : frame.pop(INTEGER); frame.push(WORD); break;
                case STACKHANDLE        : performStackHandle(); break;

                case READREG            : frame.push(WORD); break;
                case WRITEREG           : frame.pop(WORD); break;

                case READBIT            : frame.pop(INTEGER); break;

                default: {
                    verifyError("Unsupported bytecode: " + Bytecodes.nameOf(opcode));
                }
                // Checkstyle: resume
            }
            return true;
        }

        private void performStackHandle() {
            if (frame.top().isCategory2()) {
                frame.pop(CATEGORY2);
            } else {
                frame.pop(CATEGORY1);
            }
            frame.push(WORD);
        }

        private void performCompare(VerificationType type) {
            frame.pop(type);
            frame.pop(type);
            frame.push(BOOLEAN);
        }

        void pointerRead(VerificationType type, boolean intOffset) {
            frame.pop(intOffset ? INTEGER : WORD); // offset
            frame.pop(WORD); // pointer
            frame.push(type); // value
        }

        void pointerWrite(VerificationType type, boolean intOffset) {
            frame.pop(type); // value
            frame.pop(intOffset ? INTEGER : WORD); // offset
            frame.pop(WORD); // pointer
        }

        void pointerGet(VerificationType type) {
            frame.pop(INTEGER); // index
            frame.pop(INTEGER); // displacement
            frame.push(type); // value
        }

        void pointerSet(VerificationType type) {
            frame.pop(type); // value
            frame.pop(INTEGER); // index
            frame.pop(INTEGER); // displacement
            frame.pop(WORD); // pointer
        }

        private void pointerCompareAndSwap(VerificationType type, boolean intOffset) {
            frame.pop(type); // newValue
            frame.pop(type); // expectedValue
            frame.pop(intOffset ? INTEGER : WORD); // offset
            frame.pop(WORD); // pointer
            frame.push(type); // result
        }
    }
}
