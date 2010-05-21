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
package com.sun.max.tele.debug.guestvm.xen.dbchannel;

/**
 * Handles access to the image file.
 *
 * @author Mick Jordan
 *
 */

public class ImageFileHandler {
    public static ImageFileHandler open(String imageFile) {
        // TODO open file, connect via ELF
        return new ImageFileHandler();
    }

    public void close() {
        // TODO close file
    }

    /**
     * Gets the value of the symbol in the boot image which holds the the address of the boot heap base, see Native/substrate/image.c.
     * @return value of symbol
     */
    public long getBootHeapStartSymbolAddress() {
        // TODO get symbol value
        return 0;
    }

    /**
     * Gets the value of the symbol in the boot image which holds the "all threads" list head value, see guk/sched.c.
     * @return
     */
    public long getThreadListSymbolAddress() {
        // TODO get symbol value
        return 0;
    }
}
