package jtt.loop;

/**
 * Created by andyn on 11/12/14.
 */
public class LoopMethods {
    public static int testme2(int c) { return c*2;}
    public static int testme(int b) {
        return 2*testme2(b);
    }
    public static int test(int a) {
        return testme(a);
    }
}
