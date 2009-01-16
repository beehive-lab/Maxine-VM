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

import sun.management.*;

import com.sun.max.*;
import com.sun.max.asm.*;
import com.sun.max.lang.*;
import com.sun.max.platform.*;
import com.sun.max.profile.*;
import com.sun.max.program.*;
import com.sun.max.program.option.*;
import com.sun.max.util.timer.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.code.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.grip.*;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.interpret.*;
import com.sun.max.vm.jni.*;
import com.sun.max.vm.layout.*;
import com.sun.max.vm.monitor.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.run.*;
import com.sun.max.vm.trampoline.*;
import com.sun.max.vm.type.*;

/**
 * Directs and manages the creation of the prototype components, including the
 * compiled prototype (code), graph protototype (object graph), and data prototype
 * (raw data). This class also constructs the VM configuration from a number
 * of command line options.
 *
 * @author Bernd Mathiske
 * @author Doug Simon
 * @author Ben L. Titzer
 */
public final class PrototypeGenerator {

    private final OptionSet _options = new OptionSet();

    private final Option<BuildLevel> _buildLevel = _options.newEnumOption("build", BuildLevel.DEBUG, BuildLevel.class,
            "This option selects the build level of the virtual machine.");
    private final Option<ProcessorModel> _processorModel = _options.newEnumOption("cpu", null, ProcessorModel.class,
            "Specifies the target instruction set architecture.");
    private final Option<InstructionSet> _instructionSet = _options.newEnumOption("isa", null, InstructionSet.class,
            "Specifies the target instruction set.");
    private final Option<OperatingSystem> _operatingSystem = _options.newEnumOption("os", null, OperatingSystem.class,
            "Specifies the target operating system.");
    private final Option<Integer> _pageSizeOption = _options.newIntegerOption("page", null,
            "Specifies the target page size in bytes.");
    private final Option<WordWidth> _wordWidth = _options.newEnumOption("bits", null, WordWidth.class,
            "Specifies the target machine word with in bits.");
    private final Option<Endianness> _endiannessOption = _options.newEnumOption("endianness", null, Endianness.class,
            "Specifies the endianness of the target.");
    private final Option<Alignment> _alignmentOption = _options.newEnumOption("align", null, Alignment.class,
            "Specifies the alignment of the target.");
    private final Option<MaxPackage> _gripScheme = schemeOption("grip", new com.sun.max.vm.grip.Package(), GripScheme.class,
            "Specifies the grip scheme for the target.");
    private final Option<MaxPackage> _referenceScheme = schemeOption("reference", new com.sun.max.vm.reference.Package(), ReferenceScheme.class,
            "Specifies the reference scheme for the target.");
    private final Option<MaxPackage> _layoutScheme = schemeOption("layout", new com.sun.max.vm.layout.Package(), LayoutScheme.class,
            "Specifies the layout scheme for the target.");
    private final Option<MaxPackage> _heapScheme = schemeOption("heap", new com.sun.max.vm.heap.Package(), HeapScheme.class,
            "Specifies the heap scheme for the target.");
    private final Option<MaxPackage> _monitorScheme = schemeOption("monitor", new com.sun.max.vm.monitor.Package(), MonitorScheme.class,
            "Specifies the monitor scheme for the target.");
    private final Option<MaxPackage> _compilerScheme = schemeOption("compiler", new com.sun.max.vm.compiler.Package(), CompilerScheme.class,
            "Specifies the compiler scheme for the target.");
    private final Option<MaxPackage> _jitScheme = schemeOption("jit", new com.sun.max.vm.jit.Package(), DynamicCompilerScheme.class,
            "Specifies the JIT scheme for the target.");
    private final Option<MaxPackage> _interpreterScheme = schemeOption("interpreter", new com.sun.max.vm.interpret.Package(), InterpreterScheme.class,
            "Specifies the interpreter scheme for the target.");
    private final Option<MaxPackage> _trampolineScheme = schemeOption("trampoline", new com.sun.max.vm.trampoline.Package(), DynamicTrampolineScheme.class,
            "Specifies the dynamic trampoline scheme for the target.");
    private final Option<MaxPackage> _targetABIsScheme = schemeOption("abi", new com.sun.max.vm.compiler.target.Package(), TargetABIsScheme.class,
            "Specifies the ABIs scheme for the target");
    private final Option<MaxPackage> _runScheme = schemeOption("run", new com.sun.max.vm.run.Package(), RunScheme.class,
            "Specifies the run scheme for the target.");
    private final Option<Integer> _threadsOption = _options.newIntegerOption("threads", ManagementFactory.getOperatingSystemMXBean().getAvailableProcessors(),
            "Specifies the number of threads to be used for parallel compilation.");

    private Option<MaxPackage> schemeOption(String name, MaxPackage superPackage, Class cl, String help) {
        return _options.newOption(name, (MaxPackage) null, new MaxPackageOptionType(superPackage, cl), OptionSet.Syntax.REQUIRES_EQUALS, help);
    }

    /**
     * Creates a new prototype generator.
     *
     * @param optionSet the set of options to which the prototype generator specific options will be added
     */
    public PrototypeGenerator(OptionSet optionSet) {
        optionSet.addOptions(_options);
    }

    /**
     * Creates a VM configuration from the specified option set and the specified default configuration.
     * The options supplied may individually override the values in the default configuration.
     *
     * @param defaultConfiguration the default VM configuration
     * @return a new VM configuration based on the default configuration with the specific options
     * selected
     */
    private VMConfiguration createVMConfiguration(final VMConfiguration defaultConfiguration) {
        // set the defaults manually using the default configuration
        _processorModel.setDefaultValue(defaultConfiguration.platform().processorKind().processorModel());
        _instructionSet.setDefaultValue(defaultConfiguration.platform().processorKind().instructionSet());
        _operatingSystem.setDefaultValue(defaultConfiguration.platform().operatingSystem());
        _pageSizeOption.setDefaultValue(defaultConfiguration.platform().pageSize());
        _wordWidth.setDefaultValue(defaultConfiguration.platform().processorKind().dataModel().wordWidth());
        _endiannessOption.setDefaultValue(defaultConfiguration.platform().processorKind().dataModel().endianness());
        _alignmentOption.setDefaultValue(defaultConfiguration.platform().processorKind().dataModel().alignment());
        _gripScheme.setDefaultValue(defaultConfiguration.gripPackage());
        _referenceScheme.setDefaultValue(defaultConfiguration.referencePackage());
        _layoutScheme.setDefaultValue(defaultConfiguration.layoutPackage());
        _heapScheme.setDefaultValue(defaultConfiguration.heapPackage());
        _monitorScheme.setDefaultValue(defaultConfiguration.monitorPackage());
        _compilerScheme.setDefaultValue(defaultConfiguration.compilerPackage());
        _jitScheme.setDefaultValue(defaultConfiguration.jitPackage());
        _interpreterScheme.setDefaultValue(defaultConfiguration.interpreterPackage());
        _trampolineScheme.setDefaultValue(defaultConfiguration.trampolinePackage());
        _targetABIsScheme.setDefaultValue(defaultConfiguration.targetABIsPackage());
        _runScheme.setDefaultValue(defaultConfiguration.runPackage());

        if (_threadsOption.getValue() <= 0) {
            throw new Option.Error("The value specified for " + _threadsOption + " must be greater than 0.");
        }

        final DataModel dataModel = new DataModel(_wordWidth.getValue(), _endiannessOption.getValue(), _alignmentOption.getValue());
        final ProcessorKind processorKind = new ProcessorKind(_processorModel.getValue(), _instructionSet.getValue(), dataModel);
        final OperatingSystem operatingSystem = _operatingSystem.getValue();
        int pageSizeValue = _pageSizeOption.getValue();
        if (!_options.hasOptionSpecified(_pageSizeOption.getName()) && operatingSystem != defaultConfiguration.platform().operatingSystem()) {
            pageSizeValue = operatingSystem.defaultPageSize();
        }
        final Platform platform = new Platform(processorKind, operatingSystem, pageSizeValue);
        return new VMConfiguration(_buildLevel.getValue(), platform,
                                    vm(_gripScheme), vm(_referenceScheme),
                                    vm(_layoutScheme), vm(_heapScheme),
                                    vm(_monitorScheme), vm(_compilerScheme), vm(_jitScheme), vm(_interpreterScheme), vm(_trampolineScheme), vm(_targetABIsScheme),
                                    vm(_runScheme));
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
     * Utility to report a throwable caught during the course of prototype generation.
     *
     * @param throwable the throwable caught
     */
    public static void reportThrowable(Throwable throwable) {
        System.out.flush();
        System.err.print("<" + PrototypeGenerator.class.getSimpleName() + "> : something went wrong");
        final int maxRecursion = 100;
        Throwable cause = throwable;
        for (int i = 0; i < maxRecursion; i++) {
            System.err.println(",");
            System.err.print("caused by: " + cause.getMessage());
            cause = cause.getCause();
            if (cause == null) {
                break;
            }
        }
        System.err.println(".");
        throwable.printStackTrace(System.err);
    }

    /**
     * Create the Java prototype, which includes the basic JDK and Maxine classes.
     *
     * @param vmConfiguration the default VM configuration
     * @param loadingPackages a boolean indicating whether to load the basic VM and JDK packages
     * @return a new Java prototype object
     */
    public JavaPrototype createJavaPrototype(VMConfiguration vmConfiguration, boolean loadingPackages) {
        try {
            return new JavaPrototype(createVMConfiguration(vmConfiguration), loadingPackages);
        } catch (Throwable throwable) {
            reportThrowable(throwable);
            ProgramError.unexpected("Java prototype failed");
            return null;
        }
    }

    /**
     * Creates the default VM configuration.
     *
     * @return the default VM configuration
     */
    private VMConfiguration createDefaultVMConfiguration() {
        return VMConfigurations.createStandardJit(BuildLevel.DEBUG, Platform.host());
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
    public GraphPrototype createGraphPrototype(final boolean tree) {
        HackJDK.checkVMFlags();
        final JavaPrototype javaPrototype = createJavaPrototype(true);
        try {
            javaPrototype.loadCoreJavaPackages();
            final Timer compileTimer = GlobalMetrics.newTimer("CompiledPrototype", Clock.SYSTEM_MILLISECONDS);
            final Timer graphTimer = GlobalMetrics.newTimer("GraphPrototype", Clock.SYSTEM_MILLISECONDS);

            return MaxineVM.usingTarget(new Function<GraphPrototype>() {
                public GraphPrototype call() {
                    GraphPrototype graphPrototype;
                    int numberOfClassActors = 0;
                    final CompiledPrototype compiledPrototype = new CompiledPrototype(javaPrototype, _threadsOption.getValue());
                    compiledPrototype.addEntrypoints();
                    do {
                        for (MethodActor methodActor : javaPrototype.vmConfiguration().runScheme().gatherNativeInitializationMethods()) {
                            compiledPrototype.add(methodActor, null, null);
                        }
                        numberOfClassActors = currentNumberOfClasses();
                        compileTimer.start();
                        if (compiledPrototype.compile()) {
                            compileTimer.stop();
                            graphTimer.start();
                            graphPrototype = new GraphPrototype(compiledPrototype, tree);
                            graphTimer.stop();
                        } else {
                            compileTimer.stop();
                        }
                    } while (currentNumberOfClasses() != numberOfClassActors);

                    compiledPrototype.compileUnsafeMethods();
                    compiledPrototype.link();

                    graphTimer.start();
                    graphPrototype = new GraphPrototype(compiledPrototype, tree);
                    graphTimer.stop();

                    Code.bootCodeRegion().trim();

                    JniNativeInterface.checkInvariants();

                    return graphPrototype;
                }
            });
        } catch (Throwable throwable) {
            reportThrowable(throwable);
            ProgramError.unexpected("prototype failed");
            return null;
        }
    }

    /**
     * Create a data prototype, by first building a graph prototype, then choosing layouts, and
     * eventually encoding the entire graph prototype into a binary form.
     *
     * @param tree a boolean indicating whether to produce an object tree
     * @return a completed data prototype
     */
    DataPrototype createDataPrototype(boolean tree) {
        final Timer dataTimer = GlobalMetrics.newTimer("DataPrototype", Clock.SYSTEM_MILLISECONDS);
        final GraphPrototype graphPrototype = createGraphPrototype(tree);
        dataTimer.start();
        final DataPrototype dataPrototype = new DataPrototype(graphPrototype, null);
        dataTimer.stop();
        return dataPrototype;
    }


    private int currentNumberOfClasses() {
        return ClassRegistry.vmClassRegistry().numberOfClassActors();
    }
}
