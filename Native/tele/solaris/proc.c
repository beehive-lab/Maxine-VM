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

static void* _lastCall = 0;

#define proc_call(name, argsFormat, ...) do {\
    boolean trace = log_TELE && ((void *) name != (void *) Pread || _lastCall != (void *) Pread); \
    if (trace) { \
        log_println("%s:%d: %s(" argsFormat ")", file, line, STRINGIZE(name), ##__VA_ARGS__); \
    } \
    _lastCall = (void *) name; \
    return name(##__VA_ARGS__); \
} while(0)

#define proc_call_void(name, argsFormat, ...) do {\
    boolean trace = log_TELE && ((void *) name != (void *) Pread || _lastCall != (void *) Pread); \
    if (trace) { \
        log_println("%s:%d: %s(" argsFormat ")", file, line, STRINGIZE(name), ##__VA_ARGS__); \
    } \
    _lastCall = (void *) name; \
    name(##__VA_ARGS__); \
} while(0)

int	_proc_Lwait(POS_PARAMS, struct ps_lwphandle *lh, uint_t timeout) {
    proc_call(Lwait, "%p, %lu", lh, timeout);
}

ssize_t _proc_Pread(POS_PARAMS, struct ps_prochandle *ph, void *dst, size_t size, uintptr_t src) {
    proc_call(Pread, "ph=%p, dst=%p, size=%d, src=%p", ph, dst, size, src);
}

ssize_t _proc_Pwrite(POS_PARAMS, struct ps_prochandle *ph, const void *src, size_t size, uintptr_t dst) {
    proc_call(Pwrite, "ph=%p, src=%p, size=%d, dst=%p", ph, src, size, dst);
}

void _proc_Lsync(POS_PARAMS, struct ps_lwphandle *lh) {
    proc_call_void(Lsync, "%p", lh);
}

struct ps_lwphandle *_proc_Lgrab(POS_PARAMS, struct ps_prochandle *lh, lwpid_t lwpId, int *error) {
    proc_call(Lgrab, "%p, %lu, %p", lh, lwpId, error);
}

struct ps_prochandle *_proc_Pcreate(POS_PARAMS, const char *arg0, char *const *argv, int *error, char *path, size_t pathLength) {
    proc_call(Pcreate, "%s, %p, %p, %p, %d", arg0, argv, error, path, pathLength);
}

int	_proc_Lsetrun(POS_PARAMS, struct ps_lwphandle *lh, int sig, int flags) {
    proc_call(Lsetrun, "%p, %d, %d", lh, sig, flags);
}

const pstatus_t *_proc_Pstatus(POS_PARAMS, struct ps_prochandle *ph) {
    proc_call(Pstatus, "%p", ph);
}

int _proc_Pstate(POS_PARAMS, struct ps_prochandle *ph) {
    proc_call(Pstate, "%p", ph);
}

void _proc_Psync(POS_PARAMS, struct ps_prochandle *ph) {
    proc_call_void(Psync, "%p", ph);
}

int _proc_Pmapping_iter(POS_PARAMS, struct ps_prochandle *ph, proc_map_f *f, void *cd) {
    proc_call(Pmapping_iter, "%p, %p, %p", ph, f, cd);
}

void _proc_Pupdate_maps(POS_PARAMS, struct ps_prochandle *ph) {
    proc_call_void(Pupdate_maps, "%p", ph);
}

int _proc_Psetrun(POS_PARAMS, struct ps_prochandle *ph, int sig, int flags) {
    proc_call(Psetrun, "%p, %d, %d", ph, sig, flags);
}

int _proc_Pwait(POS_PARAMS, struct ps_prochandle *ph, int msec) {
    proc_call(Pwait, "%p, %d", ph, msec);
}

void _proc_Psetsysentry(POS_PARAMS, struct ps_prochandle *ph, const sysset_t *set) {
    proc_call_void(Psetsysentry, "%p, %p", ph, set);
}

void _proc_Psetsysexit(POS_PARAMS, struct ps_prochandle *ph, const sysset_t *set) {
    proc_call_void(Psetsysexit, "%p, %p", ph, set);
}

int	_proc_Lstack(POS_PARAMS, struct ps_lwphandle *lh, stack_t *stack) {
    proc_call(Lstack, "%p, %p", lh, stack);
}

int	_proc_Lmain_stack(POS_PARAMS, struct ps_lwphandle *lh, stack_t *stack) {
    proc_call(Lmain_stack, "%p, %p", lh, stack);
}

int	_proc_Lalt_stack(POS_PARAMS, struct ps_lwphandle *lh, stack_t *stack) {
    proc_call(Lalt_stack, "%p, %p", lh, stack);
}

int	_proc_Lgetareg(POS_PARAMS, struct ps_lwphandle *lh, int index, prgreg_t *result) {
    proc_call(Lgetareg, "%p, %d, %p", lh, index, result);
}

int	_proc_Lputareg(POS_PARAMS, struct ps_lwphandle *lh, int index, prgreg_t value) {
    proc_call(Lputareg, "%p, %d, %lu", lh, index, value);
}

void _proc_Lfree(POS_PARAMS, struct ps_lwphandle *lh) {
    proc_call_void(Lfree, "%p", lh);
}

int	_proc_Lclearfault(POS_PARAMS, struct ps_lwphandle *lh) {
    proc_call(Lclearfault, "%p", lh);
}

int _proc_Plwp_getregs(POS_PARAMS, struct ps_prochandle *ph, lwpid_t lwpId, prgregset_t registers) {
    proc_call(Plwp_getregs, "%p, %lu, %p", ph, lwpId, registers);
}

void log_printStatusFlags(const char *prefix, int pr_flags, const char *suffix) {
	if (prefix != NULL) {
		log_print(prefix);
	}
	if (pr_flags & PR_STOPPED) {
		/* lwp is stopped */
		log_print("PR_STOPPED ");
	}
	if (pr_flags & PR_ISTOP) {
        /* lwp is stopped on an event of interest */
        log_print("PR_ISTOP ");
    }
	if (pr_flags & PR_DSTOP) {
        /* lwp has a stop directive in effect */
        log_print("PR_DSTOP ");
    }
	if (pr_flags & PR_STEP) {
        /* lwp has a single-step directive in effect */
        log_print("PR_STEP ");
    }
	if (pr_flags & PR_ASLEEP) {
        /* lwp is sleeping in a system call */
        log_print("PR_ASLEEP ");
    }
	if (pr_flags & PR_PCINVAL) {
        /* contents of pr_instr undefined */
        log_print("PR_PCINVAL ");
    }
	if (pr_flags & PR_ASLWP) {
        /* obsolete flag; never set */
        log_print("PR_ASLWP ");
    }
	if (pr_flags & PR_AGENT) {
        /* this lwp is the /proc agent lwp */
        log_print("PR_AGENT ");
    }
	if (pr_flags & PR_DETACH) {
        /* this is a detached lwp */
        log_print("PR_DETACH ");
    }
	if (pr_flags & PR_DAEMON) {
        /* this is a daemon lwp */
        log_print("PR_DAEMON ");
    }
	/* The following flags apply to the process, not to an individual lwp */
	if (pr_flags & PR_ISSYS) {
        /* this is a system process */
        log_print("PR_ISSYS ");
    }
	if (pr_flags & PR_VFORKP) {
        /* process is the parent of a vfork()d child */
        log_print("PR_VFORKP ");
    }
	if (pr_flags & PR_ORPHAN) {
        /* process's process group is orphaned */
        log_print("PR_ORPHAN ");
    }
	/* The following process flags are modes settable by PCSET/PCUNSET */
	if (pr_flags & PR_FORK) {
        /* inherit-on-fork is in effect */
        log_print("PR_FORK ");
    }
	if (pr_flags & PR_RLC) {
        /* run-on-last-close is in effect */
        log_print("PR_RLC ");
    }
	if (pr_flags & PR_KLC) {
        /* kill-on-last-close is in effect */
        log_print("PR_KLC ");
    }
	if (pr_flags & PR_ASYNC) {
        /* asynchronous-stop is in effect */
        log_print("PR_ASYNC ");
    }
	if (pr_flags & PR_MSACCT) {
        /* micro-state usage accounting is in effect */
        log_print("PR_MSACCT ");
    }
	if (pr_flags & PR_BPTADJ) {
        /* breakpoint trap pc adjustment is in effect */
        log_print("PR_BPTADJ ");
    }
	if (pr_flags & PR_PTRACE) {
        /* ptrace-compatibility mode is in effect */
        log_print("PR_PTRACE ");
    }
	if (pr_flags & PR_MSFORK) {
        /* micro-state accounting inherited on fork */
        log_print("PR_MSFORK ");
    }
	if (pr_flags & PR_IDLE) {
        /* lwp is a cpu's idle thread */
        log_print("PR_IDLE ");
    }
    if (suffix != NULL) {
		log_print(suffix);
	}
}

void log_printWhyStopped(const char * prefix, const lwpstatus_t *lwpStatus, const char * suffix) {
	int nameLength = SYS2STR_MAX > FLT2STR_MAX ? SYS2STR_MAX : FLT2STR_MAX;
    char name[nameLength];
    uint32_t bits;

    if (prefix != NULL) {
		log_print(prefix);
	}

    switch (lwpStatus->pr_why) {
        case PR_REQUESTED:
            log_print("PR_REQUESTED");
            break;
        case PR_SIGNALLED:
            log_print("PR_SIGNALLED [%s]", proc_signame(lwpStatus->pr_what, name, sizeof(name)));
            break;
        case PR_FAULTED:
            log_print("PR_FAULTED [%s]", proc_fltname(lwpStatus->pr_what, name, sizeof(name)));
            break;
        case PR_SYSENTRY:
            log_print("PR_SYSENTRY [%s]", proc_sysname(lwpStatus->pr_what, name, sizeof(name)));
            break;
        case PR_SYSEXIT:
            log_print("PR_SYSEXIT [%s]", proc_sysname(lwpStatus->pr_what, name, sizeof(name)));
            break;
        case PR_JOBCONTROL:
            log_print("PR_JOBCONTROL [%s]", proc_signame(lwpStatus->pr_what, name, sizeof(name)));
            break;
        case PR_SUSPENDED:
            log_print("PR_SUSPENDED");
            break;
    }

    if (lwpStatus->pr_cursig)
        log_print(" current signal: %d", lwpStatus->pr_cursig);

    bits = *((uint32_t *)&lwpStatus->pr_lwppend);
    if (bits) {
        log_print(" pending signals: 0x%.8X", bits);
    }
    if (suffix != NULL) {
		log_print(suffix);
	}
}
