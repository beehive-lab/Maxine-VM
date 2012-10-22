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
package com.oracle.max.vma.tools.gen.store.txt;

import static com.oracle.max.vma.tools.gen.vma.AdviceGeneratorHelper.*;

import java.lang.reflect.*;

import com.oracle.max.vm.ext.vma.store.txt.*;
import com.oracle.max.vma.tools.gen.vma.*;

/**
 * Generate the interface for methods that contain class, field and method names as arguments
 * where they are passed without the assumption of short forms. A short form generator
 * can then be interposed to allows passing on to {@link VMATextStore}.
 */
public class VMANSFTextStoreGenerator {
    public static void main(String[] args) throws Exception {
        createGenerator(VMANSFTextStoreGenerator.class);
        generateAutoComment();
        for (Method m : VMATextStore.class.getMethods()) {
            if (include(m.getName())) {
                generate(m);
            }
        }
        AdviceGeneratorHelper.updateSource(VMANSFTextStoreIntf.class, null, false);
    }

    private static void generate(Method m) {
        final String name = m.getName();
        out.printf("%svoid %s(long time, String threadName, int bci", INDENT4, name);
        if (name.endsWith("GetField")) {
            out.print(", long objId, String className, long clId, String fieldName");
        } else if (name.contains("PutField")) {
            out.printf(", long objId, String className, long clId, String fieldName, %s value", VMATextStoreGenerator.getLastParameterNameHandlingObject(m));
        } else if (name.endsWith("GetStatic")) {
            out.print(", String className, long clId, String fieldName");
        } else if (name.contains("PutStatic")) {
            out.printf(", String className, long clId, String fieldName, %s value", VMATextStoreGenerator.getLastParameterNameHandlingObject(m));
        } else if (name.contains("New")) {
            out.print(", long objId, String className, long clId");
            if (name.contains("NewArray")) {
                out.print(", int length");
            }
        } else if (name.contains("Invoke") || name.contains("MethodEntry")) {
            out.print(", long objId, String className, long clId, String methodName");
        } else if (name.contains("CheckCast") || name.contains("InstanceOf") || name.contains("unseen")) {
            out.print(", long objId, String className, long clId");
        }
        out.println(");");
    }

    private static boolean include(String name) {
        return name.contains("Invoke") || name.contains("MethodEntry") ||
               name.contains("Static") || name.contains("Field")  ||
               isCastOrInstanceOrNewOrUnseen(name);
    }

    private static boolean isCastOrInstanceOrNewOrUnseen(String name) {
        return name.contains("CheckCast") || name.contains("InstanceOf") || name.contains("New") || name.contains("unseen");
    }

}
