*******************************************************************************
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
*******************************************************************************

This directory contains ant files to build the javadocs of the product.
The following rules describe the convention to write such files:

- An ant file must build only one javadoc.

- As there may exist several javadocs, all javadocs must be produced
  in a sub dir of ${out.dist.jdoc}. For example the user javadoc could be 
  produced into the ${out.dist.jdoc}/user directory

- The name of the ant file must be the name of the destination directory of the 
  javadoc it builds.

- Only the default task is called on an xml file.

Sample ant file:

<project name="FOO" default="dist.jdoc">

  <property name="jdoc.name" value="user"/>
  <property name="jdoc.dir" value="${out.dist.jdoc}/${jdoc.name}"/>

  <target name="dist.jdoc">
    <uptodate property="jdoc.required" targetfile="${jdoc.dir}/index.html">
      <srcfiles dir="${src}" includes="**/*.java"/>
    </uptodate>
    <antcall target="dist.jdoc.${jdoc.name}"/>
  </target>

  <target name="dist.jdoc.user" unless="jdoc.required">
    <mkdir dir="${jdoc.dir}"/>
    <javadoc destdir="${jdoc.dir}"
             windowtitle="FOO User Documentation"
             doctitle="FOO User Documentation">

       ...
       
    </javadoc>
  </target>
</project>
