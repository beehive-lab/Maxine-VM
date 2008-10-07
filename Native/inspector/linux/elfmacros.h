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
/*VCSID=deaee2b8-c4b3-401c-85fa-ab3a6d4fc8c9*/
#ifdef USE_PRAGMA_IDENT_HDR
#pragma ident "@(#)elfmacros.h	1.8 05/11/18 15:16:57 VM"
#endif
/*
 * Copyright 2006 Sun Microsystems, Inc.  All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL.  Use is subject to license terms.
 */

#ifndef _ELFMACROS_H_
#define _ELFMACROS_H_

// these are actually 32/64 bit discriminators, but we support 
// Linux on Intel, AMD processors only.

#if defined(ia64) || defined(amd64) || defined(x86_64)
#define ELF_EHDR    	Elf64_Ehdr
#define ELF_SHDR 	Elf64_Shdr
#define ELF_PHDR    	Elf64_Phdr
#define ELF_SYM         Elf64_Sym
#define ELF_NHDR	Elf64_Nhdr
#define ELF_DYN         Elf64_Dyn
#define ELF_ADDR        Elf64_Addr

#define ELF_ST_TYPE     ELF64_ST_TYPE
#endif /* ia64 || amd64 || x86_64 */

#ifdef i386
#define ELF_EHDR 	Elf32_Ehdr
#define ELF_SHDR 	Elf32_Shdr
#define ELF_PHDR 	Elf32_Phdr
#define ELF_SYM 	Elf32_Sym
#define ELF_NHDR 	Elf32_Nhdr
#define ELF_DYN         Elf32_Dyn
#define ELF_ADDR        Elf32_Addr

#define ELF_ST_TYPE     ELF32_ST_TYPE
#endif /* i386 */

#endif /* _ELFMACROS_H_ */
