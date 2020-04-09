package ipeio.api;

import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class IpeObject{
	private Map<String,String> attributes;
	private String             type;
	private List<IpeObject>    children = new ArrayList<>();
	
	public static interface Visitor{		
		public void visit(IpeObject object);		
	}
	
	public static interface HasLayer{
		void setLayer(String layer);
		String getLayer();
		default boolean belongsTo(String layer){
			return getLayer().equals(layer);
		}
	}
	
	/**
	 * Object filter used when collecting shapes from ipe objects. 
	 *
	 */
	public static interface ObjectFilter{
		
		/**
		 * return true if the given geometry should be collected
		 */
		default boolean collectGeometry(Geometry geometry){
			return true;
		}
		
		/**
		 * return true if the given geometry should be collected
		 */
		default boolean collectLabel(Label label){
			return true;
		}
		
		
		/**
		 * return true if the children of the given object should be ignored
		 */
		default boolean excludeChildren(IpeObject object){
			return false;
		}
		
		/**
		 * return true if the object should be ignored
		 */
		default boolean excludeObject(IpeObject object){
			return false;
		}
		
		/**
		 * return true if the page should be ignored. 
		 */		
		default boolean excludePage(Page page, int index){
			return false;
		}
	}
	
	public void visit(Visitor visitor){
		visitor.visit(this);
		for(IpeObject child : children){
			child.visit(visitor);
		}
	}
	
	public List<Shape> collectUntransformedShapes(){
		List<Shape> shapes = new ArrayList<>();
		visit(new Visitor() {			
			@Override
			public void visit(IpeObject object) {
				if(object instanceof Geometry){
					Geometry geo = (Geometry) object;
					shapes.add(geo.getShape());
				}
			}
		});
		return shapes;
	}
	
	public List<Shape> collectShapes(){
		return collectShapes(new AffineTransform(),new ObjectFilter() {});
	}
	
	public List<Shape> collectShapes(ObjectFilter objectFilter){
		return collectShapes(new AffineTransform(),objectFilter);
	}
	
	public List<Shape> collectShapes(AffineTransform at){
		return collectShapes(at,new ObjectFilter(){});
	}
	
    public List<Shape> collectShapes(AffineTransform basic, ObjectFilter filter){
		List<Shape> shapes = new ArrayList<>();
		if(filter.excludeObject(this)){
			return shapes;
		}
		
		
		AffineTransform trans = new AffineTransform(basic);
	    if(this instanceof TransformableObject){
	    	trans.concatenate(((TransformableObject)this).getTransform());
	    }	
		if(this instanceof Geometry){
			Geometry geo = (Geometry) this;
			if(filter.collectGeometry(geo)){
				shapes.add(trans.createTransformedShape(geo.getShape()));
			}
		}
		if(!filter.excludeChildren(this)){
			for(IpeObject child : children){
				shapes.addAll(child.collectShapes(trans,filter));
			}
		}
		return shapes;
	}
    
    
	public List<Entry<String,Point2D>> collectLabels(){
		return collectLabels(new AffineTransform(),new ObjectFilter() {});
	}
	
	public List<Entry<String,Point2D>> collectLabels(ObjectFilter objectFilter){
		return collectLabels(new AffineTransform(),objectFilter);
	}
	
	public List<Entry<String,Point2D>> collectLabels(AffineTransform at){
		return collectLabels(at,new ObjectFilter(){});
	}
	
    public List<Entry<String,Point2D>> collectLabels(AffineTransform basic,ObjectFilter filter ){
		List<Entry<String,Point2D>> objects = new ArrayList<>();
		if(filter.excludeObject(this)){
			return objects;
		}
		
		
		AffineTransform trans = new AffineTransform(basic);
	    if(this instanceof TransformableObject){
	    	trans.concatenate(((TransformableObject)this).getTransform());
	    }	
		if(this instanceof Label){
			Label label = (Label) this;
			if(filter.collectLabel(label)){
				objects.add(new AbstractMap.SimpleEntry<String,Point2D>(label.text,trans.transform(label.getCoords(),null)));
			}
		}
		if(!filter.excludeChildren(this)){
			for(IpeObject child : children){
				objects.addAll(child.collectLabels(trans,filter));
			}
		}
		return objects;
	}
	
	public List<IpeObject> getChildren() {
		return children;
	}
	
	public String getType() {
		return type;
	}
	
	public String getAttribute(String key){
		return attributes.get(key);
	}
	
	public boolean hasAttribute(String key, String value){
		String val = getAttribute(key);		
		return val != null && value.equals(val);
	}
	
	public IpeObject(String type, Map<String, String> attributes, List<IpeObject> children) {
		super();
		this.attributes = attributes;
		this.type = type;
		this.children = children;
	}
	

	
	public static class Style extends IpeObject{

		public Style(String name, Map<String, String> attributes, List<IpeObject> children) {
			super(name, attributes, children);
		}
		
	}
	
	public static class Document extends IpeObject{
		private List<Style> styles = new ArrayList<>();
		private List<Page>  pages = new ArrayList<>(); 
		public Document(String name, Map<String, String> attributes, List<IpeObject> children) {
			super(name, attributes, children);
			for(IpeObject obj : children){
				if(obj instanceof Style){
					styles.add((Style)obj);
				}
				if(obj instanceof Page){
					pages.add((Page)obj);
				}
			}
		}		
		
		public Document() {
			super("document",new HashMap<>(),new ArrayList<>());
		}		
		
		
		public List<Shape> collectShapesFromPages(ObjectFilter filter){
			return collectShapesFromPages(new AffineTransform(),filter);
		}
		
		public List<Shape> collectShapesFromPages(AffineTransform at){
			return collectShapesFromPages(at,new ObjectFilter(){});
		}
		
		
		public List<Shape> collectShapesFromPages(){
			return collectShapesFromPages(new AffineTransform(),new ObjectFilter(){});
		}
		
		public List<Shape> collectShapesFromPages(AffineTransform at, ObjectFilter filter){
			List<Shape> shapes = new ArrayList<>();
			for(int i=0; i < pages.size(); i++){
				IpeObject.Page page = pages.get(i);
				if(!filter.excludePage(page,i)){
					shapes.addAll(page.collectShapes(at,filter));
				}
			}
			return shapes;
		}
		
		public List<Shape> collectUntransformedShapesFromPages(){
			List<Shape> shapes = new ArrayList<>();
			for(IpeObject.Page page : pages){
				shapes.addAll(page.collectUntransformedShapes());
			}
			return shapes;
		}
		
	}
	
	
	public static class TransformableObject extends IpeObject{
		private AffineTransform transform = new AffineTransform();
		
		public TransformableObject(String name, Map<String, String> attributes, List<IpeObject> children) {
			super(name, attributes, children);
		}
		
		public TransformableObject(String name, Map<String, String> attributes, List<IpeObject> children, 
				AffineTransform transform) {
			super(name, attributes, children);
			this.transform = transform;
		}
		
		public AffineTransform getTransform() {
			return transform;
		}
		
	}
	
	public static class Page extends IpeObject{

		public Page(String name, Map<String, String> attributes, List<IpeObject> children) {
			super(name, attributes, children);
		}
		
	}
	
	public static class Group extends TransformableObject implements HasLayer{

		String layer;
		
		public Group(String name, Map<String, String> attributes, List<IpeObject> children) {
			super(name, attributes, children);
		}

		public Group(String name, Map<String, String> attributes, List<IpeObject> children, AffineTransform transform) {
			super(name, attributes, children, transform);
		}
		
		@Override
		public void setLayer(String layer) {
			this.layer = layer;
		}
		
		@Override
		public String getLayer() {
			return layer;
		}		
	}
	
	public static class Label extends TransformableObject implements HasLayer{

		public Label(String name, Map<String, String> attributes, String text, Point2D p) {
			super(name, attributes, new ArrayList<>());
			this.text = text;
			this.coords = p;
		}
		
		public Label(String name, Map<String, String> attributes, String text, Point2D p, AffineTransform transform) {
			super(name, attributes, new ArrayList<>(),transform);
			this.coords = p;
			this.text = text;
		}
		

		private String text;
		private Point2D coords;
		private String layer;
		
		
		@Override
		public void setLayer(String layer) {
			this.layer = layer;
		}

		@Override
		public String getLayer() {
			return layer;
		}
		
		public String getText() {
			return text;
		}
		
		public Point2D getCoords() {
			return coords;
		}
		
	}
	
	public static class Geometry extends TransformableObject implements HasLayer{

		private Shape shape;		
		private String layer;
		public Geometry(String name, Map<String, String> attributes, Shape shape) {
			super(name, attributes, new ArrayList<>());
			this.shape = shape;
		}		
		
		
		
		public Geometry(String name, Map<String, String> attributes,
				Shape shape, AffineTransform transform) {
			super(name, attributes, new ArrayList<>(), transform);
			this.shape = shape;
		}
		
		public Geometry(String name, Map<String, String> attributes, List<IpeObject> children) {
			super(name, attributes, children);
			// TODO Auto-generated constructor stub
		}
		
		public Shape getShape() {
			return shape;
		}
		
		@Override
		public void setLayer(String layer) {
			this.layer = layer;
		}
		
		@Override
		public String getLayer() {
			return layer;
		}
		
	}
}