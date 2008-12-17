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

package com.sun.max.jdwp.generate;

import java.io.*;
import java.util.*;
import java.util.logging.*;

/**
 * @author JDK7: jdk/make/tools/src/build/tools/jdwpgen
 * @author Thomas Wuerthinger
 */
class RootNode extends AbstractNamedNode {

    private static final Logger LOGGER = Logger.getLogger(RootNode.class.getName());

    @Override
    void constrainComponent(Context ctx, Node node) {
        if (node instanceof CommandSetNode || node instanceof ConstantSetNode) {
            node.constrain(ctx);
        } else {
            error("Expected 'CommandSet' item, got: " + node);
        }
    }

    private void printHeader(Writer writer, Reader headerReader) throws IOException {
        final BufferedReader bufferedHeaderReader = new BufferedReader(headerReader);
        String curLine = bufferedHeaderReader.readLine();
        final PrintWriter out = new PrintWriter(writer);
        while (curLine != null) {
            out.println(curLine);
            curLine = bufferedHeaderReader.readLine();
        }
        out.flush();
    }

    void genJava(String protocolDirName, String constantsDirName, String protocolPackage, String constantsPackage, String dataPackage, Reader headerReader, int depth) {

        final StringWriter headerString = new StringWriter();
        try {
            printHeader(headerString, headerReader);
        } catch (IOException e) {
            LOGGER.severe("Exception while reading header: " + e);
        }

        for (final Iterator it = _components.iterator(); it.hasNext();) {
            FileWriter fw = null;
            try {
                final Node n = (Node) it.next();

                if (n._components.size() > 0) {

                    String dirName = protocolDirName;
                    if (n instanceof ConstantSetNode) {
                        dirName = constantsDirName;
                    }
                    fw = new FileWriter(dirName + "/" + n.javaType() + ".java");
                    final PrintWriter writer = new PrintWriter(fw);

                    writer.println(headerString.toString());
                    printHeader(writer, headerReader);

                    if (n instanceof CommandSetNode) {
                        writer.println("package " + protocolPackage + ";");
                        writer.println("import " + dataPackage + ".*;");
                        writer.println("import " + constantsPackage + ".*;");
                        writer.println("@SuppressWarnings(\"unused\")");
                    } else {
                        assert n instanceof ConstantSetNode;
                        writer.println("package " + constantsPackage + ";");
                    }

                    n.genJava(writer, depth);
                }
            } catch (IOException ex) {
                LOGGER.log(Level.SEVERE, null, ex);
            } finally {
                try {
                    if (fw != null) {
                        fw.close();
                    }
                } catch (IOException ex) {
                    LOGGER.log(Level.SEVERE, null, ex);
                }
            }
        }
    }
}
