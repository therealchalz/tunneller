TUNNELLER

This is a program written in Java that keeps tunnels (local and remote SSH port
forwards) alive.  If they fail for some reason, tunneller will keep attempting
to restart them.

The program uses the Apache Commons Daemon API, meaning that it can be installed
as a daemon on Linux systems or as a service on Windows systems.

The SSH functionality is provided by the JSCH Java SSH library, via the brootils
wrapper library.

DEPENDENCIES

This program depends directly on the following libraries:
commons-daemon-1.0.13.jar
log4j-api-2.0-beta7.jar
log4j-core-2.0-beta7.jar
brootils.jar
jsvc binary from the commons-daemon project (for linux)
procrun binary from the commons-daemon project (for windows)

See the brootils project/library for info on what that library needs.

CONFIGURATION

The installation comes with a sample tunneller.xml file.  It is very simple.
Check out the tunneller.dtd file to see how the xml file must be laid out.  The
logs will be output to the install's home directory.

INSTALLATION

Linux

Put all the *.jar files in /usr/share/java.
Install the jsvc binary to /usr/local/bin/jsvc.
Put put the config files (tunneller.dtd, tunneller.xml, log4j.xml) in
/etc/tunneller.  Logs will be saved here as well.
Put the tunneller.init.d.sh script in /etc/init.d/ (rename it for your 
convenience)
Ensure that the tunneller init.d script is executable.
Install the init.d script with update-rc.d

Windows

TODO
