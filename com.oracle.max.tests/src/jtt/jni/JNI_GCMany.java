package jtt.jni;

import java.lang.reflect.*;


public class JNI_GCMany {
    private native Object jniGC(Method gcMethod, Class<?> systemClass, Object o1, Object o2, Object o3, Object o4, Object o5);

    public static boolean test(int arg) throws Exception {
        Method gcMethod = System.class.getDeclaredMethod("gc");
        Object o1 = "o1";
        Object o2 = "o2";
        Object o3 = "o3";
        Object o4 = "o4";
        Object o5 = "o5";
        JNI_GCMany self = new JNI_GCMany();
        return o5 == self.jniGC(gcMethod, System.class, o1, o2, o3, o4, o5);
    }

}
