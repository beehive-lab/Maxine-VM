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
code[3] = 0xe3;
code[4] = 0xff;
code[5] = 0xaf;// r10?
code[6] =0x4f;
code[7] = 0xe3; // do load or r9 twice
code[8] = 0xfe;
code[9] = 0xff;
code[10] = 0xff;
code[11] = 0xea;
*/

/*tmpit.code[0] = 0xff;
tmpit.code[1] =0x9f;

tmpit.code[2] = 0x4f;
tmpit.code[3] = 0xe3;
tmpit.code[4] = 0xff;
tmpit.code[5] = 0x9f;
tmpit.code[6] =0x4f;
tmpit.code[7] = 0xe3;
tmpit.code[8] = 0x4f;
*/
/*
	Apologies, need to pass a parameter to this function, and can no longer do this in the test harness ... will try and fix this prior to pushing 
 expect this to break some of the other tests 
*/
void (*pf)(int) = (0);
pf  = (void (*))(code);
 print_uart0("changed test.c!\n");
(*pf)(1); // Need to change this to something related to the test itself
/*
ie the call is hardcoded with 1, it needs to reflect the arguments required by the test, also the buffer with instructions in it should be placed at a point with static initialisation, it will then be seen as statically initialised data that could be disasm-ed using objdump -D ... at the moment we initialise the array when we execute, this slows down emulation etc, and we could remove the print_uart0 and make the simulation faster
*/
asm("forever: b forever");
}

