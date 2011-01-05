/*
 * Copyright (c) 2007, 2008, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.jdwp.vm.proxy;

import com.sun.max.jdwp.vm.core.*;

/**
 * Class representing a reference type in the JDWP protocol.
 *
 * @author Thomas Wuerthinger
 *
 */
public interface ReferenceTypeProvider extends ObjectProvider {

    public final class ClassStatus {

        public static final int VERIFIED = 1;
        public static final int PREPARED = 2;
        public static final int INITIALIZED = 4;
        public static final int ERROR = 8;
    }

    @ConstantReturnValue
    VMValue.Type getType();

    int getStatus();

    @ConstantReturnValue
    int getFlags();

    @ConstantReturnValue
    String getSourceFileName();

    @ConstantReturnValue
    String getName();

    @ConstantReturnValue
    String getSignature();

    @ConstantReturnValue
    String getSignatureWithGeneric();

    @ConstantReturnValue
    ClassLoaderProvider classLoader();

    @ConstantReturnValue
    FieldProvider[] getFields();

    @ConstantReturnValue
    InterfaceProvider[] getImplementedInterfaces();

    @ConstantReturnValue
    MethodProvider[] getMethods();

    @ConstantReturnValue
    ReferenceTypeProvider[] getNestedTypes();

    @ConstantReturnValue
    ClassObjectProvider classObject();

    ObjectProvider[] getInstances();

    @ConstantReturnValue
    int majorVersion();

    @ConstantReturnValue
    int minorVersion();
}
