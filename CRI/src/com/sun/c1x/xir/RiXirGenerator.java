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

import java.util.List;

import com.sun.c1x.ri.RiField;
import com.sun.c1x.ri.RiMethod;
import com.sun.c1x.ri.RiType;
import com.sun.c1x.ci.CiKind;

/**
 * This class represents the interface through which the compiler requests the XIR for a given
 * bytecode from the runtime system.
 *
 * @author Thomas Wuerthinger
 * @author Ben L. Titzer
 */
public abstract class RiXirGenerator {

    public XirSnippet genResolveClassObject(RiType type) {
        return null;
    }

    public XirSnippet genIntrinsic(XirArgument[] arguments, RiMethod method) {
        return null;
    }

    public XirSnippet genInvokeInterface(XirArgument receiver, RiMethod method) {
        return null;
    }

    public XirSnippet genInvokeVirtual(XirArgument receiver, RiMethod method) {
        return null;
    }

    public XirSnippet genInvokeSpecial(XirArgument receiver, RiMethod method) {
        return null;
    }

    public XirSnippet genInvokeStatic(RiMethod method) {
        return null;
    }

    public XirSnippet genMonitorEnter(XirArgument receiver) {
        return null;
    }

    public XirSnippet genMonitorExit(XirArgument receiver) {
        return null;
    }

    public XirSnippet genGetField(XirArgument receiver, RiField field) {
        return null;
    }

    public XirSnippet genPutField(XirArgument receiver, RiField field, XirArgument value) {
        return null;
    }

    public XirSnippet genGetStatic(RiField field) {
        return null;
    }

    public XirSnippet genPutStatic(XirArgument value, RiField field) {
        return null;
    }

    public XirSnippet genNewInstance(RiType type) {
        return null;
    }

    public XirSnippet genNewArray(XirArgument length, CiKind elementKind, RiType componentType, RiType arrayType) {
        return null;
    }

    public XirSnippet genNewMultiArray(XirArgument[] lengths, RiType type) {
        return null;
    }

    public XirSnippet genCheckCast(XirArgument receiver, RiType type) {
        return null;
    }

    public XirSnippet genInstanceOf(XirArgument receiver, RiType type) {
        return null;
    }

    public XirSnippet genArrayLoad(XirArgument array, XirArgument index, XirArgument length, CiKind elementKind, RiType elementType) {
        return null;
    }

    public XirSnippet genArrayStore(XirArgument array, XirArgument index, XirArgument length, XirArgument value, CiKind elementKind, RiType elementType) {
        return null;
    }

    public XirSnippet genArrayLength(XirArgument array) {
        return null;
    }

	public List<XirTemplate> buildTemplates(CiXirAssembler asm) {
		return null;
	}
}
