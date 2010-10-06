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
package test.bench.java.util;

import java.util.*;
import test.bench.util.*;

/**
 * Utility class for HashMap benchmarks.
 *
 * @author Mick Jordan
 */

public abstract class HashMapBase  extends RunBench.MicroBenchmark {

    public final Map<Integer, Integer> map;
    public final Random random = new Random(46763);
    public final Integer value = new Integer(0);
    public final Integer key = random.nextInt();

    public HashMapBase() {
        this(new HashMap<Integer, Integer>());
    }

    public HashMapBase(Map<Integer, Integer> map) {
        this.map = map;
        for (int i = 0; i < 1000; i++) {
            map.put(random.nextInt(), value);
        }
    }
}
