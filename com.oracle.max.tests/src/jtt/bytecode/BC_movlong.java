package jtt.bytecode;

/**
 * Created by andyn on 18/11/14.
 */
public class BC_movlong {
    public static long test(int i) {
/* must match with the JTT array or this is garbage!!!
        long [] values= new long [10];
        values[0] = 0L;
        values[1] = -1L;
        values[2] = (long)Integer.MIN_VALUE;
        values[3] = (long)Integer.MAX_VALUE;
        values[4] = Long.MAX_VALUE;
        values[5] = Long.MIN_VALUE;
        values[6] = Long.MIN_VALUE + 5;
        values[7] = Long.MAX_VALUE -5;
        values[8] = ((long)Integer.MIN_VALUE) +5L;
        values[9] = ((long)Integer.MAX_VALUE) -5L;
*/
        long tmp = 0;
        if(i == 0 ) tmp = 0L;
        if(i == 1) tmp = -1L;
        if(i ==2) tmp = (long)Integer.MIN_VALUE;
        if(i == 3) tmp = (long)Integer.MAX_VALUE;
        if( i == 4) tmp = Long.MAX_VALUE;
        if(i == 5) tmp = Long.MIN_VALUE;
        if (i ==6) tmp  = Long.MIN_VALUE + 5L;
        if( i == 7) tmp = Long.MAX_VALUE -5L;
        if(i == 8) tmp = ((long)Integer.MIN_VALUE) +5L;
        if(i ==9) tmp = ((long)Integer.MAX_VALUE) -5L;
        return tmp;
    }
}
