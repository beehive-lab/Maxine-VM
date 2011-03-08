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
package com.sun.max.vm.cps.ir.interpreter.eir;

import java.lang.reflect.*;
import java.util.*;

import com.sun.max.*;
import com.sun.max.annotate.*;
import com.sun.max.lang.*;
import com.sun.max.program.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.cps.*;
import com.sun.max.vm.cps.eir.*;
import com.sun.max.vm.cps.ir.*;
import com.sun.max.vm.cps.ir.interpreter.*;
import com.sun.max.vm.cps.ir.interpreter.eir.EirCPU.InstructionAddress;
import com.sun.max.vm.cps.ir.interpreter.eir.EirStack.StackAddressOutOfBoundsException;
import com.sun.max.vm.jni.*;
import com.sun.max.vm.thread.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;

/**
 * An interpreter for EIR representations of methods.
 *
 * Instances of this interpreter are configured by the following properties where the
 * value of a property is retrieved by calling {@link System#getProperty(String)} with
 * a key composed of the property name prefixed by {@link IrInterpreter#PROPERTY_PREFIX}.
 * For example, to enable interpreter tracing at level 1, the following statement needs
 * to be called before the interpreter is used:
 * <p>
 * <pre>
 *     Interpreter.setProperty("trace.level", "1");
 * </pre>
 *
 *
 *
 * <p>
 * Property: {@code "jit"} <br />
 * Default: {@code false} <br />
 * Description: Enables {@linkplain #jitEnabled() JIT compilation}. This option is useful for
 *              testing the correctness of compiled code with respect to calling conventions.
 * <p>
 * Property: {@code "trace.level"} <br />
 * Default: {@code 3} <br />
 * Description: Specifies the {@linkplain Trace#level() level} at which the interpreter will emit tracing while interpreting.
 * <p>
 * Property: {@code "trace.cpu"} <br />
 * Default: {@code false} <br />
 * Description: Includes CPU state {@linkplain EirCPU#dump(java.io.PrintStream) dumps} after each instruction traced.
 *
 * @author Bernd Mathiske
 * @author Doug Simon
 */
public abstract class EirInterpreter extends IrInterpreter<EirMethod> implements EirInstructionVisitor {

    private final EirGenerator eirGenerator;

    public EirGenerator<?> eirGenerator() {
        return eirGenerator;
    }

    private EirFrame frame;

    /**
     * @return the current interpreter frame.
     */
    public EirFrame frame() {
        return frame;
    }

    protected void pushFrame(EirFrame f) {
        this.frame = f;
    }

    protected void popFrame() {
        frame = frame.caller();
        if (frame != null && trace) {
            // This just helps reading traces of large methods where the entry point is off-screen
            Trace.stream().println(traceIndentation + "RE-ENTER: " + frame.method());
            Trace.stream().flush();
        }
    }

    class InitialEirFrame extends EirFrame {
        InitialEirFrame() {
            super(null, null);
        }

        @Override
        public EirABI abi() {
            return eirGenerator.eirABIsScheme().nativeABI;
        }
    }

    protected EirFrame initialEirFrame() {
        return new InitialEirFrame();
    }

    /**
     * @return the current interpreter frame's abi.
     */
    public EirABI abi() {
        return frame.abi();
    }

    public EirInterpreter(EirGenerator eirGenerator) {
        this.eirGenerator = eirGenerator;
        this.traceFilters = traceFiltersOption.getValue();
        this.traceCpu = traceCpuOption.getValue();
        this.traceStack = traceStackOption.getValue();
        this.jitEnabled = jitOption.getValue();
        this.trace = traceFilters != null ||
                 traceCpu ||
                 traceStack ||
                 traceOption.getValue();
        this.frame = initialEirFrame();
    }

    protected abstract EirCPU cpu();

    @Override
    public Value execute(IrMethod method, Value... arguments) throws InvocationTargetException {
        EirMethod eirMethod = (EirMethod) method;
        if (eirMethod.isNative() && !eirMethod.isGenerated()) {
            try {
                return eirMethod.classMethodActor().invoke(arguments);
            } catch (IllegalAccessException illegalAccessException) {
                ProgramError.unexpected("could not access method for invocation: " + eirMethod.classMethodActor());
            }
        }

        final boolean savedTraceCpu = this.traceCpu;
        final boolean savedTrace = this.trace;
        if (traceFilters != null) {
            this.traceCpu = false;
            this.trace = false;
            final String name = eirMethod.classMethodActor().holder().name + "." + eirMethod.name();
            for (String filter : traceFilters) {
                if (name.contains(filter)) {
                    this.traceCpu = savedTraceCpu;
                    this.trace = savedTrace;
                    break;
                }
            }
        }

        if (trace && !tracedMethods.contains(eirMethod.classMethodActor())) {
            tracedMethods.add(eirMethod.classMethodActor());
            Trace.stream().println(eirMethod.traceToString());
        }

        try {
            return interpret(eirMethod, arguments);
        } finally {
            this.traceCpu = savedTraceCpu;
            this.trace = savedTrace;
        }
    }

    protected EirLocation[] argumentLocations(EirMethod eirMethod) {
        return eirMethod.parameterLocations();
    }

    /**
     * Location the caller used to retrieve the returned result.
     * Override this if it isn't the same as the method's result location
     * (e.g., if the target uses register windows).
     * @param eirMethod
     * @return
     */
    protected EirLocation returnedResultLocation(EirMethod eirMethod) {
        return eirMethod.resultLocation();
    }

    /**
     * Returns the location, at the caller, of the receiver for the specified method.
     * Default is the location of the first parameter.
     *
     * @param eirMethod
     * @return
     */
    protected EirLocation receiverLocation(EirMethod eirMethod) {
        return eirMethod.parameterLocations()[0];
    }

    protected Value interpret(EirMethod eirMethod, Value... arguments) throws InvocationTargetException {
        // Set up the parameters
        if (arguments.length != 0) {
            setupParameters(argumentLocations(eirMethod), arguments);
        }

        // Set up the call and enter the interpreter
        final Word entrySP = cpu().read(cpu().stackPointer()).asWord();
        final InstructionAddress returnAddress = call(eirMethod);
        interpret(returnAddress);
        final Word exitStackPointer = cpu().read(cpu().stackPointer()).asWord();
        assert exitStackPointer.equals(entrySP);
        assert cpu().stack().sp().equals(entrySP);

        if (returnedResultLocation(eirMethod) != null) {
            final Value result = cpu().read(eirMethod.resultKind(), returnedResultLocation(eirMethod));
            return eirMethod.resultKind().convert(result);
        } else if (eirMethod.classMethodActor().isInstanceInitializer()) {
            final EirLocation receiverLocation = receiverLocation(eirMethod);
            arguments[0] = cpu().read(receiverLocation);
        }
        return VoidValue.VOID;
    }

    /**
     * Writes the arguments of a method about to be called into the CPU state.
     *
     * @param locations the locations to which the arguments will be stored
     * @param arguments the argument values
     */
    protected void setupParameters(EirLocation[] locations, Value... arguments) {
        final Map<EirStackSlot, Value> parameterArguments = new HashMap<EirStackSlot, Value>();
        for (int i = arguments.length - 1; i >= 0; --i) {
            final EirLocation location = locations[i];
            if (location instanceof EirStackSlot) {
                final EirStackSlot slot = (EirStackSlot) location;
                assert slot.purpose == EirStackSlot.Purpose.PARAMETER;
                parameterArguments.put(slot, arguments[i]);
                cpu().push(arguments[i]);
            } else {
                cpu().write(location, arguments[i]);
            }
        }
        for (Map.Entry<EirStackSlot, Value> entry : parameterArguments.entrySet()) {
            final Value value = cpu().stack().read(cpu().readStackPointer().plus(entry.getKey().offset));
            assert value == entry.getValue();
        }
    }

    /**
     * Specializes memory load when the base address is a Word or a long value in which case it must be
     * the address of a stack slot. This will be a write to a JNI handle or a {@linkplain VmThreadLocal thread local variable}.
     */
    @Override
    public Value pointerLoad(Kind kind, Value[] arguments) {
        if (arguments[0].kind() == Kind.LONG || arguments[0].kind().isWord) {
            // Assume this dereferencing a pointer to a stack slot
            assert arguments.length == 2 : "expecting a stack address dereference";
            final Address address = arguments[0].toWord().asAddress();
            final int offset = arguments[1].toInt();
            try {
                return cpu().stack().read(address.plus(offset));
            } catch (StackAddressOutOfBoundsException e) {
                ProgramError.unexpected("bad JNI handle by the looks of it");
            }
        }
        return super.pointerLoad(kind, arguments);
    }

    /**
     * Specializes memory load when the base address is a Word or a long value in which case it must be
     * the address of a stack slot. This will be a write to a JNI handle or a {@linkplain VmThreadLocal thread local variable}.
     */
    @Override
    public void pointerStore(Kind kind, Value[] arguments) {
        if (arguments[0].kind() == Kind.LONG || arguments[0].kind().isWord) {
            // Assume this dereferencing a pointer to a stack slot
            assert arguments.length == 3 : "expecting a stack address dereference";
            final Address address = arguments[0].toWord().asAddress();
            final int offset = arguments[1].toInt();
            try {
                cpu().stack().write(address.plus(offset), arguments[2]);
            } catch (StackAddressOutOfBoundsException e) {
                ProgramError.unexpected("bad JNI handle by the looks of it");
            }
        } else {
            super.pointerStore(kind, arguments);
        }
    }

    private static final String TRACE_INDENTATION_UNIT = "    ";

    private static final String NO_INDENT = new String();

    private String traceIndentation = NO_INDENT;

    private void indent() {
        if (traceIndentation == NO_INDENT) {
            traceIndentation = "";
        } else {
            traceIndentation += TRACE_INDENTATION_UNIT;
        }
    }

    private void outdent() {
        if (traceIndentation != NO_INDENT) {
            if (traceIndentation.length() >= TRACE_INDENTATION_UNIT.length()) {
                traceIndentation = traceIndentation.substring(TRACE_INDENTATION_UNIT.length());
            } else {
                traceIndentation = NO_INDENT;
            }
        }
    }

    /**
     * Default behavior of a call instruction: push the return address on top of the stack and create a new frame.
     * @param eirMethod
     * @return
     */
    protected  InstructionAddress callAndLink(EirMethod eirMethod) {
        final InstructionAddress returnAddress = cpu().nextInstructionAddress();
        cpu().push(ReferenceValue.from(returnAddress));
        frame = new EirFrame(frame, eirMethod);
        return returnAddress;
    }

    public InstructionAddress call(EirMethod eirMethod) {
        if (eirMethod.classMethodActor().isAnnotationPresent(SNIPPET.class)) {
            EirFrame outer = frame == null ? null : frame.caller();
            while (outer != null) {
                ProgramError.check(outer.method() != eirMethod, "snippet implementation is recursive: " + eirMethod);
                outer = outer.caller();
            }
        }

        final InstructionAddress returnAddress = callAndLink(eirMethod);

        cpu().gotoBlock(eirMethod.blocks()[0]);
        if (trace) {
            indent();
            Trace.stream().println(traceIndentation + "ENTER: " + eirMethod);
            Trace.stream().flush();
        }
        return returnAddress;
    }

    protected void ret(InstructionAddress instructionAddress) {
        cpu().gotoInstruction(instructionAddress);
        if (trace) {
            Trace.stream().println(traceIndentation + "EXIT: " + frame.method());
            Trace.stream().flush();
            outdent();
        }
    }

    public void ret() {
        ret((InstructionAddress) cpu().pop().asObject());
        popFrame();
    }

    private final boolean jitEnabled;

    /**
     * Determines if this interpreter instance should try to compile a callee as it is called
     * and then execute the compiled version or if the method should be called via reflection.
     */
    public boolean jitEnabled() {
        return jitEnabled;
    }

    private final List<String> traceFilters;
    private boolean trace;
    private boolean traceCpu;
    private boolean traceStack;
    private final Set<ClassMethodActor> tracedMethods = new HashSet<ClassMethodActor>();

    public boolean traceStack() {
        return traceStack;
    }

    private void dispatchInvocationTargetException(InvocationTargetException invocationTargetException) throws InvocationTargetException {
        while (frame != null) {
            if (frame.catchBlock() != null) {
                if (trace) {
                    Trace.stream().println("Dispatching exception: " + invocationTargetException.getTargetException());
                    Trace.stream().flush();
                    outdent();
                }
                ReferenceValue throwable = ReferenceValue.from(invocationTargetException.getTargetException());
                cpu().write(eirGenerator.catchParameterLocation(), throwable);
                CPSAbstractCompiler.INTERPRETER_EXCEPTION.set(invocationTargetException.getTargetException());
                cpu().gotoBlock(frame.catchBlock());
                return;
            }

            // Pop frame
            if (trace) {
                Trace.stream().println("Unwinding: " + frame.method() + "  due to exception: " + invocationTargetException.getTargetException());
                Trace.stream().flush();
                outdent();
            }
            frame = frame.caller();
        }
        throw invocationTargetException;
    }

    /**
     * Enters (or re-enters) the interpreter loop and interprets until a given instruction address is about to be interpreted.
     *
     * @param returnAddress  the interpreter returns before this instruction address is about to be interpreted
     */
    public void interpret(InstructionAddress returnAddress) throws InvocationTargetException {
        while (cpu().nextInstructionAddress() != returnAddress) {
            try {
                final Class<EirInstruction<EirInstructionVisitor, ?>> type = null;
                final EirInstruction<EirInstructionVisitor, ?> instruction = Utils.cast(type, cpu().nextInstruction());
                if (trace) {
                    if (traceCpu) {
                        cpu().dump(Trace.stream());
                    }
                    Trace.stream().println(traceIndentation + "catch Block: " + frame.catchBlock());
                    Trace.stream().println(traceIndentation + (traceCpu ? frame.method() : "") + "#" + cpu().currentInstructionAddress() + "   " + instruction.toString());
                    Trace.stream().flush();
                }
                instruction.acceptVisitor(this);
            } catch (InvocationTargetException invocationTargetException) {
                dispatchInvocationTargetException(invocationTargetException);
            } catch (ProgramError programError) {
                throw programError;
            } catch (Throwable t) {
                dispatchInvocationTargetException(new InvocationTargetException(t));
            }
        }
    }

    public void visit(EirTry eirTry) {
        frame().setCatchBlock(eirTry.catchBlock());
    }

    public void visit(EirCatch eirCatch) {
    }

    public void visit(EirFiller filler) {
    }

    public void visit(EirInfopoint safepoint) {
    }

    public void visit(EirBreakpoint breakpoint) {
    }

    protected Value adjustToAssignmentType(Value value) {
        if (value == null) {
            return value;
        }
        switch (value.kind().asEnum) {
            case BYTE: {
                return new WordValue(Address.fromInt(value.asByte() & 0xff));
            }
            case SHORT: {
                return new WordValue(Address.fromInt(value.asShort() & 0xffff));
            }
            case BOOLEAN:
            case CHAR:
            case INT: {
                return new WordValue(Address.fromInt(value.toInt()));
            }
            case LONG:
            case FLOAT:
            case DOUBLE:
            case WORD:
            case REFERENCE: {
                return value;
            }
            case VOID: {
                break;
            }
        }
        throw ProgramError.unexpected();
    }

    /**
     * Calls the compiled version of a method by entering a new frame on the interpreter.
     * The method is compiled first if necessary.
     */
    protected void callCompiled(ClassMethodActor classMethodActor) {
        final EirMethod eirMethod = eirGenerator().makeIrMethod(classMethodActor, true);
        call(eirMethod);
    }

    /**
     * Interprets a call to a constructor by calling the real constructor via reflection.
     *
     * @param classMethodActor the actor for the constructor to be called
     */
    protected void callConstructor(ClassMethodActor classMethodActor) {
        final EirABI abi = eirGenerator().createIrMethod(classMethodActor).abi;
        final Kind[] parameterKinds = classMethodActor.getParameterKinds();
        final EirLocation[] argumentLocations = abi.getParameterLocations(EirStackSlot.Purpose.LOCAL, parameterKinds);
        final Value[] arguments = new Value[argumentLocations.length - 1];

        final Value uninitializedValue = cpu().read(parameterKinds[0], argumentLocations[0]);

        for (int i = 1; i < argumentLocations.length; i++) {
            arguments[i - 1] = cpu().read(parameterKinds[i], argumentLocations[i]);
        }

        try {
            final Value initializedValue = classMethodActor.invokeConstructor(arguments);
            Objects.copy(initializedValue.asObject(), uninitializedValue.asObject());
        } catch (InstantiationException e) {
            ProgramError.unexpected("error calling " + classMethodActor, e);
        } catch (IllegalAccessException e) {
            ProgramError.unexpected("could not access " + classMethodActor, e);
        } catch (InvocationTargetException e) {
            ProgramError.unexpected("error calling " + classMethodActor, e.getTargetException());
        }
    }

    private EirMethod jniHandleUnhand;

    /**
     * Gets the compiled EIR version of {@link JniHandle#unhand()}.
     */
    protected EirMethod jniHandleUnhandMethod() {
        if (jniHandleUnhand == null) {
            try {
                final Method javaMethod = JniHandle.class.getDeclaredMethod("unhand");
                final ClassMethodActor classMethodActor = ClassMethodActor.fromJava(javaMethod);
                jniHandleUnhand = eirGenerator().makeIrMethod(classMethodActor, true);
            } catch (NoSuchMethodException e) {
                ProgramError.unexpected("could not find 'JniHandle.get()' method used in JNI stub");
            }
        }
        return jniHandleUnhand;
    }

    /**
     * Unwraps a JNI handle value to get the wrapped reference (or null).
     *
     * @param handle the handle value to unwrap
     * @param argumentLocations the locations holding the parameters to the native method about to be called
     * @param valuesToHandles a mapping from wrapped references to their handles
     * @return the reference wrapped by {@code handle} (which may be {@link ReferenceValue#NULL}
     */
    protected Value unwrapJniHandle(Value handle, EirLocation[] argumentLocations, Map<Value, Value> valuesToHandles) throws InvocationTargetException {
        final Value value = execute(jniHandleUnhandMethod(), handle);
        valuesToHandles.put(value, handle);
        return value;
    }

    /**
     * Calls a method via reflection.
     *
     * @param isNativeInvoke specifies if this is a call to a native method from within the stub generated for the
     *            native method
     * @param classMethodActor the method actor
     * @param parameterKinds the parameter kinds
     * @param argumentLocations the ABI specified locations where the parameter values are held
     */
    private void callViaReflection(boolean isNativeInvoke, ClassMethodActor classMethodActor, Kind[] parameterKinds, EirLocation[] argumentLocations) throws InvocationTargetException {
        final EirABI abi = eirGenerator().createIrMethod(classMethodActor).abi;
        final Value[] arguments = new Value[argumentLocations.length];
        final Map<Value, Value> valuesToHandles = isNativeInvoke ? new HashMap<Value, Value>(argumentLocations.length) : null;
        for (int i = 0; i < argumentLocations.length; i++) {
            if (isNativeInvoke && parameterKinds[i].isReference) {
                final Value argumentValue = cpu().read(Kind.fromJava(JniHandle.class), argumentLocations[i]);
                arguments[i] = unwrapJniHandle(argumentValue, argumentLocations, valuesToHandles);
            } else {
                arguments[i] = cpu().read(parameterKinds[i], argumentLocations[i]);
            }
        }
        try {
            final Value result = classMethodActor.invoke(arguments);
            assert (result == VoidValue.VOID) == (classMethodActor.resultKind() == Kind.VOID);
            if (result != VoidValue.VOID) {
                if (isNativeInvoke && classMethodActor.resultKind().isReference) {
                    final Value handle;
                    handle = valuesToHandles.get(result);
                    ProgramError.check(handle != null, "reference result from native function must be one of the parameters");
                    cpu().write(abi.getResultLocation(handle.kind()), handle);
                } else {
                    cpu().write(abi.getResultLocation(classMethodActor.resultKind()), result);
                }
            }
        } catch (IllegalAccessException illegalAccessException) {
            ProgramError.unexpected("could not invoke method: " + classMethodActor);
        }
    }

    /**
     * Calls a method via reflection, reading the arguments from the parameter locations specified by the ABI.
     */
    protected void callViaReflection(ClassMethodActor classMethodActor) throws InvocationTargetException {
        final EirABI abi = eirGenerator().createIrMethod(classMethodActor).abi;
        final Kind[] parameterKinds = classMethodActor.getParameterKinds();
        final EirLocation[] argumentLocations = abi.getParameterLocations(EirStackSlot.Purpose.LOCAL, parameterKinds);
        callViaReflection(false, classMethodActor, parameterKinds, argumentLocations);
    }

    /**
     * Calls a native method within the JNI stub generated for the native method. The arguments to the
     * native method are read from the parameter locations specified by the ABI.
     */
    protected void callNativeMethodFromNativeStub(ClassMethodActor classMethodActor) throws InvocationTargetException {
        // Unwrap the object arguments and strip the JNIEnv first parameter as well as the class
        // parameter if this is a static method
        final EirABI abi = eirGenerator().createIrMethod(classMethodActor).abi;
        final boolean isStatic = classMethodActor.isStatic();
        final Kind[] parameterKinds = classMethodActor.getParameterKinds();
        final Kind[] jniParameterKinds = Utils.prepend(parameterKinds, Kind.WORD, Kind.WORD);
        final int firstJavaParameterIndex = isStatic ? 2 : 1;
        EirLocation[] parameterLocations = abi.getParameterLocations(EirStackSlot.Purpose.LOCAL, jniParameterKinds);
        final EirLocation[] javaArgumentLocations = Arrays.copyOfRange(parameterLocations, firstJavaParameterIndex, parameterLocations.length);
        final Kind[] javaParameterKinds = Arrays.copyOfRange(jniParameterKinds, firstJavaParameterIndex, jniParameterKinds.length);
        callViaReflection(true, classMethodActor, javaParameterKinds, javaArgumentLocations);
    }

    public void visit(EirCall instruction) throws InvocationTargetException {
        final boolean isNativeStub = frame().method().isNative();
        ClassMethodActor classMethodActor = null;
        final Value method = cpu().read(instruction.function().location());
        final MethodID methodID = MethodID.fromWord(method.asWord());
        classMethodActor = (ClassMethodActor) MethodID.toMethodActor(methodID);
        if (classMethodActor.isInstanceInitializer()) {
            callConstructor(classMethodActor);
        } else if (classMethodActor.holder().toJava() == JniHandles.class) {
            // The method JniHandles.get() *must* have its compiled version interpreted as it
            // may load values from the stack. The other methods in JniHandles invoked from
            // a JNI stub can be interpreted either via reflection or compiled. We go with
            // the latter for now.
            callCompiled(classMethodActor);
        } else if (isNativeStub) {
            if (classMethodActor == frame().method().classMethodActor()) {
                callNativeMethodFromNativeStub(classMethodActor);
            } else {
                callViaReflection(classMethodActor);
            }
        } else {
            // Only compile the callee if the interpreter is not in the entry frame (i.e. it is executing a
            // nested computation such as the method for unwrapping a JNI handle) AND the interpreter allows JIT compilation.
            if (frame().caller() != null && jitEnabled()) {
                callCompiled(classMethodActor);
            } else {
                callViaReflection(classMethodActor);
            }
        }
    }
}
