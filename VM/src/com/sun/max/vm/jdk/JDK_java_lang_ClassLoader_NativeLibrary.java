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
package com.sun.max.vm.jdk;

import com.sun.max.annotate.*;
import com.sun.max.lang.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.jni.*;

/**
 * Implements method substitutions necessary for native library handling.
 * This class makes substitutions for methods of a non-public inner class of
 * {@link java.lang.ClassLoader ClassLoader} called {@code NativeLibrary}.
 * Thus, it is only used internally in the JDK to handle loading and linking of
 * native libraries and symbols.
 */
@METHOD_SUBSTITUTIONS(value = ClassLoader.class, innerClass = "NativeLibrary")
class JDK_java_lang_ClassLoader_NativeLibrary {

    /**
     * The JDK implementation of this class has a non-public field called "handle" of
     * type {@code long} that is used to store a pointer (the handle) to the native
     * library. The {@code FieldActor} for that field is used here to gain access.
     */
    @CONSTANT_WHEN_NOT_ZERO
    private static LongFieldActor _handleFieldActor;

    /**
     * Apparently the body of this method is bootstrap-sensitive depending on your write barrier.
     * Keeping it never inlined breaks the potential circular dependency.
     */
    @NEVER_INLINE
    private static LongFieldActor getHandleFieldActor(Class javaClass) {
        if (_handleFieldActor == null) {
            _handleFieldActor =  (LongFieldActor) FieldActor.fromJava(Classes.getDeclaredField(javaClass, "handle", long.class));
        }
        return _handleFieldActor;
    }

    /**
     * Gets the field actor for the "handle" field.
     * @return the field actor for reading the "handle" field from the underlying native library object
     */
    @INLINE
    private LongFieldActor handleFieldActor() {
        return getHandleFieldActor(getClass());
    }

    /**
     * Loads a library with the give absolute path.
     * @see java.lang.ClassLoader.NativeLibrary#load(String)
     * @param absolutePathname the pathname to the file that is a native library
     */
    @SUBSTITUTE
    void load(String absolutePathname) {
        handleFieldActor().writeLong(this, DynamicLinker.load(absolutePathname).asAddress().toLong());
    }

    /**
     * Looks up a symbol in this native library.
     * @see java.lang.ClassLoader.NativeLibrary#load(String)
     * @param symbolName the name of the symbol as a string
     * @return a pointer to the native symbol encoded as a long; {@code 0L} if the symbol is not found
     */
    @SUBSTITUTE
    long find(String symbolName) {
        return DynamicLinker.lookupSymbol(Address.fromLong(handleFieldActor().readLong(this)), symbolName).asAddress().toLong();
    }

    /**
     * Unloads this native library.
     * @see java.lang.ClassLoader.NativeLibrary#unload()
     */
    @SUBSTITUTE
    void unload() {
        DynamicLinker.close(Address.fromLong(handleFieldActor().readLong(this)));
    }

}
