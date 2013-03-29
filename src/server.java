import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

public class server {
		
	public static int bufferSize = 1024;
	public static Packet cur;

	server() {
	}

	private static class GameState {
		StringBuilder board;
		String turn;
		String p1;
		String p2;
		GameState( String p1, String p2, String turn, StringBuilder board ) {
			this.p1 = p1;
			this.p2 = p2;
			this.turn = turn;
			this.board = board;
		}
		public String toString() {
			return p1 + "\t" + p2 + "\tturn = " + turn + "\t" + board;
		}
	}

	private static class Packet {
		public String name;
		public String clientIP;
		public int clientPort;
		public String packet;
		Packet() {}
		Packet(String clientIP, int clientPort, String packet) {
			this.clientIP = clientIP;
			this.clientPort = clientPort;
			this.packet = packet;
			this.name = name;
		}
		public String toString() {
			return name + "\t" + clientIP + "\t" + clientPort + "\t" + packet;
		}
	}
	
	public static DatagramSocket socket;

	public static void execute() throws SocketException {
		socket = new DatagramSocket( 4119 );
		//System.out.println("Receiving at port " + 4119 + " ...");
		while (true) {
			cur = receive();
			inp.add( cur );
			Packet msg = procesackPacket( cur );
			if (! cur.packet.startsWith("logout")) {
				send( msg.packet, msg.clientIP, msg.clientPort );
			}
			outp.add( msg );
		//	display();
		}
	}

		
	public static void display() {
		System.out.println("\n*************** Display ****************");
		System.out.println("\tipPool");
		for (Map.Entry<String, Integer> e : ipPool.entrySet()) {
			System.out.println("\t" + e.getKey() + "\t" + e.getValue());
		}
		System.out.println();
		System.out.println("\tpairs");
		for (Map.Entry<String, String> e : pairs.entrySet()) {
			System.out.println("\t" + e.getKey() + "\t" + e.getValue());
		}
		System.out.println();
		System.out.println("\tstate");
		for (Map.Entry<String, String> e : state.entrySet()) {
			System.out.println("\t" + e.getKey() + "\t" + e.getValue());
		}
		System.out.println();
		System.out.println("\tnamePort");
		for (Entry<String, Integer> e : namePort.entrySet()) {
			System.out.println("\t" + e.getKey() + "\t" + e.getValue());
		}
		System.out.println();
		System.out.println("\tgames");
		for (Map.Entry<String, GameState> e : games.entrySet()) {
			System.out.println("\t" + e.getKey() + "\t" + e.getValue());
		}
		System.out.println();
		System.out.println("\tinPackets");
		for (Packet e : inp) {
			System.out.println("\t" + e.toString());
		}
		System.out.println();
		System.out.println("\toutPackets");
		for (Packet e : outp) {
			System.out.println("\t" + e.toString());
		}
		System.out.println("\n\n---------------------------------");
	}

	public static HashMap<String, String> pairs;
	public static TreeMap<String, String> state;
	public static HashMap<String, Integer> namePort;
	public static HashMap<String, GameState> games;
	public static HashMap<String, Integer> ipPool;
	public static ArrayList<Packet> inp;
	public static ArrayList<Packet> outp;
	public static String IP;

	public static Packet procesackPacket( Packet cur ) throws SocketException {
		Packet ret = new Packet();
		String[] p = cur.packet.trim().split(",");
		for (String str : p) {
			str = str.trim();
		}

		int sP = cur.clientPort;
		IP = cur.clientIP;
		ret.clientIP = "127.0.0.1";
		/******		login		******/
		if (p[0].compareTo("login")==0) { // login,1,john,2134
			ret.clientPort = Integer.parseInt(p[3]);
			int ackP = ret.clientPort;
			send( "ack,"+p[1], IP, sP );
			outp.add( new Packet(IP, sP, "ack,"+p[1]) ); 

			String sendip = cur.clientIP;
			if ( ipPool.containsKey( sendip ) ) {
				if ( ipPool.get(sendip).intValue() == 5 ) {
					ret.packet = "acklogin,F";
					return ret;
				} else {
					ipPool.put( sendip, ipPool.get(sendip).intValue() + 1 );
				}
			} else {
				ipPool.put( sendip, 1 );
			}

			if (state.containsKey(p[2])) { // no user?
				ret.packet = "acklogin,F";
			} else {
				ret.packet = "acklogin,S";
				state.put( p[2], "free" );
				namePort.put( p[2], ret.clientPort );
			}
			
		/******		list		******/
		} else if (p[0].compareTo("list")==0) { // list,2,john
			ret.clientPort = namePort.get(p[2]);
			int ackP = ret.clientPort;
			send( "ack,"+p[1], IP, sP );

			StringBuilder msg = new StringBuilder("ackls");
			for (Entry<String, String> e : state.entrySet()) {
				msg.append(","+e.getKey()+","+e.getValue());
			}
			ret.packet = msg.toString();

		/******		choose		******/
		} else if (p[0].compareTo("choose")==0) {// choose,3,john,matt
			ret.clientPort = namePort.get(p[2]);
			int ackP = namePort.get(p[2]);
			send( "ack,"+p[1], IP, sP );

			String sender = p[2];
			String rever = p[3];
			if (! state.containsKey( rever )) {
				ret.packet = "ackchoose," + rever + ",F";

			} else if (state.get( sender ).compareTo("free")!=0) {
				ret.packet = "ackchoose," + rever + ",F";

			} else if (state.get( rever ).compareTo("free")!=0) {
				ret.packet = "ackchoose," + rever + ",F";

			} else {
				state.put( sender, "decision");
				state.put( rever, "decision");
				ret.clientPort = namePort.get( rever );
				ret.packet = "request," + sender;

				pairs.put( sender, rever );
				pairs.put( rever, sender );
			}
			
		/******		ackchoose		******/
		} else if (p[0].compareTo("ackchoose")==0) { // ackchoose,2,matt,john,A
			// from John to Matt
			ret.clientPort = namePort.get( p[2] );
			int ackP = namePort.get( p[3] );
			send( "ack,"+p[1], IP, sP );

			if (p[4].charAt(0)=='A') {
				state.put(p[2], "busy");
				state.put(p[3], "busy");
				String k;
				if (p[2].compareTo(p[3]) > 0) {
					k = p[3] + " " + p[2];
				} else {
					k = p[2] + " " + p[3];
				}
				games.put(k, new GameState( p[2], p[3], p[2], new StringBuilder("000000000")));
				send("ackchoose," + p[2] + ",A", "127.0.0.1", namePort.get( p[2] ) );

				ret.packet = "play,"+games.get(k).board;

			} else { // 'D'
				ret.packet = "ackchoose," + p[2] + ",D";
				state.put( p[2], "free" );
				state.put( p[3], "free" );
				pairs.remove( p[2] );
				pairs.remove( p[3] );
			}
			
		/******		play		******/
		} else if (p[0].compareTo("play")==0) { // play,2,john,9
			ret.clientPort = namePort.get(p[2]);
			int ackP = namePort.get( p[2] );
			send( "ack,"+p[1], IP, sP );

			String n1 = p[2];
			String n2 = pairs.get( n1 );
			if (n1.compareTo(n2) > 0) {
				String tmp = n1;
				n1 = n2;
				n2 = tmp;
			}
			//System.out.println( n1 + " " + n2 );
			GameState gs = games.get( n1 + " " + n2 );
			int mv = Integer.parseInt( p[3] ) - 1;
			if ( gs.turn.compareTo(p[2])!=0 ) {
				ret.packet = "ackplay,T";
			} else if ( gs.board.charAt(mv) != '0' ) {
				ret.packet = "ackplay,O";
			} else {
				if ( p[2].compareTo(gs.p1)==0 ) {
					gs.board.setCharAt(mv, '1');
				} else {
					gs.board.setCharAt(mv, '2');
				}
				int flag = checkResult( gs.board.toString() );
				if ( flag == 1 ) { // result play 1 win
					ret.packet = "result,W";
					ret.clientPort = namePort.get(gs.p1);
					send( "result,L", "127.0.0.1", namePort.get(gs.p2) );
					state.put(n1, "free");
					state.put(n2, "free");
					pairs.remove( n1 );
					pairs.remove( n2 );
					games.remove( n1 + " " + n2 );
				} else if (flag == 2) { // result play 2 win
					ret.packet = "result,W";
					ret.clientPort = namePort.get(gs.p2);
					send( "result,L", "127.0.0.1", namePort.get(gs.p1) );
					state.put(n1, "free");
					state.put(n2, "free");
					pairs.remove( n1 );
					pairs.remove( n2 );
					games.remove( n1 + " " + n2 );
				} else if (flag == 0) { // result draw
					ret.packet = "result,D";
					ret.clientPort = namePort.get(gs.p1);
					send( "result,D", "127.0.0.1", namePort.get(gs.p2) );
					state.put(n1, "free");
					state.put(n2, "free");
					pairs.remove( n1 );
					pairs.remove( n2 );
					games.remove( n1 + " " + n2 );
				} else { // play normally
					if (gs.turn.compareTo(gs.p1)==0) {
						gs.turn = gs.p2;
					} else {
						gs.turn = gs.p1;
					}
					ret.packet = "play," + gs.board.toString();
					ret.clientPort = namePort.get(gs.turn);
				}
			}
			
		/******		logout		******/
		} else if (p[0].compareTo("logout")==0) {	// logout,6,john
			int ackP = namePort.get( p[2] );
			send( "ack,"+p[1], IP, sP );

			ipPool.put( cur.clientIP, ipPool.get( cur.clientIP ).intValue() - 1 );

			state.remove( p[2] );
			namePort.remove( p[2] );
		}
		return ret;
	}

	public static int checkResult(String a) {
		int ret = 0;
		if (a.charAt(0) == a.charAt(1) && a.charAt(1) == a.charAt(2) && a.charAt(0) != '0') {
			ret = Integer.parseInt( a.substring(0, 1) );
		} else if (a.charAt(3) == a.charAt(4) && a.charAt(4) == a.charAt(5) && a.charAt(3) != '0') {
			ret = Integer.parseInt( a.substring(3, 4) );
		} else if (a.charAt(6) == a.charAt(7) && a.charAt(7) == a.charAt(8) && a.charAt(6) != '0') {
			ret = Integer.parseInt( a.substring(6, 7) );

		} else if (a.charAt(0) == a.charAt(3) && a.charAt(3) == a.charAt(6) && a.charAt(0) != '0') {
			ret = Integer.parseInt( a.substring(0, 1) );
		} else if (a.charAt(1) == a.charAt(4) && a.charAt(4) == a.charAt(7) && a.charAt(1) != '0') {
			ret = Integer.parseInt( a.substring(1, 2) );
		} else if (a.charAt(2) == a.charAt(5) && a.charAt(5) == a.charAt(8) && a.charAt(2) != '0') {
			ret = Integer.parseInt( a.substring(2, 3) );

		} else if (a.charAt(0) == a.charAt(4) && a.charAt(4) == a.charAt(8) && a.charAt(0) != '0') {
			ret = Integer.parseInt( a.substring(0, 1) );
		} else if (a.charAt(2) == a.charAt(4) && a.charAt(4) == a.charAt(6) && a.charAt(2) != '0') {
			ret = Integer.parseInt( a.substring(2, 3) );
		} else {
			for (int i=0; i<a.length(); i++) {
				if (a.charAt(i)=='0') { // exist empty position indicating normal game
					ret = -1;
					break;
				}	
			}
		}
		return ret;
	}

	public static Packet receive() {
		Packet p = null;
		try {
			byte[] buffer = new byte[bufferSize];
			DatagramPacket receiverPacket = new DatagramPacket( buffer, buffer.length );
			socket.receive( receiverPacket );
			String senderIP = receiverPacket.getAddress().getHostAddress();
			int senderPort = receiverPacket.getPort();
			String packet = new String(buffer, 0, receiverPacket.getLength());

			p = new Packet( senderIP, senderPort, packet );

			//System.out.println(/*"[" + Calendar.getInstance().getTimeInMillis() + 
			//	"]*/"Receive from client (IP: " + senderIP + ", Port: " + 
			//	String.valueOf(senderPort) + "): " + packet);
		} catch (IOException e) {
			System.out.println( e );
		}
		return p;
	}
	
	public static void send(String packet, String clientIP, int clientPort) throws SocketException {
		byte[] buffer = new byte[bufferSize];
		try {
			buffer = packet.getBytes();
			DatagramPacket sendPacket = new DatagramPacket( buffer, buffer.length,
					InetAddress.getByName( clientIP ), clientPort );
			socket.send( sendPacket );
			//System.out.println( "Send to client " + clientIP + ":" + clientPort + "\t" + packet );
		} catch (IOException e) {
			System.out.println( e );
		}
	}

	public static void main(String[] args) throws IOException, InterruptedException {

		ipPool =  new HashMap<String, Integer>();
		pairs = new HashMap<String, String>();
		state = new TreeMap<String, String>();
		namePort = new HashMap<String, Integer>();
		games = new HashMap<String, GameState>();
		inp = new ArrayList<Packet>();
		outp = new ArrayList<Packet>();
		
		execute();
	}
}
