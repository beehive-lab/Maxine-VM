/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.tele.object;

import java.util.logging.*;

import com.sun.max.jdwp.vm.proxy.*;
import com.sun.max.tele.*;
import com.sun.max.vm.reference.*;

/**
 *
 * Class representing a reference to an array class in the {@link TeleVM}.
 *
 * @author Michael Van De Vanter
 * @author Thomas Wuerthinger
 */
public class TeleArrayClassActor extends TeleReferenceClassActor implements ArrayTypeProvider {

    private static final Logger LOGGER = Logger.getLogger(TeleArrayClassActor.class.getName());

    protected TeleArrayClassActor(TeleVM teleVM, Reference referenceClassActorReference) {
        super(teleVM, referenceClassActorReference);
    }

    public ReferenceTypeProvider elementType() {
        return vm().classRegistry().findTeleClassActor(this.classActor().componentClassActor().typeDescriptor);
    }

    public ArrayProvider newInstance(int length) {
        // TODO: Implement the creation of objects in the target VM.
        LOGGER.warning("New instance of length " + length + " requested for array " + this + ", returning null");
        return null;
    }
}
