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
import static com.sun.max.vm.stack.VMFrameLayout.*;
import static com.sun.max.vm.t1x.T1XOptions.*;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;

import com.sun.c1x.*;
import com.sun.c1x.debug.*;
import com.sun.c1x.util.*;
import com.sun.cri.ci.CiCallingConvention.Type;
import com.sun.cri.ci.*;
import com.sun.max.*;
import com.sun.max.annotate.*;
import com.sun.max.asm.dis.*;
import com.sun.max.lang.*;
import com.sun.max.platform.*;
import com.sun.max.program.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.MaxineVM.Phase;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.compiler.c1x.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.verifier.*;

/**
 *
 *
 * @author Doug Simon
 */
public class T1X implements RuntimeCompiler {

    final T1XTemplate[] templates;

    /**
     * The number of slots to be reserved in each T1X frame for template spill slots.
     */
    int templateSlots;

    @HOSTED_ONLY
    public T1X() {
        templates = new T1XTemplate[T1XTemplateTag.values().length];
    }

    private final ThreadLocal<T1XCompilation> compilation = new ThreadLocal<T1XCompilation>() {
        @Override
        protected T1XCompilation initialValue() {
            return new T1XCompilation(T1X.this);
        }
    };

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
        if (c.method != null) {
            throw new InternalError("T1X is not re-entrant");
        }

        long startTime = 0;
        int index = T1XMetrics.CompiledMethods++;
        if (PrintCompilation) {
            TTY.print(String.format("T1X %4d %-70s %-45s | ", index, method.holder().name(), method.name()));
            startTime = System.nanoTime();
        }

        CFGPrinter cfgPrinter = null;
        ByteArrayOutputStream cfgPrinterBuffer = null;
        if (PrintCFGToFile && method != null) {
            // Cannot write to file system at runtime until the VM is in the RUNNING phase
            if (isHosted() || isRunning()) {
                cfgPrinterBuffer = new ByteArrayOutputStream();
                cfgPrinter = new CFGPrinter(cfgPrinterBuffer, target());
                cfgPrinter.printCompilation(method);
            }
        }

        TTY.Filter filter = PrintFilter == null ? null : new TTY.Filter(PrintFilter, method);
        try {
            T1XTargetMethod t1xMethod = c.compile(method, install);
            T1XMetrics.BytecodesCompiled += t1xMethod.codeAttribute.code().length;
            T1XMetrics.CodeBytesEmitted += t1xMethod.code().length;
            if (stats != null) {
                stats.bytecodeCount = t1xMethod.codeAttribute.code().length;
            }
            printMachineCodeTo(t1xMethod, cfgPrinter);
            return t1xMethod;
        } finally {
            if (filter != null) {
                filter.remove();
            }
            if (PrintCompilation) {
                long time = (System.nanoTime() - startTime) / 100000;
                TTY.println(String.format("%3d.%dms", time / 10, time % 10));
            }
            if (cfgPrinter != null) {
                cfgPrinter.flush();
                OutputStream cfgFileStream = CFGPrinter.cfgFileStream();
                if (cfgFileStream != null) {
                    synchronized (cfgFileStream) {
                        try {
                            cfgFileStream.write(cfgPrinterBuffer.toByteArray());
                        } catch (IOException e) {
                            TTY.println("WARNING: Error writing CFGPrinter output for %s to disk: %s", method, e.getMessage());
                        }
                    }
                }
            }
            c.cleanup();
        }
    }

    void printMachineCodeTo(T1XTargetMethod t1xMethod, CFGPrinter cfgPrinter) {
        if (isHosted() && cfgPrinter != null) {
            byte[] code = t1xMethod.code();
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PrintStream ps = new PrintStream(baos);
            ps.println(";; " + t1xMethod.frame.toString().replaceAll("\\n", "\n;; "));
            final Pointer startAddress = t1xMethod.codeStart();
            final DisassemblyPrinter disassemblyPrinter = new T1XDisassemblyPrinter(t1xMethod);
            Disassembler.disassemble(baos, code, platform().isa, platform().wordWidth(), startAddress.toLong(), null, disassemblyPrinter);
            if (t1xMethod.handlers.length != 0) {
                ps.print(";; ------ Exception Handlers ------");
                for (CiExceptionHandler e : t1xMethod.handlers) {
                    if (e.catchTypeCPI == T1XTargetMethod.SYNC_METHOD_CATCH_TYPE_CPI) {
                        ps.println(";;     <any> @ [" +
                                        e.startBCI() + " .. " +
                                        e.endBCI() + ") -> " +
                                        e.handlerBCI());
                    } else {
                        ps.println(";;     " + (e.catchType == null ? "<any>" : e.catchType) + " @ [" +
                                        t1xMethod.posForBci(e.startBCI()) + " .. " +
                                        t1xMethod.posForBci(e.endBCI()) + ") -> " +
                                        t1xMethod.posForBci(e.handlerBCI()));
                    }
                }

            }
            ps.flush();
            String dis = baos.toString();
            cfgPrinter.printMachineCode(dis, "After code generation");
        }
    }

    public <T extends TargetMethod> Class<T> compiledType() {
        Class<Class<T>> type = null;
        return Utils.cast(type, T1XTargetMethod.class);
    }

    @Override
    public CallEntryPoint calleeEntryPoint() {
        return CallEntryPoint.JIT_ENTRY_POINT;
    }

    @Override
    public void initialize(Phase phase) {
        if (isHosted() && phase == Phase.COMPILING) {

            Trace.begin(1, "creating T1X templates");
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

            final Method[] templateMethods = T1XTemplateSource.class.getDeclaredMethods();
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
                            StringBuilder sb = new StringBuilder("Template must not have *any* reference literals: " + templateCode);
                            for (int i = 0; i < templateCode.referenceLiterals().length; i++) {
                                Object literal = templateCode.referenceLiterals()[i];
                                sb.append("\n  " + i + ": " + literal.getClass().getName() + " // \"" + literal + "\"");
                            }
                            throw FatalError.unexpected(sb.toString());
                        }
                        FatalError.check(templateCode.scalarLiterals() == null, "Template must not have *any* scalar literals: " + templateCode + "\n\n" + templateCode);
                        codeSize += templateCode.codeLength();
                        int frameSlots = Ints.roundUp(templateCode.frameSize(), STACK_SLOT_SIZE) / STACK_SLOT_SIZE;
                        if (frameSlots > templateSlots) {
                            templateSlots = frameSlots;
                        }
                        T1XTemplate template = templates[tag.ordinal()];
                        if (template != null) {
                            FatalError.unexpected("Template tag " + tag + " is already bound to " + template.method + ", cannot rebind to " + templateSource);
                        }
                        templates[tag.ordinal()] = new T1XTemplate(templateCode, tag, templateSource);
                    }
                }
            }
            Trace.end(1, "creating T1X templates [templates code size: " + codeSize + "]", startTime);
            comp.extensions = oldExtensions;
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
    private static boolean hasStackParameters(ClassMethodActor classMethodActor) {
        CiKind receiver = !classMethodActor.isStatic() ? classMethodActor.holder().kind() : null;
        for (CiValue arg : vm().registerConfigs.standard.getCallingConvention(Type.JavaCall, Util.signatureToKinds(classMethodActor.signature(), receiver), target()).locations) {
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
