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
#ifndef _proc_service_h_
#define _proc_service_h_ 1
/* 
 * Most of this file is copied from Solaris "proc_service.h"
 * (via the HotSpot SA's version of this).
 * 
 * Linux does not have the proc service library, although it does provide the
 * thread_db library which can be used to manipulate threads without having
 * to know the details of LinuxThreads or NPTL.
 */

#include <stdio.h>
#include <thread_db.h>
#include <unistd.h>

/*
 * Defined by the thread_db client, i.e. us.
 */ 
struct ps_prochandle {
    pid_t pid;
    int num_libs;
    LibInfo *libs;
    LibInfo *lib_tail;
};

typedef enum {
        PS_OK,          /* generic "call succeeded" */
        PS_ERR,         /* generic error */
        PS_BADPID,      /* bad process handle */
        PS_BADLID,      /* bad lwp identifier */
        PS_BADADDR,     /* bad address */
        PS_NOSYM,       /* p_lookup() could not find given symbol */
        PS_NOFREGS      /* FPU register set not available for given lwp */
} ps_err_e;

extern pid_t ps_getpid (struct ps_prochandle *ph);

extern ps_err_e ps_pstop(struct ps_prochandle *ph);

extern ps_err_e ps_pcontinue(struct ps_prochandle *ph);

extern ps_err_e ps_lstop(struct ps_prochandle *ph, lwpid_t lwpid);

extern ps_err_e ps_lcontinue(struct ps_prochandle *ph, lwpid_t lwpid);

extern ps_err_e ps_pglobal_lookup(struct ps_prochandle *ph, const char *objectName, const char *symbolName, psaddr_t *symbolAddress);

extern ps_err_e ps_pdread(struct ps_prochandle *ph, psaddr_t address, void *buffer, size_t size);

extern ps_err_e ps_pdwrite(struct ps_prochandle *ph, psaddr_t address, const void *buffer, size_t size);

extern ps_err_e ps_lsetfpregs(struct ps_prochandle *ph, lwpid_t lwpid, const prfpregset_t *fpRegisters);

extern ps_err_e ps_lsetregs(struct ps_prochandle *ph, lwpid_t lwpid, const prgregset_t gRegisters);
  
extern ps_err_e  ps_lgetfpregs(struct ps_prochandle  *ph,  lwpid_t lwpid, prfpregset_t *fpRegisters);

extern ps_err_e ps_lgetregs(struct ps_prochandle *ph, lwpid_t lwpid, prgregset_t gRegisters);

extern ps_err_e ps_lgetxregsize (struct ps_prochandle *ph, lwpid_t lwpid, int *xregsize);

extern ps_err_e ps_lgetxregs(struct ps_prochandle *ph, lwpid_t lwpid, caddr_t xregset);

extern ps_err_e ps_lsetxregs(struct ps_prochandle *ph, lwpid_t lwpid, caddr_t xregset);

extern void ps_plog (const char *format, ...);

// new libthread_db of NPTL seems to require this symbol
extern ps_err_e ps_get_thread_area();

#endif /*_proc_service_h_*/
