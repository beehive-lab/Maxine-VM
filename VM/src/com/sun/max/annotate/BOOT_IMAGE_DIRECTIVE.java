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
 * Use to indicate various directives to the boot image generator to include or exclude a method from the boot image.
 * This is currently used to experiment with JIT and may be removed in the long run.
 *
 * @author Laurent Daynes
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface BOOT_IMAGE_DIRECTIVE {
    /**
     * Indicate whether the method (and its callees) should be compiled with the JIT compiler. Default is false (i.e., it
     * must be compiled with the optimizing compiler).
     * @return
     */
    boolean useJitCompiler() default false;

    /**
     * Indicate whether the method should be excluded from the boot image. Convenience for testing runtime compilations.
     * @return
     */
    boolean exclude() default false;

    /**
     * Indicate whether the method should be left unlinked in the boot image. Convenience for testing runtime method linking mechanisms
     * (e.g., trampolines).
     */
    boolean keepUnlinked() default false;
}
