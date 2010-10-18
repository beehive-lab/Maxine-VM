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
    return ((Address) (result == (void *) MAP_FAILED ? ALLOC_FAILED : result));
}

/* Generic virtual space allocator.
 * If the address parameters is specified, allocate at the specified address and fail if it cannot be allocated.
 * Use MAP_NORESERVE if reserveSwap is true
 * Use PROT_NONE if protNone is true, otherwise set all protection (i.e., allow any type of access).
 */
Address virtualMemory_allocatePrivateAnon(Address address, Size size, jboolean reserveSwap, jboolean protNone, int type) {
  int flags = MAP_PRIVATE | MAP_ANON;
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
  return check_mmap_result(result);
}


Address virtualMemory_mapFile(Size size, jint fd, Size offset) {
	return check_mmap_result(mmap(0, (size_t) size, PROT, MAP_PRIVATE, fd, (off_t) offset));
 }

JNIEXPORT Address JNICALL
Java_com_sun_max_memory_VirtualMemory_mapFile(JNIEnv *env, jclass c, jlong size, jint fd, jlong offset) {
    return virtualMemory_mapFile((Size) size, fd, (Size) offset);
}

Address virtualMemory_mapFileIn31BitSpace(jint size, jint fd, Size offset) {
	return check_mmap_result(mmap(0, (size_t) size, PROT, MAP_PRIVATE | MAP_32BIT, fd, (off_t) offset));
}

JNIEXPORT Address JNICALL
Java_com_sun_max_memory_VirtualMemory_mapFileIn31BitSpace(JNIEnv *env, jclass c, jint size, jint fd, jlong offset) {
    return virtualMemory_mapFileIn31BitSpace(size, fd, (Size) offset);
}

Address virtualMemory_mapFileAtFixedAddress(Address address, Size size, jint fd, Size offset) {
    return check_mmap_result(mmap((void *) address, (size_t) size, PROT, MAP_PRIVATE | MAP_FIXED, fd, (off_t) offset));
}

// end of conditional exclusion of mmap stuff not available (or used) on GUESTVMXEN
#endif // GUESTVMXEN


Address virtualMemory_allocate(Size size, int type) {
#if os_GUESTVMXEN
	return (Address) guestvmXen_virtualMemory_allocate(size, type);
#else
    return check_mmap_result(mmap(0, (size_t) size, PROT, MAP_ANON | MAP_PRIVATE, -1, (off_t) 0));
#endif
}

Address virtualMemory_allocateIn31BitSpace(Size size, int type) {
#if os_LINUX
    return check_mmap_result(mmap(0, (size_t) size, PROT, MAP_ANON | MAP_PRIVATE | MAP_32BIT, -1, (off_t) 0));
#elif os_GUESTVMXEN
    return (Address) guestvmXen_virtualMemory_allocateIn31BitSpace(size, type);
#else
    c_UNIMPLEMENTED();
    return 0;
#endif
}

Address virtualMemory_deallocate(Address start, Size size, int type) {
#if os_GUESTVMXEN
    return (Address) guestvmXen_virtualMemory_deallocate((void *)start, size, type);
#else
    int result = munmap((void *) start, (size_t) size);
    return result == -1 ? 0 : start;
#endif
}

boolean virtualMemory_allocateAtFixedAddress(Address address, Size size, int type) {
#if os_SOLARIS || os_DARWIN || os_LINUX
    return check_mmap_result(mmap((void *) address, (size_t) size, PROT, MAP_ANON | MAP_PRIVATE | MAP_FIXED, -1, (off_t) 0)) != ALLOC_FAILED;
#elif os_GUESTVMXEN
    return (Address) guestvmXen_virtualMemory_allocateAtFixedAddress((unsigned long)address, size, type) != ALLOC_FAILED;
#else
    c_UNIMPLEMENTED();
    return false;
#endif
}

void virtualMemory_protectPages(Address address, int count) {
    c_ASSERT(virtualMemory_pageAlign(address) == address);

#if os_SOLARIS || os_DARWIN || os_LINUX
    if (mprotect((void *) address, count * virtualMemory_getPageSize(), PROT_NONE) != 0) {
         int error = errno;
         log_exit(error, "protectPages: mprotect(%p) failed: %s", address, strerror(error));
    }
#elif os_GUESTVMXEN
    guestvmXen_virtualMemory_protectPages(address, count);
#else
    c_UNIMPLEMENTED();
#endif
}

void virtualMemory_unprotectPages(Address address, int count) {
	c_ASSERT(virtualMemory_pageAlign(address) == address);
#if os_SOLARIS || os_DARWIN || os_LINUX
	if (mprotect((void *) address, count * virtualMemory_getPageSize(), PROT_READ| PROT_WRITE) != 0) {
         int error = errno;
	     log_exit(error, "unprotectPages: mprotect(%p) failed: %s", address, strerror(error));
	}
#elif os_GUESTVMXEN
	guestvmXen_virtualMemory_unProtectPages(address, count);
#else
	c_UNIMPLEMENTED();
#endif
}

static unsigned int pageSize = 0;

unsigned int virtualMemory_getPageSize(void) {
#if os_GUESTVMXEN
    return guestvmXen_virtualMemory_pageSize();
#else
    if (pageSize == 0) {
        pageSize = getpagesize();
    }
    return pageSize;
#endif
}

/*
 * Aligns a given address up to the next page-aligned address if it is not already page-aligned.
 */
Address virtualMemory_pageAlign(Address address) {
    long alignment = virtualMemory_getPageSize() - 1;
    return ((long)(address + alignment) & ~alignment);
}
