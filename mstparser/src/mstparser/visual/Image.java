package mstparser.visual;

import javax.imageio.ImageIO;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import org.w3c.dom.Element;

import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;


/**
 * This is a wrapper class for all the data types
 * associated with an image. Most of the actual data
 * is encapsulated inside the Poylgon objects.
 *
 * @author delliott
 *
 */

public class Image {
	
	public ParserPolygon[] polygons;
	public double[] dimensions;
	public double imageArea;
	public String filename;
	public String xmlFilename;
	public String dotFilename;
	//public IplImage image;
	
	public Image(String imageFile)
	{
		this.filename = imageFile;
		this.dimensions = new double[2];
	}
	
	public void getImageDimensions()
	{
		try 
		{
			BufferedImage img = ImageIO.read(new File(this.filename));
			this.dimensions[0] = img.getWidth();
			this.dimensions[1] = img.getHeight();
			this.imageArea = this.dimensions[0] * this.dimensions[1];
			img = null;
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
		}
		catch(IllegalArgumentException iae)
		{
			// This only happens for one file 2010__006219.jpg. 
			// There must be something wrong with how it is encoded.
			this.dimensions[0] = -1;
			this.dimensions[1] = -1;
			System.err.println("Could not read image from disk: " + this.filename);
		}
	}
	
	public void calculatePolygonAreas()
	{
		for (ParserPolygon p: this.polygons)
		{
			p.calculateArea(this.imageArea);
		}
	}
	
	public void setXMLFile(String filename)
	{
		this.xmlFilename = filename;
	}
	
	public void setDOTFile(String filename)
	{
		this.dotFilename = filename;
	}
	
	/**
	 * This method returns the most likely Polygon object in the image
	 * for the entry in the dependency structure input to the parser.
	 * 
	 * We need this method because the XML representation can contain more
	 * entries than the dependency structure input.
	 * 
	 * @param label the label of the entry in the .conll file
	 * @param centroid the centroid in the FEATS column of the entry.
	 * @return            System.out.println(feature.toString());            

	 */
	public int findPolygon(String label, Point2D centroid)
	{
	    for (int i = 0; i < polygons.length; i++)
	    {
	        ParserPolygon p = polygons[i];
	        if (p.label.equals(label))
	        {
	            // The polygon has the same label and is within two pixels
	            if (p.centroid.distance(centroid) < 10.0)
	            {
	                //System.out.println(p.label);
	                return i;
	            }
	        }
	    }
	    // This is a problem.
	    return -1;
	}
	
	public void populateQuadrants()
	{
		for (ParserPolygon p: polygons)
		{
			p.imageQuadrant = ImageQuadrant.getPolygonQuadrant(p, this);
			p.calculateDistanceFromCentre(this.dimensions[0], this.dimensions[1]);
		}
	}
	
	public void calculateSpatialRelationships()
	{
	    for (ParserPolygon p1: polygons)
	    {
	        int i = 0;
	        p1.spatialRelations = new SpatialRelation.Relations[polygons.length];
	        for (ParserPolygon p2: polygons)
	        {
	            p1.spatialRelations[i] = SpatialRelation.GetSpatialRelationship(p1, p2);
	            i++;
	        }
	    }
	}
	
	public void findNearestPolygons()
	{
		double distance = 100000000000.0;
		ParserPolygon nearest = null;
		int nearestIndex = -1;
		int i = 0;
		for (ParserPolygon p1: polygons)
		{
			int j = 0;
			if (p1.nearestPolygon != null)
			{
				continue;
			}
			for (ParserPolygon p2: polygons)
			{
				if (p1.equals(p2))
				{
					j++;
					continue;
				}
				double calculation = p1.calculateDistanceFromObject(p2);
				if (calculation - 0.0000001 < distance)
				{
					distance = calculation;
					nearest = p2;
					nearestIndex = j;
				}
				j++;
			}
			p1.nearestPolygon = nearest;
			nearest.nearestPolygon = p1;
			p1.nearestIndex = nearestIndex;
			nearest.nearestIndex = i;
			i++;
		}
	}
	
	public void parseXMLFile()
	{
		if (this.xmlFilename != null)
		{
		    try 
		    {
		        File fXmlFile = new File(this.xmlFilename);
		        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		        Document doc = dBuilder.parse(fXmlFile);
		     
		        //optional, but recommended
		        //read this - http://stackoverflow.com/questions/13786607/normalization-in-dom-parsing-with-java-how-does-it-work
		        doc.getDocumentElement().normalize();
		     		     
		        NodeList objectNodeList = doc.getElementsByTagName("object");
		        this.polygons = new ParserPolygon[objectNodeList.getLength()];
		     
		        for (int objectCounter = 0; objectCounter < objectNodeList.getLength(); objectCounter++) {
		     
		            Node objectNode = objectNodeList.item(objectCounter);
		          
		            if (objectNode.getNodeType() == Node.ELEMENT_NODE) 
		            {
		                Element objectElement = (Element) objectNode;
                        String label = objectElement.getElementsByTagName("name").item(0).getTextContent();
		                
		                NodeList polygonNodeList = objectElement.getElementsByTagName("polygon");
		                
		                for (int p = 0; p < polygonNodeList.getLength(); p++)
		                {
		                    Node polygonNode = polygonNodeList.item(p);
		                    	                    
		                    if (objectNode.getNodeType() == Node.ELEMENT_NODE) 
		                    {
		                        Element pElement = (Element) polygonNode;
   	                            ParserPolygon poly = new ParserPolygon(label);
   	                            
		                        NodeList points = pElement.getElementsByTagName("pt");
		                        Point2D[] polyPoints = new Point2D[points.getLength()];
                                
		                        for (int q = 0; q < points.getLength(); q++)
		                        {
		                            Node point = points.item(q);
		                            if (point.getNodeType() == Node.ELEMENT_NODE) 
		                            {
		                                Element pt = (Element) point; 
		                                double x = new Double(pt.getElementsByTagName("x").item(0).getTextContent());
		                                double y = new Double(pt.getElementsByTagName("y").item(0).getTextContent());
		                                Point2D s = new Point2D.Double();
		                                s.setLocation(x, y);
		                                polyPoints[q] = s;		                                
		                            }
	                            }
		                        poly.setPoints(polyPoints);
		                        this.polygons[objectCounter] = poly;
		                    }
		                }		     
		            }
		        } 
		    } 
		    catch (Exception e) 
		    {
		        e.printStackTrace();
		    }			
		}
		Arrays.sort(polygons, new Comparator<ParserPolygon>()
        {
		    public int compare(ParserPolygon one, ParserPolygon two)
		    {
		        return one.label.compareTo(two.label);
		    }
        });
	}

	public String toString()
	{
	    StringBuilder sb = new StringBuilder();
	    sb.append(this.xmlFilename + "\n");
	    sb.append("---\n");
	    for (ParserPolygon p: this.polygons)
	    {
	        sb.append(p.toString());
	    }
	    sb.append("\n");
	    return sb.toString();
	}
}
