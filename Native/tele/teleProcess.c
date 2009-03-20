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

#include "c.h"
#include "log.h"
#include "jni.h"
#include "threadSpecifics.h"
#include "teleProcess.h"

static jmethodID _jniGatherThreadID = NULL;

void teleProcess_jniGatherThread(JNIEnv *env, jobject teleProcess, jobject threadSequence, jlong handle, ThreadState_t state, ThreadSpecifics threadSpecifics) {

    if (_jniGatherThreadID == NULL) {
        jclass c = (*env)->GetObjectClass(env, teleProcess);
        c_ASSERT(c != NULL);
        _jniGatherThreadID = (*env)->GetMethodID(env, c, "jniGatherThread", "(Lcom/sun/max/collect/AppendableSequence;IJIJJJJJ)V");
        c_ASSERT(_jniGatherThreadID != NULL);
    }

    ThreadSpecificsStruct noThreadSpecifics;
    if (threadSpecifics == NULL) {
        threadSpecifics = &noThreadSpecifics;
        memset(threadSpecifics, 0, sizeof(noThreadSpecifics));
        jint id = handle;
        // Made id negative to indicate no thread specifics were available for the thread
        threadSpecifics->id = id < 0 ? id : -id;
    }

    tele_log_println("Gathered thread[id=%d, handle=%lu, stackBase=%p, stackEnd=%p, stackSize=%lu, triggeredVmThreadLocals=%p, enabledVmThreadLocals=%p, disabledVmThreadLocals=%p]",
                    threadSpecifics->id,
                    handle,
                    threadSpecifics->stackBase,
                    threadSpecifics->stackBase + threadSpecifics->stackSize,
                    threadSpecifics->stackSize,
                    threadSpecifics->triggeredVmThreadLocals,
                    threadSpecifics->enabledVmThreadLocals,
                    threadSpecifics->disabledVmThreadLocals);

    (*env)->CallVoidMethod(env, teleProcess, _jniGatherThreadID, threadSequence,
                    threadSpecifics->id,
                    handle,
                    state,
                    threadSpecifics->stackBase,
                    threadSpecifics->stackSize,
                    threadSpecifics->triggeredVmThreadLocals,
                    threadSpecifics->enabledVmThreadLocals,
                    threadSpecifics->disabledVmThreadLocals);
}
