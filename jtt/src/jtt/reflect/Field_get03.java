/*
 * Copyright (c) 2007, 2012, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package jtt.reflect;

import java.lang.reflect.*;

/*
 * @Harness: java
 * @Runs: 0=true; 1=true; 2=true; 3=true; 4=true; 5=true; 6=true; 7=true; 8=false
 */
public class Field_get03 {

    private static Field BYTE_FIELD;
    private static Field SHORT_FIELD;
    private static Field CHAR_FIELD;
    private static Field INT_FIELD;
    private static Field LONG_FIELD;
    private static Field FLOAT_FIELD;
    private static Field DOUBLE_FIELD;
    private static Field BOOLEAN_FIELD;

    static {
        try {
            BYTE_FIELD  = Field_get03.class.getField("byteField");
            SHORT_FIELD = Field_get03.class.getField("shortField");
            CHAR_FIELD = Field_get03.class.getField("charField");
            INT_FIELD = Field_get03.class.getField("intField");
            LONG_FIELD = Field_get03.class.getField("longField");
            FLOAT_FIELD = Field_get03.class.getField("floatField");
            DOUBLE_FIELD = Field_get03.class.getField("doubleField");
            BOOLEAN_FIELD = Field_get03.class.getField("booleanField");
        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
    }

    private static final Field_get03 object = new Field_get03();

    public final byte byteField = 11;
    public final short shortField = 12;
    public final char charField = 13;
    public final int intField = 14;
    public final long longField = 15;
    public final float floatField = 16;
    public final double doubleField = 17;
    public final boolean booleanField = true;

    public static boolean test(int arg) throws NoSuchFieldException, IllegalAccessException {
        if (arg == 0) {
            return BYTE_FIELD.get(object).equals(object.byteField);
        } else if (arg == 1) {
            return SHORT_FIELD.get(object).equals(object.shortField);
        } else if (arg == 2) {
            return CHAR_FIELD.get(object).equals(object.charField);
        } else if (arg == 3) {
            return INT_FIELD.get(object).equals(object.intField);
        } else if (arg == 4) {
            return LONG_FIELD.get(object).equals(object.longField);
        } else if (arg == 5) {
            return FLOAT_FIELD.get(object).equals(object.floatField);
        } else if (arg == 6) {
            return DOUBLE_FIELD.get(object).equals(object.doubleField);
        } else if (arg == 7) {
            return BOOLEAN_FIELD.get(object).equals(object.booleanField);
        }
        return false;
    }
}
