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
/*VCSID=e41a0fef-5a42-49fa-9b28-da61c85178be*/
package test.com.sun.max.asm;

import com.sun.max.asm.gen.*;
import com.sun.max.program.option.*;

/**
 * Base class for assembler tests that use an external assembler which may
 * be executed remotely on another machine by using the '-remote=user@host'
 * program option.
 * 
 * @author Doug Simon
 * @author Bernd Mathiske
 */
public abstract class ExternalAssemblerTestCase extends AssemblerTestCase {

    private final Option<String> _removeUserOption = _options.newStringOption("remote", null,
            "execute commands via an ssh connection with supplied user@hostname");
    private final Option<String> _removePathOption = _options.newStringOption("remote-asm-path", null,
            "specifies an absolute path to the directory containing an assembler executable");

    public ExternalAssemblerTestCase() {
        super();
    }

    public ExternalAssemblerTestCase(String name) {
        super(name);
    }

    @Override
    protected void configure(AssemblyTester tester) {
        tester.setRemoteUserAndHost(_removeUserOption.getValue());
        if (_removePathOption.getValue() != null) {
            tester.setRemoteAssemblerPath(_removePathOption.getValue());
        }
    }
}
