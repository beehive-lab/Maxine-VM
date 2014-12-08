package jtt.bytecode;

/**
 * Created by andyn on 06/12/14.
 */
public class BC_charComp {
    public static boolean test(int test, char a, char b) {
        if(test == 0) {
            return a == b;
        } else if (test == -1) {
            return a < b;

        } else if (test < -1) {
            return a <= b;
        } else if (test ==1) {
            return a > b;
        } else if (test == 2) {
            return a >= b;
        } else {
            return a !=b;
        }
    }
}
