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
package test.com.sun.max.vm.cps;

import static com.sun.max.vm.classfile.constant.SymbolTable.*;
import static com.sun.max.vm.hosted.JavaPrototype.*;
import static com.sun.max.vm.reflection.InvocationStub.*;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;

import org.junit.runner.*;

import sun.reflect.*;
import test.com.sun.max.vm.*;
import test.com.sun.max.vm.bytecode.*;
import test.com.sun.max.vm.cps.bytecode.create.*;

import com.sun.max.*;
import com.sun.max.annotate.*;
import com.sun.max.asm.*;
import com.sun.max.asm.dis.*;
import com.sun.max.config.*;
import com.sun.max.lang.*;
import com.sun.max.program.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.bytecode.*;
import com.sun.max.vm.classfile.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.cps.ir.*;
import com.sun.max.vm.cps.ir.interpreter.*;
import com.sun.max.vm.cps.target.*;
import com.sun.max.vm.hosted.*;
import com.sun.max.vm.reflection.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;

/**
 *
 * @author Bernd Mathiske
 * @author Doug Simon
 */
@RunWith(org.junit.runners.AllTests.class)
public abstract class CompilerTestCase<Method_Type extends IrMethod> extends VmTestCase {

    protected CompilerTestCase() {
        super();
    }

    protected CompilerTestCase(String name) {
        super(name);
    }

    private final List<ClassMethodActor> compiledMethods = new LinkedList<ClassMethodActor>();

    @Override
    public void tearDown() {
        for (ClassMethodActor classMethodActor : compiledMethods) {
            CompilationScheme.Static.resetMethodState(classMethodActor);
        }
    }

    public Method_Type compile(TestBytecodeAssembler asm, Class superClass) {
        return compileMethod(asm.classMethodActor(superClass));
    }

    protected ClassMethodActor getClassMethodActor(Class javaClass, String methodName, SignatureDescriptor signature) {
        final ClassMethodActor classMethodActor = ClassActor.fromJava(javaClass).findClassMethodActor(makeSymbol(methodName), signature);
        if (classMethodActor == null) {
            fail("No such method: " + javaClass.getName() + "." + methodName + signature);
        }
        return classMethodActor;
    }

    protected ClassMethodActor getClassMethodActor(Class javaClass, String methodName) {
        Class thisClass = javaClass;
        ClassMethodActor classMethodActor;
        do {
            classMethodActor = ClassActor.fromJava(thisClass).findLocalClassMethodActor(makeSymbol(methodName), null);
            thisClass = thisClass.getSuperclass();
        } while (classMethodActor == null && thisClass != null);
        if (classMethodActor == null) {
            fail("No such method: " + javaClass.getName() + "." + methodName);
        }
        return classMethodActor;
    }

    protected ClassMethodActor getClassMethodActor(String methodName, SignatureDescriptor signature) {
        final Class testClass = Classes.load(HostedBootClassLoader.HOSTED_BOOT_CLASS_LOADER, getClass().getName());
        return getClassMethodActor(testClass, methodName, signature);
    }

    protected ClassMethodActor getClassMethodActor(String methodName) {
        final Class testClass = Classes.load(HostedBootClassLoader.HOSTED_BOOT_CLASS_LOADER, getClass().getName());
        return getClassMethodActor(testClass, methodName);
    }

    protected CompilerTestSetup<Method_Type> compilerTestSetup() {
        final Class<CompilerTestSetup<Method_Type>> compilerTestSetupType = null;
        return Utils.cast(compilerTestSetupType, CompilerTestSetup.compilerTestSetup());
    }

    private void reportNondeterministicTranslation(Method_Type method1, Method_Type method2) {
        System.err.println("Compiler output differs for different translations to " + method1.getClass().getSimpleName() + " of " + method1.classMethodActor());
        System.err.println("---- Begin first translation ----");
        System.err.println(method1.traceToString());
        System.err.println("---- End first translation ----");
        System.err.println("---- Begin second translation ----");
        System.err.println(method2.traceToString());
        System.err.println("---- End second translation ----");
        addTestError(new Throwable("Compiler output differs for different translations to " + method1.getClass().getSimpleName() + " of " + method1.classMethodActor()));
    }

    private void compareCompilerOutput(Method_Type method1, Method_Type method2) {
        assert method1 != method2;
        if (method1 instanceof TargetMethod) {
            final byte[] code1 = ((TargetMethod) method1).code();
            final byte[] code2 = ((TargetMethod) method2).code();
            assert code1 != code2;
            if (!java.util.Arrays.equals(code1, code2)) {
                reportNondeterministicTranslation(method1, method2);
            }
        } else {
            final String code1 = method1.traceToString();
            final String code2 = method2.traceToString();
            if (!code1.equals(code2)) {
                reportNondeterministicTranslation(method1, method2);
            }
        }
    }

    protected Method_Type compileMethod(final ClassMethodActor classMethodActor) {
        compiledMethods.add(classMethodActor);
        try {
            final Method_Type method = compilerTestSetup().translate(classMethodActor);

            assertNotNull(method);

            if (Trace.hasLevel(3) && method instanceof CPSTargetMethod) {
                final CPSTargetMethod targetMethod = (CPSTargetMethod) method;
                Trace.line(3, "Bundle and code for " + targetMethod);
                traceBundleAndDisassemble(targetMethod);
            }

            if (System.getProperty("testDeterministicCompilation") != null) {
                CompilationScheme.Static.resetMethodState(classMethodActor);
                compareCompilerOutput(method, compilerTestSetup().translate(classMethodActor));
            }
            return method;
        } catch (NoSuchMethodError noSuchMethodError) {
            if (classMethodActor.isClassInitializer()) {
                ProgramWarning.message("NoSuchMethodError - probably caused by <clinit> referring to method with @HOSTED_ONLY: " + noSuchMethodError);
                return null;
            }
            throw noSuchMethodError;
        }
    }

    protected Method_Type compileMethod(Class type, String methodName, SignatureDescriptor signature) {
        final ClassMethodActor classMethodActor = getClassMethodActor(type, methodName, signature);
        return compileMethod(classMethodActor);
    }

    protected Method_Type compileMethod(Class type, String methodName) {
        final ClassMethodActor classMethodActor = getClassMethodActor(type, methodName);
        return compileMethod(classMethodActor);
    }

    protected Method_Type compileMethod(String methodName, SignatureDescriptor signature) {
        final ClassMethodActor classMethodActor = getClassMethodActor(methodName, signature);
        return compileMethod(classMethodActor);
    }

    protected Method_Type compileMethod(String methodName) {
        final ClassMethodActor classMethodActor = getClassMethodActor(methodName);
        return compileMethod(classMethodActor);
    }

    protected Method_Type compileMethod(final String className, final byte[] classfileBytes, final String methodName, final SignatureDescriptor signature) {
        final Class testClass = MillClassLoader.makeClass(className, classfileBytes);

        // Save the generated class file to the filesystem so that a generated stub for a method in the
        // generated class can find the corresponding Class instance
        ClassfileReader.saveClassfile(className, classfileBytes);

        final ClassMethodActor classMethodActor = ClassActor.fromJava(testClass).findLocalStaticMethodActor(makeSymbol(methodName), signature);
        assertNotNull(classMethodActor);

        return compileMethod(classMethodActor);
    }

    protected void compileClass(ClassActor classActor) {
        for (MethodActor methodActor : classActor.getLocalMethodActors()) {
            if (!methodActor.isAbstract() && !methodActor.isBuiltin()) {
                final ClassMethodActor classMethodActor = (ClassMethodActor) methodActor;
                if (classMethodActor.isClassInitializer() && classMethodActor.holder().name.toString().contains("HexByte")) {
                    continue;
                }

                if (classMethodActor.isInstanceInitializer() && InjectedReferenceFieldActor.class.equals(classActor.toJava().getEnclosingClass())) {
                    // These anonymous inner classes call a super constructor that is annotated with HOSTED_ONLY
                    continue;
                }

                if (Word.class.isAssignableFrom(classActor.toJava()) && !classMethodActor.isStatic() && !classMethodActor.isInstanceInitializer()) {
                    if (!methodActor.isAnnotationPresent(INLINE.class)) {
                        Trace.line(2, "skipping non-static method in Word subclass that is not annotated with @INLINE: " + methodActor);
                        continue;
                    }
                }

                Trace.begin(2, "compiling method: " + methodActor);
                try {
                    compileMethod(classMethodActor);
                } catch (Throwable error) {
                    addTestError(new Throwable("Error occurred while compiling " + methodActor, error));
                } finally {
                    CompilationScheme.Static.resetMethodState(classMethodActor); // conserve heap space
                    Trace.end(2, "compiling method: " + methodActor);
                }
            }
        }
    }

    protected void compileClass(Class javaClass) {
        // UnsafeBox classes cannot be compiled as they are treated as Word types and any attempt to
        // access fields to will fail as field access only works for non Word types. An example
        // of such a type is BoxedJniHandle.
        assert !(Boxed.class.isAssignableFrom(javaClass));

        final ClassActor classActor = ClassActor.fromJava(javaClass);
        if (classActor != null) {
            Trace.begin(1, "compiling class: " + classActor);
            compileClass(classActor);
            Trace.end(1, "compiling class: " + classActor);
        }
    }

    protected void compilePackage(BootImagePackage p) {
        Trace.begin(1, "compiling package: " + p.name());
        for (Class javaType : javaPrototype().packageLoader().load(p, true)) {
            compileClass(javaType);
        }
        Trace.end(1, "compiling package: " + p.name());
    }

    protected void compilePackages(List<BootImagePackage> packages) {
        for (BootImagePackage p : packages) {
            compilePackage(p);
        }
    }

    public boolean hasInterpreter() {
        return compilerTestSetup().createInterpreter() != null;
    }

    protected IrInterpreter<? extends IrMethod> createInterpreter() {
        final IrInterpreter<? extends IrMethod> interpreter = compilerTestSetup().createInterpreter();
        ProgramError.check(interpreter != null, "no interpreter available for this representation");
        return interpreter;
    }

    protected Method_Type generateAndCompileStubFor(MethodActor classMethodActor, Boxing boxing) {
        if (classMethodActor.isInstanceInitializer()) {
            final ConstructorInvocationStub stub = newConstructorStub(classMethodActor.toJavaConstructor(), null, boxing);
            final ClassActor stubActor = ClassActor.fromJava(stub.getClass());
            compileClass(stubActor);
            final ClassMethodActor newInstanceActor = stubActor.findClassMethodActor(makeSymbol("newInstance"), boxing.newInstanceSignature());
            assert newInstanceActor != null;
            return compileMethod(newInstanceActor);
        }
        final MethodAccessor stub = newMethodStub(classMethodActor.toJava(), boxing);
        final ClassActor stubActor = ClassActor.fromJava(stub.getClass());
        compileClass(stubActor);
        final ClassMethodActor invokeActor = stubActor.findClassMethodActor(makeSymbol("invoke"), boxing.invokeSignature());
        assert invokeActor != null;
        return compileMethod(invokeActor);

    }

    /**
     * Executes a {@linkplain #generateAndCompileStubFor(ClassMethodActor, boolean) generated stub}.
     *
     * @param classMethodActor the method for which the stub was generated
     * @param stub the compiled stub
     * @param arguments the execution arguments
     */
    protected Value executeInvocationStub(MethodActor classMethodActor, Method_Type stub, Boxing boxing, Value... arguments) throws InvocationTargetException {
        final Value[] boxedArguments;
        if (classMethodActor.isStatic()) {
            assert !classMethodActor.isInstanceInitializer();
            if (boxing == Boxing.JAVA) {
                // Signature: invoke(Object receiver, Object[] args)
                boxedArguments = new Value[]{
                    ReferenceValue.NULL,
                    ReferenceValue.NULL,
                    ReferenceValue.from(Value.asBoxedJavaValues(0, arguments.length, arguments))
                };
            } else {
                // Signature: invoke(Value[] args)
                boxedArguments = new Value[]{
                    ReferenceValue.NULL,
                    ReferenceValue.from(arguments)
                };
            }
        } else {
            if (classMethodActor.isInstanceInitializer()) {
                // Signature: newInstance(Object[] args)  | useJavaBoxing == true
                //            newInstance(Value[] args)   | useJavaBoxing == false
                boxedArguments = new Value[]{
                    ReferenceValue.NULL,
                    ReferenceValue.from(boxing == Boxing.JAVA ? Value.asBoxedJavaValues(0, arguments.length, arguments) : arguments)
                };
            } else {
                if (boxing == Boxing.JAVA) {
                    // Signature: invoke(Object receiver, Object[] args)
                    boxedArguments = new Value[]{
                        ReferenceValue.NULL,
                        arguments[0],
                        ReferenceValue.from(Value.asBoxedJavaValues(1, arguments.length - 1, arguments))
                    };
                } else {
                    // Signature: invoke(Value[] args)
                    boxedArguments = new Value[]{
                        ReferenceValue.NULL,
                        ReferenceValue.from(arguments)
                    };
                }
            }
        }

        Value returnValue = createInterpreter().execute(stub, boxedArguments);
        if (classMethodActor.isInstanceInitializer()) {
            return returnValue;
        }
        if (classMethodActor.resultKind() == Kind.VOID) {
            if (boxing == Boxing.JAVA) {
                assertTrue(returnValue == ReferenceValue.NULL);
                returnValue = VoidValue.VOID;
            } else {
                assertTrue(returnValue.asObject() == VoidValue.VOID);
            }
        } else if (classMethodActor.resultKind().javaClass.isPrimitive()) {
            returnValue = Value.fromBoxedJavaValue(returnValue.asBoxedJavaValue());
        }
        return returnValue;
    }

    public static boolean usesWordTypes(MethodActor classMethodActor) {
        final SignatureDescriptor signature = classMethodActor.descriptor();
        if (signature.resultKind().isWord) {
            return true;
        }
        for (int i = 0; i < signature.numberOfParameters(); i++) {
            if (signature.parameterDescriptorAt(i).toKind().isWord) {
                return true;
            }
        }
        return false;
    }

    /**
     * Testing generation of stubs for reflective method invocation as an extra on the side.
     */
    private void testStubs(Method_Type method, Value[] arguments, Value returnValue) {
        final MethodActor classMethodActor = method.classMethodActor();
        for (Boxing boxing : Boxing.values()) {
            // Only test with Java boxing if none of the parameter or return types is a Word type
            if (boxing == Boxing.JAVA) {
                final boolean usesWordTypes = usesWordTypes(classMethodActor);
                if (usesWordTypes) {
                    continue;
                }
            }
            final STUB_TEST_PROPERTIES stubProperties = classMethodActor.getAnnotation(STUB_TEST_PROPERTIES.class);
            if (stubProperties == null || stubProperties.execute()) {
                try {
                    final Method_Type stub = generateAndCompileStubFor(method.classMethodActor(), boxing);
                    try {
                        final Value stubReturnValue = executeInvocationStub(classMethodActor, stub, boxing, arguments);
                        if (stubProperties == null || stubProperties.compareResult()) {
                            if (returnValue.kind().isReference) {
                                if (boxing == Boxing.VALUE) {
                                    assertEquals(((Value) stubReturnValue.asObject()).asObject(), returnValue.asObject());
                                } else {
                                    assertEquals(stubReturnValue.asObject(), returnValue.asObject());
                                }
                            } else {
                                if (boxing == Boxing.VALUE) {
                                    assertEquals(returnValue, stubReturnValue.asObject());
                                } else {
                                    assertEquals(returnValue, stubReturnValue);
                                }
                            }
                        }
                    } catch (Throwable e) {
                        CodeAttribute codeAttribute = method.classMethodActor().codeAttribute();
                        ProgramWarning.message("Failed " + boxing + " stub execution or result comparison for " + classMethodActor);
                        System.err.println("original method:");
                        System.err.println(BytecodePrinter.toString(codeAttribute.cp, new BytecodeBlock(codeAttribute.code())));
                        codeAttribute = stub.classMethodActor().codeAttribute();
                        System.err.println("stub: " + stub.classMethodActor());
                        System.err.println(BytecodePrinter.toString(codeAttribute.cp, new BytecodeBlock(codeAttribute.code())));
                        System.err.println("stub IR:");
                        System.err.println(stub.traceToString());
                        e.printStackTrace();
                    }
                } catch (Throwable e) {
                    ProgramWarning.message("Failed " + boxing + " stub generation for " + classMethodActor);
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * TODO: debug stub interpretation and reenable this.
     */
    protected boolean shouldTestStubs() {
        return false;
    }

    protected ReferenceValue newInstance(ClassActor classActor) {
        try {
            return ReferenceValue.from(Objects.allocateInstance(classActor.toJava()));
        } catch (InstantiationException e) {
            throw FatalError.unexpected("Error instantiating instance of " + classActor, e);
        }
    }

    /**
     * Executes a given compiled method on an interpreter for the compiler's IR.
     *
     * @param method the compiled method to execute
     * @param arguments the arguments (including the receiver for a non-static method). If {@code method} is a
     *            constructor, then a {@linkplain #newInstance(ClassActor) new uninitialized instance} is prepended to
     *            the arguments
     * @return the result of the execution which will be a newly created and initalized object of the appropriate type
     *         if {@code method} is a constructor
     */
    protected final Value executeWithException(final Method_Type method, final Value... arguments) throws InvocationTargetException {
        Trace.begin(3, "interpreting " + method);
        try {
            final ClassMethodActor classMethodActor = method.classMethodActor();
            final boolean isConstructor = classMethodActor.isInstanceInitializer();
            final Value[] executeArguments = isConstructor ? Utils.prepend(arguments, newInstance(classMethodActor.holder())) : arguments;
            final SignatureDescriptor signature = classMethodActor.descriptor();
            int argumentIndex = arguments.length - 1;
            int parameterIndex = signature.numberOfParameters() - 1;
            while (parameterIndex >= 0) {
                final Kind argumentKind = arguments[argumentIndex].kind();
                final Kind parameterKind = signature.parameterDescriptorAt(parameterIndex).toKind();
                ProgramError.check(argumentKind == parameterKind, "Argument " + argumentIndex + " has kind " + argumentKind + " where as kind " + parameterKind + " is expected");
                parameterIndex--;
                argumentIndex--;
            }

            final Value value = createInterpreter().execute(method, executeArguments);
            final Value returnValue = isConstructor ? executeArguments[0] : value;

            if (shouldTestStubs()) {
                testStubs(method, arguments, returnValue);
            }

            return returnValue;
        } finally {
            Trace.end(3, "interpreting " + method);
        }
    }

    protected final Value executeWithReceiverAndException(Method_Type method, Value... arguments) throws InvocationTargetException {
        return executeWithException(method, Utils.prepend(arguments, (Value) ReferenceValue.from(this)));
    }

    protected Value execute(Method_Type method, Value... arguments) {
        try {
            return executeWithException(method, arguments);
        } catch (InvocationTargetException invocationTargetException) {
            fail(Utils.stackTraceAsString(invocationTargetException));
            return null;
        }
    }

    protected Value executeWithReceiver(Method_Type method, Value... arguments) {
        try {
            return executeWithReceiverAndException(method, arguments);
        } catch (InvocationTargetException invocationTargetException) {
            fail(Utils.stackTraceAsString(invocationTargetException));
            return null;
        }
    }

    /**
     * Executes a given method with {@code args}.
     * The method to be executed is expected to raise an exception of type assignable to {@code expectedExceptionType}.
     * If it does not, the test immediately {@linkplain #fail() fails}.
     */
    protected void executeWithExpectedException(final Method_Type method, Class<? extends Throwable> expectedExceptionType, Value... arguments) {
        try {
            executeWithException(method, arguments);
            fail("expected " + expectedExceptionType.getName());
        } catch (InvocationTargetException invocationTargetException) {
            final Throwable exception = invocationTargetException.getCause();
            if (exception == null) {
                fail("expected " + expectedExceptionType.getName());
            }
            if (!expectedExceptionType.isAssignableFrom(exception.getClass())) {
                fail("expected " + expectedExceptionType.getName() + " but got " + Utils.stackTraceAsString(exception));
            }
        }
    }

    /**
     * Executes a given method with {@code args}.
     * The method to be executed is expected to raise an exception of type assignable to {@code expectedExceptionType}.
     * If it does not, the test immediately {@linkplain #fail() fails}.
     */
    protected void executeWithReceiverAndExpectedException(final Method_Type method, Class<? extends Throwable> expectedExceptionType, Value... arguments) {
        try {
            executeWithReceiverAndException(method, arguments);
            fail("expected " + expectedExceptionType.getName());
        } catch (InvocationTargetException invocationTargetException) {
            final Throwable exception = invocationTargetException.getCause();
            if (exception == null) {
                fail("expected " + expectedExceptionType.getName());
            }
            if (!expectedExceptionType.isAssignableFrom(exception.getClass())) {
                fail("expected " + expectedExceptionType.getName() + " but got " + Utils.stackTraceAsString(exception));
            }
        }
    }

    /**
     * Gets a disassembler for a given target method.
     *
     * @param targetMethod a compiled method whose {@linkplain TargetMethod#code() code} is to be disassembled
     * @return a disassembler for the ISA specific code in {@code targetMethod} or null if no such disassembler is available
     */
    protected Disassembler disassemblerFor(TargetMethod targetMethod) {
        return compilerTestSetup().disassemblerFor(targetMethod);
    }

    /**
     * Traces the metadata of the compiled code represented by a given target method followed by a disassembly of
     * the compiled code. If a disassembler is not available for the code, then only the metadata is traced.
     * The trace is sent to the standard {@linkplain Trace#stream() trace stream}.
     */
    public void traceBundleAndDisassemble(TargetMethod targetMethod) {
        targetMethod.traceBundle(INDENT_WRITER);
        disassemble(targetMethod);
    }

    /**
     * Disassembles the compiled code of a given target method if a disassembler is
     * {@linkplain #disassemblerFor(TargetMethod) available}. The disassembler output is sent to the standard
     * {@linkplain Trace#stream() trace stream}.
     */
    protected void disassemble(TargetMethod targetMethod) {
        final int level = Trace.level();
        try {
            Trace.off();
            final Disassembler disassembler = disassemblerFor(targetMethod);
            if (disassembler == null) {
                return;
            }
            final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            final byte[] code = targetMethod.code();
            if (code.length != 0) {
                disassembler.scanAndPrint(new BufferedInputStream(new ByteArrayInputStream(code)), buffer);
                INDENT_WRITER.printLines(new ByteArrayInputStream(buffer.toByteArray()));
            }
        } catch (IOException e) {
            ProgramError.unexpected("disassembly failed for target method " + targetMethod + " :" + e.getMessage());
        } catch (AssemblyException e) {
            ProgramError.unexpected("disassembly failed for target method " + targetMethod + " :" + e.getMessage());
        } finally {
            Trace.on(level);
        }
    }
}
