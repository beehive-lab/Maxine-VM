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
/*VCSID=36bca7e2-b3d9-4e6f-953e-14261eee412d*/
package test.bytecode;

/*
 * @Harness: java
 * @Runs: (0, 0.01f) = 0.01f; (1, -1.4f) = -1.4f; (2, 0.01f) = 0.01f; (3, -1.4f) = -1.4f;
 */
public class BC_fastore {

    static float[] _array = {0, 0, 0, 0};

    public static float test(int arg, float val) {
        _array[arg] = val;
        return _array[arg];
    }
}
