package atlas;

import java.awt.Dimension;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

public class Atlas {

	private static void createAndShowGUI() {			
		JFrame frame = new JFrame();
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		DataManager dm = new DataManager(frame);
		dm.loadNewWindow(new BrowserWindow(dm));
		frame.setPreferredSize(new Dimension(1200,700));
		frame.addComponentListener(new ComponentAdapter() {
			@Override //keep window the same size when resized
			public void componentResized(ComponentEvent e) {
				frame.setPreferredSize(frame.getSize());
			}
		});
		frame.pack();
		frame.setVisible(true);
	}
	
	public static void main(String[] args) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				createAndShowGUI();
			}
		});
	}
}
