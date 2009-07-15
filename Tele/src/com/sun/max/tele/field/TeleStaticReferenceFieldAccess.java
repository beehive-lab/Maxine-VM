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
package com.sun.max.tele.field;

import com.sun.max.program.*;
import com.sun.max.tele.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.type.*;

/**
 * @author Bernd Mathiske
 */
public class TeleStaticReferenceFieldAccess extends TeleStaticFieldAccess {

    public TeleStaticReferenceFieldAccess(Class holder, String name, Class type) {
        super(holder, name, Kind.fromJava(type));
        ProgramError.check(ClassActor.fromJava(type).isAssignableFrom(fieldActor().descriptor().resolve(fieldActor().holder().classLoader)), "field has wrong type: " + name + " in class: " + holder);
    }

    public Reference readReference(TeleVM teleVM) {
        return staticTupleReference(teleVM).readReference(fieldActor().offset());
    }

    /**
     * Read a reference as a Word from the field in the tele VM.
     * ATTENTION: an implicit type cast happens via remote access.
     */
    public Word readWord(TeleVM teleVM) {
        return staticTupleReference(teleVM).readWord(fieldActor().offset());
    }

    /**
     * Write a word as a reference into the field in the tele VM.
     * ATTENTION: an implicit type cast happens via remote access.
     */
    public void writeWord(TeleVM teleVM, Word value) {
        staticTupleReference(teleVM).writeWord(fieldActor().offset(), value);
    }
}
