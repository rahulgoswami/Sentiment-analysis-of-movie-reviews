/*
 * Author: Rahul Goswami
 */
package com.dm.sentiment;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.dm.sentiment.config.Config;
import com.dm.sentiment.util.DMUtil;

import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.ling.Sentence;
import edu.stanford.nlp.ling.TaggedWord;
import edu.stanford.nlp.tagger.maxent.MaxentTagger;

public class SentimentClassifier {
	//private Map<String,Double> adjectiveMap;
	private Map<String,Double> swnDict;
	MaxentTagger tagger;
	public SentimentClassifier() throws Exception{
		
		try{
			SentiWordNetDemoCode swn=new SentiWordNetDemoCode(Config.SENTI_WORDNET_DICTIONARY_PATH);
			swnDict=swn.getDictionary();
			tagger = new MaxentTagger("taggers\\bidirectional-distsim-wsj-0-18.tagger");
		//	adjectiveMap=DMUtil.getAdjectiveMap(swnDict);
		}
		catch(Exception ex){
			ex.printStackTrace();
			System.out.println("=========SentiWordNet Dictionary creation failed!!!=========");
			throw ex;
		}
		
	}
	
	public Map<String,List<String>> getClassificationForDataSet(String folderPath) throws IOException{
		String filePath;
		String classification;
		Map<String,List<String>> classificationMap=new HashMap<String,List<String>>();
		List<String> posClassFileList=new ArrayList<String>();
		List<String> negClassFileList=new ArrayList<String>();
		
		classificationMap.put(Config.POS_CLASSNAME,posClassFileList);
		classificationMap.put(Config.NEG_CLASSNAME,negClassFileList);
		
		File folder = new File(folderPath);
		File[] listOfFiles = folder.listFiles();
		
		for(int i=0; i<listOfFiles.length; i++){
			if (listOfFiles[i].isFile()) {
		        filePath=folderPath+"\\"+listOfFiles[i].getName();
		        classification=getClassification(filePath);
		        classificationMap.get(classification).add(listOfFiles[i].getName());
		      }
		}
		return classificationMap;
	}	
		/*    for (int i = 0; i < listOfFiles.length; i++) {
		      if (listOfFiles[i].isFile()) {
		        System.out.println("File " + listOfFiles[i].getName());
		      } else if (listOfFiles[i].isDirectory()) {
		        System.out.println("Directory " + listOfFiles[i].getName());
		      }
		    }
		*/
		//getClassification();
	
/*	public static void main(String[] args){
		getClassificationForDataSet(Config.folderPath);
	}
	*/

	public String getClassification(String filePath) throws IOException {
		// TODO Auto-generated method stub
		List<String> nGramsList=new ArrayList<String>();
		String taggedText="";
		try{
			

			/*       // The sample string
        String sample = "This is a sample text";

        // The tagged string
        String tagged = tagger.tagString(sample);
			 */
			@SuppressWarnings("unchecked")
			List<List<HasWord>> sentences = tagger.tokenizeText(new BufferedReader(new FileReader(filePath)));
			for (List<HasWord> sentence : sentences) {
				ArrayList<TaggedWord> tSentence = tagger.tagSentence(sentence);
				taggedText+=" "+Sentence.listToString(tSentence, false);
			}
			taggedText=taggedText.replace("/", "_");
			DMUtil.writeData(Config.TAGGED_TEXT_FILE_PATH,taggedText);
		}
		catch(Exception ex){
			ex.printStackTrace();
			System.out.println("=======Error tagging file "+filePath);
		}
		
		List<String> tokenList=DMUtil.tokenize(Config.TAGGED_TEXT_FILE_PATH, Config.DELIMITERS);
		tokenList=DMUtil.sanitizeList(tokenList);
/*		
		if(Config.NGRAM_MODE.equals("tri")){
			List<String> bigrams=addBigrams(tokenList);
			List<String> trigrams=addTrigrams(tokenList);

			for(String token: bigrams){
				tokenList.add(token);
			}

			for(String token: trigrams){
				tokenList.add(token);
			}
		}
*/		
		//	DMUtil.writeData("temp.txt", tokenList.toString());
		Double score = getSentimentScore(tokenList);
		/*
		 * Since we feel SentiWordNet is slightly biased towards positive words
		 * based on the number of false positives, a score of 0 is classified as negative
		 * (as a balancing factor)
		 */
		if(score<=0.25){
			return Config.NEG_CLASSNAME;
		}
		else
			return Config.POS_CLASSNAME;
	}

	private List<String> addTrigrams(List<String> tokenList) {
		// TODO Auto-generated method stub
		int len=tokenList.size();
		List<String> triGramsList=new ArrayList<String>();
		for(int i=0;i<len-2;i++){
			String trigram=""+tokenList.get(i)+"_"+tokenList.get(i+1)+"_"+tokenList.get(i+2);
			triGramsList.add(trigram);
		}
		return triGramsList;
	}

	private List<String> addBigrams(List<String> tokenList) {
		// TODO Auto-generated method stub
		int len=tokenList.size();
		List<String> biGramsList=new ArrayList<String>();
		for(int i=0;i<len-1;i++){
			String bigram=""+tokenList.get(i)+"_"+tokenList.get(i+1);
			biGramsList.add(bigram);
		}
		return biGramsList;
	}

	private Double getSentimentScore(List<String> tokenList) {
		// TODO Auto-generated method stub
		Double score=0D;
		Map<String,String> tokenPOSMap=new HashMap<String,String>();
		for(String token : tokenList){
			String newToken;
			String POS=getPOS(token);
			if(token.indexOf("_")!=-1 && token.indexOf("_")!=0){
				newToken=token.substring(0,token.indexOf("_"));
				newToken=newToken.trim();
				//System.out.println(newToken);
				switch(POS)
				{
				case Config.ADJ:
					if(swnDict.get(newToken+"#a")!=null){
						score+=swnDict.get(newToken+"#a");
					}
					break;
				case Config.NOUN:
					if(swnDict.get(newToken+"#n")!=null){
						score+=swnDict.get(newToken+"#n");
					}
					break;
				case Config.ADV:
					if(swnDict.get(newToken+"#r")!=null){
						score+=swnDict.get(newToken+"#r");
					}
					break;
				case Config.VERB:
					if(swnDict.get(newToken+"#v")!=null){
						score+=swnDict.get(newToken+"#v");
					}
					break;
				default:
					continue;
				}
			}
			else
				continue;
		}
		return score;
	}
/*
		for(String token:tokenList){
			System.out.println(token);
			
			double tokenScore=0D;
			int POSCount=0;
			if(swnDict.get(token+"#a")!=null){
				tokenScore+=swnDict.get(token+"#a");
				POSCount++;
			}
			
			if(Config.POS_MODE.equals("all")){
				if(swnDict.get(token+"#n")!=null){
					tokenScore+=swnDict.get(token+"#n");
					POSCount++;
				}
				if(swnDict.get(token+"#r")!=null){
					tokenScore+=swnDict.get(token+"#r");
					POSCount++;
				}
				if(swnDict.get(token+"#v")!=null){
					tokenScore+=swnDict.get(token+"#v");
					POSCount++;
				}
			}
			
			//Take the average of a word's sentiment score across all POS 
			if(POSCount!=0)
				score+=tokenScore/POSCount;

		}
*/
	
	
	
	
	private String getPOS(String token) {
		// TODO Auto-generated method stub
		if(token.contains("_JJ") || token.contains("_JJR")||token.contains("_JJS")){
			return Config.ADJ;
		}
		if(token.contains("_NN") || token.contains("_NNP")||token.contains("_NNPS")||token.contains("_NNS")){
			return Config.NOUN;
		}
		if(token.contains("_RB") || token.contains("_RBR")||token.contains("_RBS")||token.contains("_WRB")){
			return Config.ADV;
		}
		if(token.contains("_VB") || token.contains("_VBD")||token.contains("_VBG")||token.contains("_VBN")||token.contains("_VBP")||token.contains("_VBZ")){
			return Config.VERB;
		}
		return "none";
	}

	public static void main(String[] args){

//		String fileName="test_data.txt";
		String fileName=args[0];
		try{
			SentimentClassifier sc=new SentimentClassifier();
			System.out.println("The Sentiment is :"+sc.getClassification(fileName));
		}
		catch(Exception ex){
			ex.printStackTrace();
			System.out.println("Error classifying file: "+fileName);
		}
		
	}
 
}
