package atlas;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashSet;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

@SuppressWarnings("serial")
public class EditorWindow extends JPanel {
	public final DataManager dataManager;
	private JLabel nameLabel;
	public Card card;
	private CardHolder cardHolder;
	public boolean unsavedChanges = false;
	private String showNewAnswerDialogReturn = null;
	
	/**
	 * Use this constructor if the window should immediately display the
	 * asterisk that means changes have been made, i.e. if the window
	 * has been passed a new, unsaved card
	 */
	public EditorWindow(DataManager dm, Card card_, boolean needToSave) {
		this(dm, card_);
		if( needToSave ) changeMade();
	}
	
	public EditorWindow(DataManager dm, Card card_) {
		dataManager = dm;
		card = card_;
		for( CRectangle box : card.boxes )
			box.selected = CRectangle.NOT_SELECTED;
		dataManager.setTitle("Editing card " + card.name);
		setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		
		nameLabel = new JLabel();
		nameLabel.setText(createNameLabelText());
		nameLabel.setFont( new Font("Sans Serif", Font.PLAIN, 18) );
		c.gridx = 0;
		c.gridy = 0;
		c.anchor = GridBagConstraints.CENTER;
		c.gridwidth = GridBagConstraints.REMAINDER;
		add(nameLabel, c);

		//card component
		cardHolder = new CardHolder(card, this);
		c.gridx = 0;
		c.gridy = 1;
		c.fill = GridBagConstraints.BOTH;
		c.gridwidth = GridBagConstraints.REMAINDER;
		c.weightx = .5;
		c.weighty = .5;
		cardHolder.setBorder(BorderFactory.createLineBorder(Color.black));
		add(cardHolder, c);
	}
	
	public void setSelecting(boolean isSelecting) {
		cardHolder.selecting = isSelecting;
	}
	
	public void changeMade() {
		if( unsavedChanges ) return; //already called - do nothing
		unsavedChanges = true;
		nameLabel.setText( nameLabel.getText() + "*");
	}
	
	private String createNameLabelText() {
		String ret = "Editing card: \"" + card.name + "\"; Labels: ";
		for( String s : card.labels )
			ret += "\"" + s + "\", ";
		return ret.substring(0, ret.length() - (card.labels.size() == 0 ? 0 : 2) ); //chop off extra ", " if it exists
	}
	
	public void save() {
		try {
			card.writeToFile();
			unsavedChanges = false;
			nameLabel.setText(createNameLabelText());
		} catch (FileNotFoundException e) {
			JOptionPane.showMessageDialog(EditorWindow.this, "problem saving file");
			e.printStackTrace();
		}
	}
	
	public void unlinkSelectedBoxes() {
		if( cardHolder.selection.size() == 0 ) return;
		for( CRectangle box : cardHolder.selection ){
			card.groups.remove(box);
		}
		changeMade();
		repaint();
	}
	
	public void groupSelectedBoxes() {
		if( cardHolder.selection.size() < 2 ) return;
		if( !verifyForGrouping() ) return;
		Integer id = cardHolder.selection.hashCode();
		for( CRectangle box : cardHolder.selection )
			card.groups.put(box, id);
		changeMade();
		repaint();
	}
	
	/** returns true if the selection is good for grouping; 
	 * false if it should not be grouped*/
	private boolean verifyForGrouping() {
		for( CRectangle box : cardHolder.selection ){
			if( card.groups.containsKey(box) ){
				JOptionPane.showMessageDialog(this, 
						"Remove existing links before proceeding.");
				return false;
			}
			if( box.locked ){
				JOptionPane.showMessageDialog(this, 
						"Locked boxes cannot be linked.");
				return false;
			}
		}
		return true;
	}
	
	public void lockOrUnlockSelectedBoxes() {
		CRectangle[] selection = cardHolder.selection.toArray(new CRectangle[0]);
		if( selection.length == 0 ) return;
		
		//make sure all boxes can legally be locked/unlocked
		boolean alreadyLocked = selection[0].locked;
		for( CRectangle box : selection ){
			if( box.locked != alreadyLocked ){
				JOptionPane.showMessageDialog(this, 
						"The locked status of all selected boxes must be the same \n"
						+ "to lock/unlock as a group.");
				return;
			} else if( card.groups.containsKey(box) ){
				JOptionPane.showMessageDialog(this, 
						"Linked boxes cannot be locked.");
				return;
			}
		}
		
		for( CRectangle box : selection ){
			box.locked = !box.locked;
			box.selected = CRectangle.NOT_SELECTED;
			cardHolder.selection.clear();
		}
		changeMade();
		repaint();
	}
	
	public void deleteSelectedBox() {
		if( !cardHolder.selection.isEmpty() ){
			int selection = JOptionPane.showConfirmDialog(
					EditorWindow.this, "Delete box" 
							+ ((cardHolder.selection.size() == 1) ? "" : "es")
							+ "? This action is final", 
							"Confirm deletion", JOptionPane.YES_NO_OPTION);
			if( selection == JOptionPane.YES_OPTION ){
				for( CRectangle box : cardHolder.selection )
					card.boxes.remove(box);
				cardHolder.selection.clear();
				changeMade();
				repaint();
			}
		}
	}

	public String showNewAnswerDialog(String startText) {
		JTextArea textArea = new JTextArea( (startText != null ) ? startText : "");
		textArea.setColumns(25);
		textArea.setRows(3);
		JScrollPane scrollPane = new JScrollPane(textArea);
		scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		Object[] message = { "New answer: ("
				+ PreferencesWindow.createAccelString(KeyStroke.getKeyStroke("meta ENTER")) 
				+ " to accept)", scrollPane };
		JButton okButton = new JButton();
		JButton cancelButton = new JButton();
		Object[] options = { okButton, cancelButton };
		JOptionPane optionPane = new JOptionPane(message, JOptionPane.PLAIN_MESSAGE, 
				JOptionPane.OK_CANCEL_OPTION, null, options);
		JDialog dialog = new JDialog(dataManager.frame, "New Answer", true);
		optionPane.addPropertyChangeListener(new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent e) {
				if( e.getNewValue().equals("OK_OPTION") ||
						e.getNewValue().equals("CANCEL_OPTION") ||
						e.getNewValue().equals(JOptionPane.CLOSED_OPTION) ){
					dialog.setVisible(false);
					if( e.getNewValue().equals("CANCEL_OPTION") ||
							e.getNewValue().equals(JOptionPane.CLOSED_OPTION) )
						showNewAnswerDialogReturn = null;
					else
						showNewAnswerDialogReturn = textArea.getText();
				}
			}
		});
		okButton.setAction(new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				optionPane.setValue("OK_OPTION");
			}
		});
		okButton.setText("OK");
		okButton.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
				KeyStroke.getKeyStroke("meta ENTER"), "ok");
		okButton.getActionMap().put("ok", okButton.getAction());
		cancelButton.setAction(new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				optionPane.setValue("CANCEL_OPTION");
			}
		});
		cancelButton.setText("Cancel");
		dialog.setContentPane(optionPane);
		dialog.pack();
		dialog.setLocationRelativeTo(null);
		dialog.setVisible(true);
		
		return showNewAnswerDialogReturn;
	}

}

@SuppressWarnings("serial")
class CardHolder extends JPanel implements MouseListener, MouseMotionListener {
	Card card;
	Point lastPoint;
	boolean creatingNewBox = false;
	HashSet<CRectangle> selection = new HashSet<CRectangle>();
	CRectangle draggingBox = null, draggingCorner = null;
	final EditorWindow editorWindow;
	boolean selecting = false;
	SelectionBox selectionBox = null;
	
	public CardHolder(Card card_, EditorWindow e) {
		editorWindow = e;
		card = card_;
		card.clearMouseListeners();
		for( CRectangle box : card.boxes ){
			box.showAnswer = true;
			box.falseAnswer = null;
		}
		setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.weightx = .5;
		c.weighty = .5;
		c.fill = GridBagConstraints.BOTH;
		JScrollPane scrollPane = new JScrollPane(card);
		add(scrollPane, c);
		
		card.addMouseListener(this);
		card.addMouseMotionListener(this);
	}

	private ArrayList<CRectangle> getUnselectedBoxes() {
		ArrayList<CRectangle> ret = new ArrayList<CRectangle>();
		for( CRectangle box : card.boxes )
			if( !selection.contains(box) ) ret.add(box);
		return ret;
	}
	
	@Override
	public void mousePressed(MouseEvent e) {
		/* If press is near the lower right of a selected box:
		 *   resize the box
		 * Else if press is in the body of a selected box:
		 *   move the box
		 * Else : press is outside any selected box
		 *   if selecting:
		 *     initialize selection box
		 *   else : not selecting
		 *     initialize a new CRectangle
		 */
		//if( e.getButton() == 1 ){
		if( SwingUtilities.isLeftMouseButton(e) ){	
			for( CRectangle box : selection ){
				if( box.nearLowerRightCorner(e.getPoint()) ){
					draggingCorner = box;
					break;
				} else if( box.contains(e.getPoint()) ){
					draggingBox = box;
					break;
				}
			}
			if( draggingBox == null && draggingCorner == null ){
				if( selecting ){
					selectionBox = new SelectionBox(e.getX(), e.getY(), 
							getUnselectedBoxes(), selection);
				} else {
					creatingNewBox = true;
				}
			}
			lastPoint = e.getPoint();
		}
	}

	@Override
	public void mouseDragged(MouseEvent e) {
		/* Dragging either:
		 *  box, box corner, new box corner, selection bounds
		 */
		//if( e.getButton() == 1 ){
		if( SwingUtilities.isLeftMouseButton(e) ){
			int xDiff = e.getX() - lastPoint.x;
			int yDiff = e.getY() - lastPoint.y;
			
			if( draggingBox != null ){
				draggingBox.x += xDiff;
				draggingBox.y += yDiff;
				editorWindow.changeMade();
			} else if( draggingCorner != null ){
				draggingCorner.width += xDiff;
				draggingCorner.height += yDiff;
				editorWindow.changeMade();
			} else if( creatingNewBox ){
				if( card.tempRect == null ){ 
					card.tempRect = card.createNewBox(lastPoint.x, lastPoint.y, 0, 0);
				}
				card.tempRect.width += xDiff;
				card.tempRect.height += yDiff;
				editorWindow.changeMade();
			} else {
				selectionBox.changeWidth(xDiff);
				selectionBox.changeHeight(yDiff);
			}
			
			lastPoint = e.getPoint();
			repaint();
		}
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		draggingBox = null;
		if( draggingCorner != null ){
			draggingCorner.normalize();
			draggingCorner = null;
		} else if( creatingNewBox && card.tempRect != null ){
			String input = editorWindow.showNewAnswerDialog(null);
			if( input != null ){
				card.tempRect.answer = input;
				card.addBox(card.tempRect);
			}
			card.tempRect = null;
			creatingNewBox = false;
		}
		selectionBox = null;
		repaint();
	}

	@Override
	public void mouseClicked(MouseEvent e) {
		if( SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 2 ){ //double click left button
			CRectangle box = card.getBoxAtPoint(e.getPoint());
			if( box == null ) return;
			String answer = editorWindow.showNewAnswerDialog(box.answer);
			if( answer == null ) return;
			box.answer = answer;
			editorWindow.changeMade();
		}
		
		//left click - deselect if not in a selected box
		else if( SwingUtilities.isLeftMouseButton(e) && !selection.isEmpty() ){
			Point mouse = e.getPoint();
			boolean mouseInBox = false;
			for( CRectangle box : selection )
				if( box.contains(mouse) ){
					mouseInBox = true;
					break;
				}
			if( !mouseInBox ){ //not in any selected box
				for( CRectangle box : selection )
					box.selected = CRectangle.NOT_SELECTED;
				selection.clear();
			}
		}
		
		else if( SwingUtilities.isRightMouseButton(e) ){
			/* if right click is on a selected box, deselect that box
			 * else if right click is on a non-selected box, select that box
			 */
			CRectangle box = card.getBoxAtPoint(e.getPoint());
			if( box == null ) return;
			boolean clickInSelectedBox = false;
			for( CRectangle selectionBox : selection ){
				if( selectionBox.equals(box) ){ 
					box.selected = CRectangle.NOT_SELECTED;
					clickInSelectedBox = true;
					selection.remove(box);
					break;
				}
			}
			if( !clickInSelectedBox ){ //right click was on a non-selected box
				box.selected = CRectangle.EDIT_SELECTED;
				selection.add(box);
			}
		}
		
		repaint();
	}

	@Override
	public void mouseEntered(MouseEvent e) { }

	@Override
	public void mouseExited(MouseEvent e) { }

	@Override
	public void mouseMoved(MouseEvent e) { }

	@Override
	public void paint(Graphics g) {
		super.paint(g);
		if( selectionBox != null ) selectionBox.paint(g);
	}
}
