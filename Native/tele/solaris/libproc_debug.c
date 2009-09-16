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

void print_lwpstatus(struct lwpstatus *status) {

  log_print("\n*** lwpstatus ***\n\n");

  log_print("pr_flags (flags): %d\n", status->pr_flags);
  log_print("pr_lwpid (specific lwp identifier): %d\n", status->pr_lwpid);
  log_print("pr_what (more detailed reason): %d\n", status->pr_what);
  log_print("pr_cursig (current signal, if any): %d\n", status->pr_cursig);
  log_print("pr_info (info associated with signal or fault): %d %d %d\n", status->pr_info.si_signo, status->pr_info.si_code, status->pr_info.si_errno);
  log_print("pr_lwppend (set of signals pending to the lwp): %d %d %d %d\n", status->pr_lwppend.__sigbits[0], status->pr_lwppend.__sigbits[1], status->pr_lwppend.__sigbits[2], status->pr_lwppend.__sigbits[3]);
  log_print("pr_lwphold (set of signals blocked by the lwp): %d %d %d %d\n", status->pr_lwphold.__sigbits[0], status->pr_lwphold.__sigbits[1], status->pr_lwphold.__sigbits[2], status->pr_lwphold.__sigbits[3]);
}

void print_pstatus(pstatus_t *status, char *name) {

  log_print("\n*** pstatus %s ***\n\n", name);

  log_print("pr_flags (flags): %d\n", status->pr_flags);
  log_print("pr_nlwp (number of active lwps in the process): %d\n", status->pr_nlwp);
  log_print("pr_pid (process id): %d\n", status->pr_pid);
  log_print("pr_ppid (parent process id): %d\n", status->pr_ppid);
  log_print("pr_pgid (process group id): %d\n");
  log_print("pr_sid (session id): %d\n", status->pr_sid);
  log_print("pr_agentid (wp id of the /proc agent lwp, if any): %d\n", status->pr_agentid);
  log_print("pr_sigpend (set of process pending signals): %d %d %d %d\n", status->pr_sigpend.__sigbits[0], status->pr_sigpend.__sigbits[1], status->pr_sigpend.__sigbits[2], status->pr_sigpend.__sigbits[3]);
  log_print("pr_sigtrace (set of traced signals): %d %d %d %d\n", status->pr_sigtrace.__sigbits[0], status->pr_sigtrace.__sigbits[1], status->pr_sigtrace.__sigbits[2], status->pr_sigtrace.__sigbits[3]);
  log_print("pr_flttrace (set of traced faults): %d %d %d %d\n", status->pr_flttrace.word[0], status->pr_flttrace.word[1], status->pr_flttrace.word[2], status->pr_flttrace.word[3]);
  log_print("pr_nzomb (number of zombie lwps in the process): %d\n", status->pr_nzomb);

  print_lwpstatus(&status->pr_lwp);
}

void print_lwphandle(struct ps_lwphandle *lwp, int i) {

  log_print("\n*** lwphandle %d ***\n\n", i);

  log_print("lwp_id (lwp identifier): %d\n", lwp->lwp_id);
  log_print("lwp_state (state of the lwp): %d\n", lwp->lwp_state);
  log_print("lwp_ctlfd (/proc/<pid>/lwp/<lwpid>/lwpctl): %d\n", lwp->lwp_ctlfd);
  log_print("lwp_statfd (proc/<pid>/lwp/<lwpid>/lwpstatus): %d\n", lwp->lwp_statfd);

  print_lwpstatus(&lwp->lwp_status);
}

void print_ps_prochandle(struct ps_prochandle *ps) {

  log_print("\n*** ps_prochandle ***\n\n");

  log_print("pid: %d\n", ps->pid);
  log_print("state: %d\n", ps->state);
  print_pstatus(&ps->orig_status, "orig_status");
  print_pstatus(&ps->status, "status");

  struct ps_lwphandle *lwp;
  int perr;
  int i;
  for (i=1; i<1024; i++) {
    lwp = Lgrab(ps, i, &perr);
    if (lwp == NULL) {
       break;
    }
    print_lwphandle(lwp, i);
    Lfree(lwp);
  }

}
