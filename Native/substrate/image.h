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
/**
 * Load and mmap the binary boot image of the VM.
 *
 * @author Bernd Mathiske
 */
#ifndef __image_h__
#define __image_h__ 1

#include "word.h"
#include "jni.h"

/*
 * Definition of fields in the image header struct.
 */
#define IMAGE_HEADER_FIELDS(f) \
    f(isBigEndian) /* 0: LITTLE, anything else: BIG. Must be first.  */ \
    f(identification) /* Magic number that must be present and have the same value in every Maxine boot image file  */ \
    f(bootImageFormatVersion) /* Version of boot image file format  */ \
    f(randomID) /* Specific to one particular instance of boot image file  */ \
    f(wordSize) /* 4 or 8  */ \
    f(cacheAlignment) \
    f(pageSize) /* multiple of 1024  */ \
    f(vmRunMethodOffset) \
    f(vmThreadAddMethodOffset) \
    f(vmThreadRunMethodOffset) \
    f(vmThreadAttachMethodOffset) \
    f(vmThreadDetachMethodOffset) \
    f(classRegistryOffset) \
    f(stringDataSize) \
    f(relocationDataSize) \
    f(heapSize) /* multiple of 'pageSize'  */ \
    f(codeSize) /* multiple of 'pageSize'  */ \
    f(dynamicHeapRegionsArrayOffset) \
    f(reservedVirtualSpaceSize) /* Amount of contiguous virtual space to reserve at boot image load-time  */ \
    f(reservedVirtualSpaceFieldOffset) /* offset where to store the address of the reserved contiguous virtual space, if any*/ \
    f(bootRegionMappingConstraint) \
    f(tlaListHeadOffset) /* See the comment for the 'tlaListHead' field in the VmThreadMap class.  */ \
    f(primordialETLAOffset) \
    f(tlaSize) /* The size of a TLA.  */ \
    f(SAFEPOINT_LATCH) \
    f(ETLA) \
    f(DTLA) \
    f(TTLA) \
    f(NATIVE_THREAD_LOCALS) \
    f(FORWARD_LINK) \
    f(BACKWARD_LINK) \
    f(ID) \
    f(JNI_ENV) \
    f(LAST_JAVA_FRAME_ANCHOR) \
    f(TRAP_NUMBER) \
    f(TRAP_INSTRUCTION_POINTER) \
    f(TRAP_FAULT_ADDRESS) \
    f(TRAP_LATCH_REGISTER) \
    f(STACK_REFERENCE_MAP) \
    f(STACK_REFERENCE_MAP_SIZE)

#define DEFINE_IMAGE_HEADER_FIELD(name) jint name;

/**
 *  ATTENTION: this struct and the below items must match
 *             'com.sun.max.vm.hosted.BootImage.Header'.
 */
typedef struct image_Header {
   IMAGE_HEADER_FIELDS(DEFINE_IMAGE_HEADER_FIELD)
} *image_Header;

/**
 * Must only be called after calling 'load_image()'.
 *
 * @return a pointer to information about the boot image as described in the struct above
 */
extern image_Header image_header(void);

typedef struct image_KeyValue {
    char *key;
    char *value;
} *image_KeyValue;

/*
 *  ATTENTION: this must match 'com.sun.max.vm.hosted.BootImage.StringInfo'.
 */
typedef struct image_StringInfo {
    int count;
    image_KeyValue* values;
} *image_StringInfo;

/**
 * Must only be called after calling 'load_image()'.
 *
 * @return a pointer to information about the boot image as described in the struct above
 */
extern image_StringInfo image_stringInfo(void);

/**
 * ATTENTION: this struct must match 'com.sun.max.vm.hosted.BootImage.Trailer'.
 */
typedef struct image_Trailer {
    jint randomID;
    jint bootImageFormatVersion;
    jint identification;
} *image_Trailer;

/**
 *  Read and verify the boot image file header, the string info section and the trailer,
 *  then verify these, then mmap the boot image, then relocate pointers in it.
 *
 *  Subsequently, after the string section:
 *   - relocation data
 *   - page padding
 *   - boot heap data
 *   - boot code data
 *
 * @param imageFileName full path of the boot image file
 */
extern int image_load(char *imageFileName);

/**
 * Must only be called after calling 'load_image()'.
 *
 * @return a pointer to the boot heap region
 */
extern Address image_heap(void);

/**
 * Must only be called after calling 'load_image()'.
 *
 * @return a pointer to the boot code region
 */
extern Address image_code(void);

/**
 * Must only be called after calling 'load_image()'.
 *
 * @return a pointer to the end of the boot code region
 */
extern Address image_code_end(void);


/**
 * Gets an address in the boot image based on a known offset in the image.
 *
 * Must only be called after calling 'load_image()'.
 *
 * @param type the type of the value
 * @param offset an offset in the image
 *
 * @return the effective address computed by 'image_heap() + offset', cast to 'type'
 */
#define image_offset_as_address(type, offset) ((type) (image_heap() + image_header()->offset))

/**
 * Reads a value from the boot image whose address is at a known offset from the start of the image.
 *
 * Must only be called after calling 'load_image()'.
 *
 * @param type the type of the value
 * @param offset the offset of the value. This denotes a member of the image_Header struct whose name end with "Offset".
 *
 * @return the value at 'image_heap() + offset', cast to 'type'
 */
#define image_read_value(type, offset) (*((type *) (image_heap() + image_header()->offset)))

/**
 * Writes a value in the boot image whose address is at a known offset from the start of the image.
 *
 * Must only be called after calling 'load_image()'.
 *
 * @param type the type of the value
 * @param offset the offset of the value. This denotes a member of the image_Header struct whose name end with "Offset".
 * @param value the value to write
 */
#define image_write_value(type, offset, value) do { \
    type *__fieldAddress = (type *) (image_heap() + image_header()->offset); \
    *__fieldAddress = value; \
} while(0)

#endif /*__image_h__*/
