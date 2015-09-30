package atlas;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;

@SuppressWarnings("serial")
public class CRectangle extends Rectangle {
	public String answer;
	public static final int NOT_SELECTED = 0;
	public static final int STUDY_SELECTED = 1;
	public static final int EDIT_SELECTED = 2;
	public static final int RESULT_SELECTED = 3;
	public static final int RESULT_SELECTED_DISPLAY_CORRECT = 4;
	public static final int STUDY_FOCUS_SELECTED = 5;
	public int selected = NOT_SELECTED;
	public boolean showAnswer = true;
	public String falseAnswer = null;
	public final Integer id;
	public boolean locked = false;
	
	public CRectangle(int id_) { id = id_; }
	
	public CRectangle(int x, int y, int w, int h, int id_) {
		super(x, y, w, h);
		id = id_;
	}
	
	public void displayFalseAnswer(String falseAnswer_) {
		falseAnswer = falseAnswer_;
	}
	
	public void hideText() {
		showAnswer = false;
		falseAnswer = null;
	}

	/**
	 * Returns the box's currently displayed text
	 * @return the currently displayed text, or null if blank
	 */
	public String getDisplayedText() {
		if( showAnswer ) return answer;
		if( falseAnswer != null ) return falseAnswer;
		return null;
	}
	
	/**
	 * give the rectangle positive width and height and an x,y that is actually the upper left
	 */
	public CRectangle normalize() {
		x = getDrawX();
		y = getDrawY();
		width = getDrawWidth();
		height = getDrawHeight();
		return this;
	}
	
	public boolean nearLowerRightCorner(Point p) {
		int dist = 5;
		Point lowerRight = new Point();
		lowerRight.x = getDrawX() + getDrawWidth();
		lowerRight.y = getDrawY() + getDrawHeight();
		if( p.x > lowerRight.x-dist && p.x < lowerRight.x+dist &&
				p.y > lowerRight.y-dist && p.y < lowerRight.y+dist ){
			return true;
		}
		return false;
	}
	
	@Override
	public boolean contains(Point p) {
		return toRectangle().contains(p);
	}
	
	@Override
	public boolean contains(Rectangle r) {
		return toRectangle().contains(r);
	}
	
	/**
	 * upper left x coord recalculated to take negative width into account
	 */
	public int getDrawX() {
		if( width >= 0 ) return x;
		return x + width;
	}
	
	/**
	 * upper left y coord recalculated to take negative height into account
	 */
	public int getDrawY() {
		if( height >= 0 ) return y;
		return y + height;
	}
	
	/**
	 * absolute value of width
	 */
	public int getDrawWidth() {
		return Math.abs(width);
	}
	
	/**
	 * absolute value of height
	 */
	public int getDrawHeight() {
		return Math.abs(height);
	}
		
	public Rectangle toRectangle() {
		return new Rectangle(getDrawX(), getDrawY(), getDrawWidth(), getDrawHeight());
	}

	@Override
	public String toString() {
		return "CRectangle [answer=" + answer + ", selected=" + selected
				+ ", showAnswer=" + showAnswer + ", falseAnswer=" + falseAnswer
				+ "]";
	}
	
	@Override
	public int hashCode() {
		return System.identityHashCode(id);
	}

	/**
	 * only compares the id values
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!(obj instanceof CRectangle))
			return false;
		CRectangle other = (CRectangle) obj;
		if( !other.id.equals(id) )
			return false;
		return true;
	}

	/**
	 * If true, shows correct answer and green background whether or not supplied answer is correct.
	 * This is intended for use with mouse rollovers to temporarily display the correct answer
	 */
	public void setDisplayCorrectAnswerForResults(boolean displayCorrectAnswer) {
		if( displayCorrectAnswer )
			selected = RESULT_SELECTED_DISPLAY_CORRECT;
		else
			selected = RESULT_SELECTED;
	}
	
	public void paint(Graphics g) {
		if( locked ){
			g.setColor(Color.darkGray);
			if( selected == EDIT_SELECTED )
				g.drawRect(getDrawX(), getDrawY(), getDrawWidth(), getDrawHeight());
			else
				g.fillRect(getDrawX(), getDrawY(), getDrawWidth(), getDrawHeight());
		} else {
			switch( selected ){
			case EDIT_SELECTED:
				g.setColor(Color.red);
				g.drawRect(getDrawX(), getDrawY(), getDrawWidth(), getDrawHeight());
				break;
			case STUDY_FOCUS_SELECTED:
				g.setColor(Color.red);
				int x = getDrawX();
				int midy = getDrawY()+(getDrawHeight()/2);
				//draw thick horizontal line
				g.drawLine(x-15, midy, x, midy);
				//g.drawLine(x-15, midy+1, x, midy+1);
				g.drawLine(x-15, midy-1, x, midy-1);
				//draw upper diagonal line
				g.drawLine(x-5, midy-6, x, midy-1);
				g.drawLine(x-6, midy-6, x-1, midy-1);
				//draw lower diagonal line
				g.drawLine(x-5, midy+5, x, midy);
				g.drawLine(x-6, midy+5, x-1, midy);
			case STUDY_SELECTED:
				g.setColor(Color.red);
				g.fillRect(getDrawX(), getDrawY(), getDrawWidth(), getDrawHeight());
				break;
			case RESULT_SELECTED:
				g.setColor( showAnswer ? Color.green : Color.red);
				g.fillRect(getDrawX(), getDrawY(), getDrawWidth(), getDrawHeight());
				break;
			case RESULT_SELECTED_DISPLAY_CORRECT:
				g.setColor(Color.green);
				g.fillRect(getDrawX(), getDrawY(), getDrawWidth(), getDrawHeight());
				break;
			case NOT_SELECTED:
				g.setColor(Color.lightGray);
				g.fillRect(getDrawX(), getDrawY(), getDrawWidth(), getDrawHeight());
				break;
			}
		}
		g.setColor(Color.black);
		
		if( showAnswer || falseAnswer != null || locked || 
				selected == RESULT_SELECTED || 
				selected == RESULT_SELECTED_DISPLAY_CORRECT ){
			
			g.setFont(new Font("HelveticaNeue", Font.PLAIN, 14));
			Graphics2D g2 = (Graphics2D)g;
			g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, 
					RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
			FontMetrics fm = g.getFontMetrics();
			
			String ans = showAnswer ? answer : falseAnswer;
			if( selected == RESULT_SELECTED_DISPLAY_CORRECT || locked ) 
				ans = answer;
			if( ans == null ) ans = ""; //no answer given
			String[] lines = ans.split("\n");
			
			int y = getDrawY();
			int height = fm.getHeight();
			for( String line : lines ){
				Rectangle2D bg = fm.getStringBounds(line, g);
				//text background
				g.setColor(Color.white); 
				g.fillRect(getDrawX()+1, y+1, (int)bg.getWidth(), (int)bg.getHeight());
				//text box
				if( locked ) 
					g.setColor(Color.black);
				else if( selected == STUDY_SELECTED || 
						selected == STUDY_FOCUS_SELECTED || 
						selected == EDIT_SELECTED )
					g.setColor(Color.red);
				else if( selected == RESULT_SELECTED_DISPLAY_CORRECT )
					g.setColor(Color.green);
				else
					g.setColor(Color.lightGray);
				g.drawRect(getDrawX(), y, (int)bg.getWidth()+1, (int)bg.getHeight()+1);
				//text
				g.setColor(Color.black); 
				g.drawString( line, getDrawX()+1, y+1 + fm.getMaxAscent());
				y += height;
			}
		}
	}
	
}
