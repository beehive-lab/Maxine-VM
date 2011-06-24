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

import static com.sun.max.vm.t1x.T1XTemplateGenerator.*;
import static com.oracle.max.vma.tools.gen.vma.AdviceGeneratorHelper.*;

import java.lang.reflect.*;
import java.util.*;
import java.util.Map.Entry;

import com.oracle.max.vm.ext.vma.*;


public class TransientVMAdviceHandlerTypesGenerator {

    private static final String ADVISE_BEFORE = "adviseBefore";
    private static final String ADVISE_AFTER = "adviseAfter";
    private static final String ADVICE_RECORD = "AdviceRecord";
    private static boolean first = true;
    public static SortedMap<String, Method> enumtoMethod = new TreeMap<String, Method>();
    public static Map<Method, String> methodToEnum = new HashMap<Method, String>();
    public static Map<String, String> enumToRecordName = new HashMap<String, String>();
    public static Map<String, ArrayList<String>> recordToEnumList = new HashMap<String, ArrayList<String>>();

    public static void main(String[] args) {
        createGenerator(TransientVMAdviceHandlerTypesGenerator.class);
        generateAutoComment();
        for (Method m : VMAdviceHandler.class.getMethods()) {
            if (m.getName().startsWith("advise")) {
                generateEnum(m);
                first = false;
            }
        }
        out.printf(";%n%n");
        generateNewAdviceRecord();
    }

    public static void createEnumMaps() {
        for (Method m : VMAdviceHandler.class.getMethods()) {
            if (m.getName().startsWith("advise")) {
                createEnum(m);
            }
        }
    }

    private static String createEnum(Method m) {
        String name = removeAdvise(m.getName());
        String lastParam = getLastParameterName(m);
        if (name.equals("ConstLoad") || name.endsWith("Store") || name.endsWith("Conversion") ||
                        name.contains("Return") ||  name.contains("PutField") ||
                        name.contains("PutStatic") || name.contains("Operation")) {
            if (lastParam != null) {
                name += toFirstUpper(lastParam);
            }
        } else if (name.contains("GetField") || name.contains("GetStatic")) {
            if (m.getParameterTypes().length > 2) {
                name += toFirstUpper(lastParam);
            }
        } else if (name.endsWith("If")) {
            name += toFirstUpper(lastParam);
        }
        methodToEnum.put(m, name);
        if (enumtoMethod.get(name) != null) {
            return null;
        }
        enumtoMethod.put(name, m);
        return name;
    }

    private static void generateEnum(Method m) {
        String name = createEnum(m);
        if (name == null) {
            return;
        }
        if (!first) {
            out.printf(",%n");
        }
        out.printf("        %s", name);
    }

    public static void generateRecordToEnumList() {
        for (Entry<String, Method> e : enumtoMethod.entrySet()) {
            String name = e.getKey();
            Method m = e.getValue();
            String lastParam = getLastParameterName(m);
            String uLastParam = lastParam == null ? "" : toFirstUpper(lastParam);

            String adviceRecordName = "";
            if (name.contains("ConstLoad") || name.startsWith("Store") || name.contains("Conversion") ||
                            name.contains("Return")) {
                adviceRecordName = uLastParam;
            } else if (name.contains("GetField") || name.contains("GetStatic")) {
                adviceRecordName = "Object";
            } else if (name.contains("PutField") || name.contains("PutStatic") ||
                            name.startsWith("ArrayStore")) {
                adviceRecordName = "Object" + uLastParam;
            } else if (name.contains("Operation")) {
                adviceRecordName = uLastParam + uLastParam;
            } else if (name.equals("IfInt") || name.equals("IInc")) {
                adviceRecordName = "LongLong";
            } else if (name.equals("IfObject") || name.equals("CheckCast") || name.equals("InstanceOf") ||
                            name.endsWith("MultiNewArray")) {
                adviceRecordName = "ObjectObject";
            } else if (name.endsWith("ArrayLength") || name.endsWith("Throw") ||
                            name.contains("Monitor") || name.contains("New") || name.startsWith("ArrayLoad")) {
                adviceRecordName = "Object";
            } else if (name.contains("Invoke")) {
                adviceRecordName = "ObjectMethod";
            }
            adviceRecordName += ADVICE_RECORD;
            ArrayList<String> list = recordToEnumList.get(adviceRecordName);
            if (list == null) {
                list = new ArrayList<String>();
                recordToEnumList.put(adviceRecordName, list);
            }
            list.add(name);
            enumToRecordName.put(name, adviceRecordName);
        }
    }

    private static void generateNewAdviceRecord() {
        generateRecordToEnumList();
        out.printf("        public AdviceRecord newAdviceRecord() {%n");
        out.printf("            switch (this) {%n");
        for (Map.Entry<String, ArrayList<String>> entry : recordToEnumList.entrySet()) {
            for (String record : entry.getValue()) {
                out.printf("                case %s:%n", record);
            }
            out.printf("                    return new %s();%n", entry.getKey());
        }
    }

    private static String removeAdvise(String name) {
        int ix = name.indexOf(ADVISE_BEFORE);
        if (ix >= 0) {
            return name.substring(ADVISE_BEFORE.length());
        }
        ix = name.indexOf(ADVISE_AFTER);
        if (ix >= 0) {
            return name.substring(ADVISE_AFTER.length());
        }
        return name;
    }
}
