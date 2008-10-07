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
/*VCSID=df469aac-a309-4397-a90b-44b9c81da9f3*/
package com.sun.max.annotate.processor;

import java.util.*;

import javax.annotation.processing.*;
import javax.lang.model.element.*;
import javax.lang.model.util.*;

import com.sun.max.annotate.*;

/**
 * An annotation processor that validates the use of (some of) the compile-time annotations
 * in the VM project.
 *
 * @author Doug Simon
 * @author Bernd Mathiske
 */
public class VMAnnotationProcessor extends BaseAnnotationProcessor {

    private TypeElement _cFunctionAnnotationElement;
    private CFunctionScanner _cFunctionScanner;

    @Override
    public synchronized void init(ProcessingEnvironment environment) {
        super.init(environment);
        _cFunctionAnnotationElement = _elementUtils.getTypeElement(C_FUNCTION.class.getName());
        _cFunctionScanner = new CFunctionScanner();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        super.process(annotations, roundEnv);
        if (!roundEnv.processingOver()) {
            for (Element element : roundEnv.getRootElements()) {
                _cFunctionScanner.scan(element);
            }
        }
        return false;
    }

    protected boolean isPrivate(Element element) {
        return element.getModifiers().contains(Modifier.PRIVATE);
    }

    class CFunctionScanner extends ElementScanner6<Void, Void> {
        @Override
        public Void visitExecutable(ExecutableElement e, Void p) {
            if (e.getKind() == ElementKind.METHOD) {
                for (AnnotationMirror annotation : e.getAnnotationMirrors()) {
                    if (annotation.getAnnotationType().equals(_cFunctionAnnotationElement.asType())) {
                        if (!isPrivate(e)) {
                            warning(e, "Method annotated with @C_FUNCTION must be private");
                        }
                    }
                }
            }
            try {
                super.visitExecutable(e, p);
            } catch (NullPointerException exception) {
                // Not sure why this happens: seems to be a bug in javac
            }
            return null;
        }
    }
}
