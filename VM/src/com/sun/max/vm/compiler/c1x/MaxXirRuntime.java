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
import com.sun.c1x.util.Util;
import com.sun.c1x.target.x86.X86;
import com.sun.max.vm.layout.Layout;
import com.sun.max.vm.VMConfiguration;

/**
 * @author Thomas Wuerthinger
 * @author Ben L. Titzer
 */
public class MaxXirRuntime extends XirRuntime {

    private final CiTarget target;

    private final XirTemplate[] putFieldTemplates;
    private final XirTemplate[] getFieldTemplates;
    private final XirTemplate[] resolvedInvokeVirtualTemplates;
    private final XirTemplate[] resolvedInvokeInterfaceTemplates;
    private final XirTemplate[] resolvedInvokeSpecialTemplates;
    private final XirTemplate[] resolvedInvokeStaticTemplates;
    private final XirTemplate[] arrayLoadTemplates;
    private final XirTemplate[] arrayStoreTemplates;

    private XirTemplate safepointTemplate;
    private XirTemplate monitorEnterTemplate;
    private XirTemplate monitorExitTemplate;
    private XirTemplate resolvedNewInstanceTemplate;
    private XirTemplate resolvedNewArrayTemplate;
    private XirTemplate newPrimitiveArrayTemplate;
    private XirTemplate resolvedCheckcastForLeafTemplate;
    private XirTemplate resolvedCheckcastForClassTemplate;
    private XirTemplate resolvedCheckcastForInterfaceTemplate;
    private XirTemplate resolvedInstanceofForLeafTemplate;
    private XirTemplate resolvedInstanceofForClassTemplate;
    private XirTemplate resolvedInstanceofForInterfaceTemplate;
    private XirTemplate throwCheckcastStub;
    private XirTemplate throwBoundsFailStub;
    private final int hubOffset;

    public MaxXirRuntime(CiTarget target) {
        this.target = target;
        CiKind[] kinds = CiKind.values();
        putFieldTemplates = new XirTemplate[kinds.length];
        getFieldTemplates = new XirTemplate[kinds.length];
        resolvedInvokeVirtualTemplates = new XirTemplate[kinds.length];
        resolvedInvokeInterfaceTemplates = new XirTemplate[kinds.length];
        resolvedInvokeSpecialTemplates = new XirTemplate[kinds.length];
        resolvedInvokeStaticTemplates = new XirTemplate[kinds.length];
        arrayLoadTemplates = new XirTemplate[kinds.length];
        arrayStoreTemplates = new XirTemplate[kinds.length];

        for (CiKind kind : kinds) {
            int index = kind.ordinal();
            if (kind != CiKind.Void) {
                putFieldTemplates[index] = buildPutField(kind, new XirAssembler(kind));
                getFieldTemplates[index] = buildGetField(kind, new XirAssembler(kind));
                arrayLoadTemplates[index] = buildArrayLoad(kind, new XirAssembler(kind));
                arrayStoreTemplates[index] = buildArrayStore(kind, new XirAssembler(kind));
            }
            resolvedInvokeVirtualTemplates[index] = buildResolvedInvokeVirtual(kind);
            resolvedInvokeInterfaceTemplates[index] = buildResolvedInvokeInterface(kind);
            resolvedInvokeSpecialTemplates[index] = buildResolvedInvokeSpecial(kind);
            resolvedInvokeStaticTemplates[index] = buildResolvedInvokeStatic(kind);
        }

        safepointTemplate = buildSafepoint();
        monitorEnterTemplate = buildMonitorEnter();
        monitorExitTemplate = buildMonitorExit();
        resolvedCheckcastForLeafTemplate = buildCheckcastForLeaf();
        hubOffset = VMConfiguration.target().layoutScheme().generalLayout.getOffsetFromOrigin(Layout.HeaderField.HUB).toInt();
    }

    @Override
    public XirSnippet doPutField(XirArgument receiver, XirArgument value, RiField field, char cpi, RiConstantPool constantPool) {
        XirArgument offset;
        if (field.isLoaded()) {
            offset = XirArgument.forInt(field.offset());
        } else {
            offset = XirArgument.forRuntimeCall(CiRuntimeCall.ResolveFieldOffset, XirArgument.forInt(cpi), XirArgument.forObject(constantPool.encoding().asObject()));
        }

        return new XirSnippet(putFieldTemplates[field.basicType().ordinal()], null, null, receiver, value, offset);
    }

    @Override
    public XirSnippet doGetField(XirArgument receiver, RiField field, char cpi, RiConstantPool constantPool) {
        XirArgument offset;
        if (field.isLoaded()) {
            offset = XirArgument.forInt(field.offset());
        } else {
            offset = XirArgument.forRuntimeCall(CiRuntimeCall.ResolveFieldOffset, XirArgument.forInt(cpi), XirArgument.forObject(constantPool.encoding().asObject()));
        }

        return new XirSnippet(getFieldTemplates[field.basicType().ordinal()], null, null, receiver, offset);
    }

    private XirTemplate buildSafepoint() {
        XirAssembler asm = new XirAssembler(CiKind.Void);
        XirParameter param = asm.createRegister(CiKind.Word, X86.r14);
        asm.pload(CiKind.Word, param, param);
        return asm.finished();
    }

    private XirTemplate buildArrayStore(CiKind kind, XirAssembler asm) {
        XirParameter array = asm.createInputParameter(CiKind.Object);
        XirParameter index = asm.createInputParameter(CiKind.Int);
        XirParameter value = asm.createInputParameter(kind);
        XirParameter length = asm.createTemp(CiKind.Int);
        XirLabel fail = asm.createOutOfLineLabel();
        asm.pload(CiKind.Int, length, array, Layout.arrayHeaderLayout().arrayLengthOffset());
        asm.jugteq(fail, index, length);
        int elemSize = target.sizeInBytes(kind);
        if (elemSize > 1) {
            asm.shl(index, index, asm.i(Util.log2(elemSize)));
        }
        if (kind == CiKind.Object) {
            // TODO: array store check for kind object
            // TODO: write barrier for kind object
        }
        asm.add(index, index, asm.i(Layout.byteArrayLayout().getElementOffsetFromOrigin(0).toInt()));
        asm.pstore(kind, array, index, value);
        asm.ret();
        asm.bind(fail);
        asm.stub(throwBoundsFailStub);
        return asm.finished();
    }

    private XirTemplate buildArrayLoad(CiKind kind, XirAssembler asm) {
        XirParameter array = asm.createInputParameter(CiKind.Object);
        XirParameter index = asm.createInputParameter(CiKind.Int);
        XirParameter length = asm.createTemp(CiKind.Int);
        XirParameter result = asm.getResultOperand();
        XirLabel fail = asm.createOutOfLineLabel();
        asm.pload(CiKind.Int, length, array, Layout.arrayHeaderLayout().arrayLengthOffset());
        asm.jugteq(fail, index, length);
        int elemSize = target.sizeInBytes(kind);
        if (elemSize > 1) {
            asm.shl(index, index, asm.i(Util.log2(elemSize)));
        }
        asm.add(index, index, asm.i(Layout.byteArrayLayout().getElementOffsetFromOrigin(0).toInt()));
        asm.pload(kind, result, array, index);
        asm.ret();
        asm.bind(fail);
        asm.stub(throwBoundsFailStub);
        return asm.finished();
    }

    private XirTemplate buildResolvedInvokeStatic(CiKind kind) {
        return null;
    }

    private XirTemplate buildResolvedInvokeSpecial(CiKind kind) {
        return null;
    }

    private XirTemplate buildResolvedInvokeInterface(CiKind kind) {
        return null;
    }

    private XirTemplate buildResolvedInvokeVirtual(CiKind kind) {
        return null;
    }

    private XirTemplate buildPutField(CiKind kind, XirAssembler asm) {
        XirParameter receiver = asm.createInputParameter(CiKind.Object);
        XirParameter value = asm.createInputParameter(kind);
        XirParameter fieldOffset = asm.createConstantInputParameter(CiKind.Int);
        asm.pstore(kind, receiver, fieldOffset, value);
        return asm.finished();
    }

    private XirTemplate buildGetField(CiKind kind, XirAssembler asm) {
        XirParameter receiver = asm.createInputParameter(CiKind.Object);
        XirParameter fieldOffset = asm.createConstantInputParameter(CiKind.Int);
        XirParameter resultOperand = asm.getResultOperand();
        asm.pload(kind, resultOperand, receiver, fieldOffset);
        return asm.finished();
    }

    private XirTemplate buildMonitorExit() {
        return null; // TODO: unimplemented
    }

    private XirTemplate buildMonitorEnter() {
        return null; // TODO: unimplemented
    }

    private XirTemplate buildCheckcastForLeaf() {
        XirAssembler asm = new XirAssembler(CiKind.Object);
        XirParameter object = asm.createInputParameter(CiKind.Object);
        XirParameter hub = asm.createConstantInputParameter(CiKind.Object);
        XirParameter temp = asm.createTemp(CiKind.Object);
        XirLabel fail = asm.createOutOfLineLabel();
        asm.pload(CiKind.Object, temp, object, asm.i(hubOffset));
        asm.jneq(fail, hub, temp);
        asm.ret();
        asm.bind(fail);
        asm.stub(throwCheckcastStub);
        // TODO: out of line code for throwing class cast exception
        return asm.finished();
    }

    private XirTemplate buildThrowCheckcastStub() {
        return null;
    }

}
