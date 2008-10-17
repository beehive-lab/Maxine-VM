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
import com.sun.max.vm.layout.*;
import com.sun.max.vm.monitor.*;
import com.sun.max.vm.profile.*;
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


    private final BuildLevel _buildLevel;

    @INLINE
    public BuildLevel buildLevel() {
        return _buildLevel;
    }

    private final Platform _platform;

    @INLINE
    public Platform platform() {
        return _platform;
    }

    private final VMPackage _referencePackage;

    public VMPackage referencePackage() {
        return _referencePackage;
    }

    @CONSTANT_WHEN_NOT_ZERO
    private ReferenceScheme _referenceScheme = null;

    @INLINE
    public ReferenceScheme referenceScheme() {
        return _referenceScheme;
    }

    private final VMPackage _gripPackage;

    public VMPackage gripPackage() {
        return _gripPackage;
    }

    @CONSTANT_WHEN_NOT_ZERO
    private GripScheme _gripScheme = null;

    @INLINE
    public GripScheme gripScheme() {
        return _gripScheme;
    }

    private final VMPackage _layoutPackage;

    public VMPackage layoutPackage() {
        return _layoutPackage;
    }

    @CONSTANT_WHEN_NOT_ZERO
    private LayoutScheme _layoutScheme = null;

    @INLINE
    public LayoutScheme layoutScheme() {
        return _layoutScheme;
    }

    private final VMPackage _heapPackage;

    public VMPackage heapPackage() {
        return _heapPackage;
    }

    @CONSTANT_WHEN_NOT_ZERO
    private HeapScheme _heapScheme = null;

    @INLINE
    public HeapScheme heapScheme() {
        return _heapScheme;
    }

    private final VMPackage _monitorPackage;

    public VMPackage monitorPackage() {
        return _monitorPackage;
    }

    @CONSTANT_WHEN_NOT_ZERO
    private MonitorScheme _monitorScheme = null;

    @INLINE
    public MonitorScheme monitorScheme() {
        return _monitorScheme;
    }

    private final VMPackage _compilerPackage;

    public VMPackage compilerPackage() {
        return _compilerPackage;
    }

    @CONSTANT_WHEN_NOT_ZERO
    private CompilerScheme _compilerScheme = null;

    @INLINE
    public CompilerScheme compilerScheme() {
        return _compilerScheme;
    }

    private final VMPackage _jitPackage;

    public VMPackage jitPackage() {
        return _jitPackage;
    }

    @CONSTANT_WHEN_NOT_ZERO
    private DynamicCompilerScheme _jitScheme = null;

    @INLINE
    public DynamicCompilerScheme jitScheme() {
        return _jitScheme;
    }

    @CONSTANT_WHEN_NOT_ZERO
    private CompilationScheme _compilationScheme = null;

    @CONSTANT_WHEN_NOT_ZERO
    private ProfilingScheme _profilingScheme = null;

    @INLINE
    public CompilationScheme compilationScheme() {
        return _compilationScheme;
    }

    private VMPackage _trampolinePackage;

    public VMPackage trampolinePackage() {
        return _trampolinePackage;
    }

    @CONSTANT_WHEN_NOT_ZERO
    private DynamicTrampolineScheme _trampolineScheme = null;

    @INLINE
    public DynamicTrampolineScheme trampolineScheme() {
        return _trampolineScheme;
    }

    private VMPackage _targetABIsPackage;

    public VMPackage targetABIsPackage() {
        return _targetABIsPackage;
    }

    @CONSTANT_WHEN_NOT_ZERO
    private TargetABIsScheme _targetABIsScheme = null;

    @INLINE
    public TargetABIsScheme  targetABIsScheme() {
        return _targetABIsScheme;
    }

    private final VMPackage _runPackage;

    public VMPackage runPackage() {
        return _runPackage;
    }

    @CONSTANT_WHEN_NOT_ZERO
    private RunScheme _runScheme = null;

    @INLINE
    public RunScheme runScheme() {
        return _runScheme;
    }

    public Sequence<MaxPackage> packages() {
        return new ArraySequence<MaxPackage>(referencePackage(), layoutPackage(), heapPackage(),  monitorPackage(), compilerPackage(),  trampolinePackage(), targetABIsPackage(), gripPackage(), runPackage());
    }

    private final Safepoint _safepoint;

    @FOLD
    public Safepoint safepoint() {
        return _safepoint;
    }

    /**
     * Configuration information for method entry points.
     * @see CallEntryPoint
     */
    private final int[] _offsetsToCallEntryPoints;

    public int[] offsetToCallEntryPoints() {
        return _offsetsToCallEntryPoints;
    }

    /**
     * Configuration information for method's callees entry points.
     * @see CallEntryPoint
     */
    private final int[] _offsetsToCalleeEntryPoints;

    public int[] offsetsToCalleeEntryPoints() {
        return _offsetsToCalleeEntryPoints;
    }

    public VMConfiguration(BuildLevel buildLevel, Platform platform, VMPackage gripPackage, VMPackage referencePackage, VMPackage layoutPackage, VMPackage heapPackage,
        VMPackage monitorPackage, VMPackage compilerPackage, VMPackage jitPackage, VMPackage trampolinePackage, VMPackage targetABIsPackage, VMPackage runPackage) {
        _buildLevel = buildLevel;
        _platform = platform;
        _gripPackage = gripPackage;
        _referencePackage = referencePackage;
        _layoutPackage = layoutPackage;
        _heapPackage = heapPackage;
        _monitorPackage = monitorPackage;
        _compilerPackage = compilerPackage;
        _jitPackage = jitPackage;
        _trampolinePackage = trampolinePackage;
        _targetABIsPackage = targetABIsPackage;
        _runPackage = runPackage;
        _safepoint = Safepoint.create(this);
        // FIXME: This is a hack to avoid adding an "AdapterFrameScheme".
        // It is useful for now to build a VM with a single compiler, where the JIT and optimizing compiler are the same.
        // The CallEntryPoint enum gets the value of the call entry point offset from offsetToCallEntryPoints()
        // Ideally, we would want to get it from adapterFrameScheme().offsetToCallEntryPoints()
        if (_jitPackage == null || _jitPackage.equals(_compilerPackage)) {
            // zero-fill array -- all entry points are at code start (for now -- may change with inline caches).
            _offsetsToCallEntryPoints = new int[CallEntryPoint.VALUES.length()];
            _offsetsToCalleeEntryPoints = new int[CallEntryPoint.VALUES.length()];
        } else {
            final int offsetToOptimizedEntryPoint = WordWidth.BITS_8.numberOfBytes() * 8;
            final int offsetToJitEntryPoint = 0;
            final int offsetToVtableEntryPoint = offsetToOptimizedEntryPoint;
            final int offsetToCEntryPoint = 0;
            _offsetsToCallEntryPoints = new int[]{offsetToVtableEntryPoint,  offsetToJitEntryPoint, offsetToOptimizedEntryPoint, offsetToCEntryPoint};
            // Callees have the same entry point as their caller, except for C_ENTRY_POINT, which has the C_OPTIMIZED_ENTRY_POINT
            _offsetsToCalleeEntryPoints = new int[]{offsetToVtableEntryPoint,  offsetToJitEntryPoint, offsetToOptimizedEntryPoint, offsetToOptimizedEntryPoint};
        }
    }

    private AppendableIndexedSequence<VMScheme> _vmSchemes = new ArrayListSequence<VMScheme>();

    public synchronized <VMScheme_Type extends VMScheme> VMScheme_Type loadAndInstantiateScheme(MaxPackage p, Class<VMScheme_Type> vmSchemeType, Object... arguments) {
        if (p == null) {
            ProgramError.unexpected("Package not found for scheme: " + vmSchemeType.getSimpleName());
        }
        final VMScheme_Type vmScheme = p.loadAndInstantiateScheme(vmSchemeType, arguments);
        _vmSchemes.append(vmScheme);
        return vmScheme;
    }

    private boolean _areSchemesLoadedAndInstantiated = false;

    public void loadAndInstantiateSchemes() {
        if (_areSchemesLoadedAndInstantiated) {
            return;
        }
        _gripScheme = loadAndInstantiateScheme(_gripPackage, GripScheme.class, this);
        _referenceScheme = loadAndInstantiateScheme(_referencePackage, ReferenceScheme.class, this);
        _layoutScheme = loadAndInstantiateScheme(_layoutPackage, LayoutScheme.class, this, _gripScheme);
        _monitorScheme = loadAndInstantiateScheme(_monitorPackage, MonitorScheme.class, this);
        _heapScheme = loadAndInstantiateScheme(_heapPackage, HeapScheme.class, this);
        _targetABIsScheme = loadAndInstantiateScheme(_targetABIsPackage, TargetABIsScheme.class, this);
        _compilerScheme = loadAndInstantiateScheme(_compilerPackage, CompilerScheme.class, this);
        _trampolineScheme = loadAndInstantiateScheme(_trampolinePackage, DynamicTrampolineScheme.class, this);
        if (_jitPackage != null) {
            _jitScheme = loadAndInstantiateScheme(_jitPackage, DynamicCompilerScheme.class, this);
        } else {
            // no JIT, always using the optimizing compiler
            _jitScheme = _compilerScheme;
        }
        _profilingScheme = new ProfilingScheme(this);
        _vmSchemes.append(_profilingScheme);

        _compilationScheme = new AdaptiveCompilationScheme(this);
        _vmSchemes.append(_compilationScheme);

        _runScheme = loadAndInstantiateScheme(_runPackage, RunScheme.class, this);
        _areSchemesLoadedAndInstantiated = true;
    }

    public void initializeSchemes(MaxineVM.Phase phase) {
        for (int i = 0; i < _vmSchemes.length(); i++) {
            _vmSchemes.get(i).initialize(phase);
        }
    }

    public void finalizeSchemes(MaxineVM.Phase phase) {
        for (int i = 0; i < _vmSchemes.length(); i++) {
            _vmSchemes.get(i).finalize(phase);
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
        writer.println("build level: " + _buildLevel);
        writer.println("platform: " + _platform);
        writer.println("grip package: " + _gripPackage.name());
        writer.println("reference package: " + _referencePackage.name());
        writer.println("layout package: " + _layoutPackage.name());
        writer.println("heap package: " + _heapPackage.name());
        writer.println("monitor package: " + _monitorPackage.name());
        writer.println("compiler package: " + _compilerPackage.name());
        if (_jitPackage != null) {
            writer.println("jit package: " + _jitPackage.name());
        }
        writer.println("run scheme: " + _runPackage.name());
        writer.println("trampoline package: " + _trampolinePackage.name());
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
