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
#include <stdlib.h>
#include <string.h>
#include <unistd.h>

#include "log.h"

#include "salibelf.h"

// ELF file parsing helpers. Note that we do *not* use libelf here.
int read_elf_header(int fd, ELF_EHDR* ehdr) {
   if (pread(fd, ehdr, sizeof (ELF_EHDR), 0) != sizeof (ELF_EHDR) ||
            memcmp(&ehdr->e_ident[EI_MAG0], ELFMAG, SELFMAG) != 0 ||
            ehdr->e_ident[EI_DATA] != ELFDATA2LSB ||
            ehdr->e_version != EV_CURRENT) {
        return 0;
   }
   return 1;
}

Boolean is_elf_file(int fd) {
   ELF_EHDR ehdr;
   return read_elf_header(fd, &ehdr);
}

// read program header table of an ELF file
ELF_PHDR* read_program_header_table(int fd, ELF_EHDR* hdr) {
   ELF_PHDR* phbuf = 0;
   // allocate memory for program header table
   size_t nbytes = hdr->e_phnum * hdr->e_phentsize;

   if ((phbuf = (ELF_PHDR*) malloc(nbytes)) == NULL) {
      log_println("can't allocate memory for reading program header table");
      return NULL;
   }

   if ((size_t) pread(fd, phbuf, nbytes, hdr->e_phoff) != nbytes) {
      log_println("ELF file is truncated! can't read program header table");
      free(phbuf);
      return NULL;
   }

   return phbuf;
}

// read section header table of an ELF file
ELF_SHDR* read_section_header_table(int fd, ELF_EHDR* hdr) {
   ELF_SHDR* shbuf = 0;
   // allocate memory for section header table
   size_t nbytes = hdr->e_shnum * hdr->e_shentsize;

   if ((shbuf = (ELF_SHDR*) malloc(nbytes)) == NULL) {
      log_println("can't allocate memory for reading section header table");
      return NULL;
   }

   if ((size_t) pread(fd, shbuf, nbytes, hdr->e_shoff) != nbytes) {
      log_println("ELF file is truncated! can't read section header table");
      free(shbuf);
      return NULL;
   }

   return shbuf;
}

// read a particular section's data
void* read_section_data(int fd, ELF_EHDR* ehdr, ELF_SHDR* shdr) {
  void *buf = NULL;
  if (shdr->sh_type == SHT_NOBITS || shdr->sh_size == 0) {
     return buf;
  }
  if ((buf = calloc(shdr->sh_size, 1)) == NULL) {
     log_println("can't allocate memory for reading section data");
     return NULL;
  }
  if ((size_t) pread(fd, buf, shdr->sh_size, shdr->sh_offset) != shdr->sh_size) {
     free(buf);
     log_println("section data read failed");
     return NULL;
  }
  return buf;
}

uintptr_t find_base_address(int fd, ELF_EHDR* ehdr) {
  uintptr_t baseaddr = (uintptr_t)-1;
  int cnt;
  ELF_PHDR *phbuf, *phdr;

  // read program header table
  if ((phbuf = read_program_header_table(fd, ehdr)) == NULL) {
    goto quit;
  }

  // the base address of a shared object is the lowest vaddr of
  // its loadable segments (PT_LOAD)
  for (phdr = phbuf, cnt = 0; cnt < ehdr->e_phnum; cnt++, phdr++) {
    if (phdr->p_type == PT_LOAD && phdr->p_vaddr < baseaddr) {
      baseaddr = phdr->p_vaddr;
    }
  }

quit:
  if (phbuf) free(phbuf);
  return baseaddr;
}
