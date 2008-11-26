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
package com.sun.max.tele.debug;

import com.sun.max.tele.*;

/**
 * Controls the tracing level in the tele VM.
 *
 * @author Michael Van De Vanter
 */
public final class TeleVMTrace extends TeleVMHolder {

    public TeleVMTrace(TeleVM teleVM) {
        super(teleVM);
    }

    public int readTraceLevel() {
        return teleVM().fields().Trace_level.readInt(teleVM());
    }

    public void writeTraceLevel(int newLevel) {
        teleVM().fields().Trace_level.writeInt(teleVM(), newLevel);
    }

    public long readTraceThreshold() {
        return teleVM().fields().Trace_threshold.readLong(teleVM());
    }

    public void writeTraceThreshold(long newThreshold) {
        teleVM().fields().Trace_threshold.writeLong(teleVM(), newThreshold);
    }

}
