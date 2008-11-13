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
package com.sun.max.vm.compiler.adaptive;

import java.util.*;
import java.util.concurrent.*;

import com.sun.max.annotate.*;
import com.sun.max.asm.*;
import com.sun.max.lang.Arrays;
import com.sun.max.program.*;
import com.sun.max.unsafe.*;
import com.sun.max.util.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.code.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.compiler.instrument.*;
import com.sun.max.vm.compiler.ir.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.profile.*;
import com.sun.max.vm.prototype.*;
import com.sun.max.vm.stack.*;

/**
 * This class implements an adaptive compilation system with multiple compilers with different compilation time / code
 * quality tradeoffs. It encapsulates the necessary infrastructure for recording profiling data, selecting what and when
 * to recompile, etc.
 *
 * @author Ben L. Titzer
 * @author Michael Van De Vanter
 */
public class AdaptiveCompilationScheme extends AbstractVMScheme implements CompilationScheme {

    /**
     * A constant used to indicated that recompilation is disabled.
     */
    public static final int RECOMPILATION_DISABLED = -1;

    private static final int DEFAULT_RECOMPILATION_THRESHOLD = 1500;

    /**
     * Predicted maximum number of compilations for most methods.
     */
    private static final int DEFAULT_HISTORY_LENGTH = 2;

    /**
     * Stores the default threshold at which a recompilation is triggered from the baseline compiler to the next level
     * of optimization. This is typically the number of invocations of the method.
     */
    public static int _defaultRecompilationThreshold0 = DEFAULT_RECOMPILATION_THRESHOLD;

    /**
     * Stores the default threshold at which a recompilation is triggered from optimized code to more highly optimized
     * code.
     */
    public static int _defaultRecompilationThreshold1 = DEFAULT_RECOMPILATION_THRESHOLD;

    /**
     * A queue of pending compilations.
     */
    protected final LinkedList<Compilation> _pending = new LinkedList<Compilation>();

    /**
     * The number of submitted compilations.
     */
    protected int _submitted;

    /**
     * A lock to synchronize access to {@link #_completed}.
     */
    protected final Object _completionLock = new Object();

    /**
     * The number of completed compilations. If {@code _completed < _submitted}, then there is currently a compilation
     * pending or being performed.
     */
    protected int _completed;

    /**
     * The compiler that is used as the default at prototyping time.
     */
    @PROTOTYPE_ONLY
    protected final CompilerScheme _prototypeCompiler;

    /**
     * The baseline (JIT) compiler.
     */
    protected final DynamicCompilerScheme _jitCompiler;

    /**
     * The optimizing compiler, if any.
     */
    protected final CompilerScheme _optimizingCompiler;

    private static final VMOption _jitOption = new VMOption("-Xjit",
                    "Selects JIT only mode, with no recompilation.", MaxineVM.Phase.STARTING);
    private static final VMOption _optOption = new VMOption("-Xopt",
                    "Selects optimized only mode.", MaxineVM.Phase.STARTING);
    private static final VMOption _mixedOption = new VMOption("-Xmixed",
                    "Selects mixed mode, where methods are JITted and later reoptimized.", MaxineVM.Phase.STARTING);
    private static final VMIntOption _thresholdOption = new VMIntOption("-XX:RCT=", DEFAULT_RECOMPILATION_THRESHOLD,
                    "In mixed mode, sets the recompilation threshold for methods.", MaxineVM.Phase.STARTING);
    private static final VMOption _gcOnCompileOption = new VMOption("-XX:GCOnCompilation",
                    "When specified, the compiler will request GC before every compilation operation, " +
                    "which is useful for testing corner cases in the compiler/GC interactions.", MaxineVM.Phase.STARTING);
    private static final VMOption _gcOnRecompileOption = new VMOption("-XX:GCOnRecompilation",
                    "When specified, the compiler will request GC before every re-compilation operation, " +
                    "which is useful for testing corner cases in the compiler/GC interactions.", MaxineVM.Phase.STARTING);

    /**
     * The (dynamically selected) compilation mode.
     */
    private Mode _mode = Mode.JIT;

    public Mode mode() {
        return _mode;
    }

    /**
     * Set the compilation mode of this compilation scheme, which determines which compilers to select at runtime.
     */
    public void setMode(Mode mode) {
        _mode = mode;
    }

    /**
     * The {@code MethodState} class implements a container for a number of pieces of information about a class method
     * actor that are used during compilation and recompilation, including any instrumentation attached to various
     * versions as well as the "current" compiled code.
     *
     * Multiple histories are kept for each {@link CompilationDirective}.
     *
     * @author Ben L. Titzer
     * @author Michael Bebenita
     */
    public class AdaptiveMethodState extends MethodState {

        protected TargetMethod[][] _targetMethods;

        protected MethodInstrumentation _methodInstrumentation;
        protected Compilation [] _currentCompilations;

        protected AdaptiveMethodState(ClassMethodActor classMethodActor) {
            super(classMethodActor, DEFAULT_HISTORY_LENGTH);
            _targetMethods = new TargetMethod[CompilationDirective.count()][];
            _currentCompilations = new Compilation[CompilationDirective.count()];
        }

        /**
         * Constructs the method instrumentation for this method state, if it does not exist already.
         *
         * @return the method instrumentation for this method state
         */
        public synchronized MethodInstrumentation makeMethodInstrumentation() {
            if (_methodInstrumentation == null) {
                _methodInstrumentation = new MethodInstrumentation(_classMethodActor);
            }
            return _methodInstrumentation;
        }

        /**
         * Gets the method instrumentation for this method state.
         *
         * @return the method instrumentation if it exists; {@code null} if it does not exist
         */
        public MethodInstrumentation getMethodInstrumentation() {
            return _methodInstrumentation;
        }

        /**
         * Gets the currently pending compilation for this method state, if any.
         *
         * @return any currently pending compilation
         */
        @INLINE
        private Compilation currentCompilation(CompilationDirective compilationDirective) {
            return _currentCompilations[compilationDirective.ordinal()];
        }

        private void setCurrentCompilation(Compilation compilation, CompilationDirective compilationDirective) {
            _currentCompilations[compilationDirective.ordinal()] = compilation;
        }

        /**
         * Update this method state to a new compiled version. If the current target method is {@code null} (e.g. this
         * method has never been compiled), then the update takes effect immediately. If there is already a target
         * method, then the compilation scheme will decide if the new code is a valid update, and if so, perform the
         * necessary code patching to forward calls to the old target method to the new one.
         *
         * @param newTargetMethod the new target method
         */
        protected final void setTargetMethod(TargetMethod newTargetMethod, CompilationDirective compilationDirective) {
            final TargetMethod currentTargetMethod = currentTargetMethod(compilationDirective);
            if (currentTargetMethod == null) {
                // no previous code for this method and this compilation directive
                addTargetMethod(newTargetMethod, compilationDirective);
            } else if (currentTargetMethod == newTargetMethod) {
                // nothing to be done.
            } else if (allowUpdate(this, newTargetMethod)) {
                // the method update was allowed, perform code update on previous compilations
                // we only update target methods that were compiled with promotable compilation directives
                for (CompilationDirective promotableDirective : compilationDirective.promotableFrom()) {
                    final TargetMethod[] methodHistory = methodHistory(promotableDirective);
                    if (methodHistory != null) {
                        for (int i = 0; i < methodHistory.length; i++) {
                            Code.updateTargetMethod(methodHistory[i], newTargetMethod);
                        }
                    }
                }
                addTargetMethod(newTargetMethod, compilationDirective);
            } else {
                Code.discardTargetMethod(newTargetMethod);
            }
        }

        private TargetMethod[] methodHistory(CompilationDirective compilationDirective) {
            final TargetMethod[] methodHistory = _targetMethods[compilationDirective.ordinal()];
            return methodHistory;
        }

        @Override
        public TargetMethod currentTargetMethod(CompilationDirective compilationDirective) {
            final TargetMethod[] methodHistory = methodHistory(compilationDirective);
            if (methodHistory != null) {
                return methodHistory[methodHistory.length - 1];
            }
            return null;
        }

        private void addTargetMethod(TargetMethod targetMethod, CompilationDirective compilationDirective) {
            TargetMethod[] methodHistory = methodHistory(compilationDirective);
            if (methodHistory != null) {
                methodHistory = Arrays.extend(methodHistory, methodHistory.length + 1);
            } else {
                methodHistory = new TargetMethod[1];
            }
            methodHistory[methodHistory.length - 1] = targetMethod;
            _targetMethods[compilationDirective.ordinal()] = methodHistory;
            // TODO: No need to keep track of method history in two places, remove if from the super class.
            // For now we leave it in to make the inspector happy.
            addTargetMethod(targetMethod);
        }

        public final TargetMethod jittedTargetMethod() {
            return currentTargetMethod(CompilationDirective.JIT);
        }

        public final TargetMethod tracedTargetMethod() {
            return currentTargetMethod(CompilationDirective.TRACE_JIT);
        }

        public final boolean isJitted() {
            return jittedTargetMethod() != null;
        }

        public final boolean isTraced() {
            return tracedTargetMethod() != null;
        }

        @Override
        public TargetMethod currentTargetMethod() {
            final TargetMethod targetMethod = currentTargetMethod(CompilationDirective.DEFAULT);
            if (targetMethod != null) {
                return targetMethod;
            }
            return currentTargetMethod(CompilationDirective.JIT);
        }
    }

    /**
     * This class exists solely as a work around when running the IR tests where compilation stops at some IR level
     * above TargetMethod. In this context, the only thing needed from the target method is the entry point and so this
     * hidden class is just a bridge from {@link TargetMethod#getEntryPoint(CallEntryPoint)} to
     * {@link IrMethod#getEntryPoint(CallEntryPoint)}.
     */
    public static class IrTargetMethod extends TargetMethod {

        static TargetMethod asTargetMethod(IrMethod irMethod) {
            if (irMethod == null) {
                return null;
            }
            if (irMethod instanceof TargetMethod) {
                return (TargetMethod) irMethod;
            }
            return new IrTargetMethod(irMethod);
        }

        final IrMethod _irMethod;

        IrTargetMethod(IrMethod irMethod) {
            super(irMethod.classMethodActor());
            _irMethod = irMethod;
        }

        @Override
        public Word getEntryPoint(CallEntryPoint callEntryPoint) {
            return _irMethod.getEntryPoint(callEntryPoint);
        }

        @Override
        public void forwardTo(TargetMethod newTargetMethod) {
            throw ProgramError.unexpected();
        }

        @Override
        public InstructionSet instructionSet() {
            throw ProgramError.unexpected();
        }

        @Override
        public void patchCallSite(int callOffset, Word callTarget) {
            throw ProgramError.unexpected();
        }

        @Override
        public int registerReferenceMapSize() {
            throw ProgramError.unexpected();
        }

        @Override
        public JavaStackFrameLayout stackFrameLayout() {
            throw ProgramError.unexpected();
        }

        @Override
        public boolean areReferenceMapsFinalized() {
            throw ProgramError.unexpected();
        }

        @Override
        public void finalizeReferenceMaps() {
            throw ProgramError.unexpected();
        }
    }

    /**
     * This class represents the state for the compilation of a single method, including the method to be compiled, that
     * method's state, and the compiler to use. This class also implements the {@link Future} interface, which allows
     * this compilation to be queried by the clients of the compilation scheme to obtain the target method result later.
     *
     * @author Ben L. Titzer
     * @author Michael Bebenita
     */
    protected class Compilation implements Future<TargetMethod> {

        /**
         * The state of the method this compilation is to compile.
         */
        protected final AdaptiveMethodState _methodState;

        /**
         * Compilation options to be passed on to the underlying compiler.
         */
        protected final CompilationDirective _compilationDirective;

        /**
         * The compiler to use for this compilation.
         */
        protected DynamicCompilerScheme _compiler;

        /**
         * State of this compilation. If {@code true}, then this compilation has finished and the target method is
         * available.
         */
        protected boolean _done;

        protected final Thread _compilingThread;

        Compilation(AdaptiveMethodState methodState, CompilationDirective compilationDirective) {
            _methodState = methodState;
            _compilingThread = Thread.currentThread();
            _compilationDirective = compilationDirective;
        }

        /**
         * Cancel this compilation. Ignored.
         */
        public boolean cancel(boolean mayInterruptIfRunning) {
            return false;
        }

        /**
         * Checks whether this compilation was cancelled. This method always returns {@code false}
         */
        public boolean isCancelled() {
            return false;
        }

        /**
         * Returns whether this compilation is done.
         */
        public boolean isDone() {
            return _done;
        }

        /**
         * Gets the result of this compilation, blocking if necessary.
         *
         * @return the target method that resulted from this compilation
         */
        public TargetMethod get() throws InterruptedException {
            if (!_done) {
                synchronized (_methodState) {
                    // the method state object is used here as the condition variable
                    _methodState.wait();
                    return _methodState.currentTargetMethod(_compilationDirective);
                }
            }
            return _methodState.currentTargetMethod(_compilationDirective);
        }

        /**
         * Gets the result of this compilation, blocking for a maximum amount of time.
         *
         * @return the target method that resulted from this compilation
         */
        public TargetMethod get(long timeout, TimeUnit unit) throws InterruptedException {
            if (!_done) {
                synchronized (_methodState) {
                    // the method state object is used here as the condition variable
                    _methodState.wait(timeout); // TODO: convert timeout to milliseconds
                    return _methodState.currentTargetMethod(_compilationDirective);
                }
            }
            return _methodState.currentTargetMethod(_compilationDirective);
        }

        void compile(CompilationDirective compilationDirective) {
            if (_gcOnCompileOption.isPresent()) {
                System.gc();
            }
            DynamicCompilerScheme compiler = _compiler;
            if (compiler == null) {
                if (compilationDirective.jitOnly() || compilationDirective.traceInstrument()) {
                    assert _methodState.classMethodActor().isUnsafe() == false;
                    compiler = _jitCompiler;
                } else {
                    compiler = selectCompiler(_methodState);
                }
            }
            assert _methodState.currentCompilation(compilationDirective) == this;
            TargetMethod targetMethod = null;

            if (ProfilingScheme.isProfiling()) {
                ProfilingScheme.compilerTimer().startNewComputation(this);
            }
            try {
                if (VerboseVMOption.verboseCompilation()) {
                    Log.println(compiler.name() + ": Compiling " + _methodState.classMethodActor().qualifiedName());
                    if (_compilationTimer != null) {
                        // The _compilationTimer field will still be null if this compilation occurs while it is being initialized
                        _compilationTimer.startNewComputation();
                    }
                }
                targetMethod = IrTargetMethod.asTargetMethod(compiler.compile(_methodState.classMethodActor(), compilationDirective));
                if (VerboseVMOption.verboseCompilation()) {
                    Log.print(compiler.name() + ": Compiled  " + _methodState.classMethodActor().qualifiedName() + " @ ");
                    Log.print(targetMethod.codeStart());
                    Log.print("{length=" + targetMethod.codeLength() + "}");
                    if (_compilationTimer != null) {
                        final long time = _compilationTimer.stop();
                        Log.print(" (" + (time / 1000000) + " msecs)");
                    }
                    Log.println();
                }
            } finally {
                _done = true;

                if (ProfilingScheme.isProfiling()) {
                    recordCompilationInfo(compiler, targetMethod);
                }

                if (targetMethod != null) {
                    // compilation succeeded and produced a target method
                    synchronized (_methodState) {
                        _methodState.setTargetMethod(targetMethod, compilationDirective);
                        _methodState.setCurrentCompilation(null, compilationDirective);
                        _methodState.notifyAll();
                    }
                } else {
                    // the compilation did not succeed, but notify any waiters
                    synchronized (_methodState) {
                        // notify any waiters
                        _methodState.setCurrentCompilation(null, compilationDirective);
                        _methodState.notifyAll();
                    }
                }
                synchronized (_completionLock) {
                    _completed++;
                }
            }
        }

        private void recordCompilationInfo(DynamicCompilerScheme compiler, TargetMethod targetMethod) {
            final long compilationTime = ProfilingScheme.compilerTimer().stop(this);
            ProfilingScheme.recordCompilationInfo(
                            _methodState.classMethodActor().qualifiedName(),
                            compiler.name(),
                            _methodState.classMethodActor().codeAttribute().code().length,
                            (_methodState.currentTargetMethod() == null) ? 0 : _methodState.currentTargetMethod().codeLength(),
                            (targetMethod == null) ? 0 : targetMethod.codeLength(),
                            (targetMethod == null) ? 0 : targetMethod.numberOfDirectCalls(),
                            (targetMethod == null) ? 0 : targetMethod.numberOfIndirectCalls(),
                            (targetMethod == null) ? 0 : targetMethod.numberOfSafepoints(),
                            (targetMethod == null) ? 0 : targetMethod.numberOfReferenceLiterals(),
                            (targetMethod == null) ? 0 : targetMethod.numberOfScalarLiteralBytes(),
                            compilationTime);
        }
    }

    /**
     * This method decides whether an update of the target code for a specified method is allowed. An update might be
     * disallowed for correctness reasons (e.g. due to deoptimization), or due to multiple compilations of the same
     * method at the same or different optimization levels happening because of concurrency.
     *
     * @param methodState the method state of the ClassMethodActor
     * @param newTargetMethod the new target code for the method
     * @return true if the update should be allowed; false otherwise
     */
    protected boolean allowUpdate(AdaptiveMethodState methodState, TargetMethod newTargetMethod) {
        return true;
    }

    /**
     * The constructor for this class initializes a new adaptive compilation system with the specified VM configuration,
     * configuring itself according to the compiler(s) selected in the VM configuration.
     *
     * @param vmConfiguration the configuration of the virtual machine
     */
    public AdaptiveCompilationScheme(VMConfiguration vmConfiguration) {
        super(vmConfiguration);
        _prototypeCompiler = vmConfiguration.compilerScheme();
        _optimizingCompiler = _prototypeCompiler;
        _jitCompiler = vmConfiguration.jitScheme();
    }

    private NanoTimer _compilationTimer;

    /**
     * This method initializes the adaptive compilation system, either at prototyping time or
     * at VM startup time. This implementation creates daemon threads to handle asynchronous
     * compilations.
     *
     * @param phase the phase of VM starting up.
     */
    @Override
    public void initialize(MaxineVM.Phase phase) {
        if (MaxineVM.isPrototyping()) {
            // TODO: use more than one thread during prototyping
            final CompilationThread compilationThread = new CompilationThread();
            compilationThread.setDaemon(true);
            compilationThread.start();
        } else if (phase == MaxineVM.Phase.STARTING) {
            if (_jitOption.isPresent()) {
                _defaultRecompilationThreshold0 = RECOMPILATION_DISABLED;
                _defaultRecompilationThreshold1 = RECOMPILATION_DISABLED;
                setMode(Mode.JIT);
            } else if (_optOption.isPresent()) {
                _defaultRecompilationThreshold0 = RECOMPILATION_DISABLED;
                _defaultRecompilationThreshold1 = RECOMPILATION_DISABLED;
                setMode(Mode.OPTIMIZED);
            } else {
                _defaultRecompilationThreshold0 = _thresholdOption.getValue();
                setMode(Mode.MIXED);
            }

            if (VerboseVMOption.verboseCompilation()) {
                _compilationTimer = new NanoTimer();
            }
            if (ProfilingScheme.isProfiling()) {
                final CompilerProfilingObserver observer = new CompilerProfilingObserver();
                for (IrGenerator irGen : _optimizingCompiler.irGenerators()) {
                    irGen.addIrObserver(observer);
                }
            }

            // only use one compilation thread after starting
            final CompilationThread compilationThread = new CompilationThread();
            compilationThread.setDaemon(true);
            compilationThread.start();
        }
    }

    /**
     * This method builds a new method instrumentation object for the specified method, if one
     * does not already exist.
     *
     * @param classMethodActor the method for which to make the instrumentation
     * @return the canonical method instrumentation object associated with the specified method
     */
    public MethodInstrumentation makeMethodInstrumentation(ClassMethodActor classMethodActor) {
        return makeMethodState(classMethodActor).makeMethodInstrumentation();
    }

    /**
     * This method gets a method instrumentation object associated with the specified method, if it exists.
     *
     * @param classMethodActor the method for which to get the method instrumentation
     * @return the method instrumentation associated with the specified method if it exists; null otherwise
     */
    public MethodInstrumentation getMethodInstrumentation(ClassMethodActor classMethodActor) {
        final AdaptiveMethodState methodState = getMethodState(classMethodActor);
        if (methodState != null) {
            return methodState.getMethodInstrumentation();
        }
        return null;
    }

    /**
     * Get the method state and cast it to a {@code AdaptiveMethodState} instance.
     *
     * @param classMethodActor the method for which to get the method state
     * @return the method state as an instance of the {@code AdaptiveMethodState} class
     */
    private AdaptiveMethodState getMethodState(ClassMethodActor classMethodActor) {
        synchronized (classMethodActor) {
            return (AdaptiveMethodState) classMethodActor.methodState();
        }
    }

    /**
     * Gets the method state object for the specified class method actor, building it if necessary.
     *
     * @param classMethodActor the class method for which to make the method state
     * @return the method state associated with the specified method
     */
    public AdaptiveMethodState makeMethodState(ClassMethodActor classMethodActor) {
        synchronized (classMethodActor) {
            AdaptiveMethodState methodState = (AdaptiveMethodState) classMethodActor.methodState();
            if (methodState == null) {
                methodState = new AdaptiveMethodState(classMethodActor);
                classMethodActor.setMethodState(methodState);
            }
            return methodState;
        }
    }

    /**
     * Performs a compilation of the specified method, waiting for the compilation to finish.
     *
     * @param classMethodActor the method to compile
     * @param compilationDirective the compilation directive specifying the compiler to be used
     * @return the target method that results from compiling the specified method
     */
    public TargetMethod synchronousCompile(ClassMethodActor classMethodActor, CompilationDirective compilationDirective) {
        final AdaptiveMethodState methodState = makeMethodState(classMethodActor);
        Compilation compilation = null;
        synchronized (methodState) {
            if (methodState.currentCompilation(compilationDirective) != null) {
                try {
                    // wait for the existing compilation to finish
                    return methodState.currentCompilation(compilationDirective).get();
                } catch (InterruptedException e) {
                    return null;
                }
            }
            if (methodState.currentTargetMethod(compilationDirective) != null) {
                return methodState.currentTargetMethod(compilationDirective);
            }
            compilation = new Compilation(methodState, compilationDirective);
            methodState.setCurrentCompilation(compilation, compilationDirective);
        }
        // perform a synchronous compile
        perform(compilation, true);

        // return the result
        return methodState.currentTargetMethod(compilationDirective);
    }

    /**
     * Performs a compilation of the specified method in the background.
     *
     * @param classMethodActor the method to compile
     * @param compilationDirective the compilation directive specifying the compiler to be used
     * @return a {@code Future} object that can be used to retrieve the result of the compilation later
     */
    public Future<TargetMethod> asynchronousCompile(ClassMethodActor classMethodActor, CompilationDirective compilationDirective) {
        final AdaptiveMethodState methodState = makeMethodState(classMethodActor);
        Compilation compilation = null;
        synchronized (methodState) {
            if (methodState.currentCompilation(compilationDirective) != null) {
                return methodState.currentCompilation(compilationDirective);
            }
            if (methodState.currentTargetMethod(compilationDirective) != null) {
                compilation = new Compilation(methodState, compilationDirective);
                compilation._done = true;
                return compilation;
            }
            compilation = new Compilation(methodState, compilationDirective);
            methodState.setCurrentCompilation(compilation, compilationDirective);
        }
        // perform an asynchronous compile
        return perform(compilation, false);
    }

    /**
     * Perform a compilation in either synchronous or asynchronous mode.
     *
     * @param compilation the compilation to perform
     * @param synchronous {@code true} if the compilation should be performed immediately; {@code false} if it should be
     *            queued
     * @return the compilation
     */
    @INLINE
    private Compilation perform(Compilation compilation, boolean synchronous) {
        synchronized (_pending) {
            // a compilation is required.
            _submitted++;
            if (!synchronous) {
                _pending.offer(compilation);
                _pending.notify();
            }
        }
        if (synchronous) {
            compilation.compile(compilation._compilationDirective);
        }
        return compilation;
    }

    /**
     * Checks whether there is currently a compilation being performed.
     *
     * @return {@code true} if there is currently a compilation pending or being performed; {@code false} otherwise
     */
    public boolean isCompiling() {
        synchronized (_pending) {
            synchronized (_completionLock) {
                return _submitted > _completed;
            }
        }
    }

    /**
     * Select the appropriate compiler based on the current state of the method.
     *
     * @param methodState the state of the method
     * @return the compiler that should be used to perform the next compilation of the method
     */
    private DynamicCompilerScheme selectCompiler(AdaptiveMethodState methodState) {
        final ClassMethodActor classMethodActor = methodState.classMethodActor();
        if (classMethodActor.isUnsafe()) {
            // for unsafe methods there is no other choice no matter what, since the JIT cannot handle unsafe features
            return _optimizingCompiler;
        }
        if (MaxineVM.isPrototyping()) {
            // if we are prototyping, then always use the prototype compiler
            // unless forced to use the JIT (e.g. for testing purposes)
            if (CompiledPrototype.jitCompile(classMethodActor)) {
                return _jitCompiler;
            }
            return _prototypeCompiler;
        }

        // templates should only be compiled at prototyping time
        assert !classMethodActor.isTemplate();

        if (methodState.currentTargetMethod() == null) {
            if (classMethodActor.isSynthetic() && !classMethodActor.isNative()) {
                // we must at first use the JIT for reflective invocation stubs, otherwise meta-evaluation may not
                // terminate
                return _jitCompiler;
            }
            if (_mode == Mode.OPTIMIZED) {
                return _optimizingCompiler;
            }
        } else if (_mode != Mode.JIT) {
            return _optimizingCompiler;
        }
        return _jitCompiler;
    }

    /**
     * This method provides a hint to the adaptive compilation system that it should increase the optimization level for
     * a particular method, potentially recompiling it if it has already been compiled. This method is typically called
     * in response to dynamic feedback mechanisms such as a method invocation counter.
     *
     * @param classMethodActor the method for which to increase the optimization level
     * @param synchronous a boolean indicating whether any recompilation should be performed synchronously (i.e.
     *            immediately) or asynchronously (i.e. queued and potentially performed later by another thread)
     */
    public static void increaseOptimizationLevel(ClassMethodActor classMethodActor, boolean synchronous, CompilationDirective compilationDirective) {
        final CompilationScheme compilationScheme = VMConfiguration.target().compilationScheme();
        if (compilationScheme instanceof AdaptiveCompilationScheme) {
            ((AdaptiveCompilationScheme) compilationScheme).reoptimize(classMethodActor, synchronous, compilationDirective);
        }
    }

    /**
     * This method reoptimizes the specified method with a higher level of optimization, choosing a more optimizing
     * compiler and/or more aggressive optimizations. This method can be used in either synchronous mode (i.e.
     * compilation is performed right away) or asynchronous mode (i.e. compilation is performed in the background).
     *
     * @param classMethodActor the method to be reoptimized
     * @param synchronous a boolean indicating whether the compilation should be performed immediately or queued for
     *            later
     */
    public void reoptimize(ClassMethodActor classMethodActor, boolean synchronous, CompilationDirective compilationDirective) {
        if (_gcOnRecompileOption.isPresent()) {
            System.gc();
        }
        final AdaptiveMethodState methodState = makeMethodState(classMethodActor);
        Compilation compilation = null;
        synchronized (methodState) {
            final TargetMethod targetMethod = methodState.currentTargetMethod();
            if (methodState.currentCompilation(CompilationDirective.DEFAULT) != null) {
                if (synchronous) {
                    try {
                        // wait for current compilation to finish
                        methodState.currentCompilation(CompilationDirective.DEFAULT).get();
                    } catch (InterruptedException e) {
                        // do nothing
                    }
                }
                return;
            }
            if (targetMethod != null && targetMethod.compilerScheme() == _optimizingCompiler) {
                // method is already optimized.
                return;
            }
            // start a new compilation with the optimizing compiler
            compilation = new Compilation(methodState, compilationDirective);
            compilation._compiler = _optimizingCompiler;
            methodState.setCurrentCompilation(compilation, compilationDirective);
        }
        perform(compilation, synchronous);
    }

    /**
     * This class implements a daemon thread that performs compilations in the background. Depending on the compiler
     * configuration, multiple compilation threads may be working in parallel.
     *
     * @author Ben L. Titzer
     */
    protected class CompilationThread extends Thread {

        protected CompilationThread() {
            super("compile");
            setDaemon(true);
        }

        /**
         * The current compilation being performed by this thread.
         */
        Compilation _compilation;

        /**
         * Continuously polls the compilation queue for work, performing compilations as they are removed from the
         * queue.
         */
        @Override
        public void run() {
            while (true) {
                try {
                    compileOne();
                } catch (InterruptedException e) {
                    // do nothing.
                } catch (Throwable t) {
                    Log.print("Exception during compilation of " + _compilation._methodState.classMethodActor() + " " + t.getClass());
                    t.printStackTrace();
                }
            }
        }

        /**
         * Polls the compilation queue and performs a single compilation.
         *
         * @throws InterruptedException if the thread was interrupted waiting on the queue
         */
        void compileOne() throws InterruptedException {
            _compilation = null;
            synchronized (_pending) {
                while (_compilation == null) {
                    _compilation = _pending.poll();
                    if (_compilation == null) {
                        _pending.wait();
                    }
                }
            }
            _compilation.compile(_compilation._compilationDirective);
            _compilation = null;
        }
    }
}
