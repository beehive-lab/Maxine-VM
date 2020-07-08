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
#ifndef __condition_h__
#define __condition_h__ 1

#include "mutex.h"

#if (os_DARWIN || os_LINUX)
#   include <pthread.h>
#   include <errno.h>
    typedef pthread_cond_t condition_Struct;
#elif os_SOLARIS
#   include <thread.h>
#   include <errno.h>
    typedef cond_t condition_Struct;
#elif os_MAXVE
#   include "maxve.h"
    typedef maxve_condition_t condition_Struct;
#endif

typedef condition_Struct *Condition;

extern void condition_initialize(Condition condition);
extern void condition_destroy(Condition condition);
extern boolean condition_wait(Condition condition, Mutex mutex);
extern boolean condition_timedWait(Condition condition, Mutex mutex, Unsigned8 milliSeconds);
extern boolean condition_notify(Condition condition);
extern boolean condition_notifyAll(Condition condition);

#endif /*__condition_h__*/
