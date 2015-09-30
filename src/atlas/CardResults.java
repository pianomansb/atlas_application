package atlas;

import java.io.File;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.Date;
import java.util.Map.Entry;
import java.util.TreeMap;

public class CardResults {
	/* This tree maps a date to an array of string mappings. Each entry<string, string>
	 * is a pair of <expected answer, given answer>. Each array of entries is one 
	 * instance of a study/test.
	 */
	private final TreeMap<Long, ArrayList<SimpleImmutableEntry<String, String> > > data;
	//save a little time by caching recent answers
	private final ArrayList<SimpleImmutableEntry<String, String> > recentAnswers;
	//also cached
	private TreeMap<Date, Double> performance = null;
	private final Card card;

 	public CardResults(DataManager dm, Card card_) throws FileNotFoundException, IOException {
 		card = card_;
 		File file = new File(DataManager.START_DIRECTORY, card.id + "_results");
		
		data = initializeData(file, card);
		recentAnswers = data.get(data.lastKey());
	}
	
 	/** The most recent date on which this card was tested (in long form). */
 	public long getRecentDate() {
 		return data.lastKey();
 	}
 	
	/**
	 * returns percent of correct answers from most recent card test in form ##.##
	 */
	public double getRecentPercent() {
		double sum = 0;
		for( Entry<String, String> e : recentAnswers )
			sum += ( e.getKey().equals(e.getValue()) ) ? 1 : 0;
		int bigsum = (int)((sum / recentAnswers.size()) * 10000);
		return (double)bigsum / 100;
	}
	
	public int getRecentCorrect() {
		int ret = 0;
		for( Entry<String, String> e : recentAnswers )
			if( e.getKey().equals(e.getValue()) ) ++ret;
		return ret;
	}

	public int getRecentWrong() {
		int ret = 0;
		for( Entry<String, String> e : recentAnswers )
			if( !e.getKey().equals(e.getValue()) ) ++ret;
		return ret;
	}

	public int getRecentBlank() {
		int ret = 0;
		for( Entry<String, String> e : recentAnswers )
			if( e.getValue().isEmpty() ) ++ret;
		return ret;
	}
	
	private double getPercentOnDate(Long date) {
		double sum = 0;
		ArrayList< SimpleImmutableEntry<String, String> > answers = data.get(date);
		for( Entry<String, String> e : answers )
			sum += ( e.getKey().equals(e.getValue()) ) ? 1 : 0;
		int bigsum = (int)((sum / answers.size()) * 10000);
		return (double)bigsum / 100;
	}
	
	public int getCorrectOnDate(long date) {
		int ret = 0;
		ArrayList< SimpleImmutableEntry<String, String> > answers = data.get(date);
		for( Entry<String, String> e : answers )
			ret += ( e.getKey().equals(e.getValue()) ) ? 1 : 0;
		return ret;
	}
	
	public int getTotalOnDate(long date) {
		return data.get(date).size();
	}
	
	/**
	 * Return the dates (as longs of milliseconds) this card was tested.
	 * This method makes no guarantee about the order of the dates returned. 
	 * @return array of dates contained by this results object
	 */
	public long[] getDates() {
		ArrayList<Long> dates = new ArrayList<Long>(data.keySet());
		long[] ret = new long[dates.size()];
		for( int i=0; i < ret.length; ++i )
			ret[i] = dates.get(i).longValue();
		return ret;
	}
	
	/**
	 * returns a treemap of dates mapped to the percent correct on that date
	 */
	public TreeMap<Date, Double> getPerformanceOverTime() {
		if( performance != null ) return performance;
		performance = new TreeMap<Date, Double>();
		for( Long l : data.keySet() ){
			performance.put(new Date(l), getPercentOnDate(l));
		}
		return performance;
	}
	
	public String getCardName() {
		return card.name;
	}

	public Card getCard() {
		return card;
	}

	private TreeMap<Long, ArrayList<SimpleImmutableEntry<String, String> > > initializeData(File file, Card card) 
			throws FileNotFoundException, IOException {
		TreeMap<Long, ArrayList<SimpleImmutableEntry<String, String> > > tempdata = 
				new TreeMap<Long, ArrayList<SimpleImmutableEntry<String, String> > >();
		BufferedReader reader = new BufferedReader( new FileReader(file));
		String line;
		while( (line = reader.readLine()) != null ){
			Long time = Long.parseLong(line);
			ArrayList<SimpleImmutableEntry<String, String> > timedata = 
					new ArrayList<SimpleImmutableEntry<String, String> >();
			while( !(line = reader.readLine()).equals("==========") ){
				String[] answers = line.split("::");
				if( answers.length == 1 ){ //second answer was blank
					timedata.add( new SimpleImmutableEntry<String, String>(answers[0], "") );
				} else {
					timedata.add( new SimpleImmutableEntry<String, String>(answers[0], answers[1]) );
				}
			}
			tempdata.put(time, timedata);
		}
		reader.close();
		return tempdata;
	}
	
}
