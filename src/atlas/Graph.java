package atlas;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.Date;
import java.util.TreeMap;

import javax.swing.JComponent;
import javax.swing.JScrollPane;

@SuppressWarnings("serial")
public class Graph extends JComponent {
	private TreeMap<Date, Double> data;
	private double[] yvals;
	private Rectangle[] values;
	private String[] datelabels;
	private String[] percentlabels;
	private int topmargin = 10;
	private int bottommargin = 10;
	private int leftmargin = 5;
	private int rightmargin = 10;
	private int leftTextSpace = 10;
	private int topTextSpace = 10;
	private int barheight = 15;
	private int barspace = 8;
	private Font regfont;
	private Font smallfont;
	/** Whether the graph should display dd/mm or dd/mm/yy */
	private boolean useDDMMYY = false;
	private JScrollPane scrollPane = null;
	private final Dimension DEFAULT_DIMENSION = new Dimension(200, 50);
	
	public Graph(JScrollPane scrollPane_) {
		this();
		scrollPane = scrollPane_;
	}
	
	public Graph() {
		setMinimumSize(new Dimension(150,175));
		setPreferredSize(DEFAULT_DIMENSION);
		//setPreferredSize(new Dimension(200, 500));
		setMaximumSize(new Dimension(200, 500));
		
		addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(ComponentEvent e) {
				refresh();
			}
		});
	}
	
	/**
	 * Set the mode the graph uses for displaying date strings.
	 * Default is dd/mm.
	 * @param useDDMMYY_ If true, dates will be displayed as dd/mm/yy.
	 * If false, dates will be displayed as dd/mm  .
	 */
	public void setDateMode(boolean useDDMMYY_) {
		useDDMMYY = useDDMMYY_;
	}
	
	public void setData(TreeMap<Date, Double> data_) {
		data = data_;
		refresh();
	}
	
	public void refresh() {
		if( data == null ) {
			setPreferredSize(DEFAULT_DIMENSION);
			scrollPane.revalidate();
			repaint();
			return;
		}
		
		regfont = getFont();
		if( regfont == null ){
			setFont(new Font("SansSerif", Font.PLAIN, 12));
			regfont = getFont();
		}
		smallfont = new Font(regfont.getName(), regfont.getStyle(), 10);
		
		int size = data.size();
		yvals = new double[size];
		percentlabels = new String[size];
		datelabels = new String[size];
		int index = 0;
		//for( Entry<Date, Double> e : data.entrySet() ){
		for( Date date : data.descendingKeySet() ){
			yvals[index] = data.get(date)/100;
			percentlabels[index] = String.valueOf(data.get(date));
			if( percentlabels[index].matches("\\d+\\.\\d\\d") ) // e.g. 34.36, not 50.0
				percentlabels[index] = percentlabels[index]
						.substring(0, percentlabels[index].length()-1); //remove last char
			datelabels[index] = useDDMMYY ?
					getDateStringDDMMYY(date) : 
					getDateStringDDMM(date);
			++index;
		}
		
		FontMetrics metrics = getFontMetrics(smallfont);
		leftTextSpace = (int)(metrics.getStringBounds("00/00", getGraphics()).getWidth()) + 2;
		topTextSpace = (int)(metrics.getStringBounds("50%", getGraphics()).getHeight()) + 2;
		
		int yspace = getWidth() - leftmargin - rightmargin - leftTextSpace;
		int x = leftmargin + leftTextSpace + 1;
		int y = topmargin + topTextSpace + barspace;
		values = new Rectangle[yvals.length];
		for( int i=0; i < yvals.length; ++i ){
			values[i] = new Rectangle(x, y, (int)(yvals[i] * yspace), barheight);
			y += barheight + barspace;
		}
		setPreferredSize(new Dimension(getPreferredSize().width, y));
		setMaximumSize(new Dimension(getMaximumSize().width, y)); //TODO ?
		if( scrollPane != null ) scrollPane.revalidate();
		repaint();
	}
	
	private String getDateStringDDMM(Date d) {
		String[] parts = d.toString().split(" ");
		String ret = parts[2] + "/"; //dd
		switch( parts[1] ){ //mmm
		case "Jan":
			ret += "01";
			break;
		case "Feb":
			ret += "02";
			break;
		case "Mar":
			ret += "03";
			break;
		case "Apr":
			ret += "04";
			break;
		case "May":
			ret += "05";
			break;
		case "Jun":
			ret += "06";
			break;
		case "Jul":
			ret += "07";
			break;
		case "Aug":
			ret += "08";
			break;
		case "Sep":
			ret += "09";
			break;
		case "Oct":
			ret += "10";
			break;
		case "Nov":
			ret += "11";
			break;
		case "Dec":
			ret += "12";
			break;
		}
		return ret;
	}
	
	private String getDateStringDDMMYY(Date d) {
		String ret = getDateStringDDMM(d);
		String[] parts = d.toString().split(" ");
		ret += "/" + parts[5].substring(2);
		return ret;
	}
	
	@Override
	public void paint(Graphics g) {
		//refresh();
		super.paint(g);
		
		g.setFont(new Font("HelveticaNeue", Font.PLAIN, 14));
		Graphics2D g2 = (Graphics2D)g;
		g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, 
				RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		
		//draw background
		g.setColor(Color.white);
		g.fillRect(0, 0, getWidth(), getHeight());
		g.setColor(Color.black);
		
		if( data == null ){
			g.drawString("No data", 20, 20);
			return;
		}
		
		g.drawLine(leftmargin + leftTextSpace, topmargin + topTextSpace, 
				leftmargin + leftTextSpace, getHeight() - bottommargin);
		g.drawLine(leftmargin + leftTextSpace, topmargin + topTextSpace, 
				getWidth() - rightmargin, topmargin + topTextSpace);
		
		//draw top labels
		int yspace = getWidth() - leftmargin - rightmargin - leftTextSpace;
		int y = topmargin + topTextSpace - 1;
		FontMetrics metrics = getFontMetrics(regfont);
		int halfwidth = (int)(metrics.getStringBounds("50%", g).getWidth()/2);
		g.drawString("50%", yspace/2 + leftmargin + leftTextSpace - halfwidth, y);
		g.drawString("25%", yspace/2/2 + leftmargin + leftTextSpace - halfwidth, y);
		g.drawString("75%", yspace/2 + (yspace/2/2) + leftmargin + leftTextSpace - halfwidth, y);
		
		//draw bars
		int i = 0;
		for( Rectangle r : values ){
			double percent = yvals[i];
			if( percent >= .85 ) g.setColor(Color.green);
			else if( percent >= .7 ) g.setColor(Color.yellow);
			else g.setColor(Color.red);
			g.fillRect(r.x, r.y, r.width, r.height);
			
			g.setColor(Color.black);
			g.setFont(smallfont);
			g.drawString(datelabels[i], leftmargin, r.y + 10);
			g.setFont(regfont);
			if( percent >= .85 )
				g.drawString(percentlabels[i], 
						r.x + r.width - (int)(getFontMetrics(regfont).getStringBounds("100.00", g).getWidth()), 
						r.y + 12);
			else
				g.drawString(percentlabels[i], r.x + r.width + 5, r.y + 12);
			
			++i;
		}
		
	}

}
