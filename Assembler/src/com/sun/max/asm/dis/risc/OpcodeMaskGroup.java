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
package com.sun.max.asm.dis.risc;

import java.util.*;

import com.sun.max.asm.gen.risc.*;
import com.sun.max.collect.*;

/**
 * An opcode mask group is a collection of templates that share the same opcode mask.
 * Some templates in the group may also share the same opcode.
 *
 * @author Bernd Mathiske
 * @author Doug Simon
 */
public class OpcodeMaskGroup<Template_Type extends RiscTemplate> {

    private final int mask;

    public OpcodeMaskGroup(int mask) {
        this.mask = mask;
    }

    public int mask() {
        return mask;
    }

    private final Set<Template_Type> templates = new HashSet<Template_Type>();

    private final IntHashMap<AppendableSequence<Template_Type>> templatesForOpcodes = new IntHashMap<AppendableSequence<Template_Type>>();
    private final Sequence<Template_Type> empty = new LinkSequence<Template_Type>();

    public void add(Template_Type template) {
        assert template.opcodeMask() == mask;
        templates.add(template);
        AppendableSequence<Template_Type> templatesForOpcode = templatesForOpcodes.get(template.opcode());
        if (templatesForOpcode == null) {
            templatesForOpcode = new LinkSequence<Template_Type>();
            templatesForOpcodes.put(template.opcode(), templatesForOpcode);
        }
        templatesForOpcode.append(template);
    }

    public Sequence<Template_Type> templatesFor(int opcode) {
        final Sequence<Template_Type> result = templatesForOpcodes.get(opcode);
        if (result == null) {
            return empty;
        }
        return result;
    }

    public Sequence<Template_Type> templates() {
        return new ArraySequence<Template_Type>(templates);
    }
}
