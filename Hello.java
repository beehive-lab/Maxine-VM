
public class Hello {

public static void main(String []args) {


	System.err.println("HELLO WORLD");
	int x[] = new int [1024];
	int j;
	for(j = 0; j < 1024;j++) {
		if(j == 0 || j == 1) System.err.println("DOING LOOP " + j);
		x[j] = j;
		String xx = Integer.toString(x[j]);
		System.err.println("---> " + xx);
	}
	System.err.println("DONE LOOP");
//	System.err.println(args[0]);
	for(int i = 0; i < args.length;i++)	{

		System.err.println(args[i]);

	}
	System.err.println("DONE ARGS");
System.err.println(new Date());
long xx = System.currentTimeMillis();

Date currentDate = new Date(xx);
System.err.println(currentDate);

}

};
