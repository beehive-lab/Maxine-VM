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
#include <stdlib.h>
#include <unistd.h>
#include <errno.h>
#include <fcntl.h>
#include <string.h>
#include <signal.h>
#include <syscall.h>
#include <sys/wait.h>
#include <sys/time.h>

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

/**
 * Prints the set of signals implied by a given signal mask in a human readable form to the log stream.
 */
void log_signal_mask(unsigned long signalMask) {
    size_t signal;
    Boolean first = true;
    for (signal = 0; signal < sizeof(signalMask); ++signal) {
        if ((signalMask & (1 << signal)) != 0) {
            if (first) {
                first = false;
            } else {
                log_print(", ");
            }
            log_print("%d [%s]", signal, strsignal(signal));
        }
    }
}

/**
 * Prints the contents of /proc/<tgid>/task/<tid>/stat in a human readable to the log stream.
 *
 * @param tgid a task group id
 * @param tid a task id
 * @param messageFormat the format specification of the message to be printed to the log prior to the stat details
 */
void log_task_stat(pid_t tgid, pid_t tid, const char* messageFormat, ...) {
    char *buf;
    if (asprintf(&buf, "/proc/%d/task/%d/stat", tid, tid) < 0) {
        log_println("Error calling asprintf(): %s", strerror(errno));
        return;
    }
    FILE *sp = fopen(buf, "r");
    va_list ap;
    va_start(ap,messageFormat);
    log_print_vformat(messageFormat, ap);
    va_end(ap);
    log_print_newline();

#define _STAT_FIELD(type, name, format, print) do { \
    type name; \
    fscanf(sp, format " ", &name); \
    if (print) { \
        log_println("  %20s: " format, STRINGIZE(name), name); \
    } \
} while (0)

#define STAT_FIELD(type, name, format) _STAT_FIELD(type, name, format, true)
#define STAT_FIELD_SKIP(type, name, format) _STAT_FIELD(type, name, format, false)

#define STAT_STRING_FIELD(name) do { \
    char *name; \
    fscanf(sp, "%as ", &name); \
        log_println("  %20s: %s", STRINGIZE(name), name); \
} while (0)

#define STAT_SIGNAL_MASK_FIELD(name) do { \
    unsigned long name; \
    fscanf(sp, "%lu ", &name); \
    log_print("  %20s: ", STRINGIZE(name)); \
    log_signal_mask(name); \
    log_print_newline(); \
} while (0)

#define STAT_SIGNAL_MASK_FIELD_SKIP(name) _STAT_FIELD(unsigned long, name, "%lu", false)

    STAT_FIELD(pid_t, TID, "%d");
    STAT_STRING_FIELD(comm);
    STAT_FIELD(char, State, "%c");
    STAT_FIELD(pid_t, PPID, "%d");
    STAT_FIELD(pid_t, TGID, "%d");
    STAT_FIELD_SKIP(int, Session, "%d");
    STAT_FIELD_SKIP(int, TTY, "%d");
    STAT_FIELD_SKIP(int, TTY_PGID, "%d");
    STAT_FIELD_SKIP(unsigned, Flags, "%u");
    STAT_FIELD_SKIP(unsigned long, MinorFaults, "%lu");
    STAT_FIELD_SKIP(unsigned long, MinorFaultsInChildren, "%lu");
    STAT_FIELD_SKIP(unsigned long, MajorFaults, "%lu");
    STAT_FIELD_SKIP(unsigned long, MajorFaultsInChildren, "%lu");
    STAT_FIELD_SKIP(unsigned long, UserTime, "%lu");
    STAT_FIELD_SKIP(unsigned long, KernelTime, "%lu");
    STAT_FIELD_SKIP(long, UserTimeChildren, "%ld");
    STAT_FIELD_SKIP(long, KernelTimeChildren, "%ld");
    STAT_FIELD_SKIP(long, priority, "%ld");
    STAT_FIELD_SKIP(long, Nice, "%ld");
    STAT_FIELD(long, NumberThreads, "%ld");
    STAT_FIELD_SKIP(long, SigAlarmCountdown, "%ld");
    STAT_FIELD_SKIP(unsigned long long, StartTime, "%llu");
    STAT_FIELD_SKIP(unsigned long, VirtualMemory, "%lu");
    STAT_FIELD_SKIP(long, RSS, "%ld");
    STAT_FIELD_SKIP(unsigned long, RSSLimit, "%lu");
    STAT_FIELD_SKIP(void *, StartCode, "%p");
    STAT_FIELD_SKIP(void *, EndCode, "%p");
    STAT_FIELD_SKIP(void *, StartStack, "%p");
    STAT_FIELD_SKIP(void *, KernelStackPointer, "%p");
    STAT_FIELD_SKIP(void *, KernelInstructionPointer, "%p");
    STAT_SIGNAL_MASK_FIELD_SKIP(PendingSignals);
    STAT_SIGNAL_MASK_FIELD_SKIP(BlockedSignals);
    STAT_SIGNAL_MASK_FIELD_SKIP(IgnoredSignals);
    STAT_SIGNAL_MASK_FIELD_SKIP(CaughtSignals);
    STAT_FIELD_SKIP(void *, WaitChannel, "%p");
    STAT_FIELD_SKIP(unsigned long, SwappedPages, "%lu");
    STAT_FIELD_SKIP(unsigned long, SwappedPagesChildren, "%lu");
    STAT_FIELD_SKIP(int, ExitSignal, "%d");
    STAT_FIELD(int, CPU, "%d");
    STAT_FIELD_SKIP(unsigned long, RealtimePriority, "%lu");
    STAT_FIELD_SKIP(unsigned long, SchedulingPolicy, "%lu");
    STAT_FIELD_SKIP(unsigned long long, BlockIODelays, "%llu");
#undef STAT_FIELD

    if (fclose(sp) == EOF) {
        log_println("Error closing %s: %s", buf, strerror(errno));
    }
    free(buf);
}

/**
 * Gets the state of a given task.
 *
 * @param tgid the task group id of the task
 * @param tid the id of the task
 * @return one of the following characters denoting the state of task 'tid':
 *     R: running
 *     S: sleeping in an interruptible wait
 *     D: waiting in uninterruptible disk sleep
 *     Z: zombie
 *     T: traced or stopped (on a signal)
 *     W: is paging
 */
char task_state(pid_t tgid, pid_t tid) {
    char state = 'Z';
    task_stat(tgid, tid, "%*d %*s %c", &state);
    return state;
}

static jboolean waitForSignal(pid_t tgid, pid_t tid, int signalnum) {
#define RETRY_COUNT 100
#define RETRY(format, ...) do { \
    if (retries-- <= 0) { \
        log_println("Gave up after %d attempts", RETRY_COUNT); \
        return false;\
    } else { \
        log_println(format, ##__VA_ARGS__); \
        log_println("Retrying in %dms ...", TASK_RETRY_PAUSE_MICROSECONDS / 1000); \
        usleep(TASK_RETRY_PAUSE_MICROSECONDS); \
        continue; \
    } \
} while(0)

    int retries = RETRY_COUNT;
    while (1) {
#if log_TELE
        log_task_stat(tgid, tid, "waitForSignal(%d, %d): status of %d:", tgid, tid, tid);
#endif
        char state = task_state(tgid, tid);
        switch (state) {
            case 'D': {
                RETRY("Task %d is in uninterruptible disk sleep", tid);
            }
            case 'W': {
                RETRY("Task %d is paging", tid);
            }
            case 'Z': {
                log_println("Task %d is in zombie state", tid);
                return true;
            }
            case 'S': {
                /* If this task is sleeping when another task hits a breakpoint or performs a single step, then this
                 * task will not receive the SIGTRAP for some unknown reason. So, we generate one manually to put it
                 * in the trapped state. */
                tele_log_println("Task %d is sleeping - Sending SIGTRAP to jog it into TRAPPED state", tid);
                if (kill(tid, SIGTRAP) != 0) {
                    int error = errno;
                    log_println("Error sending SIGTRAP to jog task %d: %s", tid, strerror(error));
                    return false;
                }
                break;
            }
        }

        tele_log_println("Waiting for task %d which is in state '%c' to receive signal %d [%s]", tid, task_state(tgid, tid), signalnum, strsignal(signalnum));
        int status;
        int result = waitpid(tid, &status, 0);
        if (result == -1) {
            int error = errno;
            if (error == ECHILD) {
                tele_log_println("Going to wait again for task %d which is in state '%c' to receive signal %d [%s]", tid, task_state(tgid, tid), signalnum, strsignal(signalnum));
                result = waitpid(tid, &status, __WCLONE);
            }
            if (result == -1 && error == ECHILD) {
                RETRY("waitpid(%d) failed with result %d, error %d [%s]", tid, result, error, strerror(error));
            }
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

            /* The signal is now delivered to the VM. */
            tele_log_println("Continuing task %d", tid);
            if (ptrace(PT_CONTINUE, tid, NULL, signal) != 0) {
                int error = errno;
                log_println("Continuing task %d failed: %s", tid, strerror(error));
                return false;
            }
        }
    }
}

JNIEXPORT jint JNICALL
Java_com_sun_max_tele_debug_linux_LinuxTask_nativeCreateChildProcess(JNIEnv *env, jclass c, jlong commandLineArgumentArray, jint vmAgentPort) {
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

        /* Put the VM in its own process group. */
        setpgid(0, 0);

        /* This call does not return if it succeeds: */
        tele_log_println("Launching VM executable: %s", argv[0]);
        execv(argv[0], argv);

        log_exit(1, "ptrace failed in child process");
    } else {
        /*parent:*/
        int status;
        if (waitpid(childPid, &status, 0) == childPid && WIFSTOPPED(status)) {
            /* Configure child so that it traps when it exits or starts new threads */
            ptrace(PT_SETOPTIONS, childPid, 0, PTRACE_O_TRACECLONE);
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
Java_com_sun_max_tele_debug_linux_LinuxTask_nativeSingleStep(JNIEnv *env, jclass c, jint tgid, int tid) {
    if (ptrace(PT_STEP, tid, 0, 0) != 0) {
        return false;
    }
//    task_wait_for_state(tgid, tid, "T");
    return true;
}

JNIEXPORT jboolean JNICALL
Java_com_sun_max_tele_debug_linux_LinuxTask_nativeResume(JNIEnv *env, jclass c, jint tgid, jint tid) {
    int result = ptrace(PT_CONTINUE, tid, NULL, 0);
    return result == 0;
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
    ptrace_check_tracer(POS, tgid);
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
    //task_wait_for_state(tgid, tgid, "T");
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
    //task_wait_for_state(tgid, tid, "T");

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
