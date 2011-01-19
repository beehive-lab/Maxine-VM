/*
 * Copyright (c) 2009, 2010, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
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
public abstract interface RiXirGenerator {

    public XirSnippet genPrologue(XirSite site, RiMethod method);

    public XirSnippet genEpilogue(XirSite site, RiMethod method);

    public XirSnippet genSafepoint(XirSite site);

    public XirSnippet genExceptionObject(XirSite site);

    public XirSnippet genResolveClass(XirSite site, RiType type, Representation representation);

    public XirSnippet genIntrinsic(XirSite site, XirArgument[] arguments, RiMethod method);

    public XirSnippet genInvokeInterface(XirSite site, XirArgument receiver, RiMethod method);

    public XirSnippet genInvokeVirtual(XirSite site, XirArgument receiver, RiMethod method);

    public XirSnippet genInvokeSpecial(XirSite site, XirArgument receiver, RiMethod method);

    public XirSnippet genInvokeStatic(XirSite site, RiMethod method);

    public XirSnippet genMonitorEnter(XirSite site, XirArgument receiver, XirArgument lockAddress);

    public XirSnippet genMonitorExit(XirSite site, XirArgument receiver, XirArgument lockAddress);

    public XirSnippet genGetField(XirSite site, XirArgument receiver, RiField field);

    public XirSnippet genPutField(XirSite site, XirArgument receiver, RiField field, XirArgument value);

    public XirSnippet genGetStatic(XirSite site, XirArgument staticTuple, RiField field);

    public XirSnippet genPutStatic(XirSite site, XirArgument staticTuple, RiField field, XirArgument value);

    public XirSnippet genNewInstance(XirSite site, RiType type);

    public XirSnippet genNewArray(XirSite site, XirArgument length, CiKind elementKind, RiType componentType, RiType arrayType);

    public XirSnippet genNewMultiArray(XirSite site, XirArgument[] lengths, RiType type);

    public XirSnippet genCheckCast(XirSite site, XirArgument receiver, XirArgument hub, RiType type);

    public XirSnippet genInstanceOf(XirSite site, XirArgument receiver, XirArgument hub, RiType type);

    public XirSnippet genArrayLoad(XirSite site, XirArgument array, XirArgument index, XirArgument length, CiKind elementKind, RiType elementType);

    public XirSnippet genArrayStore(XirSite site, XirArgument array, XirArgument index, XirArgument length, XirArgument value, CiKind elementKind, RiType elementType);

    public XirSnippet genArrayLength(XirSite site, XirArgument array);

    public XirSnippet genWriteBarrier(XirArgument object);
    
    public XirSnippet genArrayCopy(XirSite site, XirArgument src, XirArgument srcPos, XirArgument dest, XirArgument destPos, XirArgument length, RiType elementType, boolean inputsSame, boolean inputsDifferent);

    public XirSnippet genCurrentThread(XirSite site);
    
    /**
     * Construct the list of XIR templates using the given XIR assembler.
     * 
     * @param asm the XIR assembler
     * @return the list of templates
     */
    public List<XirTemplate> buildTemplates(CiXirAssembler asm);

}
