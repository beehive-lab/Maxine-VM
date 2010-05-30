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

import com.sun.max.collect.*;
import com.sun.max.io.*;
import com.sun.max.lang.*;
import com.sun.max.tele.*;
import com.sun.max.tele.type.*;
import com.sun.max.vm.bytecode.*;
import com.sun.max.vm.cps.target.*;
import com.sun.max.vm.reference.*;

public class TeleCPSTargetMethod extends TeleTargetMethod {

    public TeleCPSTargetMethod(TeleVM teleVM, Reference targetMethodReference) {
        super(teleVM, targetMethodReference);
    }

    @Override
    public final void disassemble(IndentWriter writer) {
        targetMethod().traceBundle(writer);
    }

    private IndexedSequence<TargetJavaFrameDescriptor> javaFrameDescriptors = null;

    @Override
    public IndexedSequence<TargetJavaFrameDescriptor> getJavaFrameDescriptors() {
        if (javaFrameDescriptors == null) {
            final byte[] compressedDescriptors = ((CPSTargetMethod) targetMethod()).compressedJavaFrameDescriptors();
            if (compressedDescriptors == null) {
                return null;
            }
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
    public BytecodeLocation getBytecodeLocation(int stopIndex) {
        final IndexedSequence<TargetJavaFrameDescriptor> javaFrameDescriptors = getJavaFrameDescriptors();
        if (javaFrameDescriptors != null && stopIndex < javaFrameDescriptors.length()) {
            return javaFrameDescriptors.get(stopIndex);
        }
        return null;
    }
}
