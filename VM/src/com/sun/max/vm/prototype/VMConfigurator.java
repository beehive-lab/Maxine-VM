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

import static com.sun.max.platform.Platform.*;
import static com.sun.max.vm.VMConfigurations.*;

import com.sun.max.*;
import com.sun.max.program.option.*;
import com.sun.max.vm.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.grip.*;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.layout.*;
import com.sun.max.vm.monitor.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.run.*;
import com.sun.max.vm.trampoline.*;

/**
 * This class constructs a {@linkplain MaxineVM VM} from a number of command line options.
 *
 * @author Doug Simon
 */
public final class VMConfigurator {

    public final OptionSet options = new OptionSet();

    public final Option<BuildLevel> buildLevel = options.newEnumOption("build", BuildLevel.PRODUCT, BuildLevel.class,
            "This option selects the build level of the virtual machine.");
    public final Option<MaxPackage> gripScheme = schemeOption("grip", new com.sun.max.vm.grip.Package(), GripScheme.class,
            "Specifies the grip scheme for the target.", defaultGripScheme());
    public final Option<MaxPackage> referenceScheme = schemeOption("reference", new com.sun.max.vm.reference.Package(), ReferenceScheme.class,
            "Specifies the reference scheme for the target.", defaultReferenceScheme());
    public final Option<MaxPackage> layoutScheme = schemeOption("layout", new com.sun.max.vm.layout.Package(), LayoutScheme.class,
            "Specifies the layout scheme for the target.", defaultLayoutScheme());
    public final Option<MaxPackage> heapScheme = schemeOption("heap", new com.sun.max.vm.heap.Package(), HeapScheme.class,
            "Specifies the heap scheme for the target.", defaultHeapScheme());
    public final Option<MaxPackage> monitorScheme = schemeOption("monitor", new com.sun.max.vm.monitor.Package(), MonitorScheme.class,
            "Specifies the monitor scheme for the target.", defaultMonitorScheme());
    public final Option<MaxPackage> bootScheme = schemeOption("boot", new com.sun.max.vm.compiler.Package(), BootstrapCompilerScheme.class,
            "Specifies the boot compiler scheme for the target.", defaultCompilerScheme());
    public final Option<MaxPackage> optScheme = schemeOption("opt", new com.sun.max.vm.compiler.Package(), RuntimeCompilerScheme.class,
            "Specifies the optimizing compiler scheme for the target.", defaultCompilerScheme());
    public final Option<MaxPackage> jitScheme = schemeOption("jit", MaxPackage.fromName("com.sun.max.vm.jit"), RuntimeCompilerScheme.class,
            "Specifies the JIT scheme for the target.", defaultJitCompilerScheme());
    public final Option<MaxPackage> trampolineScheme = schemeOption("trampoline", new com.sun.max.vm.trampoline.Package(), DynamicTrampolineScheme.class,
            "Specifies the dynamic trampoline scheme for the target.", defaultTrampolineScheme());
    public final Option<MaxPackage> targetABIsScheme = schemeOption("abi", new com.sun.max.vm.compiler.target.Package(), TargetABIsScheme.class,
            "Specifies the ABIs scheme for the target", defaultTargetABIsScheme());
    public final Option<MaxPackage> runScheme = schemeOption("run", new com.sun.max.vm.run.Package(), RunScheme.class,
            "Specifies the run scheme for the target.", defaultRunScheme());

    private Option<MaxPackage> schemeOption(String name, MaxPackage superPackage, Class cl, String help, VMPackage def) {
        return options.newOption(name, (MaxPackage) def, new MaxPackageOptionType(superPackage, cl), OptionSet.Syntax.REQUIRES_EQUALS, help);
    }

    /**
     * Creates a new prototype generator.
     *
     * @param optionSet the set of options to which the prototype generator specific options will be added
     */
    public VMConfigurator(OptionSet optionSet) {
        optionSet.addOptions(options);
    }

    /**
     * Creates a VM from the current option set.
     *
     * @param install specifies if the created VM should be {@linkplain MaxineVM#set(MaxineVM) set} as the global VM
     *            context.
     */
    public MaxineVM create(boolean install) {
        VMConfiguration config = new VMConfiguration(buildLevel.getValue(), platform(),
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
        MaxineVM vm = new MaxineVM(config);
        if (install) {
            MaxineVM.set(vm);
        }
        return vm;
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
        VMConfigurator configurator = new VMConfigurator(new OptionSet());
        if (args.length == 0) {
            configurator.options.printHelp(System.out, 80);
        } else {
            configurator.options.parseArguments(args);
            MaxineVM vm = configurator.create(false);
            System.out.println(vm.config);
        }
    }
}
