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
package test.com.sun.max.vm.jtrun.c1x;

import com.sun.max.annotate.*;
import com.sun.max.vm.*;

import test.com.sun.max.vm.jtrun.*;


public class JavaTesterRunScheme extends AbstractTester {

    public JavaTesterRunScheme(VMConfiguration vmConfiguration) {
        super(vmConfiguration);
    }

    @Override
    @PROTOTYPE_ONLY
    public Class<?>[] getClassList() {
        return classList;
    }

// GENERATED TEST RUNS
    private static final Class<?>[] classList = {
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
        jtt.except.BC_daload.class,
        jtt.except.BC_dastore.class,
        jtt.except.BC_faload.class,
        jtt.except.BC_fastore.class,
        jtt.except.BC_getfield.class,
        jtt.except.BC_iaload.class,
        jtt.except.BC_iastore.class,
        jtt.except.BC_idiv.class,
        jtt.except.BC_invokevirtual01.class,
        jtt.except.BC_irem.class,
        jtt.except.BC_laload.class,
        jtt.except.BC_lastore.class,
        jtt.except.BC_ldiv.class,
        jtt.except.BC_lrem.class,
        jtt.except.BC_monitorenter.class,
        jtt.except.BC_multianewarray.class,
        jtt.except.BC_newarray.class,
        jtt.except.BC_putfield.class,
        jtt.except.BC_saload.class,
        jtt.except.BC_sastore.class,
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
        jtt.except.Throw_Synchronized05.class
    };
    @Override
    public void runTests() {
        total = testEnd - testStart;
        testNum = testStart;
        while (testNum < testEnd) {
            switch(testNum) {
                case 0:
                    JavaTesterTests.jtt_except_BC_aaload();
                    break;
                case 1:
                    JavaTesterTests.jtt_except_BC_aastore();
                    break;
                case 2:
                    JavaTesterTests.jtt_except_BC_anewarray();
                    break;
                case 3:
                    JavaTesterTests.jtt_except_BC_arraylength();
                    break;
                case 4:
                    JavaTesterTests.jtt_except_BC_athrow();
                    break;
                case 5:
                    JavaTesterTests.jtt_except_BC_athrow1();
                    break;
                case 6:
                    JavaTesterTests.jtt_except_BC_athrow2();
                    break;
                case 7:
                    JavaTesterTests.jtt_except_BC_athrow3();
                    break;
                case 8:
                    JavaTesterTests.jtt_except_BC_baload();
                    break;
                case 9:
                    JavaTesterTests.jtt_except_BC_bastore();
                    break;
                case 10:
                    JavaTesterTests.jtt_except_BC_caload();
                    break;
                case 11:
                    JavaTesterTests.jtt_except_BC_castore();
                    break;
                case 12:
                    JavaTesterTests.jtt_except_BC_checkcast();
                    break;
                case 13:
                    JavaTesterTests.jtt_except_BC_checkcast1();
                    break;
                case 14:
                    JavaTesterTests.jtt_except_BC_checkcast2();
                    break;
                case 15:
                    JavaTesterTests.jtt_except_BC_daload();
                    break;
                case 16:
                    JavaTesterTests.jtt_except_BC_dastore();
                    break;
                case 17:
                    JavaTesterTests.jtt_except_BC_faload();
                    break;
                case 18:
                    JavaTesterTests.jtt_except_BC_fastore();
                    break;
                case 19:
                    JavaTesterTests.jtt_except_BC_getfield();
                    break;
                case 20:
                    JavaTesterTests.jtt_except_BC_iaload();
                    break;
                case 21:
                    JavaTesterTests.jtt_except_BC_iastore();
                    break;
                case 22:
                    JavaTesterTests.jtt_except_BC_idiv();
                    break;
                case 23:
                    JavaTesterTests.jtt_except_BC_invokevirtual01();
                    break;
                case 24:
                    JavaTesterTests.jtt_except_BC_irem();
                    break;
                case 25:
                    JavaTesterTests.jtt_except_BC_laload();
                    break;
                case 26:
                    JavaTesterTests.jtt_except_BC_lastore();
                    break;
                case 27:
                    JavaTesterTests.jtt_except_BC_ldiv();
                    break;
                case 28:
                    JavaTesterTests.jtt_except_BC_lrem();
                    break;
                case 29:
                    JavaTesterTests.jtt_except_BC_monitorenter();
                    break;
                case 30:
                    JavaTesterTests.jtt_except_BC_multianewarray();
                    break;
                case 31:
                    JavaTesterTests.jtt_except_BC_newarray();
                    break;
                case 32:
                    JavaTesterTests.jtt_except_BC_putfield();
                    break;
                case 33:
                    JavaTesterTests.jtt_except_BC_saload();
                    break;
                case 34:
                    JavaTesterTests.jtt_except_BC_sastore();
                    break;
                case 35:
                    JavaTesterTests.jtt_except_Catch_NPE_01();
                    break;
                case 36:
                    JavaTesterTests.jtt_except_Catch_NPE_02();
                    break;
                case 37:
                    JavaTesterTests.jtt_except_Catch_NPE_03();
                    break;
                case 38:
                    JavaTesterTests.jtt_except_Catch_NPE_04();
                    break;
                case 39:
                    JavaTesterTests.jtt_except_Catch_NPE_05();
                    break;
                case 40:
                    JavaTesterTests.jtt_except_Catch_NPE_06();
                    break;
                case 41:
                    JavaTesterTests.jtt_except_Catch_NPE_07();
                    break;
                case 42:
                    JavaTesterTests.jtt_except_Catch_NPE_08();
                    break;
                case 43:
                    JavaTesterTests.jtt_except_Catch_StackOverflowError_01();
                    break;
                case 44:
                    JavaTesterTests.jtt_except_Catch_StackOverflowError_02();
                    break;
                case 45:
                    JavaTesterTests.jtt_except_Catch_StackOverflowError_03();
                    break;
                case 46:
                    JavaTesterTests.jtt_except_Except_Synchronized01();
                    break;
                case 47:
                    JavaTesterTests.jtt_except_Except_Synchronized02();
                    break;
                case 48:
                    JavaTesterTests.jtt_except_Except_Synchronized03();
                    break;
                case 49:
                    JavaTesterTests.jtt_except_Except_Synchronized04();
                    break;
                case 50:
                    JavaTesterTests.jtt_except_Throw_InCatch01();
                    break;
                case 51:
                    JavaTesterTests.jtt_except_Throw_InCatch02();
                    break;
                case 52:
                    JavaTesterTests.jtt_except_Throw_InCatch03();
                    break;
                case 53:
                    JavaTesterTests.jtt_except_Throw_NPE_01();
                    break;
                case 54:
                    JavaTesterTests.jtt_except_Throw_Synchronized01();
                    break;
                case 55:
                    JavaTesterTests.jtt_except_Throw_Synchronized02();
                    break;
                case 56:
                    JavaTesterTests.jtt_except_Throw_Synchronized03();
                    break;
                case 57:
                    JavaTesterTests.jtt_except_Throw_Synchronized04();
                    break;
                case 58:
                    JavaTesterTests.jtt_except_Throw_Synchronized05();
            }
        }
        reportPassed(passed, total);
    }
// END GENERATED TEST RUNS
}
