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

import static com.sun.max.vm.jni.JniFunctions.JxxFunctionsLogger.*;

import java.awt.*;

import com.sun.max.ins.gui.*;
import com.sun.max.ins.value.*;
import com.sun.max.ins.value.WordValueLabel.ValueMode;
import com.sun.max.unsafe.*;
import com.sun.max.vm.jni.*;
import com.sun.max.vm.log.VMLog.Record;


public class JNIVMLogArgRenderer extends VMLogArgRenderer {

    public JNIVMLogArgRenderer(VMLogView vmLogView) {
        super(vmLogView);
    }

    @Override
    protected Component getRenderer(int header, int argNum, long argValue) {
        String text = null;
        if (argNum == 1) {
            Word mode = Address.fromLong(argValue);
            if (mode.equals(UPCALL_ENTRY)) {
                text = "UPCALL_ENTRY";
            } else if (mode.equals(UPCALL_EXIT)) {
                text = "UPCALL_EXIT";
            } else if (mode.equals(DOWNCALL_ENTRY)) {
                text = "DOWNCALL_ENTRY";
            } else if (mode.equals(DOWNCALL_EXIT)) {
                text = "DOWNCALL_EXIT";
            } else if (mode.equals(INVOKE_ENTRY)) {
                text = "INVOKE_ENTRY";
            } else if (mode.equals(LINK_ENTRY)) {
                text = "DYNAMIC_LINK";
            } else if (mode.equals(REGISTER_ENTRY)) {
                text = "REGISTER NATIVE";
            } else {
                text = "UNKNOWN MODE: " + argValue;
            }
        } else if (argNum == 2) {
            int op = Record.getOperation(header);
            if (op == JniFunctions.LogOperations.ReflectiveInvocation.ordinal() ||
                op == JniFunctions.LogOperations.NativeMethodCall.ordinal() ||
                op == JniFunctions.LogOperations.DynamicLink.ordinal() ||
                op == JniFunctions.LogOperations.RegisterNativeMethod.ordinal()) {
                return safeGetReferenceValueLabel(getTeleClassMethodActor(argValue));
            }
        }
        if (text != null) {
            return new PlainLabel(inspection(), text);
        } else {
            return new WordValueLabel(inspection(), ValueMode.WORD, Address.fromLong(argValue), vmLogView.getTable(), true);
        }
    }

}
