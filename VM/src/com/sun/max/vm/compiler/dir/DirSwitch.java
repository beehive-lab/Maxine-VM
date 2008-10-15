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
package com.sun.max.vm.compiler.dir;

import java.util.*;

import com.sun.max.vm.compiler.dir.transform.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;

/**
 * The one and only conditional control flow instruction.
 *
 * @author Bernd Mathiske
 */
public class DirSwitch extends DirInstruction {

    private final Kind _comparisonKind;
    private final ValueComparator _valueComparator;
    private final DirValue _tag;
    private final DirValue[] _matches;
    private final DirBlock[] _targetBlocks;
    private DirBlock _defaultTargetBlock;

    public DirSwitch(Kind comparisonKind, ValueComparator valueComparator, DirValue tag, DirValue[] matches, DirBlock[] targetBlocks, DirBlock defaultTargetBlock) {
        super();
        _comparisonKind = comparisonKind;
        _valueComparator = valueComparator;
        _tag = tag;
        _matches = matches;
        _targetBlocks = targetBlocks;
        _defaultTargetBlock = defaultTargetBlock;
    }

    public ValueComparator valueComparator() {
        return _valueComparator;
    }

    public Kind comparisonKind() {
        return _comparisonKind;
    }

    public DirValue tag() {
        return _tag;
    }

    public DirValue[] matches() {
        return _matches;
    }

    public DirBlock[] targetBlocks() {
        return _targetBlocks;
    }

    public DirBlock defaultTargetBlock() {
        return _defaultTargetBlock;
    }

    public void setDefaultTargetBlock(DirBlock block) {
        _defaultTargetBlock = block;
    }

    @Override
    public void substituteBlocks(Map<DirBlock, DirBlock> blockMap) {
        for (int i = 0; i < _targetBlocks.length; i++) {
            final DirBlock targetBlock = _targetBlocks[i];
            final DirBlock block = blockMap.get(targetBlock);
            if (block != null && block != targetBlock) {
                _targetBlocks[i] = block;
            }
        }
        final DirBlock block = blockMap.get(_defaultTargetBlock);
        if (block != null && block != _defaultTargetBlock) {
            _defaultTargetBlock = block;
        }
    }

    @Override
    public boolean isEquivalentTo(DirInstruction other, DirBlockEquivalence dirBlockEquivalence) {
        if (other instanceof DirSwitch) {
            final DirSwitch dirSwitch = (DirSwitch) other;
            if (!(_comparisonKind == dirSwitch._comparisonKind && _valueComparator == dirSwitch._valueComparator &&
                  _matches.length == dirSwitch._matches.length && _tag.equals(dirSwitch._tag))) {
                return false;
            }
            for (int i = 0; i < _matches.length; i++) {
                if (!_matches[i].equals(dirSwitch._matches[i])) {
                    return false;
                }
            }
            for (int i = 0; i < _matches.length; i++) {
                if (!_targetBlocks[i].isEquivalentTo(dirSwitch._targetBlocks[i], dirBlockEquivalence)) {
                    return false;
                }
            }
            return _defaultTargetBlock.isEquivalentTo(dirSwitch._defaultTargetBlock, dirBlockEquivalence);
        }
        return false;
    }

    @Override
    public int hashCodeForBlock() {
        return super.hashCodeForBlock() ^ _tag.hashCodeForBlock();
    }

    @Override
    public String toString() {
        String blocks = "";
        String separator = "";
        for (DirBlock block : _targetBlocks) {
            blocks += separator + block.serial();
            separator = " ";
        }
        String matches = "";
        separator = "";
        for (DirValue match : _matches) {
            matches += separator + match.toString();
            separator = " ";
        }
        return _comparisonKind + "-" + _valueComparator + "(" + _tag + ") [" + matches + "] -> [" + blocks + "] | " + _defaultTargetBlock.serial();
    }

    @Override
    public void acceptVisitor(DirVisitor visitor) {
        visitor.visitSwitch(this);
    }
}
