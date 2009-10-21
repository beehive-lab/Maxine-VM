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

#include <string.h>
#include <stdio.h>
#include <signal.h>
#include "log.h"
#include "errno.h"

#include "proc.h"
#include "libproc_debug.h"

void statloc_eval(int statloc) {
    log_println("statloc evaluation:");
    log_println("statloc value: %d", statloc);
    if (WIFEXITED(statloc)) {
        log_print("WIFEXITED: ");
        log_print("%d; ", WEXITSTATUS(statloc));
        log_println("Evaluates to a non-zero value if status was returned for a child process that exited normally. ");
    }
    if (WIFSIGNALED(statloc)) {
        log_print("WIFSIGNALED: ");
        log_print("%d; ", WTERMSIG(statloc));
        log_println("Evaluates to a non-zero value if status was returned for a child process that terminated due to receipt of a signal that was not caught. ");
    }
//    if (WIFCORED(statloc)) {
//        log_print("WIFCORED: ");
//          log_print("%d; ", WCORESIG(statloc))
//        log_println("If the value of WIFSIGNALED(s) is non-zero, this macro evaluates to the number of the signal that caused the termination of the child process. ");
//    }
    if (WCOREDUMP(statloc)) {
        log_print("WCOREDUMP: ");
        log_println("Evaluates to a non-zero value if status was returned for a child process that terminated due to receipt of a signal that was not caught, and whose default action is to dump core. ");
    }
    if (WIFSTOPPED(statloc)) {
        log_print("WIFSTOPPED: ");
        log_print("%d; ", WSTOPSIG(statloc));
        log_println("Evaluates to a non-zero value if status was returned for a child process that is currently stopped. ");
    }
}

static void print_lwpstatus(struct lwpstatus *status) {
    log_println("    pr_flags (flags): %d", status->pr_flags);
    log_println("    pr_lwpid (specific lwp identifier): %d", status->pr_lwpid);
    log_println("    pr_what (more detailed reason): %d", status->pr_what);
    log_println("    pr_cursig (current signal, if any): %d", status->pr_cursig);
    log_println("    pr_info (info associated with signal or fault): %d %d %d", status->pr_info.si_signo, status->pr_info.si_code, status->pr_info.si_errno);
    log_println("    pr_lwppend (set of signals pending to the lwp): %d %d %d %d", status->pr_lwppend.__sigbits[0], status->pr_lwppend.__sigbits[1], status->pr_lwppend.__sigbits[2], status->pr_lwppend.__sigbits[3]);
    log_println("    pr_lwphold (set of signals blocked by the lwp): %d %d %d %d", status->pr_lwphold.__sigbits[0], status->pr_lwphold.__sigbits[1], status->pr_lwphold.__sigbits[2], status->pr_lwphold.__sigbits[3]);
}

static void print_pstatus(pstatus_t *status, char *label) {
    log_println("  /* %s */", label);
    log_println("  pr_flags (flags): %d", status->pr_flags);
    log_println("  pr_nlwp (number of active lwps in the process): %d", status->pr_nlwp);
    log_println("  pr_pid (process id): %d", status->pr_pid);
    log_println("  pr_ppid (parent process id): %d", status->pr_ppid);
    log_println("  pr_pgid (process group id): %d");
    log_println("  pr_sid (session id): %d", status->pr_sid);
    log_println("  pr_agentid (wp id of the /proc agent lwp, if any): %d", status->pr_agentid);
    log_println("  pr_sigpend (set of process pending signals): %d %d %d %d", status->pr_sigpend.__sigbits[0], status->pr_sigpend.__sigbits[1], status->pr_sigpend.__sigbits[2], status->pr_sigpend.__sigbits[3]);
    log_println("  pr_sigtrace (set of traced signals): %d %d %d %d", status->pr_sigtrace.__sigbits[0], status->pr_sigtrace.__sigbits[1], status->pr_sigtrace.__sigbits[2], status->pr_sigtrace.__sigbits[3]);
    log_println("  pr_flttrace (set of traced faults): %d %d %d %d", status->pr_flttrace.word[0], status->pr_flttrace.word[1], status->pr_flttrace.word[2], status->pr_flttrace.word[3]);
    log_println("  pr_nzomb (number of zombie lwps in the process): %d", status->pr_nzomb);
    log_println("  Representative LWP:");
    print_lwpstatus(&status->pr_lwp);
}

static void print_lwphandle(struct ps_lwphandle *lwp, int i) {
    log_println("  LWP %d:", i);
    log_println("    lwp_id (lwp identifier): %d", lwp->lwp_id);
    log_println("    lwp_state (state of the lwp): %d", lwp->lwp_state);
    log_println("    lwp_ctlfd (/proc/<pid>/lwp/<lwpid>/lwpctl): %d", lwp->lwp_ctlfd);
    log_println("    lwp_statfd (proc/<pid>/lwp/<lwpid>/lwpstatus): %d", lwp->lwp_statfd);

    print_lwpstatus(&lwp->lwp_status);
}

void log_process(struct ps_prochandle *ps) {

    log_println("PROCESS %d:", ps->pid);
    log_println("  pid: %d", ps->pid);
    log_println("  state: %d", ps->state);
    print_pstatus(&ps->orig_status, "Original Status");
    print_pstatus(&ps->status, "Current Status");

    struct ps_lwphandle *lwp;
    int perr;
    int i;
    for (i = 1; i < 1024; i++) {
        lwp = Lgrab(ps, i, &perr);
        if (lwp == NULL) {
            break;
        }
        print_lwphandle(lwp, i);
        Lfree(lwp);
    }
}
