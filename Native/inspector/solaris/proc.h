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
/*VCSID=18a5df34-b9c7-4cf1-921b-505fec860a8e*/
#include "libproc.h"

/**
 * Interposition for libproc function call tracing.
 */

extern struct ps_prochandle *_proc_Pcreate(const char *func, int line, const char *arg0, char *const *argv, int *error, char *path, size_t pathLength);
#define proc_Pcreate(arg0, argv, error, path, pathLength) _proc_Pcreate(__func__, __LINE__, arg0, argv, error, path, pathLength)

extern const pstatus_t *_proc_Pstatus(const char *func, int line, struct ps_prochandle *ph);
#define proc_Pstatus(ph) _proc_Pstatus(__func__, __LINE__, ph)

extern int _proc_Pstate(const char *func, int line, struct ps_prochandle *ph);
#define proc_Pstate(ph) _proc_Pstate(__func__, __LINE__, ph)

extern void _proc_Psync(const char* func, int line, struct ps_prochandle *ph);
#define proc_Psync(ph) _proc_Psync(__func__, __LINE__, ph)

extern int _proc_Pmapping_iter(const char* func, int line, struct ps_prochandle *ph, proc_map_f *f, void *cd);
#define proc_Pmapping_iter(ph, f, cd) _proc_Pmapping_iter(__func__, __LINE__, ph, f, cd)

extern void _proc_Pupdate_maps(const char* func, int line, struct ps_prochandle *ph);
#define proc_Pupdate_maps(ph) _proc_Pupdate_maps(__func__, __LINE__, ph)

extern int _proc_Psetrun(const char* func, int line, struct ps_prochandle *ph, int sig, int flags);
#define proc_Psetrun(ph, sig, flags) _proc_Psetrun(__func__, __LINE__, ph, sig, flags)

extern int _proc_Pwait(const char* func, int line, struct ps_prochandle *ph, int msec);
#define proc_Pwait(ph, msec) _proc_Pwait(__func__, __LINE__, ph, msec)

extern void	_proc_Psetsysentry(const char *func, int line, struct ps_prochandle *ph, const sysset_t *set);
#define proc_Psetsysentry(ph, set) _proc_Psetsysentry(__func__, __LINE__, ph, set)

extern void _proc_Psetsysexit(const char *func, int line, struct ps_prochandle *ph, const sysset_t *set);
#define proc_Psetsysexit(ph, set) _proc_Psetsysexit(__func__, __LINE__, ph, set)

extern struct ps_lwphandle *_proc_Lgrab(const char *func, int line, struct ps_prochandle *lh, lwpid_t lwpId, int *error);
#define proc_Lgrab(lh, lwpId, error) _proc_Lgrab(__func__, __LINE__, lh, lwpId, error)

extern	void _proc_Lfree(const char *func, int line, struct ps_lwphandle *lh);
#define proc_Lfree(lh) _proc_Lfree(__func__, __LINE__, lh)

extern int	_proc_Lwait(const char *func, int line, struct ps_lwphandle *lh, uint_t timeout);
#define	proc_Lwait(lh, timeout) _proc_Lwait(__func__, __LINE__, lh, timeout)

extern int _proc_Plwp_getregs(const char *func, int line, struct ps_prochandle *, lwpid_t lwpid, prgregset_t regs);
#define proc_Plwp_getregs(ph, lwpid, regs) _proc_Plwp_getregs(__func__, __LINE__, ph, lwpid, regs)

extern	int	_proc_Lgetareg(const char *func, int line, struct ps_lwphandle *lh, int index, prgreg_t *result);
#define	proc_Lgetareg(lh, index, result) _proc_Lgetareg(__func__, __LINE__, lh, index, result)

extern	int	_proc_Lputareg(const char *func, int line, struct ps_lwphandle *lh, int index, prgreg_t value);
#define	proc_Lputareg(lh, index, value) _proc_Lputareg(__func__, __LINE__, lh, index, value)

extern	int	_proc_Lsetrun(const char *func, int line, struct ps_lwphandle *lh, int sig, int flags);
#define	proc_Lsetrun(lh, sig, flags) _proc_Lsetrun(__func__, __LINE__, lh, sig, flags)

extern	int	_proc_Lclearfault(const char *func, int line, struct ps_lwphandle *lh);
#define	proc_Lclearfault(lh) _proc_Lclearfault(__func__, __LINE__, lh)

extern ssize_t _proc_Pread(const char *func, int line, struct ps_prochandle *ph, void *dst, size_t size, uintptr_t src);
#define proc_Pread(ph, dst, size, src) _proc_Pread(__func__, __LINE__, ph, dst, size, src)

extern ssize_t _proc_Pwrite(const char *func, int line, struct ps_prochandle *ph, const void *src, size_t size, uintptr_t dst);
#define proc_Pwrite(ph, src, size, dst) _proc_Pwrite(__func__, __LINE__, ph, src, size, dst)

extern void _proc_Lsync(const char *func, int line, struct ps_lwphandle *lh);
#define proc_Lsync(lh) _proc_Lsync(__func__, __LINE__, lh)

extern	int	_proc_Lstack(const char *func, int line, struct ps_lwphandle *lh, stack_t *stack);
#define	proc_Lstack(lh, stack) _proc_Lstack(__func__, __LINE__, lh, stack)

extern	int	_proc_Lmain_stack(const char *func, int line, struct ps_lwphandle *lh, stack_t *stack);
#define	proc_Lmain_stack(lh, stack) _proc_Lmain_stack(__func__, __LINE__, lh, stack)

extern	int	_proc_Lalt_stack(const char *func, int line, struct ps_lwphandle *lh, stack_t *stack);
#define	proc_Lalt_stack(lh, stack) _proc_Lalt_stack(__func__, __LINE__, lh, stack)

/*
 * Writes a string to the debug log stream describing each status flag that is set in a given thread or process flags value.
 */
extern void debug_printStatusFlags(const char *prefix, int pr_flags, const char *suffix);

/*
 * Writes a string to the debug log stream describing the why a given lwp is stopped (if it is stopped).
 */
extern void debug_printWhyStopped(const char *prefix, lwpstatus_t *lwpstatus, const char *suffix);
