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
/*VCSID=1d6c30d1-81c9-4c17-b354-f10885acfe46*/
#include <stdarg.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <fcntl.h>
#include <thread_db.h>

#include "debug.h"

#include "libInfo.h"

static const char* alt_root = NULL;
static int alt_root_len = -1;

#define SA_ALTROOT "SA_ALTROOT"

static void init_alt_root() {
   if (alt_root_len == -1) {
      alt_root = getenv(SA_ALTROOT);
      if (alt_root) {
         alt_root_len = strlen(alt_root);
      } else {
         alt_root_len = 0;
      }
   }
}

int pathmap_open(const char* name) {
   int fd;
   char alt_path[PATH_MAX + 1];

   init_alt_root();
   fd = open(name, O_RDONLY);
   if (fd >= 0) {
      return fd;
   }

   if (alt_root_len > 0) {
      strcpy(alt_path, alt_root);
      strcat(alt_path, name);
      fd = open(alt_path, O_RDONLY);
      if (fd >= 0) {
         debug_println("path %s substituted for %s", alt_path, name);
         return fd;
      }

      if (strrchr(name, '/')) {
         strcpy(alt_path, alt_root);
         strcat(alt_path, strrchr(name, '/'));
         fd = open(alt_path, O_RDONLY);
         if (fd >= 0) {
            debug_println("path %s substituted for %s", alt_path, name);
            return fd;
         }
      }
   }

   return -1;
}

static void destroy_LibInfo(struct ps_prochandle* ph) {
   LibInfo* lib = ph->libs;
   while (lib) {
     LibInfo *next = lib->next;
     if (lib->symtab) {
        destroy_symtab(lib->symtab);
     }
     free(lib);
     lib = next;
   }
}

static LibInfo* add_LibInfo_fd(struct ps_prochandle* ph, const char* libname, int fd, uintptr_t base) {
   LibInfo* newlib;
  
   newlib = (LibInfo *) calloc(1, sizeof(struct LibInfo));
   if (newlib == NULL) {
      debug_println("can't allocate memory for LibInfo");
      return NULL;
   }

   strncpy(newlib->name, libname, sizeof(newlib->name));
   newlib->base = base;

   if (fd == -1) {
      newlib->fd = pathmap_open(newlib->name);
      if (newlib->fd < 0) {
         debug_println("can't open shared object %s", newlib->name);
         free(newlib);
         return NULL;
      }
   } else {
      newlib->fd = fd;
   }
   
   // check whether we have got an ELF file. /proc/<pid>/map
   // gives out all file mappings and not just shared objects
   if (is_elf_file(newlib->fd) == false) {
      close(newlib->fd);
      free(newlib);
      return NULL;
   } 

   newlib->symtab = build_symtab(newlib->fd);
   if (newlib->symtab == NULL) {
      debug_println("symbol table build failed for %s", newlib->name);
   }

   // even if symbol table building fails, we add the LibInfo.
   // This is because we may need to read from the ELF file for core file
   // address read functionality. lookup_symbol checks for NULL symtab.
   if (ph->libs) { 
      ph->lib_tail->next = newlib;
      ph->lib_tail = newlib;
   }  else { 
      ph->libs = ph->lib_tail = newlib;
   }
   ph->num_libs++;

   return newlib;
}

static LibInfo* add_LibInfo(struct ps_prochandle* ph, const char* libname, uintptr_t base) {
   return add_LibInfo_fd(ph, libname, -1, base);
}

/*
 * fgets without storing '\n' at the end of the string
 */
static char *fgets_no_cr(char * buf, int n, FILE *fp) {
    char * rslt = fgets(buf, n, fp);
    if (rslt && buf && *buf){
        char *p = strchr(buf, '\0');
        if (*--p=='\n') {
            *p='\0';
        }
    }
    return rslt;
}

/*
 * splits a string _str_ into substrings with delimiter _delim_ by replacing old * delimiters with _new_delim_ (ideally, '\0'). the address of each substring
 * is stored in array _ptrs_ as the return value. the maximum capacity of _ptrs_ * array is specified by parameter _n_.
 * RETURN VALUE: total number of substrings (always <= _n_)
 * NOTE: string _str_ is modified if _delim_!=_new_delim_
 */
static int split_n_str(char * str, int n, char ** ptrs, char delim, char new_delim)
{
    int i;
    for(i = 0; i < n; i++) {
        ptrs[i] = NULL;
    }
    if (str == NULL || n < 1 ) {
        return 0;
    }
   
    i = 0;

    // skipping leading blanks
    while(*str&& * str == delim) {
        str++;
    }
   
    while(*str && i < n) {
        ptrs[i++] = str;
        while(*str&& * str != delim) {
            str++;
        }
        while(*str&& * str == delim) {
            *(str++) = new_delim;
        }
    }
   
    return i;
}

Boolean find_lib(struct ps_prochandle* ph, const char *lib_name) {
    LibInfo *p = ph->libs;
    while (p) {
        if (strcmp(p->name, lib_name) == 0) {
            return true;
        }
        p = p->next;
    }
    return false;
}

Boolean read_LibInfo(struct ps_prochandle* ph) {
    char fname[32];
    char buf[256];
    FILE *file = NULL;

    sprintf(fname, "/proc/%d/maps", ph->pid);
    file = fopen(fname, "r");
    if (file == NULL) {
        debug_println("can't open /proc/%d/maps file", ph->pid);
        return false;
    }

    while(fgets_no_cr(buf, 256, file)) {
        char *word[6];
        int nwords = split_n_str(buf, 6, word, ' ', '\0');
        if (nwords > 5 && !find_lib(ph, word[5])) {
            intptr_t base;
            LibInfo* lib;
            sscanf(word[0], "%lx", &base);
            lib = add_LibInfo(ph, word[5], (uintptr_t) base);
            if (lib == NULL) {
                continue; // ignore, add_LibInfo prints error
            }
            
            // we don't need to keep the library open, symtab is already
            // built. Only for core dump we need to keep the fd open.
            close(lib->fd);
            lib->fd = -1;
        }
    }
    fclose(file);
    return true;
}

uintptr_t lookup_symbol(struct ps_prochandle* ph, const char* sym_name) {    
   LibInfo* lib = ph->libs;
   while (lib) {
      if (lib->symtab) {
         uintptr_t res = search_symbol(lib->symtab, lib->base, sym_name, NULL);
         if (res) return res;
      }
      lib = lib->next;
   }
   debug_println("lookup failed for symbol '%s'", sym_name);
   return (uintptr_t) NULL;
}

const char* symbol_for_pc(struct ps_prochandle* ph, uintptr_t addr, uintptr_t* poffset) {
   const char* res = NULL;
   LibInfo* lib = ph->libs;
   while (lib) {
      if (lib->symtab && addr >= lib->base) {
         res = nearest_symbol(lib->symtab, addr - lib->base, poffset);
         if (res) return res;
      }
      lib = lib->next;
   }
   return NULL;
}


// get number of shared objects 
int get_num_libs(struct ps_prochandle* ph) {
   return ph->num_libs;
}

// get name of n'th solib 
const char* get_lib_name(struct ps_prochandle* ph, int index) {
   int count = 0;
   LibInfo* lib = ph->libs;
   while (lib) {
      if (count == index) {
         return lib->name;
      }
      count++;
      lib = lib->next;
   }
   return NULL;
}

// get base address of a lib
uintptr_t get_lib_base(struct ps_prochandle* ph, int index) {
   int count = 0;
   LibInfo* lib = ph->libs;
   while (lib) {
      if (count == index) {
         return lib->base;
      }
      count++;
      lib = lib->next;
   }
   return (uintptr_t) NULL;
}

