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
package com.sun.max.vm.prototype;

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
     * Create the Java prototype, which includes the basic JDK and Maxine classes.
     *
     * @param complete specifies whether to load more than just the VM scheme packages
     * @return a new Java prototype object
     */
    public JavaPrototype createJavaPrototype(boolean complete) {
        return new JavaPrototype(complete);
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
        final JavaPrototype javaPrototype = createJavaPrototype(true);
        if (prototypeJit) {
            vmConfig().compilationScheme().setMode(Mode.PROTOTYPE_JIT);
        }
        javaPrototype.loadCoreJavaPackages();

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
        final DataPrototype dataPrototype = new DataPrototype(graphPrototype, null);
        return dataPrototype;
    }

    private int currentNumberOfClasses() {
        return ClassRegistry.BOOT_CLASS_REGISTRY.numberOfClassActors();
    }
}
