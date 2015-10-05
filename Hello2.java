
public class Hello2 {

public static void main(String []args) {

	int i;
	boolean xxx = false;
	i = 0;
	i = i+0xbeeff0d0;
	System.out.println("BOOSHAKA");
	try {
	jtt.optimize.Conditional01.test(1);
	System.out.println("BOOSHAKA1");
	jtt.optimize.Conditional01.test(10);
	System.out.println("BOOSHAKA10");
	jtt.optimize.Conditional01.test(48);
	System.out.println("BOOSHAKA48");
	} catch(Exception e) {
		System.err.println(e);
		e.printStackTrace();
		System.out.println("NULL PTR EXCEPTION");
	}
 for(i = 0 ; i < 3;i++) {
        xxx  =jtt.lang.System_identityHashCode01.test(i);
	System.err.println("TRYING ");
	if(xxx == true) System.err.println("TRUE");
	else System.err.println("FALSE");
	}

}

};
