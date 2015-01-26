package jtt.bytecode;

/**
 * Created by andyn on 26/01/15.
 */
public class BC_dNAN {
    public static boolean test(double a) {
        if (a != a) return true;
        else return false;
    }
}
