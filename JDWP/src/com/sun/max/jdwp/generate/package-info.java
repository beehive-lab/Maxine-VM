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

