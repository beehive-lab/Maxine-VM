package test.arm;

import com.sun.max.ide.TestCaseClassSet;
import junit.framework.Test;
import junit.framework.TestSuite;
import test.com.sun.max.vm.AllTests;
import test.com.sun.max.vm.VmTestSetup;

/**
 * Created with IntelliJ IDEA.
 * User: yaman
 * Date: 12/12/13
 * Time: 15:20
 * To change this template use File | Settings | File Templates.
 */

@org.junit.runner.RunWith(org.junit.runners.AllTests.class)
public final class AutoTest {
    private AutoTest() {
    }

    public static void main(String[] args) throws Exception {
        junit.textui.TestRunner.run(AutoTest.suite());
    }

    public static Test suite() throws Exception {
        return new TestCaseClassSet(AutoTest.class).toTestSuite();
    }

}
