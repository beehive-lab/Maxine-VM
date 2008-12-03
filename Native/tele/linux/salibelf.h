/*
 * Copyright (c) 2007 Sun Microsystems, Inc.  All rights reserved.
 *
 * Sun Microsystems, Inc. has intellectual property rights relating to technology embodied in the product
 * that is described in this document. In particular, and without limitation, these intellectual property
 * rights may include one or more of the U.S. patents listed at http://www.sun.com/patents and one or
 * more additional patents or pending patent applications in the U.S. and in other countries.
 *
 * U.S. Government Rights - Commercial software. Government users are subject to the Sun
 * Microsystems, Inc. standard license agreement and applicable provisions of the FAR and its
 * supplements.
 *
 * Use is subject to license terms. Sun, Sun Microsystems, the Sun logo, Java and Solaris are trademarks or
 * registered trademarks of Sun Microsystems, Inc. in the U.S. and other countries. All SPARC trademarks
 * are used under license and are trademarks or registered trademarks of SPARC International, Inc. in the
 * U.S. and other countries.
 *
 * UNIX is a registered trademark in the U.S. and other countries, exclusively licensed through X/Open
 * Company, Ltd.
 */
#ifndef _salibelf_h_
#define _salibelf_h_ 1

#include <elf.h>

#include "c.h"
#include "elfmacros.h"

// read ELF file header. 
extern int read_elf_header(int fd, ELF_EHDR* ehdr); 

// is given file descriptor corresponds to an ELF file?
extern Boolean is_elf_file(int fd);

// read program header table of an ELF file. caller has to
// free the result pointer after use. NULL on failure.
extern ELF_PHDR *read_program_header_table(int fd, ELF_EHDR* hdr);

// read section header table of an ELF file. caller has to
// free the result pointer after use. NULL on failure.
extern ELF_SHDR *read_section_header_table(int fd, ELF_EHDR* hdr);

// read a particular section's data. caller has to free the
// result pointer after use. NULL on failure.
extern void *read_section_data(int fd, ELF_EHDR* ehdr, ELF_SHDR* shdr);

// find the base address at which the library wants to load itself
extern uintptr_t find_base_address(int fd, ELF_EHDR* ehdr);

#endif /*_salibelf_h_*/
