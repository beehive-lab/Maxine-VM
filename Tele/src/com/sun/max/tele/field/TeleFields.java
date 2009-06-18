/*
 * Copyright (c) 2007 Sun Microsystems, Inc.  All rights reserved.
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
package com.sun.max.tele.field;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;

import com.sun.max.*;
import com.sun.max.annotate.*;
import com.sun.max.collect.*;
import com.sun.max.collect.ChainedHashMapping.*;
import com.sun.max.ide.*;
import com.sun.max.io.*;
import com.sun.max.lang.*;
import com.sun.max.memory.*;
import com.sun.max.program.*;
import com.sun.max.tele.*;
import com.sun.max.vm.actor.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.bytecode.*;
import com.sun.max.vm.classfile.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.code.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.compiler.builtin.*;
import com.sun.max.vm.compiler.ir.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.jit.*;
import com.sun.max.vm.tele.*;
import com.sun.max.vm.thread.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;

/**
 * Centralized collection of remote {@link TeleFieldAccess}s.
 * <p>
 * The {@link INSPECTED} annotation is employed to denote fields that will be read remotely.
 * A field of the appropriate {@link TeleFieldAccess} subtype is generated into this file
 * by executing the {@link #main(String[])} method in this class (ensuring that the{@link TeleVM}.
 * class path contains all the {@code com.sun.max} classes).
 *
 * @author Bernd Mathiske
 * @author Doug Simon
 * @author Michael Van De Vanter
 */
public class TeleFields extends AbstractTeleVMHolder {

    public TeleFields(TeleVM teleVM) {
        super(teleVM);
        // Uncomment to enable verifying that the generated content in this class is up to date when running the inspector
        // updateSource(true);
    }


    // Checkstyle: stop field name check

    public final <IrMethod_Type extends IrMethod> TeleInstanceReferenceFieldAccess IrMethod_classMethodActor(Class<IrMethod_Type> holderType) {
        return new TeleInstanceReferenceFieldAccess(holderType, "_classMethodActor", ClassMethodActor.class);
    }

    // VM fields:

    // START GENERATED CONTENT
    public final TeleInstanceIntFieldAccess Actor_flags = new TeleInstanceIntFieldAccess(Actor.class, "_flags");
    public final TeleInstanceReferenceFieldAccess Actor_name = new TeleInstanceReferenceFieldAccess(Actor.class, "_name", Utf8Constant.class);
    public final TeleInstanceIntFieldAccess Builtin_serial = new TeleInstanceIntFieldAccess(Builtin.class, "_serial");
    public final TeleInstanceReferenceFieldAccess ChainedHashMapping_table = new TeleInstanceReferenceFieldAccess(ChainedHashMapping.class, "table", Entry[].class);
    public final TeleInstanceReferenceFieldAccess ClassActor_classLoader = new TeleInstanceReferenceFieldAccess(ClassActor.class, "_classLoader", ClassLoader.class);
    public final TeleInstanceReferenceFieldAccess ClassActor_classfile = new TeleInstanceReferenceFieldAccess(ClassActor.class, "_classfile", byte[].class);
    public final TeleInstanceReferenceFieldAccess ClassActor_componentClassActor = new TeleInstanceReferenceFieldAccess(ClassActor.class, "_componentClassActor", ClassActor.class);
    public final TeleInstanceIntFieldAccess ClassActor_id = new TeleInstanceIntFieldAccess(ClassActor.class, "_id");
    public final TeleInstanceReferenceFieldAccess ClassActor_localInstanceFieldActors = new TeleInstanceReferenceFieldAccess(ClassActor.class, "_localInstanceFieldActors", FieldActor[].class);
    public final TeleInstanceReferenceFieldAccess ClassActor_localInterfaceMethodActors = new TeleInstanceReferenceFieldAccess(ClassActor.class, "_localInterfaceMethodActors", InterfaceMethodActor[].class);
    public final TeleInstanceReferenceFieldAccess ClassActor_localStaticFieldActors = new TeleInstanceReferenceFieldAccess(ClassActor.class, "_localStaticFieldActors", FieldActor[].class);
    public final TeleInstanceReferenceFieldAccess ClassActor_localStaticMethodActors = new TeleInstanceReferenceFieldAccess(ClassActor.class, "_localStaticMethodActors", StaticMethodActor[].class);
    public final TeleInstanceReferenceFieldAccess ClassActor_localVirtualMethodActors = new TeleInstanceReferenceFieldAccess(ClassActor.class, "_localVirtualMethodActors", VirtualMethodActor[].class);
    public final TeleInstanceReferenceFieldAccess ClassActor_mirror = new TeleInstanceReferenceFieldAccess(ClassActor.class, "_mirror", Class.class);
    public final TeleInstanceReferenceFieldAccess ClassActor_staticTuple = new TeleInstanceReferenceFieldAccess(ClassActor.class, "_staticTuple", Object.class);
    public final TeleInstanceReferenceFieldAccess ClassActor_typeDescriptor = new TeleInstanceReferenceFieldAccess(ClassActor.class, "_typeDescriptor", TypeDescriptor.class);
    public final TeleInstanceReferenceFieldAccess ClassMethodActor_codeAttribute = new TeleInstanceReferenceFieldAccess(ClassMethodActor.class, "_codeAttribute", CodeAttribute.class);
    public final TeleInstanceReferenceFieldAccess ClassMethodActor_methodState = new TeleInstanceReferenceFieldAccess(ClassMethodActor.class, "_methodState", MethodState.class);
    public final TeleInstanceReferenceFieldAccess ClassRegistry_typeDescriptorToClassActor = new TeleInstanceReferenceFieldAccess(ClassRegistry.class, "_typeDescriptorToClassActor", GrowableMapping.class);
    public final TeleStaticReferenceFieldAccess Code_bootCodeRegion = new TeleStaticReferenceFieldAccess(Code.class, "_bootCodeRegion", CodeRegion.class);
    public final TeleStaticReferenceFieldAccess Code_codeManager = new TeleStaticReferenceFieldAccess(Code.class, "_codeManager", CodeManager.class);
    public final TeleInstanceReferenceFieldAccess CodeAttribute_code = new TeleInstanceReferenceFieldAccess(CodeAttribute.class, "_code", byte[].class);
    public final TeleInstanceReferenceFieldAccess CodeAttribute_constantPool = new TeleInstanceReferenceFieldAccess(CodeAttribute.class, "_constantPool", ConstantPool.class);
    public final TeleInstanceReferenceFieldAccess CodeManager_currentCodeRegion = new TeleInstanceReferenceFieldAccess(CodeManager.class, "_currentCodeRegion", CodeRegion.class);
    public final TeleInstanceReferenceFieldAccess CodeManager_runtimeCodeRegions = new TeleInstanceReferenceFieldAccess(CodeManager.class, "_runtimeCodeRegions", CodeRegion[].class);
    public final TeleInstanceReferenceFieldAccess ConstantPool_constants = new TeleInstanceReferenceFieldAccess(ConstantPool.class, "_constants", PoolConstant[].class);
    public final TeleInstanceReferenceFieldAccess ConstantPool_holder = new TeleInstanceReferenceFieldAccess(ConstantPool.class, "_holder", ClassActor.class);
    public final TeleInstanceReferenceFieldAccess ChainedHashMapping$DefaultEntry_next = new TeleInstanceReferenceFieldAccess(ChainedHashMapping.DefaultEntry.class, "next", Entry.class);
    public final TeleInstanceReferenceFieldAccess ChainedHashMapping$DefaultEntry_value = new TeleInstanceReferenceFieldAccess(ChainedHashMapping.DefaultEntry.class, "value", Object.class);
    public final TeleInstanceReferenceFieldAccess Descriptor_string = new TeleInstanceReferenceFieldAccess(Descriptor.class, "_string", String.class);
    public final TeleStaticReferenceFieldAccess Heap_bootHeapRegion = new TeleStaticReferenceFieldAccess(Heap.class, "_bootHeapRegion", BootHeapRegion.class);
    public final TeleInstanceReferenceFieldAccess Hub_classActor = new TeleInstanceReferenceFieldAccess(Hub.class, "_classActor", ClassActor.class);
    public final TeleInstanceIntFieldAccess Hub_mTableLength = new TeleInstanceIntFieldAccess(Hub.class, "_mTableLength");
    public final TeleInstanceIntFieldAccess Hub_mTableStartIndex = new TeleInstanceIntFieldAccess(Hub.class, "_mTableStartIndex");
    public final TeleInstanceIntFieldAccess Hub_referenceMapLength = new TeleInstanceIntFieldAccess(Hub.class, "_referenceMapLength");
    public final TeleInstanceIntFieldAccess Hub_referenceMapStartIndex = new TeleInstanceIntFieldAccess(Hub.class, "_referenceMapStartIndex");
    public final TeleInstanceReferenceFieldAccess HybridClassActor_constantPool = new TeleInstanceReferenceFieldAccess(HybridClassActor.class, "_constantPool", ConstantPool.class);
    public final TeleInstanceReferenceFieldAccess JitTargetMethod_bytecodeInfos = new TeleInstanceReferenceFieldAccess(JitTargetMethod.class, "_bytecodeInfos", BytecodeInfo[].class);
    public final TeleInstanceReferenceFieldAccess JitTargetMethod_bytecodeToTargetCodePositionMap = new TeleInstanceReferenceFieldAccess(JitTargetMethod.class, "_bytecodeToTargetCodePositionMap", int[].class);
    public final TeleInstanceCharFieldAccess Kind_character = new TeleInstanceCharFieldAccess(Kind.class, "_character");
    public final TeleStaticWordFieldAccess MaxineMessenger_info = new TeleStaticWordFieldAccess(MaxineMessenger.class, "_info");
    public final TeleInstanceReferenceFieldAccess MemberActor_holder = new TeleInstanceReferenceFieldAccess(MemberActor.class, "_holder", ClassActor.class);
    public final TeleInstanceIntFieldAccess MemberActor_memberIndex = new TeleInstanceIntFieldAccess(MemberActor.class, "_memberIndex");
    public final TeleInstanceReferenceFieldAccess MethodState_classMethodActor = new TeleInstanceReferenceFieldAccess(MethodState.class, "_classMethodActor", ClassMethodActor.class);
    public final TeleInstanceIntFieldAccess MethodState_numberOfCompilations = new TeleInstanceIntFieldAccess(MethodState.class, "_numberOfCompilations");
    public final TeleInstanceReferenceFieldAccess MethodState_targetMethodHistory = new TeleInstanceReferenceFieldAccess(MethodState.class, "_targetMethodHistory", TargetMethod[].class);
    public final TeleInstanceReferenceFieldAccess ObjectReferenceValue_value = new TeleInstanceReferenceFieldAccess(ObjectReferenceValue.class, "_value", Object.class);
    public final TeleInstanceReferenceFieldAccess ClassConstant$Resolved_classActor = new TeleInstanceReferenceFieldAccess(ClassConstant.Resolved.class, "_classActor", ClassActor.class);
    public final TeleInstanceReferenceFieldAccess FieldRefConstant$Resolved_fieldActor = new TeleInstanceReferenceFieldAccess(FieldRefConstant.Resolved.class, "_fieldActor", FieldActor.class);
    public final TeleInstanceReferenceFieldAccess ResolvedMethodRefConstant_methodActor = new TeleInstanceReferenceFieldAccess(ResolvedMethodRefConstant.class, "_methodActor", MethodActor.class);
    public final TeleInstanceReferenceFieldAccess RuntimeMemoryRegion_description = new TeleInstanceReferenceFieldAccess(RuntimeMemoryRegion.class, "_description", String.class);
    public final TeleInstanceWordFieldAccess RuntimeMemoryRegion_mark = new TeleInstanceWordFieldAccess(RuntimeMemoryRegion.class, "_mark");
    public final TeleInstanceWordFieldAccess RuntimeMemoryRegion_size = new TeleInstanceWordFieldAccess(RuntimeMemoryRegion.class, "_size");
    public final TeleInstanceWordFieldAccess RuntimeMemoryRegion_start = new TeleInstanceWordFieldAccess(RuntimeMemoryRegion.class, "_start");
    public final TeleInstanceReferenceFieldAccess StringConstant_value = new TeleInstanceReferenceFieldAccess(StringConstant.class, "_value", String.class);
    public final TeleInstanceReferenceFieldAccess TargetABI_callEntryPoint = new TeleInstanceReferenceFieldAccess(TargetABI.class, "_callEntryPoint", CallEntryPoint.class);
    public final TeleInstanceReferenceFieldAccess TargetMethod_abi = new TeleInstanceReferenceFieldAccess(TargetMethod.class, "_abi", TargetABI.class);
    public final TeleInstanceReferenceFieldAccess TargetMethod_catchBlockPositions = new TeleInstanceReferenceFieldAccess(TargetMethod.class, "_catchBlockPositions", int[].class);
    public final TeleInstanceReferenceFieldAccess TargetMethod_catchRangePositions = new TeleInstanceReferenceFieldAccess(TargetMethod.class, "_catchRangePositions", int[].class);
    public final TeleInstanceReferenceFieldAccess TargetMethod_classMethodActor = new TeleInstanceReferenceFieldAccess(TargetMethod.class, "_classMethodActor", ClassMethodActor.class);
    public final TeleInstanceReferenceFieldAccess TargetMethod_code = new TeleInstanceReferenceFieldAccess(TargetMethod.class, "_code", byte[].class);
    public final TeleInstanceWordFieldAccess TargetMethod_codeStart = new TeleInstanceWordFieldAccess(TargetMethod.class, "_codeStart");
    public final TeleInstanceReferenceFieldAccess TargetMethod_compressedJavaFrameDescriptors = new TeleInstanceReferenceFieldAccess(TargetMethod.class, "_compressedJavaFrameDescriptors", byte[].class);
    public final TeleInstanceReferenceFieldAccess TargetMethod_directCallees = new TeleInstanceReferenceFieldAccess(TargetMethod.class, "_directCallees", ClassMethodActor[].class);
    public final TeleInstanceReferenceFieldAccess TargetMethod_encodedInlineDataDescriptors = new TeleInstanceReferenceFieldAccess(TargetMethod.class, "_encodedInlineDataDescriptors", byte[].class);
    public final TeleInstanceIntFieldAccess TargetMethod_frameReferenceMapSize = new TeleInstanceIntFieldAccess(TargetMethod.class, "_frameReferenceMapSize");
    public final TeleInstanceIntFieldAccess TargetMethod_numberOfIndirectCalls = new TeleInstanceIntFieldAccess(TargetMethod.class, "_numberOfIndirectCalls");
    public final TeleInstanceIntFieldAccess TargetMethod_numberOfSafepoints = new TeleInstanceIntFieldAccess(TargetMethod.class, "_numberOfSafepoints");
    public final TeleInstanceReferenceFieldAccess TargetMethod_referenceLiterals = new TeleInstanceReferenceFieldAccess(TargetMethod.class, "_referenceLiterals", Object[].class);
    public final TeleInstanceReferenceFieldAccess TargetMethod_scalarLiteralBytes = new TeleInstanceReferenceFieldAccess(TargetMethod.class, "_scalarLiteralBytes", byte[].class);
    public final TeleInstanceReferenceFieldAccess TargetMethod_stopPositions = new TeleInstanceReferenceFieldAccess(TargetMethod.class, "_stopPositions", int[].class);
    public final TeleStaticIntFieldAccess TeleClassInfo_classActorCount = new TeleStaticIntFieldAccess(TeleClassInfo.class, "_classActorCount");
    public final TeleStaticReferenceFieldAccess TeleClassInfo_classActors = new TeleStaticReferenceFieldAccess(TeleClassInfo.class, "_classActors", ClassActor[].class);
    public final TeleStaticLongFieldAccess TeleHeapInfo_collectionEpoch = new TeleStaticLongFieldAccess(TeleHeapInfo.class, "_collectionEpoch");
    public final TeleStaticReferenceFieldAccess TeleHeapInfo_memoryRegions = new TeleStaticReferenceFieldAccess(TeleHeapInfo.class, "_memoryRegions", MemoryRegion[].class);
    public final TeleStaticLongFieldAccess TeleHeapInfo_rootEpoch = new TeleStaticLongFieldAccess(TeleHeapInfo.class, "_rootEpoch");
    public final TeleStaticReferenceFieldAccess TeleHeapInfo_roots = new TeleStaticReferenceFieldAccess(TeleHeapInfo.class, "_roots", Object[].class);
    public final TeleStaticIntFieldAccess Trace_level = new TeleStaticIntFieldAccess(Trace.class, "level");
    public final TeleStaticLongFieldAccess Trace_threshold = new TeleStaticLongFieldAccess(Trace.class, "threshold");
    public final TeleInstanceReferenceFieldAccess TupleClassActor_constantPool = new TeleInstanceReferenceFieldAccess(TupleClassActor.class, "_constantPool", ConstantPool.class);
    public final TeleInstanceReferenceFieldAccess Utf8Constant_string = new TeleInstanceReferenceFieldAccess(Utf8Constant.class, "_string", String.class);
    public final TeleInstanceReferenceFieldAccess VmThread_name = new TeleInstanceReferenceFieldAccess(VmThread.class, "_name", String.class);
    // END GENERATED CONTENT

    // Injected JDK fields:

    public final TeleInstanceReferenceFieldAccess Class_classActor = new TeleInstanceReferenceFieldAccess(Class.class, ClassActor.class, InjectedReferenceFieldActor.Class_classActor);
    public final TeleInstanceReferenceFieldAccess ClassLoader_classRegistry = new TeleInstanceReferenceFieldAccess(ClassLoader.class, ClassRegistry.class, InjectedReferenceFieldActor.ClassLoader_classRegistry);
    public final TeleInstanceReferenceFieldAccess Thread_VmThread = new TeleInstanceReferenceFieldAccess(Thread.class, VmThread.class, InjectedReferenceFieldActor.Thread_vmThread);
    public final TeleInstanceReferenceFieldAccess Field_fieldActor = new TeleInstanceReferenceFieldAccess(Field.class, FieldActor.class, InjectedReferenceFieldActor.Field_fieldActor);
    public final TeleInstanceReferenceFieldAccess Method_methodActor = new TeleInstanceReferenceFieldAccess(Method.class, MethodActor.class, InjectedReferenceFieldActor.Method_methodActor);
    public final TeleInstanceReferenceFieldAccess Constructor_methodActor = new TeleInstanceReferenceFieldAccess(Constructor.class, MethodActor.class, InjectedReferenceFieldActor.Constructor_methodActor);

    // Other JDK fields:

    public final TeleInstanceReferenceFieldAccess ArrayList_elementData = new TeleInstanceReferenceFieldAccess(ArrayList.class, "elementData", Object[].class);
    public final TeleInstanceIntFieldAccess Enum_ordinal = new TeleInstanceIntFieldAccess(Enum.class, "ordinal");
    public final TeleInstanceIntFieldAccess String_count = new TeleInstanceIntFieldAccess(String.class, "count");
    public final TeleInstanceIntFieldAccess String_offset = new TeleInstanceIntFieldAccess(String.class, "offset");
    public final TeleInstanceReferenceFieldAccess String_value = new TeleInstanceReferenceFieldAccess(String.class, "value", char[].class);
    // Checkstyle: resume field name check

    public static interface InspectedMemberReifier<Member_Type extends Member> {
        /**
         * Reifies a {@link Method}, {@link Field} or {@link Constructor} annotated with {@link INSPECTED} found on a
         * classpath search.
         *
         * @param member the member to be reified
         * @param writer the Java source to which the declaration of the reified member should be written
         */
        void reify(Member_Type member, IndentWriter writer);
    }

    public static <Member_Type extends Member> void updateSource(final Class sourceClass, final Class<Member_Type> memberClass, final InspectedMemberReifier<Member_Type> memberReifier, final boolean inInspector) {
        final File sourceFile = new File(JavaProject.findSourceDirectory(), sourceClass.getName().replace('.', File.separatorChar) + ".java").getAbsoluteFile();
        if (!sourceFile.exists()) {
            ProgramWarning.message("Source file does not exist: " + sourceFile.getAbsolutePath());
        }

        final Runnable runnable = new Runnable() {
            public void run() {
                final com.sun.max.Package rootPackage = new com.sun.max.Package();
                final Classpath classpath = Classpath.fromSystem();
                final PackageLoader packageLoader = new PackageLoader(ClassLoader.getSystemClassLoader(), classpath);
                if (inInspector) {
                    packageLoader.setTraceLevel(Integer.MAX_VALUE);
                }
                final CharArraySource charArrayWriter = new CharArraySource();
                final IndentWriter writer = new IndentWriter(new PrintWriter(charArrayWriter));
                writer.indent();
                final Set<Member> reified = new TreeSet<Member>(new Comparator<Member>() {
                    public int compare(Member member1, Member member2) {
                        if (member1.equals(member2)) {
                            return 0;
                        }
                        final int classNameComparison = member1.getDeclaringClass().getSimpleName().compareTo(member2.getDeclaringClass().getSimpleName());
                        if (classNameComparison != 0) {
                            return classNameComparison;
                        }
                        final int result = member1.getName().compareTo(member2.getName());
                        assert result != 0 : member1 + " " + member2;
                        return result;
                    }

                });
                for (MaxPackage maxPackage : rootPackage.getTransitiveSubPackages(classpath)) {
                    for (Class<?> c : packageLoader.load(maxPackage, false)) {
                        final AccessibleObject[] members = memberClass.equals(Method.class) ? c.getDeclaredMethods() : (memberClass.equals(Field.class) ? c.getDeclaredFields() : c.getDeclaredConstructors());
                        for (AccessibleObject member : members) {
                            if (member.getAnnotation(INSPECTED.class) != null) {
                                if (!reified.contains(member)) {
                                    reified.add((Member) member);
                                }

                            }
                        }
                    }
                }

                for (Member member : reified) {
                    memberReifier.reify(memberClass.cast(member), writer);
                }

                try {
                    final boolean changed = Files.updateGeneratedContent(sourceFile, charArrayWriter, "    // START GENERATED CONTENT", "    // END GENERATED CONTENT");
                    if (changed) {
                        ProgramWarning.message("The source file " + sourceFile + " was updated" + (inInspector ? ": recompile and restart the inspector" : ""));
                    } else {
                        Trace.line(1, "The source file " + sourceFile + " did not need to be updated.");
                    }
                } catch (IOException exception) {
                    if (inInspector) {
                        ProgramWarning.message("Error while verifying that " + sourceFile + " is up to date: " + exception);
                    } else {
                        ProgramError.unexpected(exception);
                    }
                }
            }
        };

        if (!inInspector) {
            runnable.run();
        } else {
            final Runnable inspectorRunnable = new Runnable() {
                public void run() {
                    Trace.begin(1, "Verifying that " + sourceClass + " is up to date");
                    try {
                        runnable.run();
                    } finally {
                        Trace.end(1, "Verifying that " + sourceClass + " is up to date");
                    }
                }
            };
            final Thread thread = new Thread(inspectorRunnable, "Inspected" + memberClass.getSimpleName() + "s verifier");
            thread.setPriority(Thread.MIN_PRIORITY);
            thread.start();
        }
    }

    public static void main(String[] args) {
        Trace.on(1);
        Trace.begin(1, "TeleFields updating GENERATED CONTENT");
        updateSource(false);
        Trace.end(1, "TeleFields updating GENERATED CONTENT");
    }

    private static void updateSource(boolean inInspector) {
        final InspectedMemberReifier<Field> fieldReifier = new InspectedMemberReifier<Field>() {
            public void reify(Field field, IndentWriter writer) {
                final Class c = field.getDeclaringClass();
                final boolean isStatic = Modifier.isStatic(field.getModifiers());
                final Class type = field.getType();
                final Kind kind = Kind.fromJava(type);
                final String holder = c.getName().substring(c.getPackage().getName().length() + 1);
                final String name = field.getName();
                final String kindName = kind.name().string();
                final String inspectorFieldName = holder + (name.charAt(0) == '_' ? name : '_' + name);
                final String inspectorFieldType = "Tele" + (isStatic ? "Static" : "Instance") + Strings.capitalizeFirst(kindName, true) + "FieldAccess";
                writer.print("public final " + inspectorFieldType + " " + inspectorFieldName + " = ");

                switch (kind.asEnum()) {
                    case BOOLEAN:
                    case BYTE:
                    case CHAR:
                    case SHORT:
                    case INT:
                    case FLOAT:
                    case LONG:
                    case DOUBLE:
                    case WORD: {
                        writer.print("new " + inspectorFieldType + "(" + holder.replace('$', '.') + ".class, \"" + name + "\")");
                        break;
                    }
                    case REFERENCE: {
                        writer.print("new " + inspectorFieldType + "(" + holder.replace('$', '.') + ".class, \"" + name + "\", " +  type.getSimpleName() + ".class)");
                        break;
                    }
                    default: {
                        ProgramError.unexpected("Invalid field kind: " + kind);
                    }
                }
                writer.println(";");
            }
        };
        updateSource(TeleFields.class, Field.class, fieldReifier, inInspector);
    }
}
