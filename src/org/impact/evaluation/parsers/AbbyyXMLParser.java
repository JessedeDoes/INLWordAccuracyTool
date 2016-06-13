package org.impact.evaluation.parsers;

import java.awt.Point;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.impact.evaluation.Glyph;
import org.impact.evaluation.Page;
import org.impact.evaluation.TextLine;
import org.impact.evaluation.TextRegion;
import org.impact.evaluation.Word;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;
import org.impact.evaluation.util.*;

/*
 * We need this one because it has glyph coordinates...
 * Which means we can try to write boxTiff output from the ABBYY xml
 */

public class AbbyyXMLParser implements PageParser
{
	Document document;
	int wordCount = 0;

	@Override
	public Page parsePage(String fileName) 
	{
		// TODO Auto-generated method stub
		int nRegions=0;
		wordCount=0;
		Page page = new Page();
		try 
		{
			document = ParseUtils.parse(fileName);
			Element root = document.getDocumentElement();
			List<Element> blockElements = ParseUtils.getElementsByTagname(root, "block", false);
			for (Element b: blockElements)
			{
				TextRegion r = new TextRegion();
				r.id = "textRegion" + nRegions++;
				page.addRegion(r);
				r.setBoundingPolygon(getBounds(b));
				List<Element> lineElements = ParseUtils.getElementsByTagname(b, "line", false);
				for (Element l: lineElements)
				{
					TextLine currentLine = makeLine(l);
					r.lines.add(currentLine);
					List<Element> currentWord = new ArrayList<Element>();
					List<Element> charElements = ParseUtils.getElementsByTagname(l, "charParams", false);
				    for (Element c: charElements)
				    {
				    	String s = c.getTextContent();
				    	if (isWhiteSpace(s))
				    	{
				    		if (currentWord.size() > 0)
				    		{
				    			r.addWord(makeWord(currentWord));
				    			currentWord.clear();
				    		}
				    	} else 
				    	{
				    		currentWord.add(c);
				    	}
				    }
				    if (currentWord.size() > 0)
				    {
				    	Word w = makeWord(currentWord);
				    	r.addWord(w);
				    	currentLine.addWord(w);
				    }
				}
			}
		} catch (Exception e) 
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		return page;
	}

	private TextLine makeLine(Element l)
	{
		TextLine line = new TextLine();
		line.setBoundingPolygon(getBounds(l));
		return line;
	}
	
	private Word makeWord(List<Element> charElements) 
	{
		// TODO Auto-generated method stub
		String s="";
		int lMin=Integer.MAX_VALUE, tMin=Integer.MAX_VALUE, 
			rMax = Integer.MIN_VALUE, bMax=Integer.MIN_VALUE;
		List<Glyph> glyphs = new ArrayList<Glyph>();
		for (Element c: charElements)
		{
			int l = Integer.parseInt(c.getAttribute("l"));
			int t = Integer.parseInt(c.getAttribute("t"));
			int r = Integer.parseInt(c.getAttribute("r"));
			int b = Integer.parseInt(c.getAttribute("b"));
			Glyph g = new Glyph(c.getTextContent());
			//System.err.println(g);
			g.setBoundingPolygon(this.makeBounds(l, t, r, b));
			glyphs.add(g);
			if (l < lMin)
				lMin = l;
			if (t < tMin)
				tMin = t;
			if (r > rMax)
				rMax = r;
			if (b > bMax)
				bMax = b;
			s += c.getTextContent();
		}
		List<Point> bounds = makeBounds(lMin,tMin,rMax,bMax);
		Word w = new Word(s);
		w.glyphs = glyphs;
		w.id = "word" + wordCount++;
		w.setBoundingPolygon(bounds);
		return w;
	}
	
	private List<Point> getBounds(Element x)
	{
		int l = Integer.parseInt(x.getAttribute("l"));
		int t = Integer.parseInt(x.getAttribute("t"));
		int r = Integer.parseInt(x.getAttribute("r"));
		int b = Integer.parseInt(x.getAttribute("b"));
		return makeBounds(l,t,r,b);
	}
	
	private List<Point> makeBounds(int l, int t, int r, int b) 
	{
		List<Point> points = new ArrayList<Point>();
		points.add(new Point(l,t));
		points.add(new Point(l,b));
		points.add(new Point(r,b));
		points.add(new Point(r,t));
		return points;
	}

	private boolean isWhiteSpace(String s) 
	{
		/// TODO Auto-generated method stub
		return s.matches("^\\s*$");
	}
	
	public static void main(String[] args)
	{
		AbbyyXMLParser p = new AbbyyXMLParser();
		Page page = p.parsePage(args[0]);
		page.marshal();
	}
}
