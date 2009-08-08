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
#else
#define MEMORY_IMAGE 0
#endif

#if MEMORY_IMAGE
extern void *maxvm_image_start;
extern void *maxvm_image_end;
#endif

static struct image_Header theImageHeaderStruct;
static image_Header theImageHeader = &theImageHeaderStruct;

image_Header image_header(void) {
    c_ASSERT(theImageHeader != NULL);
    return theImageHeader;
}

static jint getLittleEndianInt(jint *pInt) {
    unsigned int i;
    jint result = 0;
    Byte *pByte = (Byte *) pInt;
    for (i = 0; i < sizeof(jint); i++) {
        result |= *pByte++ << (i * 8);
    }
    return result;
}

static jint getBigEndianInt(jint *pInt) {
    int i;
    jint result = 0;
    Byte *pByte = (Byte *) pInt;
    for (i = (int) sizeof(jint) - 1; i >= 0; i--) {
        result |= *pByte++ << (i * 8);
    }
    return result;
}

static void readHeader(int fd) {
    jint *from, *to;
    jint isBigEndian;
    unsigned int i;

#if !MEMORY_IMAGE
    struct image_Header rawHeaderStruct;
    int n = read(fd, &rawHeaderStruct, sizeof(struct image_Header));
    if (n != sizeof(struct image_Header)) {
        log_exit(1, "could not read image header");
    }
    from = (jint *) &rawHeaderStruct;
#else
    from = (jint *) &maxvm_image_start;
#if log_LOADER
    log_println("image.readHeader @ 0x%x,", &maxvm_image_start);
#endif
#endif
    to = (jint *) theImageHeader;
    isBigEndian = *from;
    for (i = 0; i < sizeof(struct image_Header) / sizeof(jint); i++) {
        if (isBigEndian == 0) {
            *to = getLittleEndianInt(from);
        } else {
            *to = getBigEndianInt(from);
        }
        from++;
        to++;
    }
}

static struct image_StringInfo theStringInfoStruct;
static image_StringInfo theStringInfo = &theStringInfoStruct;

image_StringInfo image_stringInfo(void) {
    return theStringInfo;
}

static char *nextString(char *p) {
    while (*p++ != '\0') {
    }
    return p;
}

static char *theStringInfoData;

static void readStringInfo(int fd) {
    char **p;
    char *s;
#if !MEMORY_IMAGE
    int n;
    theStringInfoData = malloc(theImageHeader->stringDataSize);
    if (theStringInfoData == NULL) {
        log_exit(1, "could not allocate string info");
    }

    n = read(fd, theStringInfoData, theImageHeader->stringDataSize);
    if (n != theImageHeader->stringDataSize) {
        log_exit(2, "could not read string info");
    }
#else
    theStringInfoData = ((char *) &maxvm_image_start + sizeof(struct image_Header));
#endif
#if log_LOADER
    log_println("image.readStringInfo @ 0x%x", theStringInfoData);
#endif
    p = (char **) theStringInfo;
    s = theStringInfoData;
    while (p < (char **) &theStringInfo[1]) {
        *p++ = s;
        s = nextString(s);
    }

    // TODO: this check is not correct, as it depends on the order of fields appear in the string info section
    //    if (s != nextString(_stringInfo->runPackageName)) {
    //        fprintf(stderr, "inconsistent string info size\n");
    //        exit(3);
    //    }
}

static char *endiannessToString(jint isBigEndian) {
    if (isBigEndian == 0) {
        return "little";
    } else {
        return "big";
    }
}

#define checkThreadLocalIndex(name) do { \
    if (theImageHeader->name != name) { \
        log_exit(2, "value of %s in image [%d] conflicts with value declared in threadLocals.h [%d]", \
                        STRINGIZE(name), theImageHeader->name, name); \
    } \
} while(0)


static void checkImage(void) {
#if log_LOADER
    log_println("image.checkImage");
#endif
    if ((theImageHeader->isBigEndian != 0) != word_BIG_ENDIAN) {
        log_exit(3, "image has wrong endianess - expected: %s, found: %s", endiannessToString(word_BIG_ENDIAN), endiannessToString(theImageHeader->isBigEndian));
    }
    if (theImageHeader->identification != (jint) IMAGE_IDENTIFICATION) {
        log_exit(2, "not a valid Maxine VM boot image file");
    }
    if (theImageHeader->version != IMAGE_VERSION) {
        log_exit(2, "wrong image format version - expected: %d, found: %d", IMAGE_VERSION, theImageHeader->version);
    }
    if ((theImageHeader->wordSize == 8) != word_64_BITS) {
        log_exit(2, "image has wrong word size - expected: %d bits, found: %d bits", word_64_BITS ? 64 : 32, theImageHeader->wordSize * 8);
    }
    if (theImageHeader->cacheAlignment < MIN_CACHE_ALIGNMENT) {
        log_exit(2, "image has insufficient alignment - expected: %d, found: %d", MIN_CACHE_ALIGNMENT, theImageHeader->cacheAlignment);
    }
    if (theImageHeader->pageSize != getpagesize()) {
        log_exit(2, "image has wrong page size - expected: %d, found: %d", getpagesize(), theImageHeader->pageSize);
    }
    checkThreadLocalIndex(SAFEPOINT_LATCH);
    checkThreadLocalIndex(SAFEPOINTS_ENABLED_THREAD_LOCALS);
    checkThreadLocalIndex(SAFEPOINTS_DISABLED_THREAD_LOCALS);
    checkThreadLocalIndex(SAFEPOINTS_TRIGGERED_THREAD_LOCALS);
    checkThreadLocalIndex(NATIVE_THREAD_LOCALS);
    checkThreadLocalIndex(FORWARD_LINK);
    checkThreadLocalIndex(BACKWARD_LINK);
    checkThreadLocalIndex(ID);
    checkThreadLocalIndex(TRAP_NUMBER);
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
    off_t fileSize, offset;
    int n;
#endif
    off_t trailerOffset;
    struct image_Trailer trailerStruct;
    image_Trailer trailerStructPtr = &trailerStruct;

    trailerOffset = pageAligned(sizeof(struct image_Header) + theImageHeader->stringDataSize + theImageHeader->relocationDataSize) + theImageHeader->bootCodeSize + theImageHeader->bootHeapSize;

#if !MEMORY_IMAGE
    fileSize = lseek(fd, 0, SEEK_END);
    if (fileSize < 0) {
        log_exit(1, "could not set end position in file");
    }
    if (fileSize - (off_t) sizeof(trailerStruct) < trailerOffset) {
        log_exit(2, "truncated file");
    }
    if (fileSize - (off_t) sizeof(trailerStruct) > trailerOffset) {
        fprintf(stderr, "WARNING: file too large - expected: %d,  found %d\n", (int) (trailerOffset + sizeof(trailerStruct)), (int) fileSize);
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

    if (trailerStructPtr->identification != theImageHeader->identification || trailerStructPtr->version != theImageHeader->version || trailerStructPtr->randomID != theImageHeader->randomID) {
        fprintf(stderr, "inconsistent trailer\n");
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
        if (trailerStructPtr->identification == theImageHeader->identification && trailerStructPtr->version == theImageHeader->version && trailerStructPtr->randomID == theImageHeader->randomID) {
            fprintf(stderr, "FYI, found valid trailer at end of file\n");
        }
        exit(2);
    }
}

static Address theHeap = 0;

Address image_heap(void) {
    return theHeap;
}

static Address theCode = 0;
static Address theCodeEnd = 0;

Address image_code(void) {
    return theCode;
}

Address image_code_end(void) {
    return theCodeEnd;
}

static void mapHeapAndCode(int fd) {
    int heapOffsetInImage = pageAligned(sizeof(struct image_Header) + theImageHeader->stringDataSize + theImageHeader->relocationDataSize);
#if log_LOADER
    log_println("image.mapHeapAndCode");
#endif
#if MEMORY_IMAGE
    theHeap = (Address) &maxvm_image_start + heapOffsetInImage;
#elif os_LINUX
    theHeap = virtualMemory_mapFileIn31BitSpace(theImageHeader->bootHeapSize + theImageHeader->bootCodeSize, fd, heapOffsetInImage);
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

    if (virtualMemory_mapFileAtFixedAddress(theHeap, theImageHeader->bootHeapSize + theImageHeader->bootCodeSize, fd, heapOffsetInImage) == ALLOC_FAILED) {
        log_exit(4, "could not map boot image");
    }
#else
    c_UNIMPLEMENTED();
#endif
#if os_GUESTVMXEN
    // heap and code must be mapped together (the method offsets in boot image are relative to heap base)
    theHeap = guestvmXen_remap_boot_code_region(theHeap, theImageHeader->bootHeapSize + theImageHeader->bootCodeSize);
#endif
    theCode = theHeap + theImageHeader->bootHeapSize;
    theCodeEnd = theCode + theImageHeader->bootCodeSize;
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

    wantedFileOffset = sizeof(struct image_Header) + theImageHeader->stringDataSize;
#if !MEMORY_IMAGE
    relocationData = (Byte *) malloc(theImageHeader->relocationDataSize);
    if (relocationData == NULL) {
        log_exit(1, "could not allocate memory for relocation data");
    }

    actualFileOffset = lseek(fd, wantedFileOffset, SEEK_SET);
    if (actualFileOffset != wantedFileOffset) {
        log_exit(1, "could not set relocation data position in file");
    }
    n = read(fd, relocationData, theImageHeader->relocationDataSize);
    if (n != theImageHeader->relocationDataSize) {
        log_exit(1, "could not read relocation data");
    }
#else
    relocationData = (Byte*)(((char*)&maxvm_image_start) + wantedFileOffset);
#endif

    relocation_apply((void *) theHeap, theImageHeader->relocationScheme, relocationData, theImageHeader->relocationDataSize, theImageHeader->cacheAlignment, word_BIG_ENDIAN, theImageHeader->wordSize);

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

Address nativeGetEndOfCodeRegion() {
    Address addr = theCodeEnd + theImageHeader->codeCacheSize;
#if log_LOADER
    log_println("nativeGetEndOfCodeRegion: end of boot region @ %p code cache size %ld code end  %p", addr, theImageHeader->codeCacheSize, theCodeEnd);
#endif
    return addr;
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
