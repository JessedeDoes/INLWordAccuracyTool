package org.impact.evaluation.external;

import org.impact.evaluation.Page;
import org.impact.evaluation.util.Options;
import org.impact.evaluation.util.ParseUtils;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

/*
 * Use Basilis' Tool per page for character error rate and "independent" word error rate
 * Results are fetched from the XML report generated by the tool
 * 
 * OCREval u8 - autf-8.txt butf-8.txt char.txt word.txt out.xml
 * OCREval u16 - batchgt.txt batch.txt char.txt word.txt out.xml
 * 
 */

public class NCSREvaluation implements ExternalEvaluation
{
	String programName = "OCREval.exe";
	String programDir = "C:/TR/IMPACT_EvalOcr";
	//String programDir = "D:/IMPACT_EvalOCR";
	String programPath; 
	
	XPathFactory xpathFactory = XPathFactory.newInstance();
	XPath xpath = xpathFactory.newXPath();
	
	
	XPathExpression accuracyXPath;
	XPathExpression wordAccuracyXPath;
	XPathExpression NCharactersXPath;
	XPathExpression NWordsXPath;
	XPathExpression NWordErrorsXPath;
	XPathExpression NCharacterErrorsXPath;
	XPathExpression WordErrorXPath;
	
	ExternalReport currentPageReport;
	
	Document resultDocument;
	
	double characterAccuracy;
	double wordAccuracy; 
	int NWords;
	int NCharacters;
	int NCharacterErrors;
	int NWordErrors;
	
	
	public NCSREvaluation()
	{
		String s;
		if ((s = Options.getOption("NCSREvaluationDir")) != null)
		{
			programDir = s;
		}
		programPath = programDir + "/" + programName;
		try
		{
			accuracyXPath = xpath.compile("//Character_Evaluation/Accuracy");
			wordAccuracyXPath = xpath.compile("//Word_Evaluation/WordAccuracy");
			NCharactersXPath = xpath.compile("//Character_Evaluation/Characters");
			NWordsXPath = xpath.compile("//Word_Evaluation/Words");
			NCharacterErrorsXPath =  xpath.compile("//Character_Evaluation/Errors");
			NWordErrorsXPath =  xpath.compile("//Word_Evaluation/Misrecognized");
			WordErrorXPath = xpath.compile("//Word_Evaluation//ConfusionNonStopword[@Missed>0]/@Word|//Word_Evaluation//ConfusionStopword[@Missed>0]/@Word");
		} catch (Exception e)
		{
			e.printStackTrace();
			//System.exit(1);
		}
	}
	
	public ExternalReport evaluate(Page gtPage, Page ocrPage)
	{
		gtPage.savePlainText(programDir + "/gt.tmp.txt");
		ocrPage.savePlainText(programDir + "/ocr.tmp.txt");
		currentPageReport = new ExternalReport();
		getEvaluationResults("gt.tmp.txt", "ocr.tmp.txt");
		return currentPageReport;
	}
	
	public List<String> getStrings(XPathExpression e)
	{
		List<String> strings = new ArrayList<String>();
		try 
		{
			NodeList l = (NodeList) e.evaluate(resultDocument,XPathConstants.NODESET);
			for (int i=0; i < l.getLength(); i++)
				strings.add(l.item(i).getNodeValue());
		} catch (XPathExpressionException e1) 
		{
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		return strings;
	}
	
	public double getDouble(XPathExpression e)
	{
		String s;
		try 
		{
			s = e.evaluate(resultDocument);
			return Double.parseDouble(s);
		} catch (XPathExpressionException e1) 
		{
			e1.printStackTrace();
		}
		return -1;
	}
	
	public int getInteger(XPathExpression e)
	{
		String s;
		try 
		{
			s = e.evaluate(resultDocument);
			return Integer.parseInt(s);
		} catch (XPathExpressionException e1) 
		{
			e1.printStackTrace();
		}
		return -1;
	}
	
	public Document getEvaluationResults(String gtFile, String ocrFile)
	{
		currentPageReport = new ExternalReport();
	    try
	    {
	    	try
	    	{
	    		new File(programDir + "/" + "NCSREvaluation.xml").delete();
	    	} catch (Exception e)
	    	{
	    		e.printStackTrace();
	    	}
	    	
	    	ProcessBuilder pb  = new ProcessBuilder(programPath, "u8", "-", 
	    			gtFile, ocrFile, "NCSRchar.txt", "NCSRword.txt", "NCSREvaluation.xml");

	    	Map<String, String> env = pb.environment();
	    	//env.put("PATH", programDir + "/");

	    	pb.directory(new File(programDir));
	    	pb.redirectErrorStream(true);

	    	final Process process = pb.start();


	    	new Thread(new Runnable() 
	    	{
	    	    @Override public void run() 
	    	    {
	    	        try 
	    	        {
	    	        	String line;
	    	        	final InputStream stdout = process.getInputStream ();
	    	    		BufferedReader brCleanUp = new BufferedReader (new InputStreamReader (stdout));
	    	    		while ((line = brCleanUp.readLine ()) != null) 
	    	    		{
	    	    			System.err.println ("[Stdout] " + line);
	    	    		}
	    	    		brCleanUp.close();
	    	        } catch (IOException e) 
	    	        {
	    	            e.printStackTrace(System.err);
	    	        }
	    	    }    
	    	}).start();
	    	
	    	process.waitFor();
	    	
	    	resultDocument = ParseUtils.parse(programDir + "/NCSREvaluation.xml");
	    	
	    	currentPageReport.characterAccuracy = getDouble(accuracyXPath);
	    	currentPageReport.wordAccuracy = getDouble(wordAccuracyXPath);
	    	currentPageReport.NCharacterErrors =  getInteger(NCharacterErrorsXPath);
	    	currentPageReport.NWordErrors =  getInteger(NWordErrorsXPath);
	    	currentPageReport.NWords = getInteger(NWordsXPath);
	    	currentPageReport.NCharacters =  getInteger(NCharactersXPath);
	    	currentPageReport.missedWords = getStrings(WordErrorXPath);
	    	
	    	// System.err.println("exit value: " + process.exitValue());
	    } catch (Exception e)
	    {
	    	e.printStackTrace();
	    }
	    return resultDocument;
	}

	public static void main(String[] args)
	{
		NCSREvaluation e = new NCSREvaluation();
		
		e.getEvaluationResults(e.programDir + "/autf-8.txt", e.programDir + "/butf-8.txt");
		System.out.println(e.characterAccuracy);
	}

	@Override
	public double getCharacterAccuracy() 
	{
		// TODO Auto-generated method stub
		return currentPageReport.characterAccuracy;
	}

	@Override
	public double getWordAccuracy() 
	{
		// TODO Auto-generated method stub
		return currentPageReport.wordAccuracy;
	}

	@Override
	public int getNCharacterErrors() {
		// TODO Auto-generated method stub
		return currentPageReport.NCharacterErrors;
	}

	@Override
	public int getNCharacters() {
		// TODO Auto-generated method stub
		return currentPageReport.NCharacters;
	}

	@Override
	public int getNWordErrors() 
	{
		// TODO Auto-generated method stub
		
		return currentPageReport.NWordErrors;
	}

	@Override
	public int getNWords() {
		// TODO Auto-generated method stub
		return currentPageReport.NWords;
	}
}
