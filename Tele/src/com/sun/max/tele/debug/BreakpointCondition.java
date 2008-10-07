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
/*VCSID=97dbbed1-bfc9-4b65-823b-98a99d696d9a*/
package com.sun.max.tele.debug;

import static java.io.StreamTokenizer.*;

import java.io.*;
import java.util.*;

import com.sun.max.tele.*;
import com.sun.max.unsafe.*;
import com.sun.max.util.*;

/**
 * Simple conditional breakpoints.
 *
 * @author Mick Jordan
 */
public class BreakpointCondition extends TeleVMHolder {

    private String _condition;
    private StreamTokenizer _streamTokenizer;
    private TeleIntegerRegisters _integerRegisters;
    private static Map<String, ? extends Symbol> _integerRegisterSymbols;
    private Expression _expression;

    public BreakpointCondition(TeleVM teleVM, String condition) throws ExpressionException {
        super(teleVM);
        _condition = condition;
        if (_integerRegisterSymbols == null) {
            _integerRegisterSymbols = Symbolizer.Static.toSymbolMap(TeleIntegerRegisters.symbolizer(teleVM.vmConfiguration()));
        }
        _expression = parse();
    }

    public boolean evaluate(TeleProcess teleProcess, TeleNativeThread teleNativeThread) {
        if (_expression == null) {
            return false;
        }
        _integerRegisters = teleNativeThread.integerRegisters();
        _integerRegisters.symbolizer();
        try {
            final Expression result = _expression.evaluate();
            if (result instanceof BooleanExpression) {
                return ((BooleanExpression) result).value();
            }
            return false;
        } catch (ExpressionException ex) {
            return false;
        }
    }

    private void initializeTokenizer() {
        _streamTokenizer = new StreamTokenizer(new StringReader(_condition));
        _streamTokenizer.ordinaryChars('0', '9');
        _streamTokenizer.wordChars('0', '9');
    }

    Expression parse()  throws ExpressionException {
        try {
            initializeTokenizer();
            _streamTokenizer.nextToken();
            final Expression result = parseExpression();
            // Must be empty or a BinaryExpr
            if (result == null || result instanceof BinaryExpression) {
                return result;
            }
            throw new ExpressionException("expression must return boolean result");
        } catch (IOException ex) {
            throw new ExpressionException("syntax error");
        }
    }

    // CheckStyle: stop inner assignment checks

    Expression parseExpression() throws ExpressionException, IOException {
        Expression result = null;
        while (_streamTokenizer.ttype != TT_EOF) {
            Operator op;
            Expression expression = null;
            if (_streamTokenizer.ttype == '(') {
                _streamTokenizer.nextToken();
                expression = parseExpression();
                _streamTokenizer.nextToken();
                expectChar(')');
            } else if (_streamTokenizer.ttype == '[') {
                expression = expectMemory();
            } else if ((expression = isRegister()) != null) {
               //
            } else if ((expression = isNumber()) != null) {
                //
            } else if ((op = isBinaryOperator()) != null) {
                if (result == null) {
                    throw new ExpressionException("no left operand for binary operator");
                }
                _streamTokenizer.nextToken();
                expression = new BinaryExpression(op, result, parseExpression());
            }
            result = expression;
            _streamTokenizer.nextToken();
        }
        return result;
    }

    // CheckStyle: resume inner assignment checks

    private RegisterExpression isRegister() {
        if (_streamTokenizer.ttype == TT_WORD) {
            final Symbol register = _integerRegisterSymbols.get(_streamTokenizer.sval);
            if (register != null) {
                return new RegisterExpression(register);
            }
        }
        return null;
    }

    private Operator isBinaryOperator() throws IOException, ExpressionException {
        switch (_streamTokenizer.ttype) {
            case '+':
                return Operator.PLUS;
            case '-':
                return Operator.MINUS;
            case '=':
                _streamTokenizer.nextToken();
                if (_streamTokenizer.ttype == '=') {
                    return Operator.EQ;
                }
                _streamTokenizer.pushBack();
                break;
            case '!':
                _streamTokenizer.nextToken();
                if (_streamTokenizer.ttype == '=') {
                    return Operator.NE;
                }
                _streamTokenizer.pushBack();
                break;
            case '>':
                _streamTokenizer.nextToken();
                if (_streamTokenizer.ttype == '=') {
                    return Operator.GE;
                }
                _streamTokenizer.pushBack();
                return Operator.GT;
            case '<':
                _streamTokenizer.nextToken();
                if (_streamTokenizer.ttype == '=') {
                    return Operator.LE;
                }
                _streamTokenizer.pushBack();
                return Operator.LT;
            case '&':
                _streamTokenizer.nextToken();
                if (_streamTokenizer.ttype == '&') {
                    return Operator.LOGAND;
                }
                _streamTokenizer.pushBack();
                break;
            case '|':
                _streamTokenizer.nextToken();
                if (_streamTokenizer.ttype == '|') {
                    return Operator.LOGOR;
                }
                _streamTokenizer.pushBack();
                break;
            default:
                break;
        }
        return null;
    }

    private Expression expectMemory() throws ExpressionException, IOException {
        if (_streamTokenizer.ttype == '[') {
            Expression result = null;
            _streamTokenizer.nextToken();
            final Expression addressExpression = expectRegisterOrNumber();
            _streamTokenizer.nextToken();
            final Operator op = isBinaryOperator();
            if (op != null) {
                if (op == Operator.PLUS || op == Operator.MINUS) {
                    _streamTokenizer.nextToken();
                    final Expression addressOffsetExpression = expectRegisterOrNumber();
                    if (addressExpression  instanceof RegisterExpression && addressOffsetExpression instanceof NumberExpression) {
                        result = new OffsetRegisterMemoryExpression((RegisterExpression) addressExpression, op, (NumberExpression) addressOffsetExpression);
                    } else if (addressOffsetExpression  instanceof RegisterExpression && addressExpression instanceof NumberExpression) {
                        result = new OffsetRegisterMemoryExpression((RegisterExpression) addressOffsetExpression, op, (NumberExpression) addressExpression);
                    } else if (addressOffsetExpression  instanceof NumberExpression && addressExpression instanceof NumberExpression) {
                        Address address1 = Address.fromLong(((NumberExpression) addressExpression).value());
                        final Address address2 = Address.fromLong(((NumberExpression) addressOffsetExpression).value());
                        switch (op) {
                            case PLUS:
                                address1 = address1.plus(address2);
                                break;
                            case MINUS:
                                address1 = address1.minus(address2);
                                break;
                            default:
                                throw new ExpressionException("illegal operator in memory expression");
                        }
                        result = new AddressMemoryExpression(address1);
                    } else {
                        throw new ExpressionException("illegal memory expression");
                    }
                    _streamTokenizer.nextToken();
                } else {
                    _streamTokenizer.pushBack();
                }
            } else {
                if (addressExpression instanceof RegisterExpression) {
                    result = new RegisterMemoryExpression((RegisterExpression) addressExpression);
                } else {
                    result = new AddressMemoryExpression(Address.fromLong(((NumberExpression) addressExpression).value()));
                }
            }
            expectChar(']');
            return result;
        }
        throw new ExpressionException("syntax error: [ expected");
    }

    private void expectChar(char ch) throws ExpressionException {
        if (_streamTokenizer.ttype != ch) {
            throw new ExpressionException("syntax error: " + ch + " expected");
        }
    }

    private Expression expectRegisterOrNumber() throws ExpressionException {
        Expression result = isRegister();
        if (result != null) {
            return result;
        }
        result = isNumber();
        if (result != null) {
            return result;
        }
        throw new ExpressionException("register or number expected");
    }

    private Expression isNumber() {
        if (_streamTokenizer.ttype == TT_WORD) {
            final int length = _streamTokenizer.sval.length();
            if (length > 0) {
                final char firstChar = _streamTokenizer.sval.charAt(0);
                if ((firstChar >= '0') && (firstChar <= '9')) {
                    final boolean isHex = (firstChar == '0') && (length > 1 && Character.toUpperCase(_streamTokenizer.sval.charAt(1)) == 'X');
                    final int radix = isHex ? 16 : 10;
                    String toParse = _streamTokenizer.sval;
                    if (isHex) {
                        toParse = toParse.substring(2);
                    }
                    return new NumberExpression(Long.parseLong(toParse, radix));
                }
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return _condition;
    }

    public static class ExpressionException extends Exception {
        ExpressionException(String s) {
            super(s);
        }
    }

    abstract static class Expression {
        private static final String ILLEGAL_OPERATION = "illegal operator for type";
        abstract Expression evaluate() throws ExpressionException;

        boolean lt(Object other) throws ExpressionException {
            throw new ExpressionException(ILLEGAL_OPERATION);
        }

        boolean le(Object other) throws ExpressionException {
            throw new ExpressionException(ILLEGAL_OPERATION);
        }

        boolean ge(Object other) throws ExpressionException {
            throw new ExpressionException(ILLEGAL_OPERATION);
        }

        boolean gt(Object other) throws ExpressionException {
            throw new ExpressionException(ILLEGAL_OPERATION);
        }
    }

    class BinaryExpression extends Expression {
        private Operator _operator;
        private Expression _left;
        private Expression _right;

        BinaryExpression(Operator operator, Expression left, Expression right) {
            _operator = operator;
            _left = left;
            _right = right;
        }

        Operator operator() {
            return _operator;
        }

        Expression left() {
            return _left;
        }

        Expression right() {
            return _right;
        }

        @Override
        Expression evaluate()  throws ExpressionException {
            final Expression leftValue = _left.evaluate();
            final Expression rightValue = _right.evaluate();

            switch (_operator) {
                case LT:
                    return new BooleanExpression(leftValue.lt(rightValue));
                case LE:
                    return new BooleanExpression(leftValue.le(rightValue));
                case EQ:
                    return new BooleanExpression(leftValue.equals(rightValue));
                case NE:
                    return new BooleanExpression(!leftValue.equals(rightValue));
                case GE:
                    return new BooleanExpression(leftValue.ge(rightValue));
                case GT:
                    return new BooleanExpression(leftValue.gt(rightValue));
                case LOGAND:
                case LOGOR:
                    final BooleanExpression leftBoolean = checkBoolean(leftValue);
                    final BooleanExpression rightBoolean = checkBoolean(rightValue);
                    if (_operator == Operator.LOGAND) {
                        return new BooleanExpression(leftBoolean.value() && rightBoolean.value());
                    }
                    return new BooleanExpression(leftBoolean.value() || rightBoolean.value());
                default:
            }
            return new BooleanExpression(false);
        }

        private BooleanExpression checkBoolean(Expression expr) throws ExpressionException {
            if (expr instanceof BooleanExpression) {
                return (BooleanExpression) expr;
            }
            throw new ExpressionException("operands must be boolean");
        }
    }

    class RegisterExpression extends Expression {
        Symbol _register;

        RegisterExpression(Symbol register) {
            _register = register;
        }

        Symbol register() {
            return _register;
        }

        @Override
        Expression evaluate() {
            return new NumberExpression(_integerRegisters.get(_register).toLong());
        }

        @Override
        public boolean equals(Object other) {
            if (other instanceof RegisterExpression) {
                final RegisterExpression otherNumber = (RegisterExpression) other;
                return evaluate().equals(otherNumber.evaluate());
            }
            return false;
        }
    }

    class NumberExpression extends Expression {
        long _value;

        NumberExpression(long value) {
            _value = value;
        }

        long value() {
            return _value;
        }

        @Override
        Expression evaluate() {
            return this;
        }

        @Override
        public boolean equals(Object other) {
            if (other instanceof NumberExpression) {
                final NumberExpression otherNumber    = (NumberExpression) other;
                return otherNumber._value == _value;
            }
            return false;
        }

        @Override
        boolean lt(Object other) {
            if (other instanceof NumberExpression) {
                final NumberExpression otherNumber    = (NumberExpression) other;
                return otherNumber._value < _value;
            }
            return false;
        }

        @Override
        boolean le(Object other) {
            if (other instanceof NumberExpression) {
                final NumberExpression otherNumber    = (NumberExpression) other;
                return otherNumber._value <= _value;
            }
            return false;
        }

        @Override
        boolean gt(Object other) {
            if (other instanceof NumberExpression) {
                final NumberExpression otherNumber    = (NumberExpression) other;
                return otherNumber._value > _value;
            }
            return false;
        }

        @Override
        boolean ge(Object other) {
            if (other instanceof NumberExpression) {
                final NumberExpression otherNumber    = (NumberExpression) other;
                return otherNumber._value >= _value;
            }
            return false;
        }
    }

    static class BooleanExpression extends Expression {
        boolean _value;

        BooleanExpression(boolean value) {
            _value = value;
        }

        boolean value() {
            return _value;
        }

        @Override
        Expression evaluate() {
            return this;
        }
    }

    abstract static class MemoryExpression extends Expression {
    }

    class AddressMemoryExpression extends MemoryExpression {
        Address _address;

        AddressMemoryExpression(Address address) {
            _address = address;
        }

        void setAddress(Address address) {
            _address = address;
        }

        Address address() {
            return _address;
        }

        @Override
        Expression evaluate() {
            return new NumberExpression(teleProcess().dataAccess().readLong(_address));
        }
    }

    class RegisterMemoryExpression extends MemoryExpression {
        RegisterExpression _registerExpression;
        AddressMemoryExpression _addressMemoryExpression = new AddressMemoryExpression(Address.zero());

        RegisterMemoryExpression(RegisterExpression registerExpression) {
            _registerExpression = registerExpression;
        }

        @Override
        Expression evaluate() {
            final NumberExpression addressExpression = (NumberExpression) _registerExpression.evaluate();
            _addressMemoryExpression.setAddress(Address.fromLong(addressExpression.value()));
            return _addressMemoryExpression.evaluate();
        }
    }

    class OffsetRegisterMemoryExpression extends RegisterMemoryExpression {
        int _offset;
        Operator _operator;

        OffsetRegisterMemoryExpression(RegisterExpression registerExpression, Operator operator, NumberExpression offset) {
            super(registerExpression);
            _offset = (int) offset.value();
            _operator = operator;
        }

        @Override
        Expression evaluate() {
            final NumberExpression addressExpression = (NumberExpression) _registerExpression.evaluate();
            Address address = Address.fromLong(addressExpression.value());
            switch (_operator) {
                case PLUS:
                    address = address.plus(_offset);
                    break;
                case MINUS:
                    address = address.minus(_offset);
                    break;
                default:
            }
            return new AddressMemoryExpression(address).evaluate();
        }
    }

    enum Operator {
        LT,
        LE,
        EQ,
        NE,
        GE,
        GT,
        PLUS,
        MINUS,
        LOGAND,
        LOGOR
    }
}
