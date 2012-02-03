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

import static com.sun.max.vm.heap.HeapSchemeWithTLAB.TLABLogger.Operation;
import static com.sun.max.vm.heap.HeapSchemeWithTLAB.TLABLogger;

import java.awt.*;

import com.sun.max.ins.gui.*;
import com.sun.max.vm.log.VMLog.Record;


public class TLABVMLogArgRenderer extends VMLogArgRenderer {
    public TLABVMLogArgRenderer(VMLogView vmLogView) {
        super(vmLogView);
    }

    @Override
    protected Component getRenderer(int header, int argNum, long argValue) {
        Operation op = TLABLogger.Operation.VALUES[Record.getOperation(header)];
        if (argNum == 1) {
            return VMLogView.ThreadCellRenderer.getThreadRenderer((int) argValue);
        }
        if ((op == Operation.REFILL && argNum == 5) ||
            (op == Operation.PAD && argNum == 2)) {
            return new PlainLabel(vmLogView.inspection(), String.valueOf(argValue));
        }
        return super.getRenderer(header, argNum, argValue);
    }

}
