package org.impact.postcorrection;

import java.util.HashMap;
import java.util.Map;

import org.impact.evaluation.Word;
import org.impact.evaluation.util.ParseUtils;
import org.w3c.dom.Element;

public class ALTOCorrector implements DocumentCorrector 
{
	private Map<String,Element> wordElementMap = new HashMap<String,Element>();
	static int nCorrections = 0;
	
	public void addWord(String id, Element e)
	{
		wordElementMap.put(id,e);
	}
	
	@Override
	public void updateWord(Word w) 
	{
		Element e = wordElementMap.get(w.id);
		nCorrections++;
		if (e != null)
		{
			e.setAttribute("originalText", e.getAttribute("CONTENT"));
			e.setAttribute("CONTENT", w.text);
		} else
		{
			System.err.println("could not get word " + w.id);
		}
		// TODO Auto-generated method stub
	}
}
