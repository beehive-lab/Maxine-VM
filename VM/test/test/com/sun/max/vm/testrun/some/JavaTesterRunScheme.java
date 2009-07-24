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
/*
 * Copyright (c) 2007 Sun Microsystems, Inc. All rights reserved. Use is subject to license terms.
 */
package test.com.sun.max.vm.testrun.some;

import test.com.sun.max.vm.testrun.*;

import com.sun.max.annotate.*;
import com.sun.max.vm.*;


public class JavaTesterRunScheme extends AbstractTester {

    public JavaTesterRunScheme(VMConfiguration vmConfiguration) {
        super(vmConfiguration);
    }

    @PROTOTYPE_ONLY
    @Override
    public Class<?>[] getClassList() {
        return classList;
    }

    @PROTOTYPE_ONLY
// GENERATED TEST RUNS
    private static final Class<?>[] classList = {
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
        test.bytecode.BC_dcmp01.class,
        test.bytecode.BC_dcmp02.class,
        test.bytecode.BC_dcmp03.class,
        test.bytecode.BC_dcmp04.class,
        test.bytecode.BC_dcmp05.class,
        test.bytecode.BC_dcmp06.class,
        test.bytecode.BC_dcmp07.class,
        test.bytecode.BC_dcmp08.class,
        test.bytecode.BC_dcmp09.class,
        test.bytecode.BC_dcmp10.class,
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
        test.bytecode.BC_fcmp01.class,
        test.bytecode.BC_fcmp02.class,
        test.bytecode.BC_fcmp03.class,
        test.bytecode.BC_fcmp04.class,
        test.bytecode.BC_fcmp05.class,
        test.bytecode.BC_fcmp06.class,
        test.bytecode.BC_fcmp07.class,
        test.bytecode.BC_fcmp08.class,
        test.bytecode.BC_fcmp09.class,
        test.bytecode.BC_fcmp10.class,
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
        test.bytecode.BC_iinc_4.class,
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
        test.bytecode.BC_ldc_06.class,
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
        test.bytecode.BC_wide02.class
    };
    @Override
    public void runTests() {
        total = testEnd - testStart;
        testNum = testStart;
        while (testNum < testEnd) {
            switch(testNum) {
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
                    JavaTesterTests.test_bytecode_BC_dcmp01();
                    break;
                case 24:
                    JavaTesterTests.test_bytecode_BC_dcmp02();
                    break;
                case 25:
                    JavaTesterTests.test_bytecode_BC_dcmp03();
                    break;
                case 26:
                    JavaTesterTests.test_bytecode_BC_dcmp04();
                    break;
                case 27:
                    JavaTesterTests.test_bytecode_BC_dcmp05();
                    break;
                case 28:
                    JavaTesterTests.test_bytecode_BC_dcmp06();
                    break;
                case 29:
                    JavaTesterTests.test_bytecode_BC_dcmp07();
                    break;
                case 30:
                    JavaTesterTests.test_bytecode_BC_dcmp08();
                    break;
                case 31:
                    JavaTesterTests.test_bytecode_BC_dcmp09();
                    break;
                case 32:
                    JavaTesterTests.test_bytecode_BC_dcmp10();
                    break;
                case 33:
                    JavaTesterTests.test_bytecode_BC_ddiv();
                    break;
                case 34:
                    JavaTesterTests.test_bytecode_BC_dmul();
                    break;
                case 35:
                    JavaTesterTests.test_bytecode_BC_dneg();
                    break;
                case 36:
                    JavaTesterTests.test_bytecode_BC_drem();
                    break;
                case 37:
                    JavaTesterTests.test_bytecode_BC_dreturn();
                    break;
                case 38:
                    JavaTesterTests.test_bytecode_BC_dsub();
                    break;
                case 39:
                    JavaTesterTests.test_bytecode_BC_f2d();
                    break;
                case 40:
                    JavaTesterTests.test_bytecode_BC_f2i();
                    break;
                case 41:
                    JavaTesterTests.test_bytecode_BC_f2i_2();
                    break;
                case 42:
                    JavaTesterTests.test_bytecode_BC_f2i_nan();
                    break;
                case 43:
                    JavaTesterTests.test_bytecode_BC_f2l();
                    break;
                case 44:
                    JavaTesterTests.test_bytecode_BC_f2l_nan();
                    break;
                case 45:
                    JavaTesterTests.test_bytecode_BC_fadd();
                    break;
                case 46:
                    JavaTesterTests.test_bytecode_BC_faload();
                    break;
                case 47:
                    JavaTesterTests.test_bytecode_BC_fastore();
                    break;
                case 48:
                    JavaTesterTests.test_bytecode_BC_fcmp01();
                    break;
                case 49:
                    JavaTesterTests.test_bytecode_BC_fcmp02();
                    break;
                case 50:
                    JavaTesterTests.test_bytecode_BC_fcmp03();
                    break;
                case 51:
                    JavaTesterTests.test_bytecode_BC_fcmp04();
                    break;
                case 52:
                    JavaTesterTests.test_bytecode_BC_fcmp05();
                    break;
                case 53:
                    JavaTesterTests.test_bytecode_BC_fcmp06();
                    break;
                case 54:
                    JavaTesterTests.test_bytecode_BC_fcmp07();
                    break;
                case 55:
                    JavaTesterTests.test_bytecode_BC_fcmp08();
                    break;
                case 56:
                    JavaTesterTests.test_bytecode_BC_fcmp09();
                    break;
                case 57:
                    JavaTesterTests.test_bytecode_BC_fcmp10();
                    break;
                case 58:
                    JavaTesterTests.test_bytecode_BC_fdiv();
                    break;
                case 59:
                    JavaTesterTests.test_bytecode_BC_fload();
                    break;
                case 60:
                    JavaTesterTests.test_bytecode_BC_fload_2();
                    break;
                case 61:
                    JavaTesterTests.test_bytecode_BC_fmul();
                    break;
                case 62:
                    JavaTesterTests.test_bytecode_BC_fneg();
                    break;
                case 63:
                    JavaTesterTests.test_bytecode_BC_frem();
                    break;
                case 64:
                    JavaTesterTests.test_bytecode_BC_freturn();
                    break;
                case 65:
                    JavaTesterTests.test_bytecode_BC_fsub();
                    break;
                case 66:
                    JavaTesterTests.test_bytecode_BC_getfield();
                    break;
                case 67:
                    JavaTesterTests.test_bytecode_BC_getstatic_b();
                    break;
                case 68:
                    JavaTesterTests.test_bytecode_BC_getstatic_c();
                    break;
                case 69:
                    JavaTesterTests.test_bytecode_BC_getstatic_d();
                    break;
                case 70:
                    JavaTesterTests.test_bytecode_BC_getstatic_f();
                    break;
                case 71:
                    JavaTesterTests.test_bytecode_BC_getstatic_i();
                    break;
                case 72:
                    JavaTesterTests.test_bytecode_BC_getstatic_l();
                    break;
                case 73:
                    JavaTesterTests.test_bytecode_BC_getstatic_s();
                    break;
                case 74:
                    JavaTesterTests.test_bytecode_BC_getstatic_z();
                    break;
                case 75:
                    JavaTesterTests.test_bytecode_BC_i2b();
                    break;
                case 76:
                    JavaTesterTests.test_bytecode_BC_i2c();
                    break;
                case 77:
                    JavaTesterTests.test_bytecode_BC_i2d();
                    break;
                case 78:
                    JavaTesterTests.test_bytecode_BC_i2f();
                    break;
                case 79:
                    JavaTesterTests.test_bytecode_BC_i2l();
                    break;
                case 80:
                    JavaTesterTests.test_bytecode_BC_i2s();
                    break;
                case 81:
                    JavaTesterTests.test_bytecode_BC_iadd();
                    break;
                case 82:
                    JavaTesterTests.test_bytecode_BC_iadd2();
                    break;
                case 83:
                    JavaTesterTests.test_bytecode_BC_iadd3();
                    break;
                case 84:
                    JavaTesterTests.test_bytecode_BC_iaload();
                    break;
                case 85:
                    JavaTesterTests.test_bytecode_BC_iand();
                    break;
                case 86:
                    JavaTesterTests.test_bytecode_BC_iastore();
                    break;
                case 87:
                    JavaTesterTests.test_bytecode_BC_iconst();
                    break;
                case 88:
                    JavaTesterTests.test_bytecode_BC_idiv();
                    break;
                case 89:
                    JavaTesterTests.test_bytecode_BC_ifeq();
                    break;
                case 90:
                    JavaTesterTests.test_bytecode_BC_ifeq_2();
                    break;
                case 91:
                    JavaTesterTests.test_bytecode_BC_ifeq_3();
                    break;
                case 92:
                    JavaTesterTests.test_bytecode_BC_ifge();
                    break;
                case 93:
                    JavaTesterTests.test_bytecode_BC_ifge_2();
                    break;
                case 94:
                    JavaTesterTests.test_bytecode_BC_ifge_3();
                    break;
                case 95:
                    JavaTesterTests.test_bytecode_BC_ifgt();
                    break;
                case 96:
                    JavaTesterTests.test_bytecode_BC_ificmplt1();
                    break;
                case 97:
                    JavaTesterTests.test_bytecode_BC_ificmplt2();
                    break;
                case 98:
                    JavaTesterTests.test_bytecode_BC_ificmpne1();
                    break;
                case 99:
                    JavaTesterTests.test_bytecode_BC_ificmpne2();
                    break;
                case 100:
                    JavaTesterTests.test_bytecode_BC_ifle();
                    break;
                case 101:
                    JavaTesterTests.test_bytecode_BC_iflt();
                    break;
                case 102:
                    JavaTesterTests.test_bytecode_BC_ifne();
                    break;
                case 103:
                    JavaTesterTests.test_bytecode_BC_ifnonnull();
                    break;
                case 104:
                    JavaTesterTests.test_bytecode_BC_ifnonnull_2();
                    break;
                case 105:
                    JavaTesterTests.test_bytecode_BC_ifnonnull_3();
                    break;
                case 106:
                    JavaTesterTests.test_bytecode_BC_ifnull();
                    break;
                case 107:
                    JavaTesterTests.test_bytecode_BC_ifnull_2();
                    break;
                case 108:
                    JavaTesterTests.test_bytecode_BC_ifnull_3();
                    break;
                case 109:
                    JavaTesterTests.test_bytecode_BC_iinc_1();
                    break;
                case 110:
                    JavaTesterTests.test_bytecode_BC_iinc_2();
                    break;
                case 111:
                    JavaTesterTests.test_bytecode_BC_iinc_3();
                    break;
                case 112:
                    JavaTesterTests.test_bytecode_BC_iinc_4();
                    break;
                case 113:
                    JavaTesterTests.test_bytecode_BC_iload_0();
                    break;
                case 114:
                    JavaTesterTests.test_bytecode_BC_iload_0_1();
                    break;
                case 115:
                    JavaTesterTests.test_bytecode_BC_iload_0_2();
                    break;
                case 116:
                    JavaTesterTests.test_bytecode_BC_iload_1();
                    break;
                case 117:
                    JavaTesterTests.test_bytecode_BC_iload_1_1();
                    break;
                case 118:
                    JavaTesterTests.test_bytecode_BC_iload_2();
                    break;
                case 119:
                    JavaTesterTests.test_bytecode_BC_iload_3();
                    break;
                case 120:
                    JavaTesterTests.test_bytecode_BC_imul();
                    break;
                case 121:
                    JavaTesterTests.test_bytecode_BC_ineg();
                    break;
                case 122:
                    JavaTesterTests.test_bytecode_BC_instanceof();
                    break;
                case 123:
                    JavaTesterTests.test_bytecode_BC_invokeinterface();
                    break;
                case 124:
                    JavaTesterTests.test_bytecode_BC_invokespecial();
                    break;
                case 125:
                    JavaTesterTests.test_bytecode_BC_invokespecial2();
                    break;
                case 126:
                    JavaTesterTests.test_bytecode_BC_invokestatic();
                    break;
                case 127:
                    JavaTesterTests.test_bytecode_BC_invokevirtual();
                    break;
                case 128:
                    JavaTesterTests.test_bytecode_BC_ior();
                    break;
                case 129:
                    JavaTesterTests.test_bytecode_BC_irem();
                    break;
                case 130:
                    JavaTesterTests.test_bytecode_BC_ireturn();
                    break;
                case 131:
                    JavaTesterTests.test_bytecode_BC_ishl();
                    break;
                case 132:
                    JavaTesterTests.test_bytecode_BC_ishr();
                    break;
                case 133:
                    JavaTesterTests.test_bytecode_BC_isub();
                    break;
                case 134:
                    JavaTesterTests.test_bytecode_BC_iushr();
                    break;
                case 135:
                    JavaTesterTests.test_bytecode_BC_ixor();
                    break;
                case 136:
                    JavaTesterTests.test_bytecode_BC_l2d();
                    break;
                case 137:
                    JavaTesterTests.test_bytecode_BC_l2f();
                    break;
                case 138:
                    JavaTesterTests.test_bytecode_BC_l2i();
                    break;
                case 139:
                    JavaTesterTests.test_bytecode_BC_ladd();
                    break;
                case 140:
                    JavaTesterTests.test_bytecode_BC_ladd2();
                    break;
                case 141:
                    JavaTesterTests.test_bytecode_BC_laload();
                    break;
                case 142:
                    JavaTesterTests.test_bytecode_BC_land();
                    break;
                case 143:
                    JavaTesterTests.test_bytecode_BC_lastore();
                    break;
                case 144:
                    JavaTesterTests.test_bytecode_BC_lcmp();
                    break;
                case 145:
                    JavaTesterTests.test_bytecode_BC_ldc_01();
                    break;
                case 146:
                    JavaTesterTests.test_bytecode_BC_ldc_02();
                    break;
                case 147:
                    JavaTesterTests.test_bytecode_BC_ldc_03();
                    break;
                case 148:
                    JavaTesterTests.test_bytecode_BC_ldc_04();
                    break;
                case 149:
                    JavaTesterTests.test_bytecode_BC_ldc_05();
                    break;
                case 150:
                    JavaTesterTests.test_bytecode_BC_ldc_06();
                    break;
                case 151:
                    JavaTesterTests.test_bytecode_BC_ldiv();
                    break;
                case 152:
                    JavaTesterTests.test_bytecode_BC_lload_0();
                    break;
                case 153:
                    JavaTesterTests.test_bytecode_BC_lload_01();
                    break;
                case 154:
                    JavaTesterTests.test_bytecode_BC_lload_1();
                    break;
                case 155:
                    JavaTesterTests.test_bytecode_BC_lload_2();
                    break;
                case 156:
                    JavaTesterTests.test_bytecode_BC_lload_3();
                    break;
                case 157:
                    JavaTesterTests.test_bytecode_BC_lmul();
                    break;
                case 158:
                    JavaTesterTests.test_bytecode_BC_lneg();
                    break;
                case 159:
                    JavaTesterTests.test_bytecode_BC_lookupswitch01();
                    break;
                case 160:
                    JavaTesterTests.test_bytecode_BC_lookupswitch02();
                    break;
                case 161:
                    JavaTesterTests.test_bytecode_BC_lookupswitch03();
                    break;
                case 162:
                    JavaTesterTests.test_bytecode_BC_lookupswitch04();
                    break;
                case 163:
                    JavaTesterTests.test_bytecode_BC_lor();
                    break;
                case 164:
                    JavaTesterTests.test_bytecode_BC_lrem();
                    break;
                case 165:
                    JavaTesterTests.test_bytecode_BC_lreturn();
                    break;
                case 166:
                    JavaTesterTests.test_bytecode_BC_lshl();
                    break;
                case 167:
                    JavaTesterTests.test_bytecode_BC_lshr();
                    break;
                case 168:
                    JavaTesterTests.test_bytecode_BC_lsub();
                    break;
                case 169:
                    JavaTesterTests.test_bytecode_BC_lushr();
                    break;
                case 170:
                    JavaTesterTests.test_bytecode_BC_lxor();
                    break;
                case 171:
                    JavaTesterTests.test_bytecode_BC_monitorenter();
                    break;
                case 172:
                    JavaTesterTests.test_bytecode_BC_multianewarray();
                    break;
                case 173:
                    JavaTesterTests.test_bytecode_BC_new();
                    break;
                case 174:
                    JavaTesterTests.test_bytecode_BC_newarray();
                    break;
                case 175:
                    JavaTesterTests.test_bytecode_BC_putfield();
                    break;
                case 176:
                    JavaTesterTests.test_bytecode_BC_putstatic();
                    break;
                case 177:
                    JavaTesterTests.test_bytecode_BC_saload();
                    break;
                case 178:
                    JavaTesterTests.test_bytecode_BC_sastore();
                    break;
                case 179:
                    JavaTesterTests.test_bytecode_BC_tableswitch();
                    break;
                case 180:
                    JavaTesterTests.test_bytecode_BC_tableswitch2();
                    break;
                case 181:
                    JavaTesterTests.test_bytecode_BC_tableswitch3();
                    break;
                case 182:
                    JavaTesterTests.test_bytecode_BC_tableswitch4();
                    break;
                case 183:
                    JavaTesterTests.test_bytecode_BC_wide01();
                    break;
                case 184:
                    JavaTesterTests.test_bytecode_BC_wide02();
            }
        }
        reportPassed(passed, total);
    }
// END GENERATED TEST RUNS
}
