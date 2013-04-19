import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Scanner;

import lejos.pc.comm.NXTComm;
import lejos.pc.comm.NXTCommException;
import lejos.pc.comm.NXTCommFactory;
import lejos.pc.comm.NXTInfo;

public class Debugger {
	private DebuggerShell shell;
	private boolean isConnected;
	private Thread readThread;

	private static NXTComm connection;
	private static boolean USBTest = false;
	private static NXTInfo[] info;
	private static long start;
	private static long latency;
	static Boolean readFlag = true;
	static Object lock = new Object();
	private OutputStream os;
	private InputStream is;
	private DataOutputStream oHandle;
	private DataInputStream iHandle;
	private static byte[] buffer = new byte[256];

	public Debugger() throws NXTCommException {
		shell = new DebuggerShell(this);
		this.isConnected = false;
	}

	public boolean isConnected() {
		return this.isConnected;
	}

	public void readFromRobot() {
		readThread = new Thread() {
			public void run() {
				try {
					while (true) {
						int count = iHandle.read(buffer);
						if (count > 0) {
							String input = (new String(buffer))
									.substring(0, 11);
							shell.printRobotMessage("Message from robot: "
									+ input);

							shell.set(input.charAt(2) + "",
									Integer.parseInt(input.substring(3, 10)));
						}
					}
				} catch (Exception e) {
					System.out.println("read error");
				}
			}
		};
		readThread.start();
	}

	public void stopReading() {
		if(readThread == null) return;
		else if(readThread.isAlive()) readThread.stop();
	}

	public void establishConnection() {
		Thread t = new Thread() {
			public void run() {
				start = System.currentTimeMillis();
				try {
					if (USBTest) {
						connection = NXTCommFactory
								.createNXTComm(NXTCommFactory.USB);
						info = connection.search(null, 0);
					} else {
						connection = NXTCommFactory
								.createNXTComm(NXTCommFactory.BLUETOOTH);
						info = connection.search("NXT", 1234);
					}
					if (info.length == 0) {
						shell.printMessage("Unable to find device");
						return;
					}

					connection.open(info[0]);
					os = connection.getOutputStream();
					is = connection.getInputStream();

					oHandle = new DataOutputStream(os);
					iHandle = new DataInputStream(is);
					latency = System.currentTimeMillis() - start;
					shell.printRobotMessage("Connection is established after "
							+ latency + "ms.");
					isConnected = true;

					readFromRobot();
					sendMessage("DMSM000001");

				} catch (Exception e) {
					shell.printMessage("Connection failed to establish.");
				}
			}
		};
		shell.printMessage("Establishing Connection...");
		t.start();
	}
	
	public void endConnection() {
		shell.printMessage("Ending Connection...");
		sendMessage("DMSM000000");
		sendMessage("EC00000000");
		stopReading();
		isConnected = false;
		shell.printRobotMessage("Disconnected from robot");
	}

	public void sendMessage(String message) {
		try {
			message += getCheckSum(message);
			shell.printMessage("Sending message: \"" + message + "\"");
			oHandle.write(message.getBytes());
			oHandle.flush();
		} catch (Exception e) {

		}
	}

	public static boolean isNumeric(String number) {
		try {
			int i = Integer.parseInt(number);
		} catch (NumberFormatException nfe) {
			return false;
		}
		return true;
	}

	public void runCommand(String command) {
		if (command.equalsIgnoreCase("help") || command.equalsIgnoreCase("?")) {
			shell.printMessage(getCommandHelp());
		} else if (!isConnected) {
			shell.printMessage("Robot is not connected!");
		} else if (command.equalsIgnoreCase("exit")) {
			endConnection();
		} else {
			String message = createCommand(command);
			sendMessage(message);
		}

	}

	public static String getCommandHelp() {
		
		return "To form each the commands follow the directions below.  Arguments are sepearated by spaces. Case does not matter."
				+ " \n\nMOVE: Type: 'move [specify direction: forward or backward] [distance in cm]' \nFor example to move forward 120 cm, type: move forward 120"
				+ " \n\nARC: Type: 'arc [specify direction: forward or backward] [specify direction to arc in: left of right].\nFor example, to arc up and right, type: arc forward right"
				+ " \n\nTURN: Type: 'turn [specify direction: right or left] [specify number of degrees to turn]"
				+ " \n\nSTOP: Type: 'stop'"
				+ " \n\nSET SPEED: Type: 'setspeed [specify: a, b, c, or d, representing motor a, b, c, or drive] [t or r for type of speed] [number of new speed].\nFor example to set motor a to speed 30 type: setspeed a 30"
				+ " \n\nREAD: Type: 'read [specify: u, t, m, l, or all, for ultrasonic, touch, microphone, light, or all sensor information respectively].  \nFor example to read information from the light sensor type: read l.  \nTo read information from all sensors type: read all"
				+ " \n\nNONE: To create NoOp message type: none";
	}

	public static String getCheckSum(String str) {
		int sum = 0;
		String ret;
		byte[] buffer = str.getBytes();
		for (int i = 0; i < buffer.length; i++) {
			sum += (int) buffer[i];
		}
		sum = sum % 256;
		byte[] checksum = new byte[1];
		checksum[0] = (byte) sum;
		ret = new String(checksum);
		return ret;
	}

	public static String createCommand(String cmd) {
		String message = "";
		String[] cmdWords = cmd.split(" ");
		String command = getCommand(cmdWords);
		String[] args = getCommandArguments(cmdWords);

		if (cmdWords.length < 1) {
			return message;
		} else {
			if (command.equalsIgnoreCase("move")) {
				message = createMoveCommand(args);
			} else if (command.equalsIgnoreCase("arc")) {
				message = createArcCommand(args);
			} else if (command.equalsIgnoreCase("turn")) {
				message = createTurnCommand(args);
			} else if (command.equalsIgnoreCase("stop")) {
				message = createStopCommand();
			} else if (command.equalsIgnoreCase("setspeed")) {
				message = createSetSpeedMessage(args);
			} else if (command.equalsIgnoreCase("read")) {
				message = createReadSensorMessage(args);
			} else if (command.equalsIgnoreCase("none")) {
				message = createNoOpMessage();
			}
		}
		return message;
	}

	private static String addPaddingZeros(String command, String endOfCommand){
		for(int i = command.length() - 1; i < 9-endOfCommand.length(); i++){
			command += "0";
		}
		return command + endOfCommand;
	}
	
	private static String addTrailingZeros(String message) {
		if (message.length() >= 10)
			return message;
		else {
			while (message.length() < 10) {
				message += "0";
			}
			return message;
		}
	}

	public static String[] getCommandArguments(String[] words) {
		String[] args = new String[words.length - 1];
		for (int i = 1; i < words.length; i++) {
			args[i - 1] = words[i];
		}
		return args;
	}

	public static String getCommand(String[] words) {
		return words[0];
	}

	public static String createMoveCommand(String[] args) {
		String command = "MS";
		if (args.length < 1) {
			return "";
		} else {
			if (args[0].equalsIgnoreCase("forward")
					|| args[0].equalsIgnoreCase("forwards")) {
				command += "F";
			} else if (args[0].equalsIgnoreCase("backward")
					|| args[0].equalsIgnoreCase("backwards")) {
				command += "B";
			}
			if (args.length == 2) {
				if (isNumeric(args[1])) {
					for (int i = 0; i < 7 - args[1].length(); i++) {
						command += "0";
					}
					command += args[1];
				} else {
					return "";
				}
			} else if (args.length > 2) {
				return "";
			} else {
				command = addTrailingZeros(command);
			}
		}
		return command;
	}

	public static String createArcCommand(String[] args) {
		String command = "MA";
		if (args.length < 1) {
			return "";
		} else {
			if (args[0].equalsIgnoreCase("forward")
					|| args[0].equalsIgnoreCase("forwards")) {
				command += "F";
			} else if (args[0].equalsIgnoreCase("backward")
					|| args[0].equalsIgnoreCase("backwards")) {
				command += "B";
			} else {
				return "";
			}
			if (args.length >= 2) {
				if (args[1].equalsIgnoreCase("left")) {
					command += "L";
				} else if (args[1].equalsIgnoreCase("right")) {
					command += "R";
				} else {
					return "";
				}
			}
			command += "090";
			command = addTrailingZeros(command);
		}
		return command;
	}

	public static String createTurnCommand(String[] args) {
		String command = "TN";
		if (args.length < 1) {
			return "";
		} else {
			if (args[0].equalsIgnoreCase("right")) {
				command += "R";
			} else if (args[0].equalsIgnoreCase("left")) {
				command += "L";
			} else {
				return "";
			}
			if (args.length > 1) {
				if (isNumeric(args[1])) {
					for (int i = 0; i < 7 - args[1].length(); i++) {
						command += "0";
					}
					command += args[1];
				} else {
					return "";
				}
			} else {
				command = addTrailingZeros(command);
			}
		}
		return command;
	}

	public static String createStopCommand() {
		return addTrailingZeros("ST");
	}

	public static String createSetSpeedMessage(String[] args) {
		String command = "SS";
		if (args.length != 3) {
			return "";
		} else {
			if (args[0].equalsIgnoreCase("a") || args[0].equalsIgnoreCase("b")
					|| args[0].equalsIgnoreCase("c")
					|| args[0].equalsIgnoreCase("d")) {
				command += args[0].toUpperCase();
			} else {
				return "";
			}
			if (args[1].equalsIgnoreCase("t") || args[1].equalsIgnoreCase("r")) {
				command += args[1].toUpperCase();
			} else {
				return "";
			}
			if (isNumeric(args[2])) {
				for (int i = 0; i < 6 - args[2].length(); i++) {
					command += "0";
				}
				command += args[2];
			} else {
				return "";
			}
		}
		return command;
	}

	public static String createReadSensorMessage(String[] args) {
		String command = "";
		if (args.length != 1) {
			return "";
		}
		if (args[0].equalsIgnoreCase("all")) {
			command = addTrailingZeros("RA");
		} else {
			if (args[0].equalsIgnoreCase("u") || args[0].equalsIgnoreCase("t")
					|| args[0].equalsIgnoreCase("m")
					|| args[0].equalsIgnoreCase("l")) {
				command = addTrailingZeros("RS" + args[0].toUpperCase());
			}
		}
		return command;
	}

	public static String createNoOpMessage() {
		return "0000000000";
	}

	public static String createMalformedMessage() {
		String alpha = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
		String[] commands = new String[8];
		commands[0] = "MSF0000000";
		commands[1] = "MAFL090000";
		commands[2] = "TNR0000000";
		commands[3] = "ST00000000";
		commands[4] = "SSA0000000";
		commands[5] = "RA00000000";
		commands[6] = "RSU0000000";
		commands[7] = "0000000000";
		int pick = (int) (Math.random() * commands.length);
		String rand = commands[pick];
		pick = (int) (Math.random() * rand.length());
		int replace = (int) (Math.random() * 26);
		String wrongString = "" + alpha.charAt(replace);
		String malformed = rand.substring(0, pick);
		malformed += wrongString;
		malformed += rand.substring(pick + 1, rand.length());

		int size = (int) (Math.random() * 3);
		if (size == 0) {
			int length = (int) (Math.random() * 9 + 1);
			malformed = malformed.substring(0, length);
		} else if (size == 2) {
			int length = (int) (Math.random() * 255 + 1);
			for (int i = 0; i < length; i++)
				;
			malformed += "0";
		}
		return malformed;
	}

	public static void main(String[] args) throws Exception {
		Debugger d = new Debugger();
	}
}
