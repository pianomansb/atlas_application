package atlas;

import java.awt.Point;
import java.util.ArrayList;
import java.util.HashMap;

public class NavigationMask {
	private HashMap<CRectangle,CRectangle> up = new HashMap<CRectangle, CRectangle>();
	private HashMap<CRectangle,CRectangle> down = new HashMap<CRectangle, CRectangle>();
	private HashMap<CRectangle,CRectangle> left = new HashMap<CRectangle, CRectangle>();
	private HashMap<CRectangle,CRectangle> right = new HashMap<CRectangle, CRectangle>();
	private final int PENALTY_MULTIPLIER = 2;

	public NavigationMask(ArrayList<CRectangle> boxes_) {
		//clear locked boxes
		ArrayList<CRectangle> boxes = new ArrayList<CRectangle>();
		for( CRectangle box : boxes_ )
			if( !box.locked ) boxes.add(box);
		
		double smalldist = Double.MAX_VALUE;
		double bigdist = Double.MIN_VALUE;
		Point origin = new Point(0,0);
		CRectangle upleft = null;
		CRectangle downright = null;
		HashMap<CRectangle, Point> centerPoints = new HashMap<CRectangle, Point>();
		for( CRectangle box : boxes ){
			Point midleft = new Point(box.getDrawX(), box.getDrawY() + box.getDrawHeight()/2 );
			double distance = distance(origin, midleft);
			if( distance < smalldist ){
				smalldist = distance;
				upleft = box;
			}
			if( distance > bigdist ){
				bigdist = distance;
				downright = box;
			}
			centerPoints.put(box, midleft);
		}
		
		//create up
		for( CRectangle fromBox : boxes ){
			Point fromPoint = centerPoints.get(fromBox);
			CRectangle bestToBox = null;
			int leastPenalty = Integer.MAX_VALUE;
			for( CRectangle toBox : boxes ){
				if( toBox.equals(fromBox) ) continue;
				Point toPoint = centerPoints.get(toBox);
				if( toPoint.y >= fromPoint.y ) continue; //this box is down
				int penalty = getVertPenalty(fromPoint, toPoint);
				if( penalty < leastPenalty ){
					leastPenalty = penalty;
					bestToBox = toBox;
				}
			}
			up.put(fromBox, bestToBox);
		}
		up.put(null, downright);
		
		//create down
		for( CRectangle fromBox : boxes ){
			Point fromPoint = centerPoints.get(fromBox);
			CRectangle bestToBox = null;
			int leastPenalty = Integer.MAX_VALUE;
			for( CRectangle toBox : boxes ){
				if( toBox.equals(fromBox) ) continue;
				Point toPoint = centerPoints.get(toBox);
				if( toPoint.y <= fromPoint.y ) continue; //this box is up
				int penalty = getVertPenalty(fromPoint, toPoint);
				if( penalty < leastPenalty ){
					leastPenalty = penalty;
					bestToBox = toBox;
				}
			}
			down.put(fromBox, bestToBox);
		}
		down.put(null, upleft);
		
		//create left
		for( CRectangle fromBox : boxes ){
			Point fromPoint = centerPoints.get(fromBox);
			CRectangle bestToBox = null;
			int leastPenalty = Integer.MAX_VALUE;
			for( CRectangle toBox : boxes ){
				if( toBox.equals(fromBox) ) continue;
				Point toPoint = centerPoints.get(toBox);
				if( toPoint.x >= fromPoint.x ) continue; //this box is right
				int penalty = getHorPenalty(fromPoint, toPoint);
				if( penalty < leastPenalty ){
					leastPenalty = penalty;
					bestToBox = toBox;
				}
			}
			left.put(fromBox, bestToBox);
		}
		left.put(null, downright);
		
		//create right
		for( CRectangle fromBox : boxes ){
			Point fromPoint = centerPoints.get(fromBox);
			CRectangle bestToBox = null;
			int leastPenalty = Integer.MAX_VALUE;
			for( CRectangle toBox : boxes ){
				if( toBox.equals(fromBox) ) continue;
				Point toPoint = centerPoints.get(toBox);
				if( toPoint.x <= fromPoint.x ) continue; //this box is left
				int penalty = getHorPenalty(fromPoint, toPoint);
				if( penalty < leastPenalty ){
					leastPenalty = penalty;
					bestToBox = toBox;
				}
			}
			right.put(fromBox, bestToBox);
		}
		right.put(null, upleft);
	}
	
	private double distance(Point a, Point b) {
		return Math.sqrt( Math.pow(a.x-b.x, 2) + Math.pow(a.y-b.y, 2) );
	}
	
	private int getVertPenalty(Point fromPoint, Point toPoint) {
		int ret = Math.abs(fromPoint.y - toPoint.y);
		ret += PENALTY_MULTIPLIER * (Math.abs(fromPoint.x - toPoint.x));
		return ret;
	}

	private int getHorPenalty(Point fromPoint, Point toPoint) {
		int ret = Math.abs(fromPoint.x - toPoint.x);
		ret += PENALTY_MULTIPLIER * (Math.abs(fromPoint.y - toPoint.y));
		return ret;
	}
	
	public CRectangle getLeftOf(CRectangle box) {
		return left.get(box);
	}
	
	public CRectangle getRightOf(CRectangle box) {
		return right.get(box);
	}
	
	public CRectangle getUpOf(CRectangle box) {
		return up.get(box);
	}
	
	public CRectangle getDownOf(CRectangle box) {
		return down.get(box);
	}
	
	
}
