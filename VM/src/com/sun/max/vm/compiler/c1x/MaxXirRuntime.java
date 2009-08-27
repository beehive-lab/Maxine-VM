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
package com.sun.max.vm.compiler.c1x;

import com.sun.c1x.ci.*;
import com.sun.c1x.ri.*;
import com.sun.c1x.xir.*;
import com.sun.c1x.xir.XirAssembler.*;


/**
 * @author Thomas Wuerthinger
 * @author Ben L. Titzer
 */
public class MaxXirRuntime extends XirRuntime {

    private XirTemplate[] putFieldTemplate;

    public MaxXirRuntime() {

        putFieldTemplate = new XirTemplate[CiKind.values().length];
        for (CiKind kind : CiKind.values()) {
            putFieldTemplate[kind.ordinal()] = buildPutField(kind);
        }
    }

    @Override
    public XirSnippet doPutField(XirArgument receiver, XirArgument value, RiField field, char cpi, RiConstantPool constantPool) {

        XirArgument offset = null;
        if (field.isLoaded()) {
            offset = XirArgument.forInt(field.offset());
        } else {
            offset = XirArgument.forRuntimeCall(CiRuntimeCall.ResolveFieldOffset, XirArgument.forInt(cpi), XirArgument.forObject(constantPool.encoding().asObject()));
        }

        return new XirSnippet(putFieldTemplate[field.basicType().ordinal()], null, null, receiver, value, offset);

    }

    public XirTemplate buildPutField(CiKind kind) {

        if (kind == CiKind.Void) {
            return null;
        }

        XirAssembler assembler = new XirAssembler(CiKind.Void);
        XirParameter receiver = assembler.createInputOperand(CiKind.Object, false);
        XirParameter value = assembler.createInputOperand(kind, false);
        XirParameter fieldOffset = assembler.createInputOperand(CiKind.Int, true);
        assembler.pstore(kind, receiver, fieldOffset, value);
        return assembler.finished();
    }

    public XirTemplate buildGetField(CiKind kind) {
        XirAssembler assembler = new XirAssembler(kind);
        XirParameter receiver = assembler.createInputOperand(CiKind.Object, false);
        XirParameter fieldOffset = assembler.createInputOperand(CiKind.Int, true);
        XirParameter resultOperand = assembler.getResultOperand();
        assembler.pload(kind, resultOperand, receiver, fieldOffset);
        return assembler.finished();
    }
}
