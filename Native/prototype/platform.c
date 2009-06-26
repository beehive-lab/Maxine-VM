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
#include <unistd.h>
#include "word.h"
#include "isa.h"
#include "jni.h"

/*
 *  ATTENTION: return value must correspond to an OperatingSystem enum value.
 */
JNIEXPORT jobject JNICALL
Java_com_sun_max_vm_prototype_Prototype_nativeGetOperatingSystem(JNIEnv *env, jclass c)
{
#   if os_DARWIN
        return (*env)->NewStringUTF(env, "DARWIN");
#   elif os_LINUX
        return (*env)->NewStringUTF(env, "LINUX");
#   elif os_SOLARIS
        return (*env)->NewStringUTF(env, "SOLARIS");
#   elif os_WINDOWS
        return (*env)->NewStringUTF(env, "WINDOWS");
#   elif os_GUESTVMXEN
        return (*env)->NewStringUTF(env, "GUESTVM");
#   else
#       error
#   endif
}

JNIEXPORT jint JNICALL
Java_com_sun_max_vm_prototype_Prototype_nativeGetPageSize(JNIEnv *env, jclass c) {
    return (jint) sysconf(_SC_PAGESIZE);
}

/*
 *  ATTENTION: return value must correspond to a ProcessorModel enum value or null.
 *  See 'Prototype.createHostPlatform()' for the meaning of a null return value.
 */
JNIEXPORT jobject JNICALL
Java_com_sun_max_vm_prototype_Prototype_nativeGetProcessorModel(JNIEnv *env, jclass c)
{
    return NULL;
}

/*
 *  ATTENTION: return value must correspond to an InstructionSet enum value.
 */
JNIEXPORT jobject JNICALL
Java_com_sun_max_vm_prototype_Prototype_nativeGetInstructionSet(JNIEnv *env, jclass c)
{
#   if isa_AMD64
	    return (*env)->NewStringUTF(env, "AMD64");
#   elif isa_IA32
	    return (*env)->NewStringUTF(env, "IA32");
#   elif isa_POWER
	    return (*env)->NewStringUTF(env, "PPC");
#   elif isa_SPARC
	    return (*env)->NewStringUTF(env, "SPARC");
#   else
#       error
#   endif
}

JNIEXPORT jboolean JNICALL
Java_com_sun_max_vm_prototype_Prototype_nativeIsBigEndian(JNIEnv *env, jclass c)
{
    return word_BIG_ENDIAN;
}

JNIEXPORT jint JNICALL
Java_com_sun_max_vm_prototype_Prototype_nativeGetWordWidth(JNIEnv *env, jclass c)
{
    return word_64_BITS ? 64 : 32;
}
