package net.sharenav.sharenav.tile;
/*
 * ShareNav - Copyright (c) 2008 Kai Krueger apmonkey at users dot sourceforge dot net 
 * See Copying
 */

import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;

import net.sharenav.sharenav.data.Legend;
import net.sharenav.sharenav.data.PaintContext;
import net.sharenav.sharenav.graphics.Projection;
import net.sharenav.util.HelperRoutines;
import net.sharenav.util.IntPoint;
import net.sharenav.util.Logger;
import net.sharenav.util.MoreMath;

//#if polish.android
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Paint.Style;
//#endif
import javax.microedition.lcdui.Graphics;


public class GpxTile extends Tile {	
	private final static Logger logger = Logger.getInstance(GpxTile.class, Logger.DEBUG);
	
	/** Arrays of latitudes and longitudes of track points in this tile.
	 *  Will point to null if tile was split.
	 */
	float [] trkPtLat;
	float [] trkPtLon;
	
	// Number of track points in this tile or number until split occurred.
	int noTrkPts;
	
	// Sub tiles, will be null if no split was yet made.
	GpxTile t1;
	GpxTile t2;
	
	/** Coordinate at which this tile was split. splitDimension determines
	 *  if it's latitude or longitude.
	 */
	float splitCoord;

	// Dimension at which this tile was split: true = lat, false = lon.
	boolean splitDimension;
	/** track is loaded from recordstore (true) or recording from gps (false) */
	private boolean loadedTrack = false;
	
	
	public GpxTile() {
		trkPtLat = new float[10];
		trkPtLon = new float[10];
		noTrkPts = 0;
		t1 = null;
		t2 = null;
	}
	/**
	 * creates a GpxTile which holds the currently recording track, or a combination of all loaded tracks depending on the argument
	 * @param loaded Describes if the track is loaded from recordstore (true) or currently recording from GPS (false)
	 */
	public GpxTile(boolean loaded){
		trkPtLat = new float[10];
		trkPtLon = new float[10];
		noTrkPts = 0;
		t1 = null;
		t2 = null;
		loadedTrack = loaded;
	}

	public synchronized void addTrkPt(float lat, float lon, boolean rad) {
		if (!rad) {
			lat = lat * MoreMath.FAC_DECTORAD;
			lon = lon * MoreMath.FAC_DECTORAD;
			rad = true;
		}
		//#debug info
		logger.debug("Adding trackpoint Nr: " + noTrkPts + " "  + lat + " " + lon);		
		if (lat < minLat)
			minLat = lat;
		if (lat > maxLat)
			maxLat = lat;
		if (lon < minLon)
			minLon = lon;
		if (lon > maxLon)
			maxLon = lon;
		
		if (trkPtLat == null) {
			if (t1t2TrackPoint(lat, lon)) {
				t1.addTrkPt(lat, lon, rad);
			} else {
				t2.addTrkPt(lat, lon, rad);
			}
		} else {
			trkPtLat[noTrkPts] = lat;
			trkPtLon[noTrkPts++] = lon;
			if (noTrkPts + 3 > trkPtLat.length) {
				if (noTrkPts > 90) {									
					splitTile();
				} else {
					extendTile();
				}
			}
		}
	}

	public boolean cleanup(int level) {
		// TODO Auto-generated method stub
		return false;
	}

	public synchronized void walk(PaintContext pc, int opt) {
		// TODO Auto-generated method stub
	}
	public int getNameIdx(float lat, float lon, short type) {
		// only interesting for SingleTile
		return -1;
	}
	
	public void paint(PaintContext pc, byte layer) {
		// It seems that making this method synchronized increases how often the
		// freezing after saving waypoints (as described in tracker item 2189029) occurs. 
		// As it doesn't alter data, it should be OK that it isn't synchronized.
		if (layer == Tile.LAYER_NODE) {
			if (contain(pc)) {			
				if ((t1 != null) && (t2 != null)) {
					t1.paint(pc, layer);
					t2.paint(pc, layer);
				} else {
					paintLocal(pc);
				}
			}
		}
	}
	
	public synchronized void dropTrk() {
		noTrkPts = 0;
		trkPtLat = null;
		trkPtLon = null;		
		if (t1 != null) {
			t1.dropTrk();		
			t2.dropTrk();
		} else {
			trkPtLat = new float[5];
			trkPtLon = new float[5];
		}
	}

	/**
	 * Painting Tracklogs
	 * @param pc
	 */
	private void paintLocal(PaintContext pc) {
		Image trkPtImage = pc.images.IMG_MARK;			
		if(loadedTrack){
			trkPtImage = pc.images.IMG_MARK_DISP;
		}
		Projection projection = pc.getP();
		IntPoint lineP2 = pc.lineP2;
		Graphics g = pc.g;
		//#if polish.api.areaoutlines
		g.getPaint().setStyle(Style.STROKE);
		float strokeWidth = g.getPaint().getStrokeWidth();
		// FIXME make width configurable some way
		g.getPaint().setStrokeWidth(5);
		Path tPath = new Path();
		if (noTrkPts > 1) {
			projection.forward(trkPtLat[0], trkPtLon[0], lineP2);
			g.drawImage(trkPtImage, lineP2.x, lineP2.y, Graphics.HCENTER | Graphics.VCENTER);
			tPath.moveTo(lineP2.x + g.getTranslateX(), lineP2.y + g.getTranslateY());
			for (int i = 1; i < noTrkPts; i++) {
				//if (projection.isPlotable(trkPtLat[i], trkPtLon[i])) {
					projection.forward(trkPtLat[i], trkPtLon[i], lineP2);
					// g.drawImage(trkPtImage, lineP2.x, lineP2.y, Graphics.HCENTER | Graphics.VCENTER);					
					g.drawImage(trkPtImage, lineP2.x, lineP2.y, Graphics.HCENTER | Graphics.VCENTER);					
					tPath.lineTo(lineP2.x + g.getTranslateX(), lineP2.y + g.getTranslateY());
					// FIXME add a new color
					g.setColor(Legend.COLORS[Legend.COLOR_ROUTE_ROUTELINE_BORDER]);
					g.getCanvas().drawPath(tPath, g.getPaint());
					//}
			}
		} else {
			for (int i = 0; i < noTrkPts; i++) {
				if (projection.isPlotable(trkPtLat[i], trkPtLon[i])) {
					projection.forward(trkPtLat[i], trkPtLon[i], lineP2);
					g.drawImage(trkPtImage, lineP2.x, lineP2.y, Graphics.HCENTER | Graphics.VCENTER);
				}
			}
		}
		g.getPaint().setStrokeWidth(strokeWidth);
		//#else
		// FIXME make line wider with triangles
		g.setStrokeStyle(Graphics.SOLID);
		int oldX = 0;
		int oldY = 0;
		for (int i = 0; i < noTrkPts; i++) {
			if (projection.isPlotable(trkPtLat[i], trkPtLon[i])) {
				projection.forward(trkPtLat[i], trkPtLon[i], lineP2);
				g.drawImage(trkPtImage, lineP2.x, lineP2.y, Graphics.HCENTER | Graphics.VCENTER);
				// FIXME add a new color
				g.setColor(Legend.COLORS[Legend.COLOR_ROUTE_ROUTELINE_BORDER]);
				if (i != 0) {
					g.drawLine(oldX, oldY, lineP2.x, lineP2.y);
				}
				oldX = lineP2.x;
				oldY = lineP2.y;
			}
		}
		//#endif
	}
	
	private void extendTile() {
		logger.info("Extending trackpoint array to: " + (trkPtLat.length + 10));
		float [] tmp;
		tmp = new float[trkPtLat.length + 10];
		System.arraycopy(trkPtLat, 0, tmp, 0, trkPtLat.length);
		trkPtLat = tmp;
		tmp = new float[trkPtLat.length];
		System.arraycopy(trkPtLon, 0, tmp, 0, trkPtLon.length);
		trkPtLon = tmp;		
	}
	
	private void splitTile() {
		logger.info("Trying to split GPX tile containing " + noTrkPts + "track points");
		if (maxLat - minLat > maxLon - minLon) {
			splitDimension = true;
			splitCoord = HelperRoutines.medianElement(trkPtLat, noTrkPts);				
		} else {
			splitDimension = false;
			splitCoord = HelperRoutines.medianElement(trkPtLon, noTrkPts);
		}
		/**
		 * Check to see that we reduce the number of track points
		 * by at least 5, as otherwise we can get into an infinite recursion.
		 * This can happen for example if many points are recorded that have
		 * identical coordinates		 *  
		 */
		int [] t1t2count = new int[2];
		for (int i = 0; i < noTrkPts; i++) {
			t1t2count[t1t2TrackPoint(trkPtLat[i], trkPtLon[i])?0:1]++;			
		}
		if (t1t2count[0] < 5 || t1t2count[1] < 5) {
			logger.info("Split was unsuccessfull, perhaps too many identical coordinates");
			extendTile();
			return;
		}
		//#debug debug
		logger.debug("Splitting GpxTile (" + splitDimension + ") "  + splitCoord);
		t1 = new GpxTile(loadedTrack);
		t2 = new GpxTile(loadedTrack);
		for (int i = 0; i < noTrkPts; i++) {
			if (t1t2TrackPoint(trkPtLat[i], trkPtLon[i]))
				t1.addTrkPt(trkPtLat[i], trkPtLon[i],true);
			else
				t2.addTrkPt(trkPtLat[i], trkPtLon[i], true);			
		}
		trkPtLat = null;
		trkPtLon = null;
	}

	private boolean t1t2TrackPoint(float lat, float lon) {		
		if (splitDimension) {
			if (lat < splitCoord) 
				return true;
			else
				return false;
		} else {
			if (lon < splitCoord) {
				return true;
			} else {
				return false;
			}
		}
	}
}
