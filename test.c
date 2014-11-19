#include <stdio.h>


int convert(long long yy) {
	return (int) yy;
}
long long mulme(long long a,long long b) {
	return a*b;
}

long long convertME(int yy) {
	return (long long) yy;
}
int main(int argc, char**argv) {
	long long xx = 0;
	long long yy = -2147483648LL;
	int y;
	int x;
	sscanf(argv[1],"%d",&y);
	switch(y) {
		case 0:
			yy  = 0LL;
		break;
		case 1:
			yy = -1LL;
		break;
		case 2:
			yy = 1LL;
		break;
		case 3:
			yy = -2147483648LL;
		break;
		case 4:
			yy = 2147483647LL;
		break;
		case 5:
			yy = 9223372036854775807LL;
		break;
		case 6:
			yy =  -9223372036854775808LL;
		break;

		case 7:
			yy=-2147483648LL; 
		break;

		default :
			yy = 7LL;
		break;
	}
	
	printf(" %lld\n",yy);
	printf("%lld\n",mulme(yy,1LL));
	x = convert(yy);

	return 0;
}
