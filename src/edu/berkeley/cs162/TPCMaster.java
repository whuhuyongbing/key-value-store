/**
 * Master for Two-Phase Commits
 * 
 * @author Mosharaf Chowdhury (http://www.mosharaf.com)
 * @author Prashanth Mohan (http://www.cs.berkeley.edu/~prmohan)
 *
 * Copyright (c) 2012, University of California at Berkeley
 * All rights reserved.
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *  * Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *  * Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *  * Neither the name of University of California, Berkeley nor the
 *    names of its contributors may be used to endorse or promote products
 *    derived from this software without specific prior written permission.
 *    
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 *  ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  DISCLAIMED. IN NO EVENT SHALL THE AUTHORS BE LIABLE FOR ANY
 *  DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 *  (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *  LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 *  ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 *  SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package edu.berkeley.cs162;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.util.TreeMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class TPCMaster {

	TreeMap<Long, SlaveInfo> slaves;
	
	/**
	 * Implements NetworkHandler to handle registration requests from 
	 * SlaveServers.
	 * 
	 */
	private class TPCRegistrationHandler implements NetworkHandler {

		private ThreadPool threadpool = null;

		public TPCRegistrationHandler() {
			// Call the other constructor
			this(1);	
		}

		public TPCRegistrationHandler(int connections) {
			threadpool = new ThreadPool(connections);	
		}

		@Override
		public void handle(Socket client) throws IOException {
			// implement me
		}
	}




	private class RegistrationHandler implements Runnable {

		private Socket client = null;


		public RegistrationHandler(Socket client) {
			this.client = client;
		}

		@Override
		public void run() {
			//TODO: implement me
			try {
				KVMessage ack;
				String response = "";
				System.out.println("isshutdown: " + client.isInputShutdown());
				System.out.println("connected: " + client.isConnected());
				InputStream input = client.getInputStream();
				KVMessage registerMsg = new KVMessage(input);
				if (!registerMsg.getMsgType().equals("register")) {
					ack = new KVMessage("resp", "Unknown Error: Incorrect message type");
					ack.sendMessage(client);
					return;
				}
				String msg = registerMsg.getMessage();
				SlaveInfo slave = new SlaveInfo(msg);
				Long slaveID = new Long(slave.getSlaveID());
				if (slaves.containsKey(slaveID)) {
					slaves.get(slaveID).update(slave.getHostName(), slave.getPort());
				} else {
					slaves.put(slaveID, slave);
				}
				response += "Successfully registered " + msg;
				ack = new KVMessage("resp", response);
				ack.sendMessage(client);
				//System.out.println(response);
			} catch (KVException e) {
					e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}

		}
	}
	
	/**
	 *  Data structure to maintain information about SlaveServers
	 *
	 */
	private class SlaveInfo {
		// 64-bit globally unique ID of the SlaveServer
		private long slaveID = -1;
		// Name of the host this SlaveServer is running on
		private String hostName = null;
		// Port which SlaveServer is listening to
		private int port = -1;
		
		// Variables to be used to maintain connection with this SlaveServer
		private KVClient kvClient = null;
		private Socket kvSocket = null;

		/**
		 * 
		 * @param slaveInfo as "SlaveServerID@HostName:Port"
		 * @throws KVException
		 */
		public SlaveInfo(String slaveInfo) throws KVException {
			// implement me
			int aPos = slaveInfo.indexOf('@');
			int cPos = slaveInfo.indexOf(':');
			if (aPos > cPos || aPos < 0 || cPos > 0) {
				throw new KVException(new KVMessage("resp", "Registration Error"));
			}

			String idStr = slaveInfo.substring(0, aPos);
			hostName = slaveInfo.substring(aPos+1, cPos);
			String portStr = slaveInfo.substring(cPos+1, slaveInfo.length());

			try {
				slaveID = Long.parseLong(idStr);
				port = Integer.parseInt(portStr);
			} catch (NumberFormatException e) {
				throw new KVException(new KVMessage("resp", "Registration Error: Received unparseable slave information"));
			}
			if (port > 65535 || port < 0) {
				System.err.println("Registration error, received bad port: " + port);
				throw new KVException(new KVMessage("resp", "Registration Error: Received bad port: " + port));
			}
		}
		
		public long getSlaveID() {
			return slaveID;
		}

		public KVClient getKvClient() {
			return kvClient;
		}

		public Socket getKvSocket() {
			return kvSocket;
		}

		public void setKvSocket(Socket kvSocket) {
			this.kvSocket = kvSocket;
		}
		public String getHostName() {
			return hostName;
		}

		public int getPort() {
			return port;
		}
		public synchronized void update(String newHostName, int newPort) {
			hostName = newHostName;
			port = newPort;
		}


	}
	
	// Timeout value used during 2PC operations
	private static final int TIMEOUT_MILLISECONDS = 5000;
	
	// Cache stored in the Master/Coordinator Server
	private KVCache masterCache = new KVCache(100, 10);
	
	// Registration server that uses TPCRegistrationHandler
	private SocketServer regServer = null;

	// Number of slave servers in the system
	private int numSlaves = -1;
	
	// ID of the next 2PC operation
	private Long tpcOpId = 0L;


	/**
	 * Creates TPCMaster
	 * 
	 * @param numSlaves number of expected slave servers to register
	 * @throws Exception
	 */
	public TPCMaster(int numSlaves) {
		// Using SlaveInfos from command line just to get the expected number of SlaveServers 
		this.numSlaves = numSlaves;

		// Create registration server
		regServer = new SocketServer("localhost", 9090);
	}
	
	/**
	 * Calculates tpcOpId to be used for an operation. In this implementation
	 * it is a long variable that increases by one for each 2PC operation. 
	 * 
	 * @return 
	 */
	private String getNextTpcOpId() {
		tpcOpId++;
		return tpcOpId.toString();		
	}
	
	/**
	 * Start registration server in a separate thread
	 */
	public void run() {
		AutoGrader.agTPCMasterStarted();
		// TODO: implement me
		regServer.addHandler(new TPCRegistrationHandler());
		Thread t =new Thread(() -> {
			try {
				regServer.connect();
				regServer.run();
			} catch (IOException e) {
				e.printStackTrace();
			}
		});
		t.start();
		AutoGrader.agTPCMasterFinished();
	}
	
	/**
	 * Converts Strings to 64-bit longs
	 * Borrowed from http://stackoverflow.com/questions/1660501/what-is-a-good-64bit-hash-function-in-java-for-textual-strings
	 * Adapted from String.hashCode()
	 * @param string String to hash to 64-bit
	 * @return
	 */
	private long hashTo64bit(String string) {
		// Take a large prime
		long h = 1125899906842597L; 
		int len = string.length();

		for (int i = 0; i < len; i++) {
			h = 31*h + string.charAt(i);
		}
		return h;
	}
	
	/**
	 * Compares two longs as if they were unsigned (Java doesn't have unsigned data types except for char)
	 * Borrowed from http://www.javamex.com/java_equivalents/unsigned_arithmetic.shtml
	 * @param n1 First long
	 * @param n2 Second long
	 * @return is unsigned n1 less than unsigned n2
	 */
	private boolean isLessThanUnsigned(long n1, long n2) {
		return (n1 < n2) ^ ((n1 < 0) != (n2 < 0));
	}
	
	private boolean isLessThanEqualUnsigned(long n1, long n2) {
		return isLessThanUnsigned(n1, n2) || n1 == n2;
	}	

	/**
	 * Find first/primary replica location
	 * @param key
	 * @return
	 */
	private SlaveInfo findFirstReplica(String key) {
		// 64-bit hash of the key
		long hashedKey = hashTo64bit(key.toString());
		// TODO: implement me
		long keyInMap = new Long(hashedKey);
		if (slaves.containsKey(keyInMap))
			return slaves.get(keyInMap);
		else {
			Long replica = slaves.higherKey(keyInMap);
			if (replica == null) {
				return slaves.firstEntry().getValue();
			} else
				return slaves.get(replica);
		}
	}
	
	/**
	 * Find the successor of firstReplica to put the second replica
	 * @param firstReplica
	 * @return
	 */
	private SlaveInfo findSuccessor(SlaveInfo firstReplica) {
		// implement me
		Long firstReplicaID = new Long(firstReplica.getSlaveID());
		Long successor = slaves.higherKey(firstReplicaID);
		if (successor ==  null)
			return slaves.firstEntry().getValue();
		else
			return slaves.get(successor);

	}
	
	/**
	 * Synchronized method to perform 2PC operations one after another
	 * 
	 * @param msg
	 * @param isPutReq
	 * @return True if the TPC operation has succeeded
	 * @throws KVException
	 */
	public synchronized boolean performTPCOperation(KVMessage msg, boolean isPutReq) throws KVException {
		AutoGrader.agPerformTPCOperationStarted(isPutReq);
		// TODO: implement me


		AutoGrader.agPerformTPCOperationFinished(isPutReq);
		return false;
	}

	/**
	 * Perform GET operation in the following manner:
	 * - Try to GET from first/primary replica
	 * - If primary succeeded, return Value
	 * - If primary failed, try to GET from the other replica
	 * - If secondary succeeded, return Value
	 * - If secondary failed, return KVExceptions from both replicas
	 * 
	 * @param msg Message containing Key to get
	 * @return Value corresponding to the Key
	 * @throws KVException
	 */
	public String handleGet(KVMessage msg) throws KVException {
		AutoGrader.aghandleGetStarted();
		// TODO: implement me
		String key = msg.getKey();
		ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
		readWriteLock.readLock().lock();
		String cacheVal = masterCache.get(key);
		// if the entry exist in the cache
		if (cacheVal != null) {
			readWriteLock.readLock().unlock();
			return cacheVal;
		}
		AutoGrader.aghandleGetFinished();
		return null;
	}
}
