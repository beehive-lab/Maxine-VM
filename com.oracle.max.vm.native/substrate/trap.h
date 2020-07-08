/*
 * Copyright (c) 2017, APT Group, School of Computer Science,
 * The University of Manchester. All rights reserved.
 * Copyright (c) 2016, Andrey Rodchenko. All rights reserved.
 * Copyright (c) 2010, 2011, Oracle and/or its affiliates. All rights reserved.
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
 */

#ifndef __trap_h__
#define __trap_h__ 1

#if os_MAXVE
#   include <maxve.h>
#else
#   include <signal.h>
#   include <stdlib.h>
#   include <string.h>
#   include <sys/ucontext.h>
#   include <unistd.h>
#endif

#include "os.h"

#if os_MAXVE
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
 * The handler for signals dispatched by SignalDispatcher.java.
 */
extern SignalHandlerFunction userSignalHandler;

/**
 * Option controlling tracing of signal related functionality.
 */
extern boolean traceSignals;

/**
 * Sets the signal mask for the current thread on thread exit.
 */
extern void setCurrentThreadSignalMaskOnThreadExit(boolean isVmOperationThread);

/**
 * Sets the signal mask for the current thread. The signals in the mask are those
 * that are blocked for the thread.
 */
extern void setCurrentThreadSignalMask(boolean isVmOperationThread);

/**
 * Sets the signal mask for the current thread on thread exit.
 */
extern void setCurrentThreadSignalMaskOnThreadExit(boolean isVmOperationThread);

#endif
