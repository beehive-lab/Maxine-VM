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
#elif os_GUESTVMXEN
#   include <guestvmXen.h>
    typedef guestvmXen_monitor_t mutex_Struct;
#endif

typedef mutex_Struct *Mutex;

extern void mutex_initialize(Mutex mutex);
extern void mutex_dispose(Mutex mutex);

extern int mutex_enter(Mutex mutex);
extern int mutex_exit(Mutex mutex);

extern Boolean mutex_isHeld(Mutex mutex);

#endif /*__mutex_h__*/
