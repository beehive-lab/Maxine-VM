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
package com.sun.max.vm.cps.dir;

import java.util.*;

import com.sun.max.vm.cps.dir.transform.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;

/**
 * The one and only conditional control flow instruction.
 *
 * @author Bernd Mathiske
 */
public class DirSwitch extends DirInstruction {

    private final Kind comparisonKind;
    private final ValueComparator valueComparator;
    private final DirValue tag;
    private final DirValue[] matches;
    private final DirBlock[] targetBlocks;
    private DirBlock defaultTargetBlock;

    public DirSwitch(Kind comparisonKind, ValueComparator valueComparator, DirValue tag, DirValue[] matches, DirBlock[] targetBlocks, DirBlock defaultTargetBlock) {
        this.comparisonKind = comparisonKind;
        this.valueComparator = valueComparator;
        this.tag = tag;
        this.matches = matches;
        this.targetBlocks = targetBlocks;
        this.defaultTargetBlock = defaultTargetBlock;
    }

    public ValueComparator valueComparator() {
        return valueComparator;
    }

    public Kind comparisonKind() {
        return comparisonKind;
    }

    public DirValue tag() {
        return tag;
    }

    public DirValue[] matches() {
        return matches;
    }

    public DirBlock[] targetBlocks() {
        return targetBlocks;
    }

    public DirBlock defaultTargetBlock() {
        return defaultTargetBlock;
    }

    public void setDefaultTargetBlock(DirBlock block) {
        defaultTargetBlock = block;
    }

    @Override
    public void substituteBlocks(Map<DirBlock, DirBlock> blockMap) {
        for (int i = 0; i < targetBlocks.length; i++) {
            final DirBlock targetBlock = targetBlocks[i];
            final DirBlock block = blockMap.get(targetBlock);
            if (block != null && block != targetBlock) {
                targetBlocks[i] = block;
            }
        }
        final DirBlock block = blockMap.get(defaultTargetBlock);
        if (block != null && block != defaultTargetBlock) {
            defaultTargetBlock = block;
        }
    }

    @Override
    public boolean isEquivalentTo(DirInstruction other, DirBlockEquivalence dirBlockEquivalence) {
        if (other instanceof DirSwitch) {
            final DirSwitch dirSwitch = (DirSwitch) other;
            if (!(comparisonKind == dirSwitch.comparisonKind && valueComparator == dirSwitch.valueComparator &&
                  matches.length == dirSwitch.matches.length && tag.equals(dirSwitch.tag))) {
                return false;
            }
            for (int i = 0; i < matches.length; i++) {
                if (!matches[i].equals(dirSwitch.matches[i])) {
                    return false;
                }
            }
            for (int i = 0; i < matches.length; i++) {
                if (!targetBlocks[i].isEquivalentTo(dirSwitch.targetBlocks[i], dirBlockEquivalence)) {
                    return false;
                }
            }
            return defaultTargetBlock.isEquivalentTo(dirSwitch.defaultTargetBlock, dirBlockEquivalence);
        }
        return false;
    }

    @Override
    public int hashCodeForBlock() {
        return super.hashCodeForBlock() ^ tag.hashCodeForBlock();
    }

    @Override
    public String toString() {
        String blocks = "";
        String separator = "";
        for (DirBlock block : targetBlocks) {
            blocks += separator + block.serial();
            separator = " ";
        }
        String matchesString = "";
        separator = "";
        for (DirValue match : this.matches) {
            matchesString += separator + match.toString();
            separator = " ";
        }
        return comparisonKind + "-" + valueComparator + "(" + tag + ") [" + matchesString + "] -> [" + blocks + "] | " + defaultTargetBlock.serial();
    }

    @Override
    public void acceptVisitor(DirVisitor visitor) {
        visitor.visitSwitch(this);
    }
}
