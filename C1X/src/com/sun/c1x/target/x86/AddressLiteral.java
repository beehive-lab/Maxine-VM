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
package com.sun.c1x.target.x86;

import com.sun.c1x.asm.*;
import com.sun.c1x.asm.RelocInfo.*;
import com.sun.c1x.util.*;


public class AddressLiteral {

    public final long target;
    public final Relocation rspec;
    public final boolean isLval;

    public AddressLiteral(long address, Type relocationType) {
        isLval = false;
        this.target = address;
        switch (relocationType) {
        case externalWordType:
          rspec = Relocation.specExternalWord(new Pointer(address));
          break;
        case internalWordType:
          rspec = Relocation.specInternalWord(new Pointer(address));
          break;
        case runtimeCallType:
          rspec = Relocation.specRuntimeCall();
          break;
        case none:
            rspec = null;
          break;
        default:
          throw Util.shouldNotReachHere();
        }
    }

    public AddressLiteral(Relocation reloc) {
        this(0, reloc, false);
    }

    public AddressLiteral(long address, Relocation rspec) {
        this(address, rspec, false);
    }

    public AddressLiteral(long address, Relocation rspec, boolean isLval) {
        this.target = address;
        this.rspec = rspec;
        this.isLval = isLval;
    }

    public AddressLiteral addr() {
        // TODO Auto-generated method stub
        return new AddressLiteral(target, rspec, true);
    }

    public Type reloc() {
        return (rspec == null) ? RelocInfo.Type.none : rspec.type();
    }

    public boolean isLval() {
        return isLval;
    }

    public Pointer target() {
        return new Pointer(target);
    }

    public Relocation rspec() {
        return (rspec == null) ? Relocation.none : rspec;
    }
}
