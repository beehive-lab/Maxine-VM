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
/*VCSID=2505c2e2-d2e0-464f-916e-bce12e0db33a*/

package com.sun.max.jdwp.generate;

import java.io.*;
import java.util.*;

/**
 * @author JDK7: jdk/make/tools/src/build/tools/jdwpgen
 * @author Thomas Wuerthinger
 */
class ConstantSetNode extends AbstractNamedNode {

    /**
     * The mapping between a constant and its value.
     */
    protected static Map<String, String> _constantMap;

    ConstantSetNode() {
        if (_constantMap == null) {
            _constantMap = new HashMap<String, String>();
        }
    }

    @Override
    void constrainComponent(Context ctx, Node node) {
        if (node instanceof ConstantNode) {
            node.constrain(ctx);
            _constantMap.put(name() + "_" + ((ConstantNode) node).getName(), node.comment());
        } else {
            error("Expected 'Constant', got: " + node);
        }
    }

    @Override
    public String javaType() {
        return name();
    }

    @Override
    void genJavaClassSpecifics(PrintWriter writer, int depth) {
    }

    @Override
    void genJava(PrintWriter writer, int depth) {
        genJavaClass(writer, depth);
    }

    public static String getConstant(String key) {
        if (_constantMap == null) {
            return "";
        }
        final String com = _constantMap.get(key);
        if (com == null) {
            return "";
        }
        return com;
    }

}
