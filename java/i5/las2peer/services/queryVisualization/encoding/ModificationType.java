package i5.las2peer.services.queryVisualization.encoding;


/**
 * ModificationType.java
 * <br>
 * Enumeration for the modification types.
 */
public enum ModificationType {
	IDENTITIY,
	LOGARITHMIC,
	NORMALIZATION;
	
	/* Some auxiliary functions */
    private static ModificationType[] values = null;
    public static ModificationType fromInt(int i) {
        if(ModificationType.values == null) {
        	ModificationType.values = ModificationType.values();
        }
        return ModificationType.values[i];
    }	
}