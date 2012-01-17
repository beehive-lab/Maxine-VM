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
package com.sun.max.ins.debug.vmlog;

import com.sun.max.ins.*;
import com.sun.max.tele.*;
import com.sun.max.tele.memory.*;
import com.sun.max.tele.object.*;
import com.sun.max.vm.log.java.fix.*;
import com.sun.max.vm.reference.*;


/**
 * Model corresponds to {@link VMLogArrayFixed}.
 * Fixed length records stored in fixed length array (circular buffer).
 * So the {@code id} trivially maps to the array index of the associated record.
 */
class VMLogArrayElementsTableModel extends VMLogElementsTableModel {
    private TeleArrayObject teleLogArrayBuffer;

    protected VMLogArrayElementsTableModel(Inspection inspection, VMLogView vmLogView) {
        super(inspection, vmLogView);
        Reference logBufferRef = vm.fields().VMLogArray_buffer.readReference(vmLogView.vmLogRef);
        teleLogArrayBuffer = (TeleArrayObject) VmObjectAccess.make(vm).makeTeleObject(logBufferRef);
    }

    @Override
    protected HostedLogRecord getRecordFromVM(int id) {
        TeleVM vm = (TeleVM) vm();
        int index = id % vmLogView.logBufferEntries;
        Reference recordRef = teleLogArrayBuffer.readElementValue(index).asReference();
        return new HostedLogRecord(
                    id,
                    vm.fields().VMLogArray$Record0_header.readInt(recordRef),
                    vm.fields().VMLogArray$Record1_arg1.readWord(recordRef),
                    vm.fields().VMLogArray$Record2_arg2.readWord(recordRef),
                    vm.fields().VMLogArray$Record3_arg3.readWord(recordRef),
                    vm.fields().VMLogArray$Record4_arg4.readWord(recordRef),
                    vm.fields().VMLogArray$Record5_arg5.readWord(recordRef),
                    vm.fields().VMLogArray$Record6_arg6.readWord(recordRef),
                    vm.fields().VMLogArray$Record7_arg7.readWord(recordRef));
    }
}

