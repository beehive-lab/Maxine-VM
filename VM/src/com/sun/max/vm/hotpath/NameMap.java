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
/*VCSID=db2923ff-239e-48a3-a79f-261f7ab008ed*/
package com.sun.max.vm.hotpath;

import com.sun.max.collect.*;
import com.sun.max.lang.*;
import com.sun.max.vm.compiler.tir.*;
import com.sun.max.vm.compiler.tir.pipeline.*;
import com.sun.max.vm.hotpath.compiler.Console.*;

public class NameMap {
    public static final Color CONSTANT_COLOR = Color.LIGHTTEAL;
    public static final Color LOCAL_COLOR = Color.LIGHTGREEN;
    public static final Color NESTED_LOCAL_COLOR = Color.LIGHTMAGENTA;

    public static final MapFunction<TirInstruction, String> COMPACT = new MapFunction<TirInstruction, String>() {
        @Override
        public String map(TirInstruction from) {
            final SingleResult<String> result = new SingleResult<String>();
            from.accept(new TirInstructionAdapter() {
                @Override
                public void visit(TirLocal local) {
                    result.setValue(Color.color(NameMap.LOCAL_COLOR, local.kind().typeDescriptor() + "$" + local.slot()));
                }

                @Override
                public void visit(TirNestedLocal local) {
                    result.setValue(Color.color(NameMap.NESTED_LOCAL_COLOR, "@" + local.slot()));
                }

                @Override
                public void visit(TirConstant constant) {
                    result.setValue(Color.color(CONSTANT_COLOR, "#" + constant.value().toString()));
                }

                @Override
                public void visit(TirBuiltinCall call) {
                    result.setValue(call.builtin().classMethodActor().name().toString());
                }

                @Override
                public void visit(TirDirCall call) {
                    result.setValue(call.method().name().toString());
                }

                @Override
                public void visit(TirInstruction instruction) {
                    result.setValue(instruction.toString());
                }
            });
            return result.value();
        }
    };

    private static IdentityHashMapping<TirTree, String> _treeNameMap = new IdentityHashMapping<TirTree, String>();

    public static String nameOf(TirTree tree) {
        String name = _treeNameMap.get(tree);
        if (name == null) {
            final int number = _treeNameMap.length();
            if (number < 'Z' - 'A') {
                name = String.valueOf((char) ('A' + number));
            } else {
                name = "T:" + number;
            }
            _treeNameMap.put(tree, name);
        }
        return name;
    }

    private static IdentityHashMapping<TirTree, MapFunction<TirInstruction, String>> _treeLabelMap = new IdentityHashMapping<TirTree, MapFunction<TirInstruction, String>>();

    public static MapFunction<TirInstruction, String> labelMap(TirTree tree) {
        return _treeLabelMap.get(tree);
    }

    public static void updateLabelMap(TirTree tree, MapFunction<TirInstruction, String> labelMap) {
        _treeLabelMap.put(tree, labelMap);
    }

    public static String nameOf(TirTree tree, TirInstruction instruction) {
        final MapFunction<TirInstruction, String> map = NameMap.labelMap(tree);
        if (map != null) {
            return map.map(instruction);
        }
        return instruction.toString();
    }
}
