/*
 * Created on 5-nov-2004
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package sip;

import org.jsresources.apps.am.audio.*;
import java.io.*;
import java.net.*;
import javax.sound.sampled.*;

/*
 *  * This file is part of ChatAnt

ChatAnt is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

ChatAnt is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with ChatAnt.  If not, see <http://www.gnu.org/licenses/>.


Copyright 2005 Alessandro Formiconi - formiconi@gmail.com
 */
public class Vsjtalk extends Thread {

	InputStream myIStream;
	OutputStream sendStream = null;
/*
	The vsjtalk class contains the application's main (playback) thread. It is responsible for core phone control logic: 
	definition and initialization of the variables that are shared between the playback and capture threads 
	initialization of the sound card hardware 
	create a session with the remote party 
	start the second thread for sound capture and send 
	wait for connect from remote party, upon connect - start sending data received to the local speaker 
	We will see each of these steps in the code below. First we can see the jsresources.org utility classes AudioCapture and AudioPlayStream being used here - this greatly simplifies the code that we have to write - by bundling in the codec: 
*/
	    private boolean Stop=false;
		private AudioCapture recordLine = null;
		private AudioPlayStream playbackLine = null;
	//The variable IpToContact will contain the remote computer's IP, supplied by the command line. If the argument is not supplied, vsjtalk will default to the local host at IP 127.0.0.1. The TCP port we used to connect the two ends is fixed at 8085, out of the privileged port range on most TCP implementation. 
		private String IpToContact = null;
		private final static String LOCAL_IP = "127.0.0.1";
		private final static int VSJ_TALK_PORT = 8085;
	//We will be using a byte array buffer to transport the GSM data from the recording hardware to the transport layer - and from the transport layer to the speaker hardware. We select a buffer size of 1 second of uncompressed voice in this case. This will imply a delay (or latency) of at least 1 second, since the sound will not be sent over the 'wire' via the transport layer until the buffer is full. 
		// GSM 160 samples of 16 bits each
		// becomes 33 GSM bytes
		// 16k bytes / 320 * 33 (1 sec GSM)
		private int bufSize = 1650;
		private Session curSession = null;
		Thread thread;
	    Thread secondThread ;

		public Vsjtalk(String ip) {
			IpToContact = ip;
			}
			
	//In the main method, we extract the remote party's IP address if supplied. Since this is the main thread of the application - it also calls the phone logic which is encapsulated in the startPhone() method of the vsjtalk class, which we will examine next. 
		public static void main(String[] args) {
			if (args.length > 1) {
				System.out.println("Usage: vsjtalk [ip address]");
				return;
				}
			String tpIPAddr = (args.length == 0)? LOCAL_IP : args[0] ;

			Vsjtalk myphone = new Vsjtalk(tpIPAddr);
			myphone.startPhone();
			}
		
		
	//The startPhone() method will ultimately block looping in the run() method. Before blocking, it will initialize the audio hardware, create a session, and start the second (capture) thread. 
		public void startPhone() {
			initAudioHardware();
			secondThread = null;
			curSession = new Session(IpToContact);
			secondThread = new Thread(new RecordSender(curSession));
			secondThread.start();
			start();
			//run();
			}
	//To initialize the audio hardware, we use the jsresources.org utility classes instead of the JavaSound API directly. These utility wrapper classes know about and are specially designed to work with the tritonus GSM encoder that we depend on. Here, we are creating an instance of AudioPlayStream called playbackLine; and an instance of AudioCapture called recordLine. In fact, the instance of AudioPlayStream can be viewed as a standard JavaSound SourceDataline wrapped with a GSM decoder - and AudioCapture instance can be viewed as a TargetDataLine wrapped with a GSM encoder. 
		public boolean initAudioHardware() {

			AudioFormat format = null;
			InputStream playbackInputStream = null;

			try {
				format = AMAudioFormat.getLineAudioFormat(AMAudioFormat.FORMAT_CODE_GSM);
				recordLine = new AudioCapture(AMAudioFormat.FORMAT_CODE_GSM);
				playbackLine = new AudioPlayStream(format);
				if (recordLine == null) {
					System.out.println("initAudioHardware: cannot create recordLine");
					throw new Exception();
					}
				if (playbackLine == null) {
					System.out.println("initAudioHardware: cannot create playbackLine");
					throw new Exception();
					}
	//The comment below points out some of the many quirks of differing JavaSound implementations on different platforms. In some cases, depending on the native sound card driver you have on your operating system, if you reverse the order of the following open() calls - the program will fail. 
				// this ordering is vital to
				// some sound card drivers
				recordLine.open();

				playbackLine.open();
				System.out.println("----- "+getClass().getName() + "--after open");
				println("initAudioHardware: Opened audio channels..");
				return true;
				} catch (Exception ex) {
				      println("----- "+getClass().getName() + " initAudioHardware --line open: hardware problem : " + ex);
				return false;
				}
			}

		public void println(String inText) {
		System.out.println(inText);
		}
	//The run() method contains the looping main logic. First it calls the listen() method of the Session object - this will block until there is a remote connect. Upon connect, it obtains an InputStream from the Session and starts reading the GSM compressed voice data and sends it to the instance of AudioPlayStream handling the GSM decoding. 
public void run() {
		
		FileOutputStream fos;
		InputStream playbackInputStream = null;
		boolean complete = false;
		Stop = false;
		byte[] gsmdata = new byte[bufSize];
		int numBytesRead = 0;

		println("Waiting for remote voice connect");
  try {
  
		try {
			curSession.listen();
			playbackInputStream = curSession.getInputStream();//qui becca errore la 2 volta

			println("Incoming voice detected");
			playbackLine.start();
			println("Start sending to speaker");
			} catch (Exception e){
			    System.out.println("Exception caught: " + e);
			    e.printStackTrace();
			}

		// read transport stream of
		// compressed voice, write to
		// speaker

		while( !Stop ){ //!Stop
			try {
				numBytesRead = playbackInputStream.read(gsmdata);
				if(numBytesRead == -1){
					complete = true;
					break;
					}

				playbackLine.write(gsmdata, 0, numBytesRead);
				
				} catch (IOException e) {
				     println("-----Stop = " + Stop);
				     println("-----Exception encountered while processing audio: " + e);
				     e.printStackTrace();
				     Stop=true;
				     break;				     
				}
				
			}
	//This cleanup code closes the AudioPlayStream and AudioCapture instances and also the transport session. 

		// clean up
		
		
     } finally {	
	 
		try {
			println(getClass().getName() + " ---- sono su finally di run() : sto chiudento le linee....");
			playbackLine.drain();
			playbackLine.close();
			playbackLine = null;
			if(recordLine!=null) {recordLine.close(); 
			recordLine = null;}
			if(curSession!=null) curSession.close();
			
			} catch (IOException e){
			     println( getClass().getName() + " ---- Exception during cleanup: " + e);
			}
		println(getClass().getName() + " Cleanup completed.");
	 }
  }
	//Concurrent recording thread - RecordSender class
	//The logic of the second thread (capture thread) is contained in the RecordSender class. This thread performs the work of capturing, compressing and transmitting the voice data. The class is initialized with an instance of the Session class. This Session manages the transport level object that the thread will use to obtain an OutputStream corresponding to the remote phone. 

  class RecordSender implements Runnable {
		Session mySession = null;
		//OutputStream sendStream = null;

		public RecordSender(Session openSession){
			mySession = openSession;
			}
	//The run() method represents the thread logic of the second thread. It waits about 10 seconds initially to ensure that the first thread has settled, and are waiting for connect - allowing simple loopback testing. Then it calls the open() method of the Session object, which will initiate the remote connection. Once connected, it calls the getOutputStream() method to obtain the transport level stream connection. 
		public void run() {
			try{
				Thread.sleep(10 * 1000);
				mySession.open();
				sendStream = mySession.getOutputStream();
				} catch (Exception e) {
				    println("---RecordSender has problem connecting to remote");
				    e.printStackTrace();
				//System.exit(1);
				}
	//A check on recordline==null serves as a guard for synchronisation problems - just in case the main playback thread for some reason has not yet completed audio hardware initialization. 
			byte[] compressedVoice = new byte[bufSize];
			int numBytesRead=0;
			if(recordLine==null){
				// should never happen
				println("RecordSender detects that recordLine not initialized");
				//system.exit(1);
				
				}

			println(getClass().getName() + " RecordSender connected successfully.");
	//Next, it reads compressed voice data from the AudioCapture instance a buffer at a time, and writes them to the OutputStream of the Session object (ie. the remote phone). 
			//InputStream myIStream = null;
			try {
				recordLine.start();
				myIStream = recordLine.getAudioInputStream();
				int b=0;
				while((!Stop) ){
					b = myIStream.read(compressedVoice,0, bufSize);
					sendStream.write(compressedVoice,0,b);
					}
				} catch (Exception e){
				    println(getClass().getName() + " Stop = " + Stop + " RecordSender caught an exception in run() : " + e);
				    e.printStackTrace();
				   
				}
				
	//And finally, some cleanup code. 
		  	finally {
				try {
				
					println(getClass().getName() + " RecordSender : eseguo finally");
				if (myIStream!=null) myIStream.close();
				if (recordLine!=null) recordLine.close();
				// if we are just recording, though, it's ok to do it here
				if (sendStream!=null) {
					sendStream.flush(); 
				    sendStream.close();
			     }    
				// this will send an eof message

				} catch (IOException e){
				println("RecordSender caught an exception during cleanup: " + e);
				}
			println("RecordSender terminated.");
			}
		  }
		}
	
	public void stopCall() throws IOException {
		
		System.out.println("--------- "+ getClass().getName() + "..... chiudo chiamata GSM" );
		Stop = true;    
	    
	    if (myIStream!=null) {
	    	myIStream.close(); 
			System.out.println("--------- "+ getClass().getName() + "..... inputStream chiusa!!");
			myIStream=null;
	    } 
		if (recordLine!=null) {
			recordLine.close(); 
			System.out.println("--------- "+ getClass().getName() + "..... AudioCapture chiusa!!");
			recordLine=null;
		} 
	    // if we are just recording, though, it's ok to do it here
		if (sendStream!=null) {			
			sendStream.flush(); 
			sendStream.close();
			System.out.println("--------- "+ getClass().getName() + "..... OutputStream chiusa!!");
			sendStream=null;
		 }    
	    
		curSession.close();
		System.out.println("--------- "+ getClass().getName() + "..... Session chiusa!!");
		
		thread.interrupt();
		System.out.println("--------- "+ getClass().getName() + "..... Thread chiuso!!");
	    thread=null;
	
	    
	    
		secondThread.interrupt();
		System.out.println("--------- "+ getClass().getName() + "..... Thread2 chiuso!!");
		secondThread=null;
		
		interrupt();

	    //stop();
	}	
	
	public void start() {
		if (thread == null){
			thread = new Thread(this);
			thread.start();
		}
	}
	
	//Session establishment and transport: session class
	//The session class encapsulates the session establishment mechanism. In this case, we simply use two TCP/IP connections - one listening on port 8085 (VSJ_TALK_PORT), and one connecting to the remote phone's IP at port 8085. The same session object can be used with minor modifications if the session initiation mechanism changes (say, to JNDI lookup or SIP - a popular IP phone session initialization protocol - based negotiation). 
	
	class Session {
		String ipAddr = null;
		Socket outSock = null;
		ServerSocket inServSock = null;
		Socket inSock = null;

		Session(String inIP) {
			ipAddr = inIP;
			}

		void open() throws IOException, UnknownHostException {
			if (ipAddr != null)
				outSock = new Socket(ipAddr,VSJ_TALK_PORT);
			}
		void listen() throws IOException {
			inServSock = new ServerSocket(VSJ_TALK_PORT);
			inSock = inServSock.accept();
			}
		public InputStream getInputStream() throws IOException {
			if (inSock != null)
				return inSock.getInputStream();
			else
				return null;
			}
		public OutputStream getOutputStream() throws IOException {
			if (outSock != null)
				return outSock.getOutputStream();
			else
				return null;
			}

		void close() throws IOException {
			
			if (inSock!=null)inSock.close();
			System.out.println("--------- "+ getClass().getName() + "..... Sock input chiusa!!");
			
			if (outSock!=null)outSock.close();
			System.out.println("--------- "+ getClass().getName() + "..... Sock output chiusa!!");
			
			if (inServSock!=null)inServSock.close();
			System.out.println("--------- "+ getClass().getName() + "..... ServerSock chiusa!!");
			inServSock=null;
			inSock=null;
			outSock=null;
			}
		}
	}



