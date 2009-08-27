/*
 * Copyright (c) 2009 Sun Microsystems, Inc.  All rights reserved.
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
package com.sun.c1x.ir;

import com.sun.c1x.ri.*;
import com.sun.c1x.value.*;

/**
 * An instruction that represents the runtime resolution of a Java class object. For example, an
 * ldc of a class constant that is unresolved.
 *
 * @author Ben L. Titzer
 * @author Thomas Wuerthinger
 */
public class ResolveClass extends StateSplit {

    public final RiType type;
    public final RiType.Representation portion;
    public final RiConstantPool constantPool;
    public final char cpi;

    public ResolveClass(RiType type, RiType.Representation r, ValueStack stateBefore, char cpi, RiConstantPool constantPool) {
        super(type.getBasicType(r), stateBefore);
        this.portion = r;
        this.type = type;
        this.cpi = cpi;
        this.constantPool = constantPool;
        setFlag(Flag.NonNull);
        assert stateBefore != null : "resolution must record state";
    }

    @Override
    public void accept(InstructionVisitor v) {
        v.visitResolveClass(this);
    }

    @Override
    public boolean canTrap() {
        return true;
    }

    @Override
    public int valueNumber() {
        return 0x50000000 | type.hashCode();
    }

    @Override
    public boolean valueEqual(Instruction x) {
        if (x instanceof ResolveClass) {
            ResolveClass r = (ResolveClass) x;
            return r.portion == portion && r.type.equals(type);
        }
        return false;
    }
}
