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
package com.sun.max.vm.classfile;

import static com.sun.max.vm.classfile.ErrorContext.*;
import static com.sun.max.vm.classfile.constant.PoolConstantFactory.*;

import java.io.*;
import java.net.*;
import java.util.*;

import com.sun.cri.bytecode.*;
import com.sun.max.*;
import com.sun.max.annotate.*;
import com.sun.max.ide.*;
import com.sun.max.io.*;
import com.sun.max.program.*;
import com.sun.max.program.option.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.bytecode.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.hosted.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;

/**
 * Writes a {@link ClassActor} out as a valid
 * <a href="http://java.sun.com/docs/books/jvms/second_edition/html/ClassFile.doc.html">JVM class file</a>.
 */
public class ClassfileWriter {

    final ConstantPoolEditor constantPoolEditor;
    DataOutputStream dataOutputStream;

    /**
     * Saves a generated class actor as a class file accessible to the {@linkplain BootClassLoader VM class loader} or to
     * the inspector.
     *
     * @param classInfo the class to be written as a class file
     * @param constantPoolEditor an editor on a constant pool associated with {@code classActor}
     */
    public static void saveGeneratedClass(ClassInfo classInfo, final ConstantPoolEditor constantPoolEditor) throws IOException {
        final byte[] classfile = toByteArray(classInfo, constantPoolEditor);
        if (MaxineVM.isHosted()) {
            ClassfileReader.saveClassfile(classInfo.actor.name.string, classfile);
        } else {
            classInfo.actor.classfile = classfile;
        }
    }

    /**
     * Creates a JVM class file for a given class.
     *
     * @param classInfo the class to be written as a class file
     * @return a byte array containing the class file
     */
    public static byte[] toByteArray(ClassInfo classInfo) {
        return toByteArray(classInfo, mutableConstantPoolEditorFor(classInfo.actor));
    }

    /**
     * Creates a JVM class file for a given class.
     *
     * @param classInfo the class to be written as a class file
     * @param constantPoolEditor an editor on a constant pool associated with {@code classInfo}
     * @return a byte array containing the class file
     */
    public static byte[] toByteArray(ClassInfo classInfo, ConstantPoolEditor constantPoolEditor) {
        final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try {
            new ClassfileWriter(classInfo, constantPoolEditor, byteArrayOutputStream);
        } catch (IOException ioException) {
            // Should never reach here
            throw ProgramError.unexpected(ioException);
        }
        return byteArrayOutputStream.toByteArray();
    }

    /**
     * Makes a copy of a class actor's constant pool and return an editor on it.
     *
     * @param classActor the class whose constant pool is to be copied
     * @return an editor on a copy of {@code classActor}'s constant pool
     */
    public static ConstantPoolEditor mutableConstantPoolEditorFor(ClassActor classActor) {
        final ConstantPoolEditor immutableConstantPoolEditor = classActor.constantPool().edit();
        try {
            return immutableConstantPoolEditor.copy();
        } finally {
            immutableConstantPoolEditor.release();
        }
    }

    /**
     * Writes a JVM class file for a given class to a given output stream. This operation does not modify the
     * given class actor's constant pool.
     */
    public ClassfileWriter(ClassActor classActor, OutputStream outputStream) throws IOException {
        this(new ClassInfo(classActor), mutableConstantPoolEditorFor(classActor), outputStream);
    }

    /**
     * Writes a JVM class file for a given class to a given output stream.
     *
     * @param classInfo the class to be written as a class file
     * @param constantPoolEditor an editor on a constant pool associated with {@code classInfo}.
     *
     *            <b>It's imperative that this constant pool editor was obtained AFTER {@code classInfo} was constructed
     *            as the construction of a ClassInfo object may add more entries to the given class actor's constant
     *            pool.</b>
     *
     * @param outputStream where to write the class file
     * @throws IOException if an IO error occurs while writing to {@code outputStream}
     */
    public ClassfileWriter(ClassInfo classInfo, ConstantPoolEditor constantPoolEditor, OutputStream outputStream) throws IOException {
        this.constantPoolEditor = constantPoolEditor;
        this.constantPoolEditor.append(makeUtf8Constant("--- START OF CONSTANTS ADDED FOR CLASS FILE GENERATION ---"));

        // Initially write nothing: this is just to flesh out the creation of constant pool entries
        dataOutputStream = new DataOutputStream(new NullOutputStream());
        classInfo.write(this);

        // Now write to the provided stream
        dataOutputStream = outputStream instanceof DataOutputStream ? (DataOutputStream) outputStream : new DataOutputStream(outputStream);
        classInfo.write(this);
    }

    /**
     * @see ConstantPoolEditor#indexOf(PoolConstant)
     */
    protected int indexOf(PoolConstant poolConstant) {
        return constantPoolEditor.indexOf(poolConstant);
    }

    protected int indexOfUtf8(Utf8Constant utf8Constant) {
        return indexOf(utf8Constant);
    }

    protected int indexOfUtf8(String string) {
        return indexOfUtf8(makeUtf8Constant(string));
    }

    protected int indexOfUtf8(Descriptor descriptor) {
        return indexOfUtf8(makeUtf8Constant(descriptor.string));
    }

    protected int indexOfClass(ClassActor classActor) {
        return indexOf(createClassConstant(classActor.typeDescriptor));
    }

    protected int indexOfClass(TypeDescriptor typeDescriptor) {
        return indexOf(createClassConstant(typeDescriptor));
    }

    protected void writeUnsigned2(int value) throws IOException {
        dataOutputStream.writeShort(value);
    }

    protected void writeUnsigned4(int value) throws IOException {
        dataOutputStream.writeInt(value);
    }

    protected void writeUnsigned1Array(byte[] value) throws IOException {
        dataOutputStream.write(value);
    }

    protected void writeUnsigned2Array(byte[] value) throws IOException {
        dataOutputStream.write(value);
    }

    public void writeAttributes(List<Attribute> attributes) throws IOException {
        writeUnsigned2(attributes.size());
        for (Attribute attribute : attributes) {
            attribute.write(this);
        }
    }

    public static class Attribute {
        public final Utf8Constant name;
        protected int length = -1;

        public Attribute(String name) {
            this.name = makeUtf8Constant(name);
        }

        protected void writeData(ClassfileWriter cf) throws IOException {
        }

        public final void write(ClassfileWriter cf) throws IOException {
            cf.writeUnsigned2(cf.indexOf(name));
            cf.writeUnsigned4(length);
            final int start = cf.dataOutputStream.size();
            writeData(cf);
            final int lth = cf.dataOutputStream.size() - start;
            assert this.length == lth || this.length == -1;
            this.length = lth;
            assert this.length != -1;
        }
    }

    public static class BytesAttribute extends Attribute {
        protected final byte[] bytes;
        public BytesAttribute(String name, byte[] bytes) {
            super(name);
            this.bytes = bytes;
        }
        @Override
        protected void writeData(ClassfileWriter cf) throws IOException {
            cf.writeUnsigned1Array(bytes);
        }
    }

    /**
     * The {@code MaxineFlags} attribute is a Maxine specific attribute for a class, method or field
     * in a class file generated from a {@link ClassActor}. It preserves the extra flags for these
     * components.
     *
     * The {@code MaxineFlags} attribute has the following format:
     * <pre>
     *     MaxineFlags_attribute {
     *         u2 attribute_name_index;
     *         u4 attribute_length;
     *         u4 flags;
     *     }
     */
    public static class MaxineFlags extends Attribute {
        public static final String NAME = "MaxineFlags";
        public final int flags;

        public MaxineFlags(int flags) {
            super(NAME);
            this.flags = flags;
        }
        @Override
        protected void writeData(ClassfileWriter cf) throws IOException {
            cf.writeUnsigned4(flags);
        }
    }

    /**
     * Represents the class file info common to classes, fields and methods.
     */
    public abstract static class Info<Actor_Type extends Actor> {
        protected final List<Attribute> attributes = new ArrayList<Attribute>();
        public final Actor_Type actor;

        protected Info(Actor_Type actor) {
            this.actor = actor;
            final Utf8Constant genericSignature = actor.genericSignature();
            final byte[] runtimeVisibleAnnotationsBytes = actor.runtimeVisibleAnnotationsBytes();
            if (genericSignature != null) {
                attributes.add(new Attribute("Signature") {
                    @Override
                    protected void writeData(ClassfileWriter cf) throws IOException {
                        cf.writeUnsigned2(cf.indexOfUtf8(genericSignature));
                    }
                });
            }
            if (runtimeVisibleAnnotationsBytes != null) {
                attributes.add(new BytesAttribute("RuntimeVisibleAnnotations", runtimeVisibleAnnotationsBytes));
            }
            if (actor.isSynthetic()) {
                final ClassActor holder;
                if (actor instanceof ClassActor) {
                    holder = (ClassActor) actor;
                } else {
                    holder = ((MemberActor) actor).holder();
                }
                if (holder.majorVersion < 49) {
                    attributes.add(new Attribute("Synthetic"));
                } else {
                    // The ACC_SYNTHETIC flag was introduced in version 49 of the class file format
                    // and so it is used in preference to the Synthetic attribute. The former is far
                    // more compact and easier to process.
                }
            }
            if (actor.isDeprecated()) {
                attributes.add(new Attribute("Deprecated"));
            }
        }

        protected abstract void write(ClassfileWriter cf) throws IOException;
    }

    /**
     * {@link Character#isDigit(char)} answers {@code true} to some non-ascii digits. This one does not.
     */
    private static boolean isAsciiDigit(char c) {
        return '0' <= c && c <= '9';
    }

    public static String innerClassName(TypeDescriptor innerClass) {
        final String javaName = innerClass.toJavaString();
        final int lastDotIndex = javaName.lastIndexOf('.');
        final int lastDollarIndex = javaName.lastIndexOf('$');
        final int chopIndex = Math.max(lastDollarIndex, lastDotIndex) + 1;
        final String simpleJavaName = javaName.substring(chopIndex);
        final int length = simpleJavaName.length();
        int index = 0;
        while (index < length && isAsciiDigit(simpleJavaName.charAt(index))) {
            index++;
        }
        // Eventually, this is the empty string iff this is an anonymous class
        return simpleJavaName.substring(index);
    }

    public int innerClassNameIndex(TypeDescriptor innerClass) {
        final String innerClassName = innerClassName(innerClass);
        if (innerClassName.isEmpty()) {
            // anonymous class
            return 0;
        }
        return indexOfUtf8(innerClassName);
    }

    /**
     * Represents the class file info for a class.
     */
    public static class ClassInfo extends Info<ClassActor> {

        protected final InterfaceActor[] interfaceActors;
        protected final MethodInfo[] methods;
        protected final FieldInfo[] fields;

        /**
         * Creates a representation of a class that can be written as a class file.
         */
        public ClassInfo(ClassActor classActor) {
            super(classActor);
            interfaceActors = classActor.localInterfaceActors();
            List<MethodActor> methodActors = classActor.getLocalMethodActors();
            methods = new MethodInfo[methodActors.size()];
            for (int i = 0; i < methods.length; ++i) {
                methods[i] = new MethodInfo(methodActors.get(i));
            }

            FieldActor[] fieldActors = Utils.concat(classActor.localInstanceFieldActors(), classActor.localStaticFieldActors());
            fields = new FieldInfo[fieldActors.length];
            for (int i = 0; i < fieldActors.length; i++) {
                fields[i] = new FieldInfo(fieldActors[i]);
            }
            final TypeDescriptor outerClass = classActor.outerClassDescriptor();
            final TypeDescriptor[] innerClasses = classActor.innerClassDescriptors();
            final EnclosingMethodInfo enclosingMethod = classActor.enclosingMethodInfo();
            final String sourceFileName = classActor.sourceFileName;

            if (sourceFileName != null) {
                attributes.add(new Attribute("SourceFile") {
                    @Override
                    protected void writeData(ClassfileWriter cf) throws IOException {
                        cf.writeUnsigned2(cf.indexOfUtf8(sourceFileName));
                    }
                });
            }
            if (enclosingMethod != null) {
                attributes.add(new Attribute("EnclosingMethod") {
                    @Override
                    protected void writeData(ClassfileWriter cf) throws IOException {
                        cf.writeUnsigned2(cf.indexOfClass(enclosingMethod.holder()));
                        if (enclosingMethod.name() != null) {
                            final Utf8Constant methodName = makeUtf8Constant(enclosingMethod.name());
                            final Utf8Constant descriptor = makeUtf8Constant(enclosingMethod.descriptor());
                            final NameAndTypeConstant nameAndType = createNameAndTypeConstant(methodName, descriptor);
                            cf.writeUnsigned2(cf.indexOf(nameAndType));
                        } else {
                            cf.writeUnsigned2(0);
                        }
                    }
                });
            }

            if (outerClass != null || innerClasses != null) {
                attributes.add(new Attribute("InnerClasses") {
                    @Override
                    protected void writeData(ClassfileWriter cf) throws IOException {
                        cf.writeUnsigned2((outerClass == null ? 0 : 1) + (innerClasses == null ? 0 : innerClasses.length));
                        if (outerClass != null) {
                            cf.writeUnsigned2(cf.indexOfClass(actor.typeDescriptor));
                            cf.writeUnsigned2(cf.indexOfClass(outerClass));
                            cf.writeUnsigned2(cf.innerClassNameIndex(actor.typeDescriptor));
                            cf.writeUnsigned2(actor.flags() & Actor.JAVA_CLASS_FLAGS);
                        }
                        if (innerClasses != null) {
                            final int outerClassIndex = cf.indexOfClass(actor.typeDescriptor);
                            for (TypeDescriptor innerClass : innerClasses) {
                                // Not really correct: would require resolving inner classes first
                                final int innerClassFlags = 0;

                                cf.writeUnsigned2(cf.indexOfClass(innerClass));
                                cf.writeUnsigned2(outerClassIndex);
                                cf.writeUnsigned2(cf.innerClassNameIndex(innerClass));
                                cf.writeUnsigned2(innerClassFlags);
                            }
                        }
                    }
                });
            }

            if ((classActor.flags() & ~Actor.JAVA_CLASS_FLAGS) != 0) {
                attributes.add(new MaxineFlags(classActor.flags()));
            }
        }

        @Override
        protected void write(ClassfileWriter cf) throws IOException {
            final String className = actor.name.string;
            final TypeDescriptor classDescriptor = JavaTypeDescriptor.getDescriptorForWellFormedTupleName(className);
            final int thisClassIndex = cf.indexOf(createClassConstant(classDescriptor));
            final ClassActor superClassActor = actor.superClassActor;
            final int superClassIndex = superClassActor == null ? 0 : cf.indexOf(createClassConstant(superClassActor.typeDescriptor));
            cf.writeUnsigned4(0xcafebabe);
            cf.writeUnsigned2(actor.minorVersion);
            cf.writeUnsigned2(actor.majorVersion);
            cf.constantPoolEditor.write(cf.dataOutputStream);

            cf.writeUnsigned2(actor.flags()/* & Actor.JAVA_CLASS_FLAGS*/);
            cf.writeUnsigned2(thisClassIndex);
            cf.writeUnsigned2(superClassIndex);

            cf.writeUnsigned2(interfaceActors.length);
            for (InterfaceActor interfaceActor : interfaceActors) {
                cf.writeUnsigned2(cf.indexOfClass(interfaceActor));
            }

            cf.writeUnsigned2(fields.length);
            for (FieldInfo field : fields) {
                field.write(cf);
            }

            cf.writeUnsigned2(methods.length);
            for (MethodInfo method : methods) {
                method.write(cf);
            }

            cf.writeAttributes(attributes);
        }
    }

    /**
     * Represents the class file info common to fields and methods.
     */
    public abstract static class MemberInfo<MemberActor_Type extends MemberActor> extends Info<MemberActor_Type> {

        protected MemberInfo(MemberActor_Type memberActor) {
            super(memberActor);
        }

        protected abstract int classfileFlags();

        @Override
        protected void write(ClassfileWriter cf) throws IOException {
            cf.writeUnsigned2(classfileFlags());
            cf.writeUnsigned2(cf.indexOfUtf8(actor.name));
            cf.writeUnsigned2(cf.indexOfUtf8(actor.descriptor));
            cf.writeAttributes(attributes);
        }
    }

    /**
     * Preserves the original bytecode loaded or generated for a method actor. This bytecode is guaranteed to be
     * valid JVM code which can be used to (re)generate a valid class file. This is required in hosted execution
     * mode when {@link ClassActor}s may have to be serialized to class files that can be loaded by the
     * underlying JVM.
     */
    @HOSTED_ONLY
    public static final HashMap<MethodActor, byte[]> classfileCodeMap = new HashMap<MethodActor, byte[]>();

    /**
     * Exists for the same reasons as {@link #classfileCodeMap}. The reason {@link #classfileCodeMap} is required
     * in addition to this map is that {@linkplain BytecodeIntrinsifier intrinsification} occurs 'in situ'.
     */
    @HOSTED_ONLY
    public static final HashMap<MethodActor, CodeAttribute> classfileCodeAttributeMap = new HashMap<MethodActor, CodeAttribute>();

    /**
     * Represents the class file info for methods.
     */
    public static class MethodInfo extends MemberInfo<MethodActor> {

        MethodInfo(MethodActor methodActor) {
            super(methodActor);
            CodeAttribute codeAttribute = methodActor instanceof ClassMethodActor ? ((ClassMethodActor) methodActor).codeAttribute() : null;
            final byte[] runtimeVisibleParameterAnnotationsBytes = methodActor.runtimeVisibleParameterAnnotationsBytes();
            final byte[] annotationDefaultBytes = methodActor.annotationDefaultBytes();
            final TypeDescriptor[] checkedExceptions = methodActor.checkedExceptions();

            if ((methodActor.flags() & ~Actor.JAVA_METHOD_FLAGS) != 0) {
                attributes.add(new MaxineFlags(methodActor.flags()));
            }

            if (runtimeVisibleParameterAnnotationsBytes != null) {
                attributes.add(new BytesAttribute("RuntimeVisibleParameterAnnotations", runtimeVisibleParameterAnnotationsBytes));
            }
            if (annotationDefaultBytes != null) {
                attributes.add(new BytesAttribute("AnnotationDefault", annotationDefaultBytes));
            }
            if (checkedExceptions.length != 0) {
                attributes.add(new Attribute("Exceptions") {
                    @Override
                    protected void writeData(ClassfileWriter cf) throws IOException {
                        cf.writeUnsigned2(checkedExceptions.length);
                        for (TypeDescriptor checkedException : checkedExceptions) {
                            final int checkedExceptionIndex = cf.indexOfClass(checkedException);
                            cf.writeUnsigned2(checkedExceptionIndex);
                        }
                    }
                });
            }

            if (codeAttribute != null) {
                if (MaxineVM.isHosted()) {
                    if (classfileCodeAttributeMap.containsKey(methodActor)) {
                        codeAttribute = classfileCodeAttributeMap.get(methodActor);
                    }
                }

                Code code = new Code(codeAttribute);
                if (MaxineVM.isHosted()) {
                    byte[] classfileCode = classfileCodeMap.get(methodActor);
                    if (classfileCode != null) {
                        code.classfileCode = classfileCode;
                    }
                }

                attributes.add(code);
            }
        }

        @Override
        protected int classfileFlags() {
            return actor.flags() & Actor.JAVA_METHOD_FLAGS;
        }
    }

    public static class Code extends Attribute {
        protected final List<Attribute> attributes = new ArrayList<Attribute>();
        protected final CodeAttribute codeAttribute;
        @HOSTED_ONLY
        protected byte[] classfileCode;

        public Code(CodeAttribute codeAttribute) {
            super("Code");
            this.codeAttribute = codeAttribute;
            final StackMapTable stackMapTable = codeAttribute.stackMapTable();
            final LineNumberTable lineNumberTable = codeAttribute.lineNumberTable();
            final LocalVariableTable localVariableTable = codeAttribute.localVariableTable();

            if (!lineNumberTable.isEmpty()) {
                attributes.add(new Attribute("LineNumberTable") {
                    @Override
                    protected void writeData(ClassfileWriter cf) throws IOException {
                        lineNumberTable.writeAttributeInfo(cf.dataOutputStream, cf.constantPoolEditor);
                    }
                });
            }
            if (!localVariableTable.isEmpty()) {
                attributes.add(new Attribute("LocalVariableTable") {
                    @Override
                    protected void writeData(ClassfileWriter cf) throws IOException {
                        localVariableTable.writeLocalVariableTableAttributeInfo(cf.dataOutputStream, cf.constantPoolEditor);
                    }
                });
            }
            if (localVariableTable.numberOfEntriesWithSignature() != 0) {
                attributes.add(new Attribute("LocalVariableTypeTable") {
                    @Override
                    protected void writeData(ClassfileWriter cf) throws IOException {
                        localVariableTable.writeLocalVariableTypeTableAttributeInfo(cf.dataOutputStream, cf.constantPoolEditor);
                    }
                });
            }
            if (stackMapTable != null) {
                attributes.add(new Attribute("StackMapTable") {
                    @Override
                    protected void writeData(ClassfileWriter cf) throws IOException {
                        stackMapTable.writeAttributeInfo(cf.dataOutputStream, cf.constantPoolEditor);
                    }
                });
            }
        }

        /**
         * Replace non-standard bytecodes with standard JVM bytecodes.
         */
        private static byte[] standardizeCode(final CodeAttribute codeAttribute, final ClassfileWriter cf) {
            final byte[] code = codeAttribute.code();
            if (code == null) {
                return code;
            }
            final byte[] codeCopy = code.clone();
            final BytecodeAdapter bytecodeAdapter = new BytecodeAdapter() {
                @Override
                protected void jnicall(int nativeFunctionDescriptorIndex) {
                    final Utf8Constant name = SymbolTable.makeSymbol("callnative_" + nativeFunctionDescriptorIndex);
                    final ConstantPool pool = codeAttribute.cp;
                    final SignatureDescriptor signature = SignatureDescriptor.create(pool.utf8At(nativeFunctionDescriptorIndex, "native function descriptor"));
                    final ClassMethodRefConstant method = PoolConstantFactory.createClassMethodConstant(pool.holder(), name, signature);
                    final int index = cf.indexOf(method);
                    final int bci = bytecodeScanner().currentOpcodeBCI();
                    codeCopy[bci] = (byte) Bytecodes.INVOKESTATIC;
                    codeCopy[bci + 1] = (byte) (index >> 8);
                    codeCopy[bci + 2] = (byte) index;
                }
            };
            new BytecodeScanner(bytecodeAdapter).scan(new BytecodeBlock(code));
            return codeCopy;
        }

        @Override
        protected void writeData(ClassfileWriter cf) throws IOException {
            final ExceptionHandlerEntry[] exceptionHandlerTable = codeAttribute.exceptionHandlerTable();
            cf.writeUnsigned2(codeAttribute.maxStack);
            cf.writeUnsigned2(codeAttribute.maxLocals);
            final byte[] code;
            if (MaxineVM.isHosted() && classfileCode != null) {
                code = classfileCode;
            } else {
                code = standardizeCode(codeAttribute, cf);
            }
            cf.writeUnsigned4(code.length);
            cf.writeUnsigned1Array(code);
            cf.writeUnsigned2(exceptionHandlerTable.length);
            for (ExceptionHandlerEntry info : exceptionHandlerTable) {
                cf.writeUnsigned2(info.startBCI()); // start_pc
                cf.writeUnsigned2(info.endBCI()); // end_pc
                cf.writeUnsigned2(info.handlerBCI()); // handler_pc
                cf.writeUnsigned2(info.catchTypeIndex()); // catch_type
            }
            cf.writeAttributes(attributes);
        }
    }

    /**
     * Represents the class file info for fields.
     */
    public static class FieldInfo extends MemberInfo<FieldActor> {
        protected FieldInfo(FieldActor fieldActor) {
            super(fieldActor);
            final Value constantValue = fieldActor.constantValue();

            if ((fieldActor.flags() & ~Actor.JAVA_FIELD_FLAGS) != 0) {
                attributes.add(new MaxineFlags(fieldActor.flags()));
            }

            if (constantValue != null) {
                attributes.add(new Attribute("ConstantValue") {
                    @Override
                    protected void writeData(ClassfileWriter cf) throws IOException {
                        switch (actor.kind.asEnum) {
                            case BOOLEAN:
                            case BYTE:
                            case CHAR:
                            case SHORT:
                            case INT:
                            {
                                cf.writeUnsigned2(cf.indexOf(createIntegerConstant(constantValue.toInt())));
                                break;
                            }
                            case FLOAT: {
                                cf.writeUnsigned2(cf.indexOf(createFloatConstant(constantValue.toFloat())));
                                break;
                            }
                            case LONG: {
                                cf.writeUnsigned2(cf.indexOf(createLongConstant(constantValue.toLong())));
                                break;
                            }
                            case DOUBLE: {
                                cf.writeUnsigned2(cf.indexOf(createDoubleConstant(constantValue.toDouble())));
                                break;
                            }
                            case REFERENCE: {
                                final Object object = constantValue.asObject();
                                if (!(object instanceof String)) {
                                    throw classFormatError("Invalid ConstantValue attribute");
                                }
                                final String string = (String) object;
                                cf.writeUnsigned2(cf.indexOfUtf8(string));
                                break;
                            }
                            default: {
                                throw classFormatError("Cannot have ConstantValue for fields of type " + actor.kind);
                            }
                        }
                    }
                });
            }
        }

        @Override
        protected int classfileFlags() {
            return actor.flags() & Actor.JAVA_FIELD_FLAGS;
        }
    }

    /**
     * A command line interface for producing Maxine preprocessed class files for one or more classes.
     */
    @HOSTED_ONLY
    public static void main(String[] args) {
        final OptionSet options = new OptionSet() {
            @Override
            protected void printHelpHeader(PrintStream stream) {
                stream.println("Usage: " + ClassfileWriter.class.getSimpleName() + " [-options] <classes>...");
                stream.println();
                stream.println("where options include:");
            }
        };
        final Option<File> outputDirectoryOption = options.newFileOption("d", new File("generated").getAbsoluteFile(),
            "Specifies where to place generated class files");
        final Option<Boolean> javapOption = options.newBooleanOption("javap", false,
            "Runs javap on the generated class file(s).");
        final Option<Boolean> helpOption = options.newBooleanOption("help", false, "Show help message and exits.");

        Trace.addTo(options);
        VMConfigurator vmConfigurator = new VMConfigurator(options);
        options.parseArguments(args);

        if (helpOption.getValue()) {
            options.printHelp(System.out, 80);
            return;
        }

        final String[] arguments = options.getArguments();
        if (arguments.length == 0) {
            options.printHelp(System.out, 80);
            return;
        }

        vmConfigurator.create(true);
        JavaPrototype.initialize(false);

        final Map<String, byte[]> classNameToClassfileMap = new LinkedHashMap<String, byte[]>();
        for (String className : arguments) {
            processClass(className, outputDirectoryOption.getValue(), classNameToClassfileMap);
        }

        Trace.line(1, "Generated " + classNameToClassfileMap.size() + " class file" + (classNameToClassfileMap.size() == 1 ? "" : "s") + " to " + outputDirectoryOption.getValue());

        testLoadGeneratedClasses(classNameToClassfileMap, outputDirectoryOption.getValue());

        if (javapOption.getValue()) {
            for (String className : classNameToClassfileMap.keySet()) {
                final Classpath cp = Classpath.fromSystem().prepend(outputDirectoryOption.getValue().getPath());
                final String[] javapArgs = {"-verbose", "-private", "-bootclasspath", cp.toString(), className};
                ToolChain.javap(javapArgs);
            }
        }
    }

    @HOSTED_ONLY
    private static void processClass(String className, File outputDirectory, Map<String, byte[]> classNameToClassfileMap) {
        Class<?> javaClass;
        try {
            javaClass = Class.forName(className);
            if (MaxineVM.isHostedOnly(javaClass)) {
                ProgramWarning.message("Cannot create a class actor for prototype only class: " + className);
                return;
            }
        } catch (VerifyError e) {
        } catch (ClassNotFoundException e) {
            ProgramWarning.message("Could not find class: " + className);
            return;
        }

        TypeDescriptor typeDescriptor = JavaTypeDescriptor.getDescriptorForJavaString(className);
        final ClassActor classActor = typeDescriptor.resolve(null);
        final byte[] classfileBytes = toByteArray(new ClassInfo(classActor));

        final File classfileFile = new File(outputDirectory, classActor.name.string.replace(".", File.separator) + ".class").getAbsoluteFile();
        BufferedOutputStream bs = null;
        try {
            final File classfileDirectory = classfileFile.getParentFile();
            if (!(classfileDirectory.exists())) {
                if (!classfileDirectory.mkdirs()) {
                    throw new IOException("Could not create directory: " + classfileDirectory);
                }
            }
            bs = new BufferedOutputStream(new FileOutputStream(classfileFile));
            bs.write(classfileBytes);

            classNameToClassfileMap.put(className, classfileBytes);
            Trace.line(1, "Generated " + classfileFile.getPath());
        } catch (IOException ex) {
            ProgramWarning.message("Error saving class file to " + classfileFile + ": " + ex.getMessage());
            return;
        } finally {
            if (bs != null) {
                try {
                    bs.close();
                } catch (IOException ex) {
                }
            }
        }
    }

    @HOSTED_ONLY
    static class TestClassLoader extends ClassLoader {
        public void load(String className, byte[] classfileBytes) {
            ClassfileReader.defineClassActor(className, this, classfileBytes, null, null, false);
        }
    }

    @HOSTED_ONLY
    private static void testLoadGeneratedClasses(Map<String, byte[]> classNameToClassfileMap, File outputDirectory) {

        try {
            final URL[] urls = {outputDirectory.toURI().toURL()};
            final ClassLoader urlClassLoader = URLClassLoader.newInstance(urls);
            final TestClassLoader testClassLoader = new TestClassLoader();
            ClassRegistry.testClassLoader = testClassLoader;
            for (Map.Entry<String, byte[]> entry : classNameToClassfileMap.entrySet()) {
                final String className = entry.getKey();
                final byte[] classfileBytes = entry.getValue();

                try {
                    testClassLoader.load(className, classfileBytes);
                } catch (LinkageError linkageError) {
                    ProgramWarning.message(linkageError.toString());
                }

                try {
                    Class.forName(className, true, urlClassLoader);
                } catch (LinkageError linkageError) {
                    ProgramWarning.message(linkageError.toString());
                } catch (ClassNotFoundException classNotFoundException) {
                    ProgramWarning.message(classNotFoundException.toString());
                }
            }
        } catch (MalformedURLException malformedURLException) {
            ProgramWarning.message(malformedURLException.toString());
        }
    }
}
