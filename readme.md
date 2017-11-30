Camel SMBJ Component
=========================

[![Build Status](https://travis-ci.org/jborza/camel-smbj.svg?branch=master)](https://travis-ci.org/jborza/camel-smbj)

This component is intended to be a drop-in replacement for [camel-jcifs component](http://camel.apache.org/jcifs.html).
It provides access to remote file systems over the SMB networking protocol with support of SMB2.

URI format

>   smb2://[[[domain;]username[:password]@]server[:port]/[[share/[dir/]]]][?options]

Where share represents the share to connect to and dir is optionally any underlying directory. Can contain nested folders.
You can append query options to the URI in the following format, ?option=value&option=value&...
This component uses the [SMBJ library](https://github.com/hierynomus/smbj) for the actual SMB work.


By itself, the camel-smbj component is an extension of the [File component](http://camel.apache.org/file2.html).

