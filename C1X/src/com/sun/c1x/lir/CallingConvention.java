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
package com.sun.c1x.lir;

import com.sun.cri.ci.*;

/**
 * This class represents a calling convention instance for a particular method invocation and describes the ABI for
 * outgoing arguments and the return value, both runtime calls and Java calls.
 *
 * @author Marcelo Cintra
 * @author Thomas Wuerthinger
 */
public class CallingConvention {

    public final int overflowArgumentSize;
    public final CiValue[] locations;
    public final CiValue[] operands;

    CallingConvention(CiValue[] locations, CiTarget target) {
        this.locations = locations;
        this.operands = new CiValue[locations.length];
        int outgoing = 0;
        for (int i = 0; i < locations.length; i++) {
            CiValue l = locations[i];
            operands[i] = l;
            if (l.isAddress()) {
                CiAddress s = (CiAddress) l;
                int spillSize = target.spillSlotSize * target.spillSlots(l.kind);
                outgoing = Math.max(outgoing, s.displacement + spillSize);
            }
        }

        overflowArgumentSize = outgoing;
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        result.append("CallingConvention[");
        for (CiValue op : operands) {
            result.append(op.toString()).append(" ");
        }
        result.append("]");
        return result.toString();
    }

}
