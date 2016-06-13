package org.impact.evaluation.external;

import org.impact.evaluation.Page;

public interface ExternalEvaluation 
{
	public ExternalReport evaluate(Page gtPage, Page ocrPage);
	public double getCharacterAccuracy(); 
	public int getNCharacterErrors();
	public int getNCharacters();
	public int getNWordErrors();
	public int getNWords();
	public double getWordAccuracy();
}
