import javax.swing.JFrame;
import javax.swing.JTextPane;
import javax.swing.text.AttributeSet;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;
import javax.swing.text.StyledDocument;

import java.awt.AWTException;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Point;
import java.awt.Robot;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.awt.Font;
import javax.swing.SpringLayout;
import java.awt.Component;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JScrollPane;

import lejos.pc.comm.NXTCommFactory;
import javax.swing.JTextField;


public class DebuggerShell implements KeyListener, ActionListener {
	
	private JFrame f;
	private JTextPane textPane;
	private String history;
	private Debugger debugger;
	
	private ArrayList<Point> commands;
	private ArrayList<Point> messages;

	private JTextField textField;
	private JTextField textField_1;
	
	public DebuggerShell(Debugger d){
		this.debugger = d;
		
		f = new JFrame();
		commands = new ArrayList<Point>();
		messages = new ArrayList<Point>();
		
		history = "";
		
		f.setTitle("NXT Debugger");
		f.getContentPane().setLayout(null);
		
		JButton btnEstablishConnection = new JButton("Establish Connection");
		btnEstablishConnection.setBounds(516, 13, 154, 25);
		f.getContentPane().add(btnEstablishConnection);
		btnEstablishConnection.addActionListener(this);
		
		JButton btnEndConnection = new JButton("End Connection");
		btnEndConnection.setBounds(516, 51, 154, 25);
		f.getContentPane().add(btnEndConnection);
		btnEndConnection.addActionListener(this);
		
		JButton btnForward = new JButton("Forward");
		btnForward.setBounds(516, 422, 154, 25);
		f.getContentPane().add(btnForward);
		btnForward.addActionListener(this);
		
		JButton btnLeft = new JButton("Left");
		btnLeft.setBounds(516, 460, 69, 25);
		f.getContentPane().add(btnLeft);
		btnLeft.addActionListener(this);
		
		JButton btnRight = new JButton("Right");
		btnRight.setBounds(601, 460, 69, 25);
		f.getContentPane().add(btnRight);
		btnRight.addActionListener(this);
		
		JButton btnBackward = new JButton("Backward");
		btnBackward.setBounds(516, 497, 154, 25);
		f.getContentPane().add(btnBackward);
		btnBackward.addActionListener(this);
		
		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setBounds(12, 13, 492, 472);
		f.getContentPane().add(scrollPane);
		textPane = new JTextPane();
		textPane.setEditable(false);
		textPane.setBounds(12, 13, 492, 509);
		textPane.setFont(new Font("Courier New", Font.PLAIN, 14));
		scrollPane.setViewportView(textPane);
		
		textField = new JTextField();
		textField.setBounds(36, 498, 468, 22);
		f.getContentPane().add(textField);
		textField.setColumns(10);
		textField.addKeyListener(this);
		
		textField_1 = new JTextField();
		textField_1.setEditable(false);
		textField_1.setText(">>");
		textField_1.setBounds(12, 498, 24, 22);
		f.getContentPane().add(textField_1);
		textField_1.setColumns(10);
		f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		f.setVisible(true);
		f.setSize(700, 580);
		
		printMessage("Debugger Started.\nPlease press \"ESTABLISH CONNECTION\" to begin debugging.");
		

		textField.requestFocus();
		textField.requestFocusInWindow();
	}
	
	
	public void printRobotMessage(String message){
		messages.add(new Point(history.length(), message.length()));
		
		history += (message+ "\n");
		textPane.setText(history);
		color();
	}
	
	
	public void printMessage(String message){
		history += (message+ "\n");
		textPane.setText(history);
		color();
	}
	
	
	private void color(){
		StyledDocument sDoc = textPane.getStyledDocument();
		StyleContext sc = StyleContext.getDefaultStyleContext();
		AttributeSet aset = sc.addAttribute(SimpleAttributeSet.EMPTY, StyleConstants.Foreground, Color.BLUE);
		AttributeSet asetM = sc.addAttribute(SimpleAttributeSet.EMPTY, StyleConstants.Foreground, Color.RED);
		AttributeSet asetT = sc.addAttribute(SimpleAttributeSet.EMPTY, StyleConstants.Foreground, Color.MAGENTA);
		sDoc.setCharacterAttributes(0, "Debugger Started.".length(), asetT, true);
		
		for(Point p: commands){
			sDoc.setCharacterAttributes(p.x, p.y, aset, true);
		}
		for(Point p: messages){
			sDoc.setCharacterAttributes(p.x, p.y, asetM, true);
		}
	}

	@Override
	public void keyPressed(KeyEvent arg0) {
		if(arg0.getKeyCode() == 10){
			int oldLength = history.length();
			String command = textField.getText();
			textField.setText("");
			history += ">> " + command + "\n";
			commands.add(new Point(oldLength, command.length() + 3));
			
			textPane.setText(history);
			color();
			
			debugger.runCommand(command);
		}
	}

	@Override
	public void keyReleased(KeyEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void keyTyped(KeyEvent arg0) {
		// TODO Auto-generated method stub
	}

	@Override
	public void actionPerformed(ActionEvent arg0) {
		// TODO Auto-generated method stub
		String ac = arg0.getActionCommand();
		if(ac.equals("Establish Connection")){
			debugger.establishConnection();
		} else if(ac.equals("End Connection")){
			debugger.endConnection();
		} else System.out.println(ac);
	}
}
