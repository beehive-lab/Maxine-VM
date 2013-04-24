/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.max.vm.ext.graal;

import static com.sun.max.vm.intrinsics.MaxineIntrinsicIDs.*;
import static com.oracle.max.cri.intrinsics.IntrinsicIDs.*;
import static com.oracle.max.vm.ext.graal.MaxWordTypeRewriterPhase.checkWord;

import java.lang.reflect.*;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.*;

import com.oracle.graal.api.code.*;
import com.oracle.graal.api.meta.*;
import com.oracle.graal.graph.*;
import com.oracle.graal.nodes.*;
import com.oracle.graal.nodes.calc.*;
import com.oracle.graal.nodes.extended.*;
import com.oracle.graal.nodes.spi.*;
import com.oracle.graal.nodes.type.*;
import com.oracle.graal.replacements.*;
import com.oracle.max.vm.ext.graal.nodes.*;
import com.oracle.max.vm.ext.graal.snippets.*;
import com.sun.cri.ri.*;
import com.sun.max.annotate.*;
import com.sun.max.program.*;
import com.sun.max.vm.actor.member.*;

public class MaxIntrinsics {

    static class NotImplementedIntrinsic extends MaxIntrinsicImpl {
        @Override
        public ValueNode createGraph(StructuredGraph graph, ResolvedJavaMethod method, NodeList<ValueNode> args) {
            throw new UnsupportedOperationException("intrinsic not implemented");
        }
    }

    static class PointerReadOffsetIntrinsic extends MaxIntrinsicImpl {
        public ValueNode create(StructuredGraph graph, ResolvedJavaMethod method, ValueNode pointer, ValueNode offset) {
            return graph.add(new UnsafeLoadNode(stampFor((ResolvedJavaType) method.getSignature().getReturnType(method.getDeclaringClass())),
                            pointer, 0, offset, checkWord(method.getSignature().getReturnType(null))));
        }
    }

    static class PointerWriteOffsetIntrinsic extends MaxIntrinsicImpl {
        public ValueNode create(StructuredGraph graph, ResolvedJavaMethod method, ValueNode pointer, ValueNode offset, ValueNode value) {
            Signature sig = method.getSignature();
            Kind dataKind = sig.getParameterKind(sig.getParameterCount(false) - 1);
            return graph.add(new UnsafeStoreNode(pointer, 0, offset, value, dataKind));
        }
    }

    static class PointerReadIndexIntrinsic extends MaxIntrinsicImpl {
        public ValueNode create(StructuredGraph graph, ResolvedJavaMethod method, ValueNode pointer, ValueNode displacement, ValueNode index) {
            Signature sig = method.getSignature();
            Kind dataKind = sig.getReturnKind();
            ValueNode scaledIndex = scaledIndex(graph, dataKind, index);
            return graph.add(ExtendedUnsafeLoadNode.create(stampFor((ResolvedJavaType) method.getSignature().getReturnType(method.getDeclaringClass())), pointer,
                            displacement, scaledIndex, checkWord(method.getSignature().getReturnType(null))));
        }
    }

    static class PointerWriteIndexIntrinsic  extends MaxIntrinsicImpl {
        public ValueNode create(StructuredGraph graph, ResolvedJavaMethod method, ValueNode pointer, ValueNode displacement, ValueNode index, ValueNode value) {
            Signature sig = method.getSignature();
            Kind dataKind = sig.getParameterKind(sig.getParameterCount(false) - 1);
            ValueNode scaledIndex = scaledIndex(graph, dataKind, index);
            return graph.add(ExtendedUnsafeStoreNode.create(pointer, displacement, scaledIndex, value, dataKind));
        }
    }

    /**
     * Have to convert {@code index} into an offset by scaling by size of {@code dataKind}.
     */
    private static ValueNode scaledIndex(StructuredGraph graph, Kind dataKind, ValueNode index) {
        int kindSizeAsShift = kindSizeAsShift(dataKind);
        if (kindSizeAsShift == 0) {
            return index;
        } else {
            LeftShiftNode result = new LeftShiftNode(Kind.Int, index, ConstantNode.forInt(kindSizeAsShift, graph));
            graph.add(result);
            return result;
        }
    }

    static class PointerCompareAndSwapIntrinsic extends MaxIntrinsicImpl {
        public ValueNode create(StructuredGraph graph, ValueNode pointer, ValueNode offset, ValueNode expectedValue, ValueNode newValue) {
            return graph.add(new MaxCompareAndSwapNode(pointer, 0, offset, expectedValue, newValue));
        }
    }

    private static int kindSizeAsShift(Kind kind) {
        // Checkstyle: stop
        switch (kind) {
            case Byte: return 0;
            case Short: case Char: return 1;
            case Int: case Float: return 2;
            case Long: case Double: case Object: return 3;
            default: assert false; return -1;
        // Checkstyle: stop
        }
    }

    public static Stamp stampFor(ResolvedJavaType resolvedJavaType) {
        if (resolvedJavaType.isPrimitive()) {
            return StampFactory.forKind(resolvedJavaType.getKind());
        } else {
            return StampFactory.declared(resolvedJavaType);
        }
    }

    static class SafepointIntrinsic extends MaxIntrinsicImpl {
        private MaxSafepointNode.Op op;

        public SafepointIntrinsic(MaxSafepointNode.Op op) {
            this.op = op;
        }

        public ValueNode create(StructuredGraph graph) {
            return graph.add(new MaxSafepointNode(op));
        }
    }

    static class ReadRegisterIntrinsic extends MaxIntrinsicImpl {
        public ValueNode create(StructuredGraph graph, ResolvedJavaMethod method, int registerId) {
            RegisterConfig registerConfig = MaxGraal.runtime().lookupRegisterConfig();
            Register register = registerConfig.getRegisterForRole(registerId);
            if (register == null) {
                throw new GraalInternalError("Unsupported READREG operand " + registerId);
            }
            MaxReadRegisterNode readRegister = graph.add(new MaxReadRegisterNode(register, (ResolvedJavaType) method.getSignature().getReturnType(null)));
            return readRegister;
        }
    }

    static class WriteRegisterIntrinsic extends MaxIntrinsicImpl {
        public ValueNode create(StructuredGraph graph, ResolvedJavaMethod method, int registerId, ValueNode value) {
            RegisterConfig registerConfig = MaxGraal.runtime().lookupRegisterConfig();
            Register register = registerConfig.getRegisterForRole(registerId);
            if (register == null) {
                throw new GraalInternalError("Unsupported WRITEREG operand " + registerId);
            }
            MaxWriteRegisterNode writeRegister = graph.add(new MaxWriteRegisterNode(register, value));
            return writeRegister;
        }
    }

    static class UnsafeCastIntrinsic extends MaxIntrinsicImpl {
        public ValueNode create(StructuredGraph graph, ResolvedJavaMethod method, ValueNode value) {
            JavaType toType = method.getSignature().getReturnType(method.getDeclaringClass());
            if (!(toType instanceof ResolvedJavaType)) {
                throw new GraalInternalError("Cannot unsafe cast to an unresolved type: %s", toType);
            }

            UnsafeCastNode unsafeCastNode = graph.add(new MaxUnsafeCastNode(value, (ResolvedJavaType) toType));
            return unsafeCastNode;
        }
    }

    static class AllocaIntrinsic extends MaxIntrinsicImpl {
        public ValueNode create(StructuredGraph graph, ResolvedJavaMethod method, int size, boolean refs) {
            return graph.add(new AllocaNode(size, refs, stampFor((ResolvedJavaType) method.getSignature().getReturnType(method.getDeclaringClass()))));
        }
    }

    static class BitIntrinsic extends MaxIntrinsicImpl {
        private final boolean forward;

        BitIntrinsic(boolean forward) {
            this.forward = forward;
        }

        public ValueNode create(StructuredGraph graph, ResolvedJavaMethod method, ValueNode value) {
            FloatingNode node = graph.unique(new MaxBitScanNode(value, forward));
            return node;
        }
    }

    /**
     * Case where a Graal snippet with exactly the same signature already exists, so we simply
     * return the graph for that.
     */
    static class GraalSnippetIntrinsic extends MaxIntrinsicImpl {
        private ResolvedJavaMethod maxMethod;
        private StructuredGraph graalGraph;

        GraalSnippetIntrinsic(ResolvedJavaMethod maxMethod) {
            this.maxMethod = maxMethod;
            this.graalGraph = (StructuredGraph) maxMethod.getCompilerStorage().get(Graph.class);
        }

        @Override
        public Object createGraph(StructuredGraph graph, ResolvedJavaMethod method, NodeList<ValueNode> args) {
            return graalGraph;
        }
    }

    /**
     * This is a vehicle for testing a snippet instantiation in hosted mode.
     */
    private static class TestIntrinsic1 extends MaxIntrinsicImpl {
        public ValueNode create(StructuredGraph graph, ResolvedJavaMethod method, ValueNode arg1) {
            return TestSnippets.createTestSnippet1(graph, method, arg1);
        }
    }
    private static class TestIntrinsic2 extends MaxIntrinsicImpl {
        public ValueNode create(StructuredGraph graph, ResolvedJavaMethod method, ValueNode arg1, ValueNode arg2) {
            return TestSnippets.createTestSnippet2(graph, method, arg1, arg2);
        }
    }

    /**
     * Registry that maps intrinsic ID strings to implementation objects.
     * Intrinsic ID strings can either be explicitly defined as String constants, or inferred from the
     * fully qualified name and signature of a method.
     */
    public static class Registry implements Iterable<Map.Entry<String, MaxIntrinsicImpl>> {
        private Map<String, MaxIntrinsicImpl> implRegistry = new ConcurrentHashMap<String, MaxIntrinsicImpl>(100, 0.75f, 1);

        /**
         * Add an implementation object for an explicitly defined intrinsic ID string.
         */
        public void add(String intrinsicId, MaxIntrinsicImpl impl) {
            assert !implRegistry.containsKey(intrinsicId);
            implRegistry.put(intrinsicId, impl);
        }

        /**
         * Add an implementation object for an intrinsic implicitly defined by its fully qualified name and signature.
         */
        public void add(RiMethod riMethod, MaxIntrinsicImpl impl) {
            add(literalId(riMethod), impl);
        }

        /**
         * Gets the implementation object for a method. First, the {@link RiMethod#intrinsic() explicit ID string} of the
         * method is searched in the registry, then the implicit ID inferred from the method name and signature.
         * @return The intrinsic implementation object, or {@code null} if none is found.
         */
        public MaxIntrinsicImpl get(MaxResolvedJavaMethod method) {
            String intrinsic = method.riResolvedMethod().intrinsic();
            if (intrinsic != null) {
                MaxIntrinsicImpl impl = implRegistry.get(intrinsic);
                if (impl != null) {
                    return impl;
                }
            }
            return implRegistry.get(literalId(method.riMethod));
        }


        private static String literalId(RiMethod method) {
            return method.holder().name() + method.name() + method.signature().asString();
        }

        @Override
        public Iterator<Entry<String, MaxIntrinsicImpl>> iterator() {
            return implRegistry.entrySet().iterator();
        }
    }

    private static Registry registry;

    static Registry getRegistry() {
        assert registry != null;
        return registry;
    }

    public static void initialize(MaxRuntime runtime, Replacements replacements, MaxTargetDescription target) {
        registry = new Registry();

        registry.add(TEST_SNIPPET_1, new TestIntrinsic1()); // TODO remove when debugged
        registry.add(TEST_SNIPPET_2, new TestIntrinsic2()); // TODO remove when debugged

        registry.add(LSB, new BitIntrinsic(false));
        registry.add(MSB, new BitIntrinsic(true));

        registry.add(UNSAFE_CAST, new UnsafeCastIntrinsic());
        registry.add(READREG, new ReadRegisterIntrinsic());
        registry.add(WRITEREG, new WriteRegisterIntrinsic());
        /*
        registry.add(IFLATCHBITREAD, new NotImplementedIntrinsic());
*/
        registry.add(PREAD_OFF, new PointerReadOffsetIntrinsic());
        registry.add(PREAD_IDX, new PointerReadIndexIntrinsic());
        registry.add(PWRITE_OFF, new PointerWriteOffsetIntrinsic());
        registry.add(PWRITE_IDX, new PointerWriteIndexIntrinsic());
        registry.add(PCMPSWP, new PointerCompareAndSwapIntrinsic());

        registry.add(SAFEPOINT_POLL, new SafepointIntrinsic(MaxSafepointNode.Op.SAFEPOINT_POLL));
        registry.add(INFO, new SafepointIntrinsic(MaxSafepointNode.Op.INFO));
        registry.add(HERE, new SafepointIntrinsic(MaxSafepointNode.Op.HERE));
        registry.add(ALLOCA, new AllocaIntrinsic());
/*
        registry.add(UNCOMMON_TRAP, new UncommonTrapIntrinsic());

        registry.add(PAUSE, new SafepointIntrinsic(SafepointNode.Op.PAUSE));
        registry.add(BREAKPOINT_TRAP, new SafepointIntrinsic(SafepointNode.Op.BREAKPOINT));

        registry.add(CMP_BYTECODE, new NormalizeCompareIntrinsic());
*/
        mapUnsignedMathIntrinsics(replacements);
    }

    /**
     * Map all cri.UnsignedMath intrinsics to appropriate Graal instrinsic.
     */
    private static void mapUnsignedMathIntrinsics(Replacements replacements) {
        new GraalMethodSubstitutions().registerReplacements(replacements);

        Method[] methods = com.oracle.max.cri.intrinsics.UnsignedMath.class.getDeclaredMethods();
        for (Method m : methods) {
            INTRINSIC intrinsic = m.getAnnotation(INTRINSIC.class);
            if (intrinsic != null) {
                String graalMethodName = null;
                switch (intrinsic.value()) {
                    case UCMP_AT:
                        graalMethodName = "aboveThan";
                        break;
                    case UCMP_AE:
                        graalMethodName = "aboveOrEqual";
                        break;
                    case UCMP_BT:
                        graalMethodName = "belowThan";
                        break;
                    case UCMP_BE:
                        graalMethodName = "belowOrEqual";
                        break;
                    case UDIV:
                        graalMethodName = "divide";
                        break;
                    case UREM:
                        graalMethodName = "remainder";
                        break;
                }
                ResolvedJavaMethod graalMethod = graalSnippet(com.oracle.graal.api.code.UnsignedMath.class, graalMethodName, m);
                StructuredGraph graph = replacements.getMethodSubstitution(graalMethod);
                MaxResolvedJavaMethod maxMethod = MaxResolvedJavaMethod.get(MethodActor.fromJava(m));
                maxMethod.getCompilerStorage().put(Graph.class, graph);
                registry.add(maxMethod.riMethod, new GraalSnippetIntrinsic(maxMethod));
            }
        }

    }

    private static MaxResolvedJavaMethod graalSnippet(Class<?> graalClass, String graalMethodName, Method maxMethod) {
        Method[] methods = graalClass.getDeclaredMethods();
        for (Method m : methods) {
            if (match(m, graalMethodName, maxMethod)) {
                MaxResolvedJavaMethod rm = MaxResolvedJavaMethod.get(MethodActor.fromJava(m));
                assert rm != null;
                return rm;
            }
        }
        ProgramError.unexpected("failed to find snippet graph for " + maxMethod.getName());
        return null;
    }

    private static boolean match(Method graalMethod, String graalMethodName, Method maxMethod) {
        if (graalMethod.getName().equals(maxMethod.getName())) {
            Class<?>[] graalParams = graalMethod.getParameterTypes();
            Class<?>[] maxParams = maxMethod.getParameterTypes();
            for (int i = 0; i < graalParams.length; i++) {
                if (graalParams[i] != maxParams[i]) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

}
