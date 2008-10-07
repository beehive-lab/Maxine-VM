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
/*VCSID=24300791-b816-4848-a7ee-4fb7123bcf2e*/
package util;

/**
 *  FortuneCookie is whatever program it happens to be today.
 *  DO NOT HACK HELLO WORLD!  HACK THIS PROGRAM INSTEAD.
 *
 *  @author Everybody
 */


public class FortuneCookie {

    int _x = 1;
    int _y = 2;
    int _z = 3;
    int _r = 4;
    int _s = 5;
    int _t = 6;

    public static int getfields(FortuneCookie f) {
        return f._x + f._y + f._z + f._r + f._s + f._t;
    }

    public static void main(String[] args) {
        System.out.println("Hello World");
        final FortuneCookie f = new FortuneCookie();
        int n = 0;
        for (int i = 0; i < 20000; i++) {
            n += getfields(f);
        }

        System.out.println("N=" + n);

        System.out.println("Hello World");
    }
}
