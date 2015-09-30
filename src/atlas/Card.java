package atlas;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Point;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.File;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;

import javax.imageio.ImageIO;
import javax.swing.JComponent;

@SuppressWarnings("serial")
public class Card extends JComponent {
	public static final Font CARD_FONT = new Font("ArialRoundedMTBold", Font.PLAIN, 14);
	private final DataManager dataManager;
	private File saveDirectory;
	public final Integer id;
	public final Instant creationDate;
	public String name = null;
	public ArrayList<String> labels = new ArrayList<String>();
	public ArrayList<CRectangle> boxes = new ArrayList<CRectangle>();
	public String imageFilename = null;
	public Image baseImage = null;
	private CRectangle lastBoxFound = null;
	private File prevFile;
	public CRectangle tempRect = null;
	public GroupManager groups = new GroupManager();
	private int nextid = 0;
	
	public static final String ALL = "All";
	public static final String RECENTLY_ADDED = "Recently added";
	public static final String RECENETLY_STUDIED = "Recently studied";
	public static final String NEVER_TESTED = "Never tested";
	
	/**
	 * should only be called by DataManager.createNewCard()
	 */
	public Card(DataManager dm, Integer id_, String name_, 
			String saveDirectory_, Instant creationDate_) {
		dataManager = dm;
		id = id_;
		creationDate = creationDate_;
		name = name_;
		saveDirectory = new File(saveDirectory_);
		prevFile = new File(saveDirectory, id + name.replace(' ', '_') + ".card");
		checkAutoLabels();
	}
	
	public void checkAutoLabels() {
		addLabel(ALL);
		
		//check recently added
		if( creationDate.plus(Duration.ofDays(3)).isAfter(Instant.now()) )
			addLabel(RECENTLY_ADDED);
		
		//check recently studied and never tested
		CardResults results;
		try {
			results = new CardResults(dataManager, this);
			Instant lastStudiedDate = Instant.ofEpochMilli(results.getRecentDate());
			if( lastStudiedDate.plus(Duration.ofDays(3)).isAfter(Instant.now()) )
				addLabel(RECENETLY_STUDIED);
		} catch (IOException e) { 
			addLabel(NEVER_TESTED);
		}
	}
	
	public static HashSet<String> getAutoLabels() {
		HashSet<String> ret = new HashSet<String>();
		ret.add(ALL);
		ret.add(RECENETLY_STUDIED);
		ret.add(RECENTLY_ADDED);
		ret.add(NEVER_TESTED);
		return ret;
	}
	
	public static ArrayList<String> removeStringsFromList(
			ArrayList<String> list, HashSet<String> toRemove) {
		Iterator<String> it = list.iterator();
		while( it.hasNext() ){
			if( toRemove.contains(it.next()) )
				it.remove();
		}
		return list;
	}
	
	public void prepareForResults() {
		for( CRectangle box : boxes )
			box.selected = CRectangle.RESULT_SELECTED;
	}
	
	public void clearMouseListeners() {
		for( MouseListener l : getMouseListeners() )
			removeMouseListener(l);
		for( MouseMotionListener l : getMouseMotionListeners() )
			removeMouseMotionListener(l);
	}
	
	public void addBox(CRectangle newbox) {
		boxes.add(newbox);
	}
	
	public File getImageFile() {
		if( imageFilename == null ) return null;
		File ret = new File(imageFilename);
		if( !ret.isAbsolute() ){
			ret = new File(DataManager.START_DIRECTORY, ret.getName());
		}
		return ret;
	}
	
	public void addImageFile(String imageFilename_) throws IOException {
		if( imageFilename_ == null ) return;
		imageFilename = imageFilename_;
		if( Paths.get(imageFilename).isAbsolute() )
			imageFilename = Paths.get(imageFilename).getFileName().toString();
		File imageFile = getImageFile();
		baseImage = ImageIO.read(imageFile);
		setPreferredSize(new Dimension(baseImage.getWidth(null), baseImage.getHeight(null)));
		revalidate();
		repaint();
	}
	
	/**
	 * Adds the given label to the card and recomputes the DataManager's label information.
	 * Adding a label already attached to the card has no effect.
	 * @param label the label to add
	 */
	public void addLabel(String label) {
		if( labels.contains(label) ) return;
		labels.add(label);
		dataManager.notifyOfNewLabel(this, label);
	}
	
	/**
	 * Removes the given label from the card and recomputes the DataManager's label information.
	 * Attempting to remove a label not associated with this card has no effect.
	 * @param label the label to remove
	 */
	public void removeLabel(String label) {
		if( !labels.contains(label) ) return;
		labels.remove(label);
		dataManager.notifyOfRemovedLabel(this, label);
	}
	
	public void deleteSaveFile() {
		try {
			Files.delete(prevFile.toPath());
		} catch (NoSuchFileException e) {
			System.err.println("Old file not found for deletion");
			//e.printStackTrace();
		} catch (DirectoryNotEmptyException e) {
			System.err.println("Attempt to delete non-empty directory");
			//e.printStackTrace();
		} catch (IOException e) {
			System.err.println("Permissions problem deleting old file");
			//e.printStackTrace();
		}		
	}
	
	/** Returns a copy of the file denoting this card's position on the disk (/foo/user.home/Atlas/IDname.card) */
	public File getCurrentFile() {
		return new File(prevFile.getPath());
	}
	
	/**
	 * Use to write to natural save destiation, i.e. /foo/bar/IDname.card
	 * @throws FileNotFoundException
	 */
	public void writeToFile() throws FileNotFoundException {
		//delete previous file in case of name change
		deleteSaveFile(); //TODO more efficient if we compare filenames and see if they're different
		
		prevFile = new File(saveDirectory, id + name.replace(' ', '_') + ".card");
		writeToFile(prevFile);
	}
	
	/**
	 * This method for when the destination file should be specified, i.e. exporting to directory.
	 * Otherwise use the card's natural write destination, i.e. ~/Atlas/
	 * @param file destination file, i.e. /foo/bar/IDname.card
	 * @throws FileNotFoundException
	 */
	public void writeToFile(File file) throws FileNotFoundException {
		PrintWriter writer = new PrintWriter(file);
		writer.write(id + "\n");
		writer.write(Long.toString(creationDate.getEpochSecond()) + "\n");
		writer.write(name + "\n");
		
		//remove auto labels from list -- they shouldn't be written
		ArrayList<String> writeLabels = removeStringsFromList(
						new ArrayList<String>(labels),
						getAutoLabels() );
		
		writer.write(String.valueOf(writeLabels.size()) + "\n");
		for( String label : writeLabels )
			writer.write(label + "\n");
		writer.write(imageFilename + "\n");
		for( CRectangle box : boxes ){
			if( box.locked )
				writer.write("locked ");
			if( groups.containsKey(box) )
				writer.write("groupid " + groups.get(box) + " ");
			writer.write(box.x + " " + box.y + " " + box.width + " " + box.height + " ");
			String[] answerLines = box.answer.split("\n");
			for( int i=0; i < answerLines.length-1; ++i )
				writer.write(answerLines[i] + "\\n");
			writer.write(answerLines[answerLines.length-1]);
			writer.write("\n");
		}
		writer.close();
	}
	
	/**
	 * Returns the CRectangle at the given point, or null if point is not over a box
	 */
	public CRectangle getBoxAtPoint(Point p) {
		for( CRectangle box : boxes )
			if( box.contains(p) ) return box;
		return null;
	}
	
	/**
	 * Mouse is dragged at the given point. If there is a box under the mouse, it is marked selected.
	 * @param p mouse point
	 * @param selectionType CRectangle STUDY_SELECTED or EDIT_SELECTED depending on the context
	 */
	public void mouseAtPoint(Point p, int selectionType) {
		boolean foundABox = false;
		CRectangle box = getBoxAtPoint(p);
		if( lastBoxFound != null && !lastBoxFound.equals(box) ){
			lastBoxFound.selected = CRectangle.NOT_SELECTED;
			groups.selectBoxesInGroup(lastBoxFound, CRectangle.NOT_SELECTED);
		}
		if( box != null ){
			foundABox = true;
			lastBoxFound = box;
			box.selected = selectionType;
			groups.selectBoxesInGroup(box, selectionType);
		}
		if( !foundABox && lastBoxFound != null ){
			lastBoxFound.selected = CRectangle.NOT_SELECTED;
			groups.selectBoxesInGroup(lastBoxFound, CRectangle.NOT_SELECTED);
		}
	}

	/**
	 * Displays the answer in the box containing the point if the supplied answer is correct.
	 * If allowWrongAnswers is set to true, the box containing the point will display the supplied string 
	 * whether or not it is correct. If there is already a string in the box and allowWrongAnswers is
	 * true, the given string will replace that answer already there. 
	 * <p>
	 * If the box is part of a group, the answer will first try to stick to the given box. If the given box
	 * already displays text, the answer will search through the boxes in the group to find one not displaying 
	 * text. If every box in the group is displaying text, the answer will replace the text in the given box.
	 * <p>
	 * The return is whatever string should be sent back to the answer box. If the given string is successfully 
	 * placed into an empty box, null is returned. If given the string is dropped on a box that won't accept it or
	 * dropped onto empty space, the given string is returned. If the given string is dropped onto a box that
	 * will accept it and an answer is already there, the answer that the given string replaces is returned. 
	 * 
	 * @param userAnswer String dropped by user at point
	 * @param p point at which answer was dropped
	 * @param allowWrongAnswers if true, box will hold an answer even if wrong; if false, vice-versa
	 * @return the answer that should be sent back to the answer box, or null if none should be
	 */
	public String dropAnswerAt(String userAnswer, Point p, 
			boolean allowWrongAnswers, boolean deselectAfterDrop) {
		return dropAnswerAt(userAnswer, getBoxAtPoint(p), allowWrongAnswers, deselectAfterDrop);
	}
	
	/**
	 * See notes for Card.dropAnswerAt(String,Point,boolean). The difference is the second parameter:
	 * this method uses the given box rather than finding the box under the given point
	 */
	public String dropAnswerAt(String userAnswer, CRectangle box, 
			boolean allowWrongAnswers, boolean deselectAfterDrop) {
		if( userAnswer == null ) throw new IllegalArgumentException("Answer may not be null");
		if( box == null || box.locked ) return userAnswer;

		String curAnswer = box.getDisplayedText();
		String ret = null;
		if( groups.get(box) == null ){ //not part of group
			if( box.answer.equals(userAnswer) ){
				box.showAnswer = true;
				box.falseAnswer = null;
				ret = curAnswer;
			} else { //userAnswer is wrong
				if( allowWrongAnswers ){
					box.showAnswer = false;
					box.falseAnswer = userAnswer;
					ret = curAnswer;
				} else { //no wrong answers, i.e. study mode
					ret = userAnswer;
				}
			}
		} else {
			ret = groups.dropAnswerOnGroup(userAnswer, box, allowWrongAnswers);
		}

		if( deselectAfterDrop ){
			box.selected = CRectangle.NOT_SELECTED;
			groups.selectBoxesInGroup(box, CRectangle.NOT_SELECTED);			
		}
		repaint();
		return ret;
	}
	
	/** convenience that forwards to the other parseFile with {@code false} as the last parameter */
	public static Card parseFile(DataManager dm, File file) throws IOException {
		return parseFile(dm, file, false);
	}
	
	/**
	 * Parse a file to create a card object. This is for loading cards in memory at the start of the 
	 * program. 
	 * @param dm
	 * @param file
	 * @param ignoreID If this method is being used to import cards from outside the normal directory,
	 * this should be set to true, telling the method to ignore the ID number supplied in the file and
	 * get an ID from the DataManager instead, as usual when creating a new card.
	 * @return
	 * @throws IOException
	 */
	public static Card parseFile(DataManager dm, File file, boolean ignoreID) throws IOException {
		if( !file.toString().endsWith(".card") ) throw new IOException("File not '.card' file");
		BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
		int id = Integer.parseInt(reader.readLine());
		
		//basic card info
		String line = reader.readLine();
		String name;
		Card ret;
		// the 'else' is for backwards compatability only - will break if a card name is 
		//  numeric and there is no creation time written 
		if( line.matches("\\d+") ){ //if there is a creationTime noted in file
			Instant creationTime = Instant.ofEpochSecond(Long.parseLong(line));
			name = reader.readLine();
			ret = ignoreID ? 
					dm.createNewCard(name, creationTime) : //use dm to get id (for importing cards)
					dm.createNewCard(name, id, creationTime); //use id in file
		} else { //if not
			name = line;
			ret = ignoreID ? 
					dm.createNewCard(name, Instant.now()) : //use dm to get id (for importing cards)
					dm.createNewCard(name, id, Instant.now()); //use id in file
		}
		
		//labels and image filename
		int numLabels = Integer.parseInt(reader.readLine());
		for( int i=0; i<numLabels; ++i )
			ret.addLabel(reader.readLine());
		ret.imageFilename = reader.readLine();
		if( ret.imageFilename == "null" ) ret.imageFilename = null;
		
		/*//TODO for rewriting files only
		if( new File(ret.imageFilename).isAbsolute() )
			ret.imageFilename = new File(ret.imageFilename).getName(); */

		//read box information
		while( (line = reader.readLine()) != null ){
			CRectangle temp = ret.createNewBox();
			String[] elements = line.split(" ");
			int count = 0;
			if( elements[count].equals("locked") ){
				temp.locked = true;
				++count;
			}
			if( elements[count].equals("groupid") ){
				ret.groups.put(temp, new Integer(elements[++count]));
				++count;
			}
			temp.x = Integer.valueOf(elements[count++]);
			temp.y = Integer.valueOf(elements[count++]);
			temp.width = Integer.valueOf(elements[count++]);
			temp.height = Integer.valueOf(elements[count++]);
			String answer = "";
			for( int i=count; i < elements.length; ++i ){
				String[] elementParts = elements[i].split("\\\\n");
				for( int j=0; j < elementParts.length-1; ++j )
					answer += elementParts[j] + "\n";
				answer += elementParts[elementParts.length-1] + " ";
			}
			temp.answer = answer.trim();
			ret.boxes.add(temp);
		}
		reader.close();
		
		return ret;
	}

	public static boolean isNameValid(String name) {
		return !name.matches(".*[_/\\\\.\\*].*");
	}
	
	public CRectangle createNewBox() {
		return new CRectangle(nextid++);
	}
	
	public CRectangle createNewBox(int x_, int y_, int width_, int height_) {
		return new CRectangle(x_, y_, width_, height_, nextid++);
	}
	
	@Override
	public String toString() {
		return "Card [name=" + name + ", boxes=" + boxes + "]";
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((boxes == null) ? 0 : boxes.hashCode());
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result
				+ ((imageFilename == null) ? 0 : imageFilename.hashCode());
		result = prime * result + ((labels == null) ? 0 : labels.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}
	
	//TODO this should be examined - two cards should maybe be considered equal IFF their id's are equal
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof Card))
			return false;
		Card other = (Card) obj;
		if (boxes == null) {
			if (other.boxes != null)
				return false;
		} else if (!boxes.equals(other.boxes))
			return false;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (imageFilename == null) {
			if (other.imageFilename != null)
				return false;
		} else if (!imageFilename.equals(other.imageFilename))
			return false;
		if (labels == null) {
			if (other.labels != null)
				return false;
		} else if (!labels.equals(other.labels))
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}
	
	@Override
	public void paint(Graphics g) {
		super.paint(g);
		
		g.setFont(Card.CARD_FONT);
		if( baseImage == null ){ //kind of a janky workaround to try loading the image if not already loaded
			try {
				addImageFile(imageFilename);
			} catch (IOException e) {
				//System.err.println("image load failed when trying to paint");
			}
		}
		if( baseImage != null ){
			g.drawImage(baseImage, 0, 0, null);
		} else {
			int errorX =200, errorY =200;
			if( imageFilename == null )
				g.drawString("no image path given", errorX, errorY);
			else {
				g.drawString("problem reading image", errorX, errorY);
			}
		}
		
		//editing window temp box
		if( tempRect != null ){
			g.setColor(Color.red);
			g.drawRect(tempRect.getDrawX(), tempRect.getDrawY(), 
					tempRect.getDrawWidth(), tempRect.getDrawHeight());
			g.setColor(Color.black);
		}
		
		groups.paint(g);
		
		for( CRectangle box : boxes ) box.paint(g);
	}

	
}
