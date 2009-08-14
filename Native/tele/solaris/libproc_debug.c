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
/*
 * @author Hannes Payer
 */

#include <string.h>
#include <stdio.h>
#include "log.h"
#include "errno.h"

#include "proc.h"
#include "libproc_debug.h"

void statloc_eval(int statloc) {
    log_println("statloc evaluation:");
    log_println("statloc value: %d", statloc);
    if (WIFEXITED(statloc)) {
        log_print("WIFEXITED: ");
        log_print("%d; ", WEXITSTATUS(statloc));
        log_println("Evaluates to a non-zero value if status was returned for a child process that exited normally. ");
    }
    if (WIFSIGNALED(statloc)) {
        log_print("WIFSIGNALED: ");
        log_print("%d; ", WTERMSIG(statloc));
        log_println("Evaluates to a non-zero value if status was returned for a child process that terminated due to receipt of a signal that was not caught. ");
    }
//    if (WIFCORED(statloc)) {
//        log_print("WIFCORED: ");
//          log_print("%d; ", WCORESIG(statloc))
//        log_println("If the value of WIFSIGNALED(s) is non-zero, this macro evaluates to the number of the signal that caused the termination of the child process. ");
//    }
    if (WCOREDUMP(statloc)) {
        log_print("WCOREDUMP: ");
        log_println("Evaluates to a non-zero value if status was returned for a child process that terminated due to receipt of a signal that was not caught, and whose default action is to dump core. ");
    }
    if (WIFSTOPPED(statloc)) {
        log_print("WIFSTOPPED: ");
        log_print("%d; ", WSTOPSIG(statloc));
        log_println("Evaluates to a non-zero value if status was returned for a child process that is currently stopped. ");
    }
}
