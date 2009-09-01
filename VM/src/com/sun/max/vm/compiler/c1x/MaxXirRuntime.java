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

    static class XirTemplates {
        final XirTemplate resolved;
        final XirTemplate unresolved;

        XirTemplates(XirTemplate r, XirTemplate u) {
            resolved = r;
            unresolved = r;
        }
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

    final XirTemplates[] putFieldTemplates;
    final XirTemplates[] getFieldTemplates;

    final XirTemplates[] putStaticTemplates;
    final XirTemplates[] getStaticTemplates;

    final XirTemplates[] invokeVirtualTemplates;
    final XirTemplates[] invokeInterfaceTemplates;
    final XirTemplates[] invokeSpecialTemplates;
    final XirTemplates[] invokeStaticTemplates;
    final XirTemplate[] arrayLoadTemplates;
    final XirTemplate[] arrayStoreTemplates;
    final XirTemplate[] newArrayTemplates;

    final XirTemplate safepointTemplate;
    final XirTemplate monitorEnterTemplate;
    final XirTemplate monitorExitTemplate;
    final XirTemplates newObjectArrayTemplate;
    final XirTemplates newInstanceTemplate;
    final XirTemplates multiNewArrayTemplate;
    final XirTemplates checkcastForLeafTemplate;
    final XirTemplates checkcastForClassTemplate;
    final XirTemplates checkcastForInterfaceTemplate;
    final XirTemplates instanceofForLeafTemplate;
    final XirTemplates instanceofForClassTemplate;
    final XirTemplates instanceofForInterfaceTemplate;

    XirTemplate throwCheckcastStub;
    XirTemplate throwBoundsFailStub;
    XirTemplate monitorEnterStub;
    XirTemplate monitorExitStub;
    XirTemplate resolveFieldOffsetStub;
    XirTemplate resolveStaticTupleStub;

    final int hubOffset;
    final int hub_mTableLength;
    final int hub_mTableStartIndex;
    final int wordSize;
    final int arrayLengthOffset = Layout.arrayHeaderLayout().arrayLengthOffset();

    public MaxXirRuntime(VMConfiguration vmConfiguration, CiTarget target) {
        this.target = target;
        this.hubOffset = vmConfiguration.layoutScheme().generalLayout.getOffsetFromOrigin(Layout.HeaderField.HUB).toInt();
        this.hub_mTableLength = FieldActor.findInstance(Hub.class, "mTableLength").offset();
        this.hub_mTableStartIndex = FieldActor.findInstance(Hub.class, "mTableStartIndex").offset();
        this.wordSize = vmConfiguration.platform.wordWidth().numberOfBytes;

        CiKind[] kinds = CiKind.values();

        putFieldTemplates = new XirTemplates[kinds.length];
        getFieldTemplates = new XirTemplates[kinds.length];

        putStaticTemplates = new XirTemplates[kinds.length];
        getStaticTemplates = new XirTemplates[kinds.length];

        invokeVirtualTemplates   = new XirTemplates[kinds.length];
        invokeInterfaceTemplates = new XirTemplates[kinds.length];
        invokeSpecialTemplates   = new XirTemplates[kinds.length];
        invokeStaticTemplates    = new XirTemplates[kinds.length];
        arrayLoadTemplates = new XirTemplate[kinds.length];
        arrayStoreTemplates = new XirTemplate[kinds.length];
        newArrayTemplates = new XirTemplate[kinds.length];

        for (CiKind kind : kinds) {
            int index = kind.ordinal();
            if (kind != CiKind.Void) {
                putFieldTemplates[index] = buildPutFieldTemplate(kind);
                getFieldTemplates[index] = buildGetFieldTemplate(kind);

                putStaticTemplates[index] = buildPutStaticTemplate(kind);
                getStaticTemplates[index] = buildGetStaticTemplate(kind);

                arrayLoadTemplates[index] = buildArrayLoad(kind, new XirAssembler(kind));
                arrayStoreTemplates[index] = buildArrayStore(kind, new XirAssembler(kind));

                newArrayTemplates[index] = buildNewArray(kind);
            }
            invokeVirtualTemplates[index] = buildResolvedInvokeVirtual(kind);
            invokeInterfaceTemplates[index] = buildResolvedInvokeInterface(kind);
            invokeSpecialTemplates[index] = buildResolvedInvokeSpecial(kind);
            invokeStaticTemplates[index] = buildResolvedInvokeStatic(kind);
        }

        safepointTemplate = buildSafepoint();
        monitorEnterTemplate = buildMonitorEnter();
        monitorExitTemplate = buildMonitorExit();

        newInstanceTemplate = buildNewInstance();
        multiNewArrayTemplate = buildMultiNewArray();
        newObjectArrayTemplate = buildNewObjectArrayTemplate();

        checkcastForLeafTemplate = buildCheckcastForLeaf();
        checkcastForClassTemplate = buildCheckcastForInterface(); // XXX: more efficient template for class checks
        checkcastForInterfaceTemplate = buildCheckcastForInterface();

        instanceofForLeafTemplate = buildInstanceofForLeaf();
        instanceofForClassTemplate = buildInstanceofForInterface(); // XXX: more efficient template for class checks
        instanceofForInterfaceTemplate = buildInstanceofForInterface();

    }

    @Override
    public XirSnippet doPutField(XirArgument receiver, XirArgument value, RiField field, char cpi, RiConstantPool constantPool) {
        XirTemplates templates = putFieldTemplates[field.basicType().ordinal()];
        if (field.isLoaded()) {
            XirArgument offset = XirArgument.forInt(field.offset());
            return new XirSnippet(templates.resolved, receiver, value, offset);
        } else {
            XirArgument guard = XirArgument.forObject(field); // TODO: get/make the resolution guard
            return new XirSnippet(templates.unresolved, receiver, value, guard);
        }
    }

    @Override
    public XirSnippet doGetField(XirArgument receiver, RiField field, char cpi, RiConstantPool constantPool) {
        XirTemplates templates = getFieldTemplates[field.basicType().ordinal()];
        if (field.isLoaded()) {
            XirArgument offset = XirArgument.forInt(field.offset());
            return new XirSnippet(templates.resolved, receiver, offset);
        } else {
            XirArgument guard = XirArgument.forObject(field); // TODO: get/make the resolution guard
            return new XirSnippet(templates.unresolved, receiver, guard);
        }
    }

    @Override
    public XirSnippet doPutStatic(XirArgument value, RiField field) {
        XirTemplates template = putStaticTemplates[field.basicType().ordinal()];
        if (field.isLoaded()) {
            XirArgument offset = XirArgument.forInt(field.offset());
            Object tuple = ((MaxRiField) field).fieldActor.holder().staticTuple();
            return new XirSnippet(template.resolved, XirArgument.forObject(tuple), value, offset);
        } else {
            XirArgument guard = XirArgument.forObject(field); // TODO: get/make the resolution guard
            return new XirSnippet(template.unresolved, value, guard);
        }
    }

    @Override
    public XirSnippet doGetStatic(RiField field) {
        XirTemplates template = getStaticTemplates[field.basicType().ordinal()];
        if (field.isLoaded()) {
            XirArgument offset = XirArgument.forInt(field.offset());
            Object tuple = ((MaxRiField) field).fieldActor.holder().staticTuple();
            return new XirSnippet(template.resolved, XirArgument.forObject(tuple), offset);
        } else {
            XirArgument guard = XirArgument.forObject(field); // TODO: get/make the resolution guard
            return new XirSnippet(template.unresolved, guard);
        }
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
        asm.pload(CiKind.Int, length, array, asm.i(arrayLengthOffset));
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
        asm.callStub(throwBoundsFailStub, null);
        return asm.finishTemplate();
    }

    private XirTemplate buildArrayLoad(CiKind kind, XirAssembler asm) {
        XirParameter array = asm.createInputParameter(CiKind.Object);
        XirParameter index = asm.createInputParameter(CiKind.Int);
        XirTemp length = asm.createTemp(CiKind.Int);
        XirParameter result = asm.getResultOperand();
        XirLabel fail = asm.createOutOfLineLabel();
        // XXX: build a version that does not include a range check
        asm.pload(CiKind.Int, length, array, asm.i(arrayLengthOffset));
        asm.jugteq(fail, index, length);
        int elemSize = target.sizeInBytes(kind);
        if (elemSize > 1) {
            asm.shl(index, index, asm.i(Util.log2(elemSize)));
        }
        asm.add(index, index, asm.i(Layout.byteArrayLayout().getElementOffsetFromOrigin(0).toInt()));
        asm.pload(kind, result, array, index);
        asm.end();
        asm.bind(fail);
        asm.callStub(throwBoundsFailStub, null);
        return asm.finishTemplate();
    }

    private XirTemplates buildResolvedInvokeStatic(CiKind kind) {
        XirTemplate resolved, unresolved;
        {
            XirAssembler asm = new XirAssembler(kind);
            XirParameter addr = asm.createConstantInputParameter(CiKind.Word);
            asm.callJava(asm.getResultOperand(), addr);
            resolved = asm.finishTemplate();
        }
        {
            // TODO: unresolved invoke static
            unresolved = null;
        }
        return new XirTemplates(resolved, unresolved);
    }

    private XirTemplate buildNewArray(CiKind kind) {
        return null;
    }

    private XirTemplates buildNewObjectArrayTemplate() {
        return null;
    }

    private XirTemplates buildMultiNewArray() {
        return null;
    }

    private XirTemplates buildNewInstance() {
        return null;
    }


    private XirTemplates buildResolvedInvokeSpecial(CiKind kind) {
        XirTemplate resolved, unresolved;
        {
            // resolved case
            XirAssembler asm = new XirAssembler(kind);
            XirParameter receiver = asm.createInputParameter(CiKind.Object); // receiver object
            XirParameter addr = asm.createConstantInputParameter(CiKind.Word); // address to call
            asm.callJava(asm.getResultOperand(), addr);
            resolved = asm.finishTemplate();
        }
        {
            // TODO: unresolved invokespecial
            unresolved = null;
        }
        return new XirTemplates(resolved, unresolved);
    }

    private XirTemplates buildResolvedInvokeInterface(CiKind kind) {
        XirTemplate resolved, unresolved;
        {
            // resolved case
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
            resolved = asm.finishTemplate();
        }
        {
            // TODO: unresolved invokeinterface
            unresolved = null;
        }
        return new XirTemplates(resolved, unresolved);
    }

    private XirTemplates buildResolvedInvokeVirtual(CiKind kind) {
        XirTemplate resolved, unresolved;
        {
            // resolved invokevirtual
            XirAssembler asm = new XirAssembler(kind);
            XirParameter receiver = asm.createInputParameter(CiKind.Object);
            XirParameter vtableOffset = asm.createConstantInputParameter(CiKind.Int);
            XirTemp hub = asm.createTemp(CiKind.Object);
            XirTemp addr = asm.createTemp(CiKind.Word);
            asm.pload(CiKind.Object, hub, receiver, asm.i(hubOffset));
            asm.pload(CiKind.Word, addr, hub, vtableOffset);
            asm.callJava(asm.getResultOperand(), addr);
            resolved = asm.finishTemplate();
        }
        {
            // TODO: unresolved invokevirtual
            unresolved = null;
        }
        return new XirTemplates(resolved, unresolved);
    }

    private XirTemplates buildPutFieldTemplate(CiKind kind) {
        XirTemplate resolved, unresolved;
        {
            // resolved case
            XirAssembler asm = new XirAssembler(CiKind.Void);
            XirParameter object = asm.createInputParameter(CiKind.Object);
            XirParameter value = asm.createInputParameter(kind);
            XirParameter fieldOffset = asm.createConstantInputParameter(CiKind.Int);
            asm.pstore(kind, object, fieldOffset, value);
            if (kind == CiKind.Object) {
                addWriteBarrier(asm, object, value);
            }
            resolved = asm.finishTemplate();
        } {
            // unresolved case
            XirAssembler asm = new XirAssembler(CiKind.Void);
            XirParameter object = asm.createInputParameter(CiKind.Object);
            XirParameter value = asm.createInputParameter(kind);
            XirParameter guard = asm.createInputParameter(CiKind.Object);
            XirCache fieldOffset = asm.createCache(CiKind.Int, false);
            XirLabel resolve = asm.createOutOfLineLabel();
            XirLabel put = asm.createInlineLabel();
            asm.jeq(resolve, fieldOffset, asm.i(0));
            asm.bind(put);
            asm.pstore(kind, object, fieldOffset, value);
            if (kind == CiKind.Object) {
                addWriteBarrier(asm, object, value);
            }
            asm.end();
            asm.bind(resolve);
            asm.callStub(resolveFieldOffsetStub, fieldOffset, guard);
            asm.jmp(put);
            unresolved = asm.finishTemplate();
        }
        return new XirTemplates(resolved, unresolved);
    }

    private XirTemplates buildGetFieldTemplate(CiKind kind) {
        XirTemplate resolved, unresolved;
        {
            // resolved case
            XirAssembler asm = new XirAssembler(kind);
            XirParameter object = asm.createInputParameter(CiKind.Object);
            XirParameter fieldOffset = asm.createConstantInputParameter(CiKind.Int);
            XirParameter resultOperand = asm.getResultOperand();
            asm.pload(kind, resultOperand, object, fieldOffset);
            resolved = asm.finishTemplate();
        }
        {
            // unresolved case
            XirAssembler asm = new XirAssembler(kind);
            XirParameter object = asm.createInputParameter(CiKind.Object);
            XirParameter resultOperand = asm.getResultOperand();
            XirParameter guard = asm.createInputParameter(CiKind.Object);
            XirCache fieldOffset = asm.createCache(CiKind.Int, false);
            XirLabel get = asm.createInlineLabel();
            XirLabel resolve = asm.createOutOfLineLabel();
            asm.jeq(resolve, fieldOffset, asm.i(0));
            asm.bind(get);
            asm.pload(kind, resultOperand, object, fieldOffset);
            asm.end();
            asm.bind(resolve);
            asm.callStub(resolveFieldOffsetStub, fieldOffset, guard);
            asm.jmp(get);
            unresolved = asm.finishTemplate();
        }
        return new XirTemplates(resolved, unresolved);
    }

    private XirTemplates buildPutStaticTemplate(CiKind kind) {
        XirTemplate resolved, unresolved;
        {
            // XXX: this is identical to put field, except the tuple is a constant
            XirAssembler asm = new XirAssembler(CiKind.Void);
            XirParameter tuple = asm.createConstantInputParameter(CiKind.Object);
            XirParameter value = asm.createInputParameter(kind);
            XirParameter fieldOffset = asm.createConstantInputParameter(CiKind.Int);
            asm.pstore(kind, tuple, fieldOffset, value);
            if (kind == CiKind.Object) {
                addWriteBarrier(asm, tuple, value);
            }
            resolved = asm.finishTemplate();
        }
        {
            // unresolved put static
            XirAssembler asm = new XirAssembler(CiKind.Void);
            XirParameter value = asm.createInputParameter(kind);
            XirParameter guard = asm.createInputParameter(CiKind.Object);
            XirCache tuple = asm.createCache(CiKind.Object, false);
            XirCache fieldOffset = asm.createCache(CiKind.Int, false);
            XirLabel resolve = asm.createOutOfLineLabel();
            XirLabel put = asm.createInlineLabel();
            asm.jeq(resolve, fieldOffset, asm.i(0));
            asm.bind(put);
            asm.pstore(kind, tuple, fieldOffset, value);
            if (kind == CiKind.Object) {
                addWriteBarrier(asm, tuple, value);
            }
            asm.end();
            asm.bind(resolve);
            asm.callStub(resolveStaticTupleStub, tuple, guard);
            asm.callStub(resolveFieldOffsetStub, fieldOffset, guard);
            asm.jmp(put);
            unresolved = asm.finishTemplate();
        }
        return new XirTemplates(resolved, unresolved);
    }

    private XirTemplates buildGetStaticTemplate(CiKind kind) {
        XirTemplate resolved, unresolved;
        {
            // resolved get static
            XirAssembler asm = new XirAssembler(kind);
            XirParameter tuple = asm.createInputParameter(CiKind.Object);
            XirParameter fieldOffset = asm.createConstantInputParameter(CiKind.Int);
            XirParameter resultOperand = asm.getResultOperand();
            asm.pload(kind, resultOperand, tuple, fieldOffset);
            resolved = asm.finishTemplate();
        }
        {
            // unresolved get static
            XirAssembler asm = new XirAssembler(kind);
            XirParameter resultOperand = asm.getResultOperand();
            XirParameter guard = asm.createInputParameter(CiKind.Object);
            XirCache fieldOffset = asm.createCache(CiKind.Int, false);
            XirCache tuple = asm.createCache(CiKind.Object, false);
            XirLabel get = asm.createInlineLabel();
            XirLabel resolve = asm.createOutOfLineLabel();
            asm.jeq(resolve, fieldOffset, asm.i(0));
            asm.bind(get);
            asm.pload(kind, resultOperand, tuple, fieldOffset);
            asm.end();
            asm.bind(resolve);
            asm.callStub(resolveStaticTupleStub, tuple, guard);
            asm.callStub(resolveFieldOffsetStub, fieldOffset, guard);
            asm.jmp(get);
            unresolved = asm.finishTemplate();
        }
        return new XirTemplates(resolved, unresolved);
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

    private XirTemplates buildCheckcastForLeaf() {
        XirTemplate resolved, unresolved;
        {
            // resolved checkcast for a leaf class
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
            asm.callStub(throwCheckcastStub, null);
            resolved = asm.finishTemplate();
        }
        {
            // TODO: return the template for unresolved checkcast
            unresolved = null;
        }
        return new XirTemplates(resolved, unresolved);
    }

    private XirTemplates buildCheckcastForInterface() {
        XirTemplate resolved, unresolved;
        {
            // resolved checkcast against an interface class
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
            // XXX: use a cache to check the last successful receiver type
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
            asm.callStub(throwCheckcastStub, null);
            resolved = asm.finishTemplate();
        }
        {
            // TODO: return the template for unresolved checkcast
            unresolved = null;
        }
        return new XirTemplates(resolved, unresolved);
    }

    private XirTemplates buildInstanceofForLeaf() {
        XirTemplate resolved, unresolved;
        {
            XirAssembler asm = new XirAssembler(CiKind.Object);
            XirParameter result = asm.getResultOperand();
            XirParameter object = asm.createInputParameter(CiKind.Object);
            XirParameter hub = asm.createConstantInputParameter(CiKind.Object);
            XirTemp temp = asm.createTemp(CiKind.Object);
            XirLabel pass = asm.createInlineLabel();
            XirLabel fail = asm.createInlineLabel();
            // XXX: build a version that does not include a null check
            asm.jeq(pass, object, asm.o(null));
            asm.pload(CiKind.Object, temp, object, asm.i(hubOffset));
            asm.jneq(fail, hub, temp);
            asm.bind(pass);
            asm.mov(result, asm.i(1));
            asm.end();
            asm.bind(fail);
            asm.mov(result, asm.i(0));
            asm.end();
            resolved = asm.finishTemplate();
        }
        {
            // TODO: return the unresolved instanceof template
            unresolved = null;
        }
        return new XirTemplates(resolved, unresolved);
    }

    private XirTemplates buildInstanceofForInterface() {
        XirTemplate resolved, unresolved;
        {
            // resolved instanceof for interface
            XirAssembler asm = new XirAssembler(CiKind.Object);
            XirParameter result = asm.getResultOperand();
            XirParameter object = asm.createInputParameter(CiKind.Object);
            XirParameter interfaceID = asm.createConstantInputParameter(CiKind.Int);
            XirTemp hub = asm.createTemp(CiKind.Object);
            XirTemp mtableLength = asm.createTemp(CiKind.Int);
            XirTemp mtableStartIndex = asm.createTemp(CiKind.Int);
            XirTemp a = asm.createTemp(CiKind.Int);
            XirLabel pass = asm.createInlineLabel();
            XirLabel fail = asm.createOutOfLineLabel();
            // XXX: build a version that does not include a null check
            // XXX: use a cache to check the last successful receiver type
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
            asm.mov(result, asm.i(1));
            asm.end();
            asm.bind(fail);
            asm.mov(result, asm.i(0));
            asm.end();
            resolved = asm.finishTemplate();
        }
        {
            // TODO: return the unresolved checkcast stub
            unresolved = null;
        }
        return new XirTemplates(resolved, unresolved);
    }

    private void addWriteBarrier(XirAssembler asm, XirVariable object, XirVariable value) {
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
