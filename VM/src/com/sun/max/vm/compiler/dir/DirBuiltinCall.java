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
/*VCSID=9a6f539f-2f64-4584-a1c9-206a0f7f7c42*/
package com.sun.max.vm.compiler.dir;

import com.sun.max.vm.compiler.builtin.*;
import com.sun.max.vm.compiler.dir.transform.*;

/**
 * A call to a compiler builtin.
 *
 * @author Bernd Mathiske
 */
public class DirBuiltinCall extends DirCall {

    private final Builtin _builtin;

    /**
     * @param result the variable which will hold the result of the builtin execution
     * @param builtin the compiler builtin
     * @param arguments arguments for the builtin
     * @param catchBlock the block where execution continues in case of an (implicit) exception thrown by the builtin
     */
    public DirBuiltinCall(DirVariable result, Builtin builtin, DirValue[] arguments, DirCatchBlock catchBlock) {
        super(result, arguments, catchBlock);
        _builtin = builtin;
    }

    public Builtin builtin() {
        return _builtin;
    }

    @Override
    public boolean isEquivalentTo(DirInstruction other, DirBlockEquivalence dirBlockEquivalence) {
        if (other instanceof DirBuiltinCall) {
            final DirBuiltinCall dirBuiltinCall = (DirBuiltinCall) other;
            if (_builtin != dirBuiltinCall._builtin) {
                return false;
            }
            assert arguments().length == dirBuiltinCall.arguments().length;
            for (int i = 0; i < arguments().length; i++) {
                if (!arguments()[i].equals(dirBuiltinCall.arguments()[i])) {
                    return false;
                }
            }
            if (catchBlock() == null) {
                return dirBuiltinCall.catchBlock() == null;
            }
            if (dirBuiltinCall.catchBlock() == null) {
                return false;
            }
            return catchBlock().isEquivalentTo(dirBuiltinCall.catchBlock(), dirBlockEquivalence);
        }
        return false;
    }

    @Override
    public int hashCodeForBlock() {
        return super.hashCodeForBlock() ^ _builtin.serial();
    }

    @Override
    public String toString() {
        String arguments = "";
        String separator = "";
        for (int i = 0; i < arguments().length; i++) {
            arguments += separator + arguments()[i];
            separator = " ";
        }
        String s = _builtin.toString() + "(" + arguments + ")";
        if (result() != null) {
            s = result().toString() + " := " +  s;
        }
        if (catchBlock() != null) {
            s += " -> #" + catchBlock().serial();
        }
        return s;
    }

    @Override
    public void acceptVisitor(DirVisitor visitor) {
        visitor.visitBuiltinCall(this);
    }
}
