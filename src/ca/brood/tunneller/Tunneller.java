/*******************************************************************************
 * Copyright (c) 2013 Charles Hache <chalz@member.fsf.org>. All rights reserved. 
 * 
 * This file is part of the tunneller project.
 * tunneller is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * tunneller is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the 
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with tunneller.  If not, see <http://www.gnu.org/licenses/>.
 * 
 * Contributors:
 *     Charles Hache <chalz@member.fsf.org> - initial API and implementation
 ******************************************************************************/
package ca.brood.tunneller;

import java.util.ArrayList;

import org.apache.commons.daemon.Daemon;
import org.apache.commons.daemon.DaemonContext;
import org.apache.commons.daemon.DaemonInitException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import ca.brood.brootils.xml.XMLConfigurable;
import ca.brood.brootils.xml.XMLFileLoader;

public class Tunneller  implements Daemon, XMLConfigurable {
	private static Tunneller tunnellerDaemon;
	
	private ArrayList<TunnelKeepaliveThread> tunnelThreads;
	private String configFile;
	private Logger log;
	
	static {
		tunnellerDaemon = new Tunneller();
	}
	
	public Tunneller() {
		tunnelThreads = new ArrayList<TunnelKeepaliveThread>();
		configFile = "tunneller.xml";
		log = LogManager.getLogger(Tunneller.class);
	}
	
	public static void main(String[] args) {
		//For testing
		Tunneller.windowsService(new String[0]);

		try {
			Thread.sleep(10000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		//To stop, uncomment this:
		String[] stop = new String[1];
		stop[0] = "stop";
		Tunneller.windowsService(stop);
	}
	
	private void tunnellerStart() {
		log.info("Tunneller is starting...");
		
		XMLFileLoader xmlLoader = new XMLFileLoader(configFile, this);
		
		boolean success = false;
		try {
			success = xmlLoader.load();
		} catch (Exception e) {
			log.fatal("Exception while loading config file: ", e);
		}
		
		if (success) {
			for (TunnelKeepaliveThread thread: tunnelThreads) {
				thread.start();
			}
		}
	}
	
	private void tunnellerStop() {
		log.info("Tunneller is stopping...");
		for (TunnelKeepaliveThread thread: tunnelThreads) {
			thread.stop();
		}
		log.info("Tunneller is done.");
	}
	
	@Override
	public boolean configure(Node rootNode) {
		NodeList nodes = rootNode.getOwnerDocument().getElementsByTagName("tunnel");

		if (nodes.getLength() == 0) {
			log.fatal("No tunneller configured");
			return false;
		}
		
		for (int i=0; i<nodes.getLength(); i++) {
			TunnelKeepaliveThread newThread = new TunnelKeepaliveThread();
			if (!newThread.configure(nodes.item(i))) {
				log.fatal("Error configuring tunneller");
				return false;
			}
			tunnelThreads.add(newThread);
		}

		return true;
	}

	@Override
	public void destroy() {
		log.info("Linux daemon received destroy command");
	}

	@Override
	public void init(DaemonContext arg) throws DaemonInitException, Exception {
		/* I think if jsvc is configured correctly, then this method is 
         * called as the root user.  After it returns, then start is called
         * as the regular user.
         */
    	//TODO: get an xml config file from the command line
    	log.info("Linux daemon received init");
    	for (String s : arg.getArguments()) {
    		log.debug("Got argument: "+s);
    	}
	}

	@Override
	public void start() throws Exception {
		log.info("Linux daemon received start command");
		tunnellerDaemon.tunnellerStart();
	}

	@Override
	public void stop() throws Exception {
		log.info("Linux daemon received stop command");
		tunnellerDaemon.tunnellerStop();
	}
	/**
     * Static method called by prunsrv to start/stop
     * the Windows service.  Pass the argument "start"
     * to start the service, and pass "stop" to
     * stop the service.
     * Stolen from FAQ at commons daemon.
     */
    public static void windowsService(String[] args) {
    	String cmd = "start";
        if (args.length > 0) {
            cmd = args[0];
        }
        

        if ("start".equals(cmd)) {
        	tunnellerDaemon.log.info("Windows service received Start command");
        	tunnellerDaemon.tunnellerStart();
        } else if ("stop".equals(cmd)) {
        	tunnellerDaemon.log.info("Windows service received Stop command");
        	tunnellerDaemon.tunnellerStop();
        } else {
        	tunnellerDaemon.log.error("Unrecognized service option: "+cmd);
        }
    }
}
