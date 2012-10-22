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
package com.oracle.max.vma.tools.gen.store;

import static com.oracle.max.vma.tools.gen.vma.AdviceGeneratorHelper.*;

import java.lang.reflect.*;

import com.oracle.max.vm.ext.vma.store.*;
import com.oracle.max.vm.ext.vma.store.txt.*;
import com.oracle.max.vma.tools.gen.vma.*;
import com.sun.max.annotate.*;


@HOSTED_ONLY
public class VMAIdStoreGenerator {
    public static void main(String[] args) throws Exception {
        createGenerator(VMAIdStoreGenerator.class);
        generateAutoComment();

        for (Method m : VMATextStore.class.getDeclaredMethods()) {
            if (include(m.getName())) {
                generate(m);
            }
        }
        AdviceGeneratorHelper.updateSource(VMAIdTextStore.class, null, false);

    }

    private static void generate(Method m) {
        String name = m.getName();
        out.printf("%svoid %s(", INDENT4, name);
        Class<?>[] params = m.getParameterTypes();
        boolean seenString = false;
        for (int i = 0; i < params.length; i++) {
            Class<?> klass = params[i];
            switch (i) {
                case 0: // time
                    assert klass == long.class;
                    out.print("long time");
                    break;

                case 1: // thread name, drop
                    assert klass == String.class;
                    continue;

                case 2: // bci
                    if (name.equals("unseenObject")) {
                        out.print(", " + klass.getSimpleName() + " objId");
                    } else {
                        assert klass == int.class;
                        out.print(", int bci");
                    }
                    break;

                default:
                    if (klass == String.class) {
                        if (!seenString) {
                            // this is the class, always followed by clId both of which we drop
                            if (isCastOrInstanceOrNewOrUnseen(name)) {
                                out.print(", int classId");
                            }
                            // Checkstyle: stop
                            i++;
                            // Checkstyle: resume
                        } else {
                            out.print(", int " + (hasMethod(name) ? "methodId" : "fieldId"));
                        }
                        seenString = true;
                    } else {
                        out.print(", " + klass.getSimpleName() + " arg" + (i + 1));
                    }
            }
        }
        out.println(");");
    }

    private static boolean hasMethod(String name) {
        return name.contains("Invoke") || name.contains("MethodEntry");
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
