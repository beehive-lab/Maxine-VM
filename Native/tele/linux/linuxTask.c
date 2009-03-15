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
 * Functions for controlling and accessing the memory of a Linux task (i.e. thread or process) via ptrace(2).
 *
 * @author Bernd Mathiske
 * @author Doug Simon
 */
#include <sys/wait.h>
#include <stdlib.h>
#include <unistd.h>
#include <errno.h>
#include <fcntl.h>
#include <string.h>

#include "log.h"
#include "ptrace.h"
#include "linuxTask.h"

int task_read_registers(pid_t tid,
    isa_CanonicalIntegerRegistersStruct *canonicalIntegerRegisters,
    isa_CanonicalStateRegistersStruct *canonicalStateRegisters,
    isa_CanonicalFloatingPointRegistersStruct *canonicalFloatingPointRegisters) {

    if (canonicalIntegerRegisters != NULL || canonicalStateRegisters != NULL) {
        struct user_regs_struct osIntegerRegisters;
        if (ptrace(PT_GETREGS, tid, 0, &osIntegerRegisters) != 0) {
            return -1;
        }
        if (canonicalIntegerRegisters != NULL) {
            isa_canonicalizeTeleIntegerRegisters(&osIntegerRegisters, canonicalIntegerRegisters);
        }
        if (canonicalStateRegisters != NULL) {
            isa_canonicalizeTeleStateRegisters(&osIntegerRegisters, canonicalStateRegisters);
        }
    }

    if (canonicalFloatingPointRegisters != NULL) {
        struct user_fpregs_struct osFloatRegisters;
        if (ptrace(PT_GETFPREGS, tid, 0, &osFloatRegisters) != 0) {
            return -1;
        }
        isa_canonicalizeTeleFloatingPointRegisters(&osFloatRegisters, canonicalFloatingPointRegisters);
    }

    return 0;
}

static jboolean handleNewThread(int tid) {
    /* Most likely a new thread. */
    int result;
    int status;
    do {
        tele_log_println("Waiting for new task %d to stop", tid);
        result = waitpid(tid, &status, __WALL);
    } while (result == -1 && errno == EINTR);

    if (result == -1) {
        log_exit(11, "Error waiting for new task %d [%s]", tid, strerror(errno));
    } else if (result != tid) {
        log_println("Wait returned unexpected PID %d", result);
        return false;
    } else if (!WIFSTOPPED(status) || WSTOPSIG(status) != SIGSTOP) {
        log_println("Wait returned status %p", status);
        return false;
    }

    ptrace(PT_SETOPTIONS, tid, 0, PTRACE_O_TRACECLONE);
    ptrace(PT_CONTINUE, tid, 0, 0);
    return true;
}

/**
 * Reads the stat of a task from /proc/<tgid>/task/<tid>/stat. See proc(5).
 *
 * @param format the format string for parsing the fields of the stat string
 * @param ... the arguments for storing the fields parsed from the stat string according to 'format'
 */
void task_stat(pid_t tgid, pid_t tid, const char* format, ...) {
    char *buf;
    if (asprintf(&buf, "/proc/%d/task/%d/stat", tid, tid) < 0) {
        log_println("Error calling asprintf(): %s", strerror(errno));
        return;
    }
    FILE *procFile = fopen(buf, "r");
    va_list ap;
    va_start(ap, format);
    if (vfscanf(procFile, format, ap) == EOF) {
        log_println("Error reading %s: %s", buf, strerror(errno));
    }
    va_end(ap);
    if (fclose(procFile) == EOF) {
        log_println("Error closing %s: %s", buf, strerror(errno));
    }
    free(buf);
}

char task_state(pid_t tgid, pid_t tid) {
    char state = 'Z';
    task_stat(tgid, tid, "%*d %*s %c", &state);
    return state;
}

static jboolean waitForSignal(pid_t tgid, pid_t tid, int signalnum) {
    while (1) {
        int status;

        tele_log_println("Waiting for task %d to receive signal %d [%s]", tid, signalnum, strsignal(signalnum));

        int result = waitpid(tid, &status, 0);
        int error = errno;
        if (result != tid) {
            log_println("waitpid failed with error %d [%s], status %p", error, strerror(error), status);
            return false;
        }

        if (WIFEXITED(status)) {
            log_println("Task %d exited with exit status %d", tid, WEXITSTATUS(status));
            return false;
        }
        if (WIFSIGNALED(status)) {
            int signal = WTERMSIG(status);
            log_println("Task %d terminated by signal %d [%s]", tid, signal, strsignal(signal));
            return false;
        }
        if (WIFSTOPPED(status)) {
            // check whether the task received a signal, and continue with it if so.
            int signal = WSTOPSIG(status);
            tele_log_println("Task %d stopped by signal %d [%s]", tid, signal, strsignal(signal));

            int event = ptraceEvent(status);
            Boolean newThreadStarted = false;
            if (event != 0) {
                unsigned long eventMsg;
                ptrace(PT_GETEVENTMSG, tid, NULL, &eventMsg);
                tele_log_println("Task %d received ptrace event %d [%s] with message %ul", tid, event, ptraceEventName(event), eventMsg);
                if (event == PTRACE_EVENT_CLONE) {
                    newThreadStarted = true;
                    if (!handleNewThread(eventMsg)) {
                        return false;
                    }
                }
            }

            if (signal == 0 || signal == signalnum) {
                return true;
            }

            tele_log_println("Continuing task %d", tid);
            ptrace(PT_CONTINUE, tid, NULL, signal);
            error = errno;
            if (error != 0) {
                log_println("Continuing task %d failed: %s", tid, strerror(error));
                return false;
            }
        }
    }
}

/* Used to enforce the constraint that all access of the ptraced process from the same process. */
extern pid_t _ptracer;

JNIEXPORT jint JNICALL
Java_com_sun_max_tele_debug_linux_LinuxTask_nativeCreateChildProcess(JNIEnv *env, jclass c, jlong commandLineArgumentArray, jint vmAgentPort) {
    char **argv = (char**) commandLineArgumentArray;

    c_ASSERT(_ptracer == 0);
    _ptracer = getpid();

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

        /* Make the VM in its own process group. */
        //setpgid(0, 0);

        /* This call does not return if it succeeds: */
        tele_log_println("Launching VM executable: %s", argv[0]);
        execv(argv[0], argv);

        log_exit(1, "ptrace failed in child process");
    } else {
        /*parent:*/
        int status;
        if (waitpid(childPid, &status, 0) == childPid && WIFSTOPPED(status)) {
            /* Configure child so that it traps when it exits or as it starts new threads */
            ptrace(PT_SETOPTIONS, childPid, 0, PTRACE_O_TRACEEXIT | PTRACE_O_TRACECLONE);
            return childPid;
        }
    }
    return -1;
}

JNIEXPORT jboolean JNICALL
Java_com_sun_max_tele_debug_linux_LinuxTask_nativeDetach(JNIEnv *env, jclass c, jint tgid, jint tid) {
    return ptrace(PT_DETACH, tid, 0, 0) == 0;
}

JNIEXPORT jboolean JNICALL
Java_com_sun_max_tele_debug_linux_LinuxTask_nativeSingleStep(JNIEnv *env, jclass c, jint tgid, int tid) {
    if (ptrace(PT_STEP, tid, 0, 0) != 0) {
        return false;
    }
    task_wait_for_state(tgid, tid, "T");
    return true;
}

JNIEXPORT jboolean JNICALL
Java_com_sun_max_tele_debug_linux_LinuxTask_nativeSuspend(JNIEnv *env, jclass c, jint tgid, jint tid, jboolean allTasks) {
    pid_t killID = allTasks ? -getpgid(tgid) : tid;
    if (kill(killID, SIGTRAP) != 0) {
        int error = errno;
        log_println("Error sending SIGTRAP to suspend %s %d: %s", tid, (allTasks ? "all tasks in group" : "task"), strerror(error));
        return false;
    }
    return true;
}

JNIEXPORT jboolean JNICALL
Java_com_sun_max_tele_debug_linux_LinuxTask_nativeResume(JNIEnv *env, jclass c, jint tgid, jint tid) {
    return ptrace(PT_CONTINUE, tid, NULL, 0) == 0;
}

JNIEXPORT jboolean JNICALL
Java_com_sun_max_tele_debug_linux_LinuxTask_nativeWait(JNIEnv *env, jclass c, jint tgid, jint tid) {
    return waitForSignal(tgid, tid, SIGTRAP);
}

JNIEXPORT jboolean JNICALL
Java_com_sun_max_tele_debug_linux_LinuxTask_nativeKill(JNIEnv *env, jclass c, jint tgid, jint tid) {
    return ptrace(PT_KILL, tid, 0, 0) == 0;
}

static int _task_memory_read_fd = 0;
static pid_t _task_memory_pid;

/**
 * Gets an open file descriptor on /proc/<pid>/mem for reading the memory of the traced process 'tgid'.
 * Unfortunately, this mechanism cannot be used for writing; ptrace must be used instead.
 *
 * TODO: Ensure that the file descriptor is closed once tracing is complete.
 */
int task_memory_read_fd(int tgid, void *address) {
    if (_ptracer != getpid()) {
        log_println("Can only read memory of %d from tracer process %d, not process %d", tgid, _ptracer, getpid());
        errno = EINVAL;
        return -1;
    }
    if (_task_memory_read_fd == 0) {
        _task_memory_pid = tgid;
        char *memoryFileName;
        asprintf(&memoryFileName, "/proc/%d/mem", tgid);
        c_ASSERT(memoryFileName != NULL);
        _task_memory_read_fd = open(memoryFileName, O_RDONLY);
        c_ASSERT(_task_memory_read_fd != -1);
        free(memoryFileName);
    } else {
        c_ASSERT(tgid == _task_memory_pid);
    }
    off64_t fdOffset = (off64_t) address;
    if (lseek64(_task_memory_read_fd, fdOffset, SEEK_SET) != fdOffset) {
        int error = errno;
        log_println("Error seeking memory file for process %d to %p [%ul]: %s", tgid, address, fdOffset, strerror(error));
    }
    return _task_memory_read_fd;
}

/**
 * Copies 'size' bytes from 'src' in the address space of 'tgid' to 'dst' in the caller's address space.
 */
size_t task_read(pid_t tgid, pid_t tid, void *src, void *dst, size_t size) {
    task_wait_for_state(tgid, tgid, "T");
    //tele_log_println("Reading %d bytes from memory of task %d at %p", size, tid, src);
    if (size <= sizeof(Address)) {
        Address word = ptrace(PT_READ_D, tid, (Address) src, NULL);
        if (errno != 0) {
            int error = errno;
            log_println("Could not read word at %p: %s", src, strerror(error));
            return -1;
        } else {
            memcpy(dst, &word, size);
        }
        return size;
    } else {
        size_t bytesRead = read(task_memory_read_fd(tgid, src), dst, size);
        if (bytesRead != size) {
            int error = errno;
            log_println("Only read %d of %d bytes from %p: %s", bytesRead, size, src, strerror(error));
        }
        return bytesRead;
    }
}

/**
 * Copies 'size' bytes from 'src' in the caller's address space to 'dst' in the address space of 'tgid'.
 * The value of 'size' must be >= 0 and < sizeof(Word).
 */
int task_write_subword(jint tgid, jint tid, void *dst, const void *src, size_t size) {
    if (size == 0) {
        return 0;
    }
    c_ASSERT(size < sizeof(Word));

    Address word = ptrace(PT_READ_D, tid, dst, NULL);
    if (errno != 0) {
        log_println("Could not read word at %p into which %d bytes will be masked", dst, size);
        return 0;
    }

    const int bitsPerByte = 8;
    Address mask = ((Address) 1 << (size * bitsPerByte)) - 1;
    Address data = *((Address *) src);
    Address value = data & mask;
    Address result = value | (word & ~mask);
#if 0
    log_println("size: %d", size);
    log_println("mask: %p", mask);
    log_println("val:  %p", value);
    log_println("old:  %p", word);
    log_println("new:  %p", result);
#endif
    if (ptrace(PT_WRITE_D, tid, dst, result) != 0) {
        log_println("Failed to write %d bytes to %p", size, dst);
        return 0;
    }
    return size;
}

/**
 * Copies 'size' bytes from 'src' in the caller's address space to 'dst' in the address space of 'tgid'.
 */
size_t task_write(pid_t tgid, pid_t tid, void *dst, const void *src, size_t size) {
    if (size == 0) {
        return 0;
    }
    task_wait_for_state(tgid, tid, "T");

    size_t bytesWritten = 0;
    const size_t wholeWords = size / sizeof(Word);
    if (wholeWords > 0) {
        Word* wordBuffer = (Word *) src;
        size_t i;
        for (i = 0 ; i < wholeWords; i++) {
            if (ptrace(PT_WRITE_D, tid, dst + bytesWritten, wordBuffer[i]) != 0) {
                log_println("Only wrote %d of %d bytes to %p", bytesWritten, size, dst);
                return bytesWritten;
            }
            bytesWritten += sizeof(Word);
        }
    }
    size_t remainingBytes = size - bytesWritten;
    c_ASSERT(remainingBytes < sizeof(Word));
    if (remainingBytes != 0) {
        bytesWritten += task_write_subword(tgid, tid, dst + bytesWritten, src + bytesWritten, remainingBytes);
    }
    return bytesWritten;
}

JNIEXPORT jint JNICALL
Java_com_sun_max_tele_debug_linux_LinuxTask_nativeWriteBytes(JNIEnv *env, jclass c, jint tgid, jint tid, jlong address, jbyteArray byteArray, jint offset, jint length) {
    void* buffer;
    Word bufferWord;
    size_t size = (size_t) length;
    if (size > sizeof(Address)) {
        buffer = (void *) malloc(size * sizeof(jbyte));
        if (buffer == NULL) {
            log_println("Failed to malloc buffer of %d bytes", size);
            return -1;
        }
    } else {
        buffer = (void *) &bufferWord;
    }

    (*env)->GetByteArrayRegion(env, byteArray, offset, length, buffer);
    if ((*env)->ExceptionOccurred(env) != NULL) {
        log_println("Failed to copy %d bytes from byteArray into buffer", size);
        return -1;
    }

    size_t bytesWritten = task_write(tgid, tid, (void *) address, buffer, size);
    if (buffer != &bufferWord) {
        free(buffer);
    }
    return bytesWritten;
}


JNIEXPORT jint JNICALL
Java_com_sun_max_tele_debug_linux_LinuxTask_nativeReadBytes(JNIEnv *env, jclass c, jint tgid, jint tid, jlong address, jbyteArray byteArray, jint offset, jint length) {
    void* buffer;
    Word bufferWord;
    size_t size = (size_t) length;
    if (size > sizeof(Word)) {
        buffer = (void *) malloc(size);
        if (buffer == 0) {
            log_println("Failed to malloc byteArray of %d bytes", size);
            return -1;
        }
    } else {
        buffer = (void *) &bufferWord;
    }

    size_t bytesRead = task_read(tgid, tid, (void *) address, buffer, size);
    if ((jint) bytesRead > 0) {
        (*env)->SetByteArrayRegion(env, byteArray, offset, bytesRead, buffer);
    }
    if (size > sizeof(Word)) {
        free(buffer);
    }

    return bytesRead;
}


JNIEXPORT jboolean JNICALL
Java_com_sun_max_tele_debug_linux_LinuxTask_nativeSetInstructionPointer(JNIEnv *env, jclass c, jint tid, jlong instructionPointer) {
    struct user_regs_struct registers;

    if (ptrace(PT_GETREGS, tid, 0, &registers) != 0) {
        return false;
    }
    registers.rip = instructionPointer;
    return ptrace(PT_SETREGS, tid, 0, &registers) == 0;
}

JNIEXPORT jboolean JNICALL
Java_com_sun_max_tele_debug_linux_LinuxTask_nativeReadRegisters(JNIEnv *env, jclass c, jint tid,
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

    if (task_read_registers(tid, &canonicalIntegerRegisters, &canonicalStateRegisters, &canonicalFloatingPointRegisters) != 0) {
        return false;
    }
    (*env)->SetByteArrayRegion(env, integerRegisters, 0, integerRegistersLength, (void *) &canonicalIntegerRegisters);
    (*env)->SetByteArrayRegion(env, stateRegisters, 0, stateRegistersLength, (void *) &canonicalStateRegisters);
    (*env)->SetByteArrayRegion(env, floatingPointRegisters, 0, floatingPointRegistersLength, (void *) &canonicalFloatingPointRegisters);
    return true;
}
