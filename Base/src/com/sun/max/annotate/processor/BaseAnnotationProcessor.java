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
/*VCSID=100b41e1-ee40-4deb-91d6-ef2253278237*/
package com.sun.max.annotate.processor;

import static javax.tools.Diagnostic.Kind.*;

import java.util.*;

import javax.annotation.processing.*;
import javax.lang.model.*;
import javax.lang.model.element.*;
import javax.lang.model.util.*;

/**
 * An annotation processor for compile-time checking of annotations.
 *
 * @author Doug Simon
 */
public class BaseAnnotationProcessor extends AbstractProcessor {

    private Messager _messager;
    private boolean _reportAsWarning;

    protected Types _typeUtils;
    protected Elements _elementUtils;

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Collections.singleton("*");
    }

    @Override
    public Set<String> getSupportedOptions() {
        return Collections.singleton("reportAsWarning");
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.RELEASE_6;
    }

    @Override
    public synchronized void init(ProcessingEnvironment environment) {
        for (Map.Entry<String, String> option : environment.getOptions().entrySet()) {
            if (option.getKey().equals("reportAsWarning")) {
                _reportAsWarning = !"false".equals(option.getValue());
            } else {
                System.err.println("Ignored unknown option: " + option);
            }
        }

        _typeUtils = environment.getTypeUtils();
        _elementUtils = environment.getElementUtils();
        _messager = environment.getMessager();

        super.init(environment);
    }

    protected void warning(Element element, String message) {
        _messager.printMessage(WARNING, message, element);
    }

    protected void error(Element element, String message) {
        _messager.printMessage(ERROR, message, element);
    }

    protected void problem(Element element, String message) {
        if (_reportAsWarning) {
            warning(element, message);
        } else {
            error(element, message);
        }
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        return false;
    }
}
