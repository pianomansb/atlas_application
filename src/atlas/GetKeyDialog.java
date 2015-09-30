package atlas;

import java.awt.Color;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashSet;
import java.util.Hashtable;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;

public class GetKeyDialog {
	JDialog dialog;
	KeyStroke keyStroke;
	
	@SuppressWarnings("serial")
	private GetKeyDialog(Frame frame, Hashtable<KeyStroke, Action> accels, String actionName) {
		GetKeyComponent comp = new GetKeyComponent(accels, actionName);
		Object[] array = { comp };
		JOptionPane optionPane = new JOptionPane(array, JOptionPane.PLAIN_MESSAGE, JOptionPane.OK_CANCEL_OPTION);
		optionPane.getInputMap().put(KeyStroke.getKeyStroke("ENTER"), "enter");
		optionPane.getActionMap().put("enter", new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
			}
		});
		optionPane.addPropertyChangeListener(new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent e) {
				if( e.getSource() == optionPane && e.getNewValue() instanceof Integer){
					if( (int)e.getNewValue() == JOptionPane.OK_OPTION ){
						if( comp.conflicting ) return;
						keyStroke = comp.keyStroke;
						dialog.setVisible(false);
					} else if( (int)e.getNewValue() == JOptionPane.CANCEL_OPTION ){
						keyStroke = null;
						dialog.setVisible(false);
					}
				}
			}
		});
		dialog = new JDialog(frame, "Get new key", true);
		dialog.setContentPane(optionPane);
		dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
	}
	
	public KeyStroke getKeyStroke() {
		return keyStroke;
	}

	public static KeyStroke showGetKeyDialog(Frame frame, 
			Hashtable<KeyStroke, Action> accels, String actionName) {
		GetKeyDialog getKey = new GetKeyDialog(frame, accels, actionName);
		getKey.dialog.pack();
		getKey.dialog.setVisible(true);
		
		return getKey.getKeyStroke();
	}

	
	@SuppressWarnings("serial")
	private class GetKeyComponent extends JComponent implements KeyListener {
		private boolean listening = false;
		private JLabel input;
		private KeyStroke keyStroke = null;
		private HashSet<Integer> allowableFinalKeys = new HashSet<Integer>();
		private final Hashtable<KeyStroke, Action> accels;
		private boolean conflicting = false;
				
		public GetKeyComponent(Hashtable<KeyStroke, Action> accels_, String actionName) {
			accels = accels_;
			initializeFinalKeys();
			setFocusTraversalKeysEnabled(false);
			JLabel title = new JLabel("Set key for \"" + actionName + "\":");
			input = new JLabel("<input>");
			JButton typeButton = new JButton("Add key");
			typeButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					listening = true;
					input.setText("<type key>");
					setFocusable(true);
					requestFocusInWindow();
				}
			});
			
			GroupLayout layout = new GroupLayout(this);
			setLayout(layout);
			layout.setHorizontalGroup(layout.createParallelGroup()
				.addComponent(title)
				.addGroup(layout.createSequentialGroup()
					.addComponent(typeButton)
					.addComponent(input)));
			layout.setVerticalGroup(layout.createSequentialGroup()
				.addComponent(title)
				.addGroup(layout.createParallelGroup()
					.addComponent(typeButton)
					.addComponent(input)));
			
			addKeyListener(this);
		}

		@Override
		public void keyTyped(KeyEvent e) {
		}

		@Override
		public void keyPressed(KeyEvent e) {
			e.consume();
			if( listening ){
				if( allowableFinalKeys.contains(e.getKeyCode()) ){
					listening = false;
					keyStroke = KeyStroke.getKeyStrokeForEvent(e);
					String keytext = PreferencesWindow.createAccelString(keyStroke);
					if( accels.get(keyStroke) != null ){
						conflicting = true;
						keytext += ": " + accels.get(keyStroke).getValue(Action.NAME);
						input.setText(keytext);
						input.setForeground(Color.red);
					} else {
						input.setText(keytext);
						input.setForeground(Color.black);
					}
				}
			}
		}

		@Override
		public void keyReleased(KeyEvent e) {
		}
	
		private void initializeFinalKeys() {
			String allowableKeyString = "27 192 49 50 51 52 53 54 55 56 57 48 45 "
					+ "61 8 9 81 87 69 82 84 89 85 73 79 80 91 93 92 65 83 "
					+ "68 70 71 72 74 75 76 59 222 90 88 67 86 66 78 77 44 "
					+ "46 47 32 38 40 37 39 112 113 114 115 116 117 118 119 "
					+ "120 121 122 123 10";
			String[] allowableKeys = allowableKeyString.split(" ");
			for( String i : allowableKeys )
				allowableFinalKeys.add(new Integer(i));
		}
	}
}

