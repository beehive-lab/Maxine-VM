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
/*VCSID=5108a000-7b0e-4029-8202-a0ffc5164c26*/
#ifndef _libInfo_h_
#define _libInfo_h_ 1

#include <unistd.h>
#include <limits.h>

#include "symtab.h"

// data structures in this file mimic those of Solaris 8.0 - libproc's Pcontrol.h

#define BUF_SIZE     (PATH_MAX + NAME_MAX + 1)

// list of shared objects
typedef struct LibInfo {
  char             name[BUF_SIZE];
  uintptr_t        base;
  struct symtab*   symtab;
  int              fd;        // file descriptor for lib 
  struct LibInfo* next;
} LibInfo;

// list of virtual memory maps
typedef struct map_info {
   int              fd;       // file descriptor 
   off_t            offset;   // file offset of this mapping 
   uintptr_t        vaddr;    // starting virtual address 
   size_t           memsz;    // size of the mapping 
   struct map_info* next;     
} map_info;

#include "proc_service.h"

extern int pathmap_open(const char* name);

extern uintptr_t lookup_symbol(struct ps_prochandle* ph, const char* sym_name);

extern Boolean read_LibInfo(struct ps_prochandle* ph);

// a test for ELF signature without using libelf
extern Boolean is_elf_file(int fd);

#endif /*_libInfo_h_*/


