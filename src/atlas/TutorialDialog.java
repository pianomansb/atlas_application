package atlas;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.GroupLayout;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;

@SuppressWarnings("serial")
public class TutorialDialog extends JDialog {
	private static final int SPLASH_START =0;
	public static final int BROWSER_START =1;
	private static final int BROWSER_SECOND =2;
	public static final int EDITOR_START =3;
	private static final int EDITOR_SECOND =4;
	public static final int TESTER_START =5;
	public static final int RESULTS_START =6;
	public static final int PREFERENCES_START =7;
	private int page;
	private static final Font DEFAULT_FONT = new Font("HelveticaNeue", Font.PLAIN, 14);
	
	private Image image = null;
	private String message = "Loading";
	private ImageHolder imageHolder = new ImageHolder();
	private JLabel title = new JLabel();
	private JLabel header = new JLabel();
	private JLabel tips = new JLabel(){
		@Override
		public void paintComponent(Graphics g) {
			Graphics2D g2 = (Graphics2D)g;
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, 
					RenderingHints.VALUE_ANTIALIAS_ON);
			super.paintComponent(g);
		}
	};
	
	public static void showTutorial(Frame frame) {
		new TutorialDialog(frame);
	}

	public TutorialDialog(Frame frame) {
		this(frame, BROWSER_START);
	}
	
	public TutorialDialog(Frame frame, int start_condition) {
		super(frame, "Tutorial");
				
		GroupLayout layout = new GroupLayout(this.getContentPane());
		getContentPane().setLayout(layout);
		layout.setAutoCreateContainerGaps(true);
		layout.setAutoCreateGaps(true);
		
		JButton nextButton = new JButton("Next");
		nextButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if( page < PREFERENCES_START )
					loadPage(++page);
			}
		});
		JButton backButton = new JButton("Back");
		backButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if( page > 0 )
					loadPage(--page);
			}
		});
		JButton closeButton = new JButton("Close");
		closeButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				setVisible(false);
			}
		});
		
		layout.setHorizontalGroup(layout.createSequentialGroup()
			.addGroup(layout.createParallelGroup()
				.addComponent(title)
				.addComponent(header)
				.addComponent(imageHolder)
			).addGroup(layout.createParallelGroup()
				.addGroup(layout.createSequentialGroup()
					.addComponent(closeButton)
					.addComponent(backButton)
					.addComponent(nextButton)
				).addComponent(tips)
			)
		);
		
		layout.setVerticalGroup(layout.createSequentialGroup()
			.addGroup(layout.createParallelGroup()
				.addComponent(title)
				.addComponent(closeButton)
				.addComponent(backButton)
				.addComponent(nextButton)
			).addComponent(header)
			.addGroup(layout.createParallelGroup()
				.addComponent(imageHolder)
				.addComponent(tips)
			)
		);
		
		title.setFont(DEFAULT_FONT.deriveFont((float)24));
		title.setFont(title.getFont().deriveFont(Font.BOLD));
		header.setFont(DEFAULT_FONT.deriveFont((float)16));
		tips.setFont(DEFAULT_FONT.deriveFont((float)14));
		imageHolder.setBorder(BorderFactory.createLineBorder(Color.black)); 
		
		page = start_condition;
		loadPage(page);
		
		setPreferredSize(new Dimension(1000,600));
		pack();
		setVisible(true);
	}
	
	private void loadPage(int pageID) {
		switch( pageID ){
		case SPLASH_START:
			break;
		case BROWSER_START:
			prepareBrowserTutorial();
			break;
		case BROWSER_SECOND:
			prepareBrowserSecondTutorial();
			break;
		case EDITOR_START:
			prepareEditorTutorial();
			break;
		case EDITOR_SECOND:
			prepareEditorSecondTutorial();
			break;
		case TESTER_START:
			prepareTesterTutorial();
			break;
		case RESULTS_START:
			prepareResultsTutorial();
			break;
		case PREFERENCES_START:
			preparePreferencesTutorial();
			break;
		}
	}
	
	private void prepareBrowserTutorial() {
		setImage("browser.png");
		title.setText("Browser Window");
		updateHeaderText("This is the main window. Navigate through cards.");
		updateTips(
				"Cards are organized by labels, which can function as decks or be used "
				+ "to mark cards for any reason. Some labels are generated automatically and "
				+ "are always capitalized. Every card carries the 'All' label." ,
				
				"This pane shows the cards associated with the selected label in alphabetical order." ,
				
				"A preview of the picture associated with the selected card displays here." ,
				
				"The results of the selected card's quizzes appear here, with the most recent "
				+ "results displayed at the top. The date of the quiz appears at the left of its "
				+ "bar in dd/mm format." ,
				
				"A similar graph appears here holding the results of all of the quizzes for "
				+ "all of the cards which carry the selected label. The results are compiled "
				+ "into daily totals with each answer box equally weighted." );
	}
	
	
	private void prepareBrowserSecondTutorial() {
		setImage("browser2.png");
		title.setText("Browser Window");
		updateHeaderText("These are the primary browser controls.");
		updateTips(
				"Create a new card. You will be prompted for a name but are not required "
				+ "to provide one." ,
				
				"Delete the selected card(s). Use with caution: no undos." ,
				
				"Test cards. This creates a quiz out of the selected cards and records "
				+ "your answers so you can track your progress." ,
				
				"Study cards. This has exactly the same functionality as 'Test' except that "
				+ "you may not get an answer wrong (only correct answers \"stick\"). No "
				+ "results are logged.");
	}
	
	private void prepareEditorTutorial() {
		setImage("editor1.png");
		title.setText("Editor Window");
		updateHeaderText("Here is where the card image and answer boxes are modified.");
		updateTips(
				"A normal box. Click and drag to create, then double click later to modify "
				+ "the text. Right click to select, then drag to move or drag the lower right "
				+ "corner to resize." , 
				
				"Linked boxes. When testing, an answer is considered correct if it is dropped "
				+ "onto the correct group, which is helpful if the order of some set of answers "
				+ "doesn't matter." ,
				
				"A locked box. This type of box may be used to hide part of the image or as a hint. "
				+ "It always displays the text it shows; it is not available to have answers "
				+ "dropped on it.");
	}
	
	private void prepareEditorSecondTutorial() {
		setImage("editor2.png");
		title.setText("Editor Window");
		updateHeaderText("These are the primary editor controls.");
		updateTips(
				"Edit info/Add image. Modify the name of the card, the image it displays, "
				+ "or the labels it carries. To add an image using a system file browser, use the "
				+ "add image button. If you don't select 'Make a local copy,' the program uses "
				+ "the URI of the image in your filesystem, and moving or deleting the image later will "
				+ "cause the import to fail." , 
				
				"Select tool. Toggle to use a graphical selector." , 
				
				"Link/Unlink. With a group of boxes selected, use this to create or dismantle a group." , 
				
				"Lock/Unlock. Change the locked status of the selected box(es)." , 
				
				"Delete box(es). Once again, use with caution: no undos." );
	}
	
	private void prepareTesterTutorial() {
		setImage("tester1.png");
		title.setText("Testing Window");
		updateHeaderText( 
				"This is the testing or studying interface. There are two differences between these modes: "
				+ "in the studyer, only correct answers will \"stick\" to their boxes and the results will not "
				+ "be recorded (since it will always be 100%). " );
		updateTips(
				"Previous/Next. If studying multiple cards, use these to switch between them. "
				+ "Pressing next on the last card in the test has the same effect as the finish button." ,
				"Finish test. Exits the study or quiz session and prepares results, if applicable. "
				+ "Exiting the tester in any other way cancels the test." ,
				"An example showing an answer being dragged onto a group of linked boxes. You may also "
				+ "use arrow keys to navigate the answer boxes and push the letter or number corresponding "
				+ "to the desired answer to place that answer in the current box.");
	}
	
	private void prepareResultsTutorial() {
		setImage("results.png");
		title.setText("Results Window");
		updateHeaderText("This is the results page, where you are brought after finishing a studying or "
				+ "testing session." );
		updateTips(
				"The score and results for all of the cards tested appear here. If only "
				+ "one card was studied, the two results areas show the same results. "
				+ "Use the next/previous buttons to navigate between cards." ,
				
				"The score and results for the current card appear here. The graph shows the current card's "
				+ "score history with the most recent score at the top." ,
				
				"A correct answer, marked in green." ,
				
				"An incorrect answer, marked in red. Rollover to view the correct answer.");
	}
	
	private void preparePreferencesTutorial() {
		setImage("preferences_tut.png");
		title.setText("Preferences Window");
		updateHeaderText("This is the preferences page, where you can change shortcut keys.");
		updateTips(
				"Double click on a command to change its key binding." ,
				"Click <add key>, then type your new key. If there's a conflict, it will appear "
				+ "in red and nothing will be changed. Changes are saved when you press OK.");
	}
	
	private void updateHeaderText(String text) {
		header.setText(String.format("<html><div WIDTH=%d>%s</div></html>", 650, text));	
	}
	
	private void updateTips(String... args) {
		String text = "";
		for( String arg : args ){
			text += "<li>" + arg + "</li><br>";
		}
		tips.setText(String.format("<html><ol WIDTH=%d>%s</ol></html>", 325, text));
	}
	
	private void setImage(String resourceName) {
		image = getToolkit().getImage(
				TutorialDialog.class.getClassLoader().getResource(resourceName) );
	}
	
	

	private class ImageHolder extends JPanel {
		//private int WIDTH = 500;
		
		public ImageHolder() {
			
		}
		
		@Override
		public Dimension getPreferredSize() {
			return new Dimension(580, 450);
		}
		
		@Override
		public Dimension getMinimumSize() {
			return new Dimension(580, 450);
		}

		@Override
		public boolean imageUpdate(Image img, int flags, int x, int y, int width, int height) {
			if( (flags & ALLBITS) != 0 ){
				message = null;
				TutorialDialog.this.pack();
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
			if( message != null )
				g.drawString(message, 20, 20);
			g.drawImage(image, 0, 0, this);
		}
	}
}

