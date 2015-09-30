package atlas;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;

import javax.swing.JOptionPane;

public class StudyManager {
	private final Card[] cards;
	private final DataManager dataManager;
	private int activeCard = 0;
	private StudyWindow[] windows;
	private boolean allowWrongAnswers;

	public StudyManager(DataManager dm, Card[] cards_, boolean allowWrongAnswers_) {
		cards = cards_;
		dataManager = dm;
		allowWrongAnswers = allowWrongAnswers_;
		windows = new StudyWindow[cards.length];
		
		for( int i=0; i < windows.length; ++i) 
			windows[i] = new StudyWindow(dataManager, cards[i], allowWrongAnswers);
		dataManager.loadNewWindow(windows[0]);
		
	}
	
	public void askToFinishQuiz() {
		int input = JOptionPane.showConfirmDialog(windows[activeCard], "Finish quiz?", 
				"Finish confirmation", JOptionPane.YES_NO_OPTION);
		if( input != JOptionPane.YES_OPTION ) return;
		
		for( Card card : cards ) //TODO not working?
			card.checkAutoLabels();
		
		if( allowWrongAnswers ) writeResultsToFile();
		dataManager.setAccelsEnabled(null, true);
		try {
			dataManager.loadNewWindow(new ResultsWindow(dataManager, cards));
		} catch (FileNotFoundException e) {
			System.err.println("Results file not found");
			dataManager.loadNewWindow(new BrowserWindow(dataManager));
		} catch (IOException e) {
			System.err.println("Problem loading results file");
			dataManager.loadNewWindow(new BrowserWindow(dataManager));
		}		
	}
	
	private void writeResultsToFile() {
		long curTime = System.currentTimeMillis();
		//write individual card results
		for( Card card : cards ){
			try( PrintWriter writer = new PrintWriter( new BufferedWriter( new FileWriter( new File(
					DataManager.START_DIRECTORY, card.id + "_results"), true))) ){
				writer.println(curTime);
				writeAnswers(writer, card);
				writer.println("==========");
			} catch (IOException e) {
				System.err.println("problem writing results file for card" + card.id);
				System.err.println(e.toString());
			}
		}
		//log test data
		if( cards.length <= 1 ) return; //only for multiple cards
		//sort ids to ensure that the same set of cards makes the same file every time
		int[] ids = new int[cards.length];
		for( int i=0; i < ids.length; ++i ){
			ids[i] = cards[i].id.intValue();
		}
		Arrays.sort(ids);
		String filename = "";
		for( int id : ids )
			filename += id + "_";
		filename += "results";
		try( PrintWriter writer = new PrintWriter( new BufferedWriter( new FileWriter( new File( 
				DataManager.START_DIRECTORY, filename), true))) ){
			writer.println(curTime);
			writer.println("==========");
		} catch (IOException e) {
			System.err.println("problem writing results file for quiz");
			System.err.println(e.toString());
		}
	}
	
	private void writeAnswers(PrintWriter writer, Card card) {
		card.groups.prepareForResults();
		for( CRectangle box : card.boxes )
			if( !box.locked ) writeSingleBoxResults(writer, box);
	}
	
	private void writeSingleBoxResults(PrintWriter writer, CRectangle box) {
		String givenAnswer = "";
		if( box.falseAnswer != null ) givenAnswer = box.falseAnswer;
		else if( box.showAnswer ) givenAnswer = box.answer;
		writeMultiLine(writer, box.answer);
		writer.write("::");
		writeMultiLine(writer, givenAnswer);
		writer.write("\n");
	}
	
	private void writeMultiLine(PrintWriter writer, String string) {
		String[] parts = string.split("\n");
		for( int i=0; i < parts.length-1; ++i )
			writer.write(parts[i] + "\\n");
		writer.write(parts[parts.length-1]);
	}
	
	public void nextCard() {
		if( activeCard == cards.length-1 ){
			askToFinishQuiz();
			return;
		}
		++activeCard;
		changeCard();
	}

	public void previousCard() {
		if( activeCard == 0 ) return;
		--activeCard;
		changeCard();
	}
	
	private void changeCard() {
		//if( windows[activeCard] == null ){
		//	windows[activeCard] = new StudyWindow(dataManager, cards[activeCard], allowWrongAnswers);
		//}
		dataManager.loadNewWindow(windows[activeCard]);		
	}

}

