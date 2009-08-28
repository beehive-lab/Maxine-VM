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
package com.sun.max.vm.prototype;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.jar.*;
import java.util.zip.*;

import com.sun.c1x.*;
import com.sun.max.collect.*;
import com.sun.max.ide.*;
import com.sun.max.lang.*;
import com.sun.max.platform.*;
import com.sun.max.profile.*;
import com.sun.max.profile.ValueMetrics.*;
import com.sun.max.program.*;
import com.sun.max.program.option.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.classfile.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.code.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.compiler.cir.*;
import com.sun.max.vm.compiler.cir.bytecode.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.type.*;

/**
 * Construction of a virtual machine image begins here by running on a host virtual
 * machine (e.g. Hotspot). This process involves creating a target VM configuration
 * and loading and initializing the classes that implement VM services. The representation
 * of the virtual machine being built is referred to as the "prototype", and the final
 * product, a binary image that contains the compiled machine code of the virtual machine
 * as well as objects and metadata that implement the virtual machine, is called the
 * "image".
 *
 * @author Bernd Mathiske
 * @author Doug Simon
 * @author Ben L. Titzer
 */
public final class BinaryImageGenerator {

    private static final String DEFAULT_IMAGE_OBJECT_TREE_FILE_NAME = "maxine.object.tree";
    private static final String DEFAULT_IMAGE_METHOD_TREE_FILE_NAME = "maxine.method.tree";
    private static final String DEFAULT_IMAGE_JAR_FILE_NAME = "maxine.jar";
    private static final String DEFAULT_IMAGE_FILE_NAME = "maxine.vm";
    private static final String OPERATING_SYSTEM_PROPERTY = "max.host.os";
    private static final String OUTPUT_DIRECTORY = "Native" + File.separator + "generated" + File.separator +
        System.getProperty(OPERATING_SYSTEM_PROPERTY, OperatingSystem.current().name()).toLowerCase();

    private final OptionSet options = new OptionSet();

    private final Option<Boolean> help = options.newBooleanOption("help", false,
            "Show help message and exits.");

    private final Option<Boolean> treeOption = options.newBooleanOption("tree", false,
            "Create a file showing the connectivity of objects in the image.");

    private final Option<Boolean> statsOption = options.newBooleanOption("stats", false,
            "Create a file detailing the number and size of each type of object in the image.");

    private final Option<File> outputDirectoryOption = options.newFileOption("output-dir", getDefaultOutputDirectory(),
            "Selects the output directory for the binary image generator.");

    private final Option<String> imageFileOption = options.newStringOption("image-file", DEFAULT_IMAGE_FILE_NAME,
            "Selects the name of the image file to generate.");

    private final Option<String> jarFileOption = options.newStringOption("jar-file", DEFAULT_IMAGE_JAR_FILE_NAME,
            "Selects the name of the jar file to generate, which contains all of the " +
            "classes in a generated image.");

    private final Option<String> statsFileOption = options.newStringOption("stats-file", "maxine.stats",
            "Specifies the name of the statistics file generated (if any).");

    private final Option<String> objectTreeFileOption = options.newStringOption("object-tree-file", DEFAULT_IMAGE_OBJECT_TREE_FILE_NAME,
            "Specifies the name of the object tree file generated (if any).");

    private final Option<String> methodTreeFileOption = options.newStringOption("method-tree-file", DEFAULT_IMAGE_METHOD_TREE_FILE_NAME,
            "Specifies the name of the method tree file generated (if any).");

    private final Option<Boolean> testCallerJit = options.newBooleanOption("test-caller-jit", false,
            "For the Java tester, this option specifies that each test case's harness should be compiled " +
            "with the JIT compiler (helpful for testing JIT->JIT and JIT->opt calls).");

    private final Option<Boolean> testCalleeJit = options.newBooleanOption("test-callee-jit", false,
            "For the Java tester, this option specifies that each test case's method should be compiled " +
            "with the JIT compiler (helpful for testing JIT->JIT and opt->JIT calls).");

    private final Option<Boolean> testCalleeC1X = options.newBooleanOption("test-callee-c1x", false,
        "For the Java tester, this option specifies that each test case's method should be compiled " +
        "with the C1X compiler (helpful for testing C1X->C1X and opt->C1X calls).");

    private final Option<Boolean> testUnlinked = options.newBooleanOption("test-unlinked", false,
            "For the Java tester, this option specifies that each test case method should be unlinked.");

    private final Option<Boolean> testNative = options.newBooleanOption("native-tests", false,
            "For the Java tester, this option specifies that " + System.mapLibraryName("javatest") + " should be dynamically loaded.");

    private final Option<String> vmArguments = options.newStringOption("vmargs", null,
            "A set of one or VM arguments. This is useful for exercising VM functionality or " +
            "enabling VM tracing while prototyping.");

    /**
     * Used in the Java tester to indicate whether to test the resolution and linking mechanism for
     * test methods.
     */
    public static boolean unlinked = false;

    /**
     * Used in the Java tester to indicate whether to compile the testing harness itself with the JIT.
     */
    public static boolean callerJit = false;

    /**
     * Used by the Java tester to indicate whether to compile the tests themselves with the JIT.
     */
    public static boolean calleeJit = false;

    /**
     * Used by the Java tester to indicate whether to compile the tests themselves with the JIT.
     */
    public static boolean calleeC1X = false;

    /**
     * Used by the Java tester to indicate that testing requires dynamically loading native libraries.
     */
    public static boolean nativeTests = false;

    /**
     * Get the default output directory, derived from the project directory.
     *
     * @return a file representing the default output directory for the image and statistics
     */
    public static File getDefaultOutputDirectory() {
        return new File(JavaProject.findVcsProjectDirectory().getParentFile().getAbsoluteFile(), OUTPUT_DIRECTORY);
    }

    /**
     * Get the default file name where to generate the boot image file.
     *
     * @return the default file to which to write the boot image
     */
    public static File getDefaultBootImageFilePath() {
        return new File(getDefaultOutputDirectory(), DEFAULT_IMAGE_FILE_NAME);
    }

    /**
     * Get the default file name where to generate the boot jar.
     *
     * @return a file representing the default file to which to write the boot jar file
     */
    public static File getDefaultBootImageJarFilePath() {
        return new File(getDefaultOutputDirectory(), DEFAULT_IMAGE_JAR_FILE_NAME);
    }

    /**
     * Get the default file name where to write the object tree file.
     * @return the file to which to write the object tree information
     */
    public static File getDefaultBootImageObjectTreeFilePath() {
        return new File(getDefaultOutputDirectory(), DEFAULT_IMAGE_OBJECT_TREE_FILE_NAME);
    }

    /**
     * Get the default file name where to write the method tree file.
     *
     * @return the file to which to write the method tree information
     */
    public static File getDefaultBootImageMethodTreeFilePath() {
        return new File(getDefaultOutputDirectory(), DEFAULT_IMAGE_METHOD_TREE_FILE_NAME);
    }

    /**
     * Creates and runs the binary image generator with the specified command line arguments.
     *
     * @param programArguments the arguments from the command line
     */
    public BinaryImageGenerator(String[] programArguments) {
        final long start = System.currentTimeMillis();
        CompilerScheme compilerScheme = null;
        try {
            TargetMethod.COLLECT_TARGET_METHOD_STATS = statsOption.getValue();
            final PrototypeGenerator prototypeGenerator = new PrototypeGenerator(options);
            Trace.addTo(options);

            options.addFieldOptions(C1XOptions.class, "C1X:");
            options.parseArguments(programArguments);

            if (help.getValue()) {
                prototypeGenerator.createVMConfiguration(prototypeGenerator.createDefaultVMConfiguration());
                options.printHelp(System.out, 80);
                return;
            }

            if (vmArguments.getValue() != null) {
                VMOption.setVMArguments(vmArguments.getValue().split("\\s+"));
            }

            BinaryImageGenerator.calleeC1X = testCalleeC1X.getValue();
            BinaryImageGenerator.calleeJit = testCalleeJit.getValue();
            BinaryImageGenerator.callerJit = testCallerJit.getValue();
            BinaryImageGenerator.unlinked = testUnlinked.getValue();
            BinaryImageGenerator.nativeTests = testNative.getValue();

            final File outputDirectory = outputDirectoryOption.getValue();
            outputDirectory.mkdirs();

            final DataPrototype dataPrototype = prototypeGenerator.createDataPrototype(treeOption.getValue());
            VMConfiguration.target().finalizeSchemes(MaxineVM.Phase.PROTOTYPING);

            final GraphPrototype graphPrototype = dataPrototype.graphPrototype();
            compilerScheme = dataPrototype.vmConfiguration().compilerScheme();

            VMOptions.beforeExit();

            // write the statistics
            if (statsOption.getValue()) {
                writeStats(graphPrototype, new File(outputDirectory, statsFileOption.getValue()));
            }
            writeJar(new File(outputDirectory, jarFileOption.getValue()));
            writeImage(dataPrototype, new File(outputDirectory, imageFileOption.getValue()));
            if (treeOption.getValue()) {
                // write the tree file only if specified by the user.
                writeObjectTree(dataPrototype, graphPrototype, new File(outputDirectory, objectTreeFileOption.getValue()));
            }
            writeMethodTree(graphPrototype.compiledPrototype, new File(outputDirectory, methodTreeFileOption.getValue()));

        } catch (IOException ioException) {
            ProgramError.unexpected("could not write file ", ioException);
        } finally {
            final long timeInMilliseconds = System.currentTimeMillis() - start;
            if (statsOption.getValue()) {
                try {
                    writeMiscStatistics(compilerScheme, Trace.stream());
                } catch (Throwable throwable) {
                    throwable.printStackTrace();
                }
            }
            Trace.line(1, "Total time: " + (timeInMilliseconds / 1000.0f) + " seconds");
            System.out.flush();
        }
    }

    /**
     * Writes the image data to the specified file.
     *
     * @param dataPrototype the data prototype containing a data-level representation of the image
     * @param file the file to which to write the data prototype
     */
    private void writeImage(DataPrototype dataPrototype, File file) {
        try {
            final FileOutputStream outputStream = new FileOutputStream(file);
            final BootImage bootImage = new BootImage(dataPrototype);
            try {
                Trace.begin(1, "writing boot image file: " + file);
                bootImage.write(outputStream);
                Trace.end(1, "end boot image file: " + file + " (" + Longs.toUnitsString(file.length()) + ")");
            } catch (IOException ioException) {
                ProgramError.unexpected("could not write file: " + file, ioException);
            } finally {
                try {
                    outputStream.close();
                } catch (IOException ioException) {
                    ProgramWarning.message("could not close file: " + file);
                }
            }
        } catch (FileNotFoundException fileNotFoundException) {
            throw ProgramError.unexpected("could not open file: " + file);
        } catch (BootImageException bootImageException) {
            ProgramError.unexpected("could not construct proper boot image", bootImageException);
        }
    }

    /**
     * Writes a jar file containing all of the (potentially rewritten) VM class files to the specified file.
     *
     * @param file the file to which to write the jar
     * @throws IOException if there is a problem writing the jar
     */
    private void writeJar(File file) throws IOException {
        Trace.begin(1, "writing boot image jar file: " + file);
        createBootImageJarFile(file);
        Trace.end(1, "end boot image jar file: " + file + " (" + Longs.toUnitsString(file.length()) + ")");
    }

    /**
     * Writes miscellaneous statistics about the boot image creation process to a file.
     *
     * @param graphPrototype the graph (i.e. nodes/edges) representation of the prototype
     * @param file the file to which to write the statistics
     * @throws IOException if there is a problem writing to the file
     */
    private void writeStats(GraphPrototype graphPrototype, File file) throws IOException {
        Trace.begin(1, "writing boot image statistics file: " + file);
        final FileOutputStream fileOutputStream = new FileOutputStream(file);
        new GraphStats(graphPrototype).dumpStats(new PrintStream(fileOutputStream));
        fileOutputStream.close();
        Trace.end(1, "end boot image statistics file: " + file + " (" + Longs.toUnitsString(file.length()) + ")");
    }

    /**
     * Writes the object tree to a file. The object tree helps to diagnose space usage problems,
     * typically caused by including too much into the image.
     *
     * @param dataPrototype the data representation of the prototype
     * @param graphPrototype the graph representation of the prototype
     * @param file the file to which to write the object
     * @throws IOException
     */
    private void writeObjectTree(DataPrototype dataPrototype, GraphPrototype graphPrototype, File file) throws IOException {
        Trace.begin(1, "writing boot image object tree file: " + file);
        final FileOutputStream fileOutputStream = new FileOutputStream(file);
        final DataOutputStream dataOutputStream = new DataOutputStream(new BufferedOutputStream(fileOutputStream, 1000000));
        BinaryImageObjectTree.saveTree(dataOutputStream, graphPrototype.links(), dataPrototype.allocationMap());
        dataOutputStream.flush();
        fileOutputStream.close();
        Trace.end(1, "writing boot image object tree file: " + file + " (" + Longs.toUnitsString(file.length()) + ")");
    }

    /**
     * Writes the method tree to a file. The method tree helps to diagnose the inclusion
     * of methods into the image that should not be included.
     *
     * @param compiledPrototype the compiled-code representation of the prototype
     * @param file the file to which to write the tree
     * @throws IOException if there is a problem writing to the file
     */
    private void writeMethodTree(CompiledPrototype compiledPrototype, File file) throws IOException {
        Trace.begin(1, "writing boot image method tree file: " + file);
        final FileOutputStream fileOutputStream = new FileOutputStream(file);
        final DataOutputStream dataOutputStream = new DataOutputStream(new BufferedOutputStream(fileOutputStream, 1000000));
        BinaryImageMethodTree.saveTree(dataOutputStream, compiledPrototype.links());
        dataOutputStream.flush();
        fileOutputStream.close();
        Trace.end(1, "writing boot image method tree file: " + file + " (" + Longs.toUnitsString(file.length()) + ")");
    }

    /**
     * The main entrypoint, which creates a binary image generator and then runs it.
     *
     * @param programArguments the arguments from the command line
     */
    public static void main(String[] programArguments) {
        new BinaryImageGenerator(programArguments);
    }

    /**
     * Creates a jar file of all the class files from the types in the VM type registry were created.
     *
     * @param jarFile   the path of the jar file to create
     */
    private static void createBootImageJarFile(File jarFile) throws IOException {
        final JarOutputStream jarOutputStream = new JarOutputStream(new FileOutputStream(jarFile));
        jarOutputStream.setLevel(Deflater.BEST_COMPRESSION);
        final Classpath classPath = PrototypeClassLoader.PROTOTYPE_CLASS_LOADER.classpath();

        for (ClassActor classActor : ClassRegistry.vmClassRegistry()) {
            if (classActor.isInterfaceActor() || classActor.isTupleClassActor() || classActor.isHybridClassActor()) {
                try {
                    final ClasspathFile classpathFile = PrototypeClassLoader.readClassFile(classPath, classActor.name.toString());

                    final String classfilePath = classActor.name.toString().replace('.', '/') + ".class";
                    final JarEntry jarEntry = new JarEntry(classfilePath);
                    jarEntry.setTime(System.currentTimeMillis());
                    jarOutputStream.putNextEntry(jarEntry);
                    jarOutputStream.write(classpathFile.contents);
                    jarOutputStream.closeEntry();
                } catch (ClassNotFoundException classNotFoundException) {
                    ProgramError.unexpected("could not find class file for " + classActor, classNotFoundException);
                }
            }
        }

        jarOutputStream.close();
    }

    /**
     * Writes various statistics about the image creation process to the standard output.
     *
     * @param compilerScheme the compiler, which typically includes compilation statistics
     * @param out the output stream to which to write the statistics
     */
    private static void writeMiscStatistics(CompilerScheme compilerScheme, PrintStream out) {
        Trace.line(1, "# utf8 constants: " + SymbolTable.length());
        Trace.line(1, "# type descriptors: " + TypeDescriptor.numberOfDescriptors());
        Trace.line(1, "# signature descriptors: " + SignatureDescriptor.totalNumberOfDescriptors());

        int totalConstants = 0;
        final int[] constantPoolHistogram = new int[ConstantPool.Tag.VALUES.length()];

        int birBytecodeTotal = 0;
        int cirBytecodeTotal = 0;

        final Field bytecodeField = Classes.getDeclaredField(CirMethod.class, "cirBytecode");
        bytecodeField.setAccessible(true);

        CirGenerator cirGenerator = null;
        if (compilerScheme != null && compilerScheme instanceof CirGeneratorScheme) {
            cirGenerator = ((CirGeneratorScheme) compilerScheme).cirGenerator();
        }
        // report total of CIR bytecodes
        for (ClassActor classActor : ClassRegistry.vmClassRegistry()) {
            final ConstantPool constantPool = classActor.constantPool();
            if (constantPool != null) {
                final int numberOfConstants = constantPool.numberOfConstants();
                totalConstants += numberOfConstants;
                for (int i = 1; i < numberOfConstants; i++) {
                    constantPoolHistogram[constantPool.tagAt(i).ordinal()]++;
                }

                final AppendableSequence<ClassMethodActor> allClassMethodActors = new ArrayListSequence<ClassMethodActor>();
                AppendableSequence.Static.appendAll(allClassMethodActors, classActor.localVirtualMethodActors());
                AppendableSequence.Static.appendAll(allClassMethodActors, classActor.localStaticMethodActors());
                for (ClassMethodActor classMethodActor : allClassMethodActors) {
                    final CodeAttribute codeAttribute = classMethodActor.codeAttribute();
                    if (codeAttribute != null) {
                        birBytecodeTotal += codeAttribute.code().length;
                    }
                    if (cirGenerator != null) {
                        final CirMethod cirMethod = cirGenerator.getCirMethod(classMethodActor);
                        if (cirMethod != null) {
                            try {
                                final CirBytecode cirBytecode = (CirBytecode) bytecodeField.get(cirMethod);
                                if (cirBytecode != null) {
                                    cirBytecodeTotal += cirBytecode.code().length;
                                }
                            } catch (Exception exception) {
                                ProgramError.unexpected(exception);
                            }
                        }
                    }
                }
            }
        }

        // report potential savings from collecting target method junk into the code region
        final int headerSize = VMConfiguration.target().layoutScheme().arrayHeaderLayout.headerSize();
        final ObjectDistribution<Object> distribution = ValueMetrics.newObjectDistribution(null);
        int zeroLiterals = 0;
        int zeroCatchRangePositions = 0;
        int zeroCatchBlockPositions = 0;
        int zeroStopPositions = 0;
        int zeroDirectCallees = 0;
        int zeroReferenceMaps = 0;
        int zeroScalarLiterals = 0;
        for (TargetMethod targetMethod : Code.bootCodeRegion.targetMethods()) {
            final Object[] referenceLiterals = targetMethod.referenceLiterals();
            if (referenceLiterals != null) {
                for (int i = 0; i < referenceLiterals.length; i++) {
                    distribution.record(referenceLiterals[i]);
                }
            }
            zeroLiterals += savingsFrom(headerSize, referenceLiterals);
            zeroCatchRangePositions += savingsFrom(headerSize, (targetMethod instanceof ExceptionRangeTargetMethod) ? ((ExceptionRangeTargetMethod) targetMethod).catchRangePositions() : null);
            zeroCatchBlockPositions += savingsFrom(headerSize, (targetMethod instanceof ExceptionRangeTargetMethod) ? ((ExceptionRangeTargetMethod) targetMethod).catchBlockPositions() : null);
            zeroStopPositions += savingsFrom(headerSize, targetMethod.stopPositions());
            zeroDirectCallees += savingsFrom(headerSize, targetMethod.directCallees());
            zeroReferenceMaps += savingsFrom(headerSize, targetMethod.referenceMaps());
            zeroScalarLiterals += savingsFrom(headerSize, targetMethod.scalarLiterals());
        }
        int redundantReferenceLiterals = 0;
        for (Map.Entry<Object, Integer> entry : distribution.asMap().entrySet()) {
            redundantReferenceLiterals += (entry.getValue() - 1) * Word.size();
        }
        out.println("Potential savings from reference literal merging: " + redundantReferenceLiterals + " bytes");
        out.println("Potential savings from compressing reference literal arrays: " + zeroLiterals + " bytes");
        out.println("Potential savings from compressing catch range position arrays: " + zeroCatchRangePositions + " bytes");
        out.println("Potential savings from compressing catch block position arrays: " + zeroCatchBlockPositions + " bytes");
        out.println("Potential savings from compressing stop position arrays: " + zeroStopPositions + " bytes");
        out.println("Potential savings from compressing reference map arrays: " + zeroReferenceMaps + " bytes");
        out.println("Potential savings from compressing scalar literal arrays: " + zeroScalarLiterals + " bytes");
        out.println("Total potential savings: " + (redundantReferenceLiterals + zeroLiterals + zeroCatchBlockPositions + zeroCatchBlockPositions + zeroStopPositions + zeroReferenceMaps + zeroScalarLiterals) + " bytes");

        out.println("Constant pool constants:");
        for (ConstantPool.Tag tag : ConstantPool.Tag.values()) {
            final int count = constantPoolHistogram[tag.ordinal()];
            if (count != 0) {
                out.printf("    %25s: %8d (%d%%)\n", tag.toString(), count, count * 100 / totalConstants);
            }
        }
        out.printf("    %25s: %8d\n", "Total", totalConstants);
        out.println("Bytecode:");
        out.println("    BIR: " + birBytecodeTotal);
        out.println("    CIR: " + cirBytecodeTotal);

        GlobalMetrics.report(Trace.stream());
    }

    /**
     * Helper to compute the savings from removing the specified object's header, and if null,
     * the field that referred to it.
     * @param headerSize the size of the object's header
     * @param o the object itself
     * @return the number of bytes that could be saved by removing the object's header and the field
     * that refers to it.
     */
    private static int savingsFrom(int headerSize, Object o) {
        if (o == null) {
            return Word.size() + headerSize;
        }
        return headerSize;
    }
}
