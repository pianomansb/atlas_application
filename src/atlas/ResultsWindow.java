package atlas;

import java.awt.Color;
import java.awt.Font;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.font.TextAttribute;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Map;

import javax.swing.GroupLayout;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.GroupLayout.Alignment;

@SuppressWarnings("serial")
public class ResultsWindow extends JPanel implements MouseMotionListener{
	private final DataManager dm;
	private final Card[] cards;
	private final CardResults[] results;
	private CRectangle lastBox = null;
	public static final Color textGreen = new Color(0, 205, 0);
	public static final Color textYellow = new Color(255, 153, 18);
	private Graph graph = new Graph();
	private JScrollPane cardImage = new JScrollPane();
	private JLabel cardName = new JLabel();
	private JLabel cardPercent = new JLabel();
	private JLabel cardCorrect = new JLabel();
	private JLabel cardWrong = new JLabel();
	private JLabel cardBlank = new JLabel();
	private int cardCounter = 0;

	public ResultsWindow (DataManager dm_, Card[] cards_) throws FileNotFoundException, IOException {
		dm = dm_;
		dm.setTitle("Results");
		cards = cards_;

		//initialize cards and results
		results = new CardResults[cards.length];
		int overallResultsCorrect = 0;
		int overallResultsWrong = 0;
		int overallResultsBlank = 0;
		int i = 0;
		for( Card c : cards ){
			c.clearMouseListeners();
			c.prepareForResults();
			results[i] = new CardResults(dm, c);
			overallResultsCorrect += results[i].getRecentCorrect();
			overallResultsWrong += results[i].getRecentWrong();
			overallResultsBlank += results[i].getRecentBlank();
			c.addMouseMotionListener(this);
			++i;
		}
		double overallResultsPercent = 
				(double)overallResultsCorrect / (overallResultsCorrect + overallResultsWrong);
		overallResultsPercent = (double)((int)(overallResultsPercent*10000))/100;
		
		refreshCardDependentFields(results[0]);
		
		GroupLayout layout = new GroupLayout(this);
		setLayout(layout);
		layout.setAutoCreateContainerGaps(true);
		layout.setAutoCreateGaps(true);
		
		Font regfont = getFont();
		if( regfont == null ){
			regfont = new Font("SansSerif", Font.PLAIN, 12);
			setFont(regfont);
		}
		Map<TextAttribute, Object> attrs = new Hashtable<TextAttribute, Object>();
		
		attrs.put(TextAttribute.SIZE, 40);
		attrs.put(TextAttribute.FAMILY, "HelveticaNeue");
		attrs.put(TextAttribute.WEIGHT, TextAttribute.WEIGHT_BOLD);
		JLabel overallPercent = generatePercentLabel(overallResultsPercent, regfont, attrs);
		
		JLabel overallScore = new JLabel("Overall score:");
		attrs.put(TextAttribute.SIZE, 14);
		attrs.put(TextAttribute.FOREGROUND, Color.black);
		attrs.put(TextAttribute.WEIGHT, TextAttribute.WEIGHT_REGULAR);
		overallScore.setFont(regfont.deriveFont(attrs));
		
		JLabel overallCorrect = new JLabel(String.valueOf(overallResultsCorrect) + " correct");
		attrs.put(TextAttribute.SIZE, 18);
		attrs.put(TextAttribute.FOREGROUND, textGreen);
		attrs.put(TextAttribute.WEIGHT, TextAttribute.WEIGHT_BOLD);
		overallCorrect.setFont(regfont.deriveFont(attrs));
		
		JLabel overallWrong = new JLabel(String.valueOf(overallResultsWrong) + " wrong");
		attrs.put(TextAttribute.FOREGROUND, Color.red);
		overallWrong.setFont(regfont.deriveFont(attrs));
		
		JLabel overallBlank = new JLabel(String.valueOf(overallResultsBlank) + " blank");
		attrs.put(TextAttribute.FOREGROUND, Color.black);
		overallBlank.setFont(regfont.deriveFont(attrs));
		
		JLabel resultsLabel = new JLabel("Results for: ");
		attrs.put(TextAttribute.SIZE, 14);
		attrs.put(TextAttribute.FOREGROUND, Color.black);
		attrs.put(TextAttribute.WEIGHT, TextAttribute.WEIGHT_REGULAR);
		resultsLabel.setFont(regfont.deriveFont(attrs));
		
		attrs.put(TextAttribute.SIZE, 16);
		attrs.put(TextAttribute.WEIGHT, TextAttribute.WEIGHT_BOLD);
		cardName.setFont(regfont.deriveFont(attrs));
		
		attrs.put(TextAttribute.SIZE, 30);
		cardPercent = generatePercentLabel(cardPercent, regfont, attrs);
		
		attrs.put(TextAttribute.SIZE, 14);
		attrs.put(TextAttribute.FOREGROUND, textGreen);
		attrs.put(TextAttribute.WEIGHT, TextAttribute.WEIGHT_BOLD);
		cardCorrect.setFont(regfont.deriveFont(attrs));
		
		attrs.put(TextAttribute.FOREGROUND, Color.red);
		cardWrong.setFont(regfont.deriveFont(attrs));
		
		attrs.put(TextAttribute.FOREGROUND, Color.black);
		cardBlank.setFont(regfont.deriveFont(attrs));
		
		JScrollPane graphPane = new JScrollPane(graph);
		
		layout.setHorizontalGroup(layout.createSequentialGroup()
			.addGroup(layout.createParallelGroup()
				.addGroup(layout.createSequentialGroup()
					.addComponent(overallPercent)
					.addGroup(layout.createParallelGroup()
						.addComponent(overallScore)
						.addGroup(layout.createSequentialGroup()
							.addComponent(overallCorrect)
							.addComponent(overallWrong)
							.addComponent(overallBlank) 
						)
					)
				).addComponent(cardImage)
			).addGroup(layout.createParallelGroup()
				.addComponent(resultsLabel)
				.addComponent(cardName)
				.addComponent(cardPercent)
				.addGroup(layout.createSequentialGroup()
					.addComponent(cardCorrect)
					.addComponent(cardWrong)
					.addComponent(cardBlank)
				).addComponent(graphPane)
			)
		);
		
		layout.setVerticalGroup(layout.createSequentialGroup()
			.addGroup(layout.createParallelGroup(Alignment.TRAILING)
				.addComponent(overallPercent)
				.addGroup(layout.createSequentialGroup()
					.addComponent(overallScore)
					.addGroup(layout.createParallelGroup()
						.addComponent(overallCorrect)
						.addComponent(overallWrong)
						.addComponent(overallBlank)
					)
				)
			).addGroup(layout.createParallelGroup()
				.addComponent(cardImage)
				.addGroup(layout.createSequentialGroup()
					.addComponent(resultsLabel)
					.addComponent(cardName)
					.addComponent(cardPercent)
					.addGroup(layout.createParallelGroup(Alignment.TRAILING)
						.addComponent(cardCorrect)
						.addComponent(cardWrong)
						.addComponent(cardBlank)
					).addComponent(graphPane)
				)
			)
		);
		
	}
	
	/**
	 * Ask the user if they want to study the current card or all the tested cards again.
	 * @return the cards to study, or null if cancel was pressed
	 */
	public Card[] restudy() {
		Object[] options = { "This card", "This set", "Cancel" };
		int choice = JOptionPane.showOptionDialog(this,
				"Use just this card, or all cards from this quiz?", 
				"Restudy confirm", 
				JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, 
				null, options, options[0]);
		
		if( choice == JOptionPane.YES_OPTION ){
			Card[] ret = { cards[cardCounter] };
			return ret;
		} else if( choice == JOptionPane.NO_OPTION ){
			return cards;
		}
		return null;
	}
	
	public void previousCard() {
		if( cardCounter == 0 ) return;
		refreshCardDependentFields(results[--cardCounter]);
	}
	
	public void nextCard() {
		if( cardCounter == results.length-1 ) return;
		refreshCardDependentFields(results[++cardCounter]);
	}
	
	public Card getCurrentCard() {
		return cards[cardCounter];
	}
	
	private void refreshCardDependentFields(CardResults cardResults) {
		graph.setData(cardResults.getPerformanceOverTime());
		cardImage.setViewportView(cardResults.getCard());
		cardName.setText(cardResults.getCardName());
		cardPercent.setText(String.valueOf(cardResults.getRecentPercent()) + "%");
		cardCorrect.setText(String.valueOf(cardResults.getRecentCorrect()) + " correct");
		cardWrong.setText(String.valueOf(cardResults.getRecentWrong()) + " wrong");
		cardBlank.setText(String.valueOf(cardResults.getRecentBlank()) + " blank");
	}
	
	/**
	 * Takes the percent to display and adds it to a jlabel. 
	 * Depending on the percent value the FOREGROUND attribute of the map
	 * is modified with the appropriate color, then the jlabel's font is set
	 * to the font derived from the attributes using the given font
	 * @param percent number to add to jlabel
	 * @param font font from which to derive new font
	 * @param attrs attributes to which to add color 
	 * @return formatted JLabel
	 */
	private JLabel generatePercentLabel(double percent, Font font, 
			Map<TextAttribute, Object> attrs) {
		JLabel ret = new JLabel(String.valueOf(percent) + "%");
		if( percent >= 85 ) 
			attrs.put(TextAttribute.FOREGROUND, textGreen);
		else if( percent >= 70 ) 
			attrs.put(TextAttribute.FOREGROUND, textYellow);
		else 
			attrs.put(TextAttribute.FOREGROUND, Color.red);
		ret.setFont(font.deriveFont(attrs));
		return ret;
	}
	
	private JLabel generatePercentLabel(JLabel label, Font font,
			Map<TextAttribute, Object> attrs) {
		String text = label.getText();
		text = text.substring(0, text.length()-1);
		return generatePercentLabel(Double.parseDouble(text), font, attrs);
	}

	@Override
	public void mouseDragged(MouseEvent e) { }

	@Override
	public void mouseMoved(MouseEvent e) {
		CRectangle box = cards[cardCounter].getBoxAtPoint(e.getPoint());
		if( box == null ){
			if( lastBox != null ){
				lastBox.setDisplayCorrectAnswerForResults(false);
				lastBox = null;
				repaint();
			}
			return;
		}
		if( box.equals(lastBox) ) return;
		else if( lastBox != null ){
			lastBox.setDisplayCorrectAnswerForResults(false);
			repaint();
		}
		box.setDisplayCorrectAnswerForResults(true);
		lastBox = box;
		repaint();
	}
	
}
