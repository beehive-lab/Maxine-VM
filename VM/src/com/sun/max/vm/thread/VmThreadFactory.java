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
package com.sun.max.vm.thread;

import java.lang.reflect.*;
import com.sun.max.annotate.*;
import com.sun.max.program.*;

/**
 * A factory that permits subclasses of VmThread to be created for, e.g. GuestVM.
 * The choice of class is controlled by a system property and is defined at image-build-time.
 *
 * @author Mick Jordan
 */
public class VmThreadFactory {

    private static final String VMTHREADCLASS_PROPERTY_NAME = "max.vmthreadclass";
    private static Constructor<?> _vmThreadConstructor;

    static {
        checkVmThreadClass();
    }

    @PROTOTYPE_ONLY
    private static void checkVmThreadClass() {
        final String vmThreadClassName = System.getProperty(VMTHREADCLASS_PROPERTY_NAME);
        if (vmThreadClassName != null) {
            try {
                final Class<?> vmThreadClass = Class.forName(vmThreadClassName);
                final Class[] partypes = new Class[1];
                partypes[0] = Class.forName("java.lang.Thread");
                _vmThreadConstructor = vmThreadClass.getConstructor(partypes);
            } catch (Throwable e) {
                ProgramError.unexpected("Error instantiating max.vmthreadclass " + vmThreadClassName, e);
            }
        }
    }

    public static VmThread create(Thread thread) {
        if (_vmThreadConstructor == null) {
            return new VmThread(thread);
        }
        final Object[] arglist = new Object[1];
        arglist[0] = thread;
        try {
            return (VmThread) _vmThreadConstructor.newInstance(arglist);
        } catch (Exception e) {
            ProgramError.unexpected(e);
            return null;
        }
    }
}
