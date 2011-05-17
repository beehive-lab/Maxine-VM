/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm;

import static com.sun.max.vm.MaxineVM.*;
import static com.sun.max.vm.compiler.CallEntryPoint.*;

import java.io.*;
import java.util.*;

import com.sun.max.annotate.*;
import com.sun.max.config.*;
import com.sun.max.platform.*;
import com.sun.max.program.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.hosted.*;
import com.sun.max.vm.layout.*;
import com.sun.max.vm.monitor.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.run.*;
import com.sun.max.vm.runtime.*;

/**
 * The configuration of a VM is defined by:
 * <ul>
 * <li> a {@link BuildLevel}
 * <li> a set of {@link BootImagePackage boot-image packages} that will be in the boot image
 * <li> a specific subset of the boot-image packages that implement the {@link VMScheme} VM schemes.
 * </ul>
 * This information is all known at the time the instance is constructed.
 *
 * @author Bernd Mathiske
 * @author Ben L. Titzer
 * @author Doug Simon
 * @author Mick Jordan
 * @author Michael Haupt
 */
public final class VMConfiguration {

    public final BuildLevel buildLevel;

    @HOSTED_ONLY public final BootImagePackage referencePackage;
    @HOSTED_ONLY public final BootImagePackage layoutPackage;
    @HOSTED_ONLY public final BootImagePackage heapPackage;
    @HOSTED_ONLY public final BootImagePackage monitorPackage;
    @HOSTED_ONLY public final BootImagePackage compilationPackage;
    @HOSTED_ONLY public final BootImagePackage runPackage;
    @HOSTED_ONLY public final List<BootImagePackage> bootImagePackages;
    @HOSTED_ONLY private final Set<BootImagePackage> schemePackages = new HashSet<BootImagePackage>();

    private ArrayList<VMScheme> vmSchemes = new ArrayList<VMScheme>();
    private boolean areSchemesLoadedAndInstantiated = false;

    @CONSTANT_WHEN_NOT_ZERO private ReferenceScheme referenceScheme;
    @CONSTANT_WHEN_NOT_ZERO private LayoutScheme layoutScheme;
    @CONSTANT_WHEN_NOT_ZERO private HeapScheme heapScheme;
    @CONSTANT_WHEN_NOT_ZERO private MonitorScheme monitorScheme;
    @CONSTANT_WHEN_NOT_ZERO private CompilationScheme compilationScheme;
    @CONSTANT_WHEN_NOT_ZERO private RunScheme runScheme;

    public VMConfiguration(BuildLevel buildLevel,
                           Platform platform,
                           BootImagePackage referencePackage,
                           BootImagePackage layoutPackage,
                           BootImagePackage heapPackage,
                           BootImagePackage monitorPackage,
                           BootImagePackage compilationPackage,
                           BootImagePackage runPackage) {
        this.buildLevel = buildLevel;
        this.referencePackage = referencePackage;
        this.layoutPackage = layoutPackage;
        this.heapPackage = heapPackage;
        this.monitorPackage = monitorPackage;
        this.compilationPackage = compilationPackage;
        this.runPackage = runPackage;
        /**
         * We now gather all the packages that might be part of the VM boot image by scanning the class
         * path from the well-defined root ({@code com.sun.max.config}) and looking for {@code Package} classes,
         * instantiating them, and in the process possibly following new roots.
         * We then ask each package if it should be included in the image in this configuration by
         * invoking the {@code isPartOfMaxineVM} method. That method may, particularly if it is
         * in a scheme instance, need to ask questions about the configuration we are constructing.
         * Valid questions concern the values (names) of the scheme packages and the classes
         * registered as the scheme implementations. Since the schemes have not be instantiated
         * at this stage, scheme instances cannot be used. i.e., one must use {@link Class#isAssignableFrom}
         * rather than {@code instanceof}. The method {@link #schemeImplClassIsSubClass} can be used to
         * check whether a given class is the same as or a subclass of the registered class for a given scheme.
         */
        addSchemePackage(referencePackage);
        addSchemePackage(layoutPackage);
        addSchemePackage(heapPackage);
        addSchemePackage(monitorPackage);
        addSchemePackage(compilationPackage);
        addSchemePackage(runPackage);

        bootImagePackages = new ArrayList<BootImagePackage>();
        for (BootImagePackage pkg : BootImagePackage.getTransitiveSubPackages(
                        HostedBootClassLoader.HOSTED_BOOT_CLASS_LOADER.classpath(),
                        new com.sun.max.config.Package())) {

            if (pkg.isPartOfMaxineVM(this)) {
                bootImagePackages.add(pkg);
            }
        }
        MaxineVM.registerBootImagePackages(bootImagePackages);
    }

    private void addSchemePackage(BootImagePackage pkg) {
        if (pkg != null) {
            schemePackages.add(pkg);
        }
    }

    @INLINE public ReferenceScheme       referenceScheme()   { return referenceScheme;   }
    @INLINE public LayoutScheme          layoutScheme()      { return layoutScheme;      }
    @INLINE public HeapScheme            heapScheme()        { return heapScheme;        }
    @INLINE public MonitorScheme         monitorScheme()     { return monitorScheme;     }
    @INLINE public CompilationScheme     compilationScheme() { return compilationScheme; }
    @INLINE public RunScheme             runScheme()         { return runScheme;         }

    @HOSTED_ONLY
    public List<BootImagePackage> packages() {
        return Arrays.asList(new BootImagePackage[] {
            referencePackage,
            layoutPackage,
            heapPackage,
            monitorPackage,
            compilationPackage,
            runPackage});
    }

    public List<VMScheme> vmSchemes() {
        return vmSchemes;
    }

    /**
     * Checks whether the actual scheme class that implements a given scheme (class) in this configuration
     * is the same class or a subclass of another given class.
     * It is analogous to comparing the scheme package but allows an assignability check
     * within {@link BootImagePackage#isPartOfMaxineVM(VMConfiguration) before
     * the schemes are instantiated.
     * @param <S>
     * @param schemeClass the scheme class being searched for
     * @param schemeSubClass the scheme class being checked
     * @return true iff the actual implementation class is same as or a subclass of schemeSubClass
     */
    @HOSTED_ONLY
    public <S extends VMScheme> boolean schemeImplClassIsSubClass(Class<S>  schemeClass, Class<? extends S> schemeSubClass) {
        for (BootImagePackage pkg : schemePackages) {
            final Class<? extends S> result = pkg.schemeTypeToImplementation(schemeClass);
            if (result != null && schemeSubClass.isAssignableFrom(result)) {
                return true;
            }
        }
        return false;
    }

    @HOSTED_ONLY
    private <VMScheme_Type extends VMScheme> VMScheme_Type loadAndInstantiateScheme(List<VMScheme> loadedSchemes, BootImagePackage p, Class<VMScheme_Type> vmSchemeType) {
        if (p == null) {
            throw ProgramError.unexpected("Package not found for scheme: " + vmSchemeType.getSimpleName());
        }

        if (loadedSchemes != null) {
            Class< ? extends VMScheme_Type> impl = p.loadSchemeImplementation(vmSchemeType);
            for (VMScheme vmScheme : loadedSchemes) {
                if (vmScheme.getClass() == impl) {
                    vmSchemes.add(vmScheme);
                    return vmSchemeType.cast(vmScheme);
                }
            }
        }

        // If one implementation class in package p implements multiple schemes, then only a single
        // instance of that class is created and shared by the VM configuration for all the schemes
        // it implements.
        for (VMScheme vmScheme : vmSchemes) {
            if (vmSchemeType.isInstance(vmScheme) && p.loadSchemeImplementation(vmSchemeType).equals(vmScheme.getClass())) {
                return vmSchemeType.cast(vmScheme);
            }
        }

        final VMScheme_Type vmScheme = p.loadAndInstantiateScheme(vmSchemeType);
        vmSchemes.add(vmScheme);
        return vmScheme;
    }

    /**
     * Loads and instantiates all the schemes of this configuration.
     *
     * @param loadedSchemes the set of schemes already loaded and instantiated in this process. If non-{@code null},
     *            this list is used to prevent any given scheme implementation from being instantiated more than once.
     */
    @HOSTED_ONLY
    public void loadAndInstantiateSchemes(List<VMScheme> loadedSchemes) {
        if (areSchemesLoadedAndInstantiated) {
            return;
        }

        referenceScheme = loadAndInstantiateScheme(loadedSchemes, referencePackage, ReferenceScheme.class);
        layoutScheme = loadAndInstantiateScheme(loadedSchemes, layoutPackage, LayoutScheme.class);
        monitorScheme = loadAndInstantiateScheme(loadedSchemes, monitorPackage, MonitorScheme.class);
        heapScheme = loadAndInstantiateScheme(loadedSchemes, heapPackage, HeapScheme.class);
        compilationScheme = loadAndInstantiateScheme(loadedSchemes, compilationPackage, CompilationScheme.class);

        if (loadedSchemes == null) {
            // FIXME: This is a hack to avoid adding an "AdapterFrameScheme".
            if (needsAdapters()) {
                OPTIMIZED_ENTRY_POINT.init(8, 8);
                BASELINE_ENTRY_POINT.init(0, 0);
                VTABLE_ENTRY_POINT.init(OPTIMIZED_ENTRY_POINT);
                // Calls made from a C_ENTRY_POINT method link to the OPTIMIZED_ENTRY_POINT of the callee
                C_ENTRY_POINT.init(0, OPTIMIZED_ENTRY_POINT.offset());
            } else {
                CallEntryPoint.initAllToZero();
            }
        }

        runScheme = loadAndInstantiateScheme(loadedSchemes, runPackage, RunScheme.class);
        areSchemesLoadedAndInstantiated = true;
    }

    /**
     * Determines if any pair of compilers in this configuration use different
     * calling conventions and thus mandate the use of {@linkplain Adapter adapters}
     * to adapt the arguments when a call crosses a calling convention boundary.
     */
    public boolean needsAdapters() {
        return compilationScheme.needsAdapters();
    }

    public void initializeSchemes(MaxineVM.Phase phase) {
        for (int i = 0; i < vmSchemes.size(); i++) {
            try {
                //Log.print("Initializing: ");
                //Log.println(vmSchemes.get(i).name());
                vmSchemes.get(i).initialize(phase);
            } catch (Throwable t) {
                FatalError.unexpected("Error initializing scheme " + vmSchemes.get(i).name() + " in phase " + phase.name(), t);
            }
        }
    }

    /**
     * Convenience method for accessing the configuration associated with the
     * current {@linkplain MaxineVM#vm() VM} context.
     */
    @FOLD
    public static VMConfiguration vmConfig() {
        return vm().config;
    }

    @Override
    public String toString() {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        print(new PrintStream(baos), "");
        return baos.toString();
    }

    public void print(PrintStream out, String indent) {
        out.println(indent + "Build level: " + buildLevel);
        for (VMScheme vmScheme : vmSchemes()) {
            final String specification = vmScheme.specification().getSimpleName();
            out.println(indent + specification.replace("Scheme", " scheme") + ": " + vmScheme.about());
        }
    }

    /**
     * Use {@link MaxineVM#isDebug()} instead of calling this directly.
     */
    @FOLD
    public boolean debugging() {
        return buildLevel == BuildLevel.DEBUG;
    }
}
