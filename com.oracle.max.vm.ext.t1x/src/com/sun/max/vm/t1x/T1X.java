/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.t1x;

import static com.sun.max.platform.Platform.*;
import static com.sun.max.vm.MaxineVM.*;
import static com.sun.max.vm.compiler.target.Stops.*;
import static com.sun.max.vm.stack.VMFrameLayout.*;
import static com.sun.max.vm.t1x.T1XOptions.*;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;

import com.sun.c1x.*;
import com.sun.c1x.debug.*;
import com.sun.cri.bytecode.*;
import com.sun.cri.ci.CiCallingConvention.Type;
import com.sun.cri.ci.*;
import com.sun.cri.ci.CiUtil.RefMapFormatter;
import com.sun.cri.ri.*;
import com.sun.max.annotate.*;
import com.sun.max.lang.*;
import com.sun.max.platform.*;
import com.sun.max.program.*;
import com.sun.max.vm.*;
import com.sun.max.vm.MaxineVM.Phase;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.bytecode.*;
import com.sun.max.vm.classfile.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.compiler.c1x.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.stack.*;
import com.sun.max.vm.verifier.*;

/**
 * The template JIT compiler based on C1X.
 */
public class T1X implements RuntimeCompiler {

    static {
        ClassfileReader.bytecodeTemplateClasses.add(T1X_TEMPLATE.class);
    }

    public final T1XTemplate[] templates = new T1XTemplate[T1XTemplateTag.values().length];

    /**
     * Support for {@link T1XOptions#PrintBytecodeHistogram}.
     */
    long[] dynamicBytecodeCount;

    /**
     * Support for {@link T1XOptions#PrintBytecodeHistogram}.
     */
    long[] staticBytecodeCount;

    /**
     * Class defining the template definitions, default is {@link T1XTemplateSource}.
     */
    private Class<?> templateSource;

    /**
     * When using non-default template definitions, this compiler provides the
     * default implementation for any non-overridden templates.
     */
    protected final T1X altT1X;

    /**
     * Factory that creates the appropriate subclass of {@link T1XCompilation}.
     */
    protected final T1XCompilationFactory t1XCompilationFactory;

    // TODO(mjj) These are not implemented by standard T1X but should they be?
    private static final EnumSet UNIMPLEMENTED_TEMPLATES = EnumSet.of(T1XTemplateTag.GOTO, T1XTemplateTag.GOTO_W,
                    T1XTemplateTag.NULL_CHECK, T1XTemplateTag.WRITEREG$link);

    @HOSTED_ONLY
    public T1X() {
        this(T1XTemplateSource.class, null, new T1XCompilationFactory());
    }

    /**
     * Creates a compiler in which some template definitions may be overridden.
     * by calling {@link #setTemplateSource(Class)} before {@link #initialize}.
     *
     * @param altT1X compiler providing implementation of non-overridden definitions
     * @param factory for creating {@link T1XCompilation} instances
     */
    @HOSTED_ONLY
    protected T1X(T1X altT1X, T1XCompilationFactory factory) {
        this(null, altT1X, factory);
    }

    @HOSTED_ONLY
    private T1X(Class<?> templateSource, T1X altT1X, T1XCompilationFactory factory) {
        this.altT1X = altT1X;
        this.templateSource = templateSource;
        this.t1XCompilationFactory = factory;
    }

    protected void setTemplateSource(Class<?> templateSource) {
        this.templateSource = templateSource;
    }

    private final ThreadLocal<T1XCompilation> compilation = new ThreadLocal<T1XCompilation>() {
        @Override
        protected T1XCompilation initialValue() {
            return t1XCompilationFactory.newT1XCompilation(T1X.this);
        }
    };

    @HOSTED_ONLY
    public void deoptimizationNotSupported() {
    }

    public boolean canProduceDeoptimizedCode() {
        return true;
    }

    public void resetMetrics() {
        for (Field f : T1XMetrics.class.getFields()) {
            if (f.getType() == int.class) {
                try {
                    f.set(null, 0);
                } catch (IllegalAccessException e) {
                    // do nothing.
                }
            }
        }
    }

    public TargetMethod compile(ClassMethodActor method, boolean install, CiStatistics stats) {
        T1XCompilation c = compilation.get();
        boolean reentrant = false;
        if (c.method != null) {
            // Re-entrant call to T1X - use a new compilation object that will be discarded
            // once the compilation is done. This should be a very rare occurrence.
            c = t1XCompilationFactory.newT1XCompilation(this);
            reentrant = true;
            if (VMOptions.verboseOption.verboseCompilation || PrintCompilation) {
                Log.println("Created temporary compilation object for re-entrant T1X compilation");
            }
        }

        long startTime = 0;
        int index = T1XMetrics.CompiledMethods++;
        if (PrintCompilation) {
            TTY.print(String.format("T1X %4d %-70s %-45s | ", index, method.holder().name(), method.name()));
            startTime = System.nanoTime();
        }

        TTY.Filter filter = PrintFilter == null ? null : new TTY.Filter(PrintFilter, method);

        try {
            T1XTargetMethod t1xMethod = c.compile(method, install);
            T1XMetrics.BytecodesCompiled += t1xMethod.codeAttribute.code().length;
            T1XMetrics.CodeBytesEmitted += t1xMethod.code().length;
            if (stats != null) {
                stats.bytecodeCount = t1xMethod.codeAttribute.code().length;
            }
            printMachineCode(c, t1xMethod, reentrant);
            return t1xMethod;
        } finally {
            if (filter != null) {
                filter.remove();
            }
            if (PrintCompilation) {
                long time = (System.nanoTime() - startTime) / 100000;
                TTY.println(String.format("%3d.%dms", time / 10, time % 10));
            }
            c.cleanup();
        }
    }

    private static final int MIN_OPCODE_LINE_LENGTH = 100;

    void printMachineCode(T1XCompilation c, T1XTargetMethod t1xMethod, boolean reentrant) {
        if (!PrintCFGToFile || reentrant || c.method == null || TTY.isSuppressed()) {
            return;
        }
        if (!isHosted() && !isRunning()) {
            // Cannot write to file system at runtime until the VM is in the RUNNING phase
            return;
        }

        ByteArrayOutputStream cfgPrinterBuffer = new ByteArrayOutputStream();
        CFGPrinter cfgPrinter = new CFGPrinter(cfgPrinterBuffer, target());

        cfgPrinter.printCompilation(c.method);

        byte[] code = t1xMethod.code();
        final Platform platform = Platform.platform();
        CiHexCodeFile hcf = new CiHexCodeFile(code, t1xMethod.codeStart().toLong(), platform.isa.name(), platform.wordWidth().numberOfBits);

        CiUtil.addAnnotations(hcf, c.codeAnnotations);
        addOpcodeComments(hcf, t1xMethod);
        addExceptionHandlersComment(t1xMethod, hcf);
        addStopPositionComments(t1xMethod, hcf);

        String label = CiUtil.format("T1X %f %R %H.%n(%P)", c.method, false);

        cfgPrinter.printMachineCode(hcf.toEmbeddedString(), label);

        String bytecodes = c.method.format("%f %R %H.%n(%P)") + String.format("%n%s", CodeAttributePrinter.toString(c.method.codeAttribute()));
        cfgPrinter.printBytecodes(bytecodes);

        cfgPrinter.flush();
        OutputStream cfgFileStream = CFGPrinter.cfgFileStream();
        if (cfgFileStream != null) {
            synchronized (cfgFileStream) {
                try {
                    cfgFileStream.write(cfgPrinterBuffer.toByteArray());
                } catch (IOException e) {
                    TTY.println("WARNING: Error writing CFGPrinter output for %s to disk: %s", c.method, e.getMessage());
                }
            }
        }
    }

    private static void addStopPositionComments(T1XTargetMethod t1xMethod, CiHexCodeFile hcf) {
        if (t1xMethod.stops().length() != 0) {
            Stops stops = t1xMethod.stops();
            Object[] directCallees = t1xMethod.directCallees();

            JVMSFrameLayout frame = t1xMethod.frame;
            RefMapFormatter slotFormatter = new RefMapFormatter(target().arch, target().spillSlotSize, frame.framePointerReg(), frame.frameReferenceMapOffset());
            int dcIndex = 0;
            for (int stopIndex = 0; stopIndex < stops.length(); ++stopIndex) {
                int pos = stops.posAt(stopIndex);
                int causePos = stops.causePosAt(stopIndex);

                CiDebugInfo info = t1xMethod.debugInfoAt(stopIndex, null);
                hcf.addComment(pos, CiUtil.append(new StringBuilder(100), info, slotFormatter).toString());

                if (stops.isSetAt(DIRECT_CALL, stopIndex)) {
                    Object callee = directCallees[dcIndex];
                    hcf.addOperandComment(causePos, String.valueOf(callee));
                    dcIndex++;
                } else if (stops.isSetAt(INDIRECT_CALL, stopIndex)) {
                    CiCodePos codePos = info.codePos;
                    if (codePos != null) {
                        RiMethod callee = t1xMethod.codeAttribute.calleeAt(codePos.bci);
                        if (callee != null) {
                            hcf.addOperandComment(causePos, String.valueOf(callee));
                        }
                    }
                } else {
                    assert stops.isSetAt(SAFEPOINT, stopIndex);
                    hcf.addOperandComment(causePos, "safepoint");
                }
            }
        }
    }

    private static void addExceptionHandlersComment(T1XTargetMethod t1xMethod, CiHexCodeFile hcf) {
        if (t1xMethod.handlers.length != 0) {
            String nl = CiHexCodeFile.NEW_LINE;
            StringBuilder buf = new StringBuilder("------ Exception Handlers ------").append(nl);
            for (CiExceptionHandler e : t1xMethod.handlers) {
                if (e.catchTypeCPI == T1XTargetMethod.SYNC_METHOD_CATCH_TYPE_CPI) {
                    buf.append("    <any> @ [").
                        append(e.startBCI()).
                        append(" .. ").
                        append(e.endBCI()).
                        append(") -> ").
                        append(e.handlerBCI()).
                        append(nl);
                } else {
                    buf.append("    ").
                        append(e.catchType == null ? "<any>" : e.catchType).append(" @ [").
                        append(t1xMethod.posForBci(e.startBCI())).append(" .. ").
                        append(t1xMethod.posForBci(e.endBCI())).append(") -> ").
                        append(t1xMethod.posForBci(e.handlerBCI())).append(nl);
                }
            }
            hcf.addComment(0, buf.toString());
        }
    }

    private static void addOpcodeComments(CiHexCodeFile hcf, T1XTargetMethod t1xMethod) {
        int[] bciToPos = t1xMethod.bciToPos;
        BytecodeStream s = new BytecodeStream(t1xMethod.codeAttribute.code());
        for (int bci = 0; bci < bciToPos.length; ++bci) {
            int pos = bciToPos[bci];
            if (pos != 0) {
                StringBuilder sb = new StringBuilder(MIN_OPCODE_LINE_LENGTH);
                if (bci != bciToPos.length - 1) {
                    sb.append("-------------------- ");
                    s.setBCI(bci);
                    sb.append(bci).append(": ").append(Bytecodes.nameOf(s.currentBC()));
                    for (int i = bci + 1; i < s.nextBCI(); ++i) {
                        sb.append(' ').append(s.readUByte(i));
                    }
                    sb.append(" --------------------");
                } else {
                    sb.append("-------------------- <epilogue> --------------------");
                }
                while (sb.length() < MIN_OPCODE_LINE_LENGTH) {
                    sb.append('-');
                }
                hcf.addComment(pos, sb.toString());
            }
        }
    }

    public CallEntryPoint calleeEntryPoint() {
        return CallEntryPoint.BASELINE_ENTRY_POINT;
    }

    public void initialize(Phase phase) {
        if (isHosted() && phase == Phase.COMPILING) {
            createTemplates(templateSource, altT1X, true, templates);
        }
        if (phase == Phase.STARTING) {
            if (T1XOptions.PrintBytecodeHistogram) {
                dynamicBytecodeCount = new long[256];
                staticBytecodeCount = new long[256];
            }
        } else if (phase == Phase.TERMINATING) {
            if (T1XOptions.PrintBytecodeHistogram) {
                Log.println("Bytecode Histogram: Mnemonic <tab> Dynamic Count <tab> Static Count");
                for (int i = 0; i < 256; i++) {
                    String name = Bytecodes.nameOf(i);
                    if (!name.startsWith("<illegal")) {
                        Log.println(name + "\t" + dynamicBytecodeCount[i] + "\t" + staticBytecodeCount[i]);
                    }
                }
            }

            if (T1XOptions.PrintMetrics) {
                T1XMetrics.print();
            }
            if (T1XOptions.PrintTimers) {
                T1XTimer.print();
            }

        }
    }

    /**
     * Create a set of templates from the methods annotated with {@link T1X_TEMPLATE} in the given class.
     * Undefined templates are filled from the templates associated with {@code altT1X}. The latter
     * maybe null iff {@code checkComplete} is {@code false}, in which case the result will only
     * contain the templates defined in the class.
     *
     * @param templateSourceClass class containing template methods
     * @param altT1X alternate compiler to use for undefined templates
     * @param checkComplete if {@code true} check the array for completeness.
     * @param templates an existing instance that will be incrementally updated.
     *        Value may be null, in which case a new array will be created.
     * @return the templates array, either as passed in or created.
     */
    @HOSTED_ONLY
    public T1XTemplate[] createTemplates(Class<?> templateSourceClass, T1X altT1X, boolean checkComplete, T1XTemplate[] templates) {
        Trace.begin(1, "creating T1X templates from " + templateSourceClass.getName());
        if (templates == null) {
            templates = new T1XTemplate[T1XTemplateTag.values().length];
        }
        long startTime = System.currentTimeMillis();

        // Create a C1X compiler to compile the templates
        C1X bootCompiler = C1X.instance;
        if (bootCompiler == null) {
            bootCompiler = new C1X();
            bootCompiler.initialize(Phase.COMPILING);
        }
        C1XCompiler comp = bootCompiler.compiler();
        List<C1XCompilerExtension> oldExtensions = comp.extensions;
        comp.extensions = Arrays.asList(new C1XCompilerExtension[] {new T1XTemplateChecker()});

        ClassActor.fromJava(T1XFrameOps.class);
        ClassActor.fromJava(T1XRuntime.class);
        ClassVerifier verifier = new TypeCheckingVerifier(ClassActor.fromJava(T1XTemplateSource.class));

        final Method[] templateMethods = templateSourceClass.getDeclaredMethods();
        int codeSize = 0;
        for (Method method : templateMethods) {
            if (Platform.platform().isAcceptedBy(method.getAnnotation(PLATFORM.class))) {
                T1X_TEMPLATE anno = method.getAnnotation(T1X_TEMPLATE.class);
                if (anno != null) {
                    T1XTemplateTag tag = anno.value();
                    ClassMethodActor templateSource = ClassMethodActor.fromJava(method);
                    templateSource.verify(verifier);

                    FatalError.check(templateSource.isTemplate(), "Method with " + T1X_TEMPLATE.class.getSimpleName() + " annotation should be a template: " + templateSource);
                    FatalError.check(!hasStackParameters(templateSource), "Template must not have *any* stack parameters: " + templateSource);

                    final C1XTargetMethod templateCode = (C1XTargetMethod) bootCompiler.compile(templateSource, true, null);
                    if (!(templateCode.referenceLiterals() == null)) {
                        final StringBuilder sb = new StringBuilder("Template must not have *any* reference literals: " + templateCode);
                        for (int i = 0; i < templateCode.referenceLiterals().length; i++) {
                            Object literal = templateCode.referenceLiterals()[i];
                            sb.append("\n  " + i + ": " + literal.getClass().getName() + " // \"" + literal + "\"\n");
                        }
                        throw FatalError.unexpected(sb.toString());
                    }
                    FatalError.check(templateCode.scalarLiterals() == null, "Template must not have *any* scalar literals: " + templateCode + "\n\n" + templateCode);
                    codeSize += templateCode.codeLength();
                    int frameSlots = Ints.roundUp(templateCode.frameSize(), STACK_SLOT_SIZE) / STACK_SLOT_SIZE;
                    if (frameSlots > T1XTargetMethod.templateSlots) {
                        T1XTargetMethod.templateSlots = frameSlots;
                    }
                    T1XTemplate template = templates[tag.ordinal()];
                    if (template != null) {
                        FatalError.unexpected("Template tag " + tag + " is already bound to " + template.method + ", cannot rebind to " + templateSource);
                    }
                    templates[tag.ordinal()] = new T1XTemplate(templateCode, tag, templateSource);
                }
            }
        }
        if (checkComplete) {
            // ensure everything is implemented
            for (int i = 0; i < T1XTemplateTag.values().length; i++) {
                if (templates[i] == null && !UNIMPLEMENTED_TEMPLATES.contains(T1XTemplateTag.values()[i])) {
                    if (altT1X == null || altT1X.templates[i] == null) {
                        FatalError.unexpected("Template tag " + T1XTemplateTag.values()[i] + " is not implemented");
                    } else {
                        templates[i] = altT1X.templates[i];
                    }
                }
            }
        }
        Trace.end(1, "creating T1X templates from " + templateSourceClass.getName() + " [templates code size: " + codeSize + "]", startTime);
        comp.extensions = oldExtensions;
        return templates;
    }

    @HOSTED_ONLY
    private static boolean hasStackParameters(ClassMethodActor classMethodActor) {
        CiKind receiver = !classMethodActor.isStatic() ? classMethodActor.holder().kind() : null;
        for (CiValue arg : vm().registerConfigs.standard.getCallingConvention(Type.JavaCall, CiUtil.signatureToKinds(classMethodActor.signature(), receiver), target(), false).locations) {
            if (!arg.isRegister()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }
}
