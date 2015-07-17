import java.awt.Graphics;
import java.awt.image.BufferedImage;
import javax.swing.JFrame;
 
public class Mandelbrot {
 
    private final int MAX_ITER = 570;
    private final double ZOOM = 150;
    private static float I[][] = new float[100][800];
    private double zx, zy, cX, cY, tmp;
 
    public Mandelbrot() {
        //setBounds(100, 100, 800, 600);
        //I = new double[100][800];
        for (int y = 0; y < 800; y++) {
            for (int x = 0; x < 100; x++) {
                zx = zy = 0;
                cX = (x - 400) / ZOOM;
                cY = (y - 300) / ZOOM;
                int iter = MAX_ITER;
                while (zx * zx + zy * zy < 4 && iter > 0) {
                    tmp = zx * zx - zy * zy + cX;
                    zy = 2.0 * zx * zy + cY;
                    zx = tmp;
                    iter--;
                }
		System.out.println("x is "+ x);
		System.out.println("y is " + y);
		I[x][y] = iter | (iter << 8);
                //I.setRGB(x, y, iter | (iter << 8));
            }
        }
    }
 
 
    public static void main(String[] args) {
        new Mandelbrot();//.setVisible(true);
    }
}
