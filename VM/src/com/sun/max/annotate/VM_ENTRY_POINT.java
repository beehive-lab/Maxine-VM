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
package com.sun.max.annotate;

import java.lang.annotation.*;

import com.sun.max.vm.jni.*;

/**
 * Denotes a method that is a VM entry point (e.g. a {@linkplain JniFunctions JNI function}.
 * These must be <i>static</i> methods and are compiled so as to
 * conform to the native ABI (e.g. implement callee saved registers if necessary) so that it can be called as
 * a C function pointer.
 *
 * No parameter type or return type of VM entry or exit points may refer to object references - only primitive Java
 * values and 'Word' values are allowed.
 *
 * @author Bernd Mathiske
 * @author Doug Simon
 * @author Mick Jordan
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface VM_ENTRY_POINT {
}
