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

#include "virtualMemory.h"
#include "log.h"

#if defined(GUESTVMXEN)
#include <guestvmXen.h>
/* No mmap function on GuestVM (yet)*/
#else
#include <stdlib.h>
#include <errno.h>
#include <string.h>
#include <sys/mman.h>

#include "jni.h"
#include "unistd.h"

/* There seems to be a problem binding these identifiers in RedHat's include files, so we fake them: */
#if os_LINUX
#   ifndef MAP_ANONYMOUS
#       define MAP_ANONYMOUS    0x20
#   endif
#   ifndef MAP_ANON
#       define MAP_ANON         MAP_ANONYMOUS
#   endif
#   ifndef MAP_NORESERVE
#       define MAP_NORESERVE    0x04000
#   endif
#   ifndef MAP_32BIT
#       define MAP_32BIT        0x40
#   endif
#else
    /* TODO */
#   ifndef MAP_32BIT
#       define MAP_32BIT            0
#   endif
#endif

#define PROT                (PROT_EXEC | PROT_READ | PROT_WRITE)

Address virtualMemory_mapFile(Size size, jint fd, Size offset) {
    return (Address) mmap(0, (size_t) size, PROT, MAP_PRIVATE, fd, (off_t) offset);
}

JNIEXPORT jlong JNICALL
Java_com_sun_max_memory_VirtualMemory_nativeMapFile(JNIEnv *env, jclass c, jlong size, jint fd, jlong offset) {
    return (jlong) virtualMemory_mapFileIn31BitSpace((Size) size, fd, (Size) offset);
}

Address virtualMemory_mapFileIn31BitSpace(jint size, jint fd, Size offset) {
    return (Address) mmap(0, (size_t) size, PROT, MAP_PRIVATE | MAP_32BIT, fd, (off_t) offset);
}

JNIEXPORT jlong JNICALL
Java_com_sun_max_memory_VirtualMemory_nativeMapFileIn31BitSpace(JNIEnv *env, jclass c, jint size, jint fd, jlong offset) {
    return (jlong) virtualMemory_mapFileIn31BitSpace(size, fd, (Size) offset);
}

jboolean virtualMemory_mapFileAtFixedAddress(Address address, Size size, jint fd, Size offset) {
    return ((Address)mmap((void *) address, (size_t) size, PROT, MAP_PRIVATE | MAP_FIXED, fd, (off_t) offset) == address);
}

JNIEXPORT Address JNICALL
Java_com_sun_max_memory_VirtualMemory_nativeAllocate(JNIEnv *env, jclass c, Size size) {
    return (Address) mmap(0, (size_t) size, PROT, MAP_ANON | MAP_PRIVATE, -1, (off_t) 0);
}

JNIEXPORT Address JNICALL
Java_com_sun_max_memory_VirtualMemory_nativeAllocateIn31BitSpace(JNIEnv *env, jclass c, Size size) {
#if os_LINUX
    return (Address) mmap(0, (size_t) size, PROT, MAP_ANON | MAP_PRIVATE | MAP_32BIT, -1, (off_t) 0);
#else
    c_UNIMPLEMENTED();
    return 0;
#endif
}

JNIEXPORT jboolean JNICALL
Java_com_sun_max_memory_VirtualMemory_nativeAllocateAtFixedAddress(JNIEnv *env, jclass c, Address address, Size size) {
    return virtualMemory_allocateAtFixedAddress(address, size);
}

Address virtualMemory_reserve(Size size) {
	Address addr = 	(Address) mmap(0, (size_t) size, PROT, MAP_ANON | MAP_PRIVATE | MAP_NORESERVE, -1, (off_t) 0);
#if log_LOADER
	log_println(" %d virtualMemory_reserve reserved %lx at %p",sizeof(size), size, addr);
#endif
	return addr;
}

JNIEXPORT Address JNICALL
Java_com_sun_max_memory_VirtualMemory_nativeReserve(JNIEnv *env, jclass c, Size size) {
    return virtualMemory_reserve(size);
}

JNIEXPORT jboolean JNICALL
Java_com_sun_max_memory_VirtualMemory_nativeRelease(JNIEnv *env, jclass c, Address start, Size size) {
    return (munmap((void *) start, (size_t) size) == 0);
}


#endif // GUESTVMXEN

jboolean virtualMemory_allocateAtFixedAddress(Address address, Size size) {
#if os_SOLARIS || os_DARWIN
  return (jboolean) ((Address) mmap((void *) address, (size_t) size, PROT, MAP_ANON | MAP_PRIVATE | MAP_FIXED, -1, (off_t) 0) == address);
#else
    c_UNIMPLEMENTED();
    return false;
#endif
}

void protectPage(Address pageAddress) {
    c_ASSERT(pageAlign(pageAddress) == pageAddress);

#if os_SOLARIS || os_DARWIN || os_LINUX
    if (mprotect((Word) pageAddress, getPageSize(), PROT_NONE) != 0) {
         int error = errno;
         log_exit(error, "protectPage: mprotect(0x%0lx) failed: %s", pageAddress, strerror(error));
    }
#elif os_GUESTVMXEN
    guestvmXen_protectPage(pageAddress);
#else
    c_UNIMPLEMENTED();
#endif
}

void unprotectPage(Address pageAddress) {
	c_ASSERT(pageAlign(pageAddress) == pageAddress);
#if os_SOLARIS || os_DARWIN || os_LINUX
	if (mprotect((Word) pageAddress, getPageSize(), PROT_READ| PROT_WRITE) != 0){
         int error = errno;
		 log_exit(error, "unprotectPage: mprotect(0x%0lx) failed: %s", pageAddress, strerror(error));
	}
#elif os_GUESTVMXEN
	guestvmXen_unProtectPage(pageAddress);
#else
	c_UNIMPLEMENTED();
#endif
}

unsigned int getPageSize(void){
#if os_GUESTVMXEN
    return guestvmXen_pageSize();
#else
    return getpagesize();
#endif
}

/*
 * Aligns a given address up to the next page-aligned address if it is not already page-aligned.
 */
Address pageAlign(Address address){
      long alignment = getPageSize() - 1;
        return ((long)(address + alignment) & ~alignment);
}
