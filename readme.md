Camel SMBJ Component
=========================

[![Build Status](https://travis-ci.org/jborza/camel-smbj.svg?branch=master)](https://travis-ci.org/jborza/camel-smbj) 
[![codecov](https://codecov.io/gh/jborza/camel-smbj/branch/master/graph/badge.svg)](https://codecov.io/gh/jborza/camel-smbj)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/ea681a23c98d4b1db9d8322393eabb17)](https://www.codacy.com/app/jborza/camel-smbj?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=jborza/camel-smbj&amp;utm_campaign=Badge_Grade)

This component is intended to be a drop-in replacement for [camel-jcifs component](http://camel.apache.org/jcifs.html).
It provides access to remote file systems over the SMB networking protocol with support of SMB2.

URI format

>   smb2://[[[domain;]username[:password]@]server[:port]/[[share/[dir/]]]][?options]

Where share represents the share to connect to and dir is optionally any underlying directory. Can contain nested folders.
You can append query options to the URI in the following format, ?option=value&option=value&...
This component uses the [SMBJ library](https://github.com/hierynomus/smbj) for the actual SMB work.


By itself, the camel-smbj component is an extension of the [File component](http://camel.apache.org/file2.html).

