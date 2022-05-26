package i5.las2peer.services.mobsos.queryVisualization.dal;

import java.io.Serializable;

import javax.validation.constraints.NotNull;

import net.minidev.json.JSONArray;

public class QVQueriesInformation implements Serializable {
	private static final long serialVersionUID = 1L;

	@NotNull
	private boolean cache;
	@NotNull
	private String dbkey;
	@NotNull
	private JSONArray queries;
	@NotNull
	private boolean save;

	public boolean isSave() {
		return save;
	}

	public void setSave(boolean save) {
		this.save = save;
	}

	public boolean isCache() {
		return cache;
	}

	public void setCache(boolean cache) {
		this.cache = cache;
	}

	public String getDbkey() {
		return dbkey;
	}

	public void setDbkey(String dbkey) {
		this.dbkey = dbkey;
	}

	public JSONArray getQueries() {
		return queries;
	}

}
