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
/**
 * Implementation of libproc function hooks for tracing calls to said functions.
 *
 * @author Doug Simon
 */
#include <stdio.h>

#include "proc.h"
#include "word.h"
#include "log.h"

void log_flags(const char *prefix, int pr_flags, const char *suffix) {
    if (prefix != NULL) {
        log_print(prefix);
    }
#define MATCH_FLAG(id, name) if (pr_flags & id) log_print(" " name)
    MATCH_FLAG(PR_STOPPED, "PR_STOPPED");
    MATCH_FLAG(PR_ISTOP, "PR_ISTOP");
    MATCH_FLAG(PR_DSTOP, "PR_DSTOP");
    MATCH_FLAG(PR_STEP, "PR_STEP");
    MATCH_FLAG(PR_ASLEEP, "PR_ASLEEP");
    MATCH_FLAG(PR_PCINVAL, "PR_PCINVAL");
    MATCH_FLAG(PR_ASLWP, "PR_ASLWP");
    MATCH_FLAG(PR_AGENT, "PR_AGENT");
    MATCH_FLAG(PR_DETACH, "PR_DETACH");
    MATCH_FLAG(PR_DAEMON, "PR_DAEMON");
    MATCH_FLAG(PR_IDLE, "PR_IDLE");
/* The following flags apply to the process, not to an individual lwp */
    MATCH_FLAG(PR_ISSYS, "PR_ISSYS");
    MATCH_FLAG(PR_VFORKP, "PR_VFORKP");
    MATCH_FLAG(PR_ORPHAN, "PR_ORPHAN");
    MATCH_FLAG(PR_NOSIGCHLD, "PR_NOSIGCHLD");
    MATCH_FLAG(PR_WAITPID, "PR_WAITPID");
/* The following process flags are modes settable by PCSET/PCUNSET */
    MATCH_FLAG(PR_FORK, "PR_FORK");
    MATCH_FLAG(PR_RLC, "PR_RLC");
    MATCH_FLAG(PR_KLC, "PR_KLC");
    MATCH_FLAG(PR_ASYNC, "PR_ASYNC");
    MATCH_FLAG(PR_MSACCT, "PR_MSACCT");
    MATCH_FLAG(PR_BPTADJ, "PR_BPTADJ");
    MATCH_FLAG(PR_PTRACE, "PR_PTRACE");
    MATCH_FLAG(PR_MSFORK, "PR_MSFORK");
#undef MATCH_FLAG
    if (suffix != NULL) {
        log_print(suffix);
    }
}

void log_printWhyStopped(const char * prefix, const lwpstatus_t *ls, const char * suffix) {
	int nameLength = SYS2STR_MAX > FLT2STR_MAX ? SYS2STR_MAX : FLT2STR_MAX;
    char name[nameLength];
    uint32_t bits;

    if (prefix != NULL) {
		log_print(prefix);
	}

    switch (ls->pr_why) {
        case PR_REQUESTED:
            log_print("PR_REQUESTED");
            break;
        case PR_SIGNALLED:
            log_print("PR_SIGNALLED [%s]", proc_signame(ls->pr_what, name, sizeof(name)));
            break;
        case PR_FAULTED:
            log_print("PR_FAULTED [%s]", proc_fltname(ls->pr_what, name, sizeof(name)));
            break;
        case PR_SYSENTRY:
            log_print("PR_SYSENTRY [%s]", proc_sysname(ls->pr_what, name, sizeof(name)));
            break;
        case PR_SYSEXIT:
            log_print("PR_SYSEXIT [%s]", proc_sysname(ls->pr_what, name, sizeof(name)));
            break;
        case PR_JOBCONTROL:
            log_print("PR_JOBCONTROL [%s]", proc_signame(ls->pr_what, name, sizeof(name)));
            break;
        case PR_SUSPENDED:
            log_print("PR_SUSPENDED");
            break;
    }

    if (ls->pr_cursig)
        log_print(" current signal: %d", ls->pr_cursig);

    bits = *((uint32_t *)&ls->pr_lwppend);
    if (bits) {
        log_print(" pending signals: 0x%.8X", bits);
    }
    if (suffix != NULL) {
		log_print(suffix);
	}
}

static void print_lwpstatus(const lwpstatus_t *ls) {
    log_println("    pr_flags (flags): %d", ls->pr_flags);
    log_flags("      ", ls->pr_flags, "\n");
    log_println("    pr_lwpid (specific lwp identifier): %d", ls->pr_lwpid);
    log_println("    pr_why (reason for lwp stop, if stopped): %d", ls->pr_why);
    log_println("    pr_what (more detailed reason): %d", ls->pr_what);
    log_printWhyStopped("      ", ls, "\n");
    log_println("    pr_cursig (current signal, if any): %d", ls->pr_cursig);
    log_println("    pr_info (info associated with signal or fault): %d %d %d", ls->pr_info.si_signo, ls->pr_info.si_code, ls->pr_info.si_errno);
    log_println("    pr_lwppend (set of signals pending to the lwp): %d %d %d %d", ls->pr_lwppend.__sigbits[0], ls->pr_lwppend.__sigbits[1], ls->pr_lwppend.__sigbits[2], ls->pr_lwppend.__sigbits[3]);
    log_println("    pr_lwphold (set of signals blocked by the lwp): %d %d %d %d", ls->pr_lwphold.__sigbits[0], ls->pr_lwphold.__sigbits[1], ls->pr_lwphold.__sigbits[2], ls->pr_lwphold.__sigbits[3]);
}

static void print_pstatus(const pstatus_t *ps) {
    log_println("  pr_flags (flags): %d", ps->pr_flags);
    log_flags("      ", ps->pr_flags, "\n");
    log_println("  pr_nlwp (number of active lwps in the process): %d", ps->pr_nlwp);
    log_println("  pr_pid (process id): %d", ps->pr_pid);
    log_println("  pr_ppid (parent process id): %d", ps->pr_ppid);
    log_println("  pr_pgid (process group id): %d");
    log_println("  pr_sid (session id): %d", ps->pr_sid);
    log_println("  pr_agentid (wp id of the /proc agent lwp, if any): %d", ps->pr_agentid);
    log_println("  pr_sigpend (set of process pending signals): %d %d %d %d", ps->pr_sigpend.__sigbits[0], ps->pr_sigpend.__sigbits[1], ps->pr_sigpend.__sigbits[2], ps->pr_sigpend.__sigbits[3]);
    log_println("  pr_sigtrace (set of traced signals): %d %d %d %d", ps->pr_sigtrace.__sigbits[0], ps->pr_sigtrace.__sigbits[1], ps->pr_sigtrace.__sigbits[2], ps->pr_sigtrace.__sigbits[3]);
    log_println("  pr_flttrace (set of traced faults): %d %d %d %d", ps->pr_flttrace.word[0], ps->pr_flttrace.word[1], ps->pr_flttrace.word[2], ps->pr_flttrace.word[3]);
    log_println("  pr_nzomb (number of zombie lwps in the process): %d", ps->pr_nzomb);
    log_println("  pr_lwp (representative lwp): %d", ps->pr_lwp);
}

static void print_lwphandle(struct ps_lwphandle *lh) {
    const lwpstatus_t *ls = Lstatus(lh);
    log_println("  LWP %d:", ls->pr_lwpid);
    log_println("    lwp_state (state of the lwp): %d", Lstate(lh));
    print_lwpstatus(ls);
}

static int print_lwp(void *data, const lwpstatus_t *lwpStatus) {
    struct ps_prochandle *ph = (struct ps_prochandle *) data;
    struct ps_lwphandle *lh;
    int error;
    lh = Lgrab(ph, lwpStatus->pr_lwpid, &error);
    if (error != 0) {
        log_println("Lgrab failed: %s", Lgrab_error(error)); \
        log_println("error grabbing handle for thread %d: %s", lwpStatus->pr_lwpid, Lgrab_error(error));
        return error;
    }
    print_lwphandle(lh);
    Lfree(lh);
}

void log_process(struct ps_prochandle *ph) {
    const pstatus_t *ps = Pstatus(ph);
    log_println("PROCESS %d:", ps->pr_pid);
    log_println("  state: %d", Pstate(ph));
    print_pstatus(ps);

    Plwp_iter(ph, print_lwp, ph);
}
