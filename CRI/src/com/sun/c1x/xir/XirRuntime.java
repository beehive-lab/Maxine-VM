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

import com.sun.c1x.ri.RiConstantPool;
import com.sun.c1x.ri.RiField;
import com.sun.c1x.ri.RiMethod;
import com.sun.c1x.ri.RiType;

/**
 * @author Thomas Wuerthinger
 * @author Ben L. Titzer
 */
public abstract class XirRuntime {

    public XirSnippet doResolveClassObject(RiType type) {
        return null;
    }

    public XirSnippet doInvokeInterface(XirArgument receiver, RiMethod method) {
        return null;
    }

    public XirSnippet doIntrinsic(XirArgument[] arguments, RiMethod method) {
        return null;
    }

    public XirSnippet doInvokeVirtual(XirArgument receiver, RiMethod method) {
        return null;
    }

    public XirSnippet doInvokeSpecial(XirArgument receiver, RiMethod method) {
        return null;
    }

    public XirSnippet doInvokeStatic(RiMethod method) {
        return null;
    }

    public XirSnippet doMonitorEnter(XirArgument receiver) {
        return null;
    }

    public XirSnippet doMonitorExit(XirArgument receiver) {
        return null;
    }

    public XirSnippet doGetField(XirArgument receiver, RiField field, char cpi, RiConstantPool constantPool) {
        return null;
    }

    public XirSnippet doPutField(XirArgument receiver, XirArgument value, RiField field, char cpi, RiConstantPool constantPool) {
        return null;
    }

    public XirSnippet doGetStatic(RiField field) {
        return null;
    }

    public XirSnippet doPutStatic(XirArgument value, RiField field) {
        return null;
    }

    public XirSnippet doNewInstance(RiType type) {
        return null;
    }

    public XirSnippet doNewArray(XirArgument length, RiType elementType) {
        return null;
    }

    public XirSnippet doNewMultiArray(XirArgument[] lengths, RiType type) {
        return null;
    }

    public XirSnippet doCheckCast(XirArgument receiver, RiType type) {
        return null;
    }

    public XirSnippet doInstanceOf(XirArgument receiver, RiType type) {
        return null;
    }

    public XirSnippet doArrayLoad(XirArgument array, XirArgument index, XirArgument length, RiType elementType) {
        return null;
    }

    public XirSnippet doArrayStore(XirArgument array, XirArgument index, XirArgument length, XirArgument value, RiType elementType) {
        return null;
    }
}
