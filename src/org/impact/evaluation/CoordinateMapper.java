package org.impact.evaluation;

import java.awt.Point;
import java.util.*;
import java.io.*;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.SAXException;
import javax.xml.parsers.*;

import org.impact.evaluation.parsers.PageXMLParser;
import org.impact.evaluation.util.ParseUtils;
import org.w3c.dom.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import javax.xml.transform.stream.*;
import java.util.HashSet;



public class CoordinateMapper 
{	
	static class rPoint
	{
		double x;
		double y;
		public String toString() { return "(" + x + ","+ y + ")"; }
	}
	
	static class Instance
	{
		Word w1, w2;
		Transformation transformation;
		public Instance(Word w1, Word w2, Transformation t)
		{
			this.w1 = w1;
			this.w2 = w2;
			this.transformation = t;
		}
	}
	
	static class Transformation
	{
		double theta;
		double lambda;
		double t1, t2;
		
		public Point apply(Point p)
		{
			Point r = new Point();
			double r1 = lambda * ( Math.cos(theta) * p.x - Math.sin(theta) * p.y);
			double r2 = lambda * ( Math.sin(theta) * p.x + Math.cos(theta) * p.y);
			r.x = (int) Math.round(r1 + t1);
			r.y = (int) Math.round(r2 + t2);
			return r;
		}
		
		public rPoint rApply(Point p)
		{
			rPoint r = new rPoint();
			double r1 = lambda * ( Math.cos(theta) * p.x - Math.sin(theta) * p.y);
			double r2 = lambda * ( Math.sin(theta) * p.x + Math.cos(theta) * p.y);
			r.x = r1 + t1;
			r.y = r2 + t2;
			return r;
		}
		
		public String toString()
		{
			return "[theta=" + (180.0 / Math.PI) * theta + ", lambda=" + lambda + " t = (" + t1 + "," + t2 + ")]"; 
		}
		
		public void transformDocument(Document d)
		{
			List<Element> pointElements = ParseUtils.getElementsByTagname(d.getDocumentElement(), "Point", false);
			for (Element p: pointElements)
			{
				int x = Integer.parseInt(p.getAttribute("x"));
				int y = Integer.parseInt(p.getAttribute("y"));
				Point p0 = new Point(x,y);
				Point p1 = this.apply(p0);
				p.setAttribute("x", "" + p1.x);
				p.setAttribute("y", "" + p1.y);
				System.err.println(p0 + " --> " + p1);
			}
		}
	}
	
	public static double norm(Point p)
	{
		return Math.sqrt(p.x*p.x + p.y*p.y);
	}
	
	public static Transformation findMapping(Point p1, Point p2, Point q1, Point q2)
	{
		Transformation t = new Transformation();
		t.t1 = t.t2 = 0;
		
		
		// find angle theta
		
		int a1 = p2.x - p1.x;
		int a2 = p2.y - p1.y;
		int b1 = q2.x - q1.x;
		int b2 = q2.y - q1.y;
		
		double cosTheta = (a1*b1 + a2*b2) / (Math.sqrt(a1*a1 + a2*a2) * Math.sqrt(b1*b1+ b2*b2));
		t.theta = Math.acos(cosTheta);
		if (a1*b2 - a2 * b1 < 0)
		{
			System.err.println("Negative!");
			t.theta = -1.0 * t.theta;
		}
		
		// find scaling factor lambda
		Point d1 = new Point(p2.x - p1.x, p2.y-  p1.y);
		Point d2 = new Point(q2.x - q1.x, q2.y-  q1.y);
		t.lambda = norm(d2) / norm(d1);
		
		
		// find translation (t1,t2)
		
		rPoint r = t.rApply(p1);
		t.t1 = q1.x - r.x;
		t.t2 = q1.y - r.y;
		
		
		// System.err.println(t);
		Point r2 = t.apply(p2);
		// System.err.println(r2 + " =~" + q2);
		return t;
	}
	
	public static Transformation findMapping(Page p1, Page p2)
	{
		Map<String, List<Word>> m1 = p1.getWordMap();
		Map<String, List<Word>> m2 = p2.getWordMap();
		
		List<Instance> instances = new ArrayList<Instance>();
		
		for (String s: m1.keySet())
		{
			List<Word> l1 = m1.get(s);
			if (l1.size() ==1)
			{
				List<Word> l2 = m2.get(s);
				if (l2 != null && l2.size() == 1)
				{
					Word w1 = l1.get(0);
					Word w2 = l2.get(0);
					Transformation t = findMapping(w1.boundingBox.get(0), w1.boundingBox.get(3),
													w2.boundingBox.get(0), w2.boundingBox.get(3));
					//System.err.println(t);
					instances.add(new Instance(w1,w2,t));
				}
			}
		}
		
		// now choose transformation to minimize error on all words...
		
		double bestError = 10e23;
		Instance bestInstance = null;
		for (Instance i: instances)
		{
			Transformation t = i.transformation;
			double error=0;
			for (Instance j: instances)
			{
				if (i.w1 != j.w1)
				{
					rPoint p = t.rApply(j.w1.boundingBox.get(0));
					Point pRef = j.w2.boundingBox.get(0);
					double d1 = pRef.x - p.x;
					double d2 = pRef.y - p.y;
					double d = Math.sqrt(d1*d1 + d2*d2);
					error += d;
				}
			}
			if (error < bestError) { bestError = error; bestInstance = i; }
		}
		System.err.println("Picked: " + bestInstance.transformation + " " + bestInstance.w1.text);
		System.err.println("Average error: " + bestError / (instances.size()));
		for (Instance k: instances)
		{
			if (k.w1 != bestInstance.w1)
			{
				rPoint p = bestInstance.transformation.rApply(k.w1.boundingBox.get(0));
				Point pRef = k.w2.boundingBox.get(0);
				double d1 = pRef.x - p.x;
				double d2 = pRef.y - p.y;
				double d = Math.sqrt(d1*d1 + d2*d2);
				System.err.println("Error on "  + k.w1.text + " : " + d);
			}
		}
		return bestInstance.transformation;
	}
	
	public static void main(String[] args)
	{
		Point p1 = new Point(1,2);
		Point p2 = new Point(3,4);
		Point q1 = new Point(0,0);
		Point q2 = new Point(0,10);
	
		findMapping(p1,p2,q1,q2);
		
		PageXMLParser p = new PageXMLParser();
		p.useWordTags = true;
		Page page1 = p.parsePage(args[0]);
		Page page2 = p.parsePage(args[1]);
		
		Transformation t = findMapping(page1,page2);
		
		try
		{
			Document d = ParseUtils.parse(args[2]);
			t.transformDocument(d);
			System.out.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
			System.out.print(ParseUtils.documentToString(d));
		} catch (Exception e)
		{
			e.printStackTrace();
		}
	}
}
