/*
 * Copyright (c) 2009 Sun Microsystems, Inc.  All rights reserved.
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
package com.sun.max.tele.object;

import com.sun.max.asm.*;
import com.sun.max.collect.*;
import com.sun.max.io.*;
import com.sun.max.lang.*;
import com.sun.max.tele.*;
import com.sun.max.tele.type.*;
import com.sun.max.vm.bytecode.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.reference.*;


public class TeleCPSTargetMethod extends TeleTargetMethod {

    public TeleCPSTargetMethod(TeleVM teleVM, Reference targetMethodReference) {
        super(teleVM, targetMethodReference);
    }

    public BytecodeInfo[] bytecodeInfos() {
        return null;
    }

    public int[] bytecodeToTargetCodePositionMap() {
        return null;
    }

    @Override
    public final void disassemble(IndentWriter writer) {
        traceExceptionHandlers(writer);
        traceDirectCallees(writer);
        traceFrameDescriptors(writer);
    }

    /**
     * @see TargetMethod#catchRangePositions()
     */
    public int[] getCatchRangePositions() {
        final Reference intArrayReference = teleVM().teleFields().CPSTargetMethod_catchRangePositions.readReference(reference());
        final TeleArrayObject teleIntArrayObject = (TeleArrayObject) teleVM().makeTeleObject(intArrayReference);
        if (teleIntArrayObject == null) {
            return null;
        }
        return (int[]) teleIntArrayObject.shallowCopy();
    }

    /**
     * @see TargetMethod#catchBlockPositions()
     */
    public int[] getCatchBlockPositions() {
        final Reference intArrayReference = teleVM().teleFields().CPSTargetMethod_catchBlockPositions.readReference(reference());
        final TeleArrayObject teleIntArrayObject = (TeleArrayObject) teleVM().makeTeleObject(intArrayReference);
        if (teleIntArrayObject == null) {
            return null;
        }
        return (int[]) teleIntArrayObject.shallowCopy();
    }

    /**
     * Gets the {@linkplain InlineDataDescriptor inline data descriptors} associated with this target method's code in the {@link TeleVM}
     * encoded as a byte array in the format described {@linkplain InlineDataDescriptor here}.
     *
     * @return null if there are no inline data descriptors associated with this target method's code
     * @see TargetMethod#encodedInlineDataDescriptors()
     */
    @Override
    public final byte[] getEncodedInlineDataDescriptors() {
        final Reference encodedInlineDataDescriptorsReference = teleVM().teleFields().CPSTargetMethod_encodedInlineDataDescriptors.readReference(reference());
        final TeleArrayObject teleEncodedInlineDataDescriptors = (TeleArrayObject) teleVM().makeTeleObject(encodedInlineDataDescriptorsReference);
        return teleEncodedInlineDataDescriptors == null ? null : (byte[]) teleEncodedInlineDataDescriptors.shallowCopy();
    }

    private IndexedSequence<TargetJavaFrameDescriptor> javaFrameDescriptors = null;

    public IndexedSequence<TargetJavaFrameDescriptor> getJavaFrameDescriptors() {
        if (javaFrameDescriptors == null) {
            final Reference byteArrayReference = teleVM().teleFields().CPSTargetMethod_compressedJavaFrameDescriptors.readReference(reference());
            final TeleArrayObject teleByteArrayObject = (TeleArrayObject) teleVM().makeTeleObject(byteArrayReference);
            if (teleByteArrayObject == null) {
                return null;
            }
            final byte[] compressedDescriptors = (byte[]) teleByteArrayObject.shallowCopy();
            try {
                javaFrameDescriptors = TeleClassRegistry.usingTeleClassIDs(new Function<IndexedSequence<TargetJavaFrameDescriptor>>() {
                    public IndexedSequence<TargetJavaFrameDescriptor> call() {
                        return TargetJavaFrameDescriptor.inflate(compressedDescriptors);
                    }
                });
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
        return javaFrameDescriptors;
    }

    /**
     * Gets the Java frame descriptor corresponding to a given stop index.
     *
     * @param stopIndex a stop index
     * @return the Java frame descriptor corresponding to {@code stopIndex} or null if there is no Java frame descriptor
     *         for {@code stopIndex}
     */
    @Override
    public TargetJavaFrameDescriptor getJavaFrameDescriptor(int stopIndex) {
        final IndexedSequence<TargetJavaFrameDescriptor> javaFrameDescriptors = getJavaFrameDescriptors();
        if (javaFrameDescriptors != null && stopIndex < javaFrameDescriptors.length()) {
            return javaFrameDescriptors.get(stopIndex);
        }
        return null;
    }

    /**
     * Traces the exception handlers of the compiled code represented by this object.
     *
     * @see TargetMethod#traceExceptionHandlers(IndentWriter)
     */
    public final void traceExceptionHandlers(IndentWriter writer) {
        final int[] catchRangePositions = getCatchRangePositions();
        if (catchRangePositions != null) {
            final int[] catchBlockPositions = getCatchBlockPositions();
            assert catchBlockPositions != null;
            writer.println("Catches: ");
            writer.indent();
            for (int i = 0; i < catchRangePositions.length; i++) {
                if (catchBlockPositions[i] != 0) {
                    final int catchRangeEnd = (i == catchRangePositions.length - 1) ? getCodeLength() : catchRangePositions[i + 1];
                    final int catchRangeStart = catchRangePositions[i];
                    writer.println("[" + catchRangeStart + " .. " + catchRangeEnd + ") -> " + catchBlockPositions[i]);
                }
            }
            writer.outdent();
        }
    }

    /**
     * Traces the {@linkplain #directCallees() direct callees} of the compiled code represented by this object.
     *
     * @see CPSTargetMethod#traceDirectCallees(IndentWriter)
     */
    public final void traceDirectCallees(IndentWriter writer) {
        final Reference[] directCallees = getDirectCallees();
        if (directCallees != null) {
            assert stopPositions != null && directCallees.length <= getNumberOfStopPositions();
            writer.println("Direct Calls: ");
            writer.indent();
            for (int i = 0; i < directCallees.length; i++) {
                final Reference classMethodActorReference = directCallees[i];
                final TeleObject teleObject = teleVM().makeTeleObject(classMethodActorReference);
                if (teleObject instanceof TeleTargetMethod) {
                    writer.println("TeleTargetMethod");
                } else {
                    assert teleObject instanceof TeleClassMethodActor;
                    final TeleClassMethodActor teleClassMethodActor = (TeleClassMethodActor) teleObject;
                    final String calleeName = teleClassMethodActor == null ? "<unknown>" :  teleClassMethodActor.classMethodActor().format("%r %n(%p)" + " in %H");
                    writer.println(getStopPositions().get(i) + " -> " + calleeName);
                }
            }
            writer.outdent();
        }
    }

    /**
     * Traces the {@linkplain #compressedJavaFrameDescriptors() frame descriptors} for the compiled code represented by this object in the {@link TeleVM}.
     *
     * @see TargetMethod#traceDebugInfo(IndentWriter)
     */
    public final void traceFrameDescriptors(IndentWriter writer) {
        final IndexedSequence<TargetJavaFrameDescriptor> javaFrameDescriptors = getJavaFrameDescriptors();
        if (javaFrameDescriptors != null) {
            writer.println("Frame Descriptors: ");
            writer.indent();
            for (int stopIndex = 0; stopIndex < javaFrameDescriptors.length(); ++stopIndex) {
                final TargetJavaFrameDescriptor frameDescriptor = javaFrameDescriptors.get(stopIndex);
                final int stopPosition = getStopPositions().get(stopIndex);
                writer.println(stopPosition + ": " + frameDescriptor);
            }
            writer.outdent();
        }
    }
}
