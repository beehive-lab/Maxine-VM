package test.arm;

import junit.framework.*;

import com.sun.max.ide.*;

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
