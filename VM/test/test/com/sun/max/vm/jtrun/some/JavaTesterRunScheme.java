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
package test.com.sun.max.vm.jtrun.some;

import test.com.sun.max.vm.jtrun.*;

import com.sun.max.annotate.*;
import com.sun.max.vm.*;
import jtt.bytecode.BC_multianewarray01;

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
        jtt.bytecode.BC_checkcast.class,
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
        BC_multianewarray01.class,
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
        jtt.bytecode.BC_wide02.class
    };
    @Override
    public void runTests() {
        total = testEnd - testStart;
        testNum = testStart;
        while (testNum < testEnd) {
            switch(testNum) {
                case 0:
                    JavaTesterTests.jtt_bytecode_BC_aaload();
                    break;
                case 1:
                    JavaTesterTests.jtt_bytecode_BC_aaload_1();
                    break;
                case 2:
                    JavaTesterTests.jtt_bytecode_BC_aastore();
                    break;
                case 3:
                    JavaTesterTests.jtt_bytecode_BC_aload_0();
                    break;
                case 4:
                    JavaTesterTests.jtt_bytecode_BC_aload_1();
                    break;
                case 5:
                    JavaTesterTests.jtt_bytecode_BC_aload_2();
                    break;
                case 6:
                    JavaTesterTests.jtt_bytecode_BC_aload_3();
                    break;
                case 7:
                    JavaTesterTests.jtt_bytecode_BC_anewarray();
                    break;
                case 8:
                    JavaTesterTests.jtt_bytecode_BC_areturn();
                    break;
                case 9:
                    JavaTesterTests.jtt_bytecode_BC_arraylength();
                    break;
                case 10:
                    JavaTesterTests.jtt_bytecode_BC_athrow();
                    break;
                case 11:
                    JavaTesterTests.jtt_bytecode_BC_baload();
                    break;
                case 12:
                    JavaTesterTests.jtt_bytecode_BC_bastore();
                    break;
                case 13:
                    JavaTesterTests.jtt_bytecode_BC_caload();
                    break;
                case 14:
                    JavaTesterTests.jtt_bytecode_BC_castore();
                    break;
                case 15:
                    JavaTesterTests.jtt_bytecode_BC_checkcast();
                    break;
                case 16:
                    JavaTesterTests.jtt_bytecode_BC_d2f();
                    break;
                case 17:
                    JavaTesterTests.jtt_bytecode_BC_d2i();
                    break;
                case 18:
                    JavaTesterTests.jtt_bytecode_BC_d2i_nan();
                    break;
                case 19:
                    JavaTesterTests.jtt_bytecode_BC_d2l();
                    break;
                case 20:
                    JavaTesterTests.jtt_bytecode_BC_d2l_nan();
                    break;
                case 21:
                    JavaTesterTests.jtt_bytecode_BC_dadd();
                    break;
                case 22:
                    JavaTesterTests.jtt_bytecode_BC_daload();
                    break;
                case 23:
                    JavaTesterTests.jtt_bytecode_BC_dastore();
                    break;
                case 24:
                    JavaTesterTests.jtt_bytecode_BC_dcmp01();
                    break;
                case 25:
                    JavaTesterTests.jtt_bytecode_BC_dcmp02();
                    break;
                case 26:
                    JavaTesterTests.jtt_bytecode_BC_dcmp03();
                    break;
                case 27:
                    JavaTesterTests.jtt_bytecode_BC_dcmp04();
                    break;
                case 28:
                    JavaTesterTests.jtt_bytecode_BC_dcmp05();
                    break;
                case 29:
                    JavaTesterTests.jtt_bytecode_BC_dcmp06();
                    break;
                case 30:
                    JavaTesterTests.jtt_bytecode_BC_dcmp07();
                    break;
                case 31:
                    JavaTesterTests.jtt_bytecode_BC_dcmp08();
                    break;
                case 32:
                    JavaTesterTests.jtt_bytecode_BC_dcmp09();
                    break;
                case 33:
                    JavaTesterTests.jtt_bytecode_BC_dcmp10();
                    break;
                case 34:
                    JavaTesterTests.jtt_bytecode_BC_ddiv();
                    break;
                case 35:
                    JavaTesterTests.jtt_bytecode_BC_dmul();
                    break;
                case 36:
                    JavaTesterTests.jtt_bytecode_BC_dneg();
                    break;
                case 37:
                    JavaTesterTests.jtt_bytecode_BC_drem();
                    break;
                case 38:
                    JavaTesterTests.jtt_bytecode_BC_dreturn();
                    break;
                case 39:
                    JavaTesterTests.jtt_bytecode_BC_dsub();
                    break;
                case 40:
                    JavaTesterTests.jtt_bytecode_BC_f2d();
                    break;
                case 41:
                    JavaTesterTests.jtt_bytecode_BC_f2i();
                    break;
                case 42:
                    JavaTesterTests.jtt_bytecode_BC_f2i_2();
                    break;
                case 43:
                    JavaTesterTests.jtt_bytecode_BC_f2i_nan();
                    break;
                case 44:
                    JavaTesterTests.jtt_bytecode_BC_f2l();
                    break;
                case 45:
                    JavaTesterTests.jtt_bytecode_BC_f2l_nan();
                    break;
                case 46:
                    JavaTesterTests.jtt_bytecode_BC_fadd();
                    break;
                case 47:
                    JavaTesterTests.jtt_bytecode_BC_faload();
                    break;
                case 48:
                    JavaTesterTests.jtt_bytecode_BC_fastore();
                    break;
                case 49:
                    JavaTesterTests.jtt_bytecode_BC_fcmp01();
                    break;
                case 50:
                    JavaTesterTests.jtt_bytecode_BC_fcmp02();
                    break;
                case 51:
                    JavaTesterTests.jtt_bytecode_BC_fcmp03();
                    break;
                case 52:
                    JavaTesterTests.jtt_bytecode_BC_fcmp04();
                    break;
                case 53:
                    JavaTesterTests.jtt_bytecode_BC_fcmp05();
                    break;
                case 54:
                    JavaTesterTests.jtt_bytecode_BC_fcmp06();
                    break;
                case 55:
                    JavaTesterTests.jtt_bytecode_BC_fcmp07();
                    break;
                case 56:
                    JavaTesterTests.jtt_bytecode_BC_fcmp08();
                    break;
                case 57:
                    JavaTesterTests.jtt_bytecode_BC_fcmp09();
                    break;
                case 58:
                    JavaTesterTests.jtt_bytecode_BC_fcmp10();
                    break;
                case 59:
                    JavaTesterTests.jtt_bytecode_BC_fdiv();
                    break;
                case 60:
                    JavaTesterTests.jtt_bytecode_BC_fload();
                    break;
                case 61:
                    JavaTesterTests.jtt_bytecode_BC_fload_2();
                    break;
                case 62:
                    JavaTesterTests.jtt_bytecode_BC_fmul();
                    break;
                case 63:
                    JavaTesterTests.jtt_bytecode_BC_fneg();
                    break;
                case 64:
                    JavaTesterTests.jtt_bytecode_BC_frem();
                    break;
                case 65:
                    JavaTesterTests.jtt_bytecode_BC_freturn();
                    break;
                case 66:
                    JavaTesterTests.jtt_bytecode_BC_fsub();
                    break;
                case 67:
                    JavaTesterTests.jtt_bytecode_BC_getfield();
                    break;
                case 68:
                    JavaTesterTests.jtt_bytecode_BC_getstatic_b();
                    break;
                case 69:
                    JavaTesterTests.jtt_bytecode_BC_getstatic_c();
                    break;
                case 70:
                    JavaTesterTests.jtt_bytecode_BC_getstatic_d();
                    break;
                case 71:
                    JavaTesterTests.jtt_bytecode_BC_getstatic_f();
                    break;
                case 72:
                    JavaTesterTests.jtt_bytecode_BC_getstatic_i();
                    break;
                case 73:
                    JavaTesterTests.jtt_bytecode_BC_getstatic_l();
                    break;
                case 74:
                    JavaTesterTests.jtt_bytecode_BC_getstatic_s();
                    break;
                case 75:
                    JavaTesterTests.jtt_bytecode_BC_getstatic_z();
                    break;
                case 76:
                    JavaTesterTests.jtt_bytecode_BC_i2b();
                    break;
                case 77:
                    JavaTesterTests.jtt_bytecode_BC_i2c();
                    break;
                case 78:
                    JavaTesterTests.jtt_bytecode_BC_i2d();
                    break;
                case 79:
                    JavaTesterTests.jtt_bytecode_BC_i2f();
                    break;
                case 80:
                    JavaTesterTests.jtt_bytecode_BC_i2l();
                    break;
                case 81:
                    JavaTesterTests.jtt_bytecode_BC_i2s();
                    break;
                case 82:
                    JavaTesterTests.jtt_bytecode_BC_iadd();
                    break;
                case 83:
                    JavaTesterTests.jtt_bytecode_BC_iadd2();
                    break;
                case 84:
                    JavaTesterTests.jtt_bytecode_BC_iadd3();
                    break;
                case 85:
                    JavaTesterTests.jtt_bytecode_BC_iaload();
                    break;
                case 86:
                    JavaTesterTests.jtt_bytecode_BC_iand();
                    break;
                case 87:
                    JavaTesterTests.jtt_bytecode_BC_iastore();
                    break;
                case 88:
                    JavaTesterTests.jtt_bytecode_BC_iconst();
                    break;
                case 89:
                    JavaTesterTests.jtt_bytecode_BC_idiv();
                    break;
                case 90:
                    JavaTesterTests.jtt_bytecode_BC_ifeq();
                    break;
                case 91:
                    JavaTesterTests.jtt_bytecode_BC_ifeq_2();
                    break;
                case 92:
                    JavaTesterTests.jtt_bytecode_BC_ifeq_3();
                    break;
                case 93:
                    JavaTesterTests.jtt_bytecode_BC_ifge();
                    break;
                case 94:
                    JavaTesterTests.jtt_bytecode_BC_ifge_2();
                    break;
                case 95:
                    JavaTesterTests.jtt_bytecode_BC_ifge_3();
                    break;
                case 96:
                    JavaTesterTests.jtt_bytecode_BC_ifgt();
                    break;
                case 97:
                    JavaTesterTests.jtt_bytecode_BC_ificmplt1();
                    break;
                case 98:
                    JavaTesterTests.jtt_bytecode_BC_ificmplt2();
                    break;
                case 99:
                    JavaTesterTests.jtt_bytecode_BC_ificmpne1();
                    break;
                case 100:
                    JavaTesterTests.jtt_bytecode_BC_ificmpne2();
                    break;
                case 101:
                    JavaTesterTests.jtt_bytecode_BC_ifle();
                    break;
                case 102:
                    JavaTesterTests.jtt_bytecode_BC_iflt();
                    break;
                case 103:
                    JavaTesterTests.jtt_bytecode_BC_ifne();
                    break;
                case 104:
                    JavaTesterTests.jtt_bytecode_BC_ifnonnull();
                    break;
                case 105:
                    JavaTesterTests.jtt_bytecode_BC_ifnonnull_2();
                    break;
                case 106:
                    JavaTesterTests.jtt_bytecode_BC_ifnonnull_3();
                    break;
                case 107:
                    JavaTesterTests.jtt_bytecode_BC_ifnull();
                    break;
                case 108:
                    JavaTesterTests.jtt_bytecode_BC_ifnull_2();
                    break;
                case 109:
                    JavaTesterTests.jtt_bytecode_BC_ifnull_3();
                    break;
                case 110:
                    JavaTesterTests.jtt_bytecode_BC_iinc_1();
                    break;
                case 111:
                    JavaTesterTests.jtt_bytecode_BC_iinc_2();
                    break;
                case 112:
                    JavaTesterTests.jtt_bytecode_BC_iinc_3();
                    break;
                case 113:
                    JavaTesterTests.jtt_bytecode_BC_iinc_4();
                    break;
                case 114:
                    JavaTesterTests.jtt_bytecode_BC_iload_0();
                    break;
                case 115:
                    JavaTesterTests.jtt_bytecode_BC_iload_0_1();
                    break;
                case 116:
                    JavaTesterTests.jtt_bytecode_BC_iload_0_2();
                    break;
                case 117:
                    JavaTesterTests.jtt_bytecode_BC_iload_1();
                    break;
                case 118:
                    JavaTesterTests.jtt_bytecode_BC_iload_1_1();
                    break;
                case 119:
                    JavaTesterTests.jtt_bytecode_BC_iload_2();
                    break;
                case 120:
                    JavaTesterTests.jtt_bytecode_BC_iload_3();
                    break;
                case 121:
                    JavaTesterTests.jtt_bytecode_BC_imul();
                    break;
                case 122:
                    JavaTesterTests.jtt_bytecode_BC_ineg();
                    break;
                case 123:
                    JavaTesterTests.jtt_bytecode_BC_instanceof();
                    break;
                case 124:
                    JavaTesterTests.jtt_bytecode_BC_invokeinterface();
                    break;
                case 125:
                    JavaTesterTests.jtt_bytecode_BC_invokespecial();
                    break;
                case 126:
                    JavaTesterTests.jtt_bytecode_BC_invokespecial2();
                    break;
                case 127:
                    JavaTesterTests.jtt_bytecode_BC_invokestatic();
                    break;
                case 128:
                    JavaTesterTests.jtt_bytecode_BC_invokevirtual();
                    break;
                case 129:
                    JavaTesterTests.jtt_bytecode_BC_ior();
                    break;
                case 130:
                    JavaTesterTests.jtt_bytecode_BC_irem();
                    break;
                case 131:
                    JavaTesterTests.jtt_bytecode_BC_ireturn();
                    break;
                case 132:
                    JavaTesterTests.jtt_bytecode_BC_ishl();
                    break;
                case 133:
                    JavaTesterTests.jtt_bytecode_BC_ishr();
                    break;
                case 134:
                    JavaTesterTests.jtt_bytecode_BC_isub();
                    break;
                case 135:
                    JavaTesterTests.jtt_bytecode_BC_iushr();
                    break;
                case 136:
                    JavaTesterTests.jtt_bytecode_BC_ixor();
                    break;
                case 137:
                    JavaTesterTests.jtt_bytecode_BC_l2d();
                    break;
                case 138:
                    JavaTesterTests.jtt_bytecode_BC_l2f();
                    break;
                case 139:
                    JavaTesterTests.jtt_bytecode_BC_l2i();
                    break;
                case 140:
                    JavaTesterTests.jtt_bytecode_BC_ladd();
                    break;
                case 141:
                    JavaTesterTests.jtt_bytecode_BC_ladd2();
                    break;
                case 142:
                    JavaTesterTests.jtt_bytecode_BC_laload();
                    break;
                case 143:
                    JavaTesterTests.jtt_bytecode_BC_land();
                    break;
                case 144:
                    JavaTesterTests.jtt_bytecode_BC_lastore();
                    break;
                case 145:
                    JavaTesterTests.jtt_bytecode_BC_lcmp();
                    break;
                case 146:
                    JavaTesterTests.jtt_bytecode_BC_ldc_01();
                    break;
                case 147:
                    JavaTesterTests.jtt_bytecode_BC_ldc_02();
                    break;
                case 148:
                    JavaTesterTests.jtt_bytecode_BC_ldc_03();
                    break;
                case 149:
                    JavaTesterTests.jtt_bytecode_BC_ldc_04();
                    break;
                case 150:
                    JavaTesterTests.jtt_bytecode_BC_ldc_05();
                    break;
                case 151:
                    JavaTesterTests.jtt_bytecode_BC_ldc_06();
                    break;
                case 152:
                    JavaTesterTests.jtt_bytecode_BC_ldiv();
                    break;
                case 153:
                    JavaTesterTests.jtt_bytecode_BC_lload_0();
                    break;
                case 154:
                    JavaTesterTests.jtt_bytecode_BC_lload_01();
                    break;
                case 155:
                    JavaTesterTests.jtt_bytecode_BC_lload_1();
                    break;
                case 156:
                    JavaTesterTests.jtt_bytecode_BC_lload_2();
                    break;
                case 157:
                    JavaTesterTests.jtt_bytecode_BC_lload_3();
                    break;
                case 158:
                    JavaTesterTests.jtt_bytecode_BC_lmul();
                    break;
                case 159:
                    JavaTesterTests.jtt_bytecode_BC_lneg();
                    break;
                case 160:
                    JavaTesterTests.jtt_bytecode_BC_lookupswitch01();
                    break;
                case 161:
                    JavaTesterTests.jtt_bytecode_BC_lookupswitch02();
                    break;
                case 162:
                    JavaTesterTests.jtt_bytecode_BC_lookupswitch03();
                    break;
                case 163:
                    JavaTesterTests.jtt_bytecode_BC_lookupswitch04();
                    break;
                case 164:
                    JavaTesterTests.jtt_bytecode_BC_lor();
                    break;
                case 165:
                    JavaTesterTests.jtt_bytecode_BC_lrem();
                    break;
                case 166:
                    JavaTesterTests.jtt_bytecode_BC_lreturn();
                    break;
                case 167:
                    JavaTesterTests.jtt_bytecode_BC_lshl();
                    break;
                case 168:
                    JavaTesterTests.jtt_bytecode_BC_lshr();
                    break;
                case 169:
                    JavaTesterTests.jtt_bytecode_BC_lsub();
                    break;
                case 170:
                    JavaTesterTests.jtt_bytecode_BC_lushr();
                    break;
                case 171:
                    JavaTesterTests.jtt_bytecode_BC_lxor();
                    break;
                case 172:
                    JavaTesterTests.jtt_bytecode_BC_monitorenter();
                    break;
                case 173:
                    JavaTesterTests.jtt_bytecode_BC_multianewarray();
                    break;
                case 174:
                    JavaTesterTests.jtt_bytecode_BC_new();
                    break;
                case 175:
                    JavaTesterTests.jtt_bytecode_BC_newarray();
                    break;
                case 176:
                    JavaTesterTests.jtt_bytecode_BC_putfield();
                    break;
                case 177:
                    JavaTesterTests.jtt_bytecode_BC_putstatic();
                    break;
                case 178:
                    JavaTesterTests.jtt_bytecode_BC_saload();
                    break;
                case 179:
                    JavaTesterTests.jtt_bytecode_BC_sastore();
                    break;
                case 180:
                    JavaTesterTests.jtt_bytecode_BC_tableswitch();
                    break;
                case 181:
                    JavaTesterTests.jtt_bytecode_BC_tableswitch2();
                    break;
                case 182:
                    JavaTesterTests.jtt_bytecode_BC_tableswitch3();
                    break;
                case 183:
                    JavaTesterTests.jtt_bytecode_BC_tableswitch4();
                    break;
                case 184:
                    JavaTesterTests.jtt_bytecode_BC_wide01();
                    break;
                case 185:
                    JavaTesterTests.jtt_bytecode_BC_wide02();
            }
        }
        reportPassed(passed, total);
    }
// END GENERATED TEST RUNS
}
