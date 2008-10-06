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
/*VCSID=8b34673c-72b0-4aa9-869b-c6bdba80d76a*/

package com.sun.max.vm.verifier.types;



/**
 * @author David Liu
 * @author Doug Simon
 */
public class Category1Type extends TopType {

    Category1Type() {
        assert CATEGORY1 == null || getClass() != Category1Type.class;
    }

    @Override
    public boolean isAssignableFromDifferentType(VerificationType from) {
        return from instanceof Category1Type;
    }

    @Override
    public final int size() {
        return 1;
    }

    @Override
    public String toString() {
        return "category1";
    }

    @Override
    public int classfileTag() {
        return -1;
    }
}
