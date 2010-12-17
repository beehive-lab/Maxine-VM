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

import static com.sun.max.lang.Classes.*;
import static com.sun.max.platform.Platform.*;

import com.sun.max.config.*;
import com.sun.max.ide.*;
import com.sun.max.lang.ISA.Category;
import com.sun.max.program.option.*;
import com.sun.max.vm.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.layout.*;
import com.sun.max.vm.monitor.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.run.*;
import com.sun.max.vm.runtime.*;

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
    public final Option<String> referenceScheme = schemeOption("reference", ReferenceScheme.class, "Specifies the reference scheme for the target.",
            VMConfigurator.defaultReferenceScheme());
    public final Option<String> layoutScheme = schemeOption("layout", LayoutScheme.class, "Specifies the layout scheme for the target.",
            VMConfigurator.defaultLayoutScheme());
    public final Option<String> heapScheme = schemeOption("heap", HeapScheme.class, "Specifies the heap scheme for the target.",
            VMConfigurator.defaultHeapScheme());
    public final Option<String> monitorScheme = schemeOption("monitor", MonitorScheme.class, "Specifies the monitor scheme for the target.",
            VMConfigurator.defaultMonitorScheme());
    public final Option<String> optScheme = schemeOption("opt", RuntimeCompilerScheme.class, "Specifies the optimizing compiler scheme for the target.",
            VMConfigurator.defaultOptCompilerScheme());
    public final Option<String> jitScheme = schemeOption("jit", RuntimeCompilerScheme.class, "Specifies the JIT scheme for the target.",
            VMConfigurator.defaultJitCompilerScheme());
    public final Option<String> compScheme = schemeOption("comp", CompilationScheme.class, "Specifies the compilation scheme for the target.",
            VMConfigurator.defaultCompilationScheme());
    public final Option<String> runScheme = schemeOption("run", RunScheme.class, "Specifies the run scheme for the target.",
            VMConfigurator.defaultRunScheme());

    private Option<String> schemeOption(String name, Class schemeClass, String help, VMPackage def) {
        String p = def.name();
        return options.newOption(name, p, new PackageOptionType(getPackageName(schemeClass)), OptionSet.Syntax.REQUIRES_EQUALS, help);
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
                                    vm(optScheme),
                                    vm(jitScheme),
                                    vm(compScheme),
                                    vm(runScheme));
        MaxineVM vm = new MaxineVM(config);
        if (install) {
            MaxineVM.set(vm);
            config.loadAndInstantiateSchemes(null);
        }
        return vm;
    }

    /**
     * Gets the package providing the default {@link VMConfiguration#optCompilerScheme()}.
     */
    public static VMPackage defaultOptCompilerScheme() {
        switch (platform().isa) {
            case AMD64:
                return new com.sun.max.vm.compiler.c1x.Package();
            default:
                throw FatalError.unexpected(platform().isa.toString());
        }
    }

    /**
     * Gets the package providing the default {@link VMConfiguration#jitCompilerScheme()}.
     */
    public static VMPackage defaultJitCompilerScheme() {
        switch (platform().isa) {
            case AMD64:
                VMPackage def = (VMPackage) BootImagePackage.fromName("com.sun.max.vm.cps.jit.amd64");
                assert def != null : "need to modify class path to include " + JavaProject.findWorkspaceDirectory() + "/CPS/bin";
                return def;
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
        switch (platform().isa) {
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
        if (platform().isa.category == Category.RISC) {
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
     * Creates and {@linkplain MaxineVM#set(MaxineVM) installs} a VM using all the defaults
     * except for a supplied build level.
     */
    public static void installStandard(BuildLevel buildLevel) {
        installStandard(buildLevel, defaultOptCompilerScheme());
    }

    /**
     * Creates and {@linkplain MaxineVM#set(MaxineVM) installs} a VM using all the defaults
     * except for a supplied compiler scheme implementation and build level.
     */
    public static void installStandard(BuildLevel buildLevel, VMPackage optPackage) {
        VMConfigurator vmConfigurator = new VMConfigurator(null);
        vmConfigurator.buildLevel.setValue(buildLevel);
        vmConfigurator.optScheme.setValue(optPackage.name());
        vmConfigurator.jitScheme.setValue(null);
        vmConfigurator.create(true);
    }

    /**
     * Cast the value of an option to a {@code VMPackage}.
     * @param option the option which contains the value
     * @return the option's value casted to a {@code VMPackage}
     */
    private static VMPackage vm(Option<String> option) {
        String value = option.getValue();
        if (value == null) {
            return null;
        }
        return (VMPackage) BootImagePackage.fromName(value);
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
