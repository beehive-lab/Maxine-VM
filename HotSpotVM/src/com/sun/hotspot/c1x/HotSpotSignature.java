package com.sun.hotspot.c1x;

import java.util.ArrayList;
import java.util.List;

import com.sun.cri.ci.CiKind;
import com.sun.cri.ri.RiSignature;
import com.sun.cri.ri.RiType;

public class HotSpotSignature implements RiSignature {

    private final List<String> arguments = new ArrayList<String>();
    private final String returnType;
    private final String originalString;

    public HotSpotSignature(String signature) {

        assert signature.length() > 0;
        this.originalString = signature;

        if (signature.charAt(0) == '(') {
            int cur = 1;
            while (cur < signature.length() && signature.charAt(cur) != ')') {
                int nextCur = parseSignature(signature, cur);
                arguments.add(signature.substring(cur, nextCur));
                cur = nextCur;
            }

            cur++;
            int nextCur = parseSignature(signature, cur);
            returnType = signature.substring(cur, nextCur);
            assert nextCur == signature.length();
        } else {
            returnType = null;
        }
    }

    private int parseSignature(String signature, int cur) {

        char first = signature.charAt(cur);
        switch (first) {

            case '[':
                return parseSignature(signature, cur + 1);

            case 'L':
                while (signature.charAt(cur) != ';')
                    cur++;
                cur++;
                break;

            case 'V':
            case 'I':
            case 'B':
            case 'C':
            case 'D':
            case 'F':
            case 'J':
            case 'S':
            case 'Z':
                cur++;
                break;

            default:
                assert false;

        }

        return cur;
    }

    @Override
    public int argumentCount(boolean withReceiver) {
        return arguments.size() + (withReceiver ? 1 : 0);
    }

    @Override
    public CiKind argumentKindAt(int index) {
        CiKind kind = CiKind.fromTypeString(arguments.get(index));
        System.out.println("argument kind: " + index + " is " + kind);
        return kind;
    }

    @Override
    public int argumentSlots(boolean withReceiver) {

        int argSlots = 0;
        for (int i = 0; i < argumentCount(false); i++) {
            argSlots += argumentKindAt(i).sizeInSlots();
        }

        return argSlots + (withReceiver ? 1 : 0);
    }

    @Override
    public RiType argumentTypeAt(int index, RiType accessingClass) {
    	System.out.println("argument type at " + index);
    	Object accessor = null;
    	if (accessingClass instanceof HotSpotType) {
    		accessor = ((HotSpotType)accessingClass).klassOop;
    	}
        return VMEntries.RiSignature_lookupType(arguments.get(index), accessor);
    }

    @Override
    public String asString() {
        return originalString;
    }

    @Override
    public CiKind returnKind() {
        return CiKind.fromTypeString(returnType);
    }

    @Override
    public RiType returnType(RiType accessingClass) {
        return VMEntries.RiSignature_lookupType(returnType, accessingClass);
    }

}
