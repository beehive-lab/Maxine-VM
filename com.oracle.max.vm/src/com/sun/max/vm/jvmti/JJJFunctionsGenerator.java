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
package com.sun.max.vm.jvmti;

import static com.sun.max.vm.jni.JniFunctionsGenerator.Customizer;
import com.sun.max.annotate.*;
import com.sun.max.vm.jni.*;

@HOSTED_ONLY
public class JJJFunctionsGenerator {

    private static final String PHASES = "// PHASES: ";
    private static final String NULLCHECK = "// NULLCHECK: ";

    public static class JvmtiCustomizer extends Customizer {
        @Override
        public String customize(String line) {
            String result = customizePhases(line);
            if (result != null) {
                return result;
            }
            result = customizeNullCheck(line);
            if (result != null) {
                return result;
            }
            return line;
        }

        /**
         * Check for a PHASES comment. If found generate the check against the currebnt phase.
         * @param line
         * @return null for no change or the string to replace the line
         */
        private String customizePhases(String line) {
            int index = line.indexOf(PHASES);
            if (index < 0) {
                return null;
            }
            String phaseList = line.substring(index + PHASES.length());
            if (phaseList.equals("ANY")) {
                return null;
            }
            String[] phases = phaseList.split(",");
            StringBuilder sb = new StringBuilder("        ");
            sb.append("if (!(");
            for (int i = 0; i < phases.length; i++) {
                if (i > 0) {
                    sb.append(" || ");
                }
                sb.append("phase == JVMTI_PHASE_");
                sb.append(phases[i]);
            }
            sb.append(")) {\n");
            sb.append("                return JVMTI_ERROR_WRONG_PHASE;\n");
            sb.append("            }");
            return sb.toString();
        }

        private String customizeNullCheck(String line) {
            int index = line.indexOf(NULLCHECK);
            if (index < 0) {
                return null;
            }
            String nullCheckList = line.substring(index + NULLCHECK.length());
            String[] nullCheck = nullCheckList.split(",");
            StringBuilder sb = new StringBuilder("        ");
            sb.append("if (");
            for (int i = 0; i < nullCheck.length; i++) {
                if (i > 0) {
                    sb.append(" || ");
                }
                sb.append(nullCheck[i]);
                sb.append(".isZero()");
            }
            sb.append(") {\n");
            sb.append("                return JVMTI_ERROR_NULL_POINTER;\n");
            sb.append("            }");
            return sb.toString();
        }
    }

    public static void main(String[] args) throws Exception {
        boolean updated = false;
        if (JniFunctionsGenerator.generate(false, JJJFunctionsSource.class, JJJFunctions.class, new JvmtiCustomizer())) {
            System.out.println("Source for " + JJJFunctions.class + " was updated");
            updated = true;
        }
        if (updated) {
            System.exit(1);
        }

    }

}
