/*
 * Copyright (c) 2009 Sun Microsystems, Inc.  All rights reserved.
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
package com.sun.c1x.asm;

import java.util.*;

import com.sun.c1x.util.*;


/**
 * The <code>Label</code> class definition.
 *
 * @author Marcelo Cintra
 *
 */
public class Label {

    private static int PATCHCACHESIZE = 4;

    // locator encodes both the binding state (via its sign)
    // and the binding locator (via its value) of a label.
    // locator >= 0   bound label, locator() encodes the target (jump) position
    // locator == -1  unbound label
    // The locator encodes both offset and section
    private int loc;

    // References to instructions that jump to this unresolved label.
    // These instructions need to be patched when the label is bound
    // using the platform-specific patchInstruction() method.
    private List<Integer> patchOverflow;

    /**
     * Creates a new Label.
     */
    public Label() {
        super();
        patchOverflow = new ArrayList<Integer>(PATCHCACHESIZE);
        this.loc = -1;
    }

    /**
     * Returns the position of the the Label in the code buffer.
     * The position is a 'locator', which encodes both offset and section.
     *
     * @return locator
     */
    public int loc() {
        assert loc >= 0 : "Unbounded label is being referenced";
        return loc;
    }

    public boolean isBound() {
        return loc >=  0;
    }

    public boolean isUnbound() {
        return loc == -1 && patchOverflow.size() > 0;
    }

    public boolean isUnused() {
        return loc == -1 && patchOverflow.size() == 0;
    }

    public void addPatchAt(int branchLocator) {
        assert loc == -1 : "Label is unbounded";
        patchOverflow.add(branchLocator);
      }

    public void printInstruction(LogStream out) {

    }

    public void printInstructions(AbstractAssembler abstractAssembler) {
        // TODO Auto-generated method stub

    }

    public void patchInstructions(AbstractAssembler masm) {

        assert isBound() : "Label is bound";

        int target = loc;
        for (int branchLoc : patchOverflow) {
            masm.pdPatchInstruction(branchLoc, target);
        }
    }

    @Override
    public String toString() {
        return "label";
    }

    public void bindLoc(int codeOffset) {
        this.loc = codeOffset;
    }
}
