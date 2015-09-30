package atlas;

import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Hashtable;

import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.DefaultListModel;
import javax.swing.GroupLayout;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;

@SuppressWarnings("serial")
public class PreferencesWindow extends JPanel {
	private final DataManager dm;
	private JList<String> itemList = new JList<String>(new DefaultListModel<String>());
	//private Hashtable<String, JMenuItem> items;
	private Hashtable<KeyStroke, Action> accels;

	public PreferencesWindow(DataManager dm_) {
		dm = dm_;
		dm.setTitle("Atlas Preferences");
		//items = menuItems;
		refresh(dm.actionMap); //populate jlist
		itemList.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e ){
				if( e.getClickCount() == 2 ){
					String actionName = getItemFromSelection();
					KeyStroke ret = GetKeyDialog.showGetKeyDialog(dm.frame, accels, actionName);
					if( ret != null ){
						Action action = dm.actionMap.get(actionName);
						action.putValue(Action.ACCELERATOR_KEY, ret);
						action.putValue(Action.SHORT_DESCRIPTION, 
								actionName + " " + createAccelString(ret));
						refresh(dm.actionMap);
						writePreferencesFile();
					}
				}
			}
		});
		JScrollPane scrollPane = new JScrollPane(itemList);
		
		GroupLayout layout = new GroupLayout(this);
		setLayout(layout);
		layout.setHorizontalGroup(layout.createSequentialGroup()
			.addComponent(scrollPane));
		
		layout.setVerticalGroup(layout.createParallelGroup()
			.addComponent(scrollPane));
	}
	
	/**
	 * refreshes the jlist and the accels mapping
	 */
	private void refresh(ActionMap actionMap) {
		DefaultListModel<String> model = (DefaultListModel<String>)itemList.getModel();
		model.removeAllElements();
		accels = new Hashtable<KeyStroke, Action>();
		/*for( JMenuItem item : items.values() ){
			model.addElement(item.getText() + ": " + createAccelString(item.getAccelerator()));
			accels.put(item.getAccelerator(), item);
		}*/
		for( Object key : actionMap.allKeys() ){
			Action action = actionMap.get(key);
			model.addElement((String)key + ": " + 
					createAccelString( (KeyStroke)action.getValue(Action.ACCELERATOR_KEY) ) );
			Object val;
			if( (val = action.getValue(Action.ACCELERATOR_KEY)) != null )
				accels.put( (KeyStroke)val, action );
		}
	}
	
	private void writePreferencesFile() {
		PrintWriter writer;
		try {
			writer = new PrintWriter(new File(DataManager.START_DIRECTORY, ".preferences"));
			for( KeyStroke k : accels.keySet() ){
				writer.write(accels.get(k).getValue(Action.NAME) + "::" + k.toString() + "\n");
			}
			writer.close();
		} catch (FileNotFoundException e) {
			System.err.println("file not found for preference file update");
		}
	}
	
	private String getItemFromSelection() {
		return itemList.getSelectedValue().split(": ")[0];
	}

	
	public static String createAccelString(KeyStroke k) {
		if( k == null ) return "";
		String text = KeyEvent.getKeyModifiersText(k.getModifiers()) + "+" + KeyEvent.getKeyText(k.getKeyCode());
		return text.replace("+","");
	}
	
}

