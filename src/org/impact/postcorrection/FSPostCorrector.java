package org.impact.postcorrection;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.impact.evaluation.*;
import org.impact.evaluation.parsers.ALTOParser;
import org.impact.evaluation.parsers.PageParser;
import org.impact.evaluation.parsers.PageXMLParser;
import org.impact.evaluation.parsers.hOCRParser;
import org.impact.evaluation.util.*;
import org.w3c.dom.Element;
/**
 * 
 * @author does
 * No serious postcorrection attempted.
 * Just to compare FS trick in external dictionary with postcorrection
 */

public class FSPostCorrector 
{
	WordList wordlist;
	SimpleTokenizer tokenizer = new SimpleTokenizer();
	CandidateGenerator candidateGenerator = new FSCandidateGenerator();
	Page currentPage;
	static int nChanges = 0;
	
	public FSPostCorrector(String wordlistFilename)
	{
		wordlist = new WordList(wordlistFilename);
	}
	
	public void correctPage(Page page)
	{
		currentPage = page;

		for (Word w: page.getWords())
		{
			correctWord(w, true);
		}
	}
	
	public void correctWord(Word w, boolean updateDocument)
	{
		tokenizer.tokenize(w.text);
		String t = tokenizer.trimmedToken;
		if (wordlist.getFrequency(t) > 0)
			return;
		
		Set<String> candidates = new HashSet<String>();
		candidateGenerator.addCandidates(candidates,t);
		
		boolean hasUpdate = false;
		
		for (String c: candidates)
		{	
			if (wordlist.getFrequency(c.toLowerCase()) > 0)
			{
				System.err.println("change " + nChanges++ + ": " + t + " --> " + c);
				hasUpdate = true;
				w.setText(tokenizer.prePunctuation + c + tokenizer.postPunctuation);
				break;
			}
		}
		
		if (hasUpdate && updateDocument)
		{
			currentPage.updateWord(w);
		}
	}

	
	public void correctFile(String inFile, String outFile)
	{
		PageParser parser;
		
		if (inFile.endsWith(".html"))
			parser = new hOCRParser();
		else if (inFile.contains("alto.xml"))
			parser = new ALTOParser();
		else
		{
			PageXMLParser x =  new PageXMLParser();
			x.useWordTags = true;
			parser = x;
		}
		
		try
		{
			Page pg = parser.parsePage(inFile);
			correctPage(pg);
			pg.saveDocumentToFile(outFile);
		} catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	public void correctFolder(String inFolder, String outFolder)
	{
		File inDir = new File(inFolder);

		if (!inDir.isDirectory()) // just compare files
		{
			correctFile(inFolder, outFolder);
		} else
		{
			File[] inList = inDir.listFiles();
			if (inList != null)
			{
				for (File f: inList)
				{
					correctFile(f.getAbsolutePath(), outFolder + "/" + f.getName());
				}
			}
		}
	}
	
	public static void main(String[] args)
	{
		if (args.length < 3)
		{
			System.err.println("At leat 3 arguments: <word list> <input files or folders> <output file or folder>");
		}
		
		FSPostCorrector p = new FSPostCorrector(args[0]);
		
		for (int i=1; i < args.length-1; i++)
		{
			p.correctFolder(args[i], args[args.length-1]);
		}
	}
}
