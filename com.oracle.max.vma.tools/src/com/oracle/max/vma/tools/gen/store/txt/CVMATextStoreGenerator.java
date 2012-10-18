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
import java.util.*;

import com.oracle.max.vm.ext.vma.*;
import com.oracle.max.vm.ext.vma.store.txt.*;
import com.oracle.max.vma.tools.gen.vma.*;

/**
 * Generate the string codes used to identify advice methods in textual log files.
 */
public class CVMATextStoreGenerator {

    private static final String ADVISE_BEFORE = "adviseBefore";
    private static final String ADVISE_AFTER = "adviseAfter";
    private static Map<String, String> codeMap = new HashMap<String, String>();
    private static Map<String, String> reverseCodeMap = new HashMap<String, String>();
    private static Map<String, String> declMap = new HashMap<String, String>();

    public static void main(String[] args) throws Exception {
        boolean duplicates = false;
        for (Method m : VMATextStore.class.getDeclaredMethods()) {
            String name = m.getName();
            if (name.equals("setTimeStampGenerator")) {
                continue;
            }
            int ix = name.lastIndexOf("Object");
            if (ix > 0) {
                name = name.substring(0, ix);
            }
            if (name.equals("initializeStore")) {
                codeMap.put(name, "IL");
                reverseCodeMap.put("IL", name);
            } else if (name.equals("finalizeStore")) {
                codeMap.put(name, "FL");
                reverseCodeMap.put("FL", name);
            } else if (name.equals("unseen")) {
                codeMap.put(name, "U");
                reverseCodeMap.put("U", name);
            } else if (name.equals("removal")) {
                codeMap.put(name, "D");
                reverseCodeMap.put("D", name);
            } else if (name.equals("threadSwitch")) {
                codeMap.put(name, "ZT");
                reverseCodeMap.put("ZT", name);
            } else if (name.equals("newThread")) {
                // ignore
            } else {
                if (codeMap.get(name) == null) {
                    String code = extractCode(name);
                    //System.out.printf("name %s, code %s%n", name, code);
                    codeMap.put(name, code);
                    String xCode = reverseCodeMap.get(code);
                    if (xCode == null) {
                        reverseCodeMap.put(code, name);
                    } else {
                        System.out.printf("  duplicate code (%s)%n", xCode);
                        duplicates = true;
                    }
                }
            }
        }
        //System.out.printf("There are %d codes%n", codeMap.size());
        if (duplicates) {
            System.err.println("duplicate codes");
        } else {
            createGenerator(CVMATextStoreGenerator.class);
            generateAutoComment();
            out.printf("    public enum Key {%n");
            out.printf("        CLASS_DEFINITION(\"C\"),%n");
            out.printf("        FIELD_DEFINITION(\"F\"),%n");
            out.printf("        THREAD_DEFINITION(\"T\"),%n");
            out.printf("        METHOD_DEFINITION(\"M\"),%n");
            int size = codeMap.size();
            int count = 1;
            for (Map.Entry<String, String> entry : codeMap.entrySet()) {
                String decl = changeCase(entry.getKey());
                out.printf("        %s(\"%s\")%s%n", decl, entry.getValue(), count < size ? "," : ";");
                declMap.put(entry.getKey(), decl);
                count++;
            }
            out.printf("        public final String code;%n");
            out.printf("        private Key(String code) {%n");
            out.printf("            this.code = code;%n");
            out.printf("        }%n");
            out.printf("    }%n%n");
            generateHasIdSet();
            out.println();
            generateHasBciSet();
            AdviceGeneratorHelper.updateSource(VMATextStoreFormat.class, null, false);
        }
    }

    private static void generateHasIdSet() {
        out.printf("    public static final EnumSet<Key> hasIdSet = EnumSet.of(%n");
        HashSet<String> doneSet = new HashSet<String>();
        boolean first = true;
        for (Method m : VMAdviceHandler.class.getMethods()) {
            String name = m.getName();
            if (name.startsWith("advise") && !doneSet.contains(name) &&
                            !(name.endsWith("Return") || name.endsWith("PutStatic") || name.endsWith("GetStatic"))) {
                String p = getNthParameterName(m, 2);
                if (p != null && (p.equals("Object") || p.equals("Throwable"))) {
                    if (first) {
                        first = false;
                    } else {
                        out.println(",");
                    }
                    out.printf("        Key.%s", declMap.get(m.getName()));
                }
                doneSet.add(name);
            }
        }
        out.println(");");
    }

    private static void generateHasBciSet() {
        out.printf("    public static final EnumSet<Key> hasBciSet = EnumSet.of(%n");
        HashSet<String> doneSet = new HashSet<String>();
        boolean first = true;
        for (Method m : VMAdviceHandler.class.getMethods()) {
            if (AdviceGeneratorHelper.isBytecodeAdviceMethod(m)) {
                String name = m.getName();
                if (!doneSet.contains(name)) {
                    if (first) {
                        first = false;
                    } else {
                        out.println(",");
                    }
                    out.printf("        Key.%s", declMap.get(m.getName()));
                }
                doneSet.add(name);
            }
        }
        out.println(");");

    }

    private static String extractCode(String name) {
        StringBuilder sb = new StringBuilder();
        if (name.startsWith(ADVISE_BEFORE)) {
            name = name.substring(ADVISE_BEFORE.length());
            sb.append('B');
        } else if (name.startsWith(ADVISE_AFTER)) {
            name = name.substring(ADVISE_AFTER.length());
            sb.append('A');
        }
        if (name.equals("InvokeSpecial")) {
            name = "InvokeZpecial";
        } else if (name.equals("MonitorExit")) {
            name = "MonitorXxit";
        } else if (name.equals("ArrayLength")) {
            name = "ArrayGength";
        } else if (name.equals("IInc")) {
            name = "INnc";
        }
        for (int i = 0; i < name.length(); i++) {
            if (Character.isUpperCase(name.charAt(i))) {
                sb.append(name.charAt(i));
            }
        }
        return sb.toString();
    }

    private static String changeCase(String name) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < name.length(); i++) {
            if (Character.isUpperCase(name.charAt(i)) && (i > 1 && !Character.isUpperCase(name.charAt(i - 1)))) {
                sb.append('_');
            }
            sb.append(Character.toUpperCase(name.charAt(i)));
        }
        return sb.toString();
    }

}
