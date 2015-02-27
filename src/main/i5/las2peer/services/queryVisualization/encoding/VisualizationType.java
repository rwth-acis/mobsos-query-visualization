package i5.las2peer.services.queryVisualization.encoding;

/**
 * VisualizationType.java
 * <br>
 * Enumeration for the visualization types.
 * <br>
 * 
 * 0  CSV<br>
 * 1  JSON<br>
 * 2  HTMLTABLE<br>
 * 3  XML<br>
 * 4  GOOGLEPIECHART<br>
 * 5  GOOGLEBARCHART<br>
 * 6  GOOGLELINECHART<br>
 * 7  GOOGLETIMELINECHART<br>
 * 8  GOOGLERADARCHART<br>
 * 9  GOOGLETABLE<br>
 * 10 GRAPH
 * 
 */
public enum VisualizationType {
	CSV,
	JSON,
	HTMLTABLE,
	XML,
	GOOGLEPIECHART, 
	GOOGLEBARCHART, 
	GOOGLELINECHART,
	GOOGLETIMELINECHART,
	GOOGLERADARCHART,
	GOOGLETABLE,
	GRAPH;
	
	
	
	/* Some auxiliary functions */
    private static VisualizationType[] values = null;
    public static VisualizationType fromInt(int i) {
        if(VisualizationType.values == null) {
        	VisualizationType.values = VisualizationType.values();
        }
        return VisualizationType.values[i];
    }
}