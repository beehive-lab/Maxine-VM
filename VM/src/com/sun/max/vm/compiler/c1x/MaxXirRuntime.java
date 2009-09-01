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
import com.sun.max.vm.type.Kind;
import com.sun.max.vm.type.KindEnum;
import com.sun.max.vm.actor.member.FieldActor;
import com.sun.max.vm.actor.holder.Hub;

/**
 * @author Thomas Wuerthinger
 * @author Ben L. Titzer
 */
public class MaxXirRuntime extends XirRuntime {

    private static final CiKind[] kindMapping;
    private static final Kind[] ciKindMapping;

    static void map(KindEnum k, CiKind ck) {
        kindMapping[k.ordinal()] = ck;
        ciKindMapping[ck.ordinal()] = k.asKind();
    }

    static {
        kindMapping = new CiKind[KindEnum.values().length];
        ciKindMapping = new Kind[CiKind.values().length];

        map(KindEnum.VOID, CiKind.Void);
        map(KindEnum.BYTE, CiKind.Byte);
        map(KindEnum.BOOLEAN, CiKind.Boolean);
        map(KindEnum.CHAR, CiKind.Char);
        map(KindEnum.SHORT, CiKind.Short);
        map(KindEnum.INT, CiKind.Int);
        map(KindEnum.FLOAT, CiKind.Float);
        map(KindEnum.LONG, CiKind.Long);
        map(KindEnum.DOUBLE, CiKind.Double);
        map(KindEnum.WORD, CiKind.Word);
        map(KindEnum.REFERENCE, CiKind.Object);
    }

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
    private XirTemplate monitorEnterStub;
    private XirTemplate monitorExitStub;

    private final int hubOffset;
    private final int hub_mTableLength;
    private final int hub_mTableStartIndex;
    private final int wordSize;

    public MaxXirRuntime(VMConfiguration vmConfiguration, CiTarget target) {
        this.target = target;
        this.hubOffset = vmConfiguration.layoutScheme().generalLayout.getOffsetFromOrigin(Layout.HeaderField.HUB).toInt();
        this.hub_mTableLength = FieldActor.findInstance(Hub.class, "mTableLength").offset();
        this.hub_mTableStartIndex = FieldActor.findInstance(Hub.class, "mTableStartIndex").offset();
        this.wordSize = vmConfiguration.platform.wordWidth().numberOfBytes;

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
        resolvedCheckcastForClassTemplate = buildCheckcastForInterface(); // XXX: more efficient template for class checks
        resolvedCheckcastForInterfaceTemplate = buildCheckcastForInterface();

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
        XirVariable param = asm.createRegister(CiKind.Word, X86.r14);
        asm.pload(CiKind.Word, param, param);
        return asm.finishTemplate();
    }

    private XirTemplate buildArrayStore(CiKind kind, XirAssembler asm) {
        XirParameter array = asm.createInputParameter(CiKind.Object);
        XirParameter index = asm.createInputParameter(CiKind.Int);
        XirParameter value = asm.createInputParameter(kind);
        XirTemp length = asm.createTemp(CiKind.Int);
        XirLabel fail = asm.createOutOfLineLabel();
        // XXX: build a version that does not include a range check
        asm.pload(CiKind.Int, length, array, Layout.arrayHeaderLayout().arrayLengthOffset());
        asm.jugteq(fail, index, length);
        int elemSize = target.sizeInBytes(kind);
        if (elemSize > 1) {
            asm.shl(index, index, asm.i(Util.log2(elemSize)));
        }
        if (kind == CiKind.Object) {
            // TODO: array store check for kind object
            addWriteBarrier(asm, array, value);
        }
        asm.add(index, index, asm.i(Layout.byteArrayLayout().getElementOffsetFromOrigin(0).toInt()));
        asm.pstore(kind, array, index, value);
        asm.end();
        asm.bind(fail);
        asm.callStub(throwBoundsFailStub);
        return asm.finishTemplate();
    }

    private XirTemplate buildArrayLoad(CiKind kind, XirAssembler asm) {
        XirParameter array = asm.createInputParameter(CiKind.Object);
        XirParameter index = asm.createInputParameter(CiKind.Int);
        XirTemp length = asm.createTemp(CiKind.Int);
        XirParameter result = asm.getResultOperand();
        XirLabel fail = asm.createOutOfLineLabel();
        // XXX: build a version that does not include a range check
        asm.pload(CiKind.Int, length, array, Layout.arrayHeaderLayout().arrayLengthOffset());
        asm.jugteq(fail, index, length);
        int elemSize = target.sizeInBytes(kind);
        if (elemSize > 1) {
            asm.shl(index, index, asm.i(Util.log2(elemSize)));
        }
        asm.add(index, index, asm.i(Layout.byteArrayLayout().getElementOffsetFromOrigin(0).toInt()));
        asm.pload(kind, result, array, index);
        asm.end();
        asm.bind(fail);
        asm.callStub(throwBoundsFailStub);
        return asm.finishTemplate();
    }

    private XirTemplate buildResolvedInvokeStatic(CiKind kind) {
        XirAssembler asm = new XirAssembler(kind);
        XirParameter addr = asm.createConstantInputParameter(CiKind.Word);
        asm.callJava(asm.getResultOperand(), addr);
        return asm.finishTemplate();
    }

    private XirTemplate buildResolvedInvokeSpecial(CiKind kind) {
        XirAssembler asm = new XirAssembler(kind);
        XirParameter receiver = asm.createInputParameter(CiKind.Object); // receiver object
        XirParameter addr = asm.createConstantInputParameter(CiKind.Word); // address to call
        asm.callJava(asm.getResultOperand(), addr);
        return asm.finishTemplate();
    }

    private XirTemplate buildResolvedInvokeInterface(CiKind kind) {
        XirAssembler asm = new XirAssembler(kind);
        XirParameter receiver = asm.createInputParameter(CiKind.Object); // receiver object
        XirParameter interfaceID = asm.createConstantInputParameter(CiKind.Int);
        XirParameter methodIndex = asm.createConstantInputParameter(CiKind.Int);
        XirTemp hub = asm.createTemp(CiKind.Object);
        XirTemp mtableLength = asm.createTemp(CiKind.Int);
        XirTemp mtableStartIndex = asm.createTemp(CiKind.Int);
        XirTemp a = asm.createTemp(CiKind.Int);
        XirTemp addr = asm.createTemp(CiKind.Word);
        asm.pload(CiKind.Object, hub, receiver, asm.i(hubOffset));
        asm.pload(CiKind.Int, mtableLength, hub, asm.i(hub_mTableLength));
        asm.pload(CiKind.Int, mtableStartIndex, hub, asm.i(hub_mTableStartIndex));
        asm.mod(a, interfaceID, mtableLength);
        asm.add(a, a, mtableStartIndex);
        asm.add(a, a, methodIndex);
        asm.mul(a, a, asm.i(wordSize));
        asm.pload(CiKind.Word, a, hub, a);
        asm.callJava(asm.getResultOperand(), addr);
        return asm.finishTemplate();
    }

    private XirTemplate buildResolvedInvokeVirtual(CiKind kind) {
        XirAssembler asm = new XirAssembler(kind);
        XirParameter receiver = asm.createInputParameter(CiKind.Object);
        XirParameter vtableOffset = asm.createConstantInputParameter(CiKind.Int);
        XirTemp hub = asm.createTemp(CiKind.Object);
        XirTemp addr = asm.createTemp(CiKind.Word);
        asm.pload(CiKind.Object, hub, receiver, asm.i(hubOffset));
        asm.pload(CiKind.Word, addr, hub, vtableOffset);
        asm.callJava(asm.getResultOperand(), addr);
        return asm.finishTemplate();
    }

    private XirTemplate buildPutField(CiKind kind, XirAssembler asm) {
        XirParameter object = asm.createInputParameter(CiKind.Object);
        XirParameter value = asm.createInputParameter(kind);
        XirParameter fieldOffset = asm.createConstantInputParameter(CiKind.Int);
        asm.pstore(kind, object, fieldOffset, value);
        if (kind == CiKind.Object) {
            addWriteBarrier(asm, object, value);
        }
        return asm.finishTemplate();
    }

    private XirTemplate buildGetField(CiKind kind, XirAssembler asm) {
        XirParameter object = asm.createInputParameter(CiKind.Object);
        XirParameter fieldOffset = asm.createConstantInputParameter(CiKind.Int);
        XirParameter resultOperand = asm.getResultOperand();
        asm.pload(kind, resultOperand, object, fieldOffset);
        return asm.finishTemplate();
    }

    private XirTemplate buildMonitorExit() {
        XirAssembler asm = new XirAssembler(CiKind.Void);
        XirParameter object = asm.createInputParameter(CiKind.Object);
        asm.callStub(monitorExitStub, object);
        return asm.finishTemplate();
    }

    private XirTemplate buildMonitorEnter() {
        XirAssembler asm = new XirAssembler(CiKind.Void);
        XirParameter object = asm.createInputParameter(CiKind.Object);
        asm.callStub(monitorEnterStub, object);
        return asm.finishTemplate();
    }

    private XirTemplate buildCheckcastForLeaf() {
        XirAssembler asm = new XirAssembler(CiKind.Object);
        XirParameter object = asm.createInputParameter(CiKind.Object);
        XirParameter hub = asm.createConstantInputParameter(CiKind.Object);
        XirTemp temp = asm.createTemp(CiKind.Object);
        XirLabel pass = asm.createInlineLabel();
        XirLabel fail = asm.createOutOfLineLabel();
        // XXX: build a version that does not include a null check
        asm.jeq(pass, object, asm.o(null));
        asm.pload(CiKind.Object, temp, object, asm.i(hubOffset));
        asm.jneq(fail, hub, temp);
        asm.bind(pass);
        asm.end();
        asm.bind(fail);
        asm.callStub(throwCheckcastStub);
        return asm.finishTemplate();
    }

    private XirTemplate buildCheckcastForInterface() {
        XirAssembler asm = new XirAssembler(CiKind.Object);
        XirParameter object = asm.createInputParameter(CiKind.Object);
        XirParameter interfaceID = asm.createConstantInputParameter(CiKind.Int);
        XirTemp hub = asm.createTemp(CiKind.Object);
        XirTemp mtableLength = asm.createTemp(CiKind.Int);
        XirTemp mtableStartIndex = asm.createTemp(CiKind.Int);
        XirTemp a = asm.createTemp(CiKind.Int);
        XirLabel pass = asm.createInlineLabel();
        XirLabel fail = asm.createOutOfLineLabel();
        // XXX: build a version that does not include a null check
        asm.jeq(pass, object, asm.o(null));
        asm.pload(CiKind.Object, hub, object, asm.i(hubOffset));
        asm.pload(CiKind.Int, mtableLength, hub, asm.i(hub_mTableLength));
        asm.pload(CiKind.Int, mtableStartIndex, hub, asm.i(hub_mTableStartIndex));
        asm.mod(a, interfaceID, mtableLength);
        asm.add(a, a, mtableStartIndex);
        asm.mul(a, a, asm.i(wordSize));
        asm.pload(CiKind.Int, a, hub, a);
        asm.jneq(fail, a, interfaceID);
        asm.bind(pass);
        asm.end();
        asm.bind(fail);
        asm.callStub(throwCheckcastStub);
        return asm.finishTemplate();
    }

    private void addWriteBarrier(XirAssembler asm, XirParameter object, XirParameter value) {
        // XXX: add write barrier mechanism
    }

    private XirTemplate buildThrowCheckcastStub() {
        XirAssembler asm = new XirAssembler(CiKind.Illegal);
        XirParameter hub = asm.createInputParameter(CiKind.Object);
        asm.callRuntime(new Object(), hub); // TODO: distinguish runtime call
        return asm.finishStub();
    }

    private void getfield(XirAssembler asm, XirParameter r, XirParameter o, FieldActor field) {
        asm.pload(toCiKind(field.descriptor().toKind()), r, o, asm.i(field.offset()));
    }

    private CiKind toCiKind(Kind k) {
        return kindMapping[k.asEnum.ordinal()];
    }

    private Kind toKind(CiKind k) {
        return ciKindMapping[k.ordinal()];
    }
}
