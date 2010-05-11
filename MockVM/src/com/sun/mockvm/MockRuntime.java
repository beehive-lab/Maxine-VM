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
package com.sun.mockvm;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.reflect.Method;

import com.sun.cri.ci.CiTargetMethod;
import com.sun.cri.ci.CiTargetMethod.Call;
import com.sun.cri.ci.CiTargetMethod.DataPatch;
import com.sun.cri.ci.CiTargetMethod.Safepoint;
import com.sun.cri.ri.RiConstantPool;
import com.sun.cri.ri.RiMethod;
import com.sun.cri.ri.RiOsrFrame;
import com.sun.cri.ri.RiRuntime;
import com.sun.cri.ri.RiSnippets;
import com.sun.cri.ri.RiType;
import com.sun.max.asm.InstructionSet;
import com.sun.max.asm.dis.DisassembledObject;
import com.sun.max.asm.dis.Disassembler;
import com.sun.max.asm.dis.DisassemblyPrinter;
import com.sun.max.io.IndentWriter;
import com.sun.max.lang.WordWidth;

/**
 * 
 * @author Thomas Wuerthinger
 *
 */
public class MockRuntime implements RiRuntime {

	@Override
	public int codeOffset() {
		return 0;
	}

	@Override
	public void codePrologue(RiMethod method, OutputStream out) {
	}

	@Override
	public String disassemble(byte[] code) {
		return disassemble(code, new DisassemblyPrinter(false));
	}
	
	private String disassemble(byte[] code, DisassemblyPrinter disassemblyPrinter) {
        final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        final IndentWriter writer = new IndentWriter(new OutputStreamWriter(byteArrayOutputStream));
        writer.flush();
        final InstructionSet instructionSet = InstructionSet.AMD64;;
        Disassembler.disassemble(byteArrayOutputStream, code, instructionSet, WordWidth.BITS_64, 0, null, disassemblyPrinter);
        return byteArrayOutputStream.toString();
	}

	@Override
	public String disassemble(final CiTargetMethod targetMethod) {
		
		final DisassemblyPrinter disassemblyPrinter = new DisassemblyPrinter(false) {
            private String toString(Call call) {
                if (call.runtimeCall != null) {
                    return "{" + call.runtimeCall.name() + "}";
                } else if (call.symbol != null) {
                    return "{" + call.symbol + "}";
                } else if (call.globalStubID != null) {
                    return "{" + call.globalStubID + "}";
                } else {
                    return "{" + call.method + "}";
                }
            }
            private String siteInfo(int pcOffset) {
                for (Call call : targetMethod.directCalls) {
                    if (call.pcOffset == pcOffset) {
                        return toString(call);
                    }
                }
                for (Call call : targetMethod.indirectCalls) {
                    if (call.pcOffset == pcOffset) {
                        return toString(call);
                    }
                }
                for (Safepoint site : targetMethod.safepoints) {
                    if (site.pcOffset == pcOffset) {
                        return "{safepoint}";
                    }
                }
                for (DataPatch site : targetMethod.dataReferences) {
                    if (site.pcOffset == pcOffset) {
                        return "{" + site.data + "}";
                    }
                }
                return null;
            }

            @Override
            protected String disassembledObjectString(Disassembler disassembler, DisassembledObject disassembledObject) {
                final String string = super.disassembledObjectString(disassembler, disassembledObject);

                String site = siteInfo(disassembledObject.startPosition());
                if (site != null) {
                    return string + " " + site;
                }
                return string;
            }
        };
		return disassemble(targetMethod.targetCode(), disassemblyPrinter);
	}

	@Override
	public String disassemble(RiMethod method) {
		return "No disassembler available";
	}

	@Override
	public RiConstantPool getConstantPool(RiMethod method) {
		
		final MockMethod mockMethod = (MockMethod) method;
		return mockMethod.getHolder().getConstantPool();
	}

	@Override
	public RiOsrFrame getOsrFrame(RiMethod method, int bci) {
		throw new UnsupportedOperationException();
	}

	@Override
	public RiType getRiType(Class<?> javaClass) {
	        return MockUniverse.lookupType(javaClass);
	}

	@Override
	public RiSnippets getSnippets() {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean mustInline(RiMethod method) {
		return false;
	}

	@Override
	public boolean mustNotCompile(RiMethod method) {
		return false;
	}

	@Override
	public boolean mustNotInline(RiMethod method) {
		return false;
	}

	@Override
	public Object registerTargetMethod(CiTargetMethod targetMethod, String name) {
		return targetMethod;
	}

	@Override
	public int sizeofBasicObjectLock() {
		return 0;
	}	
	
	@Override
	public int basicObjectLockOffsetInBytes() {
		return 0;
	}

	@Override
	public int threadExceptionOffset() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Method getFoldingMethod(RiMethod method) {
		throw new UnsupportedOperationException();
	}
}
