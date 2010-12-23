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
import com.sun.max.vm.classfile.*;
import com.sun.max.vm.reference.*;

/**
 * Canonical surrogate for an object of type {@link CodeAttribute} in the VM.
 *
 * @author Michael Van De Vanter
 */
public class TeleCodeAttribute extends TeleTupleObject {

    // Keep construction minimal for both performance and synchronization.
    protected TeleCodeAttribute(TeleVM vm, Reference codeAttributeReference) {
        super(vm, codeAttributeReference);
    }

    /**
     * Reads the Java bytecodes from the VM.
     * <br>
     * Must be called in a thread holding the VM lock.
     *
     * @return the bytecodes in an array.
     */
    public final byte[] readBytecodes() {
        assert vm().lockHeldByCurrentThread();
        final Reference byteArrayReference = vm().teleFields().CodeAttribute_code.readReference(reference());
        final TeleArrayObject teleByteArrayObject = (TeleArrayObject) heap().makeTeleObject(byteArrayReference);
        return (byte[]) teleByteArrayObject.shallowCopy();
    }

    /**
     * Gets the local surrogate for the {@link ConstantPool} associated with this code in the VM.
     */
    public final TeleConstantPool getTeleConstantPool() {
        final Reference constantPoolReference = vm().teleFields().CodeAttribute_constantPool.readReference(reference());
        return (TeleConstantPool) heap().makeTeleObject(constantPoolReference);
    }

}
