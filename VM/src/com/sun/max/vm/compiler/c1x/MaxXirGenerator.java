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

import com.sun.c1x.*;
import com.sun.c1x.ci.*;
import com.sun.c1x.ri.*;
import com.sun.c1x.xir.*;
import com.sun.c1x.xir.CiXirAssembler.*;
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
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.type.Kind;
import com.sun.max.vm.type.KindEnum;
import com.sun.max.vm.type.SignatureDescriptor;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.unsafe.Word;
import com.sun.max.unsafe.UnsafeCast;
import com.sun.max.unsafe.WordArray;
import com.sun.max.program.ProgramError;
import com.sun.max.lang.*;
import com.sun.max.lang.Arrays;
import com.sun.max.annotate.INLINE;
import com.sun.max.annotate.UNSAFE;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Array;
import java.util.*;

/**
 * This class implements the VM interface for generating XIR snippets that express
 * the low-level implementation of each bytecode for C1X compilation.
 *
 * @author Ben L. Titzer
 * @author Thomas Wuerthinger
 */
public class MaxXirGenerator extends RiXirGenerator {

    private static final CiKind[] kindMapping;
    private static final Kind[] ciKindMapping;
    private static final int SMALL_MULTIANEWARRAY_RANK = 2;

    // (tw) TODO: Up this to 255 / make a loop in the template
    private static final int MAX_MULTIANEWARRAY_RANK = 6;

    static void map(KindEnum k, CiKind ck) {
        kindMapping[k.ordinal()] = ck;
        ciKindMapping[ck.ordinal()] = k.asKind();
    }

    static class XirPair {
        final XirTemplate resolved;
        final XirTemplate unresolved;

        XirPair(XirTemplate r, XirTemplate u) {
            resolved = r;
            unresolved = u;
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

    private XirPair[] putFieldTemplates;
    private XirPair[] getFieldTemplates;

    private XirPair[] putStaticTemplates;
    private XirPair[] getStaticTemplates;

    private XirPair[] invokeVirtualTemplates;
    private XirPair[] invokeInterfaceTemplates;
    private XirPair[] invokeSpecialTemplates;
    private XirPair[] invokeStaticTemplates;
    private XirPair[] newArrayTemplates;
    private XirTemplate[] arrayLoadTemplates;
    private XirTemplate[] arrayStoreTemplates;

    private DynamicHub[] arrayHubs;

    private XirPair[] multiNewArrayTemplate;

    private XirTemplate safepointTemplate;
    private XirTemplate arraylengthTemplate;
    private XirTemplate monitorEnterTemplate;
    private XirTemplate monitorExitTemplate;
    private XirTemplate resolveClassTemplate;
    private XirPair newInstanceTemplate;
    private XirPair checkcastForLeafTemplate;
    private XirPair checkcastForClassTemplate;
    private XirPair checkcastForInterfaceTemplate;
    private XirPair instanceofForLeafTemplate;
    private XirPair instanceofForClassTemplate;
    private XirPair instanceofForInterfaceTemplate;

    List<XirTemplate> stubs = new ArrayList<XirTemplate>();

    final int offsetOfFirstArrayElement;
    final int hubOffset;
    final int hub_mTableLength;
    final int hub_mTableStartIndex;
    final int hub_componentHub;
    final int wordSize;
    final int arrayLengthOffset;

    private MaxRiRuntime runtime;

    @UNSAFE
    public MaxXirGenerator(VMConfiguration vmConfiguration, CiTarget target, MaxRiRuntime runtime) {
        this.target = target;
        this.runtime = runtime;
        this.hubOffset = vmConfiguration.layoutScheme().generalLayout.getOffsetFromOrigin(Layout.HeaderField.HUB).toInt();
        this.hub_mTableLength = FieldActor.findInstance(Hub.class, "mTableLength").offset();
        this.hub_mTableStartIndex = FieldActor.findInstance(Hub.class, "mTableStartIndex").offset();
        this.hub_componentHub = FieldActor.findInstance(Hub.class, "componentHub").offset();
        this.wordSize = vmConfiguration.platform.wordWidth().numberOfBytes;
        assert wordSize == target.arch.wordSize : "word size mismatch";
        this.arrayLengthOffset = Layout.arrayHeaderLayout().arrayLengthOffset();
        this.offsetOfFirstArrayElement = Layout.byteArrayLayout().getElementOffsetFromOrigin(0).toInt();
    }

    private CiXirAssembler asm;

    @Override
    public List<XirTemplate> buildTemplates(CiXirAssembler asm) {
        CiKind[] kinds = CiKind.values();

        this.asm = asm;

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

        arrayHubs = new DynamicHub[kinds.length];

        arrayHubs[CiKind.Boolean.ordinal()] = ClassActor.fromJava(boolean[].class).dynamicHub();
        arrayHubs[CiKind.Byte.ordinal()] = ClassActor.fromJava(byte[].class).dynamicHub();
        arrayHubs[CiKind.Short.ordinal()] = ClassActor.fromJava(short[].class).dynamicHub();
        arrayHubs[CiKind.Char.ordinal()] = ClassActor.fromJava(char[].class).dynamicHub();
        arrayHubs[CiKind.Int.ordinal()] = ClassActor.fromJava(int[].class).dynamicHub();
        arrayHubs[CiKind.Float.ordinal()] = ClassActor.fromJava(float[].class).dynamicHub();
        arrayHubs[CiKind.Double.ordinal()] = ClassActor.fromJava(double[].class).dynamicHub();
        arrayHubs[CiKind.Long.ordinal()] = ClassActor.fromJava(long[].class).dynamicHub();
        arrayHubs[CiKind.Object.ordinal()] = ClassActor.fromJava(Object[].class).dynamicHub();
        arrayHubs[CiKind.Word.ordinal()] = ClassActor.fromJava(WordArray.class).dynamicHub();

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

                asm.restart(kind);
                arrayLoadTemplates[index] = buildArrayLoad(kind, asm, true);

                asm.restart(kind);
                arrayStoreTemplates[index] = buildArrayStore(kind, asm, true, kind == CiKind.Object, kind == CiKind.Object);

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
        arraylengthTemplate = buildArrayLength();
        monitorEnterTemplate = buildMonitorEnter();
        monitorExitTemplate = buildMonitorExit();

        newInstanceTemplate = buildNewInstance();

        checkcastForLeafTemplate = buildCheckcastForLeaf(false);
        checkcastForClassTemplate = buildCheckcastForInterface(false); // XXX: more efficient template for class checks
        checkcastForInterfaceTemplate = buildCheckcastForInterface(false);

        instanceofForLeafTemplate = buildInstanceofForLeaf(false);
        instanceofForClassTemplate = buildInstanceofForInterface(false); // XXX: more efficient template for class checks
        instanceofForInterfaceTemplate = buildInstanceofForInterface(false);

        return stubs;
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
            XirArgument vtableOffset = XirArgument.forInt(methodActor.vTableIndex() * wordSize + offsetOfFirstArrayElement);
            return new XirSnippet(pair.resolved, receiver, vtableOffset);
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
    public XirSnippet genGetField(XirArgument receiver, RiField field) {
        XirPair pair = getFieldTemplates[field.kind().ordinal()];
        if (field.isLoaded()) {
            XirArgument offset = XirArgument.forInt(field.offset());
            return new XirSnippet(pair.resolved, receiver, offset);
        }
        XirArgument guard = XirArgument.forObject(guardFor(field));
        return new XirSnippet(pair.unresolved, receiver, guard);
    }

    @Override
    public XirSnippet genPutField(XirArgument receiver, RiField field, XirArgument value) {
        XirPair pair = putFieldTemplates[field.kind().ordinal()];
        if (field.isLoaded()) {
            XirArgument offset = XirArgument.forInt(field.offset());
            return new XirSnippet(pair.resolved, receiver, value, offset);
        }
        XirArgument guard = XirArgument.forObject(guardFor(field));
        return new XirSnippet(pair.unresolved, receiver, value, guard);
    }

    @Override
    public XirSnippet genGetStatic(RiField field) {
        XirPair template = getStaticTemplates[field.kind().ordinal()];
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
        XirPair template = putStaticTemplates[field.kind().ordinal()];
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
    public XirSnippet genNewArray(XirArgument length, CiKind elementKind, RiType arrayType) {
        XirPair pair = newArrayTemplates[elementKind.ordinal()];
        Object hub = arrayHubs[elementKind.ordinal()];
        if (elementKind == CiKind.Object && arrayType.isLoaded()) {
            hub = hubFor(arrayType);
        }
        if (hub != null) {
            return new XirSnippet(pair.resolved, XirArgument.forObject(hub), length);
        }
        XirArgument guard = XirArgument.forObject(guardFor(arrayType));
        return new XirSnippet(pair.unresolved, guard, length);
    }

    @Override
    public XirSnippet genNewMultiArray(XirArgument[] lengths, RiType type) {
        int rank = lengths.length;
        if (!type.isLoaded() || rank >= SMALL_MULTIANEWARRAY_RANK) {
            XirArgument guard = XirArgument.forObject(guardFor(type));
            return new XirSnippet(multiNewArrayTemplate[rank].unresolved, Arrays.append(lengths, guard));
        }
        XirArgument hub = XirArgument.forObject(hubFor(type));
        return new XirSnippet(multiNewArrayTemplate[rank].resolved, Arrays.append(lengths, hub));
    }

    @Override
    public XirSnippet genCheckCast(XirArgument receiver, RiType type) {
        if (type.isLoaded()) {
            XirTemplate template;
            if (type.isInterface()) {
                template = checkcastForInterfaceTemplate.resolved;
                MaxRiType maxType = (MaxRiType) type;
                int interfaceID = maxType.classActor.id;
                return new XirSnippet(template, receiver, XirArgument.forInt(interfaceID));
            } else if (type.isFinal()) {
                template = checkcastForLeafTemplate.resolved;
            } else {
                template = checkcastForClassTemplate.resolved;
                MaxRiType maxType = (MaxRiType) type;
                int interfaceID = maxType.classActor.id;
                return new XirSnippet(template, receiver, XirArgument.forInt(interfaceID));
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
                MaxRiType maxType = (MaxRiType) type;
                int interfaceID = maxType.classActor.id;
                return new XirSnippet(template, receiver, XirArgument.forInt(interfaceID));
            }

            XirArgument hub = XirArgument.forObject(hubFor(type));
            if (type.isFinal()) {
                template = instanceofForLeafTemplate.resolved;
            } else {
                template = instanceofForClassTemplate.resolved;
                MaxRiType maxType = (MaxRiType) type;
                int interfaceID = maxType.classActor.id;
                return new XirSnippet(template, receiver, XirArgument.forInt(interfaceID));
            }
            return new XirSnippet(template, receiver, hub);
        }
        XirArgument guard = XirArgument.forObject(guardFor(type));
        return new XirSnippet(instanceofForInterfaceTemplate.unresolved, receiver, guard);
    }

    @Override
    public XirSnippet genArrayLoad(XirArgument array, XirArgument index, XirArgument length, CiKind elementKind, RiType elementType) {
        XirTemplate template = arrayLoadTemplates[elementKind.ordinal()];
        return new XirSnippet(template, array, index);
    }

    @Override
    public XirSnippet genArrayStore(XirArgument array, XirArgument index, XirArgument length, XirArgument value, CiKind elementKind, RiType elementType) {
        XirTemplate template = arrayStoreTemplates[elementKind.ordinal()];
        return new XirSnippet(template, array, index, value);
    }

    @Override
    public XirSnippet genArrayLength(XirArgument array) {
        return new XirSnippet(arraylengthTemplate, array);
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

        if (m.isLoaded()) {
            return new ResolutionGuard(m.classActor);
        }

        return new ResolutionGuard(m.constantPool.constantPool, m.cpi);
    }

    private XirTemplate buildResolveClassObject() {
        asm.restart(CiKind.Object);
        XirParameter guard = asm.createConstantInputParameter("guard", CiKind.Object);
        resolve(asm, "resolveClassObject", asm.getResultOperand(), guard);
        return finishTemplate(asm, "resolveClassObject");
    }

    private void resolve(CiXirAssembler asm, String string, XirVariable result, XirParameter... guard) {
        callRuntimeThroughStub(asm, string, result, guard);
    }

    private XirTemplate buildSafepoint() {
        asm.restart(CiKind.Void);
        XirVariable param = asm.createRegister("safepoint", CiKind.Word, X86.r14);
        asm.pload(CiKind.Word, param, param, false);
        return finishTemplate(asm, "safepoint");
    }

    private XirTemplate buildArrayLength() {
        asm.restart(CiKind.Int);
        XirVariable param = asm.createInputParameter("param", CiKind.Object);
        asm.pload(CiKind.Word, asm.getResultOperand(), param, asm.i(arrayLengthOffset), true);
        return finishTemplate(asm, "arraylength");
    }

    private XirTemplate buildArrayStore(CiKind kind, CiXirAssembler asm, boolean genBoundsCheck, boolean genStoreCheck, boolean genWriteBarrier) {
        XirParameter array = asm.createInputParameter("array", CiKind.Object);
        XirParameter index = asm.createInputParameter("index", CiKind.Int);
        XirParameter value = asm.createInputParameter("value", kind);
        XirVariable length = asm.createTemp("length", CiKind.Int);
        XirVariable valueHub = null;
        XirVariable compHub = null;
        XirLabel store = asm.createInlineLabel("store");
        XirLabel failBoundsCheck = null;
        XirLabel slowStoreCheck = null;
        if (genBoundsCheck) {
            // load the array length and check the index
            failBoundsCheck = asm.createOutOfLineLabel("failBoundsCheck");
            asm.pload(CiKind.Int, length, array, asm.i(arrayLengthOffset), true);
            asm.jugteq(failBoundsCheck, index, length);
        }
        if (genStoreCheck) {
            slowStoreCheck = asm.createOutOfLineLabel("slowStoreCheck");
            asm.jeq(store, value, asm.o(null)); // first check if value is null
            valueHub = asm.createTemp("valueHub", CiKind.Object);
            compHub = asm.createTemp("compHub", CiKind.Object);
            asm.pload(CiKind.Object, compHub, array, asm.i(hubOffset), !genBoundsCheck);
            asm.pload(CiKind.Object, compHub, compHub, asm.i(hub_componentHub), false);
            asm.pload(CiKind.Object, valueHub, value, asm.i(hubOffset), false);
            asm.jneq(slowStoreCheck, compHub, valueHub); // then check component hub matches value hub
        }
        asm.bindInline(store);
        int elemSize = target.sizeInBytes(kind);
        if (elemSize > 1) {
            asm.shl(index, index, asm.i(Util.log2(elemSize)));
        }
        asm.add(index, index, asm.i(offsetOfFirstArrayElement));
        asm.pstore(kind, array, index, value, !genBoundsCheck && !genStoreCheck);
        if (genWriteBarrier) {
            addWriteBarrier(asm, array, value);
        }
        if (genBoundsCheck) {
            asm.bindOutOfLine(failBoundsCheck);
            callRuntimeThroughStub(asm, "throwArrayIndexOutOfBoundsException", null, index);
        }
        if (genStoreCheck) {
            asm.bindOutOfLine(slowStoreCheck);
            callRuntimeThroughStub(asm, "arrayHubStoreCheck", null, compHub, valueHub);
            asm.jmp(store);
        }
        asm.end();
        return finishTemplate(asm, "arraystore<" + kind + ">");
    }

    private XirTemplate buildArrayLoad(CiKind kind, CiXirAssembler asm, boolean genBoundsCheck) {
        XirParameter array = asm.createInputParameter("array", CiKind.Object);
        XirParameter index = asm.createInputParameter("index", CiKind.Int);
        XirVariable length = asm.createTemp("length", CiKind.Int);
        XirVariable result = asm.getResultOperand();
        XirLabel fail = null;
        if (genBoundsCheck) {
            // load the array length and check the index
            fail = asm.createOutOfLineLabel("fail");
            asm.pload(CiKind.Int, length, array, asm.i(arrayLengthOffset), true);
            asm.jugteq(fail, index, length);
        }
        int elemSize = target.sizeInBytes(kind);
        if (elemSize > 1) {
            asm.shl(index, index, asm.i(Util.log2(elemSize)));
        }
        asm.add(index, index, asm.i(offsetOfFirstArrayElement));
        asm.pload(kind, result, array, index, !genBoundsCheck);
        asm.end();
        if (genBoundsCheck) {
            asm.bindOutOfLine(fail);
            callRuntimeThroughStub(asm, "throwArrayIndexOutOfBoundsException", null, index);
        }
        return finishTemplate(asm, "arrayload<" + kind + ">");
    }

    private XirPair buildInvokeStatic(CiKind kind) {
        XirTemplate resolved;
        XirTemplate unresolved;
        {
            // resolved invokestatic template
            asm.restart();
            XirParameter addr = asm.createConstantInputParameter("addr", CiKind.Word);
            asm.callJava(addr);
            resolved = finishTemplate(asm, "invokestatic");
        }
        {
            // unresolved invokestatic template
            asm.restart();
            XirParameter guard = asm.createConstantInputParameter("guard", CiKind.Object);
            XirVariable addr = asm.createTemp("addr", CiKind.Word);
            resolve(asm, "resolveStaticMethod", addr, guard);
            asm.callJava(addr);
            unresolved = finishTemplate(asm, "invokestatic-unresolved");
        }
        return new XirPair(resolved, unresolved);
    }

    private XirPair buildInvokeSpecial(CiKind kind) {
        XirTemplate resolved;
        XirTemplate unresolved;
        {
            // resolved case
            asm.restart();
            XirParameter addr = asm.createConstantInputParameter("addr", CiKind.Word); // address to call
            asm.callJava(addr);
            resolved = finishTemplate(asm, "invokespecial<" + kind + ">");
        }
        {
            // unresolved invokespecial template
            asm.restart();
            XirParameter guard = asm.createConstantInputParameter("guard", CiKind.Object);
            XirVariable addr = asm.createTemp("addr", CiKind.Word);
            resolve(asm, "resolveSpecialMethod", addr, guard);
            asm.callJava(addr);
            unresolved = finishTemplate(asm, "invokespecial-unresolved<" + kind + ">");
        }
        return new XirPair(resolved, unresolved);
    }

    private XirPair buildInvokeInterface(CiKind kind) {
        XirTemplate resolved;
        XirTemplate unresolved;
        {
            // resolved invokeinterface
            asm.restart();
            XirParameter receiver = asm.createInputParameter("receiver", CiKind.Object); // receiver object
            XirParameter interfaceID = asm.createConstantInputParameter("interfaceID", CiKind.Int);
            XirParameter methodIndex = asm.createConstantInputParameter("methodIndex", CiKind.Int);
            XirVariable hub = asm.createTemp("hub", CiKind.Object);
            XirVariable mtableLength = asm.createTemp("mtableLength", CiKind.Int);
            XirVariable mtableStartIndex = asm.createTemp("mtableStartIndex", CiKind.Int);
            XirVariable a = asm.createTemp("a", CiKind.Int);
            asm.pload(CiKind.Object, hub, receiver, asm.i(hubOffset), true);
            asm.pload(CiKind.Int, mtableLength, hub, asm.i(hub_mTableLength), false);
            asm.pload(CiKind.Int, mtableStartIndex, hub, asm.i(hub_mTableStartIndex), false);
            asm.mod(a, interfaceID, mtableLength);
            asm.add(a, a, mtableStartIndex);
            asm.shl(a, a, asm.i(Util.log2(Ints.SIZE)));
            asm.add(a, a, asm.i(offsetOfFirstArrayElement));
            asm.pload(CiKind.Int, a, hub, a, false);
            asm.add(a, a, methodIndex);
            asm.mul(a, a, asm.i(wordSize));
            asm.add(a, a, asm.i(offsetOfFirstArrayElement));
            asm.pload(CiKind.Word, a, hub, a, false);
            asm.callJava(a);
            resolved = finishTemplate(asm, "invokeinterface<" + kind + ">");
        }
        {
            // unresolved invokeinterface
            asm.restart();
            XirParameter receiver = asm.createInputParameter("receiver", CiKind.Object); // receiver object
            XirParameter guard = asm.createInputParameter("guard", CiKind.Object); // guard
            XirVariable interfaceID = asm.createTemp("interfaceID", CiKind.Int);
            XirVariable methodIndex = asm.createTemp("methodIndex", CiKind.Int);
            XirVariable hub = asm.createTemp("hub", CiKind.Object);
            XirVariable mtableLength = asm.createTemp("mtableLength", CiKind.Int);
            XirVariable mtableStartIndex = asm.createTemp("mtableStartIndex", CiKind.Int);
            XirVariable a = asm.createTemp("a", CiKind.Int);

            resolve(asm, "resolveInterfaceMethod", methodIndex, guard);
            resolve(asm, "resolveInterfaceID", interfaceID, guard);
            asm.pload(CiKind.Object, hub, receiver, asm.i(hubOffset), true);
            asm.pload(CiKind.Int, mtableLength, hub, asm.i(hub_mTableLength), false);
            asm.pload(CiKind.Int, mtableStartIndex, hub, asm.i(hub_mTableStartIndex), false);
            asm.mod(a, interfaceID, mtableLength);
            asm.add(a, a, mtableStartIndex);
            asm.shl(a, a, asm.i(Util.log2(Ints.SIZE)));
            asm.add(a, a, asm.i(offsetOfFirstArrayElement));
            asm.pload(CiKind.Int, a, hub, a, false);
            asm.add(a, a, methodIndex);
            asm.mul(a, a, asm.i(wordSize));
            asm.add(a, a, asm.i(offsetOfFirstArrayElement));
            asm.pload(CiKind.Word, a, hub, a, false);
            asm.callJava(a);
            unresolved = finishTemplate(asm, "invokeinterface<" + kind + ">-unresolved");
        }
        return new XirPair(resolved, unresolved);
    }

    private XirPair buildResolvedInvokeVirtual(CiKind kind) {
        XirTemplate resolved;
        XirTemplate unresolved;
        {
            // resolved invokevirtual
            asm.restart();
            XirParameter receiver = asm.createInputParameter("receiver", CiKind.Object);
            XirParameter vtableOffset = asm.createConstantInputParameter("vtableOffset", CiKind.Int);
            XirVariable hub = asm.createTemp("hub", CiKind.Object);
            XirVariable addr = asm.createTemp("addr", CiKind.Word);
            asm.pload(CiKind.Object, hub, receiver, asm.i(hubOffset), true);
            asm.pload(CiKind.Word, addr, hub, vtableOffset, false);
            asm.callJava(addr);
            resolved = finishTemplate(asm, "invokevirtual<" + kind + ">");
        }
        {
            // unresolved invokevirtual template
            asm.restart();
            XirParameter guard = asm.createConstantInputParameter("guard", CiKind.Object);
            XirParameter receiver = asm.createInputParameter("receiver", CiKind.Object); // receiver object
            XirVariable vtableOffset = asm.createTemp("vtableOffset", CiKind.Int);
            resolve(asm, "resolveVirtualMethod", vtableOffset, guard);
            XirVariable hub = asm.createTemp("hub", CiKind.Object);
            XirVariable addr = asm.createTemp("addr", CiKind.Word);
            asm.pload(CiKind.Object, hub, receiver, asm.i(hubOffset), true);
            asm.pload(CiKind.Word, addr, hub, vtableOffset, false);
            asm.callJava(addr);
            unresolved = finishTemplate(asm, "invokevirtual-unresolved<" + kind + ">");
        }
        return new XirPair(resolved, unresolved);
    }

    private XirPair buildNewArray(CiKind kind) {
        XirTemplate resolved;
        XirTemplate unresolved;
        if (kind == CiKind.Object) {
            {
                // resolved new object array
                asm.restart(CiKind.Object);
                XirParameter hub = asm.createConstantInputParameter("hub", CiKind.Object);
                XirParameter length = asm.createInputParameter("length", CiKind.Int);
                callRuntimeThroughStub(asm, "allocateObjectArray", asm.getResultOperand(), hub, length);
                resolved = finishTemplate(asm, "anewarray<" + kind + ">");
            }
            {
                // unresolved new object array
                asm.restart(CiKind.Object);
                XirParameter guard = asm.createConstantInputParameter("guard", CiKind.Object);
                XirParameter length = asm.createInputParameter("length", CiKind.Int);
                XirVariable hub = asm.createTemp("hub", CiKind.Object);
                resolve(asm, "resolveNewArray", hub, guard);
                callRuntimeThroughStub(asm, "allocateObjectArray", asm.getResultOperand(), hub, length);
                asm.end();
                unresolved = finishTemplate(asm, "anewarray<" + kind + ">-unresolved");
            }

        } else {
            // XXX: specialized, inline templates for each kind
            asm.restart(CiKind.Object);
            XirParameter hub = asm.createConstantInputParameter("hub", CiKind.Object);
            XirParameter length = asm.createInputParameter("length", CiKind.Int);
            callRuntimeThroughStub(asm, "allocatePrimitiveArray", asm.getResultOperand(), hub, length);
            resolved = finishTemplate(asm, "newarray<" + kind + ">");
            unresolved = resolved;
        }
        return new XirPair(resolved, unresolved);
    }

    private XirPair buildNewMultiArray(int rank) {
        XirTemplate resolved = null;
        XirTemplate unresolved;
        if (rank < SMALL_MULTIANEWARRAY_RANK) {
            // "small" resolved multianewarray (rank 3 or less)
            asm.restart(CiKind.Object);
            XirParameter[] lengths = new XirParameter[rank];
            for (int i = 0; i < rank; i++) {
                lengths[i] = asm.createInputParameter("lengths[" + i + "]", CiKind.Int);
            }
            XirParameter hub = asm.createConstantInputParameter("hub", CiKind.Object);
            callRuntimeThroughStub(asm, "allocateMultiArray" + rank, asm.getResultOperand(), Arrays.prepend(lengths, hub));
            resolved = finishTemplate(asm, "multianewarray<" + rank + ">");
        }

        // unresolved or large multianewarray
        asm.restart(CiKind.Object);
        XirParameter[] lengths = new XirParameter[rank];
        for (int i = 0; i < rank; i++) {
            lengths[i] = asm.createInputParameter("lengths[" + i + "]", CiKind.Int);
        }
        XirParameter guard = asm.createConstantInputParameter("guard", CiKind.Object);
        XirVariable lengthArray = asm.createTemp("lengthArray", CiKind.Object);
        callRuntimeThroughStub(asm, "allocateIntArray", lengthArray, asm.i(rank));
        for (int i = 0; i < rank; i++) {
            asm.pstore(CiKind.Int, lengthArray, asm.i(offsetOfFirstArrayElement + i * target.sizeInBytes(CiKind.Int)), lengths[i], false);
        }
        callRuntimeThroughStub(asm, "allocateMultiArrayN", asm.getResultOperand(), guard, lengthArray);
        unresolved = finishTemplate(asm, "multianewarray-complex<" + rank + ">");

        return new XirPair(resolved == null ? unresolved : resolved, unresolved);
    }

    private XirPair buildNewInstance() {
        XirTemplate resolved;
        XirTemplate unresolved;
        {
            // resolved new instance
            asm.restart(CiKind.Object);
            XirParameter hub = asm.createConstantInputParameter("hub", CiKind.Object);
            callRuntimeThroughStub(asm, "allocateObject", asm.getResultOperand(), hub);
            resolved = finishTemplate(asm, "new");
        }
        {
            // unresolved new instance
            asm.restart(CiKind.Object);
            XirParameter guard = asm.createConstantInputParameter("guard", CiKind.Object);
            XirVariable hub = asm.createTemp("hub", CiKind.Object);
            resolve(asm, "resolveNew", hub, guard);
            callRuntimeThroughStub(asm, "allocateObject", asm.getResultOperand(), hub);
            asm.end();
            unresolved = finishTemplate(asm, "new-unresolved");
        }

        return new XirPair(resolved, unresolved);
    }

    private XirPair buildPutFieldTemplate(CiKind kind, boolean genWriteBarrier) {
        XirTemplate resolved;
        XirTemplate unresolved;
        {
            // resolved case
            asm.restart(CiKind.Void);
            XirParameter object = asm.createInputParameter("object", CiKind.Object);
            XirParameter value = asm.createInputParameter("value", kind);
            XirParameter fieldOffset = asm.createConstantInputParameter("fieldOffset", CiKind.Int);
            asm.pstore(kind, object, fieldOffset, value, true);
            if (genWriteBarrier) {
                addWriteBarrier(asm, object, value);
            }
            resolved = finishTemplate(asm, "putfield<" + kind + ", " + genWriteBarrier + ">");
        } {
            // unresolved case
            asm.restart(CiKind.Void);
            XirParameter object = asm.createInputParameter("object", CiKind.Object);
            XirParameter value = asm.createInputParameter("value", kind);
            XirParameter guard = asm.createInputParameter("guard", CiKind.Object);
            XirVariable fieldOffset = asm.createTemp("fieldOffset", CiKind.Int);
            resolve(asm, "resolvePutField", fieldOffset, guard);
            asm.pstore(kind, object, fieldOffset, value, true);
            if (genWriteBarrier) {
                addWriteBarrier(asm, object, value);
            }
            asm.end();
            unresolved = finishTemplate(asm, "putfield<" + kind + ", " + genWriteBarrier + ">-unresolved");
        }
        return new XirPair(resolved, unresolved);
    }

    private XirPair buildGetFieldTemplate(CiKind kind) {
        XirTemplate resolved;
        XirTemplate unresolved;
        {
            // resolved case
            asm.restart(kind);
            XirParameter object = asm.createInputParameter("object", CiKind.Object);
            XirParameter fieldOffset = asm.createConstantInputParameter("fieldOffset", CiKind.Int);
            XirVariable resultOperand = asm.getResultOperand();
            asm.pload(kind, resultOperand, object, fieldOffset, true);
            resolved = finishTemplate(asm, "getfield<" + kind + ">");
        }
        {
            // unresolved case
            asm.restart(kind);
            XirParameter object = asm.createInputParameter("object", CiKind.Object);
            XirVariable resultOperand = asm.getResultOperand();
            XirParameter guard = asm.createInputParameter("guard", CiKind.Object);
            XirVariable fieldOffset = asm.createTemp("fieldOffset", CiKind.Int);
            resolve(asm, "resolveGetField", fieldOffset, guard);
            asm.pload(kind, resultOperand, object, fieldOffset, true);
            asm.end();
            unresolved = finishTemplate(asm, "getfield<" + kind + ">-unresolved");
        }
        return new XirPair(resolved, unresolved);
    }

    private XirPair buildPutStaticTemplate(CiKind kind, boolean genWriteBarrier) {
        XirTemplate resolved;
        XirTemplate unresolved;
        {
            // XXX: this is identical to put field, except the tuple is a constant
            asm.restart(CiKind.Void);
            XirParameter tuple = asm.createConstantInputParameter("tuple", CiKind.Object);
            XirParameter value = asm.createInputParameter("value", kind);
            XirParameter fieldOffset = asm.createConstantInputParameter("fieldOffset", CiKind.Int);
            asm.pstore(kind, tuple, fieldOffset, value, true);
            if (genWriteBarrier) {
                addWriteBarrier(asm, tuple, value);
            }
            resolved = finishTemplate(asm, "putstatic<" + kind + ", " + genWriteBarrier + ">");
        }
        {
            // unresolved put static
            asm.restart(CiKind.Void);
            XirParameter value = asm.createInputParameter("value", kind);
            XirParameter guard = asm.createInputParameter("guard", CiKind.Object);
            XirVariable tuple = asm.createTemp("tuple", CiKind.Object);
            XirVariable fieldOffset = asm.createTemp("fieldOffset", CiKind.Int);
            resolve(asm, "resolvePutStatic", fieldOffset, guard);
            resolve(asm, "resolveStaticTuple", tuple, guard);
            asm.pstore(kind, tuple, fieldOffset, value, true);
            if (genWriteBarrier) {
                addWriteBarrier(asm, tuple, value);
            }
            asm.end();
            unresolved = finishTemplate(asm, "putstatic<" + kind + ", " + genWriteBarrier + ">-unresolved");
        }
        return new XirPair(resolved, unresolved);
    }

    private XirPair buildGetStaticTemplate(CiKind kind) {
        XirTemplate resolved;
        XirTemplate unresolved;
        {
            // resolved get static
            asm.restart(kind);
            XirParameter tuple = asm.createInputParameter("tuple", CiKind.Object);
            XirParameter fieldOffset = asm.createConstantInputParameter("fieldOffset", CiKind.Int);
            XirVariable resultOperand = asm.getResultOperand();
            asm.pload(kind, resultOperand, tuple, fieldOffset, false);
            resolved = finishTemplate(asm, "getstatic<" + kind + ">");
        }
        {
            // unresolved get static
            asm.restart(kind);
            XirVariable resultOperand = asm.getResultOperand();
            XirParameter guard = asm.createInputParameter("guard", CiKind.Object);
            XirVariable fieldOffset = asm.createTemp("fieldOffset", CiKind.Int);
            XirVariable tuple = asm.createTemp("tuple", CiKind.Object);
            resolve(asm, "resolveGetStatic", fieldOffset, guard);
            resolve(asm, "resolveStaticTuple", tuple, guard);
            asm.pload(kind, resultOperand, tuple, fieldOffset, false);
            asm.end();
            unresolved = finishTemplate(asm, "getstatic<" + kind + ">-unresolved");
        }
        return new XirPair(resolved, unresolved);
    }

    private XirTemplate buildMonitorExit() {
        asm.restart(CiKind.Void);
        XirParameter object = asm.createInputParameter("object", CiKind.Object);
        callRuntimeThroughStub(asm, "monitorExit", null, object);
        return finishTemplate(asm, "monitorenter");
    }

    private XirTemplate buildMonitorEnter() {
        asm.restart(CiKind.Void);
        XirParameter object = asm.createInputParameter("object", CiKind.Object);
        callRuntimeThroughStub(asm, "monitorEnter", null, object);
        return finishTemplate(asm, "monitorexit");
    }

    private XirPair buildCheckcastForLeaf(boolean nonnull) {
        XirTemplate resolved;
        XirTemplate unresolved;
        {
            // resolved checkcast for a leaf class
            asm.restart(CiKind.Object);
            XirParameter object = asm.createInputParameter("object", CiKind.Object);
            XirParameter hub = asm.createConstantInputParameter("hub", CiKind.Object);
            XirVariable temp = asm.createTemp("temp", CiKind.Object);
            XirLabel pass = asm.createInlineLabel("pass");
            XirLabel fail = asm.createOutOfLineLabel("fail");
            if (!nonnull) {
                // first check against null
                asm.jeq(pass, object, asm.o(null));
            }
            asm.pload(CiKind.Object, temp, object, asm.i(hubOffset), !nonnull);
            asm.jneq(fail, temp, hub);
            asm.bindInline(pass);
            asm.mov(asm.getResultOperand(), object);
            asm.end();
            asm.bindOutOfLine(fail);
            callRuntimeThroughStub(asm, "throwClassCastException", null);
            resolved = finishTemplate(asm, "checkcast-leaf<" + nonnull + ">");
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
            asm.restart(CiKind.Object);
            XirParameter object = asm.createInputParameter("object", CiKind.Object);
            XirParameter interfaceID = asm.createConstantInputParameter("interfaceID", CiKind.Int);
            XirVariable hub = asm.createTemp("hub", CiKind.Object);
            XirVariable mtableLength = asm.createTemp("mtableLength", CiKind.Int);
            XirVariable mtableStartIndex = asm.createTemp("mtableStartIndex", CiKind.Int);
            XirVariable a = asm.createTemp("a", CiKind.Int);
            XirLabel pass = asm.createInlineLabel("pass");
            XirLabel fail = asm.createOutOfLineLabel("fail");
            // XXX: use a cache to check the last successful receiver type
            if (!nonnull) {
                // first check for null
                asm.jeq(pass, object, asm.o(null));
            }
            asm.pload(CiKind.Object, hub, object, asm.i(hubOffset), !nonnull);
            asm.pload(CiKind.Int, mtableLength, hub, asm.i(hub_mTableLength), false);
            asm.pload(CiKind.Int, mtableStartIndex, hub, asm.i(hub_mTableStartIndex), false);
            asm.mod(a, interfaceID, mtableLength);
            asm.add(a, a, mtableStartIndex);
            asm.mul(a, a, asm.i(wordSize));
            asm.pload(CiKind.Int, a, hub, a, false);
            asm.jneq(fail, a, interfaceID);
            asm.bindInline(pass);
            asm.mov(asm.getResultOperand(), object);
            asm.end();
            asm.bindOutOfLine(fail);
            callRuntimeThroughStub(asm, "throwClassCastException", null);
            resolved = finishTemplate(asm, "checkcast-interface<" + nonnull + ">");
        }
        {
            unresolved = buildUnresolvedCheckcast(nonnull);
        }
        return new XirPair(resolved, unresolved);
    }

    private XirTemplate buildUnresolvedCheckcast(boolean nonnull) {
        XirTemplate unresolved;
        asm.restart(CiKind.Object);
        XirParameter object = asm.createInputParameter("object", CiKind.Object);
        XirParameter guard = asm.createInputParameter("guard", CiKind.Object);
        XirVariable hub = asm.createTemp("hub", CiKind.Object);
        XirLabel pass = asm.createInlineLabel("pass");
        if (!nonnull) {
            // XXX: build a version that does not include a null check
            asm.jeq(pass, object, asm.o(null));
        }
        resolve(asm, "unresolvedCheckcast", hub, object, guard);
        asm.bindInline(pass);
        asm.mov(asm.getResultOperand(), object);
        asm.end();
        unresolved = finishTemplate(asm, "checkcast-unresolved<" + nonnull + ">");
        return unresolved;
    }

    private XirPair buildInstanceofForLeaf(boolean nonnull) {
        XirTemplate resolved;
        XirTemplate unresolved;
        {
            asm.restart(CiKind.Boolean);
            XirVariable result = asm.getResultOperand();
            XirParameter object = asm.createInputParameter("object", CiKind.Object);
            XirParameter hub = asm.createConstantInputParameter("hub", CiKind.Object);
            XirVariable temp = asm.createTemp("temp", CiKind.Object);
            XirLabel pass = asm.createInlineLabel("pass");
            XirLabel fail = asm.createInlineLabel("fail");
            if (!nonnull) {
                // first check for null
                asm.jeq(fail, object, asm.o(null));
            }
            asm.pload(CiKind.Object, temp, object, asm.i(hubOffset), !nonnull);
            asm.jneq(fail, temp, hub);
            asm.bindInline(pass);
            asm.mov(result, asm.b(true));
            asm.end();
            asm.bindInline(fail);
            asm.mov(result, asm.b(false));
            asm.end();
            resolved = finishTemplate(asm, "instanceof-leaf<" + nonnull + ">");
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
            asm.restart(CiKind.Boolean);
            XirVariable result = asm.getResultOperand();
            XirParameter object = asm.createInputParameter("object", CiKind.Object);
            XirParameter interfaceID = asm.createConstantInputParameter("interfaceID", CiKind.Int);
            XirVariable hub = asm.createTemp("hub", CiKind.Object);
            XirVariable mtableLength = asm.createTemp("mtableLength", CiKind.Int);
            XirVariable mtableStartIndex = asm.createTemp("mtableStartIndex", CiKind.Int);
            XirVariable a = asm.createTemp("a", CiKind.Int);
            XirLabel pass = asm.createInlineLabel("pass");
            XirLabel fail = asm.createInlineLabel("fail");
            asm.mov(result, asm.b(false));
            // XXX: use a cache to check the last successful receiver type
            if (!nonnull) {
                // first check for null
                asm.jeq(fail, object, asm.o(null));
            }
            asm.pload(CiKind.Object, hub, object, asm.i(hubOffset), !nonnull);
            asm.pload(CiKind.Int, mtableLength, hub, asm.i(hub_mTableLength), false);
            asm.pload(CiKind.Int, mtableStartIndex, hub, asm.i(hub_mTableStartIndex), false);
            asm.mod(a, interfaceID, mtableLength);
            asm.add(a, a, mtableStartIndex);
            asm.mul(a, a, asm.i(wordSize));
            asm.pload(CiKind.Int, a, hub, a, false);
            asm.jneq(fail, a, interfaceID);
            asm.bindInline(pass);
            asm.mov(result, asm.b(true));
            asm.bindInline(fail);
            asm.end();
            resolved = finishTemplate(asm, "instanceof-interface<" + nonnull + ">");
        }
        {
            // unresolved instanceof
            unresolved = buildUnresolvedInstanceOf(nonnull);
        }
        return new XirPair(resolved, unresolved);
    }

    private XirTemplate buildUnresolvedInstanceOf(boolean nonnull) {
        XirTemplate unresolved;
        asm.restart(CiKind.Boolean);
        XirParameter object = asm.createInputParameter("object", CiKind.Object);
        XirParameter guard = asm.createInputParameter("guard", CiKind.Object);
        XirLabel fail = null;
        if (!nonnull) {
            // first check failed
            fail = asm.createInlineLabel("fail");
            asm.jeq(fail, object, asm.o(null));
        }
        callRuntimeThroughStub(asm, "unresolvedInstanceOf", asm.getResultOperand(), object, guard);
        asm.end();
        if (!nonnull) {
            // null check failed
            asm.bindInline(fail);
            asm.mov(asm.getResultOperand(), asm.b(false));
            asm.end();
        }
        asm.end();
        unresolved = finishTemplate(asm, "instanceof-unresolved<" + nonnull + ">");
        return unresolved;
    }



    private XirTemplate finishTemplate(CiXirAssembler asm, String name) {
        final XirTemplate result = asm.finishTemplate(name);
        if (C1XOptions.PrintXirTemplates) {
            result.print(System.out);
        }
        return result;
    }

    private void addWriteBarrier(CiXirAssembler asm, XirVariable object, XirVariable value) {
        // XXX: add write barrier mechanism
    }

    private CiKind toCiKind(Kind k) {
        return kindMapping[k.asEnum.ordinal()];
    }

    static {
        // search for the runtime call and register critical methods
        for (Method m : RuntimeCalls.class.getDeclaredMethods()) {
            int flags = m.getModifiers();
            if (Modifier.isStatic(flags) && Modifier.isPublic(flags)) {

                if (MaxineVM.isHosted()) {
                    // System.out.println("Registered critical method: " + m.getName() + " / " + SignatureDescriptor.create(m.getReturnType(), m.getParameterTypes()).toString());
                    new CriticalMethod(RuntimeCalls.class, m.getName(), SignatureDescriptor.create(m.getReturnType(), m.getParameterTypes()));
                }
            }
        }
    }

    private void callRuntimeThroughStub(CiXirAssembler asm, String method, XirVariable result, XirVariable... args) {
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
                    CiXirAssembler stubAsm = asm.copy();
                    stubAsm.restart(toCiKind(signature.resultKind()));

                    XirParameter[] rtArgs = new XirParameter[signature.numberOfParameters()];
                    for (int i = 0; i < signature.numberOfParameters(); i++) {
                        // create a parameter for each parameter to the runtime call
                        CiKind ciKind = toCiKind(signature.parameterDescriptorAt(i).toKind());
                        assert ciKind == args[i].kind : "type mismatch in call to " + method;
                        rtArgs[i] = stubAsm.createInputParameter("rtArgs[" + i + "]", ciKind);
                    }
                    stubAsm.callRuntime(runtime.getRiMethod((ClassMethodActor) methodActor), stubAsm.getResultOperand(), rtArgs);
                    stub = stubAsm.finishStub(method + "-stub");

                    if (C1XOptions.PrintXirTemplates) {
                        stub.print(System.out);
                    }
                    runtimeCallStubs.put(method, stub);
                }
            }

            stubs.add(stub);
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

        @UNSAFE
        public static Word resolveStaticMethod(ResolutionGuard guard) {
            return CompilationScheme.Static.compile(ResolutionSnippet.ResolveStaticMethod.resolveStaticMethod(guard), CallEntryPoint.OPTIMIZED_ENTRY_POINT);
        }

        public static int resolveVirtualMethod(ResolutionGuard guard) {
            return ResolutionSnippet.ResolveVirtualMethod.resolveVirtualMethod(guard).vTableIndex();
        }

        @UNSAFE
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
            Object[] result = UnsafeCast.asObjectArray(createObjectArray(hub1, l1));
            DynamicHub hub2 = UnsafeCast.asDynamicHub(hub1.componentHub);
            for (int i1 = 0; i1 < l1; i1++) {
                ArrayAccess.setObject(result, i1, createArray(hub2, l2));
            }
            return result;
        }

        public static Object allocateMultiArray3(DynamicHub hub1, int l1, int l2, int l3) {
            if (l1 < 0 | l2 < 0 | l3 < 0) {
                throw new NegativeArraySizeException();
            }
            Object[] result = UnsafeCast.asObjectArray(createObjectArray(hub1, l1));
            DynamicHub hub2 = UnsafeCast.asDynamicHub(hub1.componentHub);
            DynamicHub hub3 = UnsafeCast.asDynamicHub(hub2.componentHub);
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
            if (MaxineVM.isHosted()) {
                return (Object[]) Array.newInstance(hub.classActor.componentClassActor().toJava(), length);
            }
            return UnsafeCast.asObjectArray(Heap.createArray(hub, length));
        }

        @INLINE
        private static Object createArray(DynamicHub hub, int length) {
            if (MaxineVM.isHosted()) {
                return Array.newInstance(hub.classActor.componentClassActor().toJava(), length);
            }
            return Heap.createArray(hub, length);
        }

        @INLINE
        private static void safeArrayStore(Object[] array, int index, Object val) {
            if (MaxineVM.isHosted()) {
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
