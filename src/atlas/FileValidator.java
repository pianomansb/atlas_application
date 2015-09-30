package atlas;

import java.io.File;
import java.nio.file.Path;
import java.util.HashSet;

import javax.swing.Action;
import javax.swing.GroupLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;

@SuppressWarnings("serial")
public class FileValidator extends JComponent {
	private final JLabel warning = new JLabel("");
	private HashSet<String> files;
	private enum WarningType { BLANK, FILE_EXISTS, ILLEGAL_CHAR, NONE }; 
	private Action continueAction;
	final JTextField textField = new JTextField();
	
	public FileValidator(Path curDirectory) {
		files = getFiles(curDirectory);
		
		final JLabel title = new JLabel("<html><div>"
				+ "Enter export folder name <br>"
				+ "allowed characters: A-Z,a-z,0-9,_,-"
				+ "</div></html>");
		
		GroupLayout layout = new GroupLayout(this);
		setLayout(layout);
		layout.setHorizontalGroup(layout.createParallelGroup()
			.addComponent(title)
			.addComponent(textField)
			.addComponent(warning)
		);
		layout.setVerticalGroup(layout.createSequentialGroup()
			.addComponent(title)
			.addComponent(textField)
			.addComponent(warning)
		);
		
		textField.getDocument().addDocumentListener(new DocumentListener() {
			@Override
			public void removeUpdate(DocumentEvent e) {
				validate(e);
			}
			
			@Override
			public void insertUpdate(DocumentEvent e) {
				validate(e);
			}
			
			@Override
			public void changedUpdate(DocumentEvent e) {
				validate(e);
			}
		});
		
	}
	
	public String getDestinationName() {
		return textField.getText();
	}
	
	public void setAction(Action a) {
		continueAction = a;
	}
	
	private void validate(DocumentEvent e) {
		Document doc = e.getDocument();
		String text = null;
		try {
			text = doc.getText(0, doc.getLength());
		} catch (BadLocationException e1) {
			e1.printStackTrace();
		}
		if( text == null || text.isEmpty() ){
			continueAction.setEnabled(false);
			setWarning(WarningType.BLANK);
			return;
		} else if( files.contains(text) ){
			continueAction.setEnabled(false);
			setWarning(WarningType.FILE_EXISTS);
			return;
		} else if( !text.matches("[\\w-]+") ){
			continueAction.setEnabled(false);
			setWarning(WarningType.ILLEGAL_CHAR);
			return;
		}
		continueAction.setEnabled(true);
		setWarning(WarningType.NONE);
	}
	
	private void setWarning(WarningType type) {
		switch(type) {
		case BLANK:
			warning.setText("You must enter a filename.");
			break;
		case FILE_EXISTS:
			warning.setText("That file already exists.");
			break;
		case ILLEGAL_CHAR:
			warning.setText("That character is not allowed.");
			break;
		case NONE:
			warning.setText("");
			break;
		}
	}
	
	private HashSet<String> getFiles(Path dir_) {
		File dir = dir_.toFile();
		if( !dir.isDirectory() )
			dir = new File(dir.getParent());
		File[] files = dir.listFiles();
		HashSet<String> ret = new HashSet<String>();
		for( File file : files )
			ret.add(file.getName());
		return ret;
	}
	
}
