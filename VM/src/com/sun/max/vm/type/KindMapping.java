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
package com.sun.max.vm.type;

/**
 * A variation of the visitor pattern for {@link Kind}s. An implementation of this
 * interface is used to attain a "canonical" value for a given kind. For example,
 * an implementation may return the {@link BoxedZeroKindMapping canonical zero value}
 * for each kind.
 *
 * @author Bernd Mathiske
 */
public interface KindMapping<Result_Type> {

    Result_Type mapVoid();

    Result_Type mapByte();

    Result_Type mapBoolean();

    Result_Type mapShort();

    Result_Type mapChar();

    Result_Type mapInt();

    Result_Type mapFloat();

    Result_Type mapLong();

    Result_Type mapDouble();

    Result_Type mapWord();

    Result_Type mapReference();

}
