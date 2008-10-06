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
/*VCSID=8756e6eb-98ae-4405-bded-9dde401c0118*/
package com.sun.max.vm.jni;

import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;

/**
 * @see FieldID
 * @see MethodID
 * 
 * @author Bernd Mathiske
 */
public abstract class MemberID extends Word {

    static final int NUMBER_OF_MEMBER_BITS = 16;
    static final int MEMBER_INDEX_MASK = 0x0000ffff;

    public static Word create(int holderID, int memberIndex) {
        assert (0 <= memberIndex) && (memberIndex <= MEMBER_INDEX_MASK);
        final Address word = Address.fromInt(holderID).shiftedLeft(NUMBER_OF_MEMBER_BITS);
        return word.or(memberIndex);
    }

    public static Word create(MemberActor memberActor) {
        return create(memberActor.holder().id(), memberActor.memberIndex());
    }

    @INLINE
    protected final ClassActor getHolder() {
        return ClassID.toClassActor(asAddress().unsignedShiftedRight(NUMBER_OF_MEMBER_BITS).toInt());
    }

    @INLINE
    protected final int getMemberIndex() {
        final Address word = asAddress().and(MEMBER_INDEX_MASK);
        return word.toInt();
    }
}
