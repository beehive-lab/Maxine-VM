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
package com.sun.c1x.asm;

import com.sun.c1x.asm.RelocInfo.*;

/**
 *
 * @author Thomas Wuerthinger
 *
 */
public class Relocation {



    public Type type() {
        // TODO Auto-generated method stub
        return null;
    }

    public boolean isCall() {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean isData() {
        // TODO Auto-generated method stub
        return false;
    }

    public static RelocationHolder specExternalWord(Pointer loc) {
        // TODO Auto-generated method stub
        return null;
    }

    public static RelocationHolder specInternalWord(Pointer loc) {
        // TODO Auto-generated method stub
        return null;
    }

    public static RelocationHolder specRuntimeCall() {
        // TODO Auto-generated method stub
        return null;
    }

    public static RelocationHolder specSimple(Pointer loc) {
        // TODO Auto-generated method stub
        return null;
    }

    public static RelocationHolder specOptVirtualCallRelocation(long address) {
        // TODO Auto-generated method stub
        return null;
    }

    public static RelocationHolder specStaticCallRelocation(long address) {
        // TODO Auto-generated method stub
        return null;
    }

    // from here


    private static void guaranteeSize(){

    }
    // When a relocation has been created by a RelocIterator,
    // this field is non-null.  It allows the relocation to know
    // its context, such as the address to which it applies.
    private RelocIterator binding;


    protected RelocIterator binding()  {
      assert binding != null :  "must be bound";
      return binding;
    }
    protected void setBinding(RelocIterator b) {
      assert binding == null :  "must be unbound";
      binding = b;
      assert binding != null :  "must now be bound";
    }

    protected Relocation() {
      binding = null;
    }

    protected static RelocationHolder newHolder() {
      return new RelocationHolder();
    }


//    public void operator new(sizeT size,  RelocationHolder& holder) {
//      if (size > sizeof(holder.relocbuf)) guaranteeSize();
//      assert (void  *)holder.reloc() == &holder.relocbuf[0] :  "ptrs must agree";
//      return holder.reloc();
//    }

    // make a generic relocation for a given type (if possible)
    public static RelocationHolder specSimple(Type rtype){
        if (rtype == RelocInfo.Type.none) {
            return RelocationHolder.none;
        }
        RelocInfo ri = new RelocInfo(rtype, 0);
        RelocIterator itr;
//        itr.setCurrent(ri);
//        itr.reloc();
//        return itr.rh;
        return null;
    }



}
