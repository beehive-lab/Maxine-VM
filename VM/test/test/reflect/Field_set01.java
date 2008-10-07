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
package test.reflect;

/*
 * @Harness: java
 * @Runs: 0=true; 1=true; 2=true; 3=true; 4=true; 5=true; 6=true; 7=true; 8=false
 */
public class Field_set01 {
    public static byte byteField;
    public static short shortField;
    public static char charField;
    public static int intField;
    public static long longField;
    public static float floatField;
    public static double doubleField;
    public static boolean booleanField;

    public static boolean test(int arg) throws NoSuchFieldException, IllegalAccessException {
        if (arg == 0) {
            Field_set01.class.getField("byteField").set(null, Byte.valueOf((byte) 11));
            return byteField == 11;
        } else if (arg == 1) {
            Field_set01.class.getField("shortField").set(null, Short.valueOf((short) 12));
            return shortField == 12;
        } else if (arg == 2) {
            Field_set01.class.getField("charField").set(null, Character.valueOf((char) 13));
            return charField == 13;
        } else if (arg == 3) {
            Field_set01.class.getField("intField").set(null, Integer.valueOf(14));
            return intField == 14;
        } else if (arg == 4) {
            Field_set01.class.getField("longField").set(null, Long.valueOf(15L));
            return longField == 15;
        } else if (arg == 5) {
            Field_set01.class.getField("floatField").set(null, Float.valueOf(16));
            return floatField == 16;
        } else if (arg == 6) {
            Field_set01.class.getField("doubleField").set(null, Double.valueOf(17));
            return doubleField == 17;
        } else if (arg == 7) {
            Field_set01.class.getField("booleanField").set(null, true);
            return booleanField == true;
        }
        return false;
    }
}
