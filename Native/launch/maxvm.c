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
#include <dlfcn.h>
#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <strings.h>

#include "os.h"
#include "maxine.h"

typedef int (*MaxineFunction)(int argc, char *argv[], char *executablePath);

#if os_DARWIN
#include <unistd.h>
#include <libgen.h>
#define LIBRARY_NAME "libjvmlinkage.dylib"

/*
 * On Darwin, there is a fourth argument passed to main whose first element contains the path where the executing
 * binary was found on disk.
 *
 * See:
 * http://unixjunkie.blogspot.com/2006/02/char-apple-argument-vector.html
 * http://www.opensource.apple.com/darwinsource/10.5.4/dyld-96.2/src/dyldInitialization.cpp
 */
#define MAIN_EXTRA_ARGS , char *envp[], char *apple[]

/*
 * CoreFoundation will be (indirectly) loaded when libjava.jnilib is dynamically linked
 * during VM startup. This occur's on the VM's "main" thread which is _not_ the thread returned
 * by a call to 'pthread_main_np()'. As of the Snow Leopard, the '__CFInitialize' function
 * called when CoreFoundation expects to be executing on pthread_main_np().
 * As such, a constant of a CoreFoundation data type is declared here to ensure that it
 * is loaded on the correct thread. This solution was arrived at after reading the source code at:
 *
 *     http://www.opensource.apple.com/source/CF/CF-550/CFRuntime.c
 */
#include <CoreFoundation/CoreFoundation.h>
const CFNullRef initializeCoreFoundationOnMainThread;

#else
#define LIBRARY_NAME "libjvm.so"
#define MAIN_EXTRA_ARGS
#endif


/**
 * A simple launcher.
 */
int main(int argc, char *argv[] MAIN_EXTRA_ARGS) {
    char *programPath = argv[0];
	char *p = programPath;
    int prefixLength = 0;
    while (*p) {
    	if (*p == '/' || *p == '\\') {
    		prefixLength = p + 1 - programPath;
    	}
    	p++;
    }

#if os_DARWIN
    /*
     * The JDK libraries on Mac OS X either have hard-coded (<= JDK 6_17) or file-system relative (>= JDK 6_20) paths to the HotSpot VM library.
     * The work-around for the former is to make copies of the JDK libraries and patch them to link to the correct Maxine VM library.
     * This is done with the bin/mod-maxosx-javalib.sh script. For the latter, the workaround is to set
     * the DYLD_LIBRARY_PATH environment variable to the directory containing the Maxine version of libjvmlinkage.dylib
     * and re-exec the VM. The re-exec is necessary as the DYLD_LIBRARY_PATH is only read at exec.
     * Note that a similiar work-around is necessary for Java_com_sun_max_tele_debug_darwin_DarwinTeleProcess_nativeCreateChild()
     * in Native/tele/darwin/darwinTeleProcess.c.
     */
    if (getenv("DYLD_LIBRARY_PATH") == NULL) {
        char *dyldLibraryPathDef;
        char *programDir = dirname(programPath);
        if (asprintf(&dyldLibraryPathDef, "DYLD_LIBRARY_PATH=%s", programDir) == -1) {
            fprintf(stderr, "Could not allocate space for defining DYLD_LIBRARY_PATH environment variable\n");
            exit(1);
        }
        putenv(dyldLibraryPathDef);
        execv(argv[0], argv);

        fprintf(stderr, "execv failed in maxvm: %s", strerror(errno));
        exit(1);
    }
    char *libraryPath = LIBRARY_NAME;
#else
    char *libraryPath = malloc(prefixLength + strlen(LIBRARY_NAME) + 1);
    strncpy(libraryPath, programPath, prefixLength);
    strcpy(libraryPath + prefixLength, LIBRARY_NAME);
#endif

    void *handle = dlopen(libraryPath, RTLD_LAZY | RTLD_GLOBAL);
	if (handle == 0) {
		fprintf(stderr, "could not load %s: %s\n", LIBRARY_NAME, dlerror());
		exit(1);
	}

	MaxineFunction maxine = (MaxineFunction) dlsym(handle, "maxine");
	if (maxine == 0) {
        fprintf(stderr, "could not find symbol 'maxine' in %s: %s\n", LIBRARY_NAME, dlerror());
		exit(1);
	}

#if os_DARWIN
	return (*maxine)(argc, argv, apple[0]);
#else
    free(libraryPath);
	return (*maxine)(argc, argv, NULL);
#endif
}
