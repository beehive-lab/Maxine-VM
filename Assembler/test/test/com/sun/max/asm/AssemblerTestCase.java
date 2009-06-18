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
package test.com.sun.max.asm;

import java.io.*;

import com.sun.max.asm.gen.*;
import com.sun.max.ide.*;
import com.sun.max.io.*;
import com.sun.max.program.*;
import com.sun.max.program.option.*;

/**
 * Base class for assembler tests that defines program options common to
 * all assembler test harnesses.
 * 
 * @author Doug Simon
 * @author Bernd Mathiske
 */
public abstract class AssemblerTestCase extends MaxTestCase {

    protected final OptionSet _options = new OptionSet();

    private final Option<String> _templateOption = _options.newStringOption("pattern", "",
            "specifies a pattern so that only templates with the matching patterns are tested");
    private final Option<Boolean> _serialized = _options.newBooleanOption("serial", false,
            "forces testing to be single threaded");
    private final Option<Integer> _startSerialOption = _options.newIntegerOption("start", 0,
            "specifies the first serial number to begin testing");
    private final Option<Integer> _endSerialOption = _options.newIntegerOption("end", Integer.MAX_VALUE,
            "specifies the last serial number to test");
    private final Option<Boolean> _sourceOption = _options.newBooleanOption("only-make-asm-source", false,
            "specifies that the testing framework should only create the assembler source files and should not run " +
            "any tests.");

    /**
     * Subclasses override this to modify a tester that is about to be {@linkplain #run() run}.
     * Typically, the modification is based on the values of any subclasses specific addition to {@link #_options}.
     */
    protected void configure(AssemblyTester tester) {
    }

    public AssemblerTestCase() {
    }

    public AssemblerTestCase(String name) {
        super(name);
    }

    public final void run(AssemblyTester tester) {
        _options.parseArguments(getProgramArguments());
        configure(tester);
        tester.setTemplatePattern(_templateOption.getValue());
        if (_sourceOption.getValue()) {
            final File sourceFile = new File(tester.assembly().instructionSet().name().toLowerCase() + "-asmTest.s");
            try {
                final IndentWriter indentWriter = new IndentWriter(new PrintWriter(new BufferedWriter(new FileWriter(sourceFile))));
                tester.createExternalSource(_startSerialOption.getValue(), _endSerialOption.getValue(), indentWriter);
                indentWriter.close();
            } catch (IOException e) {
                ProgramError.unexpected("Could not open " + sourceFile + " for writing", e);
            }
        } else {
            tester.run(_startSerialOption.getValue(), _endSerialOption.getValue(), !_serialized.getValue());
        }
    }
}
