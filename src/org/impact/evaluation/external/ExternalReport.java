package org.impact.evaluation.external;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class ExternalReport 
{
	@XmlElement
	double characterAccuracy;
	@XmlElement
	double wordAccuracy;
	@XmlElement
	int NWords;
	@XmlElement
	int NCharacters;
	@XmlElement
	int NCharacterErrors;
	@XmlElement
	int NWordErrors;
	@XmlElement
	List<String> missedWords = new ArrayList<String>();
	
	public void inCorporate(ExternalReport other)
	{
		this.NWords += other.NWords;
		this.NCharacters += other.NCharacters;
		this.NCharacterErrors += other.NCharacterErrors;
		this.NWordErrors += other.NWordErrors;
	}
	
	public void calculateScore()
	{
		this.characterAccuracy = (NCharacters - NCharacterErrors) / (double) NCharacters;
		this.wordAccuracy =  (NWords - NWordErrors) / (double) NWords;
	}
}
