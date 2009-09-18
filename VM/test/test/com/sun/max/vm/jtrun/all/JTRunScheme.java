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
package test.com.sun.max.vm.jtrun.all;

import com.sun.max.annotate.*;
import com.sun.max.vm.*;

import test.com.sun.max.vm.jtrun.*;

public class JTRunScheme extends JTAbstractRunScheme {

    public JTRunScheme(VMConfiguration vmConfiguration) {
        super(vmConfiguration);
    }

    @Override
    @PROTOTYPE_ONLY
    public Class<?>[] getClassList() {
        return classList;
    }

    @PROTOTYPE_ONLY
// GENERATED TEST RUNS
    private static final Class<?>[] classList = {
        jtt.bytecode.BC_aaload.class,
        jtt.bytecode.BC_aaload_1.class,
        jtt.bytecode.BC_aastore.class,
        jtt.bytecode.BC_aload_0.class,
        jtt.bytecode.BC_aload_1.class,
        jtt.bytecode.BC_aload_2.class,
        jtt.bytecode.BC_aload_3.class,
        jtt.bytecode.BC_anewarray.class,
        jtt.bytecode.BC_areturn.class,
        jtt.bytecode.BC_arraylength.class,
        jtt.bytecode.BC_athrow.class,
        jtt.bytecode.BC_baload.class,
        jtt.bytecode.BC_bastore.class,
        jtt.bytecode.BC_caload.class,
        jtt.bytecode.BC_castore.class,
        jtt.bytecode.BC_checkcast01.class,
        jtt.bytecode.BC_checkcast02.class,
        jtt.bytecode.BC_d2f.class,
        jtt.bytecode.BC_d2i.class,
        jtt.bytecode.BC_d2i_nan.class,
        jtt.bytecode.BC_d2l.class,
        jtt.bytecode.BC_d2l_nan.class,
        jtt.bytecode.BC_dadd.class,
        jtt.bytecode.BC_daload.class,
        jtt.bytecode.BC_dastore.class,
        jtt.bytecode.BC_dcmp01.class,
        jtt.bytecode.BC_dcmp02.class,
        jtt.bytecode.BC_dcmp03.class,
        jtt.bytecode.BC_dcmp04.class,
        jtt.bytecode.BC_dcmp05.class,
        jtt.bytecode.BC_dcmp06.class,
        jtt.bytecode.BC_dcmp07.class,
        jtt.bytecode.BC_dcmp08.class,
        jtt.bytecode.BC_dcmp09.class,
        jtt.bytecode.BC_dcmp10.class,
        jtt.bytecode.BC_ddiv.class,
        jtt.bytecode.BC_dmul.class,
        jtt.bytecode.BC_dneg.class,
        jtt.bytecode.BC_drem.class,
        jtt.bytecode.BC_dreturn.class,
        jtt.bytecode.BC_dsub.class,
        jtt.bytecode.BC_f2d.class,
        jtt.bytecode.BC_f2i.class,
        jtt.bytecode.BC_f2i_2.class,
        jtt.bytecode.BC_f2i_nan.class,
        jtt.bytecode.BC_f2l.class,
        jtt.bytecode.BC_f2l_nan.class,
        jtt.bytecode.BC_fadd.class,
        jtt.bytecode.BC_faload.class,
        jtt.bytecode.BC_fastore.class,
        jtt.bytecode.BC_fcmp01.class,
        jtt.bytecode.BC_fcmp02.class,
        jtt.bytecode.BC_fcmp03.class,
        jtt.bytecode.BC_fcmp04.class,
        jtt.bytecode.BC_fcmp05.class,
        jtt.bytecode.BC_fcmp06.class,
        jtt.bytecode.BC_fcmp07.class,
        jtt.bytecode.BC_fcmp08.class,
        jtt.bytecode.BC_fcmp09.class,
        jtt.bytecode.BC_fcmp10.class,
        jtt.bytecode.BC_fdiv.class,
        jtt.bytecode.BC_fload.class,
        jtt.bytecode.BC_fload_2.class,
        jtt.bytecode.BC_fmul.class,
        jtt.bytecode.BC_fneg.class,
        jtt.bytecode.BC_frem.class,
        jtt.bytecode.BC_freturn.class,
        jtt.bytecode.BC_fsub.class,
        jtt.bytecode.BC_getfield.class,
        jtt.bytecode.BC_getstatic_b.class,
        jtt.bytecode.BC_getstatic_c.class,
        jtt.bytecode.BC_getstatic_d.class,
        jtt.bytecode.BC_getstatic_f.class,
        jtt.bytecode.BC_getstatic_i.class,
        jtt.bytecode.BC_getstatic_l.class,
        jtt.bytecode.BC_getstatic_s.class,
        jtt.bytecode.BC_getstatic_z.class,
        jtt.bytecode.BC_i2b.class,
        jtt.bytecode.BC_i2c.class,
        jtt.bytecode.BC_i2d.class,
        jtt.bytecode.BC_i2f.class,
        jtt.bytecode.BC_i2l.class,
        jtt.bytecode.BC_i2s.class,
        jtt.bytecode.BC_iadd.class,
        jtt.bytecode.BC_iadd2.class,
        jtt.bytecode.BC_iadd3.class,
        jtt.bytecode.BC_iaload.class,
        jtt.bytecode.BC_iand.class,
        jtt.bytecode.BC_iastore.class,
        jtt.bytecode.BC_iconst.class,
        jtt.bytecode.BC_idiv.class,
        jtt.bytecode.BC_ifeq.class,
        jtt.bytecode.BC_ifeq_2.class,
        jtt.bytecode.BC_ifeq_3.class,
        jtt.bytecode.BC_ifge.class,
        jtt.bytecode.BC_ifge_2.class,
        jtt.bytecode.BC_ifge_3.class,
        jtt.bytecode.BC_ifgt.class,
        jtt.bytecode.BC_ificmplt1.class,
        jtt.bytecode.BC_ificmplt2.class,
        jtt.bytecode.BC_ificmpne1.class,
        jtt.bytecode.BC_ificmpne2.class,
        jtt.bytecode.BC_ifle.class,
        jtt.bytecode.BC_iflt.class,
        jtt.bytecode.BC_ifne.class,
        jtt.bytecode.BC_ifnonnull.class,
        jtt.bytecode.BC_ifnonnull_2.class,
        jtt.bytecode.BC_ifnonnull_3.class,
        jtt.bytecode.BC_ifnull.class,
        jtt.bytecode.BC_ifnull_2.class,
        jtt.bytecode.BC_ifnull_3.class,
        jtt.bytecode.BC_iinc_1.class,
        jtt.bytecode.BC_iinc_2.class,
        jtt.bytecode.BC_iinc_3.class,
        jtt.bytecode.BC_iinc_4.class,
        jtt.bytecode.BC_iload_0.class,
        jtt.bytecode.BC_iload_0_1.class,
        jtt.bytecode.BC_iload_0_2.class,
        jtt.bytecode.BC_iload_1.class,
        jtt.bytecode.BC_iload_1_1.class,
        jtt.bytecode.BC_iload_2.class,
        jtt.bytecode.BC_iload_3.class,
        jtt.bytecode.BC_imul.class,
        jtt.bytecode.BC_ineg.class,
        jtt.bytecode.BC_instanceof.class,
        jtt.bytecode.BC_invokeinterface.class,
        jtt.bytecode.BC_invokespecial.class,
        jtt.bytecode.BC_invokespecial2.class,
        jtt.bytecode.BC_invokestatic.class,
        jtt.bytecode.BC_invokevirtual.class,
        jtt.bytecode.BC_ior.class,
        jtt.bytecode.BC_irem.class,
        jtt.bytecode.BC_ireturn.class,
        jtt.bytecode.BC_ishl.class,
        jtt.bytecode.BC_ishr.class,
        jtt.bytecode.BC_isub.class,
        jtt.bytecode.BC_iushr.class,
        jtt.bytecode.BC_ixor.class,
        jtt.bytecode.BC_l2d.class,
        jtt.bytecode.BC_l2f.class,
        jtt.bytecode.BC_l2i.class,
        jtt.bytecode.BC_ladd.class,
        jtt.bytecode.BC_ladd2.class,
        jtt.bytecode.BC_laload.class,
        jtt.bytecode.BC_land.class,
        jtt.bytecode.BC_lastore.class,
        jtt.bytecode.BC_lcmp.class,
        jtt.bytecode.BC_ldc_01.class,
        jtt.bytecode.BC_ldc_02.class,
        jtt.bytecode.BC_ldc_03.class,
        jtt.bytecode.BC_ldc_04.class,
        jtt.bytecode.BC_ldc_05.class,
        jtt.bytecode.BC_ldc_06.class,
        jtt.bytecode.BC_ldiv.class,
        jtt.bytecode.BC_lload_0.class,
        jtt.bytecode.BC_lload_01.class,
        jtt.bytecode.BC_lload_1.class,
        jtt.bytecode.BC_lload_2.class,
        jtt.bytecode.BC_lload_3.class,
        jtt.bytecode.BC_lmul.class,
        jtt.bytecode.BC_lneg.class,
        jtt.bytecode.BC_lookupswitch01.class,
        jtt.bytecode.BC_lookupswitch02.class,
        jtt.bytecode.BC_lookupswitch03.class,
        jtt.bytecode.BC_lookupswitch04.class,
        jtt.bytecode.BC_lor.class,
        jtt.bytecode.BC_lrem.class,
        jtt.bytecode.BC_lreturn.class,
        jtt.bytecode.BC_lshl.class,
        jtt.bytecode.BC_lshr.class,
        jtt.bytecode.BC_lsub.class,
        jtt.bytecode.BC_lushr.class,
        jtt.bytecode.BC_lxor.class,
        jtt.bytecode.BC_monitorenter.class,
        jtt.bytecode.BC_multianewarray01.class,
        jtt.bytecode.BC_multianewarray02.class,
        jtt.bytecode.BC_multianewarray03.class,
        jtt.bytecode.BC_multianewarray04.class,
        jtt.bytecode.BC_new.class,
        jtt.bytecode.BC_newarray.class,
        jtt.bytecode.BC_putfield.class,
        jtt.bytecode.BC_putstatic.class,
        jtt.bytecode.BC_saload.class,
        jtt.bytecode.BC_sastore.class,
        jtt.bytecode.BC_tableswitch.class,
        jtt.bytecode.BC_tableswitch2.class,
        jtt.bytecode.BC_tableswitch3.class,
        jtt.bytecode.BC_tableswitch4.class,
        jtt.bytecode.BC_wide01.class,
        jtt.bytecode.BC_wide02.class,
        jtt.directives.Fold01.class,
        jtt.directives.Fold02.class,
        jtt.directives.Fold03.class,
        jtt.directives.Inline01.class,
        jtt.directives.Prototyping01.class,
        jtt.except.BC_aaload.class,
        jtt.except.BC_aastore.class,
        jtt.except.BC_anewarray.class,
        jtt.except.BC_arraylength.class,
        jtt.except.BC_athrow.class,
        jtt.except.BC_athrow1.class,
        jtt.except.BC_athrow2.class,
        jtt.except.BC_athrow3.class,
        jtt.except.BC_baload.class,
        jtt.except.BC_bastore.class,
        jtt.except.BC_caload.class,
        jtt.except.BC_castore.class,
        jtt.except.BC_checkcast.class,
        jtt.except.BC_checkcast1.class,
        jtt.except.BC_checkcast2.class,
        jtt.except.BC_checkcast3.class,
        jtt.except.BC_daload.class,
        jtt.except.BC_dastore.class,
        jtt.except.BC_faload.class,
        jtt.except.BC_fastore.class,
        jtt.except.BC_getfield.class,
        jtt.except.BC_iaload.class,
        jtt.except.BC_iastore.class,
        jtt.except.BC_idiv.class,
        jtt.except.BC_idiv2.class,
        jtt.except.BC_invokevirtual01.class,
        jtt.except.BC_irem.class,
        jtt.except.BC_laload.class,
        jtt.except.BC_lastore.class,
        jtt.except.BC_ldiv.class,
        jtt.except.BC_ldiv2.class,
        jtt.except.BC_lrem.class,
        jtt.except.BC_monitorenter.class,
        jtt.except.BC_multianewarray.class,
        jtt.except.BC_newarray.class,
        jtt.except.BC_putfield.class,
        jtt.except.BC_saload.class,
        jtt.except.BC_sastore.class,
        jtt.except.Catch_Loop01.class,
        jtt.except.Catch_Loop02.class,
        jtt.except.Catch_NPE_01.class,
        jtt.except.Catch_NPE_02.class,
        jtt.except.Catch_NPE_03.class,
        jtt.except.Catch_NPE_04.class,
        jtt.except.Catch_NPE_05.class,
        jtt.except.Catch_NPE_06.class,
        jtt.except.Catch_NPE_07.class,
        jtt.except.Catch_NPE_08.class,
        jtt.except.Catch_StackOverflowError_01.class,
        jtt.except.Catch_StackOverflowError_02.class,
        jtt.except.Catch_StackOverflowError_03.class,
        jtt.except.Catch_Two01.class,
        jtt.except.Catch_Two02.class,
        jtt.except.Catch_Two03.class,
        jtt.except.Except_Synchronized01.class,
        jtt.except.Except_Synchronized02.class,
        jtt.except.Except_Synchronized03.class,
        jtt.except.Except_Synchronized04.class,
        jtt.except.Throw_InCatch01.class,
        jtt.except.Throw_InCatch02.class,
        jtt.except.Throw_InCatch03.class,
        jtt.except.Throw_NPE_01.class,
        jtt.except.Throw_Synchronized01.class,
        jtt.except.Throw_Synchronized02.class,
        jtt.except.Throw_Synchronized03.class,
        jtt.except.Throw_Synchronized04.class,
        jtt.except.Throw_Synchronized05.class,
        jtt.hotpath.HP_allocate01.class,
        jtt.hotpath.HP_allocate02.class,
        jtt.hotpath.HP_allocate03.class,
        jtt.hotpath.HP_array01.class,
        jtt.hotpath.HP_array02.class,
        jtt.hotpath.HP_array03.class,
        jtt.hotpath.HP_array04.class,
        jtt.hotpath.HP_control01.class,
        jtt.hotpath.HP_control02.class,
        jtt.hotpath.HP_convert01.class,
        jtt.hotpath.HP_count.class,
        jtt.hotpath.HP_dead01.class,
        jtt.hotpath.HP_demo01.class,
        jtt.hotpath.HP_field01.class,
        jtt.hotpath.HP_field02.class,
        jtt.hotpath.HP_field03.class,
        jtt.hotpath.HP_field04.class,
        jtt.hotpath.HP_idea.class,
        jtt.hotpath.HP_inline01.class,
        jtt.hotpath.HP_inline02.class,
        jtt.hotpath.HP_invoke01.class,
        jtt.hotpath.HP_life.class,
        jtt.hotpath.HP_nest01.class,
        jtt.hotpath.HP_nest02.class,
        jtt.hotpath.HP_scope01.class,
        jtt.hotpath.HP_scope02.class,
        jtt.hotpath.HP_series.class,
        jtt.hotpath.HP_trees01.class,
        jtt.jasm.Loop00.class,
        jtt.jdk.Class_getName.class,
        jtt.jdk.EnumMap01.class,
        jtt.jdk.EnumMap02.class,
        jtt.jdk.System_currentTimeMillis01.class,
        jtt.jdk.System_currentTimeMillis02.class,
        jtt.jdk.System_nanoTime01.class,
        jtt.jdk.System_nanoTime02.class,
        jtt.jdk.UnsafeAccess01.class,
        jtt.jni.JNI_OverflowArguments.class,
        jtt.jvmni.JVM_ArrayCopy01.class,
        jtt.jvmni.JVM_GetClassContext01.class,
        jtt.jvmni.JVM_GetClassContext02.class,
        jtt.jvmni.JVM_GetFreeMemory01.class,
        jtt.jvmni.JVM_GetMaxMemory01.class,
        jtt.jvmni.JVM_GetTotalMemory01.class,
        jtt.jvmni.JVM_IsNaN01.class,
        jtt.lang.Boxed_TYPE_01.class,
        jtt.lang.Bridge_method01.class,
        jtt.lang.ClassLoader_loadClass01.class,
        jtt.lang.Class_Literal01.class,
        jtt.lang.Class_asSubclass01.class,
        jtt.lang.Class_cast01.class,
        jtt.lang.Class_cast02.class,
        jtt.lang.Class_forName01.class,
        jtt.lang.Class_forName02.class,
        jtt.lang.Class_forName03.class,
        jtt.lang.Class_forName04.class,
        jtt.lang.Class_forName05.class,
        jtt.lang.Class_getComponentType01.class,
        jtt.lang.Class_getName01.class,
        jtt.lang.Class_getName02.class,
        jtt.lang.Class_getSimpleName01.class,
        jtt.lang.Class_getSimpleName02.class,
        jtt.lang.Class_getSuperClass01.class,
        jtt.lang.Class_isArray01.class,
        jtt.lang.Class_isAssignableFrom01.class,
        jtt.lang.Class_isAssignableFrom02.class,
        jtt.lang.Class_isAssignableFrom03.class,
        jtt.lang.Class_isInstance01.class,
        jtt.lang.Class_isInstance02.class,
        jtt.lang.Class_isInstance03.class,
        jtt.lang.Class_isInstance04.class,
        jtt.lang.Class_isInstance05.class,
        jtt.lang.Class_isInstance06.class,
        jtt.lang.Class_isInterface01.class,
        jtt.lang.Class_isPrimitive01.class,
        jtt.lang.Double_toString.class,
        jtt.lang.Int_greater01.class,
        jtt.lang.Int_greater02.class,
        jtt.lang.Int_greater03.class,
        jtt.lang.Int_greaterEqual01.class,
        jtt.lang.Int_greaterEqual02.class,
        jtt.lang.Int_greaterEqual03.class,
        jtt.lang.Int_less01.class,
        jtt.lang.Int_less02.class,
        jtt.lang.Int_less03.class,
        jtt.lang.Int_lessEqual01.class,
        jtt.lang.Int_lessEqual02.class,
        jtt.lang.Int_lessEqual03.class,
        jtt.lang.JDK_ClassLoaders01.class,
        jtt.lang.JDK_ClassLoaders02.class,
        jtt.lang.Long_greater01.class,
        jtt.lang.Long_greater02.class,
        jtt.lang.Long_greater03.class,
        jtt.lang.Long_greaterEqual01.class,
        jtt.lang.Long_greaterEqual02.class,
        jtt.lang.Long_greaterEqual03.class,
        jtt.lang.Long_less01.class,
        jtt.lang.Long_less02.class,
        jtt.lang.Long_less03.class,
        jtt.lang.Long_lessEqual01.class,
        jtt.lang.Long_lessEqual02.class,
        jtt.lang.Long_lessEqual03.class,
        jtt.lang.Long_reverseBytes01.class,
        jtt.lang.Long_reverseBytes02.class,
        jtt.lang.Math_pow.class,
        jtt.lang.Object_clone01.class,
        jtt.lang.Object_clone02.class,
        jtt.lang.Object_equals01.class,
        jtt.lang.Object_getClass01.class,
        jtt.lang.Object_hashCode01.class,
        jtt.lang.Object_notify01.class,
        jtt.lang.Object_notify02.class,
        jtt.lang.Object_notifyAll01.class,
        jtt.lang.Object_notifyAll02.class,
        jtt.lang.Object_toString01.class,
        jtt.lang.Object_toString02.class,
        jtt.lang.Object_wait01.class,
        jtt.lang.Object_wait02.class,
        jtt.lang.Object_wait03.class,
        jtt.lang.ProcessEnvironment_init.class,
        jtt.lang.StringCoding_Scale.class,
        jtt.lang.String_intern01.class,
        jtt.lang.String_intern02.class,
        jtt.lang.String_intern03.class,
        jtt.lang.String_valueOf01.class,
        jtt.lang.System_identityHashCode01.class,
        jtt.max.ImmortalHeap_allocation.class,
        jtt.max.ImmortalHeap_gc.class,
        jtt.max.ImmortalHeap_switching.class,
        jtt.max.Unsigned_idiv01.class,
        jtt.micro.ArrayCompare01.class,
        jtt.micro.ArrayCompare02.class,
        jtt.micro.BC_invokevirtual2.class,
        jtt.micro.BigByteParams01.class,
        jtt.micro.BigDoubleParams02.class,
        jtt.micro.BigFloatParams01.class,
        jtt.micro.BigFloatParams02.class,
        jtt.micro.BigIntParams01.class,
        jtt.micro.BigIntParams02.class,
        jtt.micro.BigLongParams02.class,
        jtt.micro.BigMixedParams01.class,
        jtt.micro.BigMixedParams02.class,
        jtt.micro.BigMixedParams03.class,
        jtt.micro.BigObjectParams01.class,
        jtt.micro.BigObjectParams02.class,
        jtt.micro.BigParamsAlignment.class,
        jtt.micro.BigShortParams01.class,
        jtt.micro.Bubblesort.class,
        jtt.micro.Fibonacci.class,
        jtt.micro.InvokeVirtual_01.class,
        jtt.micro.InvokeVirtual_02.class,
        jtt.micro.Loop01.class,
        jtt.micro.Loop02.class,
        jtt.micro.Loop03.class,
        jtt.micro.Loop04.class,
        jtt.micro.Loop05.class,
        jtt.micro.Loop06.class,
        jtt.micro.LoopSwitch01.class,
        jtt.micro.Matrix01.class,
        jtt.micro.StrangeFrames.class,
        jtt.micro.String_format01.class,
        jtt.micro.String_format02.class,
        jtt.micro.VarArgs_String01.class,
        jtt.micro.VarArgs_boolean01.class,
        jtt.micro.VarArgs_byte01.class,
        jtt.micro.VarArgs_char01.class,
        jtt.micro.VarArgs_double01.class,
        jtt.micro.VarArgs_float01.class,
        jtt.micro.VarArgs_int01.class,
        jtt.micro.VarArgs_long01.class,
        jtt.micro.VarArgs_short01.class,
        jtt.optimize.ArrayLength01.class,
        jtt.optimize.BC_idiv_16.class,
        jtt.optimize.BC_idiv_4.class,
        jtt.optimize.BC_imul_16.class,
        jtt.optimize.BC_imul_4.class,
        jtt.optimize.BC_ldiv_16.class,
        jtt.optimize.BC_ldiv_4.class,
        jtt.optimize.BC_lmul_16.class,
        jtt.optimize.BC_lmul_4.class,
        jtt.optimize.BC_lshr_C16.class,
        jtt.optimize.BC_lshr_C24.class,
        jtt.optimize.BC_lshr_C32.class,
        jtt.optimize.BlockSkip01.class,
        jtt.optimize.DeadCode01.class,
        jtt.optimize.Fold_Cast01.class,
        jtt.optimize.Fold_Convert01.class,
        jtt.optimize.Fold_Convert02.class,
        jtt.optimize.Fold_Convert03.class,
        jtt.optimize.Fold_Convert04.class,
        jtt.optimize.Fold_Double01.class,
        jtt.optimize.Fold_Double02.class,
        jtt.optimize.Fold_Float01.class,
        jtt.optimize.Fold_Float02.class,
        jtt.optimize.Fold_InstanceOf01.class,
        jtt.optimize.Fold_Int01.class,
        jtt.optimize.Fold_Int02.class,
        jtt.optimize.Fold_Long01.class,
        jtt.optimize.Fold_Long02.class,
        jtt.optimize.Fold_Math01.class,
        jtt.optimize.Inline01.class,
        jtt.optimize.Inline02.class,
        jtt.optimize.List_reorder_bug.class,
        jtt.optimize.NCE_01.class,
        jtt.optimize.NCE_02.class,
        jtt.optimize.NCE_03.class,
        jtt.optimize.NCE_04.class,
        jtt.optimize.NCE_FlowSensitive01.class,
        jtt.optimize.NCE_FlowSensitive03.class,
        jtt.optimize.NCE_FlowSensitive04.class,
        jtt.optimize.Narrow_byte01.class,
        jtt.optimize.Narrow_byte02.class,
        jtt.optimize.Narrow_byte03.class,
        jtt.optimize.Narrow_char01.class,
        jtt.optimize.Narrow_char02.class,
        jtt.optimize.Narrow_char03.class,
        jtt.optimize.Narrow_short01.class,
        jtt.optimize.Narrow_short02.class,
        jtt.optimize.Narrow_short03.class,
        jtt.optimize.Phi01.class,
        jtt.optimize.Phi02.class,
        jtt.optimize.Phi03.class,
        jtt.optimize.Reduce_Convert01.class,
        jtt.optimize.Reduce_Double01.class,
        jtt.optimize.Reduce_Float01.class,
        jtt.optimize.Reduce_Int01.class,
        jtt.optimize.Reduce_Int02.class,
        jtt.optimize.Reduce_Int03.class,
        jtt.optimize.Reduce_Int04.class,
        jtt.optimize.Reduce_IntShift01.class,
        jtt.optimize.Reduce_IntShift02.class,
        jtt.optimize.Reduce_Long01.class,
        jtt.optimize.Reduce_Long02.class,
        jtt.optimize.Reduce_Long03.class,
        jtt.optimize.Reduce_Long04.class,
        jtt.optimize.Reduce_LongShift01.class,
        jtt.optimize.Reduce_LongShift02.class,
        jtt.optimize.Switch01.class,
        jtt.optimize.Switch02.class,
        jtt.optimize.TypeCastElem.class,
        jtt.optimize.VN_Cast01.class,
        jtt.optimize.VN_Cast02.class,
        jtt.optimize.VN_Convert01.class,
        jtt.optimize.VN_Convert02.class,
        jtt.optimize.VN_Double01.class,
        jtt.optimize.VN_Double02.class,
        jtt.optimize.VN_Field01.class,
        jtt.optimize.VN_Field02.class,
        jtt.optimize.VN_Float01.class,
        jtt.optimize.VN_Float02.class,
        jtt.optimize.VN_InstanceOf01.class,
        jtt.optimize.VN_InstanceOf02.class,
        jtt.optimize.VN_InstanceOf03.class,
        jtt.optimize.VN_Int01.class,
        jtt.optimize.VN_Int02.class,
        jtt.optimize.VN_Int03.class,
        jtt.optimize.VN_Long01.class,
        jtt.optimize.VN_Long02.class,
        jtt.optimize.VN_Long03.class,
        jtt.optimize.VN_Loop01.class,
        jtt.reflect.Array_get01.class,
        jtt.reflect.Array_get02.class,
        jtt.reflect.Array_get03.class,
        jtt.reflect.Array_getBoolean01.class,
        jtt.reflect.Array_getByte01.class,
        jtt.reflect.Array_getChar01.class,
        jtt.reflect.Array_getDouble01.class,
        jtt.reflect.Array_getFloat01.class,
        jtt.reflect.Array_getInt01.class,
        jtt.reflect.Array_getLength01.class,
        jtt.reflect.Array_getLong01.class,
        jtt.reflect.Array_getShort01.class,
        jtt.reflect.Array_newInstance01.class,
        jtt.reflect.Array_newInstance02.class,
        jtt.reflect.Array_newInstance03.class,
        jtt.reflect.Array_newInstance04.class,
        jtt.reflect.Array_newInstance05.class,
        jtt.reflect.Array_newInstance06.class,
        jtt.reflect.Array_set01.class,
        jtt.reflect.Array_set02.class,
        jtt.reflect.Array_set03.class,
        jtt.reflect.Array_setBoolean01.class,
        jtt.reflect.Array_setByte01.class,
        jtt.reflect.Array_setChar01.class,
        jtt.reflect.Array_setDouble01.class,
        jtt.reflect.Array_setFloat01.class,
        jtt.reflect.Array_setInt01.class,
        jtt.reflect.Array_setLong01.class,
        jtt.reflect.Array_setShort01.class,
        jtt.reflect.Class_getDeclaredField01.class,
        jtt.reflect.Class_getDeclaredMethod01.class,
        jtt.reflect.Class_getField01.class,
        jtt.reflect.Class_getField02.class,
        jtt.reflect.Class_getMethod01.class,
        jtt.reflect.Class_getMethod02.class,
        jtt.reflect.Class_newInstance01.class,
        jtt.reflect.Class_newInstance02.class,
        jtt.reflect.Class_newInstance03.class,
        jtt.reflect.Class_newInstance06.class,
        jtt.reflect.Class_newInstance07.class,
        jtt.reflect.Field_get01.class,
        jtt.reflect.Field_get02.class,
        jtt.reflect.Field_get03.class,
        jtt.reflect.Field_get04.class,
        jtt.reflect.Field_getType01.class,
        jtt.reflect.Field_set01.class,
        jtt.reflect.Field_set02.class,
        jtt.reflect.Field_set03.class,
        jtt.reflect.Invoke_main01.class,
        jtt.reflect.Invoke_main02.class,
        jtt.reflect.Invoke_main03.class,
        jtt.reflect.Invoke_virtual01.class,
        jtt.reflect.Method_getParameterTypes01.class,
        jtt.reflect.Method_getReturnType01.class,
        jtt.reflect.Reflection_getCallerClass01.class,
        jtt.threads.Monitor_contended01.class,
        jtt.threads.Monitor_notowner01.class,
        jtt.threads.Monitorenter01.class,
        jtt.threads.Monitorenter02.class,
        jtt.threads.Object_wait01.class,
        jtt.threads.Object_wait02.class,
        jtt.threads.Object_wait03.class,
        jtt.threads.Object_wait04.class,
        jtt.threads.Thread_currentThread01.class,
        jtt.threads.Thread_getState01.class,
        jtt.threads.Thread_getState02.class,
        jtt.threads.Thread_holdsLock01.class,
        jtt.threads.Thread_isAlive01.class,
        jtt.threads.Thread_isInterrupted01.class,
        jtt.threads.Thread_isInterrupted02.class,
        jtt.threads.Thread_isInterrupted03.class,
        jtt.threads.Thread_isInterrupted04.class,
        jtt.threads.Thread_join01.class,
        jtt.threads.Thread_join02.class,
        jtt.threads.Thread_join03.class,
        jtt.threads.Thread_new01.class,
        jtt.threads.Thread_new02.class,
        jtt.threads.Thread_setPriority01.class,
        jtt.threads.Thread_sleep01.class,
        jtt.threads.Thread_start01.class,
        jtt.threads.Thread_yield01.class
    };
    @Override
    public void runTests() {
        JTUtil.total = testEnd - testStart;
        JTUtil.testNum = testStart;
        while (JTUtil.testNum < testEnd) {
            switch(JTUtil.testNum) {
                case 0:
                    JTRuns.jtt_bytecode_BC_aaload();
                    break;
                case 1:
                    JTRuns.jtt_bytecode_BC_aaload_1();
                    break;
                case 2:
                    JTRuns.jtt_bytecode_BC_aastore();
                    break;
                case 3:
                    JTRuns.jtt_bytecode_BC_aload_0();
                    break;
                case 4:
                    JTRuns.jtt_bytecode_BC_aload_1();
                    break;
                case 5:
                    JTRuns.jtt_bytecode_BC_aload_2();
                    break;
                case 6:
                    JTRuns.jtt_bytecode_BC_aload_3();
                    break;
                case 7:
                    JTRuns.jtt_bytecode_BC_anewarray();
                    break;
                case 8:
                    JTRuns.jtt_bytecode_BC_areturn();
                    break;
                case 9:
                    JTRuns.jtt_bytecode_BC_arraylength();
                    break;
                case 10:
                    JTRuns.jtt_bytecode_BC_athrow();
                    break;
                case 11:
                    JTRuns.jtt_bytecode_BC_baload();
                    break;
                case 12:
                    JTRuns.jtt_bytecode_BC_bastore();
                    break;
                case 13:
                    JTRuns.jtt_bytecode_BC_caload();
                    break;
                case 14:
                    JTRuns.jtt_bytecode_BC_castore();
                    break;
                case 15:
                    JTRuns.jtt_bytecode_BC_checkcast01();
                    break;
                case 16:
                    JTRuns.jtt_bytecode_BC_checkcast02();
                    break;
                case 17:
                    JTRuns.jtt_bytecode_BC_d2f();
                    break;
                case 18:
                    JTRuns.jtt_bytecode_BC_d2i();
                    break;
                case 19:
                    JTRuns.jtt_bytecode_BC_d2i_nan();
                    break;
                case 20:
                    JTRuns.jtt_bytecode_BC_d2l();
                    break;
                case 21:
                    JTRuns.jtt_bytecode_BC_d2l_nan();
                    break;
                case 22:
                    JTRuns.jtt_bytecode_BC_dadd();
                    break;
                case 23:
                    JTRuns.jtt_bytecode_BC_daload();
                    break;
                case 24:
                    JTRuns.jtt_bytecode_BC_dastore();
                    break;
                case 25:
                    JTRuns.jtt_bytecode_BC_dcmp01();
                    break;
                case 26:
                    JTRuns.jtt_bytecode_BC_dcmp02();
                    break;
                case 27:
                    JTRuns.jtt_bytecode_BC_dcmp03();
                    break;
                case 28:
                    JTRuns.jtt_bytecode_BC_dcmp04();
                    break;
                case 29:
                    JTRuns.jtt_bytecode_BC_dcmp05();
                    break;
                case 30:
                    JTRuns.jtt_bytecode_BC_dcmp06();
                    break;
                case 31:
                    JTRuns.jtt_bytecode_BC_dcmp07();
                    break;
                case 32:
                    JTRuns.jtt_bytecode_BC_dcmp08();
                    break;
                case 33:
                    JTRuns.jtt_bytecode_BC_dcmp09();
                    break;
                case 34:
                    JTRuns.jtt_bytecode_BC_dcmp10();
                    break;
                case 35:
                    JTRuns.jtt_bytecode_BC_ddiv();
                    break;
                case 36:
                    JTRuns.jtt_bytecode_BC_dmul();
                    break;
                case 37:
                    JTRuns.jtt_bytecode_BC_dneg();
                    break;
                case 38:
                    JTRuns.jtt_bytecode_BC_drem();
                    break;
                case 39:
                    JTRuns.jtt_bytecode_BC_dreturn();
                    break;
                case 40:
                    JTRuns.jtt_bytecode_BC_dsub();
                    break;
                case 41:
                    JTRuns.jtt_bytecode_BC_f2d();
                    break;
                case 42:
                    JTRuns.jtt_bytecode_BC_f2i();
                    break;
                case 43:
                    JTRuns.jtt_bytecode_BC_f2i_2();
                    break;
                case 44:
                    JTRuns.jtt_bytecode_BC_f2i_nan();
                    break;
                case 45:
                    JTRuns.jtt_bytecode_BC_f2l();
                    break;
                case 46:
                    JTRuns.jtt_bytecode_BC_f2l_nan();
                    break;
                case 47:
                    JTRuns.jtt_bytecode_BC_fadd();
                    break;
                case 48:
                    JTRuns.jtt_bytecode_BC_faload();
                    break;
                case 49:
                    JTRuns.jtt_bytecode_BC_fastore();
                    break;
                case 50:
                    JTRuns.jtt_bytecode_BC_fcmp01();
                    break;
                case 51:
                    JTRuns.jtt_bytecode_BC_fcmp02();
                    break;
                case 52:
                    JTRuns.jtt_bytecode_BC_fcmp03();
                    break;
                case 53:
                    JTRuns.jtt_bytecode_BC_fcmp04();
                    break;
                case 54:
                    JTRuns.jtt_bytecode_BC_fcmp05();
                    break;
                case 55:
                    JTRuns.jtt_bytecode_BC_fcmp06();
                    break;
                case 56:
                    JTRuns.jtt_bytecode_BC_fcmp07();
                    break;
                case 57:
                    JTRuns.jtt_bytecode_BC_fcmp08();
                    break;
                case 58:
                    JTRuns.jtt_bytecode_BC_fcmp09();
                    break;
                case 59:
                    JTRuns.jtt_bytecode_BC_fcmp10();
                    break;
                case 60:
                    JTRuns.jtt_bytecode_BC_fdiv();
                    break;
                case 61:
                    JTRuns.jtt_bytecode_BC_fload();
                    break;
                case 62:
                    JTRuns.jtt_bytecode_BC_fload_2();
                    break;
                case 63:
                    JTRuns.jtt_bytecode_BC_fmul();
                    break;
                case 64:
                    JTRuns.jtt_bytecode_BC_fneg();
                    break;
                case 65:
                    JTRuns.jtt_bytecode_BC_frem();
                    break;
                case 66:
                    JTRuns.jtt_bytecode_BC_freturn();
                    break;
                case 67:
                    JTRuns.jtt_bytecode_BC_fsub();
                    break;
                case 68:
                    JTRuns.jtt_bytecode_BC_getfield();
                    break;
                case 69:
                    JTRuns.jtt_bytecode_BC_getstatic_b();
                    break;
                case 70:
                    JTRuns.jtt_bytecode_BC_getstatic_c();
                    break;
                case 71:
                    JTRuns.jtt_bytecode_BC_getstatic_d();
                    break;
                case 72:
                    JTRuns.jtt_bytecode_BC_getstatic_f();
                    break;
                case 73:
                    JTRuns.jtt_bytecode_BC_getstatic_i();
                    break;
                case 74:
                    JTRuns.jtt_bytecode_BC_getstatic_l();
                    break;
                case 75:
                    JTRuns.jtt_bytecode_BC_getstatic_s();
                    break;
                case 76:
                    JTRuns.jtt_bytecode_BC_getstatic_z();
                    break;
                case 77:
                    JTRuns.jtt_bytecode_BC_i2b();
                    break;
                case 78:
                    JTRuns.jtt_bytecode_BC_i2c();
                    break;
                case 79:
                    JTRuns.jtt_bytecode_BC_i2d();
                    break;
                case 80:
                    JTRuns.jtt_bytecode_BC_i2f();
                    break;
                case 81:
                    JTRuns.jtt_bytecode_BC_i2l();
                    break;
                case 82:
                    JTRuns.jtt_bytecode_BC_i2s();
                    break;
                case 83:
                    JTRuns.jtt_bytecode_BC_iadd();
                    break;
                case 84:
                    JTRuns.jtt_bytecode_BC_iadd2();
                    break;
                case 85:
                    JTRuns.jtt_bytecode_BC_iadd3();
                    break;
                case 86:
                    JTRuns.jtt_bytecode_BC_iaload();
                    break;
                case 87:
                    JTRuns.jtt_bytecode_BC_iand();
                    break;
                case 88:
                    JTRuns.jtt_bytecode_BC_iastore();
                    break;
                case 89:
                    JTRuns.jtt_bytecode_BC_iconst();
                    break;
                case 90:
                    JTRuns.jtt_bytecode_BC_idiv();
                    break;
                case 91:
                    JTRuns.jtt_bytecode_BC_ifeq();
                    break;
                case 92:
                    JTRuns.jtt_bytecode_BC_ifeq_2();
                    break;
                case 93:
                    JTRuns.jtt_bytecode_BC_ifeq_3();
                    break;
                case 94:
                    JTRuns.jtt_bytecode_BC_ifge();
                    break;
                case 95:
                    JTRuns.jtt_bytecode_BC_ifge_2();
                    break;
                case 96:
                    JTRuns.jtt_bytecode_BC_ifge_3();
                    break;
                case 97:
                    JTRuns.jtt_bytecode_BC_ifgt();
                    break;
                case 98:
                    JTRuns.jtt_bytecode_BC_ificmplt1();
                    break;
                case 99:
                    JTRuns.jtt_bytecode_BC_ificmplt2();
                    break;
                case 100:
                    JTRuns.jtt_bytecode_BC_ificmpne1();
                    break;
                case 101:
                    JTRuns.jtt_bytecode_BC_ificmpne2();
                    break;
                case 102:
                    JTRuns.jtt_bytecode_BC_ifle();
                    break;
                case 103:
                    JTRuns.jtt_bytecode_BC_iflt();
                    break;
                case 104:
                    JTRuns.jtt_bytecode_BC_ifne();
                    break;
                case 105:
                    JTRuns.jtt_bytecode_BC_ifnonnull();
                    break;
                case 106:
                    JTRuns.jtt_bytecode_BC_ifnonnull_2();
                    break;
                case 107:
                    JTRuns.jtt_bytecode_BC_ifnonnull_3();
                    break;
                case 108:
                    JTRuns.jtt_bytecode_BC_ifnull();
                    break;
                case 109:
                    JTRuns.jtt_bytecode_BC_ifnull_2();
                    break;
                case 110:
                    JTRuns.jtt_bytecode_BC_ifnull_3();
                    break;
                case 111:
                    JTRuns.jtt_bytecode_BC_iinc_1();
                    break;
                case 112:
                    JTRuns.jtt_bytecode_BC_iinc_2();
                    break;
                case 113:
                    JTRuns.jtt_bytecode_BC_iinc_3();
                    break;
                case 114:
                    JTRuns.jtt_bytecode_BC_iinc_4();
                    break;
                case 115:
                    JTRuns.jtt_bytecode_BC_iload_0();
                    break;
                case 116:
                    JTRuns.jtt_bytecode_BC_iload_0_1();
                    break;
                case 117:
                    JTRuns.jtt_bytecode_BC_iload_0_2();
                    break;
                case 118:
                    JTRuns.jtt_bytecode_BC_iload_1();
                    break;
                case 119:
                    JTRuns.jtt_bytecode_BC_iload_1_1();
                    break;
                case 120:
                    JTRuns.jtt_bytecode_BC_iload_2();
                    break;
                case 121:
                    JTRuns.jtt_bytecode_BC_iload_3();
                    break;
                case 122:
                    JTRuns.jtt_bytecode_BC_imul();
                    break;
                case 123:
                    JTRuns.jtt_bytecode_BC_ineg();
                    break;
                case 124:
                    JTRuns.jtt_bytecode_BC_instanceof();
                    break;
                case 125:
                    JTRuns.jtt_bytecode_BC_invokeinterface();
                    break;
                case 126:
                    JTRuns.jtt_bytecode_BC_invokespecial();
                    break;
                case 127:
                    JTRuns.jtt_bytecode_BC_invokespecial2();
                    break;
                case 128:
                    JTRuns.jtt_bytecode_BC_invokestatic();
                    break;
                case 129:
                    JTRuns.jtt_bytecode_BC_invokevirtual();
                    break;
                case 130:
                    JTRuns.jtt_bytecode_BC_ior();
                    break;
                case 131:
                    JTRuns.jtt_bytecode_BC_irem();
                    break;
                case 132:
                    JTRuns.jtt_bytecode_BC_ireturn();
                    break;
                case 133:
                    JTRuns.jtt_bytecode_BC_ishl();
                    break;
                case 134:
                    JTRuns.jtt_bytecode_BC_ishr();
                    break;
                case 135:
                    JTRuns.jtt_bytecode_BC_isub();
                    break;
                case 136:
                    JTRuns.jtt_bytecode_BC_iushr();
                    break;
                case 137:
                    JTRuns.jtt_bytecode_BC_ixor();
                    break;
                case 138:
                    JTRuns.jtt_bytecode_BC_l2d();
                    break;
                case 139:
                    JTRuns.jtt_bytecode_BC_l2f();
                    break;
                case 140:
                    JTRuns.jtt_bytecode_BC_l2i();
                    break;
                case 141:
                    JTRuns.jtt_bytecode_BC_ladd();
                    break;
                case 142:
                    JTRuns.jtt_bytecode_BC_ladd2();
                    break;
                case 143:
                    JTRuns.jtt_bytecode_BC_laload();
                    break;
                case 144:
                    JTRuns.jtt_bytecode_BC_land();
                    break;
                case 145:
                    JTRuns.jtt_bytecode_BC_lastore();
                    break;
                case 146:
                    JTRuns.jtt_bytecode_BC_lcmp();
                    break;
                case 147:
                    JTRuns.jtt_bytecode_BC_ldc_01();
                    break;
                case 148:
                    JTRuns.jtt_bytecode_BC_ldc_02();
                    break;
                case 149:
                    JTRuns.jtt_bytecode_BC_ldc_03();
                    break;
                case 150:
                    JTRuns.jtt_bytecode_BC_ldc_04();
                    break;
                case 151:
                    JTRuns.jtt_bytecode_BC_ldc_05();
                    break;
                case 152:
                    JTRuns.jtt_bytecode_BC_ldc_06();
                    break;
                case 153:
                    JTRuns.jtt_bytecode_BC_ldiv();
                    break;
                case 154:
                    JTRuns.jtt_bytecode_BC_lload_0();
                    break;
                case 155:
                    JTRuns.jtt_bytecode_BC_lload_01();
                    break;
                case 156:
                    JTRuns.jtt_bytecode_BC_lload_1();
                    break;
                case 157:
                    JTRuns.jtt_bytecode_BC_lload_2();
                    break;
                case 158:
                    JTRuns.jtt_bytecode_BC_lload_3();
                    break;
                case 159:
                    JTRuns.jtt_bytecode_BC_lmul();
                    break;
                case 160:
                    JTRuns.jtt_bytecode_BC_lneg();
                    break;
                case 161:
                    JTRuns.jtt_bytecode_BC_lookupswitch01();
                    break;
                case 162:
                    JTRuns.jtt_bytecode_BC_lookupswitch02();
                    break;
                case 163:
                    JTRuns.jtt_bytecode_BC_lookupswitch03();
                    break;
                case 164:
                    JTRuns.jtt_bytecode_BC_lookupswitch04();
                    break;
                case 165:
                    JTRuns.jtt_bytecode_BC_lor();
                    break;
                case 166:
                    JTRuns.jtt_bytecode_BC_lrem();
                    break;
                case 167:
                    JTRuns.jtt_bytecode_BC_lreturn();
                    break;
                case 168:
                    JTRuns.jtt_bytecode_BC_lshl();
                    break;
                case 169:
                    JTRuns.jtt_bytecode_BC_lshr();
                    break;
                case 170:
                    JTRuns.jtt_bytecode_BC_lsub();
                    break;
                case 171:
                    JTRuns.jtt_bytecode_BC_lushr();
                    break;
                case 172:
                    JTRuns.jtt_bytecode_BC_lxor();
                    break;
                case 173:
                    JTRuns.jtt_bytecode_BC_monitorenter();
                    break;
                case 174:
                    JTRuns.jtt_bytecode_BC_multianewarray01();
                    break;
                case 175:
                    JTRuns.jtt_bytecode_BC_multianewarray02();
                    break;
                case 176:
                    JTRuns.jtt_bytecode_BC_multianewarray03();
                    break;
                case 177:
                    JTRuns.jtt_bytecode_BC_multianewarray04();
                    break;
                case 178:
                    JTRuns.jtt_bytecode_BC_new();
                    break;
                case 179:
                    JTRuns.jtt_bytecode_BC_newarray();
                    break;
                case 180:
                    JTRuns.jtt_bytecode_BC_putfield();
                    break;
                case 181:
                    JTRuns.jtt_bytecode_BC_putstatic();
                    break;
                case 182:
                    JTRuns.jtt_bytecode_BC_saload();
                    break;
                case 183:
                    JTRuns.jtt_bytecode_BC_sastore();
                    break;
                case 184:
                    JTRuns.jtt_bytecode_BC_tableswitch();
                    break;
                case 185:
                    JTRuns.jtt_bytecode_BC_tableswitch2();
                    break;
                case 186:
                    JTRuns.jtt_bytecode_BC_tableswitch3();
                    break;
                case 187:
                    JTRuns.jtt_bytecode_BC_tableswitch4();
                    break;
                case 188:
                    JTRuns.jtt_bytecode_BC_wide01();
                    break;
                case 189:
                    JTRuns.jtt_bytecode_BC_wide02();
                    break;
                case 190:
                    JTRuns.jtt_directives_Fold01();
                    break;
                case 191:
                    JTRuns.jtt_directives_Fold02();
                    break;
                case 192:
                    JTRuns.jtt_directives_Fold03();
                    break;
                case 193:
                    JTRuns.jtt_directives_Inline01();
                    break;
                case 194:
                    JTRuns.jtt_directives_Prototyping01();
                    break;
                case 195:
                    JTRuns.jtt_except_BC_aaload();
                    break;
                case 196:
                    JTRuns.jtt_except_BC_aastore();
                    break;
                case 197:
                    JTRuns.jtt_except_BC_anewarray();
                    break;
                case 198:
                    JTRuns.jtt_except_BC_arraylength();
                    break;
                case 199:
                    JTRuns.jtt_except_BC_athrow();
                    break;
                case 200:
                    JTRuns.jtt_except_BC_athrow1();
                    break;
                case 201:
                    JTRuns.jtt_except_BC_athrow2();
                    break;
                case 202:
                    JTRuns.jtt_except_BC_athrow3();
                    break;
                case 203:
                    JTRuns.jtt_except_BC_baload();
                    break;
                case 204:
                    JTRuns.jtt_except_BC_bastore();
                    break;
                case 205:
                    JTRuns.jtt_except_BC_caload();
                    break;
                case 206:
                    JTRuns.jtt_except_BC_castore();
                    break;
                case 207:
                    JTRuns.jtt_except_BC_checkcast();
                    break;
                case 208:
                    JTRuns.jtt_except_BC_checkcast1();
                    break;
                case 209:
                    JTRuns.jtt_except_BC_checkcast2();
                    break;
                case 210:
                    JTRuns.jtt_except_BC_checkcast3();
                    break;
                case 211:
                    JTRuns.jtt_except_BC_daload();
                    break;
                case 212:
                    JTRuns.jtt_except_BC_dastore();
                    break;
                case 213:
                    JTRuns.jtt_except_BC_faload();
                    break;
                case 214:
                    JTRuns.jtt_except_BC_fastore();
                    break;
                case 215:
                    JTRuns.jtt_except_BC_getfield();
                    break;
                case 216:
                    JTRuns.jtt_except_BC_iaload();
                    break;
                case 217:
                    JTRuns.jtt_except_BC_iastore();
                    break;
                case 218:
                    JTRuns.jtt_except_BC_idiv();
                    break;
                case 219:
                    JTRuns.jtt_except_BC_idiv2();
                    break;
                case 220:
                    JTRuns.jtt_except_BC_invokevirtual01();
                    break;
                case 221:
                    JTRuns.jtt_except_BC_irem();
                    break;
                case 222:
                    JTRuns.jtt_except_BC_laload();
                    break;
                case 223:
                    JTRuns.jtt_except_BC_lastore();
                    break;
                case 224:
                    JTRuns.jtt_except_BC_ldiv();
                    break;
                case 225:
                    JTRuns.jtt_except_BC_ldiv2();
                    break;
                case 226:
                    JTRuns.jtt_except_BC_lrem();
                    break;
                case 227:
                    JTRuns.jtt_except_BC_monitorenter();
                    break;
                case 228:
                    JTRuns.jtt_except_BC_multianewarray();
                    break;
                case 229:
                    JTRuns.jtt_except_BC_newarray();
                    break;
                case 230:
                    JTRuns.jtt_except_BC_putfield();
                    break;
                case 231:
                    JTRuns.jtt_except_BC_saload();
                    break;
                case 232:
                    JTRuns.jtt_except_BC_sastore();
                    break;
                case 233:
                    JTRuns.jtt_except_Catch_Loop01();
                    break;
                case 234:
                    JTRuns.jtt_except_Catch_Loop02();
                    break;
                case 235:
                    JTRuns.jtt_except_Catch_NPE_01();
                    break;
                case 236:
                    JTRuns.jtt_except_Catch_NPE_02();
                    break;
                case 237:
                    JTRuns.jtt_except_Catch_NPE_03();
                    break;
                case 238:
                    JTRuns.jtt_except_Catch_NPE_04();
                    break;
                case 239:
                    JTRuns.jtt_except_Catch_NPE_05();
                    break;
                case 240:
                    JTRuns.jtt_except_Catch_NPE_06();
                    break;
                case 241:
                    JTRuns.jtt_except_Catch_NPE_07();
                    break;
                case 242:
                    JTRuns.jtt_except_Catch_NPE_08();
                    break;
                case 243:
                    JTRuns.jtt_except_Catch_StackOverflowError_01();
                    break;
                case 244:
                    JTRuns.jtt_except_Catch_StackOverflowError_02();
                    break;
                case 245:
                    JTRuns.jtt_except_Catch_StackOverflowError_03();
                    break;
                case 246:
                    JTRuns.jtt_except_Catch_Two01();
                    break;
                case 247:
                    JTRuns.jtt_except_Catch_Two02();
                    break;
                case 248:
                    JTRuns.jtt_except_Catch_Two03();
                    break;
                case 249:
                    JTRuns.jtt_except_Except_Synchronized01();
                    break;
                case 250:
                    JTRuns.jtt_except_Except_Synchronized02();
                    break;
                case 251:
                    JTRuns.jtt_except_Except_Synchronized03();
                    break;
                case 252:
                    JTRuns.jtt_except_Except_Synchronized04();
                    break;
                case 253:
                    JTRuns.jtt_except_Throw_InCatch01();
                    break;
                case 254:
                    JTRuns.jtt_except_Throw_InCatch02();
                    break;
                case 255:
                    JTRuns.jtt_except_Throw_InCatch03();
                    break;
                case 256:
                    JTRuns.jtt_except_Throw_NPE_01();
                    break;
                case 257:
                    JTRuns.jtt_except_Throw_Synchronized01();
                    break;
                case 258:
                    JTRuns.jtt_except_Throw_Synchronized02();
                    break;
                case 259:
                    JTRuns.jtt_except_Throw_Synchronized03();
                    break;
                case 260:
                    JTRuns.jtt_except_Throw_Synchronized04();
                    break;
                case 261:
                    JTRuns.jtt_except_Throw_Synchronized05();
                    break;
                case 262:
                    JTRuns.jtt_hotpath_HP_allocate01();
                    break;
                case 263:
                    JTRuns.jtt_hotpath_HP_allocate02();
                    break;
                case 264:
                    JTRuns.jtt_hotpath_HP_allocate03();
                    break;
                case 265:
                    JTRuns.jtt_hotpath_HP_array01();
                    break;
                case 266:
                    JTRuns.jtt_hotpath_HP_array02();
                    break;
                case 267:
                    JTRuns.jtt_hotpath_HP_array03();
                    break;
                case 268:
                    JTRuns.jtt_hotpath_HP_array04();
                    break;
                case 269:
                    JTRuns.jtt_hotpath_HP_control01();
                    break;
                case 270:
                    JTRuns.jtt_hotpath_HP_control02();
                    break;
                case 271:
                    JTRuns.jtt_hotpath_HP_convert01();
                    break;
                case 272:
                    JTRuns.jtt_hotpath_HP_count();
                    break;
                case 273:
                    JTRuns.jtt_hotpath_HP_dead01();
                    break;
                case 274:
                    JTRuns.jtt_hotpath_HP_demo01();
                    break;
                case 275:
                    JTRuns.jtt_hotpath_HP_field01();
                    break;
                case 276:
                    JTRuns.jtt_hotpath_HP_field02();
                    break;
                case 277:
                    JTRuns.jtt_hotpath_HP_field03();
                    break;
                case 278:
                    JTRuns.jtt_hotpath_HP_field04();
                    break;
                case 279:
                    JTRuns.jtt_hotpath_HP_idea();
                    break;
                case 280:
                    JTRuns.jtt_hotpath_HP_inline01();
                    break;
                case 281:
                    JTRuns.jtt_hotpath_HP_inline02();
                    break;
                case 282:
                    JTRuns.jtt_hotpath_HP_invoke01();
                    break;
                case 283:
                    JTRuns.jtt_hotpath_HP_life();
                    break;
                case 284:
                    JTRuns.jtt_hotpath_HP_nest01();
                    break;
                case 285:
                    JTRuns.jtt_hotpath_HP_nest02();
                    break;
                case 286:
                    JTRuns.jtt_hotpath_HP_scope01();
                    break;
                case 287:
                    JTRuns.jtt_hotpath_HP_scope02();
                    break;
                case 288:
                    JTRuns.jtt_hotpath_HP_series();
                    break;
                case 289:
                    JTRuns.jtt_hotpath_HP_trees01();
                    break;
                case 290:
                    JTRuns.jtt_jasm_Loop00();
                    break;
                case 291:
                    JTRuns.jtt_jdk_Class_getName();
                    break;
                case 292:
                    JTRuns.jtt_jdk_EnumMap01();
                    break;
                case 293:
                    JTRuns.jtt_jdk_EnumMap02();
                    break;
                case 294:
                    JTRuns.jtt_jdk_System_currentTimeMillis01();
                    break;
                case 295:
                    JTRuns.jtt_jdk_System_currentTimeMillis02();
                    break;
                case 296:
                    JTRuns.jtt_jdk_System_nanoTime01();
                    break;
                case 297:
                    JTRuns.jtt_jdk_System_nanoTime02();
                    break;
                case 298:
                    JTRuns.jtt_jdk_UnsafeAccess01();
                    break;
                case 299:
                    JTRuns.jtt_jni_JNI_OverflowArguments();
                    break;
                case 300:
                    JTRuns.jtt_jvmni_JVM_ArrayCopy01();
                    break;
                case 301:
                    JTRuns.jtt_jvmni_JVM_GetClassContext01();
                    break;
                case 302:
                    JTRuns.jtt_jvmni_JVM_GetClassContext02();
                    break;
                case 303:
                    JTRuns.jtt_jvmni_JVM_GetFreeMemory01();
                    break;
                case 304:
                    JTRuns.jtt_jvmni_JVM_GetMaxMemory01();
                    break;
                case 305:
                    JTRuns.jtt_jvmni_JVM_GetTotalMemory01();
                    break;
                case 306:
                    JTRuns.jtt_jvmni_JVM_IsNaN01();
                    break;
                case 307:
                    JTRuns.jtt_lang_Boxed_TYPE_01();
                    break;
                case 308:
                    JTRuns.jtt_lang_Bridge_method01();
                    break;
                case 309:
                    JTRuns.jtt_lang_ClassLoader_loadClass01();
                    break;
                case 310:
                    JTRuns.jtt_lang_Class_Literal01();
                    break;
                case 311:
                    JTRuns.jtt_lang_Class_asSubclass01();
                    break;
                case 312:
                    JTRuns.jtt_lang_Class_cast01();
                    break;
                case 313:
                    JTRuns.jtt_lang_Class_cast02();
                    break;
                case 314:
                    JTRuns.jtt_lang_Class_forName01();
                    break;
                case 315:
                    JTRuns.jtt_lang_Class_forName02();
                    break;
                case 316:
                    JTRuns.jtt_lang_Class_forName03();
                    break;
                case 317:
                    JTRuns.jtt_lang_Class_forName04();
                    break;
                case 318:
                    JTRuns.jtt_lang_Class_forName05();
                    break;
                case 319:
                    JTRuns.jtt_lang_Class_getComponentType01();
                    break;
                case 320:
                    JTRuns.jtt_lang_Class_getName01();
                    break;
                case 321:
                    JTRuns.jtt_lang_Class_getName02();
                    break;
                case 322:
                    JTRuns.jtt_lang_Class_getSimpleName01();
                    break;
                case 323:
                    JTRuns.jtt_lang_Class_getSimpleName02();
                    break;
                case 324:
                    JTRuns.jtt_lang_Class_getSuperClass01();
                    break;
                case 325:
                    JTRuns.jtt_lang_Class_isArray01();
                    break;
                case 326:
                    JTRuns.jtt_lang_Class_isAssignableFrom01();
                    break;
                case 327:
                    JTRuns.jtt_lang_Class_isAssignableFrom02();
                    break;
                case 328:
                    JTRuns.jtt_lang_Class_isAssignableFrom03();
                    break;
                case 329:
                    JTRuns.jtt_lang_Class_isInstance01();
                    break;
                case 330:
                    JTRuns.jtt_lang_Class_isInstance02();
                    break;
                case 331:
                    JTRuns.jtt_lang_Class_isInstance03();
                    break;
                case 332:
                    JTRuns.jtt_lang_Class_isInstance04();
                    break;
                case 333:
                    JTRuns.jtt_lang_Class_isInstance05();
                    break;
                case 334:
                    JTRuns.jtt_lang_Class_isInstance06();
                    break;
                case 335:
                    JTRuns.jtt_lang_Class_isInterface01();
                    break;
                case 336:
                    JTRuns.jtt_lang_Class_isPrimitive01();
                    break;
                case 337:
                    JTRuns.jtt_lang_Double_toString();
                    break;
                case 338:
                    JTRuns.jtt_lang_Int_greater01();
                    break;
                case 339:
                    JTRuns.jtt_lang_Int_greater02();
                    break;
                case 340:
                    JTRuns.jtt_lang_Int_greater03();
                    break;
                case 341:
                    JTRuns.jtt_lang_Int_greaterEqual01();
                    break;
                case 342:
                    JTRuns.jtt_lang_Int_greaterEqual02();
                    break;
                case 343:
                    JTRuns.jtt_lang_Int_greaterEqual03();
                    break;
                case 344:
                    JTRuns.jtt_lang_Int_less01();
                    break;
                case 345:
                    JTRuns.jtt_lang_Int_less02();
                    break;
                case 346:
                    JTRuns.jtt_lang_Int_less03();
                    break;
                case 347:
                    JTRuns.jtt_lang_Int_lessEqual01();
                    break;
                case 348:
                    JTRuns.jtt_lang_Int_lessEqual02();
                    break;
                case 349:
                    JTRuns.jtt_lang_Int_lessEqual03();
                    break;
                case 350:
                    JTRuns.jtt_lang_JDK_ClassLoaders01();
                    break;
                case 351:
                    JTRuns.jtt_lang_JDK_ClassLoaders02();
                    break;
                case 352:
                    JTRuns.jtt_lang_Long_greater01();
                    break;
                case 353:
                    JTRuns.jtt_lang_Long_greater02();
                    break;
                case 354:
                    JTRuns.jtt_lang_Long_greater03();
                    break;
                case 355:
                    JTRuns.jtt_lang_Long_greaterEqual01();
                    break;
                case 356:
                    JTRuns.jtt_lang_Long_greaterEqual02();
                    break;
                case 357:
                    JTRuns.jtt_lang_Long_greaterEqual03();
                    break;
                case 358:
                    JTRuns.jtt_lang_Long_less01();
                    break;
                case 359:
                    JTRuns.jtt_lang_Long_less02();
                    break;
                case 360:
                    JTRuns.jtt_lang_Long_less03();
                    break;
                case 361:
                    JTRuns.jtt_lang_Long_lessEqual01();
                    break;
                case 362:
                    JTRuns.jtt_lang_Long_lessEqual02();
                    break;
                case 363:
                    JTRuns.jtt_lang_Long_lessEqual03();
                    break;
                case 364:
                    JTRuns.jtt_lang_Long_reverseBytes01();
                    break;
                case 365:
                    JTRuns.jtt_lang_Long_reverseBytes02();
                    break;
                case 366:
                    JTRuns.jtt_lang_Math_pow();
                    break;
                case 367:
                    JTRuns.jtt_lang_Object_clone01();
                    break;
                case 368:
                    JTRuns.jtt_lang_Object_clone02();
                    break;
                case 369:
                    JTRuns.jtt_lang_Object_equals01();
                    break;
                case 370:
                    JTRuns.jtt_lang_Object_getClass01();
                    break;
                case 371:
                    JTRuns.jtt_lang_Object_hashCode01();
                    break;
                case 372:
                    JTRuns.jtt_lang_Object_notify01();
                    break;
                case 373:
                    JTRuns.jtt_lang_Object_notify02();
                    break;
                case 374:
                    JTRuns.jtt_lang_Object_notifyAll01();
                    break;
                case 375:
                    JTRuns.jtt_lang_Object_notifyAll02();
                    break;
                case 376:
                    JTRuns.jtt_lang_Object_toString01();
                    break;
                case 377:
                    JTRuns.jtt_lang_Object_toString02();
                    break;
                case 378:
                    JTRuns.jtt_lang_Object_wait01();
                    break;
                case 379:
                    JTRuns.jtt_lang_Object_wait02();
                    break;
                case 380:
                    JTRuns.jtt_lang_Object_wait03();
                    break;
                case 381:
                    JTRuns.jtt_lang_ProcessEnvironment_init();
                    break;
                case 382:
                    JTRuns.jtt_lang_StringCoding_Scale();
                    break;
                case 383:
                    JTRuns.jtt_lang_String_intern01();
                    break;
                case 384:
                    JTRuns.jtt_lang_String_intern02();
                    break;
                case 385:
                    JTRuns.jtt_lang_String_intern03();
                    break;
                case 386:
                    JTRuns.jtt_lang_String_valueOf01();
                    break;
                case 387:
                    JTRuns.jtt_lang_System_identityHashCode01();
                    break;
                case 388:
                    JTRuns.jtt_max_ImmortalHeap_allocation();
                    break;
                case 389:
                    JTRuns.jtt_max_ImmortalHeap_gc();
                    break;
                case 390:
                    JTRuns.jtt_max_ImmortalHeap_switching();
                    break;
                case 391:
                    JTRuns.jtt_max_Unsigned_idiv01();
                    break;
                case 392:
                    JTRuns.jtt_micro_ArrayCompare01();
                    break;
                case 393:
                    JTRuns.jtt_micro_ArrayCompare02();
                    break;
                case 394:
                    JTRuns.jtt_micro_BC_invokevirtual2();
                    break;
                case 395:
                    JTRuns.jtt_micro_BigByteParams01();
                    break;
                case 396:
                    JTRuns.jtt_micro_BigDoubleParams02();
                    break;
                case 397:
                    JTRuns.jtt_micro_BigFloatParams01();
                    break;
                case 398:
                    JTRuns.jtt_micro_BigFloatParams02();
                    break;
                case 399:
                    JTRuns.jtt_micro_BigIntParams01();
                    break;
                case 400:
                    JTRuns.jtt_micro_BigIntParams02();
                    break;
                case 401:
                    JTRuns.jtt_micro_BigLongParams02();
                    break;
                case 402:
                    JTRuns.jtt_micro_BigMixedParams01();
                    break;
                case 403:
                    JTRuns.jtt_micro_BigMixedParams02();
                    break;
                case 404:
                    JTRuns.jtt_micro_BigMixedParams03();
                    break;
                case 405:
                    JTRuns.jtt_micro_BigObjectParams01();
                    break;
                case 406:
                    JTRuns.jtt_micro_BigObjectParams02();
                    break;
                case 407:
                    JTRuns.jtt_micro_BigParamsAlignment();
                    break;
                case 408:
                    JTRuns.jtt_micro_BigShortParams01();
                    break;
                case 409:
                    JTRuns.jtt_micro_Bubblesort();
                    break;
                case 410:
                    JTRuns.jtt_micro_Fibonacci();
                    break;
                case 411:
                    JTRuns.jtt_micro_InvokeVirtual_01();
                    break;
                case 412:
                    JTRuns.jtt_micro_InvokeVirtual_02();
                    break;
                case 413:
                    JTRuns.jtt_micro_Loop01();
                    break;
                case 414:
                    JTRuns.jtt_micro_Loop02();
                    break;
                case 415:
                    JTRuns.jtt_micro_Loop03();
                    break;
                case 416:
                    JTRuns.jtt_micro_Loop04();
                    break;
                case 417:
                    JTRuns.jtt_micro_Loop05();
                    break;
                case 418:
                    JTRuns.jtt_micro_Loop06();
                    break;
                case 419:
                    JTRuns.jtt_micro_LoopSwitch01();
                    break;
                case 420:
                    JTRuns.jtt_micro_Matrix01();
                    break;
                case 421:
                    JTRuns.jtt_micro_StrangeFrames();
                    break;
                case 422:
                    JTRuns.jtt_micro_String_format01();
                    break;
                case 423:
                    JTRuns.jtt_micro_String_format02();
                    break;
                case 424:
                    JTRuns.jtt_micro_VarArgs_String01();
                    break;
                case 425:
                    JTRuns.jtt_micro_VarArgs_boolean01();
                    break;
                case 426:
                    JTRuns.jtt_micro_VarArgs_byte01();
                    break;
                case 427:
                    JTRuns.jtt_micro_VarArgs_char01();
                    break;
                case 428:
                    JTRuns.jtt_micro_VarArgs_double01();
                    break;
                case 429:
                    JTRuns.jtt_micro_VarArgs_float01();
                    break;
                case 430:
                    JTRuns.jtt_micro_VarArgs_int01();
                    break;
                case 431:
                    JTRuns.jtt_micro_VarArgs_long01();
                    break;
                case 432:
                    JTRuns.jtt_micro_VarArgs_short01();
                    break;
                case 433:
                    JTRuns.jtt_optimize_ArrayLength01();
                    break;
                case 434:
                    JTRuns.jtt_optimize_BC_idiv_16();
                    break;
                case 435:
                    JTRuns.jtt_optimize_BC_idiv_4();
                    break;
                case 436:
                    JTRuns.jtt_optimize_BC_imul_16();
                    break;
                case 437:
                    JTRuns.jtt_optimize_BC_imul_4();
                    break;
                case 438:
                    JTRuns.jtt_optimize_BC_ldiv_16();
                    break;
                case 439:
                    JTRuns.jtt_optimize_BC_ldiv_4();
                    break;
                case 440:
                    JTRuns.jtt_optimize_BC_lmul_16();
                    break;
                case 441:
                    JTRuns.jtt_optimize_BC_lmul_4();
                    break;
                case 442:
                    JTRuns.jtt_optimize_BC_lshr_C16();
                    break;
                case 443:
                    JTRuns.jtt_optimize_BC_lshr_C24();
                    break;
                case 444:
                    JTRuns.jtt_optimize_BC_lshr_C32();
                    break;
                case 445:
                    JTRuns.jtt_optimize_BlockSkip01();
                    break;
                case 446:
                    JTRuns.jtt_optimize_DeadCode01();
                    break;
                case 447:
                    JTRuns.jtt_optimize_Fold_Cast01();
                    break;
                case 448:
                    JTRuns.jtt_optimize_Fold_Convert01();
                    break;
                case 449:
                    JTRuns.jtt_optimize_Fold_Convert02();
                    break;
                case 450:
                    JTRuns.jtt_optimize_Fold_Convert03();
                    break;
                case 451:
                    JTRuns.jtt_optimize_Fold_Convert04();
                    break;
                case 452:
                    JTRuns.jtt_optimize_Fold_Double01();
                    break;
                case 453:
                    JTRuns.jtt_optimize_Fold_Double02();
                    break;
                case 454:
                    JTRuns.jtt_optimize_Fold_Float01();
                    break;
                case 455:
                    JTRuns.jtt_optimize_Fold_Float02();
                    break;
                case 456:
                    JTRuns.jtt_optimize_Fold_InstanceOf01();
                    break;
                case 457:
                    JTRuns.jtt_optimize_Fold_Int01();
                    break;
                case 458:
                    JTRuns.jtt_optimize_Fold_Int02();
                    break;
                case 459:
                    JTRuns.jtt_optimize_Fold_Long01();
                    break;
                case 460:
                    JTRuns.jtt_optimize_Fold_Long02();
                    break;
                case 461:
                    JTRuns.jtt_optimize_Fold_Math01();
                    break;
                case 462:
                    JTRuns.jtt_optimize_Inline01();
                    break;
                case 463:
                    JTRuns.jtt_optimize_Inline02();
                    break;
                case 464:
                    JTRuns.jtt_optimize_List_reorder_bug();
                    break;
                case 465:
                    JTRuns.jtt_optimize_NCE_01();
                    break;
                case 466:
                    JTRuns.jtt_optimize_NCE_02();
                    break;
                case 467:
                    JTRuns.jtt_optimize_NCE_03();
                    break;
                case 468:
                    JTRuns.jtt_optimize_NCE_04();
                    break;
                case 469:
                    JTRuns.jtt_optimize_NCE_FlowSensitive01();
                    break;
                case 470:
                    JTRuns.jtt_optimize_NCE_FlowSensitive03();
                    break;
                case 471:
                    JTRuns.jtt_optimize_NCE_FlowSensitive04();
                    break;
                case 472:
                    JTRuns.jtt_optimize_Narrow_byte01();
                    break;
                case 473:
                    JTRuns.jtt_optimize_Narrow_byte02();
                    break;
                case 474:
                    JTRuns.jtt_optimize_Narrow_byte03();
                    break;
                case 475:
                    JTRuns.jtt_optimize_Narrow_char01();
                    break;
                case 476:
                    JTRuns.jtt_optimize_Narrow_char02();
                    break;
                case 477:
                    JTRuns.jtt_optimize_Narrow_char03();
                    break;
                case 478:
                    JTRuns.jtt_optimize_Narrow_short01();
                    break;
                case 479:
                    JTRuns.jtt_optimize_Narrow_short02();
                    break;
                case 480:
                    JTRuns.jtt_optimize_Narrow_short03();
                    break;
                case 481:
                    JTRuns.jtt_optimize_Phi01();
                    break;
                case 482:
                    JTRuns.jtt_optimize_Phi02();
                    break;
                case 483:
                    JTRuns.jtt_optimize_Phi03();
                    break;
                case 484:
                    JTRuns.jtt_optimize_Reduce_Convert01();
                    break;
                case 485:
                    JTRuns.jtt_optimize_Reduce_Double01();
                    break;
                case 486:
                    JTRuns.jtt_optimize_Reduce_Float01();
                    break;
                case 487:
                    JTRuns.jtt_optimize_Reduce_Int01();
                    break;
                case 488:
                    JTRuns.jtt_optimize_Reduce_Int02();
                    break;
                case 489:
                    JTRuns.jtt_optimize_Reduce_Int03();
                    break;
                case 490:
                    JTRuns.jtt_optimize_Reduce_Int04();
                    break;
                case 491:
                    JTRuns.jtt_optimize_Reduce_IntShift01();
                    break;
                case 492:
                    JTRuns.jtt_optimize_Reduce_IntShift02();
                    break;
                case 493:
                    JTRuns.jtt_optimize_Reduce_Long01();
                    break;
                case 494:
                    JTRuns.jtt_optimize_Reduce_Long02();
                    break;
                case 495:
                    JTRuns.jtt_optimize_Reduce_Long03();
                    break;
                case 496:
                    JTRuns.jtt_optimize_Reduce_Long04();
                    break;
                case 497:
                    JTRuns.jtt_optimize_Reduce_LongShift01();
                    break;
                case 498:
                    JTRuns.jtt_optimize_Reduce_LongShift02();
                    break;
                case 499:
                    JTRuns.jtt_optimize_Switch01();
                    break;
                case 500:
                    JTRuns.jtt_optimize_Switch02();
                    break;
                case 501:
                    JTRuns.jtt_optimize_TypeCastElem();
                    break;
                case 502:
                    JTRuns.jtt_optimize_VN_Cast01();
                    break;
                case 503:
                    JTRuns.jtt_optimize_VN_Cast02();
                    break;
                case 504:
                    JTRuns.jtt_optimize_VN_Convert01();
                    break;
                case 505:
                    JTRuns.jtt_optimize_VN_Convert02();
                    break;
                case 506:
                    JTRuns.jtt_optimize_VN_Double01();
                    break;
                case 507:
                    JTRuns.jtt_optimize_VN_Double02();
                    break;
                case 508:
                    JTRuns.jtt_optimize_VN_Field01();
                    break;
                case 509:
                    JTRuns.jtt_optimize_VN_Field02();
                    break;
                case 510:
                    JTRuns.jtt_optimize_VN_Float01();
                    break;
                case 511:
                    JTRuns.jtt_optimize_VN_Float02();
                    break;
                case 512:
                    JTRuns.jtt_optimize_VN_InstanceOf01();
                    break;
                case 513:
                    JTRuns.jtt_optimize_VN_InstanceOf02();
                    break;
                case 514:
                    JTRuns.jtt_optimize_VN_InstanceOf03();
                    break;
                case 515:
                    JTRuns.jtt_optimize_VN_Int01();
                    break;
                case 516:
                    JTRuns.jtt_optimize_VN_Int02();
                    break;
                case 517:
                    JTRuns.jtt_optimize_VN_Int03();
                    break;
                case 518:
                    JTRuns.jtt_optimize_VN_Long01();
                    break;
                case 519:
                    JTRuns.jtt_optimize_VN_Long02();
                    break;
                case 520:
                    JTRuns.jtt_optimize_VN_Long03();
                    break;
                case 521:
                    JTRuns.jtt_optimize_VN_Loop01();
                    break;
                case 522:
                    JTRuns.jtt_reflect_Array_get01();
                    break;
                case 523:
                    JTRuns.jtt_reflect_Array_get02();
                    break;
                case 524:
                    JTRuns.jtt_reflect_Array_get03();
                    break;
                case 525:
                    JTRuns.jtt_reflect_Array_getBoolean01();
                    break;
                case 526:
                    JTRuns.jtt_reflect_Array_getByte01();
                    break;
                case 527:
                    JTRuns.jtt_reflect_Array_getChar01();
                    break;
                case 528:
                    JTRuns.jtt_reflect_Array_getDouble01();
                    break;
                case 529:
                    JTRuns.jtt_reflect_Array_getFloat01();
                    break;
                case 530:
                    JTRuns.jtt_reflect_Array_getInt01();
                    break;
                case 531:
                    JTRuns.jtt_reflect_Array_getLength01();
                    break;
                case 532:
                    JTRuns.jtt_reflect_Array_getLong01();
                    break;
                case 533:
                    JTRuns.jtt_reflect_Array_getShort01();
                    break;
                case 534:
                    JTRuns.jtt_reflect_Array_newInstance01();
                    break;
                case 535:
                    JTRuns.jtt_reflect_Array_newInstance02();
                    break;
                case 536:
                    JTRuns.jtt_reflect_Array_newInstance03();
                    break;
                case 537:
                    JTRuns.jtt_reflect_Array_newInstance04();
                    break;
                case 538:
                    JTRuns.jtt_reflect_Array_newInstance05();
                    break;
                case 539:
                    JTRuns.jtt_reflect_Array_newInstance06();
                    break;
                case 540:
                    JTRuns.jtt_reflect_Array_set01();
                    break;
                case 541:
                    JTRuns.jtt_reflect_Array_set02();
                    break;
                case 542:
                    JTRuns.jtt_reflect_Array_set03();
                    break;
                case 543:
                    JTRuns.jtt_reflect_Array_setBoolean01();
                    break;
                case 544:
                    JTRuns.jtt_reflect_Array_setByte01();
                    break;
                case 545:
                    JTRuns.jtt_reflect_Array_setChar01();
                    break;
                case 546:
                    JTRuns.jtt_reflect_Array_setDouble01();
                    break;
                case 547:
                    JTRuns.jtt_reflect_Array_setFloat01();
                    break;
                case 548:
                    JTRuns.jtt_reflect_Array_setInt01();
                    break;
                case 549:
                    JTRuns.jtt_reflect_Array_setLong01();
                    break;
                case 550:
                    JTRuns.jtt_reflect_Array_setShort01();
                    break;
                case 551:
                    JTRuns.jtt_reflect_Class_getDeclaredField01();
                    break;
                case 552:
                    JTRuns.jtt_reflect_Class_getDeclaredMethod01();
                    break;
                case 553:
                    JTRuns.jtt_reflect_Class_getField01();
                    break;
                case 554:
                    JTRuns.jtt_reflect_Class_getField02();
                    break;
                case 555:
                    JTRuns.jtt_reflect_Class_getMethod01();
                    break;
                case 556:
                    JTRuns.jtt_reflect_Class_getMethod02();
                    break;
                case 557:
                    JTRuns.jtt_reflect_Class_newInstance01();
                    break;
                case 558:
                    JTRuns.jtt_reflect_Class_newInstance02();
                    break;
                case 559:
                    JTRuns.jtt_reflect_Class_newInstance03();
                    break;
                case 560:
                    JTRuns.jtt_reflect_Class_newInstance06();
                    break;
                case 561:
                    JTRuns.jtt_reflect_Class_newInstance07();
                    break;
                case 562:
                    JTRuns.jtt_reflect_Field_get01();
                    break;
                case 563:
                    JTRuns.jtt_reflect_Field_get02();
                    break;
                case 564:
                    JTRuns.jtt_reflect_Field_get03();
                    break;
                case 565:
                    JTRuns.jtt_reflect_Field_get04();
                    break;
                case 566:
                    JTRuns.jtt_reflect_Field_getType01();
                    break;
                case 567:
                    JTRuns.jtt_reflect_Field_set01();
                    break;
                case 568:
                    JTRuns.jtt_reflect_Field_set02();
                    break;
                case 569:
                    JTRuns.jtt_reflect_Field_set03();
                    break;
                case 570:
                    JTRuns.jtt_reflect_Invoke_main01();
                    break;
                case 571:
                    JTRuns.jtt_reflect_Invoke_main02();
                    break;
                case 572:
                    JTRuns.jtt_reflect_Invoke_main03();
                    break;
                case 573:
                    JTRuns.jtt_reflect_Invoke_virtual01();
                    break;
                case 574:
                    JTRuns.jtt_reflect_Method_getParameterTypes01();
                    break;
                case 575:
                    JTRuns.jtt_reflect_Method_getReturnType01();
                    break;
                case 576:
                    JTRuns.jtt_reflect_Reflection_getCallerClass01();
                    break;
                case 577:
                    JTRuns.jtt_threads_Monitor_contended01();
                    break;
                case 578:
                    JTRuns.jtt_threads_Monitor_notowner01();
                    break;
                case 579:
                    JTRuns.jtt_threads_Monitorenter01();
                    break;
                case 580:
                    JTRuns.jtt_threads_Monitorenter02();
                    break;
                case 581:
                    JTRuns.jtt_threads_Object_wait01();
                    break;
                case 582:
                    JTRuns.jtt_threads_Object_wait02();
                    break;
                case 583:
                    JTRuns.jtt_threads_Object_wait03();
                    break;
                case 584:
                    JTRuns.jtt_threads_Object_wait04();
                    break;
                case 585:
                    JTRuns.jtt_threads_Thread_currentThread01();
                    break;
                case 586:
                    JTRuns.jtt_threads_Thread_getState01();
                    break;
                case 587:
                    JTRuns.jtt_threads_Thread_getState02();
                    break;
                case 588:
                    JTRuns.jtt_threads_Thread_holdsLock01();
                    break;
                case 589:
                    JTRuns.jtt_threads_Thread_isAlive01();
                    break;
                case 590:
                    JTRuns.jtt_threads_Thread_isInterrupted01();
                    break;
                case 591:
                    JTRuns.jtt_threads_Thread_isInterrupted02();
                    break;
                case 592:
                    JTRuns.jtt_threads_Thread_isInterrupted03();
                    break;
                case 593:
                    JTRuns.jtt_threads_Thread_isInterrupted04();
                    break;
                case 594:
                    JTRuns.jtt_threads_Thread_join01();
                    break;
                case 595:
                    JTRuns.jtt_threads_Thread_join02();
                    break;
                case 596:
                    JTRuns.jtt_threads_Thread_join03();
                    break;
                case 597:
                    JTRuns.jtt_threads_Thread_new01();
                    break;
                case 598:
                    JTRuns.jtt_threads_Thread_new02();
                    break;
                case 599:
                    JTRuns.jtt_threads_Thread_setPriority01();
                    break;
                case 600:
                    JTRuns.jtt_threads_Thread_sleep01();
                    break;
                case 601:
                    JTRuns.jtt_threads_Thread_start01();
                    break;
                case 602:
                    JTRuns.jtt_threads_Thread_yield01();
            }
        }
        JTUtil.reportPassed(JTUtil.passed, JTUtil.total);
    }
// END GENERATED TEST RUNS
}
