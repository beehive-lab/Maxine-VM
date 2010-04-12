/*
 * Copyright (c) 2009 Sun Microsystems, Inc.  All rights reserved.
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
package com.sun.cri.xir;

import com.sun.cri.ci.*;
import com.sun.cri.xir.CiXirAssembler.*;

/**
 * This class represents a {@link XirTemplate template of XIR} along with the
 * {@link XirArgument arguments} to be passed to the template. The runtime generates
 * such snippets for each bytecode being compiled at the request of the compiler,
 * and the compiler can generate machine code for the XIR snippet.
 *
 * @author Thomas Wuerthinger
 * @author Ben L. Titzer
 */
public class XirSnippet {

    public XirSnippet(XirTemplate template, XirArgument... inputs) {
        this.template = template;
        this.arguments = inputs;
        assert assertArgumentsCorrect();
    }

    private boolean assertArgumentsCorrect() {
        assert arguments.length == template.parameters.length;
        for (int i = 0; i < arguments.length; i++) {
            assert assertArgumentCorrect(template.parameters[i], arguments[i]);

        }
        return true;
    }

    private boolean assertArgumentCorrect(XirParameter param, XirArgument arg) {

        if (param.kind == CiKind.Illegal || param.kind == CiKind.Void) {
            assert arg == null;
        } else {
            assert arg != null;
            if (arg.constant != null) {
                assert arg.constant.kind == param.kind;
            }
        }

        return true;
    }

    public final XirArgument[] arguments;
    public final XirTemplate template;

    @Override
    public String toString() {

    	StringBuffer sb = new StringBuffer();

    	sb.append(template.toString());
    	sb.append("(");
    	for (XirArgument a : arguments) {
    		sb.append(" ");
    		sb.append(a.toString());
    	}

    	sb.append(" )");

    	return sb.toString();
    }
}
