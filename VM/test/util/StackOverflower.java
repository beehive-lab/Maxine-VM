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
/*VCSID=934e7a34-04fd-478f-aef3-42b476e79134*/
package util;

public class StackOverflower {

    public StackOverflower() {
    }

    private static final int LIMIT = 1000000;

    private int _depth = 0;

    void recurse() {
        _depth++;
        if (_depth > LIMIT) {
            _depth = -1;
            return;
        }
        recurse();
    }

    public int getNumberOfInvocations() {
        _depth = 0;
        try {
            recurse();
        } catch (StackOverflowError stackOverflowError) {
        }
        return _depth;
    }

    public static void run() {
        final StackOverflower stackOverflower = new StackOverflower();
        int n = stackOverflower.getNumberOfInvocations();
        System.out.println("first overflow stackDepth: " + n);
        n = stackOverflower.getNumberOfInvocations();
        System.out.println("second overflow stackDepth: " + n);
    }

    public static void main(String[] argv) {
        run();
    }
}
