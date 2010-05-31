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
public class OpcodeMaskGroup {

    private final int mask;

    public OpcodeMaskGroup(int mask) {
        this.mask = mask;
    }

    public int mask() {
        return mask;
    }

    private final Set<RiscTemplate> templates = new HashSet<RiscTemplate>();

    private final IntHashMap<List<RiscTemplate>> templatesForOpcodes = new IntHashMap<List<RiscTemplate>>();
    private final List<RiscTemplate> empty = new LinkedList<RiscTemplate>();

    public void add(RiscTemplate template) {
        assert template.opcodeMask() == mask;
        templates.add(template);
        List<RiscTemplate> templatesForOpcode = templatesForOpcodes.get(template.opcode());
        if (templatesForOpcode == null) {
            templatesForOpcode = new LinkedList<RiscTemplate>();
            templatesForOpcodes.put(template.opcode(), templatesForOpcode);
        }
        templatesForOpcode.add(template);
    }

    public List<RiscTemplate> templatesFor(int opcode) {
        final List<RiscTemplate> result = templatesForOpcodes.get(opcode);
        if (result == null) {
            return empty;
        }
        return result;
    }

    public Collection<RiscTemplate> templates() {
        return templates;
    }
}
