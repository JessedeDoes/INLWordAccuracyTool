/*
 * Note that GT is still assumed to be PAGE
 */
package org.impact.evaluation;

import org.impact.evaluation.parsers.ALTOParser;
import org.impact.evaluation.parsers.PageParser;
import org.impact.evaluation.parsers.hOCRParser;
import org.impact.evaluation.util.Options;


public class ALTOEvaluator extends PageEvaluator
{
	public ALTOEvaluator()
	{
		super();
		
		this.ocrDocumentType = new DocumentType();
		PageParser p = new ALTOParser();
		ocrDocumentType.parser = p;
		ocrDocumentType.extension = ".alto.xml";
	}
	
	public static void main(String [] args)
	{
		Options options = new Options(args);
		args = options.commandLine.getArgs();
		PageEvaluator pc = new ALTOEvaluator();
		
		String gtFile = args[0];
		String ocrFile = args[1];
		pc.processOptions();

		pc.compareFolders(gtFile,ocrFile);
		pc.calculateScores();		
		pc.marshal();
	}
}
