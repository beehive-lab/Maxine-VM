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
package com.sun.max.vm.compiler.cir.optimize;

import java.lang.reflect.*;

/**
 * A checked exception that wraps an exception thrown when
 * {@linkplain CirFoldable#fold(CirOptimizer, com.sun.max.vm.compiler.cir.CirValue...) folding} a
 * {@linkplain CirFoldable foldable} CIR node. In general, a foldable CIR node should be able to determine whether is
 * will fold without an exception for a given set of arguments. However, guaranteeing this may make the test too
 * expensive for the common case. For example, a CIR node may indicate that it is foldable if a given set of arguments
 * are all constants even the folding may also fail if one of those arguments is null. Testing for null adds more time
 * to the common case (e.g. the CIR node for an {@code arraylength} operation) where the arguments do not contain a null
 * value.
 *
 * @author Doug Simon
 */
public class CirFoldingException extends InvocationTargetException {

    public CirFoldingException(Throwable cause) {
        super(cause);
    }
}
