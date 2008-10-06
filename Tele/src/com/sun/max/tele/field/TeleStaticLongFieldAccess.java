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
/*VCSID=e533fc62-04b6-45fb-800e-a60342862653*/
package com.sun.max.tele.field;

import com.sun.max.tele.*;
import com.sun.max.vm.type.*;

/**
 * @author Doug Simon
 */
public class TeleStaticLongFieldAccess extends TeleStaticFieldAccess {

    public TeleStaticLongFieldAccess(Class holder, String name) {
        super(holder, name, Kind.LONG);
    }

    public long readLong(TeleVM teleVM) {
        return staticTupleReference(teleVM).readLong(fieldActor().offset());
    }

    public void writeLong(TeleVM teleVM, long value) {
        staticTupleReference(teleVM).writeLong(fieldActor().offset(), value);
    }
}
