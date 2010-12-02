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

/**
 * @author JDK7: jdk/make/tools/src/build/tools/jdwpgen
 * @author Thomas Wuerthinger
 */
class ErrorNode extends AbstractNamedNode {

    protected static final String NAME_OF_ERROR_TABLE = "Error";

    ErrorNode() {
        this(new ArrayList<Node>());
    }

    ErrorNode(List<Node> components) {
        this.kind = "Error";
        this.components = components;
        this.lineno = 0;
    }

    @Override
    void constrain(Context ctx) {
        if (components.size() != 0) {
            error("Errors have no internal structure");
        }
        super.constrain(ctx);
    }

    @Override
    void genJava(PrintWriter writer, int depth) {
    }

    @Override
    public void genJavaRead(PrintWriter writer, int depth, String readLabel) {
    }

    @Override
    public void genJavaPreDef(PrintWriter writer, int depth) {
    }
}
