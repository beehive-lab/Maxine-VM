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
/*VCSID=9e8ca32b-7670-4638-80cd-157e4d983d8e*/
package util;

/**
 * A simple class that echoes its arguments to the console.
 *
 * @author Ben L. Titzer
 */
public class Echo {
    public static void main(String[] args) {
        if (args == null) {
            System.out.println("Error: arguments are null");
        } else {
            for (int i = 0; i < args.length; i++) {
                if (i > 0) {
                    System.out.print(" ");
                }
                System.out.print(args[i]);
            }
            System.out.println("");
        }
    }
}
