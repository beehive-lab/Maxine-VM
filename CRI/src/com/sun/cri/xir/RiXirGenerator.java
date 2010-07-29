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

import java.util.*;

import com.sun.cri.ci.*;
import com.sun.cri.ri.*;
import com.sun.cri.ri.RiType.*;

/**
 * Represents the interface through which the compiler requests the XIR for a given bytecode from the runtime system.
 *
 * @author Thomas Wuerthinger
 * @author Ben L. Titzer
 */
public abstract class RiXirGenerator {

    public abstract XirSnippet genEntrypoint(XirSite site);

    public abstract XirSnippet genSafepoint(XirSite site);
    
    public abstract XirSnippet genExceptionObject(XirSite site);

    public abstract XirSnippet genResolveClass(XirSite site, RiType type, Representation representation);

    public abstract XirSnippet genIntrinsic(XirSite site, XirArgument[] arguments, RiMethod method);

    public abstract XirSnippet genInvokeInterface(XirSite site, XirArgument receiver, RiMethod method);

    public abstract XirSnippet genInvokeVirtual(XirSite site, XirArgument receiver, RiMethod method);

    public abstract XirSnippet genInvokeSpecial(XirSite site, XirArgument receiver, RiMethod method);

    public abstract XirSnippet genInvokeStatic(XirSite site, RiMethod method);

    public abstract XirSnippet genMonitorEnter(XirSite site, XirArgument receiver);

    public abstract XirSnippet genMonitorExit(XirSite site, XirArgument receiver);

    public abstract XirSnippet genGetField(XirSite site, XirArgument receiver, RiField field);

    public abstract XirSnippet genPutField(XirSite site, XirArgument receiver, RiField field, XirArgument value);

    public abstract XirSnippet genGetStatic(XirSite site, XirArgument staticTuple, RiField field);

    public abstract XirSnippet genPutStatic(XirSite site, XirArgument staticTuple, RiField field, XirArgument value);

    public abstract XirSnippet genNewInstance(XirSite site, RiType type);

    public abstract XirSnippet genNewArray(XirSite site, XirArgument length, CiKind elementKind, RiType componentType, RiType arrayType);

    public abstract XirSnippet genNewMultiArray(XirSite site, XirArgument[] lengths, RiType type);

    public abstract XirSnippet genCheckCast(XirSite site, XirArgument receiver, XirArgument hub, RiType type);

    public abstract XirSnippet genInstanceOf(XirSite site, XirArgument receiver, XirArgument hub, RiType type);

    public abstract XirSnippet genArrayLoad(XirSite site, XirArgument array, XirArgument index, XirArgument length, CiKind elementKind, RiType elementType);

    public abstract XirSnippet genArrayStore(XirSite site, XirArgument array, XirArgument index, XirArgument length, XirArgument value, CiKind elementKind, RiType elementType);

    public abstract XirSnippet genArrayLength(XirSite site, XirArgument array);

    /**
     * Construct the list of XIR templates using the given XIR assembler.
     * @param asm the XIR assembler
     * @return the list of templates
     */
    public List<XirTemplate> buildTemplates(CiXirAssembler asm) {
        return null;
    }
}
