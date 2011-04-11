/*
 * Copyright (c) 2008, 2009, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
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
