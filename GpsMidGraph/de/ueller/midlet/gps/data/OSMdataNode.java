/**
 * This file is part of GpsMid 
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2 as published by
 * the Free Software Foundation.
 *
 * Copyright (C) 2009  Kai Krueger
 */

//#if polish.api.osm-editing
package de.ueller.midlet.gps.data;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Hashtable;

import de.ueller.gps.tools.HTTPhelper;
import de.ueller.midlet.gps.Logger;
import de.ueller.midlet.gps.importexport.QDGpxParser;
import de.ueller.midlet.gps.importexport.XmlParserContentHandler;

public class OSMdataNode extends OSMdataEntity implements XmlParserContentHandler{
	private final static Logger logger = Logger.getInstance(
			OSMdataNode.class, Logger.DEBUG);
	
	private float lat;
	private float lon;

	public OSMdataNode(String fullXML, int osmID) {
		super(fullXML, osmID);
	}
	
	public OSMdataNode(int osmID, float lat, float lon) {
		super(osmID);
		this.lat = lat;
		this.lon = lon;
	}
	
	protected void parseXML() {
		QDGpxParser parser = new QDGpxParser();
		logger.debug("Starting Node XML parsing with QDXML");
		InputStream in = new ByteArrayInputStream(fullXML.getBytes());
		parser.parse(in, this);
	}
	
	public void characters(char[] ch, int start, int length) {
	}

	public void endDocument() {
	}

	public void endElement(String namespaceURI, String localName, String name) {
	}

	public void startDocument() {
	}

	public void startElement(String namespaceURI, String localName,
			String name, Hashtable atts) {
		if (name.equalsIgnoreCase("node")) {
			try {
				int id = Integer.parseInt((String)atts.get("id"));
				if (id == osmID) {
					ignoring = false;
					editBy = (String)atts.get("user");
					editTime = (String)atts.get("timestamp");
					version = Integer.parseInt((String)atts.get("version"));
					changesetID = Integer.parseInt((String)atts.get("changeset"));
					lat = Float.parseFloat((String)atts.get("lat"));
					lon = Float.parseFloat((String)atts.get("lon"));
				} else {
					ignoring = true;
				}
			} catch (NumberFormatException nfe) {
				logger.exception("Failed to parse osm id", nfe);
			}
		}
		if (ignoring) {
			return;
		}

		if (name.equalsIgnoreCase("tag")) {
			tags.put(atts.get("k"), atts.get("v"));
		}
	}

	public String toXML(int commitChangesetID) {
		String xml;
		
		xml  = "<?xml version='1.0' encoding='utf-8'?>\r\n";
		xml += "<osm version='0.6' generator='GpsMid'>\r\n";
		xml += "<node id='" + osmID + "' lat='" + lat * MoreMath.FAC_RADTODEC + 
				"' lon='" + lon * MoreMath.FAC_RADTODEC + "' version='" + version + 
				"' changeset='" + commitChangesetID + "'>\r\n";
		Enumeration enKey = tags.keys();
		while (enKey.hasMoreElements()) {
			String key = (String)enKey.nextElement();
			if (key.equalsIgnoreCase("created_by")) {
				//Drop created_by tag, as it has moved into changesets
			} else {
				xml += "<tag k='" + HTTPhelper.escapeXML(key) + "' v='" + HTTPhelper.escapeXML((String)tags.get(key)) + "' />\r\n";
			}
		}
		xml += "</node>\r\n";
		xml += "</osm>\r\n";
		
		return xml;
	}
	
	public String toString() {
		String res;
		res = "\nOSM node " + osmID + "\n";
		res += "     last edited by " + editBy + " at " + editTime + "\n";
		res += "     version " + version + " in changeset " + changesetID + "\n";
		res += " Tags:\n";
		Enumeration enKey = tags.keys();
		while (enKey.hasMoreElements()) {
			String key = (String)enKey.nextElement();
			res += "   " + key + " = " + tags.get(key) + "\n";
		}
		return res;
	}
}
//#endif