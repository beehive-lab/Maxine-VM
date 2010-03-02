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
import com.sun.max.unsafe.*;

/**
 * Immutable (thread-safe) record of a thread triggering a memory watchpoint in the VM.
 *
 * @author Michael Van De Vanter
  */
public class TeleWatchpointEvent implements MaxWatchpointEvent {

    private final MaxWatchpoint maxWatchpoint;
    private final TeleNativeThread teleNativeThread;
    private final Address address;
    private final int code;

    public TeleWatchpointEvent(MaxWatchpoint maxWatchpoint, TeleNativeThread teleNativeThread, Address address, int code) {
        this.maxWatchpoint = maxWatchpoint;
        this.teleNativeThread = teleNativeThread;
        this.address = address;
        this.code = code;
    }

    public MaxWatchpoint watchpoint() {
        return maxWatchpoint;
    }

    public MaxThread thread() {
        return teleNativeThread;
    }

    public Address address() {
        return address;
    }

    public int eventCode() {
        return code;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder(50);
        sb.append(getClass().getSimpleName()).append("(");
        sb.append(maxWatchpoint.toString()).append(", ");
        sb.append(thread().toString()).append(" @");
        sb.append(address.toHexString()).append("code=");
        sb.append(Integer.toString(code)).append(")");
        return sb.toString();
    }

}
