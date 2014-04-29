package test.arm.t1x;

import com.sun.max.ide.TestCaseClassSet;
import junit.framework.Test;
import junit.framework.TestSuite;
import test.com.sun.max.vm.AllTests;

@org.junit.runner.RunWith(org.junit.runners.AllTests.class)
public final class AutoTest {
    private AutoTest() {
    }

    public static void main(String[] args) throws Exception {
        junit.textui.TestRunner.run(AutoTest.suite());
    }

    public static Test suite() throws Exception {
        final TestSuite suite = new TestCaseClassSet(AllTests.class).toTestSuite();
        suite.addTest(test.arm.t1x.AllTests.suite());
        return suite;
    }

}
