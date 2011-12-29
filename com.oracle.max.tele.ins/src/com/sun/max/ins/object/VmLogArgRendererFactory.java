/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.ins.object;

import java.util.*;

import com.sun.max.program.*;
import com.sun.max.tele.*;
import com.sun.max.vm.*;
import com.sun.max.vm.jvmti.*;

/**
 * Factory for custom renderers for {@link VMLog.Logger} arguments.
 * A custom renderer must extend the {@link VMLogArgRenderer} class.
 * Custom render classes must be defined in this package and
 * have name of the form {@code XXXVMLogArgRenderer}, where {@code XXX}
 * is the name of the logger.
 */
public abstract class VmLogArgRendererFactory {

    private static Map<String, VMLogArgRenderer> renderers = new HashMap<String, VMLogArgRenderer>();

    private static class DefaultVMLogArgRenderer extends VMLogArgRenderer {
        @Override
        String getText(TeleVM vm, int op, int argNum, long argValue) {
            return Long.toHexString(argValue);
        }
    }

    static VMLogArgRenderer getArgRenderer(VMLog.Logger logger) {
        VMLogArgRenderer result = renderers.get(logger.name);
        if (result == null) {
            try {
                result = (VMLogArgRenderer) Class.forName(VmLogArgRendererFactory.class.getPackage().getName() + logger.name + "VMLogArgRenderer").newInstance();
            } catch (Exception ex) {
                Trace.line(1, "no custom VMLog argument renderer found for " + logger.name);
                result = new DefaultVMLogArgRenderer();
            }
            renderers.put(logger.name, result);
        }
        return result;
    }
}
