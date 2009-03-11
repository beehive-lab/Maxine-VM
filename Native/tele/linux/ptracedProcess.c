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
 * @author Doug Simon
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

#include "log.h"
#include "word.h"
#include "isa.h"
#include "jni.h"
#include "ptrace.h"

#if log_TELE
#define tele_log_println log_println
#else
#define log_null(format, ...)
#define tele_log_println log_null
#endif

jboolean waitForSignal(pid_t pid, int signalnum) {
    while (1) {
        int status;
        tele_log_println("Waiting for process %d to receive signal %d [%s]", pid, signalnum, strsignal(signalnum));
        int error = waitpid(pid, &status, 0);

        if (error != pid) {
            log_println("waitpid failed with error: %d [%s]", errno, strerror(error));
            return false;
        }
        if (WIFEXITED(status)) {
            log_println("Process %d exited with exit status %d", pid, WEXITSTATUS(status));
            return false;
        }
        if (WIFSIGNALED(status)) {
            int signal = WTERMSIG(status);
            log_println("Process %d terminated by signal %d [%s]", pid, signal, strsignal(signal));
            return false;
        }
        if (WIFSTOPPED(status)) {
            // check whether the process received a signal, and continue with it if so.
            int signal = WSTOPSIG(status);

            if (signal == 0 || signal == signalnum) {
                tele_log_println("Process %d stopped by signal %d [%s]", pid, signal, strsignal(signal));
                return true;
            } else {
                ptrace(PT_CONTINUE, pid, (char*) 1, signal);
                error = errno;
                if (error != 0) {
                    log_println("Continuing process %d failed: %s", pid, strerror(error));
                    return false;
                }
            }
        }
    }
}

JNIEXPORT jint JNICALL
Java_com_sun_max_tele_debug_linux_PTracedProcess_nativeCreateChildProcess(JNIEnv *env, jclass c, jlong commandLineArgumentArray, jint vmAgentPort) {
    char **argv = (char**) commandLineArgumentArray;

    int childPid = fork();
    if (childPid == 0) {
        /*child:*/
        tele_log_println("Attaching ptrace to VM process %d", getpid());
        if (ptrace(PT_TRACEME, 0, 0, 0) != 0) {
            log_exit(1, "Failed to attach ptrace to VM process %d", getpid());
        }

        char *portDef;
        if (asprintf(&portDef, "MAX_AGENT_PORT=%u", vmAgentPort) == -1) {
            log_exit(1, "Could not allocate space for setting MAX_AGENT_PORT environment variable");
        }
        putenv(portDef);

        /* This call does not return if it succeeds: */
        tele_log_println("Launching VM executable: %s", argv[0]);
        execv(argv[0], argv);

        log_exit(1, "ptrace failed in child process");
    } else {
        /*parent:*/
        int status;
        if (waitpid(childPid, &status, 0) == childPid && WIFSTOPPED(status)) {
            /* The child traps as starts new threads */
            ptrace(PT_SETOPTIONS, childPid, 0, PTRACE_O_TRACECLONE | PTRACE_O_TRACEEXIT);
            return childPid;
        }
    }
    return -1;
}

JNIEXPORT jboolean JNICALL
Java_com_sun_max_tele_debug_linux_PTracedProcess_nativeAttach(JNIEnv *env, jclass c, jint pid) {
    return ptrace(PT_ATTACH, pid, 0, 0) == 0;
}

JNIEXPORT jboolean JNICALL
Java_com_sun_max_tele_debug_linux_PTracedProcess_nativeDetach(JNIEnv *env, jclass c, jint pid) {
    return ptrace(PT_DETACH, pid, 0, 0) == 0;
}

JNIEXPORT jboolean JNICALL
Java_com_sun_max_tele_debug_linux_PTracedProcess_nativeSingleStep(JNIEnv *env, jclass c, jint pid) {
    return ptrace(PT_STEP, pid, 0, 0) == 0;
}

JNIEXPORT jboolean JNICALL
Java_com_sun_max_tele_debug_linux_PTracedProcess_nativeSuspend(JNIEnv *env, jclass c, jint pid) {
    if (kill(pid, SIGTRAP) != 0) {
        int error = errno;
        log_println("Error sending SIGTRAP to suspend process %d: %s", pid, strerror(error));
        return false;
    }
    return true;
}

JNIEXPORT jboolean JNICALL
Java_com_sun_max_tele_debug_linux_PTracedProcess_nativeResume(JNIEnv *env, jclass c, jint pid) {
    return ptrace(PT_CONTINUE, pid, 0, 0) == 0;
}

JNIEXPORT jboolean JNICALL
Java_com_sun_max_tele_debug_linux_PTracedProcess_nativeWait(JNIEnv *env, jclass c, jint pid) {
    return waitForSignal(pid, SIGTRAP);
}

JNIEXPORT jboolean JNICALL
Java_com_sun_max_tele_debug_linux_PTracedProcess_nativeKill(JNIEnv *env, jclass c, jint pid) {
    return ptrace(PT_KILL, pid, 0, 0) == 0;
}

JNIEXPORT jint JNICALL
Java_com_sun_max_tele_debug_linux_PTracedProcess_nativeWriteBytes(JNIEnv *env, jclass c, jint pid, jlong address, jbyteArray byteArray, jint offset, jint length) {
    jbyte* buffer = (jbyte *) malloc(length * sizeof(jbyte));
    if (buffer == 0) {
        log_println("Failed to malloc byteArray of %d bytes", length);
        return -1;
    }

    (*env)->GetByteArrayRegion(env, byteArray, offset, length, buffer);
    if ((*env)->ExceptionOccurred(env) != NULL) {
        log_println("Failed to copy %d bytes from byteArray into buffer", length);
        return -1;
    }

    int bytesWritten = 0;
    Boolean ok = true;
    int wholeWords = length / sizeof(Word);
    if (wholeWords > 0) {
        Word* wordBuffer = (Word *) buffer;
        int i;
        for (i = 0 ; i < wholeWords; i++) {
            if (ptrace(PT_WRITE_D, pid, (Address) address + bytesWritten, wordBuffer[i]) != 0) {
                log_println("Only wrote %d of %d bytes", bytesWritten, length);
                ok = false;
                break;
            }
            bytesWritten += sizeof(Word);
        }
    }
    int remainingBytes = length - bytesWritten;
    if (ok && remainingBytes != 0) {
        Address highAddress = address + bytesWritten;
        /* Write remaining bytes */
        Address word = ptrace(PT_READ_D, pid, highAddress, NULL);
        if (errno != 0) {
            log_println("Could not read word at %p into which remaining bytes will be masked", highAddress);
        } else {
            Address allOnes = -1;
            Address mask = allOnes << (remainingBytes * 8);
            Address *remainingBuffer = (Address *) (buffer + bytesWritten);
            Address data = *((Address *) remainingBuffer);

            Address result = (data & ~mask) | (word & mask);

#if 0
            log_println("allOnes: %p", allOnes);
            log_println("mask: %p", mask);
            log_println("remainingBuffer: %p", remainingBuffer);
            log_println("data: %p", data);
            log_println("data & ~mask: %p", (data & ~mask));
            log_println("word & ~mask: %p", (word & ~mask));
            log_println("result: %p", result);
#endif

            if (ptrace(PT_WRITE_D, pid, (Address) highAddress, result) != 0) {
                log_println("Failed to write remaining bytes");
                ok = false;
            }
            bytesWritten += remainingBytes;
        }
    }

    free(buffer);
    return bytesWritten;
}

JNIEXPORT jint JNICALL
Java_com_sun_max_tele_debug_linux_PTracedProcess_nativeReadBytes(JNIEnv *env, jclass c, jint pid, jlong address, jbyteArray byteArray, jint offset, jint length) {
    int alignedLength = wordAlign(length);
    jbyte* buffer = (jbyte *) malloc(alignedLength);
    if (buffer == 0) {
        log_println("Failed to malloc byteArray of %d bytes", alignedLength);
        return -1;
    }

    int bytesRead = 0;

    int wholeWords = alignedLength / sizeof(Word);
    Boolean ok = true;
    Address* wordBuffer = (Address *) buffer;
    int i;
    for (i = 0; i < wholeWords; i++) {
        int off = (i * sizeof(Word));
        Address word = ptrace(PT_READ_D, pid, (Address) address + off, NULL);
        if (errno != 0) {
            log_println("Could not read word at %p+%d", address, off);
            ok = false;
            break;
        }
        wordBuffer[i] = word;
        bytesRead += sizeof(Word);
    }

    if (bytesRead > length) {
        bytesRead = length;
    }

    if ((jint) bytesRead > 0) {
        (*env)->SetByteArrayRegion(env, byteArray, offset, bytesRead, buffer);
    }
    free(buffer);

    return bytesRead;
}

JNIEXPORT jboolean JNICALL
Java_com_sun_max_tele_debug_linux_PTracedProcess_nativeSetInstructionPointer(JNIEnv *env, jclass c, jint pid, jlong instructionPointer) {
    struct user_regs_struct registers;

    if (ptrace(PT_GETREGS, pid, 0, &registers) != 0) {
        return false;
    }
    registers.rip = instructionPointer;
    return ptrace(PT_SETREGS, pid, 0, &registers) == 0;
}

JNIEXPORT jboolean JNICALL
Java_com_sun_max_tele_debug_linux_PTracedProcess_nativeReadRegisters(JNIEnv *env, jclass c, jint pid,
                jbyteArray integerRegisters, jint integerRegistersLength,
                jbyteArray floatingPointRegisters, jint floatingPointRegistersLength,
                jbyteArray stateRegisters, jint stateRegistersLength) {
    isa_CanonicalIntegerRegistersStruct canonicalIntegerRegisters;
    isa_CanonicalStateRegistersStruct canonicalStateRegisters;
    isa_CanonicalFloatingPointRegistersStruct canonicalFloatingPointRegisters;

    if (integerRegistersLength > (jint) sizeof(canonicalIntegerRegisters)) {
        log_println("buffer for integer register data is too large");
        return false;
    }

    if (stateRegistersLength > (jint) sizeof(canonicalStateRegisters)) {
        log_println("buffer for state register data is too large");
        return false;
    }

    if (floatingPointRegistersLength > (jint) sizeof(canonicalFloatingPointRegisters)) {
        log_println("buffer for floating point register data is too large");
        return false;
    }

    struct user_regs_struct osIntegerRegisters;
    if (ptrace(PT_GETREGS, pid, 0, &osIntegerRegisters) != 0) {
        return false;
    }

    struct user_fpregs_struct osFloatRegisters;
    if (ptrace(PT_GETFPREGS, pid, 0, &osFloatRegisters) != 0) {
        return false;
    }

    isa_canonicalizeTeleIntegerRegisters(&osIntegerRegisters, &canonicalIntegerRegisters);
    isa_canonicalizeTeleStateRegisters(&osIntegerRegisters, &canonicalStateRegisters);
    isa_canonicalizeTeleFloatingPointRegisters(&osFloatRegisters, &canonicalFloatingPointRegisters);

    (*env)->SetByteArrayRegion(env, integerRegisters, 0, integerRegistersLength, (void *) &canonicalIntegerRegisters);
    (*env)->SetByteArrayRegion(env, stateRegisters, 0, stateRegistersLength, (void *) &canonicalStateRegisters);
    (*env)->SetByteArrayRegion(env, floatingPointRegisters, 0, floatingPointRegistersLength, (void *) &canonicalFloatingPointRegisters);
    return true;
}
