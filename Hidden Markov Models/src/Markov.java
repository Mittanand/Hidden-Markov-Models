import java.util.*;
import java.io.*;
/**
 * 
 * Creates HMM's and uses it to tag text in a sentence for its POS
 * @author Anand Mittal, PS5, CS10, Dartmouth 18F
 */

public class Markov {
	
	private Map<String, Map<String, Double>> transitions;	//map from tag to map of transitions
	private Map<String, Map<String, Double>> observations;	//map from tag to map of observations, might not be right
	private List<String> path;	//list of tags in sentence
	private List<Map<String, String>> backtrace;	//list of maps of best paths
	private int correct;	//counts number of tags correct
	private int incorrect;	//counts incorrect tags
	
	public Markov() {	//constructor
		transitions = new HashMap<String, Map<String, Double>>();
		observations = new HashMap<String, Map<String, Double>>();
		path = new ArrayList<String>();
		backtrace = new ArrayList<Map<String, String>>();
		correct = 0;
		incorrect = 0;
	}
	
	//path getter
	public List<String> getPath() {
		return path;
	}
	
	//method that turns file into list of arrays of strings for each line
	public List<String[]> makeList(BufferedReader input) throws IOException {
		List<String[]> val = new ArrayList<String[]>();	//make empty list of string arrays
		try {
			String line = input.readLine();	//read line
			while(line != null) {	//while things to read
				line = line.toLowerCase();	//make lowercase
				val.add(line.split(" "));	//add string array of line to val
				line = input.readLine();	//go to next line
			}
		}
		catch(IOException e) {
			System.out.println("Error reading input file");
		}
		return val;	//return arrayList
	}
	
	//method that creates HMM
	public void train(List<String[]> tags, List<String[]> text) {
		transitions.put("#", new HashMap<String, Double>());	//add # start and map to transitions
		for(int i = 0; i < tags.size(); i++) {	//loop through tags
			String[] tagLine = tags.get(i);	//set tagline equal to string array of tags
			String[] wordLine = text.get(i);	//set wordline equal to string array of text
			if(transitions.get("#").containsKey(tagLine[0])) transitions.get("#").put(tagLine[0], transitions.get("#").get(tagLine[0])+1.0);
			else transitions.get("#").put(tagLine[0], 1.0);	//add first element to map
			
			for(int a = 0; a < wordLine.length; a++) {
				if(!observations.containsKey(tagLine[a])) observations.put(tagLine[a], new HashMap<String, Double>());	//put new tags into observations
				if(!observations.get(tagLine[a]).containsKey(wordLine[a])) observations.get(tagLine[a]).put(wordLine[a], 1.0);	//put word if doesn't exist
				else observations.get(tagLine[a]).put(wordLine[a], observations.get(tagLine[a]).get(wordLine[a])+1.0);	//increment word if it does exist
			}
			for(int b = 0; b < tagLine.length - 1; b++) {	//loop through again, don't hit last element because no transition
				String currKey = tagLine[b];
				String nextKey = tagLine[b+1];
				if(!transitions.containsKey(currKey)) transitions.put(currKey, new HashMap<String, Double>());	//add tag if doesn't exist
				if(!transitions.get(currKey).containsKey(nextKey)) transitions.get(currKey).put(nextKey, 1.0);	//add transition if doesn't exist
				else transitions.get(currKey).put(nextKey, transitions.get(currKey).get(nextKey)+1.0);	//add transition if doesn't exist
			}
		}
		
		for(String tag: transitions.keySet()) {	//loop through tags in transitions
			double count = 0; 	//variable counting freq of transition between two tags
			for(String next: transitions.get(tag).keySet()) {
				count+=transitions.get(tag).get(next);	//increment count
			}
			for(String next: transitions.get(tag).keySet()) {
				transitions.get(tag).put(next, Math.log(transitions.get(tag).get(next)/count));	//take average by dividing by count
			}
		}
		for(String tag: observations.keySet()) {	//do same average with observation values
			double count = 0; 
			for(String next: observations.get(tag).keySet()) {
				count+=observations.get(tag).get(next);
			}
			for(String next: observations.get(tag).keySet()) {
				observations.get(tag).put(next, Math.log(observations.get(tag).get(next)/count));
			}
		}
	}
	
	//tags words in file
	public void tag(String[] file) {
		Map<String, Double> currScores = new HashMap<String, Double>();	//make containing current scores
		backtrace = new ArrayList<Map<String, String>>();
		currScores.put("#", 0.0);	//put initial value into curr scores}
		for(int i = 0; i < file.length; i++) {	//go through file
			backtrace.add(new HashMap<String, String>());		
			Map<String, Double> nextScores = new HashMap<String, Double>();	//map containing best next scores
			for(String currKey: currScores.keySet()) {	//traverse through curr scores
				if(transitions.containsKey(currKey)) {	//if transitions contains currKey
					for(String nextKey: transitions.get(currKey).keySet()) {	//traverse through possible next scores						
						double score = currScores.get(currKey)+transitions.get(currKey).get(nextKey);	//get total score
						if(observations.get(nextKey).containsKey(file[i])) score += observations.get(nextKey).get(file[i]);	//add observations if exists
						else score-=100.0;	//otherwise add -100
						//System.out.println("Score: "+score);
						if((!nextScores.containsKey(nextKey) || score > nextScores.get(nextKey)) ) {	//if nextScores doesn't have nextKey or it's score is greater, add it
							nextScores.put(nextKey, score);		//put in nextScores map
							backtrace.get(i).put(nextKey, currKey);	//add to backtrace
						}
					}
				}
			}
			currScores = nextScores;	//increment currScores
		}
		
		double largestScore = -1000000;	//initialize to smallest value so no larger values are excluded
		String largestKey = "";	//empty String holding largest Key
		for(String key: currScores.keySet()) {	//go through currScores map, which is last map
			if(currScores.get(key) > largestScore) {	//get highest score and tag
				largestScore = currScores.get(key);
				largestKey = key;
			}
		}
		path = new ArrayList<String>();
		for(int i = file.length-1; i>=0; i--) {
			path.add(0, largestKey);	//add highest tag
			largestKey = backtrace.get(i).get(largestKey);	//go backwards and get next highest tag
		}
	}
	
	//method to tag input by user based on training data
	public void console() {
		 Scanner in = new Scanner(System.in);	//new scanner
		 System.out.println("Enter a sentence: ");	//prompt
		 String sentence = in.nextLine();	//read input
		 String[] words = sentence.split(" ");	//make input array of strings
		 
		 tag(words);	//call tag, creates path arraylist of predicted tags
		 List<String> path = getPath();		//set equal to path
		 System.out.println("Tags are: "+path);	//print path
	}
	
	//method showing number of correct and incorrect tags
	public void compare(String[] realTags) {
		for(int i = 0; i < realTags.length; i++) {	//traverse correct tags
			if(realTags[i].toLowerCase().equals(path.get(i))) correct++;	//if equal to obtained tag, increment correct
			else incorrect++;	//else increment incorrect
		}
	}
	
	//method returning accuracy of predicted tags
	public void accuracy(List<String[]> sentence, List<String[]> realTags) {
		for(int i = 0; i < sentence.size(); i++) {	//loop through list of sentences 
			tag(sentence.get(i));	//tag sentence
			compare(realTags.get(i));	//compare sentence, setting correct and incorrect
		}
		System.out.println("Correct: "+correct);	//print values
		System.out.println("Incorrect: "+incorrect);
		System.out.println((double)correct/(incorrect+correct)*100+"%");
	}
	
	
	public static void main(String[] args) {
		try {
			Markov m = new Markov();
			BufferedReader bTags = new BufferedReader(new FileReader("text-files/brown-train-tags.txt"));
			BufferedReader bText = new BufferedReader(new FileReader("text-files/brown-train-sentences.txt"));
			BufferedReader bFile = new BufferedReader(new FileReader("text-files/brown-test-sentences.txt"));
			BufferedReader bActualTags = new BufferedReader(new FileReader("text-files/brown-test-tags.txt"));
			
			
			List<String[]> bTrainTags = m.makeList(bTags);
			List<String[]> bTrainText = m.makeList(bText);
			List<String[]> bSentence = m.makeList(bFile);
			List<String[]> bRealTags = m.makeList(bActualTags);

			m.train(bTrainTags, bTrainText);
			m.accuracy(bSentence, bRealTags);
			System.out.println();
			
			Markov m2 = new Markov();
			BufferedReader sTags = new BufferedReader(new FileReader("text-files/simple-train-tags.txt"));
			BufferedReader sText = new BufferedReader(new FileReader("text-files/simple-train-sentences.txt"));
			BufferedReader sFile = new BufferedReader(new FileReader("text-files/simple-test-sentences.txt"));
			BufferedReader sActualTags = new BufferedReader(new FileReader("text-files/simple-test-tags.txt"));
			
			List<String[]> sTrainTags = m.makeList(sTags);
			List<String[]> sTrainText = m.makeList(sText);
			List<String[]> sSentence = m.makeList(sFile);
			List<String[]> sRealTags = m.makeList(sActualTags);

			m2.train(sTrainTags, sTrainText);
			m2.accuracy(sSentence, sRealTags);
			System.out.println();
			
			Markov m3 = new Markov();
			BufferedReader tTags = new BufferedReader(new FileReader("text-files/test1-train-tags.txt"));
			BufferedReader tText = new BufferedReader(new FileReader("text-files/test1-train-sentences.txt"));
			BufferedReader tFile = new BufferedReader(new FileReader("text-files/test1-test-sentences.txt"));
			BufferedReader tActualTags = new BufferedReader(new FileReader("text-files/test1-test-tags.txt"));
			
			List<String[]> tTrainTags = m.makeList(tTags);
			List<String[]> tTrainText = m.makeList(tText);
			List<String[]> tSentence = m.makeList(tFile);
			List<String[]> tRealTags = m.makeList(tActualTags);

			m3.train(tTrainTags, tTrainText);
			m3.accuracy(tSentence, tRealTags);
			System.out.println();

			Markov m4 = new Markov();
			BufferedReader t2Tags = new BufferedReader(new FileReader("text-files/test2-train-tags.txt"));
			BufferedReader t2Text = new BufferedReader(new FileReader("text-files/test2-train-sentences.txt"));
			BufferedReader t2File = new BufferedReader(new FileReader("text-files/test2-test-sentences.txt"));
			BufferedReader t2ActualTags = new BufferedReader(new FileReader("text-files/test2-test-tags.txt"));
			
			List<String[]> t2TrainTags = m.makeList(t2Tags);
			List<String[]> t2TrainText = m.makeList(t2Text);
			List<String[]> t2Sentence = m.makeList(t2File);
			List<String[]> t2RealTags = m.makeList(t2ActualTags);

			m4.train(t2TrainTags, t2TrainText);
			m4.accuracy(t2Sentence, t2RealTags);
			System.out.println();
			
			
//			Markov m5 = new Markov();
//			m5.train(bTrainTags, bTrainText);
//			m5.console();

		}
		catch(IOException e) {
			System.out.println("IO error");
		}
	}
}
