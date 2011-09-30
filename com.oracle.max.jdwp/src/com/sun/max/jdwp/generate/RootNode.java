/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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

        for (final Iterator it = components.iterator(); it.hasNext();) {
            FileWriter fw = null;
            try {
                final Node n = (Node) it.next();

                if (n.components.size() > 0) {

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
