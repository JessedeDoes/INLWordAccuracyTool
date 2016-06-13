package org.impact.postcorrection;

import java.util.HashMap;
import java.util.Map;

import org.impact.evaluation.Word;
import org.impact.evaluation.util.ParseUtils;
import org.w3c.dom.Element;

public class PageXMLCorrector implements DocumentCorrector 
{

	private Map<String,Element> wordElementMap = new HashMap<String,Element>();

	

	public void addWord(String id, Element e)
	{
		wordElementMap.put(id,e);
	}
	
	@Override
	public void updateWord(Word w) 
	{
		Element e = wordElementMap.get(w.id);
		if (e != null)
		{
			Element u = ParseUtils.getElementByTagname(e, "Unicode");
			if (u != null)
			{
				u.setAttribute("corrected", "true");
				u.setAttribute("originalText", u.getTextContent());
				u.setTextContent(w.text);
			}
		} else
		{
			System.err.println("Could not get " + w.id);
			// System.exit(1);
		}
	}
	
}
