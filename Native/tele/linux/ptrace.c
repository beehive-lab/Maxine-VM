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
 * Making calls to 'ptrace()' available via JNI.
 * This way, request constants (e.g. PTRACE_CONT) do not need to be replicated in Java and maintained there.
 *
 * @author Bernd Mathiske
 */
#include <sys/stat.h>
#include <sys/types.h>
#include <sys/user.h>
#include <sys/wait.h>
#include <stdlib.h>
#include <stdio.h>
#include <unistd.h>
#include <errno.h>
#include <fcntl.h>
#include <string.h>

#include "os.h"

#include <sys/ptrace.h>

#include "log.h"
#include "word.h"
#include "isa.h"
#include "jni.h"

jboolean ptraceWaitForSignal(jlong pid, int signalnum) {
    while (1) {
        int status;
        int error = waitpid(pid, &status, 0);

        if (error != pid) {
            log_println("waitpid failed with errno: %d", errno);
            return false;
        }
        if (WIFEXITED(status)) {
            return false;
        }
        if (WIFSIGNALED(status)) {
            return false;
        }
        if (WIFSTOPPED(status)) {
            // check whether the process received a signal, and continue with it if so.
            int signal = WSTOPSIG(status);

            if (signal == 0 || signal == signalnum) {
                return true;
            } else {
                error = ptrace(PT_CONTINUE, pid, (char*) 1, signal);
                if (error != 0) {
                    log_println("ptrace(PT_CONTINUE) failed = %d", error);
                    return false;
                }
            }
        }
    }
}

/* Linux ptrace() is unreliable, but apparently retrying after descheduling helps. */
long ptrace_withRetries(int request, int processID, void *address, void *data) {
    int microSeconds = 100000;
    int i = 0;
    while (true) {
        long result = ptrace(request, processID, address, data);
        if (result != -1 || errno == 0) {
            return result;
        }
        if (errno != ESRCH || i >= 150) {
            return -1;
        }
        usleep(microSeconds);
        i++;
        if (i % 10 == 0) {
            log_println("ptrace retrying");
        }
    }
}

JNIEXPORT jint JNICALL
Java_com_sun_max_tele_debug_linux_Ptrace_nativeCreateChildProcess(JNIEnv *env, jclass c, jlong commandLineArgumentArray, jint vmAgentPort) {
    char **argv = (char**) commandLineArgumentArray;

    int childProcessID = fork();
    if (childProcessID == 0) {
        /*child:*/
#if log_TELE
        log_println("Launching VM process: %s", argv[0]);
#endif
        if (ptrace(PTRACE_TRACEME, 0, 0, 0) != 0) {
            log_exit(1, "ptrace failed in child process");
        }

        char *portDef;
        if (asprintf(&portDef, "MAX_AGENT_PORT=%u", vmAgentPort) == -1) {
            log_exit(1, "Could not allocate space for setting MAX_AGENT_PORT environment variable");
        }
        putenv(portDef);

        /* This call does not return if it succeeds: */
        execv(argv[0], argv);

        log_exit(1, "ptrace failed in child process");
    } else {
        /*parent:*/
        int status;
        if (waitpid(childProcessID, &status, 0) == childProcessID && WIFSTOPPED(status)) {
            return childProcessID;
        }
    }
    return -1;
}

static jboolean check_result(int result, jint processID, const char *action) {
    if (result != 0) {
        log_println("%s process %d failed: %s", action, processID, strerror(result));
        return false;
    }
    return true;
}

JNIEXPORT jboolean JNICALL
Java_com_sun_max_tele_debug_linux_Ptrace_nativeAttach(JNIEnv *env, jclass c, jint processID) {
    return check_result(ptrace(PTRACE_ATTACH, processID, 0, 0), processID, "Attaching");
}

JNIEXPORT jboolean JNICALL
Java_com_sun_max_tele_debug_linux_Ptrace_nativeDetach(JNIEnv *env, jclass c, jint processID) {
    return check_result(ptrace(PTRACE_DETACH, processID, 0, 0), processID, "Detaching");
}

JNIEXPORT jboolean JNICALL
Java_com_sun_max_tele_debug_linux_Ptrace_nativeSingleStep(JNIEnv *env, jclass c, jint processID) {
    return check_result(ptrace(PTRACE_SINGLESTEP, processID, 0, 0), processID, "Stepping");
}

JNIEXPORT jboolean JNICALL
Java_com_sun_max_tele_debug_linux_Ptrace_nativeSuspend(JNIEnv *env, jclass c, jint processID) {
    return check_result(kill(processID, SIGTRAP), processID, "Suspending");
}

JNIEXPORT jboolean JNICALL
Java_com_sun_max_tele_debug_linux_Ptrace_nativeResume(JNIEnv *env, jclass c, jint processID) {
    return check_result(ptrace(PTRACE_CONT, processID, 0, 0), processID, "Resuming");
}

JNIEXPORT jboolean JNICALL
Java_com_sun_max_tele_debug_linux_Ptrace_nativeWait(JNIEnv *env, jclass c, jint processID) {
    return ptraceWaitForSignal(processID, SIGTRAP);
}

JNIEXPORT jboolean JNICALL
Java_com_sun_max_tele_debug_linux_Ptrace_nativeKill(JNIEnv *env, jclass c, jint processID) {
    return check_result(ptrace(PTRACE_KILL, processID, 0, 0), processID, "Killing");
}

static jint readByte(jint request, jint processID, Address address) {
    int index = address % sizeof(Word);
    Address base = address - index;
    Address result = ptrace(request, processID, (void *) base, 0);
    if (errno != 0) {
        return -1;
    }
#if word_BIG_ENDIAN
    return (result >> ((sizeof(Word) - (index + 1)) * 8)) & 0xff;
#else
    return (result >> (index * 8)) & 0xff;
#endif
}

JNIEXPORT jint JNICALL
Java_com_sun_max_tele_debug_linux_Ptrace_nativeReadDataByte(JNIEnv *env, jclass c, jint processID, jlong address) {
    return readByte(PTRACE_PEEKDATA, processID, (Address) address);
}

JNIEXPORT jint JNICALL
Java_com_sun_max_tele_debug_linux_Ptrace_nativeReadTextByte(JNIEnv *env, jclass c, jint processID, jlong address) {
    return readByte(PTRACE_PEEKTEXT, processID, (Address) address);
}

static jboolean writeByte(jint readRequest, jint writeRequest, jint processID, Address address, jbyte value) {
    Address word;
    long index = address % sizeof(Word);
    Address base = address - index;
    Address mask = (Address) 0x000000ff;
    Address data = (Address) value & 0x000000ff;

#if word_BIG_ENDIAN
    mask <<= (sizeof(Word) - (index + 1)) * 8;
    data <<= (sizeof(Word) - (index + 1)) * 8;
#else
    mask <<= index * 8;
    data <<= index * 8;
#endif

    word = ptrace(readRequest, processID, (void *) base, 0);
    if (errno != 0) {
        return false;
    }
    word &= ~mask;
    word |= data;
    return ptrace(writeRequest, processID, (void *) base, (void *) word) == 0;
}

static jboolean writeByteWithRetries(jint readRequest, jint writeRequest, jint processID, Address address, jbyte value) {
    int n = 0;
    while (true) {
        if (writeByte(readRequest, writeRequest, processID, address, value)) {
            jbyte r = readByte(readRequest, processID, address);
            if (r == value) {
                return true;
            }
        }
        if (n++ >= 10) {
            return false;
        }
        log_println("ptrace retrying write");
        usleep(100000);
    }
}

JNIEXPORT jboolean JNICALL
Java_com_sun_max_tele_debug_linux_Ptrace_nativeWriteDataByte(JNIEnv *env, jclass c, jint processID, jlong address, jbyte value) {
    return writeByte(PTRACE_PEEKDATA, PTRACE_POKEDATA, processID, (Address) address, value);
}

JNIEXPORT jboolean JNICALL
Java_com_sun_max_tele_debug_linux_Ptrace_nativeWriteTextByte(JNIEnv *env, jclass c, jint processID, jlong address, jbyte value) {
    return writeByte(PTRACE_PEEKTEXT, PTRACE_POKETEXT, processID, (Address) address, value);
}

typedef struct Status {
    int pid;
    char comm[256];
    char state;
    int ppid;
    int pgrp;
    int session;
    int tty_nr;
    int tpgid;
    unsigned long flags;
    unsigned long minflt;
    unsigned long cminflt;
    unsigned long majflt;
    unsigned long cmajflt;
    unsigned long utime;
    unsigned long stime;
    int cutime;
    int cstime;
    int priority;
    int nice;
    int zero;
    int itrealvalue;
    unsigned long starttime;
    unsigned long vsize;
    int rss;
    unsigned long rlim;
    unsigned long startcode;
    unsigned long endcode;
    unsigned long startstack;
    unsigned long kstkesp;
    unsigned long kstkeip;
    unsigned long signal;
    unsigned long blocked;
    unsigned long sigignore;
    unsigned long sigcatch;
    unsigned long wchan;
    unsigned long nswap;
    unsigned long cnswap;
    int exit_signal;
    int processor;
} *Status;

static Boolean readStatus(int processID, Status s) {
    char filename[256];
    FILE *f;
    int n;

    sprintf(filename, "/proc/%d/stat", processID);
    f = fopen(filename, "r");
    if (f == NULL) {
        return false;
    }
    n = fscanf(f, "%d %s %c %d %d %d %d %d %lu %lu %lu %lu %lu %lu %lu %d %d %d %d %d %d %lu %lu %d %lu %lu %lu %lu %lu %lu %lu %lu %lu %lu %lu %lu %lu %d %d",
               &s->pid, s->comm, &s->state, &s->ppid, &s->pgrp, &s->session, &s->tty_nr, &s->tpgid, &s->flags,
               &s->minflt, &s->cminflt, &s->majflt, &s->cmajflt, &s->utime, &s->stime, &s->cutime, &s->cstime,
               &s->priority, &s->nice, &s->zero, &s->itrealvalue, &s->starttime, &s->vsize, &s->rss, &s->rlim,
               &s->startcode, &s->endcode, &s->startstack, &s->kstkesp, &s->kstkeip, &s->signal, &s->blocked,
               &s->sigignore, &s->sigcatch, &s->wchan, &s->nswap, &s->cnswap, &s->exit_signal, &s->processor);
    fclose(f);
    return n == 39;
}

static jlong getInstructionPointerFromProc(int processID) {
    struct Status statusStruct;

    if (!readStatus(processID, &statusStruct)) {
        return -1;
    }
    return statusStruct.kstkeip;
}

JNIEXPORT jlong JNICALL
Java_com_sun_max_tele_debug_linux_Ptrace_nativeGetInstructionPointer(JNIEnv *env, jclass c, jint processID) {
    struct user_regs_struct registers;
    if (ptrace(PTRACE_GETREGS, processID, 0, &registers) == 0) {
        return registers.rip;
    }
    /* ptrace() is unreliable, but we can fall back on a different method of getting the same result.
     * This one has the disadvantage that it sometimes reports an outdated result if we tried it first.
     * But using it second, it usually works.
     */
    log_println("ptrace falling back on /proc");
    return getInstructionPointerFromProc(processID);
}

JNIEXPORT jboolean JNICALL
Java_com_sun_max_tele_debug_linux_Ptrace_nativeSetInstructionPointer(JNIEnv *env, jclass c, jint processID, jlong instructionPointer) {
    struct user_regs_struct registers;

    if (ptrace(PTRACE_GETREGS, processID, 0, &registers) != 0) {
        return false;
    }
    registers.rip = instructionPointer;
    return ptrace(PTRACE_SETREGS, processID, 0, &registers) == 0;
}

static jlong getStackPointerFromProc(int processID) {
    struct Status statusStruct;

    if (!readStatus(processID, &statusStruct)) {
        return -1;
    }
    return statusStruct.kstkesp;
}

JNIEXPORT jlong JNICALL
Java_com_sun_max_tele_debug_linux_Ptrace_nativeGetStackPointer(JNIEnv *env, jclass c, jint processID) {
    struct user_regs_struct registers;
    if (ptrace(PTRACE_GETREGS, processID, 0, &registers) == 0) {
        return registers.rsp;
    }
    /* ptrace() is unreliable, but we can fall back on a different method of getting the same result.
     * This one has the disadvantage that it sometimes reports an outdated result if we tried it first.
     * But using it second, it usually works.
     */
    log_println("ptrace falling back on /proc");
    return getStackPointerFromProc(processID);
}

JNIEXPORT jboolean JNICALL
Java_com_sun_max_tele_debug_linux_Ptrace_nativeReadIntegerRegisters(JNIEnv *env, jclass c, jint processID, jbyteArray buffer, jint length) {
    isa_CanonicalIntegerRegistersStruct canonicalIntegerRegisters;

    if (length > sizeof(canonicalIntegerRegisters)) {
        log_println("buffer for register data is too large");
        return false;
    }

    struct user_regs_struct osRegisters;
    if (ptrace(PTRACE_GETREGS, processID, 0, &osRegisters) != 0) {
        return false;
    }

	isa_canonicalizeTeleIntegerRegisters(&osRegisters, &canonicalIntegerRegisters);

    (*env)->SetByteArrayRegion(env, buffer, 0, length, (void *) &canonicalIntegerRegisters);
    return true;
}

JNIEXPORT jboolean JNICALL
Java_com_sun_max_tele_debug_linux_Ptrace_nativeReadFloatingPointRegisters(JNIEnv *env, jclass c, jint processID, jbyteArray buffer, jint length) {
    isa_CanonicalIntegerRegistersStruct canonicalIntegerRegisters;

    if (length > sizeof(canonicalIntegerRegisters)) {
        log_println("buffer for register data is too large");
        return false;
    }

    struct user_regs_struct osRegisters;
    if (ptrace(PTRACE_GETREGS, processID, 0, &osRegisters) != 0) {
        return false;
    }

    isa_canonicalizeTeleIntegerRegisters(&osRegisters, &canonicalIntegerRegisters);

    (*env)->SetByteArrayRegion(env, buffer, 0, length, (void *) &canonicalIntegerRegisters);
    return true;
}
