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
#include "libproc.h"

/**
 * Interposition for libproc function call tracing.
 */

#define POS_PARAMS const char *file, int line
#define POS __FILE__, __LINE__

extern struct ps_prochandle *_proc_Pcreate(POS_PARAMS, const char *arg0, char *const *argv, int *error, char *path, size_t pathLength);
#define proc_Pcreate(arg0, argv, error, path, pathLength) _proc_Pcreate(POS, arg0, argv, error, path, pathLength)

extern const pstatus_t *_proc_Pstatus(POS_PARAMS, struct ps_prochandle *ph);
#define proc_Pstatus(ph) _proc_Pstatus(POS, ph)

extern int _proc_Pstate(POS_PARAMS, struct ps_prochandle *ph);
#define proc_Pstate(ph) _proc_Pstate(POS, ph)

extern void _proc_Psync(const char* func, int line, struct ps_prochandle *ph);
#define proc_Psync(ph) _proc_Psync(POS, ph)

extern int _proc_Pmapping_iter(const char* func, int line, struct ps_prochandle *ph, proc_map_f *f, void *cd);
#define proc_Pmapping_iter(ph, f, cd) _proc_Pmapping_iter(POS, ph, f, cd)

extern void _proc_Pupdate_maps(const char* func, int line, struct ps_prochandle *ph);
#define proc_Pupdate_maps(ph) _proc_Pupdate_maps(POS, ph)

extern int _proc_Psetrun(const char* func, int line, struct ps_prochandle *ph, int sig, int flags);
#define proc_Psetrun(ph, sig, flags) _proc_Psetrun(POS, ph, sig, flags)

extern int _proc_Pwait(const char* func, int line, struct ps_prochandle *ph, int msec);
#define proc_Pwait(ph, msec) _proc_Pwait(POS, ph, msec)

extern void	_proc_Psetsysentry(POS_PARAMS, struct ps_prochandle *ph, const sysset_t *set);
#define proc_Psetsysentry(ph, set) _proc_Psetsysentry(POS, ph, set)

extern void _proc_Psetsysexit(POS_PARAMS, struct ps_prochandle *ph, const sysset_t *set);
#define proc_Psetsysexit(ph, set) _proc_Psetsysexit(POS, ph, set)

extern struct ps_lwphandle *_proc_Lgrab(POS_PARAMS, struct ps_prochandle *lh, lwpid_t lwpId, int *error);
#define proc_Lgrab(lh, lwpId, error) _proc_Lgrab(POS, lh, lwpId, error)

extern	void _proc_Lfree(POS_PARAMS, struct ps_lwphandle *lh);
#define proc_Lfree(lh) _proc_Lfree(POS, lh)

extern int	_proc_Lwait(POS_PARAMS, struct ps_lwphandle *lh, uint_t timeout);
#define	proc_Lwait(lh, timeout) _proc_Lwait(POS, lh, timeout)

extern int _proc_Plwp_getregs(POS_PARAMS, struct ps_prochandle *, lwpid_t lwpid, prgregset_t regs);
#define proc_Plwp_getregs(ph, lwpid, regs) _proc_Plwp_getregs(POS, ph, lwpid, regs)

extern	int	_proc_Lgetareg(POS_PARAMS, struct ps_lwphandle *lh, int index, prgreg_t *result);
#define	proc_Lgetareg(lh, index, result) _proc_Lgetareg(POS, lh, index, result)

extern	int	_proc_Lputareg(POS_PARAMS, struct ps_lwphandle *lh, int index, prgreg_t value);
#define	proc_Lputareg(lh, index, value) _proc_Lputareg(POS, lh, index, value)

extern	int	_proc_Lsetrun(POS_PARAMS, struct ps_lwphandle *lh, int sig, int flags);
#define	proc_Lsetrun(lh, sig, flags) _proc_Lsetrun(POS, lh, sig, flags)

extern	int	_proc_Lclearfault(POS_PARAMS, struct ps_lwphandle *lh);
#define	proc_Lclearfault(lh) _proc_Lclearfault(POS, lh)

extern ssize_t _proc_Pread(POS_PARAMS, struct ps_prochandle *ph, void *dst, size_t size, uintptr_t src);
#define proc_Pread(ph, dst, size, src) _proc_Pread(POS, ph, dst, size, src)

extern ssize_t _proc_Pwrite(POS_PARAMS, struct ps_prochandle *ph, const void *src, size_t size, uintptr_t dst);
#define proc_Pwrite(ph, src, size, dst) _proc_Pwrite(POS, ph, src, size, dst)

extern void _proc_Lsync(POS_PARAMS, struct ps_lwphandle *lh);
#define proc_Lsync(lh) _proc_Lsync(POS, lh)

extern	int	_proc_Lstack(POS_PARAMS, struct ps_lwphandle *lh, stack_t *stack);
#define	proc_Lstack(lh, stack) _proc_Lstack(POS, lh, stack)

extern	int	_proc_Lmain_stack(POS_PARAMS, struct ps_lwphandle *lh, stack_t *stack);
#define	proc_Lmain_stack(lh, stack) _proc_Lmain_stack(POS, lh, stack)

extern	int	_proc_Lalt_stack(POS_PARAMS, struct ps_lwphandle *lh, stack_t *stack);
#define	proc_Lalt_stack(lh, stack) _proc_Lalt_stack(POS, lh, stack)

/*
 * Writes a string to the debug log stream describing each status flag that is set in a given thread or process flags value.
 */
extern void log_printStatusFlags(const char *prefix, int pr_flags, const char *suffix);

/*
 * Writes a string to the debug log stream describing the why a given lwp is stopped (if it is stopped).
 */
extern void log_printWhyStopped(const char *prefix, const lwpstatus_t *lwpstatus, const char *suffix);

/*
 * Convenience macro for initializing a variable to hold the handle to a LWP denoted by a process handle and a LWP identifier.
 */
#define INIT_LWP_HANDLE(lh, ph, lwpId, errorReturnValue) \
    int error; \
    struct ps_lwphandle *lh = proc_Lgrab((struct ps_prochandle *) ph, (lwpid_t) lwpId, &error); \
    if (error != 0) { \
        log_println("Lgrab failed: %s", Lgrab_error(error)); \
        return errorReturnValue; \
    }

