/*
 * Copyright (c) 2017, APT Group, School of Computer Science,
 * The University of Manchester. All rights reserved.
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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
 */
#include "virtualMemory.h"
#include "log.h"

#if defined(MAXVE)
#include <maxve.h>
/* No mmap function on MaxVE (yet)*/
#else
#include <stdlib.h>
#include <errno.h>
#include <string.h>
#include <sys/mman.h>

#include "jni.h"
#include "unistd.h"

#if os_DARWIN
#include <sys/types.h>
#include <sys/sysctl.h>
#endif

/* There is a problem binding these identifiers in RedHat's include files, so we fake them: */
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

/* mmap returns MAP_FAILED on error, we convert to ALLOC_FAILED */
static Address check_mmap_result(void *result) {
#if log_MMAP
	if(result == MAP_FAILED) {
		switch(errno) {
			case EACCES:
			    log_println("EACCES\n");
			break;
			case EAGAIN:
			    log_println("EAGAIN\n");
			break;
			case EBADF:
			    log_println("EBADF\n");
			break;
			case EINVAL:
			    log_println("EINVAL\n");
			break;
			case ENFILE:
			    log_println("ENFILE\n");
			break;
			case ENODEV:
			    log_println("ENODEV\n");
			break;
			case ENOMEM:
			    log_println("ENOMEM\n");
			break;
			case EPERM:
			    log_println("EPERM\n");
			break;
			case ETXTBSY:
			    log_println("ETXTBSY\n");
			break;
			default:
			    log_println("UNKNOWN\n");
			break;
		}
	}
#endif
    return ((Address) (result == (void *) MAP_FAILED ? ALLOC_FAILED : result));
}

#ifdef arm
  static int attempt = 0;
  static Address allocAddress = 0x0;
#endif

/* Generic virtual space allocator.
 * If the address parameters is specified, allocate at the specified address and fail if it cannot be allocated.
 * Use MAP_NORESERVE if reserveSwap is false
 * Use PROT_NONE if protNone is true, otherwise set all protection (i.e., allow any type of access).
 */
Address virtualMemory_allocatePrivateAnon(Address address, Size size, jboolean reserveSwap, jboolean protNone, int type) {
#ifdef arm
    //We have to make sure that in ARM 32 bit archs we always allocate in positive memory addresses.
    if(attempt == 0) {
        attempt++;
        if(address == 0x0) {
            address = 0x10000000;
            allocAddress = address;
        }
    } else {
        address = allocAddress;
    }
#endif
  int flags = MAP_PRIVATE | MAP_ANON;
#if os_LINUX
  /* For some reason, subsequent calls to mmap to allocate out of the space
   * reserved here only work if the reserved space is in 32-bit space. */
#endif
  int prot = protNone == JNI_TRUE ? PROT_NONE : PROT;
  if (reserveSwap == JNI_FALSE) {
     flags |= MAP_NORESERVE;
  }
  if (address != 0) {
	  flags |= MAP_FIXED;
  }
  void * result = mmap((void*) address, (size_t) size, prot, flags, -1, 0);

#if log_LOADER
	log_println("virtualMemory_allocatePrivateAnon(address=%p, size=%p, swap=%s, prot=%s) allocated at %p",
					address, size,
					reserveSwap==JNI_TRUE ? "true" : "false",
					protNone==JNI_TRUE ? "none" : "all",
					result);
#endif
#ifdef arm
  address =  check_mmap_result(result);
  allocAddress = address + size;
  return address;
#else
  return check_mmap_result(result);
#endif
}


Address virtualMemory_mapFile(Size size, jint fd, Size offset) {
#ifdef arm
    Address address = 0x0;
    if(attempt == 0) {
        attempt++;
        if(address == 0x0) {
            address = 0x10000000;
            allocAddress = address;
        }
    } else {
        address = allocAddress;
    }
    void *result = mmap((void *)address, (size_t) size, PROT, MAP_PRIVATE, fd, (off_t) offset);
    address = check_mmap_result(result);
    allocAddress = address + size;
    return address;
#else
	return check_mmap_result(mmap(0, (size_t) size, PROT, MAP_PRIVATE, fd, (off_t) offset));
#endif
}

JNIEXPORT jlong JNICALL
Java_com_sun_max_memory_VirtualMemory_virtualMemory_1mapFile(JNIEnv *env, jclass c, jlong size, jint fd, jlong offset) {
    return virtualMemory_mapFile((Size) size, fd, (Size) offset);
}

Address virtualMemory_mapFileIn31BitSpace(jint size, jint fd, Size offset) {
	return check_mmap_result(mmap(0, (size_t) size, PROT, MAP_PRIVATE | MAP_32BIT, fd, (off_t) offset));
}

JNIEXPORT jlong JNICALL
Java_com_sun_max_memory_VirtualMemory_virtualMemory_1mapFileIn31BitSpace(JNIEnv *env, jclass c, jint size, jint fd, jlong offset) {
    return virtualMemory_mapFileIn31BitSpace(size, fd, (Size) offset);
}

Address virtualMemory_mapFileAtFixedAddress(Address address, Size size, jint fd, Size offset) {
    return check_mmap_result(mmap((void *) address, (size_t) size, PROT, MAP_PRIVATE | MAP_FIXED, fd, (off_t) offset));
}

// end of conditional exclusion of mmap stuff not available (or used) on MAXVE
#endif // MAXVE


Address virtualMemory_allocate(Size size, int type) {
#if os_MAXVE
	return (Address) maxve_virtualMemory_allocate(size, type);
#else
    return check_mmap_result(mmap(0, (size_t) size, PROT, MAP_ANON | MAP_PRIVATE, -1, (off_t) 0));
#endif
}

Address virtualMemory_allocateIn31BitSpace(Size size, int type) {
#if os_LINUX
    return check_mmap_result(mmap(0, (size_t) size, PROT, MAP_ANON | MAP_PRIVATE | MAP_32BIT, -1, (off_t) 0));
#elif os_MAXVE
    return (Address) maxve_virtualMemory_allocateIn31BitSpace(size, type);
#else
    c_UNIMPLEMENTED();
    return 0;
#endif
}

Address virtualMemory_deallocate(Address start, Size size, int type) {
#if os_MAXVE
    return (Address) maxve_virtualMemory_deallocate((void *)start, size, type);
#else
    int result = munmap((void *) start, (size_t) size);
    return result == -1 ? 0 : start;
#endif
}

boolean virtualMemory_allocateAtFixedAddress(Address address, Size size, int type) {
#if os_SOLARIS || os_DARWIN  || os_LINUX
    return check_mmap_result(mmap((void *) address, (size_t) size, PROT, MAP_ANON | MAP_PRIVATE | MAP_FIXED, -1, (off_t) 0)) != ALLOC_FAILED;
#elif os_MAXVE
    return (Address) maxve_virtualMemory_allocateAtFixedAddress((unsigned long)address, size, type) != ALLOC_FAILED;
#else
    c_UNIMPLEMENTED();
    return false;
#endif
}

void virtualMemory_protectPages(Address address, int count) {
/* log_println("---   protected %p .. %p", address, address + (count * virtualMemory_getPageSize())); */
    c_ASSERT(virtualMemory_pageAlign(address) == address);
#if os_SOLARIS || os_DARWIN || os_LINUX
    if (mprotect((void *) address, count * virtualMemory_getPageSize(), PROT_NONE) != 0) {
         int error = errno;
         log_exit(error, "protectPages: mprotect(%p) failed: %s", address, strerror(error));
    }
#elif os_MAXVE
    maxve_virtualMemory_protectPages(address, count);
#else
    c_UNIMPLEMENTED();
#endif
}

void virtualMemory_unprotectPages(Address address, int count) {
/* log_println("--- unprotected %p .. %p", address, address + (count * virtualMemory_getPageSize())); */
	c_ASSERT(virtualMemory_pageAlign(address) == address);
#if os_SOLARIS || os_DARWIN || os_LINUX
	if (mprotect((void *) address, count * virtualMemory_getPageSize(), PROT_READ| PROT_WRITE) != 0) {
         int error = errno;
	     log_exit(error, "unprotectPages: mprotect(%p) failed: %s", address, strerror(error));
	}
#elif os_MAXVE
	maxve_virtualMemory_unProtectPages(address, count);
#else
	c_UNIMPLEMENTED();
#endif
}

static unsigned int pageSize = 0;
static Size physicalMemory = 0;

unsigned int virtualMemory_getPageSize(void) {
#if os_MAXVE
    return maxve_virtualMemory_pageSize();
#else
    if (pageSize == 0) {
        pageSize = getpagesize();
    }
    return pageSize;
#endif
}

Size virtualMemory_getPhysicalMemorySize(void) {
    if (physicalMemory == 0) {
#if os_MAXVE
        // TODO
        return 0;
#elif os_SOLARIS  || os_LINUX
        Size numPhysicalPages = (Size) sysconf(_SC_PHYS_PAGES);
        physicalMemory = numPhysicalPages * virtualMemory_getPageSize();
#elif os_DARWIN
        int query[2];
        query[0] = CTL_HW;
        query[1] = HW_MEMSIZE;
        size_t len = sizeof(physicalMemory);
        int ok = sysctl(query, 2, &physicalMemory, &len, NULL, 0);
        c_ASSERT(ok == 0);
#endif
        c_ASSERT(physicalMemory > 0 && physicalMemory % virtualMemory_getPageSize() == 0);
    }
    return physicalMemory;
}

/*
 * Aligns a given address up to the next page-aligned address if it is not already page-aligned.
 */
Address virtualMemory_pageAlign(Address address) {
    long alignment = virtualMemory_getPageSize() - 1;
    return ((long)(address + alignment) & ~alignment);
}
