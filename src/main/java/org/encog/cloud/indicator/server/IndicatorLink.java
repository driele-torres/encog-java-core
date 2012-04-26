/*
 * Encog(tm) Core v3.1 - Java Version
 * http://www.heatonresearch.com/encog/
 * http://code.google.com/p/encog-java/
 
 * Copyright 2008-2012 Heaton Research, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *   
 * For more information on Heaton Research copyrights, licenses 
 * and trademarks visit:
 * http://www.heatonresearch.com/copyright
 */
package org.encog.cloud.indicator.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.List;

import org.encog.cloud.indicator.IndicatorError;
import org.encog.util.csv.CSVFormat;
import org.encog.util.csv.ParseCSVLine;
import org.encog.util.logging.EncogLogging;

/**
 * Managed a link to a remote indicator.
 */
public class IndicatorLink {	
	/**
	 * Default socket timeout.
	 */
	public final int SOCKET_TIMEOUT = 25000;
	
	/**
	 * The socket to use. (client)
	 */
	private Socket socket;
	
	/**
	 * Used to read from the remote.
	 */
	private BufferedReader inputFromRemote;
	
	/**
	 * Used to output to the socket.
	 */
	private OutputStream socketOut;
	
	/**
	 * Used to parse a CSV line(packet) read.
	 */
	private ParseCSVLine parseLine = new ParseCSVLine(CSVFormat.EG_FORMAT);
	
	/**
	 * The number of packets received.
	 */
	private int packets;
	
	/**
	 * The parent server.
	 */
	private final IndicatorServer parentServer;
	
	/**
	 * Construct an indicator link.
	 * @param node The parent server.
	 * @param s The socket. (client)
	 */
	public IndicatorLink(IndicatorServer node, Socket s) {
		try {
			this.parentServer = node;
			this.socket = s;
			this.socket.setSoTimeout(SOCKET_TIMEOUT);
			
			this.socketOut = this.socket.getOutputStream();
			this.inputFromRemote = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
		} catch (IOException ex) {
			throw new IndicatorError(ex);
		}

	}


	/**
	 * Write a packet, basically a CSV line.
	 * @param command The packet command (type).
	 * @param args The arguments for this packet.
	 */
	public void writePacket(String command, Object[] args) {
		try {
			StringBuilder line = new StringBuilder();
			line.append("\"");
			line.append(command);
			line.append("\"");
			for (int i = 0; i < args.length; i++) {
				line.append(",\"");
				line.append(args[i].toString());
				line.append("\"");
			}
			line.append("\n");
			
			byte[] b = line.toString().getBytes("US-ASCII");
			this.socketOut.write(b);
			this.socketOut.flush();
		} catch (IOException ex) {
			throw new IndicatorError(ex);
		}
	}

	/**
	 * Read a packet.
	 * @return The packet we read.
	 */
	public IndicatorPacket readPacket() {
		
		try {
			String str = this.inputFromRemote.readLine();
			List<String> list = parseLine.parse(str);
			this.packets++;
			
			EncogLogging.log(EncogLogging.LEVEL_DEBUG, "Received Packet: " + str);
			return new IndicatorPacket(list);	
		} 
		catch( SocketTimeoutException ex)
		{
			return null;
		}
		catch(IOException ex) {
			throw new IndicatorError(ex);
		}
		
	}

	/**
	 * @return The client socket.
	 */
	public Socket getSocket() {
		return this.socket;
	}
	
	/**
	 * The packet count that we've read.
	 * @return The number of pakcets read.
	 */
	public int getPackets() {
		return this.packets;
	}

	/**
	 * Close the socket.
	 */
	public void close() {
		try {
			this.socket.close();
		} catch (IOException e) {
			// ignore, we were trying to close
		}	
	}
		
	/**
	 * @return The server that created this link.
	 */
	public IndicatorServer getParentServer() {
		return parentServer;
	}

	/**
	 * Request the specified signals (i.e. HLOC(high, low, etc)). 
	 * @param dataSource The data requested.
	 */
	public void requestSignal(List<String> dataSource) {
		writePacket("signals",dataSource.toArray());
		
	}
}
