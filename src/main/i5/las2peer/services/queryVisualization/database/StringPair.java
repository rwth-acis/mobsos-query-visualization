package i5.las2peer.services.queryVisualization.database;

public class StringPair {
	final private String key1;
	final private String key2;

	public StringPair(String key1, String key2) {
		this.key1 = key1;
		this.key2 = key2;
	}

	public String getKey1() {
		return key1;
	}

	public String getKey2() {
		return key2;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((key1 == null) ? 0 : key1.hashCode());
		result = prime * result + ((key2 == null) ? 0 : key2.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		StringPair other = (StringPair) obj;
		if (key1 == null) {
			if (other.key1 != null)
				return false;
		} else if (!key1.equals(other.key1))
			return false;
		if (key2 == null) {
			if (other.key2 != null)
				return false;
		} else if (!key2.equals(other.key2))
			return false;
		return true;
	}

	@Override
	protected Object clone() throws CloneNotSupportedException {
		return new StringPair(key1, key2);
	}

	@Override
	public String toString() {
		StringBuilder b = new StringBuilder();
		return b.append("Key1: ").append(key1).append(" Key2: ").append(key2).toString();
	}
	

	
}
