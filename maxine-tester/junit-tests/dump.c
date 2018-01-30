#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>

/*
 * Writes the contents of the codebuffer to stdout in binary format.
 * The output can be disassembled using e.g. objdump.
 */
static void print_code_buffer();
static void c_entry();

int main (int c, char **v)
{
	c_entry(); /* initialise code array */
	print_code_buffer();     /* print code array in binary */
	exit(0);
}

#include "./codebuffer.c"
	
}

void print_code_buffer()
{
	int i;
	fprintf(stderr, "Bytes: %lu\n", sizeof(code)/sizeof(char));
	for (i = 0; i < (sizeof(code)/sizeof(char)); i+=4) {
		write(fileno(stdout), &code[i], 4); 
	}
}
