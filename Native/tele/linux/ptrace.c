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

#if 0 /* See ptrace.h */
/* Linux ptrace() is unreliable, but apparently retrying after descheduling helps. */
long ptrace_withRetries(int request, int processID, Address address, void *data) {
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
#endif

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

/**
 * Checks that the result of a ptrace operation was 0, printing a warning message if it wasn't.
 *
 * @param result the result of a call to ptrace
 * @param processID the identifier of the process or LWP (i.e. thread) on which the operation was performed
 * @param action a descriptive name for the operation (e.g. "Stepping" or "Resuming")
 * @return true if the call to ptrace returned 0, false otherwise
 */
static jboolean check_result_0(int result, jint processID, const char *action) {
    if (result != 0) {
        int error = errno;
        log_println("%s process %d failed: %s", action, processID, strerror(error));
        return false;
    }
    return true;
}

JNIEXPORT jboolean JNICALL
Java_com_sun_max_tele_debug_linux_Ptrace_nativeAttach(JNIEnv *env, jclass c, jint processID) {
    return check_result_0(ptrace(PTRACE_ATTACH, processID, 0, 0), processID, "Attaching");
}

JNIEXPORT jboolean JNICALL
Java_com_sun_max_tele_debug_linux_Ptrace_nativeDetach(JNIEnv *env, jclass c, jint processID) {
    return check_result_0(ptrace(PTRACE_DETACH, processID, 0, 0), processID, "Detaching");
}

JNIEXPORT jboolean JNICALL
Java_com_sun_max_tele_debug_linux_Ptrace_nativeSingleStep(JNIEnv *env, jclass c, jint processID) {
    return check_result_0(ptrace(PTRACE_SINGLESTEP, processID, 0, 0), processID, "Stepping");
}

JNIEXPORT jboolean JNICALL
Java_com_sun_max_tele_debug_linux_Ptrace_nativeSuspend(JNIEnv *env, jclass c, jint processID) {
    return check_result_0(kill(processID, SIGTRAP), processID, "Suspending");
}

JNIEXPORT jboolean JNICALL
Java_com_sun_max_tele_debug_linux_Ptrace_nativeResume(JNIEnv *env, jclass c, jint processID) {
    return check_result_0(ptrace(PTRACE_CONT, processID, 0, 0), processID, "Resuming");
}

JNIEXPORT jboolean JNICALL
Java_com_sun_max_tele_debug_linux_Ptrace_nativeWait(JNIEnv *env, jclass c, jint processID) {
    return ptraceWaitForSignal(processID, SIGTRAP);
}

JNIEXPORT jboolean JNICALL
Java_com_sun_max_tele_debug_linux_Ptrace_nativeKill(JNIEnv *env, jclass c, jint processID) {
    return check_result_0(ptrace(PTRACE_KILL, processID, 0, 0), processID, "Killing");
}

JNIEXPORT jint JNICALL
Java_com_sun_max_tele_debug_linux_Ptrace_nativeWriteBytes(JNIEnv *env, jclass c, jint processID, jlong address, jbyteArray byteArray, jint offset, jint length) {
    jbyte* buffer = (jbyte *) malloc(length * sizeof(jbyte));
    if (buffer == 0) {
        log_println("failed to malloc byteArray of %d bytes", length);
        return -1;
    }

    (*env)->GetByteArrayRegion(env, byteArray, offset, length, buffer);
    if ((*env)->ExceptionOccurred(env) != NULL) {
        log_println("failed to copy %d bytes from byteArray into buffer", length);
        return -1;
    }

    int bytesWritten = 0;
    Boolean ok = true;
    int wholeWords = length / sizeof(Word);
    if (wholeWords > 0) {
        Word* wordBuffer = (Word *) buffer;
        int i;
        for (i = 0 ; i < wholeWords; i++) {
            if (!check_result_0(ptrace(PTRACE_POKEDATA, processID, (Address) address + bytesWritten, wordBuffer[i]), processID, "Writing data to")) {
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
        Address word = ptrace(PTRACE_PEEKDATA, processID, highAddress, NULL);
        if (errno != 0) {
            log_println("Could not read word at %p into which remaining bytes will be masked", highAddress);
        } else {
            Address allOnes = -1;
            Address mask = allOnes << (remainingBytes * 8);
            Address *remainingBuffer = (Address *) (buffer + bytesWritten);
            Address data = *((Address *) remainingBuffer);

            Address result = (data & ~mask) | (word & mask);

            if (!check_result_0(ptrace(PTRACE_POKEDATA, processID, (Address) highAddress, result), processID, "Writing data to")) {
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
Java_com_sun_max_tele_debug_linux_Ptrace_nativeReadBytes(JNIEnv *env, jclass c, jint processID, jlong address, jbyteArray byteArray, jint offset, jint length) {
    int alignedLength = wordAlign(length);
    jbyte* buffer = (jbyte *) malloc(alignedLength);
    if (buffer == 0) {
        log_println("failed to malloc byteArray of %d bytes", alignedLength);
        return -1;
    }

    int bytesRead = 0;

    int wholeWords = alignedLength / sizeof(Word);
    Boolean ok = true;
    Address* wordBuffer = (Address *) buffer;
    int i;
    for (i = 0; i < wholeWords; i++) {
        int off = (i * sizeof(Word));
        Address word = ptrace(PTRACE_PEEKDATA, processID, (Address) address + off, NULL);
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
Java_com_sun_max_tele_debug_linux_Ptrace_nativeSetInstructionPointer(JNIEnv *env, jclass c, jint processID, jlong instructionPointer) {
    struct user_regs_struct registers;

    if (ptrace(PTRACE_GETREGS, processID, 0, &registers) != 0) {
        return false;
    }
    registers.rip = instructionPointer;
    return ptrace(PTRACE_SETREGS, processID, 0, &registers) == 0;
}

JNIEXPORT jboolean JNICALL
Java_com_sun_max_tele_debug_linux_Ptrace_nativeReadRegisters(JNIEnv *env, jclass c, jint processID,
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
    if (ptrace(PTRACE_GETREGS, processID, 0, &osIntegerRegisters) != 0) {
        return false;
    }

    struct user_fpregs_struct osFloatRegisters;
    if (ptrace(PTRACE_GETREGS, processID, 0, &osFloatRegisters) != 0) {
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
