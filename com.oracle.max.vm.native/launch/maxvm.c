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
#ifdef JDK7
#define LIBRARY_NAME "libjvm.dylib"
#else
#define LIBRARY_NAME "libjvmlinkage.dylib"
#endif

/*
 * On Darwin, there is a fourth argument passed to main whose first element contains the path where the executing
 * binary was found on disk.
 *
 * See:
 * http://unixjunkie.blogspot.com/2006/02/char-apple-argument-vector.html
 * http://www.opensource.apple.com/darwinsource/10.5.4/dyld-96.2/src/dyldInitialization.cpp
 */
#define MAIN_EXTRA_ARGS , char *envp[], char *apple[]
#define PROG_PATH apple[0]

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
#define PROG_PATH argv[0]
#endif


/**
 * A simple launcher.
 */
int main(int argc, char *argv[] MAIN_EXTRA_ARGS) {
    char *programPath = PROG_PATH;
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
     * The JDK6 libraries on Mac OS X either have hard-coded (<= JDK 6_17) or file-system relative (>= JDK 6_20) paths to the HotSpot VM library.
     * The work-around for the former is to make copies of the JDK libraries and patch them to link to the correct Maxine VM library.
     * This is done with the bin/mod-maxosx-javalib.sh script. For the latter, the workaround is to set
     * the DYLD_LIBRARY_PATH environment variable to the directory containing the Maxine version of libjvmlinkage.dylib / libjvm.dylib
     * and re-exec the VM. The re-exec is necessary as the DYLD_LIBRARY_PATH is only read at exec.
     * Note that a similiar work-around is necessary for Java_com_sun_max_tele_channel_natives_TeleChannelNatives_createChild()
     * in com.oracle.max.vm.native/tele/darwin/darwinTeleProcess.c.
     */
    if (getenv("DYLD_LIBRARY_PATH") == NULL) {
        char *dyldLibraryPathDef;
        char *programDir = dirname(programPath);
        if (asprintf(&dyldLibraryPathDef, "DYLD_LIBRARY_PATH=%s", programDir) == -1) {
            fprintf(stderr, "Could not allocate space for defining DYLD_LIBRARY_PATH environment variable\n");
            exit(1);
        }
        putenv(dyldLibraryPathDef);
        execv(programPath, argv);

        fprintf(stderr, "execv(%s, ...) failed in maxvm: %s\n", programPath, strerror(errno));
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
	return (*maxine)(argc, argv, programPath);
#else
    free(libraryPath);
	return (*maxine)(argc, argv, NULL);
#endif
}
