/*
 * Copyright (c) 2009, 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.tele.object;

import java.util.*;

import com.sun.max.io.*;
import com.sun.max.lang.*;
import com.sun.max.tele.*;
import com.sun.max.tele.type.*;
import com.sun.max.vm.bytecode.*;
import com.sun.max.vm.cps.target.*;
import com.sun.max.vm.reference.*;

public class TeleCPSTargetMethod extends TeleTargetMethod {

    public TeleCPSTargetMethod(TeleVM teleVM, Reference targetMethodReference) {
        super(teleVM, targetMethodReference);
    }

    @Override
    public final void disassemble(IndentWriter writer) {
        targetMethod().traceBundle(writer);
    }

    private List<TargetJavaFrameDescriptor> javaFrameDescriptors = null;

    @Override
    public List<TargetJavaFrameDescriptor> getJavaFrameDescriptors() {
        if (javaFrameDescriptors == null) {
            final byte[] compressedDescriptors = ((CPSTargetMethod) targetMethod()).compressedJavaFrameDescriptors();
            if (compressedDescriptors == null) {
                return null;
            }
            try {
                javaFrameDescriptors = TeleClassRegistry.usingTeleClassIDs(new Function<List<TargetJavaFrameDescriptor>>() {
                    public List<TargetJavaFrameDescriptor> call() {
                        return TargetJavaFrameDescriptor.inflate(compressedDescriptors);
                    }
                });
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
        return javaFrameDescriptors;
    }

    @Override
    public BytecodeLocation getBytecodeLocation(int stopIndex) {
        final List<TargetJavaFrameDescriptor> javaFrameDescriptors = getJavaFrameDescriptors();
        if (javaFrameDescriptors != null && stopIndex < javaFrameDescriptors.size()) {
            return javaFrameDescriptors.get(stopIndex);
        }
        return null;
    }
}
