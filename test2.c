#include <stdio.h>


int divide(int a, int b) {
return a/b;
}
int main(int argc, char**argv) {
	long long xx = 0;
	long long yy = -2147483648LL;
	int y;
	int x;
	x = 20;
	sscanf(argv[1],"%d",&y);
	
	printf(" %d\n",divide(x,y));
	return 0;
}
