package jtt.bootimagetest;

/*
 * @Harness: java
 * @Runs: 1 = true;
 */
public class TestHeapAllocation {


    public TestHeapAllocation() {
    }

    public static boolean test(int a) {
	System.out.println("TestHeapAllocation.test ENTER");
        TestHeapAllocation  tmp = new TestHeapAllocation();
	System.out.println("TestHeapAllocation.test DONEALLOC");
	return tmp != null;
   }
}
