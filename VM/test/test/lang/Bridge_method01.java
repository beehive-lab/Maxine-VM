package test.lang;

/*
 * @Harness: java
 * @Runs: 0=1; 1=1
 */
public class Bridge_method01 {
    private static abstract class Wrap<T> {
        abstract T get();
    }
    private static class IWrap extends Wrap<Integer> {
        @Override
        Integer get() {
            return 1;
        }
    }

    private static Wrap<Integer> _wrapped = new IWrap();
    public static int test(int arg) {
        return _wrapped.get();
    }
}
