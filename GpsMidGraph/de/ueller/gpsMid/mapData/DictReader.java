package de.ueller.gpsMid.mapData;
/*
 * GpsMid - Copyright (c) 2007 Harald Mueller james22 at users dot sourceforge dot net 
 * See Copying
 */

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

import de.ueller.midlet.gps.Logger;
import de.ueller.midlet.gps.Trace;


public class DictReader  implements Runnable {
	private Thread	processorThread;
    //#debug error
	private final static Logger logger=Logger.getInstance(DictReader.class,Logger.TRACE);
	private Tile dict;
	private final Trace	t;

	
	public DictReader(Trace t){
		super();
		this.t = t;
		processorThread = new Thread(this);
		processorThread.setPriority(Thread.MIN_PRIORITY+2);
		processorThread.start();
//		logger.info("DictReader Thread started");

	}

	public void run() {
		try {
			for (byte i=0;i<=4;i++) {
				readData(i);
			}
		} catch (IOException e) {
			//#debug error
			logger.error("Error:" + e.getMessage());
			e.printStackTrace();
		}

	}
	private void readData(byte zl) throws IOException{
		try{
		    //#debug error
		    	logger.info("open dict-"+zl+".dat");
				InputStream is = getClass().getResourceAsStream("/dict-"+zl+".dat");
//		    	logger.info("open DataInputStream");
				DataInputStream ds=new DataInputStream(is);
//				logger.info("read Magic code");
				if (! "DictMid".equals(ds.readUTF())){
					throw new IOException("not a DictMid-file");
				}
//				logger.trace("read TileType");
				byte type=ds.readByte();
//				logger.trace("TileType="+type);
				switch (type) {
				case Tile.TYPE_MAP:
					dict=new SingleTile(ds,1,zl);
					break;
				case Tile.TYPE_CONTAINER:
		    		dict=new ContainerTile(ds,1,zl);
		    		break;
				case Tile.TYPE_EMPTY:
					// empty tile;
					break;
				case Tile.TYPE_FILETILE:
					dict=new FileTile(ds,1,zl);
					break;
				case Tile.TYPE_ROUTEDATA:
					// RouteData Tile
					dict=new RouteTile(ds,1,zl);
					break;
				case Tile.TYPE_ROUTECONTAINER:
					// RouteData Tile
					dict=new RouteContainerTile(ds,1,zl);
				default:
					break;
				}
		    	
		    	t.setDict(dict,zl);
		} catch (Exception e){
			//#debug error
			logger.error(e.getMessage());
			e.printStackTrace();
		}
	}

}
