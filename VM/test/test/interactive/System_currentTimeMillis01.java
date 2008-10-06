/*VCSID=ded9966e-a3be-4bcb-807e-9b7c170629fa*/
package test.interactive;

/**
 * The <code>System_currentTimeMillis01</code> class definition.
 *
 * @author Ben L. Titzer
 */
public class System_currentTimeMillis01 {
    public static void main(String[] args) {
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < 1) {
            // do nothing.
        }
        System.out.println("done.");
    }
}
