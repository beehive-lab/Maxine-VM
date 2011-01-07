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
package com.sun.max.vm.hosted;

import static com.sun.max.vm.VMConfiguration.*;

import com.sun.max.program.option.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.code.*;
import com.sun.max.vm.compiler.CompilationScheme.Mode;
import com.sun.max.vm.type.*;

/**
 * Directs and manages the creation of the prototype components, including the
 * compiled prototype (code), graph prototype (object graph), and data prototype
 * (raw data).
 *
 * @see Prototype
 *
 * @author Bernd Mathiske
 * @author Doug Simon
 * @author Ben L. Titzer
 */
public final class PrototypeGenerator {

    private final Option<Integer> threadsOption;

    /**
     * Creates a new prototype generator.
     *
     * @param optionSet the set of options to which the prototype generator specific options will be added
     */
    public PrototypeGenerator(OptionSet optionSet) {
        threadsOption = optionSet.newIntegerOption("threads", Runtime.getRuntime().availableProcessors(),
            "Specifies the number of threads to be used for parallel compilation.");
    }

    /**
     * Create the graph prototype and explore objects, compiling methods as necessary.
     * This method computes the reachable objects and code of the prototype (i.e. it builds
     * both the {@code CompiledPrototype} and the {@code GraphPrototype} iteratively
     * until it reaches a fixpoint.
     *
     * @param tree a boolean indicating whether to record an object tree, which is useful for debugging
     * @return the final graph prototype of the VM
     */
    public GraphPrototype createGraphPrototype(final boolean tree, boolean prototypeJit) {
        if (prototypeJit) {
            vmConfig().compilationScheme().setMode(Mode.PROTOTYPE_JIT);
        }

        GraphPrototype graphPrototype;
        int numberOfClassActors = 0;
        int numberOfCompilationThreads = threadsOption.getValue();
        final CompiledPrototype compiledPrototype = new CompiledPrototype(numberOfCompilationThreads);
        compiledPrototype.addEntrypoints();
        do {
            for (MethodActor methodActor : vmConfig().runScheme().gatherNativeInitializationMethods()) {
                compiledPrototype.add(methodActor, null, null);
            }
            numberOfClassActors = currentNumberOfClasses();
            if (compiledPrototype.compile()) {
                graphPrototype = new GraphPrototype(compiledPrototype, tree);
            }
        } while (currentNumberOfClasses() != numberOfClassActors);

        compiledPrototype.compileFoldableMethods();
        compiledPrototype.link();

        graphPrototype = new GraphPrototype(compiledPrototype, tree);

        Code.bootCodeRegion.trim();
        return graphPrototype;
    }

    /**
     * Create a data prototype, by first building a graph prototype, then choosing layouts, and
     * eventually encoding the entire graph prototype into a binary form.
     *
     * @param tree a boolean indicating whether to produce an object tree
     * @return a completed data prototype
     */
    DataPrototype createDataPrototype(boolean tree, boolean prototypeJit) {
        final GraphPrototype graphPrototype = createGraphPrototype(tree, prototypeJit);
        final DataPrototype dataPrototype = new DataPrototype(graphPrototype, null, threadsOption.getValue());
        return dataPrototype;
    }

    private int currentNumberOfClasses() {
        return ClassRegistry.BOOT_CLASS_REGISTRY.numberOfClassActors();
    }
}
