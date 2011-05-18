/*
 * Copyright (c) 2007, 2008, Oracle and/or its affiliates. All rights reserved.
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
/**
 * @author JDK7: jdk/make/tools/src/build/tools/jdwpgen
 * @author Thomas Wuerthinger
 *
 * This package contains a source code modification of the jdwpgen module of JDK 7. The original source code was used
 * to build three different outputs from the JDWP specification file jdwp.spec:
 * - HTML documentation
 * - Java-level protocol access for JDI client
 * - C header files for Hotspot's C JDWP server
 *
 * The new version created by source code modification generates the package com.sun.jdwp.protocl which gives Java-level
 * protocol access for the Maxine Java JDWP server. The code was simplified and modified to meet Maxine coding conventions.
 * The generated code is also very different as it makes for example heavy use of generics to simplify handler implementations.
 *
 * In addition to the source code there are two important files in this directory:
 * - header.txt: This file is inserted at the beginning of every generated Java source file.
 * - jdwp.spec: Contains the specification of the JDWP 1.5 protocol. The specification is slightly modified to meet Maxine specific requirements.
 *
 * The structure of the generated Java files is as follows:
 * For each command set and for each constant set a separate Java file with a single public Java class is generated. The class for a constant set contains
 * a static field for each constant. The class for a command set contains an inner class for each JDWP command. For each command at least three different inner classes
 * are generated:
 * - IncomingRequest: Contains the data fields for an incoming packet of that command.
 * - Reply: Contains the data fields for an outgoing packet of that command.
 * - Handler: Abstract class that a handler of this command needs to extend.
 * Some commands may contain additional inner classes that are used to describe parts of the incoming or outgoing data packets.
 *
 * The source code for two packages is generated:
 * - {@link com.sun.max.jdwp.constants} contains int constants of the JDWP protocol.
 * - {@link com.sun.max.jdwp.protocol} contains definition for the incoming and outgoing data of the JDWP commands. This package has a dependency on the {@link com.sun.max.jdwp.data} package.
 */
package com.sun.max.jdwp.generate;

