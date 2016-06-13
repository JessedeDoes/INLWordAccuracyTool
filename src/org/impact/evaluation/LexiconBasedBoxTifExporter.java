package org.impact.evaluation;

public class LexiconBasedBoxTifExporter 
{
	public static void main(String[] args)
	{
		BoxTifExporter e = new BoxTifExporter();
		e.exportLongLexiconWordsAsBoxTif(args[0], args[1], args[2]);
	}
}
