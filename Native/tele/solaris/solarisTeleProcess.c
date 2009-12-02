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

    struct ps_prochandle *ph = Pcreate(argv[0], argv, &error, path, sizeof(path));
    if (error != 0) {
        log_println("Could not create child process: %s", Pcreate_error(error));
        return NULL;
    } else {
        _libproc_debug = log_TELE;

        /* Set the faults to be traced in the process. On incurring one of these faults, an lwp stops. */
        fltset_t faults;
        premptyset(&faults);
        praddset(&faults, FLTBPT);   /* breakpoint trap */
        praddset(&faults, FLTTRACE); /* trace trap (single-step) */
        praddset(&faults, FLTWATCH); /* watchpoint trap */
        Psetfault(ph, &faults);
   }

    return (jlong) ph;
}

JNIEXPORT void JNICALL
Java_com_sun_max_tele_debug_solaris_SolarisTeleProcess_nativeKill(JNIEnv *env, jclass c, jlong processHandle) {
    struct ps_prochandle *ph = (struct ps_prochandle *) processHandle;
    int state = Pstate(ph);
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

/**
 * Any thread in a PR_FAULTED state needs to be moved into
 * the PR_REQUESTED state so that it will be resumed when
 * the whole process is resumed. For more detail, see
 * http://monaco.sfbay.sun.com/detail.jsf?cr=4165633
 */
static int cancelFault(void *data, const lwpstatus_t *ls) {
    struct ps_prochandle *ph = (struct ps_prochandle *) data;
    pstatus_t *pStatus = (pstatus_t *) Pstatus(ph);

    jlong lwpId = ls->pr_lwpid;
    if (ls->pr_why == PR_FAULTED) {
        tele_log_println("Canceling fault on thread %d before resuming process", lwpId);
        int error;
        struct ps_lwphandle *lh = Lgrab(ph, (lwpid_t) lwpId, &error);
        if (error != 0) {
            log_println("Lgrab failed: %s [lwpId=%d]", Lgrab_error(error), lwpId);
            return error;
        } else {
            if ((error = Lclearfault(lh)) != 0) {
                log_println("Lclearfault failed: %d", error);
            } else {
                if ((error = Lsetrun(lh, 0, PRCFAULT | PRSTOP)) != 0) {
                    log_println("Lsetrun failed: %d", error);
                } else {
                    again:
                    if ((error = Lwait(lh, 0)) != 0) {
                        if (error == EINTR) {
                            log_println("Waiting for thread %d to stop...", lwpId);
                            goto again;
                        }
                        log_println("Lwait failed: %d", error);
                    }
                }
            }
            Lfree(lh);
            return error;
        }
    }
    return 0;
}

JNIEXPORT jboolean JNICALL
Java_com_sun_max_tele_debug_solaris_SolarisTeleProcess_nativeResume(JNIEnv *env, jclass c, jlong processHandle) {
    struct ps_prochandle *ph = (struct ps_prochandle *) processHandle;

    int error = Plwp_iter(ph, cancelFault, ph);
    if (error != 0) {
        log_println("Error iterating over threads of process: error=%d");
        return false;
    }

    if (Psetrun(ph, 0, 0) != 0) {
        log_println("Psetrun failed, Pstate %d", Pstate(ph));
        return false;
    }

    return true;
}

JNIEXPORT jboolean JNICALL
Java_com_sun_max_tele_debug_solaris_SolarisTeleProcess_nativeWait(JNIEnv *env, jclass c, jlong processHandle) {
    struct ps_prochandle *ph = (struct ps_prochandle *) processHandle;
    int error = Pwait(ph, 0);
    if (error != 0) {
        int rc = Pstate(ph);
        log_println("nativeWait: Pwait failed in solarisTeleProcess, Pstate %d; error: %d; errno: %d", rc, error, errno);
		log_println("ERROR: %s", strerror(errno));
        return false;
    }

    return true;
}

ThreadState_t lwpStatusToThreadState(const lwpstatus_t *ls) {
    short why = ls->pr_why;
    short what = ls->pr_what;
    int flags = ls->pr_flags;

    /* This is only called after a Pwait so all threads should be stopped. */
    c_ASSERT((ls->pr_flags & PR_STOPPED) != 0);

    ThreadState_t result = TS_SUSPENDED;
    if (why == PR_FAULTED) {
        if (what == FLTWATCH) {
            result = TS_WATCHPOINT;
        } else if (what == FLTBPT) {
            result = TS_BREAKPOINT;
        }
    }
    return result;
}

typedef struct GatherThreadArgument {
    struct ps_prochandle *ph;
    JNIEnv *env;
    jobject teleProcess;
    jobject threadSequence;
    Address threadLocalsList;
    Address primordialThreadLocals;
} *GatherThreadArgument;

static int gatherThread(void *data, const lwpstatus_t *ls) {
    GatherThreadArgument a = (GatherThreadArgument) data;

    jlong lwpId = ls->pr_lwpid;
    ThreadState_t threadState = lwpStatusToThreadState(ls);

    ThreadLocals threadLocals = (ThreadLocals) alloca(threadLocalsAreaSize());
    NativeThreadLocalsStruct nativeThreadLocalsStruct;
    Address stackPointer = ls->pr_reg[R_SP];
    Address instructionPointer = ls->pr_reg[R_PC];
    ThreadLocals tl = teleProcess_findThreadLocals(a->ph, a->threadLocalsList, a->primordialThreadLocals, stackPointer, threadLocals, &nativeThreadLocalsStruct);
    teleProcess_jniGatherThread(a->env, a->teleProcess, a->threadSequence, lwpId, threadState, instructionPointer, tl);

    return 0;
}

JNIEXPORT void JNICALL
Java_com_sun_max_tele_debug_solaris_SolarisTeleProcess_nativeGatherThreads(JNIEnv *env, jobject teleProcess, jlong processHandle, jobject threadSequence, long threadLocalsList, long primordialThreadLocals) {
    struct ps_prochandle *ph = (struct ps_prochandle *) processHandle;

    struct GatherThreadArgument a;
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

    int error = Psetwapt(ph, &w);
    if (error != 0) {
        log_println("could not set watch point - error: %d", error);
        return false;
    }

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

    return true;
}

JNIEXPORT jlong JNICALL
Java_com_sun_max_tele_debug_solaris_SolarisTeleProcess_nativeReadWatchpointAddress(JNIEnv *env, jclass c, jlong processHandle) {
    struct ps_prochandle *ph = (struct ps_prochandle *) processHandle;
    return (long) Pstatus(ph)->pr_lwp.pr_info.si_addr;
}

JNIEXPORT jint JNICALL
Java_com_sun_max_tele_debug_solaris_SolarisTeleProcess_nativeReadWatchpointAccessCode(JNIEnv *env, jclass c, jlong processHandle){
    struct ps_prochandle *ph = (struct ps_prochandle *) processHandle;
    return Pstatus(ph)->pr_lwp.pr_info.si_code;
}
