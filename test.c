#include <stdio.h>

int main(int argc, char**argv) {
	long long xx = 0;
	double mydouble;
	sscanf(argv[1],"%lf",&mydouble);
	xx = (long long) mydouble;
	printf("%lld\n",xx);
	return 0;
}
