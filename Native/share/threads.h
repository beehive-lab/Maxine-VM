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
                Address threadLocals);

/**
 * The signature of the Java method used to detach native threads.
 * This must match the signature of 'com.sun.max.vm.thread.VmThread.detach()'.
 */
typedef void (*VmThreadDetachMethod)(Address threadLocals);

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
