/*
 * Copyright (c) 2009, 2011, Oracle and/or its affiliates. All rights reserved.
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

/*
 * This is a standalone program for learning about how to use ptrace to debug
 * a multithreaded program. A lot of the code in linuxTask.c is based on the
 * lessons provided by this program.
 *
 * The program forks a child which will be traced via ptrace. The child spins
 * up a number of threads which simply spin with a short sleep (200ms) in each loop.
 *
 * To simulate hitting breakpoints, you need to send SIGTRAP to any of the threads
 * in the child via the kill facility. For example, if the child's PID is 123 and
 * one of it's threads/tasks has the TID 127:
 *
 *  % kill -s SIGTRAP 127
 *
 * This program should remain independent of the rest of the code base so that
 * it can be passed around if necessary without requiring the rest of the Maxine
 * sources.
 *
 * Build:
 *
 *  gcc -o ptraceTest -lc -lm -lpthread
 *
 */

#define _GNU_SOURCE

#include <stdio.h>
#include <stdlib.h>
#include <signal.h>
#include <string.h>
#include <errno.h>
#include <pthread.h>
#include <syscall.h>
#include <unistd.h>
#include <stdarg.h>
#include <dirent.h>
#include <sys/types.h>
#include <sys/wait.h>
#include "ptrace.h"

#define PAUSE_NANOSECONDS (200 * 1000)
#define NTHREADS 5
#define PTRACE_EVENT(waitpidStatus) (((waitpidStatus) & 0xFF0000) >> 16)

#if !defined(false) && !defined(true)
    typedef enum {false, true}  boolean;
#else
    typedef int                 boolean;
#endif

static pid_t _parent = 0;
static pid_t _child = 0;

/* Set to true to prefix output from 'println' with pgid, pid, tid and line number. */
#define DEBUG_PRINT false

static void _print(const char *format, ...) {
    pid_t pid = getpid();
    pid_t tid = syscall(__NR_gettid);
    va_list ap;
    va_start(ap, format);
    if (DEBUG_PRINT && pid != _parent) {
        printf("[pgid=%d:pid=%d:tid=%d] ", getpgid(pid), pid, tid);
    }
    vprintf(format, ap);
    va_end(ap);
}

#if DEBUG
#define println(format, ...) _print("%d: " format "\n", __LINE__, ##__VA_ARGS__);
#else
#define println(format, ...) _print(format "\n", ##__VA_ARGS__);
#endif

typedef struct {
    int done;
    int descendants;
} ThreadContextStruct, *ThreadContext;

pthread_key_t _threadContextKey;

void *child_thread_run(void *arg) {
    println("Started task %d", syscall(__NR_gettid));
    ThreadContext context = (ThreadContext) arg;
    pthread_setspecific(_threadContextKey, context);
    int loops = 0;

    ThreadContext childContext = NULL;
    int descendants = context->descendants;
    if (descendants > 0) {
        pthread_t childThread;
        ThreadContext childContext = (ThreadContext) calloc(1, sizeof(ThreadContextStruct));
        childContext->descendants = descendants - 1;
        if (pthread_create(&childThread, NULL, (void *(*)(void *)) child_thread_run, childContext) != 0) {
            perror("pthread_create failed");
            exit(1);
        }
    }

    while (!context->done) {
        //println("%d", loops);
        usleep(PAUSE_NANOSECONDS);
        ++loops;
    }

    if (childContext != NULL) {
        childContext->done = true;
    }

    println("Finished task %d", syscall(__NR_gettid));
    return NULL;
}

void child_run() {
    pthread_key_create(&_threadContextKey, free);

    ThreadContext context = (ThreadContext) calloc(1, sizeof(ThreadContextStruct));
    context->descendants = NTHREADS;
    child_thread_run(context);
}

/**
 * Waits for a newly starting thread to stop, configures for ptracing by the calling process
 * and resumes the new thread as well as the thread that started it (which is currently
 * stopped on a SIGTRAP.
 *
 * @param newTid the PID of the new thread
 * @param starterTid the thread from which 'newTid' was started (via pthread_start()).
 */
static void parent_attach_new_thread(int newTid, int starterTid) {
    int result;
    int status;
    do {
        println("Waiting for new task %d to stop", newTid);
        result = waitpid(newTid, &status, __WALL);
    } while (result == -1 && errno == EINTR);

    if (result == -1) {
        perror("Error waiting for new task to stop");
        exit(1);
    } else if (result != newTid) {
        println("Wait returned unexpected PID %d", result);
        exit(1);
    } else if (!WIFSTOPPED(status) || WSTOPSIG(status) != SIGSTOP) {
        println("Wait returned status %p", status);
        exit(1);
    }

    ptrace(PT_SETOPTIONS, newTid, 0, (void *) (PTRACE_O_TRACECLONE | PTRACE_O_TRACEEXIT));

    println("Resuming tasks %d and %d", newTid, starterTid);
    ptrace(PT_CONTINUE, newTid, 0, 0);
    ptrace(PT_CONTINUE, starterTid, 0, 0);
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
            println("Error converting dirent name \"%s\"to a long value: %s", entry->d_name, strerror(errno));
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

/**
 * Reads the stat of a task from /proc/<tgid>/task/<tid>/stat. See proc(5).
 *
 * @param format the format string for parsing the fields of the stat string
 * @param ... the arguments for storing the fields parsed from the stat string according to 'format'
 */
void task_stat(pid_t tgid, pid_t tid, const char* format, ...) {
    char *buf;
    if (asprintf(&buf, "/proc/%d/task/%d/stat", tid, tid) < 0) {
        println("Error calling asprintf(): %s", strerror(errno));
        return;
    }
    FILE *procFile = fopen(buf, "r");
    va_list ap;
    va_start(ap, format);
    if (vfscanf(procFile, format, ap) == EOF) {
        println("Error reading %s: %s", buf, strerror(errno));
    }
    va_end(ap);
    if (fclose(procFile) == EOF) {
        println("Error closing %s: %s", buf, strerror(errno));
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

/**
 * The main loop of the tracer.
 *
 * @param pid the PID of the leader task in the process being traced
 */
void parent_run(pid_t pid) {
    pid_t childGid = getpgid(pid);

    sigset_t caughtSignals;
    sigemptyset(&caughtSignals);
    sigaddset(&caughtSignals, SIGTRAP);
    sigaddset(&caughtSignals, SIGSTOP);

    boolean stopping = false;
    while (1) {

        if (stopping) {
            println("Stopping tasks...");
        }
        pid_t *tasks;
        const int nTasks = scan_process_tasks(pid, &tasks);
        if (nTasks < 0) {
            println("Error scanning /proc/%d/task directory: %s", pid, strerror(errno));
            continue;
        }

        if (stopping) {
            println("Stopping %d tasks...", nTasks);
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
                println("Waiting for %d", tid);
            }
            int status = 0;
            int result = waitpid(tid, &status, waitOptions);
            if (result == 0) {
                char state = task_state(pid, tid);
                if (state == 'T') {
                    nStopped++;
                }
                //println("No change in task %d since waitpid last called on it", tid);
            } else if (result < 0) {
                println("Error calling waitpid(%d): %s", tid, strerror(errno));
            } else if (WIFEXITED(status)) {
                println("Task %d exited with exit status %d", tid, WEXITSTATUS(status));
            } else if (WIFSIGNALED(status)) {
                int signal = WTERMSIG(status);
                println("Task %d terminated by signal %d [%s]", tid, signal, strsignal(signal));
            } else if (WIFSTOPPED(status)) {
                int signal = WSTOPSIG(status);
                println("Task %d stopped by signal %d [%s]", tid, signal, strsignal(signal));

                if (!sigismember(&caughtSignals, signal)) {
                    println("Resuming task %d with signal %d [%s]", tid, signal, strsignal(signal));
                    ptrace(PT_CONTINUE, tid, NULL, (void *) (unsigned long) signal);
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
                                parent_attach_new_thread(newTid, tid);
                                nStopped--;
                            } else if (event == PTRACE_EVENT_EXIT) {
                                /* This is the SIGTRAP event denoting that a thread is about to exit
                                 * and needs to be detached from ptrace. */
                                nExited++;
                                nStopped--;
                                println("Detaching exiting task %d", tid);
                                ptrace(PT_DETACH, tid, NULL, 0);
                            } else {
                                println("Task %d received unexpected ptrace event %d with message %ul", tid, event, eventMsg);
                            }
                        }
                    } else {
                        nStopped++;
                    }
                }
            } else {
                char state = task_state(pid, tid);
                println("Task %d not yet stopped; state = '%c'", tid, state);
                if (state == 'Z') {
                    /* Missed the PTRACE_EVENT_EXIT event for this task somehow. Still need to account
                     * for it as exited. However, we cannot no longer PT_DETACH it. */
                    println("Missed exit event for task %d: cleaning up anyway", tid);
                    nExited++;
                }
            }
            n++;
        }
        free(tasks);

        if (nExited == nTasks) {
            println("All threads have exited");
            return;
        }

        if (nStopped == 0) {
            /* No tasks are stopped yet: continue after a brief sleep */
            usleep(PAUSE_NANOSECONDS);
            continue;
        }

        if (nStopped != nTasks) {
            /* Give all tasks a brief chance to receive the last SIGSTOP (if any) */
            usleep(PAUSE_NANOSECONDS);

            /* Stop all threads by sending SIGSTOP to the process group (which is why the VM
             * must run in a separate process group from the debugger!). Note that the tasks
             * already be stopped due to a previous SIGSTOP will simply ignore this SIGSTOP.
             * However, the SIGSTOP must be sent until all tasks have stopped so that we can
             * tasks that start in between each SIGSTOP. */
            println("Not all tasks stopped yet - sending SIGSTOP to process group %d", childGid);
            kill(-childGid, SIGSTOP);
            stopping = true;
            continue;
        }

        /* Re-scan tasks to ensure we've got them all and they are all stopped. */
        const int mTasks = scan_process_tasks(pid, &tasks);
        if (mTasks < 0) {
            println("Error scanning /proc/%d/task directory: %s", pid, strerror(errno));
            continue;
        }

        int nNewTasks = mTasks - nTasks;
        if (nNewTasks != 0) {
            println("%d new tasks started since last scan - continuing...", mTasks - nTasks);
            free(tasks);
            continue;
        }

        /* We are now sure that we have stopped all the tasks. */
        stopping = false;
        println("Stopped all tasks...");

        println("\n\nSimulating debugger interaction while process stopped with short delay...\n\n");
        usleep(5000 * 1000);

        n = 0;
        while (n < mTasks) {
            pid_t tid = tasks[n];

            /* Clear any left over SIGSTOP or SIGTRAP signals. */
            siginfo_t siginfo;
            ptrace(PT_GETSIGINFO, tid, NULL, &siginfo);
            int signal = siginfo.si_signo;
            if (signal != 0) {
                if (!sigismember(&caughtSignals, signal)) {
                    println("Error: Task %d with pending signal %d [%s] should not have been stopped by debugger", tid, signal, strsignal(signal));
                } else {
                    println("Clearing signal %d [%s] for task %d before resuming it", signal, strsignal(signal), tid);
                    siginfo.si_signo = 0;
                    siginfo.si_code = 0;
                    siginfo.si_errno = 0;
                    ptrace(PT_SETSIGINFO, tid, NULL, &siginfo);
                }
            }

            println("Continuing task %d", tid);
            if (ptrace(PT_CONTINUE, tid, NULL, 0) != 0) {
                perror("PT_CONTINUE failed");
                exit(1);
            }
            n++;
        }
        free(tasks);
    }
}

static void parent_sighandler(int signal) {

    println("Received signal %d [%s]", signal, strsignal(signal));
    if (_child != 0) {
        println("Killing child %d", _child);
        kill(_child, SIGKILL);
    }
    exit(0);
}


int main(int argc, char** argv) {
    _parent = getpid();
    nice(10);
    int childPid = fork();
    if (childPid == 0) {
        /*child:*/
        if (ptrace(PT_TRACEME, 0, 0, 0) != 0) {
            perror("Failed to attach ptrace to child");
            exit(1);
        }
        /* Put the child in its own process group so that SIGSTOP can be used to
         * stop all threads in the child. */
        setpgid(0, 0);

        /* Notify parent */
        kill(getpid(), SIGTRAP);

        child_run();
    } else {
        /*parent:*/
        int status;
        println("parent waiting for child to start...");
        if (waitpid(childPid, &status, 0) == childPid && WIFSTOPPED(status)) {
            println("received child notification");

            _child = childPid;

            /* Configure child so that it traps when it exits or starts new threads */
            ptrace(PT_SETOPTIONS, childPid, 0, (void *) (PTRACE_O_TRACEEXIT | PTRACE_O_TRACECLONE));
            ptrace(PT_CONTINUE, childPid, NULL, 0);

            /* Catch CTRL-C so that child can be stopped before parent exits. */
            struct sigaction sa;
            memset((char *) &sa, 0, sizeof(sa));
            sigemptyset(&sa.sa_mask);
            sa.sa_flags = SA_SIGINFO | SA_RESTART | SA_ONSTACK;
            sa.sa_handler = parent_sighandler;

            if (sigaction(SIGINT, &sa, NULL) != 0) {
                perror("sigaction failed");
                exit(1);
            }

            parent_run(childPid);
        }
    }
    println("parent exiting...");
    return 1;
}
