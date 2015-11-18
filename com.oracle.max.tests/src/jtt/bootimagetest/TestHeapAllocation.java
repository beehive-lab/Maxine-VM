package jtt.bootimagetest;

import java.util.concurrent.atomic.*;
/*
 * @Harness: java
 * @Runs: 1 = true; 0 = true
 */
public class TestHeapAllocation {

    public TestHeapAllocation() {
    }

    public static boolean test(int a) {
        AtomicLong bb = new AtomicLong(0);
        bb.compareAndSet(0, 1);
        AtomicLong aa = new AtomicLong(1);
        aa.compareAndSet(1, 2);
        return true;
    }
}
