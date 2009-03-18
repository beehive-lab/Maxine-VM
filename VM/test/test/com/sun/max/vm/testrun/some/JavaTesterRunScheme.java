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
        return _classList;
    }

    @PROTOTYPE_ONLY
// GENERATED TEST RUNS
    private static final Class<?>[] _classList = {
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
        test.except.Catch_OutOfMemory01.class,
        test.except.Catch_StackOverflowError_01.class,
        test.except.Catch_StackOverflowError_02.class,
        test.except.Throw_InCatch01.class,
        test.except.Throw_InCatch02.class,
        test.except.Throw_InCatch03.class,
        test.except.Throw_Synchronized01.class,
        test.except.Throw_Synchronized02.class,
        test.except.Throw_Synchronized03.class,
        test.except.Throw_Synchronized04.class
    };
    @Override
    public void runTests() {
        _total = _testEnd - _testStart;
        _testNum = _testStart;
        while (_testNum < _testEnd) {
            switch(_testNum) {
                case 0:
                    JavaTesterTests.test_except_BC_aaload();
                    break;
                case 1:
                    JavaTesterTests.test_except_BC_aastore();
                    break;
                case 2:
                    JavaTesterTests.test_except_BC_anewarray();
                    break;
                case 3:
                    JavaTesterTests.test_except_BC_arraylength();
                    break;
                case 4:
                    JavaTesterTests.test_except_BC_athrow();
                    break;
                case 5:
                    JavaTesterTests.test_except_BC_athrow1();
                    break;
                case 6:
                    JavaTesterTests.test_except_BC_athrow2();
                    break;
                case 7:
                    JavaTesterTests.test_except_BC_athrow3();
                    break;
                case 8:
                    JavaTesterTests.test_except_BC_baload();
                    break;
                case 9:
                    JavaTesterTests.test_except_BC_bastore();
                    break;
                case 10:
                    JavaTesterTests.test_except_BC_caload();
                    break;
                case 11:
                    JavaTesterTests.test_except_BC_castore();
                    break;
                case 12:
                    JavaTesterTests.test_except_BC_checkcast();
                    break;
                case 13:
                    JavaTesterTests.test_except_BC_checkcast1();
                    break;
                case 14:
                    JavaTesterTests.test_except_BC_checkcast2();
                    break;
                case 15:
                    JavaTesterTests.test_except_BC_daload();
                    break;
                case 16:
                    JavaTesterTests.test_except_BC_dastore();
                    break;
                case 17:
                    JavaTesterTests.test_except_BC_faload();
                    break;
                case 18:
                    JavaTesterTests.test_except_BC_fastore();
                    break;
                case 19:
                    JavaTesterTests.test_except_BC_getfield();
                    break;
                case 20:
                    JavaTesterTests.test_except_BC_iaload();
                    break;
                case 21:
                    JavaTesterTests.test_except_BC_iastore();
                    break;
                case 22:
                    JavaTesterTests.test_except_BC_idiv();
                    break;
                case 23:
                    JavaTesterTests.test_except_BC_invokevirtual01();
                    break;
                case 24:
                    JavaTesterTests.test_except_BC_irem();
                    break;
                case 25:
                    JavaTesterTests.test_except_BC_laload();
                    break;
                case 26:
                    JavaTesterTests.test_except_BC_lastore();
                    break;
                case 27:
                    JavaTesterTests.test_except_BC_ldiv();
                    break;
                case 28:
                    JavaTesterTests.test_except_BC_lrem();
                    break;
                case 29:
                    JavaTesterTests.test_except_BC_monitorenter();
                    break;
                case 30:
                    JavaTesterTests.test_except_BC_multianewarray();
                    break;
                case 31:
                    JavaTesterTests.test_except_BC_newarray();
                    break;
                case 32:
                    JavaTesterTests.test_except_BC_putfield();
                    break;
                case 33:
                    JavaTesterTests.test_except_BC_saload();
                    break;
                case 34:
                    JavaTesterTests.test_except_BC_sastore();
                    break;
                case 35:
                    JavaTesterTests.test_except_Catch_NPE_01();
                    break;
                case 36:
                    JavaTesterTests.test_except_Catch_NPE_02();
                    break;
                case 37:
                    JavaTesterTests.test_except_Catch_NPE_03();
                    break;
                case 38:
                    JavaTesterTests.test_except_Catch_NPE_04();
                    break;
                case 39:
                    JavaTesterTests.test_except_Catch_NPE_05();
                    break;
                case 40:
                    JavaTesterTests.test_except_Catch_NPE_06();
                    break;
                case 41:
                    JavaTesterTests.test_except_Catch_NPE_07();
                    break;
                case 42:
                    JavaTesterTests.test_except_Catch_OutOfMemory01();
                    break;
                case 43:
                    JavaTesterTests.test_except_Catch_StackOverflowError_01();
                    break;
                case 44:
                    JavaTesterTests.test_except_Catch_StackOverflowError_02();
                    break;
                case 45:
                    JavaTesterTests.test_except_Throw_InCatch01();
                    break;
                case 46:
                    JavaTesterTests.test_except_Throw_InCatch02();
                    break;
                case 47:
                    JavaTesterTests.test_except_Throw_InCatch03();
                    break;
                case 48:
                    JavaTesterTests.test_except_Throw_Synchronized01();
                    break;
                case 49:
                    JavaTesterTests.test_except_Throw_Synchronized02();
                    break;
                case 50:
                    JavaTesterTests.test_except_Throw_Synchronized03();
                    break;
                case 51:
                    JavaTesterTests.test_except_Throw_Synchronized04();
            }
        }
        reportPassed(_passed, _total);
    }
// END GENERATED TEST RUNS
}
