package i5.las2peer.services.queryVisualization.database;

import java.util.HashMap;

public class FilterMap extends HashMap<StringPair, SQLFilterSettings> {

	private static final long serialVersionUID = 971003158570673502L;

	@Override
	public SQLFilterSettings get(Object key) {
		if (!(key instanceof StringPair)) {
			throw new IllegalArgumentException("You need to pass a StringPair here!");
		}
		return super.get(key);
	}

	@Override
	public boolean containsKey(Object key) {
		if (!(key instanceof StringPair)) {
			throw new IllegalArgumentException("You need to pass a StringPair here!");
		}
		return super.containsKey(key);
	}

	@Override
	public SQLFilterSettings remove(Object key) {
		if (!(key instanceof StringPair)) {
			throw new IllegalArgumentException("You need to pass a StringPair here!");
		}
		return super.remove(key);
	}

	@Override
	public boolean containsValue(Object value) {
		if (!(value instanceof SQLFilterSettings)) {
			throw new IllegalArgumentException("You need to pass SQLFilterSettings here!");
		}
		return super.containsValue(value);
	}

}
