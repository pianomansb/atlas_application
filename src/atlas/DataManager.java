package atlas;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;

import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.GroupLayout;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.KeyStroke;

import atlas.FileBrowserComponent.FileBrowserResult;

public class DataManager implements FileVisitor<Path> {
	public static final File START_DIRECTORY = new File(
			new File(System.getProperty("user.home")), "Atlas");
	public final JFrame frame;
	private final String META_KEY = 
			System.getProperty("os.name").startsWith("Mac") ? "meta" : "control"; 
	public Hashtable<Integer, Card> allCards = new Hashtable<Integer, Card>();
	public Hashtable<String, ArrayList<Integer> > labels = new Hashtable<String, ArrayList<Integer> >();
	private int nextId = 0;
	private ArrayList<File> loadedFiles;
	public ActionMap actionMap = new ActionMap();
	private JPanel currentWindow = null;
	private StudyManager studyManager = null;
	
	private JPanel toolbar;
	private ArrayList<AbstractButton> toolbarButtons = new ArrayList<AbstractButton>();
	private JPanel windowHolder;
	
	public DataManager(JFrame f) {
		frame = f;
		toolbar = new JPanel();
		toolbar.setBorder(BorderFactory.createLineBorder(Color.black));
		windowHolder = new JPanel();
		windowHolder.setLayout(new GridBagLayout());
		
		frame.getContentPane().setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = .5;
		c.weighty = 0;
		c.gridx = 0;
		c.gridy = 0;
		frame.getContentPane().add(toolbar, c);
		
		c.fill = GridBagConstraints.BOTH;
		c.weightx = .5;
		c.weighty = .5;
		c.gridx = 0;
		c.gridy = 1;
		frame.getContentPane().add(windowHolder, c);
		
		START_DIRECTORY.mkdirs(); //make sure PictureCards folder is there
		
		initializeMenuBarAndActions();
		refreshToolbar();
		loadPreferences();
		frame.pack();
		frame.repaint();
		
		//labels.put(Card.ALL, new ArrayList<Integer>());
		allCards = loadCards();
	}
	
	public void loadNewWindow(JPanel window) {		
		if( currentWindow instanceof EditorWindow ){
			EditorWindow win = (EditorWindow)currentWindow;
			if( win.unsavedChanges ){
				int in = JOptionPane.showConfirmDialog(win, "Save changes?", 
						"Unsaved changes", JOptionPane.YES_NO_OPTION);
				if( in == JOptionPane.YES_OPTION )
					win.save();
				else
					/* TODO This is inefficient, but lingering unwritten changes will persist if 
					 * cards are not reloaded from save files. Don't want people thinking things
					 * that aren't there are. 
					 */
					allCards = loadCards(); 
			}
		}
		if( currentWindow != null ) windowHolder.remove(currentWindow);
		currentWindow = window;
		GridBagConstraints c = new GridBagConstraints();
		c.weightx = .5;
		c.weighty = .5;
		c.fill = GridBagConstraints.BOTH;
		windowHolder.add(currentWindow, c);
		frame.pack();
		windowHolder.repaint();
		
		//set enabled/disabled status of buttons TODO: could be faster without iterating over all items
		/* All items:
		 * New card, Save card, Preferences, Edit card, Edit card info, Add image, 
		 * Delete boxes, Test cards, Study cards, Next card, Previous Card, Finish quiz
		 * Browser, Delete card, Select, Link boxes, Unlink boxes, Lock/Unlock, Tutorial
		 */
		HashSet<String> enable = new HashSet<String>();
		enable.add("About");
		enable.add("Tutorial");
		if( currentWindow instanceof BrowserWindow ){
			enable.add("Import");
			enable.add("Export");
			enable.add("New card");
			enable.add("Preferences");
			enable.add("Edit card");
			enable.add("Edit card info");
			enable.add("Add image");
			enable.add("Study cards");
			enable.add("Test cards");
			enable.add("Delete card");
		} else if( currentWindow instanceof EditorWindow ){
			enable.add("New card");
			enable.add("Save card");
			enable.add("Browser");
			enable.add("Preferences");
			enable.add("Edit card info");
			enable.add("Add image");
			enable.add("Study cards");
			enable.add("Test cards");
			enable.add("Delete card");
			enable.add("Delete boxes");
			enable.add("Select");
			enable.add("Link boxes");
			enable.add("Unlink boxes");
			enable.add("Lock/Unlock");
		} else if( currentWindow instanceof StudyWindow ){
			enable.add("Edit card");
			enable.add("Next card");
			enable.add("Previous card");
			enable.add("Finish quiz");
			enable.add("Browser");
		} else if( currentWindow instanceof ResultsWindow ){
			enable.add("Edit card");
			enable.add("New card");
			enable.add("Browser");
			enable.add("Next card");
			enable.add("Previous card");
			enable.add("Preferences");
			enable.add("Test cards");
			enable.add("Study cards");
		} else if( currentWindow instanceof PreferencesWindow ){
			enable.add("Browser");
		}
		for( Object key : actionMap.allKeys() )
			actionMap.get(key).setEnabled( (enable.contains(key)) ? true : false);
	}
		
	/**
	 * Try to load the specified icon image and return that icon. 
	 * If the icon cannot be found, a message is printed to stderr and 
	 * the 'backup' string is returned
	 */
	public static Object loadIconOrText(String resourceName, String backup) {
		try( InputStream is = ResultsWindow.class.getClassLoader()
				.getResourceAsStream(resourceName)) {
			return new ImageIcon(ImageIO.read(is));
		} catch (IOException e) {
			System.err.println("couldn't load icons");
		}
		return backup;
	}
	
	public JButton loadButton(Action action, KeyStroke accelKey,
			String iconName, String backupText) {
		JButton ret = new JButton();
		action.putValue(Action.ACCELERATOR_KEY, accelKey);
		ret.setAction(action);
		Object buttonImage = loadIconOrText(iconName, backupText);
		if( buttonImage instanceof Icon )
			ret.setIcon((Icon)buttonImage);
		else
			ret.setText((String)buttonImage);
		ret.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
				(KeyStroke)action.getValue(Action.ACCELERATOR_KEY), accelKey.toString());
		ret.getActionMap().put(accelKey.toString(), action);
		return ret;
	}
	
	public String[] getCardNamesForLabel(String label) {
		ArrayList<Integer> curIdSet = labels.get(label);
		String[] ret = new String[curIdSet.size()];
		for( int i=0; i < ret.length; ++i )
			ret[i] = allCards.get(curIdSet.get(i)).name;
		return ret;
	}
	
	public Card getCardFromLabel(String label, int index) {
		return allCards.get( labels.get(label).get(index) );
	}
	
	public Card[] getCardsFromLabel(String label, int[] indices) 
			throws IllegalArgumentException {
		if( label == null ) 
			throw new IllegalArgumentException("Label cannot be null");
		Card[] ret = new Card[indices.length];
		for(int i=0; i < ret.length; ++i )
			ret[i] = getCardFromLabel(label, indices[i]);
		return ret;
	}
	
	public Card[] getCardsFromLabel(String label) {
		ArrayList<Integer> ids = labels.get(label);
		if( ids == null ) throw new IllegalArgumentException("Label does not exist");
		Card[] ret = new Card[ids.size()];
		for( int i=0; i < ret.length; ++i )
			ret[i] = allCards.get( ids.get(i) );
		return ret;
	}
	
	public String[] getLabels() {
			return labels.keySet().toArray(new String[0]);
	}
	
	public void notifyOfNewLabel(Card c, String label) {
		ArrayList<Integer> curLabelSet = labels.get(label);
		if( curLabelSet == null ){ //new label
			labels.put(label, new ArrayList<Integer>());
			labels.get(label).add(c.id);
		} else { //add card to current label
			//make sure cards don't get labelled twice
			//TODO this is slow - linear time look through list
			if( !curLabelSet.contains(c.id) ){
				addLabel(c, label);
			}
		}
	}
	
	/**
	 * Insertion add to keep label arraylist alphabetized
	 */
	private void addLabel(Card card, String label) {
		ArrayList<Integer> ids = labels.get(label);
		int i1 = 0, i2 = ids.size();
		while( i1 != i2 ){
			int mid = ((i2-i1)>>1) + i1;
			if( allCards.get(ids.get(mid)).name.compareTo(card.name) <= 0 )
				i1 = mid+1;
			else
				i2 = mid;
		}
		ids.add(i1, card.id);
	}
	
	/**
	 * removes the card's id from the given label set.
	 * If the label set is empty after this removal, the label is deleted
	 */
	public void notifyOfRemovedLabel(Card c, String label) {
		if( !labels.containsKey(label) ){
			JOptionPane.showMessageDialog(frame, "Error: attempt to remove label not found in DataManager");
			return;
		}
		ArrayList<Integer> curLabelSet = labels.get(label);
		curLabelSet.remove(c.id);
		if( curLabelSet.isEmpty() )
			labels.remove(label);
	}
	
	public Hashtable<Integer, Card> loadCards() {		
		loadedFiles = new ArrayList<File>();
		try {
			Files.walkFileTree(START_DIRECTORY.toPath(), this);
		} catch (IOException e) {
			e.printStackTrace();
		}
				
		Hashtable<Integer, Card> ret = new Hashtable<Integer, Card>();
		for( File f : loadedFiles ){
			Card cur = null;
			try {
				cur = Card.parseFile(this, f);
			} catch (IOException e) {
				JOptionPane.showMessageDialog(frame, "Problem reading file " + f);
			}
			if( cur == null ) continue; //problem parsing card file
			ret.put(cur.id, cur);
			
			//keep track of highest id to avoid duplicate IDs 
			if( cur.id.intValue() >= nextId ) 
				nextId = cur.id.intValue() + 1; 
		}
		
		return ret;
	}

	public Card createNewCard(String name) {
		return createNewCard(name, nextId++, Instant.now());
	}
	
	/** 
	 * For use with importing cards when the name and creation time are known, but
	 * the ID should be DataManager-generated
	 */
	public Card createNewCard(String name, Instant creationTime) {
		return createNewCard(name, nextId++, creationTime);
	}
	
	public Card createNewCard(String name, int id, Instant creationTime) {
		Card cur = new Card(this, id, name, START_DIRECTORY.toString(), creationTime);
		allCards.put(cur.id, cur);
		return cur;		
	}

	private void deleteCards(Card[] cards) {
		Object[] options = { "Delete", "Cancel" };
		Object choice = JOptionPane.showOptionDialog(currentWindow, 
				"Delete card" + ((cards.length == 1) ? "" : "s") 
				+ "?\nThis action is final", 
				"Delete confirm", 
				JOptionPane.YES_NO_OPTION, 
				JOptionPane.WARNING_MESSAGE, 
				null, options, options[1]);
		if( choice.equals(JOptionPane.YES_OPTION) ){
			for( Card card : cards ){
				card.deleteSaveFile();
				deleteResultsFile(card);
				for( String s : labels.keySet() )
					labels.get(s).remove(card.id);
				allCards.remove(card.id);
			}
		}
	}

	private void deleteResultsFile(Card card) {
			try {
				boolean result = Files.deleteIfExists(
						Paths.get(START_DIRECTORY.getPath(), card.id + "_results"));
				if( !result ){
					System.err.println("Results file could not be deleted because it does not exist");
				}
			} catch (IOException e) {
				System.err.println("Problem deleting results file");
			}		
	}
	
	public Card showEditInfoDialog(Card c) {
		Card[] cards = { c };
		cards = showEditInfoDialog(cards);
		assert( cards.length == 1 );
		return cards[0];
	}
	
	/**
	 * Shows a dialog with various editing options.
	 * Returns the same card[] passed. The cards now reflect the user's changes
	 * @param cards cards to send for modification
	 * @return passed cards with modifications
	 */
	public Card[] showEditInfoDialog(Card[] cards) {
		if( cards.length == 0 || cards == null ) return cards;
		EditInfoComponent comp = new EditInfoComponent(this, cards);
		Object[] array = { comp };
		JOptionPane optionPane = new JOptionPane(array, JOptionPane.PLAIN_MESSAGE, JOptionPane.OK_CANCEL_OPTION);
		JDialog dialog = new JDialog(frame, "Edit card info", true);
		optionPane.addPropertyChangeListener(new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent e) {
				if( e.getSource() == optionPane && optionPane.isVisible() &&
						e.getPropertyName() == JOptionPane.VALUE_PROPERTY ){
					dialog.setVisible(false);
					Object o = e.getNewValue();
					if( o instanceof Integer ){
						int val = (int)o;
						if( val == JOptionPane.OK_OPTION ){
							if( cards.length == 1 ){ //name/imgPath changes allowable
								cards[0].name = comp.getName();
								//cards[0].imageFilename = comp.getImgPath();
							}
						} else if( val == JOptionPane.CANCEL_OPTION ||
								val == JOptionPane.CLOSED_OPTION ){
							//if there were something to undo/do on cancel
						}
					}
				}
			}
		});
		dialog.setContentPane(optionPane);
		dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
		dialog.pack();
		dialog.setVisible(true);
		
		return cards;
	}
	
	public FileBrowserResult showFileBrowserDialog() {
		FileBrowserComponent comp = new FileBrowserComponent();
		Object[] array = { comp };
		JOptionPane optionPane = new JOptionPane(array, JOptionPane.PLAIN_MESSAGE);
		//JCheckBox createLocalCopyBox = new JCheckBox("Create local copy", true);
		Object[] options = { "Open", "Cancel"/*, createLocalCopyBox*/ };
		optionPane.setOptions(options);
		JDialog dialog = new JDialog(frame, "Find a file", true);
		optionPane.addPropertyChangeListener(new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent e) {
				if( e.getNewValue() == "Open" ){
					if( comp.open() ){
						//change a property, so that propertychange can be fired again on repeat "open" click
						optionPane.setValue("dummy"); 
					} else {
						dialog.setVisible(false);
					}
				} else if( e.getNewValue() == "Cancel" ){
					dialog.setVisible(false);
				}
			}
		});
		dialog.setContentPane(optionPane);
		dialog.setPreferredSize(new Dimension(700,500));
		dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
		dialog.pack();
		dialog.setVisible(true);

		if( optionPane.getValue() == "Cancel" ) return null;
		return new FileBrowserResult(comp.getChosenFile()/*, createLocalCopyBox.isSelected()*/);
	}
	
	@Override
 	public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
			throws IOException {
		if( file.toString().endsWith(".card"))
			loadedFiles.add(file.toFile());
		return FileVisitResult.CONTINUE;
	}

	@Override
	public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
			throws IOException {
		return FileVisitResult.CONTINUE;
	}

	@Override
	public FileVisitResult postVisitDirectory(Path dir, IOException exc)
			throws IOException {
		return FileVisitResult.CONTINUE;
	}

	@Override
	public FileVisitResult visitFileFailed(Path file, IOException exc)
			throws IOException {
		return null;
	}

	private void initializeMenuBarAndActions() {
		initializeActions();
		JMenuBar menuBar = new JMenuBar();
		frame.setJMenuBar(menuBar);
		
		/*== File Menu ==*/
		JMenu menu = new JMenu("File");
		menuBar.add(menu);
		
		menu.add( createMenuItem("New card", META_KEY + " N", "new_card.png") );
		menu.add( createMenuItem("Save card", META_KEY + " S", "save.png") );
		menu.add( createMenuItem("Delete card", "shift " + META_KEY + " BACK_SPACE", "trash.png") );
		menu.addSeparator();
		menu.add( createMenuItem("Import", "shift " + META_KEY + " I", null) );
		menu.add( createMenuItem("Export", "shift " + META_KEY + " E", null) );
		menu.addSeparator();
		menu.add( createMenuItem("Preferences", META_KEY + " COMMA", null, true) );

		/*== Edit Menu ==*/
		menu = new JMenu("Edit");
		menuBar.add(menu);
		menu.add( createMenuItem("Edit card", META_KEY + " E", "edit_card.png") );
		menu.add( createMenuItem("Edit card info", META_KEY + " I", "edit_info.png") );
		menu.add( createMenuItem("Add image", "shift " + META_KEY + " A", "add_image.png") );
		menu.add( createMenuItem("Select", "S", "select.png", false, "select_selected.png") );
		menu.add( createMenuItem("Link boxes", "L", "link.png") );
		menu.add( createMenuItem("Unlink boxes", "U", "break_link.png") );
		menu.add( createMenuItem("Lock/Unlock", "X", "lock.png") );
		menu.add( createMenuItem("Delete boxes", META_KEY + " BACK_SPACE", "trash_box.png", true) );
		
		/*== Study Menu ==*/
		menu = new JMenu("Study");
		menuBar.add(menu);
		menu.add( createMenuItem("Test cards", META_KEY + " T", "test.png") );
		menu.add( createMenuItem("Study cards", "shift " + META_KEY + " T", "study.png") );
		menu.add( createMenuItem("Previous card", META_KEY + " LEFT", "left.png") );
		menu.add( createMenuItem("Next card", META_KEY + " RIGHT", "right.png") );
		menu.add( createMenuItem("Finish quiz", "ESCAPE", "finish.png", true) );
		
		/*== Navigate Menu ==*/
		menu = new JMenu("Navigate");
		menuBar.add(menu);
		menu.add( createMenuItem("Browser", "shift " + META_KEY + " B", "home.png") );
		
		/*== Help Menu ==*/
		menu = new JMenu("Help");
		menuBar.add(menu);
		menu.add( createMenuItem("About", null, null) );
		menu.add( createMenuItem("Tutorial", "shift " + META_KEY + " H", "help.png") );
	}
	
	private JMenuItem createMenuItem(String text, String accel, String iconResource) {
		return createMenuItem(text, accel, iconResource, false);
	}
	
	private JMenuItem createMenuItem(String text, String accel, String iconResource, boolean addSeparator) {
		return  createMenuItem(text, accel, iconResource, addSeparator, null);
	}
	
	private JMenuItem createMenuItem(String text, String accel, String iconResource, 
			boolean addSeparator, String toggleResource) {
		JMenuItem item;
		if( toggleResource != null )
			item = new JCheckBoxMenuItem();
		else
			item = new JMenuItem();
		Action action = actionMap.get(text);
		item.setAction(action);
		KeyStroke accelKey = KeyStroke.getKeyStroke(accel);
		action.putValue(Action.ACCELERATOR_KEY, accelKey);
		action.putValue(Action.SHORT_DESCRIPTION, text + " " + PreferencesWindow.createAccelString(accelKey));
		action.putValue(Action.NAME, text);
		item.setText(text);
		if( iconResource != null ){
			AbstractButton button;
			if( toggleResource != null ){
				button = new JToggleButton(action);
				action.putValue(Action.SELECTED_KEY, false);
			}
			else
				button = new JButton(action);
			Object display = loadIconOrText(iconResource, null);
			if( display instanceof Icon ){
				button.setIcon((Icon)display);
				button.setText(null);
			} else {
				button.setText(text);
			}
			//set selected version of icon if button is toggle button
			if( toggleResource != null ){
				Object icon = loadIconOrText(toggleResource, null);
				if( icon instanceof Icon )
					button.setSelectedIcon((Icon)icon);
			}
			button.setBorderPainted(false);
			button.setFocusPainted(false);
			button.setContentAreaFilled(false);
			toolbarButtons.add(button);
		}
		if( addSeparator ) toolbarButtons.add(createSeparator());
		return item;
	}
	
	private JButton createSeparator() {
		Object display = loadIconOrText("bar.png", "|");
		JButton ret;
		if( display instanceof Icon )
			ret = new JButton((Icon)display);
		else
			ret = new JButton((String)display);
		ret.setEnabled(false);
		ret.setContentAreaFilled(false);
		ret.setBorderPainted(false);
		ret.setFocusPainted(false);
		return ret;
	}
	
	@SuppressWarnings("serial")
	private void initializeActions() {
		actionMap.put("Import", new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Importer.runImport(DataManager.this);
			}
		});
		actionMap.put("Export", new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Exporter.runExport(DataManager.this);
			}
		});
		actionMap.put("About", new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				showAboutDialog();
			}
		});
		actionMap.put("Tutorial", new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				TutorialDialog.showTutorial(frame);
			}
		});
		actionMap.put("Lock/Unlock", new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if( currentWindow instanceof EditorWindow ){
					((EditorWindow)currentWindow).lockOrUnlockSelectedBoxes();
				}
			}
		});
		actionMap.put("Unlink boxes", new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if( currentWindow instanceof EditorWindow ){
					((EditorWindow)currentWindow).unlinkSelectedBoxes();
				}
			}
		});
		actionMap.put("Link boxes", new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if( currentWindow instanceof EditorWindow )
					((EditorWindow)currentWindow).groupSelectedBoxes();
			}
		});
		actionMap.put("Select", new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if( currentWindow instanceof EditorWindow ){
					((EditorWindow)currentWindow).setSelecting(
							(boolean)this.getValue(Action.SELECTED_KEY));
				}
			}
		});
		actionMap.put("Finish quiz", new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				studyManager.askToFinishQuiz();
			}
		});
		actionMap.put("Preferences", new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				loadNewWindow(new PreferencesWindow(DataManager.this));
			}
		});
		actionMap.put("Add image", new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if( !( currentWindow instanceof EditorWindow ||
						currentWindow instanceof BrowserWindow) ) return;
				
				Card card = null;
				if( currentWindow instanceof BrowserWindow ){
					Hashtable<String, int[]> selection = ((BrowserWindow)currentWindow).getCurrentIndices();
					String curLabel = getLabelFromHashtable(selection);
					if( selection == null || selection.get(curLabel).length != 1 ){
						JOptionPane.showMessageDialog(currentWindow, 
								"Exactly one card must be selected to add image");
						return;
					}
					card = getCardFromLabel(curLabel, selection.get(curLabel)[0] );
				} else if( currentWindow instanceof EditorWindow ){
					card = ((EditorWindow)currentWindow).card;
				}

				//get image
				FileBrowserResult result = showFileBrowserDialog();
				if( result == null ) return;
				File toAdd;
				//if( result.makeLocalCopy ){
				toAdd = new File(START_DIRECTORY.getPath(), result.file.getName());

				int counter=0; //avoid duplicate names by appending numbers until unique
				while( toAdd.exists() ) 
					toAdd = new File(toAdd.getPath() + counter);

				try {
					Files.copy(result.file.toPath(), toAdd.toPath());
				} catch (IOException e1) {
					System.err.println("Problem creating local copy of file");
				}
				/*} else {
					toAdd = result.file;
				}*/
				
				//add image
				try {
					card.addImageFile(toAdd.getPath());
				} catch (IOException e1) {
					JOptionPane.showMessageDialog(currentWindow, "Problem reading image file");
				}
				
				//save card
				if( currentWindow instanceof EditorWindow )
					((EditorWindow)currentWindow).changeMade();
				else if( currentWindow instanceof BrowserWindow ){
					try {
						card.writeToFile();
					} catch (FileNotFoundException e1) {
						JOptionPane.showMessageDialog(currentWindow, "Problem writing save file");
					}
				}
			}
		});
		actionMap.put("Test cards", new AbstractAction() {
			@Override
			//TODO duplicate code
			public void actionPerformed(ActionEvent e) {
				if( currentWindow instanceof BrowserWindow ){
					Hashtable<String, int[]> selection = ((BrowserWindow)currentWindow).getCurrentIndices();
					if( selection == null ){
						JOptionPane.showMessageDialog(currentWindow, 
								"At least one card must be selected to study");
						return;
					}
					String label = getLabelFromHashtable(selection);
					studyManager = new StudyManager(DataManager.this, 
							getCardsFromLabel(label, selection.get(label)), true);
				} else if( currentWindow instanceof EditorWindow ){
					Card[] card = { ((EditorWindow)currentWindow).card };
					studyManager = new StudyManager(DataManager.this, card, true);
				} else if( currentWindow instanceof ResultsWindow ){
					Card[] choice = ((ResultsWindow)currentWindow).restudy();
					if( choice != null )
						studyManager = new StudyManager(DataManager.this, choice, true);
				}
			}
		});
		actionMap.put("Study cards", new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if( currentWindow instanceof BrowserWindow ){
					Hashtable<String, int[]> selection = ((BrowserWindow)currentWindow).getCurrentIndices();
					if( selection == null ){
						JOptionPane.showMessageDialog(currentWindow, 
								"At least one card must be selected to study");
						return;
					}
					String label = getLabelFromHashtable(selection);
					studyManager = new StudyManager(DataManager.this, 
							getCardsFromLabel(label, selection.get(label)), false);
				} else if( currentWindow instanceof EditorWindow ){
					Card[] card = { ((EditorWindow)currentWindow).card };
					studyManager = new StudyManager(DataManager.this, card, false);
				} else if( currentWindow instanceof ResultsWindow ){
					Card[] choice = ((ResultsWindow)currentWindow).restudy();
					if( choice != null )
						studyManager = new StudyManager(DataManager.this, choice, false);
				}
			}
		});
		actionMap.put("Next card", new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if( currentWindow instanceof StudyWindow ){
					studyManager.nextCard();
				} else if( currentWindow instanceof ResultsWindow ){
					((ResultsWindow)currentWindow).nextCard();
				}
			}
		});
		actionMap.put("Previous card", new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if( currentWindow instanceof StudyWindow ){
					studyManager.previousCard();
				} else if( currentWindow instanceof ResultsWindow ){
					((ResultsWindow)currentWindow).previousCard();
				}
			}
		});
		actionMap.put("Delete boxes", new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if( currentWindow instanceof EditorWindow ){
					((EditorWindow)currentWindow).deleteSelectedBox();
				}
			}
		});
		actionMap.put("Delete card", new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if( currentWindow instanceof BrowserWindow ){
					Hashtable<String, int[]> selection = ((BrowserWindow)currentWindow).getCurrentIndices();
					String curLabel = getLabelFromHashtable(selection);
					if( selection == null || selection.get(curLabel).length < 1 ){
						JOptionPane.showMessageDialog(currentWindow, 
								"At least one card must be selected for deletion");
						return;
					}
					deleteCards(getCardsFromLabel(curLabel, selection.get(curLabel)));
					((BrowserWindow)currentWindow).refresh();
				} else if( currentWindow instanceof EditorWindow ){
					Card[] cards = { ((EditorWindow)currentWindow).card };
					deleteCards(cards);
					loadNewWindow(new BrowserWindow(DataManager.this));
				}
			}
		});
		actionMap.put("Save card", new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if( currentWindow instanceof EditorWindow ){
					((EditorWindow)currentWindow).save();
				}
			}
		});
		actionMap.put("Edit card", new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if( currentWindow instanceof BrowserWindow ){ 
					Hashtable<String, int[]> selection = ((BrowserWindow)currentWindow).getCurrentIndices();
					String curLabel = getLabelFromHashtable(selection); //TODO this is shitty
					if( selection == null || selection.get(curLabel).length != 1 ){
						JOptionPane.showMessageDialog(currentWindow, 
								"Exactly one card must be selected for editing");
						return;
					}
					int curIndex = selection.get(curLabel)[0];
					Integer curId = labels.get(curLabel).get(curIndex);
					loadNewWindow(new EditorWindow(DataManager.this, allCards.get(curId)));
				} else if( currentWindow instanceof StudyWindow ){
					studyManager = null;
					loadNewWindow(new EditorWindow(DataManager.this, ((StudyWindow)currentWindow).card));
				} else if( currentWindow instanceof ResultsWindow ){
					loadNewWindow(new EditorWindow( DataManager.this, 
							((ResultsWindow)currentWindow).getCurrentCard() ));
				}
			}
		});
		actionMap.put("Edit card info", new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if( currentWindow instanceof BrowserWindow ){
					Hashtable<String, int[]> selection = ((BrowserWindow)currentWindow).getCurrentIndices();
					String curLabel = getLabelFromHashtable(selection);
					if( selection == null || selection.get(curLabel).length == 0 ){
						JOptionPane.showMessageDialog(currentWindow, 
								"At least one card must be selected to edit");
						return;
					}
					int[] curIndices = selection.get(curLabel);
					Card[] cardsToEdit = getCardsFromLabel(curLabel, curIndices);
					Card[] cardsToSave = showEditInfoDialog(cardsToEdit);
					for( Card c : cardsToSave ){
						try {
							c.writeToFile();
						} catch (FileNotFoundException e1) {
							JOptionPane.showMessageDialog(currentWindow, 
									"Problem writing file for card " + c.name);
							e1.printStackTrace();
						}
					}
					((BrowserWindow)currentWindow).refresh();
				} else if( currentWindow instanceof EditorWindow ){
					EditorWindow win = (EditorWindow)currentWindow;
					win.card = showEditInfoDialog(win.card);
					win.changeMade();
				}
			}
		});
		actionMap.put("New card", new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String name;
				do {
					name = JOptionPane.showInputDialog("Name for new card: \n(don't use: _, /, \\, *, or .)");
					if( name == null ) return;
				} while( !Card.isNameValid(name) );
				loadNewWindow(new EditorWindow(DataManager.this, createNewCard(name), true));
			}
		});
		actionMap.put("Browser", new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				loadNewWindow(new BrowserWindow(DataManager.this));
			}
		});
	}
	
	private void showAboutDialog() {
		JLabel message = new JLabel("<html><div>"
				+ "<h3>Atlas 1.0</h3>"
				+ "<p>written by Sam Bedell, 2015</p>"
				+ "</div></html>");
		JOptionPane.showMessageDialog(currentWindow, 
				message, "About Atlas", JOptionPane.PLAIN_MESSAGE);
	}
	
	/**
	 * Removes/replaces the accel key binding for toolbar/menu actions if they key matches
	 * one given. If true is passed for enabled, 'keys' is not checked and may be null
	 * @param keys KeyStrokes to remove as accelerators
	 * @param enabled disable or enable accelerator functionality
	 */
	public void setAccelsEnabled(ArrayList<KeyStroke> keys, boolean enabled) {
		/* TODO this method of enabling will only work if there are no simple single key accels in the original package,
		 *  i.e. for this function to mean anything, the user has written preferences that will be restored
		 *  with loadPreferences 
		 */
		if( enabled ){
			loadPreferences();
			return;
		}
		for( KeyStroke accel : keys ){
			//char accelChar = Character.toUpperCase(accel.getKeyChar());
			for( Object key : actionMap.allKeys() ){
				KeyStroke actionStroke = (KeyStroke)actionMap.get(key).getValue(Action.ACCELERATOR_KEY);
				if( actionStroke == null || actionStroke.getModifiers() != 0 ) continue;
				//char actionChar = (char)actionStroke.getKeyCode();
				//if( accelChar == actionChar )
				//	actionMap.get(key).putValue(Action.ACCELERATOR_KEY, null);
				if( actionStroke.equals(accel) )
					actionMap.get(key).putValue(Action.ACCELERATOR_KEY, null);
			}
		}
	}
	
	/**
	 * load keystroke preferences from file
	 */
	private void loadPreferences() {
		try( BufferedReader reader = new BufferedReader( new FileReader(
					new File(START_DIRECTORY.getPath(), ".preferences"))) ){
			String line;
			while( (line = reader.readLine()) != null ){
				String[] lineParts = line.split("::");
				Action action = actionMap.get(lineParts[0]);
				if( action != null ){
					KeyStroke key = KeyStroke.getKeyStroke(lineParts[1]);
					action.putValue(Action.ACCELERATOR_KEY, key);
					action.putValue(Action.SHORT_DESCRIPTION, 
							action.getValue(Action.NAME) + " " + PreferencesWindow.createAccelString(key));
				}
			}
		} catch (FileNotFoundException e) {
			System.err.println("preferences file not found");
		} catch (IOException e) {
			System.err.println("problem reading preferences file");
		}
	}
	
	private void refreshToolbar() {
		toolbar.removeAll();
		GroupLayout layout = new GroupLayout(toolbar);
		toolbar.setLayout(layout);
		GroupLayout.SequentialGroup horizontal = layout.createSequentialGroup();
		GroupLayout.ParallelGroup vertical = layout.createParallelGroup();
		for( AbstractButton b : toolbarButtons ){
			horizontal.addComponent(b);
			vertical.addComponent(b);
		}
		layout.setHorizontalGroup(horizontal);
		layout.setVerticalGroup(vertical);
		toolbar.validate();
	}
	
	/** set frame header text */
	public void setTitle(String title) {
		frame.setTitle(title);
	}
	
	public void refreshBrowser() {
		if( !(currentWindow instanceof BrowserWindow) ) return;
		((BrowserWindow)currentWindow).refresh();
	}
	
	/**
	 * For use with BrowserWindow.getCurrentIndices()
	 * hashtable should have exactly one entry if non-null. This returns null if the hashtable is null,
	 * or the single key (label) if non-null
	 */
	private String getLabelFromHashtable(Hashtable<String, int[]> h) {
		if( h == null ) return null;
		return h.keySet().toArray(new String[0])[0];
	}
	
	/**
	 * Return a string representation of all labels and the associated card information for each.
	 * No dependencies; for development only
	 */
	public String printLabels() {
		String ret = "Labels:\n";
		for( String label : labels.keySet() ){
			ret += " label " + label + "\n";
			ArrayList<Integer> curIdSet = labels.get(label);
			for( Integer id : curIdSet ){
				ret += "  " + id.toString() + " " + allCards.get(id).name + "\n";
			}
		}
		return ret;
	}
	

}
