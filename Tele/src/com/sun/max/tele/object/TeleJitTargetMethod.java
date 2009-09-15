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
package com.sun.max.tele.object;

import com.sun.max.tele.*;
import com.sun.max.vm.bytecode.*;
import com.sun.max.vm.reference.*;

/**
 * Canonical surrogate for a Jit compilation of a Java {@link ClassMethod} in the {@link TeleVM}.
 *
 * @author Michael Van De Vanter
 */
public class TeleJitTargetMethod extends TeleTargetMethod {

    TeleJitTargetMethod(TeleVM teleVM, Reference jitTargetMethodReference) {
        super(teleVM, jitTargetMethodReference);
    }

    public int[] bytecodeToTargetCodePositionMap() {
        final Reference intArrayReference = teleVM().fields().JitTargetMethod_bytecodeToTargetCodePositionMap.readReference(reference());
        final TeleArrayObject teleIntArray = (TeleArrayObject) teleVM().makeTeleObject(intArrayReference);
        return teleIntArray == null ? null : (int[]) teleIntArray.shallowCopy();
    }

    public BytecodeInfo[] bytecodeInfos() {
        final Reference infoArrayReference = teleVM().fields().JitTargetMethod_bytecodeInfos.readReference(reference());
        final TeleArrayObject teleBytecodeInfoArray = (TeleArrayObject) teleVM().makeTeleObject(infoArrayReference);
        return teleBytecodeInfoArray == null ? null : (BytecodeInfo[]) teleBytecodeInfoArray.deepCopy();
    }

    @Override
    protected DeepCopier reducedDeepCopier() {
        return new ReducedDeepCopier().omit(teleVM().fields().JitTargetMethod_referenceMapEditor.fieldActor());
    }
}
