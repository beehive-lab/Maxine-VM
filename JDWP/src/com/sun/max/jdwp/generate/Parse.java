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

package com.sun.max.jdwp.generate;

import java.io.*;
import java.util.*;

/**
 * @author JDK7: jdk/make/tools/src/build/tools/jdwpgen
 * @author Thomas Wuerthinger
 */
class Parse {

    final StreamTokenizer _izer;
    final Map<String, Node> _kindMap = new HashMap<String, Node>();

    Parse(Reader reader) {
        _izer = new StreamTokenizer(new BufferedReader(reader));
        _izer.resetSyntax();
        _izer.slashStarComments(true);
        _izer.slashSlashComments(true);
        _izer.wordChars('a', 'z');
        _izer.wordChars('A', 'Z');
        _izer.wordChars('0', '9');
        _izer.wordChars('_', '_');
        _izer.wordChars('-', '-');
        _izer.wordChars('.', '.');
        _izer.whitespaceChars(0, 32);
        _izer.quoteChar('"');
        _izer.quoteChar('\'');

        _kindMap.put("CommandSet", new CommandSetNode());
        _kindMap.put("Command", new CommandNode());
        _kindMap.put("Out", new OutNode());
        _kindMap.put("Reply", new ReplyNode());
        _kindMap.put("ErrorSet", new ErrorSetNode());
        _kindMap.put("Error", new ErrorNode());
        _kindMap.put("Event", new EventNode());
        _kindMap.put("Repeat", new RepeatNode());
        _kindMap.put("Group", new GroupNode());
        _kindMap.put("Select", new SelectNode());
        _kindMap.put("Alt", new AltNode());
        _kindMap.put("ConstantSet", new ConstantSetNode());
        _kindMap.put("Constant", new ConstantNode());
        _kindMap.put("int", new SimpleTypeNode("int", "int", "ps.readInt()"));
        _kindMap.put("long", new SimpleTypeNode("long", "long", "ps.readLong()"));
        _kindMap.put("boolean", new SimpleTypeNode("boolean", "boolean", "ps.readBoolean()"));

        _kindMap.put("object", new SimpleIDTypeNode("ObjectID"));
        _kindMap.put("threadObject", new SimpleIDTypeNode("ThreadID"));
        _kindMap.put("threadGroupObject", new SimpleIDTypeNode("ThreadGroupID"));
        _kindMap.put("arrayObject", new SimpleIDTypeNode("ArrayID"));
        _kindMap.put("stringObject", new SimpleIDTypeNode("StringID"));
        _kindMap.put("classLoaderObject", new SimpleIDTypeNode("ClassLoaderID"));
        _kindMap.put("classObject", new SimpleIDTypeNode("ClassObjectID"));
        _kindMap.put("referenceType", new SimpleIDTypeNode("ReferenceTypeID"));
        _kindMap.put("referenceTypeID", new SimpleIDTypeNode("ReferenceTypeID"));
        _kindMap.put("classType", new SimpleIDTypeNode("ClassID"));
        _kindMap.put("interfaceType", new SimpleIDTypeNode("InterfaceID"));
        _kindMap.put("arrayType", new SimpleIDTypeNode("ArrayTypeID"));
        _kindMap.put("method", new SimpleIDTypeNode("MethodID"));
        _kindMap.put("field", new SimpleIDTypeNode("FieldID"));
        _kindMap.put("frame", new SimpleIDTypeNode("FrameID"));
        _kindMap.put("referenceTypeID", new SimpleIDTypeNode("ReferenceTypeID"));

        _kindMap.put("string", new SimpleTypeNode("string", "String", "ps.readString()"));
        _kindMap.put("value", new SimpleTypeNode("value", "JDWPValue", "ps.readValue()"));
        _kindMap.put("byte", new SimpleTypeNode("byte", "byte", "ps.readByte()"));
        _kindMap.put("location", new SimpleTypeNode("location", "JDWPLocation", "ps.readLocation()"));
        _kindMap.put("tagged-object", new TaggedObjectTypeNode());
        _kindMap.put("typed-sequence", new SimpleTypeNode("arrayregion", "java.util.List<? extends JDWPValue>", "ps.readArrayRegion()"));
        _kindMap.put("untagged-value", new UntaggedValueTypeNode());
    }

    RootNode items() throws IOException {
        final List<Node> list = new ArrayList<Node>();

        while (_izer.nextToken() != StreamTokenizer.TT_EOF) {
            _izer.pushBack();
            list.add(item());
        }
        final RootNode node = new RootNode();
        node.set("Root", list, 1);
        return node;
    }

    Node item() throws IOException {
        switch (_izer.nextToken()) {
            case StreamTokenizer.TT_EOF:
                error("Unexpect end-of-file");
                return null;

            case StreamTokenizer.TT_WORD: {
                final String name = _izer.sval;
                if (_izer.nextToken() == '=') {
                    final int ntok = _izer.nextToken();
                    if (ntok == StreamTokenizer.TT_WORD) {
                        return new NameValueNode(name, _izer.sval);
                    } else if (ntok == '\'') {
                        return new NameValueNode(name, _izer.sval.charAt(0));
                    } else {
                        error("Expected value after: " + name + " =");
                        return null;
                    }
                }
                _izer.pushBack();
                return new NameNode(name);
            }

            case '"':
                return new CommentNode(_izer.sval);

            case '(': {
                if (_izer.nextToken() == StreamTokenizer.TT_WORD) {
                    final String kind = _izer.sval;
                    final List<Node> list = new ArrayList<Node>();

                    while (_izer.nextToken() != ')') {
                        _izer.pushBack();
                        list.add(item());
                    }
                    final Node proto = _kindMap.get(kind);
                    if (proto == null) {
                        error("Invalid kind: " + kind);
                        return null;
                    }
                    try {

                        if (proto instanceof SimpleTypeNode) {
                            final Node node = ((SimpleTypeNode) proto).copy();
                            node.set(kind, list, _izer.lineno());
                            return node;
                        }
                        final Node node = proto.getClass().newInstance();
                        node.set(kind, list, _izer.lineno());
                        return node;
                    } catch (Exception exc) {
                        error(exc.toString());
                        return null;
                    }
                }
                error("Expected kind identifier, got " + _izer.ttype + " : " + _izer.sval);
                return null;
            }

            default:
                error("Unexpected character: '" + (char) _izer.ttype + "'");
                return null;
        }
    }

    void error(String errmsg) {
        System.err.println("Error:" + _izer.lineno() + ": " + errmsg);
        System.exit(1);
    }
}
