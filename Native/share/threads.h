/*
 * Copyright (c) 2007, 2010, Oracle and/or its affiliates. All rights reserved.
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
#ifndef __threads_h__
#define __threads_h__ 1

#include "os.h"
#include "jni.h"
#include "word.h"
#include "threadLocals.h"

/*
 * The constants must be in sync with the static variables of the same name in VmThread.java
 */
#define STACK_YELLOW_ZONE_PAGES 1
#define STACK_RED_ZONE_PAGES 1

/**
 * The signature of the VM entry point for adding a thread to the thread list.
 * This must match the signature of 'com.sun.max.vm.thread.VmThread.add()'.
 */
typedef jint (*VmThreadAddMethod)(jint id,
                jboolean daemon,
                Address nativeThread,
                Address threadLocals,
                Address stackBase,
                Address stackEnd,
                Address stackYellowZone);

/**
 * The signature of the VM entry point for running a new VM-created thread.
 * This must match the signature of 'com.sun.max.vm.thread.VmThread.run()'.
 */
typedef void (*VmThreadRunMethod)(Address threadLocals,
                Address stackBase,
                Address stackEnd);

/**
 * The signature of the VM entry point for attaching a native thread.
 * This must match the signature of 'com.sun.max.vm.thread.VmThread.attach()'.
 */
typedef int (*VmThreadAttachMethod)(
                Address name,
                Address group,
                jboolean daemon,
                Address stackBase,
                Address stackEnd,
                Address tla);

/**
 * The signature of the Java method used to detach native threads.
 * This must match the signature of 'com.sun.max.vm.thread.VmThread.detach()'.
 */
typedef void (*VmThreadDetachMethod)(Address tla);

/**
 * Sleeps the current thread for a given number of milliseconds.
 *
 * @return true if the sleep was interrupted
 */
extern jboolean thread_sleep(jlong numberOfMilliSeconds);

int thread_attachCurrent(void **penv, JavaVMAttachArgs* args, boolean daemon);
int thread_detachCurrent();

/**
 * Gets the address and size of the calling thread's stack.
 *
 * @param stackBase the base (i.e. lowest) address of the stack is returned in this argument
 * @param stackSize the size of the stack is returned in this argument
 */
extern void thread_getStackInfo(Address *stackBase, Size* stackSize);

/**
 * For debugging purposes:
 */
extern void *thread_self(void);

#endif /*__threads_h__*/
