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
/*VCSID=5c0897af-e03f-471a-add1-6d2c568de993*/

package com.sun.max.vm.verifier.types;


/**
 * @author David Liu
 * @author Doug Simon
 */
public class Category2Type extends TopType {

    Category2Type() {
        assert CATEGORY2 == null || getClass() != Category2Type.class;
    }

    @Override
    public boolean isAssignableFromDifferentType(VerificationType from) {
        return from instanceof Category2Type;
    }

    @Override
    public final int size() {
        return 2;
    }

    @Override
    public int classfileTag() {
        return -1;
    }

    @Override
    public boolean isCategory2() {
        return true;
    }

    @Override
    public String toString() {
        return "category2";
    }
}
