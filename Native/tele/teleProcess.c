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
#include <string.h>
#include <stdlib.h>

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


ThreadSpecifics teleProcess_findThreadSpecifics(PROCESS_MEMORY_PARAMS Address threadSpecificsListAddress, Address stackPointer, ThreadSpecifics threadSpecifics) {
    ThreadSpecificsListStruct threadSpecificsListStruct;

    ThreadSpecificsList threadSpecificsList = &threadSpecificsListStruct;
    READ_PROCESS_MEMORY(threadSpecificsListAddress, threadSpecificsList, sizeof(ThreadSpecificsListStruct));
    Address threadSpecificsAddress = (Address) threadSpecificsList->head;
    while (threadSpecificsAddress != 0) {
        READ_PROCESS_MEMORY(threadSpecificsAddress, threadSpecifics, sizeof(ThreadSpecificsStruct));
#if log_TELE
        log_print("teleProcess_findThreadSpecifics(%p): ", stackPointer);
        threadSpecifics_println(threadSpecifics);
#endif
        Address stackBase = threadSpecifics->stackBase;
        Size stackSize = threadSpecifics->stackSize;
        if (stackBase <= stackPointer && stackPointer < (stackBase + stackSize)) {
            return threadSpecifics;
        }
        threadSpecificsAddress = (Address) threadSpecifics->next;
    }

    return NULL;
}

int teleProcess_read(PROCESS_MEMORY_PARAMS JNIEnv *env, jclass c, jlong src, jobject dst, jboolean isDirectByteBuffer, jint offset, jint length) {
    Word bufferWord;
    void* dstBuffer;
    size_t size = (size_t) length;
    if (isDirectByteBuffer) {
        // Direct ByteBuffer: get the address of the buffer and adjust it by offset
        dstBuffer = (*env)->GetDirectBufferAddress(env, dst);
        if (dstBuffer == 0) {
            log_println("Failed to get address from NIO direct buffer");
            return -1;
        }
        dstBuffer = (jbyte *) dstBuffer + offset;
    } else {
        if (size >  sizeof(Word)) {
            // More than a word's worth of bytes: allocate a buffer
            dstBuffer = (void *) malloc(length * sizeof(jbyte));
            if (dstBuffer == 0) {
                log_println("Failed to malloc byte array of %d bytes", length);
                return -1;
            }
        } else {
            // Less than or equal to a word's woth of bytes: use stack memory
            dstBuffer = (void *) &bufferWord;
        }
    }

    // Do the read
    jint bytesRead = READ_PROCESS_MEMORY(src, dstBuffer, size);

    if (!isDirectByteBuffer) {
        if (bytesRead > 0) {
            (*env)->SetByteArrayRegion(env, dst, offset, bytesRead, dstBuffer);
        }
        if (dstBuffer != (void *) &bufferWord) {
            free(dstBuffer);
        }
    }
    return bytesRead;
}

int teleProcess_write(PROCESS_MEMORY_PARAMS JNIEnv *env, jclass c, jlong dst, jobject src, jboolean isDirectByteBuffer, jint offset, jint length) {
    Word bufferWord;
    void* srcBuffer;
    size_t size = (size_t) length;
    if (isDirectByteBuffer) {
        srcBuffer = (*env)->GetDirectBufferAddress(env, src);
        if (srcBuffer == 0) {
            log_println("Failed to get address from NIO direct buffer");
            return -1;
        }
        srcBuffer = (jbyte *) srcBuffer + offset;
    } else {
        if (size >  sizeof(Word)) {
            srcBuffer = malloc(length * sizeof(jbyte));
            if (srcBuffer == 0) {
                log_println("failed to malloc byte array of %d bytes", length);
                return -1;
            }
        } else {
            // Less than or equal to a word's woth of bytes: use stack memory
            srcBuffer = (void *) &bufferWord;
        }
        (*env)->GetByteArrayRegion(env, src, offset, length, srcBuffer);
        if ((*env)->ExceptionOccurred(env) != NULL) {
            log_println("failed to copy %d bytes from byte array into buffer", length);
            return -1;
        }
    }

    int result = WRITE_PROCESS_MEMORY(dst, srcBuffer, size);

    if (!isDirectByteBuffer) {
        if (srcBuffer != (void *) &bufferWord) {
            free(srcBuffer);
        }
    }
    return result;
}

