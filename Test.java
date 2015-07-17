
public class Test {

public static void main (String args[]) {
        byte x[] = new byte[2];
        float inputD[] = new float [2];
        x[0] = -1;
        x[1] = 1;
        inputD[0] = (float)x[0];
        inputD[0] = (float)x[1];
        int z;
        z = (int) x[0];
        System.out.println("INT " + z);
        z = (int) x[1];
        System.out.println("INT " + z);
        System.out.println("HELO");
        System.out.println(inputD[0] + " " + inputD[1]);
        System.out.println("DOUBLE " + inputD[0] + " " + -1.0*inputD[1] + " as int " + (int)inputD[1]);
        System.out.println(x[0]);
        System.out.println(x[1]);
    }
}
