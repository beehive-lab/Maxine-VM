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
package com.sun.max.vm.hosted;

import static com.sun.max.platform.Platform.*;

import com.sun.max.*;
import com.sun.max.asm.ISA.Category;
import com.sun.max.program.option.*;
import com.sun.max.vm.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.layout.*;
import com.sun.max.vm.monitor.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.run.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.trampoline.*;

/**
 * This class is used to create and install a {@linkplain MaxineVM VM} whose configuration
 * can be specified by a number of command line options. It also contains static methods to
 * create and install "standard" configurations. Lastly, it defines the default implementations
 * for the VM schemes.
 *
 * @author Doug Simon
 */
public final class VMConfigurator {

    public final OptionSet options = new OptionSet();

    public final Option<BuildLevel> buildLevel = options.newEnumOption("build", BuildLevel.PRODUCT, BuildLevel.class,
            "This option selects the build level of the virtual machine.");
    public final Option<MaxPackage> referenceScheme = schemeOption("reference", new com.sun.max.vm.reference.Package(), ReferenceScheme.class,
            "Specifies the reference scheme for the target.", VMConfigurator.defaultReferenceScheme());
    public final Option<MaxPackage> layoutScheme = schemeOption("layout", new com.sun.max.vm.layout.Package(), LayoutScheme.class,
            "Specifies the layout scheme for the target.", VMConfigurator.defaultLayoutScheme());
    public final Option<MaxPackage> heapScheme = schemeOption("heap", new com.sun.max.vm.heap.Package(), HeapScheme.class,
            "Specifies the heap scheme for the target.", VMConfigurator.defaultHeapScheme());
    public final Option<MaxPackage> monitorScheme = schemeOption("monitor", new com.sun.max.vm.monitor.Package(), MonitorScheme.class,
            "Specifies the monitor scheme for the target.", VMConfigurator.defaultMonitorScheme());
    public final Option<MaxPackage> bootScheme = schemeOption("boot", new com.sun.max.vm.compiler.Package(), BootstrapCompilerScheme.class,
            "Specifies the boot compiler scheme for the target.", VMConfigurator.defaultCompilerScheme());
    public final Option<MaxPackage> optScheme = schemeOption("opt", new com.sun.max.vm.compiler.Package(), RuntimeCompilerScheme.class,
            "Specifies the optimizing compiler scheme for the target.", VMConfigurator.defaultCompilerScheme());
    public final Option<MaxPackage> jitScheme = schemeOption("jit", new com.sun.max.vm.compiler.Package(), RuntimeCompilerScheme.class,
            "Specifies the JIT scheme for the target.", VMConfigurator.defaultJitCompilerScheme());
    public final Option<MaxPackage> compScheme = schemeOption("comp", new com.sun.max.vm.compiler.Package(), CompilationScheme.class,
            "Specifies the compilation scheme for the target.", VMConfigurator.defaultCompilationScheme());
    public final Option<MaxPackage> trampolineScheme = schemeOption("trampoline", new com.sun.max.vm.trampoline.Package(), DynamicTrampolineScheme.class,
            "Specifies the dynamic trampoline scheme for the target.", VMConfigurator.defaultTrampolineScheme());
    public final Option<MaxPackage> targetABIsScheme = schemeOption("abi", new com.sun.max.vm.compiler.target.Package(), TargetABIsScheme.class,
            "Specifies the ABIs scheme for the target", VMConfigurator.defaultTargetABIsScheme());
    public final Option<MaxPackage> runScheme = schemeOption("run", new com.sun.max.vm.run.Package(), RunScheme.class,
            "Specifies the run scheme for the target.", VMConfigurator.defaultRunScheme());

    private Option<MaxPackage> schemeOption(String name, MaxPackage superPackage, Class cl, String help, VMPackage def) {
        return options.newOption(name, (MaxPackage) def, new MaxPackageOptionType(superPackage, cl), OptionSet.Syntax.REQUIRES_EQUALS, help);
    }

    /**
     * Creates a VMConfiurator.
     *
     * @param externalOptions if non-{@code null}, then the command line options encapsulated by the new VMConfigurator
     *            are added to this set of options
     */
    public VMConfigurator(OptionSet externalOptions) {
        if (externalOptions != null) {
            externalOptions.addOptions(options);
        }
    }

    /**
     * Creates a VM from the current option set.
     *
     * @param install specifies if the created VM should be {@linkplain MaxineVM#set(MaxineVM) set} as the global VM
     *            context. If {@code true}, then the schemes in the VM configuration are also
     *            {@linkplain VMConfiguration#loadAndInstantiateSchemes(VMConfiguration) loaded and instantiated}.
     */
    public MaxineVM create(boolean install) {
        VMConfiguration config = new VMConfiguration(buildLevel.getValue(), platform(),
                                    vm(referenceScheme),
                                    vm(layoutScheme),
                                    vm(heapScheme),
                                    vm(monitorScheme),
                                    vm(bootScheme),
                                    vm(jitScheme),
                                    vm(optScheme),
                                    vm(compScheme),
                                    vm(trampolineScheme),
                                    vm(targetABIsScheme), vm(runScheme));
        MaxineVM vm = new MaxineVM(config);
        if (install) {
            MaxineVM.set(vm);
            config.loadAndInstantiateSchemes(null);
        }
        return vm;
    }

    /**
     * Gets the package providing the default {@link BootstrapCompilerScheme}.
     */
    public static VMPackage defaultCompilerScheme() {
        switch (platform().isa()) {
            case AMD64:
                return (VMPackage) MaxPackage.fromName("com.sun.max.vm.cps.b.c.d.e.amd64.target");
            default:
                throw FatalError.unexpected(platform().isa().toString());
        }
    }

    /**
     * Gets the package providing the default {@link RuntimeCompilerScheme}.
     */
    public static VMPackage defaultJitCompilerScheme() {
        switch (platform().isa()) {
            case AMD64:
                return (VMPackage) MaxPackage.fromName("com.sun.max.vm.cps.jit.amd64");
            default:
                throw FatalError.unimplemented();
        }
    }

    /**
     * Gets the package providing the default {@link CompilationScheme}.
     */
    public static VMPackage defaultCompilationScheme() {
        return new com.sun.max.vm.compiler.adaptive.Package();
    }

    /**
     * Gets the package providing the default {@link TargetABIsScheme}.
     */
    public static VMPackage defaultTargetABIsScheme() {
        switch (platform().isa()) {
            case AMD64:
                return new com.sun.max.vm.compiler.target.amd64.Package();
            default:
                throw FatalError.unimplemented();
        }
    }

    /**
     * Gets the package providing the default {@link HeapScheme}.
     */
    public static VMPackage defaultHeapScheme() {
        return new com.sun.max.vm.heap.sequential.semiSpace.Package();
    }

    /**
     * Gets the package providing the default {@link ReferenceScheme}.
     */
    public static VMPackage defaultReferenceScheme() {
        return new com.sun.max.vm.reference.direct.Package();
    }

    /**
     * Gets the package providing the default {@link LayoutScheme}.
     */
    public static VMPackage defaultLayoutScheme() {
        if (platform().isa().category == Category.RISC) {
            // On SPARC, the HOM layout enables more optimized code for accessing array elements
            // smaller than a word as there is no need to perform address arithmetic to skip
            // over the header; the origin is pointing at array element 0.
            // A disadvantage of HOM is that the cell and origin addresses
            // of an object are not one and the same (which they are for OHM)
            // and converting between them requires reading memory.
            return new com.sun.max.vm.layout.hom.Package();
        }
        return new com.sun.max.vm.layout.ohm.Package();
    }

    /**
     * Gets the package providing the default {@link RunScheme}.
     */
    public static VMPackage defaultRunScheme() {
        return new com.sun.max.vm.run.java.Package();
    }

    /**
     * Gets the package providing the default {@link MonitorScheme}.
     */
    public static VMPackage defaultMonitorScheme() {
        return new com.sun.max.vm.monitor.modal.schemes.thin_inflated.Package();
    }

    /**
     * Gets the package providing the default {@link DynamicTrampolineScheme}.
     */
    public static VMPackage defaultTrampolineScheme() {
        return new com.sun.max.vm.trampoline.template.Package();
    }

    public static void installStandardJit(BuildLevel buildLevel) {
        installStandard(buildLevel);
//        VMConfigurator vmConfigurator = new VMConfigurator(null);
//        vmConfigurator.buildLevel.setValue(buildLevel);
//        vmConfigurator.jitScheme.setValue(defaultJitCompilerScheme());
//        vmConfigurator.create(true);
    }

    /**
     * Creates and {@linkplain MaxineVM#set(MaxineVM) installs} a VM using all the defaults
     * except for a supplied build level.
     */
    public static void installStandard(BuildLevel buildLevel) {
        installStandard(buildLevel, defaultCompilerScheme());
    }

    /**
     * Creates and {@linkplain MaxineVM#set(MaxineVM) installs} a VM using all the defaults
     * except for a supplied compiler scheme implementation and build level.
     */
    public static void installStandard(BuildLevel buildLevel, VMPackage compilerPackage) {
        VMConfigurator vmConfigurator = new VMConfigurator(null);
        vmConfigurator.buildLevel.setValue(buildLevel);
        vmConfigurator.bootScheme.setValue(compilerPackage);
        vmConfigurator.jitScheme.setValue(null);
        vmConfigurator.optScheme.setValue(null);
        vmConfigurator.create(true);
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
     * The command line interface for either printing the help message or
     * printing a VM configuration corresponding to the command line
     * parameters.
     *
     * @param args if {@code args.length == 0}, then help message is printed to {@link System#out}
     * otherwise they are used to configure a VM whose configuration will be printed to {@link System#out}
     */
    public static void main(String[] args) {
        VMConfigurator configurator = new VMConfigurator(null);
        if (args.length == 0) {
            configurator.options.printHelp(System.out, 80);
        } else {
            configurator.options.parseArguments(args);
            MaxineVM vm = configurator.create(true);
            System.out.println(vm.config);
        }
    }
}
