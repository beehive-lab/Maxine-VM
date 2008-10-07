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
/*VCSID=f1fed7c2-3f26-4b36-837d-fce608e9a09c*/
package com.sun.max.vm.compiler.cir.optimize;

import com.sun.max.vm.compiler.cir.*;

/**
 * A builtin or a method that can be folded (aka meta-evaluated).
 * 
 * "Folded" means rewritten in a strictly monotonic direction,
 * i.e. repeated folding does come to a deterministic end.
 * So this does not necessarily mean "'constant'-folded" and
 * on the other hand, it is neither as general as merely "rewritten".
 *
 * @author Bernd Mathiske
 */
public interface CirFoldable {

    boolean isFoldable(CirOptimizer cirOptimizer, CirValue[] arguments);

    CirCall fold(CirOptimizer cirOptimizer, CirValue... arguments);

}
