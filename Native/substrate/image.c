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

#include "image.h"
#include "debug.h"
#include "word.h"

/* TODO: make this cpu-dependent: */
#define MIN_ALIGNMENT 4

#define IMAGE_IDENTIFICATION             0xcafe4dad
#define IMAGE_VERSION                    1
#define DEFAULT_RELOCATION_SCHEME        0
#define TERA_BYTE (1024*1024*1024*1024L)

#if os_GUESTVMXEN
#define MEMORY_IMAGE 1
#else
#define MEMORY_IMAGE 0
#endif

#if MEMORY_IMAGE
extern void *maxvm_image_start;
extern void *maxvm_image_end;
#endif

static struct image_Header _headerStruct;
static image_Header _header = &_headerStruct;

image_Header image_header(void) {
    return _header;
}

static jint getLittleEndianInt(jint *pInt) {
	int i;
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
	for (i = sizeof(jint) - 1; i >= 0; i--) {
		result |= *pByte++ << (i * 8);
	}
	return result;
}

static void readHeader(int fd) {
 	jint *from, *to;
	jint isBigEndian;
	int i;

#if !MEMORY_IMAGE
	struct image_Header rawHeaderStruct;
    int n = read(fd, &rawHeaderStruct, sizeof(struct image_Header));
    if (n != sizeof(struct image_Header)) {
      debug_exit(1, "could not read image header");
    }
	from = (jint *) &rawHeaderStruct;
#else
	from = (jint *) &maxvm_image_start;
#if DEBUG_LOADER
	debug_println("image.readHeader @ 0x%x,", &maxvm_image_start);
#endif
#endif
	to = (jint *) _header;
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

static struct image_StringInfo _stringInfoStruct;
static image_StringInfo _stringInfo = &_stringInfoStruct;

image_StringInfo image_stringInfo(void) {
    return _stringInfo;
}

static char *nextString(char *p) {
    while (*p++ != '\0') {
    }
    return p;
}

static char *_stringInfoData;

static void readStringInfo(int fd) {
	char **p;
	char *s;
#if !MEMORY_IMAGE
	int n;
    _stringInfoData = malloc(_header->stringDataSize);
    if (_stringInfoData == NULL) {
      debug_exit(1, "could not allocate string info");
    }

    n = read(fd, _stringInfoData, _header->stringDataSize);
    if (n != _header->stringDataSize) {
      debug_exit(2, "could not read string info");
    }
#else
    _stringInfoData = ((char *) &maxvm_image_start + sizeof(struct image_Header));
#endif
#if DEBUG_LOADER
	 debug_println("image.readStringInfo @ 0x%x", _stringInfoData);
#endif
	p = (char **) _stringInfo;
    s = _stringInfoData;
	while (p < (char **) &_stringInfo[1]) {
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

static void checkImage(void) {
#if DEBUG_LOADER
	debug_println("image.checkImage");
#endif
	if ((_header->isBigEndian != 0) != word_BIG_ENDIAN) {
        debug_exit(3, "image has wrong endianess - expected: %s, found: %s",
                endiannessToString(word_BIG_ENDIAN), endiannessToString(_header->isBigEndian));
    }
    if (_header->identification != IMAGE_IDENTIFICATION) {
      debug_exit(2, "not a valid Maxine VM boot image file");
    }
    if (_header->version != IMAGE_VERSION) {
      debug_exit(2, "wrong image format version - expected: %d, found: %d", IMAGE_VERSION, _header->version);
    }
    if ((_header->wordSize == 8) != word_64_BITS) {
        debug_exit(2, "image has wrong word size - expected: %d bits, found: %d bits",
                word_64_BITS ? 64 : 32, _header->wordSize * 8);
    }
    if (_header->alignmentSize < MIN_ALIGNMENT) {
        debug_exit(2, "image has insufficient alignment - expected: %d, found: %d",
                MIN_ALIGNMENT, _header->alignmentSize);
    }
    if (_header->pageSize != getpagesize()) {
        debug_exit(2, "image has wrong page size - expected: %d, found: %d",
                getpagesize(), _header->pageSize);
    }
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

    trailerOffset = pageAligned(sizeof(struct image_Header) + _header->stringDataSize + _header->relocationDataSize) + _header->bootCodeSize + _header->bootHeapSize;

#if !MEMORY_IMAGE
    fileSize = lseek(fd, 0, SEEK_END);
    if (fileSize < 0) {
      debug_exit(1, "could not set end position in file");
    }
    if (fileSize - sizeof(trailerStruct) < trailerOffset) {
      debug_exit(2, "truncated file");
    }
    if (fileSize - sizeof(trailerStruct) > trailerOffset) {
        fprintf(stderr, "WARNING: file too large - expected: %d,  found %d\n", (int) (trailerOffset + sizeof(trailerStruct)), (int) fileSize);
    }
    offset = lseek(fd, trailerOffset, SEEK_SET);
    if (offset != trailerOffset) {
      debug_exit(1, "could not set trailer position in file");
    }
    n = read(fd, &trailerStruct, sizeof(trailerStruct));
    if (n != sizeof(trailerStruct)) {
      debug_exit(1, "could not read trailer");
    }
 #else
#if DEBUG_LOADER
   debug_println("image.checkTrailer offset: %d", trailerOffset);
#endif
   trailerStructPtr = (image_Trailer)(((char*)&maxvm_image_start) + trailerOffset);
#endif

    if (trailerStructPtr->identification != _header->identification || trailerStructPtr->version != _header->version || trailerStructPtr->randomID != _header->randomID) {
        fprintf(stderr, "inconsistent trailer\n");
#if !MEMORY_IMAGE
        offset = lseek(fd, -sizeof(trailerStruct), SEEK_END);
        if (offset != fileSize - sizeof(trailerStruct)) {
	  debug_exit(1, "could not set trailer position at end of file");
        }
        n = read(fd, &trailerStruct, sizeof(trailerStruct));
        if (n != sizeof(trailerStruct)) {
	  debug_exit(1, "could not read trailer at end of file");
        }
#else
        trailerStructPtr = (image_Trailer)(((char*)&maxvm_image_end) - sizeof(trailerStruct));
#endif
        if (trailerStructPtr->identification == _header->identification && trailerStructPtr->version == _header->version && trailerStructPtr->randomID == _header->randomID) {
            fprintf(stderr, "FYI, found valid trailer at end of file\n");
        }
        exit(2);
    }
}

static Address _heap = 0;

Address image_heap(void) {
    return _heap;
}

static Address _code = 0;
static Address _codeEnd = 0;

Address image_code(void) {
    return _code;
}

static void mapHeapAndCode(int fd) {
    int fileOffset = pageAligned(sizeof(struct image_Header) + _header->stringDataSize + _header->relocationDataSize);
#if DEBUG_LOADER
    debug_println("image.mapHeapAndCode");
#endif
#if MEMORY_IMAGE
    _heap = (Address) &maxvm_image_start + fileOffset;
#elif os_LINUX
    _heap = virtualMemory_mapFileIn31BitSpace(_header->bootHeapSize + _header->bootCodeSize, fd, fileOffset);
    if (_heap == 0) {
      debug_exit(4, "could not map boot image");
    }
#elif os_SOLARIS || os_DARWIN
    // Reserve more than -Xmx should ever demand.
    // Most of this will be released again once in Java code by the heap scheme
    _heap = virtualMemory_reserve(TERA_BYTE);
    if (_heap == 0) {
      debug_exit(4, "could not reserve boot image");
    }
#if DEBUG_LOADER
    debug_println("reserved 1 TB at %p", _heap);
    debug_println("reserved address space ends at %p", _heap + TERA_BYTE);
#endif

    if (!virtualMemory_mapFileAtFixedAddress(_heap, _header->bootHeapSize + _header->bootCodeSize, fd, fileOffset)) {
      debug_exit(4, "could not map boot image");
    }
#else
#error Unimplemented
#endif
    _code = _heap + _header->bootHeapSize;
    _codeEnd = _code + _header->bootCodeSize;
}

static void relocate(int fd) {
   off_t wantedFileOffset;
    Byte *relocationData;
#if DEBUG_LOADER
	debug_println("image.relocate");
#endif
#if !MEMORY_IMAGE
    off_t actualFileOffset;
    int n;
#endif

    wantedFileOffset = sizeof(struct image_Header) + _header->stringDataSize;
#if !MEMORY_IMAGE
    relocationData = (Byte *) malloc(_header->relocationDataSize);
    if (relocationData == NULL) {
      debug_exit(1, "could not allocate memory for relocation data");
    }

    actualFileOffset = lseek(fd, wantedFileOffset, SEEK_SET);
    if (actualFileOffset != wantedFileOffset) {
      debug_exit(1, "could not set relocation data position in file");
    }
    n = read(fd, relocationData, _header->relocationDataSize);
    if (n != _header->relocationDataSize) {
      debug_exit(1, "could not read relocation data");
    }
#else
    relocationData = (Byte*)(((char*)&maxvm_image_start) + wantedFileOffset);
#endif

    relocation_apply(
        (void *) _heap,
        _header->relocationScheme,
        relocationData,
        _header->relocationDataSize,
        _header->alignmentSize,
        word_BIG_ENDIAN,
        _header->wordSize);

#if !MEMORY_IMAGE
    free(relocationData);
#endif
}

int image_load(char *imageFileName) {
	if (_heap != 0) {
		// loaded already (via inspector)
		return 0;
	}
	int fd = -1;
#if !MEMORY_IMAGE
#if DEBUG_LOADER
	 debug_println("reading image from %s", imageFileName);
#endif
    fd = open(imageFileName, O_RDWR);
    if (fd < 0) {
      debug_exit(1, "could not open image file: %s", imageFileName);
    }
#endif

    readHeader(fd);
    checkImage();
    readStringInfo(fd);
    checkTrailer(fd);
	mapHeapAndCode(fd);
#if DEBUG_LOADER
	 debug_println("code @%p codeEnd @%p heap @%p", _code,_codeEnd, _heap);
#endif
    relocate(fd);
#if DEBUG_LOADER
	 debug_println("code @%p codeEnd @%p heap @%p", _code,_codeEnd, _heap);
#endif
    return fd;
}

Address nativeGetEndOfCodeRegion(){
	Address addr = _codeEnd + _header->codeCacheSize ;
#if DEBUG_LOADER
	debug_println("nativeGetEndOfCodeRegion: end of boot region @ %p code cache size %ld code end  %p", addr, _header->codeCacheSize, _codeEnd);
#endif
	return addr;
}


void image_printAddress(Address address) {
#if word_64_BITS
  debug_print("0x%016lx", address);
#else
  debug_print("0x%08lx", address);
#endif
  if (address >= _heap && address < _code) {
    debug_print("(heap + %d)", (int)(address - _heap));
  } else if (address >= _code && address < _codeEnd) {
    debug_print("(code + %d)", (int)(address - _code));
  }
}
