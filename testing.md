## Running the integration tests

#### on Linux

In general, using a Samba docker image will make the life easier

     mkdir /tmp/camel-smbj
     docker run --name smb -it -p 4139:139 -p 4445:445 -v /tmp/camel-smbj:/share -d jborza/samba-root -u "user;pass" -s "share;/share;yes;no;yes;user"

#### on Windows

    mkdir c:\temp\camel-smbj
    docker run --name smb -it -p 4139:139 -p 4445:445 -v c:/temp/camel-smbj:/share -d jborza/samba-root -u "user;pass" -s "share;/share;yes;no;yes;user"

Why port 4139 and 4445? On my Windows machine, the default ports 139 and 445 were already used.