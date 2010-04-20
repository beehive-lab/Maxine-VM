/*
 * Copyright (c) 2010 Sun Microsystems, Inc.  All rights reserved.
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

import com.sun.max.platform.*;

/**
 * This annotation is used to filter out classes, methods and fields from the boot image
 * based on the target platform. For example, to specify that a method should only be
 * included in an image built for SPARC platform, this annotation is used as follows:
 * <pre>
 *     @PLATFORM(cpu = "sparc")
 *     public native void flushRegisterWindows();
 * </pre>
 *
 * The value of each annotation element is a string used to match against the
 * named platform component. The {@link Platform#isAcceptedBy(PLATFORM)} method
 * performs the test.
 *
 * @author Doug Simon
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE, ElementType.CONSTRUCTOR, ElementType.FIELD})
public @interface PLATFORM {
    /**
     * Specifies an operating system constraint on the annotated element.
     * Example values are {@code "!windows"} and {@code "darwin"}.
     */
    String os() default "";

    /**
     * Specifies a processor model constraint on the annotated element.
     * Example values are {@code "(amd64|ia32)"}, {@code "sparcv9"}, {@code "!arm.*"}.
     */
    String cpu() default "";
}
