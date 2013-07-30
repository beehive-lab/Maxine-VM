/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.max.vm.ext.graal;

import com.sun.max.program.*;


/**
 * A factory that permits subclasses of MaxGraal to be created. To create instances of a {@code MaxGraal} subclass,
 * the {@link #MAXGRAAL_FACTORY_CLASS_PROPERTY_NAME} property needs to be defined at image build time.
 */
public class MaxGraalFactory {
    /**
     * The name of the system property specifying a subclass of {@link MaxGraalFactory} that is
     * to be instantiated and used at runtime to create MaxGraal instances. If not specified,
     * then a default factory is used that simply creates plain MaxGraal instances.
     */
    public static final String MAXGRAAL_FACTORY_CLASS_PROPERTY_NAME = "max.graal.factory.class";

    private static final MaxGraalFactory instance;

    static {
        final String factoryClassName = System.getProperty(MAXGRAAL_FACTORY_CLASS_PROPERTY_NAME);
        if (factoryClassName == null) {
            instance = new MaxGraalFactory();
        } else {
            try {
                instance = (MaxGraalFactory) Class.forName(factoryClassName).newInstance();
            } catch (Exception exception) {
                throw ProgramError.unexpected("Error instantiating " + factoryClassName, exception);
            }
        }
    }

    /**
     * Subclasses override this method to instantiate objects of a MaxGraal subclass.
     *
     */
    protected MaxGraal newMaxGraal() {
        return new MaxGraal();
    }

    /**
     * Creates a {@link MaxGraal} instance.
     */
    public static MaxGraal create() {
        return instance.newMaxGraal();
    }

}
