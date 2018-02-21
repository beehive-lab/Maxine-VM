/*
 * Copyright (c) 2018, APT Group, School of Computer Science,
 * The University of Manchester. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

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
