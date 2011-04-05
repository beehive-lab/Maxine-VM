/*
 * Copyright (c) 2007, 2009, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
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
