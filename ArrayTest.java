public class ArrayTest {

static Object []array = { null, null, ""};
public static Object test(int arg) { final Object[] obj = arg == -2 ? null : array; return obj[arg];}
public static void main(String []args) {

	int x[] = new int[100];
	for(int i = 0; i < 100;i++)	{
		x[i] = i;
		//System.out.println(Integer.toString(x[i]));
	}
	System.out.println("1D-Int array");
	for(int i =0 ; i < array.length;i++)
	System.out.println("EXPECT  null " + test(0));	
	try {
	System.out.println(test(-1));
	} catch(Throwable e) {
		System.out.println("EXPECT java.lang.ArrayIndexOutOfBoundsException " +e.getClass());
		e.printStackTrace();
	}
	try {
	System.out.println(test(-2));
	} catch(Throwable e) {
		System.out.println("EXPECT java.lang.NullPointerException " +e.getClass());
		e.printStackTrace();
	}
	try {
        System.out.println("EXPECT null " + test(0));
        } catch(Throwable e) {
                System.out.println("ERROR");
		e.printStackTrace();
        }

	System.out.println("DONE TEST");
	jtt.except.BC_aaload1 xz = new jtt.except.BC_aaload1();
	try {
		System.out.println("EXPECT null " +xz.test(0));
		System.out.println("EXPECT null " +xz.test(-2));
		xz.test(-1);
		
	} catch(Throwable e) {
		System.out.println("EXPECT java.lang.ArrayIndexOutOfBoundsException " +e.getClass());
		e.printStackTrace();

	}
	int y[][] = new int[100][100];
	System.out.println("Created 2D int array");
	System.out.println("REALLY");
	for(int i = 0; i < 100;i++)	{
		y[i][i] = i;
		System.out.println(Integer.toString(y[i][i]));
	}
	System.out.println("2D-Int array");
	char a[] = new char[100];
	for(int i = 0 ;i < 100;i++) {
		a[i] = 'a';
		//System.out.println(a[i]);
	}
	System.out.println("1D-char array");
	long d[] = new long[100];
	for(int i = 0; i < 100;i++)	{
		d[i] = (long)i;
	}
	System.out.println("1D-long array");
	double c[] = new double[100];
	for(int i  = 0; i < 100;i++)	{
		c[i] = (double) i;
	}
	System.out.println("1D-double array");
	float b[] = new float[100];
	for(int i = 0; i <  100;i++)	{
		b[i] = (float)i;
	}
	System.out.println("1D-float array");
	double [][]dd = new double[100][100];
	for(int i = 0; i < 100; i++)	{
		dd[i][i] = (double)i;
		int j = (int)((float) dd[i][i]);
		System.out.println("2DDoubles " + Integer.toString(j));
	}
	long ll[][] = new long[100][100];
	for(int i = 0; i < 100;i++)	{
		ll[i][i] = (long)i;
		System.out.println("2DLONGS " + Integer.toString((int) ll[i][i]));
	}
	System.out.println("2D Double ... should print 0..99) above");
	float [][]ddd = new float[100][100];
        for(int i = 0; i < 100; i++)    {
                ddd[i][i] = (float)i;
                int j = (int) ddd[i][i];
                System.out.println("2DFLOATS" + Integer.toString(j));
        }
        System.out.println("2D Float ... should print 0..99) above");


}

}
