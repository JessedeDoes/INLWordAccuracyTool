package org.impact.evaluation;

import org.impact.evaluation.parsers.PageParser;
import org.impact.evaluation.parsers.PageXMLParser;
import org.impact.evaluation.parsers.hOCRParser;
import org.impact.evaluation.util.Options;

public class hOCREvaluator extends PageEvaluator 
{
	public hOCREvaluator()
	{
		super();
		
		this.ocrDocumentType = new DocumentType();
		PageParser p = new hOCRParser();
		ocrDocumentType.parser = p;
		ocrDocumentType.extension = ".html";
		
		PageXMLParser p1 = new PageXMLParser();
		p1.useWordTags = true;
		this.gtDocumentType.parser = p1;
	}
	
	public static void main(String [] args)
	{
		
		Options options = new Options(args);
		args = options.commandLine.getArgs();
		PageEvaluator pc = new hOCREvaluator();
		
		String gtFile = args[0];
		String ocrFile = args[1];
		pc.processOptions();

		
		pc.compareFolders(gtFile,ocrFile);
		pc.calculateScores();		
		pc.marshal();
	}
}
