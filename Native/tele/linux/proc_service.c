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
#include <errno.h>
#include <stdarg.h>

#include "log.h"
#include "libInfo.h"
#include "ptrace.h"

#include "proc_service.h"

pid_t ps_getpid (struct ps_prochandle *ph)
{
    return ph->pid;
}

ps_err_e ps_pstop(struct ps_prochandle *ph) {
    /* The process is always stopped when under controll of the Inspector. */
    return PS_OK;
}

ps_err_e ps_pcontinue(struct ps_prochandle *ph) {
    /* The Inspector is supposed to control this behavior on its own. */
    return PS_OK;
}

ps_err_e ps_lstop(struct ps_prochandle *ph, lwpid_t lwpid) {
    /* This routine is allegedly never used by thread_db. */
    log_println("ps_lstop");
    return PS_ERR;
}

ps_err_e ps_lcontinue(struct ps_prochandle *ph, lwpid_t lwpid) {
    /* This routine is allegedly never used by thread_db. */
    log_println("ps_lcontinue");
    return PS_ERR;
}

ps_err_e ps_pglobal_lookup(struct ps_prochandle *ph, const char *objectName, const char *symbolName, psaddr_t *symbolAddress) {
    *symbolAddress = NULL;
    if (symbolName == NULL) {
        return PS_NOSYM;
    }
    *symbolAddress = (psaddr_t) lookup_symbol(ph, symbolName);
    return (*symbolAddress != NULL) ? PS_OK : PS_NOSYM;
}

static inline uintptr_t align(uintptr_t ptr, size_t size) {
    return (ptr & ~(size - 1));
}

ps_err_e ps_pdread(struct ps_prochandle *ph, psaddr_t addr, void *buffer, size_t size) {
    char *buf = (char *) buffer;
    long rslt;
    size_t i, words;
    uintptr_t uaddr = (uintptr_t) addr;
    uintptr_t end_addr = uaddr + size;
    uintptr_t aligned_addr = align(uaddr, sizeof(long));

    if (aligned_addr != uaddr) {
        char *ptr = (char *) &rslt;
        errno = 0;
        rslt = ptrace(PT_READ_D, ph->pid, (Address) aligned_addr, 0);
        if (errno) {
            log_println("ptrace(PTRACE_PEEKDATA, ..) failed for %d bytes @ %lx", size, addr);
            return PS_ERR;
        }
        for (; aligned_addr != uaddr; aligned_addr++, ptr++) {
        }
        for (; ((intptr_t)aligned_addr % sizeof(long)) && aligned_addr < end_addr; aligned_addr++) {
            *(buf++) = *(ptr++);
        }
    }

    words = (end_addr - aligned_addr) / sizeof(long);

    // assert((intptr_t)aligned_addr % sizeof(long) == 0);
    for (i = 0; i < words; i++) {
        errno = 0;
        rslt = ptrace(PT_READ_D, ph->pid, (Address) aligned_addr, 0);
        if (errno) {
            log_println("ptrace(PTRACE_PEEKDATA, ..) failed for %d bytes @ %lx", size, addr);
            return false;
        }
        *(long *)buf = rslt;
        buf += sizeof(long);
        aligned_addr += sizeof(long);
    }

    if (aligned_addr != end_addr) {
        char *ptr = (char *) &rslt;
        errno = 0;
        rslt = ptrace(PT_READ_D, ph->pid, (Address) aligned_addr, 0);
        if (errno) {
            log_println("ptrace(PTRACE_PEEKDATA, ..) failed for %d bytes @ %lx", size, addr);
            return false;
        }
        for (; aligned_addr != end_addr; aligned_addr++) {
            *(buf++) = *(ptr++);
        }
    }
    return PS_OK;
}

ps_err_e ps_pdwrite(struct ps_prochandle *ph, psaddr_t addr, const void *buf, size_t size) {
    log_println("ps_pdwrite");
    return PS_ERR;
}

ps_err_e ps_lsetfpregs(struct ps_prochandle *ph, lwpid_t lid, const prfpregset_t *fpregs) {
    log_println("ps_lsetfpregs");
    return PS_ERR;
}

ps_err_e ps_lsetregs(struct ps_prochandle *ph, lwpid_t lid, const prgregset_t gregset) {
    log_println("ps_lsetregs");
    return PS_ERR;
}

ps_err_e ps_lgetfpregs(struct  ps_prochandle  *ph,  lwpid_t lid, prfpregset_t *fpregs) {
    log_println("ps_lgetfpregs");
    return PS_ERR;
}

ps_err_e ps_lgetregs(struct ps_prochandle *ph, lwpid_t lid, prgregset_t gregset) {
    log_println("ps_lgetregs");
    return PS_ERR;
}

ps_err_e ps_lgetxregsize(struct ps_prochandle *ph, lwpid_t lwpid, int *xregsize) {
    log_println("ps_lgetxregsize");
    return PS_ERR;
}

ps_err_e ps_lgetxregs(struct ps_prochandle *ph, lwpid_t lwpid, caddr_t xregset) {
    log_println("ps_lgetxregs");
    return PS_ERR;
}

ps_err_e ps_lsetxregs(struct ps_prochandle *ph, lwpid_t lwpid, caddr_t xregset) {
    log_println("ps_lsetxregs");
    return PS_ERR;
}

void ps_plog (const char *format, ...) {
    va_list a;

    va_start(a, format);
    vfprintf(stderr, format, a);
}

ps_err_e ps_get_thread_area() {
    log_println("ps_get_thread_area");
    return PS_ERR;
}
