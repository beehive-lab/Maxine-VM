/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.max.graal.callanalysis;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.Map.Entry;

import com.oracle.max.graal.compiler.debug.*;
import com.oracle.max.graal.compiler.debug.BasicIdealGraphPrinter.Edge;
import com.oracle.max.graal.compiler.graphbuilder.*;
import com.oracle.max.graal.cri.*;
import com.oracle.max.graal.nodes.*;
import com.sun.cri.ci.*;
import com.sun.cri.ri.*;
import com.sun.max.program.*;

/**
 * Analyzes static calling relationships between methods, building a graph for the Ideal Graph Visualizer.
 */
public class StaticCallAnalyzer {

    private final GraalRuntime runtime;
    private final BasicIdealGraphPrinter printer;

    private Set<RiResolvedType> knownTypes;
    private Map<Class<?>, Set<Class<?>>> subtypeRelations;
    private Map<RiResolvedMethod, Integer> knownMethods;
    private Set<Edge> edges;

    private int nextNodeId;
    private int nextBlockId;

    private final String[] includePrefixes;
    private final ClassLoader loader;

    /**
     * Constructor for an analyzer that writes Ideal Graph Visualizer input to the specified stream, scans only classes
     * with the given prefixes and uses the specified classloader to load them (if necessary).
     */
    public StaticCallAnalyzer(OutputStream stream, String[] includePrefixes, ClassLoader loader) {
        this.printer = new BasicIdealGraphPrinter(stream);
        this.runtime = GraalRuntimeAccess.getGraalRuntime();
        this.includePrefixes = Arrays.copyOf(includePrefixes, includePrefixes.length);
        this.loader = loader;
    }

    /**
     * Start a new graph document consisting of a single group with the specified title.
     */
    public void start(String title) {
        printer.begin();
        printer.beginGroup();
        printer.beginProperties();
        printer.printProperty("name", title);
        printer.printProperty("origin", "Graal Call Graph");
        printer.endProperties();
        printer.beginMethod(title, title, -1);
        printer.endMethod();
    }

    /**
     * Starts a new graph with the specified title.
     */
    public void startGraph(String title) {
        if (subtypeRelations == null) {
            SubtypeDiscovery discovery = new SubtypeDiscovery(loader);
            for (String prefix : includePrefixes) {
                String path = prefix.replace('.', File.separatorChar);
                discovery.run(Classpath.fromSystem(), path);
            }
            subtypeRelations = discovery.getSubtypeRelations();
        }

        printer.beginGraph(title);
        printer.beginNodes();

        knownTypes = new HashSet<RiResolvedType>();
        knownMethods = new HashMap<RiResolvedMethod, Integer>();
        edges = new HashSet<Edge>();
        nextNodeId = 0;
        nextBlockId = 0;
    }

    /**
     * Analyzes the specified method of the specified class, and methods reachable from it, and adds corresponding
     * nodes, edges and blocks to the graph.
     */
    public void analyze(String className, String methodName) throws ClassNotFoundException {
        analyze(findMethod(className, methodName));
    }

    /**
     * Analyzes the specified method obtained by reflection, and methods reachable from it, and adds corresponding
     * nodes, edges and blocks to the graph.
     */
    public void analyze(Method method) {
        analyze(getRiMethod(method));
    }

    /**
     * Finishes the current graph started with {@link #startGraph(String)}.
     */
    public void endGraph() {
        printer.endNodes();

        printer.beginEdges();
        for (Edge edge : edges) {
            printer.printEdge(edge);
        }
        printer.endEdges();

        printer.beginControlFlow();
        for (RiResolvedType type : knownTypes) {
            printer.beginBlock(CiUtil.toJavaName(type, true));
            printer.beginBlockNodes();
            for (Entry<RiResolvedMethod, Integer> entry : knownMethods.entrySet()) {
                RiResolvedMethod method = entry.getKey();
                if (method.holder() == type) {
                    Integer nodeId = entry.getValue();
                    printer.printBlockNode(nodeId.toString());
                }
            }
            printer.endBlockNodes();
            printer.endBlock();
        }
        printer.endControlFlow();

        printer.endGraph();
    }

    /**
     * Finished the current graph document.
     */
    public void end() {
        printer.endGroup();
        printer.end();
    }

    private Integer analyze(RiResolvedMethod method) {
        Integer methodNodeId = knownMethods.get(method);
        if (methodNodeId != null) {
            // already analyzed earlier
            return methodNodeId;
        }

        methodNodeId = createNodeForMethod(method);

        final int flags = method.accessFlags();
        if (Modifier.isNative(flags)) {
            return methodNodeId;
        }

        if (includePrefixes != null) {
            boolean included = false;
            for (String pkg : includePrefixes) {
                if (CiUtil.internalNameToJava(method.holder().name(), true).startsWith(pkg)) {
                    included = true;
                    break;
                }
            }
            if (!included) {
                return methodNodeId;
            }
        }

        if (!Modifier.isPrivate(flags) && !Modifier.isStatic(flags) && !Modifier.isFinal(flags)) {
            analyzeOverrides(method, methodNodeId);
        }

        if (Modifier.isAbstract(flags)) {
            return methodNodeId;
        }

        StructuredGraph graph = buildGraph(method);
        for (Invoke invoke : graph.getInvokes()) {
            // TODO: Anonymous class instantiations
            RiResolvedMethod target = invoke.callTarget().targetMethod();
            Integer nodeId = analyze(target);
            if (nodeId != null) {
                edges.add(new Edge(methodNodeId.toString(), 0, nodeId.toString(), 0, "calls"));
            }
        }

        return methodNodeId;
    }

    private int createNodeForMethod(RiResolvedMethod method) {
        assert !knownMethods.containsKey(method);

        knownTypes.add(method.holder());

        int nodeId = nextNodeId++;
        knownMethods.put(method, nodeId);

        int flags = method.accessFlags();
        boolean leaf = (Modifier.isFinal(flags) || Modifier.isPrivate(flags) || Modifier.isStatic(flags));

        Map<String, String> properties = new HashMap<String, String>();
        properties.put("idx", Integer.toString(nodeId));
        properties.put("name", CiUtil.format("%n(%p): %r", method));
        properties.put("qualifiedName", CiUtil.format("%n(%P): %R", method));
        properties.put("class", CiUtil.format("%h", method));
        properties.put("qualifiedClass", CiUtil.format("%H", method));
        properties.put("native", Modifier.isNative(flags) ? "1" : "0");
        properties.put("abstract", Modifier.isAbstract(flags) ? "1" : "0");
        properties.put("leaf", leaf ? "1" : "0");
        printer.printNode(Integer.toString(nodeId), properties);

        return nodeId;
    }

    private void analyzeOverrides(RiResolvedMethod method, Integer methodNodeId) {
        Class<?> clazz = method.holder().toJava();
        if (!subtypeRelations.containsKey(clazz)) {
            return;
        }

        // Build signature for reflection lookup
        RiSignature signature = method.signature();
        int nargs = signature.argumentCount(false);
        Class<?>[] arguments = new Class<?>[nargs];
        for (int i = 0; i < arguments.length; i++) {
            Class< ? > type;
            RiType ritype = signature.argumentTypeAt(i, null);
            type = getClassForType(ritype);
            arguments[i] = type;
        }

        analyzeOverrides(method, methodNodeId, arguments);
    }

    private void analyzeOverrides(RiResolvedMethod method, Integer methodNodeId, Class<?>[] arguments) {
        Class<?> clazz = method.holder().toJava();
        Collection<Class<?>> subtypes = subtypeRelations.get(clazz);
        if (subtypes == null) {
            return;
        }

        for (Class<?> subtype : subtypes) {
            try {
                RiResolvedMethod overridden = getRiMethod(subtype.getDeclaredMethod(method.name(), arguments));
                Integer nodeId = analyze(overridden);
                if (nodeId != null) {
                    String label = "overrides";
                    if (method.holder().isInterface()) {
                        label = "implements";
                    }
                    edges.add(new Edge(methodNodeId.toString(), 1, nodeId.toString(), 1, label));

                    analyzeOverrides(overridden, nodeId, arguments);
                }
            } catch (NoSuchMethodException e) {
                // ignore
            }
        }
    }

    private Class<?> getClassForType(RiType riType) {
        Class<?> clazz;
        if (riType instanceof RiResolvedType) {
            clazz = ((RiResolvedType) riType).toJava();
        } else {
            CiKind kind = riType.kind(false);
            if (kind != CiKind.Object) {
                clazz = kind.toJavaClass();
            } else {
                String name;
                if (riType.name().charAt(0) == '[') {
                    // Array: Class.forName() expects names such as: [Ljava.lang.String;
                    name = riType.name().replace('/', '.');
                } else {
                    name = CiUtil.toJavaName(riType);
                }
                try {
                    clazz = Class.forName(name, false, getClass().getClassLoader());
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return clazz;
    }

    private Method findMethod(String className, String methodName) throws ClassNotFoundException {
        Class<?> clazz = Class.forName(className);

        Method found = null;
        for (Method method : clazz.getDeclaredMethods()) {
            if (method.getName().equals(methodName)) {
                assert (found == null);
                found = method;
            }
        }
        return found;
    }

    private RiResolvedMethod getRiMethod(Method method) {
        if (method == null) {
            return null;
        }
        return runtime.getRiMethod(method);
    }

    private StructuredGraph buildGraph(RiResolvedMethod riMethod) {
        StructuredGraph graph = new StructuredGraph();
        new GraphBuilderPhase(runtime, riMethod, null, new GraphBuilderConfiguration(false, true, null)).apply(graph);
        return graph;
    }

}
