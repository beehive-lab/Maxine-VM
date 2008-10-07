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

#if os_DARWIN
#   define LIBRARY_NAME "libjvm.dylib"
#else
#   define LIBRARY_NAME "libjvm.so"
#endif

typedef int (*MaxineFunction)(int argc, char *argv[], char *executablePath);

/**
 * A simple launcher.
 */
#if os_DARWIN
/*
 * On Darwin, there is a fourth argument passed to main whose first element contains the path where the executing
 * binary was found on disk.
 *
 * See:
 * http://unixjunkie.blogspot.com/2006/02/char-apple-argument-vector.html
 * http://www.opensource.apple.com/darwinsource/10.5.4/dyld-96.2/src/dyldInitialization.cpp
 */
int main(int argc, char *argv[], char *envp[], char *apple[]) {
#else
int main(int argc, char *argv[]) {
#endif
    char *programPath = argv[0];
	char *p = programPath;
    int prefixLength = 0;
    while (*p) {
    	if (*p == '/' || *p == '\\') {
    		prefixLength = p + 1 - programPath;
    	}
    	p++;
    }
	char *libraryPath = malloc(prefixLength + strlen(LIBRARY_NAME) + 1);
	strncpy(libraryPath, programPath, prefixLength);
	strcpy(libraryPath + prefixLength, LIBRARY_NAME);

	void *handle = dlopen(libraryPath, RTLD_LAZY | RTLD_GLOBAL);
	if (handle == 0) {
		fprintf(stderr, "could not load libjvm.so: %s\n", dlerror());
		exit(1);
	}
	free(libraryPath);

	MaxineFunction maxine = (MaxineFunction) dlsym(handle, "maxine");
	if (maxine == 0) {
        fprintf(stderr, "could not find entry point in libjvm.so: %s\n", dlerror());
		exit(1);
	}

#if os_DARWIN
	return (*maxine)(argc, argv, apple[0]);
#else
	return (*maxine)(argc, argv, NULL);
#endif
}
