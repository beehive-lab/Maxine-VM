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
	#if !os_WINDOWS
		#include <sys/mman.h>
	#else
		#ifdef _WIN32_WINNT
		#undef _WIN32_WINNT 
		#endif
		#define _WIN32_WINNT 0x0601 //needed for tools like MINGW which declare an earlier version of Windows making some features of win32 api unavailable. Visual Studio might not need this.
		#include <windows.h>
	#endif
#include "jni.h"
#include "unistd.h"

#if os_DARWIN
#include <sys/types.h>
#include <sys/sysctl.h>
#endif

/* There is a problem binding these identifiers in RedHat's include files, so we fake them: */
#if os_LINUX || os_WINDOWS
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
#if !os_WINDOWS
#define PROT                (PROT_EXEC | PROT_READ | PROT_WRITE)
#endif

#if !os_WINDOWS //we do not use mmap on Windows so this function is not needed.

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
#endif


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
#if os_WINDOWS
	//Windows got no Swap space, so  jboolean reserveSwap is redundant
	Address result;
	if(protNone == JNI_TRUE){ 
	
		 result = (Address) VirtualAlloc( (void *) address, size,     MEM_RESERVE,   PAGE_NOACCESS| PAGE_WRITECOPY);
		
		//virtualalloc is the only win32 function that supports the PROT_NONE equivalent PAGE_NOACCESS
		//PAGE_WRITECOPY is equivalent to MAP_PRIVATE
		if(!result)
			log_println("%d\n", GetLastError());
	}
	else {
		//if protnone is not used, we can use CreateFileMappingA + MapViewOfFile combination to emulate mmap() on Windows
		//INVALID_HANDLE_VALUE means that we dont use an actual file but the system pagefile, similar to fd= -1 & MPI_ANON in mmap()
		 HANDLE fmapping = CreateFileMappingA( INVALID_HANDLE_VALUE  , NULL , PAGE_EXECUTE_READWRITE  | SEC_RESERVE,0u ,size,   NULL);
	//	FILE_MAP_COPY is equivalent to MAP_PRIVATE
if(!fmapping)
					log_println("%d\n", GetLastError());
		result = (Address) MapViewOfFileEx (fmapping,   FILE_MAP_ALL_ACCESS | FILE_MAP_COPY,   0, 0,   size, (LPVOID)address);
			// result = (Address) VirtualAlloc( (void *) address, size,   MEM_RESERVE,    PAGE_READWRITE);

	if(!result)
					log_println("%d %d\n", GetLastError(), size);
			

	}
	return result;

#else
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
#elif os_WINDOWS
	HANDLE fmapping = CreateFileMappingA( (HANDLE)_get_osfhandle(fd)  , NULL , PAGE_READWRITE  | SEC_COMMIT,0u ,size,   NULL);
	//_get_osfhandle returns a Windows HANDLE for the file descriptor fd, needed by CreateFileMappingA
	Address result = (Address) MapViewOfFile (fmapping,   FILE_MAP_READ | FILE_MAP_WRITE| FILE_MAP_COPY,   (DWORD)(offset >> 32), (DWORD) offset,   size);
	//FILE_MAP_COPY is equivalent to mmap's MAP_PRIVATE. It maps a copy-on-write view of the file that is private to the process.
	//MapViewOfFile needs lower and high order of offset (last and first 32 bits).	We get high and lower orders of the offset (which might be 64bit long) by doing casts to DWORD( 32 BITS) and Binary shifts
	if(!result)
		log_println("%d\n", GetLastError());
	return result;
#else
	return check_mmap_result(mmap(0, (size_t) size, PROT, MAP_PRIVATE, fd, (off_t) offset));
#endif
}

JNIEXPORT jlong JNICALL
Java_com_sun_max_memory_VirtualMemory_virtualMemory_1mapFile(JNIEnv *env, jclass c, jlong size, jint fd, jlong offset) {
    return virtualMemory_mapFile((Size) size, fd, (Size) offset);
}

Address virtualMemory_mapFileIn31BitSpace(jint size, jint fd, Size offset) {
	#if os_WINDOWS //MAP_32BIT is not supported on Windows.... Also in Linux, it is no longer really needed 
	/*"It was added to allow thread stacks to be
              allocated somewhere in the first 2 GB of memory, so as to
              improve context-switch performance on some early 64-bit
              processors.  Modern x86-64 processors no longer have this
              performance problem, so use of this flag is not required on
              those systems.
			  
	https://man7.org/linux/man-pages/man2/mmap.2.html		  
			  */
		return virtualMemory_mapFile(size, fd, offset);
	#else
	return check_mmap_result(mmap(0, (size_t) size, PROT, MAP_PRIVATE | MAP_32BIT, fd, (off_t) offset));
	#endif
}

JNIEXPORT jlong JNICALL
Java_com_sun_max_memory_VirtualMemory_virtualMemory_1mapFileIn31BitSpace(JNIEnv *env, jclass c, jint size, jint fd, jlong offset) {
    return virtualMemory_mapFileIn31BitSpace(size, fd, (Size) offset);
}

Address virtualMemory_mapFileAtFixedAddress(Address address, Size size, jint fd, Size offset) {
	#if os_WINDOWS
	HANDLE fmapping = CreateFileMappingA( (HANDLE)_get_osfhandle(fd)  , NULL , PAGE_READWRITE  | SEC_COMMIT,0u ,0,   NULL);
	//_get_osfhandle returns a Windows HANDLE for the file descriptor fd, needed by CreateFileMappingA
	if(!fmapping)
					log_println("%d\n", GetLastError());
	Address result = (Address) MapViewOfFileEx (fmapping,   FILE_MAP_ALL_ACCESS| FILE_MAP_COPY,   (DWORD)(offset >> 32), (DWORD) offset,   size,(LPVOID) address);
	//the only diffrence is that we use MapViewOfFileEx instead MapViewOfFile. The first one allows us to provide an initial base address where the mapping begins (last argument)
	
	if(!result)
		log_println("%d\n", GetLastError());
	return result;
	#else
    return check_mmap_result(mmap((void *) address, (size_t) size, PROT, MAP_PRIVATE | MAP_FIXED, fd, (off_t) offset));
	#endif
}

// end of conditional exclusion of mmap stuff not available (or used) on MAXVE
#endif // MAXVE


Address virtualMemory_allocate(Size size, int type) {
#if os_MAXVE
	return (Address) maxve_virtualMemory_allocate(size, type);
#elif os_WINDOWS
		HANDLE fmapping = CreateFileMappingA( INVALID_HANDLE_VALUE  , NULL , PAGE_READWRITE  | SEC_COMMIT,0u ,size,   NULL);
		Address result = (Address) MapViewOfFile (fmapping,   FILE_MAP_READ | FILE_MAP_WRITE,   0, 0,   size);
		if(!result)
					log_println("%d\n", GetLastError());
		return result;	
#else
    return check_mmap_result(mmap(0, (size_t) size, PROT, MAP_ANON | MAP_PRIVATE, -1, (off_t) 0));
#endif
}

Address virtualMemory_allocateIn31BitSpace(Size size, int type) {
#if os_LINUX 
    return check_mmap_result(mmap(0, (size_t) size, PROT, MAP_ANON | MAP_PRIVATE | MAP_32BIT, -1, (off_t) 0));
	
#elif os_WINDOWS
	return virtualMemory_allocate(size, type); //windows do not have equivalent of MAP_32BIT, also obsolete in Linux
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
#elif os_WINDOWS
	if( UnmapViewOfFile((LPVOID) start) == 0){ // VirtualFree will fail for addresses mapped with MapViewOfFile so we use UnmapViewOfFile first
		int result = VirtualFree((LPVOID)start, size, type); //if UnmapViewOfFile failed, we try virtualalloc (the memory might got mapped with VirtualAlloc)
	//type can be MEM_RELEASE or whatever the user provides. (It was implemented for MAXVE that way)
		if (!result)
			return result;
		else
			return start;
	}
	else 
		return start;
#else
    int result = munmap((void *) start, (size_t) size);
    return result == -1 ? 0 : start;
#endif
}

boolean virtualMemory_allocateAtFixedAddress(Address address, Size size, int type) {
#if os_SOLARIS || os_DARWIN  || os_LINUX
    return check_mmap_result(mmap((void *) address, (size_t) size, PROT, MAP_ANON | MAP_PRIVATE | MAP_FIXED, -1, (off_t) 0)) != ALLOC_FAILED;
#elif os_WINDOWS
	HANDLE fmapping = CreateFileMappingA( INVALID_HANDLE_VALUE  , NULL , PAGE_READWRITE  | SEC_COMMIT,0u ,size,   NULL);
	//_get_osfhandle returns a Windows HANDLE for the file descriptor fd, needed by CreateFileMappingA
	Address result = (Address) MapViewOfFileEx (fmapping,   FILE_MAP_READ | FILE_MAP_WRITE|  FILE_MAP_COPY,   0, 0,   size,(LPVOID) address);
	if(!result)
		log_println("%d\n", GetLastError());
	return result;
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
#elif os_WINDOWS
	DWORD old; //needed for VirtualProtect
	int error = 0 ;
	if(!VirtualProtect((LPVOID) address,count * virtualMemory_getPageSize(), PAGE_NOACCESS, &old)) //PAGE_NOACCESS (WINAPI)  = PROT_NONE (UNIX)
	    log_exit(error, "protectPages: VirtualProtect(%p) failed %d", address, GetLastError());
	

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
#elif os_WINDOWS
	DWORD old; //needed for VirtualProtect
	VirtualProtect((LPVOID) address,count * virtualMemory_getPageSize(), PAGE_READWRITE, &old); //PAGE_NOACCESS (WINAPI)  = PROT_NONE (UNIX)
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
		#if os_WINDOWS
		SYSTEM_INFO systemInfo = {0};
		GetSystemInfo(&systemInfo);
		pageSize = systemInfo.dwAllocationGranularity ; 
		#else
        pageSize = getpagesize();
	#endif
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
#elif os_WINDOWS
	 GetPhysicallyInstalledSystemMemory(&physicalMemory);
	 return physicalMemory * 1024; //we want bytes
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
	#if os_WINDOWS //aparrently windows do not care about page alignment but rather memory allocation granularity
		SYSTEM_INFO systemInfo = {0};
		GetSystemInfo(&systemInfo);
		long alignment = systemInfo.dwAllocationGranularity - 1 ; 
	#else
    long alignment = virtualMemory_getPageSize() - 1;
	#endif
    return ((long)(address + alignment) & ~alignment);
}
