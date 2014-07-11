volatile unsigned int * const UART0DR = (unsigned int *)0x101f1000;
 
void print_uart0(const char *s) {
 while(*s != '\0') { /* Loop until end of string */
 *UART0DR = (unsigned int)(*s); /* Transmit char */
 s++; /* Next char */
 }
}
#include "./codebuffer.c"

/*unsigned char  code[12] __attribute__((aligned(0x1000))) ;
void c_entry() {
code[0] = 0xff;
code[1] = 0x90;
code[2] = 0xa0;
code[11] = 0xea;
*/

/*
	Apologies, need to pass a parameter to this function, and can no longer do this in the test harness ... will try and fix this prior to pushing 
 expect this to break some of the other tests 
*/
//void (*pf)(int) = (0); pf  = (void (*))(code);
#ifdef OLDCOMPILE
void (*pf)(int) = (void (*))(code);
 print_uart0("changed test.c!\n");
(*pf)(1); // Need to change this to something related to the test itself
asm volatile("forever: b forever");
#endif
}

