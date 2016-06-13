package org.impact.evaluation;

public class Glyph extends TextRegion 
{
	public char character;
	
	public Glyph(String s)
	{
		if (s != null && !(s.length()==0))
			character = s.charAt(0);
	}

	public Glyph() 
	{
		// TODO Auto-generated constructor stub
	}
	public void setText(char character)
	{
		this.character = character;
	}
}
