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
package com.sun.c1x.value;

import com.sun.c1x.lir.*;
import com.sun.c1x.lir.LIRAddress.*;

/**
 * The <code>Address</code> class definition.
 *
 * @author Marcelo Cintra
 * @author Thomas Wuerthinger
 *
 */
public class Address {

    private long address;

    /**
     * Constructs a new Address, which holds a runtime address.
     *
     * @param address
     */
    public Address(long address) {
        super();
        this.address = address;
    }

    public Address(Register tmp, int i) {
        // TODO Auto-generated constructor stub
    }

    public Address(Register base, Register index, Scale scale, int displacement) {
        // TODO Auto-generated constructor stub
    }

    /**
     * @return the long value which represents an internal address
     */
    public long address() {
        return address;
    }

    /**
     * Sets the address.
     *
     * @param address
     *            the new address
     */
    public void setAddress(long address) {
        this.address = address;
    }

    public int asInt() {
        // TODO Auto-generated method stub
        return 0;
    }

    public int disp() {
        // TODO Auto-generated method stub
        return 0;
    }

    public int sub(Address codeBegin) {
        long result = address - codeBegin.address;
        assert result == (int) result : "overflow";
        return (int) result;
    }
}
