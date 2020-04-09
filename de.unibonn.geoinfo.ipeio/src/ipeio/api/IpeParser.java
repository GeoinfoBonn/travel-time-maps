package ipeio.api;

import java.awt.geom.AffineTransform;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.Characters;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import ipeio.api.IpeFactory;
import ipeio.api.IpeObject;
import ipeio.api.IpeObject.Document;

public class IpeParser {

	
	private StringBuffer log = new StringBuffer();
	private IpeFactory factory = new IpeFactory();
	private String currentLayer = "undefined";
	
	public static interface Options{
		/**
		 * If this option is set, the style sheets at the beginning are read. 
		 */
		default boolean readStyleFiles(){
			return false;
		}
		
		/** 
		 * If this option is set, the objects are directly transformed when they are created.
		 **/
		default boolean directTransform(){
			return true;
		}
		
		/**
		 * 
		 */
		default AffineTransform basicTransform(){
			return new AffineTransform();
		}
	}
	
	@Override
	public String toString() {
		return log.toString() + " "+factory.toString();
	}
	
	public Document parseFile(InputStream in) throws XMLStreamException{
		return parseFile(in,new Options(){});
	}
	
	public Document parseFile(InputStream in, Options options) throws XMLStreamException{
		XMLInputFactory factory = XMLInputFactory.newInstance();
		XMLEventReader parser = factory.createXMLEventReader( in );
		
		if(options.readStyleFiles()){
			throw new RuntimeException("Option not yet supported.");
		}

		while ( parser.hasNext() )
		{
		  XMLEvent event = parser.nextEvent();				
		  switch ( event.getEventType() )
		  {
		    case XMLStreamConstants.START_ELEMENT:
		      StartElement start = event.asStartElement();
		 	  if(start.getName().toString().equals("ipe")){
		 		  Document  doc = (Document)readIpeObject(parser, start, options, options.basicTransform());
     		 	  if(doc == null){
			 		  return new Document();
			 	  }
     		 	  return doc;
		 	  }
		  }		
		}		
		return new Document();
	}
	
	interface GeometryFactory{
		void readAttributes(StartElement element);
		void readContent(String content);
	}
	
	private AffineTransform createTransform(Map<String,String> attributes){
		String matrix = attributes.get("matrix");
		if(matrix != null){
			String words [] = matrix.split(" ");
			double c [] = new double[words.length];
			for(int i=0; i < words.length; i++){
				c[i] = Double.parseDouble(words[i]);
			}
			return new AffineTransform(c);
		}
		return new AffineTransform();
	}
	
	private IpeObject readIpeObject(XMLEventReader parser, StartElement origin, Options options, AffineTransform trans) throws XMLStreamException{
	
		
		Map<String,String> objAttributes = new HashMap<>();
		for ( Iterator<?> attributes = origin.getAttributes();
				attributes.hasNext(); ){
			Attribute attribute = (Attribute) attributes.next();
			objAttributes.put(attribute.getName().toString(),attribute.getValue());
		}
		if(objAttributes.containsKey("layer")){
			currentLayer = objAttributes.get("layer");
		}
		if(objAttributes.containsKey("matrix")){
			System.out.println();
		}
		List<IpeObject> children = new ArrayList<>();
		StringBuffer content = new StringBuffer();
		AffineTransform currentTrans = new AffineTransform(trans);
		if(options.directTransform()){
			currentTrans.concatenate(createTransform(objAttributes));
		}
		while(parser.hasNext()){
			 XMLEvent event = parser.nextEvent();
			 switch(event.getEventType()){
			 
			 case XMLStreamConstants.START_ELEMENT:
				 StartElement start = event.asStartElement();
				 IpeObject res = readIpeObject(parser, start,options,currentTrans);
				 if(res != null){
					 children.add(res);
				 }
				 break;
			
			 case XMLStreamConstants.CHARACTERS:	 
				  Characters characters = event.asCharacters();
				  if(!characters.isWhiteSpace()){
					  content.append(characters.getData());
				  }
				  break;		 
			 
			 case XMLStreamConstants.END_ELEMENT:
				 EndElement end = event.asEndElement();
				 String name = end.getName().toString();					
				 if(name.equals(origin.getName().toString())){
					 return factory.create(name,content.toString(),objAttributes,
							               children,currentTrans,options.directTransform(),currentLayer);
				 }
				 break;
			 }
		}
		throw new RuntimeException("Could not find end of page");
	}
	

}
