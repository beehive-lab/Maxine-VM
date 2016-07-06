import java.util.*;

public class Hello {

public static void main(String []args) {

	int a = 0;
	int b = 0;
	int c = 0 ;
	while (true) {
		System.out.println("IN " + a);
		a++;
		
			outer: if (c % 2 == 0) {
				if (b >= 0) {
					b = -1;
					System.out.println("MAKE -1");
					break outer;
				} else {
					b = 1;
					System.out.println("MAKE 1");
				}
				c++;
			}
		
		System.out.println("OUT " + a);
		
		
	}

}

}
