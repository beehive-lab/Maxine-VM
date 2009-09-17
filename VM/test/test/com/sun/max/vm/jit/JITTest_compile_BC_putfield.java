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

import test.com.sun.max.vm.compiler.*;
import test.com.sun.max.vm.jtrun.all.*;

import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.template.source.*;


/**
 * @author Bernd Mathiske
 */
public class JITTest_compile_BC_putfield  extends JitCompilerTestCase {

/*    public void test_compileMethod() {
        final TargetMethod targetMethod = compileMethod(BC_putfield.class, "test");
        traceBundleAndDisassemble(targetMethod);
    }*/
/*
    public void test_compileMethod() {
        final TargetMethod targetMethod = compileMethod(List_reorder_bug.class, "match");
        traceBundleAndDisassemble(targetMethod);
    }
*/
    public void test_compileMethod() {
        final CPSTargetMethod targetMethod = compileMethod(JavaTesterTests.class, "test_bytecode_BC_athrow");
        traceBundleAndDisassemble(targetMethod);
    }

    public static String getName(int a) {
        if (a == 0) {
            return String.class.getName();
        }
        return "";
    }

    @Override
    protected Class[] templateSources() {
        return TemplateTableConfiguration.OPTIMIZED_TEMPLATE_SOURCES;
    }

}
