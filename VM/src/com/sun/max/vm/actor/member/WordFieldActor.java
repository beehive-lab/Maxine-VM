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
/*VCSID=d910f059-57a6-4bdd-8064-bc43a9ebb0d0*/
package com.sun.max.vm.actor.member;

import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.object.*;
import com.sun.max.vm.object.host.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;

/**
 * @author Bernd Mathiske
 */
public class WordFieldActor extends FieldActor<WordValue> {

    public WordFieldActor(Utf8Constant name, TypeDescriptor descriptor, int flags) {
        super(Kind.WORD,
              name,
              descriptor,
              flags);
    }

    @INLINE
    public final Word readWord(Object object) {
        return TupleAccess.readWord(object, offset());
    }

    @INLINE
    public final void writeWord(Object object, Word value) {
        if (MaxineVM.isPrototyping()) {
            // We need to execute this during IR interpretation when setting resolution guards for methods:
            HostTupleAccess.writeWord(object, this, value);
        } else {
            TupleAccess.writeWord(object, offset(), value);
        }
    }

}
