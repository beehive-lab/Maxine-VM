public class ArrayTest {

static Object []array = { null, null, ""};
public static Object test(int arg) { final Object[] obj = arg == -2 ? null : array; return obj[arg];}
public static void main(String []args) {

	System.out.println("D2F " + jtt.bytecode.BC_d2f.test(1.0d));
	System.out.println("D2I " + jtt.bytecode.BC_d2i01.test(1.0d));
	try {
	System.out.println("INVOKESPECIALTEST\n");
	boolean y = jtt.except.BC_invokespecial01.test(1);
	System.out.println("SHOULDHAVEFAULTED");
	y  = jtt.except.BC_invokevirtual01.test(0);
	System.out.println(y);
	y = jtt.except.BC_invokevirtual02.test(0);
	System.out.println("NO FAULTS");
	y = jtt.except.BC_invokevirtual02.test(1);
	y = jtt.except.BC_invokevirtual01.test(1);
	if(y== true)
	System.out.println("true");
	else
	System.out.println("false");
	//System.out.println(y);
	int x  =jtt.except.BC_idiv.test(11,0);
	//x  =jtt.except.BC_idiv.test(11,0);
	//System.out.println(jtt.except.BC_idiv.test(11,0));
	//System.out.println(jtt.except.BC_idiv.test(11,0));
	//float one = 1.0f;
	//float two = one/0.0f;
	//System.out.println(one/0.0f);
	} catch(Throwable e) {
		e.printStackTrace();
		System.err.println(e);
	}
	for(int yy = 0; yy < 5;yy++)	{
		System.out.println("BC_D2l01 " + yy+ " VAL " + jtt.bytecode.BC_d2l01.test((double)75+yy));
		System.out.println("BC_f2l01 " + yy+ " VAL " + jtt.bytecode.BC_f2l01.test((float)80+yy));
		System.out.println("BC_D2l02 " + yy+ " VAL " + (float)-1.3e44d + " " + jtt.bytecode.BC_d2l02.test(yy));
		System.out.println("BC_F2l02 " + yy+ " VAL " + (float)-1.3e44d + " " + jtt.bytecode.BC_f2l02.test(yy));

	}

	        /*jtt.except.BC_aastore0 aastore0 = new jtt.except.BC_aastore0();
	try {
		//System.out.println("FAULTING 0.0f/0.0f" + 0.0f/0.0f);
		//System.out.println("FAULTING 0.0d/0.0d" + 0.0d/0.0d);
		//System.out.println("FAULTING 0/0." + 0/0);
		//System.out.println("FAULTING 0L/0L" + 0L/0L);
	System.out.println( jtt.except.BC_invokespecial01.test(1) );
	System.out.println( jtt.except.BC_invokespecial01.test(1) );
	} catch(Throwable e) {
		  System.out.println("EXPECT java.lang.NullPointerException " + e.getClass());
		  e.printStackTrace();

	}
		
        int zz;
	jtt.bytecode.BC_d2l02 zzz = new jtt.bytecode.BC_d2l02();
	for(zz = 0; zz < 5;zz++) {
		System.out.println("ALIVE");
		System.out.println("ITER " + zz + " RESULT " +zzz.test(zz) + " GET " + zzz.get(zz));
	}
        try {
        zz = aastore0.test(true,-2);
        } catch(Throwable e) {
                System.out.println("EXPECT java.lang.NullPointerException " + e.getClass());
        }
        try {
        zz = aastore0.test(true,-1);
        } catch(Throwable e) {
                System.out.println("EXPECT java.lang.ArrayIndexOutOfBoundsException " + e.getClass());
        }
        for(int yy = 0; yy <= 2;yy++) {
                zz = aastore0.test(true,yy);
                System.out.println("EXPECT equals " + yy + " " + zz);

        }
        try {
        zz = aastore0.test(true,3);
        } catch(Throwable e) {
                System.out.println("EXPECT java.lang.ArrayIndexOutOfBoundsException " + e.getClass());
        }
	*/
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
