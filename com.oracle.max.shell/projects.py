#
# projdb.py - library for accessing Maxine projects.properties files
#
# ----------------------------------------------------------------------------------------------------
#
# Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
# DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
#
# This code is free software; you can redistribute it and/or modify it
# under the terms of the GNU General Public License version 2 only, as
# published by the Free Software Foundation.
#
# This code is distributed in the hope that it will be useful, but WITHOUT
# ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
# FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
# version 2 for more details (a copy is included in the LICENSE file that
# accompanied this code).
#
# You should have received a copy of the GNU General Public License version
# 2 along with this work; if not, write to the Free Software Foundation,
# Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
#
# Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
# or visit www.oracle.com if you need additional information or have any
# questions.
#
# ----------------------------------------------------------------------------------------------------

from os.path import isabs, exists, join, expandvars
import os
import types

class Dependency:
    def __init__(self, name, baseDir):
        self.name = name
        self.baseDir = baseDir
        self.env = None
        
    def __str__(self):
        return self.name
    
    def __eq__(self, other):
        return self.name == other.name
    
    def __ne__(self, other):
        return self.name != other.name

    def __hash__(self):
        return hash(self.name)
    
class Project(Dependency):
    def __init__(self, baseDir, name, srcDirs, deps):
        Dependency.__init__(self, name, baseDir)
        self.srcDirs = srcDirs
        self.deps = deps
        self.eclipseOutput = 'bin'
        self.checkstyleProj = name

    def print_properties(self, env):
        env.log('project@' + self.name + '@sourceDirs=' + ', '.join(self.srcDirs))
        env.log('project@' + self.name + '@dependencies=' + ', '.join(self.deps))
        env.log('project@' + self.name + '@eclipse.ouput=' + self.eclipseOutput)
        
    def all_deps(self, deps, pdb, includeLibs):
        if self in deps:
            return deps
        for name in self.deps:
            assert name != self.name
            dep = pdb.libs.get(name, None)
            if dep is not None:
                if includeLibs and not dep in deps:
                    deps.append(dep)
            else:
                dep = pdb.project(name)
                if not dep in deps:
                    dep.all_deps(deps, pdb, includeLibs)
        if not self in deps:
            deps.append(self)
        return deps
    
    def _compute_max_dep_distances(self, name, distances, dist, pdb):
        currentDist = distances.get(name);
        if currentDist is None or currentDist < dist:
            distances[name] = dist
            if pdb.projects.has_key(name):
                p = pdb.project(name)
                for dep in p.deps:
                    self._compute_max_dep_distances(dep, distances, dist + 1, pdb)
                

    def canonical_deps(self, env, pdb):
        distances = dict()
        result = set()
        self._compute_max_dep_distances(self.name, distances, 0, pdb)
        for n,d in distances.iteritems():
            assert d > 0 or n == self.name
            if d == 1:
                result.add(n)
                
            
        if len(result) == len(self.deps) and frozenset(self.deps) == result:
            return self.deps
        return result;
    

    def source_dirs(self):
        return [join(self.baseDir, self.name, s) for s in self.srcDirs]
        
    def output_dir(self):
        if os.getenv('MAXINE_IDE', None) is 'INTELLIJ':
            return join(self.baseDir, 'out', 'production', self.name)
        else:
            return join(self.baseDir, self.name, 'bin')

    def classpath(self, resolve, env):
        classesDir = join(self.baseDir, 'classes')
        if exists(classesDir):
            return [self.output_dir(), classesDir]
        return [self.output_dir()]
    


class Library(Dependency):
    def __init__(self, baseDir, name, path, mustExist, urls):
        Dependency.__init__(self, name, baseDir)
        self.path = path
        self.urls = urls
        self.mustExist = mustExist
    
    def print_properties(self, env):
        env.log('library@' + self.name + '@path=' + self.path);
        env.log('library@' + self.name + '@optional=' + str(not self.mustExist))
        env.log('library@' + self.name + '@urls=' +  ', '.join(self.urls))
        if self.eclipseContainer is not None:
            env.log('library@' + self.name + '@eclipse.container=' + self.eclipseContainer)
        
    def classpath(self, resolve, env):
        path = self.path
        if not isabs(path):
            path = join(self.baseDir, path)
        if resolve and self.mustExist and not exists(path):
            assert not len(self.urls) == 0, 'cannot find required library  ' + self.name;
            env.download(path, self.urls)

        if exists(path) or not resolve:
            return [path]
        return []
    
class ProjectsDB():
    def _load(self, d):
        env = self.env
        libsMap = dict()
        projsMap = dict() 
        with open(join(d, 'projects.properties')) as f:
            for line in f:
                line = line.strip()
                if len(line) != 0 and line[0] != '#':
                    key, value = line.split('=', 1)
                    
                    parts = key.split('@')
                    if len(parts) != 3:
                        env.abort('Property name does not have 3 parts separated by "@": ' + key)
                    kind, name, attr = parts
                    if kind == 'project':
                        m = projsMap
                    elif kind == 'library':
                        m = libsMap
                    else:
                        env.abort('Property name does not start with "project@" or "library@": ' + key)
                        
                    attrs = m.get(name)
                    if attrs is None:
                        attrs = dict()
                        m[name] = attrs
                    attrs[attr] = value
                        
        def get_list(attrs, name):
            v = attrs.get(name)
            if v is None or len(v.strip()) == 0:
                return []
            return [n.strip() for n in v.split(',')]
            
        for name, attrs in projsMap.iteritems():
            if self.projects.has_key(name):
                env.abort('cannot override project  ' + name + ' in ' + self.project(name).baseDir + " with project of the same name in  " + d)
            srcDirs = get_list(attrs, 'sourceDirs')
            deps = get_list(attrs, 'dependencies')
            p = Project(d, name, srcDirs, deps)
            p.eclipseOutput = attrs.get('eclipse.output', 'bin')
            p.checkstyleProj = attrs.get('checkstyle', name)
            self.projects[name] = p

        for name, attrs in libsMap.iteritems():
            if self.libs.has_key(name):
                env.abort('cannot redefine library ' + name)
            
            path = expandvars(attrs['path'])
            mustExist = attrs.get('optional', 'false') != 'true'
            urls = get_list(attrs, 'urls')
            l = Library(d, name, path, mustExist, urls)
            l.eclipseContainer = attrs.get('eclipse.container', None);
            self.libs[name] = l
        
    def project_names(self):
        return ' '.join(self.projects.keys())
        
    def project(self, name):
        p = self.projects.get(name)
        if p is None:
            self.env.abort('project named ' + name + ' not found')
        return p
    
    def library(self, name):
        l = self.libs.get(name)
        if l is None:
            self.env.abort('library named ' + name + ' not found')
        return l

    def _as_classpath(self, deps, resolve):
        cp = []
        if self.env.cp_prefix is not None:
            cp = [self.env.cp_prefix]
        for d in deps:
            cp += d.classpath(resolve, self.env)
        if self.env.cp_suffix is not None:
            cp += [self.env.cp_suffix]
        return os.pathsep.join(cp)

    def classpath(self, names=None, resolve=True):
        if names is None:
            return self._as_classpath(self.sorted_deps(True), resolve)
        deps = []
        if isinstance(names, types.StringTypes):
            self.project(names).all_deps(deps, self, True)
        else:
            for n in names:
                self.project(n).all_deps(deps, self, True)
        return self._as_classpath(deps, resolve)
        
    def sorted_deps(self, includeLibs=False):
        deps = []
        for p in self.projects.itervalues():
            p.all_deps(deps, self, includeLibs)
        return deps
    
    def __init__(self, env):
        self.env = env
        self.projects = dict()
        self.libs = dict()
        
        d = env.maxine_home 
        self.projectDirs = [d]
        self._load(d)
        for d in env.extraProjectDirs:
            self.projectDirs += [d]
            self._load(d)
        

