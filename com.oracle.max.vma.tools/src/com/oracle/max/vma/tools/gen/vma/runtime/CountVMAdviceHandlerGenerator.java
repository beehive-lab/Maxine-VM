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
package com.oracle.max.vma.tools.gen.vma.runtime;

import static com.oracle.max.vma.tools.gen.vma.AdviceGeneratorHelper.*;
import java.lang.reflect.*;
import java.util.*;

import com.oracle.max.vm.ext.vma.*;
import com.oracle.max.vm.ext.vma.runtime.*;
import com.oracle.max.vma.tools.gen.vma.*;

public class CountVMAdviceHandlerGenerator {
    private static int maxLength;

    public static void main(String[] args) throws Exception {
        createGenerator(CountVMAdviceHandlerGenerator.class);
        generateAutoComment();
        SortedMap<String, Integer> enumMap = createEnum(VMAdviceHandler.class);
        out.printf("%n    private static final int MAX_LENGTH = %d;%n%n", maxLength);
        for (Method m : VMAdviceHandler.class.getMethods()) {
            String name = m.getName();
            if (name.startsWith("advise")) {
                generate(m, enumMap);
            }
        }
        AdviceGeneratorHelper.updateSource(CountsVMAdviceHandler.class, null, false);
    }

    private static void generate(Method m, SortedMap<String, Integer> enumMap) {
        out.printf("    @Override%n");
        generateSignature(m, null);
        out.printf(" {%n");
        String[] name = stripPrefix(m.getName());
        out.printf("        counts[%d][%d]++;%n", enumMap.get(name[1]), AdviceMode.valueOf(name[0]).ordinal());
        out.printf("    }%n%n");
    }

    private static final String ADVISE_BEFORE = "adviseBefore";
    private static final String ADVISE_AFTER = "adviseAfter";

    public static String[] stripPrefix(String name) {
        String[] result = new String[2];
        int index = name.indexOf(ADVISE_BEFORE);
        if (index >= 0) {
            result[0] = AdviceMode.BEFORE.name();
            result[1] = name.substring(ADVISE_BEFORE.length());
            return result;
        }
        index = name.indexOf(ADVISE_AFTER);
        if (index >= 0) {
            result[0] = AdviceMode.AFTER.name();
            result[1] = name.substring(ADVISE_AFTER.length());
            return result;
        }
        assert false;
        return null;
    }

    public static SortedMap<String, Integer> createEnum(Class<?> adviceClass) {
        SortedMap<String, Integer> result = new TreeMap<String, Integer>();
        out.printf("    enum AdviceMethod {%n");
        int ordinal = 0;
        boolean first = true;
        for (Method m : adviceClass.getMethods()) {
            if (m.getName().startsWith("advise")) {
                String[] name = stripPrefix(m.getName());
                if (result.get(name[1]) == null) {
                    if (!first) {
                        out.printf(",%n");
                    }
                    out.printf("        %s", name[1]);
                    result.put(name[1], ordinal++);
                    first = false;
                    if (name[1].length() > maxLength) {
                        maxLength = name[1].length();
                    }
                }
            }
        }
        out.printf(";%n    }%n");
        return result;
    }
}
