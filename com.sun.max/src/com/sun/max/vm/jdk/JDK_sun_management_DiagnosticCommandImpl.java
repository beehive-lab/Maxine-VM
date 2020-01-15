/*
 * Copyright (c) 2019, APT Group, School of Computer Science,
 * The University of Manchester. All rights reserved.
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
 */
package com.sun.max.vm.jdk;

import com.sun.max.annotate.*;
import com.sun.max.program.*;

/**
 * Method substitutions for sun.management.DiagnosticCommandImpl.
 */
@METHOD_SUBSTITUTIONS(className = "sun.management.DiagnosticCommandImpl")
final class JDK_sun_management_DiagnosticCommandImpl {

    @SUBSTITUTE(optional = true) // Not available in JDK 7
    private void setNotificationEnabled(boolean enabled) {
        // Do nothing
        ProgramWarning.message("setNotificationEnabled not implemented");
    }

    @SUBSTITUTE(optional = true) // Not available in JDK 7
    private String[] getDiagnosticCommands() {
        ProgramWarning.message("getDiagnosticCommands not implemented");
        return new String[0];
    }

    @SUBSTITUTE(signatureDescriptor = "([Ljava/lang/String;)[Lsun/management/DiagnosticCommandInfo;", optional = true) // Not available in JDK 7
    private Object[] getDiagnosticCommandInfo(String[] commands) {
        ProgramWarning.message("getDiagnosticCommandInfo not implemented");
        return null;
    }

    @SUBSTITUTE(optional = true) // Not available in JDK 7
    private String executeDiagnosticCommand(String command) {
        ProgramWarning.message("executeDiagnosticCommand not implemented");
        return "";
    }

}
