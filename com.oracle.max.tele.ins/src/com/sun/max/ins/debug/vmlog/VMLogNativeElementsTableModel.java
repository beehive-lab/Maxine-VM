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
import com.sun.max.tele.debug.*;
import com.sun.max.tele.object.*;
import com.sun.max.unsafe.*;


abstract class VMLogNativeElementsTableModel extends VMLogElementsTableModel {

    protected final TeleVMLogNative teleVMLogNative;

    protected VMLogNativeElementsTableModel(Inspection inspection, TeleVMLog teleVMLog) {
        super(inspection, teleVMLog);
        this.teleVMLogNative = (TeleVMLogNative) teleVMLog;
    }

    /**
     * Get the size of the record at {@code r}.
     * Default implementation is fixed length.
     */
    protected int nativeRecordSize(Pointer r) {
        return teleVMLogNative.defaultNativeRecordSize();
    }

    protected int nativeRecordSize() {
        return nativeRecordSize(Pointer.zero());
    }

    @Override
    protected TeleHostedLogRecord getRecordFromVM(int id) {
        // native buffer, access directly
        Pointer recordAddress = getRecordAddress(id);
        return teleVMLogNative.getLogRecord(recordAddress, id);
    }

    protected abstract Pointer getRecordAddress(long id);

}

