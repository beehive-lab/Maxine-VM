/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.max.jdwp.generate;

import java.io.*;
import java.util.*;

/**
 * @author JDK7: jdk/make/tools/src/build/tools/jdwpgen
 */
class Parse {

    final StreamTokenizer izer;
    final Map<String, Node> kindMap = new HashMap<String, Node>();

    Parse(Reader reader) {
        izer = new StreamTokenizer(new BufferedReader(reader));
        izer.resetSyntax();
        izer.slashStarComments(true);
        izer.slashSlashComments(true);
        izer.wordChars('a', 'z');
        izer.wordChars('A', 'Z');
        izer.wordChars('0', '9');
        izer.wordChars('_', '_');
        izer.wordChars('-', '-');
        izer.wordChars('.', '.');
        izer.whitespaceChars(0, 32);
        izer.quoteChar('"');
        izer.quoteChar('\'');

        kindMap.put("CommandSet", new CommandSetNode());
        kindMap.put("Command", new CommandNode());
        kindMap.put("Out", new OutNode());
        kindMap.put("Reply", new ReplyNode());
        kindMap.put("ErrorSet", new ErrorSetNode());
        kindMap.put("Error", new ErrorNode());
        kindMap.put("Event", new EventNode());
        kindMap.put("Repeat", new RepeatNode());
        kindMap.put("Group", new GroupNode());
        kindMap.put("Select", new SelectNode());
        kindMap.put("Alt", new AltNode());
        kindMap.put("ConstantSet", new ConstantSetNode());
        kindMap.put("Constant", new ConstantNode());
        kindMap.put("int", new SimpleTypeNode("int", "int", "ps.readInt()"));
        kindMap.put("long", new SimpleTypeNode("long", "long", "ps.readLong()"));
        kindMap.put("boolean", new SimpleTypeNode("boolean", "boolean", "ps.readBoolean()"));

        kindMap.put("object", new SimpleIDTypeNode("ObjectID"));
        kindMap.put("threadObject", new SimpleIDTypeNode("ThreadID"));
        kindMap.put("threadGroupObject", new SimpleIDTypeNode("ThreadGroupID"));
        kindMap.put("arrayObject", new SimpleIDTypeNode("ArrayID"));
        kindMap.put("stringObject", new SimpleIDTypeNode("StringID"));
        kindMap.put("classLoaderObject", new SimpleIDTypeNode("ClassLoaderID"));
        kindMap.put("classObject", new SimpleIDTypeNode("ClassObjectID"));
        kindMap.put("referenceType", new SimpleIDTypeNode("ReferenceTypeID"));
        kindMap.put("referenceTypeID", new SimpleIDTypeNode("ReferenceTypeID"));
        kindMap.put("classType", new SimpleIDTypeNode("ClassID"));
        kindMap.put("interfaceType", new SimpleIDTypeNode("InterfaceID"));
        kindMap.put("arrayType", new SimpleIDTypeNode("ArrayTypeID"));
        kindMap.put("method", new SimpleIDTypeNode("MethodID"));
        kindMap.put("field", new SimpleIDTypeNode("FieldID"));
        kindMap.put("frame", new SimpleIDTypeNode("FrameID"));
        kindMap.put("referenceTypeID", new SimpleIDTypeNode("ReferenceTypeID"));

        kindMap.put("string", new SimpleTypeNode("string", "String", "ps.readString()"));
        kindMap.put("value", new SimpleTypeNode("value", "JDWPValue", "ps.readValue()"));
        kindMap.put("byte", new SimpleTypeNode("byte", "byte", "ps.readByte()"));
        kindMap.put("location", new SimpleTypeNode("location", "JDWPLocation", "ps.readLocation()"));
        kindMap.put("tagged-object", new TaggedObjectTypeNode());
        kindMap.put("typed-sequence", new SimpleTypeNode("arrayregion", "java.util.List<? extends JDWPValue>", "ps.readArrayRegion()"));
        kindMap.put("untagged-value", new UntaggedValueTypeNode());
    }

    RootNode items() throws IOException {
        final List<Node> list = new ArrayList<Node>();

        while (izer.nextToken() != StreamTokenizer.TT_EOF) {
            izer.pushBack();
            list.add(item());
        }
        final RootNode node = new RootNode();
        node.set("Root", list, 1);
        return node;
    }

    Node item() throws IOException {
        switch (izer.nextToken()) {
            case StreamTokenizer.TT_EOF:
                error("Unexpect end-of-file");
                return null;

            case StreamTokenizer.TT_WORD: {
                final String name = izer.sval;
                if (izer.nextToken() == '=') {
                    final int ntok = izer.nextToken();
                    if (ntok == StreamTokenizer.TT_WORD) {
                        return new NameValueNode(name, izer.sval);
                    } else if (ntok == '\'') {
                        return new NameValueNode(name, izer.sval.charAt(0));
                    } else {
                        error("Expected value after: " + name + " =");
                        return null;
                    }
                }
                izer.pushBack();
                return new NameNode(name);
            }

            case '"':
                return new CommentNode(izer.sval);

            case '(': {
                if (izer.nextToken() == StreamTokenizer.TT_WORD) {
                    final String kind = izer.sval;
                    final List<Node> list = new ArrayList<Node>();

                    while (izer.nextToken() != ')') {
                        izer.pushBack();
                        list.add(item());
                    }
                    final Node proto = kindMap.get(kind);
                    if (proto == null) {
                        error("Invalid kind: " + kind);
                        return null;
                    }
                    try {

                        if (proto instanceof SimpleTypeNode) {
                            final Node node = ((SimpleTypeNode) proto).copy();
                            node.set(kind, list, izer.lineno());
                            return node;
                        }
                        final Node node = proto.getClass().newInstance();
                        node.set(kind, list, izer.lineno());
                        return node;
                    } catch (Exception exc) {
                        error(exc.toString());
                        return null;
                    }
                }
                error("Expected kind identifier, got " + izer.ttype + " : " + izer.sval);
                return null;
            }

            default:
                error("Unexpected character: '" + (char) izer.ttype + "'");
                return null;
        }
    }

    void error(String errmsg) {
        System.err.println("Error:" + izer.lineno() + ": " + errmsg);
        System.exit(1);
    }
}
