/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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
#ifndef __mutex_h__
#define __mutex_h__ 1

#include "os.h"

#if (os_DARWIN || os_LINUX)
#   include <pthread.h>
    typedef pthread_mutex_t mutex_Struct;
#elif os_SOLARIS
#   include <thread.h>
#   include <synch.h>
    typedef mutex_t mutex_Struct;
#elif os_MAXVE
#   include <maxve.h>
    typedef maxve_monitor_t mutex_Struct;
#endif

typedef mutex_Struct *Mutex;

extern void mutex_initialize(Mutex mutex);
extern void mutex_dispose(Mutex mutex);

extern int mutex_enter(Mutex mutex);
extern int mutex_exit(Mutex mutex);

extern int mutex_enter_nolog(Mutex mutex);
extern int mutex_exit_nolog(Mutex mutex);

#endif /*__mutex_h__*/
