/*
 * Copyright (c) 2009 Sun Microsystems, Inc.  All rights reserved.
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
package com.sun.c1x;

import com.sun.c1x.ci.CiMethod;
import com.sun.c1x.ci.CiType;
import com.sun.c1x.util.Util;

import java.util.HashMap;

/**
 * The <code>C1XIntrinsic</code> class represents an intrinsic, i.e. a library method that
 * is treated specially by the compiler. Note that the list includes more intrinsics
 * than are currently handled by C1X.
 *
 * @author Ben L. Titzer
 */
public enum C1XIntrinsic {

    // java.lang.Object
    java_lang_Object$init     ("()V"),
    java_lang_Object$hashCode ("()I"),
    java_lang_Object$getClass ("()Ljava/lang/Class;"),
    java_lang_Object$clone    ("()Ljava/lang/Object;"),

    // java.lang.Class
    java_lang_Class$isAssignableFrom ("(Ljava/lang/Class;)Z"),
    java_lang_Class$isInstance       ("(Ljava/lang/Object;)Z"),
    java_lang_Class$getModifiers     ("()I"),
    java_lang_Class$isInterface      ("()Z"),
    java_lang_Class$isArray          ("()Z"),
    java_lang_Class$isPrimitive      ("()Z"),
    java_lang_Class$getSuperclass    ("()Ljava/lang/Class;"),
    java_lang_Class$getComponentType ("()Ljava/lang/Class;"),

    // java.lang.String
    java_lang_String$compareTo ("(Ljava/lang/String;)I"),
    java_lang_String$indexOf   ("(Ljava/lang/String;)I"),
    java_lang_String$equals    ("(Ljava/lang/Object;)Z"),

    // java.lang.Math
    java_lang_Math$abs   ("(D)D"),
    java_lang_Math$sin   ("(D)D"),
    java_lang_Math$cos   ("(D)D"),
    java_lang_Math$tan   ("(D)D"),
    java_lang_Math$atan2 ("(DD)D"),
    java_lang_Math$sqrt  ("(D)D"),
    java_lang_Math$log   ("(D)D"),
    java_lang_Math$log10 ("(D)D"),
    java_lang_Math$pow   ("(DD)D"),
    java_lang_Math$exp   ("(D)D"),
    java_lang_Math$min   ("(II)I"),
    java_lang_Math$max   ("(II)I"),

    // java.lang.Float
    java_lang_Float$floatToRawIntBits ("(F)I"),
    java_lang_Float$floatToIntBits    ("(F)I"),
    java_lang_Float$intBitsToFloat    ("(I)F"),

    // java.lang.Double
    java_lang_Double$doubleToRawLongBits ("(D)J"),
    java_lang_Double$doubleToLongBits    ("(D)J"),
    java_lang_Double$longBitsToDouble    ("(J)D"),

    // java.lang.Integer
    java_lang_Integer$bitCount     ("(I)I"),
    java_lang_Integer$reverseBytes ("(I)I"),

    // java.lang.Long
    java_lang_Long$bitCount     ("(J)I"),
    java_lang_Long$reverseBytes ("(J)J"),

    // java.lang.System
    java_lang_System$identityHashCode  ("(Ljava/lang/Object;)I"),
    java_lang_System$currentTimeMillis ("()J"),
    java_lang_System$nanoTime          ("()J"),
    java_lang_System$arraycopy         ("(Ljava/lang/Object;ILjava/lang/Object;II)V"),

    // java.lang.Thread
    java_lang_Thread$isInterrupted ("()Z"),
    java_lang_Thread$currentThread ("()Ljava/lang/Thread;"),

    // java.lang.Throwable
    java_lang_Throwable$fillInStackTrace ("()Ljava/lang/Throwable;"),

    // java.util.Arrays
    java_util_Arrays$copyOf ("([Ljava/lang/Object;ILjava/lang/Class;)[Ljava/lang/Object;"),
    java_util_Arrays$copyOfRange ("([Ljava/lang/Object;IILjava/lang/Class;)[Ljava/lang/Object;"),
    java_util_Arrays$equals ("([C[C)Z"),

    // java.lang.reflect.Array
    java_lang_reflect_Array$getLength ("(Ljava/lang/Object;)I"),
    java_lang_reflect_Array$newArray  ("(Ljava/lang/Class;I)Ljava/lang/Object;"),

    // java.lang.reflect.Method
    java_lang_reflect_Method$invoke ("(Ljava/lang/Object;[Ljava/lang/Object)Ljava/lang/Object"),

    // java.nio.Buffer
    java_nio_Buffer$checkIndex ("(I)I"),

    // sun.reflect.Reflection
    sun_reflect_Reflection$getClassAccessFlags ("(Ljava/lang/Class;)I"),
    sun_reflect_Reflection$getCallerClass      ("(I)Ljava/lang/Class;"),

    // sun.misc.AtomicLongCSImpl
    sun_misc_AtomicLongCSImpl$get ("()J"),
    sun_misc_AtomicLongCSImpl$attemptUpdate ("(JJ)Z"),

    // sun.misc.Unsafe
    sun_misc_Unsafe$allocateInstance ("(Ljava/lang/Class;)Ljava/lang/Object;"),
    sun_misc_Unsafe$copyMemory       ("(Ljava/lang/Object;JLjava/lang/Object;JJ)V"),
    sun_misc_Unsafe$park             ("(ZJ)V"),
    sun_misc_Unsafe$unpark           ("(Ljava/lang/Object;)V"),
    // unsafe object access
    sun_misc_Unsafe$getObject        ("(Ljava/lang/Object;J)Ljava/lang/Object;"),
    sun_misc_Unsafe$getBoolean       ("(Ljava/lang/Object;J)Z"),
    sun_misc_Unsafe$getByte          ("(Ljava/lang/Object;J)B"),
    sun_misc_Unsafe$getShort         ("(Ljava/lang/Object;J)S"),
    sun_misc_Unsafe$getChar          ("(Ljava/lang/Object;J)C"),
    sun_misc_Unsafe$getInt           ("(Ljava/lang/Object;J)I"),
    sun_misc_Unsafe$getLong          ("(Ljava/lang/Object;J)J"),
    sun_misc_Unsafe$getFloat         ("(Ljava/lang/Object;J)F"),
    sun_misc_Unsafe$getDouble        ("(Ljava/lang/Object;J)D"),
    sun_misc_Unsafe$putObject        ("(Ljava/lang/Object;JLjava/lang/Object;)V"),
    sun_misc_Unsafe$putBoolean       ("(Ljava/lang/Object;JZ)V"),
    sun_misc_Unsafe$putByte          ("(Ljava/lang/Object;JB)V"),
    sun_misc_Unsafe$putShort         ("(Ljava/lang/Object;JS)V"),
    sun_misc_Unsafe$putChar          ("(Ljava/lang/Object;JC)V"),
    sun_misc_Unsafe$putInt           ("(Ljava/lang/Object;JI)V"),
    sun_misc_Unsafe$putLong          ("(Ljava/lang/Object;JJ)V"),
    sun_misc_Unsafe$putFloat         ("(Ljava/lang/Object;JF)V"),
    sun_misc_Unsafe$putDouble        ("(Ljava/lang/Object;JD)V"),
    // unsafe volatile object access
    sun_misc_Unsafe$getObjectVolatile        ("(Ljava/lang/Object;J)Ljava/lang/Object;"),
    sun_misc_Unsafe$getBooleanVolatile       ("(Ljava/lang/Object;J)Z"),
    sun_misc_Unsafe$getByteVolatile          ("(Ljava/lang/Object;J)B"),
    sun_misc_Unsafe$getShortVolatile         ("(Ljava/lang/Object;J)S"),
    sun_misc_Unsafe$getCharVolatile          ("(Ljava/lang/Object;J)C"),
    sun_misc_Unsafe$getIntVolatile           ("(Ljava/lang/Object;J)I"),
    sun_misc_Unsafe$getLongVolatile          ("(Ljava/lang/Object;J)J"),
    sun_misc_Unsafe$getFloatVolatile         ("(Ljava/lang/Object;J)F"),
    sun_misc_Unsafe$getDoubleVolatile        ("(Ljava/lang/Object;J)D"),
    sun_misc_Unsafe$putObjectVolatile        ("(Ljava/lang/Object;JLjava/lang/Object;)V"),
    sun_misc_Unsafe$putBooleanVolatile       ("(Ljava/lang/Object;JZ)V"),
    sun_misc_Unsafe$putByteVolatile          ("(Ljava/lang/Object;JB)V"),
    sun_misc_Unsafe$putShortVolatile         ("(Ljava/lang/Object;JS)V"),
    sun_misc_Unsafe$putCharVolatile          ("(Ljava/lang/Object;JC)V"),
    sun_misc_Unsafe$putIntVolatile           ("(Ljava/lang/Object;JI)V"),
    sun_misc_Unsafe$putLongVolatile          ("(Ljava/lang/Object;JJ)V"),
    sun_misc_Unsafe$putFloatVolatile         ("(Ljava/lang/Object;JF)V"),
    sun_misc_Unsafe$putDoubleVolatile        ("(Ljava/lang/Object;JD)V"),
    // unsafe raw access (note these overload methods above)
    sun_misc_Unsafe$getObject_raw        ("getObject", "(JJ)Ljava/lang/Object;"),
    sun_misc_Unsafe$getBoolean_raw       ("getBoolean", "(JJ)Z"),
    sun_misc_Unsafe$getByte_raw          ("getByte", "(JJ)B"),
    sun_misc_Unsafe$getShort_raw         ("getShort", "(JJ)S"),
    sun_misc_Unsafe$getChar_raw          ("getChar", "(JJ)C"),
    sun_misc_Unsafe$getInt_raw           ("getInt", "(JJ)I"),
    sun_misc_Unsafe$getLong_raw          ("getLong", "(JJ)J"),
    sun_misc_Unsafe$getFloat_raw         ("getFloat", "(JJ)F"),
    sun_misc_Unsafe$getDouble_raw        ("getDouble", "(JJ)D"),
    sun_misc_Unsafe$putObject_raw        ("putObject", "(JJLjava/lang/Object;)V"),
    sun_misc_Unsafe$putBoolean_raw       ("putBoolean", "(JJZ)V"),
    sun_misc_Unsafe$putByte_raw          ("putByte", "(JJB)V"),
    sun_misc_Unsafe$putShort_raw         ("putShort", "(JJS)V"),
    sun_misc_Unsafe$putChar_raw          ("putChar", "(JJC)V"),
    sun_misc_Unsafe$putInt_raw           ("putInt", "(JJI)V"),
    sun_misc_Unsafe$putLong_raw          ("putLong", "(JJJ)V"),
    sun_misc_Unsafe$putFloat_raw         ("putFloat", "(JJF)V"),
    sun_misc_Unsafe$putDouble_raw        ("putDouble", "(JJD)V"),
    // unsafe atomic operations
    sun_misc_Unsafe$compareAndSwapObject ("(Ljava/lang/Object;JLjava/lang/Object;Ljava/lang/Object;)Z"),
    sun_misc_Unsafe$compareAndSwapLong   ("(Ljava/lang/Object;JJJ)Z"),
    sun_misc_Unsafe$compareAndSwapInt    ("(Ljava/lang/Object;JII)Z"),
    sun_misc_Unsafe$putObjectOrdered     ("(Ljava/lang/Object;JLjava/lang/Object;)V"),
    sun_misc_Unsafe$putLongOrdered       ("(Ljava/lang/Object;JJ)V"),
    sun_misc_Unsafe$putIntOrdered        ("(Ljava/lang/Object;JI)V"),
    // prefetch operations
    sun_misc_Unsafe$prefetchRead         ("(Ljava/lang/Object;J)V"),
    sun_misc_Unsafe$prefetchWrite        ("(Ljava/lang/Object;J)V"),
    sun_misc_Unsafe$prefetchReadStatic   ("(Ljava/lang/Object;J)V"),
    sun_misc_Unsafe$prefetchWriteStatic  ("(Ljava/lang/Object;J)V");

    private static HashMap<String, HashMap<String, C1XIntrinsic>> intrinsicMap = new HashMap<String, HashMap<String, C1XIntrinsic>>(100);

    private final String className;
    private final String simpleClassName;
    private final String methodName;
    private final String signature;

    C1XIntrinsic(String signature) {
        this(null, signature);
    }

    C1XIntrinsic(String methodName, String signature) {
        String name = name();
        // parse the class name from the name of this enum
        int index = name.indexOf('$');
        assert index != -1;
        this.className = name.substring(0, index).replace('_', '.');
        this.methodName = methodName == null ? name.substring(index + 1) : methodName;
        this.signature = signature;
        index = className.lastIndexOf('.');
        if (index == -1) {
            this.simpleClassName = className;
        } else {
            this.simpleClassName = className.substring(index + 1);
        }
    }

    /**
     * Gets the name of the class in which this method is declared.
     * @return the name of the class declaring this intrinsic method
     */
    public String className() {
        return className;
    }

    /**
     * Gets the {@linkplain Class#getSimpleName() simple} name of the class in which this method is declared.
     * @return the simple name of the class declaring this intrinsic method
     */
    public String simpleClassName() {
        return simpleClassName;
    }

    /**
     * Gets the name of this intrinsic method.
     * @return the name of this method
     */
    public String methodName() {
        return methodName;
    }

    /**
     * Gets the signature of this intrinsic method as a string.
     * @return the signature
     */
    public String signature() {
        return signature;
    }

    static {
        // iterate through all the intrinsics and add them to the map
        for (C1XIntrinsic i : C1XIntrinsic.values()) {
            // note that the map uses internal names to map lookup faster
            String className = Util.toInternalName(i.className());
            HashMap<String, C1XIntrinsic> map = intrinsicMap.get(className);
            if (map == null) {
                map = new HashMap<String, C1XIntrinsic>();
                intrinsicMap.put(className, map);
            }
            map.put(i.methodName() + i.signature(), i);
        }
    }

    /**
     * Looks up an intrinsic for the specified method.
     * @param method the compiler interface method
     * @return a reference to the intrinsic for the method, if the method is an intrinsic
     * (and is loaded); <code>null</code> otherwise
     */
    public static C1XIntrinsic getIntrinsic(CiMethod method) {
        CiType holder = method.holder();
        if (method.isLoaded() && holder.isLoaded() && holder.isInitialized()) {
            // note that the map uses internal names to map lookup faster
            HashMap<String, C1XIntrinsic> map = intrinsicMap.get(holder.name());
            if (map != null) {
                return map.get(method.name() + method.signatureType().asString());
            }
        }
        return null;
    }
}
