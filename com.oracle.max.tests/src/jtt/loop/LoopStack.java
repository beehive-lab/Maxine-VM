package jtt.loop;

/**
 * Created by andyn on 28/11/14.
 */
public class LoopStack {
 // idea call this with 0 and it should trigger the stack issue!

        public static int test(int arg) {
            if(arg != 0)
            return test(arg, arg, arg, arg, arg, arg);
            else return -1;
        }

        public static int test(int i1, int i2, int i3, int i4, int i5, int i6) {
            if (i1 == 0) {
                i1 = 2;
            } else {
                i2 = 2;
            }
            for (int i = 0; i < 10; i++) {
                if (i == 0) {
                    i3 = 2;
                } else {
                    i4 = 2;
                }

           for (int j = 0; j < 10; j++) {
                if (j == 0) {
                    i5 = 2;
                } else {
                    i6 = 2;
                }
            }
            }

            return i1 + i2 + i3 + i4 + i5 + i6;
        }
 }

