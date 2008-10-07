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
// Checkstyle: stop
package test.hotpath;

import java.util.*;

/*
 * @Harness: java
 * @Runs: 5 = -1756613086;
 */
public class HP_life {

    public static int test(int generations) {
        reset();
        for (int i = 0; i < generations; ++i) {
            step();
        }
        int sum = 0;
        for (int row = 0; row < _rows; ++row) {
            for (int col = 0; col < _cols; ++col) {
                boolean value = cell(row, col);
                // System.out.print(value ? "1" : "0");
                sum += (row * 15223242 + col * 21623234) ^ ((value ? 1 : 0) * 15323142);
            }
        }
        return sum;
    }

    private static final int _rows = 20;
    private static final int _cols = 20;
    private static boolean _cells[] = new boolean[_rows * _cols];

    private static boolean cell(int row, int col) {
        return ((row >= 0) && (row < _rows) && (col >= 0) && (col < _cols) && _cells[row * _cols + col]);
    }

    private static boolean step() {
        boolean next[] = new boolean[_rows * _cols];
        boolean changed = false;
        for (int row = _rows - 1; row >= 0; --row) {
            int row_offset = row * _cols;
            for (int col = _cols - 1; col >= 0; --col) {
                int count = 0;
                if (cell(row - 1, col - 1)) {
                    count++;
                }
                if (cell(row - 1, col)) {
                    count++;
                }
                if (cell(row - 1, col + 1)) {
                    count++;
                }
                if (cell(row, col - 1)) {
                    count++;
                }
                if (cell(row, col + 1)) {
                    count++;
                }
                if (cell(row + 1, col - 1)) {
                    count++;
                }
                if (cell(row + 1, col)) {
                    count++;
                }
                if (cell(row + 1, col + 1)) {
                    count++;
                }
                boolean old_state = _cells[row_offset + col];
                boolean new_state = (!old_state && count == 3) || (old_state && (count == 2 || count == 3));
                if (!changed && new_state != old_state) {
                    changed = true;
                }
                next[row_offset + col] = new_state;
            }
        }
        _cells = next;
        return changed;
    }

    private static void reset() {
        Random random = new Random(0);
        boolean cells[] = _cells;
        for (int offset = 0; offset < cells.length; ++offset) {
            cells[offset] = random.nextDouble() > 0.5;
        }
    }

}
