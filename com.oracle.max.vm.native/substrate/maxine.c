/*
 * Copyright (c) 2007, 2012, Oracle and/or its affiliates. All rights reserved.
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
 * The main program of the VM.
 * Loads, verifies and mmaps the boot image,
 * hands control over to the VM's compiled code, which has been written in Java,
 * by calling a VM entry point as a C function.
 */
#include <dlfcn.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <alloca.h>
#include <errno.h>
#include <pwd.h>
#include <time.h>
#include <sys/param.h>

#include "log.h"
#include "image.h"
#include "threads.h"
#include "os.h"
#include "virtualMemory.h"

#include "maxine.h"

#if os_MAXVE
#include "maxve.h"
#endif

#define IMAGE_FILE_NAME  "maxine.vm"
#define DARWIN_STACK_ALIGNMENT ((Address) 16)

#if os_DARWIN
static char *_executablePath;
#endif

static void getExecutablePath(char *result) {
#if os_DARWIN
    if (realpath(_executablePath, result) == NULL) {
        fprintf(stderr, "could not read %s\n", _executablePath);
        exit(1);
    }
    int numberOfChars = strlen(result);
#elif os_MAXVE
    result[0] = 0;
    return;
#elif os_LINUX
    char *linkName = "/proc/self/exe";
#elif os_SOLARIS
    char *linkName = "/proc/self/path/a.out";
#else
#   error getExecutablePath() not supported on other platforms yet
#endif

#if os_LINUX || os_SOLARIS
    // read the symbolic link to figure out what the executable is.
    int numberOfChars = readlink(linkName, result, MAX_PATH_LENGTH);
    if (numberOfChars < 0) {
        log_exit(1, "Could not read %s\n", linkName);
    }
#endif

#if !os_MAXVE
    char *p;
    // chop off the name of the executable
    for (p = result + numberOfChars; p >= result; p--) {
        if (*p == '/') {
            p[1] = 0;
            break;
        }
    }
#endif
}

static void getImageFilePath(char *result) {
#if !os_MAXVE
    getExecutablePath(result);

    // append the name of the image to the executable path
    strcpy(result + strlen(result), IMAGE_FILE_NAME);
#endif
}

static int loadImage(void) {
    char imageFilePath[MAX_PATH_LENGTH];
    getImageFilePath(imageFilePath);
    return image_load(imageFilePath);
}

static void *openLibrary(char *path) {
#if log_LINKER
    if (path == NULL) {
        log_println("openLibrary(null)");
    } else {
        log_println("openLibrary(\"%s\")", path);
    }
#endif
    void *result = dlopen(path, RTLD_LAZY);
#if log_LINKER
    char* errorMessage = dlerror();
    if (path == NULL) {
        log_println("openLibrary(null) = %p", result);
    } else {
        log_println("openLibrary(\"%s\") = %p", path, result);
    }
    if (errorMessage != NULL) {
        log_println("Error message: %s", errorMessage);
    }
#endif
    return result;
}

static void* loadSymbol(void* handle, const char* symbol) {
#if log_LINKER
    log_println("loadSymbol(%p, \"%s\")", handle, symbol);
#endif
    void* result = dlsym(handle, symbol);
#if log_LINKER
#if os_MAXVE
    log_println("loadSymbol(%p, \"%s\") = %p", handle, symbol, result);
#else
    char* errorMessage = dlerror();
    Dl_info info;
    void* address = result;
    if (dladdr(address, &info) != 0) {
        log_println("loadSymbol(%p, \"%s\") = %p from %s", handle, symbol, result, info.dli_fname);
    } else {
        log_println("loadSymbol(%p, \"%s\") = %p", handle, symbol, result);
    }
    if (errorMessage != NULL) {
        log_println("Error message: %s", errorMessage);
    }
#endif
#endif
    return result;
}

#if os_DARWIN || os_SOLARIS || os_LINUX

#include <netinet/in.h>
#include <netdb.h>
#include <fcntl.h>
#include <sys/socket.h>
#include <sys/types.h>
#include <signal.h>

#if os_DARWIN
#include <crt_externs.h>
#elif os_LINUX
#include <sys/prctl.h>
#elif os_SOLARIS
#define _STRUCTURED_PROC 1 /* Use new definitions in procfs.h instead of those in procfs_old.h */
#include <sys/procfs.h>
#endif

/**
 * Communicates the boot image address to a debugger that is listening on the port defined by the
 * MAX_AGENT_PORT environment variable. If this environment variable is not defined, then no
 * action is taken.
 *
 * Once the boot image address has been sent over the socket, this process puts itself into the
 * 'stopped' state expected by the debugger mechanism being used to control this process. For
 * example, under 'ptrace' this means raising a SIGTRAP.
 */
void debugger_initialize() {

    char *port = getenv("MAX_AGENT_PORT");
    if (port != NULL) {

#if os_LINUX && defined(PR_SET_PTRACER)
        /* See info about PR_SET_PTRACER at https://wiki.ubuntu.com/Security/Features#ptrace */
        char *val = getenv("MAX_AGENT_PID");
        if (val == NULL) {
            log_exit(11, "MAX_AGENT_PID must be set to the agent's PID so that ptrace can access the VM process");
        }
        long pid = strtol(val, NULL, 10);
        if (errno != 0) {
            log_exit(11, "Error converting MAX_AGENT_PID value \"%s\" to a long value: %s", val, strerror(errno));
        }
        prctl(PR_SET_PTRACER, pid, 0, 0, 0);
#endif

        char *hostName = "localhost";
#if log_TELE
        log_println("Opening agent socket connection to %s:%s", hostName, port);
#endif
        struct addrinfo hints, *res;
        memset(&hints, 0, sizeof hints);
        hints.ai_family = AF_UNSPEC;
        hints.ai_socktype = SOCK_STREAM;

        getaddrinfo(hostName, port, &hints, &res);

        int sockfd = socket(res->ai_family, res->ai_socktype, res->ai_protocol);
        if (sockfd == -1) {
            int error = errno;
            log_exit(11, "Could not create socket for communicating with debugger: %s", strerror(error));
        }

        if (connect(sockfd, res->ai_addr, res->ai_addrlen)) {
            int error = errno;
            log_exit(11, "Could not connect to debugger at %s:%s [%s]", hostName, port, strerror(error));
        }

        Address heap = image_heap();
#if log_TELE
        log_println("Sending boot heap address %p to debugger", heap);
#endif
        if (send(sockfd, &heap, sizeof(heap), 0) != sizeof(heap)) {
            log_exit(11, "Error sending boot image address to debugger");
        }

        if (close(sockfd) != 0) {
            int error = errno;
            log_exit(11, "Error closing socket to debugger: %s", strerror(error));
        }

        freeaddrinfo(res);

        /* Stop this process in such a way that control of this process is returned to the debugger. */
#if log_TELE
        log_println("Stopping VM for debugger");
#endif
#if os_DARWIN || os_LINUX
        kill(getpid(), SIGTRAP);
#elif os_SOLARIS
        int ctlfd = open("/proc/self/ctl", O_WRONLY);
        long controlCode = PCDSTOP;
        write(ctlfd, &controlCode, sizeof(controlCode));
#else
        c_UNIMPLEMENTED();
#endif
#if log_TELE
        log_println("VM resumed by debugger");
#endif
    }
}
#else
#define debugger_initialize()
#endif

/**
 * Gets a pointer to the global JNI function table.
 *
 * Defined in Native/substrate/jni.c
 */
extern JNIEnv jniEnv();

/**
 * Get pointers to the global JMM/JVMTI function tables.
 *
 * Defined in Native/substrate/{jmm.c,jvmti.c}
 */
void* getJMMInterface(int version);
void* getJVMTIInterface(int version);

/**
 *  ATTENTION: this signature must match the signatures of 'com.sun.max.vm.MaxineVM.run()':
 */
typedef jint (*VMRunMethod)(
                Address tlBlock,
                int tlBlockSize,
                Address bootHeapRegionStart,
                void *openLibrary(char *),
                void *dlsym(void *, const char *),
                char *dlerror(void),
                JNIEnv jniEnv,
                void *jmmInterface,
                void *jvmtiInterface,
                int argc,
                char *argv[]);

int maxine(int argc, char *argv[], char *executablePath) {
    VMRunMethod method;
    int exitCode = 0;
    int fd;
    int i;

    /* Extract the '-XX:LogFile' argument and pass the rest through to MaxineVM.run(). */
    const char *logFilePath = getenv("MAXINE_LOG_FILE");
    for (i = 1; i < argc; i++) {
        const char *arg = argv[i];
        if (strncmp(arg, "-XX:LogFile=", 12) == 0) {
            logFilePath = arg + 12;
            /* Null out the argument so that it is not parsed later. */
            argv[i] = NULL;
            break;
        }
    }
    log_initialize(logFilePath);

#if os_DARWIN
    _executablePath = executablePath;
#endif

#if log_LOADER
#if !os_MAXVE
    char *ldpath = getenv("LD_LIBRARY_PATH");
    if (ldpath == NULL) {
        log_println("LD_LIBRARY_PATH not set");
    } else {
        log_println("LD_LIBRARY_PATH=%s", ldpath);
    }
#endif
    log_println("Arguments: argc %d, argv %lx", argc, argv);
    for (i = 0; i < argc; i++) {
        const char *arg = argv[i];
        if (arg != NULL) {
            log_println("arg[%d]: %p, \"%s\"", i, arg, arg);
        } else {
            log_println("arg[%d]: %p", i, arg);
        }
    }
#endif

    fd = loadImage();

    tla_initialize(image_header()->tlaSize);

    debugger_initialize();

    method = image_offset_as_address(VMRunMethod, vmRunMethodOffset);

    Address tlBlock = threadLocalsBlock_create(PRIMORDIAL_THREAD_ID, 0, 0);
    NativeThreadLocals ntl = NATIVE_THREAD_LOCALS_FROM_TLBLOCK(tlBlock);

#if log_LOADER
    log_println("entering Java by calling MaxineVM.run(tlBlock=%p, bootHeapRegionStart=%p, openLibrary=%p, dlsym=%p, dlerror=%p, jniEnv=%p, jmmInterface=%p, jvmtiInterface=%p, argc=%d, argv=%p)",
                    tlBlock, image_heap(), openLibrary, loadSymbol, dlerror, jniEnv(), getJMMInterface(-1), getJVMTIInterface(-1), argc, argv);
#endif
    exitCode = (*method)(tlBlock, ntl->tlBlockSize, image_heap(), openLibrary, loadSymbol, dlerror, jniEnv(), getJMMInterface(-1), getJVMTIInterface(-1), argc, argv);

#if log_LOADER
    log_println("start method exited with code: %d", exitCode);
#endif

    if (exitCode == 0) {
        // Initialization succeeded: now run the main Java thread
        thread_run((void *) tlBlock);

        // The thread-specific data destructor function is not called for the main thread
        // TODO (dns) this is the behavior seen on Darwin-pthreads - confirm that it is true on other platforms
        // TODO (dns) should we delete the thread-specific data key (e.g. pthread_key_delete(3))? Can only do so if
        //      all other threads created by or attached the VM are guaranteed to be dead by now
        threadLocalsBlock_setCurrent(0);
        threadLocalsBlock_destroy(tlBlock);

        exitCode = image_read_value(int, exitCodeOffset);
    }

    if (fd > 0) {
        int error = close(fd);
        if (error != 0) {
            log_println("WARNING: could not close image file");
        }
    }

#if log_LOADER
    log_println("exit code: %d", exitCode);
#endif

    return exitCode;
}

/*
 * Native support. These global natives can be called from Java to get some basic services
 * from the C language and environment.
 */
void *native_executablePath() {
    static char result[MAX_PATH_LENGTH];
    getExecutablePath(result);
    return result;
}

void native_exit(jint code) {
    exit(code);
}

void core_dump() {
#if !os_MAXVE
    log_print("dumping core....\n  heap @ ");
    log_print_symbol(image_heap());
    log_print_newline();
    kill(getpid(), SIGABRT);
    sleep(3);
#endif
}

void native_trap_exit(int code, Address address) {
    log_print("In ");
    log_print_symbol(address);
    log_print_newline();
    log_exit(code, "Trap in native code at %p\n", address);
}

#if !os_DARWIN
extern
#endif
char **environ;

void *native_environment() {
#if os_DARWIN
    environ = (char **)*_NSGetEnviron();
#endif
#if log_LOADER
    int i = 0;
    for (i = 0; environ[i] != NULL; i++)
    log_println("native_environment[%d]: %s", i, environ[i]);
#endif
    return (void *)environ;
}

void *native_properties(void) {
    static native_props_t nativeProperties = {0, 0, 0};
    if (nativeProperties.user_dir != NULL) {
        return &nativeProperties;
    }
#if os_MAXVE
    maxve_native_props(&nativeProperties);
#else
    /* user properties */
    {
        struct passwd *pwent = getpwuid(getuid());
        nativeProperties.user_name = pwent ? strdup(pwent->pw_name) : "?";
        nativeProperties.user_home = pwent ? strdup(pwent->pw_dir) : "?";
    }

    /* Current directory */
    {
        char buf[MAXPATHLEN];
        errno = 0;
        if (getcwd(buf, sizeof(buf)) == NULL) {
            /* Error will be reported by Java caller. */
            nativeProperties.user_dir = NULL;
        } else {
            nativeProperties.user_dir = strdup(buf);
        }
    }
#endif
#if log_LOADER
    log_println("native_properties: user_name=%s", nativeProperties.user_name);
    log_println("native_properties: user_home=%s", nativeProperties.user_home);
    log_println("native_properties: user_dir=%s", nativeProperties.user_dir);
#endif
    return &nativeProperties;
}

float native_parseFloat(const char* cstring, float nan) {
#if os_MAXVE
    // TODO
    return nan;
#else
    char *endptr;
    float result = strtof(cstring, (char**) &endptr);
    if (endptr != cstring + strlen(cstring)) {
        result = nan;
    }
    return result;
#endif
}
