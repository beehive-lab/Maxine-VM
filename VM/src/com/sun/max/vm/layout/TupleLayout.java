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
/*VCSID=62a1103f-ba2b-46dc-aee6-0c85199ac4eb*/
package com.sun.max.vm.layout;

import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;

/**
 * @author Bernd Mathiske
 */
public interface TupleLayout extends SpecificLayout {

    int getFieldOffsetInCell(FieldActor fieldActor);

    /**
     * Determine offsets for the given field actors.
     * Update each field actor with its offset.
     * 
     * @param superClassActor super class that we inherit already laid out fields from
     * @param fieldActors field actors that will have their offsets assigned
     * @return the resulting object size (including header and fields)
     */
    Size layoutFields(ClassActor superClassActor, FieldActor[] fieldActors);
}
