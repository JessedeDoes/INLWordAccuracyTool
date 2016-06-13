package org.impact.evaluation;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Map;

import javax.xml.bind.annotation.XmlAnyElement;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import org.impact.evaluation.util.ParseUtils;
import org.impact.postcorrection.DocumentCorrector;
import org.w3c.dom.*;

import javax.xml.bind.*;

@XmlRootElement
public class Page 
{
	@XmlAttribute
	public String id=null;
	HashMap<String,TextRegion> regionMap = new HashMap<String,TextRegion>();
	@XmlElement(name="TextRegion")
	public List<TextRegion> textRegions = new ArrayList<TextRegion>();
	
	@XmlAttribute
	public int width;
	@XmlAttribute
	public int height;

	
	protected Document document=null; // the original Page XML document
	@XmlTransient
	public DocumentCorrector corrector = null;
	
	public Page()
	{
		
	}
	
	public void setDocument(Document document)
	{
		this.document = document;
	}
	
	public void marshal()
	{
		try
		{
			JAXBContext jaxbContext=JAXBContext.newInstance(
					new Class[] 
				{org.impact.evaluation.Page.class, org.impact.evaluation.TextRegion.class, 
							org.impact.evaluation.Word.class});

			Marshaller marshaller=jaxbContext.createMarshaller();
			marshaller.setProperty( Marshaller.JAXB_FORMATTED_OUTPUT, true);
			marshaller.marshal( this, System.out );
		} catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public void addRegion(TextRegion r)
	{ 
		textRegions.add(r); 
		regionMap.put(r.id, r);
	}
	
	public TextRegion getRegionById(String id)
	{
		return regionMap.get(id);
	}
	
	public List<Word> getWords()
	{
		ArrayList<Word> list = new ArrayList<Word>();
		for (TextRegion r: textRegions) for (Word w: r.words) { list.add(w); }
		return list;
	}
	
	public Map<String, List<Word>> getWordMap()
	{
		Map<String, List<Word>> wordMap = new HashMap<String, List<Word>>();
		for (Word w: getWords())
		{
			List<Word> l = wordMap.get(w.text);
			if (l == null) { l = new ArrayList<Word>(); wordMap.put(w.text,l); }
			l.add(w);
		}
		return wordMap;
	}
	
	public void savePlainText(String fileName)
	{
		try
		{
			OutputStreamWriter p = new OutputStreamWriter(new FileOutputStream(fileName), "UTF-8");
			for (TextRegion r: textRegions)
			{
				List<String> words = new ArrayList<String>();
				for (Word w: r.words)
				{
					String x = w.text;
					x = Ligatures.replaceLigatures(x).toLowerCase(); // locale ??
					words.add(x);
				}
				String regionText = ParseUtils.join(words, " ");
				p.write(regionText);
				p.write("\n");
				p.flush();
			}
		} catch (Exception e)
		{
		}
	}
	
	public void saveDocumentToFile(String fileName)
	{
		try
		{
			OutputStreamWriter p = new OutputStreamWriter(new FileOutputStream(fileName), "UTF-8");
			String s = ParseUtils.documentToString(document);
			p.write(s);
			p.close();
		} catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	@XmlTransient
	public Document getDocument() 
	{
		// TODO Auto-generated method stub
		return document;
	}

	public void updateWord(Word w) 
	{
		// TODO Auto-generated method stub
		if (this.corrector != null)
			corrector.updateWord(w);
	}

	public void removeWord(Word w) 
	{
		// TODO Auto-generated method stub
		w.glyphs = new ArrayList<Glyph>();
	}
}
