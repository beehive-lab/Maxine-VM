/*
 * Copyright (c) 2010 Sun Microsystems, Inc.  All rights reserved.
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

#ifndef __trap_h__
#define __trap_h__ 1

#if os_GUESTVMXEN
#   include <guestvmXen.h>
#else
#   include <signal.h>
#   include <stdlib.h>
#   include <string.h>
#   include <sys/ucontext.h>
#   include <unistd.h>
#endif

#include "os.h"

#if os_GUESTVMXEN
#define SignalHandlerFunction fault_handler_t
#else
typedef ucontext_t UContext;
typedef siginfo_t SigInfo;
typedef void (*SignalHandlerFunction)(int signal, SigInfo *signalInfo, void *ucontext);
#endif

/**
 * Installs a handler for a signal and returns the previously installed handler.
 */
void* setSignalHandler(int signal, SignalHandlerFunction handler);

/**
 * The handler for the signals handled directly by the VM.
 */
extern SignalHandlerFunction vmSignalHandler;

/**
 * Sets the signal mask for the current thread. The signals in the mask are those
 * that are blocked for the thread.
 */
extern void setCurrentThreadSignalMask(boolean isVmOperationThread);

#endif
