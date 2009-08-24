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
package com.sun.max.vm.compiler.target;

import com.sun.max.annotate.*;
import com.sun.max.io.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.code.*;
import com.sun.max.vm.compiler.*;

/**
 *
 * Target method that saves for each catch block the ranges in the code that can refer to them. Does not include the type of the catched exception.
 *
 * @author Thomas Wuerthinger
 */
public abstract class ExceptionRangeTargetMethod extends TargetMethod {

    @INSPECTED
    private int[] catchRangePositions;
    @INSPECTED
    private int[] catchBlockPositions;

    public ExceptionRangeTargetMethod(ClassMethodActor classMethodActor, DynamicCompilerScheme compilerScheme) {
        super(classMethodActor, compilerScheme);
    }

    /**
     * Gets the array of positions denoting which ranges of code are covered by an
     * exception dispatcher. The {@code n}th range includes positions
     * {@code [catchRangePositions()[n] .. catchRangePositions()[n + 1])} unless {@code n == catchRangePositions().length - 1}
     * in which case it includes positions {@code [catchRangePositions()[n] .. codeLength())}. Note that these range
     * specifications exclude the last position.
     * <p>
     * The address of the dispatcher for range {@code n} is {@code catchBlockPositions()[n]}. If
     * {@code catchBlockPositions()[n] == 0}, then {@code n} denotes a code range not covered by an exception dispatcher.
     * <p>
     * In the example below, any exception that occurs while the instruction pointer corresponds to position 3, 4, 5 or 6 will
     * be handled by the dispatch code at position 7. An exception that occurs at any other position will be propagated to
     * the caller.
     * <pre>{@code
     *
     * catch range positions: [0, 3, 7]
     * catch block positions: [0, 7, 0]
     *
     *             caller          +------+      caller
     *                ^            |      |        ^
     *                |            |      V        |
     *          +-----|-----+------|------+--------|---------+
     *     code | exception |  exception  |    exception     |
     *          +-----------+-------------+------------------+
     *           0        2  3           6 7               12
     * }</pre>
     *
     *
     * @return positions of exception dispatcher ranges in the machine code, matched with the
     *         {@linkplain #catchBlockPositions() catch block positions}
     */
    public final int[] catchRangePositions() {
        return catchRangePositions;
    }

    /**
     * @see #catchRangePositions()
     */
    public final int numberOfCatchRanges() {
        return (catchRangePositions == null) ? 0 : catchRangePositions.length;
    }

    /**
     * @see #catchRangePositions()
     */
    public final int[] catchBlockPositions() {
        return catchBlockPositions;
    }

    /**
     * Traces the exception handlers of the compiled code represented by this object.
     *
     * @param writer where the trace is written
     */
    @Override
    public final void traceExceptionHandlers(IndentWriter writer) {
        if (catchRangePositions != null) {
            assert catchBlockPositions != null;
            writer.println("Catches:");
            writer.indent();
            for (int i = 0; i < catchRangePositions.length; i++) {
                if (catchBlockPositions[i] != 0) {
                    final int catchRangeEnd = (i == catchRangePositions.length - 1) ? code.length : catchRangePositions[i + 1];
                    final int catchRangeStart = catchRangePositions[i];
                    writer.println("[" + catchRangeStart + " .. " + catchRangeEnd + ") -> " + catchBlockPositions[i]);
                }
            }
            writer.outdent();
        }
    }

    public final void setGenerated(int[] catchRangePositions,
                    int[] catchBlockPositions,
                    int[] stopPositions,
                    byte[] compressedJavaFrameDescriptors,
                    Object[] directCallees,
                    int numberOfIndirectCalls,
                    int numberOfSafepoints,
                    byte[] referenceMaps,
                    byte[] scalarLiterals,
                    Object[] referenceLiterals,
                    Object codeOrCodeBuffer,
                    byte[] encodedInlineDataDescriptors,
                    int frameSize,
                    int frameReferenceMapSize,
                    TargetABI abi) {
        this.catchRangePositions = catchRangePositions;
        this.catchBlockPositions = catchBlockPositions;
        super.setGenerated(stopPositions, compressedJavaFrameDescriptors, directCallees, numberOfIndirectCalls, numberOfSafepoints, referenceMaps, scalarLiterals, referenceLiterals, codeOrCodeBuffer,
            encodedInlineDataDescriptors, frameSize, frameReferenceMapSize, abi);
    }

    public final TargetMethod duplicate() {
        final TargetGeneratorScheme targetGeneratorScheme = (TargetGeneratorScheme) compilerScheme;
        final ExceptionRangeTargetMethod duplicate = targetGeneratorScheme.targetGenerator().createIrMethod(classMethodActor());
        final TargetBundleLayout targetBundleLayout = TargetBundleLayout.from(this);
        Code.allocate(targetBundleLayout, duplicate);
        duplicate.setGenerated(
            catchRangePositions(),
            catchBlockPositions(),
            stopPositions,
            compressedJavaFrameDescriptors,
            directCallees(),
            numberOfIndirectCalls(),
            numberOfSafepoints(),
            referenceMaps(),
            scalarLiterals(),
            referenceLiterals(),
            code(),
            encodedInlineDataDescriptors,
            frameSize(),
            frameReferenceMapSize(),
            abi()
        );
        return duplicate;
    }

    /**
     * Overwrite this method if the top frame instruction pointer must be adjusted for this target method.
     * @return the value that should be added to the top frame instruction pointer
     */
    public int topFrameInstructionAdjustment() {
        return 0;
    }

    @Override
    public final Address throwAddressToCatchAddress(boolean isTopFrame, Address throwAddress, Class<? extends Throwable> throwableClass) {
        if (catchRangePositions != null) {


            int throwOffset = throwAddress.minus(codeStart).toInt();


            throwOffset += topFrameInstructionAdjustment();

            for (int i = catchRangePositions.length - 1; i >= 0; i--) {
                if (throwOffset >= catchRangePositions[i]) {
                    final int catchBlockPosition = catchBlockPositions[i];
                    if (catchBlockPosition <= 0) {
                        return Address.zero();
                    }
                    return codeStart.plus(catchBlockPosition);
                }
            }
        }
        return Address.zero();
    }
}
