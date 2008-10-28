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
package com.sun.max.asm.gen;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.Arrays;

import com.sun.max.*;
import com.sun.max.asm.*;
import com.sun.max.asm.dis.*;
import com.sun.max.collect.*;
import com.sun.max.ide.*;
import com.sun.max.io.*;
import com.sun.max.lang.*;
import com.sun.max.program.*;
import com.sun.max.program.option.*;

/**
 * Source code generator for raw and label assembler methods derived from an ISA specification.
 *
 * @author Bernd Mathiske
 * @author Doug Simon
 */
public abstract class AssemblerGenerator<Template_Type extends Template> {

    protected OptionSet _options = new OptionSet();

    private final Option<File> _outputDirectory = _options.newFileOption("d", JavaProject.findSourceDirectory(),
            "Source directory of the class(es) containing the for generated assembler methods.");
    private final Option<String> _assemblerInterfaceName = _options.newStringOption("i", null,
            "Interface used to constrain which assembler methods will be generated. " +
            "If absent, an assembler method is generated for each template in the specification.");
    private final Option<String> _rawAssemblerClassName = _options.newStringOption("r", null,
            "Class containing the generated raw assembler methods.");
    private final Option<String> _labelAssemblerClassName = _options.newStringOption("l", null,
            "Class containing the generated label assembler methods.");

    private final Assembly<Template_Type> _assembly;
    private final boolean _sortAssemblerMethods;
    private Sequence<Template_Type> _templates;
    private Sequence<Template_Type> _labelTemplates;

    protected AssemblerGenerator(Assembly<Template_Type> assembly, boolean sortAssemblerMethods) {
        Trace.addTo(_options);
        _assembly = assembly;
        final String isa = assembly.instructionSet().name();
        final String defaultOutputPackage = MaxPackage.fromClass(Assembler.class).subPackage(isa.toLowerCase()).name() + ".complete";
        _rawAssemblerClassName.setDefaultValue(defaultOutputPackage + "." + isa + "RawAssembler");
        _labelAssemblerClassName.setDefaultValue(defaultOutputPackage + "." + isa + "LabelAssembler");
        _sortAssemblerMethods = sortAssemblerMethods;
    }

    public Assembly<Template_Type> assembly() {
        return _assembly;
    }

    static class MethodKey {
        final String _name;
        final Class[] _parameterTypes;

        MethodKey(Method method) {
            _name = method.getName();
            _parameterTypes = method.getParameterTypes();
        }

        MethodKey(Template template, boolean asLabelTemplate) {
            _name = template.assemblerMethodName();
            _parameterTypes = template.parameterTypes();
            if (asLabelTemplate) {
                final int labelParameterIndex = template.labelParameterIndex();
                assert labelParameterIndex != -1;
                _parameterTypes[labelParameterIndex] = Label.class;
            }
        }

        @Override
        public boolean equals(Object object) {
            if (object instanceof MethodKey) {
                final MethodKey other = (MethodKey) object;
                return other._name.equals(_name) && Arrays.equals(_parameterTypes, other._parameterTypes);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return _name.hashCode() ^ _parameterTypes.length;
        }

        @Override
        public String toString() {
            final String parameterTypes = Arrays.toString(_parameterTypes);
            return _name + "(" + parameterTypes.substring(1, parameterTypes.length() - 1) + ")";
        }


    }

    /**
     * Initializes the set of label and raw templates that will be generated as assembler methods.
     * This includes doing any filtering out of templates based on an {@linkplain #_assemblerInterfaceName assembler interface}.
     */
    private void initTemplates() {
        assert (_labelTemplates == null) == (_templates == null);
        if (_templates == null) {
            final String assemblerInterfaceName = _assemblerInterfaceName.getValue();
            if (assemblerInterfaceName == null) {
                _templates = assembly().templates();
                _labelTemplates = assembly().labelTemplates();
            } else {
                final AppendableSequence<Template_Type> templates = new ArrayListSequence<Template_Type>();
                final AppendableSequence<Template_Type> labelTemplates = new ArrayListSequence<Template_Type>();

                Class assemberInterface = null;
                try {
                    assemberInterface = Class.forName(assemblerInterfaceName);
                    ProgramError.check(assemberInterface.isInterface(), "The class " + assemblerInterfaceName + " is not an interface");
                } catch (ClassNotFoundException e) {
                    ProgramError.unexpected("The assembler interface class " + assemblerInterfaceName + " must be on the class path");
                }
                final Set<MethodKey> assemblerInterfaceMethods = new HashSet<MethodKey>();
                for (Method assemblerInterfaceMethod : assemberInterface.getDeclaredMethods()) {
                    assemblerInterfaceMethods.add(new MethodKey(assemblerInterfaceMethod));
                }

                for (Template_Type labelTemplate : assembly().labelTemplates()) {
                    if (assemblerInterfaceMethods.contains(new MethodKey(labelTemplate, true))) {
                        assert labelTemplate.labelParameterIndex() != -1;
                        labelTemplates.append(labelTemplate);
                    }
                }

                for (Template_Type template : assembly().templates()) {
                    if (template.labelParameterIndex() != -1 && assemblerInterfaceMethods.contains(new MethodKey(template, true))) {
                        templates.append(template);
                    } else if (assemblerInterfaceMethods.contains(new MethodKey(template, false))) {
                        templates.append(template);
                    }
                }

                _templates = templates;
                _labelTemplates = labelTemplates;

                Trace.line(1, "Based on " + assemberInterface + ", " + (assembly().templates().length() - templates.length()) + " (of " + assembly().templates().length() + ") raw templates and " +
                              (assembly().labelTemplates().length() - labelTemplates.length()) + " (of " + assembly().labelTemplates().length() + ") label templates will be omitted from generated assembler methods");
            }

            if (_sortAssemblerMethods) {
                _templates = Sequence.Static.sort(_templates, assembly().templateType());
                _labelTemplates = Sequence.Static.sort(_labelTemplates, assembly().templateType());
            }
        }
    }

    protected final Sequence<Template_Type> templates() {
        initTemplates();
        return _templates;
    }

    protected final Sequence<Template_Type> labelTemplates() {
        initTemplates();
        return _labelTemplates;
    }

    /**
     * Gets the absolute path to the source file that will updated to include the generated assembler methods.
     *
     * @param className
     *                the name of the Java class that contains the generated assembler methods
     */
    private File getSourceFileFor(String className) {
        return new File(_outputDirectory.getValue(), className.replace('.', File.separatorChar) + ".java").getAbsoluteFile();
    }

    protected final String formatParameterList(String separator, Sequence<? extends Parameter> parameters, boolean typesOnly) {
        String sep = separator;
        final StringBuilder sb = new StringBuilder();
        for (Parameter parameter : parameters) {
            sb.append(sep);
            sb.append(Classes.getSimpleName(parameter.type(), true));
            if (!typesOnly) {
                sb.append(" ");
                sb.append(parameter.variableName());
            }
            if (!sep.startsWith(", ")) {
                sep = ", " + sep;
            }
        }
        return sb.toString();
    }

    /**
     * Prints the source code for the raw assembler method for to a given template.
     *
     * @return the number of source code lines printed
     */
    protected abstract int printMethod(IndentWriter writer, Template_Type template);

    /**
     * Prints the source code for support methods that are used by the methods printed by {@link #printMethod(IndentWriter, Template)}.
     *
     * @return the number of subroutines printed
     */
    protected int printSubroutines(IndentWriter writer) {
        return 0;
    }

    /**
     * Gets the set of packages that must be imported for the generated code to compile successfully.
     *
     * @param className
     *                the name of the Java class that contains the assembler methods generated from {@code templates}
     * @param templates
     *                the list of templates for which code is being generated
     * @return a set of packages sorted by name
     */
    public Set<MaxPackage> getImportPackages(String className, Iterable<Template_Type> templates) {
        final int indexOfLastPeriod = className.lastIndexOf('.');
        final String outputPackageName = indexOfLastPeriod == -1 ? "" : className.substring(0, indexOfLastPeriod);
        final MaxPackage outputPackage = MaxPackage.fromName(outputPackageName);
        final Set<MaxPackage> packages = new TreeSet<MaxPackage>();
        packages.add(MaxPackage.fromClass(AssemblyException.class));
        packages.add(MaxPackage.fromClass(Label.class));
        for (Template_Type template : templates) {
            for (Parameter parameter : template.parameters()) {
                final Class type = parameter.type();
                if (!type.isPrimitive()) {
                    final MaxPackage p = MaxPackage.fromClass(type);
                    if (!p.equals(outputPackage)) {
                        packages.add(p);
                    }
                }
            }
        }
        return packages;
    }

    /**
     * Prints the Javadoc comment for a template followed by a C++ style comment stating the template's number (it's
     * position in the order of emitted assembler methods) and its serial (a unique identifier given to every template).
     */
    protected void printMethodComment(IndentWriter writer, Template_Type template, int number, boolean forLabelAssemblerMethod) {
        printMethodJavadoc(writer, template, forLabelAssemblerMethod);
        writer.println("// Template#: " + number + ", Serial#: " + template.serial());
    }

    /**
     * Determines if a given label template should be omitted from assembler method generation.
     * This method is overridden by subclasses that may generate the code for 2 related label templates
     * in a single assembler method. For example, on X86 most branch instructions can take offsets of variable bit widths
     * and the logic for decoding the bit width of a {@link Label} value may be generated in a single assembler method.
     * <p>
     * The default implementation of this method returns {@code false}.
     */
    protected boolean omitLabelTemplate(Template_Type labelTemplate) {
        return false;
    }

    /**
     * Gets a reference to the architecture manual section describing the given template. The
     * returned string should conform to the format of the {@code @see} Javadoc tag.
     */
    protected String getJavadocManualReference(Template_Type template) {
        return null;
    }

    /**
     * Allows subclasses to print ISA specific details for a template. For example, RISC synthetic instructions
     * print what raw instruction they are derived from.
     *
     * @param extraLinks
     *                a sequence to which extra javadoc links should be appended
     */
    protected void printExtraMethodJavadoc(IndentWriter writer, Template_Type template, AppendableSequence<String> extraLinks, boolean forLabelAssemblerMethod) {
    }

    /**
     * Writes the Javadoc comment for an assembler method.
     *
     * @param template the template from which the assembler method is generated
     */
    protected void printMethodJavadoc(IndentWriter writer, Template_Type template, boolean forLabelAssemblerMethod) {
        final AppendableSequence<String> extraLinks = new LinkSequence<String>();
        final Sequence<? extends Parameter> parameters = getParameters(template, forLabelAssemblerMethod);
        writer.println("/**");
        writer.println(" * Pseudo-external assembler syntax: {@code " + template.externalName() + externalMnemonicSuffixes(parameters) + "  }" + externalParameters(parameters));

        final boolean printExampleInstruction = true;
        if (printExampleInstruction) {
            if (template.serial() == 264) {
                System.console();
            }
            final AppendableIndexedSequence<Argument> arguments = new ArrayListSequence<Argument>();
            final AddressMapper addressMapper = new AddressMapper();
            int parameterIndex = 0;
            for (Parameter p : template.parameters()) {
                final Argument exampleArg = p.getExampleArgument();
                if (exampleArg != null) {
                    arguments.append(exampleArg);
                } else {
                    break;
                }
                if (template.labelParameterIndex() == parameterIndex) {
                    addressMapper.add((ImmediateArgument) exampleArg, "L1");
                }
                parameterIndex++;
            }
            if (arguments.length() == template.parameters().length()) {
                final String exampleInstruction = generateExampleInstruction(template, arguments, addressMapper);
                writer.println(" * Example disassembly syntax: {@code " + exampleInstruction + "}");
            }
        }

        printExtraMethodJavadoc(writer, template, extraLinks, forLabelAssemblerMethod);
        final Sequence<InstructionConstraint> constraints = Sequence.Static.filter(template.instructionDescription().specifications(), InstructionConstraint.class);
        if (!constraints.isEmpty()) {
            writer.println(" * <p>");
            for (InstructionConstraint constraint : constraints) {
                final Method predicateMethod = constraint.predicateMethod();
                if (predicateMethod != null) {
                    extraLinks.append(predicateMethod.getDeclaringClass().getName() + "#" + predicateMethod.getName());
                }
                writer.println(" * Constraint: {@code " + constraint.asJavaExpression() + "}<br />");
            }
        }

        if (!extraLinks.isEmpty()) {
            writer.println(" *");
            for (String link : extraLinks) {
                writer.println(" * @see " + link);
            }
        }

        final String ref = getJavadocManualReference(template);
        if (ref != null) {
            writer.println(" *");
            writer.println(" * @see " + ref);
        }
        writer.println(" */");
    }

    protected abstract String generateExampleInstruction(Template_Type template, IndexedSequence<Argument> arguments, AddressMapper addressMapper);

    private String externalParameters(Sequence< ? extends Parameter> parameters) {
        final StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (Parameter parameter : parameters) {
            if (!ExternalMnemonicSuffixArgument.class.isAssignableFrom(parameter.type())) {
                if (!first) {
                    sb.append(", ");
                }
                sb.append("<i>").append(parameter.variableName()).append("</i>");
                first = false;
            }
        }
        return sb.toString();
    }

    private String externalMnemonicSuffixes(Sequence< ? extends Parameter> parameters) {
        final StringBuilder sb = new StringBuilder();
        for (Parameter parameter : parameters) {
            if (ExternalMnemonicSuffixArgument.class.isAssignableFrom(parameter.type())) {
                boolean first = true;
                String close = "]";
                for (Argument argument : parameter.getLegalTestArguments()) {
                    final String externalValue = argument.externalValue();
                    if (externalValue.length() != 0) {
                        if (!first) {
                            sb.append("|");
                        } else {
                            if (((ExternalMnemonicSuffixArgument) argument).isOptional()) {
                                sb.append("{");
                                close = "}";
                            } else {
                                sb.append("[");
                            }
                        }
                        sb.append(externalValue);
                        first = false;
                    }
                }
                sb.append(close);
            }
        }
        return sb.toString();
    }

    private boolean generateRawAssemblerMethods(String rawAssemblerClassName) throws IOException {
        Trace.line(1, "Generating raw assembler methods");
        final Sequence<Template_Type> templates = templates();
        final File sourceFile = getSourceFileFor(rawAssemblerClassName);
        ProgramError.check(sourceFile.exists(), "Source file for class containing raw assembler methods does not exist: " + sourceFile);
        final CharArraySource charArrayWriter = new CharArraySource((int) sourceFile.length());
        final IndentWriter writer = new IndentWriter(new PrintWriter(charArrayWriter));
        writer.indent();

        int codeLineCount = 0;
        final Map<InstructionDescription, Integer> instructionDescriptions = new HashMap<InstructionDescription, Integer>();
        int maxTemplatesPerDescription = 0;
        int i = 0;
        for (Template_Type template : templates) {
            printMethodComment(writer, template, i + 1, false);
            codeLineCount += printMethod(writer, template);
            writer.println();

            Integer count = instructionDescriptions.get(template.instructionDescription());
            if (count == null) {
                count = 1;
            } else {
                count = count + 1;
            }
            if (count > maxTemplatesPerDescription) {
                maxTemplatesPerDescription = count;
            }
            instructionDescriptions.put(template.instructionDescription(), count);
            i++;
        }
        final int subroutineCount = printSubroutines(writer);

        writer.outdent();
        writer.close();

        Trace.line(1, "Generated raw assembler methods" +
                        " [code line count=" + codeLineCount + ", total line count=" + writer.lineCount() +
                        ", method count=" + (templates.length() + subroutineCount) +
                        ", instruction templates=" + templates.length() + ", max templates per description=" + maxTemplatesPerDescription +
                        "]");

        return Files.updateGeneratedContent(sourceFile, charArrayWriter, "// START GENERATED RAW ASSEMBLER METHODS", "// END GENERATED RAW ASSEMBLER METHODS");
    }

    /**
     * Gets the parameters for a template.
     *
     * @param forLabelAssemblerMethod
     *                if true and template contains a label parameter, then this parameter is represented as a
     *                {@link LabelParameter} object in the returned sequence
     */
    protected static Sequence<Parameter> getParameters(Template template, boolean forLabelAssemblerMethod) {
        if (!forLabelAssemblerMethod || template.labelParameterIndex() == -1) {
            final Class<IndexedSequence<Parameter>> type = null;
            return StaticLoophole.cast(type, template.parameters());
        }
        final MutableSequence<Parameter> parameters = new ArrayListSequence<Parameter>(template.parameters());
        parameters.set(template.labelParameterIndex(), LabelParameter.LABEL);
        return parameters;
    }

    protected void printLabelMethodHead(IndentWriter writer, Template_Type template, Sequence<Parameter> parameters) {
        writer.print("public void " + template.assemblerMethodName() + "(");
        writer.print(formatParameterList("final ", parameters, false));
        writer.println(") {");
        writer.indent();
    }

    /**
     * Prints an assembler method for a template that refers to an address via a {@linkplain Label label}.
     *
     * @param writer
     *                the writer to which code will be printed
     * @param labelTemplate
     *                a template that has a label parameter (i.e. its {@linkplain Template#labelParameterIndex() label parameter index} is not -1)
     * @param assemblerClassName
     *                the name of the class enclosing the assembler method declaration
     */
    protected abstract void printLabelMethod(IndentWriter writer, Template_Type labelTemplate, String assemblerClassName);

    /**
     * Mechanism that writes the body of the {@link MutableAssembledObject#assemble} method in a generated label method helper class.
     */
    public class InstructionWithLabelSubclass {

        final Class<? extends InstructionWithLabel> _superClass;
        final String _name;
        final String _extraConstructorArguments;
        final String _labelArgumentPrefix;

        public InstructionWithLabelSubclass(Template template, Class<? extends InstructionWithLabel> superClass, String extraConstructorArguments) {
            _superClass = superClass;
            _name = template.assemblerMethodName() + "_" + template.serial();
            _extraConstructorArguments = extraConstructorArguments;
            final String labelType;
            if (superClass == InstructionWithAddress.class) {
                labelType = "address";
            } else if (superClass == InstructionWithOffset.class) {
                labelType = "offset";
            } else {
                throw ProgramError.unexpected("Unknown instruction with label type: " + superClass);
            }
            _labelArgumentPrefix = labelType + "As";
        }

        /**
         * Prints the body of the {@link MutableAssembledObject#assemble} method in a label method helper class being
         * generated by a call to {@link AssemblerGenerator#printLabelMethodHelper}.
         * <p>
         * The default implementation generates a call to the raw assembler method generated for {@code template}
         *
         * @param writer
         * @param template
         */
        protected void printAssembleMethodBody(IndentWriter writer, Template template) {
            writer.print(template.assemblerMethodName() + "(");
            final IndexedSequence<? extends Parameter> parameters = template.parameters();
            String separator = "";
            int index = 0;
            final int labelParameterIndex = template.labelParameterIndex();
            final String labelArgument = _labelArgumentPrefix + Strings.firstCharToUpperCase(parameters.get(labelParameterIndex).type().getName()) + "()";
            for (Parameter parameter : parameters) {
                writer.print(separator);
                if (index == labelParameterIndex) {
                    writer.print(labelArgument);
                } else {
                    writer.print("_" + parameter.variableName());
                }
                separator = ", ";
                index++;
            }
            writer.println(");");
        }

        @Override
        public String toString() {
            return _name;
        }
    }

    /**
     * Prints the code that emits the place holder bytes for a label instruction before a value has been bound to the label.
     *
     * @param writer
     * @param template
     * @param placeholderInstructionSize
     *                the number of place holder bytes written to the instruction stream before the label's value has
     *                been determined. If this value is -1, then the size depends on the arguments to the method and
     *                so a call to the raw assembler method is made to determine the size.
     * @return an expression denoting the number of place holder bytes emitted
     */
    private String printPlaceholderBytes(IndentWriter writer, Template_Type template, int placeholderInstructionSize) {
        if (placeholderInstructionSize == -1) {
            writer.println("final " + template.parameters().get(template.labelParameterIndex()).type() + " placeHolder = 0;");
            writer.print(template.assemblerMethodName() + "(");
            String separator = "";
            for (int i = 0; i < template.parameters().length(); i++) {
                writer.print(separator);
                if (i == template.labelParameterIndex()) {
                    writer.print("placeHolder");
                } else {
                    writer.print(template.parameters().get(i).variableName());
                }
                separator = ", ";
            }
            writer.println(");");
            return "currentPosition() - startPosition";
        }

        if (placeholderInstructionSize == 2) {
            writer.println("emitShort(0);");
        } else if (placeholderInstructionSize == 4) {
            writer.println("emitInt(0);");
        } else if (placeholderInstructionSize == 8) {
            writer.println("emitLong(0);");
        } else {
            writer.println("emitZeroes(" + placeholderInstructionSize + ");");
        }
        return String.valueOf(placeholderInstructionSize);
    }

    /**
     * Handles most of the work of {@link #printLabelMethod(IndentWriter, Template, String)}.
     *
     * @param writer
     *                the writer to which code will be printed
     * @param template
     *                a template that has a label parameter (i.e. its
     *                {@linkplain Template#labelParameterIndex() label parameter index} is not -1)
     * @param parameters
     *                the parameters of the template with the label parameter represented as a {@link LabelParameter}
     *                object
     * @param placeholderInstructionSize
     *                the number of place holder bytes written to the instruction stream before the label's value has
     *                been determined. If this value is -1, then the size depends on the arguments to the method and
     *                so a call to the raw assembler method is made to determine the size.
     * @param assemblerClassName
     *                the name of the class in which the assembler methods will be declared
     * @param labelInstructionSubclassGenerator
     *                the object that writes the body of the {@link MutableAssembledObject#assemble} method in a generated label method helper class
     */
    protected final void printLabelMethodHelper(IndentWriter writer,
                    Template_Type template,
                    Sequence<Parameter> parameters,
                    int placeholderInstructionSize,
                    String assemblerClassName,
                    InstructionWithLabelSubclass labelInstructionSubclassGenerator) {
        assert template.labelParameterIndex() != -1;
        printLabelMethodHead(writer, template, parameters);
        writer.println("final int startPosition = currentPosition();");
        final String size = printPlaceholderBytes(writer, template, placeholderInstructionSize);
        writer.print("new " + labelInstructionSubclassGenerator._name + "(startPosition, " + size + ", ");
        for (Parameter parameter : parameters) {
            if (!(parameter instanceof LabelParameter)) {
                writer.print(parameter.variableName() + ", ");
            }
        }
        writer.println("label);");
        writer.outdent();
        writer.println("}");
        writer.println();

        final StringWriter stringWriter = new StringWriter();
        final IndentWriter indentWriter = new IndentWriter(new PrintWriter(stringWriter));
        indentWriter.indent();
        printLabelMethodHelperClass(
                        indentWriter,
                        template,
                        parameters,
                        assemblerClassName,
                        labelInstructionSubclassGenerator);
        _labelMethodHelperClasses.append(stringWriter.toString());
    }

    private final AppendableSequence<String> _labelMethodHelperClasses = new ArrayListSequence<String>();

    private void printLabelMethodHelperClass(
                    IndentWriter writer,
                    Template_Type template,
                    Sequence<Parameter> parameters,
                    String assemblerClassName,
                    InstructionWithLabelSubclass labelInstructionSubclass) {
        final String simpleAssemblerClassName = assemblerClassName.substring(assemblerClassName.lastIndexOf('.') + 1);
        writer.println("class " + labelInstructionSubclass + " extends " + labelInstructionSubclass._superClass.getSimpleName() + " {");
        writer.indent();
        String parametersDecl = "";
        for (Parameter parameter : parameters) {
            if (!(parameter instanceof LabelParameter)) {
                final Class parameterType = parameter.type();
                final String typeName = Classes.getSimpleName(parameterType, true);
                final String variableName = parameter.variableName();
                writer.println("private final " + typeName + " _" + variableName + ";");
                parametersDecl = parametersDecl + typeName + " " + variableName + ", ";
            }
        }

        writer.println(labelInstructionSubclass + "(int startPosition, int endPosition, " + parametersDecl + "Label label) {");
        writer.indent();
        writer.println("super(" + simpleAssemblerClassName + ".this, startPosition, currentPosition(), label" + labelInstructionSubclass._extraConstructorArguments + ");");
        for (Parameter parameter : parameters) {
            if (!(parameter instanceof LabelParameter)) {
                final String variableName = parameter.variableName();
                writer.println("_" + variableName + " = " + variableName + ";");
            }
        }
        writer.outdent();
        writer.println("}");
        writer.println("@Override");
        writer.println("protected void assemble() throws AssemblyException {");
        writer.indent();
        labelInstructionSubclass.printAssembleMethodBody(writer, template);
        writer.outdent();
        writer.println("}");
        writer.outdent();
        writer.println("}");
        writer.println();
    }

    private boolean generateLabelAssemblerMethods(String labelAssemblerClassName) throws IOException {
        Trace.line(1, "Generating label assembler methods");
        final Sequence<Template_Type> labelTemplates = labelTemplates();
        final File sourceFile = getSourceFileFor(labelAssemblerClassName);
        ProgramError.check(sourceFile.exists(), "Source file for class containing label assembler methods does not exist: " + sourceFile);
        final CharArraySource charArrayWriter = new CharArraySource((int) sourceFile.length());
        final IndentWriter writer = new IndentWriter(new PrintWriter(charArrayWriter));
        writer.indent();

        int codeLineCount = 0;
        int i = 0;
        for (Template_Type labelTemplate : labelTemplates) {
            if (!omitLabelTemplate(labelTemplate)) {
                printMethodComment(writer, labelTemplate, i + 1, true);
                final int startLineCount = writer.lineCount();
                printLabelMethod(writer, labelTemplate, labelAssemblerClassName);
                codeLineCount += writer.lineCount() - startLineCount;
                i++;
            }
        }
        writer.outdent();

        for (String labelMethodHelperClass : _labelMethodHelperClasses) {
            writer.print(labelMethodHelperClass);
        }

        writer.close();

        Trace.line(1, "Generated label assembler methods" +
                      " [code line count=" + codeLineCount +
                      ", total line count=" + writer.lineCount() +
                      ", method count=" + templates().length() + ")");

        return Files.updateGeneratedContent(sourceFile, charArrayWriter, "// START GENERATED LABEL ASSEMBLER METHODS", "// END GENERATED LABEL ASSEMBLER METHODS");
    }

    protected void emitByte(IndentWriter writer, String byteValue) {
        writer.print("emitByte(" + byteValue + ");");
    }

    protected void emitByte(IndentWriter writer, byte value) {
        emitByte(writer, "((byte) " + Bytes.toHexLiteral(value) + ")");
    }

    protected void generate() {
        try {
            final String rawAssemblerClassName = _rawAssemblerClassName.getValue();
            final String labelAssemblerClassName = _labelAssemblerClassName.getValue();

            final boolean rawAssemblerMethodsUpdated = generateRawAssemblerMethods(rawAssemblerClassName);
            final boolean labelAssemblerMethodsUpdated = generateLabelAssemblerMethods(labelAssemblerClassName);

            if (rawAssemblerClassName.equals(labelAssemblerClassName)) {
                if (rawAssemblerMethodsUpdated || labelAssemblerMethodsUpdated) {
                    System.out.println("modified: " + getSourceFileFor(rawAssemblerClassName));
                    if (!ToolChain.compile(rawAssemblerClassName)) {
                        throw ProgramError.unexpected("compilation failed for: " + rawAssemblerClassName +
                                        "[Maybe missing an import statement for one of the following packages: " +
                                        getImportPackages(rawAssemblerClassName, Sequence.Static.concatenated(templates(), labelTemplates())));
                    }
                } else {
                    System.out.println("unmodified: " + getSourceFileFor(rawAssemblerClassName));
                }
            } else {
                if (rawAssemblerMethodsUpdated) {
                    System.out.println("modified: " + getSourceFileFor(rawAssemblerClassName));
                    if (!ToolChain.compile(rawAssemblerClassName)) {
                        throw ProgramError.unexpected("compilation failed for: " + rawAssemblerClassName +
                                        "[Maybe missing an import statement for one of the following packages: " +
                                        getImportPackages(rawAssemblerClassName, templates()));
                    }
                } else {
                    System.out.println("unmodified: " + getSourceFileFor(rawAssemblerClassName));
                }

                if (labelAssemblerMethodsUpdated) {
                    System.out.println("modified: " + getSourceFileFor(labelAssemblerClassName));
                    if (!ToolChain.compile(labelAssemblerClassName)) {
                        throw ProgramError.unexpected("compilation failed for: " + labelAssemblerClassName +
                                        "[Maybe missing an import statement for one of the following packages: " +
                                        getImportPackages(labelAssemblerClassName, labelTemplates()));
                    }
                } else {
                    System.out.println("unmodified: " + getSourceFileFor(labelAssemblerClassName));
                }

            }

            Trace.line(1, "done");
        } catch (Throwable throwable) {
            throwable.printStackTrace();
            System.err.println("something went wrong: " + throwable + ": " + throwable.getMessage());
        }
    }

}
