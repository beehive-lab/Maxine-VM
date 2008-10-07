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
/*VCSID=17d2ff05-5070-45f5-b398-8bd296e1b2a6*/
package com.sun.max.asm.gen;

import java.util.*;

import com.sun.max.asm.*;
import com.sun.max.collect.*;

/**
 * @author Bernd Mathiske
 */
public final class LabelParameter implements Parameter {

    public static final LabelParameter LABEL = new LabelParameter();

    private LabelParameter() {
    }

    public Class type() {
        return Label.class;
    }

    public String variableName() {
        return "label";
    }

    public String valueString() {
        return variableName();
    }

    public ArgumentRange argumentRange() {
        return null;
    }


    public Iterable<Label> getLegalTestArguments() {
        return Iterables.empty();
    }

    public Argument getExampleArgument() {
        return null;
    }

    public Iterable<? extends Argument> getIllegalTestArguments() {
        return Iterables.empty();
    }

    public Set<Argument> excludedDisassemblerTestArguments() {
        return Sets.empty(Argument.class);
    }

    public Set<Argument> excludedExternalTestArguments() {
        return Sets.empty(Argument.class);
    }

    public int compareTo(Parameter other) {
        return type().getName().compareTo(other.type().getName());
    }

    @Override
    public String toString() {
        return "<LabelParameter>";
    }

}
