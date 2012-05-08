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

import javax.swing.table.*;

import com.sun.max.ins.*;
import com.sun.max.ins.value.*;
import com.sun.max.lang.*;
import com.sun.max.tele.*;
import com.sun.max.tele.object.*;
import com.sun.max.tele.type.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.jni.*;
import com.sun.max.vm.reference.*;

/**
 * Base class for custom {@link VMlog.Logger} argument renderers.
 */
public abstract class VMLogArgRenderer extends AbstractInspectionHolder {
    protected VMLogView vmLogView;

    // TODO (mlvdv) extend MaxVM interfaces so that the implementation class is not needed.
    protected TeleVM vm;

    public VMLogArgRenderer(VMLogView vmLogView) {
        super(vmLogView.inspection());
        this.vmLogView = vmLogView;
        this.vm = (TeleVM) vmLogView.vm();
    }

    /**
     * Returns an appropriate {@link TableCellRenderer} for this argument.
     *
     * @param header value from log buffer
     * @param argNum argument index {@code [1 .. N-1]}
     * @param argValue argument value
     * @return
     */
    protected Component getRenderer(int header, int argNum, long argValue) {
        return VMLogArgRendererFactory.defaultVMLogArgRenderer.getRenderer(header, argNum, argValue);
    }

    /**
     * Convenience method for converting a C string to a {@link String}.
     * Perhaps should be elsewhere.
     * @param vm
     * @param cString
     * @return
     */
    static String stringFromCString(TeleVM vm, Pointer cString) {
        byte[] bytes = new byte[1024];
        int index = 0;
        while (true) {
            byte b = vm.memoryIO().readByte(cString, index);
            if (b == 0) {
                break;
            }
            bytes[index++] = b;
        }
        return new String(bytes, 0, index);
    }

    MethodActor getMethodActor(long arg) {
        final MethodID methodID = MethodID.fromWord(Address.fromLong(arg));
        MethodActor methodActor = VmClassAccess.usingTeleClassIDs(new Function<MethodActor>() {
            @Override
            public MethodActor call() throws Exception {
                return MethodID.toMethodActor(methodID);
            }
        });
        return methodActor;
    }

    TeleClassMethodActor getTeleClassMethodActor(long arg) {
        final MethodActor methodActor = getMethodActor(arg);
        final MethodKey methodKey = new MethodKey.MethodActorKey(methodActor);
        final TeleClassMethodActor teleClassMethodActor =  vm.methods().findClassMethodActor(methodKey);
        return teleClassMethodActor;
    }

    TeleMethodActor getTeleMethodActor(long arg) {
        final MethodActor methodActor = getMethodActor(arg);
        final MethodKey methodKey = new MethodKey.MethodActorKey(methodActor);
        final TeleMethodActor teleMethodActor =  vm.methods().findMethodActor(methodKey);
        return teleMethodActor;
    }

    protected WordValueLabel getReferenceValueLabel(Reference reference) {
        return new WordValueLabel(inspection(), WordValueLabel.ValueMode.REFERENCE, reference.toOrigin(), vmLogView.getTable());
    }

    protected Component safeGetReferenceValueLabel(TeleObject teleObject) {
        if (teleObject == null) {
            return gui().getUnavailableDataTableCellRenderer();
        } else {
            return getReferenceValueLabel(teleObject.getReference());
        }

    }

    protected ClassActor getClassActor(final long arg) {
        ClassActor classActor = VmClassAccess.usingTeleClassIDs(new Function<ClassActor>() {
            @Override
            public ClassActor call() throws Exception {
                return ClassID.toClassActor((int) arg);
            }
        });
        return classActor;
    }

    protected TeleClassActor getTeleClassActor(long arg) {
        ClassActor classActor = getClassActor(arg);
        TeleClassActor teleClassActor = vm.classes().findTeleClassActor((int) arg);
        assert teleClassActor.classActor() == classActor;
        return teleClassActor;
    }

    protected String formatMethodActor(long arg) {
        return getMethodActor(arg).format("%H.%n(%p)");
    }

    protected String classActorName(final long arg) {
        return getClassActor(arg).toString();
    }

}
