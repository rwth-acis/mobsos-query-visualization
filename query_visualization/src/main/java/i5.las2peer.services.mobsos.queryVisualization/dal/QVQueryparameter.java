package i5.las2peer.services.mobsos.queryVisualization.dal;

import java.io.Serializable;

import javax.validation.constraints.NotNull;

public class QVQueryparameter implements Serializable {
	private static final long serialVersionUID = 1L;

	@NotNull
	private String[] queryparams;

	public String[] getQueryparams() {
		return queryparams;
	}

	public void setQuery(String[] queryparams) {
		this.queryparams = queryparams;
	}

}
