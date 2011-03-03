/*
 * Copyright (c) 2007, 2010, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.cps.cir.gui;
import java.io.*;
import java.util.*;

import com.sun.max.util.*;

/**
 * An implementation of a Longest Common Subsequence (LCS) algorithm for computing the diff between two object arrays. This
 * implementation is based on the algorithm described on page 353 of
 * <a href="http://mitpress.mit.edu/algorithms">Introduction to Algorithms, Second Edition</a>.
 *
 * @author Doug Simon
 */
public class Diff {

    /**
     * An implementation of this interface provides the equality test used by the diff algorithm.
     */
    public static interface Equality {

        /**
         * Determines if two given objects are equal.
         */
        boolean test(Object object1, Object object2);
    }

    /**
     * An implementation of an equality test that delegates to {@link Object#equals(Object)}.
     */
    public static class ObjectEquality implements Equality {

        public boolean test(Object object1, Object object2) {
            return object1.equals(object2);
        }
    }

    /**
     * An implementation of an equality test that delegates to object identity.
     */
    public static class ObjectIdentity implements Equality {

        public boolean test(Object object1, Object object2) {
            return object1 == object2;
        }
    }

    private final List<Range> deletions;

    private final List<Range> insertions;

    /**
     * Computes the LCS of two object arrays. The LCS can be obtained by applying a sequence of
     * {@linkplain #deletions() deletions} from {@code x}. Further applying a sequence of
     * {@linkplain #insertions() insertions} to the LCS will result in {@code y}. That is the composition of the
     * deletions and insertions comprises a change script to transform {@code x} into {@code y} with the minimal number
     * of modifications.
     */
    public Diff(Object[] x, Object[] y, Equality equality) {
        // opt[i][j] = length of LCS of x[i..M] and y[j..N]
        final int[][] opt = new int[x.length + 1][y.length + 1];

        // compute length of LCS and all subproblems via dynamic programming
        for (int i = x.length - 1; i >= 0; i--) {
            for (int j = y.length - 1; j >= 0; j--) {
                if (equality.test(x[i], y[j])) {
                    opt[i][j] = opt[i + 1][j + 1] + 1;
                } else {
                    opt[i][j] = Math.max(opt[i + 1][j], opt[i][j + 1]);
                }
            }
        }

        final List<Range> dels = new ArrayList<Range>();
        final List<Range> adds = new ArrayList<Range>();
        int i = 0;
        int j = 0;
        while (i < x.length && j < y.length) {
            if (equality.test(x[i], y[j])) {
                i++;
                j++;
            } else if (opt[i + 1][j] >= opt[i][j + 1]) {
                final int start = i++;
                while (i < x.length && (opt[i + 1][j] >= opt[i][j + 1])) {
                    ++i;
                }
                assert i > start;
                dels.add(new Range(start, i));
            } else {
                final int start = j++;
                while (j < y.length && (opt[i + 1][j] < opt[i][j + 1])) {
                    ++j;
                }
                assert j > start;
                adds.add(new Range(start, j));
            }
        }

        if (i < x.length) {
            dels.add(new Range(i, x.length));
        } else if (j < y.length) {
            adds.add(new Range(j, y.length));
        }

        this.deletions = dels;
        this.insertions = adds;
    }

    /**
     * Gets the indexes of the elements that when deleted from {@code x} give the LCS of {@code x} and {@code y}.
     *
     * @return a sequence of indexes into {@code x} (represented as a sequence of consecutive index ranges)
     */
    public List<Range> deletions() {
        return deletions;
    }

    /**
     * Gets the indexes of the elements that when deleted from {@code y} give the LCS of {@code x} and {@code y}.
     *
     * @return a sequence of indexes into {@code y} (represented as a sequence of consecutive index ranges)
     */
    public List<Range> insertions() {
        return insertions;
    }

    /**
     * A command line interface for using this diff utility to implement a textual, line-based file diff.
     */
    public static void main(String[] args) throws IOException {
        final Object[] leftFile = readLines(new File(args[0])).toArray(new Object[readLines(new File(args[0])).size()]);
        final Object[] rightFile = readLines(new File(args[1])).toArray(new Object[readLines(new File(args[1])).size()]);
        final Diff diff = new Diff(leftFile, rightFile, new ObjectEquality());

        System.out.println("------- " + args[0] + " --------");
        print(leftFile, "< ", diff.deletions());
        System.out.println("------- " + args[1] + " --------");
        print(rightFile, "> ", diff.insertions());
    }

    static List<String> readLines(File file) throws IOException {
        final List<String> lines = new ArrayList<String>();
        final BufferedReader reader = new BufferedReader(new FileReader(file));
        final String nl = System.getProperty("line.separator");
        String line;
        while ((line = reader.readLine()) != null) {
            lines.add(line + nl);
        }
        return lines;
    }

    public static void print(Object[] objects, String changePrefix, List<Range> changes) {
        final Iterator<Range> iterator = changes.iterator();
        int nextIndexPrint = 0;
        while (iterator.hasNext()) {
            final Range range = iterator.next();
            while (nextIndexPrint < range.start()) {
                System.out.print(objects[nextIndexPrint++]);
            }
            while (nextIndexPrint != range.end()) {
                System.out.print(changePrefix + objects[nextIndexPrint++]);
            }
        }
        while (nextIndexPrint < objects.length) {
            System.out.print(objects[nextIndexPrint++]);
        }
    }

}
