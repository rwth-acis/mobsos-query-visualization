package i5.las2peer.services.mobsos.queryVisualization.dal;

import java.io.Serializable;

import javax.validation.constraints.NotNull;

public class QVQuery implements Serializable {
	private static final long serialVersionUID = 1L;

	@NotNull
	private String query;

	public String getQuery() {
		return query;
	}

	public void setQuery(String query) {
		this.query = query;
	}

}
