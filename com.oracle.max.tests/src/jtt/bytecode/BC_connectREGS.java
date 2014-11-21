package jtt.bytecode;

/**
 * Created by andyn on 17/11/14.
 */
public class BC_connectREGS {
    public static long test(long a) {
        return a;
    }
    public static boolean le(long a, long b) {
        boolean value = false;
        value =  a <=b;
        return value;
    }
    public static boolean ge(long a, long b) { return a >=b;}
    public static boolean eq(long a, long b) { return a ==b;}
    public static boolean ne(long a, long b) { return a !=b;}
    public static boolean lt(long a, long b) { return a <b;}
    public static boolean gt(long a, long b) { return a >b;}


}
