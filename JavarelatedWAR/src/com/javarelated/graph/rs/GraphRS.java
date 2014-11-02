package com.javarelated.graph.rs;

import javax.ejb.EJB;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.javarelated.graph.ejb.GraphHistory;

@Path("/graph")
public class GraphRS {

	@EJB private GraphHistory graphHistory;
	
	@GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/history")
    public String getHistory(@QueryParam("interval") int interval) {
		return graphHistory.getData(interval);
    }
}
