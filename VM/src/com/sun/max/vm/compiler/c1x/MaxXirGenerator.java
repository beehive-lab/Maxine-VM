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
import com.sun.max.vm.MaxineVM;
import com.sun.max.vm.object.ObjectAccess;
import com.sun.max.vm.object.ArrayAccess;
import com.sun.max.vm.heap.Heap;
import com.sun.max.vm.compiler.snippet.ResolutionSnippet;
import com.sun.max.vm.compiler.CompilationScheme;
import com.sun.max.vm.compiler.CallEntryPoint;
import com.sun.max.vm.runtime.ResolutionGuard;
import com.sun.max.vm.runtime.Throw;
import com.sun.max.vm.type.Kind;
import com.sun.max.vm.type.KindEnum;
import com.sun.max.vm.type.SignatureDescriptor;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.actor.holder.Hub;
import com.sun.max.vm.actor.holder.DynamicHub;
import com.sun.max.vm.actor.holder.ClassActor;
import com.sun.max.unsafe.Word;
import com.sun.max.unsafe.UnsafeLoophole;
import com.sun.max.program.ProgramError;
import com.sun.max.lang.Arrays;
import com.sun.max.annotate.INLINE;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Array;
import java.util.HashMap;

/**
 * This class implements the VM interface for generating XIR snippets that express
 * the low-level implementation of each bytecode for C1X compilation.
 *
 * @author Ben L. Titzer
 * @author Thomas Wuerthinger
 */
public class MaxXirGenerator extends XirGenerator {

    private static final CiKind[] kindMapping;
    private static final Kind[] ciKindMapping;
    private int offsetOfFirstArrayElement;
    private static final int SMALL_MULTIANEWARRAY_RANK = 4;
    private static final int MAX_MULTIANEWARRAY_RANK = 255;

    static void map(KindEnum k, CiKind ck) {
        kindMapping[k.ordinal()] = ck;
        ciKindMapping[ck.ordinal()] = k.asKind();
    }

    static class XirPair {
        final XirTemplate resolved;
        final XirTemplate unresolved;

        XirPair(XirTemplate r, XirTemplate u) {
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

    final HashMap<String, XirTemplate> runtimeCallStubs = new HashMap<String, XirTemplate>();

    private final CiTarget target;

    final XirPair[] putFieldTemplates;
    final XirPair[] getFieldTemplates;

    final XirPair[] putStaticTemplates;
    final XirPair[] getStaticTemplates;

    final XirPair[] invokeVirtualTemplates;
    final XirPair[] invokeInterfaceTemplates;
    final XirPair[] invokeSpecialTemplates;
    final XirPair[] invokeStaticTemplates;
    final XirPair[] newArrayTemplates;
    final XirTemplate[] arrayLoadTemplates;
    final XirTemplate[] arrayStoreTemplates;

    final XirPair[] multiNewArrayTemplate;

    final XirTemplate safepointTemplate;
    final XirTemplate monitorEnterTemplate;
    final XirTemplate monitorExitTemplate;
    final XirTemplate resolveClassTemplate;
    final XirPair newInstanceTemplate;
    final XirPair checkcastForLeafTemplate;
    final XirPair checkcastForClassTemplate;
    final XirPair checkcastForInterfaceTemplate;
    final XirPair instanceofForLeafTemplate;
    final XirPair instanceofForClassTemplate;
    final XirPair instanceofForInterfaceTemplate;

    final int hubOffset;
    final int hub_mTableLength;
    final int hub_mTableStartIndex;
    final int hub_componentHub;
    final int wordSize;
    final int arrayLengthOffset;

    public MaxXirGenerator(VMConfiguration vmConfiguration, CiTarget target) {
        this.target = target;
        this.hubOffset = vmConfiguration.layoutScheme().generalLayout.getOffsetFromOrigin(Layout.HeaderField.HUB).toInt();
        this.hub_mTableLength = FieldActor.findInstance(Hub.class, "mTableLength").offset();
        this.hub_mTableStartIndex = FieldActor.findInstance(Hub.class, "mTableStartIndex").offset();
        this.hub_componentHub = FieldActor.findInstance(Hub.class, "componentHub").offset();
        this.wordSize = vmConfiguration.platform.wordWidth().numberOfBytes;
        assert wordSize == target.arch.wordSize : "word size mismatch";
        this.arrayLengthOffset = Layout.arrayHeaderLayout().arrayLengthOffset();

        CiKind[] kinds = CiKind.values();

        putFieldTemplates = new XirPair[kinds.length];
        getFieldTemplates = new XirPair[kinds.length];

        putStaticTemplates = new XirPair[kinds.length];
        getStaticTemplates = new XirPair[kinds.length];

        invokeVirtualTemplates   = new XirPair[kinds.length];
        invokeInterfaceTemplates = new XirPair[kinds.length];
        invokeSpecialTemplates   = new XirPair[kinds.length];
        invokeStaticTemplates    = new XirPair[kinds.length];
        newArrayTemplates = new XirPair[kinds.length];
        arrayLoadTemplates = new XirTemplate[kinds.length];
        arrayStoreTemplates = new XirTemplate[kinds.length];

        for (CiKind kind : kinds) {
            int index = kind.ordinal();
            if (kind == CiKind.Illegal || kind == CiKind.Jsr) {
                continue;
            }
            if (kind != CiKind.Void) {
                putFieldTemplates[index] = buildPutFieldTemplate(kind, kind == CiKind.Object);
                getFieldTemplates[index] = buildGetFieldTemplate(kind);

                putStaticTemplates[index] = buildPutStaticTemplate(kind, kind == CiKind.Object);
                getStaticTemplates[index] = buildGetStaticTemplate(kind);

                arrayLoadTemplates[index] = buildArrayLoad(kind, new XirAssembler(kind), true);
                arrayStoreTemplates[index] = buildArrayStore(kind, new XirAssembler(kind), true, kind == CiKind.Object, kind == CiKind.Object);

                newArrayTemplates[index] = buildNewArray(kind);
            }
            invokeVirtualTemplates[index] = buildResolvedInvokeVirtual(kind);
            invokeInterfaceTemplates[index] = buildInvokeInterface(kind);
            invokeSpecialTemplates[index] = buildInvokeSpecial(kind);
            invokeStaticTemplates[index] = buildInvokeStatic(kind);
        }

        multiNewArrayTemplate = new XirPair[MAX_MULTIANEWARRAY_RANK + 1];

        for (int i = 1; i < MAX_MULTIANEWARRAY_RANK + 1; i++) {
            multiNewArrayTemplate[i] = buildNewMultiArray(i);
        }

        resolveClassTemplate = buildResolveClassObject();

        safepointTemplate = buildSafepoint();
        monitorEnterTemplate = buildMonitorEnter();
        monitorExitTemplate = buildMonitorExit();

        newInstanceTemplate = buildNewInstance();

        checkcastForLeafTemplate = buildCheckcastForLeaf(false);
        checkcastForClassTemplate = buildCheckcastForInterface(false); // XXX: more efficient template for class checks
        checkcastForInterfaceTemplate = buildCheckcastForInterface(false);

        instanceofForLeafTemplate = buildInstanceofForLeaf(false);
        instanceofForClassTemplate = buildInstanceofForInterface(false); // XXX: more efficient template for class checks
        instanceofForInterfaceTemplate = buildInstanceofForInterface(false);

        offsetOfFirstArrayElement = Layout.byteArrayLayout().getElementOffsetFromOrigin(0).toInt();
    }

    @Override
    public XirSnippet genResolveClassObject(RiType type) {
        return new XirSnippet(resolveClassTemplate, XirArgument.forObject(guardFor(type)));
    }

    @Override
    public XirSnippet genInvokeInterface(XirArgument receiver, RiMethod method) {
        XirPair pair = invokeInterfaceTemplates[method.signatureType().returnBasicType().ordinal()];
        if (method.isLoaded()) {
            InterfaceMethodActor methodActor = ((MaxRiMethod) method).asInterfaceMethodActor("invokeinterface");
            XirArgument interfaceID = XirArgument.forInt(methodActor.holder().id);
            XirArgument methodIndex = XirArgument.forInt(methodActor.iIndexInInterface());
            return new XirSnippet(pair.resolved, receiver, interfaceID, methodIndex);
        }
        XirArgument guard = XirArgument.forObject(guardFor(method));
        return new XirSnippet(pair.unresolved, receiver, guard);
    }

    @Override
    public XirSnippet genInvokeVirtual(XirArgument receiver, RiMethod method) {
        XirPair pair = invokeVirtualTemplates[method.signatureType().returnBasicType().ordinal()];
        if (method.isLoaded()) {
            VirtualMethodActor methodActor = ((MaxRiMethod) method).asVirtualMethodActor("invokevirtual");
            XirArgument vtableIndex = XirArgument.forInt(methodActor.vTableIndex());
            return new XirSnippet(pair.resolved, receiver, vtableIndex);
        }
        XirArgument guard = XirArgument.forObject(guardFor(method));
        return new XirSnippet(pair.unresolved, receiver, guard);
    }

    @Override
    public XirSnippet genInvokeSpecial(XirArgument receiver, RiMethod method) {
        return genInvokeDirect(method, invokeSpecialTemplates);
    }

    @Override
    public XirSnippet genInvokeStatic(RiMethod method) {
        return genInvokeDirect(method, invokeStaticTemplates);
    }

    @Override
    public XirSnippet genMonitorEnter(XirArgument receiver) {
        return new XirSnippet(monitorEnterTemplate, receiver);
    }

    @Override
    public XirSnippet genMonitorExit(XirArgument receiver) {
        return new XirSnippet(monitorExitTemplate, receiver);
    }

    @Override
    public XirSnippet genGetField(XirArgument receiver, RiField field, char cpi, RiConstantPool constantPool) {
        XirPair pair = getFieldTemplates[field.basicType().ordinal()];
        if (field.isLoaded()) {
            XirArgument offset = XirArgument.forInt(field.offset());
            return new XirSnippet(pair.resolved, receiver, offset);
        }
        XirArgument guard = XirArgument.forObject(guardFor(field));
        return new XirSnippet(pair.unresolved, receiver, guard);
    }

    @Override
    public XirSnippet genPutField(XirArgument receiver, XirArgument value, RiField field, char cpi, RiConstantPool constantPool) {
        XirPair pair = putFieldTemplates[field.basicType().ordinal()];
        if (field.isLoaded()) {
            XirArgument offset = XirArgument.forInt(field.offset());
            return new XirSnippet(pair.resolved, receiver, value, offset);
        }
        XirArgument guard = XirArgument.forObject(guardFor(field));
        return new XirSnippet(pair.unresolved, receiver, value, guard);
    }

    @Override
    public XirSnippet genGetStatic(RiField field) {
        XirPair template = getStaticTemplates[field.basicType().ordinal()];
        if (field.isLoaded()) {
            XirArgument offset = XirArgument.forInt(field.offset());
            Object tuple = ((MaxRiField) field).fieldActor.holder().staticTuple();
            return new XirSnippet(template.resolved, XirArgument.forObject(tuple), offset);
        }
        XirArgument guard = XirArgument.forObject(guardFor(field));
        return new XirSnippet(template.unresolved, guard);
    }

    @Override
    public XirSnippet genPutStatic(XirArgument value, RiField field) {
        XirPair template = putStaticTemplates[field.basicType().ordinal()];
        if (field.isLoaded()) {
            XirArgument offset = XirArgument.forInt(field.offset());
            Object tuple = ((MaxRiField) field).fieldActor.holder().staticTuple();
            return new XirSnippet(template.resolved, XirArgument.forObject(tuple), value, offset);
        }
        XirArgument guard = XirArgument.forObject(guardFor(field));
        return new XirSnippet(template.unresolved, value, guard);
    }

    @Override
    public XirSnippet genNewInstance(RiType type) {
        if (type.isLoaded() && type.isInitialized()) {
            return new XirSnippet(newInstanceTemplate.resolved, XirArgument.forObject(hubFor(type)));
        }
        XirArgument guard = XirArgument.forObject(guardFor(type));
        return new XirSnippet(newInstanceTemplate.unresolved, guard);
    }

    private DynamicHub hubFor(RiType type) {
        return ((MaxRiType) type).asClassActor("new instance").dynamicHub();
    }

    @Override
    public XirSnippet genNewArray(XirArgument length, RiType elementType) {
        XirPair pair = newArrayTemplates[elementType.basicType().ordinal()];
        if (elementType.isLoaded()) {
            Object hub = hubFor(elementType.arrayOf());
            return new XirSnippet(pair.resolved, XirArgument.forObject(hub));
        }
        XirArgument guard = XirArgument.forObject(guardFor(elementType));
        return new XirSnippet(pair.unresolved, guard);
    }

    @Override
    public XirSnippet genNewMultiArray(XirArgument[] lengths, RiType type) {
        int rank = lengths.length;
        if (!type.isLoaded() || rank >= SMALL_MULTIANEWARRAY_RANK) {
            XirArgument guard = XirArgument.forObject(guardFor(type));
            return new XirSnippet(multiNewArrayTemplate[rank].resolved, Arrays.prepend(lengths, guard));
        }
        XirArgument hub = XirArgument.forObject(hubFor(type));
        return new XirSnippet(multiNewArrayTemplate[rank].resolved, Arrays.prepend(lengths, hub));
    }

    @Override
    public XirSnippet genCheckCast(XirArgument receiver, RiType type) {
        if (type.isLoaded()) {
            XirTemplate template;
            if (type.isInterface()) {
                template = checkcastForInterfaceTemplate.resolved;
            } else if (type.isFinal()) {
                template = checkcastForLeafTemplate.resolved;
            } else {
                template = checkcastForClassTemplate.resolved;
            }
            XirArgument hub = XirArgument.forObject(hubFor(type));
            return new XirSnippet(template, receiver, hub);
        }
        XirArgument guard = XirArgument.forObject(guardFor(type));
        return new XirSnippet(checkcastForInterfaceTemplate.unresolved, receiver, guard);
    }

    @Override
    public XirSnippet genInstanceOf(XirArgument receiver, RiType type) {
        if (type.isLoaded()) {
            XirTemplate template;
            if (type.isInterface()) {
                template = instanceofForInterfaceTemplate.resolved;
            } else if (type.isFinal()) {
                template = instanceofForLeafTemplate.resolved;
            } else {
                template = instanceofForClassTemplate.resolved;
            }
            XirArgument hub = XirArgument.forObject(hubFor(type));
            return new XirSnippet(template, receiver, hub);
        }
        XirArgument guard = XirArgument.forObject(guardFor(type));
        return new XirSnippet(instanceofForInterfaceTemplate.unresolved, receiver, guard);
    }

    @Override
    public XirSnippet genArrayLoad(XirArgument array, XirArgument index, XirArgument length, RiType elementType) {
        XirTemplate template = arrayLoadTemplates[elementType.basicType().ordinal()];
        return new XirSnippet(template, array, index);
    }

    @Override
    public XirSnippet genArrayStore(XirArgument array, XirArgument index, XirArgument length, XirArgument value, RiType elementType) {
        XirTemplate template = arrayStoreTemplates[elementType.basicType().ordinal()];
        return new XirSnippet(template, array, index, value);
    }

    private XirSnippet genInvokeDirect(RiMethod method, XirPair[] templateArray) {
        XirPair pair = templateArray[method.signatureType().returnBasicType().ordinal()];
        if (method.isLoaded()) {
            return new XirSnippet(pair.resolved, XirArgument.forWord(0));
        }
        XirArgument guard = XirArgument.forObject(guardFor(method));
        return new XirSnippet(pair.unresolved, guard);
    }


    private ResolutionGuard guardFor(RiField field) {
        // XXX: cache resolution guards
        MaxRiField m = (MaxRiField) field;
        return new ResolutionGuard(m.constantPool.constantPool, m.cpi);
    }

    private ResolutionGuard guardFor(RiMethod method) {
        // XXX: cache resolution guards
        MaxRiMethod m = (MaxRiMethod) method;
        return new ResolutionGuard(m.constantPool.constantPool, m.cpi);
    }

    private ResolutionGuard guardFor(RiType type) {
        // XXX: cache resolution guards
        MaxRiType m = (MaxRiType) type;
        return new ResolutionGuard(m.constantPool.constantPool, m.cpi);
    }

    private XirTemplate buildResolveClassObject() {
        XirAssembler asm = new XirAssembler(CiKind.Object);
        XirParameter guard = asm.createConstantInputParameter(CiKind.Object);
        XirCache classObject = asm.createCache(CiKind.Object, false);
        XirLabel resolve = asm.createOutOfLineLabel();
        asm.jeq(resolve, classObject, asm.o(null));
        asm.mov(asm.getResultOperand(), classObject);
        asm.end();
        asm.bindOutOfLine(resolve);
        callRuntimeThroughStub(asm, "resolveClassObject", classObject, guard);
        return asm.finishTemplate();
    }

    private XirTemplate buildSafepoint() {
        XirAssembler asm = new XirAssembler(CiKind.Void);
        XirVariable param = asm.createRegister(CiKind.Word, X86.r14);
        asm.pload(CiKind.Word, param, param);
        return asm.finishTemplate();
    }

    private XirTemplate buildArrayStore(CiKind kind, XirAssembler asm, boolean genBoundsCheck, boolean genStoreCheck, boolean genWriteBarrier) {
        XirParameter array = asm.createInputParameter(CiKind.Object);
        XirParameter index = asm.createInputParameter(CiKind.Int);
        XirParameter value = asm.createInputParameter(kind);
        XirTemp length = asm.createTemp(CiKind.Int);
        XirTemp valueHub = null;
        XirTemp compHub = null;
        XirLabel store = asm.createInlineLabel();
        XirLabel failBoundsCheck = null;
        XirLabel slowStoreCheck = null;
        if (genBoundsCheck) {
            // load the array length and check the index
            failBoundsCheck = asm.createOutOfLineLabel();
            asm.pload(CiKind.Int, length, array, asm.i(arrayLengthOffset));
            asm.jugteq(failBoundsCheck, index, length);
        }
        if (genStoreCheck) {
            slowStoreCheck = asm.createOutOfLineLabel();
            asm.jeq(store, value, asm.o(null)); // first check if value is null
            valueHub = asm.createTemp(CiKind.Object);
            compHub = asm.createTemp(CiKind.Object);
            asm.pload(CiKind.Object, compHub, array, asm.i(hubOffset));
            asm.pload(CiKind.Object, compHub, compHub, asm.i(hub_componentHub));
            asm.pload(CiKind.Object, valueHub, value, asm.i(hubOffset));
            asm.jneq(slowStoreCheck, compHub, valueHub); // then check component hub matches value hub
        }
        asm.bindInline(store);
        int elemSize = target.sizeInBytes(kind);
        if (elemSize > 1) {
            asm.shl(index, index, asm.i(Util.log2(elemSize)));
        }
        asm.add(index, index, asm.i(offsetOfFirstArrayElement));
        asm.pstore(kind, array, index, value);
        if (genWriteBarrier) {
            addWriteBarrier(asm, array, value);
        }
        asm.end();
        if (genBoundsCheck) {
            asm.bindOutOfLine(failBoundsCheck);
            callRuntimeThroughStub(asm, "throwArrayIndexOutOfBoundsException", null, index);
            asm.end();
        }
        if (genStoreCheck) {
            asm.bindOutOfLine(slowStoreCheck);
            callRuntimeThroughStub(asm, "arrayHubStoreCheck", null, compHub, valueHub);
            asm.end();
        }
        return asm.finishTemplate();
    }

    private XirTemplate buildArrayLoad(CiKind kind, XirAssembler asm, boolean genBoundsCheck) {
        XirParameter array = asm.createInputParameter(CiKind.Object);
        XirParameter index = asm.createInputParameter(CiKind.Int);
        XirTemp length = asm.createTemp(CiKind.Int);
        XirParameter result = asm.getResultOperand();
        XirLabel fail = null;
        if (genBoundsCheck) {
            // load the array length and check the index
            fail = asm.createOutOfLineLabel();
            asm.pload(CiKind.Int, length, array, asm.i(arrayLengthOffset));
            asm.jugteq(fail, index, length);
        }
        int elemSize = target.sizeInBytes(kind);
        if (elemSize > 1) {
            asm.shl(index, index, asm.i(Util.log2(elemSize)));
        }
        asm.add(index, index, asm.i(offsetOfFirstArrayElement));
        asm.pload(kind, result, array, index);
        asm.end();
        if (genBoundsCheck) {
            asm.bindOutOfLine(fail);
            callRuntimeThroughStub(asm, "throwArrayIndexOutOfBoundsException", null, index);
        }
        return asm.finishTemplate();
    }

    private XirPair buildInvokeStatic(CiKind kind) {
        XirTemplate resolved;
        XirTemplate unresolved;
        {
            // resolved invokestatic template
            XirAssembler asm = new XirAssembler(kind);
            XirParameter addr = asm.createConstantInputParameter(CiKind.Word);
            asm.callJava(asm.getResultOperand(), addr);
            resolved = asm.finishTemplate();
        }
        {
            // unresolved invokestatic template
            XirAssembler asm = new XirAssembler(kind);
            XirParameter guard = asm.createConstantInputParameter(CiKind.Object);
            XirCache addr = asm.createCache(CiKind.Word, false);
            XirLabel call = asm.createInlineLabel();
            XirLabel resolve = asm.createOutOfLineLabel();
            asm.jeq(resolve, addr, asm.w(0));
            asm.bindInline(call);
            asm.callJava(asm.getResultOperand(), addr);
            asm.end();
            asm.bindOutOfLine(resolve);
            callRuntimeThroughStub(asm, "resolveStaticMethod", addr, guard);
            asm.jmp(call);
            unresolved = asm.finishTemplate();
        }
        return new XirPair(resolved, unresolved);
    }

    private XirPair buildInvokeSpecial(CiKind kind) {
        XirTemplate resolved;
        XirTemplate unresolved;
        {
            // resolved case
            XirAssembler asm = new XirAssembler(kind);
            XirParameter addr = asm.createConstantInputParameter(CiKind.Word); // address to call
            asm.callJava(asm.getResultOperand(), addr);
            resolved = asm.finishTemplate();
        }
        {
            // unresolved invokespecial template
            XirAssembler asm = new XirAssembler(kind);
            XirParameter guard = asm.createConstantInputParameter(CiKind.Object);
            XirCache addr = asm.createCache(CiKind.Word, false);
            XirLabel call = asm.createInlineLabel();
            XirLabel resolve = asm.createOutOfLineLabel();
            asm.jeq(resolve, addr, asm.w(0));
            asm.bindInline(call);
            asm.callJava(asm.getResultOperand(), addr);
            asm.end();
            asm.bindOutOfLine(resolve);
            callRuntimeThroughStub(asm, "resolveSpecialMethod", addr, guard);
            asm.jmp(call);
            unresolved = asm.finishTemplate();
        }
        return new XirPair(resolved, unresolved);
    }

    private XirPair buildInvokeInterface(CiKind kind) {
        XirTemplate resolved;
        XirTemplate unresolved;
        {
            // resolved invokeinterface
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
            // unresolved invokeinterface
            XirAssembler asm = new XirAssembler(kind);
            XirParameter receiver = asm.createInputParameter(CiKind.Object); // receiver object
            XirParameter guard = asm.createInputParameter(CiKind.Object); // guard
            XirCache interfaceID = asm.createCache(CiKind.Int, false);
            XirCache methodIndex = asm.createCache(CiKind.Int, false);
            XirTemp hub = asm.createTemp(CiKind.Object);
            XirTemp mtableLength = asm.createTemp(CiKind.Int);
            XirTemp mtableStartIndex = asm.createTemp(CiKind.Int);
            XirTemp a = asm.createTemp(CiKind.Int);
            XirTemp addr = asm.createTemp(CiKind.Word);
            XirLabel call = asm.createInlineLabel();
            XirLabel resolve = asm.createOutOfLineLabel();
            asm.jeq(resolve, interfaceID, asm.i(0));
            asm.bindInline(call);
            asm.pload(CiKind.Object, hub, receiver, asm.i(hubOffset));
            asm.pload(CiKind.Int, mtableLength, hub, asm.i(hub_mTableLength));
            asm.pload(CiKind.Int, mtableStartIndex, hub, asm.i(hub_mTableStartIndex));
            asm.mod(a, interfaceID, mtableLength);
            asm.add(a, a, mtableStartIndex);
            asm.add(a, a, methodIndex);
            asm.mul(a, a, asm.i(wordSize));
            asm.pload(CiKind.Word, a, hub, a);
            asm.callJava(asm.getResultOperand(), addr);
            asm.end();
            asm.bindOutOfLine(resolve);
            callRuntimeThroughStub(asm, "resolveInterfaceMethod", methodIndex, guard);
            callRuntimeThroughStub(asm, "resolveInterfaceID", interfaceID, guard);
            asm.jmp(call);
            unresolved = asm.finishTemplate();
        }
        return new XirPair(resolved, unresolved);
    }

    private XirPair buildResolvedInvokeVirtual(CiKind kind) {
        XirTemplate resolved;
        XirTemplate unresolved;
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
            // unresolved invokevirtual template
            XirAssembler asm = new XirAssembler(kind);
            XirParameter guard = asm.createConstantInputParameter(CiKind.Object);
            XirParameter receiver = asm.createInputParameter(CiKind.Object); // receiver object
            XirCache vtableOffset = asm.createCache(CiKind.Int, false);
            XirLabel call = asm.createInlineLabel();
            XirLabel resolve = asm.createOutOfLineLabel();
            asm.jeq(resolve, vtableOffset, asm.w(0));
            asm.bindInline(call);
            XirTemp hub = asm.createTemp(CiKind.Object);
            XirTemp addr = asm.createTemp(CiKind.Word);
            asm.pload(CiKind.Object, hub, receiver, asm.i(hubOffset));
            asm.pload(CiKind.Word, addr, hub, vtableOffset);
            asm.callJava(asm.getResultOperand(), addr);
            asm.end();
            asm.bindOutOfLine(resolve);
            callRuntimeThroughStub(asm, "resolveVirtualMethod", vtableOffset, guard);
            asm.jmp(call);
            unresolved = asm.finishTemplate();
        }
        return new XirPair(resolved, unresolved);
    }

    private XirPair buildNewArray(CiKind kind) {
        XirTemplate resolved;
        XirTemplate unresolved;
        if (kind == CiKind.Object) {
            {
                // resolved new object array
                XirAssembler asm = new XirAssembler(CiKind.Object);
                XirParameter hub = asm.createConstantInputParameter(CiKind.Object);
                XirParameter length = asm.createInputParameter(CiKind.Int);
                callRuntimeThroughStub(asm, "allocateObjectArray", asm.getResultOperand(), hub, length);
                resolved = asm.finishTemplate();
            }
            {
                // unresolved new object array
                XirAssembler asm = new XirAssembler(CiKind.Object);
                XirParameter guard = asm.createConstantInputParameter(CiKind.Object);
                XirParameter length = asm.createInputParameter(CiKind.Int);
                XirCache hub = asm.createCache(CiKind.Object, false);
                XirLabel resolve = asm.createOutOfLineLabel();
                XirLabel alloc = asm.createInlineLabel();
                asm.jeq(resolve, hub, asm.o(null));
                asm.bindInline(alloc);
                callRuntimeThroughStub(asm, "allocateObjectArray", asm.getResultOperand(), hub, length);
                asm.end();
                asm.bindOutOfLine(resolve);
                callRuntimeThroughStub(asm, "resolveNewArray", hub, guard);
                asm.jmp(alloc);
                unresolved = asm.finishTemplate();
            }

        } else {
            // XXX: specialized, inline templates for each kind
            XirAssembler asm = new XirAssembler(CiKind.Object);
            XirParameter hub = asm.createConstantInputParameter(CiKind.Object);
            XirParameter length = asm.createInputParameter(CiKind.Int);
            callRuntimeThroughStub(asm, "allocatePrimitiveArray", asm.getResultOperand(), hub, length);
            resolved = asm.finishTemplate();
            unresolved = resolved;
        }
        return new XirPair(resolved, unresolved);
    }

    private XirPair buildNewMultiArray(int rank) {
        XirTemplate resolved = null;
        XirTemplate unresolved;
        if (rank < SMALL_MULTIANEWARRAY_RANK) {
            // "small" resolved multianewarray (rank 3 or less)
            XirAssembler asm = new XirAssembler(CiKind.Object);
            XirParameter[] lengths = new XirParameter[rank];
            for (int i = 0; i < rank; i++) {
                lengths[i] = asm.createInputParameter(CiKind.Int);
            }
            XirParameter hub = asm.createConstantInputParameter(CiKind.Object);
            callRuntimeThroughStub(asm, "allocateMultiArray" + rank, asm.getResultOperand(), Arrays.prepend(lengths, hub));
            resolved = asm.finishTemplate();
        }

        // unresolved or large multianewarray
        XirAssembler asm = new XirAssembler(CiKind.Object);
        XirParameter[] lengths = new XirParameter[rank];
        for (int i = 0; i < rank; i++) {
            lengths[i] = asm.createInputParameter(CiKind.Int);
        }
        XirParameter guard = asm.createConstantInputParameter(CiKind.Object);
        XirTemp lengthArray = asm.createTemp(CiKind.Object);
        callRuntimeThroughStub(asm, "allocateIntArray", lengthArray, asm.i(rank));
        for (int i = 0; i < rank; i++) {
            asm.pstore(CiKind.Int, lengthArray, asm.i(offsetOfFirstArrayElement + i * target.sizeInBytes(CiKind.Int)), lengths[i]);
        }
        callRuntimeThroughStub(asm, "allocateMultiArrayN", asm.getResultOperand(), guard, lengthArray);
        unresolved = asm.finishTemplate();

        return new XirPair(resolved == null ? unresolved : resolved, unresolved);
    }

    private XirPair buildNewInstance() {
        XirTemplate resolved;
        XirTemplate unresolved;
        {
            // resolved new instance
            XirAssembler asm = new XirAssembler(CiKind.Object);
            XirParameter hub = asm.createConstantInputParameter(CiKind.Object);
            callRuntimeThroughStub(asm, "allocateObject", asm.getResultOperand(), hub);
            resolved = asm.finishTemplate();
        }
        {
            // unresolved new instance
            XirAssembler asm = new XirAssembler(CiKind.Object);
            XirParameter guard = asm.createConstantInputParameter(CiKind.Object);
            XirCache hub = asm.createCache(CiKind.Object, false);
            XirLabel resolve = asm.createOutOfLineLabel();
            XirLabel alloc = asm.createInlineLabel();
            asm.jeq(resolve, hub, asm.o(null));
            asm.bindInline(alloc);
            callRuntimeThroughStub(asm, "allocateObject", asm.getResultOperand(), hub);
            asm.end();
            asm.bindOutOfLine(resolve);
            callRuntimeThroughStub(asm, "resolveNew", hub, guard);
            asm.jmp(alloc);
            unresolved = asm.finishTemplate();
        }

        return new XirPair(resolved, unresolved);
    }

    private XirPair buildPutFieldTemplate(CiKind kind, boolean genWriteBarrier) {
        XirTemplate resolved;
        XirTemplate unresolved;
        {
            // resolved case
            XirAssembler asm = new XirAssembler(CiKind.Void);
            XirParameter object = asm.createInputParameter(CiKind.Object);
            XirParameter value = asm.createInputParameter(kind);
            XirParameter fieldOffset = asm.createConstantInputParameter(CiKind.Int);
            asm.pstore(kind, object, fieldOffset, value);
            if (genWriteBarrier) {
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
            asm.bindInline(put);
            asm.pstore(kind, object, fieldOffset, value);
            if (genWriteBarrier) {
                addWriteBarrier(asm, object, value);
            }
            asm.end();
            asm.bindOutOfLine(resolve);
            callRuntimeThroughStub(asm, "resolvePutField", fieldOffset, guard);
            asm.jmp(put);
            unresolved = asm.finishTemplate();
        }
        return new XirPair(resolved, unresolved);
    }

    private XirPair buildGetFieldTemplate(CiKind kind) {
        XirTemplate resolved;
        XirTemplate unresolved;
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
            asm.bindInline(get);
            asm.pload(kind, resultOperand, object, fieldOffset);
            asm.end();
            asm.bindOutOfLine(resolve);
            callRuntimeThroughStub(asm, "resolveGetField", fieldOffset, guard);
            asm.jmp(get);
            unresolved = asm.finishTemplate();
        }
        return new XirPair(resolved, unresolved);
    }

    private XirPair buildPutStaticTemplate(CiKind kind, boolean genWriteBarrier) {
        XirTemplate resolved;
        XirTemplate unresolved;
        {
            // XXX: this is identical to put field, except the tuple is a constant
            XirAssembler asm = new XirAssembler(CiKind.Void);
            XirParameter tuple = asm.createConstantInputParameter(CiKind.Object);
            XirParameter value = asm.createInputParameter(kind);
            XirParameter fieldOffset = asm.createConstantInputParameter(CiKind.Int);
            asm.pstore(kind, tuple, fieldOffset, value);
            if (genWriteBarrier) {
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
            asm.jeq(resolve, tuple, asm.w(0));
            asm.bindInline(put);
            asm.pstore(kind, tuple, fieldOffset, value);
            if (genWriteBarrier) {
                addWriteBarrier(asm, tuple, value);
            }
            asm.end();
            asm.bindOutOfLine(resolve);
            callRuntimeThroughStub(asm, "resolvePutStatic", fieldOffset, guard);
            callRuntimeThroughStub(asm, "resolveStaticTuple", tuple, guard);
            asm.jmp(put);
            unresolved = asm.finishTemplate();
        }
        return new XirPair(resolved, unresolved);
    }

    private XirPair buildGetStaticTemplate(CiKind kind) {
        XirTemplate resolved;
        XirTemplate unresolved;
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
            asm.jeq(resolve, tuple, asm.w(0));
            asm.bindInline(get);
            asm.pload(kind, resultOperand, tuple, fieldOffset);
            asm.end();
            asm.bindOutOfLine(resolve);
            callRuntimeThroughStub(asm, "resolveGetStatic", fieldOffset, guard);
            callRuntimeThroughStub(asm, "resolveStaticTuple", tuple, guard);
            asm.jmp(get);
            unresolved = asm.finishTemplate();
        }
        return new XirPair(resolved, unresolved);
    }

    private XirTemplate buildMonitorExit() {
        XirAssembler asm = new XirAssembler(CiKind.Void);
        XirParameter object = asm.createInputParameter(CiKind.Object);
        callRuntimeThroughStub(asm, "monitorExit", null, object);
        return asm.finishTemplate();
    }

    private XirTemplate buildMonitorEnter() {
        XirAssembler asm = new XirAssembler(CiKind.Void);
        XirParameter object = asm.createInputParameter(CiKind.Object);
        callRuntimeThroughStub(asm, "monitorEnter", null, object);
        return asm.finishTemplate();
    }

    private XirPair buildCheckcastForLeaf(boolean nonnull) {
        XirTemplate resolved;
        XirTemplate unresolved;
        {
            // resolved checkcast for a leaf class
            XirAssembler asm = new XirAssembler(CiKind.Object);
            XirParameter object = asm.createInputParameter(CiKind.Object);
            XirParameter hub = asm.createConstantInputParameter(CiKind.Object);
            XirTemp temp = asm.createTemp(CiKind.Object);
            XirLabel pass = asm.createInlineLabel();
            XirLabel fail = asm.createOutOfLineLabel();
            if (!nonnull) {
                // first check against null
                asm.jeq(pass, object, asm.o(null));
            }
            asm.pload(CiKind.Object, temp, object, asm.i(hubOffset));
            asm.jneq(fail, hub, temp);
            asm.bindInline(pass);
            asm.end();
            asm.bindOutOfLine(fail);
            callRuntimeThroughStub(asm, "throwClassCastException", null);
            resolved = asm.finishTemplate();
        }
        {
            // unresolved checkcast
            unresolved = buildUnresolvedCheckcast(nonnull);
        }
        return new XirPair(resolved, unresolved);
    }

    private XirPair buildCheckcastForInterface(boolean nonnull) {
        XirTemplate resolved;
        XirTemplate unresolved;
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
            // XXX: use a cache to check the last successful receiver type
            if (!nonnull) {
                // first check for null
                asm.jeq(pass, object, asm.o(null));
            }
            asm.pload(CiKind.Object, hub, object, asm.i(hubOffset));
            asm.pload(CiKind.Int, mtableLength, hub, asm.i(hub_mTableLength));
            asm.pload(CiKind.Int, mtableStartIndex, hub, asm.i(hub_mTableStartIndex));
            asm.mod(a, interfaceID, mtableLength);
            asm.add(a, a, mtableStartIndex);
            asm.mul(a, a, asm.i(wordSize));
            asm.pload(CiKind.Int, a, hub, a);
            asm.jneq(fail, a, interfaceID);
            asm.bindInline(pass);
            asm.end();
            asm.bindOutOfLine(fail);
            callRuntimeThroughStub(asm, "throwClassCastException", null);
            resolved = asm.finishTemplate();
        }
        {
            unresolved = buildUnresolvedCheckcast(nonnull);
        }
        return new XirPair(resolved, unresolved);
    }

    private XirTemplate buildUnresolvedCheckcast(boolean nonnull) {
        XirTemplate unresolved;
        XirAssembler asm = new XirAssembler(CiKind.Object);
        XirParameter object = asm.createInputParameter(CiKind.Object);
        XirParameter guard = asm.createInputParameter(CiKind.Object);
        XirCache hub = asm.createCache(CiKind.Object, false);
        XirTemp temp = asm.createTemp(CiKind.Object);
        XirLabel pass = asm.createInlineLabel();
        XirLabel resolve = asm.createOutOfLineLabel();
        if (!nonnull) {
            // XXX: build a version that does not include a null check
            asm.jeq(pass, object, asm.o(null));
        }
        asm.pload(CiKind.Object, temp, object, asm.i(hubOffset));
        asm.jneq(resolve, hub, temp);
        asm.bindInline(pass);
        asm.end();
        asm.bindOutOfLine(resolve);
        callRuntimeThroughStub(asm, "unresolvedCheckcast", hub, object, guard);
        unresolved = asm.finishTemplate();
        return unresolved;
    }

    private XirPair buildInstanceofForLeaf(boolean nonnull) {
        XirTemplate resolved;
        XirTemplate unresolved;
        {
            XirAssembler asm = new XirAssembler(CiKind.Boolean);
            XirParameter result = asm.getResultOperand();
            XirParameter object = asm.createInputParameter(CiKind.Object);
            XirParameter hub = asm.createConstantInputParameter(CiKind.Object);
            XirTemp temp = asm.createTemp(CiKind.Object);
            XirLabel pass = asm.createInlineLabel();
            XirLabel fail = asm.createInlineLabel();
            if (!nonnull) {
                // first check for null
                asm.jeq(pass, object, asm.o(null));
            }
            asm.pload(CiKind.Object, temp, object, asm.i(hubOffset));
            asm.jneq(fail, hub, temp);
            asm.bindInline(pass);
            asm.mov(result, asm.b(true));
            asm.end();
            asm.bindInline(fail);
            asm.mov(result, asm.b(false));
            asm.end();
            resolved = asm.finishTemplate();
        }
        {
            // unresolved instanceof
            unresolved = buildUnresolvedInstanceOf(nonnull);
        }
        return new XirPair(resolved, unresolved);
    }

    private XirPair buildInstanceofForInterface(boolean nonnull) {
        XirTemplate resolved;
        XirTemplate unresolved;
        {
            // resolved instanceof for interface
            XirAssembler asm = new XirAssembler(CiKind.Boolean);
            XirParameter result = asm.getResultOperand();
            XirParameter object = asm.createInputParameter(CiKind.Object);
            XirParameter interfaceID = asm.createConstantInputParameter(CiKind.Int);
            XirTemp hub = asm.createTemp(CiKind.Object);
            XirTemp mtableLength = asm.createTemp(CiKind.Int);
            XirTemp mtableStartIndex = asm.createTemp(CiKind.Int);
            XirTemp a = asm.createTemp(CiKind.Int);
            XirLabel pass = asm.createInlineLabel();
            XirLabel fail = asm.createInlineLabel();
            // XXX: use a cache to check the last successful receiver type
            if (!nonnull) {
                // first check for null
                asm.jeq(fail, object, asm.o(null));
            }
            asm.pload(CiKind.Object, hub, object, asm.i(hubOffset));
            asm.pload(CiKind.Int, mtableLength, hub, asm.i(hub_mTableLength));
            asm.pload(CiKind.Int, mtableStartIndex, hub, asm.i(hub_mTableStartIndex));
            asm.mod(a, interfaceID, mtableLength);
            asm.add(a, a, mtableStartIndex);
            asm.mul(a, a, asm.i(wordSize));
            asm.pload(CiKind.Int, a, hub, a);
            asm.jneq(fail, a, interfaceID);
            asm.bindInline(pass);
            asm.mov(result, asm.b(true));
            asm.end();
            asm.bindInline(fail);
            asm.mov(result, asm.b(false));
            asm.end();
            resolved = asm.finishTemplate();
        }
        {
            // unresolved instanceof
            unresolved = buildUnresolvedInstanceOf(nonnull);
        }
        return new XirPair(resolved, unresolved);
    }

    private XirTemplate buildUnresolvedInstanceOf(boolean nonnull) {
        XirTemplate unresolved;
        XirAssembler asm = new XirAssembler(CiKind.Boolean);
        XirParameter object = asm.createInputParameter(CiKind.Object);
        XirParameter guard = asm.createInputParameter(CiKind.Object);
        XirCache hub = asm.createCache(CiKind.Object, false);
        XirTemp temp = asm.createTemp(CiKind.Object);
        XirLabel pass = asm.createInlineLabel();
        XirLabel resolve = asm.createOutOfLineLabel();
        XirLabel fail = null;
        if (!nonnull) {
            // first check failed
            fail = asm.createInlineLabel();
            asm.jeq(fail, object, asm.o(null));
        }
        asm.pload(CiKind.Object, temp, object, asm.i(hubOffset));
        asm.jneq(resolve, hub, temp);
        asm.bindInline(pass);
        // quick check passed
        asm.mov(asm.getResultOperand(), asm.b(true));
        asm.end();
        if (!nonnull) {
            // null check failed
            asm.bindInline(fail);
            asm.mov(asm.getResultOperand(), asm.b(false));
            asm.end();
        }
        asm.bindOutOfLine(resolve);
        callRuntimeThroughStub(asm, "resolveHub", hub, guard);
        callRuntimeThroughStub(asm, "unresolvedInstanceOf", asm.getResultOperand(), object, guard);
        asm.end();
        unresolved = asm.finishTemplate();
        return unresolved;
    }



    private void addWriteBarrier(XirAssembler asm, XirVariable object, XirVariable value) {
        // XXX: add write barrier mechanism
    }

    private CiKind toCiKind(Kind k) {
        return kindMapping[k.asEnum.ordinal()];
    }

    private void callRuntimeThroughStub(XirAssembler asm, String method, XirVariable result, XirVariable... args) {
        XirTemplate stub = runtimeCallStubs.get(method);
        if (stub == null) {
            // search for the runtime call and create the stub
            for (Method m : RuntimeCalls.class.getDeclaredMethods()) {
                int flags = m.getModifiers();
                if (Modifier.isStatic(flags) && Modifier.isPublic(flags) && m.getName().equals(method)) {
                    // runtime call found. create a global stub that calls the runtime method
                    MethodActor methodActor = MethodActor.fromJava(m);
                    SignatureDescriptor signature = methodActor.descriptor();
                    if (result == null) {
                        assert signature.resultKind() == Kind.VOID;
                    } else {
                        CiKind ciKind = toCiKind(signature.resultKind());
                        assert ciKind == result.kind : "return type mismatch in call to " + method;
                    }

                    assert signature.numberOfParameters() == args.length : "parameter mismatch in call to " + method;
                    XirAssembler stubAsm = new XirAssembler(toCiKind(signature.resultKind()));
                    XirParameter[] rtArgs = new XirParameter[signature.numberOfParameters()];
                    for (int i = 0; i < signature.numberOfParameters(); i++) {
                        // create a parameter for each parameter to the runtime call
                        CiKind ciKind = toCiKind(signature.parameterDescriptorAt(i).toKind());
                        assert ciKind == args[i].kind : "type mismatch in call to " + method;
                        rtArgs[i] = stubAsm.createInputParameter(ciKind);
                    }
                    stubAsm.callRuntime(methodActor, stubAsm.getResultOperand(), rtArgs);
                    stub = stubAsm.finishStub();
                    runtimeCallStubs.put(method, stub);
                }
            }
        }
        if (stub == null) {
            throw ProgramError.unexpected("could not find runtime call: " + method);
        }
        asm.callStub(stub, result, args);
    }

    public static class RuntimeCalls {
        public static Class resolveClassObject(ResolutionGuard guard) {
            return ResolutionSnippet.ResolveClass.resolveClass(guard).mirror();
        }

        public static Object resolveHub(ResolutionGuard guard) {
            return ResolutionSnippet.ResolveClass.resolveClass(guard).dynamicHub();
        }

        public static Object resolveNew(ResolutionGuard guard) {
            return ResolutionSnippet.ResolveClassForNew.resolveClassForNew(guard).dynamicHub();
        }

        public static Object resolveNewArray(ResolutionGuard guard) {
            return ResolutionSnippet.ResolveArrayClass.resolveArrayClass(guard).dynamicHub();
        }

        public static int resolveGetField(ResolutionGuard guard) {
            return ResolutionSnippet.ResolveInstanceFieldForReading.resolveInstanceFieldForReading(guard).offset();
        }

        public static int resolvePutField(ResolutionGuard guard) {
            return ResolutionSnippet.ResolveInstanceFieldForWriting.resolveInstanceFieldForWriting(guard).offset();
        }

        public static int resolveGetStatic(ResolutionGuard guard) {
            return ResolutionSnippet.ResolveStaticFieldForReading.resolveStaticFieldForReading(guard).offset();
        }

        public static int resolvePutStatic(ResolutionGuard guard) {
            return ResolutionSnippet.ResolveStaticFieldForWriting.resolveStaticFieldForWriting(guard).offset();
        }

        public static Object resolveStaticTuple(ResolutionGuard guard) {
            return ResolutionSnippet.ResolveStaticFieldForReading.resolveStaticFieldForReading(guard).holder().staticTuple();
        }

        public static Word resolveStaticMethod(ResolutionGuard guard) {
            return CompilationScheme.Static.compile(ResolutionSnippet.ResolveStaticMethod.resolveStaticMethod(guard), CallEntryPoint.OPTIMIZED_ENTRY_POINT);
        }

        public static int resolveVirtualMethod(ResolutionGuard guard) {
            return ResolutionSnippet.ResolveVirtualMethod.resolveVirtualMethod(guard).vTableIndex();
        }

        public static Word resolveSpecialMethod(ResolutionGuard guard) {
            return CompilationScheme.Static.compile(ResolutionSnippet.ResolveSpecialMethod.resolveSpecialMethod(guard), CallEntryPoint.OPTIMIZED_ENTRY_POINT);
        }

        public static int resolveInterfaceMethod(ResolutionGuard guard) {
            return ResolutionSnippet.ResolveInterfaceMethod.resolveInterfaceMethod(guard).iIndexInInterface();
        }

        public static int resolveInterfaceID(ResolutionGuard guard) {
            return ResolutionSnippet.ResolveInterfaceMethod.resolveInterfaceMethod(guard).holder().id;
        }

        public static Object allocatePrimitiveArray(DynamicHub hub, int length) {
            if (length < 0) {
                throw new NegativeArraySizeException();
            }
            return Heap.createArray(hub, length);
        }

        public static Object allocateObjectArray(DynamicHub hub, int length) {
            if (length < 0) {
                throw new NegativeArraySizeException();
            }
            return Heap.createArray(hub, length);
        }

        public static Object allocateObject(DynamicHub hub) {
            return Heap.createTuple(hub);
        }

        public static int[] allocateIntArray(int length) {
            return new int[length];
        }

        public static Object allocateMultiArray1(DynamicHub hub, int l1) {
            if (l1 < 0) {
                throw new NegativeArraySizeException();
            }
            return createArray(hub, l1);
        }

        public static Object allocateMultiArray2(DynamicHub hub1, int l1, int l2) {
            if (l1 < 0 | l2 < 0) {
                throw new NegativeArraySizeException();
            }
            Object[] result = UnsafeLoophole.cast(createObjectArray(hub1, l1));
            DynamicHub hub2 = UnsafeLoophole.cast(hub1.componentHub);
            for (int i1 = 0; i1 < l1; i1++) {
                ArrayAccess.setObject(result, i1, createArray(hub2, l2));
            }
            return result;
        }

        public static Object allocateMultiArray3(DynamicHub hub1, int l1, int l2, int l3) {
            if (l1 < 0 | l2 < 0 | l3 < 0) {
                throw new NegativeArraySizeException();
            }
            Object[] result = UnsafeLoophole.cast(createObjectArray(hub1, l1));
            DynamicHub hub2 = UnsafeLoophole.cast(hub1.componentHub);
            DynamicHub hub3 = UnsafeLoophole.cast(hub2.componentHub);
            for (int i1 = 0; i1 < l1; i1++) {
                Object[] result2 = createObjectArray(hub2, l2);
                safeArrayStore(result, i1, result2);
                for (int i2 = 0; i2 < l2; i2++) {
                    safeArrayStore(result2, i2, createArray(hub3, l3));
                }
            }
            return result;
        }

        public static Object allocateMultiArrayN(ResolutionGuard guard, int[] lengths) {
            for (int length : lengths) {
                if (length < 0) {
                    Throw.negativeArraySizeException(length);
                }
            }
            ClassActor actor = ResolutionSnippet.ResolveClass.resolveClass(guard);
            return recursiveNewMultiArray(0, actor, lengths);
        }

        private static Object recursiveNewMultiArray(int index, ClassActor arrayClassActor, int[] lengths) {
            final int length = lengths[index];
            final Object result = createArray(arrayClassActor.dynamicHub(), length);
            if (length > 0) {
                final int nextIndex = index + 1;
                if (nextIndex < lengths.length) {
                    Object[] array = (Object[]) result;
                    final ClassActor subArrayClassActor = arrayClassActor.componentClassActor();
                    for (int i = 0; i < length; i++) {
                        safeArrayStore(array, i, recursiveNewMultiArray(nextIndex, subArrayClassActor, lengths));
                    }
                }
            }
            return result;
        }

        @INLINE
        private static Object[] createObjectArray(DynamicHub hub, int length) {
            if (MaxineVM.isPrototyping()) {
                return (Object[]) Array.newInstance(hub.classActor.componentClassActor().toJava(), length);
            }
            return UnsafeLoophole.cast(Heap.createArray(hub, length));
        }

        @INLINE
        private static Object createArray(DynamicHub hub, int length) {
            if (MaxineVM.isPrototyping()) {
                return Array.newInstance(hub.classActor.componentClassActor().toJava(), length);
            }
            return Heap.createArray(hub, length);
        }

        @INLINE
        private static void safeArrayStore(Object[] array, int index, Object val) {
            if (MaxineVM.isPrototyping()) {
                array[index] = val;
            } else {
                ArrayAccess.setObject(array, index, val);
            }
        }

        public static Hub unresolvedCheckcast(Object object, ResolutionGuard guard) {
            final ClassActor classActor = ResolutionSnippet.ResolveClass.resolveClass(guard);
            if (!ObjectAccess.readHub(object).isSubClassHub(classActor)) {
                throw new ClassCastException();
            }
            return classActor.dynamicHub();
        }

        public static boolean unresolvedInstanceOf(Object object, ResolutionGuard guard) {
            final ClassActor classActor = ResolutionSnippet.ResolveClass.resolveClass(guard);
            return ObjectAccess.readHub(object).isSubClassHub(classActor);
        }

        public static void arrayHubStoreCheck(DynamicHub componentHub, DynamicHub valueHub) {
            if (!valueHub.isSubClassHub(componentHub.classActor)) {
                throw new ArrayStoreException();
            }
        }

        public static void throwClassCastException() {
            throw new ClassCastException();
        }

        public static void throwNullPointerException() {
            throw new NullPointerException();
        }

        public static void throwArrayIndexOutOfBoundsException(int index) {
            throw new ArrayIndexOutOfBoundsException(index);
        }

        public static void monitorEnter(Object o) {
            VMConfiguration.target().monitorScheme().monitorEnter(o);
        }

        public static void monitorExit(Object o) {
            VMConfiguration.target().monitorScheme().monitorExit(o);
        }
    }
}
