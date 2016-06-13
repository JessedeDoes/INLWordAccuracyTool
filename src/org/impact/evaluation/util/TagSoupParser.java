package org.impact.evaluation.util;

import java.io.File;
import java.io.InputStreamReader;
import java.net.URL;

import org.apache.xalan.xsltc.trax.SAX2DOM;
import org.ccil.cowan.tagsoup.Parser;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

public class TagSoupParser 
{
	public static Document parse2DOM(String sURL)
	{
		Parser p = new Parser();
		SAX2DOM sax2dom = null;
		org.w3c.dom.Node doc  = null;

		try 
		{ 
			File f = new File(sURL);
			//URL url = new URL(sURL);
			URL url = f.toURI().toURL();
			//System.err.println(url);
			p.setFeature(Parser.namespacesFeature, false);
			p.setFeature(Parser.namespacePrefixesFeature, false);
			sax2dom = new SAX2DOM();
			p.setContentHandler(sax2dom);
			p.parse(new InputSource(new InputStreamReader(url.openStream(),"UTF-8")));
			doc = sax2dom.getDOM();
			//System.err.println(doc);
		} catch (Exception e) 
		{
			// TODO handle exception
			e.printStackTrace();
		}
		return (Document) doc;
	}
}
