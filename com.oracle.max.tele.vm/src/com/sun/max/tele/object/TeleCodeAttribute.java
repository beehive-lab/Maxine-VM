/*
 * Copyright (c) 2008, 2012, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.tele.object;

import com.sun.max.tele.*;
import com.sun.max.tele.reference.*;
import com.sun.max.vm.classfile.*;
import com.sun.max.vm.classfile.constant.*;

/**
 * Canonical surrogate for an object of type {@link CodeAttribute} in the VM.
 */
public class TeleCodeAttribute extends TeleTupleObject {

    // Keep construction minimal for both performance and synchronization.
    protected TeleCodeAttribute(TeleVM vm, RemoteReference codeAttributeReference) {
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
        final RemoteReference byteArrayReference = jumpForwarder(fields().CodeAttribute_code.readReference(reference()));
        final TeleArrayObject teleByteArrayObject = (TeleArrayObject) objects().makeTeleObject(byteArrayReference);
        return (byte[]) teleByteArrayObject.shallowCopy();
    }

    /**
     * Gets the local surrogate for the {@link ConstantPool} associated with this code in the VM.
     */
    public final TeleConstantPool getTeleConstantPool() {
        final RemoteReference constantPoolReference = jumpForwarder(fields().CodeAttribute_cp.readReference(reference()));
        return (TeleConstantPool) objects().makeTeleObject(constantPoolReference);
    }

}
