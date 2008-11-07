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
        test.fail.BC_invokespecial01.class,
        test.fail.BC_invokevirtual02.class,
        test.lang.ClassLoader_loadClass01.class,
        test.lang.Class_forName03.class,
        test.lang.Class_forName05.class,
        test.fail.HP_StringFormat01.class
    };
    @Override
    public void run() {
        _testEnd = 6;
        vmStartUp();
        _total = _testEnd - _testStart;
        _testNum = _testStart;
        while (_testNum < _testEnd) {
            switch(_testNum) {
                case 0:
                    JavaTesterTests.test_fail_BC_invokespecial01();
                    break;
                case 1:
                    JavaTesterTests.test_fail_BC_invokevirtual02();
                    break;
                case 2:
                    JavaTesterTests.test_fail_ClassLoader_loadClass01();
                    break;
                case 3:
                    JavaTesterTests.test_fail_Class_forName03();
                    break;
                case 4:
                    JavaTesterTests.test_fail_Class_forName05();
                    break;
                case 5:
                    JavaTesterTests.test_fail_HP_StringFormat01();
            }
        }
        reportPassed(_passed, _total);
    }
// END GENERATED TEST RUNS
}
