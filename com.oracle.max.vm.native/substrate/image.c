/*
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
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
#include "os.h"

#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <unistd.h>
#include <stdio.h>
#include <stdlib.h>

#include "relocation.h"
#include "word.h"
#include "virtualMemory.h"
#include "threadLocals.h"

#include "image.h"
#include "log.h"
#include "word.h"

/* TODO: make this cpu-dependent: */
#define MIN_CACHE_ALIGNMENT 8

#define IMAGE_IDENTIFICATION             0xcafe4dad
/*
 * Image format version checked against com.sun.max.vm.hosted.BootImage.BOOT_IMAGE_FORMAT_VERSION
 */
#define IMAGE_FORMAT_VERSION                    2
#define DEFAULT_RELOCATION_SCHEME        0

#if os_MAXVE
#define MEMORY_IMAGE 1
#include <maxve.h>
#include <string.h>
#else
#define MEMORY_IMAGE 0
#endif

#if MEMORY_IMAGE
extern void *maxvm_image_start;
extern void *maxvm_image_end;
#endif

/*
 * The data loaded/initialized from the boot image.
 */
static image_Header     theHeader = 0;
static image_StringInfo theStringInfo = 0;
static Address          theHeap = 0;
static Address          theCode = 0;
static Address          theCodeEnd = 0;

/*************************************************************************
 Functions for accessing image sections (once they are loaded).
 ************************************************************************/

/**
 * Gets a pointer to the Header section in the boot image.
 */
image_Header image_header(void) {
    c_ASSERT(theHeader != NULL);
    return theHeader;
}

/**
 * Gets a pointer to the StringInfo section in the boot image.
 */
image_StringInfo image_stringInfo(void) {
    return theStringInfo;
}

/**
 * Gets a pointer to the (page-aligned) object heap in the boot image.
 */
Address image_heap(void) {
    return theHeap;
}

/**
 * Gets a pointer to the (page-aligned) code section in the boot image.
 */
Address image_code(void) {
    return theCode;
}

Address image_code_end(void) {
    return theCodeEnd;
}

/*************************************************************************
 Functions for loading the image from a file.
 ************************************************************************/

static char *nextString(char *p) {
    while (*p++ != '\0') {
    }
    return p;
}

static char *endiannessToString(jint isBigEndian) {
    if (isBigEndian == 0) {
        return "little";
    } else {
        return "big";
    }
}

static struct image_Header theHeaderStruct;

/**
 * Reads the Header section from a boot image.
 *
 * @param fd a file descriptor  opened on the boot image file currently positioned at the start of the Header section
 */
static void readHeader(int fd) {
    theHeader = &theHeaderStruct;
#if !MEMORY_IMAGE
    int n = read(fd, theHeader, sizeof(struct image_Header));
    if (n != sizeof(struct image_Header)) {
        log_exit(1, "could not read image header");
    }
#else
    memcpy((void *) theHeader, (void *) &maxvm_image_start, sizeof(struct image_Header));
#endif
#if log_LOADER
    log_println("ImageHeader @ %p {", theHeader);
#define PRINT_IMAGE_HEADER_FIELD(name) log_println("    %35s: %d (0x%04x)", STRINGIZE(name), theHeader->name, theHeader->name);
    IMAGE_HEADER_FIELDS(PRINT_IMAGE_HEADER_FIELD)
#undef PRINT_IMAGE_HEADER_FIELD
    log_println("}");
#endif

    if ((theHeader->isBigEndian != 0) != (word_BIG_ENDIAN != 0)) {
        log_exit(3, "image has wrong endianness - expected: %s, found: %s", endiannessToString(word_BIG_ENDIAN), endiannessToString(theHeader->isBigEndian));
    }
}

static struct image_StringInfo theStringInfoStruct;

/**
 * Reads the StringInfo section from a boot image.
 *
 * @param fd a file descriptor  opened on the boot image file currently positioned at the start of the StringInfo section
 */
static void readStringInfo(int fd) {
    char *s;
    int keyValueCount = 0;
    char *keyValueData;
    int n;
#if !MEMORY_IMAGE
    n = read(fd, &keyValueCount, 4);
    if (n != 4) {
        log_exit(2, "could not read string info key/value count");
    }
    int keyValueDataSize = theHeader->stringDataSize - 4;
    keyValueData = malloc(keyValueDataSize);
    if (keyValueData == NULL) {
        log_exit(1, "could not allocate string info");
    }

    n = read(fd, keyValueData, keyValueDataSize);
    if (n != keyValueDataSize) {
        log_exit(2, "could not read string info");
    }
#else
    keyValueCount = *((int *) ((char *) &maxvm_image_start + sizeof(struct image_Header)));
    keyValueData = ((char *) &maxvm_image_start + sizeof(struct image_Header) + 4);
#endif
#if log_LOADER
    log_println("image.readStringInfo @ 0x%x [count = %d]", keyValueData, keyValueCount);
#endif
    theStringInfo = &theStringInfoStruct;
    theStringInfo->count = keyValueCount;
    theStringInfo->values = malloc(keyValueCount * sizeof(struct image_KeyValue));
    s = keyValueData;
    for (n = 0; n < keyValueCount; n++) {
        image_KeyValue kv = (image_KeyValue) &(theStringInfo->values[n]);
        kv->key = s;
        s = nextString(s);
        kv->value = s;
        s = nextString(s);
#if log_LOADER
        log_println("    %s: %s", kv->key, kv->value);
#endif
    }
}

#define checkThreadLocalIndex(name) do { \
    if (theHeader->name != name) { \
        log_exit(2, "index of thread local %s in image [%d] conflicts with value declared in threadLocals.h [%d]" \
                    "\nEdit the number in threadLocals.h to reflect the current index of the thread local in the image.", \
                        STRINGIZE(name), theHeader->name, name); \
    } \
} while(0)

#define CHECK_THREAD_LOCAL(name, index) checkThreadLocalIndex(name);

static void checkImage(void) {
#if log_LOADER
    log_println("image.checkImage");
#endif
    if ((theHeader->isBigEndian != 0) != word_BIG_ENDIAN) {
        log_exit(3, "image has wrong endianness - expected: %s, found: %s", endiannessToString(word_BIG_ENDIAN), endiannessToString(theHeader->isBigEndian));
    }
    if (theHeader->identification != (jint) IMAGE_IDENTIFICATION) {
        log_exit(2, "not a valid Maxine VM boot image file");
    }
    if (theHeader->bootImageFormatVersion != IMAGE_FORMAT_VERSION) {
        log_exit(2, "wrong image format version - expected: %d, found: %d", IMAGE_FORMAT_VERSION, theHeader->bootImageFormatVersion);
    }
    if ((theHeader->wordSize == 8) != word_64_BITS) {
        log_exit(2, "image has wrong word size - expected: %d bits, found: %d bits", word_64_BITS ? 64 : 32, theHeader->wordSize * 8);
    }
    if (theHeader->cacheAlignment < MIN_CACHE_ALIGNMENT) {
        log_exit(2, "image has insufficient alignment - expected: %d, found: %d", MIN_CACHE_ALIGNMENT, theHeader->cacheAlignment);
    }
    if (theHeader->pageSize != (jint) virtualMemory_getPageSize()) {
        log_exit(2, "image has wrong page size - expected: %d, found: %d", virtualMemory_getPageSize(), theHeader->pageSize);
    }

    FOR_ALL_THREAD_LOCALS(CHECK_THREAD_LOCAL)
}

static void checkTrailer(int fd) {
#if !MEMORY_IMAGE
    off_t fileSize, expectedFileSize, offset;
    int n;
#endif
    off_t trailerOffset;
    struct image_Trailer trailerStruct;
    image_Trailer trailerStructPtr = &trailerStruct;

    trailerOffset = virtualMemory_pageAlign(sizeof(struct image_Header) + theHeader->stringDataSize + theHeader->relocationDataSize) + theHeader->codeSize + theHeader->heapSize;

#if !MEMORY_IMAGE
    fileSize = lseek(fd, 0, SEEK_END);
    expectedFileSize = trailerOffset + sizeof(trailerStruct);
    if (fileSize < 0) {
        log_exit(1, "could not set end position in file");
    }
    if (fileSize != expectedFileSize) {
        log_exit(2, "wrong image file size: expected %u bytes, read %u", expectedFileSize,  fileSize);
    }
    offset = lseek(fd, trailerOffset, SEEK_SET);
    if (offset != trailerOffset) {
        log_exit(1, "could not set trailer position in file");
    }
    n = read(fd, &trailerStruct, sizeof(trailerStruct));
    if (n != sizeof(trailerStruct)) {
        log_exit(1, "could not read trailer");
    }
#else
#if log_LOADER
    log_println("image.checkTrailer offset: %d", trailerOffset);
#endif
    trailerStructPtr = (image_Trailer)(((char*)&maxvm_image_start) + trailerOffset);
#endif

    if (trailerStructPtr->identification != theHeader->identification || trailerStructPtr->bootImageFormatVersion != theHeader->bootImageFormatVersion || trailerStructPtr->randomID != theHeader->randomID) {
        log_println("inconsistent trailer");
#if !MEMORY_IMAGE
        offset = lseek(fd, -sizeof(trailerStruct), SEEK_END);
        if (offset != fileSize - (off_t) sizeof(trailerStruct)) {
            log_exit(1, "could not set trailer position at end of file");
        }
        n = read(fd, &trailerStruct, sizeof(trailerStruct));
        if (n != sizeof(trailerStruct)) {
            log_exit(1, "could not read trailer at end of file");
        }
#else
        trailerStructPtr = (image_Trailer)(((char*)&maxvm_image_end) - sizeof(trailerStruct));
#endif
        if (trailerStructPtr->identification == theHeader->identification && trailerStructPtr->bootImageFormatVersion == theHeader->bootImageFormatVersion && trailerStructPtr->randomID == theHeader->randomID) {
            log_println("FYI, found valid trailer at end of file");
        }
        exit(2);
    }
}

static void mapHeapAndCode(int fd) {
    int heapOffsetInImage = virtualMemory_pageAlign(sizeof(struct image_Header) + theHeader->stringDataSize + theHeader->relocationDataSize);
    int heapAndCodeSize = theHeader->heapSize + theHeader->codeSize;
    c_ASSERT(virtualMemory_pageAlign((Size) heapAndCodeSize) == (Size) heapAndCodeSize);
#if log_LOADER
    log_println("image.mapHeapAndCode");
#endif
#if MEMORY_IMAGE
    theHeap = (Address) &maxvm_image_start + heapOffsetInImage;
#elif os_LINUX
    theHeap = virtualMemory_mapFileIn31BitSpace(heapAndCodeSize, fd, heapOffsetInImage);
    if (theHeap == ALLOC_FAILED) {
        log_exit(4, "could not map boot image");
    }
#elif os_SOLARIS || os_DARWIN
    Address reservedVirtualSpace = (Address) 0;
    size_t virtualSpaceSize = 1024L * theHeader->reservedVirtualSpaceSize;
    c_ASSERT(virtualMemory_pageAlign((Size) virtualSpaceSize) == (Size) virtualSpaceSize);
    if (virtualSpaceSize != 0) {
        // VM configuration asks for reserving an address space of size reservedVirtualSpaceSize.
        // The following will create a mapping in virtual space of the requested size.The address returned might subsequently be used to memory map the
        // boot heap region, automatically splitting this mapping in two.
        // In any case,  the VM (mostly the heap scheme) is responsible for reserved space.
        reservedVirtualSpace = virtualMemory_allocatePrivateAnon((Address) 0, virtualSpaceSize, JNI_FALSE, JNI_FALSE, HEAP_VM);
        if (reservedVirtualSpace == ALLOC_FAILED) {
            log_exit(4, "could not reserve requested virtual space");
        }
    }
    if (theHeader->bootRegionMappingConstraint == 1) {
        // Map the boot heap region at the start of the reserved space
        theHeap = reservedVirtualSpace;
    } else if (theHeader->bootRegionMappingConstraint == 2) {
        // Map the boot heap region at the end of the reserved space. The start of the boot heap region is page-aligned.
        theHeap = reservedVirtualSpace + virtualSpaceSize - heapAndCodeSize;
    } else {
        // Map the boot heap region anywhere.
        theHeap = virtualMemory_allocatePrivateAnon((Address) 0, heapAndCodeSize, JNI_FALSE, JNI_FALSE, HEAP_VM);
        if (theHeap == ALLOC_FAILED) {
            log_exit(4, "could not reserve virtual space for boot image");
        }
    }
    if (virtualMemory_mapFileAtFixedAddress(theHeap, heapAndCodeSize, fd, heapOffsetInImage) == ALLOC_FAILED) {
        log_exit(4, "could not map boot image");
    }
    if (reservedVirtualSpace) {
        Address *addr = image_offset_as_address(Address *, reservedVirtualSpaceFieldOffset);
        *addr = reservedVirtualSpace;
    }
#else
    c_UNIMPLEMENTED();
#endif
#if os_MAXVE
    // boot heap and code must be mapped together (the method offsets in boot image are relative to heap base)
    theHeap = maxve_remap_boot_code_region(theHeap, heapAndCodeSize);
#endif
#if log_LOADER
    log_println("boot heap mapped at %p", theHeap);
#endif
    theCode = theHeap + theHeader->heapSize;
    theCodeEnd = theCode + theHeader->codeSize;
}

static void relocate(int fd) {
    off_t wantedFileOffset;
    Byte *relocationData;
#if log_LOADER
    log_println("image.relocate");
#endif
#if !MEMORY_IMAGE
    off_t actualFileOffset;
    int n;
#endif

    wantedFileOffset = sizeof(struct image_Header) + theHeader->stringDataSize;
#if !MEMORY_IMAGE
    relocationData = (Byte *) malloc(theHeader->relocationDataSize);
    if (relocationData == NULL) {
        log_exit(1, "could not allocate memory for relocation data");
    }

    actualFileOffset = lseek(fd, wantedFileOffset, SEEK_SET);
    if (actualFileOffset != wantedFileOffset) {
        log_exit(1, "could not set relocation data position in file");
    }
    n = read(fd, relocationData, theHeader->relocationDataSize);
    if (n != theHeader->relocationDataSize) {
        log_exit(1, "could not read relocation data");
    }
#else
    relocationData = (Byte*)(((char*)&maxvm_image_start) + wantedFileOffset);
#endif

#if log_LOADER
    log_println("image.relocate [relocation map: %d bytes]", theHeader->relocationDataSize);
#endif
    relocation_apply((void *) theHeap, theHeap, relocationData, theHeader->relocationDataSize, word_BIG_ENDIAN, theHeader->wordSize);

#if !MEMORY_IMAGE
    free(relocationData);
#endif
#if log_LOADER
    log_println("image.relocate: done");
#endif
}

int image_load(char *imageFileName) {
    if (theHeap != 0) {
        // loaded already (via inspector)
        return 0;
    }
    int fd = -1;
#if !MEMORY_IMAGE
#if log_LOADER
    log_println("reading image from %s", imageFileName);
#endif
    fd = open(imageFileName, O_RDWR);
    if (fd < 0) {
        log_exit(1, "could not open image file: %s", imageFileName);
    }
#endif

    readHeader(fd);
    checkImage();
    readStringInfo(fd);
    checkTrailer(fd);
    mapHeapAndCode(fd);
#if log_LOADER
    log_println("code @%p codeEnd @%p heap @%p", theCode, theCodeEnd, theHeap);
#endif
    relocate(fd);
#if log_LOADER
    log_println("code @%p codeEnd @%p heap @%p", theCode, theCodeEnd, theHeap);
#endif
    return fd;
}

void image_printAddress(Address address) {
#if word_64_BITS
    log_print("0x%016lx", address);
#else
    log_print("0x%08lx", address);
#endif
    if (address >= theHeap && address < theCode) {
        log_print("(heap + %d)", (int) (address - theHeap));
    } else if (address >= theCode && address < theCodeEnd) {
        log_print("(code + %d)", (int) (address - theCode));
    }
}
