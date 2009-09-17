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
 * @author Bernd Mathiske
 * @author Hannes Payer
 */

#include <string.h>
#include <stdio.h>
#include <alloca.h>

#include "log.h"
#include "jni.h"
#include "errno.h"

#include "proc.h"
#include "libproc_debug.h"

#include "threadLocals.h"
#include "teleNativeThread.h"
#include "threads.h"
#include "teleProcess.h"

#include <sys/types.h>
#include <sys/wait.h>

void teleProcess_initialize(void) {
}


JNIEXPORT jint JNICALL
Java_com_sun_max_tele_debug_solaris_SolarisTeleProcess_nativeReadBytes(JNIEnv *env, jclass c, jlong handle, jlong src, jobject dst, jboolean isDirectByteBuffer, jint dstOffset, jint length) {
    struct ps_prochandle *ph = (struct ps_prochandle *) handle;
    return teleProcess_read(ph, env, c, src, dst, isDirectByteBuffer, dstOffset, length);
}

JNIEXPORT jint JNICALL
Java_com_sun_max_tele_debug_solaris_SolarisTeleProcess_nativeWriteBytes(JNIEnv *env, jclass c, jlong handle, jlong dst, jobject src, jboolean isDirectByteBuffer, jint srcOffset, jint length) {
    struct ps_prochandle *ph = (struct ps_prochandle *) handle;
    return teleProcess_write(ph, env, c, dst, src, isDirectByteBuffer, srcOffset, length);
}

JNIEXPORT jlong JNICALL
Java_com_sun_max_tele_debug_solaris_SolarisTeleProcess_nativeCreateChild(JNIEnv *env, jclass c, long commandLineArgumentArray, jint vmAgentPort) {
    int error;
    char path[MAX_PATH_LENGTH];
    char **argv = (char**) commandLineArgumentArray;

#if log_TELE
    log_println("argv[0]: %s", argv[0]);
#endif

    int portDefSize = strlen("MAX_AGENT_PORT=") + 11;
    char *portDef = (char *) malloc(portDefSize);
    if (portDef == NULL || snprintf(portDef, portDefSize, "MAX_AGENT_PORT=%u", vmAgentPort) < 0) {
        log_exit(1, "Could not set MAX_AGENT_PORT environment variable");
    }
    putenv(portDef);

    struct ps_prochandle *ph = proc_Pcreate(argv[0], argv, &error, path, sizeof(path));
    if (error != 0) {
        log_println("Could not create child process: %s", Pcreate_error(error));
        return NULL;
    }

    return (jlong) ph;
}

JNIEXPORT void JNICALL
Java_com_sun_max_tele_debug_solaris_SolarisTeleProcess_nativeKill(JNIEnv *env, jclass c, jlong processHandle) {
    struct ps_prochandle *ph = (struct ps_prochandle *) processHandle;
    int state = proc_Pstate(ph);
    if (state != PS_LOST && state != PS_DEAD && state != PS_UNDEAD) {
        Prelease(ph, PRELEASE_KILL);
    }
}

JNIEXPORT jboolean JNICALL
Java_com_sun_max_tele_debug_solaris_SolarisTeleProcess_nativeSuspend(JNIEnv *env, jclass c, jlong processHandle) {
    struct ps_prochandle *ph = (struct ps_prochandle *) processHandle;
    if (Pdstop(ph) != 0) {
        log_println("Cannot stop the process");
        return false;
    }
    return true;
}

#ifdef false
int
Psetrun_dbg(struct ps_prochandle *P,
    int sig,    /* signal to pass to process */
    int flags)  /* PRSTEP|PRSABORT|PRSTOP|PRCSIG|PRCFAULT */
{
    int ctlfd = (P->agentctlfd >= 0) ? P->agentctlfd : P->ctlfd;
    int sbits = (PR_DSTOP | PR_ISTOP | PR_ASLEEP);

    long ctl[1 +                    /* PCCFAULT */
        1 + sizeof (siginfo_t)/sizeof (long) +  /* PCSSIG/PCCSIG */
        2 ] = {0};                    /* PCRUN    */

    int ctl_len = 1 +                    /* PCCFAULT */
                    1 + sizeof (siginfo_t)/sizeof (long) +  /* PCSSIG/PCCSIG */
                    2;

    log_println("CTLlen: %d\n", ctl_len);


    long *ctlp = ctl;
    size_t size;

    if (P->state != PS_STOP && (P->status.pr_lwp.pr_flags & sbits) == 0) {
        errno = EBUSY;
        return (-1);
    }

    Psync(P);   /* flush tracing flags and registers */

    if (flags & PRCFAULT) {     /* clear current fault */
        *ctlp++ = PCCFAULT;
        flags &= ~PRCFAULT;
    }

    if (flags & PRCSIG) {       /* clear current signal */
        *ctlp++ = PCCSIG;
        flags &= ~PRCSIG;
    } else if (sig && sig != P->status.pr_lwp.pr_cursig) {
        /* make current signal */
        siginfo_t *infop;

        *ctlp++ = PCSSIG;
        infop = (siginfo_t *)ctlp;
        (void) memset(infop, 0, sizeof (*infop));
        infop->si_signo = sig;
        ctlp += sizeof (siginfo_t) / sizeof (long);
    }

    *ctlp++ = PCRUN;
    *ctlp++ = flags;
    size = (char *)ctlp - (char *)ctl;

    P->info_valid = 0;  /* will need to update map and file info */

    /*
     * If we've cached ucontext-list information while we were stopped,
     * free it now.
     */
    if (P->ucaddrs != NULL) {
        free(P->ucaddrs);
        P->ucaddrs = NULL;
        P->ucnelems = 0;
    }


    log_println("BEFORE WRITE ctlfd: %d; ctl: %ld; size: %ld\n", ctlfd, ctl, size);
    int i;
    for(i=0; i<ctl_len; i++) {
        printf("%d ", ctl[i]);
    }
    print_ps_prochandle(P);
    printf("\n");
    if (write(ctlfd, ctl, size) != size) {
        log_println("PROBLEM WRITE\n");
        print_ps_prochandle(P);
        /* If it is dead or lost, return the real status, not PS_RUN */
        if (errno == ENOENT || errno == EAGAIN) {
            (void) Pstopstatus(P, PCNULL, 0);
            return (0);
        }
        /* If it is not in a jobcontrol stop, issue an error message */
        if (errno != EBUSY ||
            P->status.pr_lwp.pr_why != PR_JOBCONTROL) {
            log_println("Psetrun: %s\n", strerror(errno));
            return (-1);
        }
        /* Otherwise pretend that the job-stopped process is running */
    }

    log_println("AFTER WRITE\n");
    print_ps_prochandle(P);

    P->state = PS_RUN;
    return (0);
}
#endif


JNIEXPORT jboolean JNICALL
Java_com_sun_max_tele_debug_solaris_SolarisTeleProcess_nativeResume(JNIEnv *env, jclass c, jlong processHandle) {
    struct ps_prochandle *ph = (struct ps_prochandle *) processHandle;
    _libproc_debug = log_TELE;

    sysset_t syscalls;
    premptyset(&syscalls);
    proc_Psetsysentry(ph, &syscalls);
    proc_Psetsysexit(ph, &syscalls);

    sigset_t signals;
    premptyset(&signals);
    Psetsignal(ph, &signals);

    fltset_t faults;
    premptyset(&faults);
    praddset(&faults, FLTILL);
    praddset(&faults, FLTPRIV);
    praddset(&faults, FLTBPT);
    praddset(&faults, FLTTRACE);
    //    praddset(&faults, FLTACCESS);
    //    praddset(&faults, FLTBOUNDS);
    praddset(&faults, FLTIOVF);
    //    praddset(&faults, FLTIZDIV);
    praddset(&faults, FLTFPE);
    praddset(&faults, FLTSTACK);
    praddset(&faults, FLTWATCH);
    Psetfault(ph, &faults);

    if (Pclearfault(ph) != 0) {
        log_println("Pclearfault failed");
        return false;
    }
    if (Pclearsig(ph) != 0) {
        log_println("Pclearsig failed");
        return false;
    }

    proc_Psync(ph);

    if (Psetrun(ph, 0, 0) != 0) {
        log_println("Psetrun failed, proc_Pstate %d", proc_Pstate(ph));
#ifdef __SunOS_5_11
        /* For some unknown reason, Psetrun can return a non-zero result on OpenSolaris
         * even though the VM seems to have successfully started running. */
        log_println("**** Ignoring negative result of calling Psetrun on OpenSolaris *****");
        return true;
#else
        return false;
#endif
    }

    return true;
}

JNIEXPORT jboolean JNICALL
Java_com_sun_max_tele_debug_solaris_SolarisTeleProcess_nativeWait(JNIEnv *env, jclass c, jlong processHandle) {
    struct ps_prochandle *ph = (struct ps_prochandle *) processHandle;
    int error = proc_Pwait(ph, 0);
    if (error != 0) {
        int rc = proc_Pstate(ph);
        log_println("nativeWait: Pwait failed in solarisTeleProcess, proc_Pstate %d; error: %d; errno: %d", rc, error, errno);
		log_println("ERROR: %s", strerror(errno));

		int statloc = 0;
		waitpid(ph->pid, &statloc, 0);
		statloc_eval(statloc);

        return false;
    }

    if (Pclearfault(ph) != 0) {
        int rc = proc_Pstate(ph);
        log_println("Pclearfault failed, proc_Pstate %d", rc);
        return false;
    }

    if (Pclearsig(ph) != 0) {
        int rc = proc_Pstate(ph);
        log_println("Pclearsig failed, proc_Pstate %d", rc);
        return false;
    }

    proc_Psync(ph);
    return true;
}

ThreadState_t lwpStatusToThreadState(const lwpstatus_t *lwpStatus) {
    short why = lwpStatus->pr_why;
    short what = lwpStatus->pr_what;
    int flags = lwpStatus->pr_flags;

    /* This is only called after a Pwait so all threads should be stopped. */
    c_ASSERT((lwpStatus->pr_flags & PR_STOPPED) != 0);

    ThreadState_t result = TS_SUSPENDED;
    if (why == PR_FAULTED) {
        if (what == FLTWATCH) {
            result = TS_WATCHPOINT;
        }
    }
    return result;
}

typedef struct Argument {
    struct ps_prochandle *ph;
    JNIEnv *env;
    jobject teleProcess;
    jobject threadSequence;
    Address threadLocalsList;
    Address primordialThreadLocals;
} *Argument;

static long getRegister(struct ps_prochandle *ph, jlong lwpId, int registerIndex) {
    INIT_LWP_HANDLE(lh, ph, lwpId, false);

    proc_Lsync(lh);

    jlong result = -1L;
    if (proc_Lgetareg(lh, registerIndex, &result) != 0) {
        log_println("Lgetareg failed");
        proc_Lfree(lh);
        return -1L;
    }
    proc_Lfree(lh);
    return result;
}

static int gatherThread(void *data, const lwpstatus_t *lwpStatus) {
    Argument a = (Argument) data;
    pstatus_t *pStatus = (pstatus_t *) proc_Pstatus(a->ph);
    if (lwpStatus->pr_lwpid == pStatus->pr_agentid) {
        // Ignore the agent thread (i.e. the thread communicating with the inspector)
        return 0;
    }

    jlong lwpId = lwpStatus->pr_lwpid;
    ThreadState_t threadState = lwpStatusToThreadState(lwpStatus);

    ThreadLocals threadLocals = (ThreadLocals) alloca(threadLocalsSize());
    NativeThreadLocalsStruct nativeThreadLocalsStruct;
    Address stackPointer = getRegister(a->ph, lwpId, R_SP);
    Address instructionPointer = getRegister(a->ph, lwpId, R_PC);
    ThreadLocals tl = teleProcess_findThreadLocals(a->ph, a->threadLocalsList, a->primordialThreadLocals, stackPointer, threadLocals, &nativeThreadLocalsStruct);
    teleProcess_jniGatherThread(a->env, a->teleProcess, a->threadSequence, lwpId, threadState, instructionPointer, tl);

    return 0;
}

JNIEXPORT void JNICALL
Java_com_sun_max_tele_debug_solaris_SolarisTeleProcess_nativeGatherThreads(JNIEnv *env, jobject teleProcess, jlong processHandle, jobject threadSequence, long threadLocalsList, long primordialThreadLocals) {
    struct ps_prochandle *ph = (struct ps_prochandle *) processHandle;

    if (Pcreate_agent(ph) != 0) {
        log_println("could not create agent lwp in tele process");
    }

    struct Argument a;
    a.ph = ph;
    a.env = env;
    a.teleProcess = teleProcess;
    a.threadSequence = threadSequence;
    a.threadLocalsList = threadLocalsList;
    a.primordialThreadLocals = primordialThreadLocals;

    int error = Plwp_iter(ph, gatherThread, &a);
    if (error != 0) {
        log_println("Error iterating over threads of process");
    }

    Pdestroy_agent(ph);
}

JNIEXPORT jboolean JNICALL
Java_com_sun_max_tele_debug_solaris_SolarisTeleProcess_nativeActivateWatchpoint(JNIEnv *env, jclass c, jlong processHandle, jlong address, jlong size, jboolean after, jboolean read, jboolean write, jboolean exec) {
    struct ps_prochandle *ph = (struct ps_prochandle *) processHandle;
    prwatch_t w;
    w.pr_vaddr = address;
    w.pr_size = size;
    w.pr_wflags = 0;

    if (read) {
        w.pr_wflags |= WA_READ;
    }
    if (write) {
        w.pr_wflags |= WA_WRITE;
    }
    if (exec) {
        w.pr_wflags |= WA_EXEC;
    }
    if (after) {
        w.pr_wflags |= WA_TRAPAFTER;
    }

    w.pr_pad = 0;

    log_println("BEFORE PSETWAPT\n");
    print_ps_prochandle(ph);

    int error = Psetwapt(ph, &w);
    if (error != 0) {
        log_println("could not set watch point - error: %d", error);
        return false;
    }

    log_println("AFTER PSETWAPT\n");
    print_ps_prochandle(ph);

    int rc = proc_Pstate(ph);
    log_println("nativeActivateWatchpoint before sync: proc_Pstate %d; errno: %d", rc, errno);
    log_println("ERROR: %s", strerror(errno));

    proc_Psync(ph);

    rc = proc_Pstate(ph);
    log_println("nativeActivateWatchpoint after sync: proc_Pstate %d; errno: %d", rc, errno);
    log_println("ERROR: %s", strerror(errno));

    return true;
}

JNIEXPORT jboolean JNICALL
Java_com_sun_max_tele_debug_solaris_SolarisTeleProcess_nativeDeactivateWatchpoint(JNIEnv *env, jclass c, jlong processHandle, jlong address, jlong size) {
    struct ps_prochandle *ph = (struct ps_prochandle *) processHandle;
    prwatch_t w;
    w.pr_vaddr = address;
    w.pr_size = size;
    w.pr_pad = 0;

    int error = Pdelwapt(ph, &w);
    if (error != 0) {
        log_println("could not set watch point - error: %d", error);
        return false;
    }

    proc_Psync(ph);
    return true;
}

JNIEXPORT jlong JNICALL
Java_com_sun_max_tele_debug_solaris_SolarisTeleProcess_nativeReadWatchpointAddress(JNIEnv *env, jclass c, jlong processHandle) {
    struct ps_prochandle *ph = (struct ps_prochandle *) processHandle;
    pstatus_t *ps;
    long addr;

    addr = (long)proc_Pstatus(ph)->pr_lwp.pr_info.si_addr;

    return addr;
}

JNIEXPORT jint JNICALL
Java_com_sun_max_tele_debug_solaris_SolarisTeleProcess_nativeReadWatchpointAccessCode(JNIEnv *env, jclass c, jlong processHandle){
    struct ps_prochandle *ph = (struct ps_prochandle *) processHandle;
    pstatus_t *ps;
    int code = 0;

    code = proc_Pstatus(ph)->pr_lwp.pr_info.si_code;

    return code;
}
