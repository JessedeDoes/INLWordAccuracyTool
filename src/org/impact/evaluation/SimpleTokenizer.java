package org.impact.evaluation;

import java.util.regex.*;
import java.util.ArrayList;
import java.util.List;
import java.io.*;

import org.impact.evaluation.parsers.PageXMLParser;

public class SimpleTokenizer
{
    static Pattern nonWordPattern = Pattern.compile("\\W+");
    static Pattern punctuationPattern = Pattern.compile("^\\p{P}+$");
    
	static Pattern prePunctuationPattern = Pattern.compile("(^|\\s)\\p{P}+");
	static Pattern postPunctuationPattern = Pattern.compile("\\p{P}+($|\\s)");

	static Pattern leadingBlanks = Pattern.compile("^\\s+");
	static Pattern trailingBlanks = Pattern.compile("\\s+$");
	
	public String prePunctuation="";
	public String postPunctuation="";
	public String trimmedToken="";

	public void tokenize(String t)
	{
		Matcher m1 = prePunctuationPattern.matcher(t);
		Matcher m2 = postPunctuationPattern.matcher(t);
		
		int s=0; int e = t.length();

		if (m1.find())
	 		s = m1.end();
		if (m2.find())
			e = m2.start();	

		if (e < s) e=s;
		trimmedToken = t.substring(s,e);
		prePunctuation = t.substring(0,s);
		postPunctuation = t.substring(e,t.length());
	}

	public void tokenizePageXML(String fileName, PrintStream out)
	{
		PageXMLParser p = new PageXMLParser();
		p.useWordTags = true;
		p.removePunctuation = false;
		Page page = p.parsePage(fileName);
		List<Word> words = page.getWords();
		for (Word word: words)
		{
			String w = word.text;
			tokenize(w);
			String lastOut="";
			if (prePunctuation.length()>0)
			{
				out.println(prePunctuation + "\t" + word.id);
				lastOut = prePunctuation;
			}	
			if (trimmedToken.length()>0)
			{
				out.println(trimmedToken + "\t" + word.id);
				lastOut = trimmedToken;
			}
			if (postPunctuation.length()>0)
			{
				out.println(postPunctuation + "\t" + word.id);
				lastOut = postPunctuation;
			}
			if (w.endsWith(".") || w.endsWith("?")	 || w.endsWith("!"))
			{
				out.println("\tNONE");
			}
		}			
	}

	public static String trim(String s)
	{
		Matcher x = prePunctuationPattern.matcher(s);
		s = x.replaceAll("");
		Matcher y = postPunctuationPattern.matcher(s);
		return y.replaceAll("");
	}
	
	public static String trimWhiteSpace(String s)
	{
		Matcher x = leadingBlanks.matcher(s);
		s = x.replaceAll("");
		Matcher y = trailingBlanks.matcher(s);
		return y.replaceAll("");
	}


	
	public static void main(String[] args)
	{
		SimpleTokenizer t = new SimpleTokenizer();
		for (String a: args)
		{
			t.tokenizePageXML(a,System.out);
		}
	}
	
	public static boolean isPunctuation(Word w)
	{
		String t = w.text;
		Matcher m = punctuationPattern.matcher(t);
		return m.matches();
		//return t.equals("-") || t.equals(".") || t.equals(",") || t.equals("?") || t.equals("!");
	}
	
	public static List<Word> removePunctuation(List<Word> words)
	{
		List<Word> newList = new ArrayList<Word>();
		for (int i=0; i < words.size(); i++)
		{
			Word w = words.get(i);
			w.text = Ligatures.replaceLigatures(w.text);
			w.text = SimpleTokenizer.trim(w.text);
			if (w.text.length()>0)
				newList.add(w);
		}
		return newList;
	}

}
