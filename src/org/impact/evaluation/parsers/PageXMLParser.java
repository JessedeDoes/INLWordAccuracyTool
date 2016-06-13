package org.impact.evaluation.parsers;
import java.util.*;
import java.io.*;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.SAXException;
import javax.xml.parsers.*;

import org.impact.evaluation.Glyph;
import org.impact.evaluation.Page;
import org.impact.evaluation.SimpleTokenizer;
import org.impact.evaluation.TextLine;
import org.impact.evaluation.TextRegion;
import org.impact.evaluation.Word;
import org.impact.evaluation.util.ParseUtils;
import org.impact.postcorrection.PageXMLCorrector;
import org.w3c.dom.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import javax.xml.transform.stream.*;
import java.util.HashSet;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PageXMLParser implements PageParser
{
	boolean cleanupTokenization = false;
	public boolean removePunctuation = true;
	public boolean useWordTags = false;
	boolean lookForNETags = true;
	boolean addGlyphs = true;
	boolean addLines = false;
	Document document = null;
	String NEtag = "IGT:NE";
	Page currentPage;
	PageXMLCorrector corrector;
	
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


	public List<Word> cleanupTokenizationErrors(List<Word> words)
	{
		List<Word> newList = new ArrayList<Word>();
		for (int i=0; i < words.size(); i++)
		{
			Word w = words.get(i);
			newList.add(w);
			if (i < words.size()-1)
			{
				Word nextWord = words.get(i+1);
				if (SimpleTokenizer.isPunctuation(nextWord))
				{
					w.text += nextWord.text;
					//System.err.println("Tokenization fixed: " + w.text);
					i++;
				}	
			}
		}
		return newList;
	}

	public Page parsePage(Document d)
	{
		Page page = new Page();
		corrector = new PageXMLCorrector();
		page.corrector = corrector;
		currentPage = page;
		page.setDocument(d);
	 	Element pageElement = ParseUtils.getElementByTagname(d.getDocumentElement(), "Page");	
		page.id = pageElement.getAttribute("imageFilename");
		try
		{
			page.width = Integer.parseInt(pageElement.getAttribute("imageWidth"));
			page.height = Integer.parseInt(pageElement.getAttribute("imageHeight"));
		} catch (Exception e)
		{
			
		}
		// oops, error in XSLT...
		
		if (page.id == null || page.id.equals(""))
			page.id = pageElement.getAttribute("imageFileName");
		
		List<Element> regionElements = ParseUtils.getElementsByTagname(d.getDocumentElement(), 
				"TextRegion", false);
		
		//int nW=0;
		
		/*
		List<Element> allGlyphs =  ParseUtils.getElementsByTagname(d.getDocumentElement(), 
				"Glyph", false);
		int nGlyphs = allGlyphs.size();
		*/
		
		for (Element r: regionElements)
		{	
			TextRegion region = new TextRegion();
			region.id = r.getAttribute("id");
			page.addRegion(region);
			Element c = ParseUtils.findFirstChild(r, "Coords");
			List<Point> points = getPoints(c);
			region.setBoundingPolygon(points);
			
			if (useWordTags) // and line tags as well...
			{
				List<Element> wordElements = ParseUtils.getElementsByTagname(r, "Word", false);
				for (Element w: wordElements)
				{
					region.words.add(getWord(w));
					corrector.addWord(w.getAttribute("id"), w);
				}
				if (cleanupTokenization)
					region.words = cleanupTokenizationErrors(region.words);
				
				if (addLines)
				{
					List<Element> lineElements = ParseUtils.getElementsByTagname(r, "TextLine", false);
					for (Element l: lineElements)
					{
						TextLine line = new TextLine();
						line.id = l.getAttribute("id");
						Element x = ParseUtils.findFirstChild(l, "Coords");
						List<Point> lpoints = getPoints(x);
						line.setBoundingPolygon(lpoints);
						region.lines.add(line);
						for (Element lw: ParseUtils.getElementsByTagname(l, "Word", false))
							line.words.add(getWord(lw));
					}
				}
			} else
			{
				/*
				 * This will not do if we are interested in NE tags and such things...
				 */
				int nWords=0;
				if (lookForNETags) // something like this.....
				{
					Element unicodeNode = ParseUtils.getElementByTagname(r, "Unicode");
					List<Node> textNodes = ParseUtils.getTextNodesBelow((Node) unicodeNode);
					for (Node t: textNodes)
					{
						String text = t.getTextContent();
						Element neNode = ParseUtils.findAncestor(t,this.NEtag);
						boolean inNE = (neNode != null);
						String[] words = text.split("\\s+");
						for (String s: words)
						{
							Word w = new Word(s);
							w.id = region.id + "." + (nWords++);
							w.type = inNE ? Word.TYPE_NE:Word.TYPE_GENERAL;
							if (w.text.length() > 0)
							{
								region.addWord(w);
								//System.err.println("Piep: " + nW++ + " : " + w.text + " --> " + w.tokenizedText);
							}
						}
					}
				} else
				{
					String text = ParseUtils.getElementContent(r, "Unicode");
					String[] words = text.split("\\s+");
					for (String s: words)
					{
						Word w = new Word(s);
						region.addWord(w);
					}
				}
			}
		}
		return page;
	}
	
	public String getUnicodeText(Element e)
	{
		try
		{
			Element te = ParseUtils.findFirstChild(e, "TextEquiv");
			return ParseUtils.getElementContent(te, "Unicode");
		} catch (Exception e1)
		{
			return null;
		}
	}
	
	static List<Point> getPoints(Element coordsElement)
	{
		List<Element> l = ParseUtils.getElementsByTagname(coordsElement,"Point", false);
		List<Point> points = new ArrayList<Point>();
		for (Element p: l)
		{
			int x = Integer.parseInt(p.getAttribute("x"));
			int y = Integer.parseInt(p.getAttribute("y"));
			points.add(new Point(x,y));
		}
		return points;
	}
	
	Word getWord(Element wordElement)
	{
		Word w= new Word();
		w.id = wordElement.getAttribute("id");
		try
		{
			Element te = ParseUtils.findFirstChild(wordElement, "TextEquiv"); // pas op veranderd om glyphs mogelijk te maken, 8 aug 2012
			String t = ParseUtils.getElementContent(te, "Unicode");
			if (t != null)
				w.setText(t);
			Element c = ParseUtils.getElementByTagname(wordElement, "Coords");
			List<Point> points = getPoints(c);
			w.setBoundingPolygon(points);
			if (this.addGlyphs)
			{
				List<Element> glyphElements = ParseUtils.getElementsByTagname(wordElement, "Glyph", false);
				for (Element g: glyphElements)
					w.glyphs.add(getGlyph(g));
			}
		} catch (Exception e)
		{
			e.printStackTrace();
		}
		return w;
	}
	
	Glyph getGlyph(Element glyphElement)
	{
		Glyph g = new Glyph();
		g.id = glyphElement.getAttribute("id");
		try
		{
			Element c = ParseUtils.getElementByTagname(glyphElement, "Coords");
			
			List<Point> points = getPoints(c);
			g.setBoundingPolygon(points);
			String t = getUnicodeText(glyphElement);
			if (t != null && t.length() > 0)
				g.setText(getUnicodeText(glyphElement).charAt(0));
			//w.id = wordElement.getAttribute("id");
		} catch (Exception e)
		{
			e.printStackTrace();
		}
		return g;
	}
	
	/*
	 * Of cause only possible if we have character-level ground truth....
	 */
	
	public void makeBoxTiff(Document d, String outputFileName)
	{
		OutputStreamWriter out;
		try 
		{
			out = new OutputStreamWriter(new FileOutputStream(outputFileName),"UTF-8");
		} catch (UnsupportedEncodingException e) 
		{
			e.printStackTrace();
			return;
		} catch (FileNotFoundException e) 
		{
			e.printStackTrace();
			return;
		}
		
		List<Element> regionElements = ParseUtils.getElementsByTagname(d.getDocumentElement(), 
				"TextRegion", true);
		
		List<Element> glyphElements = ParseUtils.getElementsByTagname(d.getDocumentElement(), "Glyph", true);
		System.err.println("Total nr. of glyphs in document: "  + glyphElements.size());
		
		for (Element r: regionElements)
		{	
			makeBoxTiff(r,out);
		}
	}
	
	public void makeBoxTiff(Element regionElement, OutputStreamWriter out)
	{
		try
		{
			List<Element> glyphElements = ParseUtils.getElementsByTagname(regionElement, "Glyph", true);
			System.err.println("Glyphs in region:"  + glyphElements.size());
			List<Glyph> glyphs  = new ArrayList<Glyph>();
			for (Element g: glyphElements) 
				glyphs.add(getGlyph(g));
		
			if (glyphs.size() > 0)
			{
				// System.err.println("OK: " + glyphs.size());
				
				for (int i=0; i < glyphs.size(); i++)
				{
					Glyph g = glyphs.get(i);
					int h = currentPage.height;
					Rectangle r = g.getBoundingBox();
					int lly = h - (r.y + r.height);
					int llx = r.x;
					int urx = r.x + r.width;
					int ury = h - (r.y);
					
					out.write(g.character + " "  + llx + " " + lly + " " + urx + " " + ury + "\n");
				}
			} else
			{
				System.err.println(regionElement.getAttribute("id") + ": geen glyphjes!");
			} 
		} catch (Exception e)
		{
			e.printStackTrace();
		}
		try
		{
			out.flush();
		} catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public void placePendingGlyphs(Document document, String outputFileName)
	{
		OutputStreamWriter out;
		try 
		{
			out = new OutputStreamWriter(new FileOutputStream(outputFileName),"UTF-8");
		} catch (UnsupportedEncodingException e) 
		{
			e.printStackTrace();
			return;
		} catch (FileNotFoundException e) 
		{
			e.printStackTrace();
			return;
		}
		
		List<Element> glyphElements = ParseUtils.getElementsByTagname(document.getDocumentElement(), "Glyph", true);
		List<Glyph> glyphs  = new ArrayList<Glyph>();
		for (Element g: glyphElements) 
			glyphs.add(getGlyph(g));
		
		List<Element> wordElements = ParseUtils.getElementsByTagname(document.getDocumentElement(), "Word", true);
	
		List<Word> words  = new ArrayList<Word>();
		Map<String,Element> wMap = new HashMap<String,Element>();
		for (Element g: wordElements) 
		{
			wMap.put(g.getAttribute("id"),g);
			words.add(getWord(g));
		}

		for (Element ge: glyphElements)
		{
			Glyph g = getGlyph(ge);
			Point p = g.getCenter();
			for (Word w: words)
			{
				if (w.inside(p))
				{
					if (ge==null)
						System.err.println("Not found: " + g.id);
					else
					{
						if (ge.getParentNode() != null)
						{
							ge.getParentNode().removeChild(ge);
							Element we = wMap.get(w.id);
							if (we == null)
								System.err.println("Word not found: " + w.id);
							else
							{
								System.err.println("move " + g.id + " to " + w.id);
								we.appendChild(ge);
							}
							break;
						}
					}
				}
			}
		}
		for (Element we: wordElements)
		{
			Element te = ParseUtils.findFirstChild(we, "TextEquiv");
			if (te != null)
			{
				we.removeChild(te);
				we.appendChild(te);
			}
		}
		try 
		{
			out.write(ParseUtils.documentToString(document));
			out.flush();
		} catch (IOException e) 
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args)
	{
		PageXMLParser parser = new PageXMLParser();
		parser.useWordTags = true;
		Page page = parser.parsePage(args[0]);
		//page.marshal();
		parser.makeBoxTiff(page.getDocument(), args[0] + ".box.out");
		//parser.placePendingGlyphs(page.getDocument(), args[0] + ".nopending.xml");
	}
}
