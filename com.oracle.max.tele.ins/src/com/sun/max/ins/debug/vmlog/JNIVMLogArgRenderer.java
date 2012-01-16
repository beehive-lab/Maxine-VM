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

import com.sun.max.lang.*;
import com.sun.max.tele.*;
import com.sun.max.tele.type.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.jni.*;
import com.sun.max.vm.log.VMLog.Record;

import static com.sun.max.vm.jni.JniFunctions.JxxFunctionsLogger.*;


public class JNIVMLogArgRenderer extends VMLogArgRenderer {

    @Override
    String getText(TeleVM vm, int header, int argNum, long argValue) {
        if (argNum == 1) {
            Word mode = Address.fromLong(argValue);
            if (mode.equals(UPCALL_ENTRY)) {
                return "UPCALL_ENTRY";
            } else if (mode.equals(UPCALL_EXIT)) {
                return "UPCALL_EXIT";
            } else if (mode.equals(DOWNCALL_ENTRY)) {
                return "DOWNCALL_ENTRY";
            } else if (mode.equals(DOWNCALL_EXIT)) {
                return "DOWNCALL_EXIT";
            } else if (mode.equals(INVOKE_ENTRY)) {
                return "INVOKE_ENTRY";
            } else if (mode.equals(LINK_ENTRY)) {
                return "DYNAMIC_LINK";
            } else if (mode.equals(REGISTER_ENTRY)) {
                return "REGISTER NATIVE";
            } else {
                return "UNKNOWN MODE: " + argValue;
            }
        } else if (argNum == 2) {
            int op = Record.getOperation(header);
            if (op == JniFunctions.LogOperations.ReflectiveInvocation.ordinal() ||
                op == JniFunctions.LogOperations.NativeMethodCall.ordinal() ||
                op == JniFunctions.LogOperations.DynamicLink.ordinal() ||
                op == JniFunctions.LogOperations.RegisterNativeMethod.ordinal()) {
                final MethodID methodID = MethodID.fromWord(Address.fromLong(argValue));
                MethodActor methodActor = VmClassAccess.usingTeleClassIDs(new Function<MethodActor>() {
                    @Override
                    public MethodActor call() throws Exception {
                        return MethodID.toMethodActor(methodID);
                    }
                });
                return methodActor.format("%H.%n(%p)");
            }
        }
        // default
        return VMLogArgRendererFactory.defaultVMLogArgRenderer.getText(vm, header, argNum, argValue);
    }

}
