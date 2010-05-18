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
 * Every thus annotated method is to be inlined unconditionally by the VM's optimizing compiler
 * and the receiver is never null-checked.
 *
 * This annotation exists primarily for annotating methods that <b>must</b> be inlined
 * for semantic reasons as opposed to those that could be inlined for performance reasons.
 * Using this annotation for the latter should be done very rarely and only when
 * profiling highlights a performance bottleneck or such a bottleneck is known <i>a priori</i>.
 *
 * If the {@linkplain #override() override} element of this annotation is set to true, then
 * an annotated method must be {@code static} or {@code final} (implicitly or explicitly).
 * Additionally, only a INLINE virtual method with this element set to true can be overridden.
 *
 * @author Bernd Mathiske
 * @author Doug Simon
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface INLINE {

    /**
     * If true, this element specifies that the annotated method provides the prototypical implementation
     * of the functionality expected (in the target VM) of every method that overrides
     * the annotated method. That is, the code produced by the compiler for every overriding method
     * must be functionality equivalent to the code produced for the prototypical method.
     *
     * <b>WARNING: Setting this element to true implies that you guarantee that all overriders
     * satisfy the above stated invariant.</b> There is no (easy) way to test for violations of
     * the invariant.
     *
     * A method annotated with INLINE should only be overridden for one of the following reasons:
     *
     *  1. To coerce the value returned by the overridden method to a subtype.
     *     See {@link MemberActor#holder()} and {@link FieldActor#holder()} for an example.
     *
     *  2. A method is overridden to make bootstrapping work.
     *     See {@link ClassActor#toJava()} and {@link ArrayClassActor#toJava()} for an example.
     *
     *  3. A method is overridden because a subclass can provide a more efficient implementation
     *     and it is known that certain call sites will be reduced to a constant receiver
     *     (not just a known receiver type but a known receiver object) via annotation driven
     *     compile-time {@linkplain FOLD folding}. This is how calls to the {@link GeneralLayout layout}
     *     interface methods are reduced to monomorphic calls at compile-time.
     *
     *     See {@link ClassActor#toJava()} and {@link ArrayClassActor#toJava()} for an example.
     */
    boolean override() default false;

    /**
     * If true, this element specifies that inlining of the thus annotated method is only allowed
     * after snippet compilation has concluded.
     *
     * @see CompilerScheme#areSnippetsCompiled()
     */
    boolean afterSnippetsAreCompiled() default false;
}
