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
package test.com.sun.max.vm.testrun.all;

import com.sun.max.annotate.*;
import com.sun.max.vm.*;
import test.com.sun.max.vm.testrun.*;


public class JavaTesterRunScheme extends AbstractTester {

    public JavaTesterRunScheme(VMConfiguration vmConfiguration) {
        super(vmConfiguration);
    }

    @Override
    @PROTOTYPE_ONLY
    public Class<?>[] getClassList() {
        return _classList;
    }

    @PROTOTYPE_ONLY
// GENERATED TEST RUNS
    private static final Class<?>[] _classList = {
        test.bytecode.BC_aaload.class,
        test.bytecode.BC_aastore.class,
        test.bytecode.BC_aload_0.class,
        test.bytecode.BC_aload_1.class,
        test.bytecode.BC_aload_2.class,
        test.bytecode.BC_aload_3.class,
        test.bytecode.BC_anewarray.class,
        test.bytecode.BC_areturn.class,
        test.bytecode.BC_arraylength.class,
        test.bytecode.BC_athrow.class,
        test.bytecode.BC_baload.class,
        test.bytecode.BC_bastore.class,
        test.bytecode.BC_caload.class,
        test.bytecode.BC_castore.class,
        test.bytecode.BC_checkcast.class,
        test.bytecode.BC_d2f.class,
        test.bytecode.BC_d2i.class,
        test.bytecode.BC_d2i_nan.class,
        test.bytecode.BC_d2l.class,
        test.bytecode.BC_d2l_nan.class,
        test.bytecode.BC_dadd.class,
        test.bytecode.BC_daload.class,
        test.bytecode.BC_dastore.class,
        test.bytecode.BC_dcmp.class,
        test.bytecode.BC_ddiv.class,
        test.bytecode.BC_dmul.class,
        test.bytecode.BC_dneg.class,
        test.bytecode.BC_drem.class,
        test.bytecode.BC_dreturn.class,
        test.bytecode.BC_dsub.class,
        test.bytecode.BC_f2d.class,
        test.bytecode.BC_f2i.class,
        test.bytecode.BC_f2i_2.class,
        test.bytecode.BC_f2i_nan.class,
        test.bytecode.BC_f2l.class,
        test.bytecode.BC_f2l_nan.class,
        test.bytecode.BC_fadd.class,
        test.bytecode.BC_faload.class,
        test.bytecode.BC_fastore.class,
        test.bytecode.BC_fcmp.class,
        test.bytecode.BC_fdiv.class,
        test.bytecode.BC_fload.class,
        test.bytecode.BC_fload_2.class,
        test.bytecode.BC_fmul.class,
        test.bytecode.BC_fneg.class,
        test.bytecode.BC_frem.class,
        test.bytecode.BC_freturn.class,
        test.bytecode.BC_fsub.class,
        test.bytecode.BC_getfield.class,
        test.bytecode.BC_getstatic_b.class,
        test.bytecode.BC_getstatic_c.class,
        test.bytecode.BC_getstatic_d.class,
        test.bytecode.BC_getstatic_f.class,
        test.bytecode.BC_getstatic_i.class,
        test.bytecode.BC_getstatic_l.class,
        test.bytecode.BC_getstatic_s.class,
        test.bytecode.BC_getstatic_z.class,
        test.bytecode.BC_i2b.class,
        test.bytecode.BC_i2c.class,
        test.bytecode.BC_i2d.class,
        test.bytecode.BC_i2f.class,
        test.bytecode.BC_i2l.class,
        test.bytecode.BC_i2s.class,
        test.bytecode.BC_iadd.class,
        test.bytecode.BC_iadd2.class,
        test.bytecode.BC_iadd3.class,
        test.bytecode.BC_iaload.class,
        test.bytecode.BC_iand.class,
        test.bytecode.BC_iastore.class,
        test.bytecode.BC_iconst.class,
        test.bytecode.BC_idiv.class,
        test.bytecode.BC_ifeq.class,
        test.bytecode.BC_ifeq_2.class,
        test.bytecode.BC_ifeq_3.class,
        test.bytecode.BC_ifge.class,
        test.bytecode.BC_ifge_2.class,
        test.bytecode.BC_ifge_3.class,
        test.bytecode.BC_ifgt.class,
        test.bytecode.BC_ificmplt1.class,
        test.bytecode.BC_ificmplt2.class,
        test.bytecode.BC_ificmpne1.class,
        test.bytecode.BC_ificmpne2.class,
        test.bytecode.BC_ifle.class,
        test.bytecode.BC_iflt.class,
        test.bytecode.BC_ifne.class,
        test.bytecode.BC_ifnonnull.class,
        test.bytecode.BC_ifnonnull_2.class,
        test.bytecode.BC_ifnonnull_3.class,
        test.bytecode.BC_ifnull.class,
        test.bytecode.BC_ifnull_2.class,
        test.bytecode.BC_ifnull_3.class,
        test.bytecode.BC_iinc_1.class,
        test.bytecode.BC_iinc_2.class,
        test.bytecode.BC_iinc_3.class,
        test.bytecode.BC_iload_0.class,
        test.bytecode.BC_iload_0_1.class,
        test.bytecode.BC_iload_0_2.class,
        test.bytecode.BC_iload_1.class,
        test.bytecode.BC_iload_1_1.class,
        test.bytecode.BC_iload_2.class,
        test.bytecode.BC_iload_3.class,
        test.bytecode.BC_imul.class,
        test.bytecode.BC_ineg.class,
        test.bytecode.BC_instanceof.class,
        test.bytecode.BC_invokeinterface.class,
        test.bytecode.BC_invokespecial.class,
        test.bytecode.BC_invokespecial2.class,
        test.bytecode.BC_invokestatic.class,
        test.bytecode.BC_invokevirtual.class,
        test.bytecode.BC_ior.class,
        test.bytecode.BC_irem.class,
        test.bytecode.BC_ireturn.class,
        test.bytecode.BC_ishl.class,
        test.bytecode.BC_ishr.class,
        test.bytecode.BC_isub.class,
        test.bytecode.BC_iushr.class,
        test.bytecode.BC_ixor.class,
        test.bytecode.BC_l2d.class,
        test.bytecode.BC_l2f.class,
        test.bytecode.BC_l2i.class,
        test.bytecode.BC_ladd.class,
        test.bytecode.BC_ladd2.class,
        test.bytecode.BC_laload.class,
        test.bytecode.BC_land.class,
        test.bytecode.BC_lastore.class,
        test.bytecode.BC_lcmp.class,
        test.bytecode.BC_ldc_01.class,
        test.bytecode.BC_ldc_02.class,
        test.bytecode.BC_ldc_03.class,
        test.bytecode.BC_ldc_04.class,
        test.bytecode.BC_ldc_05.class,
        test.bytecode.BC_ldiv.class,
        test.bytecode.BC_lload_0.class,
        test.bytecode.BC_lload_01.class,
        test.bytecode.BC_lload_1.class,
        test.bytecode.BC_lload_2.class,
        test.bytecode.BC_lload_3.class,
        test.bytecode.BC_lmul.class,
        test.bytecode.BC_lneg.class,
        test.bytecode.BC_lookupswitch01.class,
        test.bytecode.BC_lookupswitch02.class,
        test.bytecode.BC_lookupswitch03.class,
        test.bytecode.BC_lookupswitch04.class,
        test.bytecode.BC_lor.class,
        test.bytecode.BC_lrem.class,
        test.bytecode.BC_lreturn.class,
        test.bytecode.BC_lshl.class,
        test.bytecode.BC_lshr.class,
        test.bytecode.BC_lsub.class,
        test.bytecode.BC_lushr.class,
        test.bytecode.BC_lxor.class,
        test.bytecode.BC_monitorenter.class,
        test.bytecode.BC_multianewarray.class,
        test.bytecode.BC_new.class,
        test.bytecode.BC_newarray.class,
        test.bytecode.BC_putfield.class,
        test.bytecode.BC_putstatic.class,
        test.bytecode.BC_saload.class,
        test.bytecode.BC_sastore.class,
        test.bytecode.BC_tableswitch.class,
        test.bytecode.BC_tableswitch2.class,
        test.bytecode.BC_tableswitch3.class,
        test.bytecode.BC_tableswitch4.class,
        test.bytecode.BC_wide01.class,
        test.bytecode.BC_wide02.class,
        test.except.BC_aaload.class,
        test.except.BC_aastore.class,
        test.except.BC_anewarray.class,
        test.except.BC_arraylength.class,
        test.except.BC_athrow.class,
        test.except.BC_athrow1.class,
        test.except.BC_athrow2.class,
        test.except.BC_athrow3.class,
        test.except.BC_baload.class,
        test.except.BC_bastore.class,
        test.except.BC_caload.class,
        test.except.BC_castore.class,
        test.except.BC_checkcast.class,
        test.except.BC_checkcast1.class,
        test.except.BC_checkcast2.class,
        test.except.BC_daload.class,
        test.except.BC_dastore.class,
        test.except.BC_faload.class,
        test.except.BC_fastore.class,
        test.except.BC_getfield.class,
        test.except.BC_iaload.class,
        test.except.BC_iastore.class,
        test.except.BC_idiv.class,
        test.except.BC_invokevirtual01.class,
        test.except.BC_irem.class,
        test.except.BC_laload.class,
        test.except.BC_lastore.class,
        test.except.BC_ldiv.class,
        test.except.BC_lrem.class,
        test.except.BC_monitorenter.class,
        test.except.BC_multianewarray.class,
        test.except.BC_newarray.class,
        test.except.BC_putfield.class,
        test.except.BC_saload.class,
        test.except.BC_sastore.class,
        test.except.Catch_NPE_01.class,
        test.except.Catch_NPE_02.class,
        test.except.Catch_NPE_03.class,
        test.except.Catch_NPE_04.class,
        test.except.Catch_NPE_05.class,
        test.except.Catch_NPE_06.class,
        test.except.Catch_NPE_07.class,
        test.except.Catch_StackOverflowError_01.class,
        test.except.Catch_StackOverflowError_02.class,
        test.except.Throw_InCatch01.class,
        test.except.Throw_InCatch02.class,
        test.except.Throw_InCatch03.class,
        test.except.Throw_Synchronized01.class,
        test.except.Throw_Synchronized02.class,
        test.except.Throw_Synchronized03.class,
        test.except.Throw_Synchronized04.class,
        test.optimize.BC_idiv_16.class,
        test.optimize.BC_idiv_4.class,
        test.optimize.BC_imul_16.class,
        test.optimize.BC_imul_4.class,
        test.optimize.BC_ldiv_16.class,
        test.optimize.BC_ldiv_4.class,
        test.optimize.BC_lmul_16.class,
        test.optimize.BC_lmul_4.class,
        test.optimize.BC_lshr_C16.class,
        test.optimize.BC_lshr_C24.class,
        test.optimize.BC_lshr_C32.class,
        test.optimize.List_reorder_bug.class,
        test.optimize.TypeCastElem.class,
        test.lang.Boxed_TYPE_01.class,
        test.lang.Bridge_method01.class,
        test.lang.Class_Literal01.class,
        test.lang.Class_asSubclass01.class,
        test.lang.Class_cast01.class,
        test.lang.Class_cast02.class,
        test.lang.Class_forName01.class,
        test.lang.Class_forName02.class,
        test.lang.Class_getComponentType01.class,
        test.lang.Class_getName01.class,
        test.lang.Class_getName02.class,
        test.lang.Class_getSimpleName01.class,
        test.lang.Class_getSimpleName02.class,
        test.lang.Class_getSuperClass01.class,
        test.lang.Class_isArray01.class,
        test.lang.Class_isAssignableFrom01.class,
        test.lang.Class_isAssignableFrom02.class,
        test.lang.Class_isAssignableFrom03.class,
        test.lang.Class_isInstance01.class,
        test.lang.Class_isInstance02.class,
        test.lang.Class_isInstance03.class,
        test.lang.Class_isInstance04.class,
        test.lang.Class_isInstance05.class,
        test.lang.Class_isInstance06.class,
        test.lang.Class_isInterface01.class,
        test.lang.Class_isPrimitive01.class,
        test.lang.Double_toString.class,
        test.lang.Int_greater01.class,
        test.lang.Int_greater02.class,
        test.lang.Int_greater03.class,
        test.lang.Int_greaterEqual01.class,
        test.lang.Int_greaterEqual02.class,
        test.lang.Int_greaterEqual03.class,
        test.lang.Int_less01.class,
        test.lang.Int_less02.class,
        test.lang.Int_less03.class,
        test.lang.Int_lessEqual01.class,
        test.lang.Int_lessEqual02.class,
        test.lang.Int_lessEqual03.class,
        test.lang.JDK_ClassLoaders01.class,
        test.lang.JDK_ClassLoaders02.class,
        test.lang.Long_greater01.class,
        test.lang.Long_greater02.class,
        test.lang.Long_greater03.class,
        test.lang.Long_greaterEqual01.class,
        test.lang.Long_greaterEqual02.class,
        test.lang.Long_greaterEqual03.class,
        test.lang.Long_less01.class,
        test.lang.Long_less02.class,
        test.lang.Long_less03.class,
        test.lang.Long_lessEqual01.class,
        test.lang.Long_lessEqual02.class,
        test.lang.Long_lessEqual03.class,
        test.lang.Math_pow.class,
        test.lang.Object_clone01.class,
        test.lang.Object_clone02.class,
        test.lang.Object_equals01.class,
        test.lang.Object_getClass01.class,
        test.lang.Object_hashCode01.class,
        test.lang.Object_notify01.class,
        test.lang.Object_notify02.class,
        test.lang.Object_notifyAll01.class,
        test.lang.Object_notifyAll02.class,
        test.lang.Object_toString01.class,
        test.lang.Object_toString02.class,
        test.lang.Object_wait01.class,
        test.lang.Object_wait02.class,
        test.lang.Object_wait03.class,
        test.lang.StringCoding_Scale.class,
        test.lang.String_intern01.class,
        test.lang.String_intern02.class,
        test.lang.String_intern03.class,
        test.lang.String_valueOf01.class,
        test.lang.System_identityHashCode01.class,
        test.hotpath.HP_allocate01.class,
        test.hotpath.HP_allocate02.class,
        test.hotpath.HP_allocate03.class,
        test.hotpath.HP_array01.class,
        test.hotpath.HP_array02.class,
        test.hotpath.HP_array03.class,
        test.hotpath.HP_array04.class,
        test.hotpath.HP_control01.class,
        test.hotpath.HP_control02.class,
        test.hotpath.HP_convert01.class,
        test.hotpath.HP_count.class,
        test.hotpath.HP_dead01.class,
        test.hotpath.HP_demo01.class,
        test.hotpath.HP_field01.class,
        test.hotpath.HP_field02.class,
        test.hotpath.HP_field03.class,
        test.hotpath.HP_field04.class,
        test.hotpath.HP_idea.class,
        test.hotpath.HP_inline01.class,
        test.hotpath.HP_inline02.class,
        test.hotpath.HP_invoke01.class,
        test.hotpath.HP_life.class,
        test.hotpath.HP_nest01.class,
        test.hotpath.HP_nest02.class,
        test.hotpath.HP_scope01.class,
        test.hotpath.HP_scope02.class,
        test.hotpath.HP_series.class,
        test.hotpath.HP_trees01.class,
        test.reflect.Array_get01.class,
        test.reflect.Array_get02.class,
        test.reflect.Array_get03.class,
        test.reflect.Array_getBoolean01.class,
        test.reflect.Array_getByte01.class,
        test.reflect.Array_getChar01.class,
        test.reflect.Array_getDouble01.class,
        test.reflect.Array_getFloat01.class,
        test.reflect.Array_getInt01.class,
        test.reflect.Array_getLength01.class,
        test.reflect.Array_getLong01.class,
        test.reflect.Array_getShort01.class,
        test.reflect.Array_newInstance01.class,
        test.reflect.Array_newInstance02.class,
        test.reflect.Array_newInstance03.class,
        test.reflect.Array_newInstance04.class,
        test.reflect.Array_newInstance05.class,
        test.reflect.Array_newInstance06.class,
        test.reflect.Array_set01.class,
        test.reflect.Array_set02.class,
        test.reflect.Array_set03.class,
        test.reflect.Array_setBoolean01.class,
        test.reflect.Array_setByte01.class,
        test.reflect.Array_setChar01.class,
        test.reflect.Array_setDouble01.class,
        test.reflect.Array_setFloat01.class,
        test.reflect.Array_setInt01.class,
        test.reflect.Array_setLong01.class,
        test.reflect.Array_setShort01.class,
        test.reflect.Class_getDeclaredField01.class,
        test.reflect.Class_getDeclaredMethod01.class,
        test.reflect.Class_getField01.class,
        test.reflect.Class_getField02.class,
        test.reflect.Class_getMethod01.class,
        test.reflect.Class_getMethod02.class,
        test.reflect.Class_newInstance01.class,
        test.reflect.Class_newInstance02.class,
        test.reflect.Class_newInstance03.class,
        test.reflect.Class_newInstance06.class,
        test.reflect.Class_newInstance07.class,
        test.reflect.Field_get01.class,
        test.reflect.Field_get02.class,
        test.reflect.Field_get03.class,
        test.reflect.Field_get04.class,
        test.reflect.Field_getType01.class,
        test.reflect.Field_set01.class,
        test.reflect.Field_set02.class,
        test.reflect.Field_set03.class,
        test.reflect.Invoke_main01.class,
        test.reflect.Invoke_main02.class,
        test.reflect.Invoke_main03.class,
        test.reflect.Invoke_virtual01.class,
        test.reflect.Method_getParameterTypes01.class,
        test.reflect.Method_getReturnType01.class,
        test.reflect.Reflection_getCallerClass01.class,
        test.jdk.Class_getName.class,
        test.jdk.EnumMap01.class,
        test.jdk.EnumMap02.class,
        test.jdk.System_currentTimeMillis01.class,
        test.jdk.System_currentTimeMillis02.class,
        test.jdk.System_nanoTime01.class,
        test.jdk.System_nanoTime02.class,
        test.threads.Monitor_contended01.class,
        test.threads.Monitor_notowner01.class,
        test.threads.Monitorenter01.class,
        test.threads.Monitorenter02.class,
        test.threads.Object_wait01.class,
        test.threads.Object_wait02.class,
        test.threads.Object_wait03.class,
        test.threads.Object_wait04.class,
        test.threads.Thread_currentThread01.class,
        test.threads.Thread_getState01.class,
        test.threads.Thread_getState02.class,
        test.threads.Thread_holdsLock01.class,
        test.threads.Thread_isAlive01.class,
        test.threads.Thread_isInterrupted01.class,
        test.threads.Thread_isInterrupted02.class,
        test.threads.Thread_isInterrupted03.class,
        test.threads.Thread_isInterrupted04.class,
        test.threads.Thread_join01.class,
        test.threads.Thread_join02.class,
        test.threads.Thread_join03.class,
        test.threads.Thread_new01.class,
        test.threads.Thread_new02.class,
        test.threads.Thread_setPriority01.class,
        test.threads.Thread_sleep01.class,
        test.threads.Thread_start01.class,
        test.threads.Thread_yield01.class,
        test.micro.BC_invokevirtual2.class,
        test.micro.BigDoubleParams02.class,
        test.micro.BigFloatParams01.class,
        test.micro.BigFloatParams02.class,
        test.micro.BigIntParams01.class,
        test.micro.BigIntParams02.class,
        test.micro.BigLongParams02.class,
        test.micro.BigMixedParams01.class,
        test.micro.BigMixedParams02.class,
        test.micro.BigMixedParams03.class,
        test.micro.BigObjectParams01.class,
        test.micro.BigObjectParams02.class,
        test.micro.BigParamsAlignment.class,
        test.micro.Bubblesort.class,
        test.micro.Fibonacci.class,
        test.micro.InvokeVirtual_01.class,
        test.micro.InvokeVirtual_02.class,
        test.micro.StrangeFrames.class,
        test.micro.VarArgs_String01.class,
        test.micro.VarArgs_boolean01.class,
        test.micro.VarArgs_byte01.class,
        test.micro.VarArgs_char01.class,
        test.micro.VarArgs_double01.class,
        test.micro.VarArgs_float01.class,
        test.micro.VarArgs_int01.class,
        test.micro.VarArgs_long01.class,
        test.micro.VarArgs_short01.class,
        test.jvmni.JVM_ArrayCopy01.class,
        test.jvmni.JVM_GetClassContext01.class,
        test.jvmni.JVM_GetFreeMemory01.class,
        test.jvmni.JVM_GetMaxMemory01.class,
        test.jvmni.JVM_GetTotalMemory01.class,
        test.jvmni.JVM_IsNaN01.class
    };
    @Override
    public void run() {
        _testEnd = 452;
        vmStartUp();
        _total = _testEnd - _testStart;
        _testNum = _testStart;
        while (_testNum < _testEnd) {
            switch(_testNum) {
                case 0:
                    JavaTesterTests.test_bytecode_BC_aaload();
                    break;
                case 1:
                    JavaTesterTests.test_bytecode_BC_aastore();
                    break;
                case 2:
                    JavaTesterTests.test_bytecode_BC_aload_0();
                    break;
                case 3:
                    JavaTesterTests.test_bytecode_BC_aload_1();
                    break;
                case 4:
                    JavaTesterTests.test_bytecode_BC_aload_2();
                    break;
                case 5:
                    JavaTesterTests.test_bytecode_BC_aload_3();
                    break;
                case 6:
                    JavaTesterTests.test_bytecode_BC_anewarray();
                    break;
                case 7:
                    JavaTesterTests.test_bytecode_BC_areturn();
                    break;
                case 8:
                    JavaTesterTests.test_bytecode_BC_arraylength();
                    break;
                case 9:
                    JavaTesterTests.test_bytecode_BC_athrow();
                    break;
                case 10:
                    JavaTesterTests.test_bytecode_BC_baload();
                    break;
                case 11:
                    JavaTesterTests.test_bytecode_BC_bastore();
                    break;
                case 12:
                    JavaTesterTests.test_bytecode_BC_caload();
                    break;
                case 13:
                    JavaTesterTests.test_bytecode_BC_castore();
                    break;
                case 14:
                    JavaTesterTests.test_bytecode_BC_checkcast();
                    break;
                case 15:
                    JavaTesterTests.test_bytecode_BC_d2f();
                    break;
                case 16:
                    JavaTesterTests.test_bytecode_BC_d2i();
                    break;
                case 17:
                    JavaTesterTests.test_bytecode_BC_d2i_nan();
                    break;
                case 18:
                    JavaTesterTests.test_bytecode_BC_d2l();
                    break;
                case 19:
                    JavaTesterTests.test_bytecode_BC_d2l_nan();
                    break;
                case 20:
                    JavaTesterTests.test_bytecode_BC_dadd();
                    break;
                case 21:
                    JavaTesterTests.test_bytecode_BC_daload();
                    break;
                case 22:
                    JavaTesterTests.test_bytecode_BC_dastore();
                    break;
                case 23:
                    JavaTesterTests.test_bytecode_BC_dcmp();
                    break;
                case 24:
                    JavaTesterTests.test_bytecode_BC_ddiv();
                    break;
                case 25:
                    JavaTesterTests.test_bytecode_BC_dmul();
                    break;
                case 26:
                    JavaTesterTests.test_bytecode_BC_dneg();
                    break;
                case 27:
                    JavaTesterTests.test_bytecode_BC_drem();
                    break;
                case 28:
                    JavaTesterTests.test_bytecode_BC_dreturn();
                    break;
                case 29:
                    JavaTesterTests.test_bytecode_BC_dsub();
                    break;
                case 30:
                    JavaTesterTests.test_bytecode_BC_f2d();
                    break;
                case 31:
                    JavaTesterTests.test_bytecode_BC_f2i();
                    break;
                case 32:
                    JavaTesterTests.test_bytecode_BC_f2i_2();
                    break;
                case 33:
                    JavaTesterTests.test_bytecode_BC_f2i_nan();
                    break;
                case 34:
                    JavaTesterTests.test_bytecode_BC_f2l();
                    break;
                case 35:
                    JavaTesterTests.test_bytecode_BC_f2l_nan();
                    break;
                case 36:
                    JavaTesterTests.test_bytecode_BC_fadd();
                    break;
                case 37:
                    JavaTesterTests.test_bytecode_BC_faload();
                    break;
                case 38:
                    JavaTesterTests.test_bytecode_BC_fastore();
                    break;
                case 39:
                    JavaTesterTests.test_bytecode_BC_fcmp();
                    break;
                case 40:
                    JavaTesterTests.test_bytecode_BC_fdiv();
                    break;
                case 41:
                    JavaTesterTests.test_bytecode_BC_fload();
                    break;
                case 42:
                    JavaTesterTests.test_bytecode_BC_fload_2();
                    break;
                case 43:
                    JavaTesterTests.test_bytecode_BC_fmul();
                    break;
                case 44:
                    JavaTesterTests.test_bytecode_BC_fneg();
                    break;
                case 45:
                    JavaTesterTests.test_bytecode_BC_frem();
                    break;
                case 46:
                    JavaTesterTests.test_bytecode_BC_freturn();
                    break;
                case 47:
                    JavaTesterTests.test_bytecode_BC_fsub();
                    break;
                case 48:
                    JavaTesterTests.test_bytecode_BC_getfield();
                    break;
                case 49:
                    JavaTesterTests.test_bytecode_BC_getstatic_b();
                    break;
                case 50:
                    JavaTesterTests.test_bytecode_BC_getstatic_c();
                    break;
                case 51:
                    JavaTesterTests.test_bytecode_BC_getstatic_d();
                    break;
                case 52:
                    JavaTesterTests.test_bytecode_BC_getstatic_f();
                    break;
                case 53:
                    JavaTesterTests.test_bytecode_BC_getstatic_i();
                    break;
                case 54:
                    JavaTesterTests.test_bytecode_BC_getstatic_l();
                    break;
                case 55:
                    JavaTesterTests.test_bytecode_BC_getstatic_s();
                    break;
                case 56:
                    JavaTesterTests.test_bytecode_BC_getstatic_z();
                    break;
                case 57:
                    JavaTesterTests.test_bytecode_BC_i2b();
                    break;
                case 58:
                    JavaTesterTests.test_bytecode_BC_i2c();
                    break;
                case 59:
                    JavaTesterTests.test_bytecode_BC_i2d();
                    break;
                case 60:
                    JavaTesterTests.test_bytecode_BC_i2f();
                    break;
                case 61:
                    JavaTesterTests.test_bytecode_BC_i2l();
                    break;
                case 62:
                    JavaTesterTests.test_bytecode_BC_i2s();
                    break;
                case 63:
                    JavaTesterTests.test_bytecode_BC_iadd();
                    break;
                case 64:
                    JavaTesterTests.test_bytecode_BC_iadd2();
                    break;
                case 65:
                    JavaTesterTests.test_bytecode_BC_iadd3();
                    break;
                case 66:
                    JavaTesterTests.test_bytecode_BC_iaload();
                    break;
                case 67:
                    JavaTesterTests.test_bytecode_BC_iand();
                    break;
                case 68:
                    JavaTesterTests.test_bytecode_BC_iastore();
                    break;
                case 69:
                    JavaTesterTests.test_bytecode_BC_iconst();
                    break;
                case 70:
                    JavaTesterTests.test_bytecode_BC_idiv();
                    break;
                case 71:
                    JavaTesterTests.test_bytecode_BC_ifeq();
                    break;
                case 72:
                    JavaTesterTests.test_bytecode_BC_ifeq_2();
                    break;
                case 73:
                    JavaTesterTests.test_bytecode_BC_ifeq_3();
                    break;
                case 74:
                    JavaTesterTests.test_bytecode_BC_ifge();
                    break;
                case 75:
                    JavaTesterTests.test_bytecode_BC_ifge_2();
                    break;
                case 76:
                    JavaTesterTests.test_bytecode_BC_ifge_3();
                    break;
                case 77:
                    JavaTesterTests.test_bytecode_BC_ifgt();
                    break;
                case 78:
                    JavaTesterTests.test_bytecode_BC_ificmplt1();
                    break;
                case 79:
                    JavaTesterTests.test_bytecode_BC_ificmplt2();
                    break;
                case 80:
                    JavaTesterTests.test_bytecode_BC_ificmpne1();
                    break;
                case 81:
                    JavaTesterTests.test_bytecode_BC_ificmpne2();
                    break;
                case 82:
                    JavaTesterTests.test_bytecode_BC_ifle();
                    break;
                case 83:
                    JavaTesterTests.test_bytecode_BC_iflt();
                    break;
                case 84:
                    JavaTesterTests.test_bytecode_BC_ifne();
                    break;
                case 85:
                    JavaTesterTests.test_bytecode_BC_ifnonnull();
                    break;
                case 86:
                    JavaTesterTests.test_bytecode_BC_ifnonnull_2();
                    break;
                case 87:
                    JavaTesterTests.test_bytecode_BC_ifnonnull_3();
                    break;
                case 88:
                    JavaTesterTests.test_bytecode_BC_ifnull();
                    break;
                case 89:
                    JavaTesterTests.test_bytecode_BC_ifnull_2();
                    break;
                case 90:
                    JavaTesterTests.test_bytecode_BC_ifnull_3();
                    break;
                case 91:
                    JavaTesterTests.test_bytecode_BC_iinc_1();
                    break;
                case 92:
                    JavaTesterTests.test_bytecode_BC_iinc_2();
                    break;
                case 93:
                    JavaTesterTests.test_bytecode_BC_iinc_3();
                    break;
                case 94:
                    JavaTesterTests.test_bytecode_BC_iload_0();
                    break;
                case 95:
                    JavaTesterTests.test_bytecode_BC_iload_0_1();
                    break;
                case 96:
                    JavaTesterTests.test_bytecode_BC_iload_0_2();
                    break;
                case 97:
                    JavaTesterTests.test_bytecode_BC_iload_1();
                    break;
                case 98:
                    JavaTesterTests.test_bytecode_BC_iload_1_1();
                    break;
                case 99:
                    JavaTesterTests.test_bytecode_BC_iload_2();
                    break;
                case 100:
                    JavaTesterTests.test_bytecode_BC_iload_3();
                    break;
                case 101:
                    JavaTesterTests.test_bytecode_BC_imul();
                    break;
                case 102:
                    JavaTesterTests.test_bytecode_BC_ineg();
                    break;
                case 103:
                    JavaTesterTests.test_bytecode_BC_instanceof();
                    break;
                case 104:
                    JavaTesterTests.test_bytecode_BC_invokeinterface();
                    break;
                case 105:
                    JavaTesterTests.test_bytecode_BC_invokespecial();
                    break;
                case 106:
                    JavaTesterTests.test_bytecode_BC_invokespecial2();
                    break;
                case 107:
                    JavaTesterTests.test_bytecode_BC_invokestatic();
                    break;
                case 108:
                    JavaTesterTests.test_bytecode_BC_invokevirtual();
                    break;
                case 109:
                    JavaTesterTests.test_bytecode_BC_ior();
                    break;
                case 110:
                    JavaTesterTests.test_bytecode_BC_irem();
                    break;
                case 111:
                    JavaTesterTests.test_bytecode_BC_ireturn();
                    break;
                case 112:
                    JavaTesterTests.test_bytecode_BC_ishl();
                    break;
                case 113:
                    JavaTesterTests.test_bytecode_BC_ishr();
                    break;
                case 114:
                    JavaTesterTests.test_bytecode_BC_isub();
                    break;
                case 115:
                    JavaTesterTests.test_bytecode_BC_iushr();
                    break;
                case 116:
                    JavaTesterTests.test_bytecode_BC_ixor();
                    break;
                case 117:
                    JavaTesterTests.test_bytecode_BC_l2d();
                    break;
                case 118:
                    JavaTesterTests.test_bytecode_BC_l2f();
                    break;
                case 119:
                    JavaTesterTests.test_bytecode_BC_l2i();
                    break;
                case 120:
                    JavaTesterTests.test_bytecode_BC_ladd();
                    break;
                case 121:
                    JavaTesterTests.test_bytecode_BC_ladd2();
                    break;
                case 122:
                    JavaTesterTests.test_bytecode_BC_laload();
                    break;
                case 123:
                    JavaTesterTests.test_bytecode_BC_land();
                    break;
                case 124:
                    JavaTesterTests.test_bytecode_BC_lastore();
                    break;
                case 125:
                    JavaTesterTests.test_bytecode_BC_lcmp();
                    break;
                case 126:
                    JavaTesterTests.test_bytecode_BC_ldc_01();
                    break;
                case 127:
                    JavaTesterTests.test_bytecode_BC_ldc_02();
                    break;
                case 128:
                    JavaTesterTests.test_bytecode_BC_ldc_03();
                    break;
                case 129:
                    JavaTesterTests.test_bytecode_BC_ldc_04();
                    break;
                case 130:
                    JavaTesterTests.test_bytecode_BC_ldc_05();
                    break;
                case 131:
                    JavaTesterTests.test_bytecode_BC_ldiv();
                    break;
                case 132:
                    JavaTesterTests.test_bytecode_BC_lload_0();
                    break;
                case 133:
                    JavaTesterTests.test_bytecode_BC_lload_01();
                    break;
                case 134:
                    JavaTesterTests.test_bytecode_BC_lload_1();
                    break;
                case 135:
                    JavaTesterTests.test_bytecode_BC_lload_2();
                    break;
                case 136:
                    JavaTesterTests.test_bytecode_BC_lload_3();
                    break;
                case 137:
                    JavaTesterTests.test_bytecode_BC_lmul();
                    break;
                case 138:
                    JavaTesterTests.test_bytecode_BC_lneg();
                    break;
                case 139:
                    JavaTesterTests.test_bytecode_BC_lookupswitch01();
                    break;
                case 140:
                    JavaTesterTests.test_bytecode_BC_lookupswitch02();
                    break;
                case 141:
                    JavaTesterTests.test_bytecode_BC_lookupswitch03();
                    break;
                case 142:
                    JavaTesterTests.test_bytecode_BC_lookupswitch04();
                    break;
                case 143:
                    JavaTesterTests.test_bytecode_BC_lor();
                    break;
                case 144:
                    JavaTesterTests.test_bytecode_BC_lrem();
                    break;
                case 145:
                    JavaTesterTests.test_bytecode_BC_lreturn();
                    break;
                case 146:
                    JavaTesterTests.test_bytecode_BC_lshl();
                    break;
                case 147:
                    JavaTesterTests.test_bytecode_BC_lshr();
                    break;
                case 148:
                    JavaTesterTests.test_bytecode_BC_lsub();
                    break;
                case 149:
                    JavaTesterTests.test_bytecode_BC_lushr();
                    break;
                case 150:
                    JavaTesterTests.test_bytecode_BC_lxor();
                    break;
                case 151:
                    JavaTesterTests.test_bytecode_BC_monitorenter();
                    break;
                case 152:
                    JavaTesterTests.test_bytecode_BC_multianewarray();
                    break;
                case 153:
                    JavaTesterTests.test_bytecode_BC_new();
                    break;
                case 154:
                    JavaTesterTests.test_bytecode_BC_newarray();
                    break;
                case 155:
                    JavaTesterTests.test_bytecode_BC_putfield();
                    break;
                case 156:
                    JavaTesterTests.test_bytecode_BC_putstatic();
                    break;
                case 157:
                    JavaTesterTests.test_bytecode_BC_saload();
                    break;
                case 158:
                    JavaTesterTests.test_bytecode_BC_sastore();
                    break;
                case 159:
                    JavaTesterTests.test_bytecode_BC_tableswitch();
                    break;
                case 160:
                    JavaTesterTests.test_bytecode_BC_tableswitch2();
                    break;
                case 161:
                    JavaTesterTests.test_bytecode_BC_tableswitch3();
                    break;
                case 162:
                    JavaTesterTests.test_bytecode_BC_tableswitch4();
                    break;
                case 163:
                    JavaTesterTests.test_bytecode_BC_wide01();
                    break;
                case 164:
                    JavaTesterTests.test_bytecode_BC_wide02();
                    break;
                case 165:
                    JavaTesterTests.test_except_BC_aaload();
                    break;
                case 166:
                    JavaTesterTests.test_except_BC_aastore();
                    break;
                case 167:
                    JavaTesterTests.test_except_BC_anewarray();
                    break;
                case 168:
                    JavaTesterTests.test_except_BC_arraylength();
                    break;
                case 169:
                    JavaTesterTests.test_except_BC_athrow();
                    break;
                case 170:
                    JavaTesterTests.test_except_BC_athrow1();
                    break;
                case 171:
                    JavaTesterTests.test_except_BC_athrow2();
                    break;
                case 172:
                    JavaTesterTests.test_except_BC_athrow3();
                    break;
                case 173:
                    JavaTesterTests.test_except_BC_baload();
                    break;
                case 174:
                    JavaTesterTests.test_except_BC_bastore();
                    break;
                case 175:
                    JavaTesterTests.test_except_BC_caload();
                    break;
                case 176:
                    JavaTesterTests.test_except_BC_castore();
                    break;
                case 177:
                    JavaTesterTests.test_except_BC_checkcast();
                    break;
                case 178:
                    JavaTesterTests.test_except_BC_checkcast1();
                    break;
                case 179:
                    JavaTesterTests.test_except_BC_checkcast2();
                    break;
                case 180:
                    JavaTesterTests.test_except_BC_daload();
                    break;
                case 181:
                    JavaTesterTests.test_except_BC_dastore();
                    break;
                case 182:
                    JavaTesterTests.test_except_BC_faload();
                    break;
                case 183:
                    JavaTesterTests.test_except_BC_fastore();
                    break;
                case 184:
                    JavaTesterTests.test_except_BC_getfield();
                    break;
                case 185:
                    JavaTesterTests.test_except_BC_iaload();
                    break;
                case 186:
                    JavaTesterTests.test_except_BC_iastore();
                    break;
                case 187:
                    JavaTesterTests.test_except_BC_idiv();
                    break;
                case 188:
                    JavaTesterTests.test_except_BC_invokevirtual01();
                    break;
                case 189:
                    JavaTesterTests.test_except_BC_irem();
                    break;
                case 190:
                    JavaTesterTests.test_except_BC_laload();
                    break;
                case 191:
                    JavaTesterTests.test_except_BC_lastore();
                    break;
                case 192:
                    JavaTesterTests.test_except_BC_ldiv();
                    break;
                case 193:
                    JavaTesterTests.test_except_BC_lrem();
                    break;
                case 194:
                    JavaTesterTests.test_except_BC_monitorenter();
                    break;
                case 195:
                    JavaTesterTests.test_except_BC_multianewarray();
                    break;
                case 196:
                    JavaTesterTests.test_except_BC_newarray();
                    break;
                case 197:
                    JavaTesterTests.test_except_BC_putfield();
                    break;
                case 198:
                    JavaTesterTests.test_except_BC_saload();
                    break;
                case 199:
                    JavaTesterTests.test_except_BC_sastore();
                    break;
                case 200:
                    JavaTesterTests.test_except_Catch_NPE_01();
                    break;
                case 201:
                    JavaTesterTests.test_except_Catch_NPE_02();
                    break;
                case 202:
                    JavaTesterTests.test_except_Catch_NPE_03();
                    break;
                case 203:
                    JavaTesterTests.test_except_Catch_NPE_04();
                    break;
                case 204:
                    JavaTesterTests.test_except_Catch_NPE_05();
                    break;
                case 205:
                    JavaTesterTests.test_except_Catch_NPE_06();
                    break;
                case 206:
                    JavaTesterTests.test_except_Catch_NPE_07();
                    break;
                case 207:
                    JavaTesterTests.test_except_Catch_StackOverflowError_01();
                    break;
                case 208:
                    JavaTesterTests.test_except_Catch_StackOverflowError_02();
                    break;
                case 209:
                    JavaTesterTests.test_except_Throw_InCatch01();
                    break;
                case 210:
                    JavaTesterTests.test_except_Throw_InCatch02();
                    break;
                case 211:
                    JavaTesterTests.test_except_Throw_InCatch03();
                    break;
                case 212:
                    JavaTesterTests.test_except_Throw_Synchronized01();
                    break;
                case 213:
                    JavaTesterTests.test_except_Throw_Synchronized02();
                    break;
                case 214:
                    JavaTesterTests.test_except_Throw_Synchronized03();
                    break;
                case 215:
                    JavaTesterTests.test_except_Throw_Synchronized04();
                    break;
                case 216:
                    JavaTesterTests.test_optimize_BC_idiv_16();
                    break;
                case 217:
                    JavaTesterTests.test_optimize_BC_idiv_4();
                    break;
                case 218:
                    JavaTesterTests.test_optimize_BC_imul_16();
                    break;
                case 219:
                    JavaTesterTests.test_optimize_BC_imul_4();
                    break;
                case 220:
                    JavaTesterTests.test_optimize_BC_ldiv_16();
                    break;
                case 221:
                    JavaTesterTests.test_optimize_BC_ldiv_4();
                    break;
                case 222:
                    JavaTesterTests.test_optimize_BC_lmul_16();
                    break;
                case 223:
                    JavaTesterTests.test_optimize_BC_lmul_4();
                    break;
                case 224:
                    JavaTesterTests.test_optimize_BC_lshr_C16();
                    break;
                case 225:
                    JavaTesterTests.test_optimize_BC_lshr_C24();
                    break;
                case 226:
                    JavaTesterTests.test_optimize_BC_lshr_C32();
                    break;
                case 227:
                    JavaTesterTests.test_optimize_List_reorder_bug();
                    break;
                case 228:
                    JavaTesterTests.test_optimize_TypeCastElem();
                    break;
                case 229:
                    JavaTesterTests.test_lang_Boxed_TYPE_01();
                    break;
                case 230:
                    JavaTesterTests.test_lang_Bridge_method01();
                    break;
                case 231:
                    JavaTesterTests.test_lang_Class_Literal01();
                    break;
                case 232:
                    JavaTesterTests.test_lang_Class_asSubclass01();
                    break;
                case 233:
                    JavaTesterTests.test_lang_Class_cast01();
                    break;
                case 234:
                    JavaTesterTests.test_lang_Class_cast02();
                    break;
                case 235:
                    JavaTesterTests.test_lang_Class_forName01();
                    break;
                case 236:
                    JavaTesterTests.test_lang_Class_forName02();
                    break;
                case 237:
                    JavaTesterTests.test_lang_Class_getComponentType01();
                    break;
                case 238:
                    JavaTesterTests.test_lang_Class_getName01();
                    break;
                case 239:
                    JavaTesterTests.test_lang_Class_getName02();
                    break;
                case 240:
                    JavaTesterTests.test_lang_Class_getSimpleName01();
                    break;
                case 241:
                    JavaTesterTests.test_lang_Class_getSimpleName02();
                    break;
                case 242:
                    JavaTesterTests.test_lang_Class_getSuperClass01();
                    break;
                case 243:
                    JavaTesterTests.test_lang_Class_isArray01();
                    break;
                case 244:
                    JavaTesterTests.test_lang_Class_isAssignableFrom01();
                    break;
                case 245:
                    JavaTesterTests.test_lang_Class_isAssignableFrom02();
                    break;
                case 246:
                    JavaTesterTests.test_lang_Class_isAssignableFrom03();
                    break;
                case 247:
                    JavaTesterTests.test_lang_Class_isInstance01();
                    break;
                case 248:
                    JavaTesterTests.test_lang_Class_isInstance02();
                    break;
                case 249:
                    JavaTesterTests.test_lang_Class_isInstance03();
                    break;
                case 250:
                    JavaTesterTests.test_lang_Class_isInstance04();
                    break;
                case 251:
                    JavaTesterTests.test_lang_Class_isInstance05();
                    break;
                case 252:
                    JavaTesterTests.test_lang_Class_isInstance06();
                    break;
                case 253:
                    JavaTesterTests.test_lang_Class_isInterface01();
                    break;
                case 254:
                    JavaTesterTests.test_lang_Class_isPrimitive01();
                    break;
                case 255:
                    JavaTesterTests.test_lang_Double_toString();
                    break;
                case 256:
                    JavaTesterTests.test_lang_Int_greater01();
                    break;
                case 257:
                    JavaTesterTests.test_lang_Int_greater02();
                    break;
                case 258:
                    JavaTesterTests.test_lang_Int_greater03();
                    break;
                case 259:
                    JavaTesterTests.test_lang_Int_greaterEqual01();
                    break;
                case 260:
                    JavaTesterTests.test_lang_Int_greaterEqual02();
                    break;
                case 261:
                    JavaTesterTests.test_lang_Int_greaterEqual03();
                    break;
                case 262:
                    JavaTesterTests.test_lang_Int_less01();
                    break;
                case 263:
                    JavaTesterTests.test_lang_Int_less02();
                    break;
                case 264:
                    JavaTesterTests.test_lang_Int_less03();
                    break;
                case 265:
                    JavaTesterTests.test_lang_Int_lessEqual01();
                    break;
                case 266:
                    JavaTesterTests.test_lang_Int_lessEqual02();
                    break;
                case 267:
                    JavaTesterTests.test_lang_Int_lessEqual03();
                    break;
                case 268:
                    JavaTesterTests.test_lang_JDK_ClassLoaders01();
                    break;
                case 269:
                    JavaTesterTests.test_lang_JDK_ClassLoaders02();
                    break;
                case 270:
                    JavaTesterTests.test_lang_Long_greater01();
                    break;
                case 271:
                    JavaTesterTests.test_lang_Long_greater02();
                    break;
                case 272:
                    JavaTesterTests.test_lang_Long_greater03();
                    break;
                case 273:
                    JavaTesterTests.test_lang_Long_greaterEqual01();
                    break;
                case 274:
                    JavaTesterTests.test_lang_Long_greaterEqual02();
                    break;
                case 275:
                    JavaTesterTests.test_lang_Long_greaterEqual03();
                    break;
                case 276:
                    JavaTesterTests.test_lang_Long_less01();
                    break;
                case 277:
                    JavaTesterTests.test_lang_Long_less02();
                    break;
                case 278:
                    JavaTesterTests.test_lang_Long_less03();
                    break;
                case 279:
                    JavaTesterTests.test_lang_Long_lessEqual01();
                    break;
                case 280:
                    JavaTesterTests.test_lang_Long_lessEqual02();
                    break;
                case 281:
                    JavaTesterTests.test_lang_Long_lessEqual03();
                    break;
                case 282:
                    JavaTesterTests.test_lang_Math_pow();
                    break;
                case 283:
                    JavaTesterTests.test_lang_Object_clone01();
                    break;
                case 284:
                    JavaTesterTests.test_lang_Object_clone02();
                    break;
                case 285:
                    JavaTesterTests.test_lang_Object_equals01();
                    break;
                case 286:
                    JavaTesterTests.test_lang_Object_getClass01();
                    break;
                case 287:
                    JavaTesterTests.test_lang_Object_hashCode01();
                    break;
                case 288:
                    JavaTesterTests.test_lang_Object_notify01();
                    break;
                case 289:
                    JavaTesterTests.test_lang_Object_notify02();
                    break;
                case 290:
                    JavaTesterTests.test_lang_Object_notifyAll01();
                    break;
                case 291:
                    JavaTesterTests.test_lang_Object_notifyAll02();
                    break;
                case 292:
                    JavaTesterTests.test_lang_Object_toString01();
                    break;
                case 293:
                    JavaTesterTests.test_lang_Object_toString02();
                    break;
                case 294:
                    JavaTesterTests.test_lang_Object_wait01();
                    break;
                case 295:
                    JavaTesterTests.test_lang_Object_wait02();
                    break;
                case 296:
                    JavaTesterTests.test_lang_Object_wait03();
                    break;
                case 297:
                    JavaTesterTests.test_lang_StringCoding_Scale();
                    break;
                case 298:
                    JavaTesterTests.test_lang_String_intern01();
                    break;
                case 299:
                    JavaTesterTests.test_lang_String_intern02();
                    break;
                case 300:
                    JavaTesterTests.test_lang_String_intern03();
                    break;
                case 301:
                    JavaTesterTests.test_lang_String_valueOf01();
                    break;
                case 302:
                    JavaTesterTests.test_lang_System_identityHashCode01();
                    break;
                case 303:
                    JavaTesterTests.test_hotpath_HP_allocate01();
                    break;
                case 304:
                    JavaTesterTests.test_hotpath_HP_allocate02();
                    break;
                case 305:
                    JavaTesterTests.test_hotpath_HP_allocate03();
                    break;
                case 306:
                    JavaTesterTests.test_hotpath_HP_array01();
                    break;
                case 307:
                    JavaTesterTests.test_hotpath_HP_array02();
                    break;
                case 308:
                    JavaTesterTests.test_hotpath_HP_array03();
                    break;
                case 309:
                    JavaTesterTests.test_hotpath_HP_array04();
                    break;
                case 310:
                    JavaTesterTests.test_hotpath_HP_control01();
                    break;
                case 311:
                    JavaTesterTests.test_hotpath_HP_control02();
                    break;
                case 312:
                    JavaTesterTests.test_hotpath_HP_convert01();
                    break;
                case 313:
                    JavaTesterTests.test_hotpath_HP_count();
                    break;
                case 314:
                    JavaTesterTests.test_hotpath_HP_dead01();
                    break;
                case 315:
                    JavaTesterTests.test_hotpath_HP_demo01();
                    break;
                case 316:
                    JavaTesterTests.test_hotpath_HP_field01();
                    break;
                case 317:
                    JavaTesterTests.test_hotpath_HP_field02();
                    break;
                case 318:
                    JavaTesterTests.test_hotpath_HP_field03();
                    break;
                case 319:
                    JavaTesterTests.test_hotpath_HP_field04();
                    break;
                case 320:
                    JavaTesterTests.test_hotpath_HP_idea();
                    break;
                case 321:
                    JavaTesterTests.test_hotpath_HP_inline01();
                    break;
                case 322:
                    JavaTesterTests.test_hotpath_HP_inline02();
                    break;
                case 323:
                    JavaTesterTests.test_hotpath_HP_invoke01();
                    break;
                case 324:
                    JavaTesterTests.test_hotpath_HP_life();
                    break;
                case 325:
                    JavaTesterTests.test_hotpath_HP_nest01();
                    break;
                case 326:
                    JavaTesterTests.test_hotpath_HP_nest02();
                    break;
                case 327:
                    JavaTesterTests.test_hotpath_HP_scope01();
                    break;
                case 328:
                    JavaTesterTests.test_hotpath_HP_scope02();
                    break;
                case 329:
                    JavaTesterTests.test_hotpath_HP_series();
                    break;
                case 330:
                    JavaTesterTests.test_hotpath_HP_trees01();
                    break;
                case 331:
                    JavaTesterTests.test_reflect_Array_get01();
                    break;
                case 332:
                    JavaTesterTests.test_reflect_Array_get02();
                    break;
                case 333:
                    JavaTesterTests.test_reflect_Array_get03();
                    break;
                case 334:
                    JavaTesterTests.test_reflect_Array_getBoolean01();
                    break;
                case 335:
                    JavaTesterTests.test_reflect_Array_getByte01();
                    break;
                case 336:
                    JavaTesterTests.test_reflect_Array_getChar01();
                    break;
                case 337:
                    JavaTesterTests.test_reflect_Array_getDouble01();
                    break;
                case 338:
                    JavaTesterTests.test_reflect_Array_getFloat01();
                    break;
                case 339:
                    JavaTesterTests.test_reflect_Array_getInt01();
                    break;
                case 340:
                    JavaTesterTests.test_reflect_Array_getLength01();
                    break;
                case 341:
                    JavaTesterTests.test_reflect_Array_getLong01();
                    break;
                case 342:
                    JavaTesterTests.test_reflect_Array_getShort01();
                    break;
                case 343:
                    JavaTesterTests.test_reflect_Array_newInstance01();
                    break;
                case 344:
                    JavaTesterTests.test_reflect_Array_newInstance02();
                    break;
                case 345:
                    JavaTesterTests.test_reflect_Array_newInstance03();
                    break;
                case 346:
                    JavaTesterTests.test_reflect_Array_newInstance04();
                    break;
                case 347:
                    JavaTesterTests.test_reflect_Array_newInstance05();
                    break;
                case 348:
                    JavaTesterTests.test_reflect_Array_newInstance06();
                    break;
                case 349:
                    JavaTesterTests.test_reflect_Array_set01();
                    break;
                case 350:
                    JavaTesterTests.test_reflect_Array_set02();
                    break;
                case 351:
                    JavaTesterTests.test_reflect_Array_set03();
                    break;
                case 352:
                    JavaTesterTests.test_reflect_Array_setBoolean01();
                    break;
                case 353:
                    JavaTesterTests.test_reflect_Array_setByte01();
                    break;
                case 354:
                    JavaTesterTests.test_reflect_Array_setChar01();
                    break;
                case 355:
                    JavaTesterTests.test_reflect_Array_setDouble01();
                    break;
                case 356:
                    JavaTesterTests.test_reflect_Array_setFloat01();
                    break;
                case 357:
                    JavaTesterTests.test_reflect_Array_setInt01();
                    break;
                case 358:
                    JavaTesterTests.test_reflect_Array_setLong01();
                    break;
                case 359:
                    JavaTesterTests.test_reflect_Array_setShort01();
                    break;
                case 360:
                    JavaTesterTests.test_reflect_Class_getDeclaredField01();
                    break;
                case 361:
                    JavaTesterTests.test_reflect_Class_getDeclaredMethod01();
                    break;
                case 362:
                    JavaTesterTests.test_reflect_Class_getField01();
                    break;
                case 363:
                    JavaTesterTests.test_reflect_Class_getField02();
                    break;
                case 364:
                    JavaTesterTests.test_reflect_Class_getMethod01();
                    break;
                case 365:
                    JavaTesterTests.test_reflect_Class_getMethod02();
                    break;
                case 366:
                    JavaTesterTests.test_reflect_Class_newInstance01();
                    break;
                case 367:
                    JavaTesterTests.test_reflect_Class_newInstance02();
                    break;
                case 368:
                    JavaTesterTests.test_reflect_Class_newInstance03();
                    break;
                case 369:
                    JavaTesterTests.test_reflect_Class_newInstance06();
                    break;
                case 370:
                    JavaTesterTests.test_reflect_Class_newInstance07();
                    break;
                case 371:
                    JavaTesterTests.test_reflect_Field_get01();
                    break;
                case 372:
                    JavaTesterTests.test_reflect_Field_get02();
                    break;
                case 373:
                    JavaTesterTests.test_reflect_Field_get03();
                    break;
                case 374:
                    JavaTesterTests.test_reflect_Field_get04();
                    break;
                case 375:
                    JavaTesterTests.test_reflect_Field_getType01();
                    break;
                case 376:
                    JavaTesterTests.test_reflect_Field_set01();
                    break;
                case 377:
                    JavaTesterTests.test_reflect_Field_set02();
                    break;
                case 378:
                    JavaTesterTests.test_reflect_Field_set03();
                    break;
                case 379:
                    JavaTesterTests.test_reflect_Invoke_main01();
                    break;
                case 380:
                    JavaTesterTests.test_reflect_Invoke_main02();
                    break;
                case 381:
                    JavaTesterTests.test_reflect_Invoke_main03();
                    break;
                case 382:
                    JavaTesterTests.test_reflect_Invoke_virtual01();
                    break;
                case 383:
                    JavaTesterTests.test_reflect_Method_getParameterTypes01();
                    break;
                case 384:
                    JavaTesterTests.test_reflect_Method_getReturnType01();
                    break;
                case 385:
                    JavaTesterTests.test_reflect_Reflection_getCallerClass01();
                    break;
                case 386:
                    JavaTesterTests.test_jdk_Class_getName();
                    break;
                case 387:
                    JavaTesterTests.test_jdk_EnumMap01();
                    break;
                case 388:
                    JavaTesterTests.test_jdk_EnumMap02();
                    break;
                case 389:
                    JavaTesterTests.test_jdk_System_currentTimeMillis01();
                    break;
                case 390:
                    JavaTesterTests.test_jdk_System_currentTimeMillis02();
                    break;
                case 391:
                    JavaTesterTests.test_jdk_System_nanoTime01();
                    break;
                case 392:
                    JavaTesterTests.test_jdk_System_nanoTime02();
                    break;
                case 393:
                    JavaTesterTests.test_threads_Monitor_contended01();
                    break;
                case 394:
                    JavaTesterTests.test_threads_Monitor_notowner01();
                    break;
                case 395:
                    JavaTesterTests.test_threads_Monitorenter01();
                    break;
                case 396:
                    JavaTesterTests.test_threads_Monitorenter02();
                    break;
                case 397:
                    JavaTesterTests.test_threads_Object_wait01();
                    break;
                case 398:
                    JavaTesterTests.test_threads_Object_wait02();
                    break;
                case 399:
                    JavaTesterTests.test_threads_Object_wait03();
                    break;
                case 400:
                    JavaTesterTests.test_threads_Object_wait04();
                    break;
                case 401:
                    JavaTesterTests.test_threads_Thread_currentThread01();
                    break;
                case 402:
                    JavaTesterTests.test_threads_Thread_getState01();
                    break;
                case 403:
                    JavaTesterTests.test_threads_Thread_getState02();
                    break;
                case 404:
                    JavaTesterTests.test_threads_Thread_holdsLock01();
                    break;
                case 405:
                    JavaTesterTests.test_threads_Thread_isAlive01();
                    break;
                case 406:
                    JavaTesterTests.test_threads_Thread_isInterrupted01();
                    break;
                case 407:
                    JavaTesterTests.test_threads_Thread_isInterrupted02();
                    break;
                case 408:
                    JavaTesterTests.test_threads_Thread_isInterrupted03();
                    break;
                case 409:
                    JavaTesterTests.test_threads_Thread_isInterrupted04();
                    break;
                case 410:
                    JavaTesterTests.test_threads_Thread_join01();
                    break;
                case 411:
                    JavaTesterTests.test_threads_Thread_join02();
                    break;
                case 412:
                    JavaTesterTests.test_threads_Thread_join03();
                    break;
                case 413:
                    JavaTesterTests.test_threads_Thread_new01();
                    break;
                case 414:
                    JavaTesterTests.test_threads_Thread_new02();
                    break;
                case 415:
                    JavaTesterTests.test_threads_Thread_setPriority01();
                    break;
                case 416:
                    JavaTesterTests.test_threads_Thread_sleep01();
                    break;
                case 417:
                    JavaTesterTests.test_threads_Thread_start01();
                    break;
                case 418:
                    JavaTesterTests.test_threads_Thread_yield01();
                    break;
                case 419:
                    JavaTesterTests.test_micro_BC_invokevirtual2();
                    break;
                case 420:
                    JavaTesterTests.test_micro_BigDoubleParams02();
                    break;
                case 421:
                    JavaTesterTests.test_micro_BigFloatParams01();
                    break;
                case 422:
                    JavaTesterTests.test_micro_BigFloatParams02();
                    break;
                case 423:
                    JavaTesterTests.test_micro_BigIntParams01();
                    break;
                case 424:
                    JavaTesterTests.test_micro_BigIntParams02();
                    break;
                case 425:
                    JavaTesterTests.test_micro_BigLongParams02();
                    break;
                case 426:
                    JavaTesterTests.test_micro_BigMixedParams01();
                    break;
                case 427:
                    JavaTesterTests.test_micro_BigMixedParams02();
                    break;
                case 428:
                    JavaTesterTests.test_micro_BigMixedParams03();
                    break;
                case 429:
                    JavaTesterTests.test_micro_BigObjectParams01();
                    break;
                case 430:
                    JavaTesterTests.test_micro_BigObjectParams02();
                    break;
                case 431:
                    JavaTesterTests.test_micro_BigParamsAlignment();
                    break;
                case 432:
                    JavaTesterTests.test_micro_Bubblesort();
                    break;
                case 433:
                    JavaTesterTests.test_micro_Fibonacci();
                    break;
                case 434:
                    JavaTesterTests.test_micro_InvokeVirtual_01();
                    break;
                case 435:
                    JavaTesterTests.test_micro_InvokeVirtual_02();
                    break;
                case 436:
                    JavaTesterTests.test_micro_StrangeFrames();
                    break;
                case 437:
                    JavaTesterTests.test_micro_VarArgs_String01();
                    break;
                case 438:
                    JavaTesterTests.test_micro_VarArgs_boolean01();
                    break;
                case 439:
                    JavaTesterTests.test_micro_VarArgs_byte01();
                    break;
                case 440:
                    JavaTesterTests.test_micro_VarArgs_char01();
                    break;
                case 441:
                    JavaTesterTests.test_micro_VarArgs_double01();
                    break;
                case 442:
                    JavaTesterTests.test_micro_VarArgs_float01();
                    break;
                case 443:
                    JavaTesterTests.test_micro_VarArgs_int01();
                    break;
                case 444:
                    JavaTesterTests.test_micro_VarArgs_long01();
                    break;
                case 445:
                    JavaTesterTests.test_micro_VarArgs_short01();
                    break;
                case 446:
                    JavaTesterTests.test_jvmni_JVM_ArrayCopy01();
                    break;
                case 447:
                    JavaTesterTests.test_jvmni_JVM_GetClassContext01();
                    break;
                case 448:
                    JavaTesterTests.test_jvmni_JVM_GetFreeMemory01();
                    break;
                case 449:
                    JavaTesterTests.test_jvmni_JVM_GetMaxMemory01();
                    break;
                case 450:
                    JavaTesterTests.test_jvmni_JVM_GetTotalMemory01();
                    break;
                case 451:
                    JavaTesterTests.test_jvmni_JVM_IsNaN01();
            }
        }
        reportPassed(_passed, _total);
    }
// END GENERATED TEST RUNS
}
