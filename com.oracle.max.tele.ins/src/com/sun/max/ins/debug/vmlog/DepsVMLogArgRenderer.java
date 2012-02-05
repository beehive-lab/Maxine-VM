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

import static com.sun.max.vm.compiler.deps.DependenciesManager.Logger.*;

import java.awt.*;

import com.sun.max.ins.gui.*;
import com.sun.max.vm.compiler.deps.*;
import com.sun.max.vm.log.VMLog.Record;

/**
 * Argument rendering for {@link DependenciesManager}.
 */
public class DepsVMLogArgRenderer extends VMLogArgRenderer {

    private static final Operation[] OPERATION_VALUES = Operation.values();

    public DepsVMLogArgRenderer(VMLogView vmLogView) {
        super(vmLogView);
    }

    @Override
    protected Component getRenderer(int header, int argNum, long argValue) {
        String text = null;
        if (argNum == 1) {
            if (argValue == DependenciesManager.Logger.NULL_TM.asAddress().toLong()) {
                text = "UNSET";
            } else {
                return safeGetReferenceValueLabel(getTeleClassMethodActor(argValue));
            }
        } else {
            int opCode = Record.getOperation(header);
            Operation op = OPERATION_VALUES[opCode];
            if (op == Operation.Add || op == Operation.Remove) {
                if (argNum == 2) {
                    text = String.valueOf(argValue);
                } else if (argNum == 3) {
                    return safeGetReferenceValueLabel(getTeleClassActor(argValue));
                }
            } else if (op == Operation.Register) {
                if (argNum == 2) {
                    text = String.valueOf(argValue);
                }
            } else if (op == Operation.InvalidateDeps) {
                if (argNum == 2) {
                    return safeGetReferenceValueLabel(getTeleClassActor(argValue));
                }
            } else if (op == Operation.Invalidated) {
                if (argNum == 2) {
                    text = String.valueOf(argValue);
                }
            } else if (op == Operation.InvalidateUCT) {
                if (argNum == 2 || argNum == 3) {
                    return safeGetReferenceValueLabel(getTeleClassActor(argValue));
                }
            } else if (op == Operation.InvalidateUCM) {
                if (argNum == 2) {
                    return safeGetReferenceValueLabel(getTeleClassActor(argValue));
                } else if (argNum == 3 || argNum == 4) {
                    return safeGetReferenceValueLabel(getTeleClassMethodActor(argValue));
                }
            }
        }
        if (text != null) {
            return new PlainLabel(inspection(), text);
        } else {
            return super.getRenderer(header, argNum, argValue);
        }
    }


}
