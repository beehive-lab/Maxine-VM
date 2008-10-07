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
/*VCSID=263564a0-f456-4738-b0f6-fedafdcf03fe*/
/*
 * darwinDataAccess.c
 *
 *  Created on: May 2, 2008
 *      Author: Ben L. Titzer
 */

#include "debug.h"
#include "jni.h"

#include <stdlib.h>

#include <sys/types.h>
#include <mach/mach_types.h>
#include <mach/mach_vm.h>
#include <mach/mach_init.h>

#include <unistd.h>

long page_size;

// read a potentially unaligned address in a remote process by
// reading the surrounding pages and return a pointer to a local copy
void *vm_read_unaligned(task_t task, mach_vm_address_t address, int size) {
  mach_msg_type_number_t count;
  vm_offset_t buffer;
  if (page_size == 0) page_size = getpagesize();
  int offset = (int)(address & (page_size - 1));
  int readsize = page_size;
  if (offset + size > page_size) readsize += page_size;

  /* read the entire page */
  if (mach_vm_read(task, address - offset, readsize, &buffer, &count) != KERN_SUCCESS) {
	return NULL;
  }
  return ((void *)buffer) + offset;
}

// free a buffer that has been allocated by a previous vm_read_aligned() call
void vm_free_unaligned(void *buffer, int size) {
  mach_vm_address_t address = (mach_vm_address_t) buffer;
  if (page_size == 0) page_size = getpagesize();
  int offset = (int)(address & (page_size - 1));
  int readsize = page_size;
  if (offset + size > page_size) readsize += page_size;
  if (mach_vm_deallocate(mach_task_self(), address - offset, readsize) != KERN_SUCCESS) {
    	debug_println("vm_deallocate failed");
  }
}

// write a potentially unaligned address in a remote process
// the page offset of the local address and the remote address must match.
void *vm_write_unaligned(task_t task, mach_vm_address_t address, void *buffer, int size) {
  if (page_size == 0) page_size = getpagesize();
  int offset = (int)(address & (page_size - 1));
  int readsize = page_size;
  if (offset + size > page_size) readsize += page_size;
  if (mach_vm_write(task, address - offset, (vm_offset_t) (buffer - offset), readsize) != KERN_SUCCESS) {
	return NULL;
  }
  return buffer;
}

JNIEXPORT jint JNICALL
Java_com_sun_max_tele_debug_darwin_DarwinDataAccess_nativeReadByte(JNIEnv *env, jclass c, jlong task, jlong address) {
  jbyte value = 0;
  mach_vm_size_t count;
  kern_return_t result = mach_vm_read_overwrite(task, (vm_address_t) address, 1, (vm_address_t) &value, &count);
  if (result == KERN_SUCCESS && count == 1) {
    return value;
  }
  return (jint) -1;
}

JNIEXPORT jint JNICALL
Java_com_sun_max_tele_debug_darwin_DarwinDataAccess_nativeReadShort(JNIEnv *env, jclass c, jlong task, jlong address) {
  jshort value = 0;
  mach_vm_size_t count;
  kern_return_t result = mach_vm_read_overwrite(task, (vm_address_t) address, 2, (vm_address_t) &value, &count);
  if (result == KERN_SUCCESS && count == 2) {
    return value;
  }
  return (jint) -1;
}

JNIEXPORT jlong JNICALL
Java_com_sun_max_tele_debug_darwin_DarwinDataAccess_nativeReadInt(JNIEnv *env, jclass c, jlong task, jlong address) {
  jint value = 0;
  mach_vm_size_t count;
  kern_return_t result = mach_vm_read_overwrite(task, (vm_address_t) address, 4, (vm_address_t) &value, &count);
  if (result == KERN_SUCCESS && count == 4) {
    return value;
  }
  return (jlong) -1;
}

JNIEXPORT jint JNICALL
Java_com_sun_max_tele_debug_darwin_DarwinDataAccess_nativeReadBytes(JNIEnv *env, jclass c, jlong task, jlong address, jbyteArray byteArray, jint offset, jint length) {
  mach_vm_size_t bytesRead;
  
  jbyte* buffer = (jbyte *) malloc(length * sizeof(jbyte));
  if (buffer == 0) {
      debug_println("failed to malloc byteArray of %d bytes", length);
      return -1;
  }
  
  kern_return_t result = mach_vm_read_overwrite(task, (vm_address_t) address, length, (vm_address_t) buffer, &bytesRead);
  if (bytesRead > 0) {
      (*env)->SetByteArrayRegion(env, byteArray, offset, bytesRead, buffer);
  }
  free(buffer);

  return result == KERN_SUCCESS ? bytesRead : -1;
}


JNIEXPORT jboolean JNICALL
Java_com_sun_max_tele_debug_darwin_DarwinDataAccess_nativeWriteByte(JNIEnv *env, jclass c, jlong task, jlong address, jbyte value) {
  return mach_vm_write(task, (vm_address_t) address, (vm_offset_t) &value, 1) == KERN_SUCCESS;
}

JNIEXPORT jboolean JNICALL
Java_com_sun_max_tele_debug_darwin_DarwinDataAccess_nativeWriteShort(JNIEnv *env, jclass c, jlong task, jlong address, jshort value) {
  return mach_vm_write(task, (vm_address_t) address, (vm_offset_t) &value, 2) == KERN_SUCCESS;
}

JNIEXPORT jboolean JNICALL
Java_com_sun_max_tele_debug_darwin_DarwinDataAccess_nativeWriteInt(JNIEnv *env, jclass c, jlong task, jlong address, jint value) {
  return mach_vm_write(task, (vm_address_t) address, (vm_offset_t) &value, 4) == KERN_SUCCESS;
}

JNIEXPORT jboolean JNICALL
Java_com_sun_max_tele_debug_darwin_DarwinDataAccess_nativeWriteLong(JNIEnv *env, jclass c, jlong task, jlong address, jlong value) {
  return mach_vm_write(task, (vm_address_t) address, (vm_offset_t) &value, 8) == KERN_SUCCESS;
}

JNIEXPORT jint JNICALL
Java_com_sun_max_tele_debug_darwin_DarwinDataAccess_nativeWriteBytes(JNIEnv *env, jclass c, jlong task, jlong address, jbyteArray byteArray, jint offset, jint length) {
    jbyte* buffer = (jbyte *) malloc(length * sizeof(jbyte));
    if (buffer == 0) {
        debug_println("failed to malloc byteArray of %d bytes", length);
        return -1;
    }

    (*env)->GetByteArrayRegion(env, byteArray, offset, length, buffer);
    if ((*env)->ExceptionOccurred(env) != NULL) {
        debug_println("failed to copy %d bytes from byteArray into buffer", length);
        return -1;
    }

    
    kern_return_t result = mach_vm_write(task, (vm_address_t) address, (vm_offset_t) buffer, length);
    free(buffer);
    return result == KERN_SUCCESS ? length : -1;
}
