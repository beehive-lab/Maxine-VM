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
/*VCSID=d9d34178-b436-4231-b11e-62c495f26a76*/
package com.sun.max.vm.verifier.types;


/**
 * @author Doug Simon
 */
public final class Long2Type extends TopType {

    Long2Type() {
        // Ensures that only the one singleton instance of this class is created.
        assert LONG2 == null;
    }

    @Override
    public boolean isAssignableFromDifferentType(VerificationType from) {
        assert this != from;
        return false;
    }

    @Override
    public String toString() {
        return "long2";
    }

    @Override
    public boolean isSecondWordType() {
        return true;
    }

    @Override
    public int size() {
        return 1;
    }
}
