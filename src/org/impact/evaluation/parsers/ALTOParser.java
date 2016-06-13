/*
 * Preliminary alto support.
 * Just gets textblocks and strings
 * No hyphenation support
 * Only rectangular regions
 */
package org.impact.evaluation.parsers;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

import org.impact.evaluation.Page;
import org.impact.evaluation.TextRegion;
import org.impact.evaluation.Word;
import org.impact.evaluation.util.ParseUtils;
import org.impact.postcorrection.ALTOCorrector;
import org.impact.postcorrection.DocumentCorrector;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class ALTOParser implements PageParser 
{
	@Override
	
	public Page parsePage(String fileName) 
	{
		try
		{
			Document d = ParseUtils.parse(fileName);
			return parsePage(d);
		} catch (Exception e)
		{
			System.err.println("Could not parse page at " + fileName);
			System.err.println(e);
		}
		return null;
	}
	
	List<Point> getPoints(Element r)
	{
		int HPOS = new Integer(r.getAttribute("HPOS"));
		int VPOS = new Integer(r.getAttribute("VPOS"));
		int WIDTH = new Integer(r.getAttribute("WIDTH"));
		int HEIGHT = new Integer(r.getAttribute("HEIGHT"));
		
		Point p0 = new Point(HPOS,VPOS);
		Point p1 = new Point(HPOS + WIDTH, VPOS);
		Point p2 = new Point(HPOS + WIDTH, VPOS + HEIGHT);
		Point p3 = new Point(HPOS, VPOS + HEIGHT);
		
		List<Point> l = new ArrayList<Point>();
		
		l.add(p3); l.add(p2); l.add(p1); l.add(p0);
		return l;
	}
	
	public Page parsePage(Document d)
	{
		Page page = new Page();
		ALTOCorrector corrector = new ALTOCorrector();
		page.corrector = corrector;
		page.setDocument(d);
		List<Element> regionElements = ParseUtils.getElementsByTagname(d.getDocumentElement(), 
				"TextBlock", false);

		// <TextBlock ID="P1_TB00001" HPOS="102" VPOS="775" WIDTH="1346" HEIGHT="181" STYLEREFS="TXT_0 PAR_LEFT">
		
		for (Element r: regionElements)
		{
			TextRegion region = new TextRegion();
			region.id = r.getAttribute("ID");
			page.addRegion(region);
			List<Point> points = getPoints(r);
			region.setBoundingPolygon(points);
			List<Element> wordElements = ParseUtils.getElementsByTagname(r, "String", false);
			
			for (Element w: wordElements)
			{
				region.words.add(getWord(w));
				corrector.addWord(w.getAttribute("ID"), w);
			}
		}
		
		return page;
	}

	// <String ID="P1_ST00005" HPOS="806" VPOS="778" WIDTH="197" HEIGHT="38" CONTENT="Feestdagen" WC="0.99" CC="0031060907"/>
	
	private Word getWord(Element s) 
	{
		Word w = new Word();
		w.setText(s.getAttribute("CONTENT"));
		w.id = s.getAttribute("ID");
		w.setBoundingPolygon(getPoints(s));
		return w;
	}
}
