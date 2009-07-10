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
package util;

import java.text.*;


public class RandomDoubles {
    public static void main(String[] args) {
        double max = 100;
        int num = 100;
        int digits = 2;
        if (args.length > 0) {
            num = Integer.parseInt(args[0]);
        }
        if (args.length > 1) {
            max = Double.parseDouble(args[1]);
        }
        if (args.length > 2) {
            digits = Integer.parseInt(args[2]);
        }
        final NumberFormat format = new DecimalFormat();
        format.setMaximumFractionDigits(digits);
        format.setMinimumFractionDigits(digits);
        for (int i = 0; i < num; i++) {
            System.out.println(format.format(Math.random() * max));
        }
    }
}
