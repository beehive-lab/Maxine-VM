/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.max.vm.ext.graal.hosted;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;

import com.oracle.graal.options.*;
import com.oracle.max.vm.ext.graal.*;
import com.sun.max.ide.*;
import com.sun.max.lang.*;
import com.sun.max.program.*;
import com.sun.max.vm.runtime.*;

/**
 * Generates the set of Maxine {@link VMOption} values from Graal {@link OptionDescriptor} values. To ensure repeatable
 * results, the values are sorted by class and field.
 *
 */
public class MaxGraalOptionsGenerator extends SourceGenerator {

    MaxGraalOptionsGenerator(PackageChecker packageChecker) {
        setPackageChecker(packageChecker);
    }

    private class OptionsClassSearch extends ClassSearch {

        final Map<String, Boolean> seenPackages = new HashMap<>();
        Set<Class< ? >> optionsSources = new HashSet<Class< ? >>();

        @Override
        protected boolean visitClass(boolean isArchiveEntry, String className) {
            if (!className.endsWith("package-info")) {
                String pkg = Classes.getPackageName(className);
                Boolean include = seenPackages.get(pkg);
                if (include == null) {
                    include = includePackage(pkg);
                    seenPackages.put(pkg,  include);
                }
                if (!include) {
                    // skip class
                    return true;
                }
                Class< ? > source = null;
                try {
                    source = Classes.forName(className, false, getClass().getClassLoader());
                } catch (Throwable ex) {
                    // Ignore
                    System.err.println(ex);
                    System.err.println("while trying to load: " + className);
                    return true;
                }
                for (Class< ? > intf : source.getInterfaces()) {
                    if (intf == Options.class) {
                        optionsSources.add(source);
                    }
                }
            }
            return true;
        }

    }

    private static File workspace;
    private static final String GraalPrefix = "-G:";

    private static class CClass implements Comparable<CClass> {

        Class< ? > klass;

        CClass(Class< ? > klass) {
            this.klass = klass;
        }

        @Override
        public int compareTo(CClass o) {
            return klass.getName().compareTo(o.klass.getName());
        }
    }

    private static class COptionDescriptor implements Comparable<COptionDescriptor> {

        OptionDescriptor optionDescriptor;

        COptionDescriptor(OptionDescriptor optionDescriptor) {
            this.optionDescriptor = optionDescriptor;
        }

        @Override
        public int compareTo(COptionDescriptor o) {
            return optionDescriptor.getName().compareTo(o.optionDescriptor.getName());
        }
    }

    @Override
    protected void doGenerate() throws IOException {
        out.println("    static {");
        workspace = JavaProject.findWorkspace();
        OptionsClassSearch search = new OptionsClassSearch();
        search.run(Classpath.fromSystem(), "com/oracle/graal");
        // Sort
        CClass[] optionClassArray = new CClass[search.optionsSources.size()];
        int ix = 0;
        for (Class<?> klass : search.optionsSources) {
            optionClassArray[ix++] = new CClass(klass);
        }
        Arrays.sort(optionClassArray);

        out.printf("        // CheckStyle: stop line length check%n");
        // Process options
        for (CClass optionsGClass : optionClassArray) {
            Class< ? > optionsClass = optionsGClass.klass;
            try {
                Method iterMethod = optionsClass.getDeclaredMethod("iterator");
                Options options = (Options) optionsClass.newInstance();
                @SuppressWarnings("unchecked")
                Iterator<OptionDescriptor> iter = (Iterator<OptionDescriptor>) iterMethod.invoke(options);
                int numOptions = 0;
                while (iter.hasNext()) {
                    numOptions++;
                    iter.next();
                }
                COptionDescriptor[] optionDescriptorArray = new COptionDescriptor[numOptions];
                @SuppressWarnings("unchecked")
                Iterator<OptionDescriptor> iter2 = (Iterator<OptionDescriptor>) iterMethod.invoke(options);
                for (int i = 0; i < optionDescriptorArray.length; i++) {
                    optionDescriptorArray[i] = new COptionDescriptor(iter2.next());
                }
                Arrays.sort(optionDescriptorArray);

                out.printf("        // Options from %s%n", optionsClass.getName().replace("_Options", ""));
                for (int i = 0; i < optionDescriptorArray.length; i++) {
                    OptionDescriptor d = optionDescriptorArray[i].optionDescriptor;
                    String typeName = toFirstUpper(d.getType().getSimpleName());
                    String space = typeName.equals("String") ? "false, " : "";
                    String defaultValue = "";
                    boolean noDefaultValue = false;
                    String prefix = GraalPrefix + d.getName() + '=';
                    String name = ""; // encode in prefix except for Boolean
                    // Checkstyle: stop
                    switch (typeName) {
                        case "Boolean": {
                            prefix = GraalPrefix + ((Boolean) d.getOptionValue().getValue() ? '+' : '-');
                            name = "\"" + d.getName() + "\"" + ", ";
                            noDefaultValue = true;
                            break;
                        }
                        case "String": {
                            String v = (String) d.getOptionValue().getValue();
                            defaultValue = v == null ? "null" : "\"" + v + "\"";
                            break;
                        }
                        case "Integer":
                            defaultValue = ((Integer) d.getOptionValue().getValue()).toString();
                            typeName = "Int";
                            break;
                        case "Float":
                            defaultValue = ((Float) d.getOptionValue().getValue()).toString() + "f";
                            break;
                        case "Double":
                            defaultValue = ((Double) d.getOptionValue().getValue()).toString();
                            break;
                        default:
                            assert false;
                    }
                    if (!noDefaultValue) {
                        defaultValue += ", ";
                    }
                    // Checkstyle: resume
                    String className = d.getDeclaringClass().getName().replace('$', '.');
                    out.printf("        register(new VM%sOption(\"%s\", %s%s%s\"%s\"), %s, \"%s\");%n",
                                    typeName, prefix, name, space, defaultValue, d.getHelp(), className + ".class", d.getFieldName());
                }
            } catch (NoSuchMethodException | IllegalAccessException | InstantiationException | InvocationTargetException ex) {
                FatalError.unexpected("error processing " + optionsClass.getName(), ex);
            }
        }
        out.printf("        // CheckStyle: resume line length check%n");
        out.println("    }");
    }

    public static boolean check(PackageChecker packageChecker) {
        try {
            return new MaxGraalOptionsGenerator(packageChecker).generate(true, MaxGraalOptions.class);
        } catch (IOException ex) {
            FatalError.unexpected("error checking MaxGraalOptions: " + ex);
            return false;
        }
    }

    public static void main(String[] args) throws IOException {
        boolean check = args.length > 0 && args[0].equals("-check");
        if (new MaxGraalOptionsGenerator(null).generate(check, MaxGraalOptions.class)) {
            System.exit(1);
        }
    }

}
