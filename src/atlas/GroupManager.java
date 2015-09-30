package atlas;

import java.awt.BasicStroke;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

@SuppressWarnings("serial")
public class GroupManager extends HashMap<CRectangle, Integer> {
	private HashMap<Integer, ArrayList<CRectangle> > reverse = 
			new HashMap<Integer, ArrayList<CRectangle> >();
	private final float[] dash = { 10 };
	private final Stroke dashed = new BasicStroke(1, 
			BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10, dash, 0);

	
	public GroupManager() {	}
	
	/** don't allow null keys */
	@Override
	public Integer put(CRectangle key, Integer value) throws IllegalArgumentException {
		//keep track of reverse mapping as well for printing
		if( key == null ) throw new IllegalArgumentException(
				"Trying to pass null to GroupManager, which doesn't accept null keys" );
		if( value == null ) throw new IllegalArgumentException(
				"Trying to pass null to GroupManager, which doesn't accept null values" );
		if( reverse.containsKey(value) )
			reverse.get(value).add(key);
		else
			reverse.put(value, new ArrayList<CRectangle>(Arrays.asList(key)) );
		
		return super.put(key, value);
	}
	
	public Integer remove(CRectangle key) {
		Integer ret = super.remove(key);
		if( ret == null ) return null;
		reverse.get(ret).remove(key);
		if( reverse.get(ret).isEmpty() )
			reverse.remove(ret);
		return ret;
	}
	
	/**
	 * selects all boxes in the same group as the given box with the given selection type
	 * @param selectionType one of the CRectangle selection types
	 */
	public void selectBoxesInGroup(CRectangle inputbox, int selectionType) {
		ArrayList<CRectangle> boxes = reverse.get( this.get(inputbox) );
		if( boxes == null ) return;
		for( CRectangle box : boxes )
			box.selected = selectionType;
	}
	
	/** returns null if the box is not in a group */
	public Integer getGroupForBox(CRectangle box) {
		return get(box);
	}

	/**
	 * See notes for Card.dropAnswerAt(String,Point,boolean,boolean)
	 */
	public String dropAnswerOnGroup(String userAnswer, CRectangle box, boolean allowWrongAnswers) {
		if( !allowWrongAnswers ) return dropAnswerOnGroupStudyMode(userAnswer, box);
		
		String curAnswer = box.getDisplayedText();
		if( curAnswer == null ){
			dropTextOnBox(userAnswer, box);
			return null;
		}
		
		//curAnswer is filled - try to find an empty box in the group
		ArrayList<CRectangle> group = reverse.get( getGroupForBox(box) );
		for( CRectangle peer : group ){
			if( box.equals(peer) ) continue;
			if( peer.getDisplayedText() == null ){
				dropTextOnBox(userAnswer, peer);
				return null;
			}
		}
		
		//every box was filled - replace answer in given box
		dropTextOnBox(userAnswer, box);
		return curAnswer;
	}
	
	private void dropTextOnBox(String answer, CRectangle box) {
		if( box.answer.equals(answer) ){
			box.showAnswer = true;
			box.falseAnswer = null;
		} else {
			box.showAnswer = false;
			box.falseAnswer = answer;
		}
	}
	
	/**
	 * For use in study mode. The supplied answer will stick to the box in the its group
	 * for which it is correct. If it is not correct for any box in the group,
	 * it is returned.
	 * @param userAnswer
	 * @param box
	 * @return null if the supplied answer stuck to a box; the supplied answer if it does
	 * not match any box in the group 
	 */
	private String dropAnswerOnGroupStudyMode(String userAnswer, CRectangle box) {
		for( CRectangle peer : reverse.get(getGroupForBox(box)) ){
			if( peer.answer.equals(userAnswer) ){
				dropTextOnBox(userAnswer, peer);
				return null;
			}
		}
		return userAnswer;
	}
	
	public void prepareForResults() {
		for( ArrayList<CRectangle> boxes : reverse.values() ){
			HashSet<String> givenAnswers = new HashSet<String>();
			for( CRectangle box : boxes ){
				givenAnswers.add(getGivenAnswer(box));
			}
			ArrayList<CRectangle> wrongBoxes = new ArrayList<CRectangle>();
			for( CRectangle box : boxes ){
				if( givenAnswers.contains(box.answer) ){
					box.showAnswer = true;
					box.falseAnswer = null;
					givenAnswers.remove(box.answer);
				} else {
					wrongBoxes.add(box);
				}
			}

			ArrayList<String> remainingGivenAnswers = new ArrayList<String>();
			for( String s : givenAnswers ) 
				remainingGivenAnswers.add(s);
			//add empty string answers for blanks that got sucked up by the hashSet representation
			int numBlanks = wrongBoxes.size() - givenAnswers.size();
			for( int i=0; i<numBlanks; ++i )
				remainingGivenAnswers.add("");
			int i=0;
			for( CRectangle box : wrongBoxes ){
				box.showAnswer = false;
				box.falseAnswer = remainingGivenAnswers.get(i);
			}
		}
	}
	
	private String getGivenAnswer(CRectangle box) {
		String ret = "";
		if( box.falseAnswer != null ) ret = box.falseAnswer;
		else if( box.showAnswer ) ret = box.answer;
		return ret;
	}
	
	public void paint(Graphics g) {
		Graphics2D g2 = (Graphics2D)g;
		Stroke defaultstroke = g2.getStroke();
		g2.setStroke(dashed);
		g2.setColor(ResultsWindow.textGreen);
		for( ArrayList<CRectangle> group : reverse.values() ){
			int smallx = Integer.MAX_VALUE, bigx = Integer.MIN_VALUE, 
					smally = Integer.MAX_VALUE, bigy = Integer.MIN_VALUE;
			for( CRectangle box : group ){
				if( box.x < smallx ) smallx = box.x;
				if( box.y < smally ) smally = box.y;
				if( box.x + box.width > bigx ) bigx = box.x + box.width;
				if( box.y + box.height > bigy ) bigy = box.y + box.height;
			}
			int buffer = 5;
			g.drawRect(smallx - buffer, smally - buffer, bigx - smallx + 2*buffer, bigy - smally + 2*buffer);
		}
		g2.setStroke(defaultstroke);
	}

	
}
