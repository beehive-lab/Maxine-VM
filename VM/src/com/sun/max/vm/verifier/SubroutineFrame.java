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
package com.sun.max.vm.verifier;

import static com.sun.max.vm.verifier.types.VerificationType.*;

import com.sun.max.vm.verifier.types.*;

/**
 * An object for encapsulating the linkage of nested subroutine calls.
 *
 * @author Doug Simon
 */
public class SubroutineFrame {

    /**
     * Shared object used to denote the result of merging two or more subroutines.
     */
    public static final Subroutine MERGED_SUBROUTINE = SUBROUTINE;

    /**
     * Constant denoting the top level caller of all subroutines which is not a subroutine itself.
     */
    public static final SubroutineCall TOP = new SubroutineCall(null, null, null);

    private final Subroutine _subroutine;
    private final int _depth;
    private final SubroutineFrame _parent;

    public SubroutineFrame(Subroutine subroutine, SubroutineFrame parent) {
        assert (subroutine != null && parent != null) || TOP == null;
        _subroutine = subroutine;
        _parent = parent;
        _depth = parent == null ? 0 : 1 + parent._depth;
    }

    /**
     * Determines if this subroutine context or any of its callers is for a given subroutine.
     */
    public boolean contains(Subroutine subroutine) {
        if (subroutine == _subroutine) {
            return true;
        }
        return _parent == null ? false : _parent.contains(subroutine);
    }

    /**
     * Gets the subroutine for which this context models a call to.
     * @return
     */
    public Subroutine subroutine() {
        return _subroutine;
    }

    public SubroutineFrame parent() {
        return _parent;
    }

    public int depth() {
        return _depth;
    }

    /**
     * Merges this context with another. If the result of the merge is identical to this context, then this context object is returned.
     */
    public SubroutineFrame merge(SubroutineFrame subroutineFrame) {
        assert depth() == subroutineFrame.depth();
        if (subroutineFrame == this) {
            return this;
        }
        final SubroutineFrame mergedParent = _parent.merge(subroutineFrame.parent());
        if (_subroutine != subroutineFrame.subroutine()) {
            if (_subroutine == MERGED_SUBROUTINE && mergedParent == _parent) {
                return this;
            }
            return new SubroutineFrame(MERGED_SUBROUTINE, mergedParent);
        }
        if (mergedParent == _parent) {
            return this;
        }
        return new SubroutineFrame(_subroutine, mergedParent);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder(_subroutine == null ? "method-entry-frame" : _subroutine.toString());
        if (_parent != null) {
            sb.append("\n    called from ").append(_parent.toString());
        }
        return sb.toString();
    }
}
