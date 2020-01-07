/**
 * Client component for generating load for the KeyValue store. 
 * This is also used by the Master server to reach the slave nodes.
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
import java.net.Socket;
import java.net.SocketException;


/**
 * This class is used to communicate with (appropriately marshalling and unmarshalling) 
 * objects implementing the {@link KeyValueInterface}.
 *
 */
public class KVClient implements KeyValueInterface {

	private String server = null;
	private int port = 0;
	
	/**
	 * @param server is the DNS reference to the Key-Value server
	 * @param port is the port on which the Key-Value server is listening
	 */
	public KVClient(String server, int port) {
		this.server = server;
		this.port = port;
	}
	
	public Socket connectHost() throws KVException {
	    // TODO: Implement Me!
		Socket socket = null;
		try {
			socket = new Socket(this.server, this.port);
		} catch (IOException e) {
			throw new KVException(new KVMessage("resp","can not create socket"));
		}
		return socket;
	}
	
	public void closeHost(Socket sock) throws KVException {
	    // TODO: Implement Me!

		try {
			sock.close();
		} catch (IOException e) {
			throw new KVException(new KVMessage("resp",e.getMessage()));
		}
	}
	
	public void put(String key, String value) throws KVException {
	    // TODO: Implement Me from Project 3
		this.put(key, value, null, Integer.MAX_VALUE);

	}
	
	public void put(String key, String value, String tpcOpId, int timeout) throws KVException {
		// TODO: Implement Me for Project 4
		KVMessage resp = null;

		KVMessage kvMessage = new KVMessage("putreq");
		kvMessage.setValue(value);
		kvMessage.setKey(key);
		kvMessage.setTpcOpId(tpcOpId);

		Socket socket = connectHost();

		try {
			socket.setSoTimeout(timeout);
		} catch (SocketException e) {
			throw new KVException(new KVMessage("resp", "Network error:" + e.getMessage()));
		}

		kvMessage.sendMessage(socket);


		try {
			resp = new KVMessage(socket.getInputStream());
			socket.close();
		} catch (IOException e) {
			throw new KVException(new KVMessage("resp", e.getMessage()));
		}


		//handle resp
//		if (!resp.getMsgType().equals("resp"))
//			throw new KVException(new KVMessage("resp", "the message type is not resp"));
//		if (!resp.getMessage().equals("Success"))
//			throw new KVException(new KVMessage(resp));


	}

	public String get(String key) throws KVException {
		// TODO: Implement Me from Project 3
		return this.get(key,Integer.MAX_VALUE);
	}
	
	public String get(String key, int timeout) throws KVException {
		// TODO: Implement Me for Project 4
		KVMessage resp = null;
		Socket socket = connectHost();

		KVMessage kvMessage = new KVMessage("getreq");
		kvMessage.setKey(key);

		try {
			socket.setSoTimeout(timeout);
			kvMessage.sendMessage(socket);
			resp = new KVMessage(socket.getInputStream());
			socket.close();
		} catch (IOException e) {
			throw new KVException(new KVMessage("resp", "Network error: " + e.getMessage()));
		}

		//handle resp
		if (!resp.getMsgType().equals("resp"))
			throw new KVException(new KVMessage("resp", "the message type is not resp"));

		if (resp.getValue() == null)
			throw new KVException(resp);


		return resp.getValue();

	}
	
	public void del(String key) throws KVException {
		// TODO: Implement Me from Project 3
		this.del(key, null, Integer.MAX_VALUE);

	}
	
	public void del(String key, String tpcOpId, int timeout) throws KVException {
		// TODO: Implement Me for Project 4
		KVMessage resp = null;
		Socket socket = connectHost();

		KVMessage kvMessage = new KVMessage("delreq");
		kvMessage.setKey(key);
		kvMessage.setTpcOpId(tpcOpId);
		try {
			socket.setSoTimeout(timeout);
			kvMessage.sendMessage(socket);
			resp = new KVMessage(socket.getInputStream());
			socket.close();
		} catch (IOException e) {
			throw new KVException(new KVMessage("resp", e.getMessage()));
		}


		if (!resp.getMsgType().equals("resp"))
			throw new KVException(new KVMessage("resp", "the message type is not resp"));

		if (!resp.getMessage().equals("Success"))
			throw new KVException(new KVMessage(resp));

	}
	
	public void ignoreNext() throws KVException {
	    // TODO: Implement Me!
	}
}
