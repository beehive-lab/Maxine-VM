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
/*VCSID=c4273279-690e-4b1c-8ed7-b06e59fa5847*/
package util;

/**
 * small test case for mantis bug 6: recompilation segmentation fault.
 * @author Yi Guo
 */
public class RctBug_Mantis6 {
    static class S {

        public int _x = 12;

        public int b() {
            try {
                throw new Exception("abc");
            } catch (Exception e) {
                return _x;
            }
        }
    }

    public static void main(String[] args) {
        System.out.println("starting...");
        final S f = new S();
        String s = "";
        /* 10000 iterations to trigger recompilation of f.b() if enabled.*/
        for (int i = 0; i < 10000; i++) {
            s += f.b();
        }
        System.out.println(s.length());
        System.out.println("done!");
    }
}
