package com.sun.c1x.ir;

import com.sun.c1x.ci.*;
import com.sun.c1x.util.*;
import com.sun.c1x.value.*;


/**
 * An instruction that represents the runtime resolution of a Java class object. For example, an ldc of a class constant that is unresolved.
 *
 * @author Ben L. Titzer
 * @author Thomas Wuerthinger
 *
 */
public class ResolveClass extends Instruction {

    public final CiType ciType;
    private final ValueStack state;

    public ResolveClass(CiType type, ValueStack stack) {
        super(ValueType.OBJECT_TYPE);
        this.ciType = type;
        assert stack != null;
        this.state = stack;
        setFlag(Flag.NonNull);
    }

    @Override
    public void accept(InstructionVisitor v) {
        v.visitResolveClass(this);
    }

    public ValueStack state() {
        return state;
    }

    @Override
    public boolean canTrap() {
        return true;
    }

    /**
     * Iterates over the "other" values in this instruction. In the case of constants,
     * this method iterates over any values in the state if this constant may need patching.
     * @param closure the closure to apply to each value
     */
    @Override
    public void otherValuesDo(InstructionClosure closure) {
        state.valuesDo(closure);
    }

    @Override
    public int valueNumber() {
        return ciType.hashCode() | 0x50000000;
    }

    @Override
    public boolean valueEqual(Instruction i) {
        if (i instanceof ResolveClass) {
            final ResolveClass other = (ResolveClass) i;
            return other.ciType.equals(ciType);
        }
        return false;
    }
}
