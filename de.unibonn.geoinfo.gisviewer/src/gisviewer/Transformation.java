package gisviewer;

/**
 * a transformation between image coordinates (row, column) and map coordinates (x, y)
 * @author haunert
 */
public class Transformation {
    
    /**
     * scale factor
     */
    private double m;
    
    /**
     * origin of the map coordinate system in image coordinates
     */
    private int ColumnOrigin, RowOrigin;
        
    /**
     * standard constructor setting scale to 1 and origin to (0,0)
     */
    public Transformation() {
        m = 1.0;
        ColumnOrigin = 0;
        RowOrigin = 0;
    }
    
    /**
     * constructor setting transformation parameters as specified
     * @param m: the scale factor
     * @param ColumnOrigin: column of the origin of the map coordinate system
     * @param RowOrigin: row of the origin of the map coordinate system
     */
    public Transformation(double m, int ColumnOrigin, int RowOrigin) {
        this.m = m;
        this.ColumnOrigin = ColumnOrigin;
        this.RowOrigin = RowOrigin;
    }
    
    /**
     * getter for accessing the scale factor
     * @return the scale factor
     */
    public double getM() {
        return m;
    }
    
    /**
     * setter for setting the scale factor
     * @param myM: the scale factor
     */
    public void setM(double myM) {
        m = myM;
    }
    
    /**
     * getter for accessing the column of the map coordinate system's origin
     * @return the column
     */
    public int getColumnOrigin() {
        return ColumnOrigin;
    }
    
    /**
     * setter for setting the column of the map coordinate system's origin
     * @param cOrigin: the column
     */
    public void setColumnOrigin(int cOrigin) {
        ColumnOrigin = cOrigin;
    }
    
    /**
     * getter for accessing the row of the map coordinate system's origin
     * @return the row
     */
    public int getRowOrigin() {
        return RowOrigin;
    }
    
    /**
     * setter for setting the row of the map coordinate system's origin
     * @param rOrigin: the row
     */
    public void setRowOrigin(int rOrigin) {
        RowOrigin = rOrigin;
    }
       
    /**
     * method for computing the row for a given y-coordinate
     * @param y: the y-coordinate 
     * @return the row
     */
    public int getRow(double y) {
        return RowOrigin - (int) Math.rint(m * y);
    }
    
    /**
     * method for computing the column for a given x-coordinate
     * @param x: the x-coordinate
     * @return the column
     */
    public int getColumn(double x) {
        return ColumnOrigin + (int) Math.rint(m * x);
    }
    
    /**
     * method for computing the x-coordinate for a given column
     * @param c: the column
     * @return the x-coordinate
     */
    public double getX(int c) {
        return (c - ColumnOrigin) / m;
    }
    
    /**
     * method for computing the y-coordinate for a given row
     * @param r: the row
     * @return the y-coordinate
     */
    public double getY(int r) {
        return (RowOrigin - r) / m;
    }
    
    
}