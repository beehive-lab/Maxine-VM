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
#define IMAGE_VERSION                    1
#define DEFAULT_RELOCATION_SCHEME        0
#define TERA_BYTE (1024*1024*1024*1024L)

#if os_GUESTVMXEN
#define MEMORY_IMAGE 1
#include <guestvmXen.h>
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
    log_println("image.readHeader @ %p", theHeader);
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
    char **p;
    char *s;
    char *stringInfoData;
#if !MEMORY_IMAGE
    int n;
    stringInfoData = malloc(theHeader->stringDataSize);
    if (stringInfoData == NULL) {
        log_exit(1, "could not allocate string info");
    }

    n = read(fd, stringInfoData, theHeader->stringDataSize);
    if (n != theHeader->stringDataSize) {
        log_exit(2, "could not read string info");
    }
#else
    stringInfoData = ((char *) &maxvm_image_start + sizeof(struct image_Header));
#endif
#if log_LOADER
    log_println("image.readStringInfo @ 0x%x", stringInfoData);
#endif
    theStringInfo = &theStringInfoStruct;
    p = (char **) theStringInfo;
    s = stringInfoData;
    while (p < (char **) &theStringInfo[1]) {
        *p++ = s;
        s = nextString(s);
    }
}

#define checkThreadLocalIndex(name) do { \
    if (theHeader->name != name) { \
        log_exit(2, "index of thread local %s in image [%d] conflicts with value declared in threadLocals.h [%d]" \
        		    "\nEdit the number in threadLocals.h to reflect the current index of the thread local in the image.", \
                        STRINGIZE(name), theHeader->name, name); \
    } \
} while(0)


static void checkImage(void) {
#if log_LOADER
    log_println("image.checkImage");
#endif
    if ((theHeader->isBigEndian != 0) != word_BIG_ENDIAN) {
        log_exit(3, "image has wrong endianess - expected: %s, found: %s", endiannessToString(word_BIG_ENDIAN), endiannessToString(theHeader->isBigEndian));
    }
    if (theHeader->identification != (jint) IMAGE_IDENTIFICATION) {
        log_exit(2, "not a valid Maxine VM boot image file");
    }
    if (theHeader->version != IMAGE_VERSION) {
        log_exit(2, "wrong image format version - expected: %d, found: %d", IMAGE_VERSION, theHeader->version);
    }
    if ((theHeader->wordSize == 8) != word_64_BITS) {
        log_exit(2, "image has wrong word size - expected: %d bits, found: %d bits", word_64_BITS ? 64 : 32, theHeader->wordSize * 8);
    }
    if (theHeader->cacheAlignment < MIN_CACHE_ALIGNMENT) {
        log_exit(2, "image has insufficient alignment - expected: %d, found: %d", MIN_CACHE_ALIGNMENT, theHeader->cacheAlignment);
    }
    if (theHeader->pageSize != getpagesize()) {
        log_exit(2, "image has wrong page size - expected: %d, found: %d", getpagesize(), theHeader->pageSize);
    }
    checkThreadLocalIndex(SAFEPOINT_LATCH);
    checkThreadLocalIndex(SAFEPOINTS_ENABLED_THREAD_LOCALS);
    checkThreadLocalIndex(SAFEPOINTS_DISABLED_THREAD_LOCALS);
    checkThreadLocalIndex(SAFEPOINTS_TRIGGERED_THREAD_LOCALS);
    checkThreadLocalIndex(NATIVE_THREAD_LOCALS);
    checkThreadLocalIndex(FORWARD_LINK);
    checkThreadLocalIndex(BACKWARD_LINK);
    checkThreadLocalIndex(ID);
    checkThreadLocalIndex(LAST_JAVA_FRAME_ANCHOR);
    checkThreadLocalIndex(TRAP_INSTRUCTION_POINTER);
    checkThreadLocalIndex(TRAP_FAULT_ADDRESS);
    checkThreadLocalIndex(TRAP_LATCH_REGISTER);
}

static off_t pageAligned(off_t offset) {

    int pageSize = getpagesize();
    int rest = offset % pageSize;
    if (rest == 0) {
        return offset;
    }
    return offset + pageSize - rest;
}

static void checkTrailer(int fd) {
#if !MEMORY_IMAGE
    off_t fileSize, expectedFileSize, offset;
    int n;
#endif
    off_t trailerOffset;
    struct image_Trailer trailerStruct;
    image_Trailer trailerStructPtr = &trailerStruct;

    trailerOffset = pageAligned(sizeof(struct image_Header) + theHeader->stringDataSize + theHeader->relocationDataSize) + theHeader->codeSize + theHeader->heapSize;

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

    if (trailerStructPtr->identification != theHeader->identification || trailerStructPtr->version != theHeader->version || trailerStructPtr->randomID != theHeader->randomID) {
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
        if (trailerStructPtr->identification == theHeader->identification && trailerStructPtr->version == theHeader->version && trailerStructPtr->randomID == theHeader->randomID) {
            log_println("FYI, found valid trailer at end of file");
        }
        exit(2);
    }
}

static void mapHeapAndCode(int fd) {
    int heapOffsetInImage = pageAligned(sizeof(struct image_Header) + theHeader->stringDataSize + theHeader->relocationDataSize);
#if log_LOADER
    log_println("image.mapHeapAndCode");
#endif
#if MEMORY_IMAGE
    theHeap = (Address) &maxvm_image_start + heapOffsetInImage;
#elif os_LINUX
    theHeap = virtualMemory_mapFileIn31BitSpace(theHeader->heapSize + theHeader->codeSize, fd, heapOffsetInImage);
    if (theHeap == ALLOC_FAILED) {
        log_exit(4, "could not map boot image");
    }
#elif os_SOLARIS || os_DARWIN
    // Reserve more than -Xmx should ever demand.
    // Most of this will be released again once in Java code by the heap scheme
    theHeap = virtualMemory_allocateNoSwap(TERA_BYTE, HEAP_VM);
    if (theHeap == ALLOC_FAILED) {
        log_exit(4, "could not reserve boot image");
    }
#if log_LOADER
    log_println("reserved 1 TB at %p", theHeap);
    log_println("reserved address space ends at %p", theHeap + TERA_BYTE);
#endif
    if (virtualMemory_mapFileAtFixedAddress(theHeap, theHeader->heapSize + theHeader->codeSize, fd, heapOffsetInImage) == ALLOC_FAILED) {
        log_exit(4, "could not map boot image");
    }
#else
    c_UNIMPLEMENTED();
#endif
#if os_GUESTVMXEN
    // heap and code must be mapped together (the method offsets in boot image are relative to heap base)
    theHeap = guestvmXen_remap_boot_code_region(theHeap, theHeader->heapSize + theHeader->codeSize);
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

    relocation_apply((void *) theHeap, theHeap, relocationData, theHeader->relocationDataSize, word_BIG_ENDIAN, theHeader->wordSize);

#if !MEMORY_IMAGE
    free(relocationData);
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
    log_println("code @%p codeEnd @%p heap @%p", theCode,theCodeEnd, theHeap);
#endif
    relocate(fd);
#if log_LOADER
    log_println("code @%p codeEnd @%p heap @%p", theCode,theCodeEnd, theHeap);
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
