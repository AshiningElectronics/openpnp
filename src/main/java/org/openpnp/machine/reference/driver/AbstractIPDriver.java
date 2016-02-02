package org.openpnp.machine.reference.driver;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.concurrent.TimeoutException;

import javax.swing.Icon;

import org.openpnp.machine.reference.ReferenceDriver;
import org.openpnp.machine.reference.ReferencePasteDispenser;
import org.openpnp.model.Location;
import org.simpleframework.xml.Attribute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * A base class for basic SerialPort based Drivers. Includes functions
 * for connecting, disconnecting, reading and sending lines. 
 */
public abstract class AbstractIPDriver implements ReferenceDriver, Closeable {
    private static final Logger logger = LoggerFactory.getLogger(AbstractIPDriver.class);
    
    @Attribute(required=false)
    protected String host = "localhost";
    @Attribute(required=false)
    protected int port = 4242;
    
    protected Socket clientSocket;
    protected DataOutputStream outToServer;
    //protected SerialPort serialPort;
    //protected SerialInputStream input;
    //protected OutputStream output;
    
    protected synchronized void connect() throws Exception {
    	System.out.println("connect");
    	//String sentence;
    	//String modifiedSentence;
    	//BufferedReader inFromUser = new BufferedReader( new InputStreamReader(System.in));
    	clientSocket = new Socket(host, port);
    	outToServer = new DataOutputStream(clientSocket.getOutputStream());
    	//BufferedReader inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
    	//sentence = inFromUser.readLine();
    	outToServer.writeBytes("hello" + '\n');
    	//modifiedSentence = inFromServer.readLine();
    	//System.out.println("FROM SERVER: " + modifiedSentence);
    	//clientSocket.close();

    }
    
    protected synchronized void disconnect() throws Exception {
    	System.out.println("disconnect");
    	clientSocket.close();
    	/*
        if (serialPort != null && serialPort.isOpened()) {
            serialPort.closePort();
            input = null;
            output = null;
            serialPort = null;
        }
        */
    }
    
    @Override
    public void dispense(ReferencePasteDispenser dispenser,
            Location startLocation, Location endLocation,
            long dispenseTimeMilliseconds) throws Exception {
        // Do nothing. This is just stubbed in so that it can be released
        // without breaking every driver in the wild.
    }

    /**
     * Read a line from the serial port. Blocks for the default timeout. If
     * the read times out a TimeoutException is thrown. Any other failure
     * to read results in an IOExeption;
     * 
     * @return
     * @throws TimeoutException
     * @throws IOException
     */
    protected String readLine() throws TimeoutException, IOException {
		return null;
    	/*
        StringBuffer line = new StringBuffer();
        while (true) {
            try {
                int ch = input.read();
                if (ch == -1) {
                    return null;
                }
                else if (ch == '\n' || ch == '\r') {
                    if (line.length() > 0) {
                        return line.toString();
                    }
                }
                else {
                    line.append((char) ch);
                }
            }
            catch (IOException ex) {
                if (ex.getCause() instanceof SerialPortTimeoutException) {
                    throw new TimeoutException(ex.getMessage());
                }
                throw ex;
            }
        }
        */
    }

    @Override
    public void close() throws IOException {
    	/*
        try {
            disconnect();
        }
        catch (Exception e) {
            throw new IOException(e);
        }
        */
    }
    

    @Override
    public Icon getPropertySheetHolderIcon() {
        // TODO Auto-generated method stub
        return null;
    }

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}
 
}

