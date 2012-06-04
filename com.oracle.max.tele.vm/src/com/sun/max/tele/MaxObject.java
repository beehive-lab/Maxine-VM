/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.tele;

import com.sun.max.tele.memory.*;
import com.sun.max.tele.object.*;
import com.sun.max.tele.object.TeleObject.ObjectKind;
import com.sun.max.tele.reference.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.layout.*;
import com.sun.max.vm.layout.Layout.HeaderField;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;

/**
 * Access to an object in the VM via a surrogate that may have specialized knowledge of how to use
 * the information held by the object.  This is especially important for objects that hold significant
 * meta-information needed to understand other aspects of the VM's runtime state.
 */
public interface MaxObject {

    /**
     * @return to which of the Maxine heap object representations does this surrogate refer?
     */
    ObjectKind kind();

    /**
     * Gets the reference to the object in the VM represented by this instance.  Note that
     * in the refresh cycle, the reference will be updated before this instance is updated, so that
     * the {@link RemoteReference#status()} can be relied upon after heap references have been
     * updated, even if there is uncertainty whether any cached information about the object
     * has been refreshed yet.
     *
     * @return canonical reference to this object in the VM, never {@code null}.
     */
    RemoteReference reference();

    /**
     * Gets the {@linkplain RemoteObjectStatus status} of the {@link RemoteReference} held by this remote
     * {@linkplain MaxObject object instance}.
     * <p>
     * Note that during the normal refresh cycle, all instances of {@link RemoteReference}, including the one held here,
     * will be updated before any instance of {@link MaxObject}. As a consequence, this method can be relied upon
     * during the object-updating part of the refresh cycle, even if this particular instance has not yet been updated.
     * This is important when dealing with circularities.
     *
     * @return the {@linkplain RemoteObjectStatus status} of the {@link RemoteReference} held by this instance, independent of
     *         any other state cached by this instance.
     */
    RemoteObjectStatus status();

    /**
     * The current "origin" of the object in VM memory, or (if the object status is {@linkplain RemoteObjectStatus#DEAD DEAD})
     * the origin at which the object was list object status is {@linkplain RemoteObjectStatus#LIVE LIVE}
     * <p>
     * Note that the origin is not necessarily beginning of the object's memory allocation, depending on the particular
     * object layout used.
     *
     * @return current absolute location of the object's origin, subject to change by GC.
     *
     * @see GeneralLayout
     */
    Pointer origin();

    /**
     * Gets the current area of memory in which the object is stored.
     *
     * @return current memory region occupied by this object in the VM,
     * subject to relocation by GC, {@code null} if object is {@linkplain RemoteObjectStatus#DEAD DEAD}.
     */
    TeleFixedMemoryRegion objectMemoryRegion();

    /**
     * @return a number that uniquely identifies this object in the VM for the duration of the inspection
     */
    long getOID();

    /**
     * Gets a local {@link ClassActor}, equivalent to the one in the
     * VM that describes the type of this object in the VM. Note that
     * in the singular instance of {@link StaticTuple} this does not
     * correspond to the actual type of the object, which is an
     * exceptional Maxine object that has no ordinary Java type; it
     * returns in this case the type of the class the tuple helps implement.
     */
    ClassActor classActorForObjectType();

    /**
     * return local surrogate for the{@link ClassMethodActor} associated with this object in the VM, either because it
     * is a {@link ClassMethodActor} or because it is a class closely associated with a method that refers to a
     * {@link ClassMethodActor}. Null otherwise.
     */
    TeleClassMethodActor getTeleClassMethodActorForObject();

    /**
     * Gets the "hub" object pointed to in the object's header.
     *
     * @return the local surrogate for the Hub of this object
     */
    TeleHub getTeleHub();

    /**
     * Gets the contents of the misc" word in the object's header.
     *
     * @return the "misc" word from the header of this object in the VM
     */
    Word readMiscWord();

    /**
     * The fields in the object's header.
     *
     * @return enumeration of the fields in the header of this object
     */
    HeaderField[] headerFields();

    /**
     * The type of a field in the obejct's header, calling out specially
     * the standard ones. Unknown ones are treated as words.
     *
     * @param headerField identifies a header field in the object layout
     * @return the type of the header field, Word if unknown.
     */
    TypeDescriptor headerType(HeaderField headerField);

    /**
     * The memory region in which an object header field is stored, subject to change by GC relocation.
     *
     * @param headerField a field in the object's header
     * @return current memory region occupied by a header field in this object in the VM
     */
    TeleFixedMemoryRegion headerMemoryRegion(HeaderField headerField);

    /**
     * Address of a field in the object's header.
     *
     * @param headerField identifies a header field in the object layout
     * @return the location of the header in VM memory
     */
    Address headerAddress(HeaderField headerField);

    /**
     * Offset from the object's origin of a field in the object's header.
     *
     * @param headerField identifies a header field in the object layout
     * @return the location of the header field relative to object origin
     */
    int headerOffset(HeaderField headerField);

    /**
     * The memory region in which field in the object is stored, subject to change by GC relocation.
     *
     * @param fieldActor a field in the object
     * @return current memory region occupied by the field in this object in the VM, subject to relocation by GC.
     */
    TeleFixedMemoryRegion fieldMemoryRegion(FieldActor fieldActor);

    /**
     * @param fieldActor local {@link FieldActor}, part of the
     * {@link ClassActor} for the type of this object, that
     * describes a field in this object in the VM
     *
     * @return contents of the designated field in this VM object
     */
    Value readFieldValue(FieldActor fieldActor);

    /**
     * @return a short string describing the role played by this object if it is of special interest in the Maxine
     *         implementation, null if any other kind of object.
     */
    String maxineRole();

    /**
     * @return an extremely short, abbreviated version of the string {@link #maxineRole()}, describing the role played
     *         by this object in just a few characters.
     */
    String maxineTerseRole();

    @Deprecated
    TeleObject getForwardedTeleObject();

}
