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
package com.sun.hotspot.c1x;

import java.util.ArrayList;
import java.util.List;

import com.sun.cri.ci.CiKind;
import com.sun.cri.ri.RiField;
import com.sun.cri.ri.RiMethod;
import com.sun.cri.ri.RiType;
import com.sun.cri.ri.RiType.Representation;
import com.sun.cri.xir.CiXirAssembler;
import com.sun.cri.xir.RiXirGenerator;
import com.sun.cri.xir.XirArgument;
import com.sun.cri.xir.XirSite;
import com.sun.cri.xir.XirSnippet;
import com.sun.cri.xir.XirTemplate;
import com.sun.cri.xir.CiXirAssembler.XirOperand;

/**
 * 
 * @author Thomas Wuerthinger
 *
 */
public class HotSpotXirGenerator extends RiXirGenerator {

	private XirTemplate[] emptyTemplates = new XirTemplate[CiKind.values().length];
	
	@Override
	public List<XirTemplate> buildTemplates(CiXirAssembler asm) {

		List<XirTemplate> templates = new ArrayList<XirTemplate>();
		for (int i=0; i<CiKind.values().length; i++) {
			
			CiKind curKind = CiKind.values()[i];

			if (curKind == CiKind.Float || curKind == CiKind.Double) continue;
			
			if (CiKind.values()[i] == CiKind.Void) {
				asm.restart(CiKind.values()[i]);
				emptyTemplates[i] = asm.finishTemplate("empty-" + CiKind.values()[i]);
			} else {
				asm.restart();
				XirOperand result = asm.createTemp("result", CiKind.values()[i]);
				emptyTemplates[i] = asm.finishTemplate(result, "empty-" + CiKind.values()[i]);
			}
			templates.add(emptyTemplates[i]);
		}
		
		
		return templates;
	}
	
	@Override
	public XirSnippet genArrayLength(XirSite site, XirArgument array) {
		return new XirSnippet(emptyTemplates[CiKind.Int.ordinal()]);
	}

	@Override
	public XirSnippet genArrayLoad(XirSite site, XirArgument array,
			XirArgument index, XirArgument length, CiKind elementKind,
			RiType elementType) {
		return new XirSnippet(emptyTemplates[elementKind.ordinal()]);
	}

	@Override
	public XirSnippet genArrayStore(XirSite site, XirArgument array,
			XirArgument index, XirArgument length, XirArgument value,
			CiKind elementKind, RiType elementType) {
		return new XirSnippet(emptyTemplates[CiKind.Void.ordinal()]);
	}

	@Override
	public XirSnippet genCheckCast(XirSite site, XirArgument receiver,
			XirArgument hub, RiType type) {
		return new XirSnippet(emptyTemplates[CiKind.Object.ordinal()]);
	}

	@Override
	public XirSnippet genEntrypoint(XirSite site) {
		return null;
	}

	@Override
	public XirSnippet genGetField(XirSite site, XirArgument receiver,
			RiField field) {
		return new XirSnippet(emptyTemplates[field.kind().ordinal()]);
	}

	@Override
	public XirSnippet genGetStatic(XirSite site, XirArgument staticTuple,
			RiField field) {
		return new XirSnippet(emptyTemplates[field.kind().ordinal()]);
	}

	@Override
	public XirSnippet genInstanceOf(XirSite site, XirArgument receiver,
			XirArgument hub, RiType type) {
		return new XirSnippet(emptyTemplates[CiKind.Boolean.ordinal()]);
	}

	@Override
	public XirSnippet genIntrinsic(XirSite site, XirArgument[] arguments,
			RiMethod method) {
		return null;
	}

	@Override
	public XirSnippet genInvokeInterface(XirSite site, XirArgument receiver,
			RiMethod method) {
		return new XirSnippet(emptyTemplates[CiKind.Word.ordinal()]);
	}

	@Override
	public XirSnippet genInvokeSpecial(XirSite site, XirArgument receiver,
			RiMethod method) {
		return new XirSnippet(emptyTemplates[CiKind.Word.ordinal()]);
	}

	@Override
	public XirSnippet genInvokeStatic(XirSite site, RiMethod method) {
		return new XirSnippet(emptyTemplates[CiKind.Word.ordinal()]);
	}

	@Override
	public XirSnippet genInvokeVirtual(XirSite site, XirArgument receiver,
			RiMethod method) {
		return new XirSnippet(emptyTemplates[CiKind.Word.ordinal()]);
	}

	@Override
	public XirSnippet genMonitorEnter(XirSite site, XirArgument receiver) {
		return new XirSnippet(emptyTemplates[CiKind.Void.ordinal()]);
	}

	@Override
	public XirSnippet genMonitorExit(XirSite site, XirArgument receiver) {
		return new XirSnippet(emptyTemplates[CiKind.Void.ordinal()]);
	}

	@Override
	public XirSnippet genNewArray(XirSite site, XirArgument length,
			CiKind elementKind, RiType componentType, RiType arrayType) {
		return new XirSnippet(emptyTemplates[CiKind.Object.ordinal()]);
	}

	@Override
	public XirSnippet genNewInstance(XirSite site, RiType type) {
		return new XirSnippet(emptyTemplates[CiKind.Object.ordinal()]);
	}

	@Override
	public XirSnippet genNewMultiArray(XirSite site, XirArgument[] lengths,
			RiType type) {
		return new XirSnippet(emptyTemplates[CiKind.Object.ordinal()]);
	}

	@Override
	public XirSnippet genPutField(XirSite site, XirArgument receiver,
			RiField field, XirArgument value) {
		return new XirSnippet(emptyTemplates[CiKind.Void.ordinal()]);
	}

	@Override
	public XirSnippet genPutStatic(XirSite site, XirArgument staticTuple,
			RiField field, XirArgument value) {
		return new XirSnippet(emptyTemplates[CiKind.Void.ordinal()]);
	}

	@Override
	public XirSnippet genResolveClass(XirSite site, RiType type,
			Representation representation) {
		return new XirSnippet(emptyTemplates[CiKind.Object.ordinal()]);
	}

	@Override
	public XirSnippet genSafepoint(XirSite site) {
		return new XirSnippet(emptyTemplates[CiKind.Void.ordinal()]);
	}

}
