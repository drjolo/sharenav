package de.ueller.midlet.gps.data;
/*
 * GpsMid - Copyright (c) 2008 Kai Krueger apm at users dot sourceforge dot net 
 * See Copying
 */

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.Math;
import java.util.Calendar;
import java.util.Date;
import javax.microedition.rms.InvalidRecordIDException;
import javax.microedition.rms.RecordEnumeration;
import javax.microedition.rms.RecordStore;
import javax.microedition.rms.RecordStoreException;
import javax.microedition.rms.RecordStoreFullException;
import javax.microedition.rms.RecordStoreNotFoundException;
import javax.microedition.rms.RecordStoreNotOpenException;


import de.ueller.gps.data.Configuration;
import de.ueller.gps.data.Position;
import de.ueller.gpsMid.mapData.GpxTile;
import de.ueller.gpsMid.mapData.Tile;
import de.ueller.midlet.gps.GpsMid;
import de.ueller.midlet.gps.Logger;
import de.ueller.midlet.gps.Trace;
import de.ueller.midlet.gps.UploadListener;
import de.ueller.midlet.gps.importexport.ExportSession;
import de.ueller.midlet.gps.importexport.GpxParser;
import de.ueller.midlet.gps.tile.PaintContext;

public class Gpx extends Tile implements Runnable {
	private float maxDistance;
	
	
	// statics for user-defined rules for record trackpoint
	private static long oldMsTime;
	private static float oldlat;
	private static float oldlon;		
	
	private final static Logger logger=Logger.getInstance(Gpx.class,Logger.DEBUG);
	
	private RecordStore trackDatabase;
	private RecordStore wayptDatabase;
	public int recorded=0;
	public int delay = 0;
	
	
	private Thread processorThread=null;
	private String url=null;
	
	private boolean sendWpt;
	private boolean sendTrk;
	private boolean reloadWpt;
	
	private boolean applyRecordingRules = true;
	
	private String trackName;
	private PersistEntity currentTrk;
	
	private UploadListener feedbackListener;
	
	private String importExportMessage;
	
	/**
	 * Variables used for transmitting GPX data:
	 */
	
	private InputStream in;
	
	private ByteArrayOutputStream baos;
	private DataOutputStream dos;
		
	private GpxTile tile;
	
	public Gpx() {
		tile = new GpxTile();		
		loadTrksFromDatabase();
		reloadWpt = true;
		processorThread = new Thread(this);
		processorThread.setPriority(Thread.MIN_PRIORITY);
		processorThread.start();		
	}
	
	public void displayWaypoints(boolean displayWpt) {
		
	}
	
	public void displayTrk(PersistEntity trk) {
		if (trk == null) {
			//TODO:
		} else {
			try {
			tile.dropTrk();
			DataInputStream dis1 = new DataInputStream(new ByteArrayInputStream(trackDatabase.getRecord(trk.id)));
			trackName = dis1.readUTF();
			recorded = dis1.readInt();
			int trackSize = dis1.readInt();
			byte[] trackArray = new byte[trackSize];
			dis1.read(trackArray);
			DataInputStream trackIS = new DataInputStream(new ByteArrayInputStream(trackArray));
			for (int i = 0; i < recorded; i++) {
				tile.addTrkPt(trackIS.readFloat(), trackIS.readFloat(), false);
				trackIS.readShort(); //altitude
				trackIS.readLong();	//Time			
				trackIS.readByte(); //Speed				
			}
			dis1.close();
			dis1 = null;
			} catch (IOException e) {
				e.printStackTrace();
			} catch (RecordStoreNotOpenException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InvalidRecordIDException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (RecordStoreException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
	}
	
	public void addWayPt(PositionMark waypt) {
		byte[] buf = waypt.toByte();
		try {
			int id = wayptDatabase.addRecord(buf, 0, buf.length);
			waypt.id = id;
		} catch (RecordStoreNotOpenException e) {			
			logger.exception("Exception storing waypoint (database not open)", e);
		} catch (RecordStoreFullException e) {
			logger.exception("Record store is full, could not store waypoint", e);			
		} catch (RecordStoreException e) {
			logger.exception("Exception storing waypoint", e);
		}
		tile.addWayPt(waypt);		
	}
	
	public boolean existsWayPt(PositionMark newWayPt) {
		if (tile != null) {
			return tile.existsWayPt(newWayPt);
		}
		return false;		
	}
		
	
	public void addTrkPt(Position trkpt) {
		Configuration config=GpsMid.getInstance().getConfig();
		logger.info("Adding trackpoint: " + trkpt);
		boolean doRecord=false;
		try {
			// always record when i.e. receiving or loading tracklogs
			// or when starting to record
			if (!applyRecordingRules || recorded==0) {
				doRecord=true;
			}
			// check which record rules to apply
			else if (config.getGpxRecordRuleMode()==Configuration.GPX_RECORD_ADAPTIVE) {
				/** adaptive recording
				 * 
				 * When saving tracklogs and adaptive recording is enabled,
				 * we reduce the frequency of saved samples if the speed drops
				 * to less than a certain amount. This should increase storage
				 * efficiency if one doesn't need if one doesn't need to repeatedly
				 * store positions if the device is not moving
				 * 
				 * Chose the following arbitrary sampling frequency:
				 * Greater 8km/h (2.22 m/s): every sample
				 * Greater 4km/h (1.11 m/s): every second sample
				 * Greater 2km/h (0.55 m/s): every fourth sample
				 * Below 2km/h (0.55 m/s): every tenth sample
				 */
				if ( (trkpt.speed > 2.222f) || ((trkpt.speed > 1.111f) && (delay > 0 )) || 
						((trkpt.speed > 0.556) && delay > 3 ) || (delay > 10)) {
					doRecord=true;
					delay = 0;
				} else {
					delay++;
				}			
			} else {
				/*
				 user-specified recording rules
				*/
				long msTime=System.currentTimeMillis();
				float lat=trkpt.latitude*MoreMath.FAC_DECTORAD;
				float lon=trkpt.longitude*MoreMath.FAC_DECTORAD;
				float distance = 100*ProjMath.getDistance(lat, lon, oldlat, oldlon); 
				if ( 
						// is not always record distance not set
						// or always record distance reached
						(
							config.getGpxRecordAlwaysDistanceCentimeters()!=0 &&
							distance >= config.getGpxRecordAlwaysDistanceCentimeters()
						)
						||
						(  
							(
								// is minimum time interval not set
								// or interval at least minimum interval?
								config.getGpxRecordMinMilliseconds() == 0 ||
								Math.abs(msTime-oldMsTime) >= config.getGpxRecordMinMilliseconds()
							)
							&&
							(
							// is minimum distance not set
							// or distance at least minimum distance?
							config.getGpxRecordMinDistanceCentimeters()==0 ||
							distance >= config.getGpxRecordMinDistanceCentimeters()
							)
						)
				) {
					doRecord=true;
					oldMsTime=msTime;
					oldlat=lat;
					oldlon=lon;
				}
			}
			if(doRecord) {
				dos.writeFloat(trkpt.latitude);
				dos.writeFloat(trkpt.longitude);
				dos.writeShort((short)trkpt.altitude);
				dos.writeLong(trkpt.date.getTime());
				dos.writeByte((byte)(trkpt.speed*3.6f)); //Convert to km/h				
				recorded++;
				tile.addTrkPt(trkpt.latitude, trkpt.longitude, false);
			}
		} catch (OutOfMemoryError oome) {
			try {				
				Trace.getInstance().dropCache();
				logger.info("Was out of memory, but we might have recovered");
			}catch (OutOfMemoryError oome2) {
				logger.fatal("Out of memory, can't add trackpoint");				
			}			
		} catch (IOException e) {
			logger.exception("Could not add trackpoint", e);
		}
	}
	
	public void deleteWayPt(PositionMark waypt) {
		deleteWayPt(waypt, null);
	}
	public void deleteWayPt(PositionMark waypt, UploadListener ul) {
		this.feedbackListener = ul;
		try {
			wayptDatabase.deleteRecord(waypt.id);
		} catch (RecordStoreNotOpenException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvalidRecordIDException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (RecordStoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void reloadWayPts() {
		if (processorThread != null && processorThread.isAlive()) {
			/* Already reloading, nothing to do */
			return;
		}
		reloadWpt = true;
		processorThread = new Thread(this);
		processorThread.setPriority(Thread.MIN_PRIORITY);
		processorThread.start();
	}
	
	public void newTrk() {
		logger.debug("Starting a new track recording");
		tile.dropTrk();
		Calendar cal = Calendar.getInstance();
		cal.setTime(new Date());
		
		//Construct a track name from the current time
		StringBuffer trkName = new StringBuffer();
		trkName.append(cal.get(Calendar.YEAR)).append("-").append(formatInt2(cal.get(Calendar.MONTH) + 1));
		trkName.append("-").append(formatInt2(cal.get(Calendar.DAY_OF_MONTH))).append("_");
		trkName.append(formatInt2(cal.get(Calendar.HOUR_OF_DAY))).append("-").append(formatInt2(cal.get(Calendar.MINUTE)));
		trackName = trkName	.toString();
		
		baos = new ByteArrayOutputStream();
		dos = new DataOutputStream(baos);
		recorded = 0;		
	}
	
	public void saveTrk() {
		try {
			logger.debug("Finishing track with " + recorded + " points");
			if (dos == null) {
				logger.debug("Not currently recording, so can't save the track");
				return;
			}
			dos.flush();			
			ByteArrayOutputStream baosDb = new ByteArrayOutputStream();
			DataOutputStream dosDb = new DataOutputStream(baosDb);
			dosDb.writeUTF(trackName);
			dosDb.writeInt(recorded);
			dosDb.writeInt(baos.size());
			dosDb.write(baos.toByteArray());
			dosDb.flush();
			trackDatabase.addRecord(baosDb.toByteArray(), 0, baosDb.size());
			dos.close();
			dos = null;
			baos = null;
			tile.dropTrk();
		} catch (IOException e) {
			logger.error("IOE: " + e.getMessage());
		} catch (RecordStoreNotOpenException e) {
			logger.error("RSNOE: " + e.getMessage());
		} catch (RecordStoreFullException e) {
			logger.error("RSFE: " + e.getMessage());
		} catch (RecordStoreException e) {
			logger.error("RSE: " + e.getMessage());
		} catch (OutOfMemoryError oome) {
			logger.fatal("Out of memory, can't save tracklog");			
		}
		
	}
	
	public void deleteTrk(PersistEntity trk) {
		try {
			trackDatabase.deleteRecord(trk.id);
			tile.dropTrk();
		} catch (RecordStoreNotOpenException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvalidRecordIDException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (RecordStoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
	}
	
	public void receiveGpx(InputStream in, UploadListener ul, float maxDistance) {
		this.maxDistance=maxDistance;
		this.in = in;
		this.feedbackListener = ul;
		
		if (in == null) {
			logger.error("Could not open input stream to gpx file");
		}
		if ((processorThread != null) && (processorThread.isAlive())) {
			logger.error("Still processing another gpx file");
		}
		processorThread = new Thread(this);
		processorThread.setPriority(Thread.MIN_PRIORITY);
		processorThread.start();
	}
	
	public void sendTrk(String url, UploadListener ul, PersistEntity trk) {
		logger.debug("Sending " + trk + " to " + url);
		feedbackListener = ul;
		this.url = url;
		sendTrk = true;
		tile.dropTrk();
		currentTrk = trk;
		processorThread = new Thread(this);
		processorThread.setPriority(Thread.MIN_PRIORITY);
		processorThread.start();
	}
	
	public void sendWayPt(String url, UploadListener ul) {
		this.url = url;
		feedbackListener = ul;
		sendWpt = true;
		processorThread = new Thread(this);
		processorThread.setPriority(Thread.MIN_PRIORITY);
		processorThread.start();
	}
	
	public PositionMark [] listWayPt() {
		return tile.listWayPt();
	}
	
	public PersistEntity[] listTrks() {
		PersistEntity[] trks;
		byte [] record = new byte[16000];		
		DataInputStream dis = new DataInputStream(new ByteArrayInputStream(record));		
		try {
			if (trackDatabase == null)
				trackDatabase = RecordStore.openRecordStore("tracks", false);
			logger.info("GPX database has " +trackDatabase.getNumRecords() + " entries and a size of " + trackDatabase.getSize());
			trks = new PersistEntity[trackDatabase.getNumRecords()];
			
			RecordEnumeration p = trackDatabase.enumerateRecords(null, null, false);			
			logger.info("Enumerating tracks: " + p.numRecords());
			int i = 0;
			while (p.hasNextElement()) {
				int idx = p.nextRecordId();
				while (trackDatabase.getRecordSize(idx) > record.length) {
					record = new byte[record.length + 16000];
					dis = new DataInputStream(new ByteArrayInputStream(record));
				}
				trackDatabase.getRecord(idx, record, 0);
				dis.reset();
				
				String trackName = dis.readUTF();
				int noTrackPoints = dis.readInt();
				logger.debug("Found track " + trackName + " with " + noTrackPoints + "TrkPoints");
				PersistEntity trk = new PersistEntity();
				trk.id = idx;
				trk.displayName = trackName + " (" + noTrackPoints + ")";
				trks[i++] = trk;
			}
			logger.info("Enumerated tracks");
			return trks;
		} catch (RecordStoreFullException e) {
			logger.error("Record Store is full, can't load list" + e.getMessage());
		} catch (RecordStoreNotFoundException e) {
			logger.error("Record Store not found, can't load list" + e.getMessage());
		} catch (RecordStoreException e) {
			logger.error("Record Store exception, can't load list" + e.getMessage());
		} catch (IOException e) {
			logger.error("IO exception, can't load list" + e.getMessage());
		}
		return null;
	}
	
	public void dropCache() {
		tile.dropTrk();
		tile.dropWayPt();
		System.gc();
		if (isRecordingTrk())
			saveTrk();		
	}
	
	public boolean cleanup(int level) {
		// TODO Auto-generated method stub
		return false;
	}

	public void paint(PaintContext pc, byte layer) {
		tile.paint(pc, layer);
		
	}
	
	public boolean isRecordingTrk() {
		return (dos != null);
	}
	
	public void run() {
		logger.info("GPX processing thread started");
		boolean success = false;
		if (reloadWpt) {			
			try {
				/**
				 * Sleep for a while to limit the number of reloads happening
				 */
				Thread.sleep(300);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			loadWaypointsFromDatabase();
			reloadWpt = false;
			if (feedbackListener != null) {
				feedbackListener.uploadAborted();
				feedbackListener = null;
			}
			return;
		} else if (sendTrk || sendWpt) {
			success = sendGpx();
		} else if (in != null) {
			success = receiveGpx();
		} else {
			logger.error("Did not know whether to send or receive");
		}
		if (feedbackListener != null) {			
			feedbackListener.completedUpload(success, importExportMessage);
		}
		feedbackListener = null;
		sendTrk = false;
		sendWpt = false;
	}
	
	/**
	 * Read tracks from the GPX recordStore and display the names in the list on screen.
	 */
	private void loadWaypointsFromDatabase() {		
		try {
			tile.dropWayPt();
			RecordEnumeration renum;
			
			logger.info("Loading waypoints into tile");
			wayptDatabase = RecordStore.openRecordStore("waypoints", true);			
			renum = wayptDatabase.enumerateRecords(null, null, false);			
			while (renum.hasNextElement()) {
				int id;			
				id = renum.nextRecordId();			
				PositionMark waypt = new PositionMark(id,wayptDatabase.getRecord(id));
				tile.addWayPt(waypt);						
			}
		} catch (RecordStoreFullException e) {
			logger.error("Recordstore is full while trying to open waypoints");
		} catch (RecordStoreNotFoundException e) {
			logger.error("Waypoints recordstore not found");
		} catch (RecordStoreException e) {
			logger.exception("RecordStoreException", e);
		}  catch (OutOfMemoryError oome) {
			logger.error("Out of memory loading waypoints");
		}
	}
	
	private void loadTrksFromDatabase() {
		try {			
			logger.info("Opening track database");
			trackDatabase = RecordStore.openRecordStore("tracks", true);			
		} catch (RecordStoreFullException e) {
			logger.error("Recordstore is full while trying to open waypoints");
		} catch (RecordStoreNotFoundException e) {
			logger.error("Waypoints recordstore not found");
		} catch (RecordStoreException e) {
			logger.exception("RecordStoreException", e);
		} catch (OutOfMemoryError oome) {
			logger.error("Out of memory opening Tracks");
		}
	}
	
	
	private void streamTracks (OutputStream oS) throws IOException, RecordStoreNotOpenException, InvalidRecordIDException, RecordStoreException{		
		DataInputStream dis1 = new DataInputStream(new ByteArrayInputStream(trackDatabase.getRecord(currentTrk.id)));
		trackName = dis1.readUTF();
		recorded = dis1.readInt();
		int trackSize = dis1.readInt();
		byte[] trackArray = new byte[trackSize];
		dis1.read(trackArray);
		DataInputStream trackIS = new DataInputStream(new ByteArrayInputStream(trackArray));
				
		oS.write("<trk>\r\n<trkseg>\r\n".getBytes());						
		
		for (int i = 1; i <= recorded; i++) {
			StringBuffer sb = new StringBuffer(128);
			sb.append("<trkpt lat='").append(trackIS.readFloat()).append("' lon='").append(trackIS.readFloat()).append("' >\r\n");
			sb.append("<ele>").append(trackIS.readShort()).append("</ele>\r\n");
			sb.append("<time>").append(formatUTC(new Date(trackIS.readLong()))).append("</time>\r\n");
			sb.append("</trkpt>\r\n");				
			// Read extra bytes in the buffer, that are currently not written to the GPX file.
			// Will add these at a later time.
			trackIS.readByte(); //Speed			
			oS.write(sb.toString().getBytes());
		}
		oS.write("</trkseg>\r\n</trk>\r\n".getBytes());
		
	}
	
	private void streamWayPts (OutputStream oS) throws IOException{		
		PositionMark[] waypts = tile.listWayPt();
		PositionMark wayPt = null;
		
		for (int i = 0; i < waypts.length; i++) {
			wayPt = waypts[i];			
			StringBuffer sb = new StringBuffer(128);
			sb.append("<wpt lat='").append(wayPt.lat*MoreMath.FAC_RADTODEC).append("' lon='").append(wayPt.lon*MoreMath.FAC_RADTODEC).append("' >\r\n");
			sb.append("<name>").append(wayPt.displayName).append("</name>\r\n");
			sb.append("</wpt>\r\n");
			oS.write(sb.toString().getBytes());
		}
	}
	
	private boolean sendGpx() {
		try {
			String name = null;
			
			logger.trace("Starting to send a GPX file, about to open a connection to" + url);
			
			if (sendTrk) {
				name = currentTrk.displayName;
			} else if (sendWpt)
				name = "Waypoints";
			
			if (url == null) {
				importExportMessage = "No GPX receiver specified. Please select a GPX receiver in the setup menu";
				return false;
			}
			
			OutputStream oS = null;
			ExportSession session = null;
			try {
				/**
				 * We jump through hoops here (Class.forName) in order to decouple
				 * the implementation of JSRs. The problem is, that not all phones have all
				 * of the JSRs, and if we simply called the Class directly, it would
				 * cause the whole app to crash. With this method, we can hopefully catch
				 * the missing JSRs and gracefully report an error to the user that the operation
				 * is not available on this phone. 
				 */
				/**
				 * The Class.forName and the instantition of the class must be separate
				 * statements, as otherwise this confuses the proguard obfuscator when
				 * rewriting the flattened renamed classes.
				 */
				Class tmp = null;
				if (url.startsWith("file:")) {
					tmp = Class.forName("de.ueller.midlet.gps.importexport.FileExportSession");
					
				} else if (url.startsWith("comm:")) {
					tmp = Class.forName("de.ueller.midlet.gps.importexport.CommExportSession");					
				} else if (url.startsWith("btgoep:")){
					tmp = Class.forName("de.ueller.midlet.gps.importexport.ObexExportSession");					
				}
				if (tmp != null)
					logger.info("Got class: " + tmp);
					Object objTmp = tmp.newInstance();
					if (objTmp instanceof ExportSession) {
						session = (ExportSession)(objTmp);
					} else {
						logger.info("objTmp: " + objTmp + "is not part of " + ExportSession.class.getName());
					}
					
			} catch (ClassNotFoundException cnfe) {
				importExportMessage = "Your phone does not support this form of exporting, pleas choose a different one";
				session = null;
				return false;
			} catch (ClassCastException cce) {
				logger.exception("Could not cast the class", cce);				
			}
			if (session == null) {
				importExportMessage = "Your phone does not support this form of exporting, pleas choose a different one";
				return false;
			}
			oS = session.openSession(url, name);
			if (oS == null) {
				importExportMessage = "Could not obtain a valid connection to " + url;
				return false;
			}
			oS.write("<?xml version='1.0' encoding='UTF-8'?>\r\n".getBytes());
			oS.write("<gpx version='1.1' creator='GPSMID' xmlns='http://www.topografix.com/GPX/1/1'>\r\n".getBytes());
			
			if (sendWpt)
				streamWayPts(oS);
			if (sendTrk)
				streamTracks(oS);
			
			oS.write("</gpx>\r\n\r\n".getBytes());
						
			oS.flush();
			oS.close();
			session.closeSession();
			importExportMessage = "success";
			return true;
		} catch (IOException e) {			
			logger.error("IOE:" + e);	
		} catch (OutOfMemoryError oome) {
			logger.fatal("Out of memory, can't transmit tracklogs");
		} catch (Exception ee) {			
			logger.error("Error while sending tracklogs: " + ee);
		}
		return false;
	}
	
	private boolean receiveGpx() {		
		try {
			boolean success;
			String jsr172Version = null;
			Class parserClass;
			Object parserObject;
			GpxParser parser;
//			try {
//				jsr172Version = System.getProperty("xml.jaxp.subset.version");
//			} catch (RuntimeException re) {
//				/**
//				 * Some phones throw exceptions if trying to access properties that don't
//				 * exist, so we have to catch these and just ignore them.
//				 */
//			} catch (Exception e) {
//				/**
//				 * See above 
//				 */				
//			}
//			if ((jsr172Version != null) &&  (jsr172Version.length() > 0)) {
//				logger.info("Using builtin jsr 172 XML parser");
//				parserClass = Class.forName("de.ueller.midlet.gps.importexport.Jsr172GpxParser");				
//			} else {
				logger.info("Using MinML2 XML parser");
				parserClass = Class.forName("de.ueller.midlet.gps.importexport.MinML2GpxParser");
//			}
			parserObject = parserClass.newInstance();
			parser = (GpxParser) parserObject;
			
			applyRecordingRules = false;
			success = parser.parse(in, maxDistance, this);
			applyRecordingRules = true;
			in.close();
			importExportMessage = parser.getMessage();
			
			return success;
		} catch (ClassNotFoundException cnfe) {
			importExportMessage = "Your phone does not support XML parsing";
		} catch (Exception e) {
			importExportMessage = "Something went wrong while importing GPX, " + e;
		}
		return false;
	}
	
	/**
	 * Formats an integer to 2 digits, as used for example in time.
	 * I.e. a 0 gets printed as 00. 
	 **/
	private static final String formatInt2(int n) {
		if (n < 10) {
			return "0" + n;
		} else {
			return Integer.toString(n);
		}
			
	}	
	/**
	 * Date-Time formater that corresponds to the standard UTC time as used in XML
	 * @param time
	 * @return
	 */
	private static final String formatUTC(Date time) {
		// This function needs optimising. It has a too high object churn.
		Calendar c = null;
		if (c == null)
			c = Calendar.getInstance();
		c.setTime(time);
		return c.get(Calendar.YEAR) + "-" + formatInt2(c.get(Calendar.MONTH) + 1) + "-" +
		formatInt2(c.get(Calendar.DAY_OF_MONTH)) + "T" + formatInt2(c.get(Calendar.HOUR_OF_DAY)) + ":" +
		formatInt2(c.get(Calendar.MINUTE)) + ":" + formatInt2(c.get(Calendar.SECOND)) + "Z";		 
		
	}


	public void walk(PaintContext pc, int opt) {
		// TODO Auto-generated method stub
		
	}
}
