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

    public XirSnippet genEntrypoint(XirSite site) {
        return null;
    }

    public XirSnippet genSafepoint(XirSite site) {
        return null;
    }

    public XirSnippet genResolveClassObject(XirSite site, RiType type) {
        return null;
    }

    public XirSnippet genIntrinsic(XirSite site, XirArgument[] arguments, RiMethod method) {
        return null;
    }

    public XirSnippet genInvokeInterface(XirSite site, XirArgument receiver, RiMethod method) {
        return null;
    }

    public XirSnippet genInvokeVirtual(XirSite site, XirArgument receiver, RiMethod method) {
        return null;
    }

    public XirSnippet genInvokeSpecial(XirSite site, XirArgument receiver, RiMethod method) {
        return null;
    }

    public XirSnippet genInvokeStatic(XirSite site, RiMethod method) {
        return null;
    }

    public XirSnippet genMonitorEnter(XirSite site, XirArgument receiver) {
        return null;
    }

    public XirSnippet genMonitorExit(XirSite site, XirArgument receiver) {
        return null;
    }

    public XirSnippet genGetField(XirSite site, XirArgument receiver, RiField field) {
        return null;
    }

    public XirSnippet genPutField(XirSite site, XirArgument receiver, RiField field, XirArgument value) {
        return null;
    }

    public XirSnippet genGetStatic(XirSite site, XirArgument staticTuple, RiField field) {
        return null;
    }

    public XirSnippet genPutStatic(XirSite site, XirArgument staticTuple, RiField field, XirArgument value) {
        return null;
    }

    public XirSnippet genNewInstance(XirSite site, RiType type) {
        return null;
    }

    public XirSnippet genNewArray(XirSite site, XirArgument length, CiKind elementKind, RiType componentType, RiType arrayType) {
        return null;
    }

    public XirSnippet genNewMultiArray(XirSite site, XirArgument[] lengths, RiType type) {
        return null;
    }

    public XirSnippet genCheckCast(XirSite site, XirArgument receiver, XirArgument hub, RiType type) {
        return null;
    }

    public XirSnippet genInstanceOf(XirSite site, XirArgument receiver, XirArgument hub, RiType type) {
        return null;
    }

    public XirSnippet genArrayLoad(XirSite site, XirArgument array, XirArgument index, XirArgument length, CiKind elementKind, RiType elementType) {
        return null;
    }

    public XirSnippet genArrayStore(XirSite site, XirArgument array, XirArgument index, XirArgument length, XirArgument value, CiKind elementKind, RiType elementType) {
        return null;
    }

    public XirSnippet genArrayLength(XirSite site, XirArgument array) {
        return null;
    }

	public List<XirTemplate> buildTemplates(CiXirAssembler asm) {
		return null;
	}
}
