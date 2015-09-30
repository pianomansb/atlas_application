package atlas;

import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Hashtable;
import java.util.Map.Entry;
import java.util.TreeMap;

import javax.swing.DefaultListModel;
import javax.swing.GroupLayout;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

@SuppressWarnings("serial")
public class BrowserWindow extends JPanel {
	public final DataManager dataManager;
	private JList<String> decks, cards;
	private Graph labelGraph = new Graph();
	private Graph cardGraph;
	
	public BrowserWindow(DataManager dm) {
		dataManager = dm;
		dataManager.setTitle("Atlas Browser");
				
		decks = new JList<String>(new DefaultListModel<String>());
		decks.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		cards = new JList<String>(new DefaultListModel<String>());
		cards.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		cards.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if( e.getClickCount() == 2 ){
					dataManager.actionMap.get("Edit card").actionPerformed(
							new ActionEvent(e.getSource(), ActionEvent.ACTION_PERFORMED, "edit card") );
				}
			}
		});
		
		PicturePreviewComponent preview = new PicturePreviewComponent();
		JScrollPane cardsHolder = new JScrollPane(cards);
		JScrollPane decksHolder = new JScrollPane(decks);
		JScrollPane cardGraphPane = new JScrollPane();
		cardGraph = new Graph(cardGraphPane);
		cardGraphPane.setViewportView(cardGraph);
		JScrollPane labelGraphPane = new JScrollPane(labelGraph);
		
		GroupLayout layout = new GroupLayout(this);
		setLayout(layout);

		layout.setHorizontalGroup(layout.createSequentialGroup()
			.addGroup(layout.createParallelGroup()
				.addComponent(decksHolder)
				.addComponent(preview)
			).addGroup(layout.createParallelGroup()
				.addComponent(cardsHolder)
				.addComponent(cardGraphPane)
			).addComponent(labelGraphPane)
		);
		
		layout.setVerticalGroup(layout.createParallelGroup()
			.addGroup(layout.createSequentialGroup()
				.addComponent(decksHolder)
				.addComponent(preview)
			).addGroup(layout.createSequentialGroup()
				.addComponent(cardsHolder)
				.addComponent(cardGraphPane)
			).addComponent(labelGraphPane)
		);

		decks.addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent e) {
				String selection = decks.getSelectedValue();
				DefaultListModel<String> model = (DefaultListModel<String>)cards.getModel();
				if( e.getValueIsAdjusting() == false && selection != null){
					model.removeAllElements();
					String[] cards = dataManager.getCardNamesForLabel(selection);
					for( String name : cards ) model.addElement(name);
					labelGraph.setData(updateLabelGraphData(selection));
				} else if( selection == null ){
					model.removeAllElements();
				}
			}
		});
		
		cards.addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent e) {
				int index = cards.getSelectedIndex();
				if( index == -1 ) return;
				String label = decks.getSelectedValue();
				Card card = dataManager.allCards.get( dataManager.labels.get(label).get(index) );
				if( card.imageFilename != null )
					preview.setFile(card.getImageFile());
				else
					preview.setFile(null);
				cardGraph.setData(updateCardGraphData(card));
			}
		});
		
		refresh();
	}
	
	public void refresh() {
		DefaultListModel<String> model = (DefaultListModel<String>)decks.getModel();
		model.removeAllElements();
		String[] labels = dataManager.getLabels();
		Arrays.sort(labels);
		for( String s : labels ){
			model.addElement(s);
		}
		decks.setSelectedValue(Card.ALL, true);
	}
		
	/**
	 * Get the indices of the currently selected cards. 
	 * These must correspond to the indices in the label's array, since that's how the 
	 * cards are displayed. TODO: find times when the order of the cards might change between 
	 * the start of the execution of this function and the access of the cards by the manager
	 * @return A hashtable with one entry: a label name mapped to an array of indices 
	 * of the ArrayList<Integer> to which the label is mapped in the DataManager. 
	 * Return is null if there is no selection.
	 */
	public Hashtable<String, int[]> getCurrentIndices() {
		String curLabel = decks.getSelectedValue();
		int[] curIndices = cards.getSelectedIndices();
		if( curLabel == null || curIndices.length == 0 ) return null;
		Hashtable<String, int[]> ret = new Hashtable<String, int[]>();
		ret.put(curLabel, curIndices);
		return ret;
	}
	
	/**
	 * Returns data to be sent to a graph, or null if there are no results data for the card
	 */
	private TreeMap<Date,Double> updateCardGraphData(Card card) {
		try {
			CardResults results = new CardResults(dataManager, card);
			return results.getPerformanceOverTime();
		} catch (IOException e) {
			return null;
		}
		
	}
	
	/* The local variable data will be used to store the day (LocalDate)
	 * of any test event and an entry of <Total Correct, Total Attempted>
	 * for that day in any card in the current label
	 */
	private TreeMap<Date,Double> updateLabelGraphData(String label) {
		Card[] cards = dataManager.getCardsFromLabel(label);
		ArrayList<CardResults> results = new ArrayList<CardResults>();
		for( int i=0; i < cards.length; ++i ){
			try {
				results.add(new CardResults(dataManager, cards[i]));
			} catch (IOException e) {
				//TODO faster to check if file exists?
				//System.err.println("No results file for card " + cards[i].name);
				continue;
			}
		}
		
		TreeMap<LocalDate, SimpleEntry<Integer, Integer> > data = 
				getData(results);

		return parseDataForGraph(data);
	}
	
	private TreeMap<Date, Double> parseDataForGraph(
			TreeMap<LocalDate, SimpleEntry<Integer, Integer> > data) {
		
		TreeMap<Date, Double> ret = new TreeMap<Date, Double>();
		for( LocalDate ld : data.keySet() ){
			Date curDate = new Date(getMillisFromLocalDate(ld));
			Entry<Integer,Integer> curVals = data.get(ld);
			double curPercent = curVals.getKey().doubleValue() / curVals.getValue().intValue();
			curPercent = (double)((int)(curPercent * 10000)) / 100; //from .123456 -> 12.34
			ret.put(curDate, curPercent);
		}
		
		return ret;
	}
	
	/* Use LocalDate to normalize tests to the day (get rid of excess accuracy) 
	 */
	private TreeMap<LocalDate, SimpleEntry<Integer,Integer> > getData(ArrayList<CardResults> results) {
		TreeMap<LocalDate, SimpleEntry<Integer, Integer> > data = 
				new TreeMap<LocalDate, SimpleEntry<Integer,Integer> >();
		for( CardResults r : results ){
			long[] dates = r.getDates();
			for( long d : dates ){
				LocalDate date = getLocalDateForMillis(d);
				SimpleEntry<Integer, Integer> dataForDate = data.get(date);
				Integer curCorrect = (dataForDate == null) ? 0 : dataForDate.getKey();
				Integer curTotal = (dataForDate == null ) ? 0 : dataForDate.getValue();
				dataForDate = new SimpleEntry<Integer, Integer>(
						curCorrect + r.getCorrectOnDate(d), 
						curTotal + r.getTotalOnDate(d) );
				data.put(date, dataForDate);
			}
		}
		return data;
	}
	
	private long getMillisFromLocalDate(LocalDate date) {
		return ZonedDateTime.of(date, LocalTime.of(0, 0), ZoneId.systemDefault()).toInstant().toEpochMilli();
	}
	
	/** Converts a number of milliseconds from the Epoch to a LocalDate */
	private LocalDate getLocalDateForMillis(long millis) {
		return Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate();
	}
	
}
