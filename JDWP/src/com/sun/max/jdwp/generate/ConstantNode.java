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
/*VCSID=27cf6886-9567-4c5c-aa24-7ef204de5c72*/

package com.sun.max.jdwp.generate;

import java.io.*;
import java.util.*;

/**
 * @author JDK7: jdk/make/tools/src/build/tools/jdwpgen
 * @author Thomas Wuerthinger
 */
class ConstantNode extends AbstractNamedNode {

    ConstantNode() {
        this(new ArrayList<Node>());
    }

    ConstantNode(List<Node> components) {
        this._kind = "Constant";
        this._components = components;
        this._lineno = 0;
    }

    @Override
    void constrain(Context ctx) {
        if (_components.size() != 0) {
            error("Constants have no internal structure");
        }
        super.constrain(ctx);
    }

    @Override
    void genJava(PrintWriter writer, int depth) {
        indent(writer, depth);
        writer.println("public static final int " + name() + " = " + nameNode().value() + ";");
    }

    public String getName() {

        if (name() == null || name().length() == 0) {
            prune();
        }
        return name();
    }
}
