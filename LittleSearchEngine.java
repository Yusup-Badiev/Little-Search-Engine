package lse;

import java.io.*;
import java.util.*;

/**
 * This class builds an index of keywords. Each keyword maps to a set of pages in
 * which it occurs, with frequency of occurrence in each page.
 *
 */
public class LittleSearchEngine {
	
	/**
	 * This is a hash table of all keywords. The key is the actual keyword, and the associated value is
	 * an array list of all occurrences of the keyword in documents. The array list is maintained in 
	 * DESCENDING order of frequencies.
	 */
	HashMap<String,ArrayList<Occurrence>> keywordsIndex;
	
	HashSet<String> noiseWords;
	
	public LittleSearchEngine() {
		keywordsIndex = new HashMap<String,ArrayList<Occurrence>>(1000,2.0f);
		noiseWords = new HashSet<String>(100,2.0f);
	}
	
	public HashMap<String,Occurrence> loadKeywordsFromDocument(String docFile) 
	throws FileNotFoundException {
		if(!(new File(docFile).exists())) {
			throw new FileNotFoundException();
		}
		HashMap<String,Occurrence> re = new HashMap<String,Occurrence>();
		Scanner scan = new Scanner(new File(docFile));
		while(scan.hasNextLine()) {	
			String StrD = scan.nextLine();
			StringTokenizer tok = new StringTokenizer(StrD, " ");
			while(tok.hasMoreTokens()) {
				String Tstring = tok.nextToken();
				String keyWord = getKeyword(Tstring);
				if(keyWord != null) {
					if(re.containsKey(keyWord)) {
						re.get(keyWord).frequency += 1;
					}
					else {
						re.put(keyWord, new Occurrence(docFile,1));
					}
				}
			}
		}
		scan.close();
		return re;
	}
	
	/**
	 * Merges the keywords for a single document into the master keywordsIndex
	 * hash table. For each keyword, its Occurrence in the current document
	 * must be inserted in the correct place (according to descending order of
	 * frequency) in the same keyword's Occurrence list in the master hash table. 
	 * This is done by calling the insertLastOccurrence method.
	 * 
	 * @param kws Keywords hash table for a document
	 */
	public void mergeKeywords(HashMap<String,Occurrence> kws) {
		for(String key : kws.keySet()) {
			ArrayList<Occurrence> put = new ArrayList<Occurrence>();
			put.add(kws.get(key));
			if(keywordsIndex.get(key) == null) {
				keywordsIndex.put(key, put);
			}
			else{
				keywordsIndex.get(key).add(kws.get(key));
			}
			insertLastOccurrence(keywordsIndex.get(key));
		}
	}
	
	public String getKeyword(String word) {
		while(word.length() != 0 && (word.charAt(word.length()-1) == '.' || word.charAt(word.length()-1) == ',' || word.charAt(word.length()-1) == '!' || word.charAt(word.length()-1) == '?' || word.charAt(word.length()-1) == ':' || word.charAt(word.length()-1) == ';')) {
			word = word.substring(0, word.length()-1);
		}
		
		word = word.toLowerCase();
		
		if(word.length() == 0 || noiseWords.contains(word)) {
			return null;
		}
		
		if(word.matches("[a-z]*")) {
			return word;
		}
		
		return null;
	}
	
	/**
	 * Inserts the last occurrence in the parameter list in the correct position in the
	 * list, based on ordering occurrences on descending frequencies. The elements
	 * 0..n-2 in the list are already in the correct order. Insertion is done by
	 * first finding the correct spot using binary search, then inserting at that spot.
	 * 
	 * @param occs List of Occurrences
	 * @return Sequence of mid point indexes in the input list checked by the binary search process,
	 *         null if the size of the input list is 1. This returned array list is only used to test
	 *         your code - it is not used elsewhere in the program.
	 */

	public ArrayList<Integer> insertLastOccurrence(ArrayList<Occurrence> occs) {
		if(occs.size() == 1)
			return null;
		ArrayList<Integer> re = new ArrayList<Integer>();
		Occurrence holdOc = occs.get(occs.size()-1);
		occs.remove(occs.size()-1);
		int first = 0;
		int last = occs.size()-1;
		if(occs.size() == 1) {
			re.add(0);
			if(occs.get(0).frequency <= holdOc.frequency)
				occs.add(0,holdOc);
			else {
				occs.add(holdOc);
			}
			return re;
		}
		
		int middle;
		boolean go = true;
		while(go) {
			middle = ((last-first)/2) + first;
			re.add(middle);
			
			if(first >= last) {
				if(occs.get(middle).frequency >= holdOc.frequency) {
					occs.add(middle + 1, holdOc);
					break;
				}
				else {
					occs.add(middle, holdOc);
					break;
				}
			}
			
			if(occs.get(middle).frequency == holdOc.frequency) {
				occs.add(middle, holdOc);
				break;
			}
			
			else if(occs.get(middle).frequency > holdOc.frequency) {
				first = middle + 1;
			}
			
			else {
				last = middle - 1;
			}
		
		}
		return re;
	}
	
	/**
	 * This method indexes all keywords found in all the input documents. When this
	 * method is done, the keywordsIndex hash table will be filled with all keywords,
	 * each of which is associated with an array list of Occurrence objects, arranged
	 * in decreasing frequencies of occurrence.
	 * 
	 * @param docsFile Name of file that has a list of all the document file names, one name per line
	 * @param noiseWordsFile Name of file that has a list of noise words, one noise word per line
	 * @throws FileNotFoundException If there is a problem locating any of the input files on disk
	 */
	public void makeIndex(String docsFile, String noiseWordsFile) 
	throws FileNotFoundException {
		// load noise words to hash table for noiseWords 
		Scanner sc = new Scanner(new File(noiseWordsFile));
		while (sc.hasNext()) {
			String word = sc.next();
			noiseWords.add(word);
		}
		
		// index all keywords
		sc = new Scanner(new File(docsFile));
		while (sc.hasNext()) {
			String docFile = sc.next();
			HashMap<String,Occurrence> kws = loadKeywordsFromDocument(docFile); /*inputs document and returns all hey words**/
			mergeKeywords(kws);
		}
		sc.close();
	}
	
	/**
	 * Search result for "kw1 or kw2". A document is in the result set if kw1 or kw2 occurs in that
	 * document. Result set is arranged in descending order of document frequencies. (Note that a
	 * matching document will only appear once in the result.) Ties in frequency values are broken
	 * in favor of the first keyword. (That is, if kw1 is in doc1 with frequency f1, and kw2 is in doc2
	 * also with the same frequency f1, then doc1 will take precedence over doc2 in the result. 
	 * The result set is limited to 5 entries. If there are no matches at all, result is null.
	 * 
	 * @param kw1 First keyword
	 * @param kw1 Second keyword
	 * @return List of documents in which either kw1 or kw2 occurs, arranged in descending order of
	 *         frequencies. The result size is limited to 5 documents. If there are no matches, returns null.
	 */
	
	public ArrayList<String> top5search(String kw1, String kw2) {
		ArrayList<Occurrence> KW1 = new ArrayList<Occurrence>();
		ArrayList<Occurrence> KW2 = new ArrayList<Occurrence>();
		int i;
		ArrayList<String> re = new ArrayList<String>();
		if(keywordsIndex.get(kw1) != null) {
			for(i = 0; i < keywordsIndex.get(kw1).size() && i<5; i++) {
				KW1.add(keywordsIndex.get(kw1).get(i));
			}
		}
		
		if(keywordsIndex.get(kw2) != null) {
			for(i = 0; i < keywordsIndex.get(kw2).size() && i<5; i++) {
				if(keywordsIndex.get(kw2).get(i) != null) {
					KW2.add(keywordsIndex.get(kw2).get(i));
				}
			}
		}
		
		int Counter1 = 0;
		int Counter2 = 0; 
		while(re.size() != 5) {
			if(Counter1 == KW1.size()) {
				for(int j = Counter2; re.size() != 5 && j< KW2.size(); j++) {
					if(!(re.contains(KW2.get(j).document)))
						re.add(KW2.get(j).document);
				}
				break;
			}
			if(Counter2 == KW2.size()) {
				for(int j = Counter1; re.size() != 5 && j< KW1.size(); j++) {
					if(!(re.contains(KW1.get(j).document)))
						re.add(KW1.get(j).document);
				}
				break;
			}
			if(KW1.get(Counter1).frequency > KW2.get(Counter2).frequency) {
				if(!(re.contains(KW1.get(Counter1).document)))
					re.add(KW1.get(Counter1).document);
				Counter1++;
			}
			else if(KW1.get(Counter1).frequency < KW2.get(Counter2).frequency) {
				if(!(re.contains(KW2.get(Counter2).document)))
					re.add(KW2.get(Counter2).document);
				Counter2++;
			}
			else {
				if(!(re.contains(KW1.get(Counter1).document)))
					re.add(KW1.get(Counter1).document);
				Counter1++;
			}
		} 
		
		return re;
	}
}
