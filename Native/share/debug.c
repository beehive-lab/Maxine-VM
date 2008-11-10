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
 * @author Bernd Mathiske
 */
#include <stdarg.h>
#include <stdlib.h>
#include <stdio.h>
#include <string.h>

#include "debug.h"
#include "jni.h"
#include "mutex.h"
#include "threads.h"

#if !os_GUESTVMXEN
static FILE *fileStream = NULL;
#endif

void debug_assert(Boolean condition, char *conditionString, char *fileName, int lineNumber) {
    if (!condition) {
        debug_println("debug_assert %s[%d]: %s", fileName, lineNumber, conditionString);
        exit(1);
    }
}

static jboolean _isLockInitialized = false;
static mutex_Struct _mutex;

void debug_lock(void) {
	if (!_isLockInitialized) {
		mutex_initialize(&_mutex);
		_isLockInitialized = true;
	}
	int result;
	if ((result = mutex_lock(&_mutex)) != 0) {
	    debug_exit(-1, "Could not lock mutex: %s", strerror(result));
	}
}

void debug_unlock(void) {
    int result;
	if ((result = mutex_unlock(&_mutex)) != 0) {
        debug_exit(-1, "Could not lock mutex: %s", strerror(result));
	}
}

#if !os_GUESTVMXEN
FILE *getFileStream() {
    if (fileStream == NULL) {
        char *path = getenv("MAXINE_LOG_FILE");
        if (path == NULL) {
            path = "stdout";
        }
        if (strncmp(path, "stdout\0", 7) == 0) {
            fileStream = stdout;
            /* Set the file stream to flush whenever a newline character is encountered */
            setlinebuf(fileStream);
        } else if (strncmp(path, "stderr\0", 7) == 0) {
            fileStream = stderr;
        } else {
            fileStream = fopen(path, "w");
            if (fileStream == NULL) {
                fprintf(stderr, "Could not open file for VM output stream: %s\n", path);
                exit(1);
            }
            /* Set the file stream to flush whenever a newline character is encountered */
            setlinebuf(fileStream);
        }

    }
    return fileStream;
}
#endif

void debug_print_format(char *format, ...) {
#if !os_GUESTVMXEN
    va_list ap;
    va_start(ap, format);
    FILE* out = getFileStream();
    vfprintf(out, format, ap);
    va_end(ap);
#else
    va_list ap;
    va_start(ap, format);
    vprintf(format, ap);
    va_end(ap);
#endif
}

void debug_print_int(int val) {
    debug_print_format("%d", val);
}

void debug_print_boolean(char val) {
	if (val == 0) {
	    debug_print_format("false");
    } else {
        debug_print_format("true");
    }
}

void debug_print_char(int val) {
	debug_print_format("%c", val);
}

void debug_print_long(jlong val) {
	debug_print_format("%ld", val);
}

void debug_print_buffer(char *buffer) {
	debug_print_format("%s", buffer);
}

void debug_print_word(Address address) {
    debug_print_format(ADDRESS_FORMAT, address);
}

void debug_print_newline() {
    debug_print_format(NEWLINE_STRING);
}

void debug_print_float(float f) {
	// TODO: fprintf may not produce exactly the same format of floating point numbers
	debug_print_format("%f", f);
}

void debug_print_double(double d) {
	// TODO: fprintf may not produce exactly the same format of floating point numbers
	debug_print_format("%lf", d);

}
