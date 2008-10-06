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
/*VCSID=8fcef754-7fab-48de-9597-96090329e635*/
package com.sun.max.util;
import java.io.*;
import java.util.*;

import com.sun.max.collect.*;

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

    private final Sequence<Range> _deletions;

    private final Sequence<Range> _insertions;

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

        final AppendableSequence<Range> deletions = new ArrayListSequence<Range>();
        final AppendableSequence<Range> insertions = new ArrayListSequence<Range>();
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
                deletions.append(new Range(start, i));
            } else {
                final int start = j++;
                while (j < y.length && (opt[i + 1][j] < opt[i][j + 1])) {
                    ++j;
                }
                assert j > start;
                insertions.append(new Range(start, j));
            }
        }

        if (i < x.length) {
            deletions.append(new Range(i, x.length));
        } else if (j < y.length) {
            insertions.append(new Range(j, y.length));
        }

        _deletions = deletions;
        _insertions = insertions;
    }

    /**
     * Gets the indexes of the elements that when deleted from {@code x} give the LCS of {@code x} and {@code y}.
     * 
     * @return a sequence of indexes into {@code x} (represented as a sequence of consecutive index ranges)
     */
    public Sequence<Range> deletions() {
        return _deletions;
    }

    /**
     * Gets the indexes of the elements that when deleted from {@code y} give the LCS of {@code x} and {@code y}.
     * 
     * @return a sequence of indexes into {@code y} (represented as a sequence of consecutive index ranges)
     */
    public Sequence<Range> insertions() {
        return _insertions;
    }

    /**
     * A command line interface for using this diff utility to implement a textual, line-based file diff.
     */
    public static void main(String[] args) throws IOException {
        final Object[] leftFile = Sequence.Static.toArray(readLines(new File(args[0])), Object.class);
        final Object[] rightFile = Sequence.Static.toArray(readLines(new File(args[1])), Object.class);
        final Diff diff = new Diff(leftFile, rightFile, new ObjectEquality());

        System.out.println("------- " + args[0] + " --------");
        print(leftFile, "< ", diff.deletions());
        System.out.println("------- " + args[1] + " --------");
        print(rightFile, "> ", diff.insertions());
    }

    static Sequence<String> readLines(File file) throws IOException {
        final AppendableSequence<String> lines = new ArrayListSequence<String>();
        final BufferedReader reader = new BufferedReader(new FileReader(file));
        final String nl = System.getProperty("line.separator");
        String line;
        while ((line = reader.readLine()) != null) {
            lines.append(line + nl);
        }
        return lines;
    }

    public static void print(Object[] objects, String changePrefix, Sequence<Range> changes) {
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
