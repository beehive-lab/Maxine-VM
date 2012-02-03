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

import static com.sun.max.vm.heap.Heap.AllocationLogger.*;

import java.awt.*;

import com.sun.max.ins.gui.*;
import com.sun.max.ins.value.*;
import com.sun.max.ins.value.WordValueLabel.ValueMode;
import com.sun.max.unsafe.*;
import com.sun.max.vm.log.VMLog.Record;


public class AllocationVMLogArgRenderer extends VMLogArgRenderer {
    public AllocationVMLogArgRenderer(VMLogView vmLogView) {
        super(vmLogView);
    }

    @Override
    protected Component getRenderer(int header, int argNum, long argValue) {
        if (argNum == 1) {
            // classActor id
            return getReferenceValueLabel(getTeleClassActor(argValue).getReference());
        } else {
            Operation op = Operation.VALUES[Record.getOperation(header)];
            if (op == Operation.CREATE_ARRAY) {
                if (argNum == 2) {
                    // length
                    return new PlainLabel(vmLogView.inspection(), String.valueOf(argValue));
                } else {
                    argNum--;
                }
            }
            if (argNum == 2) {
                // origin
                return new WordValueLabel(vmLogView.inspection(), ValueMode.WORD, Address.fromLong(argValue), vmLogView.getTable());
            } else if (argNum == 3) {
                // size
                return new PlainLabel(vmLogView.inspection(), String.valueOf(argValue));
            }
        }
        return super.getRenderer(header, argNum, argValue);
    }
}
