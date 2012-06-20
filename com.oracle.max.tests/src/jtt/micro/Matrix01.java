/*
 * Copyright (c) 2009, 2012, Oracle and/or its affiliates. All rights reserved.
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
package jtt.micro;

/*
 * @Harness: java
 * @Runs: 0=8; 1=34; 2=152; 3=204; 4=1547; 5=42
 */
public class Matrix01 {
    final int id;

    Matrix01(int id) {
        this.id = id;
    }

    public static int test(int arg) {
        if (arg == 0) {
            return matrix1(3) + matrix1(5);
        }
        if (arg == 1) {
            return matrix2(3) + matrix2(5);
        }
        if (arg == 2) {
            return matrix3(3) + matrix3(5);
        }
        if (arg == 3) {
            return matrix4(3) + matrix4(5);
        }
        if (arg == 4) {
            return matrix5(3) + matrix5(5);
        }
        return 42;
    }

    static int matrix1(int size) {
        Matrix01[] matrix = new Matrix01[size];
        fillMatrix(matrix, size);
        int count = 0;
        for (Matrix01 m : matrix) {
            if (m != null) {
                count++;
            }
        }
        return count;
    }

    static int matrix2(int size) {
        Matrix01[][] matrix = new Matrix01[size][size];
        fillMatrix(matrix, size * size);
        int count = 0;
        for (Matrix01[] n : matrix) {
            for (Matrix01 m : n) {
                if (m != null) {
                    count++;
                }
            }
        }
        return count;
    }

    static int matrix3(int size) {
        Matrix01[][][] matrix = new Matrix01[size][5][size];
        fillMatrix(matrix, size * size * size);
        int count = 0;
        for (Matrix01[][] o : matrix) {
            for (Matrix01[] n : o) {
                for (Matrix01 m : n) {
                    if (m != null) {
                        count++;
                    }
                }
            }
        }
        return count;
    }

    static int matrix4(int size) {
        Matrix01[][][][] matrix = new Matrix01[size][2][size][3];
        fillMatrix(matrix, size * size * size * size);
        int count = 0;
        for (Matrix01[][][] p : matrix) {
            for (Matrix01[][] o : p) {
                for (Matrix01[] n : o) {
                    for (Matrix01 m : n) {
                        if (m != null) {
                            count++;
                        }
                    }
                }
            }
        }
        return count;
    }

    static int matrix5(int size) {
        Matrix01[][][][][] matrix = new Matrix01[size][size][3][4][size];
        fillMatrix(matrix, size * size * size * size * size);
        int count = 0;
        for (Matrix01[][][][] q : matrix) {
            for (Matrix01[][][] p : q) {
                for (Matrix01[][] o : p) {
                    for (Matrix01[] n : o) {
                        for (Matrix01 m : n) {
                            if (m != null) {
                                count++;
                            }
                        }
                    }
                }
            }
        }
        return count;
    }

    static void fillMatrix(Object[] matrix, int total) {
        for (int i = 0; i < 10000; i += 7) {
            int number = i % total;
            set(matrix, number);
        }
    }

    static void set(Object[] matrix, int number) {
        int val = number;
        Object[] array = matrix;
        while (!(array instanceof Matrix01[])) {
            int index = val % array.length;
            val = val / array.length;
            array = (Object[]) array[index];
        }
        ((Matrix01[]) array)[val % array.length] = new Matrix01(number);
    }
}
