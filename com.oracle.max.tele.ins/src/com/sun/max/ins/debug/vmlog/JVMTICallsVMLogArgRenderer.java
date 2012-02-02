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

import java.awt.*;

import com.sun.max.ins.gui.*;
import com.sun.max.tele.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.jvmti.*;
import com.sun.max.vm.log.*;
import com.sun.max.vm.reference.*;


public class JVMTICallsVMLogArgRenderer extends VMLogArgRenderer {

    public JVMTICallsVMLogArgRenderer(VMLogView vmLogView) {
        super(vmLogView);
    }

    @Override
    public Component getRenderer(int header, int argNum, long argValue) {
        TeleVM vm = (TeleVM) vmLogView.vm();
        String text = "";
        if (argNum == 1) {
            // Env
            text = Long.toHexString(argValue);
        } else {
            switch (JVMTIFunctions.LogOperations.values()[VMLog.Record.getOperation(header)]) {
                // arg0 is always the env value
                case SetEventNotificationMode:
                    if (argNum == 2) {
                        text = argValue == 1 ? "ENABLE" : "DISABLE";
                    } else if (argNum == 3) {
                        text = JVMTIEvent.name((int) argValue);
                    }
                    break;

                case CreateRawMonitor:
                    if (argNum == 2) {
                        text = stringFromCString(vm, Address.fromLong(argValue).asPointer());
                    }
                    break;

                case RawMonitorEnter:
                case RawMonitorExit:
                case RawMonitorWait:
                case RawMonitorNotify:
                case RawMonitorNotifyAll:
                    // arg1 value is the address (origin) of the JVMTIRawMonitor.Monitor object
                    // from which we can get the name
                    if (argNum == 2) {
                        Reference rawMonitor = Reference.fromOrigin(Address.fromLong(argValue).asPointer());
                        Pointer nameCString = vm.fields().JVMTIRawMonitor$Monitor_name.readWord(rawMonitor).asPointer();
                        text = stringFromCString(vm, nameCString);
                    }
                    break;

                case GetSystemProperty:
                    if (argNum == 2) {
                        text = stringFromCString(vm, Address.fromLong(argValue).asPointer());
                    }
                    break;

                case SetBreakpoint:
                    // Checkstyle: stop
                    if (argNum == 2) {
                    }
                    // Checkstyle: resume
                    break;

                default:
                    text = Long.toHexString(argValue);
            }
        }
        return new PlainLabel(inspection(), text);
    }

}
