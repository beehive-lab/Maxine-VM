/*
 * Copyright (c) 2007, 2010, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

/**
 * Functions for controlling and accessing the memory of a Linux task (i.e. thread or process) via ptrace(2).
 *
 * @author Bernd Mathiske
 * @author Doug Simon
 */
#include <stdlib.h>
#include <unistd.h>
#include <ctype.h>
#include <errno.h>
#include <fcntl.h>
#include <string.h>
#include <signal.h>
#include <dirent.h>
#include <sys/wait.h>
#include <sys/time.h>
#include <sys/prctl.h>

#include "log.h"
#include "ptrace.h"
#include "threadLocals.h"
#include "teleProcess.h"
#include "linuxTask.h"

/* The set of signals intercepted by the debugger to implement breakpoints,
 * task stopping/suspension. */
static sigset_t _caughtSignals;

/**
 * Waits for a newly started thread to stop (via a SIGSTOP), configures it for ptracing
 * and resumes the new thread as well as the thread that started it (which is currently
 * stopped on a SIGTRAP).
 *
 * @param newTid the PID of the new thread
 * @param starterTid the thread from which 'newTid' was started (via pthread_start()).
 */
static void task_attach_ptrace_to_new_task(int newTid, int starterTid) {
    int result;
    int status;
    do {
        tele_log_println("Waiting for new task %d to stop", newTid);
        result = waitpid(newTid, &status, __WALL);
    } while (result == -1 && errno == EINTR);

    if (result == -1) {
        perror("Error waiting for new task to stop");
        exit(1);
    } else if (result != newTid) {
        log_println("Wait returned unexpected PID %d", result);
        exit(1);
    } else if (!WIFSTOPPED(status) || WSTOPSIG(status) != SIGSTOP) {
        log_println("Wait returned status %p", status);
        exit(1);
    }

    ptrace(PT_SETOPTIONS, newTid, 0, PTRACE_O_TRACECLONE | PTRACE_O_TRACEEXIT);

    tele_log_println("Resuming tasks %d and %d", newTid, starterTid);
    ptrace(PT_CONTINUE, newTid, 0, 0);
    ptrace(PT_CONTINUE, starterTid, 0, 0);
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
    boolean first = true;
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
    state = toupper(state);
    return state;
}

/**
 * Converts a directory entry to a numeric PID.
 *
 * @param entry the directory entry to convert
 * @return the numeric PID corresponding to 'entry->d_name' or 0 if the name
 *         is not a valid PID or entry does is not a directory
 */
int dirent_task_pid(const struct dirent *entry) {
    char *endptr;
    if (entry->d_type == DT_DIR) {
        errno = 0;
        pid_t tid = (pid_t) strtol(entry->d_name, &endptr, 10);
        if (errno != 0) {
            log_println("Error converting dirent name \"%s\"to a long value: %s", entry->d_name, strerror(errno));
            return 0;
        }
        if (*endptr == '\0') {
            return tid;
        }
    }
    return 0;
}

/**
 * Scans a directory in the /proc filesystem for task subdirectories.
 *
 * @param pid the PID of the process whose /proc/<pid>/task directory will be scanned
 * @param tasks [out] an array of PIDs corresponding to the entries in the scanned directory
 *        for which dirent_task_pid() returns a non-zero result. The memory allocated for this
 *        array needs to be reclaimed by the caller.
 * @return the number of entries returned in 'tasks' or -1 if an error occurs. If an error occurs,
 *        no memory has been allocated and the value of '*tasks' is undefined.
 */
int scan_process_tasks(pid_t pid, pid_t **tasks) {
    char *taskDirPath;
    asprintf(&taskDirPath, "/proc/%d/task", (pid_t) pid);
    c_ASSERT(taskDirPath != NULL);

    struct dirent **entries = NULL;
    const int nEntries = scandir(taskDirPath, &entries, dirent_task_pid, alphasort);
    if (nEntries > 0) {
        (*tasks) = (pid_t *) malloc(nEntries * sizeof(pid_t *));
        int n = 0;
        while (n < nEntries) {
            pid_t tid = dirent_task_pid(entries[n]);
            (*tasks)[n] = tid;
            free(entries[n]);
            n++;
        }
        free(entries);
    }
    int error = errno;
    free(taskDirPath);
    errno = error;
    return nEntries;
}

/* The pause time between each poll of the VM to see if at least one thread has stopped */
#define PROCESS_POLL_PAUSE_NANOSECONDS (200 * 1000)

jboolean process_resume_all_threads(pid_t pid) {
    pid_t *tasks = NULL;
    const int nTasks = scan_process_tasks(pid, &tasks);
    if (nTasks < 0) {
        log_println("Error scanning /proc/%d/task directory: %s", pid, strerror(errno));
        return false;
    }

    boolean result = true;
    int n = 0;
    while (n < nTasks) {
        pid_t tid = tasks[n];

        /* Clear any left over SIGSTOP or SIGTRAP signals. */
        siginfo_t siginfo;
        ptrace(PT_GETSIGINFO, tid, NULL, &siginfo);
        int signal = siginfo.si_signo;
        if (signal != 0) {
            if (!sigismember(&_caughtSignals, signal)) {
                log_println("Error: Task %d with pending signal %d [%s] should not have been stopped by debugger", tid, signal, strsignal(signal));
            } else {
                tele_log_println("Clearing signal %d [%s] for task %d before resuming it", signal, strsignal(signal), tid);
                siginfo.si_signo = 0;
                siginfo.si_code = 0;
                siginfo.si_errno = 0;
                ptrace(PT_SETSIGINFO, tid, NULL, &siginfo);
            }
        }

        tele_log_println("Resuming task %d", tid);
        if (ptrace(PT_CONTINUE, tid, 0, 0) != 0) {
            result = false;
        }
        n++;
    }
    free(tasks);
    return result;
}

int process_wait_all_threads_stopped(pid_t pid) {
    pid_t pgid = getpgid(pid);

    boolean stopping = false;
    while (1) {

        pid_t *tasks;
        const int nTasks = scan_process_tasks(pid, &tasks);
        if (nTasks < 0) {
            log_println("Error scanning /proc/%d/task directory: %s", pid, strerror(errno));
            return -1;
        }

        if (stopping) {
            tele_log_println("Stopping %d tasks...", nTasks);
        } else {
            tele_log_println("Scanning %d tasks...", nTasks);
        }

        int nStopped = 0;
        int nExited = 0;
        int n = 0;
        while (n < nTasks) {
            pid_t tid = tasks[n];

            /* The WNOHANG option means that we won't be blocked on the waitpid() call if the signal
             * state of the task has not changed since the last time waitpid() was called on it.
             * The __WALL option is necessary so that we can wait on a thread not directly
             * created by the primordial VM thread. This strangeness is due to the way threads
             * are implemented on Linux. See the waitpid(2) man page for more detail. */
            int waitOptions = WNOHANG | __WALL;
            if (stopping) {
                tele_log_println("Waiting for %d", tid);
            }
            int status = 0;
            int result = waitpid(tid, &status, waitOptions);
            if (result == 0) {
                char state = task_state(pid, tid);
                if (state == 'T') {
                    nStopped++;
                }
                tele_log_println("No change in task %d since waitpid last called on it", tid);
            } else if (result < 0) {
                log_println("Error calling waitpid(%d): %s", tid, strerror(errno));
            } else if (WIFEXITED(status)) {
                log_println("Task %d exited with exit status %d", tid, WEXITSTATUS(status));
            } else if (WIFSIGNALED(status)) {
                int signal = WTERMSIG(status);
                log_println("Task %d terminated by signal %d [%s]", tid, signal, strsignal(signal));
            } else if (WIFSTOPPED(status)) {
                int signal = WSTOPSIG(status);
                tele_log_println("Task %d stopped by signal %d [%s]", tid, signal, strsignal(signal));

                if (!sigismember(&_caughtSignals, signal)) {
                    tele_log_println("Resuming task %d with signal %d [%s]", tid, signal, strsignal(signal));
                    ptrace(PT_CONTINUE, tid, NULL, (Address) signal);
                } else {
                    if (signal == SIGTRAP) {
                        nStopped++;
                        int event = PTRACE_EVENT(status);
                        if (event != 0) {
                            unsigned long eventMsg;
                            ptrace(PT_GETEVENTMSG, tid, NULL, &eventMsg);
                            if (event == PTRACE_EVENT_CLONE) {
                                /* This is the SIGTRAP event denoting that a new thread has been started. */
                                int newTid = eventMsg;
                                task_attach_ptrace_to_new_task(newTid, tid);
                                nStopped--;
                            } else if (event == PTRACE_EVENT_EXIT) {
                                /* This is the SIGTRAP event denoting that a thread is about to exit
                                 * and needs to be detached from ptrace. */
                                nExited++;
                                nStopped--;
                                tele_log_println("Detaching exiting task %d", tid);
                                ptrace(PT_DETACH, tid, NULL, 0);
                            } else {
                                log_println("Task %d received unexpected ptrace event %d with message %ul", tid, event, eventMsg);
                            }
                        }
                    } else {
                        nStopped++;
                    }
                }
            } else {
                char state = task_state(pid, tid);
                tele_log_println("Task %d not yet stopped; state = '%c'", tid, state);
                if (state == 'Z') {
                    /* Missed the PTRACE_EVENT_EXIT event for this task somehow. Still need to account
                     * for it as exited. However, we cannot no longer PT_DETACH it. */
                    tele_log_println("Missed exit event for task %d: cleaning up anyway", tid);
                    nExited++;
                }
            }
            n++;
        }
        free(tasks);

        if (nExited == nTasks) {
            tele_log_println("All threads have exited");
            return 0;
        }

        if (nStopped == 0) {
            /* No tasks are stopped yet: continue after a brief sleep */
            usleep(PROCESS_POLL_PAUSE_NANOSECONDS);
            continue;
        }

        if (nStopped != nTasks) {
            /* Give all tasks a brief chance to receive the last SIGSTOP (if any) */
            usleep(PROCESS_POLL_PAUSE_NANOSECONDS);

            /* Stop all threads by sending SIGSTOP to the process group (which is why the VM
             * must run in a separate process group from the debugger!). Note that the tasks
             * already stopped due to a previous SIGSTOP will simply ignore this SIGSTOP.
             * However, the SIGSTOP must be sent until all tasks have stopped so that we catch
             * tasks that start in between each SIGSTOP. */
            tele_log_println("Not all tasks stopped yet - sending SIGSTOP to process group %d", pgid);
            kill(-pgid, SIGSTOP);
            stopping = true;
            continue;
        }

        /* Re-scan tasks to ensure we've got them all and they are all stopped. */
        const int mTasks = scan_process_tasks(pid, &tasks);
        if (mTasks < 0) {
            log_println("Error scanning /proc/%d/task directory: %s", pid, strerror(errno));
            continue;
        }
        free(tasks);

        int nNewTasks = mTasks - nTasks;
        if (nNewTasks != 0) {
            tele_log_println("%d new tasks started since last scan - continuing...", mTasks - nTasks);
            continue;
        }

        /* We are now sure that we have stopped all the tasks. */
        stopping = false;
        tele_log_println("Stopped all tasks...");
        return mTasks;
    }
}

/* An alternative version that reduces the amount of calls to waitpid() by polling the state
 * of each task first and only calling waitpid() that are known to be in the 'T' state. I'm
 * not yet sure which version performs better once there are lots of threads so I'm leaving
 * it around for now [Doug]. */
int process_wait_all_threads_stopped_alternative(pid_t pid) {
    pid_t pgid = getpgid(pid);

    /* The debugging related signals we want to intercept. */
    sigset_t caughtSignals;
    sigemptyset(&caughtSignals);
    sigaddset(&caughtSignals, SIGTRAP);
    sigaddset(&caughtSignals, SIGSTOP);

    boolean stopping = false;
    while (1) {

        pid_t *tasks;
        const int nTasks = scan_process_tasks(pid, &tasks);
        if (nTasks < 0) {
            log_println("Error scanning /proc/%d/task directory: %s", pid, strerror(errno));
            return -1;
        }

        if (stopping) {
            tele_log_println("Stopping %d tasks...", nTasks);
        } else {
            tele_log_println("Scanning %d tasks...", nTasks);
        }

        int nStopped = 0;
        int nExited = 0;
        boolean allStopped = true;
        int n = 0;
        while (n < nTasks) {
            pid_t tid = tasks[n];
            char state = task_state(pid, tid);
            if (state == 'T') {
                nStopped++;

                /* The WNOHANG option means that we won't be blocked on the waitpid() call if the signal
                 * state of the task has not changed since the last time waitpid() was called on it.
                 * The __WALL option is necessary so that we can wait on a thread not directly
                 * created by the primordial VM thread. This strangeness is due to the way threads
                 * are implemented on Linux. See the waitpid(2) man page for more detail. */
                int waitOptions = WNOHANG | __WALL;
                if (stopping) {
                    tele_log_println("Waiting for %d", tid);
                }
                int status = 0;
                int result = waitpid(tid, &status, waitOptions);
                if (result == 0) {
                    tele_log_println("No change in task %d since waitpid last called on it", tid);
                } else if (result < 0) {
                    log_println("Error calling waitpid(%d): %s", tid, strerror(errno));
                } else {
                    c_ASSERT(result == tid);
                    if (WIFEXITED(status)) {
                        log_println("Task %d exited with exit status %d", tid, WEXITSTATUS(status));
                    } else if (WIFSIGNALED(status)) {
                        int signal = WTERMSIG(status);
                        log_println("Task %d terminated by signal %d [%s]", tid, signal, strsignal(signal));
                    } else {
                        if (!WIFSTOPPED(status)) {
                            log_println("Task %d should be stopped!", tid);
                        }
                        int signal = WSTOPSIG(status);
                        tele_log_println("Task %d stopped by signal %d [%s]", tid, signal, strsignal(signal));

                        if (!sigismember(&caughtSignals, signal)) {
                            tele_log_println("Resuming task %d with signal %d [%s]", tid, signal, strsignal(signal));
                            allStopped = false;
                            nStopped--;
                            ptrace(PT_CONTINUE, tid, NULL, (Address) signal);
                        } else {
                            if (signal == SIGTRAP) {
                                int event = PTRACE_EVENT(status);
                                if (event != 0) {
                                    unsigned long eventMsg;
                                    ptrace(PT_GETEVENTMSG, tid, NULL, &eventMsg);
                                    if (event == PTRACE_EVENT_CLONE) {
                                        /* This is the SIGTRAP event denoting that a new thread has been started. */
                                        allStopped = false;
                                        nStopped--;
                                        int newTid = eventMsg;
                                        task_attach_ptrace_to_new_task(newTid, tid);
                                    } else if (event == PTRACE_EVENT_EXIT) {
                                        /* This is the SIGTRAP event denoting that a thread is about to exit
                                         * and needs to be detached from ptrace. */
                                        nStopped--;
                                        nExited++;
                                        tele_log_println("Detaching exiting task %d", tid);
                                        ptrace(PT_DETACH, tid, NULL, 0);
                                    } else {
                                        log_println("Task %d received unexpected ptrace event %d with message %ul", tid, event, eventMsg);
                                    }
                                }
                            } else {
                                c_ASSERT(signal == SIGSTOP);
                            }
                        }
                    }
                }
            } else {
                allStopped = false;
                tele_log_println("Task %d not yet stopped; state = '%c'", tid, state);
            }
            n++;
        }
        free(tasks);

        if (nExited == nTasks) {
            tele_log_println("All threads have exited");
            return 0;
        }

        if (nStopped == 0) {
            /* No tasks are stopped yet: continue after a brief sleep */
            usleep(PROCESS_POLL_PAUSE_NANOSECONDS);
            continue;
        }

        if (!allStopped) {
            /* Give all tasks a brief chance to receive the last SIGSTOP (if any) */
            usleep(PROCESS_POLL_PAUSE_NANOSECONDS);

            /* Stop all threads by sending SIGSTOP to the process group (which is why the VM
             * must run in a separate process group from the debugger!). Note that the tasks
             * already be stopped due to a previous SIGSTOP will simply ignore this SIGSTOP.
             * However, the SIGSTOP must be sent until all tasks have stopped so that we can
             * tasks that start in between each SIGSTOP. */
            tele_log_println("Not all tasks stopped yet - sending SIGSTOP to process group %d", pgid);
            kill(-pgid, SIGSTOP);
            stopping = true;
            continue;
        }

        /* Re-scan tasks to ensure we've got them all and they are all stopped. */
        const int mTasks = scan_process_tasks(pid, &tasks);
        if (mTasks < 0) {
            log_println("Error scanning /proc/%d/task directory: %s", pid, strerror(errno));
            continue;
        }
        free(tasks);

        int nNewTasks = mTasks - nTasks;
        if (nNewTasks != 0) {
            tele_log_println("%d new tasks started since last scan - continuing...", mTasks - nTasks);
            continue;
        }

        /* We are now sure that we have stopped all the tasks. */
        stopping = false;
        tele_log_println("Stopped all tasks...");
        return mTasks;
    }
}

JNIEXPORT jint JNICALL
Java_com_sun_max_tele_debug_linux_LinuxTask_nativeCreateChildProcess(JNIEnv *env, jclass c, jlong commandLineArgumentArray, jint vmAgentPort) {
    char **argv = (char**) commandLineArgumentArray;

    /* Configure the debugging related signals we want to intercept. */
    sigemptyset(&_caughtSignals);
    sigaddset(&_caughtSignals, SIGTRAP);
    sigaddset(&_caughtSignals, SIGSTOP);

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

#ifdef PR_SET_PTRACER
        /* See info about PR_SET_PTRACER at https://wiki.ubuntu.com/Security/Features#ptrace */
        char *pidDef;
        int parentPid = getppid();
        if (asprintf(&pidDef, "MAX_AGENT_PID=%u", parentPid) == -1) {
            log_exit(1, "Could not allocate space for setting MAX_AGENT_PID environment variable");
        }
        putenv(pidDef);
#endif

        /* Put the VM in its own process group so that SIGSTOP can be used to
         * stop all threads in the child. */
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
            ptrace(PT_SETOPTIONS, childPid, 0, PTRACE_O_TRACECLONE | PTRACE_O_TRACEEXIT);
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
    tele_log_println("Sending SIGTRAP to %d", tid, killID);
    if (kill(killID, SIGTRAP) != 0) {
        log_println("Error sending SIGTRAP to suspend %s %d: %s", tid, (allTasks ? "all tasks in group" : "task"), strerror(errno));
        return false;
    }
    return true;
}

JNIEXPORT jboolean JNICALL
Java_com_sun_max_tele_debug_linux_LinuxTask_nativeSingleStep(JNIEnv *env, jclass c, jint tgid, int tid) {
    if (ptrace(PT_STEP, tid, 0, 0) != 0) {
        return false;
    }
    return true;
}

JNIEXPORT jboolean JNICALL
Java_com_sun_max_tele_debug_linux_LinuxTask_nativeResume(JNIEnv *env, jclass c, jint tgid, jint tid, jboolean allTasks) {
    if (allTasks) {
        return process_resume_all_threads(tgid);
    }
    int result = ptrace(PT_CONTINUE, tid, NULL, 0);
    return result == 0;
}

JNIEXPORT jint JNICALL
Java_com_sun_max_tele_debug_linux_LinuxTask_nativeWait(JNIEnv *env, jclass c, jint tgid, jint tid, jboolean allTasks) {
    if (allTasks) {
        if (process_wait_all_threads_stopped(tgid) > 0) {
            return PS_STOPPED;
        }
        return PS_TERMINATED;
    }
    c_UNIMPLEMENTED();
    return PS_UNKNOWN;
}

JNIEXPORT jboolean JNICALL
Java_com_sun_max_tele_debug_linux_LinuxTask_nativeKill(JNIEnv *env, jclass c, jint tgid, jint tid) {
    pid_t killID = -getpgid(tgid);
    tele_log_println("Sending SIGKILL to %d", tid, killID);
    if (kill(killID, SIGKILL) != 0) {
        log_println("Error sending SIGKILL to kill process %d: %s", tgid, strerror(errno));
        return false;
    }
    return true;
}

/**
 * Gets an open file descriptor on /proc/<pid>/mem for reading the memory of the traced process 'tgid'.
 *
 * @param tgid the task group id of the traced process
 * @param address the address at which the memory of tgid is to be read
 * @return a file descriptor opened on the memory file and positioned at 'address' or -1 if there was an error.
 *        If there was no error, it is the caller's responsibility to close the file descriptor.
 */
int task_memory_read_fd(int tgid, const void *address) {
    ptrace_check_tracer(POS, tgid);
    char *memoryFileName;
    asprintf(&memoryFileName, "/proc/%d/mem", tgid);
    c_ASSERT(memoryFileName != NULL);
    int fd = open(memoryFileName, O_RDONLY);
    if (fd < 0) {
        log_println("Error opening %s: %s", memoryFileName, strerror(errno));
        return fd;
    }
    free(memoryFileName);
    off64_t fdOffset = (off64_t) address;
    if (lseek64(fd, fdOffset, SEEK_SET) != fdOffset) {
        log_println("Error seeking memory file for process %d to %p [%ul]: %s", tgid, address, fdOffset, strerror(errno));
        close(fd);
        return -1;
    }
    return fd;
}

/**
 * Copies 'size' bytes from 'src' in the address space of 'tgid' to 'dst' in the caller's address space.
 */
size_t task_read(pid_t tgid, pid_t tid, const void *src, void *dst, size_t size) {
    char state;
    if ((state = task_state(tgid, tid)) != 'T') {
        log_println("Cannot read memory of task %d while it is in state '%c'", tid, state);
        return 0;
    }

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
        int fd = task_memory_read_fd(tgid, src);
        if (fd < 0) {
            return -1;
        }
        size_t bytesRead = read(fd, dst, size);
        if (bytesRead != size) {
            log_println("Only read %d of %d bytes from %p: %s", bytesRead, size, src, strerror(errno));
        }
        close(fd);
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
    char state;
    if ((state = task_state(tgid, tid)) != 'T') {
        log_println("Cannot write to memory of task %d while it is in state '%c'", tid, state);
        return 0;
    }

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
Java_com_sun_max_tele_debug_linux_LinuxTask_nativeWriteBytes(JNIEnv *env, jclass c, jint tgid, jint tid, jlong dst, jobject src, jboolean isDirectByteBuffer, jint srcOffset, jint length) {
    ProcessHandleStruct ph = {tgid, tid};
    return teleProcess_write(&ph, env, c, dst, src, isDirectByteBuffer, srcOffset, length);
}

JNIEXPORT jint JNICALL
Java_com_sun_max_tele_debug_linux_LinuxTask_nativeReadBytes(JNIEnv *env, jclass c, jint tgid, jint tid, jlong src, jobject dst, jboolean isDirectByteBuffer, jint dstOffset, jint length) {
    ProcessHandleStruct ph = {tgid, tid};
    return teleProcess_read(&ph, env, c, src, dst, isDirectByteBuffer, dstOffset, length);
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

/*
 * Function copies from native register data structures to Java byte arrays. Does 3 things:
 * 1. Checks size of provided array lengths
 * 2. Canonicalizes the native register data structures
 * 3. Copies the canonicalized structures into the byte arrays
 */
static jboolean copyRegisters(JNIEnv *env, jobject  this, struct user_regs_struct *osRegisters, struct user_fpregs_struct  *osFloatingPointRegisters,
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

    isa_canonicalizeTeleIntegerRegisters(&osRegisters[0], &canonicalIntegerRegisters);
    isa_canonicalizeTeleStateRegisters(&osRegisters[0], &canonicalStateRegisters);
    isa_canonicalizeTeleFloatingPointRegisters(osFloatingPointRegisters, &canonicalFloatingPointRegisters);

    (*env)->SetByteArrayRegion(env, integerRegisters, 0, integerRegistersLength, (void *) &canonicalIntegerRegisters);
    (*env)->SetByteArrayRegion(env, stateRegisters, 0, stateRegistersLength, (void *) &canonicalStateRegisters);
    (*env)->SetByteArrayRegion(env, floatingPointRegisters, 0, floatingPointRegistersLength, (void *) &canonicalFloatingPointRegisters);
    return true;
}

JNIEXPORT jboolean JNICALL
Java_com_sun_max_tele_debug_linux_LinuxTask_nativeReadRegisters(JNIEnv *env, jclass c, jint tid,
                jbyteArray integerRegisters, jint integerRegistersLength,
                jbyteArray floatingPointRegisters, jint floatingPointRegistersLength,
                jbyteArray stateRegisters, jint stateRegistersLength) {

    struct user_regs_struct osRegisters;
    if (ptrace(PT_GETREGS, tid, 0, &osRegisters) != 0) {
        return false;
    }

    struct user_fpregs_struct osFloatRegisters;
    if (ptrace(PT_GETFPREGS, tid, 0, &osFloatRegisters) != 0) {
        return false;
    }

    return copyRegisters(env, c, &osRegisters, &osFloatRegisters,
                    integerRegisters, integerRegistersLength,
                    floatingPointRegisters, floatingPointRegistersLength,
                    stateRegisters, stateRegistersLength);
}

// The following methods support core-dump access for Linux
#include <sys/procfs.h>

extern ThreadState_t toThreadState(char state, pid_t tid);

JNIEXPORT jint JNICALL
Java_com_sun_max_tele_debug_linux_LinuxDumpThreadAccess_taskStatusToThreadState(JNIEnv *env, jclass  class, jobject bytebuffer) {
    prpsinfo_t * prpsinfo = (prpsinfo_t *)  ((*env)->GetDirectBufferAddress(env, bytebuffer));
    return toThreadState(prpsinfo->pr_sname, prpsinfo->pr_pid);
}

JNIEXPORT jint JNICALL
Java_com_sun_max_tele_debug_linux_LinuxDumpThreadAccess_taskId(JNIEnv *env, jclass  class,  jobject bytebuffer) {
    prstatus_t * prstatus = (prstatus_t *) ((*env)->GetDirectBufferAddress(env, bytebuffer));
    return prstatus->pr_pid;
}

JNIEXPORT jint JNICALL
Java_com_sun_max_tele_debug_linux_LinuxDumpThreadAccess_taskRegisters(JNIEnv *env, jclass  class,  jobject bytebuffer_status, jobject bytebuffer_fpreg,
                jbyteArray integerRegisters, jint integerRegistersLength,
                jbyteArray floatingPointRegisters, jint floatingPointRegistersLength,
                jbyteArray stateRegisters, jint stateRegistersLength) {
    prstatus_t * prstatus = (prstatus_t *) ((*env)->GetDirectBufferAddress(env, bytebuffer_status));
    elf_fpregset_t *fpregset = (elf_fpregset_t *) ((*env)->GetDirectBufferAddress(env, bytebuffer_fpreg));
    return copyRegisters(env, class, (struct user_regs_struct *) &prstatus->pr_reg[0], fpregset,
                    integerRegisters, integerRegistersLength,
                    floatingPointRegisters, floatingPointRegistersLength,
                    stateRegisters, stateRegistersLength);
}

