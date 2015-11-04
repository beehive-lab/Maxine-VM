#include <stdio.h>


int convert(long long yy) {
	return (int) yy;
}
long long mulme(long long a,long long b) {
	return a*b;
}
int compare(long long a, long long b) {
	int x = 0;
	if(a<b) x = -1;
	if(a==b)x = 0;
	if(a>b) x =1;
	return x;	
}
int compareeq(long long a, long long b) {
	int x = 0;
	if(a==b) x = 1;
	return x;
}
int comparene(long long a, long long b) {
	int x = 0;
	if(a!=b) x = 1;
	return x;
}
int comparele(long long a , long long b) {
	int x = 5;
	if(a<=b) x = 1;
        else x = 0;
	return x;
}
int comparege(long long a , long long b) {
	int x = 5;
	if(a>=b) x = 1;
	else x = 0;
	return x;
}

int comparelt(long long a , long long b) {
	return a < b;
}

int uaboveThan(unsigned long long a,
		unsigned long long b) {

	return a > b;
}
int comparegt(long long a , long long b) {
        return a > b;
}

int aboveThan(long long a,long long b) {
return (a < b) ^ ((a < 0) != (b < 0));

}
int compare2(long long a, long long b) {
	int x = 5;
	if(a!=b) x = 1;
	else x= 0;
	return x;
}
long long convertME(int yy) {
	return (long long) yy;
}

long long longdiv(long long a, long long b) {
	return a/b;
}
unsigned int umull(unsigned int a, unsigned int b) {
return a*b;
}
unsigned int uadd(unsigned int a, unsigned int b) {
return a+b;
}
unsigned int usub(unsigned int a, unsigned int b) {
return a-b;
}
int main(int argc, char**argv) {
	long long xx = 0;
	long long yy = -2147483648LL;
	int y;
	int x;
	printf("ABOVETHAN %d\n",uaboveThan(64,47));
	printf("ABOVE THANK %d\n", comparegt(64,47));
	printf("ABOVE THANK %d\n", comparegt(64,-47));
	printf("ADD %x\n", -16372);
	printf("BLX %x\n", -196);
	printf("BLX2 %x\n", -516948164);
	printf("EXPECTED ADD %x\n",-527450100);

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
			yy = 214227483647LL;
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
		case 8:
			yy = -1;
		break;
		case 9:
			yy =1;
		break;
		default :
			yy = 7LL;
		break;
	}
	
	printf(" %lld\n",yy);
	if(yy == 0) {
	printf("FDIV gives %f\n",1.0f/0.0f);
	printf("DIV by zero reetues %d\n",1/((int)yy));
	printf("DEAD\n");
	return 0;
	printf("DONE\n");
	}
	compare(7LL,6LL);
	compareeq(7LL,6LL);
	comparene(6LL,8LL);	
	comparelt(6LL,8LL);	
	comparegt(6LL,8LL);	
	comparege(6LL,8LL);	
	comparele(6LL,8LL);	
yy 	 = convertME(-1);
yy = convertME(0x80000000);
	return 0;
}
