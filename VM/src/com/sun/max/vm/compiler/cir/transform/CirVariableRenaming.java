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
/*VCSID=9a10449f-9a4d-4c10-a062-f28d199f788f*/
package com.sun.max.vm.compiler.cir.transform;

import com.sun.max.program.*;
import com.sun.max.vm.compiler.cir.variable.*;

/**
 * Simple variable environment for renaming purposes.
 * 
 * @author Bernd Mathiske
 */
public final class CirVariableRenaming {
    private final CirVariableRenaming _parent;

    public CirVariableRenaming parent() {
        return _parent;
    }

    private final CirVariable _from;

    public CirVariable from() {
        return _from;
    }

    private final CirVariable _to;

    public CirVariable to() {
        return _to;
    }

    public CirVariableRenaming(CirVariableRenaming parent, CirVariable from, CirVariable to) {
        _parent = parent;
        _from = from;
        _to = to;
    }

    public CirVariable find(CirVariable from) {
        CirVariableRenaming r = this;
        do {
            if (from == r.from()) {
                return r.to();
            }
            r = r.parent();
        } while (r != null);
        return null;
    }

    @Override
    public String toString() {
        String s = "";
        String separator = "";
        CirVariableRenaming renaming = this;
        do {
            s += separator + renaming._from + "->" + renaming._to;
            separator = " ";
            renaming = renaming._parent;
        } while (renaming != null);
        return s;
    }

    public void trace(int level) {
        if (Trace.hasLevel(level)) {
            CirVariableRenaming renaming = this;
            do {
                Trace.line(level, renaming._from + "->" + renaming._to);
                renaming = renaming._parent;
            } while (renaming != null);
        }
    }
}
