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
#ifndef __teleProcess_h__
#define __teleProcess_h__ 1

#include "teleNativeThread.h"

extern void teleProcess_initialize(void);

/*
 * This enum must be kept in sync with com.sun.max.tele.debug.ProcessState.java.
 */
typedef enum ProcessState {
    PS_UNKNOWN,
    PS_STOPPED,
    PS_RUNNING,
    PS_TERMINATED
} ProcessState;

/*
 * Definition of the platform specific type 'ProcessHandle' and the two operations
 * 'readProcessMemory' & 'writeProcessMemory' that use the handle to access the
 * memory of the process.
 */
#if os_LINUX
#include <sys/types.h>
size_t task_read(pid_t tgid, pid_t tid, const void *src, void *dst, size_t size);
size_t task_write(pid_t tgid, pid_t tid, void *dst, const void *src, size_t size);
typedef struct {
    pid_t tgid;
    pid_t tid;
} ProcessHandleStruct, *ProcessHandle;
#define readProcessMemory(ph, src, dst, size) task_read(ph->tgid, ph->tid, (const void *) src, (void *) dst, (size_t) size)
#define writeProcessMemory(ph, dst, src, size) task_write(ph->tgid, ph->tid, (void *) dst, (const void *) src, (size_t) size)
#elif os_DARWIN
#include <mach/mach.h>
int task_read(task_t task, vm_address_t src, void *dst, size_t size);
int task_write(task_t task, vm_address_t dst, void *src, size_t size);
typedef task_t ProcessHandle;
#define readProcessMemory(ph, src, dst, size) task_read(ph, (vm_address_t) src, (void *) dst, (size_t) size)
#define writeProcessMemory(ph, dst, src, size) task_write(ph, (vm_address_t) dst, (void *) src, (size_t) size)
#elif os_SOLARIS
#include "proc.h"
typedef struct ps_prochandle *ProcessHandle;
#define readProcessMemory(ph, src, dst, size) Pread(ph, (void *) dst, (size_t) size, (uintptr_t) src)
#define writeProcessMemory(ph, dst, src, size) Pwrite(ph, src, length, (uintptr_t) dst);
#elif os_MAXVE
// N.B The tele library may execute on either a 32 bit or a 64 bit host depending on the particular Xen/Linux configration.
// In particular, Oracle VM 2.x has a 32 bit dom0.
// We (ab)use the ProcessHandle argument to distinguish the two different native implementations (xg,db) of tele
#include <stdint.h>
typedef int (*MaxVEMemoryHandler)(uint64_t, char *, unsigned short);
struct maxve_memory_handler {
    MaxVEMemoryHandler readbytes;
    MaxVEMemoryHandler writebytes;
};
typedef struct maxve_memory_handler *ProcessHandle;
#define readProcessMemory(ph, src, dst, size) ph->readbytes((uint64_t) src, (char *) dst, (unsigned short) size)
#define writeProcessMemory(ph, dst, src, size) ph->writebytes((uint64_t) dst, (char *) src, (unsigned short) size);
#else
#error
#endif

/**
 * Searches the thread locals list in the VM's address space for an entry 'tla' such that:
 *
 *   tla.stackBase <= stackPointer && stackPointer < (tla.stackBase + tla.stackSize)
 *
 * If such an entry is found, then its contents are copied from the VM to the structs pointed to by 'tlCopy' and 'ntlCopy'.
 *
 * @param ph a platform specific process handle
 * @param tlaList the head of the thread locals list in the VM's address space
 * @param primordialETLA the primordial TLA in the VM's address space
 * @param stackPointer the stack pointer to search with
 * @param tlaCopy pointer to a TLA which the found entry (if any) will be copied from the VM's address space
 * @param ntlCopy pointer to storage for a NativeThreadLocalsStruct into which the native thread locals of the found entry
 *        (if any) will be copied from the VM's address space
 * @return the entry that was found, NULL otherwise
 */
extern TLA teleProcess_findTLA(ProcessHandle ph, Address tlaList, Address primordialETLA, Address stackPointer, TLA tlaCopy, NativeThreadLocals ntlCopy);

/**
 * Makes the upcall to TeleProcess.jniGatherThread
 *
 * @param teleProcess the TeleProcess object gathering the threads
 * @param threads a List<TeleNativeThread> object used to gather the threads
 * @param localHandle the debug handle to a thread (which may differ from the native thread handle in the VM's address space)
 * @param state the execution state of the thread
 * @param instructionPointer
 * @param tla the TLA found based on the stack pointer of the thread or NULL if no such thread locals were found
 */
extern void teleProcess_jniGatherThread(JNIEnv *env, jobject teleProcess, jobject threadList, jlong localHandle, ThreadState_t state, jlong instructionPointer, TLA tla);

/**
 * Copies bytes from the tele process into a given direct ByteBuffer or byte array.
 *
 * @param ph a platform specific process handle
 * @param src the address in the tele process to copy from
 * @param dst the destination of the copy operation. This is a direct java.nio.ByteBuffer or a byte array
 *            depending on the value of 'isDirectByteBuffer'
 * @param isDirectByteBuffer
 * @param dstOffset the offset in 'dst' at which to start writing
 * @param length the number of bytes to copy
 * @return the number of bytes copied or -1 if there was an error
 */
extern int teleProcess_read(ProcessHandle ph, JNIEnv *env, jclass c, jlong src, jobject dst, jboolean isDirectByteBuffer, jint dstOffset, jint length);

/**
 * Copies bytes from a given direct ByteBuffer or byte array into the tele process.
 *
 * @param ph a platform specific process handle
 * @param dst the address in the tele process to copy to
 * @param src the source of the copy operation. This is a direct java.nio.ByteBuffer or byte array
 *            depending on the value of 'isDirectByteBuffer'
 * @param isDirectByteBuffer
 * @param srcOffset the offset in 'src' at which to start reading
 * @param length the number of bytes to copy
 * @return the number of bytes copied or -1 if there was an error
 */
extern int teleProcess_write(ProcessHandle ph, JNIEnv *env, jclass c, jlong dst, jobject src, jboolean isDirectByteBuffer, jint srcOffset, jint length);

#endif /*__teleProcess_h__*/
