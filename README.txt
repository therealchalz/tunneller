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
brootils-1.0.jar
jsvc binary from the commons-daemon project (for linux)
procrun/prunsrv binary from the commons-daemon project (for windows)

See the brootils project/library for info on what that library needs.

CONFIGURATION

The installation comes with a sample tunneller.xml file.  It is very simple.
Check out the tunneller.dtd file to see how the xml file must be laid out.  The
logs will be output to the install's home directory.

INSTALLATION

Linux

Put all the *.jar files in /usr/share/java.
Install the jsvc binary to /usr/bin/jsvc.
Put the config files (tunneller.dtd, tunneller.xml, log4j.xml) in
/etc/tunneller.  Logs will be saved here as well.
Put the tunneller.init.d.sh script in /etc/init.d/ (rename it for your 
convenience)
Ensure that the tunneller init.d script is executable.
Install the init.d script with update-rc.d

Windows
Put all the *.jar files in C:\tunneller.
Install prunsrv to somewhere PATH-accessible.
Put the config files (tunneller.dtd, tunneller.xml, log4j.xml) in
C:\tunneller.  Logs will be saved here as well.
Install the service with the following command (without quotes):
"prunsrv //IS//Tunneller --Description="SSH Tunnel Keepalive Service" --DisplayName="Tunneller" --Startup=auto --Classpath=c:\tunneller\* --StartMode=jvm --StartPath=c:\tunneller --StartClass=ca.brood.tunneller.Tunneller --StartMethod=windowsService --StartParams=start --StopMode=jvm --StopClass=ca.brood.tunneller.Tunneller --StopMethod=windowsService --StopParams=stop --LogPath=C:\tunneller --LogPrefix=TunnellerServiceLog --StdOutput=auto --StdError=auto"