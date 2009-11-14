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
 * Load and mmap the binary boot image of the VM.
 *
 * @author Bernd Mathiske
 */
#ifndef __image_h__
#define __image_h__ 1

#include "word.h"
#include "jni.h"

/**
 *  ATTENTION: this struct and the below items must match
 *             'com.sun.max.vm.prototype.BootImage.Header'.
 */
typedef struct image_Header {
    jint isBigEndian;        /* 0: LITTLE, anything else: BIG. Must be first. */

    jint identification;     /* Magic number that must be present and have the same value in every Maxine boot image file */
    jint version;            /* Version of boot image file format */
    jint randomID;           /* Specific to one particular instance of boot image file */

    jint wordSize;           /* 4 or 8 */
    jint cacheAlignment;

    jint pageSize;           /* multiple of 1024 */

    jint vmRunMethodOffset;
    jint vmThreadRunMethodOffset;
    jint vmThreadAttachMethodOffset;
    jint vmThreadDetachMethodOffset;
    jint runSchemeRunMethodOffset;

    jint classRegistryOffset;

    jint stringDataSize;
    jint relocationDataSize;

    jint heapSize;     /* multiple of 'pageSize' */
    jint codeSize;     /* multiple of 'pageSize' */
    jint codeCacheSize;    /* multiple of 'pageSize' */

    jint heapRegionsPointerOffset;

    /* Some extra space that the substrate allocates by malloc().
     * Used e.g. for the primordial card table. */
    jint auxiliarySpaceSize;

    /* See the comment for the 'info' static field in the MaxineMessenger class. */
    jint messengerInfoOffset;

    /* See the comment for the 'threadLocalsListHead' field in the VmThreadMap class. */
    jint threadLocalsListHeadOffset;

    jint primordialThreadLocalsOffset;

    /* The storage size of one set of VM thread locals. */
    jint threadLocalsSize;

    /* The storage size of a Java frame anchor. */
    jint javaFrameAnchorSize;

    /* The indexes of the VM thread locals accessed directly by C code. */
    jint SAFEPOINT_LATCH;
    jint SAFEPOINTS_ENABLED_THREAD_LOCALS;
    jint SAFEPOINTS_DISABLED_THREAD_LOCALS;
    jint SAFEPOINTS_TRIGGERED_THREAD_LOCALS;
    jint NATIVE_THREAD_LOCALS;
    jint FORWARD_LINK;
    jint BACKWARD_LINK;
    jint ID;
    jint JNI_ENV;
    jint LAST_JAVA_FRAME_ANCHOR;
    jint TRAP_NUMBER;
    jint TRAP_INSTRUCTION_POINTER;
    jint TRAP_FAULT_ADDRESS;
    jint TRAP_LATCH_REGISTER;

} *image_Header;

/**
 * Must only be called after calling 'load_image()'.
 *
 * @return a pointer to information about the boot image as described in the struct above
 */
extern image_Header image_header(void);

/*
 *  ATTENTION: this must match 'com.sun.max.vm.prototype.BootImage.StringInfo'.
 */
typedef struct image_StringInfo {
    char *buildLevel;
    char *processorModel;
    char *instructionSet;
    char *operatingSystem;

    char *gripPackageName;
    char *referencePackageName;
    char *layoutPackageName;
    char *heapPackageName;
    char *monitorPackageName;
    char *compilerPackageName;
    char *jitPackageName;
    char *trampolinePackageName;
    char *targetABIsPackageName;
    char *runPackageName;
} *image_StringInfo;

/**
 * Must only be called after calling 'load_image()'.
 *
 * @return a pointer to information about the boot image as described in the struct above
 */
extern image_StringInfo image_stringInfo(void);

/**
 * ATTENTION: this struct must match 'com.sun.max.vm.prototype.BootImage.Trailer'.
 */
typedef struct image_Trailer {
    jint randomID;
    jint version;
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
