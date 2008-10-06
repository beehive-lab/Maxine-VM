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
/*VCSID=46f75cb3-22e5-40ba-97bc-d7959d768ea5*/
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

/* Set to true to multicast all debug output to stdout as well as the relevant log file. */
#if !os_GUESTVMXEN
static Boolean printToStdout = false;
static FILE *logFile = NULL;
#endif

#if PROTOTYPE
#   define LOG_FILE_NAME    "logPrototype.txt"
#elif SUBSTRATE
#   define LOG_FILE_NAME    "logSubstrate.txt"
#elif INSPECTOR
#   define LOG_FILE_NAME    "logInspector.txt"
#else
#   define LOG_FILE_NAME    "log.txt"
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

FILE *getfstream(int fd) {
	if (fd == STDOUT) {
	   return stdout;
	}
	if (fd == STDERR) {
	   return stderr;
	}
#if os_GUESTVMXEN
	return NULL;
#else
	if (fd == LOGFILE) {
		if (logFile == NULL) {
			logFile = fopen(LOG_FILE_NAME, "w");
			/* Set the log file stream to flush whenever a newline character is encountered */
			setlinebuf(logFile);
		}
		return logFile;
	}
	FILE *file = fdopen(fd, "a");
    setlinebuf(file);
	return file;
#endif
}

void debug_print_format(int fd, char *format, ...) {
#if !os_GUESTVMXEN
    va_list ap;
    va_start(ap, format);
    vfprintf(getfstream(fd), format, ap);
    va_end(ap);

    if (printToStdout && fd != STDOUT) {
#endif
        va_list ap;
        va_start(ap, format);
        vprintf(format, ap);
        va_end(ap);
#if !os_GUESTVMXEN
    }
#endif
}

void debug_print_int(int fd, int val) {
    debug_print_format(fd, "%d", val);
}

void debug_print_boolean(int fd, char val) {
	if (val == 0) {
	    debug_print_format(fd, "false");
    } else {
        debug_print_format(fd, "true");
    }
}

void debug_print_char(int fd, int val) {
	debug_print_format(fd, "%c", val);
}

void debug_print_long(int fd, jlong val) {
	debug_print_format(fd, "%ld", val);
}

void debug_print_buffer(int fd, char *buffer) {
	debug_print_format(fd, "%s", buffer);
}

void debug_print_word(int fd, Address address) {
    debug_print_format(fd, ADDRESS_FORMAT, address);
}

void debug_print_newline(int fd) {
    debug_print_format(fd, NEWLINE_STRING);
}

void debug_print_float(int fd, float f) {
	// TODO: fprintf may not produce exactly the same format of floating point numbers
	debug_print_format(fd, "%f", f);
}

void debug_print_double(int fd, double d) {
	// TODO: fprintf may not produce exactly the same format of floating point numbers
	debug_print_format(fd, "%lf", d);

}
