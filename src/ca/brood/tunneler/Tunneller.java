package ca.brood.tunneler;

import org.apache.commons.daemon.Daemon;
import org.apache.commons.daemon.DaemonContext;
import org.apache.commons.daemon.DaemonInitException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Node;

import ca.brood.brootils.xml.XMLConfigurable;
import ca.brood.brootils.xml.XMLFileLoader;

public class Tunneller  implements Daemon, XMLConfigurable {
	private static Tunneller tunnellerDaemon;
	
	private TunnelKeepaliveThread tunnelThread;
	private String configFile;
	private Logger log;
	
	static {
		tunnellerDaemon = new Tunneller();
	}
	
	public Tunneller() {
		tunnelThread = new TunnelKeepaliveThread();
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
			tunnelThread.start();
		}
	}
	
	private void tunnellerStop() {
		log.info("Tunneller is stopping...");
		tunnelThread.stop();
		log.info("Tunneller is done.");
	}
	
	@Override
	public boolean configure(Node rootNode) {
		// TODO Auto-generated method stub
		return false;
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
		tunnellerDaemon.start();
	}

	@Override
	public void stop() throws Exception {
		log.info("Linux daemon received stop command");
		tunnellerDaemon.stop();
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
