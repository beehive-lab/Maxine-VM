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
package test.com.sun.max.vm.jit;
import static com.sun.max.annotate.BYTECODE_TEMPLATE.Static.*;
import static com.sun.max.vm.bytecode.Bytecode.Flags.*;

import java.lang.reflect.*;

import test.com.sun.max.vm.compiler.*;

import com.sun.max.annotate.*;
import com.sun.max.asm.*;
import com.sun.max.asm.dis.*;
import com.sun.max.lang.*;
import com.sun.max.program.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.bytecode.*;
import com.sun.max.vm.code.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.template.*;
import com.sun.max.vm.template.source.*;
import com.sun.max.vm.type.*;

/**
 * Encapsulate helpers common to all TemplateTable test cases.
 *
 * @author Laurent Daynes
 */
public abstract class TemplateTableTestCase extends CompilerTestCase<TargetMethod> {

    protected static String fieldActorToString(FieldActor fieldActor) {
        final StringBuilder sb = new StringBuilder();
        if (fieldActor.isFinal()) {
            sb.append("final ");
        } else if (!(fieldActor instanceof InjectedReferenceFieldActor) && fieldActor.toJava().isAnnotationPresent(CONSTANT.class)) {
            sb.append("@CONSTANT ");
        }
        if (fieldActor.isStatic()) {
            sb.append("static ");
        }
        if (fieldActor instanceof InjectedReferenceFieldActor) {
            sb.append("<InjectedFieldActor.holder>");
        } else {
            sb.append(fieldActor.holder().name());
        }
        sb.append('.');

        sb.append(fieldActor.name());
        sb.append(" (offset = ");
        sb.append(fieldActor.offset());
        sb.append(')');
        return sb.toString();
    }

    protected TemplateChooser.Selector selectorOf(Class< ? > templateSource) {
        final TEMPLATE t = templateSource.getAnnotation(TEMPLATE.class);
        return new TemplateChooser.Selector(t.resolved(), t.initialized(), t.instrumented(), t.traced());
    }

    protected Bytecode[] bytecodesImplementedByTemplateSource(Class< ? > templateSource) {
        final Method[] templateMethods = templateSource.getDeclaredMethods();
        final Bytecode[] bytecodes = new Bytecode[templateMethods.length];
        final boolean[] seen = new boolean[12];
        int i = 0;
        for (Method m : templateMethods) {
            final Bytecode bytecode = bytecodeImplementedBy(m);
            if (bytecode == null) {
                Trace.line(2, "Method " + m.getName() + ": not a template source");
                continue;
            }
            if (bytecode.is(FIELD_READ | FIELD_WRITE)) {
                final int index = bytecode.ordinal() - Bytecode.GETSTATIC.ordinal();
                if (seen[index]) {
                    continue;
                }
                seen[index] = true;
            } else if (bytecode.is(LDC_)) {
                final int index = 4 + (bytecode.ordinal() - Bytecode.LDC.ordinal());
                if (seen[index]) {
                    continue;
                }
                seen[index] = true;
            } else if (bytecode == Bytecode.MULTIANEWARRAY) {
                if (seen[7]) {
                    continue;
                }
                seen[7] = true;
            } else if (bytecode.is(INVOKE_)) {
                final int index = 8 + (bytecode.ordinal() - Bytecode.INVOKEVIRTUAL.ordinal());
                if (seen[index]) {
                    continue;
                }
                seen[index] = true;
            }
            bytecodes[i++] = bytecode;
        }
        if (i < bytecodes.length) {
            // trim array to real size
            final Bytecode[] trimmed = new Bytecode[i];
            System.arraycopy(bytecodes, 0, trimmed, 0, i);
            return trimmed;
        }
        return bytecodes;
    }

    protected static interface TemplateProcessor {

        void processTemplate(CompiledBytecodeTemplate template);
    }

    protected interface Fixer {
        void fix(InstructionModifier modifier, byte[] templateCopy) throws AssemblyException;
    }

    static class TemplateStats {

        int _bytesCount = 0;
        int _templateCount = 0;
        int _minSize = 999999;
        int _maxSize = 0;
        int _minSizeCount = 0; // How many times have we seen the min size
        int _maxSizeCount = 0; // How many times have we seen the max size
        int _minFrameSize = 999999;
        int _maxFrameSize = 0;

        void update(CompiledBytecodeTemplate template) {
            final int len = template.targetMethod().codeLength();
            _bytesCount += len;
            _templateCount++;
            if (len <= _minSize) {
                if (_minSize == len) {
                    _minSizeCount++;
                } else {
                    _minSize = len;
                    _minSizeCount = 1;
                }
            }
            if (len >= _maxSize) {
                if (_maxSize == len) {
                    _maxSizeCount++;
                } else {
                    _maxSize = len;
                    _maxSizeCount = 1;
                }
            }
            final int frameSize = template.targetMethod().frameSize();

            if (frameSize > _maxFrameSize) {
                _maxFrameSize = frameSize;
            } else if (frameSize < _minFrameSize) {
                _minFrameSize = frameSize;
            }
        }

        void report() {
            Trace.line(1, "Templates #: " + _templateCount + " Total Size = " + _bytesCount + " bytes");
            if (_templateCount > 0) {
                final int avg = _bytesCount / _templateCount;
                Trace.line(1, "avg size = " + avg + ", min size = " + _minSize + " (" + _minSizeCount + "),  max size = " + _maxSize + " (" + _maxSizeCount + ")");
                Trace.line(1, "Frame Size:  min = " + _minFrameSize + ", max =" + _maxFrameSize);
            }
        }
    }

    protected void templateTableIterate(TemplateTable templateTable, Class< ? > templateSource, TemplateProcessor processor) {
        final TemplateChooser.Selector selector = selectorOf(templateSource);
        final Bytecode[] bytecodes = bytecodesImplementedByTemplateSource(templateSource);
        for (Bytecode bytecode : bytecodes) {
            assert bytecode != null;
            Kind[] kinds = null;
            if (bytecode.is(FIELD_READ | FIELD_WRITE)) {
                kinds = Arrays.append(Kind.PRIMITIVE_VALUES, Kind.REFERENCE);
            } else if (bytecode.is(LDC_)) {
                if (bytecode == Bytecode.LDC2_W) {
                    kinds = new Kind[]{Kind.LONG, Kind.DOUBLE};
                } else {
                    if (selector.resolved() == TemplateChooser.Resolved.YES) {
                        kinds = new Kind[] {Kind.REFERENCE};
                    } else {
                        kinds = new Kind[]{Kind.INT, Kind.FLOAT};
                    }
                }
            } else if (bytecode.is(INVOKE_)) {
                kinds = new Kind[]{Kind.VOID, Kind.WORD, Kind.LONG, Kind.FLOAT, Kind.DOUBLE};
            }
            if (kinds != null) {
                for (Kind k : kinds) {
                    final CompiledBytecodeTemplate template = templateTable.get(bytecode, k, selector);
                    if (template != null) {
                        processor.processTemplate(template);
                    }
                }
            } else if (bytecode == Bytecode.MULTIANEWARRAY) {
                final CompiledBytecodeTemplate defaultTemplate = templateTable.get(bytecode, selector);
                processor.processTemplate(defaultTemplate);
            } else {
                final CompiledBytecodeTemplate template = templateTable.get(bytecode, selector);
                processor.processTemplate(template);
            }
        }
    }

    protected void validateTemplateTable(TemplateTable templateTable, Class< ? > templateSource) {
        final TemplateStats stats = new TemplateStats();

        final TemplateProcessor processor = new TemplateProcessor() {
            public void processTemplate(CompiledBytecodeTemplate template) {
                final Bytecode bytecode = template.bytecode();
                final TargetMethod targetMethod = template.targetMethod();
                final Kind kind = template.kind();
                String numOperands = "";
                if (bytecode == Bytecode.MULTIANEWARRAY) {
                    final String suffix = template.targetMethod().name().substring(bytecode.name().length());
                    if (suffix.length() > 0) {
                        numOperands = "[:" + suffix + "] ";
                    }
                }
                Trace.line(1, "Generated Template for " + bytecode + (kind == null ? " (" : " [" + kind + "] (") + numOperands + targetMethod.codeLength() + " bytes)");
                if (targetMethod.code().length > 0) {
                    traceBundleAndDisassemble(targetMethod);
                    if (template.targetMethod().numberOfCatchRanges() > 0) {
                        Trace.line(1, "\t*** WARNING: template has exception handlers: " + targetMethod.numberOfCatchRanges() + " catch ranges");
                    }
                }
                stats.update(template);
            }
        };
        templateTableIterate(templateTable, templateSource, processor);

        Trace.line(1, "Templates generated from " + templateSource.getName());
        stats.report();
    }

    protected void generateTable(Class... templateSources) {
        final TemplateTable templateTable = new TemplateTable(templateSources);
        for (Class c : templateSources) {
            validateTemplateTable(templateTable, c);
        }
        Trace.line(1, "\ndone\n");
    }

    protected void generateTable(Class templateSource) {
        final TemplateTable templateTable = new TemplateTable(templateSource);
        validateTemplateTable(templateTable, templateSource);
        Trace.line(1, "\ndone\n");
    }

    protected void generateOptimizedTable(Class< ? > optimizedTemplateSource) {
        final TemplateTable templateTable = new TemplateTable(UnoptimizedBytecodeTemplateSource.class, optimizedTemplateSource);
        validateTemplateTable(templateTable, optimizedTemplateSource);
        Trace.line(1, "\ndone\n");
    }

    private JITTestSetup jitTestSetup() {
        assert compilerTestSetup() instanceof JITTestSetup;
        return (JITTestSetup) compilerTestSetup();
    }

    @Override
    protected Disassembler disassemblerFor(TargetMethod targetMethod) {
        return jitTestSetup().disassemblerFor(targetMethod);
    }

    @Override
    protected CompilerTestSetup<TargetMethod> compilerTestSetup() {
        final Class<CompilerTestSetup<TargetMethod>> compilerTestSetupType = null;
        return StaticLoophole.cast(compilerTestSetupType, CompilerTestSetup.compilerTestSetup());
    }

}
