package graph.types;

public interface IsoVertex extends Comparable<IsoVertex> {
    /*
     * // Street- or Stopname private String name = null; private int id = 0;
     */

    public String getName();

    public void setName(String name);

    public int getId();

    public void setId(int id);

    public int compareTo(IsoVertex otherVertex);
}
