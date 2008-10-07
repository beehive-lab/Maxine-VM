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
/*VCSID=ed51c39b-5099-4aa2-857f-b5842d8118db*/
package com.sun.max.vm.compiler.cir.transform;

import com.sun.max.vm.compiler.cir.*;

/**
 * Traverses a CIR graph,
 * visiting each node exactly once,
 * in some undefined order,
 * calling the appropriate CirVisitor routine for each node.
 * 
 * @author Bernd Mathiske
 */
public class CirVisitingTraversal extends CirTraversal {

    public CirVisitingTraversal(CirNode node) {
        super(node);
    }

    public void run(CirVisitor visitor) {
        while (!_toDo.isEmpty()) {
            final CirNode node = _toDo.removeFirst();
            node.acceptVisitor(visitor);
            node.acceptVisitor(this);
        }
    }

    public static void apply(CirNode node, CirVisitor visitor) {
        final CirVisitingTraversal traversal = new CirVisitingTraversal(node);
        traversal.run(visitor);
    }

}
