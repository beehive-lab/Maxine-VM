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
#include "debug.h"

static Boolean _logging = false;

int	_proc_Lwait(const char* func, int line, struct ps_lwphandle *lh, uint_t timeout) {
	if (_logging) {
		debug_println("%s:%d Lwait(0x%lx, %lu)", func, line, (Address) lh, timeout);
	}
	return Lwait(lh, timeout);
}

ssize_t _proc_Pread(const char *func, int line, struct ps_prochandle *ph, void *dst, size_t size, uintptr_t src) {
	if (_logging) {
		debug_println("%s:%d Pread(ph=0x%lx, dst=0x%lx, size=%d, src=0x%lx)", func, line, (Address) ph, (Address) dst, size, (Address) src);
	}
	return Pread(ph, dst, size, src);
}

ssize_t _proc_Pwrite(const char *func, int line, struct ps_prochandle *ph, const void *src, size_t size, uintptr_t dst) {
	if (_logging) {
		debug_println("%s:%d Pwrite(ph=0x%lx, src=0x%lx, size=%d, dst=0x%lx)", func, line, (Address) ph, (Address) src, size, (Address) dst);
	}
	return Pwrite(ph, src, size, dst);
}

void _proc_Lsync(const char* func, int line, struct ps_lwphandle *lh) {
	if (_logging) {
		debug_println("%s:%d Lsync(0x%lx)", func, line, (Address) lh);
	}
	Lsync(lh);
}

struct ps_lwphandle *_proc_Lgrab(const char* func, int line, struct ps_prochandle *lh, lwpid_t lwpId, int *error) {
	if (_logging) {
		debug_println("%s:%d Lgrab(0x%lx, %lu, 0x%lx)", func, line, (Address) lh, lwpId, (Address) error);
	}
	return Lgrab(lh, lwpId, error);
}

struct ps_prochandle *_proc_Pcreate(const char* func, int line, const char *arg0, char *const *argv, int *error, char *path, size_t pathLength) {
	if (_logging) {
		debug_println("%s:%d Pcreate(%s, 0x%lx, 0x%lx, 0x%lx, %d)", func, line, arg0, argv, (Address) error, path, pathLength);
	}
	return Pcreate(arg0, argv, error, path, pathLength);
}

int	_proc_Lsetrun(const char* func, int line, struct ps_lwphandle *lh, int sig, int flags) {
	if (_logging) {
		debug_println("%s:%d Lsetrun(0x%lx, %d, %d)", func, line, (Address) lh, sig, flags);
	}
	return Lsetrun(lh, sig, flags);
}

const pstatus_t *_proc_Pstatus(const char* func, int line, struct ps_prochandle *ph) {
	if (_logging) {
		debug_println("%s:%d Pstatus(0x%lx)", func, line, (Address) ph);
	}
	return Pstatus(ph);
}

int _proc_Pstate(const char* func, int line, struct ps_prochandle *ph) {
	if (_logging) {
		debug_println("%s:%d Pstate(0x%lx)", func, line, (Address) ph);
	}
	return Pstate(ph);
}

void _proc_Psync(const char* func, int line, struct ps_prochandle *ph) {
	if (_logging) {
		debug_println("%s:%d Psync(0x%lx)", func, line, (Address) ph);
	}
	Psync(ph);
}

int _proc_Pmapping_iter(const char* func, int line, struct ps_prochandle *ph, proc_map_f *f, void *cd) {
	if (_logging) {
		debug_println("%s:%d Pmapping_iter(0x%lx, 0x%lx, 0x%lx)", func, line, (Address) ph, (Address) f, (Address) cd);
	}
	return Pmapping_iter(ph, f, cd);
}

void _proc_Pupdate_maps(const char* func, int line, struct ps_prochandle *ph) {
	if (_logging) {
		debug_println("%s:%d Pupdate_maps(0x%lx)", func, line, (Address) ph);
	}
	Pupdate_maps(ph);
}

int _proc_Psetrun(const char* func, int line, struct ps_prochandle *ph, int sig, int flags) {
	if (_logging) {
		debug_println("%s:%d Psetrun(0x%lx, %d, %d)", func, line, (Address) ph, sig, flags);
	}
	return Psetrun(ph, sig, flags);
}

int _proc_Pwait(const char* func, int line, struct ps_prochandle *ph, int msec) {
	if (_logging) {
		debug_println("%s:%d Pwait(0x%lx, %d)", func, line, (Address) ph, msec);
	}
	return Pwait(ph, msec);
}

void _proc_Psetsysentry(const char *func, int line, struct ps_prochandle *ph, const sysset_t *set) {
	if (_logging) {
		debug_println("%s:%d Psetsysentry(0x%lx, 0x%lx)", func, line, (Address) ph, (Address) set);
	}
	Psetsysentry(ph, set);
}

void _proc_Psetsysexit(const char *func, int line, struct ps_prochandle *ph, const sysset_t *set) {
	if (_logging) {
		debug_println("%s:%d Psetsysexit(0x%lx, 0x%lx)", func, line, (Address) ph, (Address) set);
	}
	Psetsysexit(ph, set);
}

int	_proc_Lstack(const char* func, int line, struct ps_lwphandle *lh, stack_t *stack) {
	if (_logging) {
		debug_println("%s:%d Lstack(0x%lx, 0x%lx)", func, line, (Address) lh, stack);
	}
	return Lstack(lh, stack);
}

int	_proc_Lmain_stack(const char* func, int line, struct ps_lwphandle *lh, stack_t *stack) {
	if (_logging) {
		debug_println("%s:%d Lmain_stack(0x%lx, 0x%lx)", func, line, (Address) lh, stack);
	}
	return Lmain_stack(lh, stack);
}

int	_proc_Lalt_stack(const char* func, int line, struct ps_lwphandle *lh, stack_t *stack) {
	if (_logging) {
		debug_println("%s:%d Lalt_stack(0x%lx, 0x%lx)", func, line, (Address) lh, stack);
	}
	return Lalt_stack(lh, stack);
}

int	_proc_Lgetareg(const char* func, int line, struct ps_lwphandle *lh, int index, prgreg_t *result) {
	if (_logging) {
		debug_println("%s:%d Lgetareg(0x%lx, %d, 0x%lx)", func, line, (Address) lh, index, result);
	}
	return Lgetareg(lh, index, result);
}

int	_proc_Lputareg(const char* func, int line, struct ps_lwphandle *lh, int index, prgreg_t value) {
	if (_logging) {
		debug_println("%s:%d Lputareg(0x%lx, %d, %lu)", func, line, (Address) lh, index, value);
	}
	return Lputareg(lh, index, value);
}

void _proc_Lfree(const char* func, int line, struct ps_lwphandle *lh) {
	if (_logging) {
		debug_println("%s:%d Lfree(0x%lx)", func, line, (Address) lh);
	}
	Lfree(lh);
}

int	_proc_Lclearfault(const char* func, int line, struct ps_lwphandle *lh) {
	if (_logging) {
		debug_println("%s:%d Lclearfault(0x%lx)", func, line, (Address) lh);
	}
	return Lclearfault(lh);
}

int _proc_Plwp_getregs(const char* func, int line, struct ps_prochandle *ph, lwpid_t lwpId, prgregset_t registers) {
	if (_logging) {
		debug_println("%s:%d Plwp_getregs(0x%lx, %lu, 0x%lx)", func, line, (Address) ph, lwpId, registers);
	}
	return Plwp_getregs(ph, lwpId, registers);
}

void debug_printStatusFlags(const char *prefix, int pr_flags, const char *suffix) {
	if (prefix != NULL) {
		debug_print(prefix);
	}
	if (pr_flags & PR_STOPPED) {
		/* lwp is stopped */
		debug_print("PR_STOPPED ");
	}
	if (pr_flags & PR_ISTOP) {
        /* lwp is stopped on an event of interest */
        debug_print("PR_ISTOP ");
    }
	if (pr_flags & PR_DSTOP) {
        /* lwp has a stop directive in effect */
        debug_print("PR_DSTOP ");
    }
	if (pr_flags & PR_STEP) {
        /* lwp has a single-step directive in effect */
        debug_print("PR_STEP ");
    }
	if (pr_flags & PR_ASLEEP) {
        /* lwp is sleeping in a system call */
        debug_print("PR_ASLEEP ");
    }
	if (pr_flags & PR_PCINVAL) {
        /* contents of pr_instr undefined */
        debug_print("PR_PCINVAL ");
    }
	if (pr_flags & PR_ASLWP) {
        /* obsolete flag; never set */
        debug_print("PR_ASLWP ");
    }
	if (pr_flags & PR_AGENT) {
        /* this lwp is the /proc agent lwp */
        debug_print("PR_AGENT ");
    }
	if (pr_flags & PR_DETACH) {
        /* this is a detached lwp */
        debug_print("PR_DETACH ");
    }
	if (pr_flags & PR_DAEMON) {
        /* this is a daemon lwp */
        debug_print("PR_DAEMON ");
    }
	/* The following flags apply to the process, not to an individual lwp */
	if (pr_flags & PR_ISSYS) {
        /* this is a system process */
        debug_print("PR_ISSYS ");
    }
	if (pr_flags & PR_VFORKP) {
        /* process is the parent of a vfork()d child */
        debug_print("PR_VFORKP ");
    }
	if (pr_flags & PR_ORPHAN) {
        /* process's process group is orphaned */
        debug_print("PR_ORPHAN ");
    }
	/* The following process flags are modes settable by PCSET/PCUNSET */
	if (pr_flags & PR_FORK) {
        /* inherit-on-fork is in effect */
        debug_print("PR_FORK ");
    }
	if (pr_flags & PR_RLC) {
        /* run-on-last-close is in effect */
        debug_print("PR_RLC ");
    }
	if (pr_flags & PR_KLC) {
        /* kill-on-last-close is in effect */
        debug_print("PR_KLC ");
    }
	if (pr_flags & PR_ASYNC) {
        /* asynchronous-stop is in effect */
        debug_print("PR_ASYNC ");
    }
	if (pr_flags & PR_MSACCT) {
        /* micro-state usage accounting is in effect */
        debug_print("PR_MSACCT ");
    }
	if (pr_flags & PR_BPTADJ) {
        /* breakpoint trap pc adjustment is in effect */
        debug_print("PR_BPTADJ ");
    }
	if (pr_flags & PR_PTRACE) {
        /* ptrace-compatibility mode is in effect */
        debug_print("PR_PTRACE ");
    }
	if (pr_flags & PR_MSFORK) {
        /* micro-state accounting inherited on fork */
        debug_print("PR_MSFORK ");
    }
	if (pr_flags & PR_IDLE) {
        /* lwp is a cpu's idle thread */
        debug_print("PR_IDLE ");
    }
    if (suffix != NULL) {
		debug_print(suffix);
	}
}

void debug_printWhyStopped(const char * prefix, lwpstatus_t *lwpStatus, const char * suffix) {
	int nameLength = SYS2STR_MAX > FLT2STR_MAX ? SYS2STR_MAX : FLT2STR_MAX;
    char name[nameLength];
    uint32_t bits;

    if (prefix != NULL) {
		debug_print(prefix);
	}

    switch (lwpStatus->pr_why) {
        case PR_REQUESTED:
            debug_print("PR_REQUESTED");
            break;
        case PR_SIGNALLED:
            debug_print("PR_SIGNALLED [%s]", proc_signame(lwpStatus->pr_what, name, sizeof(name)));
            break;
        case PR_FAULTED:
            debug_print("PR_FAULTED [%s]", proc_fltname(lwpStatus->pr_what, name, sizeof(name)));
            break;
        case PR_SYSENTRY:
            debug_print("PR_SYSENTRY [%s]", proc_sysname(lwpStatus->pr_what, name, sizeof(name)));
            break;
        case PR_SYSEXIT:
            debug_print("PR_SYSEXIT [%s]", proc_sysname(lwpStatus->pr_what, name, sizeof(name)));
            break;
        case PR_JOBCONTROL:
            debug_print("PR_JOBCONTROL [%s]", proc_signame(lwpStatus->pr_what, name, sizeof(name)));
            break;
        case PR_SUSPENDED:
            debug_print("PR_SUSPENDED");
            break;
    }

    if (lwpStatus->pr_cursig)
        debug_print(" current signal: %d", lwpStatus->pr_cursig);

    bits = *((uint32_t *)&lwpStatus->pr_lwppend);
    if (bits) {
        debug_print(" pending signals: 0x%.8X", bits);
    }
    if (suffix != NULL) {
		debug_print(suffix);
	}
}
