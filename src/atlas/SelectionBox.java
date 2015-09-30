package atlas;

import java.awt.BasicStroke;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.util.ArrayList;
import java.util.HashSet;

@SuppressWarnings("serial")
public class SelectionBox extends CRectangle {
	private final ArrayList<CRectangle> boxes;
	private HashSet<CRectangle> selection;
	/** Keep track of the index in selection to make removal faster */
	private HashSet<CRectangle> curFound = new HashSet<CRectangle>();
	private final float[] dash = { 10 };
	private final Stroke dashed = new BasicStroke(1, 
			BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10, dash, 0);
	private Rectangle representation = new Rectangle();

	public SelectionBox(int x, int y, 
			ArrayList<CRectangle> unselectedBoxes, HashSet<CRectangle> selection_) {
		super(x, y, 0, 0, -1); //id shouldn't matter for this
		boxes = unselectedBoxes;
		selection = selection_;
	}
	
	/**
	 * Changes the width by adding the given int to the current width
	 * @param widthChange amount to be added to current width
	 */
	public void changeWidth(int widthChange) {
		width += widthChange;
		checkContains();
	}
	
	/**
	 * Changes the width by adding the given int to the current width
	 * @param heightChange amount to be added to current width
	 */
	public void changeHeight(int heightChange) {
		height += heightChange;
		checkContains();
	}
	
	/**
	 * go through boxes to see if the state of any need to be changed
	 */
	private void checkContains() {
		/* For each box we should keep an eye on:
		 *   if we currently contain the box:
		 *     if we didn't already contain it:
		 *       change its status to EDIT_SELECTED
		 *       add it to our curFound
		 *       add it to the editorWindow's selection
		 *   else if we don't currently contain the box
		 *     if we did contain it before:
		 *       change its status to NOT_SELECTED
		 *       remove it from curFound
		 *       remove it from selection
		 */
		for( CRectangle box : boxes ){
			if( this.contains(box.normalize()) ){
				if( !curFound.contains(box) ){
					box.selected = CRectangle.EDIT_SELECTED;
					curFound.add(box); 
					selection.add(box);
				}
			} else { //we don't contain the box
				if( curFound.contains(box) ){
					box.selected = CRectangle.NOT_SELECTED;
					curFound.remove(box);
					selection.remove(box);
				}
			}
		}
	}

	@Override
	public void paint(Graphics g) {
		Graphics2D g2 = (Graphics2D)g;
		g2.setStroke(dashed);
		representation.x = getDrawX();
		representation.y = getDrawY();
		representation.width = getDrawWidth();
		representation.height = getDrawHeight();
		g2.draw(representation);
	}
}
