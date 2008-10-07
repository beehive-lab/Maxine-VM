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
/*VCSID=3e2d78ed-7a50-46cb-aa6c-e8cdf6c310c9*/
/**
 * @author Bernd Mathiske
 */
#include "jni.h"
#include "word.h"

JNIEXPORT jobject JNICALL 
Java_com_sun_max_unsafe_UnsafeLoophole_nativeCastObject(JNIEnv *env, jclass c, jobject type, jobject object) 
{
  return object;
}

JNIEXPORT jobject JNICALL 
Java_com_sun_max_unsafe_UnsafeLoophole_nativeIntToWord(JNIEnv *env, jclass c, jobject type, jint value) 
{
#if word_32_BITS
    return (jobject) value;
#else
    return (jobject) (jlong) value;
#endif    
}

JNIEXPORT jobject JNICALL 
Java_com_sun_max_unsafe_UnsafeLoophole_nativeLongToWord(JNIEnv *env, jclass c, jobject type, jlong value) 
{
#if word_32_BITS
	return (jobject) (jint) value;
#else
    return (jobject) value;
#endif
}

JNIEXPORT jint JNICALL 
Java_com_sun_max_unsafe_UnsafeLoophole_nativeWordToInt(JNIEnv *env, jclass c, jobject value) 
{
#if word_32_BITS
	return (jint) value;
#else
    return (jint) (jlong) value;
#endif
}

JNIEXPORT jlong JNICALL 
Java_com_sun_max_unsafe_UnsafeLoophole_nativeWordToLong(JNIEnv *env, jclass c, jobject value) 
{
#if word_32_BITS
	return (jlong) (jint) value;
#else
    return (jlong) value;
#endif
}

JNIEXPORT jboolean JNICALL 
Java_com_sun_max_unsafe_UnsafeLoophole_nativeByteToBoolean(JNIEnv *env, jclass c, jbyte value) 
{
    return (jboolean) value;
}

JNIEXPORT jbyte JNICALL 
Java_com_sun_max_unsafe_UnsafeLoophole_nativeBooleanToByte(JNIEnv *env, jclass c, jboolean value) 
{
    return (jbyte) value;
}

JNIEXPORT jchar JNICALL 
Java_com_sun_max_unsafe_UnsafeLoophole_nativeShortToChar(JNIEnv *env, jclass c, jshort value) 
{
  union {
      jshort shortValue;
      jchar charValue;
  } result;
  result.shortValue = value;
  return result.charValue;
}

JNIEXPORT jshort JNICALL 
Java_com_sun_max_unsafe_UnsafeLoophole_nativeCharToShort(JNIEnv *env, jclass c, jchar value) 
{
  union {
      jchar charValue;
      jshort shortValue;
  } result;
  result.charValue = value;
  return result.shortValue;
}

JNIEXPORT jfloat JNICALL 
Java_com_sun_max_unsafe_UnsafeLoophole_nativeIntToFloat(JNIEnv *env, jclass c, jint value) 
{
  union {
      jint intValue;
      jfloat floatValue;
  } result;
  result.intValue = value;
  return result.floatValue;
}

JNIEXPORT jint JNICALL 
Java_com_sun_max_unsafe_UnsafeLoophole_nativeFloatToInt(JNIEnv *env, jclass c, jfloat value) 
{
  union {
      jfloat floatValue;
      jint intValue;
  } result;
  result.floatValue = value;
  return result.intValue;
}

JNIEXPORT jdouble JNICALL 
Java_com_sun_max_unsafe_UnsafeLoophole_nativeLongToDouble(JNIEnv *env, jclass c, jlong value) 
{
  union {
      jlong longValue;
      jdouble doubleValue;
  } result;
  result.longValue = value;
  return result.doubleValue;
}

JNIEXPORT jlong JNICALL 
Java_com_sun_max_unsafe_UnsafeLoophole_nativeDoubleToLong(JNIEnv *env, jclass c, jdouble value) 
{
  union {
      jdouble doubleValue;
      jlong longValue;
  } result;
  result.doubleValue = value;
  return result.longValue;
}

JNIEXPORT jobject JNICALL 
Java_com_sun_max_unsafe_UnsafeLoophole_nativeReferenceToWord(JNIEnv *env, jclass c, jobject object) 
{
  return object;
}

JNIEXPORT jobject JNICALL 
Java_com_sun_max_unsafe_UnsafeLoophole_nativeWordToReference(JNIEnv *env, jclass c, jobject object) 
{
  return object;
}

JNIEXPORT jobject JNICALL 
Java_com_sun_max_unsafe_UnsafeLoophole_nativeWordToObject(JNIEnv *env, jclass c, jobject object) 
{
  return object;
}

JNIEXPORT jobject JNICALL 
Java_com_sun_max_unsafe_UnsafeLoophole_nativeGripToWord(JNIEnv *env, jclass c, jobject object) 
{
  return object;
}

JNIEXPORT jobject JNICALL 
Java_com_sun_max_unsafe_UnsafeLoophole_nativeWordToGrip(JNIEnv *env, jclass c, jobject object) 
{
  return object;
}

JNIEXPORT jobject JNICALL 
Java_com_sun_max_unsafe_UnsafeLoophole_nativeGripToReference(JNIEnv *env, jclass c, jobject object) 
{
  return object;
}

JNIEXPORT jobject JNICALL 
Java_com_sun_max_unsafe_UnsafeLoophole_nativeReferenceToGrip(JNIEnv *env, jclass c, jobject object) 
{
  return object;
}

JNIEXPORT jobject JNICALL 
Java_com_sun_max_unsafe_UnsafeLoophole_nativeWordCast(JNIEnv *env, jclass c, jobject type, jobject object) 
{
  return object;
}
