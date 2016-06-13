package org.impact.evaluation;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAnyElement;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.impact.evaluation.external.ExternalReport;
import org.impact.evaluation.metadata.KeyValuePair;
import org.impact.evaluation.metadata.MetadataProvider;

@XmlRootElement
public class PageReport 
{
	@XmlAttribute
	String id; // should be the prima id, except for KB files
	@XmlElement
	String imageFilename;
	@XmlElement
	int nWordsInOCR = 0;
	@XmlElement
	int nWordsInGT = 0;
	@XmlElement
	int nDictionaryWordsInGT =0;
	@XmlElement
	int nIncludedWords = 0;
	@XmlElement
	int nDiscardedWords = 0;
	@XmlElement
	int nRealWordErrors = 0;
	@XmlElement
	int nMissedDictionaryWords = 0;
	@XmlElement
	int nMissedGTWords = 0;
	@XmlElement
	int nCorrect = 0;
	@XmlElement
	int nWrong = 0;
	@XmlElement
	int nNEsInGT =0;
	@XmlElement
	int nNEsFound = 0;
	@XmlElement
	double precision = 0;
	@XmlElement
	double recall = 0;
	@XmlElement
	double strictPrecision=0;
	@XmlElement
	double dictionaryWordRecall=0;
	@XmlElement
	double NERecall;
	@XmlElement
	double dictionaryCoverage;
	@XmlElement
	int nPages=0;
	
	@XmlElement
	ExternalReport externalReport = null;

	@XmlAnyElement
	public List<Error> wordErrors = new ArrayList<Error>();
	MetadataProvider metadata=null;
	public List<Word> discardedWords = null;
	
	@XmlAnyElement
	public List<KeyValuePair> getMetadata()
	{
		if (metadata != null)
		{
			return metadata.getMetadata(this.id);
		}
		return null;
	}
	
	public void calculateScores()
	{

		double dWords = this.nWordsInOCR;
		this.precision = ((double) this.nCorrect) / dWords;
		this.strictPrecision = ((double) nCorrect) / nIncludedWords;
		dWords = this.nWordsInGT;
		this.recall = (this.nWordsInGT - this.nMissedGTWords) / dWords;
		double t = nNEsInGT;
		NERecall = (nNEsFound / t);
		dictionaryCoverage = this.nDictionaryWordsInGT / (double) this.nWordsInGT;
		if (this.nDictionaryWordsInGT > 0)
			dictionaryWordRecall = (this.nDictionaryWordsInGT - this.nMissedDictionaryWords) 
									/ (double) this.nDictionaryWordsInGT;
		if (this.externalReport != null)
			this.externalReport.calculateScore();
	}
	
	public void incorporate(PageReport p)
	{
		nWordsInOCR += p.nWordsInOCR;
		nIncludedWords += p.nIncludedWords;
		nDiscardedWords += p.nDiscardedWords;
		nRealWordErrors += p.nRealWordErrors;
		nDictionaryWordsInGT += p.nDictionaryWordsInGT;
		nMissedDictionaryWords += p.nMissedDictionaryWords;
		nWordsInGT += p.nWordsInGT;
		nMissedGTWords += p.nMissedGTWords;
		nCorrect += p.nCorrect;
		nWrong  += p.nWrong;
		nNEsInGT += p.nNEsInGT;
		nNEsFound += p.nNEsFound;
		if (this.externalReport != null && p.externalReport != null)
			this.externalReport.inCorporate(p.externalReport);
		nPages++;
	}
}
