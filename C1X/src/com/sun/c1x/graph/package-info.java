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
package com.sun.c1x.graph;

/**
 * @author Ben Titzer
 * @author Mick Jordan
 *
 *         IR Graph building.
 *
 *         The {@link IR} class drives the generation of the HIR graph for a method, making use of other utility classes
 *         in this package.
 *
 *         The graph building is separated into a basic build phase ({@link IR#buildGraph} method)and (currently) two
 *         optimization phases ({@link IR#optimize1} and {@link IR#optimize2}) although the basic phase also does some (basic)
 *         optimizations.
 *
 *         {@link IR#buildGraph} creates an {@link IRScope} object, that represents a context for inlining, and then invokes the
 *         constructor for the {@link GraphBuilder} class, passing the {@link C1XCompilation}, {@link IRScope} and {@link IR} instances.
 *         Uncharacteristically, the constructor actually does the work, that is, there is no {@code GraphBuilder.build}
 *         method. The following support objects are created in the constructor:
 *
 *         <ul>
 *         <li>memoryMap: an instance of {@link MemoryMap}
 *         <li>localValueMap: an instance of {@link ValueMap}
 *         <li>canonicalizer: an instance of {@link Canonicalizer}
 *         </ul>
 *
 *         The {@link startBlock} field of the {@link IR} instance is set to a newly created {@link BlockBegin} node, with bytecode
 *         index 0 and then the {@link BlockMap} is constructed by calling {@link C1XCompilation#getBlockMap}. This behaves
 *         slightly differently depending on whether this is an OSR compilation. If so, a new {@link BlockBegin} node is
 *         added to the map at the OSR bytecode index. The map is then built by the {@link BlockMap#build} method, which
 *         takes a boolean argument that controls whether a second pass is made over the bytecodes to compute stores in
 *         loops. This always false for an OSR compilation (why?). Otherwise, it is only true if enabled by the
 *         {@link C1XOptions#PhiLoopStores} compilation option.
 *
 *         On return some unneeded state from the map is removed by the {@link BlockMap#cleanup} method, and the stats are
 *         updated.
 *
 *         Next the {@link pushRootScope} method is called, with the passed-in {@link IRScope} object, {@link BlockMap} returned by
 *         build and the {@link ir.startBlock}. (Note: Unlike {@link pushScope}, this method does not propagate the
 *         {@link storeInLoops} field of the {@link BlockMap} to the {@link IRScope} object. This means that
 *         {@link BlockBegin.insertLoopPhis} will always get null for this value. Is this a bug?).
 *
 *         This initializes the {@link scopeData} field with a {@link ScopeData} instance, with null parent. The
 *         {@link compilation.runtime} instance is called to get an {@link RiConstantPool}, which is C1X's interface to constant
 *         pool information. The {@link curBlock} field is set to the start block.
 *
 *         Now a {@link FrameState} object is created by {@link stateAtEntry}. If the method is not static, then a {@link Local}
 *         instance is created at index 0. Since the receiver cannot be {@link null}, the {@link Value.Flag.NonNull} flag is
 *         set. Additional {@link Local} instances are created for the arguments to the method. The index is incremented by
 *         the number of slots occupied by the {@link CiKind} corresponding to the argument type. All the Local instances
 *         are stored in the {@link FrameState} using the {@link FrameState.storeLocal} method. This {@link FrameState} is then
 *         merged into the {@link stateBefore} for the {@link startBlock}, which just results in a copy since {@link stateBefore}
 *         will be {@link null}.
 *
 *         Step 3 sets up three instance fields: {@link curBlock} and {@link lastInstr} to {@link startBlock} and {@link curState} to
 *         {@link initialState}. (N.B. the setting of {@link curBlock} is redundant as it is done in {@link pushScope}).
 *
 *         Step 4 contains special handling for synchronized methods (TBD), otherwise it calls {@link finishStartBlock}
 *         which adds a {@link Base} block as the end of the {@link startBlock}. The {@link Base} block has one successor set to the
 *         {@link StdEntry} block, which was created by {@link BlockMap.build} (and possibly a successor to an OSREntry block).
 *
 *         Then the {@link IRScope.lockStackSize} is computed. (TBD)
 *
 *         Then the method is checked for being intrinsic, i.e., one that has a hard-wired implementation known to C1X.
 *         If so, and {@link C1XOptions#OptIntrinsify} is set, an attempt is made to inline it (TBD). Otherwise, or if the
 *         intrinsification fails, normal processing continues by adding the {@link stdEntry} block to the {@link ScopeData} work
 *         list (kept topologically sorted) and calling {@link iterateAllBlocks}.
 *
 *         {@link iterateAllBlocks} repeatedly removes a block from the work list and, if not already visited, marks it so,
 *         kills the current memory map, sets {@link curBlock}, {@link curState} and {@link lastInstr} and then calls
 *         {@link iterateBytecodesForBlock}.
 *
 *         {@link iterateBytecodesForBlock} performs an abstract interpretation of the bytecodes in the block, appending new
 *         nodes as necessary, until the last added node is an instance of {@link BlockEnd}. (Note: It has an explicit check
 *         for finding a new {@link BlockBegin} before a {@link BlockEnd} but {@link BlockMap.moveSuccessorlists} has a similar
 *         check so this may be redundant). For example, consider the following bytecodes:
 *         <pre><code>
 *         0: iconst_0
 *         1: istore_2
 *         2: goto 22
 *         <code></pre>
 *
 *         The {@code iconst_0} bytecode causes a {@link Constant} node representing zero to be pushed on the {@link curState} stack
 *         and the node to be appended to the {@link BlockBegin} node associated with index 0. The {@code istore_2} causes the
 *         node to be popped of the stack and stored in the local slot 2. No IR node is generated for the {@code istore_2}. The
 *         {@link goto} creates a {@link Goto} node which is a subclass of {@link BlockEnd}, so this terminates the iteration. As
 *         part of termination the{@link  Goto} node is marked as the end node of the current block and the{@link FrameState} is
 *         propagated to the successor node(s) by merging any existing {@link FrameState} with the current state. If the
 *         target is a loop header node this involves inserting {@link Phi} nodes. Finally, the target node is added to the
 *         {@link scopeData} work list.
 *
 *         This process continues until all the blocks have been visited (processed) after which control returns to
 *         {@link IR.buildGraph}.
 *
 */
