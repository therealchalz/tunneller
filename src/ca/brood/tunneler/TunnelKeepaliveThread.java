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
package ca.brood.tunneler;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import ca.brood.brootils.ssh.PortForward;
import ca.brood.brootils.ssh.SSHSession;
import ca.brood.brootils.xml.XMLConfigurable;

/** A thread that ensures tunnels are kept alive.
 * @author Charles Hache
 *
 */
public class TunnelKeepaliveThread implements Runnable, XMLConfigurable {
	private int pollingInterval;
	private Logger log;
	private Collection<PortForward> portForwards;
	private SSHSession ssh;
	private boolean keepRunning;
	private Object runningLock;
	private Thread myThread;
	private boolean loggedError;
	
	//Connection parameters
	private String user = "";
	private String password = "";
	private String host = "";
	private String keyfile = "";
	private String passphrase = null;
	private int port = 22;
	
	/** Creates a new {@link TunnelKeepaliveThread}.
	 * 
	 */
	public TunnelKeepaliveThread() {
		log = LogManager.getLogger(TunnelKeepaliveThread.class);
		keepRunning = false;
		ssh = null;
		runningLock = new Object();
		pollingInterval = 10000;	//10 seconds
		portForwards = new ArrayList<PortForward>();
		myThread = new Thread(this);
		loggedError = false;
	}
	
	/** Adds a local port to forward.
	 * @param localPort The local port to forward from.
	 * @param host The remote host to forward to.
	 * @param remotePort The remote port to forward to.
	 * @return true if the forward was successful, false otherwise.
	 */
	public boolean addLocalForward(int localPort, String host, int remotePort) {
		PortForward toAdd = new PortForward(localPort, host, remotePort, false); 
		portForwards.add(toAdd);
		try {
			ssh.forwardPort(toAdd);
		} catch (Exception e) {
			log.error("Failed to forward port: "+toAdd);
			return false;
		}
		return true;
	}
	
	/** Adds a remote port to forward.
	 * @param remotePort The remote port to forward from.
	 * @param host The local host to forward to.
	 * @param localPort The local port to forward to.
	 * @return true if the forward was successful, false otherwise.
	 */
	public boolean addRemoteForward(int remotePort, String host, int localPort) {
		PortForward toAdd = new PortForward(localPort, host, remotePort, true); 
		try {
			ssh.forwardPort(toAdd);
		} catch (Exception e) {
			log.error("Failed to forward port: "+toAdd);
			return false;
		}
		return true;
	}
	
	/** Configures this {@link TunnelKeepaliveThread} via an XML node.
	 * Review the DTD file for tunneller to see what this is expecting.
	 * @param rootNode The root node for this tunneller.
	 * @return true if the XML was valid and this {@link TunnelKeepaliveThread} was configured successfully.
	 */
	@Override
	public boolean configure(Node rootNode) {

		NodeList elements = rootNode.getChildNodes();
		for (int i=0; i<elements.getLength(); i++) {
			Node element = elements.item(i);
			
			if (("#text".equalsIgnoreCase(element.getNodeName()))||
					("#comment".equalsIgnoreCase(element.getNodeName())))	{
				continue;
			} else if ("user".equalsIgnoreCase(element.getNodeName())) {
				user = element.getFirstChild().getNodeValue();
			} else if ("host".equalsIgnoreCase(element.getNodeName())) {
				host = element.getFirstChild().getNodeValue(); 
			} else if ("password".equalsIgnoreCase(element.getNodeName())) {
				password = element.getFirstChild().getNodeValue(); 
			} else if ("keyfile".equalsIgnoreCase(element.getNodeName())) {
				keyfile = element.getFirstChild().getNodeValue(); 
			} else if ("passphrase".equalsIgnoreCase(element.getNodeName())) {
				passphrase = element.getFirstChild().getNodeValue(); 
			} else if ("port".equalsIgnoreCase(element.getNodeName())) {
				try {
					port = Integer.parseInt(element.getFirstChild().getNodeValue());
				} catch (Exception e) {
					log.warn("Invalid port number specified: "+element.getFirstChild().getNodeValue()+". Using default of 22.");
				}
			} else if ("forward".equalsIgnoreCase(element.getNodeName())) {
				PortForward f = new PortForward();
				if (f.configure(element)) {
					portForwards.add(f);
				}
			} else {
				log.warn("Got unexpected node in config: "+element.getNodeName());
			}			
		}

		if (password.equals("") && keyfile.equals("")) {
			log.fatal("A password or a keyfile must be configured.");
			return false;
		}
		if (user.equals("")) {
			log.fatal("No user configured.");
			return false;
		}
		if (host.equals("")) {
			log.fatal("No host configured.");
			return false;
		}
		if (portForwards.size() == 0) {
			log.fatal("No port forwards configured.");
			return false;
		}
		
		return true;
	}
	
	/** Configures the remote host.
	 * @param host The remote host.
	 * @param port The remote port.
	 * @param user The user for authentication.
	 */
	public void configure(String host, int port, String user) {
		ssh.configure(host, port, user);
	}
	
	/** Check if this {@link TunnelKeepaliveThread} is running.
	 * @return true if this thread is running.
	 */
	public boolean getRunning() {
		synchronized(runningLock) {
			return keepRunning;
		}
	}
	
	/** This isn't the method you're looking for.
	 * You'll want to use the {@link #start()} method unless you actually do want to run
	 * this in your thread.  If you use the {@link #start()} then this will start in it's own
	 * thread.
	 */
	@Override
	public void run() {
		log.info("Thread started");
		
		respawn();
		
		while (getRunning()) {
			if (!ssh.areTunnelsActive()) {
				if (!respawn()) {
					log.warn("***Tunnels failed to restart***");
				}
			}
			
			try {
				Thread.sleep(pollingInterval);
			} catch (InterruptedException e) { }
		}
		
		ssh.close();
		
		log.info("Thread exiting");
	}
	
	
	/** Assigns a key file and, optionally, a passphrase to this {@link TunnelKeepaliveThread}.
	 * One must either {@link #setPasswordAuth(String)} or {@link #setKeyfileAuth(String, String)} to connect to a remote host.
	 * @param keyfile The path to the key file.
	 * @param passphrase The passphrase of the keyfile, or null if none is required.
	 */
	public void setKeyfileAuth(String keyfile, String passphrase) {
		this.keyfile = keyfile;
		this.passphrase = passphrase;
	}
	
	/** Assigns a password to this {@link TunnelKeepaliveThread}.
	 * One must either {@link #setPasswordAuth(String)} or {@link #setKeyfileAuth(String, String)} to connect to a remote host.
	 * If this is called multiple times, only the last password is used.
	 * @param pass The password to 
	 */
	public void setPasswordAuth(String pass) {
		this.password = pass;
	}
	
	/** This probably isn't the function you're looking for.
	 * Typically a {@link TunnelKeepaliveThread} will manage it's own thread so you'll 
	 * control it via the {@link #start()} and {@link #stop()} functions.
	 * <p>
	 * If for some reason you need to use your own thread and just use {@link TunnelKeepaliveThread}
	 * as a Runnable, then this function can be used to control the loop sentinel in the Runnable's
	 * main loop.
	 * @param running
	 */
	public void setRunning(boolean running) {
		synchronized(runningLock) {
			keepRunning = running;
		}
	}
	
	/** Start this {@link TunnelKeepaliveThread}.
	 */
	public void start() {
		setRunning(true);
		myThread.start();
	}
	
	/** Stops this {@link TunnelKeepaliveThread}.
	 * 
	 */
	public void stop() {
		setRunning(false);
		myThread.interrupt();
		myThread = new Thread(this);
	}

	private boolean respawn() {
		boolean ret = false;
		if (ssh != null)
			ssh.close();
		
		try {
			ssh = new SSHSession();
			
			ssh.configure(host, port, user);
			if (password != null)
				ssh.setPasswordAuth(password);
			if (keyfile != null)
				ssh.setKeyfileAuth(keyfile, passphrase);
			
			ret = true;
			for (PortForward p : this.portForwards) {
				ssh.forwardPort(p);
			}
			
		} catch (Exception e) {
			if (!loggedError) {			
				log.error("Error connecting tunnel" , e);
				loggedError = true;
			}
			ssh.close();
			ret = false;
		}
		
		if (ret) {
			log.info("Tunnels connected successfully!");
			loggedError = false;
		}
		
		return ret;
	}
}
