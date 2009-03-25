package test.lang;

/*
 * @Harness: java
 * @Runs: 2d=4.0d; 3.1d=8.574187700290345d
 */
public class Math_pow {
    public static double test(double pow) {
        return Math.pow(2.0d, pow);
    }
}
