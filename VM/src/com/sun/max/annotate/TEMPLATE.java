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

import com.sun.max.vm.template.*;

/**
 * Declares that a class is a source for bytecode templates (i.e., all its methods are templates for bytecodes).
 * All methods within the class inherit the annotations of the enclosing templates class.
 *
 * TODO from Laurent: This is probably not the best way to do this (see comment below).
 * Right now, the only purpose of the template is to limit changes to the current compiler infrastructure:
 * this enable the AMD64EirGenerator to check for the presence of the TEMPLATE annotation to decide
 * that a AMD64EirBytecodeTemplate should be used instead of a AMD64EirCompiledMethod.
 * This choice drives other choice in the upper layer of the compiler (e.g., no prologue/epilogue, different ABI,
 * decoration of target code with dependencies, etc.).
 * This annotation makes testing the condition simpler, although one could have test more directly, e.g., by testing if the holder
 * of the compiled method is a subclass of some "templates source" class.
 * The alternative to testing for the TEMPLATE annotation (or other test directly in the AMDEirGenerator) would be making
 * specific hierarchy of compilers, irgenerators etc... which requires creating a lot more infrastructure.
 *
 * @author Laurent Daynes
 * @author Aziz Ghuloum
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface TEMPLATE {
    TemplateChooser.Initialized initialized() default TemplateChooser.Initialized.NO_ASSUMPTION;
    TemplateChooser.Resolved resolved() default TemplateChooser.Resolved.NO_ASSUMPTION;
    TemplateChooser.Instrumented instrumented() default TemplateChooser.Instrumented.NO;
    TemplateChooser.Traced traced() default TemplateChooser.Traced.NO;
}
