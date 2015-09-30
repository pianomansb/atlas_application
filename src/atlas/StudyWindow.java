package atlas;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.TreeMap;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

@SuppressWarnings("serial")
public class StudyWindow extends JPanel implements MouseListener, MouseMotionListener {
	public final DataManager dataManager;
	public Card card;
	private JScrollPane scrollPane;
	private JPanel answerBox;
	private String selectedAnswer = null;
	private MouseEvent mousePressed = null;
	private Point selectedAnswerPoint = new Point();
	private ArrayList<JLabel> answerBoxAnswers = new ArrayList<JLabel>();
	private boolean allowWrongAnswers;
	private CRectangle selection = null;
	private NavigationMask navMask;
	
	public StudyWindow(DataManager dm, Card card_, boolean allowWrongAnswers_){
		dataManager = dm;
		card = card_;
		card.clearMouseListeners();
		navMask = new NavigationMask(card.boxes);
		allowWrongAnswers = allowWrongAnswers_;
		dataManager.setTitle( (allowWrongAnswers ? "Testing" : "Studying") + " card " + card.name);
		setLayout(new GridBagLayout());
		scrollPane = new JScrollPane(card);
		card.addMouseListener(this);
		card.addMouseMotionListener(this);
		GridBagConstraints c = new GridBagConstraints();
				
		//add card
		c.gridx = 0;
		c.gridy = 0;
		c.fill = GridBagConstraints.BOTH;
		c.anchor = GridBagConstraints.FIRST_LINE_START;
		c.weightx = .5;
		c.weighty = .5;
		add(scrollPane, c);
		
		answerBox = createAnswerBox();
		c.gridx = 1;
		c.gridy = 0;
		c.weightx = .01;
		c.weighty = .25;
		add(answerBox, c);
		
		initializeCard();
	}
	
	private void initializeCard() {
		for( CRectangle box : card.boxes ){
			box.selected = CRectangle.NOT_SELECTED;
			box.hideText();
		}
	}
	
	private JPanel createAnswerBox() {
		JPanel ret = new JPanel();
		ret.setBorder(BorderFactory.createLineBorder(Color.black));
		ret.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		int rowcounter = 0;
		
		ArrayList<CRectangle> answers = new ArrayList<CRectangle>(card.boxes);
		Collections.shuffle(answers);
		TreeMap<KeyStroke, JLabel> map = initializeKeyboardControl(buildKeyboardTreeMap(answers));
		for( KeyStroke key : map.keySet() ){
			JLabel label = map.get(key);
			answerBoxAnswers.add(label);
			label.addMouseListener(this);
			label.addMouseMotionListener(this);
			c.gridx = 0;
			c.gridy = rowcounter++;
			c.anchor = GridBagConstraints.PAGE_START;
			ret.add(label, c);
		}
		return ret;
	}
	
	private TreeMap<KeyStroke,JLabel> buildKeyboardTreeMap(ArrayList<CRectangle> answers) {
		TreeMap<KeyStroke,JLabel> ret = new TreeMap<KeyStroke, JLabel>(new Comparator<KeyStroke>() {
			@Override
			public int compare(KeyStroke o1, KeyStroke o2) {
				return Integer.compare(o1.getKeyCode(), o2.getKeyCode());
			}
		});
		char c = 'a';
		int max = (answers.size() > 36) ? 36 : answers.size(); //36 possible simple keys, a-z && 0-9
		for( int i=0; i < max; ++i ){
			if( answers.get(i).locked ) continue; 
			
			ret.put( KeyStroke.getKeyStroke("pressed " + Character.toUpperCase(c)), 
					new JLabel(c + ") " + answers.get(i).answer) );
			if( c == 'z' ) c = '0';
			else ++c;
		}
		//TODO deal with more answers than 36
		return ret;
	}
	
	/** 
	 * @return the same map that was passed with no changes  
	 */
	//backspace keystroke will remove an answer from a box
	private TreeMap<KeyStroke, JLabel> initializeKeyboardControl(TreeMap<KeyStroke,JLabel> map) {
		ArrayList<KeyStroke> requiredKeys = new ArrayList<KeyStroke>(map.keySet());
		KeyStroke backspace = KeyStroke.getKeyStroke("pressed BACK_SPACE"); 
		KeyStroke left = KeyStroke.getKeyStroke("pressed LEFT");
		KeyStroke right = KeyStroke.getKeyStroke("pressed RIGHT");
		KeyStroke up = KeyStroke.getKeyStroke("pressed UP");
		KeyStroke down = KeyStroke.getKeyStroke("pressed DOWN");
		requiredKeys.add(backspace);
		requiredKeys.add(left);
		requiredKeys.add(right);
		requiredKeys.add(up);
		requiredKeys.add(down);
		dataManager.setAccelsEnabled(requiredKeys, false);
		InputMap inputMap = getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
		ActionMap actionMap = getActionMap();
		inputMap.put(backspace, backspace.toString());
		actionMap.put(backspace.toString(), new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if( selection != null ){
					handleReturnedAnswer(selection.getDisplayedText());
					selection.hideText();
				}
			}
		});
		inputMap.put(up, up.toString());
		actionMap.put(up.toString(), new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				CRectangle toBox = navMask.getUpOf(selection);
				if( toBox != null )
					selectBox(toBox);
				checkSelectionVisible();
			}
		});
		inputMap.put(down, down.toString());
		actionMap.put(down.toString(), new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				CRectangle toBox = navMask.getDownOf(selection);
				if( toBox != null )
					selectBox(toBox);
				checkSelectionVisible();
			}
		});
		inputMap.put(right, right.toString());
		actionMap.put(right.toString(), new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				CRectangle toBox = navMask.getRightOf(selection);
				if( toBox != null )
					selectBox(toBox);
				checkSelectionVisible();
			}
		});
		inputMap.put(left, left.toString());
		actionMap.put(left.toString(), new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				CRectangle toBox = navMask.getLeftOf(selection);
				if( toBox != null )
					selectBox(toBox);
				checkSelectionVisible();
			}
		});
		for( KeyStroke key : map.keySet() ){
			getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(key, key.toString());
			getActionMap().put(key.toString(), new AbstractAction() {
				@Override
				public void actionPerformed(ActionEvent e) {
					if( selection != null ){
						JLabel label = map.get(key);
						if( !label.isVisible() ) return;
						label.setVisible(false);
						handleReturnedAnswer( 
								card.dropAnswerAt(label.getText().substring(3), 
								selection, allowWrongAnswers, false) );
					}
				}
			});
		}
		return map;
	}
	
	private void handleReturnedAnswer(String returnedAnswer) {
		if( returnedAnswer != null ){
			//find a  JLabel with returned text that isn't visible and set it visible
			//TODO this could easily be faster by changing the structure of answerBoxAnswers
			for( JLabel label : answerBoxAnswers ){
				if( label.getText().substring(3).equals(returnedAnswer) && 
						label.isVisible() == false )
					label.setVisible(true);
			}
		}
		selectedAnswer = null;
		repaint();
	}
	
	private void checkSelectionVisible () {
		Rectangle visible = scrollPane.getVisibleRect();
		int horval = scrollPane.getHorizontalScrollBar().getValue();
		int vertval = scrollPane.getVerticalScrollBar().getValue();
		visible.x += horval;
		visible.y += vertval;
		if( visible.contains(selection) ) return;
		
		//try to center box in view window
		int selmidx = selection.getDrawX() + selection.getDrawWidth()/2;
		int selmidy = selection.getDrawY() + selection.getDrawHeight()/2;
		int vismidx = visible.x + visible.width/2;
		int vismidy = visible.y + visible.height/2;

		scrollPane.getHorizontalScrollBar().setValue(horval + (selmidx - vismidx) );
		scrollPane.getVerticalScrollBar().setValue(vertval + (selmidy - vismidy) );
		
	}
	
	/**
	 * Sets the selection to the given CRectangle. If the parameter
	 * is null, the selected box is deselected if it exists.
	 * @param box the CRectangle to be selected, or null to deselect
	 */
	private void selectBox(CRectangle box) {
		GroupManager groups = card.groups;
		
		if( box == null || box.locked ){
			if( selection != null ){
				selection.selected = CRectangle.NOT_SELECTED;
				groups.selectBoxesInGroup(selection, CRectangle.NOT_SELECTED);
				selection = null;
			}
			repaint();
			return;
		}
		
		//if the previous selection is the same as this selection 
		//  or is not in the same group as this selection
		if( selection != null ){
			if( groups.getGroupForBox(selection) != null && 
					!( groups.getGroupForBox(selection).equals(groups.getGroupForBox(box)) )){
				groups.selectBoxesInGroup(selection, CRectangle.NOT_SELECTED);
			} else if( !selection.equals(box) )
				selection.selected = CRectangle.NOT_SELECTED;
		}
		
		selection = box;
		if( groups.getGroupForBox(selection) != null ){
			groups.selectBoxesInGroup(selection, CRectangle.STUDY_SELECTED);
			selection.selected = CRectangle.STUDY_FOCUS_SELECTED;
		} else {
			selection.selected = CRectangle.STUDY_SELECTED;
		}
		repaint();
	}

	@Override
	public void mouseDragged(MouseEvent e) {
		if( mousePressed != null ){
			Component sourceComp = mousePressed.getComponent();
			if( sourceComp instanceof JLabel ){
				JLabel source = (JLabel)sourceComp;
				selectedAnswer = source.getText().substring(3); //get rid of "a) " preceding each answer
				source.setVisible(false);
			} else { //click was somewhere other than over a JLabel
				CRectangle box = card.getBoxAtPoint(mousePressed.getPoint());
				//click was over a box and box is displaying text: that text should be movable
				if( box != null ){
					String displayedText = box.getDisplayedText();
					if( displayedText != null ){
						selectedAnswer = displayedText;
						box.hideText(); 
					}
				}
			}
			mousePressed = null;
		}
		if( selectedAnswer != null ){
			Point mouseLoc = e.getLocationOnScreen();
			Point thisLoc = this.getLocationOnScreen();
			Point cardLoc = card.getLocationOnScreen();
			selectedAnswerPoint.x = mouseLoc.x - thisLoc.x;
			selectedAnswerPoint.y = mouseLoc.y - thisLoc.y;
			Point cardPoint = new Point(
					mouseLoc.x - cardLoc.x, 
					mouseLoc.y - cardLoc.y );
			card.mouseAtPoint(cardPoint, CRectangle.STUDY_SELECTED);
		}
		repaint();
	}
	
	@Override
	public void mousePressed(MouseEvent e) {
		mousePressed = e;
		/* since all of the pressed functionality is initializing drag, it makes sense to
		 * make sure a drag is happening before executing it. So, if press precedes a drag, 
		 * mouseDragged can handle it, and if it precedes a click, mouseReleased or Clicked
		 * can nullify mousePressed
		 */
	}
	
	@Override
	public void mouseReleased(MouseEvent e) {
		if( mousePressed != null ){
			//mouse was pressed but not followed by a drag
			mousePressed = null;
			return;
		}
		if( selectedAnswer == null ) return;
		Point mouseLoc = e.getLocationOnScreen();
		Point cardLoc = card.getLocationOnScreen();
		Point cardPoint = new Point(mouseLoc.x - cardLoc.x, mouseLoc.y - cardLoc.y);
		handleReturnedAnswer( card.dropAnswerAt(selectedAnswer, cardPoint, allowWrongAnswers, true) );
	}

	@Override
	public void mouseMoved(MouseEvent e) { }

	@Override
	public void mouseClicked(MouseEvent e) { 
		if( SwingUtilities.isLeftMouseButton(e) ){
			selectBox(card.getBoxAtPoint(e.getPoint()));
		}
	}

	@Override
	public void mouseEntered(MouseEvent e) { }

	@Override
	public void mouseExited(MouseEvent e) { }
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((card == null) ? 0 : card.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof StudyWindow))
			return false;
		StudyWindow other = (StudyWindow) obj;
		if (card == null) {
			if (other.card != null)
				return false;
		} else if (!card.equals(other.card))
			return false;
		return true;
	}
	
	@Override
	public void paint(Graphics g) {
		super.paint(g);
		
		if( selectedAnswer != null && selectedAnswerPoint != null )
			g.drawString(selectedAnswer, selectedAnswerPoint.x, selectedAnswerPoint.y);
	}

}
