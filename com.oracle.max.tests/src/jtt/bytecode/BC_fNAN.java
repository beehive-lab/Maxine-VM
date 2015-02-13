package jtt.bytecode;

/**
 * Created by andyn on 26/01/15.
 */
public class BC_fNAN {
    public static boolean test(float a) {
        if (a != a) return true;
        else return false;
    }
}
