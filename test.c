#include <stdio.h>


int convert(long long yy) {
	return (int) yy;
}
int main(int argc, char**argv) {
	long long xx = 0;
	int y;
	sscanf(argv[1],"%d",&y);
	xx = (long long) mydouble;
	switch(y) {
		case 0:
			xx = 0;
		break;
		case 1: xx = 0x7fffffffffffffff; break;
		case 2: xx = 0x8000000000000000; break;
		case 3: xx = 0xffffffff; break; 
		case 4: xx = 0x8002000000000020; break;
		case 5: xx = 0x7fffffffffff; break;
	}
	x = convert(xx);
	return 0;
}
