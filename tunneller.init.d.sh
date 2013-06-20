#! /bin/bash

# This is the script to be called by init.d. Install it as per
# usual init.d script install procedures.

# Setup/Installation:
# Put JSVC in /usr/local/bin (or update its location below)
# Put tunneller.jar and all of the required libs in /usr/share/java
# Put required config files and stuff in /etc/tunneller


# This is a bit of a funny init.d script which calls
# itself with nohup.  The reason it is done in this
# roundabout way is that restarting tunneller could restart
# the ssh tunnel that the admin is logged in through.
# The restart would kill the tunnel, which would hangup the
# admin's session, and thus hup any processes he's started -
# including this script.  The result is that, when restarting,
# the script would die/stop executing before it got a chance
# to restart the tunnel.  The nohup fixes this by ensuring 
# this script will keep running even if our tty hangs up.


PROCESS_NAME=tunneller
START_CLASS=ca.brood.tunneller.Tunneller
HOME_DIR=/etc/tunneller
PID_FILE=/var/run/tunneller.pid

CPHOME=/usr/share/java
JSVC=/usr/local/bin/jsvc
JVM=/usr/lib/jvm/default-java

CP=$CPHOME/activation.jar:\
$CPHOME/brootils.jar:\
$CPHOME/commons-daemon-1.0.13.jar:\
$CPHOME/jsch-0.1.49.jar:\
$CPHOME/log4j-1.2.16.jar:\
$CPHOME/mail.jar:\
$CPHOME/tunneller.jar

# Verify the arguments before we nohup so we can send
# error messages to the tty instead of to nohup.out
if [ "$1" != "nohup" ] && [ "$1" != "start" ] && [ "$1" != "stop" ] && [ "$1" != "restart" ]; then
	echo "Usage: $0 {start|stop|restart}"
	exit 1
fi

# If the first argument is not nohup, then nohup this
# script.
if [ "$1" != "nohup" ]; then
	nohup ./$0 nohup $1 &
	exit 0
fi

# We only get here if this script has been called with nohup
case "$2" in
        start)
                echo -n "Starting daemon "
				$JSVC -cp $CP -home $JVM -cwd $HOME_DIR -pidfile $PID_FILE -procname $PROCESS_NAME $START_CLASS
                ;;
        stop)
                echo -n "Shutting down daemon "
				$JSVC -stop -cp $CP -home $JVM -cwd $HOME_DIR -pidfile $PID_FILE -procname $PROCESS_NAME $START_CLASS
                ;;
        restart)
                ## Stop the service and regardless of whether it was
                ## running or not, then start it again.
                $0 nohup stop
                $0 nohup start
                ;;
        *)
                ## If no parameters are given, print which are avaiable.
                echo "Usage: $0 {start|stop|restart}"
                exit 1
                ;;
esac
