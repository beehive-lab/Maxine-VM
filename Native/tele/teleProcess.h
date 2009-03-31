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
#ifndef __teleProcess_h__
#define __teleProcess_h__ 1

#include "teleNativeThread.h"

extern void teleProcess_initialize(void);

#if os_LINUX
#include <sys/types.h>
size_t task_read(pid_t tgid, pid_t tid, const void *src, void *dst, size_t size);
size_t task_write(pid_t tgid, pid_t tid, void *dst, const void *src, size_t size);
#define PROCESS_MEMORY_PARAMS pid_t tgid, pid_t tid,
#define READ_PROCESS_MEMORY(src, dst, size) task_read(tgid, tid, (const void *) src, (void *) dst, (size_t) size)
#define WRITE_PROCESS_MEMORY(dst, src, size) task_write(tgid, tid, (void *) dst, (const void *) src, (size_t) size)
#elif os_DARWIN
#include <mach/mach.h>
int task_read(task_t task, vm_address_t src, void *dst, size_t size);
int task_write(task_t task, vm_address_t dst, void *src, size_t size);
#define PROCESS_MEMORY_PARAMS task_t task,
#define READ_PROCESS_MEMORY(src, dst, size) task_read(task, (vm_address_t) src, (void *) dst, (size_t) size)
#define WRITE_PROCESS_MEMORY(dst, src, size) task_write(task, (vm_address_t) dst, (void *) src, (size_t) size)
#elif os_SOLARIS
#include "proc.h"
#define PROCESS_MEMORY_PARAMS struct ps_prochandle *ph,
#define READ_PROCESS_MEMORY(src, dst, size) proc_Pread(ph, (void *) dst, (size_t) size, (uintptr_t) src)
#define WRITE_PROCESS_MEMORY(dst, src, size) proc_Pwrite(ph, src, length, (uintptr_t) dst);
#elif os_GUESTVMXEN
extern unsigned short readbytes(unsigned long src, char *dst, unsigned short n);
extern unsigned short readbytes(unsigned long dst, char *src, unsigned short n);
#define PROCESS_MEMORY_PARAMS
#define READ_PROCESS_MEMORY(src, dst, size) readbytes((unsigned long) src, (char *) dst, (unsigned short) size)
#define WRITE_PROCESS_MEMORY(dst, src, size) writes((unsigned long) dst, (char *) src, (unsigned short) size);
#else
#error
#endif

/**
 * Searches a ThreadSpecificsList (see threadSpecifics.h) in the VM's address space for a ThreadSpecifics
 * entry 'ts' such that:
 *
 *   ts.stackBase <= stackPointer && stackPointer < (ts.stackBase + ts.stackSize)
 *
 * If such an entry is found, then its contents are copied from the VM to the given 'threadSpecifics' struct.
 *
 * @param threadSpecificsListAddress the address of the ThreadSpecificsList in the VM's address space
 * @param stackPointer the stack pointer to search with
 * @param threadSpecifics pointer to storage for a ThreadSpecificsStruct into which the found entry
 *        (if any) will be copied from the VM's address space
 * @return the entry that was found, NULL otherwise
 */
extern ThreadSpecifics teleProcess_findThreadSpecifics(PROCESS_MEMORY_PARAMS Address threadSpecificsListAddress, Address stackPointer, ThreadSpecifics threadSpecifics);

/**
 * Makes the upcall to TeleProcess.jniGatherThread
 *
 * @param teleProcess the TeleProcess object gathering the threads
 * @param threads a Sequence<TeleNativeThread> object used to gather the threads
 * @param handle the native thread library handle to a thread (e.g. the LWP of a Solaris thread)
 * @param state the execution state of the thread
 * @param instructionPointer
 * @param threadSpecifics the thread specifics found based on the stack pointer of the thread or NULL if no such thread specifics were found
 */
extern void teleProcess_jniGatherThread(JNIEnv *env, jobject teleProcess, jobject threadSequence, jlong handle, ThreadState_t state, jlong instructionPointer, ThreadSpecifics threadSpecifics);

/**
 * Copies bytes from the tele process into a given direct ByteBuffer or byte array.
 *
 * @param src the address in the tele process to copy from
 * @param dst the destination of the copy operation. This is a direct java.nio.ByteBuffer or a byte array
 *            depending on the value of 'isDirectByteBuffer'
 * @param isDirectByteBuffer
 * @param dstOffset the offset in 'dst' at which to start writing
 * @param length the number of bytes to copy
 * @return the number of bytes copied or -1 if there was an error
 */
extern int teleProcess_read(PROCESS_MEMORY_PARAMS JNIEnv *env, jclass c, jlong src, jobject dst, jboolean isDirectByteBuffer, jint dstOffset, jint length);

/**
 * Copies bytes from a given direct ByteBuffer or byte array into the tele process.
 *
 * @param dst the address in the tele process to copy to
 * @param src the source of the copy operation. This is a direct java.nio.ByteBuffer or byte array
 *            depending on the value of 'isDirectByteBuffer'
 * @param isDirectByteBuffer
 * @param srcOffset the offset in 'src' at which to start reading
 * @param length the number of bytes to copy
 * @return the number of bytes copied or -1 if there was an error
 */
extern int teleProcess_write(PROCESS_MEMORY_PARAMS JNIEnv *env, jclass c, jlong dst, jobject src, jboolean isDirectByteBuffer, jint srcOffset, jint length);

#endif /*__teleProcess_h__*/
