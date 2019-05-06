package com.camunda.bpm;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Stack;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.impl.instance.BusinessRuleTaskImpl;
import org.camunda.bpm.model.bpmn.impl.instance.GatewayImpl;
import org.camunda.bpm.model.bpmn.impl.instance.ServiceTaskImpl;
import org.camunda.bpm.model.bpmn.impl.instance.UserTaskImpl;
import org.camunda.bpm.model.bpmn.instance.CallActivity;
import org.camunda.bpm.model.bpmn.instance.EndEvent;
import org.camunda.bpm.model.bpmn.instance.SequenceFlow;
import org.camunda.bpm.model.bpmn.instance.StartEvent;
import org.camunda.commons.utils.IoUtil;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;

/**
 * @author Arnav Bhati
 *
 */
public class Graph {

	public static BpmnModelInstance modelInstance = null;
	//public static String source = "approveInvoice";
	//public static String target = "invoiceProcessed";

	
	public static void main(String args[]) throws Exception {
		// Create a Closable HTTP Client
		CloseableHttpClient httpclient = HttpClients.createDefault();

		try {
			HttpGet httpget = new HttpGet(
					"https://elxkoom6p4.execute-api.eu-central-1.amazonaws.com/prod/engine-rest/process-definition/key/invoice/xml");
			System.out.println("Executing request " + httpget.getRequestLine());
			CloseableHttpResponse response = httpclient.execute(httpget);
			try {
				System.out.println("-----------------------------------------------------------------------------");
				System.out.println("Response received is:" + response.getStatusLine());
				HttpEntity entity = response.getEntity();

				if (entity != null) {
					InputStream inStream = entity.getContent();
					try {
						String theString = IoUtil.inputStreamAsString(inStream);
						modelInstance = Bpmn.readModelFromStream(IoUtil.stringAsInputStream(fromJson(theString)));

						// Call the method to print the path
						System.out.println(doBFSShortestPath(args[0], args[1]));
					} catch (Exception ex) {
						throw ex;
					} finally {
						inStream.close();
					}
				}
			} finally {
				response.close();
			}
		} finally {
			httpclient.close();
		}
	}

	
	/**
	 * @param theString
	 * @return
	 * @throws JsonParseException
	 * @throws JsonMappingException
	 * @throws IOException
	 */
	public static String fromJson(String theString) throws JsonParseException, JsonMappingException, IOException {
		Response res = new ObjectMapper().readValue(theString, Response.class);

		return res.getBpmn20Xml();
	}

	
	/**
	 * @param id
	 * @return
	 */
	public static ArrayList<String> traverseGraphOnId(String id) {
		ArrayList<String> out = new ArrayList<String>();
		if (modelInstance.getModelElementById(id) instanceof GatewayImpl) {
			GatewayImpl gImpl = (GatewayImpl) modelInstance.getModelElementById(id);
			for (SequenceFlow sq : gImpl.getOutgoing()) {
				out.add(sq.getTarget().getId());
			}

		} else if (modelInstance.getModelElementById(id) instanceof SequenceFlow) {
			SequenceFlow gImpl = (SequenceFlow) modelInstance.getModelElementById(id);
			// traverseId(gImpl.getTarget().getId(),target);
			out.add(gImpl.getTarget().getId());
		} else if (modelInstance.getModelElementById(id) instanceof BusinessRuleTaskImpl) {

			BusinessRuleTaskImpl tskImpl = (BusinessRuleTaskImpl) modelInstance.getModelElementById(id);
			for (SequenceFlow sq : tskImpl.getOutgoing()) {
				out.add(sq.getTarget().getId());
			}
		}

		else if (modelInstance.getModelElementById(id) instanceof UserTaskImpl) {

			UserTaskImpl tskImpl = (UserTaskImpl) modelInstance.getModelElementById(id);
			for (SequenceFlow sq : tskImpl.getOutgoing()) {
				out.add(sq.getTarget().getId());
			}

		} else if (modelInstance.getModelElementById(id) instanceof ServiceTaskImpl) {

			ServiceTaskImpl tskImpl = (ServiceTaskImpl) modelInstance.getModelElementById(id);
			for (SequenceFlow sq : tskImpl.getOutgoing()) {
				out.add(sq.getTarget().getId());
			}

		} else if (modelInstance.getModelElementById(id) instanceof CallActivity) {

			CallActivity tskImpl = (CallActivity) modelInstance.getModelElementById(id);
			for (SequenceFlow sq : tskImpl.getOutgoing()) {
				out.add(sq.getTarget().getId());
			}

		} else if (modelInstance.getModelElementById(id) instanceof EndEvent) {

			EndEvent tskImpl = (EndEvent) modelInstance.getModelElementById(id);
			for (SequenceFlow sq : tskImpl.getOutgoing()) {
				out.add(sq.getTarget().getId());
			}

		} else if (modelInstance.getModelElementById(id) instanceof StartEvent) {

			StartEvent tskImpl = (StartEvent) modelInstance.getModelElementById(id);
			for (SequenceFlow sq : tskImpl.getOutgoing()) {
				out.add(sq.getTarget().getId());
			}

		}

		return out;

	}

	/**
	 * @param id
	 * @return Arraylist of Edges for a given node
	 */
	public static ArrayList<String> getOutEdges(String id) {
		return traverseGraphOnId(id);
	}

	/**
	 * @param currentSrc
	 * @param node
	 * @return boolean 
	 * 
	 * This method checks if the node is neighbor of current node.
	 */
	public static boolean isNeighbor(String currentSrc, String node) {
		return traverseGraphOnId(node).contains(currentSrc);
	}

	/**
	 * @param source
	 * @param dest
	 * @return ArrayList of path
	 * 
	 * This method does a BFS and provides the shortest path
	 */
	public static ArrayList<String> doBFSShortestPath(String source, String dest) {
		ArrayList<String> shortestPathList = new ArrayList<String>();
		HashMap<String, Boolean> visited = new HashMap<String, Boolean>();

		if (source.equals(dest)) {
			System.out.println("Source and destination cannot be same");
			return null;
		}
		Queue<String> queue = new LinkedList<String>();
		Stack<String> pathStack = new Stack<String>();

		queue.add(source);
		pathStack.add(source);
		visited.put(source, true);

		while (!queue.isEmpty()) {
			String u = queue.poll();
			ArrayList<String> adjList = getOutEdges(u);

			for (String v : adjList) {
				if (!visited.containsKey(v)) {
					queue.add(v);
					visited.put(v, true);
					pathStack.add(v);
					if (u.equals(dest))
						break;
				}
			}
		}

		// To find the path
		String node, currentSrc = dest;
		shortestPathList.add(dest);
		while (!pathStack.isEmpty()) {
			node = pathStack.pop();
			if (isNeighbor(currentSrc, node)) {
				shortestPathList.add(node);
				currentSrc = node;
				if (node.equals(source))
					break;
			}
		}
		ArrayList<String> out = new ArrayList<String>();
		for (int i = shortestPathList.size() - 1; i >= 0; i--) {
			out.add(shortestPathList.get(i));
		}
		return out;
	}
}
