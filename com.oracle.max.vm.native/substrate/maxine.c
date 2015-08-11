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
#include <sys/resource.h>
#include <math.h>
#include "log.h"
#include "image.h"
#include "threads.h"
#include "os.h"
#include "vm.h"
#include "virtualMemory.h"
#include "maxine.h"
#include <fenv.h>

#if os_MAXVE
#include "maxve.h"
#endif
#ifdef arm
//beginning of simulation platform functions
// defined in libCCluster.a
extern void reportCounters();
extern int initialiseMemoryCluster();
extern int getCoreCount();
extern void reportSpec();
extern void pushDLD(unsigned int address);
extern void pushILD(unsigned int address);
extern void pushDSTR(unsigned int address);
// end of simulation platform functions
void divideByZeroExceptions();
#   include <pthread.h>

static unsigned int *simPtr = (0);
static FILE *simFile = (0);

#endif
jint  maxine_instrumentationBuffer()  {
#ifdef arm
        if(simPtr != (0)) {
                printf("Really bad ERROR!!!!!!!! multiple initialisations of simptr in substrate");
                printf("NEEDS EXTENSION to work correctly with multiple thrreads\n");
        }
        simPtr = (unsigned int *) malloc(sizeof(unsigned int)*4096);
        *(simPtr +1023) = (unsigned int)simPtr;
        //printf("ALLOCATED at %u last element %u\n",(unsigned int)simPtr,*(simPtr +1023));
        return (jint)simPtr;
#else
	printf("INSTRUMENTATION for simulation not implemented for non armv7 platforms yet\n");
	return (jint)0;
#endif
}
#ifdef arm
void  maxine_close() {
        if(simFile != (0)) {
                fclose(simFile);
        }
}
#endif
void real_maxine_instrumentation(unsigned int address) {
	// the address has been altered to have a r/1 and a code/data bit set
#ifdef arm
	/*extern unsigned int getTID(unsigned int);
	unsigned int tid  = pthread_self();
	tid = getTID(tid);
	printf("THREAD ID is %u ADDRESS %x\n",tid,address);
	*/

	int isInstruction = 0;
	int isStore = 0;
	isInstruction = address & 0x2;
	isStore = address & 0x1;
	address = address & 0xfffffffd;
	if(isInstruction) {
		pushILD(address);
	} else {
		if(isStore) {
			pushDSTR(address);
		} else {
			pushDLD(address);
		}
	}
#endif
}
void  real_maxine_flush_instrumentationBuffer(unsigned int *bufPtr) {
#ifdef arm
        unsigned int i;
	printf("INSTRUMENTATION NEEDS to be rewritten to identify thread %lu\n", (unsigned long int)thread_self());
	printf("and to push a single address at a time");
        if((*(simPtr +1023)) != (unsigned int)(simPtr +1022)) {
                printf("ERROR VALSTORED %u VALEXPECTED %u SIMPTR %u\n", *(simPtr +1023) ,((unsigned int) (simPtr))+4*1022,(unsigned int)simPtr);
        }
        //printf("FLUSHING at %u\n",(unsigned int)simPtr);

// defined in libCCluster.a
        if(simFile == (0)) {
                simFile = fopen("address.trace","w");
        }    
        for(i = 0;i < *(simPtr + 1023);i++) {
                //printf("%x\n",*(simPtr+i));
                //printf("%x\n",*(bufPtr+i));
                fprintf(simFile,"%x\n",*(bufPtr+i));
                *(bufPtr+i) = 0;
    
        }
        *(simPtr +1023) = (unsigned int)simPtr;
#else
	printf("INSTRUMENTATION for simulation not implemented for non armv7 platforms yet\n");

#endif
}
jint  maxine_flush_instrumentationBuffer() {
#ifdef arm

        //return (jint) real_maxine_flush_instrumentationBuffer; // dirty yes ... but it should work      
	return (jint) real_maxine_instrumentation;
#else
	printf("INSTRUMENTATION for simulation not implemented for non armv7 platforms yet\n");
	return (jint)0;
#endif
}


long long d2long(double x) {
	//printf("As a long long %lld %lf\n",(long long) x,x);
	if(isnan(x)) {
		return (long long)0;
	}
	if (x <= (double)((long long)-9223372036854775808ULL))  return -9223372036854775808ULL;
	if (x >= (double)((long long)9223372036854775807ULL)) return 9223372036854775807ULL;
	return (long long)x;
}

long long  f2long(float x) {
	if(isnan(x)) {
		return (jlong)0;
	}
	if (x <= (float)((long long)-9223372036854775808ULL))  return -9223372036854775808ULL;
        if (x >= (float)((long long)9223372036854775807ULL)) return 9223372036854775807ULL;

	return (long long)x;
}

long long  arithmeticldiv(long long x, long long y) {
	if (y == 0) {
		//raise(SIGFPE);
		return 0;
	}
	return x/y;
}

jlong arithmeticlrem(jlong x, jlong y) {
	if(y == 0) {
		//raise(SIGFPE);
		return 0;
	}
	
	return x % y; 
}

unsigned long long  arithmeticludiv(unsigned long long x , unsigned long long y) {
	if(y == 0 ) {
		//raise(SIGFPE);
		return 0;
	}
	return x/y; 
}

unsigned long long  arithmeticlurem(unsigned long long x , unsigned long long y) {
	if(y == 0 ) {
		//raise(SIGFPE);
		return 0;
	}
	return x%y; 
}

double l2double(jlong x) {
	return (jdouble) x;
}

float l2float(jlong x) {
	return (jfloat) x;
}

static void max_fd_limit() {
#if os_LINUX || os_SOLARIS || os_DARWIN
    // set the number of file descriptors to max. print out error
    // if getrlimit/setrlimit fails but continue regardless.
    struct rlimit nbr_files;
    int status = getrlimit(RLIMIT_NOFILE, &nbr_files);
    if (status != 0) {
        log_println("getrlimit failed");
    } else {
#if os_DARWIN
	nbr_files.rlim_cur = MIN(OPEN_MAX, nbr_files.rlim_max);
#else
        nbr_files.rlim_cur = nbr_files.rlim_max;
#endif
        status = setrlimit(RLIMIT_NOFILE, &nbr_files);
        if (status != 0) {
            log_println("setrlimit failed");
        }
    }
#endif
}


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

static void loadImage(void) {
    char imageFilePath[MAX_PATH_LENGTH];
    getImageFilePath(imageFilePath);
	//printf("FILEPATH %s\n",imageFilePath);
    image_load(imageFilePath);
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
 *  ATTENTION: this signature must match the signatures of 'com.sun.max.vm.MaxineVM.run()':
 */
typedef jint (*VMRunMethod)(
                Address tlBlock,
                int tlBlockSize,
                Address bootHeapRegionStart,
                void *openLibrary(char *),
                void *dlsym(void *, const char *),
                char *dlerror(void),
                void* vmInterface,
                JNIEnv jniEnv,
                void *jmmInterface,
                void *jvmtiInterface,
                int argc,
                char *argv[]);

int maxine(int argc, char *argv[], char *executablePath) {
    VMRunMethod method;
    int exitCode = 0;
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
    max_fd_limit();

    loadImage();
    //printf("IMAGE LOADED\n");
    tla_initialize(image_header()->tlaSize);
    //printf("TLA INITED\n");

    debugger_initialize();
    //printf("DEBUGGER INITED\n");

    method = image_offset_as_address(VMRunMethod, vmRunMethodOffset);
    //printf("VMRUNMETHODOFFSET 0x%x\n", vmRunMethodOffset);//vmRunMethodOffset);

    Address tlBlock = threadLocalsBlock_create(PRIMORDIAL_THREAD_ID, 0, 0);
    NativeThreadLocals ntl = NATIVE_THREAD_LOCALS_FROM_TLBLOCK(tlBlock);
#ifdef arm
    //printf("THREAD LOCALS method entry %p NTL %x\n",method,(Address ) ntl);
    //printf("THREAD LOCALS block size %u \n",ntl->tlBlockSize);
    //printf("Main method entry %p\n", method);
#else
    //printf("THREAD LOCALS method entry %p NTL %llx\n",method,(Address ) ntl);
    //printf("THREAD LOCALS block size %llu \n",ntl->tlBlockSize);
    //printf("Main method entry %p\n", method);
#endif

#ifdef arm
divideByZeroExceptions();        
#ifdef SIMULATIONPLATFORM
initialiseMemoryCluster();
#endif
#endif

#if log_LOADER
    log_println("entering Java by calling MaxineVM.run(tlBlock=%p, bootHeapRegionStart=%p, openLibrary=%p, dlsym=%p, dlerror=%p, vmInterface=%p, jniEnv=%p, jmmInterface=%p, jvmtiInterface=%p, argc=%d, argv=%p)",
                    tlBlock, image_heap(), openLibrary, loadSymbol, dlerror, getVMInterface(), jniEnv(), getJMMInterface(-1), getJVMTIInterface(-1), argc, argv);
#endif
    //printf("dlopen %p dlsym %p dlsym %p\n",openLibrary,loadSymbol,dlerror);
    exitCode = (*method)(tlBlock, ntl->tlBlockSize, image_heap(), openLibrary, loadSymbol, dlerror, getVMInterface(), jniEnv(), getJMMInterface(-1), getJVMTIInterface(-1), argc, argv);

#if log_LOADER
    log_println("start method exited with code: %d", exitCode);
#endif

    if (exitCode == 0) {
        // Initialization succeeded: now run the main Java thread
	//printf("ENTERING MAIN JAVA THREAD TO RUN\n");
        thread_run((void *) tlBlock);
    } else {
        printf("NON ZERO NATIVE EXIT %d\n",exitCode);
	//real_maxine_flush_instrumentationBuffer(simPtr);
#ifdef arm
#ifdef SIMULATIONPLATFORM
	printf("ABOUT to report counters\n");
	reportCounters();
#endif
	maxine_close();
#endif
        native_exit(exitCode);
    }
    //printf("NEVER REACHED\n");
    // All exits should be routed through native_exit().
    log_exit(-1, "Should not reach here\n");
}

/*
 * Native support. These global natives can be called from Java to get some basic services
 * from the C language and environment.
 */
void *native_executablePath() {
#ifdef arm
	divideByZeroExceptions();
#endif
    static char result[MAX_PATH_LENGTH];
    getExecutablePath(result);
    return result;
}

static void cleanupCurrentThreadBlockBeforeExit() {
    Address tlBlock = threadLocalsBlock_current();
    if (tlBlock != 0) {
        threadLocalsBlock_setCurrent(0);
        threadLocalsBlock_destroy(tlBlock);
    }
}

void native_exit(jint code) {
    // TODO: unmap the image
    // (mjj) It is not clear to me why it is important to clean up
    // (just) the current thread block since we are exiting anyway,
    // but it is a bad idea if we crashed because it calls back into the VM,
    // which can cause a recursive crash.
#ifdef arm
#ifdef SIMUATIONPLATFORM
	printf("NATIVE EXIT ABOUT to report counters\n");
	reportCounters();
#endif
	maxine_close();
#endif
    if (code != 11) {
        cleanupCurrentThreadBlockBeforeExit();
    }
    exit(code);
}

void core_dump() {
#if !os_MAXVE
    log_print("dumping core....\n  heap @ ");
    log_print_symbol(image_heap());
    log_print_newline();
    // Use kill instead of abort so the vm process keeps running after the core is created.
    kill(getpid(), SIGABRT);
    sleep(3);
#endif
}

void native_trap_exit(int code, Address address) {
    log_print("In ");
    log_print_symbol(address);
    log_print_newline();
    // See native_exit
    // cleanupCurrentThreadBlockBeforeExit();
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
#ifdef arm
void divideByZeroExceptions() {
#ifdef arm
	asm volatile("vmrs r12, FPSCR");
	asm volatile("movw r0,0x100");
	asm volatile("orr r12,r12,r0");
	asm volatile("vmsr FPSCR,r12");
/* need to setup the environment variable appropriately 
	int x =feenableexcept(FE_DIVBYZERO);
	printf("feenableexcepts %d\n",x);
	x = fegetexcept();
	printf("feget %d\n",x);
*/
#endif
}
void maxine_cacheflush(char *start, int length) {
	char * end = start + length;
	//printf("FLUSHED CACHE \n");
	asm volatile("isb ");
	asm volatile("dsb ");
	asm volatile("dmb ");
	__clear_cache(start, end);
	asm volatile("isb ");
	asm volatile("dsb ");
	asm volatile("dmb ");


#endif

}
/*
static unsigned int *simPtr = (0);
static FILE *simFile = (0);
jint  maxine_instrumentationBuffer()  {
	if(simPtr != (0)) {
		printf("Really bad ERROR!!!!!!!! multiple initialisations of simptr in substrate");
		printf("NEEDS EXTENSION to work correctly with multiple thrreads\n");
	}
	simPtr = (unsigned int *) malloc(sizeof(unsigned int)*4096);
	*(simPtr +1023) = (unsigned int)simPtr;
	//printf("ALLOCATED at %u last element %u\n",(unsigned int)simPtr,*(simPtr +1023));
	return (jint)simPtr;
}
void  maxine_close() {
	if(simFile != (0)) {
		fclose(simFile);
	}
}
void  real_maxine_flush_instrumentationBuffer(unsigned int *bufPtr) {
	unsigned int i;
	if((*(simPtr +1023)) != (unsigned int)(simPtr +1022)) {
		printf("ERROR VALSTORED %u VALEXPECTED %u SIMPTR %u\n", *(simPtr +1023) ,((unsigned int) (simPtr))+4*1022,(unsigned int)simPtr);
	}
	//printf("FLUSHING at %u\n",(unsigned int)simPtr);

	if(simFile == (0)) {
		simFile = fopen("address.trace","w");
	}	
	for(i = 0;i < *(simPtr + 1023);i++) {
		//printf("%x\n",*(simPtr+i));
		printf("%x\n",*(bufPtr+i));
		fprintf(simFile,"%x\n",*(bufPtr+i));
		*(bufPtr+i) = 0;
		
	}
	*(simPtr +1023) = (unsigned int)simPtr;
}
jint  maxine_flush_instrumentationBuffer() {
	return (jint) real_maxine_flush_instrumentationBuffer; // dirty yes ... but it should work	
}
*/
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

double native_parseDouble(const char* cstring, double nan) {
#if os_MAXVE
    // TODO
    return nan;
#else
    char *endptr;
    double result = strtod(cstring, (char**) &endptr);
    if (endptr != cstring + strlen(cstring)) {
        result = nan;
    }
    return result;
#endif
}
