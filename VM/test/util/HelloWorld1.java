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
/*VCSID=5d3d6db4-ac1a-4771-9106-e39c40698161*/
package util;

/**
 *
 * @author Christos Kotselidis
 */
public class HelloWorld1 {

    public static void main(String[] args) {
        System.out.println("HelloWorld");
        for (long i = 0; i < Long.MAX_VALUE; i++) {
            final BigObject oj = new BigObject(i);
            System.out.println("HelloWorld " + oj.getID());

        }
    }

}

class BigObject {

    long[] _array;
    long _id;

    BigObject(long id) {
        _id = id;
        _array = new long[500];

    }

    public long getID() {
        return _id;
    }
}
