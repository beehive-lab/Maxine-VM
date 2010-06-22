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
package com.sun.max.util;

/**
 * Java enums are insufficient in that their ordinals have to be successive.
 * An Enumerable has an additional arbitrary int "value",
 * which may incur gaps between ordinal-successive Enumerables.
 * <p>
 * An Enumerator can be called upon to provide the respective Enumerable matching a given value.
 * <p>
 * See <a href="http://www.ejournal.unam.mx/cys/vol07-02/CYS07205.pdf">"Inheritance, Generics and Binary Methods in Java"</a>
 * for an explanation of how to interpret a recursive generic type.
 * <p>
 *
 * @see Enumerator
 *
 * @author Bernd Mathiske
 */
public interface Enumerable<E extends Enum<E> & Enumerable<E>> extends Symbol {

    // We are merely declaring this method to lock in the same parameter type for the corresponding enumerator,
    // not for any actual use
    Enumerator<E> enumerator();

}
