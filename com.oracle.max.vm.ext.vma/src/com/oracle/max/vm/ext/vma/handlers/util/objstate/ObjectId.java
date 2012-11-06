/*
 * Copyright (c) 2011, 2012, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.max.vm.ext.vma.handlers.util.objstate;

import com.sun.max.vm.jni.*;
import com.sun.max.vm.reference.*;

/**
 * Support for unique identifiers for objects, using the {@link ObjectState} word.
 * Separate methods are defined for "seen" and "unseen" objects, of which the former
 * are expected to dominate. A "seen" object is one that the advice handler observed the
 * creation of through one of the {@code NEW} bytecodes. An "unseen" object is one
 * that is passed as an argument to a handler, but its creation was not advised.
 */
public interface ObjectId {
    /**
     * Create and assign a unique id for a "seen" object.
     */
    ObjectID assignId(Object obj);

    /**
     * Create and assign a unique id for a "seen" object via its reference.
     */
    ObjectID assignId(Reference objRef);

    /**
     * Create a unique id for an "unseen" object.
     */
    ObjectID assignUnseenId(Object obj);

    /**
     * Return the unique id for given object or zero if {@code obj == null}.
     * @param obj
     */
    ObjectID readId(Object obj);

    /**
     * Allows the "id" field to be used for arbitrary purposes.
     */
    void writeID(Object obj, ObjectID id);

}
