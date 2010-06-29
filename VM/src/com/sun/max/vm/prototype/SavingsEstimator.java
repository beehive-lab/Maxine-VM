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
package com.sun.max.vm.prototype;

import com.sun.max.lang.WordWidth;

import java.io.PrintStream;
import java.util.Collection;

/**
 * This class is useful for prototyping refactorings aimed at reducing the size of the image.
 * An estimate for the amount of space saved can be computed after a graph has been built.
 *
 * @author Ben L. Titzer
 */
public class SavingsEstimator {
    final GraphStats graphStats;

    public SavingsEstimator(GraphStats graphStats) {
        this.graphStats = graphStats;
    }

    public void report(PrintStream printStream) {
        if (graphStats.graphPrototype.vmConfiguration().wordWidth() == WordWidth.BITS_64) {
            reportCompressRefsSavings(printStream);
        }
    }

    private void reportCompressRefsSavings(PrintStream printStream) {
        Collection<GraphPrototype.ClassInfo> cstats = graphStats.graphPrototype.classInfos.values();
        printStream.println("Estimating savings from compressed references, ignoring alignment...");
        int totalRefs = 0;
        for (GraphPrototype.ClassInfo info : cstats) {
            final GraphStats.ClassStats s = GraphStats.getClassStats(info);
            int instanceRefs = info.instanceFields.size();
            int staticRefs = info.staticFields.size();
            int savedRefs = instanceRefs * s.objectCount + staticRefs;
            if (savedRefs > 0) {
                // save instance refs and static refs
//                printStream.println(info.clazz + " " + instanceRefs + " x " + s.objectCount + " " + staticRefs + " = " + savedRefs);
                totalRefs += savedRefs;
            }
        }

        for (Object o : graphStats.graphPrototype.objects) {
            totalRefs += 2; // two words per header
            if (o instanceof Object[]) {
                int length = ((Object[]) o).length;
                totalRefs += length;
            }
        }
        printStream.println("Estimated savings from compressed refs (" + totalRefs + " refs) = " + totalRefs * 4 + " bytes");
    }
}
