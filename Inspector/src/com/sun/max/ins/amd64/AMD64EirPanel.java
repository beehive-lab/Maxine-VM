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
package com.sun.max.ins.amd64;

import java.awt.*;
import java.lang.reflect.*;

import javax.swing.*;

import com.sun.max.collect.*;
import com.sun.max.gui.*;
import com.sun.max.ins.*;
import com.sun.max.ins.gui.*;
import com.sun.max.ins.method.*;
import com.sun.max.ins.value.*;
import com.sun.max.lang.*;
import com.sun.max.tele.value.*;
import com.sun.max.vm.compiler.eir.*;
import com.sun.max.vm.compiler.eir.amd64.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;

/**
 * @author Bernd Mathiske
 */
public final class AMD64EirPanel extends EirPanel<EirMethod> {

    @Override
    public String createTitle() {
        return eirMethod().name() + "(" + Arrays.toString(eirMethod().parameterLocations(), ", ") + ") -> " + eirMethod().resultLocation();
    }

    public void refresh(long epoch) {
    }

    public void redisplay() {
        // TODO (mlvdv)  redisplay this
    }

    private void addLiterals(Sequence<EirLiteral> literals) {
        if (literals == null || literals.isEmpty()) {
            return;
        }

        final JPanel panel = new JPanel(new SpringLayout());
        panel.setBorder(BorderFactory.createMatteBorder(2, 0, 0, 0, Color.BLUE));
        panel.add(new Space());
        panel.add(new TitleLabel(inspection(), literals.first().value().kind() == Kind.REFERENCE ? "References" : "Scalars"));

        for (EirLiteral literal : literals) {
            panel.add(new TextLabel(inspection(), literal.value().kind().character() + "#" + Integer.toString(literal.index()) + ":"));
            if (literal.value().kind() == Kind.REFERENCE) {
                panel.add(new ReferenceValueLabel(inspection(), (TeleReferenceValue) literal.value()));
            } else {
                panel.add(new PrimitiveValueLabel(inspection(), literal.value()));
            }
        }

        SpringUtilities.makeCompactGrid(panel, 2);
        add(panel);
    }

    private static final int _numberOfColumns = 10;

    private final class InstructionRenderer extends AMD64EirInstructionAdapter {

        private final JPanel _panel;

        private InstructionRenderer(JPanel panel) {
            _panel = panel;
        }

        @Override
        public void visitInstruction(EirInstruction instruction) {
            _panel.add(new TextLabel(inspection(), instruction.toString()));
        }

        private void addSpace() {
            _panel.add(new Space());
        }

        private void addName(AMD64EirInstruction instruction) {
            _panel.add(new TextLabel(inspection(), instruction.getClass().getSimpleName()));
        }

        private void addValue(String prefix, Value value) {
            if (value.kind() == Kind.REFERENCE) {
                final ReferenceValueLabel r = new ReferenceValueLabel(inspection(), value.asReference());
                r.setPrefix(prefix);
                _panel.add(r);
            } else {
                _panel.add(new TextLabel(inspection(), prefix + value.toString()));
            }
        }

        private void addEirOperand(EirOperand eirOperand) {
            if (eirOperand.location() instanceof EirLiteral) {
                final EirLiteral literal = (EirLiteral) eirOperand.location();
                addValue(literal.value().kind().character() + "#" + literal.index() + ": ", literal.value());
            } else if (eirOperand.location() instanceof EirImmediate) {
                final EirImmediate immediate = (EirImmediate) eirOperand.location();
                addValue(immediate.value().kind().character() + ": ", immediate.value());
            } else {
                _panel.add(new TextLabel(inspection(), eirOperand.toString()));
            }
        }

        @Override
        public void visitUnaryOperation(AMD64EirUnaryOperation instruction) {
            addName(instruction);
            addSpace();
            addEirOperand(instruction.operand());
        }

        @Override
        public void visitBinaryOperation(AMD64EirBinaryOperation instruction) {
            addName(instruction);
            addSpace();
            addEirOperand(instruction.destinationOperand());
            addSpace();
            addEirOperand(instruction.sourceOperand());
        }

        @Override
        public void visitPointerOperation(AMD64EirPointerOperation instruction) {
            addName(instruction);
            addSpace();
            addEirOperand(instruction.destinationOperand());
            addSpace();
            addEirOperand(instruction.offsetOperand());
            addSpace();
            addEirOperand(instruction.indexOperand());
        }

        @Override
        public void visit(EirPrologue instruction) {
            _panel.add(new TextLabel(inspection(), "prologue"));
            addSpace();
            _panel.add(new TextLabel(inspection(), instruction.eirMethod().frameSize()));
        }

        @Override
        public void visit(EirEpilogue instruction) {
            _panel.add(new TextLabel(inspection(), "epilogue"));
            addSpace();
            _panel.add(new TextLabel(inspection(), instruction.eirMethod().frameSize()));
        }

        @Override
        public void visit(EirAssignment instruction) {
            addSpace();
            addSpace();
            addEirOperand(instruction.destinationOperand());
            _panel.add(new TextLabel(inspection(), ":="));
            addEirOperand(instruction.sourceOperand());
        }

        @Override
        public void visit(AMD64EirLoad instruction) {
            _panel.add(new TextLabel(inspection(), "load-" + instruction.kind().character()));
            addSpace();
            addEirOperand(instruction.destinationOperand());
            _panel.add(new TextLabel(inspection(), ":="));
            _panel.add(new TextLabel(inspection(), instruction.addressString()));
        }

        @Override
        public void visit(AMD64EirStore instruction) {
            _panel.add(new TextLabel(inspection(), "store-" + instruction.kind().character()));
            addSpace();
            _panel.add(new TextLabel(inspection(), instruction.addressString()));
            _panel.add(new TextLabel(inspection(), ":="));
            addEirOperand(instruction.sourceOperand());
        }

        @Override
        public void visit(EirCall instruction) {
            addSpace();
            _panel.add(new TextLabel(inspection(), Arrays.toString(instruction.arguments(), ", ")));
            if (instruction.result() != null) {
                _panel.add(new TextLabel(inspection(), "->"));
                addEirOperand(instruction.result());
            }
        }
    }

    private void align(JPanel panel) {
        while ((panel.getComponentCount() % _numberOfColumns) != 0) {
            panel.add(new Space());
        }
    }

    private void addBlock(EirBlock block) {
        final JPanel panel = new JPanel(new SpringLayout());
        panel.setBorder(BorderFactory.createMatteBorder(2, 0, 0, 0, Color.BLUE));
        panel.add(new Space());
        panel.add(new Space());
        final TitleLabel blockTitle = new TitleLabel(inspection(), "BLOCK #" + block.serial());
        blockTitle.setForeground(Color.BLUE);
        panel.add(blockTitle);
        align(panel);

        final InstructionRenderer instructionRenderer = new InstructionRenderer(panel);
        for (int i = 0; i < block.instructions().length(); i++) {
            panel.add(new TextLabel(inspection(), Integer.toString(i)));
            panel.add(new Space());
            try {
                final Class<EirInstruction<AMD64EirInstructionVisitor, ?>> type = null;
                final EirInstruction<AMD64EirInstructionVisitor, ?> instruction =
                    StaticLoophole.cast(type, block.instructions().get(i));
                instruction.acceptVisitor(instructionRenderer);
            } catch (InvocationTargetException invocationTargetException) {
                throw new InspectorError(invocationTargetException);
            }
            align(panel);
        }

        SpringUtilities.makeCompactGrid(panel, _numberOfColumns);
        add(panel);
    }

    public AMD64EirPanel(Inspection inspection, EirMethod eirMethod) {
        super(inspection, eirMethod);
        for (EirBlock block : eirMethod.blocks()) {
            addBlock(block);
        }
        addLiterals(eirMethod.literalPool().referenceLiterals());
        addLiterals(eirMethod.literalPool().scalarLiterals());
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
    }

}
