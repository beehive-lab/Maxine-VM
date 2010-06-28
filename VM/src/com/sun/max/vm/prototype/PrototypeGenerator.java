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

import com.sun.max.*;
import com.sun.max.asm.*;
import com.sun.max.lang.*;
import com.sun.max.platform.*;
import com.sun.max.program.option.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.code.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.compiler.CompilationScheme.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.grip.*;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.layout.*;
import com.sun.max.vm.monitor.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.run.*;
import com.sun.max.vm.trampoline.*;
import com.sun.max.vm.type.*;

/**
 * Directs and manages the creation of the prototype components, including the
 * compiled prototype (code), graph prototype (object graph), and data prototype
 * (raw data). This class also constructs the VM configuration from a number
 * of command line options.
 *
 * @author Bernd Mathiske
 * @author Doug Simon
 * @author Ben L. Titzer
 */
public final class PrototypeGenerator {

    private final OptionSet options = new OptionSet();

    private final Option<BuildLevel> buildLevel = options.newEnumOption("build", BuildLevel.PRODUCT, BuildLevel.class,
            "This option selects the build level of the virtual machine.");
    private final Option<ProcessorModel> processorModel = options.newEnumOption("cpu", null, ProcessorModel.class,
            "Specifies the target instruction set architecture.");
    private final Option<InstructionSet> instructionSet = options.newEnumOption("isa", null, InstructionSet.class,
            "Specifies the target instruction set.");
    private final Option<OperatingSystem> operatingSystem = options.newEnumOption("os", null, OperatingSystem.class,
            "Specifies the target operating system.");
    private final Option<Integer> pageSizeOption = options.newIntegerOption("page", null,
            "Specifies the target page size in bytes.");
    private final Option<WordWidth> wordWidth = options.newEnumOption("bits", null, WordWidth.class,
            "Specifies the target machine word with in bits.");
    private final Option<Endianness> endiannessOption = options.newEnumOption("endianness", null, Endianness.class,
            "Specifies the endianness of the target.");
    private final Option<Integer> cacheAlignmentOption = options.newIntegerOption("align", null,
            "Specifies the cache alignment of the target.");
    private final Option<MaxPackage> gripScheme = schemeOption("grip", new com.sun.max.vm.grip.Package(), GripScheme.class,
            "Specifies the grip scheme for the target.");
    private final Option<MaxPackage> referenceScheme = schemeOption("reference", new com.sun.max.vm.reference.Package(), ReferenceScheme.class,
            "Specifies the reference scheme for the target.");
    private final Option<MaxPackage> layoutScheme = schemeOption("layout", new com.sun.max.vm.layout.Package(), LayoutScheme.class,
            "Specifies the layout scheme for the target.");
    private final Option<MaxPackage> heapScheme = schemeOption("heap", new com.sun.max.vm.heap.Package(), HeapScheme.class,
            "Specifies the heap scheme for the target.");
    private final Option<MaxPackage> monitorScheme = schemeOption("monitor", new com.sun.max.vm.monitor.Package(), MonitorScheme.class,
            "Specifies the monitor scheme for the target.");
    private final Option<MaxPackage> bootScheme = schemeOption("boot", new com.sun.max.vm.compiler.Package(), BootstrapCompilerScheme.class,
            "Specifies the boot compiler scheme for the target.");
    private final Option<MaxPackage> optScheme = schemeOption("opt", new com.sun.max.vm.compiler.Package(), RuntimeCompilerScheme.class,
            "Specifies the optimizing compiler scheme for the target.");
    private final Option<MaxPackage> jitScheme = schemeOption("jit", MaxPackage.fromName("com.sun.max.vm.jit"), RuntimeCompilerScheme.class,
            "Specifies the JIT scheme for the target.");
    private final Option<MaxPackage> trampolineScheme = schemeOption("trampoline", new com.sun.max.vm.trampoline.Package(), DynamicTrampolineScheme.class,
            "Specifies the dynamic trampoline scheme for the target.");
    private final Option<MaxPackage> targetABIsScheme = schemeOption("abi", new com.sun.max.vm.compiler.target.Package(), TargetABIsScheme.class,
            "Specifies the ABIs scheme for the target");
    private final Option<MaxPackage> runScheme = schemeOption("run", new com.sun.max.vm.run.Package(), RunScheme.class,
            "Specifies the run scheme for the target.");
    private final Option<Integer> threadsOption = options.newIntegerOption("threads", Runtime.getRuntime().availableProcessors(),
            "Specifies the number of threads to be used for parallel compilation.");

    private Option<MaxPackage> schemeOption(String name, MaxPackage superPackage, Class cl, String help) {
        return options.newOption(name, (MaxPackage) null, new MaxPackageOptionType(superPackage, cl), OptionSet.Syntax.REQUIRES_EQUALS, help);
    }

    /**
     * Creates a new prototype generator.
     *
     * @param optionSet the set of options to which the prototype generator specific options will be added
     */
    public PrototypeGenerator(OptionSet optionSet) {
        optionSet.addOptions(options);
    }

    /**
     * Creates a VM configuration from the specified option set and the specified default configuration.
     * The options supplied may individually override the values in the default configuration.
     *
     * @param defaultConfiguration the default VM configuration
     * @return a new VM configuration based on the default configuration with the specific options
     * selected
     */
    VMConfiguration createVMConfiguration(final VMConfiguration defaultConfiguration) {
        OperatingSystem defaultOperatingSystem = defaultConfiguration.platform().operatingSystem;
        // set the defaults manually using the default configuration
        processorModel.setDefaultValue(defaultConfiguration.platform().processorKind.processorModel);
        instructionSet.setDefaultValue(defaultConfiguration.platform().processorKind.instructionSet);
        operatingSystem.setDefaultValue(defaultOperatingSystem);
        pageSizeOption.setDefaultValue(defaultConfiguration.platform().pageSize);
        wordWidth.setDefaultValue(defaultConfiguration.platform().processorKind.dataModel.wordWidth);
        endiannessOption.setDefaultValue(defaultConfiguration.platform().processorKind.dataModel.endianness);
        cacheAlignmentOption.setDefaultValue(defaultConfiguration.platform().processorKind.dataModel.cacheAlignment);
        gripScheme.setDefaultValue(defaultConfiguration.gripPackage);
        referenceScheme.setDefaultValue(defaultConfiguration.referencePackage);
        layoutScheme.setDefaultValue(defaultConfiguration.layoutPackage);
        heapScheme.setDefaultValue(defaultConfiguration.heapPackage);
        monitorScheme.setDefaultValue(defaultConfiguration.monitorPackage);
        bootScheme.setDefaultValue(defaultConfiguration.bootCompilerPackage);
        optScheme.setDefaultValue(defaultConfiguration.optCompilerPackage);
        jitScheme.setDefaultValue(defaultConfiguration.jitCompilerPackage);
        trampolineScheme.setDefaultValue(defaultConfiguration.trampolinePackage);
        targetABIsScheme.setDefaultValue(defaultConfiguration.targetABIsPackage);
        runScheme.setDefaultValue(defaultConfiguration.runPackage);

        if (threadsOption.getValue() <= 0) {
            throw new Option.Error("The value specified for " + threadsOption + " must be greater than 0.");
        }

        final DataModel dataModel = new DataModel(wordWidth.getValue(), endiannessOption.getValue(), cacheAlignmentOption.getValue());
        final ProcessorKind processorKind = new ProcessorKind(processorModel.getValue(), instructionSet.getValue(), dataModel);
        final OperatingSystem operatingSystem = this.operatingSystem.getValue();
        int pageSizeValue = pageSizeOption.getValue();
        if (!options.hasOptionSpecified(pageSizeOption.getName()) && operatingSystem != defaultOperatingSystem) {
            pageSizeValue = operatingSystem.defaultPageSize();
        }
        final Platform platform = new Platform(processorKind, operatingSystem, pageSizeValue);
        return new VMConfiguration(buildLevel.getValue(), platform,
                                    vm(gripScheme),
                                    vm(referenceScheme),
                                    vm(layoutScheme),
                                    vm(heapScheme),
                                    vm(monitorScheme),
                                    vm(bootScheme),
                                    vm(jitScheme),
                                    vm(optScheme),
                                    vm(trampolineScheme),
                                    vm(targetABIsScheme),
                                    vm(runScheme));
    }

    /**
     * Cast the value of an option to a {@code VMPackage}.
     * @param option the option which contains the value
     * @return the option's value casted to a {@code VMPackage}
     */
    private static VMPackage vm(Option<MaxPackage> option) {
        return (VMPackage) option.getValue();
    }

    /**
     * Create the Java prototype, which includes the basic JDK and Maxine classes.
     *
     * @param vmConfiguration the default VM configuration
     * @param complete specifies whether to load more than just the VM scheme packages
     * @return a new Java prototype object
     */
    public JavaPrototype createJavaPrototype(VMConfiguration vmConfiguration, boolean complete) {
        return new JavaPrototype(createVMConfiguration(vmConfiguration), complete);
    }

    /**
     * Creates the default VM configuration.
     *
     * @return the default VM configuration
     */
    VMConfiguration createDefaultVMConfiguration() {
        return VMConfigurations.createStandardJit(BuildLevel.PRODUCT, Platform.host());
    }

    /**
     * Creates the default Java prototype.
     *
     * @param loadPackages a boolean indicating whether to load the basic VM and JDK packages
     * @return the new default Java prototype
     */
    public JavaPrototype createJavaPrototype(boolean loadPackages) {
        return createJavaPrototype(createDefaultVMConfiguration(), loadPackages);
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
            javaPrototype.vmConfiguration().compilationScheme().setMode(Mode.PROTOTYPE_JIT);
        }
        javaPrototype.loadCoreJavaPackages();

        return MaxineVM.usingTarget(new Function<GraphPrototype>() {
            public GraphPrototype call() {
                GraphPrototype graphPrototype;
                int numberOfClassActors = 0;
                int numberOfCompilationThreads = threadsOption.getValue();
                final CompiledPrototype compiledPrototype = new CompiledPrototype(javaPrototype, numberOfCompilationThreads);
                compiledPrototype.addEntrypoints();
                do {
                    for (MethodActor methodActor : javaPrototype.vmConfiguration().runScheme().gatherNativeInitializationMethods()) {
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
        });
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
