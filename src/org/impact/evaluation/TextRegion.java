package org.impact.evaluation;
import java.util.ArrayList;
import java.util.List;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;

import javax.xml.bind.annotation.XmlAnyElement;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

@XmlRootElement
public class TextRegion 
{
	@XmlAttribute
	public String id=null;
	int readingOrderProblems = 0;
	//public List<java.awt.Point> boundingBox = new ArrayList<java.awt.Point>();
	//@XmlElement
	
	private Polygon boundingPolygon = new Polygon();
	List<Point> boundingBox = new ArrayList<Point>();
	
	@XmlTransient
	public
	List<TextLine> lines = new ArrayList<TextLine>();
	
	public TextRegion()
	{
		
	}
/*
	TextRegion(Point p1, Point p2, Point p3, Point  p4)
	{
		boundingPolygon.addPoint(p1.x, p1.y);
		boundingPolygon.addPoint(p2.x, p2.y);
		boundingPolygon.addPoint(p3.x, p3.y);
		boundingPolygon.addPoint(p4.x, p4.y);
	}
*/	
	@XmlElement(name="Word")
	public List<Word> words =  new ArrayList<Word>();

	/*
		A word is counted as contained if center is inside the bounding polygon
		Maybe this is slightly too strict?
	*/	
	public boolean contains(Word w)
	{
		Point c = w.getCenter();
		return inside(c);
	}
	
	public boolean inside(Point p)
	{
		return boundingPolygon.contains(p);
	}
	
	public void addWord(Word w)
	{
	   words.add(w);	
	}
	
	public TextRegion cloneShapeOnly()
	{
		TextRegion r = new TextRegion();
		r.boundingPolygon = this.boundingPolygon;
		r.id=this.id;
		return r;
	}
	
	boolean hasCoords()
	{
		return (this.boundingPolygon != null && this.boundingPolygon.npoints > 0); 
	}

	/*
	 * Note that with polygons, the "center" need not be in the region
	 * so it is bad for insideness testing
	 */
	public Point getCenter()
	{
		if (!hasCoords())
			return null;
		
		double cx = 0;
		double cy = 0;
		double N =  boundingPolygon.npoints;
		
		for (int i=0; i < N; i++)
		{
			cx += boundingPolygon.xpoints[i];
			cy += boundingPolygon.ypoints[i];
		}
		
		cx = Math.round(cx / N);
		cy = Math.round(cy / N);
		
		return new Point((int) cx, (int) cy);
	}
	
	// I am clearly above another word if my center is above the other's bounding box top y?
	
	public boolean isClearlyAbove(TextRegion other)
	{
		if (!hasCoords() || !other.hasCoords())
			return false;
		int centerY = this.getCenter().y;
		int otherTop = other.boundingPolygon.getBounds().y;
		int otherHeight =  other.boundingPolygon.getBounds().height;
		int myHeight = boundingPolygon.getBounds().height;
		return centerY + myHeight < otherTop - otherHeight;
	}
	
	/*
	 * If a GT region is split vertically in the OCR regions side by side,
	 * there should be a case where a later word is clearly 'above' an earlier word
	 * in the  reading order suggested by the OCR.
	 */
	
	public boolean hasSuspectReadingOrder()
	{
		int lowWordY = Integer.MIN_VALUE;
		Word lowWord = null;
		int problems = 0;
		
		for (Word w: words)
		{
			w.outOfOrder = false;
			if (w.hasCoords())
			{
				int y = w.getCenter().y;
				if (y > lowWordY)
				{	
					// System.err.println(">>>>> " + y + ": " + w.text);
					lowWordY = y;
					lowWord = w;
				} else if (w.isClearlyAbove(lowWord))
				{
					w.outOfOrder = true; // but so is any preceding word 'below' me....
					problems++;
				}
			}
		}
		if (problems > 0)
		{
			//System.err.println("Region " + id + " order problems: " + problems + " out of " + words.size() + " words");
		}
		this.readingOrderProblems = problems;
		return (problems > 0);
	}
	@XmlTransient
	public Polygon getBounds()
	{
		return this.boundingPolygon;
	}
	@XmlTransient
	public Rectangle getBoundingBox()
	{
		return boundingPolygon.getBounds();
	}
	
	public void setBoundingPolygon(List<Point> points)
	{
		boundingPolygon.reset();
		for (Point point: points) 
			boundingPolygon.addPoint(point.x,point.y);
		Rectangle r = boundingPolygon.getBounds();
		
		boundingBox.add(new Point(r.x, r.y));
		boundingBox.add(new Point(r.x+r.width, r.y));
		boundingBox.add(new Point(r.x+r.width, r.y+r.height));
		boundingBox.add(new Point(r.x, r.y+r.height));
	}
}
