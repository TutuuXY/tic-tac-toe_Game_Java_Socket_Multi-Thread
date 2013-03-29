import java.io.BufferedReader;
import java.net.SocketException;
import java.lang.InterruptedException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Calendar;
import java.util.Vector;

public class client {
	public static int serverPort;
	public static int clientPort;
	public static String serverIP;
	public static String resp;
	public static int packetID;
	public static String state;
	public static String loginName;
	public static Vector<Packet> outp;

	private static class Packet {
		public String serverIP;
		public int serverPort;
		public String packet;
		public int packetID;
		public boolean acked;
		Packet() {}
		Packet(String serverIP, int serverPort, String packet, int packetID) {
			this.serverIP = serverIP;
			this.serverPort = serverPort;
			this.packet = packet;
			this.packetID = packetID;
			this.acked = false;
		}
		public String toString() {
			return packetID + "\t" + serverIP + "\t" + serverPort + "\t" + packet + "\t" + acked;
		}
	}

	client() { }

	client(int serverPort, String serverIP, int clientPort) {
		this.serverPort = serverPort;
		this.serverIP = serverIP;
		this.clientPort = clientPort;
	}

	private static class SendThread extends Thread {
		public void run() {
			try {
				int bufferSize = 1024;
				
				// Begin to send
				BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
				byte[] buffer = new byte[bufferSize];
				// send out
				while (true) {
					String inputString = input.readLine();
					String packet = getPacket( inputString );
					
					Packet ack = new Packet(serverIP, serverPort, packet, packetID); 
					outp.add( ack );

					/*
					System.out.println("outp contents");
					for (Packet p : outp) {
						System.out.println(p);
					}
					*/

					buffer = packet.getBytes();
					DatagramPacket sendPacket = new DatagramPacket( buffer, buffer.length,
							InetAddress.getByName( serverIP ), serverPort );
					socket.send( sendPacket );

					//System.out.println( "Send to server: " + packet );
				}
			} catch (IOException e) {
				System.out.println( e );
			}
		}

		public String getPacket(String inputString) {
			String ret = "";
			packetID++; 
			String id = String.valueOf(packetID);
			if (inputString.startsWith("login")) { // login,1,john,2134
				String name = inputString.substring( inputString.indexOf(" ") + 1 );
				ret = "login," + id + "," + name + "," + String.valueOf(clientPort);
				loginName = name;

			} else if (inputString.startsWith("ls")) { // list,2,john
				ret = "list," + id + "," + loginName;

			} else if (inputString.startsWith("choose")) { // choose,3,john,matt
				String name = inputString.substring( inputString.indexOf(" ") + 1 );
				ret = "choose," + id + "," + loginName + "," + name;

			} else if (inputString.startsWith("accept")) { // ackchoose,2,matt,john,A
				ret = "ackchoose," + id + "," + requesterName + "," + loginName + ",A";

			} else if (inputString.startsWith("deny")) { // ackchoose,3,matt,john,D
				ret = "ackchoose," + id + "," + requesterName + "," + loginName + ",D";

			} else if (inputString.startsWith("play")) { // play,2,john,9
				String num = inputString.substring( inputString.indexOf(" ") + 1 );
				System.out.println(loginName+" "+num);
				ret = "play," + id + "," + loginName + "," + num;

			} else if (inputString.startsWith("logout")) { // logout,john
				ret = "logout," + id + "," + loginName;
				System.out.println( loginName + " logout" );
			}
			return ret;
		}
	}
	public static String requesterName;

	public static void sendMissing(String packet, String clientIP, int clientPort) throws SocketException {
		byte[] buffer = new byte[1024];
		try {
			buffer = packet.getBytes();
			DatagramPacket sendPacket = new DatagramPacket( buffer, buffer.length,
					InetAddress.getByName( clientIP ), clientPort );
			socket.send( sendPacket );
			//System.out.println( "Send Dropped Msg to " + clientIP + ":" + clientPort + "\t" + packet );
		} catch (IOException e) {
			System.out.println( e );
		}
	}

	private static class RecThread extends Thread {
		public void run() {
			try {
				int bufferSize = 1024;
				
				//System.out.println("Receiving at port " + String.valueOf( clientPort ) + " ...");
				
				byte[] buffer = new byte[bufferSize];
				DatagramPacket receiverPacket = new DatagramPacket( buffer, buffer.length);
				socket.setSoTimeout(10000);
				
				while (true) {
					try {
						socket.receive( receiverPacket );

						String senderIP = receiverPacket.getAddress().getHostAddress();
						int senderPort = receiverPacket.getPort();
						String packet = new String(buffer, 0, receiverPacket.getLength());

						processPacket( packet );

						//System.out.println("Receive from sender (IP: " + senderIP + 
						//	", Port: " + String.valueOf(senderPort) + "): " + packet);
			//			System.out.println( packet );

					} catch(IOException e) {
						for (Packet p : outp) {
							if (p.acked == false) {
								sendMissing(p.packet, p.serverIP, p.serverPort);
							}
						}
					}
				}
			} catch (IOException e) {
				System.out.println( e );
			}
		}
		public void processPacket( String packet ) {
			String[] p = packet.trim().split(",");
			//System.out.println( "ack content:\t" + packet );
			for (String str : p) {
				str = str.trim();
			//	System.out.println("trim\t\t" + str + "\t\t" + str.length() );
			}

			if (p[0].compareTo("acklogin")==0) { //	acklogin,F/S
				if (p[1].charAt(0) == 'F') {
					System.out.println("login fail " + loginName);
				} else {
					System.out.println("login success " + loginName);
				}

			} else if (p[0].compareTo("ackls")==0) { // ackls,name,state,name,state..
				for (int i=1; i<p.length; i+=2) {
					System.out.println( p[i] + " " + p[i+1] );
				}
				System.out.println("EOL");

			} else if (p[0].compareTo("request")==0) { // request,name
				System.out.println("request from " + p[1]);
				requesterName = p[1];

			} else if (p[0].compareTo("ackchoose")==0) { //ackchoose,name,A/D/F
				if (p[2].charAt(0) == 'A') {
					System.out.println("request accepted by " + p[1]);
				} else if (p[2].charAt(0) == 'D') {
					System.out.println("request denied by " + p[1]);
				} else if (p[2].charAt(0) == 'F') {
					System.out.println("request to " + p[1] + " failed");
				}

			} else if (p[0].compareTo("ack")==0) { // ack,packetID
//				System.out.println("packet " + p[1] + " received by server");
				outp.get( Integer.parseInt(p[1]) ).acked = true;

			} else if (p[0].compareTo("play")==0) { // play,000000000
				for (int i=0; i<p[1].length(); i++) {
					if ( i % 3 == 0 ) {
						System.out.println();
					}
					if (p[1].charAt(i) == '0') {
						System.out.print('_');
					} else if (p[1].charAt(i) == '1') {
						System.out.print('1');
					} else if (p[1].charAt(i) == '2') {
						System.out.print('2');
					}
				}
				System.out.println();

			} else if (p[0].compareTo("ackplay")==0) { // ackplay,O/T
				if (p[1].charAt(0) == 'O') {
					System.out.println("Occupied");
				} else if (p[1].charAt(0) == 'T') {
					System.out.println("Out of turn");
				}

			} else if (p[0].compareTo("result")==0) { // result,W/L/D
				if (p[1].charAt(0) == 'W') {
					System.out.println(loginName + " win");
				} else if (p[1].charAt(0) == 'L') {
					System.out.println(loginName + " lose");
				} else if (p[1].charAt(0) == 'D') {
					System.out.println(loginName + " draw");
				}
			}
		}
	}
	public static DatagramSocket socket;
	
	public static void main(String[] args) throws IOException, InterruptedException {
		if (args.length < 3) {
			System.out.println("Terminated due to Error: The command line arguments are not valid.");
			return;
		}

		clientPort = Integer.parseInt(args[0]);
		socket = new DatagramSocket( clientPort );
		serverPort = Integer.parseInt(args[2]);
		serverIP = args[1];
		packetID = -1;
		state = "free";
		outp = new Vector<Packet>();
		
//		System.out.println( clientPort + "\t" + serverIP + "\t" + serverPort );

		SendThread sender = new SendThread();
		RecThread receiver = new RecThread();

		receiver.start();
		sender.start();

		sender.join();
		receiver.join();
	}
}
