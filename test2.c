#include <stdio.h>


int divide(int a, int b) {
return a/b;
}

unsigned long long shiftme (unsigned long long int a, int b) {

	a = a >> b;
	return a;
}
long long shiftright(long long a, int b) {
	a = a >> b;
	return a;

}
int main(int argc, char**argv) {
	long long xx = 0;
	long long yy = -2147483648LL;
	int y;
	int x;
	x = 20;
	sscanf(argv[1],"%d",&y);
	unsigned long long fred = 737869762604784681LL;
	fred = shiftright(fred,33);
	printf(" %lld\n",fred);
	fred = shiftme(fred,33);
	printf(" %lld\n",fred);
	

	
	return 0;
}
