/**
 * This file is part of OSM2GpsMid 
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2 as published by
 * the Free Software Foundation.
 *
 * @version $Revision$ ($Name$)
 *
 * Copyright (C) 2007 Harald Mueller
 */
package de.ueller.osmToGpsMid.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * @author hmueller
 *
 */
public class Path {
	private List<Node> nodeList = new ArrayList<Node>();
//	private LinkedList<SubPath> subPathList;
//	private SubPath currentSeg = null;
	
	public Path() {
		super();
	}
	
	public Path(ArrayList<Node> newNodeList) {
		nodeList = newNodeList;
	}

	
//	public Path(LinkedList<SubPath> subPathList) {
//		super();
//		this.subPathList = subPathList;
//	}

	public void add(Node n) {
		nodeList.add(n);
	}
	
//	protected void addNewSegment() {
//		if (subPathList == null) {
//			subPathList = new LinkedList<SubPath>();
//		}
//		if (currentSeg != null && currentSeg.getLineCount() >0) {
//			currentSeg = null;
//		}
//	}
	
	@Deprecated
	public boolean isMultiPath() {
//		if (subPathList != null) {
//			if (subPathList.size() == 1) {
//				return false;
//			} else {
//				return true;
//			}
//		}
		return false;
	}

	@Deprecated
	public int getPathCount() {
		return 1;
	}

	/**
	 * Replaces node1 with node2 in this path.
	 * @param node1 Node to be replaced
	 * @param node2 Node by which to replace node1.
	 */
	public void replace(Node node1, Node node2) {
		int occur = nodeList.indexOf(node1);
		while (occur != -1) {
			nodeList.set(occur, node2);
			occur = nodeList.indexOf(node1);
		}
	}

	/** replaceNodes lists nodes and by which nodes they have to be replaced.
	 * This method applies this list to this path.
	 * @param replaceNodes Hashmap of pairs of nodes
	 */
	public void replace(HashMap<Node, Node> replaceNodes) {
		for (int i = 0; i < nodeList.size(); i++) {
			Node newNode = replaceNodes.get(nodeList.get(i));
			if (newNode != null) {
				nodeList.set(i, newNode);	
			}
		}		
	}


	/**
	 * 
	 */
	public int getLineCount() {
		if (nodeList == null) {
			return 0;
		}
		if (nodeList.size() > 1) {
			return (nodeList.size() - 1);
		}
		return 0;
	}
	public int getNodeCount() {
		if (nodeList == null) {
			return 0;
		}
		if (nodeList.size() > 1) {
			return (nodeList.size());
		}
		return 0;
	}
	
	/**
	 * split this Path at the half subPath elements
	 * @return null if the Path already have one Subpath, 
	 *         a new Path with the rest after split.  
	 */
	public Path split() {
		if (nodeList == null || getLineCount() < 2) {
			return null;
		}
		int splitP = nodeList.size() / 2;
		ArrayList<Node> newNodeList = new ArrayList<Node>();
		int a = 0;
		for (Iterator<Node> si = nodeList.iterator(); si.hasNext();) {
			Node t = si.next();
			if (a >= splitP) {
				newNodeList.add(t);
				if (a > splitP) {
					si.remove();
				}
			}
			a++;
		}
		Path newPath = new Path(newNodeList);
//		System.out.println("old SubPath has " + getLineCount()+ " lines");
//		System.out.println("new SubPath has " + newSubPath.getLineCount()+ " lines");
		return newPath;
	}
	
// this has removed Segements with emty nodeList: not used anymore
//	public void clean() {
//		nodeList = new ArrayList<Node>();
//	}

	/**
	 * @param bound
	 */
	public void extendBounds(Bounds bound) {
		for (Node n:nodeList) {
			bound.extend(n.lat, n.lon);
		}
	}


	
	public List<Node> getNodes() {
		return nodeList;
	}
}
