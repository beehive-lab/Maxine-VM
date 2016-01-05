
public class X2 {

private static double one[] = new double[1000];
private static double two[] = new double[1000];
private static double acc[] = new double[1000];
private void accumulate(int i, int j) {
	for(int a = i; a < j;a++)	{
		acc[a] = one[a] + two[a];
	}
}
public static void main(String args[]) {

	int a;
	int b;
	X2 oner = new X2();
	a = Integer.parseInt(args[0]);
	b = Integer.parseInt(args[1]);
	for(int i = 0; i < 1000; i++)	{
		acc[i] = 0.0;
		one[i] = 0.00000000000001;
		two[i] = one[i];
	}
	for(int i = 0 ; i < 6000;i++)	{
		oner.accumulate(a,b);
	}
	System.out.println(acc[a] + " " + acc[b-1]);
	
}
};
