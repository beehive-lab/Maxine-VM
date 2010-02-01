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

import com.sun.max.program.*;

/**
 * A factory that permits subclasses of VmThread to be created. To create instances of a {@code VmThread} subclass,
 * the {@link #VMTHREAD_FACTORY_CLASS_PROPERTY_NAME} property needs to be defined at image build time.
 *
 * @author Mick Jordan
 * @author Doug Simon
 */
public class VmThreadFactory {

    /**
     * The name of the system property specifying a subclass of {@link VmThreadFactory} that is
     * to be instantiated and used at runtime to create VmThread instances. If not specified,
     * then a default factory is used that simply creates plain VmThread instances.
     */
    public static final String VMTHREAD_FACTORY_CLASS_PROPERTY_NAME = "max.vmthread.factory.class";

    private static final VmThreadFactory instance;

    static {
        final String factoryClassName = System.getProperty(VMTHREAD_FACTORY_CLASS_PROPERTY_NAME);
        if (factoryClassName == null) {
            instance = new VmThreadFactory();
        } else {
            try {
                instance = (VmThreadFactory) Class.forName(factoryClassName).newInstance();
            } catch (Exception exception) {
                throw ProgramError.unexpected("Error instantiating " + factoryClassName, exception);
            }
        }
    }

    /**
     * Subclasses override this method to instantiate objects of a VmThread subclass.
     *
     * @param javaThread the Java thread object to which the VM thread object is bound
     * @return a VmThread object implementing the VM specific semantics of the given Java thread
     */
    protected VmThread newVmThread(Thread javaThread) {
        return new VmThread(javaThread);
    }

    /**
     * Creates a VmThread object bound to a given Java thread object.
     *
     * @param javaThread the Java thread object to which the VM thread object is bound
     * @return a VmThread object implementing the VM specific semantics of the given Java thread
     */
    public static VmThread create(Thread javaThread) {
        return instance.newVmThread(javaThread);
    }
}
