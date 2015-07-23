import java.util.Date;
public class Hello {

public static void main(String []args) {


	int x[] = new int [1024];
	int j;
	for(j = 0; j < 1024;j++) {
		x[j] = j;
		String xx = Integer.toString(x[j]);
	}
	//System.err.println(j);
	System.err.println(args[0]);
	System.err.println(new Date());
	System.err.println("HELLO WORLD");
	for(int i = 0; i < args.length;i++)	{

		System.err.println(args[i]);

	}

}

};
