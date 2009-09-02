/*
 * Copyright (c) 2009 Sun Microsystems, Inc.  All rights reserved.
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
