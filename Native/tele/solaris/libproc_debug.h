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
/*
 * @author Hannes Payer
 */

/**
 * Structs copied out of Pcontrol.h libproc header file. For debugging purpose.
 */

#ifndef LIBPROC_DEBUG_H
#define LIBPROC_DEBUG_H

#include <sys/types.h>
#include <sys/wait.h>
#include <synch.h>

typedef struct P_list {
      struct P_list   *list_forw;
      struct P_list   *list_back;
} plist_t;


typedef struct sym_tbl {    /* symbol table */
    Elf_Data *sym_data_pri; /* primary table */
    Elf_Data *sym_data_aux; /* auxiliary table */
    size_t  sym_symn_aux;   /* number of entries in auxiliary table */
    size_t  sym_symn;   /* total number of entries in both tables */
    char    *sym_strs;  /* ptr to strings */
    size_t  sym_strsz;  /* size of string table */
    GElf_Shdr sym_hdr_pri;  /* primary symbol table section header */
    GElf_Shdr sym_hdr_aux;  /* auxiliary symbol table section header */
    GElf_Shdr sym_strhdr;   /* string table section header */
    Elf *sym_elf;   /* faked-up ELF handle from core file */
    void    *sym_elfmem;    /* data for faked-up ELF handle */
    uint_t  *sym_byname;    /* symbols sorted by name */
    uint_t  *sym_byaddr;    /* symbols sorted by addr */
    size_t  sym_count;  /* number of symbols in each sorted list */
} sym_tbl_t;

typedef struct file_info {  /* symbol information for a mapped file */
    plist_t file_list;  /* linked list */
    char    file_pname[PRMAPSZ];    /* name from prmap_t */
    struct map_info *file_map;  /* primary (text) mapping */
    int file_ref;   /* references from map_info_t structures */
    int file_fd;    /* file descriptor for the mapped file */
    int file_init;  /* 0: initialization yet to be performed */
    GElf_Half file_etype;   /* ELF e_type from ehdr */
    GElf_Half file_class;   /* ELF e_ident[EI_CLASS] from ehdr */
    rd_loadobj_t *file_lo;  /* load object structure from rtld_db */
    char    *file_lname;    /* load object name from rtld_db */
    char    *file_lbase;    /* pointer to basename of file_lname */
    char    *file_rname;    /* resolved on-disk object pathname */
    char    *file_rbase;    /* pointer to basename of file_rname */
    Elf *file_elf;  /* ELF handle so we can close */
    void    *file_elfmem;   /* data for faked-up ELF handle */
    sym_tbl_t file_symtab;  /* symbol table */
    sym_tbl_t file_dynsym;  /* dynamic symbol table */
    uintptr_t file_dyn_base;    /* load address for ET_DYN files */
    uintptr_t file_plt_base;    /* base address for PLT */
    size_t  file_plt_size;  /* size of PLT region */
    uintptr_t file_jmp_rel; /* base address of PLT relocations */
    uintptr_t file_ctf_off; /* offset of CTF data in object file */
    size_t  file_ctf_size;  /* size of CTF data in object file */
    int file_ctf_dyn;   /* does the CTF data reference the dynsym */
    void    *file_ctf_buf;  /* CTF data for this file */
    ctf_file_t *file_ctfp;  /* CTF container for this file */
    char    *file_shstrs;   /* section header string table */
    size_t  file_shstrsz;   /* section header string table size */
    uintptr_t *file_saddrs; /* section header addresses */
    uint_t  file_nsaddrs;   /* number of section header addresses */
} file_info_t;

typedef struct map_info {   /* description of an address space mapping */
    prmap_t map_pmap;   /* /proc description of this mapping */
    file_info_t *map_file;  /* pointer into list of mapped files */
    off64_t map_offset; /* offset into core file (if core) */
    int map_relocate;   /* associated file_map needs to be relocated */
} map_info_t;

typedef struct lwp_info {   /* per-lwp information from core file */
    plist_t lwp_list;   /* linked list */
    lwpid_t lwp_id;     /* lwp identifier */
    lwpsinfo_t lwp_psinfo;  /* /proc/<pid>/lwp/<lwpid>/lwpsinfo data */
    lwpstatus_t lwp_status; /* /proc/<pid>/lwp/<lwpid>/lwpstatus data */
#if defined(sparc) || defined(__sparc)
    gwindows_t *lwp_gwins;  /* /proc/<pid>/lwp/<lwpid>/gwindows data */
    prxregset_t *lwp_xregs; /* /proc/<pid>/lwp/<lwpid>/xregs data */
    int64_t *lwp_asrs;  /* /proc/<pid>/lwp/<lwpid>/asrs data */
#endif
} lwp_info_t;

typedef struct core_info {  /* information specific to core files */
    char core_dmodel;   /* data model for core file */
    int core_errno;     /* error during initialization if != 0 */
    plist_t core_lwp_head;  /* head of list of lwp info */
    lwp_info_t *core_lwp;   /* current lwp information */
    uint_t core_nlwp;   /* number of lwp's in list */
    off64_t core_size;  /* size of core file in bytes */
    char *core_platform;    /* platform string from core file */
    struct utsname *core_uts;   /* uname(2) data from core file */
    prcred_t *core_cred;    /* process credential from core file */
    core_content_t core_content;    /* content dumped to core file */
    prpriv_t *core_priv;    /* process privileges from core file */
    size_t core_priv_size;  /* size of the privileges */
    void *core_privinfo;    /* system privileges info from core file */
    priv_impl_info_t *core_ppii;    /* NOTE entry for core_privinfo */
    char *core_zonename;    /* zone name from core file */
#if defined(__i386) || defined(__amd64)
    struct ssd *core_ldt;   /* LDT entries from core file */
    uint_t core_nldt;   /* number of LDT entries in core file */
#endif
} core_info_t;

typedef struct ps_rwops {   /* ops vector for Pread() and Pwrite() */
    ssize_t (*p_pread)(struct ps_prochandle *,
        void *, size_t, uintptr_t);
    ssize_t (*p_pwrite)(struct ps_prochandle *,
        const void *, size_t, uintptr_t);
} ps_rwops_t;

struct ps_prochandle {
    struct ps_lwphandle **hashtab;  /* hash table for LWPs (Lgrab()) */
    mutex_t proc_lock;  /* protects hash table; serializes Lgrab() */
    pstatus_t orig_status;  /* remembered status on Pgrab() */
    pstatus_t status;   /* status when stopped */
    psinfo_t psinfo;    /* psinfo_t from last Ppsinfo() request */
    uintptr_t sysaddr;  /* address of most recent syscall instruction */
    pid_t   pid;        /* process-ID */
    int state;      /* state of the process, see "libproc.h" */
    uint_t  flags;      /* see defines below */
    uint_t  agentcnt;   /* Pcreate_agent()/Pdestroy_agent() ref count */
    int asfd;       /* /proc/<pid>/as filedescriptor */
    int ctlfd;      /* /proc/<pid>/ctl filedescriptor */
    int statfd;     /* /proc/<pid>/status filedescriptor */
    int agentctlfd; /* /proc/<pid>/lwp/agent/ctl */
    int agentstatfd;    /* /proc/<pid>/lwp/agent/status */
    int info_valid; /* if zero, map and file info need updating */
    map_info_t *mappings;   /* cached process mappings */
    size_t  map_count;  /* number of mappings */
    size_t  map_alloc;  /* number of mappings allocated */
    uint_t  num_files;  /* number of file elements in file_info */
    plist_t file_head;  /* head of mapped files w/ symbol table info */
    char    *execname;  /* name of the executable file */
    auxv_t  *auxv;      /* the process's aux vector */
    int nauxv;      /* number of aux vector entries */
    rd_agent_t *rap;    /* cookie for rtld_db */
    map_info_t *map_exec;   /* the mapping for the executable file */
    map_info_t *map_ldso;   /* the mapping for ld.so.1 */
    const ps_rwops_t *ops;  /* pointer to ops-vector for read and write */
    core_info_t *core;  /* information specific to core (if PS_DEAD) */
    uintptr_t *ucaddrs; /* ucontext-list addresses */
    uint_t  ucnelems;   /* number of elements in the ucaddrs list */
    char    *zoneroot;  /* cached path to zone root */
};

struct ps_lwphandle {
    struct ps_prochandle *lwp_proc; /* process to which this lwp belongs */
    struct ps_lwphandle *lwp_hash;  /* hash table linked list */
    lwpstatus_t lwp_status; /* status when stopped */
    lwpsinfo_t  lwp_psinfo; /* lwpsinfo_t from last Lpsinfo() */
    lwpid_t     lwp_id;     /* lwp identifier */
    int     lwp_state;  /* state of the lwp, see "libproc.h" */
    uint_t      lwp_flags;  /* SETHOLD and/or SETREGS */
    int     lwp_ctlfd;  /* /proc/<pid>/lwp/<lwpid>/lwpctl */
    int     lwp_statfd; /* /proc/<pid>/lwp/<lwpid>/lwpstatus */
};


void print_lwpstatus(struct lwpstatus *status);

void print_pstatus(pstatus_t *status, char *name);

void print_lwphandle(struct ps_lwphandle *lwp, int i);

void print_ps_prochandle(struct ps_prochandle *ps);

void statloc_eval(int statloc);

#endif
