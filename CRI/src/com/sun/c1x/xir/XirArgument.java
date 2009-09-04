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
package com.sun.c1x.xir;

import com.sun.c1x.ci.CiConstant;
import com.sun.c1x.ci.CiRuntimeCall;

/**
 * @author Thomas Wuerthinger
 * @author Ben L. Titzer
 */
public class XirArgument {

    public final CiConstant constant;
    public final Object object;
    public final CiRuntimeCall runtimeCall;
    public final XirArgument[] arguments;

    private XirArgument(CiRuntimeCall runtimeCall, XirArgument... arguments) {
        this.constant = null;
        this.object = null;
        this.runtimeCall = runtimeCall;
        this.arguments = arguments;
    }

    private XirArgument(CiConstant value) {
        this.constant = value;
        this.object = null;
        this.runtimeCall = null;
        this.arguments = null;
    }

    private XirArgument(Object o) {
        this.constant = null;
        this.object = o;
        this.runtimeCall = null;
        this.arguments = null;
    }

    public static XirArgument forInternalObject(Object o) {
        return new XirArgument(o);
    }

    public static XirArgument forInt(int x) {
        return new XirArgument(CiConstant.forInt(x));
    }

    public static XirArgument forWord(long x) {
        return new XirArgument(CiConstant.forWord(x));
    }

    public static XirArgument forObject(Object o) {
        return new XirArgument(CiConstant.forObject(o));
    }

    public static XirArgument forRuntimeCall(CiRuntimeCall runtimeCall, XirArgument... arguments) {
        return new XirArgument(runtimeCall, arguments);
    }
}
