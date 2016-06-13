package org.impact.evaluation;
import java.util.ArrayList;
import java.util.List;
import java.awt.Point;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

@XmlRootElement
public class Word extends TextRegion
{
	@XmlElement
	public String text="";
	@XmlElement
	public String tokenizedText="";
	@XmlAttribute
	boolean outOfOrder = false;
	@XmlAttribute
	public boolean inDictionary  = false;
	@XmlAttribute
	public String id="";	
	int status=-1;
	@XmlAttribute
	public int type=-1;
	@XmlTransient
	public List<Glyph> glyphs = new ArrayList<Glyph>();
	
	public static int OCR_ONLY=1;
	public static int GROUNDTRUTH_ONLY=2;
	public static int DELETED=3;
	public static int CORRECT=4;
	
	public static int TYPE_NE=1;
	public static int TYPE_GENERAL=0;
	

	
	public Word(String s)
	{
		setText(s);
	}
	
	public Word()
	{
		
	}

	public void setText(String s) 
	{
		for (int i=0; i < s.length(); i++)
		{
			char c = s.charAt(i);
			if (c <= 0x001f)
			{
				text = tokenizedText = "HMPF?" + (int) c;
				return;
			}
		}
		text  = s;
		tokenizedText = Ligatures.replaceLigatures(text);
		tokenizedText = SimpleTokenizer.trim(tokenizedText);
		// TODO Auto-generated method stub
	}
	
	void addGlyph(Glyph g)
	{
		glyphs.add(g);
	}
}
