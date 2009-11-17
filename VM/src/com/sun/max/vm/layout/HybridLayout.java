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
package com.sun.max.vm.layout;

import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.layout.Layout.*;

/**
 * A hub is like a tuple and a word array at the same time.
 * Fields and word array elements overlap.
 * All fields are concentrated in a bounded area of the word array,
 * which starts at array offset zero.
 * There is a fixed array index from which on there are no more fields.
 *
 * @author Bernd Mathiske
 */
public interface HybridLayout extends TupleLayout, WordArrayLayout {

    int firstAvailableWordArrayIndex(Size tupleSize);

    /**
     * The resulting size must be aligned with a word array size.
     */
    Size layoutFields(ClassActor superClassActor, FieldActor[] fieldActors);

    /**
     * Gets the header fields of this hybrid object layout.
     *
     * @return an array of header field descriptors sorted by ascending order of the field addresses in memory
     */
    HeaderField[] headerFields();
}
