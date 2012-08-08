/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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
import com.sun.max.tele.debug.*;
import com.sun.max.tele.reference.*;
import com.sun.max.vm.log.*;
import com.sun.max.vm.log.java.*;

/**
 * Access to simple array implementations the VM's {@linkplain VMLog log}.
 *
 * @see VMLogArray
 */
public final class TeleVMLogArray extends TeleVMLog {

    private TeleArrayObject teleLogArrayBuffer = null;

    protected TeleVMLogArray(TeleVM vm, RemoteReference vmLogArrayReference) {
        super(vm, vmLogArrayReference);
    }

    public TeleHostedLogRecord getLogRecord(int id) {
        if (teleLogArrayBuffer == null) {
            final RemoteReference logBufferRef = fields().VMLogArray_buffer.readRemoteReference(reference());
            if (!logBufferRef.isZero()) {
                teleLogArrayBuffer = (TeleArrayObject) objects().makeTeleObject(logBufferRef);
            }
        }
        if (teleLogArrayBuffer != null) {
            int index = id % logEntries();
            RemoteReference recordRef = teleLogArrayBuffer.readRemoteReference(index);
            return new TeleHostedLogRecord(
                        id,
                        fields().VMLogArray$Record0_header.readInt(recordRef),
                        fields().VMLogArray$Record1_arg1.readWord(recordRef),
                        fields().VMLogArray$Record2_arg2.readWord(recordRef),
                        fields().VMLogArray$Record3_arg3.readWord(recordRef),
                        fields().VMLogArray$Record4_arg4.readWord(recordRef),
                        fields().VMLogArray$Record5_arg5.readWord(recordRef),
                        fields().VMLogArray$Record6_arg6.readWord(recordRef),
                        fields().VMLogArray$Record7_arg7.readWord(recordRef));
        }
        return null;
    }
}

