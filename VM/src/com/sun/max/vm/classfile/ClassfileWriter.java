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
/*VCSID=bc083467-48bc-40ab-81b4-ba717bd38833*/
package com.sun.max.vm.classfile;

import static com.sun.max.vm.classfile.ErrorContext.*;
import static com.sun.max.vm.classfile.constant.PoolConstantFactory.*;

import java.io.*;
import java.net.*;
import java.util.*;

import com.sun.max.annotate.*;
import com.sun.max.collect.*;
import com.sun.max.ide.*;
import com.sun.max.io.*;
import com.sun.max.lang.Arrays;
import com.sun.max.platform.*;
import com.sun.max.program.*;
import com.sun.max.program.option.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.prototype.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;

/**
 * Writes a {@link ClassActor} out as a valid
 * <a href="http://java.sun.com/docs/books/jvms/second_edition/html/ClassFile.doc.html">JVM class file</a>.
 *
 * @author Doug Simon
 */
public class ClassfileWriter {

    final ConstantPoolEditor _constantPoolEditor;
    DataOutputStream _dataOutputStream;

    /**
     * Saves a generated class actor as a class file accessible to the {@linkplain VmClassLoader VM class loader} or to the inspector.
     *
     * @param classInfo
     *                the class to be written as a class file
     * @param constantPoolEditor
     *                an editor on a constant pool associated with {@code classActor}
     */
    public static void saveGeneratedClass(ClassInfo classInfo, final ConstantPoolEditor constantPoolEditor) throws IOException {
        final byte[] classfile = toByteArray(classInfo, constantPoolEditor);
        if (MaxineVM.isPrototyping()) {
            VmClassLoader.VM_CLASS_LOADER.saveGeneratedClassfile(classInfo._actor.name().string(), classfile);
        } else {
            classInfo._actor.setClassfile(classfile);
        }
    }

    /**
     * Creates a JVM class file for a given class.
     *
     * @param classInfo
     *                the class to be written as a class file
     * @return a byte array containing the class file
     */
    public static byte[] toByteArray(ClassInfo classInfo) {
        return toByteArray(classInfo, mutableConstantPoolEditorFor(classInfo._actor));
    }

    /**
     * Creates a JVM class file for a given class.
     *
     * @param classInfo
     *                the class to be written as a class file
     * @param constantPoolEditor
     *                an editor on a constant pool associated with {@code classInfo}
     * @return a byte array containing the class file
     */
    public static byte[] toByteArray(ClassInfo classInfo, ConstantPoolEditor constantPoolEditor) {
        final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try {
            new ClassfileWriter(classInfo, constantPoolEditor, byteArrayOutputStream);
        } catch (IOException ioException) {
            // Should never reach here
            ProgramError.unexpected(ioException);
        }
        return byteArrayOutputStream.toByteArray();
    }

    /**
     * Makes a copy of a class actor's constant pool and return an editor on it.
     *
     * @param classActor
     *                the class whose constant pool is to be copied
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
     * @param classInfo
     *                the class to be written as a class file
     * @param constantPoolEditor
     *                an editor on a constant pool associated with {@code classInfo}.
     *                <p>
     *                <b>It's imperative that this constant pool editor was obtained AFTER {@code classInfo} was
     *                constructed as the construction of a ClassInfo object may add more entries to the given class
     *                actor's constant pool.</b>
     * @param outputStream
     *                where to write the class file
     * @throws IOException
     *                 if an IO error occurs while writing to {@code outputStream}
     */
    public ClassfileWriter(ClassInfo classInfo, ConstantPoolEditor constantPoolEditor, OutputStream outputStream) throws IOException {
        _constantPoolEditor = constantPoolEditor;
        _constantPoolEditor.append(makeUtf8Constant("--- START OF CONSTANTS ADDED FOR CLASS FILE GENERATION ---"));

        // Initially write nothing: this is just to flesh out the creation of constant pool entries
        _dataOutputStream = new DataOutputStream(new NullOutputStream());
        classInfo.write(this);

        // Now write to the provided stream
        _dataOutputStream = outputStream instanceof DataOutputStream ? (DataOutputStream) outputStream : new DataOutputStream(outputStream);
        classInfo.write(this);
    }

    /**
     * @see ConstantPoolEditor#indexOf(PoolConstant)
     */
    protected int indexOf(PoolConstant poolConstant) {
        return _constantPoolEditor.indexOf(poolConstant);
    }

    protected int indexOfUtf8(Utf8Constant utf8Constant) {
        return indexOf(utf8Constant);
    }

    protected int indexOfUtf8(String string) {
        return indexOfUtf8(makeUtf8Constant(string));
    }

    protected int indexOfUtf8(Descriptor descriptor) {
        return indexOfUtf8(makeUtf8Constant(descriptor.string()));
    }

    protected int indexOfClass(ClassActor classActor) {
        return indexOf(createClassConstant(classActor.typeDescriptor()));
    }

    protected int indexOfClass(TypeDescriptor typeDescriptor) {
        return indexOf(createClassConstant(typeDescriptor));
    }

    protected void writeUnsigned2(int value) throws IOException {
        _dataOutputStream.writeShort(value);
    }

    protected void writeUnsigned4(int value) throws IOException {
        _dataOutputStream.writeInt(value);
    }

    protected void writeUnsigned1Array(byte[] value) throws IOException {
        _dataOutputStream.write(value);
    }

    protected void writeUnsigned2Array(byte[] value) throws IOException {
        _dataOutputStream.write(value);
    }

    public void writeAttributes(Sequence<Attribute> attributes) throws IOException {
        writeUnsigned2(attributes.length());
        for (Attribute attribute : attributes) {
            attribute.write(this);
        }
    }

    public static class Attribute {
        public final Utf8Constant _name;
        protected int _length = -1;

        public Attribute(String name) {
            _name = makeUtf8Constant(name);
        }

        protected void writeData(ClassfileWriter cf) throws IOException {
        }

        public final void write(ClassfileWriter cf) throws IOException {
            cf.writeUnsigned2(cf.indexOf(_name));
            cf.writeUnsigned4(_length);
            final int start = cf._dataOutputStream.size();
            writeData(cf);
            final int length =  cf._dataOutputStream.size() - start;
            assert _length == length || _length == -1;
            _length = length;
            assert _length != -1;
        }
    }

    public static class BytesAttribute extends Attribute {
        protected final byte[] _bytes;
        public BytesAttribute(String name, byte[] bytes) {
            super(name);
            _bytes = bytes;
        }
        @Override
        protected void writeData(ClassfileWriter cf) throws IOException {
            cf.writeUnsigned1Array(_bytes);
        }
    }

    /**
     * Represents the class file info common to classes, fields and methods.
     */
    public abstract static class Info<Actor_Type extends Actor> {
        protected final AppendableSequence<Attribute> _attributes = new ArrayListSequence<Attribute>();
        public final Actor_Type _actor;

        protected Info(Actor_Type actor) {
            _actor = actor;
            final Utf8Constant genericSignature = actor.genericSignature();
            final byte[] runtimeVisibleAnnotationsBytes = actor.runtimeVisibleAnnotationsBytes();
            if (genericSignature != null) {
                _attributes.append(new Attribute("Signature") {
                    @Override
                    protected void writeData(ClassfileWriter cf) throws IOException {
                        cf.writeUnsigned2(cf.indexOfUtf8(genericSignature));
                    }
                });
            }
            if (runtimeVisibleAnnotationsBytes != null) {
                _attributes.append(new BytesAttribute("RuntimeVisibleAnnotations", runtimeVisibleAnnotationsBytes));
            }
            if (_actor.isSynthetic()) {
                final ClassActor holder;
                if (actor instanceof ClassActor) {
                    holder = (ClassActor) actor;
                } else {
                    holder = ((MemberActor) actor).holder();
                }
                if (holder.majorVersion() < 49) {
                    _attributes.append(new Attribute("Synthetic"));
                } else {
                    // The ACC_SYNTHETIC flag was introduced in version 49 of the class file format
                    // and so it is used in preference to the Synthetic attribute. The former is far
                    // more compact and easier to process.
                }
            }
            if (_actor.isDeprecated()) {
                _attributes.append(new Attribute("Deprecated"));
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

        protected final InterfaceActor[] _interfaceActors;
        protected final MethodInfo[] _methods;
        protected final FieldInfo[] _fields;

        /**
         * Creates a representation of a class that can be written as a class file.
         */
        public ClassInfo(ClassActor classActor) {
            super(classActor);
            _interfaceActors = classActor.localInterfaceActors();
            _methods = Arrays.map(Arrays.join(MethodActor.class, classActor.localInterfaceMethodActors(), classActor.localVirtualMethodActors(), classActor.localStaticMethodActors()), MethodInfo.class, new MapFunction<MethodActor, MethodInfo>() {
                @Override
                public MethodInfo map(MethodActor methodActor) {
                    return new MethodInfo(methodActor);
                }
            });
            _fields = Arrays.map(Arrays.join(FieldActor.class, classActor.localInstanceFieldActors(), classActor.localStaticFieldActors()), FieldInfo.class, new MapFunction<FieldActor, FieldInfo>() {
                @Override
                public FieldInfo map(FieldActor fieldActor) {
                    return new FieldInfo(fieldActor);
                }
            });
            final TypeDescriptor outerClass = classActor.outerClassDescriptor();
            final TypeDescriptor[] innerClasses = classActor.innerClassDescriptors();
            final EnclosingMethodInfo enclosingMethod = classActor.enclosingMethodInfo();
            final String sourceFileName = classActor.sourceFileName();

            if (sourceFileName != null) {
                class SourceFile extends Attribute {
                    SourceFile() {
                        super("SourceFile");
                    }
                }

                _attributes.append(new Attribute("SourceFile") {
                    @Override
                    protected void writeData(ClassfileWriter cf) throws IOException {
                        cf.writeUnsigned2(cf.indexOfUtf8(sourceFileName));
                    }
                });
            }
            if (enclosingMethod != null) {
                _attributes.append(new Attribute("EnclosingMethod") {
                    @Override
                    protected void writeData(ClassfileWriter cf) throws IOException {
                        cf.writeUnsigned2(cf.indexOfClass(enclosingMethod.holder()));
                        if (enclosingMethod.name() != null) {
                            final Utf8Constant name = makeUtf8Constant(enclosingMethod.name());
                            final Utf8Constant descriptor = makeUtf8Constant(enclosingMethod.descriptor());
                            final NameAndTypeConstant nameAndType = createNameAndTypeConstant(name, descriptor);
                            cf.writeUnsigned2(cf.indexOf(nameAndType));
                        } else {
                            cf.writeUnsigned2(0);
                        }
                    }
                });
            }

            if (outerClass != null || innerClasses != null) {
                _attributes.append(new Attribute("InnerClasses") {
                    @Override
                    protected void writeData(ClassfileWriter cf) throws IOException {
                        cf.writeUnsigned2((outerClass == null ? 0 : 1) + (innerClasses == null ? 0 : innerClasses.length));
                        if (outerClass != null) {
                            cf.writeUnsigned2(cf.indexOfClass(_actor.typeDescriptor()));
                            cf.writeUnsigned2(cf.indexOfClass(outerClass));
                            cf.writeUnsigned2(cf.innerClassNameIndex(_actor.typeDescriptor()));
                            cf.writeUnsigned2(_actor.flags() & Actor.JAVA_CLASS_FLAGS);
                        }
                        if (innerClasses != null) {
                            final int outerClassIndex = cf.indexOfClass(_actor.typeDescriptor());
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
        }

        @Override
        protected void write(ClassfileWriter cf) throws IOException {
            final String className = _actor.name().string();
            final TypeDescriptor classDescriptor = JavaTypeDescriptor.getDescriptorForWellFormedTupleName(className);
            final int thisClassIndex = cf.indexOf(createClassConstant(classDescriptor));
            final ClassActor superClassActor = _actor.superClassActor();
            final int superClassIndex = superClassActor == null ? 0 : cf.indexOf(createClassConstant(superClassActor.typeDescriptor()));
            cf.writeUnsigned4(0xcafebabe);
            cf.writeUnsigned2(_actor.minorVersion());
            cf.writeUnsigned2(_actor.majorVersion());
            cf._constantPoolEditor.write(cf._dataOutputStream);

            cf.writeUnsigned2(_actor.flags() & Actor.JAVA_CLASS_FLAGS);
            cf.writeUnsigned2(thisClassIndex);
            cf.writeUnsigned2(superClassIndex);

            cf.writeUnsigned2(_interfaceActors.length);
            for (InterfaceActor interfaceActor : _interfaceActors) {
                cf.writeUnsigned2(cf.indexOfClass(interfaceActor));
            }

            cf.writeUnsigned2(_fields.length);
            for (FieldInfo field : _fields) {
                field.write(cf);
            }

            cf.writeUnsigned2(_methods.length);
            for (MethodInfo method : _methods) {
                method.write(cf);
            }

            cf.writeAttributes(_attributes);
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
            cf.writeUnsigned2(cf.indexOfUtf8(_actor.name()));
            cf.writeUnsigned2(cf.indexOfUtf8(_actor.descriptor()));
            cf.writeAttributes(_attributes);
        }
    }

    /**
     * Represents the class file info for methods.
     */
    public static class MethodInfo extends MemberInfo<MethodActor> {

        MethodInfo(MethodActor methodActor) {
            super(methodActor);
            final CodeAttribute codeAttribute = methodActor instanceof ClassMethodActor ? ((ClassMethodActor) methodActor).codeAttribute() : null;
            final byte[] runtimeVisibleParameterAnnotationsBytes = methodActor.runtimeVisibleParameterAnnotationsBytes();
            final byte[] annotationDefaultBytes = methodActor.annotationDefaultBytes();
            final TypeDescriptor[] checkedExceptions = methodActor.checkedExceptions();
            if (runtimeVisibleParameterAnnotationsBytes != null) {
                _attributes.append(new BytesAttribute("RuntimeVisibleParameterAnnotations", runtimeVisibleParameterAnnotationsBytes));
            }
            if (annotationDefaultBytes != null) {
                _attributes.append(new BytesAttribute("AnnotationDefault", annotationDefaultBytes));
            }
            if (checkedExceptions.length != 0) {
                _attributes.append(new Attribute("Exceptions") {
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
                _attributes.append(new Code(codeAttribute));
            }
        }

        @Override
        protected int classfileFlags() {
            return _actor.flags() & Actor.JAVA_METHOD_FLAGS;
        }
    }

    public static class Code extends Attribute {
        protected final AppendableSequence<Attribute> _attributes = new ArrayListSequence<Attribute>();
        protected final CodeAttribute _codeAttribute;

        public Code(CodeAttribute codeAttribute) {
            super("Code");
            _codeAttribute = codeAttribute;

            final StackMapTable stackMapTable = codeAttribute.stackMapTable();
            final LineNumberTable lineNumberTable = codeAttribute.lineNumberTable();
            final LocalVariableTable localVariableTable = codeAttribute.localVariableTable();

            if (!lineNumberTable.isEmpty()) {
                _attributes.append(new Attribute("LineNumberTable") {
                    @Override
                    protected void writeData(ClassfileWriter cf) throws IOException {
                        lineNumberTable.writeAttributeInfo(cf._dataOutputStream, cf._constantPoolEditor);
                    }
                });
            }
            if (!localVariableTable.isEmpty()) {
                _attributes.append(new Attribute("LocalVariableTable") {
                    @Override
                    protected void writeData(ClassfileWriter cf) throws IOException {
                        localVariableTable.writeLocalVariableTableAttributeInfo(cf._dataOutputStream, cf._constantPoolEditor);
                    }
                });
            }
            if (localVariableTable.numberOfEntriesWithSignature() != 0) {
                _attributes.append(new Attribute("LocalVariableTypeTable") {
                    @Override
                    protected void writeData(ClassfileWriter cf) throws IOException {
                        localVariableTable.writeLocalVariableTypeTableAttributeInfo(cf._dataOutputStream, cf._constantPoolEditor);
                    }
                });
            }
            if (stackMapTable != null) {
                _attributes.append(new Attribute("StackMapTable") {
                    @Override
                    protected void writeData(ClassfileWriter cf) throws IOException {
                        stackMapTable.writeAttributeInfo(cf._dataOutputStream, cf._constantPoolEditor);
                    }
                });
            }
        }

        @Override
        protected void writeData(ClassfileWriter cf) throws IOException {
            final Sequence<ExceptionHandlerEntry> exceptionHandlerTable = _codeAttribute.exceptionHandlerTable();
            cf.writeUnsigned2(_codeAttribute.maxStack());
            cf.writeUnsigned2(_codeAttribute.maxLocals());
            cf.writeUnsigned4(_codeAttribute.code().length);
            cf.writeUnsigned1Array(_codeAttribute.code());
            cf.writeUnsigned2(exceptionHandlerTable.length());
            for (ExceptionHandlerEntry info : exceptionHandlerTable) {
                cf.writeUnsigned2(info.startPosition()); // start_pc
                cf.writeUnsigned2(info.endPosition()); // end_pc
                cf.writeUnsigned2(info.handlerPosition()); // handler_pc
                cf.writeUnsigned2(info.catchTypeIndex()); // catch_type
            }
            cf.writeAttributes(_attributes);
        }
    }

    /**
     * Represents the class file info for fields.
     */
    public static class FieldInfo extends MemberInfo<FieldActor> {
        protected FieldInfo(FieldActor fieldActor) {
            super(fieldActor);
            final Value constantValue = fieldActor.constantValue();

            if (constantValue != null) {
                _attributes.append(new Attribute("ConstantValue") {
                    @Override
                    protected void writeData(ClassfileWriter cf) throws IOException {
                        switch (_actor.kind().asEnum()) {
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
                                throw classFormatError("Cannot have ConstantValue for fields of type " + _actor.kind());
                            }
                        }
                    }
                });
            }
        }

        @Override
        protected int classfileFlags() {
            return _actor.flags() & Actor.JAVA_FIELD_FLAGS;
        }
    }

    /**
     * A command line interface for producing Maxine preprocessed class files for one or more classes.
     */
    @PROTOTYPE_ONLY
    public static void main(String[] args) {
        final OptionSet options = new OptionSet();
        final Option<File> outputDirectoryOption = options.newFileOption("d", new File("generated").getAbsoluteFile(),
            "Specifies where to place generated class files");
        final Option<Boolean> javapOption = options.newBooleanOption("javap", false,
            "Runs javap on the generated class file(s).");
        Trace.addTo(options);
        options.parseArguments(args);
        final String[] arguments = options.getArguments();
        if (arguments.length == 0) {
            System.out.println("Usage: " + ClassfileWriter.class.getName() + " [-options] <classes>...");
            System.out.println();
            System.out.println("where options include:");
            options.printHelp(System.out, 80);
            return;
        }
        new PrototypeGenerator().createJavaPrototype(options.getArgumentsAndUnrecognizedOptions(), VMConfigurations.createStandard(BuildLevel.PRODUCT, Platform.host()), false);
        ClassActor.prohibitPackagePrefix(null); // allow extra classes

        final Map<String, byte[]> classNameToClassfileMap = new LinkedHashMap<String, byte[]>();
        for (String className : arguments) {
            processClass(className, outputDirectoryOption.getValue(), classNameToClassfileMap);
        }

        Trace.line(1, "Generated " + classNameToClassfileMap.size() + " class file" + (classNameToClassfileMap.size() == 1 ? "" : "s") + " to " + outputDirectoryOption.getValue());

        testLoadGeneratedClasses(classNameToClassfileMap, outputDirectoryOption.getValue());

        if (javapOption.getValue()) {
            for (String className : classNameToClassfileMap.keySet()) {
                final String[] javapArgs = {"-verbose", "-private", "-classpath", outputDirectoryOption.getValue().getPath(), className};
                ToolChain.javap(javapArgs);
            }
        }
    }

    @PROTOTYPE_ONLY
    private static void processClass(String className, File outputDirectory, Map<String, byte[]> classNameToClassfileMap) {
        Class<?> javaClass;
        try {
            javaClass = Class.forName(className);
        } catch (ClassNotFoundException e) {
            ProgramWarning.message("Could not find class: " + className);
            return;
        }
        if (MaxineVM.isPrototypeOnly(javaClass)) {
            ProgramWarning.message("Cannot create a class actor for prototype only class: " + className);
            return;
        }
        final ClassActor classActor = ClassActor.fromJava(javaClass);
        final byte[] classfileBytes = toByteArray(new ClassInfo(classActor));

        final File classfileFile = new File(outputDirectory, classActor.name().string().replace(".", File.separator) + ".class").getAbsoluteFile();
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

    @PROTOTYPE_ONLY
    static class TestClassLoader extends ClassLoader {
        public void load(String className, byte[] classfileBytes) {
            ClassfileReader.defineClassActor(className, this, classfileBytes, null, null);
        }
    }

    @PROTOTYPE_ONLY
    private static void testLoadGeneratedClasses(Map<String, byte[]> classNameToClassfileMap, File outputDirectory) {
        try {
            final URL[] urls = {outputDirectory.toURI().toURL()};
            final ClassLoader urlClassLoader = URLClassLoader.newInstance(urls);
            final TestClassLoader testClassLoader = new TestClassLoader();
            ClassRegistry._classLoaderToRegistryMap.put(testClassLoader, null);
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
