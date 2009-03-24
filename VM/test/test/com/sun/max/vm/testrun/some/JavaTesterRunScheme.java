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
        test.threads.Thread_yield01.class
    };
    @Override
    public void runTests() {
        _total = _testEnd - _testStart;
        _testNum = _testStart;
        while (_testNum < _testEnd) {
            switch(_testNum) {
                case 0:
                    JavaTesterTests.test_threads_Monitor_contended01();
                    break;
                case 1:
                    JavaTesterTests.test_threads_Monitor_notowner01();
                    break;
                case 2:
                    JavaTesterTests.test_threads_Monitorenter01();
                    break;
                case 3:
                    JavaTesterTests.test_threads_Monitorenter02();
                    break;
                case 4:
                    JavaTesterTests.test_threads_Object_wait01();
                    break;
                case 5:
                    JavaTesterTests.test_threads_Object_wait02();
                    break;
                case 6:
                    JavaTesterTests.test_threads_Object_wait03();
                    break;
                case 7:
                    JavaTesterTests.test_threads_Object_wait04();
                    break;
                case 8:
                    JavaTesterTests.test_threads_Thread_currentThread01();
                    break;
                case 9:
                    JavaTesterTests.test_threads_Thread_getState01();
                    break;
                case 10:
                    JavaTesterTests.test_threads_Thread_getState02();
                    break;
                case 11:
                    JavaTesterTests.test_threads_Thread_holdsLock01();
                    break;
                case 12:
                    JavaTesterTests.test_threads_Thread_isAlive01();
                    break;
                case 13:
                    JavaTesterTests.test_threads_Thread_isInterrupted01();
                    break;
                case 14:
                    JavaTesterTests.test_threads_Thread_isInterrupted02();
                    break;
                case 15:
                    JavaTesterTests.test_threads_Thread_isInterrupted03();
                    break;
                case 16:
                    JavaTesterTests.test_threads_Thread_isInterrupted04();
                    break;
                case 17:
                    JavaTesterTests.test_threads_Thread_join01();
                    break;
                case 18:
                    JavaTesterTests.test_threads_Thread_join02();
                    break;
                case 19:
                    JavaTesterTests.test_threads_Thread_join03();
                    break;
                case 20:
                    JavaTesterTests.test_threads_Thread_new01();
                    break;
                case 21:
                    JavaTesterTests.test_threads_Thread_new02();
                    break;
                case 22:
                    JavaTesterTests.test_threads_Thread_setPriority01();
                    break;
                case 23:
                    JavaTesterTests.test_threads_Thread_sleep01();
                    break;
                case 24:
                    JavaTesterTests.test_threads_Thread_start01();
                    break;
                case 25:
                    JavaTesterTests.test_threads_Thread_yield01();
            }
        }
        reportPassed(_passed, _total);
    }
// END GENERATED TEST RUNS
}
