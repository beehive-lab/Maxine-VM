/***
 * ASM: a very small and fast Java bytecode manipulation framework
 * Copyright (c) 2000-2011 INRIA, France Telecom
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the copyright holders nor the names of its
 *    contributors may be used to endorse or promote products derived from
 *    this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
 * THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.objectweb.asm.commons;

import java.io.FileInputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.TypePath;
import org.objectweb.asm.util.ASMifier;
import org.objectweb.asm.util.Printer;
import org.objectweb.asm.util.TraceClassVisitor;

/**
 * A {@link MethodVisitor} that prints the ASM code that generates the methods
 * it visits by using the GeneratorAdapter class.
 * 
 * @author Eric Bruneton
 * @author Eugene Kuleshov
 */
public class GASMifier extends ASMifier implements Opcodes {

    int access;

    Type[] argumentTypes;

    int firstLocal;

    Map<Integer, Integer> locals;

    List<String> localTypes;

    public GASMifier() {
        super(Opcodes.ASM5, "cw", 0);
    }

    public GASMifier(final String name, final int id) {
        super(Opcodes.ASM5, name, id);
    }

    public GASMifier(final int access, final String desc) {
        super(Opcodes.ASM5, "mg", 0);
        this.access = access;
        this.argumentTypes = Type.getArgumentTypes(desc);
        int nextLocal = (Opcodes.ACC_STATIC & access) != 0 ? 0 : 1;
        for (int i = 0; i < argumentTypes.length; i++) {
            nextLocal += argumentTypes[i].getSize();
        }
        this.firstLocal = nextLocal;
        this.locals = new HashMap<Integer, Integer>();
        this.localTypes = new ArrayList<String>();
    }

    /**
     * Prints the ASM source code to generate the given class to the standard
     * output.
     * <p>
     * Usage: ASMifierClassVisitor [-debug] &lt;fully qualified class name or
     * class file name&gt;
     * 
     * @param args
     *            the command line arguments.
     * 
     * @throws Exception
     *             if the class cannot be found, or if an IO exception occurs.
     */
    public static void main(final String[] args) throws Exception {
        int i = 0;
        int flags = ClassReader.SKIP_DEBUG;

        boolean ok = true;
        if (args.length < 1 || args.length > 2) {
            ok = false;
        }
        if (ok && args[0].equals("-debug")) {
            i = 1;
            flags = 0;
            if (args.length != 2) {
                ok = false;
            }
        }
        if (!ok) {
            System.err
                    .println("Prints the ASM code to generate the given class.");
            System.err.println("Usage: GASMifierClassVisitor [-debug] "
                    + "<fully qualified class name or class file name>");
            System.exit(-1);
        }
        ClassReader cr;
        if (args[i].endsWith(".class")) {
            cr = new ClassReader(new FileInputStream(args[i]));
        } else {
            cr = new ClassReader(args[i]);
        }
        cr.accept(new TraceClassVisitor(null, new GASMifier(), new PrintWriter(
                System.out)), ClassReader.EXPAND_FRAMES | flags);
    }

    @Override
    public void visit(final int version, final int access, final String name,
            final String signature, final String superName,
            final String[] interfaces) {
        super.visit(version, access, name, signature, superName, interfaces);
        int n;
        if (name.lastIndexOf('/') != -1) {
            n = 1;
        } else {
            n = 0;
        }
        text.set(n + 5,
                "ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);\n");
        text.set(n + 7, "TestGeneratorAdapter mg;\n");
        text.add(n + 1, "import org.objectweb.asm.commons.*;\n");
    }

    @Override
    public ASMifier visitMethod(final int access, final String name,
            final String desc, final String signature, final String[] exceptions) {
        buf.setLength(0);
        buf.append("{\n");
        buf.append("mg = new TestGeneratorAdapter(");
        buf.append(access);
        buf.append(", ");
        buf.append(getMethod(name, desc));
        buf.append(", ");
        if (signature == null) {
            buf.append("null");
        } else {
            buf.append('"').append(signature).append('"');
        }
        buf.append(", ");
        if (exceptions != null && exceptions.length > 0) {
            buf.append("new Type[] {");
            for (int i = 0; i < exceptions.length; ++i) {
                buf.append(i == 0 ? " " : ", ");
                buf.append(getType(exceptions[i]));
            }
            buf.append(" }");
        } else {
            buf.append("null");
        }
        buf.append(", cw);\n");
        text.add(buf.toString());
        GASMifier av = new GASMifier(access, desc);
        text.add(av.getText());
        text.add("}\n");
        return av;
    }

    @Override
    public void visitInsn(final int opcode) {
        buf.setLength(0);
        switch (opcode) {
        case IRETURN:
        case LRETURN:
        case FRETURN:
        case DRETURN:
        case ARETURN:
        case RETURN:
            buf.append("mg.returnValue();\n");
            break;
        case NOP:
            buf.append("mg.visitInsn(Opcodes.NOP);\n");
            break;
        case ACONST_NULL:
            buf.append("mg.push((String)null);\n");
            break;
        case ICONST_M1:
        case ICONST_0:
        case ICONST_1:
        case ICONST_2:
        case ICONST_3:
        case ICONST_4:
        case ICONST_5:
            buf.append("mg.push(").append(opcode - ICONST_0).append(");\n");
            break;
        case LCONST_0:
        case LCONST_1:
            buf.append("mg.push(").append(opcode - LCONST_0).append("L);\n");
            break;
        case FCONST_0:
        case FCONST_1:
        case FCONST_2:
            buf.append("mg.push(").append(opcode - FCONST_0).append("f);\n");
            break;
        case DCONST_0:
        case DCONST_1:
            buf.append("mg.push(").append(opcode - DCONST_0).append("d);\n");
            break;
        case POP:
            buf.append("mg.pop();\n");
            break;
        case POP2:
            buf.append("mg.pop2();\n");
            break;
        case DUP:
            buf.append("mg.dup();\n");
            break;
        case DUP_X1:
            buf.append("mg.dupX1();\n");
            break;
        case DUP_X2:
            buf.append("mg.dupX2();\n");
            break;
        case DUP2:
            buf.append("mg.dup2();\n");
            break;
        case DUP2_X1:
            buf.append("mg.dup2X1();\n");
            break;
        case DUP2_X2:
            buf.append("mg.dup2X2();\n");
            break;
        case SWAP:
            buf.append("mg.swap();\n");
            break;
        case MONITORENTER:
            buf.append("mg.monitorEnter();\n");
            break;
        case MONITOREXIT:
            buf.append("mg.monitorExit();\n");
            break;
        case ARRAYLENGTH:
            buf.append("mg.arrayLength();\n");
            break;
        case IALOAD:
            buf.append("mg.arrayLoad(Type.INT_TYPE);\n");
            break;
        case LALOAD:
            buf.append("mg.arrayLoad(Type.LONG_TYPE);\n");
            break;
        case FALOAD:
            buf.append("mg.arrayLoad(Type.FLOAT_TYPE);\n");
            break;
        case DALOAD:
            buf.append("mg.arrayLoad(Type.DOUBLE_TYPE);\n");
            break;
        case AALOAD:
            buf.append("mg.arrayLoad(" + getType("java/lang/Object") + ");\n");
            break;
        case BALOAD:
            buf.append("mg.arrayLoad(Type.BYTE_TYPE);\n");
            break;
        case CALOAD:
            buf.append("mg.arrayLoad(Type.CHAR_TYPE);\n");
            break;
        case SALOAD:
            buf.append("mg.arrayLoad(Type.SHORT_TYPE);\n");
            break;
        case IASTORE:
            buf.append("mg.arrayStore(Type.INT_TYPE);\n");
            break;
        case LASTORE:
            buf.append("mg.arrayStore(Type.LONG_TYPE);\n");
            break;
        case FASTORE:
            buf.append("mg.arrayStore(Type.FLOAT_TYPE);\n");
            break;
        case DASTORE:
            buf.append("mg.arrayStore(Type.DOUBLE_TYPE);\n");
            break;
        case AASTORE:
            buf.append("mg.arrayStore(" + getType("java/lang/Object") + ");\n");
            break;
        case BASTORE:
            buf.append("mg.arrayStore(Type.BYTE_TYPE);\n");
            break;
        case CASTORE:
            buf.append("mg.arrayStore(Type.CHAR_TYPE);\n");
            break;
        case SASTORE:
            buf.append("mg.arrayStore(Type.SHORT_TYPE);\n");
            break;
        case IADD:
            buf.append("mg.math(GeneratorAdapter.ADD, Type.INT_TYPE);\n");
            break;
        case LADD:
            buf.append("mg.math(GeneratorAdapter.ADD, Type.LONG_TYPE);\n");
            break;
        case FADD:
            buf.append("mg.math(GeneratorAdapter.ADD, Type.FLOAT_TYPE);\n");
            break;
        case DADD:
            buf.append("mg.math(GeneratorAdapter.ADD, Type.DOUBLE_TYPE);\n");
            break;
        case ISUB:
            buf.append("mg.math(GeneratorAdapter.SUB, Type.INT_TYPE);\n");
            break;
        case LSUB:
            buf.append("mg.math(GeneratorAdapter.SUB, Type.LONG_TYPE);\n");
            break;
        case FSUB:
            buf.append("mg.math(GeneratorAdapter.SUB, Type.FLOAT_TYPE);\n");
            break;
        case DSUB:
            buf.append("mg.math(GeneratorAdapter.SUB, Type.DOUBLE_TYPE);\n");
            break;
        case IMUL:
            buf.append("mg.math(GeneratorAdapter.MUL, Type.INT_TYPE);\n");
            break;
        case LMUL:
            buf.append("mg.math(GeneratorAdapter.MUL, Type.LONG_TYPE);\n");
            break;
        case FMUL:
            buf.append("mg.math(GeneratorAdapter.MUL, Type.FLOAT_TYPE);\n");
            break;
        case DMUL:
            buf.append("mg.math(GeneratorAdapter.MUL, Type.DOUBLE_TYPE);\n");
            break;
        case IDIV:
            buf.append("mg.math(GeneratorAdapter.DIV, Type.INT_TYPE);\n");
            break;
        case LDIV:
            buf.append("mg.math(GeneratorAdapter.DIV, Type.LONG_TYPE);\n");
            break;
        case FDIV:
            buf.append("mg.math(GeneratorAdapter.DIV, Type.FLOAT_TYPE);\n");
            break;
        case DDIV:
            buf.append("mg.math(GeneratorAdapter.DIV, Type.DOUBLE_TYPE);\n");
            break;
        case IREM:
            buf.append("mg.math(GeneratorAdapter.REM, Type.INT_TYPE);\n");
            break;
        case LREM:
            buf.append("mg.math(GeneratorAdapter.REM, Type.LONG_TYPE);\n");
            break;
        case FREM:
            buf.append("mg.math(GeneratorAdapter.REM, Type.FLOAT_TYPE);\n");
            break;
        case DREM:
            buf.append("mg.math(GeneratorAdapter.REM, Type.DOUBLE_TYPE);\n");
            break;
        case INEG:
            buf.append("mg.math(GeneratorAdapter.NEG, Type.INT_TYPE);\n");
            break;
        case LNEG:
            buf.append("mg.math(GeneratorAdapter.NEG, Type.LONG_TYPE);\n");
            break;
        case FNEG:
            buf.append("mg.math(GeneratorAdapter.NEG, Type.FLOAT_TYPE);\n");
            break;
        case DNEG:
            buf.append("mg.math(GeneratorAdapter.NEG, Type.DOUBLE_TYPE);\n");
            break;
        case ISHL:
            buf.append("mg.math(GeneratorAdapter.SHL, Type.INT_TYPE);\n");
            break;
        case LSHL:
            buf.append("mg.math(GeneratorAdapter.SHL, Type.LONG_TYPE);\n");
            break;
        case ISHR:
            buf.append("mg.math(GeneratorAdapter.SHR, Type.INT_TYPE);\n");
            break;
        case LSHR:
            buf.append("mg.math(GeneratorAdapter.SHR, Type.LONG_TYPE);\n");
            break;
        case IUSHR:
            buf.append("mg.math(GeneratorAdapter.USHR, Type.INT_TYPE);\n");
            break;
        case LUSHR:
            buf.append("mg.math(GeneratorAdapter.USHR, Type.LONG_TYPE);\n");
            break;
        case IAND:
            buf.append("mg.math(GeneratorAdapter.AND, Type.INT_TYPE);\n");
            break;
        case LAND:
            buf.append("mg.math(GeneratorAdapter.AND, Type.LONG_TYPE);\n");
            break;
        case IOR:
            buf.append("mg.math(GeneratorAdapter.OR, Type.INT_TYPE);\n");
            break;
        case LOR:
            buf.append("mg.math(GeneratorAdapter.OR, Type.LONG_TYPE);\n");
            break;
        case IXOR:
            buf.append("mg.math(GeneratorAdapter.XOR, Type.INT_TYPE);\n");
            break;
        case LXOR:
            buf.append("mg.math(GeneratorAdapter.XOR, Type.LONG_TYPE);\n");
            break;
        case ATHROW:
            buf.append("mg.throwException();\n");
            break;
        case I2L:
            buf.append("mg.cast(Type.INT_TYPE, Type.LONG_TYPE);\n");
            break;
        case I2F:
            buf.append("mg.cast(Type.INT_TYPE, Type.FLOAT_TYPE);\n");
            break;
        case I2D:
            buf.append("mg.cast(Type.INT_TYPE, Type.DOUBLE_TYPE);\n");
            break;
        case L2I:
            buf.append("mg.cast(Type.LONG_TYPE, Type.INT_TYPE);\n");
            break;
        case L2F:
            buf.append("mg.cast(Type.LONG_TYPE, Type.FLOAT_TYPE);\n");
            break;
        case L2D:
            buf.append("mg.cast(Type.LONG_TYPE, Type.DOUBLE_TYPE);\n");
            break;
        case F2I:
            buf.append("mg.cast(Type.FLOAT_TYPE, Type.INT_TYPE);\n");
            break;
        case F2L:
            buf.append("mg.cast(Type.FLOAT_TYPE, Type.LONG_TYPE);\n");
            break;
        case F2D:
            buf.append("mg.cast(Type.FLOAT_TYPE, Type.DOUBLE_TYPE);\n");
            break;
        case D2I:
            buf.append("mg.cast(Type.DOUBLE_TYPE, Type.INT_TYPE);\n");
            break;
        case D2L:
            buf.append("mg.cast(Type.DOUBLE_TYPE, Type.LONG_TYPE);\n");
            break;
        case D2F:
            buf.append("mg.cast(Type.DOUBLE_TYPE, Type.FLOAT_TYPE);\n");
            break;
        case I2B:
            // TODO detect if previous element in 'text' is a cast,
            // for possible optimisations (e.g. cast(F,I) cast(I,B) =
            // cast(F,B))
            buf.append("mg.cast(Type.INT_TYPE, Type.BYTE_TYPE);\n");
            break;
        case I2C: // idem
            buf.append("mg.cast(Type.INT_TYPE, Type.CHAR_TYPE);\n");
            break;
        case I2S: // idem
            buf.append("mg.cast(Type.INT_TYPE, Type.SHORT_TYPE);\n");
            break;
        case LCMP:
        case FCMPL:
        case FCMPG:
        case DCMPL:
        case DCMPG:
            // TODO detect xCMPy IF_ICMP -> ifCmp(..., ..., label)
            buf.append("mg.visitInsn(").append(OPCODES[opcode]).append(");\n");
            break;
        default:
            throw new RuntimeException("unexpected case");
        }
        text.add(buf.toString());
    }

    @Override
    public void visitIntInsn(final int opcode, final int operand) {
        buf.setLength(0);
        if (opcode == NEWARRAY) {
            String type;
            switch (operand) {
            case T_BOOLEAN:
                type = "Type.BOOLEAN_TYPE";
                break;
            case T_CHAR:
                type = "Type.CHAR_TYPE";
                break;
            case T_FLOAT:
                type = "Type.FLOAT_TYPE";
                break;
            case T_DOUBLE:
                type = "Type.DOUBLE_TYPE";
                break;
            case T_BYTE:
                type = "Type.BYTE_TYPE";
                break;
            case T_SHORT:
                type = "Type.SHORT_TYPE";
                break;
            case T_INT:
                type = "Type.INT_TYPE";
                break;
            case T_LONG:
                type = "Type.LONG_TYPE";
                break;
            default:
                throw new RuntimeException("unexpected case");
            }
            buf.append("mg.newArray(").append(type).append(");\n");
        } else {
            buf.append("mg.push(").append(operand).append(");\n");
        }
        text.add(buf.toString());
    }

    @Override
    public void visitVarInsn(final int opcode, final int var) {
        buf.setLength(0);
        try {
            switch (opcode) {
            case RET:
                if (var < firstLocal) {
                    buf.append("mg.ret(");
                    buf.append(var);
                    buf.append(");\n");
                } else {
                    int v = generateNewLocal(var, "Type.INT_TYPE");
                    buf.append("mg.ret(");
                    buf.append("local").append(v);
                    buf.append(");\n");
                }
                break;

            case ILOAD:
                generateLoadLocal(var, "Type.INT_TYPE");
                break;
            case LLOAD:
                generateLoadLocal(var, "Type.LONG_TYPE");
                break;
            case FLOAD:
                generateLoadLocal(var, "Type.FLOAT_TYPE");
                break;
            case DLOAD:
                generateLoadLocal(var, "Type.DOUBLE_TYPE");
                break;
            case ALOAD:
                generateLoadLocal(var, getType("java/lang/Object"));
                break;

            case ISTORE:
                generateStoreLocal(var, "Type.INT_TYPE");
                break;
            case LSTORE:
                generateStoreLocal(var, "Type.LONG_TYPE");
                break;
            case FSTORE:
                generateStoreLocal(var, "Type.FLOAT_TYPE");
                break;
            case DSTORE:
                generateStoreLocal(var, "Type.DOUBLE_TYPE");
                break;
            case ASTORE:
                generateStoreLocal(var, getType("java/lang/Object"));
                break;

            default:
                throw new RuntimeException("unexpected case");
            }
        } catch (RuntimeException e) {
            buf.append("mg.visitVarInsn(" + OPCODES[opcode] + ", " + var
                    + ");\n");
        }
        text.add(buf.toString());
    }

    private void generateLoadLocal(final int var, final String type) {
        if (var < firstLocal) {
            if (var == 0 && (access & ACC_STATIC) == 0) {
                buf.append("mg.loadThis();\n");
            } else {
                int index = getArgIndex(var);
                buf.append("mg.loadArg(").append(index).append(");\n");
            }
        } else {
            int local = generateNewLocal(var, type);
            buf.append("mg.loadLocal(local").append(local);
            if (!type.equals(localTypes.get(local))) {
                localTypes.set(local, type);
                buf.append(", ").append(type);
            }
            buf.append(");\n");
        }
    }

    private void generateStoreLocal(final int var, final String type) {
        if (var < firstLocal) {
            if (var == 0 && (access & ACC_STATIC) == 0) {
                buf.append("mg.visitVarInsn(ASTORE, " + var + ");\n");
            } else {
                int index = getArgIndex(var);
                buf.append("mg.storeArg(").append(index).append(");\n");
            }
        } else {
            int local = generateNewLocal(var, type);
            buf.append("mg.storeLocal(local").append(local);
            if (!type.equals(localTypes.get(local))) {
                localTypes.set(local, type);
                buf.append(", ").append(type);
            }
            buf.append(");\n");
        }
    }

    private int generateNewLocal(final int var, final String type) {
        Integer i = locals.get(new Integer(var));
        if (i == null) {
            int local = locals.size();
            locals.put(new Integer(var), new Integer(local));
            localTypes.add(type);
            buf.append("int local" + local + " = mg.newLocal(" + type + ");\n");
            return local;
        }
        return i.intValue();
    }

    private int getArgIndex(final int var) {
        int nextLocal = (Opcodes.ACC_STATIC & access) != 0 ? 0 : 1;
        int i = 0;
        while (nextLocal != var) {
            nextLocal += argumentTypes[i++].getSize();
        }
        return i;
    }

    @Override
    public void visitTypeInsn(final int opcode, final String type) {
        String typ = getType(type);
        buf.setLength(0);
        if (opcode == NEW) {
            buf.append("mg.newInstance(").append(typ).append(");\n");
        } else if (opcode == ANEWARRAY) {
            buf.append("mg.newArray(").append(typ).append(");\n");
        } else if (opcode == CHECKCAST) {
            buf.append("mg.checkCast(").append(typ).append(");\n");
        } else if (opcode == INSTANCEOF) {
            buf.append("mg.instanceOf(").append(typ).append(");\n");
        }
        text.add(buf.toString());
    }

    @Override
    public void visitFieldInsn(final int opcode, final String owner,
            final String name, final String desc) {
        buf.setLength(0);
        switch (opcode) {
        case GETFIELD:
            buf.append("mg.getField(");
            break;
        case PUTFIELD:
            buf.append("mg.putField(");
            break;
        case GETSTATIC:
            buf.append("mg.getStatic(");
            break;
        case PUTSTATIC:
            buf.append("mg.putStatic(");
            break;
        default:
            throw new RuntimeException("unexpected case");
        }
        buf.append(getType(owner));
        buf.append(", \"");
        buf.append(name);
        buf.append("\", ");
        buf.append(getDescType(desc));
        buf.append(");\n");
        text.add(buf.toString());
    }

    @Override
    public void visitMethodInsn(final int opcode, final String owner,
            final String name, final String desc, final boolean itf) {
        buf.setLength(0);
        switch (opcode) {
        case INVOKEVIRTUAL:
            buf.append("mg.invokeVirtual(");
            break;
        case INVOKESPECIAL:
            buf.append("mg.invokeConstructor(");
            break;
        case INVOKESTATIC:
            buf.append("mg.invokeStatic(");
            break;
        case INVOKEINTERFACE:
            buf.append("mg.invokeInterface(");
            break;
        default:
            throw new RuntimeException("unexpected case");
        }
        if (owner.charAt(0) == '[') {
            buf.append(getDescType(owner));
        } else {
            buf.append(getType(owner));
        }
        buf.append(", ");
        buf.append(getMethod(name, desc));
        buf.append(");\n");
        text.add(buf.toString());
    }

    @Override
    public void visitInvokeDynamicInsn(String name, String desc, Handle bsm,
            Object... bsmArgs) {
        buf.setLength(0);
        buf.append("mg.invokeDynamic(");
        appendConstant(name);
        buf.append(", ");
        appendConstant(desc);
        buf.append(", ");
        appendConstant(bsm);
        buf.append(", new Object[] {");
        for (int i = 0; i < bsmArgs.length; ++i) {
            appendConstant(bsmArgs[i]);
            if (i != bsmArgs.length - 1) {
                buf.append(", ");
            }
        }
        buf.append("});\n");
        text.add(buf.toString());
    }

    @Override
    public void visitJumpInsn(final int opcode, final Label label) {
        buf.setLength(0);
        declareLabel(label);
        if (opcode == GOTO || opcode == IFNULL || opcode == IFNONNULL) {
            if (opcode == GOTO) {
                buf.append("mg.goTo(");
            }
            if (opcode == IFNULL) {
                buf.append("mg.ifNull(");
            }
            if (opcode == IFNONNULL) {
                buf.append("mg.ifNonNull(");
            }
            appendLabel(label);
            buf.append(");\n");
        } else if (opcode == IF_ICMPEQ) {
            buf.append("mg.ifICmp(GeneratorAdapter.EQ, ");
            appendLabel(label);
            buf.append(");\n");
        } else if (opcode == IF_ICMPNE) {
            buf.append("mg.ifICmp(GeneratorAdapter.NE, ");
            appendLabel(label);
            buf.append(");\n");
        } else if (opcode == IF_ICMPLT) {
            buf.append("mg.ifICmp(GeneratorAdapter.LT, ");
            appendLabel(label);
            buf.append(");\n");
        } else if (opcode == IF_ICMPGE) {
            buf.append("mg.ifICmp(GeneratorAdapter.GE, ");
            appendLabel(label);
            buf.append(");\n");
        } else if (opcode == IF_ICMPGT) {
            buf.append("mg.ifICmp(GeneratorAdapter.GT, ");
            appendLabel(label);
            buf.append(");\n");
        } else if (opcode == IF_ICMPLE) {
            buf.append("mg.ifICmp(GeneratorAdapter.LE, ");
            appendLabel(label);
            buf.append(");\n");
        } else if (opcode == IF_ACMPEQ) {
            buf.append("mg.ifCmp(");
            buf.append(getType("java/lang/Object")).append(", ")
                    .append("GeneratorAdapter.EQ, ");
            appendLabel(label);
            buf.append(");\n");
        } else if (opcode == IF_ACMPNE) {
            buf.append("mg.ifCmp(");
            buf.append(getType("java/lang/Object")).append(", ")
                    .append("GeneratorAdapter.NE, ");
            appendLabel(label);
            buf.append(");\n");
        } else if (opcode == IFEQ) {
            buf.append("mg.ifZCmp(GeneratorAdapter.EQ, ");
            appendLabel(label);
            buf.append(");\n");
        } else if (opcode == IFNE) {
            buf.append("mg.ifZCmp(GeneratorAdapter.NE, ");
            appendLabel(label);
            buf.append(");\n");
        } else if (opcode == IFLT) {
            buf.append("mg.ifZCmp(GeneratorAdapter.LT, ");
            appendLabel(label);
            buf.append(");\n");
        } else if (opcode == IFGE) {
            buf.append("mg.ifZCmp(GeneratorAdapter.GE, ");
            appendLabel(label);
            buf.append(");\n");
        } else if (opcode == IFGT) {
            buf.append("mg.ifZCmp(GeneratorAdapter.GT, ");
            appendLabel(label);
            buf.append(");\n");
        } else if (opcode == IFLE) {
            buf.append("mg.ifZCmp(GeneratorAdapter.LE, ");
            appendLabel(label);
            buf.append(");\n");
        } else {
            buf.append("mg.visitJumpInsn(").append(OPCODES[opcode])
                    .append(", ");
            appendLabel(label);
            buf.append(");\n");
        }
        text.add(buf.toString());
    }

    @Override
    public void visitLabel(final Label label) {
        buf.setLength(0);
        declareLabel(label);
        buf.append("mg.mark(");
        appendLabel(label);
        buf.append(");\n");
        text.add(buf.toString());
    }

    @Override
    public void visitLdcInsn(final Object cst) {
        buf.setLength(0);
        buf.append("mg.push(");
        appendConstant(cst);
        buf.append(");\n");
        text.add(buf.toString());
    }

    @Override
    public void visitIincInsn(final int var, final int increment) {
        buf.setLength(0);
        if (var < firstLocal) {
            buf.append("mg.iinc(").append(var);
        } else {
            int v = generateNewLocal(var, "Type.INT_TYPE");
            buf.append("mg.iinc(local").append(v);
        }
        buf.append(", ").append(increment).append(");\n");
        text.add(buf.toString());
    }

    @Override
    public void visitLocalVariable(final String name, final String desc,
            final String signature, final Label start, final Label end,
            final int index) {
        String idx = index < firstLocal ? new Integer(index).toString()
                : "local" + locals.get(new Integer(index));
        buf.setLength(0);
        buf.append(this.name).append(".getNext().visitLocalVariable(");
        appendConstant(name);
        buf.append(", ");
        appendConstant(desc);
        buf.append(", ");
        appendConstant(signature);
        buf.append(", ");
        appendLabel(start);
        buf.append(", ");
        appendLabel(end);
        buf.append(", ").append(idx).append(");\n");
        text.add(buf.toString());
    }

    @Override
    public Printer visitLocalVariableAnnotation(int typeRef, TypePath typePath,
            Label[] start, Label[] end, int[] index, String desc,
            boolean visible) {
        buf.setLength(0);
        buf.append("{\n").append("av0 = ").append(name)
                .append(".getNext().visitLocalVariableAnnotation(");
        buf.append(typeRef);
        buf.append(", TypePath.fromString(\"").append(typePath).append("\"), ");
        buf.append("new Label[] {");
        for (int i = 0; i < start.length; ++i) {
            buf.append(i == 0 ? " " : ", ");
            appendLabel(start[i]);
        }
        buf.append(" }, new Label[] {");
        for (int i = 0; i < end.length; ++i) {
            buf.append(i == 0 ? " " : ", ");
            appendLabel(end[i]);
        }
        buf.append(" }, new int[] {");
        for (int i = 0; i < index.length; ++i) {
            String idx = index[i] < firstLocal ? new Integer(index[i])
                    .toString() : "local" + locals.get(new Integer(index[i]));
            buf.append(i == 0 ? " " : ", ").append(idx);
        }
        buf.append(" }, ");
        appendConstant(desc);
        buf.append(", ").append(visible).append(");\n");
        text.add(buf.toString());
        ASMifier a = createASMifier("av", 0);
        text.add(a.getText());
        text.add("}\n");
        return a;
    }

    @Override
    public void visitMaxs(final int maxStack, final int maxLocals) {
        text.add("mg.endMethod();\n");
    }

    @Override
    protected ASMifier createASMifier(final String name, final int id) {
        return new GASMifier(name, id);
    }

    static String getType(final String internalName) {
        return "Type.getObjectType(\"" + internalName + "\")";
    }

    static String getDescType(final String desc) {
        if (desc.equals("Z")) {
            return "Type.BOOLEAN_TYPE";
        }
        if (desc.equals("B")) {
            return "Type.BYTE_TYPE";
        }
        if (desc.equals("C")) {
            return "Type.CHAR_TYPE";
        }
        if (desc.equals("D")) {
            return "Type.DOUBLE_TYPE";
        }
        if (desc.equals("F")) {
            return "Type.FLOAT_TYPE";
        }
        if (desc.equals("I")) {
            return "Type.INT_TYPE";
        }
        if (desc.equals("J")) {
            return "Type.LONG_TYPE";
        }
        if (desc.equals("S")) {
            return "Type.SHORT_TYPE";
        }
        if (desc.equals("V")) {
            return "Type.VOID_TYPE";
        }
        return "Type.getType(\"" + desc + "\")";
    }

    static String getMethod(final String name, final String desc) {
        Type rt = Type.getReturnType(desc);
        Type[] argt = Type.getArgumentTypes(desc);
        StringBuffer buf = new StringBuffer();
        buf.append("Method.getMethod(\"");
        buf.append(rt.getClassName()).append(' ');
        buf.append(name).append('(');
        for (int i = 0; i < argt.length; ++i) {
            if (i > 0) {
                buf.append(',');
            }
            buf.append(argt[i].getClassName());
        }
        buf.append(")\")");
        return buf.toString();
    }

    @Override
    protected void declareLabel(final Label l) {
        if (labelNames == null) {
            labelNames = new HashMap<Label, String>();
        }
        String name = labelNames.get(l);
        if (name == null) {
            name = "label" + labelNames.size();
            labelNames.put(l, name);
            buf.append("Label ").append(name).append(" = mg.newLabel();\n");
        }
    }
}
