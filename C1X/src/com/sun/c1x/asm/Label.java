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
    private int locator;

    // References to instructions that jump to this unresolved label.
    // These instructions need to be patched when the label is bound
    // using the platform-specific patchInstruction() method.
    private int [] patches;
    private int patchIndex;
    private ArrayList <Integer> patchOverflow;

    /**
     * Creates a new Label.
     */
    public Label() {
        super();
        patches = new int [PATCHCACHESIZE];
    }

    public void init() {
        locator = -1;
        patchIndex = 0;
        patchOverflow = null;
      }

    /**
     * Returns the position of the the Label in the code buffer.
     * The position is a 'locator', which encodes both offset and section.
     *
     * @return locator
     */
    public int locator() {
        assert locator >= 0 : "Unbounded label is being referenced";
        return locator;
    }

    /**
     * Binds this label to a given location.
     *
     * @param locator the locator to bind this label
     */
    public void bindLocation(int locator) {
        assert locator >= 0 : "The locator " + locator + " is not a valid binding locator";
        assert this.locator == -1 : "The Label is already baunded";
        this.locator = locator;
    }

    /**
     * Gets the offset encoded in the locator.
     *
     * @return the locator's offset.
     */
    public int locatorOffset() {
        // TODO: to be implemented. Depends on CodeBuffer
        return locator >> 2;
    }

    /**
     * Gets the section encoded by the locator.
     *
     * @return the locator's section
     */
    public int locatorSection() {
        // TODO: to be implemented. Depends on CodeBuffer
        return locator & 2;
    }

    public boolean isBound() {
        return locator >=  0;
    }

    public boolean isUnbound() {
        return locator == -1 && patchIndex > 0;
    }

    public boolean isUnused() {
        return locator == -1 && patchIndex == 0;
    }

    public void addPatchAt(CodeBuffer cb, int branchLocator) {
        assert locator == -1 : "Label is unbounded";
        if (patchIndex < PATCHCACHESIZE) {
          patches[patchIndex] = branchLocator;
        } else {
          if (patchOverflow == null) {
            //TODO: patchOverflow = cb->create_patch_overflow();
          }
          patchOverflow.add(branchLocator);
        }
        patchIndex++;
      }

    public void printInstruction(LogStream out) {

    }

//    class Label VALUE_OBJ_CLASS_SPEC {
//        private:
//         enum { PatchCacheSize = 4 };
//
//         // _loc encodes both the binding state (via its sign)
//         // and the binding locator (via its value) of a label.
//         //
//         // _loc >= 0   bound label, loc() encodes the target (jump) position
//         // _loc == -1  unbound label
//         int _loc;
//
//         // References to instructions that jump to this unresolved label.
//         // These instructions need to be patched when the label is bound
//         // using the platform-specific patchInstruction() method.
//         //
//         // To avoid having to allocate from the C-heap each time, we provide
//         // a local cache and use the overflow only if we exceed the local cache
//         int _patches[PatchCacheSize];
//         int _patch_index;
//         GrowableArray<int>* _patch_overflow;
//
//         Label(const Label&) { ShouldNotReachHere(); }
//
//        public:
//
//         /**
//          * After binding, be sure 'patch_instructions' is called later to link
//          */
//         void bind_loc(int loc) {
//           assert(loc >= 0, "illegal locator");
//           assert(_loc == -1, "already bound");
//           _loc = loc;
//         }
//         void bind_loc(int pos, int sect);  // = bind_loc(locator(pos, sect))
//
//       #ifndef PRODUCT
//         // Iterates over all unresolved instructions for printing
//         void print_instructions(MacroAssembler* masm) const;
//       #endif // PRODUCT

}
