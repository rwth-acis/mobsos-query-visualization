package i5.las2peer.services.mobsos.queryVisualization.dal;

import java.io.Serializable;

import javax.validation.constraints.NotNull;

public class QVQueryInformation implements Serializable {
	private static final long serialVersionUID = 1L;

	@NotNull
	private boolean cache;
	@NotNull
	private String dbkey;
	@NotNull
	private String height;
	@NotNull
	private String width;
	@NotNull
	private int modtypei;
	@NotNull
	private String query;
	@NotNull
	private String[] queryparams;
	@NotNull
	private String title;
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

	public String getHeight() {
		return height;
	}

	public void setHeight(String height) {
		this.height = height;
	}

	public String getWidth() {
		return width;
	}

	public void setWidth(String width) {
		this.width = width;
	}

	public int getModtypei() {
		return modtypei;
	}

	public void setModtypei(int modtypei) {
		this.modtypei = modtypei;
	}

	public String getQuery() {
		return query;
	}

	public void setQuery(String query) {
		this.query = query;
	}

	public String[] getQueryparams() {
		return queryparams;
	}

	public void setQueryparams(String[] queryparams) {
		this.queryparams = queryparams;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

}

