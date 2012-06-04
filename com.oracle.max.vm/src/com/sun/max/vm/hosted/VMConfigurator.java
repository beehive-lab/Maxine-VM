/*
 * Copyright (c) 2010, 2011, Oracle and/or its affiliates. All rights reserved.
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

import static com.sun.max.lang.Classes.*;
import static com.sun.max.platform.Platform.*;

import com.sun.max.config.*;
import com.sun.max.lang.ISA.Category;
import com.sun.max.program.option.*;
import com.sun.max.vm.*;
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
    public final Option<String> runScheme = schemeOption("run", RunScheme.class, "Specifies the run scheme for the target.",
            VMConfigurator.defaultRunScheme());

    private Option<String> schemeOption(String name, Class schemeClass, String help, BootImagePackage def) {
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
     */
    public MaxineVM create() {
        VMConfiguration config = new VMConfiguration(buildLevel.getValue(), platform(),
                                    vm(referenceScheme),
                                    vm(layoutScheme),
                                    vm(heapScheme),
                                    vm(monitorScheme),
                                    vm(runScheme));
        MaxineVM vm = new MaxineVM(config);
        MaxineVM.set(vm);
        config.gatherBootImagePackages();
        config.loadAndInstantiateSchemes(null);
        return vm;
    }

    /**
     * Gets the package providing the default {@link TargetABIsScheme}.
     */
    public static BootImagePackage defaultTargetABIsScheme() {
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
    public static BootImagePackage defaultHeapScheme() {
        return new com.sun.max.vm.heap.sequential.semiSpace.Package();
    }

    /**
     * Gets the package providing the default {@link ReferenceScheme}.
     */
    public static BootImagePackage defaultReferenceScheme() {
        return new com.sun.max.vm.reference.direct.Package();
    }

    /**
     * Gets the package providing the default {@link LayoutScheme}.
     */
    public static BootImagePackage defaultLayoutScheme() {
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
    public static BootImagePackage defaultRunScheme() {
        return new com.sun.max.vm.run.java.Package();
    }

    /**
     * Gets the package providing the default {@link MonitorScheme}.
     */
    public static BootImagePackage defaultMonitorScheme() {
        return new com.sun.max.vm.monitor.modal.schemes.thin_inflated.Package();
    }

    /**
     * Creates and {@linkplain MaxineVM#set(MaxineVM) installs} a VM using all the defaults
     * except for a supplied build level.
     */
    public static void installStandard(BuildLevel buildLevel) {
        VMConfigurator vmConfigurator = new VMConfigurator(null);
        vmConfigurator.buildLevel.setValue(buildLevel);
        vmConfigurator.create();
    }

    /**
     * Cast the value of an option to a {@code BootImagePackage}.
     * @param option the option which contains the value
     * @return the option's value casted to a {@code BootImagePackage}
     */
    private static BootImagePackage vm(Option<String> option) {
        String value = option.getValue();
        if (value == null) {
            return null;
        }
        return BootImagePackage.fromName(value);
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
            MaxineVM vm = configurator.create();
            System.out.println(vm.config);
        }
    }
}
