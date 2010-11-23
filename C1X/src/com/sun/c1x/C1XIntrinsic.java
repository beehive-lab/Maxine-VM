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

import java.util.*;

import com.sun.cri.ci.*;
import com.sun.cri.ri.*;

class ClassNames {
    public static final String OBJECT_CLASS = "java.lang.Object";
    public static final String CLASS_CLASS = "java.lang.Class";
    public static final String STRING_CLASS = "java.lang.String";
    public static final String MATH_CLASS = "java.lang.Math";
    public static final String FLOAT_CLASS = "java.lang.Float";
    public static final String DOUBLE_CLASS = "java.lang.Double";
    public static final String INTEGER_CLASS = "java.lang.Integer";
    public static final String LONG_CLASS = "java.lang.Long";
    public static final String SYSTEM_CLASS = "java.lang.System";
    public static final String THREAD_CLASS = "java.lang.Thread";
    public static final String ARRAY_CLASS = "java.lang.reflect.Array";
    public static final String BUFFER_CLASS = "java.nio.Buffer";
    public static final String UNSAFE_CLASS = "sun.misc.Unsafe";
}

/**
 * This enum represents all of the intrinsics, i.e. a library methods that
 * are treated specially by the compiler. Note that the list includes more intrinsics
 * than are currently handled by C1X.
 *
 * @author Ben L. Titzer
 */
public enum C1XIntrinsic {

    // java.lang.Object
    java_lang_Object$init     (ClassNames.OBJECT_CLASS, "init", "()V"),
    java_lang_Object$hashCode (ClassNames.OBJECT_CLASS, "hashCode", "()I"),
    java_lang_Object$getClass (ClassNames.OBJECT_CLASS, "getClass", "()Ljava/lang/Class;"),
    java_lang_Object$clone    (ClassNames.OBJECT_CLASS, "clone", "()Ljava/lang/Object;"),

    // java.lang.Class
    java_lang_Class$isAssignableFrom (ClassNames.CLASS_CLASS, "isAssignableFrom", "(Ljava/lang/Class;)Z"),
    java_lang_Class$isInstance       (ClassNames.CLASS_CLASS, "isInstance", "(Ljava/lang/Object;)Z"),
    java_lang_Class$getModifiers     (ClassNames.CLASS_CLASS, "getModifiers", "()I"),
    java_lang_Class$isInterface      (ClassNames.CLASS_CLASS, "isInterface", "()Z"),
    java_lang_Class$isArray          (ClassNames.CLASS_CLASS, "isArray", "()Z"),
    java_lang_Class$isPrimitive      (ClassNames.CLASS_CLASS, "isPrimitive", "()Z"),
    java_lang_Class$getSuperclass    (ClassNames.CLASS_CLASS, "getSuperclass", "()Ljava/lang/Class;"),
    java_lang_Class$getComponentType (ClassNames.CLASS_CLASS, "getComponentType", "()Ljava/lang/Class;"),

    // java.lang.String
    java_lang_String$compareTo (ClassNames.STRING_CLASS, "compareTo", "(Ljava/lang/String;)I"),
    java_lang_String$indexOf   (ClassNames.STRING_CLASS, "indexOf", "(Ljava/lang/String;)I"),
    java_lang_String$equals    (ClassNames.STRING_CLASS, "equals", "(Ljava/lang/Object;)Z"),

    // java.lang.Math
    java_lang_Math$abs   (ClassNames.MATH_CLASS, "abs", "(D)D"),
    java_lang_Math$sin   (ClassNames.MATH_CLASS, "sin", "(D)D"),
    java_lang_Math$cos   (ClassNames.MATH_CLASS, "cos", "(D)D"),
    java_lang_Math$tan   (ClassNames.MATH_CLASS, "tan", "(D)D"),
    java_lang_Math$atan2 (ClassNames.MATH_CLASS, "atan2", "(DD)D"),
    java_lang_Math$sqrt  (ClassNames.MATH_CLASS, "sqrt", "(D)D"),
    java_lang_Math$log   (ClassNames.MATH_CLASS, "log", "(D)D"),
    java_lang_Math$log10 (ClassNames.MATH_CLASS, "log10", "(D)D"),
    java_lang_Math$pow   (ClassNames.MATH_CLASS, "pow", "(DD)D"),
    java_lang_Math$exp   (ClassNames.MATH_CLASS, "exp", "(D)D"),
    java_lang_Math$min   (ClassNames.MATH_CLASS, "min", "(II)I"),
    java_lang_Math$max   (ClassNames.MATH_CLASS, "max", "(II)I"),

    // java.lang.Float
    java_lang_Float$floatToRawIntBits (ClassNames.FLOAT_CLASS, "floatToRawIntBits", "(F)I"),
    java_lang_Float$floatToIntBits    (ClassNames.FLOAT_CLASS, "floatToIntBits", "(F)I"),
    java_lang_Float$intBitsToFloat    (ClassNames.FLOAT_CLASS, "intBitsToFloat", "(I)F"),

    // java.lang.Double
    java_lang_Double$doubleToRawLongBits (ClassNames.DOUBLE_CLASS, "doubleToRawLongBits", "(D)J"),
    java_lang_Double$doubleToLongBits    (ClassNames.DOUBLE_CLASS, "doubleToLongBits", "(D)J"),
    java_lang_Double$longBitsToDouble    (ClassNames.DOUBLE_CLASS, "longBitsToDouble", "(J)D"),

    // java.lang.Integer
    java_lang_Integer$bitCount     (ClassNames.INTEGER_CLASS, "bitCount", "(I)I"),
    java_lang_Integer$reverseBytes (ClassNames.INTEGER_CLASS, "reverseBytes", "(I)I"),

    // java.lang.Long
    java_lang_Long$bitCount     (ClassNames.LONG_CLASS, "bitCount", "(J)I"),
    java_lang_Long$reverseBytes (ClassNames.LONG_CLASS, "reverseBytes", "(J)J"),

    // java.lang.System
    java_lang_System$identityHashCode  (ClassNames.SYSTEM_CLASS, "identityHashCode", "(Ljava/lang/Object;)I"),
    java_lang_System$currentTimeMillis (ClassNames.SYSTEM_CLASS, "currentTimeMillis", "()J"),
    java_lang_System$nanoTime          (ClassNames.SYSTEM_CLASS, "nanoTime", "()J"),
    java_lang_System$arraycopy         (ClassNames.SYSTEM_CLASS, "arraycopy", "(Ljava/lang/Object;ILjava/lang/Object;II)V"),

    // java.lang.Thread
    java_lang_Thread$currentThread (ClassNames.THREAD_CLASS, "currentThread", "()Ljava/lang/Thread;"),

    // java.lang.reflect.Array
    java_lang_reflect_Array$getLength (ClassNames.ARRAY_CLASS, "getLength", "(Ljava/lang/Object;)I"),
    java_lang_reflect_Array$newArray  (ClassNames.ARRAY_CLASS, "newArray", "(Ljava/lang/Class;I)Ljava/lang/Object;"),

    // java.nio.Buffer
    java_nio_Buffer$checkIndex (ClassNames.BUFFER_CLASS, "checkIndex", "(I)I"),

    // sun.misc.Unsafe
    sun_misc_Unsafe$compareAndSwapObject (ClassNames.UNSAFE_CLASS, "compareAndSwapObject", "(Ljava/lang/Object;JLjava/lang/Object;Ljava/lang/Object;)Z"),
    sun_misc_Unsafe$compareAndSwapLong   (ClassNames.UNSAFE_CLASS, "compareAndSwapLong", "(Ljava/lang/Object;JJJ)Z"),
    sun_misc_Unsafe$compareAndSwapInt    (ClassNames.UNSAFE_CLASS, "compareAndSwapInt", "(Ljava/lang/Object;JII)Z");

    private static HashMap<String, HashMap<String, C1XIntrinsic>> intrinsicMap = new HashMap<String, HashMap<String, C1XIntrinsic>>(100);

    private final String className;
    private final String methodName;
    private final String signature;

    C1XIntrinsic(String className, String methodName, String signature) {
        // Check that enum names are according to convention.
        assert className.equals(name().substring(0, name().indexOf('$')).replace('_', '.'));
        assert methodName.equals(name().substring(name().indexOf('$') + 1));
        this.methodName = methodName;
        this.className = className;
        this.signature = signature;
    }

    /**
     * Gets the name of the class in which this method is declared.
     * @return the name of the class declaring this intrinsic method
     */
    public String className() {
        return className;
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
            String className = CiUtil.toInternalName(i.className());
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
     * (and is loaded); {@code null} otherwise
     */
    public static C1XIntrinsic getIntrinsic(RiMethod method) {
        RiType holder = method.holder();
        if (method.isResolved() && holder.isResolved() && holder.isInitialized()) {
            // note that the map uses internal names to make lookup faster
            HashMap<String, C1XIntrinsic> map = intrinsicMap.get(holder.name());
            if (map != null) {
                return map.get(method.name() + method.signature().asString());
            }
        }
        return null;
    }
}
