package jtt.bootimagetest;

/*
 * @Harness: java
 * @Runs: 1 = true; 0 = false
 */
public class TestHeapAllocation {


    public TestHeapAllocation() {
    }

    public static boolean test(int a) {

	System.out.println("TestHeapAllocation.test ENTER");
        TestHeapAllocation  tmp = new TestHeapAllocation();

	if(a == 0) tmp = null;

	return tmp != null  ;
   }
}
