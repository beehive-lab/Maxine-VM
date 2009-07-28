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

import com.sun.c1x.debug.LogStream;

/**
 * The <code>MonitorValue</code> class definition.
 *
 * @author Marcelo Cintra
 * @author Thomas Wuerthinger
 *
 */
public class MonitorValue extends ScopeValue {

    private ScopeValue owner;
    private Location basicLock;
    private boolean eliminated;

    public MonitorValue(ScopeValue owner, Location basicLock) {
        this(owner, basicLock, false);
    }

    public MonitorValue(ScopeValue owner, Location basicLock, boolean eliminated) {
        this.owner = owner;
        this.basicLock = basicLock;
        this.eliminated = eliminated;
    }

    // Serialization of debugging information
    public MonitorValue(DebugInfoReadStream stream) {
        basicLock = new Location(stream);
        owner = ScopeValue.readFrom(stream);
        eliminated = (stream.readBool() != false);
    }

    // Accessors
    public ScopeValue owner() {
        return owner;
    }

    public Location basicLock() {
        return basicLock;
    }

    public boolean eliminated() {
        return eliminated;
    }

    @Override
    public void writeOn(DebugInfoWriteStream stream) {
        basicLock.writeOn(stream);
        owner.writeOn(stream);
        stream.writeBool(eliminated);
    }

    @Override
    public void printOn(LogStream out) {
        out.print("monitor{");
        owner.printOn(out);
        out.print(",");
        basicLock.printOn(out, 8); // TODO: !!!! resolve to logBytesPerInt
        out.print("}");
        if (eliminated) {
            out.print(" (eliminated)");
        }
    }
}
