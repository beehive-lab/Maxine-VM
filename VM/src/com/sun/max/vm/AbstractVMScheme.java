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
package com.sun.max.vm;

import com.sun.max.*;
import com.sun.max.lang.*;
import com.sun.max.program.*;

/**
 * @author Bernd Mathiske
 */
public abstract class AbstractVMScheme extends AbstractScheme {

    private final VMConfiguration vmConfiguration;
    private final Class<? extends VMScheme> specification;

    public VMConfiguration vmConfiguration() {
        return vmConfiguration;
    }

    protected AbstractVMScheme(VMConfiguration vmConfiguration) {
        this.vmConfiguration = vmConfiguration;
        Class<? extends VMScheme> specification = null;
        Class<?> implementation = getClass();
        ProgramError.check(VMScheme.class.isAssignableFrom(implementation), "Subclass of " + AbstractVMScheme.class + " must implement " + VMScheme.class + ": " + implementation);
        final Class<Class<? extends VMScheme>> type = null;
        Class<? extends VMScheme> last = StaticLoophole.cast(type, implementation);
        while (!implementation.equals(AbstractVMScheme.class) && specification == null) {
            for (Class<?> interfaceClass : implementation.getInterfaces()) {
                if (!VMScheme.class.equals(interfaceClass) && VMScheme.class.isAssignableFrom(interfaceClass)) {
                    specification = StaticLoophole.cast(type, interfaceClass);
                    break;
                }
            }
            implementation = implementation.getSuperclass();
            if (!VMScheme.class.equals(implementation) && VMScheme.class.isAssignableFrom(implementation)) {
                last = StaticLoophole.cast(type, implementation);
            }
        }
        if (specification == null) {
            specification = last;
        }
        ProgramError.check(specification != null, "Cannot find specification for scheme implemented by " + getClass());
        this.specification = specification;
    }

    public Class<? extends VMScheme> specification() {
        return specification;
    }

    public void initialize(MaxineVM.Phase phase) {
        // default: do nothing.
    }

    public void finalize(MaxineVM.Phase phase) {
        // default: do nothing.
    }

}
