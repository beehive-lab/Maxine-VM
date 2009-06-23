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

/**
 * @author Thomas Wuerthinger
 */
public class SimpleIDTypeNode extends SimpleTypeNode {

    private String typeName;

    private static String getDocType(String name) {
        assert name.length() > 0;
        return Character.toLowerCase(name.charAt(0)) + name.substring(1);
    }

    private static String getJavaType(String name) {
        return "ID." + name;
    }

    private static String getJavaRead(String name) {
        return "ID.read(ps.getInputStream(), " + getJavaType(name) + ".class)";
    }

    public SimpleIDTypeNode(String name) {
        super(getDocType(name), getJavaType(name), getJavaRead(name));
        typeName = name;
    }

    @Override
    public void genJavaWrite(PrintWriter writer, int depth, String writeLabel) {
        indent(writer, depth);
        writer.println(writeLabel + ".write(ps.getOutputStream());");
    }

    @Override
    public void genJavaToString(PrintWriter writer, int depth, String writeLabel) {
        indent(writer, depth);
        writer.println("stringBuilder.append(\"" + writeLabel + "=\" + " + writeLabel + ");");
    }

    @Override
    public Node copy() {
        return new SimpleIDTypeNode(typeName);
    }
}
