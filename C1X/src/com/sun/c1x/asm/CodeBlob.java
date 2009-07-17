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


public class CodeBlob {

     char name;
     int size; // total size of CodeBlob in bytes
     int headerSize; // size of header (depends on subclass)
     int relocationSize; // size of relocation
     int instructionsOffset; // offset to where instructions region begins
     int frameCompleteOffset; // instruction offsets in [0..frameCompleteOffset) have
                                     // not finished setting up their frame. Beware of pc's in
                                     // that range. There is a similar range(s) on returns
                                     // which we don't detect.
     int dataOffset; // offset to where data region begins
     int oopsOffset; // offset to where embedded oop table begins (inside data)
     int oopsLength; // number of embedded oops
     int frameSize; // size of stack frame
         OopMapSet oopMaps;                          // OopMap for this CodeBlob
     CodeComments comments;


     void fixOopRelocations(Pointer begin, Pointer end, boolean initializeImmediates) {

     }

     void initializeImmediateOop(OopDesc dest, Object handle) {

     }

     // Returns the space needed for CodeBlob
     public static int allocationSize(CodeBuffer cb, int headerSize) {
         return 0; // TODO Not implemented yet
     }

     // Creation
     // a) simple CodeBlob
     //    frameComplete is the offset from the beginning of the instructions
     //    to where the frame setup (from stackwalk viewpoint) is complete.
     public CodeBlob(char name, int headerSize, int size, int frameComplete, int locsSize) {

     }

     // b) full CodeBlob
     public CodeBlob(char name, CodeBuffer cb, int headerSize, int size, int frameComplete, int frameSize, OopMapSet oopMaps) {

     }

     // Deletion
     void flush() {

     }

     // Typing
    public boolean isBufferBlob() {
        return false;
    }

    public boolean isNmethod() {
        return false;
    }

    public boolean isRuntimeStub() {
        return false;
    }

    public boolean isDeoptimizationStub() {
        return false;
    }

    public boolean isUncommonTrapStub() {
        return false;
    }

    public boolean isExceptionStub() {
        return false;
    }

    public boolean isSafepointStub() {
        return false;
    }

    public boolean isAdapterBlob() {
        return false;
    }

    public boolean isCompiledByC2() {
        return false;
    }

    public boolean isCompiledByC1() {
        return false;
    }

//     // Boundaries
//     Pointer    headerBegin()                 { return new Pointer(null, 0); }
//     Pointer    headerEnd()                   { return Pointer.class. }

//     RelocInfo relocationBegin()             { return (relocInfo) headerEnd(); };
//     relocInfo relocationEnd()               { return (relocInfo)(headerEnd()   + relocationSize); }
//     Pointer    instructionsBegin()           { return (Pointer)    headerBegin() + instructionsOffset;  }
//     Pointer    instructionsEnd()             { return (Pointer)    headerBegin() + dataOffset; }
//     Pointer    dataBegin()                   { return (Pointer)    headerBegin() + dataOffset; }
//     Pointer    dataEnd()                     { return (Pointer)    headerBegin() + size; }
//     oop       oopsBegin()                   { return (oop)      (headerBegin() + oopsOffset); }
//     oop       oopsEnd()                     { return                oopsBegin() + oopsLength; }
//
//     // Offsets
//     int relocationOffset()                   { return headerSize; }
//     int instructionsOffset()                 { return instructionsOffset; }
//     int dataOffset()                         { return dataOffset; }
//     int oopsOffset()                         { return oopsOffset; }
//
//     // Sizes
//     int size()                                { return size; }
//     int headerSize()                         { return headerSize; }
//     int relocationSize()                     { return (address) relocationEnd() - (address) relocationBegin(); }
//     int instructionsSize()                   { return instructionsEnd() - instructionsBegin();  }
//     int dataSize()                           { return dataEnd() - dataBegin(); }
//     int oopsSize()                           { return (address) oopsEnd() - (address) oopsBegin(); }
//
//     // Containment
//     boolean blobContains(address addr)          { return headerBegin()       <= addr && addr < dataEnd(); }
//     boolean relocationContains(relocInfo addr) { return relocationBegin()   <= addr && addr < relocationEnd(); }
//     boolean instructionsContains(address addr)  { return instructionsBegin() <= addr && addr < instructionsEnd(); }
//     boolean dataContains(address addr)          { return dataBegin()         <= addr && addr < dataEnd(); }
//     boolean oopsContains(oop addr)             { return oopsBegin()         <= addr && addr < oopsEnd(); }
//     boolean contains(address addr)               { return instructionsContains(addr); }
//     boolean isFrameCompleteAt(address addr)   { return instructionsContains(addr) &&
//                                                             addr >= instructionsBegin() + frameCompleteOffset; }
//
//     // Relocation support
//     void fixOopRelocations(address begin, address end) {
//       fixOopRelocations(begin, end, false);
//     }
//     void fixOopRelocations() {
//       fixOopRelocations(null, null, false);
//     }
//     relocInfo.relocType relocTypeForAddress(address pc);
//     boolean isAtPollReturn(address pc);
//     boolean isAtPollOrPollReturn(address pc);
//
//     // Support for oops in scopes and relocs:
//     // Note: index 0 is reserved for null.
//     oop  oopAt(int index)                    { return index == 0? (oop)null: *oopAddrAt(index); }
//     oop oopAddrAt(int index) {             // for GC
//       // relocation indexes are biased by 1 (because 0 is reserved)
//       assert index > 0 && index <= oopsLength :  "must be a valid non-zero index";
//       return &oopsBegin()[index-1];
//     }
//
//     void copyOops(GrowableArray<jobject> oops);
//
//     // CodeCache support: really only used by the nmethods, but in order to get
//     // assert  and certain bookkeeping to work in the CodeCache they are defined
//     // virtual here.
//     virtual boolean isZombie()                  { return false; }
//     virtual boolean isLockedByVm()            { return false; }
//
//     virtual boolean isUnloaded()                { return false; }
//     virtual boolean isNotEntrant()             { return false; }
//
//     // GC support
//     virtual boolean isAlive()                   = 0;
//     virtual void doUnloading(BoolObjectClosure isAlive,
//                               OopClosure keepAlive,
//                               boolean unloadingOccurred);
//     virtual void oopsDo(OopClosure f) = 0;
//
//     // OopMap for frame
//     OopMapSet oopMaps()                     { return oopMaps; }
//     void setOopMaps(OopMapSet p);
//     OopMap oopMapForReturnAddress(address returnAddress);
//     virtual void preserveCalleeArgumentOops(frame fr,  RegisterMap regMap, OopClosure f)  { Util.shouldNotReachHere(); }
//
//     // Frame support
//     int  frameSize()                         { return frameSize; }
//     void setFrameSize(int size)                  { frameSize = size; }
//
//     // Returns true :  if the next frame is responsible for GC'ing oops passed as arguments
//     virtual boolean callerMustGcArguments(JavaThread thread)  { return false; }
//
//     // Naming
//      char name()                        { return name; }
//     void setName( char name)                { name = name; }
//
//     // Debugging
//     virtual void verify();
//     virtual void print()                      PRODUCTRETURN;
//     virtual void printValueOn(outputStream st)  PRODUCTRETURN;
//
//     // Print the comment associated with offset on stream :  if there is one
//     void printBlockComment(outputStream stream, intptrT offset) {
//       comments.printBlockComment(stream, offset);
//     }
//
//     // Transfer ownership of comments to this CodeBlob
//     void setComments(CodeComments& comments) {
//       comments.assign(comments);
//     }


    public void decode() {
        // TODO Auto-generated method stub

    }


}
