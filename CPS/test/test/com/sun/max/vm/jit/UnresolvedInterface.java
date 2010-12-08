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
package test.com.sun.max.vm.jit;

/**
 * The purpose of this interface is solely for testing cases JIT-compilation of bytecode with operands to interfaces that aren't resolved
 * at compile-time. We need to use a interface different than UnresolvedAtCompileTimeInterface  to make sure the patching of literal references to
 * the class by the template-based JIT compiler are effective. The interface also declare several methods so that testing can verify that
 * an interface index different from the one in the template is being set in place in the copy of the template emitted by the JIT.
 *
 * @author Laurent Daynes
 */
public interface UnresolvedInterface {
    void parameterlessUnresolvedInterfaceMethod0();
    void parameterlessUnresolvedInterfaceMethod1();
    void parameterlessUnresolvedInterfaceMethod();
    void unresolvedInterfaceMethod(int i1, int i2);
}
