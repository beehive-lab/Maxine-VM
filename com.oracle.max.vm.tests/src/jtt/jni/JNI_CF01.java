package jtt.jni;

import com.sun.max.annotate.*;


public class JNI_CF01 {

    public static boolean test(int arg) throws Exception {
        noArgs();
        return true;
    }

    @C_FUNCTION
    private static native void noArgs();

}

