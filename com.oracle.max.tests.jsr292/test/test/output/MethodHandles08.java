package test.output;

import java.lang.invoke.*;
import static java.lang.invoke.MethodType.*;

/**
 *
 * Test for method handle constructor.
 *
 */
public class MethodHandles08 {

    static MethodHandles.Lookup lookup() {
        return MethodHandles.lookup();
    }

    @Override
    public String toString() {
        return "MethodHandles08";
    }

    public static void main (String [] args) {
        try {
            MethodHandle mh = lookup().findConstructor(MethodHandles08.class, methodType(void.class));
            MethodHandles08 mh08 = (MethodHandles08)mh.invokeExact();
            System.out.println(mh08.toString());
        } catch (Throwable e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
