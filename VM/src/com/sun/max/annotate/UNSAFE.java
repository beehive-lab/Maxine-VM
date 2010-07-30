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

/**
 * Methods with this annotation must be compiled with the bootstrap compiler, not the JIT.
 * Neither can they be interpreted.
 * <p>
 * This is the case when a method uses values of type {@link Word} or
 * any constant folding or dead code elimination must take place
 * before the code makes sense in the target VM.
 * <p>
 * Most of these methods are recognized automatically.
 * Only those not captured during {@linkplain Intrinsics#run() intrinsification}
 * need to be annotated.
 * <p>
 * Some other annotations imply UNSAFE:
 * <ul>
 * <li>{@link BUILTIN}</li>
 * <li>{@link C_FUNCTION}</li>
 * <li>{@link VM_ENTRY_POINT}</li>
 * <li>{@link ACCESSOR}</li>
 * <li>{@link SUBSTITUTE}: the substitutee is unsafe</li>
 * <li>{@link LOCAL_SUBSTITUTION}: the substitutee is unsafe</li>
 * </ul>
 * <p>
 * However, some must be pointed out manually with this annotation.
 *
 * @see ClassfileReader
 *
 * @author Bernd Mathiske
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
public @interface UNSAFE {
}
