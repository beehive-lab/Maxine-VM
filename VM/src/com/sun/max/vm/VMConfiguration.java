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
package com.sun.max.vm;

import java.io.*;

import com.sun.max.*;
import com.sun.max.annotate.*;
import com.sun.max.asm.*;
import com.sun.max.collect.*;
import com.sun.max.lang.*;
import com.sun.max.platform.*;
import com.sun.max.program.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.compiler.adaptive.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.grip.*;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.interpret.*;
import com.sun.max.vm.layout.*;
import com.sun.max.vm.monitor.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.run.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.trampoline.*;

/**
 * Aggregation of configuration choices for building a VM.
 * A VM can currently have up to two dynamic compilers: an optimizing compiler,
 * and an optional JIT compiler whose focus is speed of compilation.
 *
 * @author Bernd Mathiske
 */
public final class VMConfiguration {

    private final BuildLevel buildLevel;

    @INLINE
    public BuildLevel buildLevel() {
        return buildLevel;
    }

    private final Platform platform;

    @INLINE
    public Platform platform() {
        return platform;
    }

    private final VMPackage referencePackage;

    public VMPackage referencePackage() {
        return referencePackage;
    }

    @CONSTANT_WHEN_NOT_ZERO
    private ReferenceScheme referenceScheme = null;

    @INLINE
    public ReferenceScheme referenceScheme() {
        return referenceScheme;
    }

    private final VMPackage gripPackage;

    public VMPackage gripPackage() {
        return gripPackage;
    }

    @CONSTANT_WHEN_NOT_ZERO
    private GripScheme gripScheme = null;

    @INLINE
    public GripScheme gripScheme() {
        return gripScheme;
    }

    private final VMPackage layoutPackage;

    public VMPackage layoutPackage() {
        return layoutPackage;
    }

    @CONSTANT_WHEN_NOT_ZERO
    private LayoutScheme layoutScheme;

    @INLINE
    public LayoutScheme layoutScheme() {
        return layoutScheme;
    }

    public VMPackage heapPackage() {
        return heapPackage;
    }

    private final VMPackage heapPackage;

    @CONSTANT_WHEN_NOT_ZERO
    private HeapScheme heapScheme = null;

    @INLINE
    public HeapScheme heapScheme() {
        return heapScheme;
    }

    private final VMPackage monitorPackage;

    public VMPackage monitorPackage() {
        return monitorPackage;
    }

    @CONSTANT_WHEN_NOT_ZERO
    private MonitorScheme monitorScheme = null;

    @INLINE
    public MonitorScheme monitorScheme() {
        return monitorScheme;
    }

    private final VMPackage compilerPackage;

    public VMPackage compilerPackage() {
        return compilerPackage;
    }

    @CONSTANT_WHEN_NOT_ZERO
    private CompilerScheme compilerScheme = null;

    @INLINE
    public CompilerScheme compilerScheme() {
        return compilerScheme;
    }

    private final VMPackage jitPackage;

    public VMPackage jitPackage() {
        return jitPackage;
    }

    @CONSTANT_WHEN_NOT_ZERO
    private DynamicCompilerScheme jitScheme = null;

    @INLINE
    public DynamicCompilerScheme jitScheme() {
        return jitScheme;
    }

    private final VMPackage interpreterPackage;

    public VMPackage interpreterPackage() {
        return interpreterPackage;
    }

    @CONSTANT_WHEN_NOT_ZERO
    private InterpreterScheme interpreterScheme = null;

    @INLINE
    public InterpreterScheme interpreterScheme() {
        return interpreterScheme;
    }

    @CONSTANT_WHEN_NOT_ZERO
    private CompilationScheme compilationScheme = null;

    @INLINE
    public CompilationScheme compilationScheme() {
        return compilationScheme;
    }

    private final VMPackage trampolinePackage;

    public VMPackage trampolinePackage() {
        return trampolinePackage;
    }

    @CONSTANT_WHEN_NOT_ZERO
    private DynamicTrampolineScheme trampolineScheme = null;

    @INLINE
    public DynamicTrampolineScheme trampolineScheme() {
        return trampolineScheme;
    }

    private final VMPackage targetABIsPackage;

    public VMPackage targetABIsPackage() {
        return targetABIsPackage;
    }

    @CONSTANT_WHEN_NOT_ZERO
    private TargetABIsScheme targetABIsScheme = null;

    @INLINE
    public TargetABIsScheme  targetABIsScheme() {
        return targetABIsScheme;
    }

    private final VMPackage runPackage;

    public VMPackage runPackage() {
        return runPackage;
    }

    @CONSTANT_WHEN_NOT_ZERO
    private RunScheme runScheme = null;

    @INLINE
    public RunScheme runScheme() {
        return runScheme;
    }

    public Sequence<MaxPackage> packages() {
        return new ArraySequence<MaxPackage>(
                        referencePackage,
                        layoutPackage,
                        heapPackage,
                        monitorPackage,
                        compilerPackage,
                        trampolinePackage,
                        targetABIsPackage,
                        gripPackage,
                        runPackage);
    }

    private final Safepoint safepoint;

    @FOLD
    public Safepoint safepoint() {
        return safepoint;
    }

    /**
     * Configuration information for method entry points.
     * @see CallEntryPoint
     */
    private final int[] offsetsToCallEntryPoints;

    public int[] offsetToCallEntryPoints() {
        return offsetsToCallEntryPoints;
    }

    /**
     * Configuration information for method's callees entry points.
     * @see CallEntryPoint
     */
    private final int[] offsetsToCalleeEntryPoints;

    public int[] offsetsToCalleeEntryPoints() {
        return offsetsToCalleeEntryPoints;
    }

    public VMConfiguration(BuildLevel buildLevel, Platform platform, VMPackage gripPackage, VMPackage referencePackage, VMPackage layoutPackage, VMPackage heapPackage,
        VMPackage monitorPackage, VMPackage compilerPackage, VMPackage jitPackage, VMPackage interpreterPackage, VMPackage trampolinePackage, VMPackage targetABIsPackage, VMPackage runPackage) {
        this.buildLevel = buildLevel;
        this.platform = platform;
        this.gripPackage = gripPackage;
        this.referencePackage = referencePackage;
        this.layoutPackage = layoutPackage;
        this.heapPackage = heapPackage;
        this.monitorPackage = monitorPackage;
        this.compilerPackage = compilerPackage;
        this.jitPackage = jitPackage;
        this.interpreterPackage = interpreterPackage;
        this.trampolinePackage = trampolinePackage;
        this.targetABIsPackage = targetABIsPackage;
        this.runPackage = runPackage;
        this.safepoint = Safepoint.create(this);
        // FIXME: This is a hack to avoid adding an "AdapterFrameScheme".
        // It is useful for now to build a VM with a single compiler, where the JIT and optimizing compiler are the same.
        // The CallEntryPoint enum gets the value of the call entry point offset from offsetToCallEntryPoints()
        // Ideally, we would want to get it from adapterFrameScheme().offsetToCallEntryPoints()
        if (jitPackage() == null || jitPackage().equals(compilerPackage())) {
            // zero-fill array -- all entry points are at code start (for now -- may change with inline caches).
            this.offsetsToCallEntryPoints = new int[CallEntryPoint.VALUES.length()];
            this.offsetsToCalleeEntryPoints = new int[CallEntryPoint.VALUES.length()];
        } else {
            final int offsetToOptimizedEntryPoint = WordWidth.BITS_8.numberOfBytes * 8;
            final int offsetToJitEntryPoint = 0;
            final int offsetToVtableEntryPoint = offsetToOptimizedEntryPoint;
            final int offsetToCEntryPoint = 0;
            final int offsetToInterpreterEntryPoint = 0;
            this.offsetsToCallEntryPoints = new int[]{offsetToVtableEntryPoint,  offsetToJitEntryPoint, offsetToOptimizedEntryPoint, offsetToCEntryPoint, offsetToInterpreterEntryPoint};
            // Callees have the same entry point as their caller, except for C_ENTRY_POINT, which has the C_OPTIMIZED_ENTRY_POINT
            this.offsetsToCalleeEntryPoints = new int[]{offsetToVtableEntryPoint,  offsetToJitEntryPoint, offsetToOptimizedEntryPoint, offsetToOptimizedEntryPoint, offsetToInterpreterEntryPoint};
        }
    }

    private AppendableIndexedSequence<VMScheme> vmSchemes = new ArrayListSequence<VMScheme>();

    public Sequence<VMScheme> vmSchemes() {
        return vmSchemes;
    }

    public synchronized <VMScheme_Type extends VMScheme> VMScheme_Type loadAndInstantiateScheme(MaxPackage p, Class<VMScheme_Type> vmSchemeType, Object... arguments) {
        if (p == null) {
            ProgramError.unexpected("Package not found for scheme: " + vmSchemeType.getSimpleName());
        }
        final VMScheme_Type vmScheme = p.loadAndInstantiateScheme(vmSchemeType, arguments);
        vmSchemes.append(vmScheme);
        return vmScheme;
    }

    private boolean areSchemesLoadedAndInstantiated = false;

    public void loadAndInstantiateSchemes() {
        if (areSchemesLoadedAndInstantiated) {
            return;
        }
        gripScheme = loadAndInstantiateScheme(gripPackage(), GripScheme.class, this);
        referenceScheme = loadAndInstantiateScheme(referencePackage(), ReferenceScheme.class, this);
        layoutScheme = loadAndInstantiateScheme(layoutPackage, LayoutScheme.class, this, gripScheme);
        monitorScheme = loadAndInstantiateScheme(monitorPackage(), MonitorScheme.class, this);
        heapScheme = loadAndInstantiateScheme(heapPackage(), HeapScheme.class, this);
        targetABIsScheme = loadAndInstantiateScheme(targetABIsPackage(), TargetABIsScheme.class, this);
        compilerScheme = loadAndInstantiateScheme(compilerPackage(), CompilerScheme.class, this);
        trampolineScheme = loadAndInstantiateScheme(trampolinePackage(), DynamicTrampolineScheme.class, this);
        if (jitPackage() != null) {
            jitScheme = loadAndInstantiateScheme(jitPackage(), DynamicCompilerScheme.class, this);
        } else {
            // no JIT, always using the optimizing compiler
            jitScheme = compilerScheme;
        }
        interpreterScheme = loadAndInstantiateScheme(interpreterPackage(), InterpreterScheme.class, this);

        compilationScheme = new AdaptiveCompilationScheme(this);
        vmSchemes.append(compilationScheme);

        runScheme = loadAndInstantiateScheme(runPackage(), RunScheme.class, this);
        areSchemesLoadedAndInstantiated = true;
    }

    public void initializeSchemes(MaxineVM.Phase phase) {
        for (int i = 0; i < vmSchemes.length(); i++) {
            vmSchemes.get(i).initialize(phase);
        }
    }

    public void finalizeSchemes(MaxineVM.Phase phase) {
        for (int i = 0; i < vmSchemes.length(); i++) {
            vmSchemes.get(i).finalize(phase);
        }
    }

    public static VMConfiguration host() {
        return MaxineVM.host().configuration();
    }

    @FOLD
    public static VMConfiguration target() {
        return MaxineVM.target().configuration();
    }

    @UNSAFE
    @FOLD
    public static VMConfiguration hostOrTarget() {
        return MaxineVM.hostOrTarget().configuration();
    }

    @Override
    public String toString() {
        final CharArrayWriter charArrayWriter = new CharArrayWriter();
        print(new PrintWriter(charArrayWriter));
        return charArrayWriter.toString();
    }

    public void print(PrintWriter writer) {
        writer.println("build level: " + buildLevel());
        writer.println("platform: " + platform());
        for (VMScheme vmScheme : vmSchemes()) {
            final String specification = vmScheme.specification().getSimpleName();
            writer.println(specification.replace("Scheme", " scheme") + ": " + vmScheme.getClass().getPackage().getName());
        }
    }

    @FOLD
    public boolean debugging() {
        return buildLevel() == BuildLevel.DEBUG;
    }

    @INLINE
    public WordWidth wordWidth() {
        return platform().processorKind().dataModel().wordWidth();
    }

    /**
     * Determines if a given package is considered part of the VM under this VM configuration.
     */
    public boolean isMaxineVMPackage(MaxPackage maxPackage) {
        if (maxPackage instanceof BasePackage) {
            return true;
        }
        if (maxPackage instanceof AsmPackage) {
            final AsmPackage asmPackage = (AsmPackage) maxPackage;
            return asmPackage.isPartOfAssembler(platform().processorKind().instructionSet());
        }
        if (maxPackage instanceof VMPackage) {
            final VMPackage vmPackage = (VMPackage) maxPackage;
            return vmPackage.isPartOfMaxineVM(this);
        }
        return false;
    }

}
