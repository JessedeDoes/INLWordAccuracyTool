package org.impact.evaluation;

import java.awt.Rectangle;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.impact.evaluation.parsers.AbbyyXMLParser;
import org.impact.evaluation.parsers.PageXMLParser;

public class BoxTifExporter 
{
	public void exportBoxTif(Page page)
	{
		
	}
	
	public void exportLongLexiconWordsAsBoxTif(WordList lexicon, Page ocrPage, Writer out)
	{
		int h = ocrPage.height;
		int threshold = 1;
		int minLength = 5;
		for (Word w: ocrPage.getWords())
		{
			//System.err.println(w.tokenizedText);
			if (w.tokenizedText.length() > minLength && lexicon.getFrequency(w.tokenizedText) > threshold)
			{	
				System.err.println("OK: "  + w.tokenizedText);			
				printGlyphs(h, out, w);
			}
		}
		
	}
	
	public void exportLongLexiconWordsAsBoxTif(String lexiconFile, String ocrFile, String outFile)
	{
		WordList lexicon = new WordList(lexiconFile);
		PageXMLParser p = new PageXMLParser();
		p.useWordTags = true;
		Page ocrPage = p.parsePage(ocrFile);
		try 
		{
			Writer out = new OutputStreamWriter(new FileOutputStream(outFile), "UTF-8");
			exportLongLexiconWordsAsBoxTif(lexicon, ocrPage, out);
			out.flush();
			out.close();
		} catch (UnsupportedEncodingException e) 
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (FileNotFoundException e) 
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	public static void stripInvalidWords(Page gtPage, Page ocrPage, Writer out)
	{
		PageEvaluator evaluator = new PageEvaluator();
		evaluator.caseSensitive = true;
		evaluator.comparePages(gtPage, ocrPage);
		Set<String> incorrect = new HashSet<String>();
		
		PageReport report = evaluator.pageReports.get(evaluator.pageReports.size()-1);
		
		for (Word w: report.discardedWords)
		{
			incorrect.add(w.id);
		}
		
		for (Error e: report.wordErrors)
		{
			for (Word w: e.ocr)
				incorrect.add(w.id);
		}
		List<Word> rejects = new ArrayList<Word>();
		
		for (Word w: ocrPage.getWords())
		{
			int h = gtPage.height;
			if (incorrect.contains(w.id))
			{	
				rejects.add(w);
			}
		}
		
		for (Word w: rejects)
		{
			ocrPage.removeWord(w);
		}
		
		
	}

	public void exportValidWordsAsBoxTif(Page gtPage, Page ocrPage, Writer out)
	{
		PageEvaluator evaluator = new PageEvaluator();
		evaluator.caseSensitive = true;
		evaluator.comparePages(gtPage, ocrPage);
		Set<String> incorrect = new HashSet<String>();
		
		PageReport report = evaluator.pageReports.get(evaluator.pageReports.size()-1);
		
		for (Word w: report.discardedWords)
		{
			incorrect.add(w.id);
		}
		
		for (Error e: report.wordErrors)
		{
			for (Word w: e.ocr)
				incorrect.add(w.id);
		}
		
		for (Word w: ocrPage.getWords())
		{
			int h = gtPage.height;
			if (!incorrect.contains(w.id))
			{	
				System.err.println(w.tokenizedText);			
				printGlyphs(h, out, w);
			}
		}
		
		try 
		{
			out.flush();
		} catch (IOException e1) 
		{
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}

	private void printGlyphs(int h, Writer out, Word w) 
	{
		List<Glyph> glyphs = w.glyphs;
		for (int i=0; i < glyphs.size(); i++)
		{
			Glyph g = glyphs.get(i);
			
		
			Rectangle r = g.getBoundingBox();
			int lly = h - (r.y + r.height);
			int llx = r.x;
			int urx = r.x + r.width;
			int ury = h - (r.y);
			try
			{
				out.write(g.character + " "  + llx + " " + lly + " " + urx + " " + ury + "\n");
			} catch (Exception e)  
			{ 
				e.printStackTrace(); 
			}
		}
	}

	public static void main(String[] args)
	{
		
		Page gtPage = new PageXMLParser().parsePage(args[0]);
		Page ocrPage = new AbbyyXMLParser().parsePage(args[1]); // we need abbyy XML for this
		try 
		{
			Writer out = new OutputStreamWriter(new FileOutputStream(args[2]), "UTF-8");
			new BoxTifExporter().exportValidWordsAsBoxTif(gtPage, ocrPage, out);
		} catch (UnsupportedEncodingException e) 
		{
			e.printStackTrace();
		} catch (FileNotFoundException e) 
		{
			e.printStackTrace();
		}
	}
}
