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
#ifdef USE_PRAGMA_IDENT_HDR
#pragma ident "@(#)symtab.h	1.8 05/11/18 15:16:58 VM"
#endif
/*
 * Copyright 2006 Sun Microsystems, Inc.  All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL.  Use is subject to license terms.
 */

#ifndef _SYMTAB_H_
#define _SYMTAB_H_

#include <stdint.h>

// interface to manage ELF symbol tables

struct symtab;

// build symbol table for a given ELF file descriptor
struct symtab* build_symtab(int fd);

// destroy the symbol table
void destroy_symtab(struct symtab* symtab);

// search for symbol in the given symbol table. Adds offset
// to the base uintptr_t supplied. Returns NULL if not found.
uintptr_t search_symbol(struct symtab* symtab, uintptr_t base,
                      const char *sym_name, int *sym_size);

// look for nearest symbol for a given offset (not address - base
// subtraction done by caller
const char* nearest_symbol(struct symtab* symtab, uintptr_t offset,
                      uintptr_t* poffset);

#endif /*_SYMTAB_H_*/
