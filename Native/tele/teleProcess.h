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
#ifndef __teleProcess_h__
#define __teleProcess_h__ 1

#include "teleNativeThread.h"

extern void teleProcess_initialize(void);

/**
 * Makes the upcall to TeleProcess.jniGatherThread
 *
 * @param teleProcess the TeleProcess object gathering the threads
 * @param threads a Sequence<TeleNativeThread> object used to gather the threads
 * @param handle the native thread library handle to a thread (e.g. the LWP of a Solaris thread)
 * @param state the execution state of the thread
 * @param threadSpecifics the thread specifics foudn based on the stack pointer of the thread or NULL if no such thread specifics were found
 */
extern void teleProcess_jniGatherThread(JNIEnv *env, jobject teleProcess, jobject threadSequence, jlong handle, ThreadState_t state, ThreadSpecifics threadSpecifics);

#endif /*__teleProcess_h__*/
