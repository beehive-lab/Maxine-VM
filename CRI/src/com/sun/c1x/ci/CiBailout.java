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
package com.sun.c1x.ci;

/**
 * This exception is thrown when C1X refuses to compile a method because of problems with the method.
 * e.g. bytecode wouldn't verify, too big, JSR/ret too complicated, etc. This exception is <i>not</i>
 * meant to indicate problems with the compiler itself.
 *
 * @author Ben L. Titzer
 */
public class CiBailout extends RuntimeException {

    public static final long serialVersionUID = 8974598793458772L;

    /**
     * Create a new bailout.
     * @param reason a message indicating the reason
     */
    public CiBailout(String reason) {
        super(reason);
    }

    /**
     * Create a new bailout due to an internal exception being thrown.
     * @param reason a message indicating the reason
     * @param cause the throwable that was the cause of the bailout
     */
    public CiBailout(String reason, Throwable cause) {
        super(reason, cause);
    }
}
