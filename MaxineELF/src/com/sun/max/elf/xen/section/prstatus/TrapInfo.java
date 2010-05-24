/*
 * Copyright (c) 2009 Sun Microsystems, Inc., 4150 Network Circle, Santa
 * Clara, California 95054, U.S.A. All rights reserved.
 *
 * U.S. Government Rights - Commercial software. Government users are
 * subject to the Sun Microsystems, Inc. standard license agreement and
 * applicable provisions of the FAR and its supplements.
 *
 * Use is subject to license terms.
 *
 * This distribution may include materials developed by third parties.
 *
 * Parts of the product may be derived from Berkeley BSD systems,
 * licensed from the University of California. UNIX is a registered
 * trademark in the U.S.  and in other countries, exclusively licensed
 * through X/Open Company, Ltd.
 *
 * Sun, Sun Microsystems, the Sun logo and Java are trademarks or
 * registered trademarks of Sun Microsystems, Inc. in the U.S. and other
 * countries.
 *
 * This product is covered and controlled by U.S. Export Control laws and
 * may be subject to the export or import laws in other
 * countries. Nuclear, missile, chemical biological weapons or nuclear
 * maritime end uses or end users, whether direct or indirect, are
 * strictly prohibited. Export or reexport to countries subject to
 * U.S. embargo or to entities identified on U.S. export exclusion lists,
 * including, but not limited to, the denied persons and specially
 * designated nationals lists is strictly prohibited.
 *
 */
package com.sun.max.elf.xen.section.prstatus;


/**
 * @author Puneeet Lakhina
 *
 */
public class TrapInfo {

    private short vector;
    private short flags;
    private int codeSelector;
    private long codeOffset;
    /**
     * @return the vector
     */
    public short getVector() {
        return vector;
    }

    /**
     * @param vector the vector to set
     */
    public void setVector(short vector) {
        this.vector = vector;
    }

    /**
     * @return the flags
     */
    public short getFlags() {
        return flags;
    }

    /**
     * @param flags the flags to set
     */
    public void setFlags(short flags) {
        this.flags = flags;
    }

    /**
     * @return the codeSelector
     */
    public int getCodeSelector() {
        return codeSelector;
    }

    /**
     * @param codeSelector the codeSelector to set
     */
    public void setCodeSelector(int codeSelector) {
        this.codeSelector = codeSelector;
    }

    /**
     * @return the codeOffset
     */
    public long getCodeOffset() {
        return codeOffset;
    }

    /**
     * @param codeOffset the codeOffset to set
     */
    public void setCodeOffset(long codeOffset) {
        this.codeOffset = codeOffset;
    }



}
