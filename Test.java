
public class Test {

public static void main (String args[]) {
        byte x[] = new byte[2];
	byte zz[] = new byte[4];

	zz[0] = 60;
	zz[1] = -1;
	zz[2] = 47;
	zz[3] = -31;

	int temp = 0;
	temp = (zz[3] <<24) | (zz[2] <<16) | (zz[1] << 8) | zz[0];
	System.out.println("TEMP is "+ temp);
	temp = ((zz[3] <<24) & 0xff000000) | ((zz[2] <<16)&0xff0000) | ((zz[1] << 8) & 0xff00) | (zz[0]&0xff);
	System.out.println("TEMP is "+ temp);
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
