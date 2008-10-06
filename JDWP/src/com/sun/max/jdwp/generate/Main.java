/*
 * Copyright (c) 2007 Sun Microsystems, Inc.  All rights reserved.
 *
 * Sun Microsystems, Inc. has intellectual property rights relating to technology embodied in the product
 * that is described in this document. In particular, and without limitation, these intellectual property
 * rights may include one or more of the U.S. patents listed at http://www.sun.com/patents and one or
 * more additional patents or pending patent applications in the U.S. and in other countries.
 *
 * U.S. Government Rights - Commercial software. Government users are subject to the Sun
 * Microsystems, Inc. standard license agreement and applicable provisions of the FAR and its
 * supplements.
 *
 * Use is subject to license terms. Sun, Sun Microsystems, the Sun logo, Java and Solaris are trademarks or
 * registered trademarks of Sun Microsystems, Inc. in the U.S. and other countries. All SPARC trademarks
 * are used under license and are trademarks or registered trademarks of SPARC International, Inc. in the
 * U.S. and other countries.
 *
 * UNIX is a registered trademark in the U.S. and other countries, exclusively licensed through X/Open
 * Company, Ltd.
 */
/*VCSID=c5097222-b2ab-4bb0-a3fc-71ad718ddd19*/

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
