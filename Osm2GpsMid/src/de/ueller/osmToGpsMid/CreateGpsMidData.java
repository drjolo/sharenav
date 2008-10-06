package de.ueller.osmToGpsMid;


import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Stack;
import java.util.TreeSet;

import de.ueller.osmToGpsMid.model.Bounds;
import de.ueller.osmToGpsMid.model.Connection;
import de.ueller.osmToGpsMid.model.MapName;
import de.ueller.osmToGpsMid.model.SoundDescription;
import de.ueller.osmToGpsMid.model.Node;
import de.ueller.osmToGpsMid.model.POIdescription;
import de.ueller.osmToGpsMid.model.WayDescription;
import de.ueller.osmToGpsMid.model.RouteNode;
import de.ueller.osmToGpsMid.model.Sequence;
import de.ueller.osmToGpsMid.model.SubPath;
import de.ueller.osmToGpsMid.model.Tile;
import de.ueller.osmToGpsMid.model.Way;
import de.ueller.osmToGpsMid.model.name.Names;



public class CreateGpsMidData {
	
	/**
	 * This class is used in order to store a tuple on a dedicated stack.
	 * So that it is not necessary to use the OS stack in recursion
	 */
	class TileTuple {
		public Tile t;
		public Bounds bound;
		TileTuple(Tile t, Bounds b) {
			this.t = t;
			this.bound = b;
		}
	}
	
	public final static byte LEGEND_FLAG_IMAGE = 0x01;
	public final static byte LEGEND_FLAG_SEARCH_IMAGE = 0x02;
	public final static byte LEGEND_FLAG_MIN_IMAGE_SCALE = 0x04;
	public final static byte LEGEND_FLAG_TEXT_COLOR = 0x08;
	public final static byte LEGEND_FLAG_NON_HIDEABLE = 0x10;
	
		
//	private final static int MAX_TILE_FILESIZE=20000;
//	private final static int MAX_ROUTETILE_FILESIZE=5000;
	public  final static int MAX_DICT_DEEP=5;
	public  final static int ROUTEZOOMLEVEL=4;
	OxParser parser;
	Tile tile[]= new Tile[ROUTEZOOMLEVEL+1];
	private final String	path;
	TreeSet<MapName> names;
	Names names1;
	StringBuffer sbCopiedMedias= new StringBuffer();
	short mediaInclusionErrors=0;
	
	private final static int INODE=1;
	private final static int SEGNODE=2;
//	private Bounds[] bounds=null;
	private Configuration configuration;
	private int totalWaysWritten=0;
	private int totalSegsWritten=0;
	private int totalNodesWritten=0;
	private int totalPOIsWritten=0;
	private RouteData rd;
	
	
	public CreateGpsMidData(OxParser parser,String path) {
		super();
		this.parser = parser;
		this.path = path;
		File dir=new File(path);
		// first of all, delete all data-files from a previous run or files that comes
		// from the mid jar file
		if (dir.isDirectory()){
			File[] files = dir.listFiles();
			for (File f : files) {
				if (f.getName().endsWith(".d") || f.getName().endsWith(".dat")){
					if (! f.delete()){
						System.out.println("failed to delete file " + f.getName());
					}
				}
			}
		}
	}
	

	public void exportMapToMid(){
		names1=getNames1();
		exportLegend(path);
		SearchList sl=new SearchList(names1);
		sl.createNameList(path);
		for (int i=0;i<=3;i++){
			System.out.println("export Tiles for zoomlevel " + i);
			exportMapToMid(i);
		}
		System.out.println("export RouteTiles");
		if (configuration.useRouting) {
			exportMapToMid(ROUTEZOOMLEVEL);
		}
//		for (int x=1;x<12;x++){
//			System.out.print("\n" + x + " :");
//			tile[ROUTEZOOMLEVEL].printHiLo(1, x);
//		}
//		System.exit(2);
		sl.createSearchList(path);
		System.out.println("Total Ways:"+totalWaysWritten 
				         + " Seg:"+totalSegsWritten
				         + " Pkt:"+totalNodesWritten
				         + " POI:"+totalPOIsWritten);
	}
	

	private Names getNames1(){
		Names na=new Names();
		for (Way w : parser.getWays()) {
			na.addName(w);		
		}
		for (Node n : parser.getNodes()) {
			na.addName(n);
		}
		System.out.println("found " + na.getNames().size() + " names " + na.getCanons().size() + " canon");
		na.calcNameIndex();
		return (na);
	}
	
	private void exportLegend(String path) {
		FileOutputStream foi;
		String outputMedia;
		try {
			foi = new FileOutputStream(path + "/legend.dat");
			DataOutputStream dsi = new DataOutputStream(foi);
			dsi.writeShort(Configuration.MAP_FORMAT_VERSION);
			/**
			 * Writing gloabal info 
			 */
			dsi.writeInt(Configuration.getConfiguration().background_color);
			/**
			 * Writing POI legend data			 * 
			 */
			dsi.writeByte(Configuration.getConfiguration().getPOIDescs().size());
			for (POIdescription poi : Configuration.getConfiguration().getPOIDescs()) {
				byte flags = 0;
				if (poi.image != null && !poi.image.equals(""))
					flags |= LEGEND_FLAG_IMAGE;
				if (poi.searchIcon != null)
					flags |= LEGEND_FLAG_SEARCH_IMAGE;
				if (poi.minImageScale != poi.minTextScale)
					flags |= LEGEND_FLAG_MIN_IMAGE_SCALE;
				if (poi.textColor != 0)
					flags |= LEGEND_FLAG_TEXT_COLOR;				
				if (!poi.hideable)
					flags |= LEGEND_FLAG_NON_HIDEABLE;
				dsi.writeByte(poi.typeNum);
				dsi.writeByte(flags);
				dsi.writeUTF(poi.description);
				dsi.writeBoolean(poi.imageCenteredOnNode);
				dsi.writeInt(poi.minImageScale);
				if ((flags & LEGEND_FLAG_IMAGE) > 0) {
					outputMedia=copyMediaToMid(poi.image, path, "png");
					dsi.writeUTF(outputMedia);
				}
				if ((flags & LEGEND_FLAG_SEARCH_IMAGE) > 0) {					
					outputMedia=copyMediaToMid(poi.searchIcon, path, "png");
					dsi.writeUTF(outputMedia);
				}
				if ((flags & LEGEND_FLAG_MIN_IMAGE_SCALE) > 0)
					dsi.writeInt(poi.minTextScale);
				if ((flags & LEGEND_FLAG_TEXT_COLOR) > 0)
					dsi.writeInt(poi.textColor);
				// System.out.println(poi);
	
			}
			/**
			 * Writing Way legend data 
			 */
			dsi.writeByte(Configuration.getConfiguration().getWayDescs().size());
			for (WayDescription way : Configuration.getConfiguration().getWayDescs()) {
				byte flags = 0;
				if (!way.hideable)
					flags |= LEGEND_FLAG_NON_HIDEABLE;				
				dsi.writeByte(way.typeNum);
				dsi.writeByte(flags);
				dsi.writeUTF(way.description);								
				dsi.writeInt(way.minScale);
				dsi.writeInt(way.minTextScale);
				dsi.writeBoolean(way.isArea);
				dsi.writeInt(way.lineColor);
				dsi.writeInt(way.boardedColor);
				dsi.writeByte(way.wayWidth);
				dsi.writeBoolean(way.lineStyleDashed);
				// System.out.println(way);
			}
			/**
			 * Writing Sound Descriptions 
			 */
			dsi.writeByte(Configuration.getConfiguration().getSoundDescs().size());
			for (SoundDescription sound : Configuration.getConfiguration().getSoundDescs()) {
				dsi.writeUTF(sound.name);								
				outputMedia=copyMediaToMid(sound.soundFile, path, "sound");
				dsi.writeUTF(outputMedia);
			}

			// show summary for copied media files
			if (sbCopiedMedias.length()!=0) {
				System.out.println("External media inclusion summary:");
				System.out.println(sbCopiedMedias.toString());
				if (mediaInclusionErrors!=0) {					
					System.out.println("");
					System.out.println("Warning: " + mediaInclusionErrors + " media files could NOT be included - see details above");
					System.out.println("");
				}
			}
			
			dsi.close();
			foi.close();
			
			if (! Configuration.getConfiguration().useRouting) {
				System.out.println("Routing disabled - removing routing sound files from midlet:");
				removeSoundFile("AGAIN");
				removeSoundFile("CHECK_DIRECTION");
				removeSoundFile("CONTINUE");
				removeSoundFile("HALF");
				removeSoundFile("HARD");
				removeSoundFile("LEFT");
				removeSoundFile("PREPARE");
				removeSoundFile("RIGHT");
				removeSoundFile("ROUTE_RECALCULATION");
				removeSoundFile("SOON");
				removeSoundFile("STRAIGHTON");
				//removeSoundFile("TARGET_REACHED");
				removeSoundFile("THEN");
			}
			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	} 

	private void removeSoundFile(String soundName) {
		String soundFile = null;
		SoundDescription sDes = Configuration.getConfiguration().getSoundDescription(soundName);
		if (sDes != null) {
			soundFile = sDes.soundFile;
		} else {
			soundFile = soundName.toLowerCase() + ".amr";
		}
		
		File target = new File(path + "/" + soundFile);
 
		if (target.exists()) {
			target.delete();
			System.out.println(" - removed " + soundFile);
		}		
	}
	
	
	/* Copies the given file in mediaPath to destDir
	 * - if you specify a filename only it will look for the file in this order 1. current directory 2. additional source subdirectory 3.internal file
	 * - for file names only preceded by a single "/" Osm2GpsMid will always assume you want to explicitely use the internal media file
	 * - directory path information as part of source media path is allowed, however the media file will ALWAYS be copied to destDir root
	 * - remembers copied files in sbCopiedMedias (adds i.e. "(REPLACED)" for replaced files)
	 */
	private String copyMediaToMid(String mediaPath, String destDir, String additionalSrcPath) {
		// output filename is just the name part of the imagePath filename preceded by "/"  
		int iPos=mediaPath.lastIndexOf("/");
		String outputMediaName;
		// if no "/" is contained look for file in current directory and /png
		if(iPos==-1) {
			outputMediaName="/" + mediaPath;
			// check if file exists in current directory
			if (! (new File(mediaPath).exists())) {
				// check if file exists in current directory + "/png"
				if (! (new File(additionalSrcPath+"/"+mediaPath).exists())) {
					// if not check if we can use the internal image file
					if (!(new File(path + outputMediaName).exists())) {	
						// append media name if first media or " ,"+media name for the following ones
						sbCopiedMedias.append( (sbCopiedMedias.length()==0)?mediaPath:", " + mediaPath);				
						sbCopiedMedias.append("(ERROR: file not found)");
						mediaInclusionErrors++;
					}
					return outputMediaName;
				}
				else {
					// otherwise use from additional directory
					mediaPath=additionalSrcPath+"/"+mediaPath;
				}
			}
		// if the first and only "/" is at the beginning its the explicit syntax for internal images
		} else if(iPos==0) {
			if (!(new File(path + mediaPath).exists())) {	
				// append media name if first media or " ,"+media name for the following ones
				sbCopiedMedias.append( (sbCopiedMedias.length()==0)?mediaPath:", " + mediaPath);				
				sbCopiedMedias.append("(ERROR: INTERNAL media file not found)");
				mediaInclusionErrors++;
			}
			return mediaPath;
		// else it's an external file with explicit path
		} else {
			outputMediaName=mediaPath.substring(iPos);
		}
		
		// append media name if first media or " ,"+media name for the following ones
		sbCopiedMedias.append( (sbCopiedMedias.length()==0)?mediaPath:", " + mediaPath);					

		try {
			//System.out.println("Copying " + mediaPath + " as " + outputMediaName + " into the midlet");
			FileChannel fromChannel = new FileInputStream(mediaPath).getChannel();
			// Copy Media file
			try {
				// check if output file already exists
				boolean alreadyExists= (new File(destDir + outputMediaName).exists());
				FileChannel toChannel = new FileOutputStream(destDir + outputMediaName).getChannel();
				fromChannel.transferTo(0, fromChannel.size(), toChannel);
				toChannel.close();
				if(alreadyExists) {
					sbCopiedMedias.append("(REPLACED " + outputMediaName + ")");
				}
			}
			catch(Exception e) {
				sbCopiedMedias.append("(ERROR accessing destination file " + destDir + outputMediaName + ")");
				mediaInclusionErrors++;
				e.printStackTrace();
			}
			fromChannel.close();
		}
		catch(Exception e) {
			System.out.println("Error accessing source file: " + mediaPath);
			sbCopiedMedias.append("(ERROR accessing source file " + mediaPath + ")");
			mediaInclusionErrors++;
			e.printStackTrace();
		}
		return outputMediaName;
	}

	
	private void exportMapToMid(int zl){
// System.out.println("Total ways : " + parser.ways.size() + " Nodes : " +
// parser.nodes.size());
		try {
			FileOutputStream fo = new FileOutputStream(path+"/dict-"+zl+".dat");
			DataOutputStream ds = new DataOutputStream(fo);
// Node min=new Node(90f,180f,0);
// Node max=new Node(-90f,-180f,0);
			ds.writeUTF("DictMid"); // magig number
			Bounds allBound=new Bounds();
			for (Way w1 : parser.getWays()) {				
				if (w1.getZoomlevel(configuration) != zl) continue;
				w1.used=false;
				allBound.extend(w1.getBounds());
			}			
			if (zl == ROUTEZOOMLEVEL){
				// for RouteNodes
				for (Node n : parser.getNodes()) {
					n.used=false;
					if (n.routeNode == null) continue;
					allBound.extend(n.lat,n.lon);
				}
			} else {
				for (Node n : parser.getNodes()) {
					if (n.getZoomlevel(configuration) != zl) continue;
					allBound.extend(n.lat,n.lon);
				}
			}			
			tile[zl]=new Tile((byte) zl);
			Sequence routeNodeSeq=new Sequence();
			Sequence tileSeq=new Sequence();
			tile[zl].ways=parser.getWays();
			tile[zl].nodes=parser.getNodes();
			// create the tiles and write the content 
			exportTile(tile[zl],tileSeq,allBound,routeNodeSeq);
			
			if (tile[zl].type != Tile.TYPE_ROUTECONTAINER && tile[zl].type != Tile.TYPE_CONTAINER) {
				/*
				 * We must have so little data, that it completely fits within one tile.
				 * Never the less, the top tile should be a container tile
				 */								
				Tile ct = new Tile((byte)zl);
				ct.t1 = tile[zl];
				ct.t2 = new Tile((byte)zl);
				ct.t2.type = Tile.TYPE_EMPTY;
				if (zl == ROUTEZOOMLEVEL) {
					ct.type = Tile.TYPE_ROUTECONTAINER;
				} else {
					ct.type = Tile.TYPE_CONTAINER;
				}
				tile[zl] = ct;
			}
			tile[zl].recalcBounds();			
			if (zl == ROUTEZOOMLEVEL){
				Sequence rnSeq=new Sequence();
				tile[zl].renumberRouteNode(rnSeq);
				tile[zl].calcHiLo();
				tile[zl].writeConnections(path);
		        tile[zl].type=Tile.TYPE_ROUTECONTAINER;
			} 
			Sequence s=new Sequence();
			tile[zl].writeTileDict(ds,1,s,path);			
			ds.writeUTF("END"); // Magic number
			fo.close();

		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void exportTile(Tile t,Sequence tileSeq,Bounds tileBound, Sequence routeNodeSeq) throws IOException{
		Bounds realBound=new Bounds();
		LinkedList<Way> ways;
		Collection<Node> nodes;
		int maxSize;
		boolean unsplittableTile;
		boolean tooLarge;
		/*
		 * Using recursion can cause a stack overflow on large projects,
		 * so need an explicit stack that can grow larger;
		 */
		Stack<TileTuple> expTiles = new Stack<TileTuple>();
		byte [] out=new byte[1];
		expTiles.push(new TileTuple(t,tileBound));
		byte [] connOut;
		System.out.println("Exporting Tiles");
		while (!expTiles.isEmpty()) {			
			TileTuple tt = expTiles.pop();
			unsplittableTile = false;
			tooLarge = false;
			t = tt.t; tileBound = tt.bound;
			ways=new LinkedList<Way>();
			nodes=new ArrayList<Node>();
			realBound=new Bounds();

			// Reduce the content of t.ways and t.nodes to all relevant elements
			// in the given bounds and create the binary midlet representation
			if (t.zl != ROUTEZOOMLEVEL){
				maxSize=configuration.getMaxTileSize();
				ways=getWaysInBound(t.ways, t.zl,tileBound,realBound);				
				if (ways.size() == 0){
					t.type=3;
				}
				int mostlyInBound=ways.size();				
				addWaysCompleteInBound(ways,t.ways,t.zl,realBound);
				if (ways.size() > 2*mostlyInBound){
					realBound=new Bounds();
					ways=getWaysInBound(t.ways, t.zl,tileBound,realBound);					
				}				
				nodes=getNodesInBound(t.nodes,t.zl,realBound);
				if (ways.size() <= 255){
					t.bounds=realBound.clone();
					if ((MyMath.degToRad(t.bounds.maxLat - t.bounds.minLat) > (Short.MAX_VALUE - Short.MIN_VALUE - 2000)/Tile.fpm) ||
						(MyMath.degToRad(t.bounds.maxLon - t.bounds.minLon) > (Short.MAX_VALUE - Short.MIN_VALUE - 2000)/Tile.fpm)) {
							//System.out.println("Tile spacially to large (" + ((Short.MAX_VALUE - Short.MIN_VALUE - 2000)/Tile.fpm) + ": " + t.bounds);
							tooLarge = true;
							
					} else {
						t.centerLat = (t.bounds.maxLat - t.bounds.minLat) / 2 + t.bounds.minLat;
						t.centerLon = (t.bounds.maxLon - t.bounds.minLon) / 2 + t.bounds.minLon;
						out=createMidContent(ways,nodes,t);
					}
				}
				/**
				 * If the number of nodes and ways in the new tile is the same, and the bound
				 * has already been shrunk to less than 0.001°, then give up and declare it a
				 * unsplittable Tile and just live with the fact that this tile is to big.
				 * Otherwise we can get into an endless loop of trying to split up this tile
				 */
				if ((t.nodes.size() == nodes.size()) && (t.ways.size() == ways.size()) && (tileBound.maxLat - tileBound.minLat < 0.001)) {
					System.out.println("WARNING: could not reduce tile size for tile " + t);
					System.out.println("t.ways " + t.ways.size() + " t.nodes " + t.nodes.size());
					for (Way w : t.ways) {
						System.out.println("Way: " + w);						
					}
					
					unsplittableTile = true;										
				}
				t.nodes=nodes;
				t.ways=ways;
			} else {
				// Route Nodes
				maxSize=configuration.getMaxRouteTileSize();
				nodes=getRouteNodesInBound(t.nodes,tileBound,realBound);
				byte[][] erg=createMidContent(nodes,t);
				out=erg[0];
				connOut = erg[1];
				t.nodes=nodes;
			}
			
			if (unsplittableTile && tooLarge) {
				System.out.println("Error: Tile is unsplittable, but too large. Can't deal with this!");
			}

			// split tile if more then 255 Ways or binary content > MAX_TILE_FILESIZE but not if only one Way
			if ((!unsplittableTile) && ((ways.size() > 255 || (out.length > maxSize && ways.size() != 1) || tooLarge))){
				//System.out.println("create Subtiles size="+out.length+" ways=" + ways.size());
				t.bounds=realBound.clone();
				if (t.zl != ROUTEZOOMLEVEL){
					t.type=Tile.TYPE_CONTAINER;				
				} else {
					t.type=Tile.TYPE_ROUTECONTAINER;
				}
				t.t1=new Tile((byte) t.zl,ways,nodes);
				t.t2=new Tile((byte) t.zl,ways,nodes);
				t.setRouteNodes(null);
				if ((tileBound.maxLat-tileBound.minLat) > (tileBound.maxLon-tileBound.minLon)){
					// split to half latitude
					float splitLat=(tileBound.minLat+tileBound.maxLat)/2;
					Bounds nextTileBound=tileBound.clone();
					nextTileBound.maxLat=splitLat;				
					expTiles.push(new TileTuple(t.t1,nextTileBound));
					nextTileBound=tileBound.clone();
					nextTileBound.minLat=splitLat;				
					expTiles.push(new TileTuple(t.t2,nextTileBound));
				} else {
					// split to half longitude
					float splitLon=(tileBound.minLon+tileBound.maxLon)/2;
					Bounds nextTileBound=tileBound.clone();
					nextTileBound.maxLon=splitLon;				
					expTiles.push(new TileTuple(t.t1,nextTileBound));
					nextTileBound=tileBound.clone();
					nextTileBound.minLon=splitLon;				
					expTiles.push(new TileTuple(t.t2,nextTileBound));
				}
				t.ways=null;
				t.nodes=null;

				//			System.gc();
			} else {
				if (ways.size() > 0 || nodes.size() > 0){					
					// Write as dataTile
					t.fid=tileSeq.next();
					if (t.zl != ROUTEZOOMLEVEL) {
						t.setWays(ways);
						writeRenderTile(t, tileBound, realBound, nodes, out);
					} else {
						writeRouteTile(t, tileBound, realBound, nodes, out);
					}

				} else {
					//Write as emty box
					t.type=Tile.TYPE_EMPTY;
				}
			}
		}
		return;
	}


	/**
	 * @param t
	 * @param tileBound
	 * @param realBound
	 * @param nodes
	 * @param out
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	private void writeRouteTile(Tile t, Bounds tileBound, Bounds realBound,
			Collection<Node> nodes, byte[] out) {
		//System.out.println("Write renderTile "+t.zl+":"+t.fid + " nodes:"+nodes.size());
		t.type=Tile.TYPE_MAP;
		t.bounds=tileBound.clone();
		t.type=Tile.TYPE_ROUTEDATA;
		for (RouteNode n:t.getRouteNodes()){
			n.node.used=true;
		}
	}
	/**
	 * @param t
	 * @param tileBound
	 * @param realBound
	 * @param ways
	 * @param nodes
	 * @param out
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	private void writeRenderTile(Tile t, Bounds tileBound, Bounds realBound,
			 Collection<Node> nodes, byte[] out)
			throws FileNotFoundException, IOException {
		//System.out.println("Write routeTile "+t.zl+":"+t.fid+ " ways:"+t.ways.size() + " nodes:"+nodes.size());
		totalNodesWritten+=nodes.size();
		totalWaysWritten+=t.ways.size();
		
		//TODO: Is this safe to comment out??
		//Collections.sort(t.ways);
		for (Way w: t.ways){
			totalSegsWritten+=w.getLineCount();
		}
		if (t.zl != ROUTEZOOMLEVEL) {
			for (Node n : nodes) {
				if (n.getType(null) > -1 )
					totalPOIsWritten++;
			}
		}
		
		t.type=Tile.TYPE_MAP;
		// RouteTiles will be written later because of renumbering
		if (t.zl != ROUTEZOOMLEVEL) {
			t.bounds=realBound.clone();
			FileOutputStream fo = new FileOutputStream(path + "/t" + t.zl
					+ t.fid + ".d");
			DataOutputStream tds = new DataOutputStream(fo);
			tds.write(out);
			fo.close();
			// mark nodes as written to MidStorage 
			for (Node n : nodes) { 
				if (n.fid != 0) {
					System.out.println("DATA DUPPLICATION: This node has been written already! " + n);
				}
				n.fid = t.fid; 
			}
			// mark ways as written to MidStorage
			for (Iterator<Way> wi = t.ways.iterator(); wi.hasNext();) {
				Way w1=wi.next();
				w1.used=true;
				w1.fid=t.fid;
			}
		} else {
			t.bounds=tileBound.clone();
			t.type=Tile.TYPE_ROUTEDATA;
			for (RouteNode n:t.getRouteNodes()){
				n.node.used=true;
			}
		}
	}
	
	
	private LinkedList<Way> getWaysInBound(Collection<Way> parentWays,int zl,Bounds targetTile,Bounds realBound){
		LinkedList<Way> ways = new LinkedList<Way>();
//		System.out.println("search for ways mostly in " + targetTile + " from " + parentWays.size() + " ways");
		// collect all way that are in this rectangle
		for (Way w1 : parentWays) {
			byte type=w1.getType();
			if (type < 1) continue;
			if (w1.getZoomlevel(configuration) != zl) continue;
			if (w1.used) continue;
			Bounds wayBound=w1.getBounds();
			if (targetTile.isMostlyIn(wayBound)){
				realBound.extend(wayBound);
				ways.add(w1);
			}
		}
//		System.out.println("getWaysInBound found " + ways.size() + " ways");
		return ways;
	}
	private LinkedList<Way> addWaysCompleteInBound(LinkedList<Way> ways,Collection<Way> parentWays,int zl,Bounds targetTile){
		// collect all way that are in this rectangle
//		System.out.println("search for ways total in " + targetTile + " from " + parentWays.size() + " ways");
		//This is bit of a hack. We should probably propagate the TreeSet through out,
		//But that needs more effort and time than I currently have. And this way we get
		//rid of a O(n^2) bottle neck
		TreeSet<Way> waysTS = new TreeSet<Way>(ways);
		for (Way w1 : parentWays) {
			byte type=w1.getType();
			if (type == 0) continue;
			if (w1.getZoomlevel(configuration) != zl) continue;
			if (w1.used) continue;
			if (waysTS.contains(w1)) continue;
			Bounds wayBound=w1.getBounds();
			if (targetTile.isCompleteIn(wayBound)){
				waysTS.add(w1);
				ways.add(w1);
			}
		}
//		System.out.println("addWaysCompleteInBound found " + ways.size() + " ways");
		return ways;
	}
	
	public Collection<Node> getNodesInBound(Collection<Node> parentNodes,int zl,Bounds targetBound){
		Collection<Node> nodes = new LinkedList<Node>();
		for (Node node : parentNodes){
			//Check to see if the node has already been written to MidStorage
			//If yes, then ignore the node here, to prevent duplicate nodes
			//due to overlapping tiles
			if (node.fid != 0) continue;
			if (node.getType(configuration) < 0) continue;
			if (node.getZoomlevel(configuration) != zl) continue;
			if (! targetBound.isIn(node.lat,node.lon)) continue;
			nodes.add(node);
		}
//		System.out.println("getNodesInBound found " + nodes.size() + " nodes");
		return nodes;
	}
	public Collection<Node> getRouteNodesInBound(Collection<Node> parentNodes,Bounds targetBound,Bounds realBound){
		Collection<Node> nodes = new LinkedList<Node>();
		for (Node node : parentNodes){
			if (node.routeNode == null) continue;
			if (! targetBound.isIn(node.lat,node.lon)) continue;
//			System.out.println(node.used);
			if (! node.used) {
				realBound.extend(node.lat,node.lon);
				nodes.add(node);
//				node.used=true;
			} 
		}
		return nodes;
	}

	/**
	 * Create the data-content for a route-tile. Containing a list of nodes and a list
	 * of connections from each node.
	 * @param interestNodes list of all Nodes that should included in this tile
	 * @param t the Tile that holds the meta-data
	 * @return in array[0][] the file-format for all nodes and in array[1][] the
	 * file-format for all connections whithin this tile.
	 * @throws IOException
	 */
	public byte[][] createMidContent(Collection<Node> interestNodes, Tile t) throws IOException{
		ByteArrayOutputStream nfo = new ByteArrayOutputStream();
		DataOutputStream nds = new DataOutputStream(nfo);
		ByteArrayOutputStream cfo = new ByteArrayOutputStream();
		DataOutputStream cds = new DataOutputStream(cfo);
		nds.writeByte(0x54); // magic number
		
		nds.writeShort(interestNodes.size());		
		for (Node n : interestNodes) {
			writeRouteNode(n,nds,cds);
				if (n.routeNode != null){
					t.addRouteNode(n.routeNode);
				}

		}

		nds.writeByte(0x56); // magic number
		nfo.close();
		cfo.close();
		byte [][] ret = new byte[2][];
		ret[0]=nfo.toByteArray();
		ret[1]=cfo.toByteArray();
		return ret;
	}

	/**
	 * Create the Data-content for a SingleTile in memory. This will later directly 
	 * written on Disk if the byte array is not to big otherwise this tile will
	 * splitted in smaller tiles. 
	 * @param ways a Collection of ways that are chosen to be in this tile.
	 * @param interestNodes all additional Nodes like places, parking and so on 
	 * @param t the Tile, holds the metadata for this area.
	 * @return a byte array that represents a file content. This could be written
	 * directly ond disk.
	 * @throws IOException
	 */
	public byte[] createMidContent(Collection<Way> ways,Collection<Node> interestNodes, Tile t) throws IOException{
		Map<Long,Node> wayNodes = new HashMap<Long,Node>();
		int ren=0;
		// reset all used flags of all Nodes that are part of ways in <code>ways</code>
		for (Way way : ways) {
			for (SubPath sp:way.getSubPaths()){
				for (Node n:sp.getNodes()){
					n.used=false;
				}
			}
		}
		// mark all interestNodes as used
		for (Node n1 : interestNodes){
			n1.used=true;
		}
		// find all nodes that are part of a way but not in interestNodes
		for (Way w1: ways) {
			for (SubPath sp:w1.getSubPaths()){
				for (Node n:sp.getNodes()){
					Long id=new Long(n.id);
					if ((!wayNodes.containsKey(id)) && !n.used){
						wayNodes.put(id, n);
					}

				}
			}
		}

		// create a byte arrayStream which holds the Singeltile-Data
		// this is created in memory and written later if file is 
		// not to big.
		ByteArrayOutputStream fo = new ByteArrayOutputStream();
		DataOutputStream ds = new DataOutputStream(fo);
		ds.writeByte(0x54); // Magic number
		ds.writeFloat(MyMath.degToRad(t.centerLat));
		ds.writeFloat(MyMath.degToRad(t.centerLon));
		ds.writeShort(interestNodes.size()+wayNodes.size());
		ds.writeShort(interestNodes.size());		
		for (Node n : interestNodes) {
			n.renumberdId=(short) ren++;
			//The exclusion of nodes is not perfect, as there
			//is a time between adding nodes to the write buffer
			//and before marking them as written, so we might
			//still hit the case when a node is written twice.
			//Warn about this fact to fix this correctly at a
			//later stage
			if (n.fid != 0) 
				System.out.println("Writing interest node twice? " + n); 
			writeNode(n,ds,INODE,t);
		}
		for (Node n : wayNodes.values()) {
			n.renumberdId=(short) ren++;
			writeNode(n,ds,SEGNODE,t);
		}
		ds.writeByte(0x55); // Magic number
		ds.writeByte(ways.size());
		for (Way w : ways){
			w.write(ds, names1,t);
		}
		ds.writeByte(0x56); // Magic number
		fo.close();
		return fo.toByteArray();
	}
	
	private void writeRouteNode(Node n,DataOutputStream nds,DataOutputStream cds) throws IOException{
		nds.writeByte(4);
		nds.writeFloat(MyMath.degToRad(n.lat));
		nds.writeFloat(MyMath.degToRad(n.lon));
		nds.writeInt(cds.size());
		nds.writeByte(n.routeNode.connected.size());
		for (Connection c : n.routeNode.connected){
			cds.writeInt(c.to.node.renumberdId);
			cds.writeShort((int) c.time);
			cds.writeShort((int) c.length);
			cds.writeByte(c.startBearing);
			cds.writeByte(c.endBearing);
		}
	}

	private void writeNode(Node n,DataOutputStream ds,int type, Tile t) throws IOException{
		
		int flags=0;
		int nameIdx = -1;
		if (n.routeNode != null){
			flags += Constants.NODE_MASK_ROUTENODELINK;
		}
		if (type == INODE){
			if (! "".equals(n.getName())){
				flags += Constants.NODE_MASK_NAME;
				nameIdx = names1.getNameIdx(n.getName());
				if (nameIdx >= Short.MAX_VALUE) {
					flags += Constants.NODE_MASK_NAMEHIGH;
				} 
			}
			if (n.getType(configuration) != -1){
				flags += Constants.NODE_MASK_TYPE;
			}
		}
		ds.writeByte(flags);
		if ((flags & Constants.NODE_MASK_ROUTENODELINK) > 0){
			ds.writeShort(n.routeNode.id);			
		}
		
		/**
		 * Convert coordinates to relative fixpoint (integer) coordinates
		 * The reference point is the center of the tile.
		 * With 16bit shorts, this should allow for tile sizes of
		 * about 65km in width and with 1m accuracy at the equator.  
		 */
		double tmpLat = (MyMath.degToRad(n.lat - t.centerLat)) * Tile.fpm;
		double tmpLon = (MyMath.degToRad(n.lon - t.centerLon)) * Tile.fpm;
		if ((tmpLat > Short.MAX_VALUE) || (tmpLat < Short.MIN_VALUE)) {
			System.out.println("Numeric Over flow of Latitude for node: " + n.id);
		}
		if ((tmpLon > Short.MAX_VALUE) || (tmpLon < Short.MIN_VALUE)) {
			System.out.println("Numeric Over flow of Longitude for node: " + n.id);
		}
		ds.writeShort((short)tmpLat);
		ds.writeShort((short)tmpLon);
		
		
		if ((flags & Constants.NODE_MASK_NAME) > 0){
			if ((flags & Constants.NODE_MASK_NAMEHIGH) > 0) {
				ds.writeInt(nameIdx);
			} else {
				ds.writeShort(nameIdx);
			}
					
		}
		if ((flags & Constants.NODE_MASK_TYPE) > 0){
			ds.writeByte(n.getType(configuration));
		}

	}



	/**
	 * @param c
	 */
	public void setConfiguration(Configuration c) {
		this.configuration = c;
		// TODO Auto-generated method stub
		
	}

	/**
	 * @param rd
	 */
	public void setRouteData(RouteData rd) {
		this.rd = rd;
	}

}
