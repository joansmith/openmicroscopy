#!/usr/bin/env python
"""
   Library for managing user sessions.

   Copyright 2010 Glencoe Software, Inc. All rights reserved.
   Use is subject to license terms supplied in LICENSE.txt

"""

"""
 * Track last used
 * provide single library (with lock) which does all of this
   - save session
   - clear session
   - check session # detachOnDestroy
   - list previous sessions
 * Use an environment variable for changing directories

import subprocess, optparse, os, sys
import getpass, pickle
import omero.java
from omero.cli import Arguments, BaseControl, VERSION
from path import path

"""

from omero.util import get_user_dir, make_logname
from path import path

import logging
import exceptions


class SessionsStore(object):
    """
    The store is a file-based repository of user sessions.
    By default, stores use $HOME/omero/sessions as their
    repository path.

    Use add() to add items to the repository
    """

    def __init__(self, dir = None):
        """
        """
        self.logger = logging.getLogger(make_logname(self))
        if dir == None:
            dir = get_user_dir()
        self.dir = path(dir) / "omero" / "sessions"
        if not self.dir.exists():
            self.dir.makedirs()
        try:
            self.dir.chmod(0700)
        except:
            print "WARN: failed to chmod %s" % self.dir

    #
    # File-only methods
    #

    def report(self):
        """
        Simple dump utility
        """
        for host in self.dir.dirs():
            print "[%s]" % host
            for name in host.dirs():
                print " -> %s : " % name
                for sess in name.files():
                    print "    %s" % sess

    def add(self, host, name, id, props):
        """
        Stores a file containing the properties at
        REPO/host/name/id
        """

        props["omero.host"] = host
        props["omero.user"] = name
        props["omero.sess"] = id

        lines = []
        for k,v in props.items():
            lines.append("%s=%s" % (k, v))

        dhn = self.dir / host / name
        if not dhn.exists():
            dhn.makedirs()

        (dhn / id).write_lines(lines)
        self.set_current(host, name, id)

    def conflicts(self, host, name, id, new_props):
        """
        Compares if the passed properties are compatible with
        with those for the host, name, id tuple
        """
        conflicts = ""
        old_props = self.get(host, name, id)
        for key in ("omero.group", "omero.port"):
            vals = [x.get(key, None) for x in (old_props, new_props)]
            if vals[0] != vals[1]:
                conflicts += (key + (":%s!=%s;" % tuple(vals)))
        return conflicts

    def remove(self, host, name, uuid):
        """
        Removes the given session file from the store
        """
        (self.dir / host / name / uuid).remove()

    def get(self, host, name, uuid):
        """
        Returns the properties stored in the given session file
        """
        return self.props(self.dir / host / name / uuid)

    def available(self, host, name):
        """
        Returns the path to property files which are stored.
        Internal accounting files are not returned.
        """
        d = self.dir / host / name
        if not d.exists():
            return []
        return [x.basename() for x in self.non_dot(d)]

    def set_current(self, host, name = None, uuid = None):
        """
        Sets the current session, user, and host files
        These are used as defaults by other methods.
        """
        if host is not None: self.host_file().write_text(host)
        if name is not None: self.user_file(host).write_text(name)
        if uuid is not None: self.sess_file(host, name).write_text(uuid)

    def get_current(self):
        host = None
        name = None
        uuid = None
        if self.host_file().exists():
            host = self.host_file().text().strip()
        if host:
            try:
                name = self.user_file(host).text().strip()
            except IOError:
                pass
        if name:
            try:
                uuid = self.sess_file(host, name).text().strip()
            except IOError:
                pass
        return (host, name, uuid)

    def last_host(self):
        """
        Prints either the last saved host (see get_current())
        or "localhost"
        """
        f = self.host_file()
        if not f.exists():
            return "localhost"
        text = f.text().strip()
        if not text:
            return "localhost"
        return text

    def contents(self):
        """
        Returns a map of maps with all the contents
        of the store. Internal accounting files are
        skipped.
        """
        rv = {}
        Dhosts = self.dir.dirs()
        for Dhost in Dhosts:
            host = str(Dhost.basename())
            if host not in rv:
                 rv[host] = {}
            Dnames = Dhost.dirs()
            for Dname in Dnames:
                name = str(Dname.basename())
                if name not in rv[host]:
                    rv[host][name] = {}
                Dids = self.non_dot(Dname)
                for Did in Dids:
                    id = str(Did.basename())
                    props = self.props(Did)
                    props["active"] = "unknown"
                    rv[host][name][id] = props
        return rv

    def count(self, host=None, name=None):
        """
        Returns the sum of all files visited by walk()
        """
        def f(h, n, s):
            f.i += 1
        f.i = 0
        self.walk(f, host, name)
        return f.i

    def walk(self, func, host=None, name=None, sess=None):
        """
        Applies func to all host, name, and session path-objects.
        """
        for h in self.dir.dirs():
            if host is None or str(h.basename()) == host:
                for n in h.dirs():
                    if name is None or str(n.basename()) == name:
                        for s in self.non_dot(n):
                            if sess is None or str(s.basename()) == sess:
                                func(h, n, s)


    #
    # Server-requiring methods
    #

    def attach(self, server, name, sess):
        """
        Simple helper. Delegates to create() using the session
        as both the username and the password. This reproduces
        the logic of client.joinSession()
        """
        props = self.get(server, name, sess)
        return self.create(sess, sess, props, new=False)

    def create(self, name, pasw, props, new=True):
        """
        Creates a new omero.client object, and returns:
        (cilent, session_id, timeToIdle, timeToLive)
        """
        import omero.clients
        props = dict(props)
        client = omero.client(props)
        client.setAgent("OMERO.sessions")
        sf = client.createSession(name, pasw)
        uuid = sf.ice_getIdentity().name
        sf.detachOnDestroy()
        sess = sf.getSessionService().getSession(uuid)
        timeToIdle = sess.getTimeToIdle().getValue()
        timeToLive = sess.getTimeToLive().getValue()
        if new:
            self.add(props["omero.host"], name, uuid, props)
        return client, uuid, timeToIdle, timeToLive

    def clear(self, host = None, name = None, sess = None):
        """
        Walks through all sessions and calls killSession.
        Regardless of exceptions, it will remove the session files
        from the store.
        """
        removed = []
        def f(h, n, s):
            hS = str(h.basename())
            nS = str(n.basename())
            sS = str(s.basename())
            try:
                client = self.attach(hS, nS, sS)
                client.killSession()
            except exceptions.Exception, e:
                self.logger.debug("Exception on killSession: %s" % e)
            s.remove()
            removed.append(s)
        self.walk(f, host, name, sess)
        return removed

    ##
    ## Helpers. Do not modify or rely on mutable state.
    ##

    def host_file(self):
        """ Returns the path-object which stores the last active host """
        return self.dir / "._LASTHOST_"

    def user_file(self, host):
        """ Returns the path-object which stores the last active user """
        d = self.dir / host
        if not d.exists():
            d.makedirs()
        return d / "._LASTUSER_"

    def sess_file(self, host, user):
        """ Returns the path-object which stores the last active session """
        d = self.dir / host / user
        if not d.exists():
            d.makedirs()
        return d / "._LASTSESS_"

    def non_dot(self, d):
        """ Only returns the files (not directories) contained in d that don't start with a dot """
        return [f for f in d.files("*") if not str(f.basename()).startswith(".")]

    def props(self, f):
        """
        Parses the path-object into properties
        """
        txt = f.text()
        lines = txt.split("\n")
        props = {}
        for line in lines:
            if not line:
                continue
            parts = line.split("=",1)
            if len(parts) == 1:
                parts.append("")
            props[parts[0]] = parts[1]
        return props

if __name__ == "__main__":
    SessionsStore().report()
