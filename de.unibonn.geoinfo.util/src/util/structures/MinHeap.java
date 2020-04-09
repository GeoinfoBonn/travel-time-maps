package util.structures;

import java.util.ArrayList;
import java.util.List;


public class MinHeap<T> {
	
	public static class HeapItem<T>{
		
		private double key;
		private T value;
		private int    index;
		
	    private HeapItem(double key, T value, int index){
	    	this.key = key;
	    	this.value = value; 
	    	this.index = index;
	    }
		public double getKey() {
			return key;
		}

		public void setKey(double key) {
			this.key = key;
		}

		public int getIndex() {
			return index;
		}

		public void setIndex(int index) {
			this.index = index;
		}

		public T getValue() {
			return value;
		}

	}
	
	private ArrayList<HeapItem<T>> items = new ArrayList<HeapItem<T>>();
	
	public MinHeap(List<Double> keys, List<T> values) {
		for(int i=0; i < keys.size(); ++i){
			double key = keys.get(i);
			T value = values.get(i);
			items.add(new HeapItem<T>(key,value,i));
		}
		for(int i = keys.size()-1; i>=0; --i){
			heapify(items.get(i));
		}
	}
	
	public MinHeap() {
	
	}


	public void decreaseKey(HeapItem<T> item, double key) {
		item.setKey(key);
		while(getParent(item)!= null && getParent(item).getKey()>key){
			exchange(item,getParent(item));
		}		
	}	
	
	private void exchange(HeapItem<T> item1, HeapItem<T> item2){
		items.set(item1.getIndex(),item2);
		items.set(item2.getIndex(),item1);
		int temp = item1.getIndex();
		item1.setIndex(item2.getIndex());
		item2.setIndex(temp);
	}


	public HeapItem<T> insertItem(double key, T value) {
		HeapItem<T> item = new HeapItem<T>(Double.POSITIVE_INFINITY,value,items.size());
		items.add(item);
		decreaseKey(item, key);
		return item;
	}
	


	public HeapItem<T> getMin() {
		return getRoot();
	}


	public HeapItem<T> extractMin() {
		HeapItem<T> minItem = getMin();
		HeapItem<T> lastItem = items.get(items.size()-1);
		exchange(minItem, lastItem);
		items.remove(items.size()-1);
		heapify(lastItem);
		return minItem;
	}


	public HeapItem<T> getRightChild(HeapItem<T> item) {
		if(rightChildIndex(item.getIndex())>=items.size()){
			return null;
		}
		return items.get(rightChildIndex(item.getIndex()));
	}


	public HeapItem<T> getLeftChild(HeapItem<T> item) {
		if(leftChildIndex(item.getIndex())>=items.size()){
			return null;
		}
		return items.get(leftChildIndex(item.getIndex()));
	}



	public HeapItem<T> getRoot() {
		return items.isEmpty() ? null : items.get(0);
	}
	

	public HeapItem<T> getParent(HeapItem<T> item) {
		if(item == getRoot()){return null;}
		return items.get(parentIndex(item.getIndex()));
	}
	
	
    private int leftChildIndex(int i) {
	        return i * 2 + 1;
	}
	    
	    
	private int rightChildIndex(int i) {
	        return i * 2 + 2;
	}
	
	private int parentIndex(int i){
			return (i+1)/2-1;
	}
	

	public int height() {
		return (int)(Math.log(size())/Math.log(2))+1;
	}


	public boolean hasLeftChild(HeapItem<T> item) {
		return getLeftChild(item)!=null;
	}


	public boolean hasRightChild(HeapItem<T> item) {
		return getRightChild(item)!=null;
	}
	
	
	public void heapify(HeapItem<T> item){
		HeapItem<T> child = getLeftChild(item);
		if(child == null || (hasRightChild(item) && getRightChild(item).getKey() < child.getKey())){
			child = getRightChild(item);
		}
		if(child != null && child.getKey() < item.getKey()){
			exchange(child,item);
			heapify(item);
		}
	}
	

	public int size() {
		return items.size();
	}
	
	
}