/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.max.vm.ext.t1x;

import static com.oracle.max.cri.intrinsics.IntrinsicIDs.*;
import static com.oracle.max.vm.ext.t1x.T1XOptions.*;
import static com.oracle.max.vm.ext.t1x.T1XTemplateTag.*;
import static com.sun.max.platform.Platform.*;
import static com.sun.max.vm.MaxineVM.*;
import static com.sun.max.vm.compiler.target.Safepoints.*;
import static com.sun.max.vm.intrinsics.MaxineIntrinsicIDs.*;
import static com.sun.max.vm.stack.VMFrameLayout.*;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;

import com.oracle.max.criutils.*;
import com.oracle.max.vm.ext.maxri.*;
import com.oracle.max.vm.ext.t1x.jvmti.*;
import com.sun.cri.bytecode.*;
import com.sun.cri.ci.CiCallingConvention.Type;
import com.sun.cri.ci.*;
import com.sun.cri.ci.CiUtil.RefMapFormatter;
import com.sun.cri.ri.*;
import com.sun.max.annotate.*;
import com.sun.max.lang.*;
import com.sun.max.platform.*;
import com.sun.max.program.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.MaxineVM.Phase;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.bytecode.*;
import com.sun.max.vm.classfile.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.heap.debug.*;
import com.sun.max.vm.jvmti.*;
import com.sun.max.vm.layout.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.stack.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.verifier.*;

/**
 * The template JIT compiler based on C1X.
 */
public class T1X implements RuntimeCompiler {

    static {
        ClassfileReader.bytecodeTemplateClasses.add(T1X_TEMPLATE.class);
        ClassfileReader.bytecodeTemplateClasses.add(T1X_INTRINSIC_TEMPLATE.class);
    }

    public final T1XTemplate[] templates = new T1XTemplate[T1XTemplateTag.values().length];

    public Map<RiMethod, T1XTemplate> intrinsicTemplates;

    /**
     * Class defining the template definitions, default is {@link T1XTemplateSource}.
     */
    private Class<?> templateSource;

    /**
     * When using non-default template definitions, this compiler provides the
     * default implementation for any non-overridden templates.
     */
    public final T1X altT1X;

    /**
     * This compiler is used when a method needs to be compiled to generate JVMTI events.
     */
    private T1X jvmtiT1X;

    /**
     * Factory that creates the appropriate subclass of {@link T1XCompilation}.
     */
    protected final T1XCompilationFactory t1XCompilationFactory;

    @HOSTED_ONLY
    public T1X() {
        this(T1XTemplateSource.class, null, new T1XCompilationFactory());
        this.jvmtiT1X = new T1X(JVMTI_T1XTemplateSource.class, this, new JVMTI_T1XCompilationFactory());
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
    protected T1X(Class<?> templateSource, T1X altT1X, T1XCompilationFactory factory) {
        this.altT1X = altT1X;
        this.templateSource = templateSource;
        this.t1XCompilationFactory = factory;
    }

    private final ThreadLocal<T1XCompilation> compilation = new ThreadLocal<T1XCompilation>() {
        @Override
        protected T1XCompilation initialValue() {
            return t1XCompilationFactory.newT1XCompilation(T1X.this);
        }
    };

    @Override
    public RuntimeCompiler.Nature nature() {
        return RuntimeCompiler.Nature.BASELINE;
    }

    public TargetMethod compile(ClassMethodActor method, boolean install, CiStatistics stats) {
        T1X t1x = this;
        if (!MaxineVM.isHosted() && useJVMTITemplates(method)) {
            // Use JVMTI templates to create code-related events.
            t1x = jvmtiT1X;
        }
        T1XCompilation c = t1x.compilation.get();
        boolean reentrant = false;
        if (c.method != null) {
            // Re-entrant call to T1X - use a new compilation object that will be discarded
            // once the compilation is done. This should be a very rare occurrence.
            c = t1x.t1XCompilationFactory.newT1XCompilation(t1x);
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

    /**
     * Checks whether to use the JVMTI templates.
     * @param methodActor
     * @return
     */
    private boolean useJVMTITemplates(ClassMethodActor methodActor) {
        if (MaxineVM.isHosted()) {
            return false;
        }
        // N.B. We do not instrument reflection stubs. Amongst other reasons they
        // can be generated by upcalls from JVMTI agents using JNI, e.g. Class.getName,
        // causing runaway recursion.
        ClassActor holder = methodActor.holder();
        boolean consider = !holder.isReflectionStub() && JVMTI.compiledCodeEventsNeeded();
        if (!consider) {
            return false;
        }
        // Nor do we instrument VM classes
        String name = holder.packageName();
        return !(name.startsWith("com.sun.max") || name.startsWith("com.oracle.max"));
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

        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        CompilationPrinter cprinter = new CompilationPrinter(buf);

        cprinter.printCompilation(c.method);

        byte[] code = t1xMethod.code();
        final Platform platform = Platform.platform();
        HexCodeFile hcf = new HexCodeFile(code, t1xMethod.codeStart().toLong(), platform.isa.name(), platform.wordWidth().numberOfBits);

        HexCodeFile.addAnnotations(hcf, c.codeAnnotations);
        addOpcodeComments(hcf, t1xMethod);
        addExceptionHandlersComment(t1xMethod, hcf);
        addSafepointPositionComments(t1xMethod, hcf);

        String label = CiUtil.format("T1X %f %R %H.%n(%P)", c.method);

        cprinter.printMachineCode(HexCodeFileTool.toText(hcf), label);

        String bytecodes = c.method.format("%f %R %H.%n(%P)") + String.format("%n%s", CodeAttributePrinter.toString(c.method.codeAttribute()));
        cprinter.printBytecodes(bytecodes);

        cprinter.flush();
        OutputStream cout = CompilationPrinter.globalOut();
        if (cout != null) {
            synchronized (cout) {
                try {
                    cout.write(buf.toByteArray());
                } catch (IOException e) {
                    TTY.println("WARNING: Error writing CFGPrinter output for %s to disk: %s", c.method, e.getMessage());
                }
            }
        }
    }

    private static void addSafepointPositionComments(T1XTargetMethod t1xMethod, HexCodeFile hcf) {
        if (t1xMethod.safepoints().size() != 0) {
            Safepoints safepoints = t1xMethod.safepoints();
            Object[] directCallees = t1xMethod.directCallees();

            JVMSFrameLayout frame = t1xMethod.frame;
            RefMapFormatter slotFormatter = new RefMapFormatter(target().arch, target().spillSlotSize, frame.framePointerReg(), frame.frameReferenceMapOffset());
            int dcIndex = 0;
            for (int safepointIndex = 0; safepointIndex < safepoints.size(); ++safepointIndex) {
                int pos = safepoints.posAt(safepointIndex);
                int causePos = safepoints.causePosAt(safepointIndex);

                CiDebugInfo info = t1xMethod.debugInfoAt(safepointIndex, null);
                hcf.addComment(pos, CiUtil.append(new StringBuilder(100), info, slotFormatter).toString());

                String callComment = null;
                if (safepoints.isSetAt(DIRECT_CALL, safepointIndex)) {
                    Object callee = directCallees[dcIndex];
                    callComment = String.valueOf(callee);
                    dcIndex++;
                } else if (safepoints.isSetAt(TEMPLATE_CALL, safepointIndex)) {
                    callComment = "<template_call>";
                } else if (safepoints.isSetAt(INDIRECT_CALL, safepointIndex)) {
                    CiCodePos codePos = info.codePos;
                    if (codePos != null) {
                        RiMethod callee = t1xMethod.codeAttribute.calleeAt(codePos.bci);
                        if (callee != null) {
                            callComment = String.valueOf(callee);
                        }
                    }
                }
                if (callComment != null) {
                    hcf.addOperandComment(causePos, callComment);
                }
            }
        }
    }

    private static void addExceptionHandlersComment(T1XTargetMethod t1xMethod, HexCodeFile hcf) {
        if (t1xMethod.handlers.length != 0) {
            String nl = HexCodeFile.NEW_LINE;
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

    private static void addOpcodeComments(HexCodeFile hcf, T1XTargetMethod t1xMethod) {
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

    public void initialize(Phase phase) {
        if (isHosted() && phase == Phase.HOSTED_COMPILING) {

            RuntimeCompiler compiler = createBootCompiler();

            createTemplates(compiler, templateSource, altT1X, true, templates);
            if (altT1X != null) {
                intrinsicTemplates = altT1X.intrinsicTemplates;
            } else {
                intrinsicTemplates = createIntrinsicTemplates(compiler);
            }
            if (jvmtiT1X != null) {
                jvmtiT1X.initialize(phase);
            }
        }
        if (phase == Phase.TERMINATING) {

            if (T1XOptions.PrintMetrics) {
                T1XMetrics.print();
            }
            if (T1XOptions.PrintTimers) {
                T1XTimer.print();
            }

        }
    }

    @HOSTED_ONLY
    protected RuntimeCompiler createBootCompiler() {
        // Create a boot compiler to compile the templates
        RuntimeCompiler compiler = vm().compilationBroker.optimizingCompiler;
        if (compiler == null) {
            compiler = CompilationBroker.instantiateCompiler(RuntimeCompiler.optimizingCompilerOption.getValue());
            compiler.initialize(Phase.HOSTED_COMPILING);
        }
        return compiler;
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
    public T1XTemplate[] createTemplates(RuntimeCompiler compiler, Class<?> templateSourceClass, T1X altT1X, boolean checkComplete, T1XTemplate[] templates) {
        Trace.begin(1, "creating T1X templates from " + templateSourceClass.getName());
        if (templates == null) {
            templates = new T1XTemplate[T1XTemplateTag.values().length];
        }
        long startTime = System.currentTimeMillis();

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
                    MaxTargetMethod templateCode = compileTemplate(compiler, templateSource);
                    codeSize += templateCode.codeLength();
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
                T1XTemplateTag tag  = T1XTemplateTag.values()[i];
                if (templates[i] == null && !isUnimplemented(tag)) {
                    if (altT1X == null || (altT1X.templates[i] == null && !altT1X.isUnimplemented(tag))) {
                        FatalError.unexpected("Template tag " + tag + " is not implemented");
                    } else {
                        templates[i] = altT1X.templates[i];
                    }
                }
            }
        }
        Trace.end(1, "creating T1X templates from " + templateSourceClass.getName() + " [templates code size: " + codeSize + "]", startTime);
        return templates;
    }

    /**
     * These templates are not used by T1X, but may be used by the VMA variant. Since enums cannot be
     * subclassed, it is convenient to keep them in {@link T1XTemplateTag} and just avoid checking
     * that they are implemented.
     */
    protected static final EnumSet UNIMPLEMENTED_TEMPLATES = EnumSet.of(NOP, ACONST_NULL, ICONST, LCONST, FCONST, DCONST, BIPUSH, SIPUSH, LDC$int, LDC$long, LDC$float, LDC$double,
                    LDC$reference$resolved, ILOAD, LLOAD, FLOAD, DLOAD, ALOAD, ISTORE, LSTORE, FSTORE, DSTORE, ASTORE, POP, POP2, DUP, DUP_X1, DUP_X2, DUP2, DUP2_X1, DUP2_X2, SWAP,
                    IINC, IFEQ, IFNE, IFLT, IFGE, IFGT, IFLE, IF_ICMPEQ, IF_ICMPNE, IF_ICMPLT, IF_ICMPGE, IF_ICMPGT, IF_ICMPLE, IF_ACMPEQ, IF_ACMPNE, IFNULL, IFNONNULL, GOTO, GOTO_W,
                    INVOKESPECIAL$void$resolved, INVOKESPECIAL$float$resolved, INVOKESPECIAL$long$resolved, INVOKESPECIAL$double$resolved, INVOKESPECIAL$reference$resolved,
                    INVOKESPECIAL$word$resolved, INVOKESTATIC$void$init, INVOKESTATIC$float$init, INVOKESTATIC$long$init, INVOKESTATIC$double$init, INVOKESTATIC$reference$init,
                    INVOKESTATIC$word$init, INVOKEVIRTUAL$adviseafter, INVOKEINTERFACE$adviseafter, INVOKESPECIAL$adviseafter, INVOKESTATIC$adviseafter, BREAKPOINT);

    protected boolean isUnimplemented(T1XTemplateTag tag) {
        return UNIMPLEMENTED_TEMPLATES.contains(tag);
    }

    private MaxTargetMethod compileTemplate(RuntimeCompiler bootCompiler, ClassMethodActor templateSource) {
        FatalError.check(templateSource.isTemplate(), "Method with " + T1X_TEMPLATE.class.getSimpleName() + " annotation should be a template: " + templateSource);
        FatalError.check(!hasStackParameters(templateSource), "Template must not have *any* stack parameters: " + templateSource);
        FatalError.check(templateSource.resultKind().stackKind == templateSource.resultKind(), "Template return type must be a stack kind: " + templateSource);
        for (int i = 0; i < templateSource.getParameterKinds().length; i++) {
            Kind k = templateSource.getParameterKinds()[i];
            FatalError.check(k.stackKind == k, "Template parameter " + i + " is not a stack kind: " + templateSource);
        }

        final MaxTargetMethod templateCode = (MaxTargetMethod) bootCompiler.compile(templateSource, true, null);
        FatalError.check(templateCode.scalarLiterals() == null, "Template must not have *any* scalar literals: " + templateCode);
        int frameSlots = Ints.roundUp(templateCode.frameSize(), STACK_SLOT_SIZE) / STACK_SLOT_SIZE;
        if (frameSlots > T1XTargetMethod.templateSlots) {
            T1XTargetMethod.templateSlots = frameSlots;
        }
        return templateCode;
    }

    @HOSTED_ONLY
    private static final Set<String> templateIntriniscIDs = new HashSet<String>(Arrays.asList(
        UCMP_AT, UCMP_AE, UCMP_BT, UCMP_BE,
        UDIV, UREM,
        LSB, MSB,
        PREAD_OFF, PREAD_IDX, PWRITE_OFF, PWRITE_IDX, PCMPSWP,
        HERE,
        PAUSE
    ));

    /**
     * List of intrinsic that T1X cannot handle, i.e., methods that call these intrinsics lead to a bailout.
     */
    public static final Set<String> unsafeIntrinsicIDs = new HashSet<String>(Arrays.asList(
        READREG, WRITEREG, IFLATCHBITREAD,
        SAFEPOINT_POLL, HERE, INFO, BREAKPOINT_TRAP,
        ALLOCA
    ));

    @HOSTED_ONLY
    private static final Class[] templateIntrinsicClasses = {
        com.sun.max.unsafe.Pointer.class,
        com.oracle.max.cri.intrinsics.UnsignedMath.class,
        com.sun.max.vm.intrinsics.Infopoints.class,
        com.sun.max.vm.Intrinsics.class
    };

    @HOSTED_ONLY
    public static List<ClassMethodActor> intrinsicTemplateMethods() {
        // Process all classes of the statically defined list above, then check that the list does not miss any classes.
        // The rational behind that: The list of classes processed must be stable, otherwise the code generator
        // complains that the generated wrapper source code has changed, which requires a recompile cycle.
        ArrayList<ClassMethodActor> result = new ArrayList<ClassMethodActor>();
        for (Class clazz : templateIntrinsicClasses) {
            for (MethodActor methodActor : ClassActor.fromJava(clazz).getLocalMethodActors()) {
                if (T1X.templateIntriniscIDs.contains(methodActor.intrinsic())) {
                    result.add((ClassMethodActor) methodActor);
                }
            }
        }
        // TODO Change to VM_CLASS_REGISTRY
        for (ClassActor classActor : ClassRegistry.BOOT_CLASS_REGISTRY.allBootImageClasses()) {
            for (MethodActor methodActor : classActor.getLocalMethodActors()) {
                if (T1X.templateIntriniscIDs.contains(methodActor.intrinsic()) && !result.contains(methodActor)) {
                    System.out.printf("%nClass with intrinisc methods found that should be in templateIntrinsicClasses: class %s, method %s%n%n",
                        classActor, methodActor);
                    System.exit(1);
                }
            }
        }
        return result;
    }

    @HOSTED_ONLY
    public Map<RiMethod, T1XTemplate> createIntrinsicTemplates(RuntimeCompiler bootCompiler) {
        Map<RiMethod, T1XTemplate> result = new HashMap<RiMethod, T1XTemplate>();

        Trace.begin(1, "creating T1X templates for intrinsics");
        long startTime = System.currentTimeMillis();

        try {
            boolean modified = T1XIntrinsicTemplateGenerator.generate(T1XIntrinsicTemplateSource.class);
            if (modified) {
                System.out.printf("%nThe generated content in %s was regenerated. Recompile (or refresh it in your IDE) and restart the bootstrapping process.%n%n", T1XIntrinsicTemplateSource.class.getSimpleName());
                System.exit(1);
            }
        } catch (Exception e) {
            FatalError.unexpected("Error while generating source for " + T1XIntrinsicTemplateSource.class, e);
        }

        ClassActor source = ClassActor.fromJava(T1XIntrinsicTemplateSource.class);
        for (ClassMethodActor intrinsicMethod : intrinsicTemplateMethods()) {
            ClassMethodActor templateSource = source.findLocalStaticMethodActor(SymbolTable.makeSymbol(T1XIntrinsicTemplateGenerator.templateInvokerName(intrinsicMethod)));
            MaxTargetMethod templateCode = compileTemplate(bootCompiler, templateSource);
            result.put(intrinsicMethod, new T1XTemplate(templateCode, null, templateSource));
        }

        Trace.end(1, "creating T1X templates for intrinsics", startTime);
        return result;
    }

    static {
        try {
            if (T1XTemplateGenerator.generate(true, T1XTemplateSource.class)) {
                String thisFile = T1XTemplateSource.class.getSimpleName();
                System.out.printf("%nThe generated content in %s " +
                    " is out of sync. Edit %s instead to make the desired changes and then run 'max t1xgen', " +
                    "recompile %s (or refresh it in your IDE) and restart the bootstrapping process.%n%n",
                    thisFile, T1XTemplateGenerator.class.getSimpleName(), thisFile);
                System.exit(1);
            }
        } catch (Exception e) {
            FatalError.unexpected("Error while generating source for " + T1XTemplateSource.class, e);
        }
    }

    @HOSTED_ONLY
    private static boolean hasStackParameters(ClassMethodActor classMethodActor) {
        for (CiValue arg : vm().registerConfigs.standard.getCallingConvention(Type.JavaCall, CiUtil.signatureToKinds(classMethodActor), target(), false).locations) {
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

    @Override
    public boolean matches(String compilerName) {
        return compilerName.equals("T1X");
    }

    /**
     * Gets the displacement from the start of the code to the address of some data co-located with the code.
     *
     * @param objectLiteralsLength the total number of object literals associated with the code
     * @param scalarLiteralsLength the size of the serialized scalar literals associated with the code
     * @param dataIndex the index into the object literals array or the serialized scalar literals of the data
     * @param isObject specifies if the data is an object
     */
    public static int dispFromCodeStart(int objectLiteralsLength, int scalarLiteralsLength, int dataIndex, boolean isObject) {
        int distance = Layout.byteArrayLayout().headerSize();
        if (DebugHeap.isTagging()) {
            distance += Word.size();
        }
        if (isObject) {
            distance += (objectLiteralsLength - dataIndex) * Word.size();
        } else {
            distance += objectLiteralsLength * Word.size();
            distance += Layout.referenceArrayLayout().headerSize();
            if (DebugHeap.isTagging()) {
                distance += Word.size();
            }
            distance += scalarLiteralsLength - dataIndex;
        }
        return -distance;
    }

    /**
     * Determines if the target ISA is AMD64.
     */
    @FOLD
    public static boolean isAMD64() {
        return platform().isa == ISA.AMD64;
    }

    /**
     * Called to denote some functionality is not yet implemented for the target ISA.
     */
    @NEVER_INLINE
    public static FatalError unimplISA() {
        throw FatalError.unexpected("Unimplemented platform: " + platform().isa);
    }
}
