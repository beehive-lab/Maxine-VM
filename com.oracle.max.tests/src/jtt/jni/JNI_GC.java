package jtt.jni;

import java.lang.reflect.*;


public class JNI_GC {

    private native Object jniGC(Method gcMethod, Class<?> systemClass);

    public static boolean test(int arg) throws Exception {
        Method gcMethod = System.class.getDeclaredMethod("gc");
        JNI_GC self = new JNI_GC();
        return self == self.jniGC(gcMethod, System.class);
    }
}
