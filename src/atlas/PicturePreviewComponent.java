package atlas;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.RenderingHints;
import java.io.File;

import javax.swing.JComponent;

@SuppressWarnings("serial")
public class PicturePreviewComponent extends JComponent {
	private Image image = null;
	private String message = "No data";

	public PicturePreviewComponent() {
		setPreferredSize(new Dimension(200,200));
		setLayout(new GridBagLayout());
	}
	
	public void setFile(File file) {
		message = "Loading...";
		image = null;
		if( file != null ){
			image = getToolkit().getImage(file.toString());
		}
		repaint();
	}
	
	@Override
	public boolean imageUpdate(Image img, int flags, int x, int y, int width, int height) {
		if( (flags & ALLBITS) != 0 ){
			message = null;
			repaint();
			return false;
		}
		if( (flags & ABORT) != 0 ){
			image = null;
			message = "No data";
			repaint();
			return false;
		}
		return true;
	};

	@Override
	public void paint(Graphics g) {
		super.paint(g);
		Graphics2D g2 = (Graphics2D)g;
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		if( message != null )
			g.drawString(message, 5, 25);
		try {
			//scale without messing ratio
			int width = image.getWidth(this);
			int height = image.getHeight(this);
			if( width != -1 && height != -1 ){
				double scaleFactor = 1;
				if( width > height && width > getWidth() )
					scaleFactor = (double)getWidth() / width;
				else if( height > width && height > getHeight() )
					scaleFactor = (double)getHeight() / height;
				width *= scaleFactor;
				height *= scaleFactor;
				g.drawImage(image, 0, 0, width, height, this);
			}
		} catch( NullPointerException e ){
			g.drawImage(image, 0, 0, getWidth(), getHeight(), this);
		}
	}
}
