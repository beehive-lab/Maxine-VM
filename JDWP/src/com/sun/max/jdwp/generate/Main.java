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

package com.sun.max.jdwp.generate;

import java.io.*;
import java.util.logging.*;

/**
 * @author JDK7: jdk/make/tools/src/build/tools/jdwpgen
 * @author Thomas Wuerthinger
 */
class Main {

    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());

    private static final String PROTOCOL_DIR = "./src/com/sun/max/jdwp/protocol";
    private static final String PROTOCOL_PACKAGE = "com.sun.max.jdwp.protocol";
    private static final String HEADER_FILE = "./src/com/sun/max/jdwp/generate/header.txt";
    private static final String CONSTANTS_DIR = "./src/com/sun/max/jdwp/constants";
    private static final String CONSTANTS_PACKAGE = "com.sun.max.jdwp.constants";
    private static final String SPECIFICATION_FILE = "./src/com/sun/max/jdwp/generate/jdwp.spec";
    private static final String DATA_PACKAGE = "com.sun.max.jdwp.data";

    public static void main(String[] args) throws IOException {

        final Parse parse = new Parse(new FileReader(SPECIFICATION_FILE));
        final RootNode root = parse.items();
        root.parentAndExtractComments();
        root.prune();
        root.constrain(new Context());
        root.genJava(PROTOCOL_DIR, CONSTANTS_DIR, PROTOCOL_PACKAGE, CONSTANTS_PACKAGE, DATA_PACKAGE, new FileReader(HEADER_FILE), 0);

        LOGGER.info("Finished generating files");
    }
}
