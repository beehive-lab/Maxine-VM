package test.output;

import java.lang.invoke.*;

/**
 * Unit test for method handle getters.
 *
 */
public class MethodHandles09 {

    static String hello = "Hello ";

    String world = "World!";


    static MethodHandles.Lookup lookup() {
        return MethodHandles.lookup();
    }

    public static void main (String [] args) {
        try {
            MethodHandle getHello = lookup().findStaticGetter(MethodHandles09.class, "hello", String.class);
            MethodHandle getWorld = lookup().findGetter(MethodHandles09.class, "world", String.class);

            MethodHandles09 mh09 = new MethodHandles09();
            String h  = (String)getHello.invokeExact();
            String w = (String)getWorld.invokeExact(mh09);
            System.out.print(h);
            System.out.println(w);

        } catch (Throwable e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

}
