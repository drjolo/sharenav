/*
 * Configuration - Copyright (c) 2007 Harald Mueller james22 at users dot sourceforge dot net
 * See COPYING
 */

package de.ueller.gps.data;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Vector;

import javax.microedition.io.Connection;
import javax.microedition.io.Connector;
//#if polish.api.fileconnection
import javax.microedition.io.file.FileConnection;
//#endif
import javax.microedition.lcdui.Command;
import javax.microedition.rms.InvalidRecordIDException;
import javax.microedition.rms.RecordStore;

import net.sourceforge.util.zip.ZipFile;

import de.ueller.gps.tools.BufferedReader;
import de.ueller.gps.tools.StringTokenizer;
import de.ueller.gps.tools.intTree;
import de.ueller.gpsMid.mapData.QueueReader;
import de.ueller.midlet.gps.Logger;
import de.ueller.midlet.gps.data.Node;
import de.ueller.midlet.gps.data.ProjFactory;
import de.ueller.midlet.gps.routing.TravelMode;

/**
 * This class holds all the configurable data (i.e. the settings) that GpsMid has.
 */
public class Configuration {
	
	private static Logger logger;
	
	/** VERSION of the Configuration
	 *  If in the recordstore (configVersionStored) there is a lower version than this,
	 *  the default values for the features added between configVersionStored
	 *  and VERSION will be set, before the version in the recordstore is increased to VERSION.
	 */
	public final static int VERSION = 17;

	public final static int LOCATIONPROVIDER_NONE = 0;
	public final static int LOCATIONPROVIDER_SIRF = 1;
	public final static int LOCATIONPROVIDER_NMEA = 2;
	public final static int LOCATIONPROVIDER_JSR179 = 3;
	public final static int LOCATIONPROVIDER_SECELL = 4;
	
	// bit 0: render as street
	public final static byte CFGBIT_STREETRENDERMODE = 0;
//	// bit 1 have default values been once applied?
//	public final static byte CFGBIT_DEFAULTVALUESAPPLIED = 1;
	// bit 2: show POITEXT
	public final static byte CFGBIT_POITEXTS = 2;
	// bit 3: show WAYTEXT
	public final static byte CFGBIT_WAYTEXTS = 3;
	// bit 4: show AREATEXT
	public final static byte CFGBIT_AREATEXTS = 4;
	// bit 5: show POIS
	public final static byte CFGBIT_POIS = 5;
	// bit 6: show WPTTTEXT
	public final static byte CFGBIT_WPTTEXTS = 6;
	// bit 7: show descriptions
	public final static byte CFGBIT_SHOWWAYPOITYPE = 7;
	// bit 8: show latlon
	public final static byte CFGBIT_SHOWLATLON = 8;
	// bit 9: full screen
	public final static byte CFGBIT_FULLSCREEN = 9;
	// bit 10: keep backlight on
	public final static byte CFGBIT_BACKLIGHT_ON = 10;
	// bit 11: backlight on map screen as keep-alive (every 60 s) only
	public final static byte CFGBIT_BACKLIGHT_ONLY_KEEPALIVE = 11;
	// bit 12: backlight method MIDP2
	public final static byte CFGBIT_BACKLIGHT_MIDP2 = 12;
	// bit 13: backlight method NOKIA
	public final static byte CFGBIT_BACKLIGHT_NOKIA = 13;
	// bit 14: backlight method NOKIA/FLASH
	public final static byte CFGBIT_BACKLIGHT_NOKIAFLASH = 14;
	// bit 15: backlight only on while GPS is started
	public final static byte CFGBIT_BACKLIGHT_ONLY_WHILE_GPS_STARTED = 15;
	// bit 16: save map position on exit
	public final static byte CFGBIT_AUTOSAVE_MAPPOS = 16;
	// bit 17: Sound on Connect
	public final static byte CFGBIT_SND_CONNECT = 17;
	// bit 18: Sound on Disconnect
	public final static byte CFGBIT_SND_DISCONNECT = 18;
	// bit 19: Routing Instructions
	public final static byte CFGBIT_SND_ROUTINGINSTRUCTIONS = 19;
	// bit 20: Gps Auto Reconnect
	public final static byte CFGBIT_GPS_AUTORECONNECT = 20;
	// bit 21: Sound when destination reached
	public final static byte CFGBIT_SND_DESTREACHED = 21;
	// bit 22: auto recalculate route
	public final static byte CFGBIT_ROUTE_AUTO_RECALC = 22;
	// bit 23: use JSR135 or JSR 234 for taking pictures;
	public final static byte CFGBIT_USE_JSR_234 = 23;
	// bit 25: show point of compass in rotated map
	public final static byte CFGBIT_SHOW_POINT_OF_COMPASS = 25;
	// bit 26: add geo reference into the exif of a photo;
	public final static byte CFGBIT_ADD_EXIF = 26;
	// bit 27: show AREAS
	public final static byte CFGBIT_AREAS = 27;
	// bit 28: big poi labels
	public final static byte CFGBIT_POI_LABELS_LARGER = 28;
	// bit 29: big wpt labels
	public final static byte CFGBIT_WPT_LABELS_LARGER = 29;
	// bit 30: show oneway arrows
	public final static byte CFGBIT_ONEWAY_ARROWS = 30;
	// bit 31: Debug Option: show route connections
	public final static byte CFGBIT_ROUTE_CONNECTIONS = 31;
	// bit 32: backlight method SIEMENS
	public final static byte CFGBIT_BACKLIGHT_SIEMENS = 32;
	// bit 33: Skip initial splash screen
	public final static byte CFGBIT_SKIPP_SPLASHSCREEN = 33;
	// bit 34: show place labels
	public final static byte CFGBIT_PLACETEXTS = 34;
	// bit 35: Sound alert for speeding
	public final static byte CFGBIT_SPEEDALERT_SND = 35;
	// bit 36: Visual alert for speeding
	public final static byte CFGBIT_SPEEDALERT_VISUAL = 36;
	// bit 37: Debug Option: show route bearings
	public final static byte CFGBIT_ROUTE_BEARINGS = 37;
	// bit 38: Debug Option: hide quiet arrows
	public final static byte CFGBIT_ROUTE_HIDE_QUIET_ARROWS = 38;
	// bit 39: in route mode up/down keys are for route browsing
	public final static byte CFGBIT_ROUTE_BROWSING = 39;
	// bit 40: Show scale bar on map
	public final static byte CFGBIT_SHOW_SCALE_BAR = 40;
	// bit 41: Log cell-ids to directory
	public final static byte CFGBIT_CELLID_LOGGING = 41;
	// bit 42: Flag whether to also put waypoints in GPX track
	public final static byte CFGBIT_WPTS_IN_TRACK = 42;
	// bit 43: Ask for GPX track name when starting recording
	public final static byte CFGBIT_GPX_ASK_TRACKNAME_START = 43;
	// bit 44: Ask for GPX track name when starting recording
	public final static byte CFGBIT_GPX_ASK_TRACKNAME_STOP = 44;
	// bit 45: Flag whether to always upload cellid log to opencellid
	public final static byte CFGBIT_CELLID_ALWAYS = 45;
	// bit 46: Flag whether to upload cellid log to opencellid after confirm
	public final static byte CFGBIT_CELLID_CONFIRM = 46;
	// bit 47: Flag whether to fall back to cellid location when GPS fix not available
	public final static byte CFGBIT_CELLID_FALLBACK = 47;
	// bit 48: Flag whether to cache cellid locations
	public final static byte CFGBIT_CELLID_OFFLINEONLY = 48;
	// bit 49: Flag whether to cache cellid locations
	public final static byte CFGBIT_CELLID_ONLINEONLY = 49;
	// bit 50: Flag whether to also put waypoints in waypoint store when recording GPX
	public final static byte CFGBIT_WPTS_IN_WPSTORE = 50;
	// bit 51: Flag whether to show turn restrictions for debugging
	public final static byte CFGBIT_SHOW_TURN_RESTRICTIONS = 51;
	// bit 52: Flag whether turn restrictions should be used for route calculation
	public final static byte CFGBIT_USE_TURN_RESTRICTIONS_FOR_ROUTE_CALCULATION = 52;
	// bit 53: Flag whether iconMenus should be used
	public final static byte CFGBIT_ICONMENUS = 53;
	// bit 54: Flag whether iconMenus should be fullscreen
	public final static byte CFGBIT_ICONMENUS_FULLSCREEN = 54;
	// bit 55: Flag whether iconMenus should be optimized for routing
	public final static byte CFGBIT_ICONMENUS_ROUTING_OPTIMIZED = 55;
	// bit 56: Flag whether night style should be applied
	public final static byte CFGBIT_NIGHT_MODE = 56;
	// bit 57: Flag whether turbo route calc should be used
	public final static byte CFGBIT_TURBO_ROUTE_CALC = 57;
	// bit 58: Flag whether speed should be displayed in map screen
	public final static byte CFGBIT_SHOW_SPEED_IN_MAP = 58;
	// bit 59: Flag whether to start GPS reception when entering map
	public final static byte CFGBIT_AUTO_START_GPS = 59;
	// bit 60: Flag whether to display in metric or imperial units
	public final static byte CFGBIT_METRIC = 60;
	// bit 61: Flag whether air distance to destination should be displayed in map screen
	public final static byte CFGBIT_SHOW_AIR_DISTANCE_IN_MAP = 61;
	// bit 62: Flag whether offset to route should be displayed in map screen
	public final static byte CFGBIT_SHOW_OFF_ROUTE_DISTANCE_IN_MAP = 62;
	// bit 63: Flag whether route duration should be displayed in map screen
	public final static byte CFGBIT_SHOW_ROUTE_DURATION_IN_MAP = 63;
	// bit 64: Flag whether altitude should be displayed in map screen
	public final static byte CFGBIT_SHOW_ALTITUDE_IN_MAP = 64;
	// bit 65: Flag whether to show buildings in map
	public final static byte CFGBIT_BUILDINGS = 65;
	// bit 66: Flag whether to show building labels in map
	public final static byte CFGBIT_BUILDING_LABELS = 66;
	// bit 67: Flag if current time is shown on map
	public final static byte CFGBIT_SHOW_CLOCK_IN_MAP = 67;
	// bit 68: Flag if ETA is shown on map
	public final static byte CFGBIT_SHOW_ETA_IN_MAP = 68;
	// bit 69: Flag if seasonal (winter) speed limits are applied
	public final static byte CFGBIT_MAXSPEED_WINTER = 69;
	// bit 70: Flag whether iconMenus should have icons mapped on keys
	public final static byte CFGBIT_ICONMENUS_MAPPED_ICONS = 70;
	/** bit 71: Flag whether display size specific defaults are set
	(we need a canvas to determine display size, so we can't determine appropriate defaults in Configuration) */
	public final static byte CFGBIT_DISPLAYSIZE_SPECIFIC_DEFAULTS_DONE = 71;
	/** bit 72: Flag whether initial Setup Forms were shown to the user */
	public final static byte CFGBIT_INITIAL_SETUP_DONE = 72;
	/** bit 73: Flag whether to add a Back command in fullscreen menu */
	public final static byte CFGBIT_ICONMENUS_BACK_CMD = 73;
	/** bit 74: Flag whether the route algorithm should try to find a motorway within 20 km */
	public final static byte CFGBIT_ROUTE_TRY_FIND_MOTORWAY = 74;
	/** bit 75: Flag whether the route algorithm deeply examines motorways */
	public final static byte CFGBIT_ROUTE_BOOST_MOTORWAYS = 75;
	/** bit 76: Flag whether the route algorithm deeply examines trunks and primarys */
	public final static byte CFGBIT_ROUTE_BOOST_TRUNKS_PRIMARYS = 76;
	/** bit 77: Flag whether iconMenus should have bigger tabs */
	public final static byte CFGBIT_ICONMENUS_BIG_TAB_BUTTONS = 77;
	/** bit 78: Flag whether the map should be auto scaled to speed */
	public final static byte CFGBIT_AUTOZOOM = 78;
	/** bit 79: Flag whether, if available, tone sequences should be played instead of sound samples */
	public final static byte CFGBIT_SND_TONE_SEQUENCES_PREFERRED = 79;
	/** bit 80: Flag whether internal PNG files should be preferred when using an external map (faster on some Nokias) */
	public final static byte CFGBIT_PREFER_INTERNAL_PNGS = 80;
	/** bit 81: Flag whether the menu with predefined way points is shown. */
	public final static byte CFGBIT_WAYPT_OFFER_PREDEF = 81;
	
	/**
	 * These are the database record IDs for each configuration option
	 */
	private static final int RECORD_ID_BT_URL = 1;
	private static final int RECORD_ID_LOCATION_PROVIDER = 2;
	private static final int RECORD_ID_CFGBITS_0_TO_63 = 3;
	private static final int RECORD_ID_GPX_URL = 4;
	private static final int RECORD_ID_MAP_FROM_JAR = 5;
	private static final int RECORD_ID_MAP_FILE_URL = 6;
	//private static final int RECORD_ID_BACKLIGHT_DEFAULT = 7;
	private static final int RECORD_ID_LOG_RAW_GPS_URL = 8;
	private static final int RECORD_ID_LOG_RAW_GPS_ENABLE = 9;
	private static final int RECORD_ID_LOG_DEBUG_URL = 10;
	private static final int RECORD_ID_LOG_DEBUG_ENABLE = 11;
	private static final int RECORD_ID_DETAIL_BOOST = 12;
	private static final int RECORD_ID_GPX_FILTER_MODE = 13;
	private static final int RECORD_ID_GPX_FILTER_TIME = 14;
	private static final int RECORD_ID_GPX_FILTER_DIST = 15;
	private static final int RECORD_ID_GPX_FILTER_ALWAYS_DIST = 16;
	private static final int RECORD_ID_LOG_DEBUG_SEVERITY = 17;
	private static final int RECORD_ID_ROUTE_ESTIMATION_FAC = 18;
	private static final int RECORD_ID_CONTINUE_MAP_WHILE_ROUTING = 19;
	private static final int RECORD_ID_BT_KEEPALIVE = 20;
	private static final int RECORD_ID_STARTUP_RADLAT = 21;
	private static final int RECORD_ID_STARTUP_RADLON = 22;
	private static final int RECORD_ID_PHOTO_URL = 23;
	private static final int RECORD_ID_GPS_RECONNECT = 24;
	private static final int RECORD_ID_PHOTO_ENCODING = 25;
	private static final int RECORD_ID_MAP_PROJECTION = 26;
	private static final int RECORD_ID_CONFIG_VERSION = 27;
	private static final int RECORD_ID_SMS_RECIPIENT = 28;
	private static final int RECORD_ID_SPEED_TOLERANCE = 29;
	private static final int RECORD_ID_OSM_USERNAME = 30;
	private static final int RECORD_ID_OSM_PWD = 31;
	private static final int RECORD_ID_OSM_URL = 32;
	private static final int RECORD_ID_MIN_ROUTELINE_WIDTH = 33;
	private static final int RECORD_ID_KEY_SHORTCUT = 34;
	private static final int RECORD_ID_AUTO_RECENTER_TO_GPS_MILLISECS = 35;
	private static final int RECORD_ID_ROUTE_TRAVEL_MODE = 36;
	private static final int RECORD_ID_OPENCELLID_APIKEY = 37;
	private static final int RECORD_ID_PHONE_ALL_TIME_MAX_MEMORY = 38;
	private static final int RECORD_ID_CFGBITS_64_TO_127 = 39;
	private static final int RECORD_ID_MAINSTREET_NET_DISTANCE_KM = 40;
	private static final int RECORD_ID_DETAIL_BOOST_POI = 41;
	private static final int RECORD_ID_TRAFFIC_SIGNAL_CALC_DELAY = 42;
	private static final int RECORD_ID_WAYPT_SORT_MODE = 43;
	private static final int RECORD_ID_BACKLIGHTLEVEL = 43;

	// Gpx Recording modes
	// GpsMid determines adaptive if a trackpoint is written
	public final static int GPX_RECORD_ADAPTIVE = 0;
	// User specified options define if a trackpoint is written
	public final static int GPX_RECORD_MINIMUM_SECS_DIST = 1;
	
	public static int KEYCODE_CAMERA_COVER_OPEN = -34;
	public static int KEYCODE_CAMERA_COVER_CLOSE = -35;
	public static int KEYCODE_CAMERA_CAPTURE = -26;

	public static final int MAX_WAYPOINTNAME_LENGTH = 255;
	public static final int MAX_WAYPOINTNAME_DRAWLENGTH = 25;
	public static final int MAX_TRACKNAME_LENGTH = 50;
	public static final int MAX_WAYPOINTS_NAME_LENGTH = 50;
	
	public final static String[] LOCATIONPROVIDER = { "None", "Bluetooth (Sirf)",
		"Bluetooth (NMEA)", "Internal (JSR179)", "Cell-ID (OpenCellId.org)" };
	
	private static final String[] compassDirections  =
	{ "N", "NNE", "NE", "ENE", "E", "ESE", "SE", "SSE",
	  "S", "SSW", "SW", "WSW", "W", "WNW", "NW", "NNW",
	  "N" };
	
	private final static byte[] empty = "".getBytes();

	private static String btUrl;
	/** This URL is used to store logs of raw data received from the GPS receiver*/
	private static String rawGpsLogUrl;
	private static boolean rawGpsLogEnable;
	private static String rawDebugLogUrl;
	private static boolean rawDebugLogEnable;
	private static int locationProvider = 0;
	private static int gpxRecordRuleMode;
	private static int gpxRecordMinMilliseconds;
	private static int gpxRecordMinDistanceCentimeters;
	private static int gpxRecordAlwaysDistanceCentimeters;
	private static long cfgBits_0_to_63 = 0;
	private static long cfgBitsDefault_0_to_63 = 0;
	private static long cfgBits_64_to_127 = 0;
	private static long cfgBitsDefault_64_to_127 = 0;
	private static int detailBoost = 0;
	private static int detailBoostPOI = 0;
	private static int detailBoostDefault = 0;
	private static int detailBoostDefaultPOI = 0;
	private static float detailBoostMultiplier;
	private static float detailBoostMultiplierPOI;
	private static String gpxUrl;
	private static String photoUrl;
	private static String photoEncoding;
	private static int debugSeverity;
	private static int routeEstimationFac = 6;
	
	// Constants for continueMapWhileRouteing
	public static final int continueMap_Not = 0;
	public static final int continueMap_At_Route_Line_Creation = 1;
	public static final int continueMap_Always = 2;
		
	/** 0 = do not continue map, 1 = continue map only during route line production, 2 = continue map all the time */
	private static int continueMapWhileRouteing = continueMap_At_Route_Line_Creation;

	private static boolean btKeepAlive = false;
	private static boolean btAutoRecon = false;
	private static Node startupPos = new Node(0.0f, 0.0f);
	private static byte projTypeDefault = ProjFactory.NORTH_UP;
	
	private static boolean mapFromJar;
	private static String mapFileUrl;
	private static ZipFile mapZipFile;

	private static String smsRecipient;
	private static int speedTolerance = 0;
	
	private static String utf8encodingstring = null;
	
	private static String osm_username;
	private static String osm_pwd;
	private static String osm_url;

	private static String opencellid_apikey;

	private static long phoneAllTimeMaxMemory = 0;
	
	private static int minRouteLineWidth = 0;
	private static int mainStreetDistanceKm = 0;
	private static int autoRecenterToGpsMilliSecs = 10;
	private static int currentTravelModeNr = 0;
	private static int currentTravelMask = 0;
	
	// Constants for way point sort mode
	public static final int WAYPT_SORT_MODE_NONE = 0;
	public static final int WAYPT_SORT_MODE_NEW_FIRST = 1;
	public static final int WAYPT_SORT_MODE_OLD_FIRST = 2;
	public static final int WAYPT_SORT_MODE_ALPHABET = 3;
	public static final int WAYPT_SORT_MODE_DISTANCE = 4;
	
	private static int wayptSortMode = WAYPT_SORT_MODE_NEW_FIRST;

	private static int trafficSignalCalcDelay = 5;

	private static volatile int backLightLevel = 50;
	
	public static void read() {
	logger = Logger.getInstance(Configuration.class, Logger.DEBUG);
	RecordStore	database;
		try {
			database = RecordStore.openRecordStore("Receiver", true);
			if (database == null) {
				//#debug debug
				System.out.println("Could not open config"); // Logger won't work if config is not read yet
				return;
			}
			cfgBits_0_to_63 = readLong(database, RECORD_ID_CFGBITS_0_TO_63);
			cfgBits_64_to_127 = readLong(database, RECORD_ID_CFGBITS_64_TO_127);
			btUrl = readString(database, RECORD_ID_BT_URL);
			locationProvider = readInt(database, RECORD_ID_LOCATION_PROVIDER);
			gpxUrl = readString(database, RECORD_ID_GPX_URL);
			photoUrl = readString(database, RECORD_ID_PHOTO_URL);
			photoEncoding = readString(database, RECORD_ID_PHOTO_ENCODING);
			mapFromJar = (readInt(database, RECORD_ID_MAP_FROM_JAR) == 0);
			mapFileUrl = readString(database, RECORD_ID_MAP_FILE_URL);
			rawGpsLogUrl = readString(database, RECORD_ID_LOG_RAW_GPS_URL);
			rawGpsLogEnable = (readInt(database, RECORD_ID_LOG_RAW_GPS_ENABLE) !=0);
			detailBoost = readInt(database, RECORD_ID_DETAIL_BOOST);
			detailBoostDefault = detailBoost;
			detailBoostPOI = readInt(database, RECORD_ID_DETAIL_BOOST_POI);
			detailBoostDefaultPOI = detailBoostPOI;
			calculateDetailBoostMultipliers();
			gpxRecordRuleMode = readInt(database, RECORD_ID_GPX_FILTER_MODE);
			gpxRecordMinMilliseconds = readInt(database, RECORD_ID_GPX_FILTER_TIME);
			gpxRecordMinDistanceCentimeters = readInt(database, RECORD_ID_GPX_FILTER_DIST);
			gpxRecordAlwaysDistanceCentimeters = readInt(database, RECORD_ID_GPX_FILTER_ALWAYS_DIST);
			rawDebugLogUrl = readString(database, RECORD_ID_LOG_DEBUG_URL);
			rawDebugLogEnable = (readInt(database,  RECORD_ID_LOG_DEBUG_ENABLE) !=0);
			debugSeverity = readInt(database, RECORD_ID_LOG_DEBUG_SEVERITY);
			routeEstimationFac = readInt(database, RECORD_ID_ROUTE_ESTIMATION_FAC);
			continueMapWhileRouteing = readInt(database, RECORD_ID_CONTINUE_MAP_WHILE_ROUTING);
			btKeepAlive = (readInt(database, RECORD_ID_BT_KEEPALIVE) !=0);
			btAutoRecon = (readInt(database, RECORD_ID_GPS_RECONNECT) !=0);
			String s = readString(database, RECORD_ID_STARTUP_RADLAT);
			String s2 = readString(database, RECORD_ID_STARTUP_RADLON);
			if (s != null && s2 != null) {
				try {
					startupPos.radlat = Float.parseFloat(s);
					startupPos.radlon = Float.parseFloat(s2);
				} catch (NumberFormatException nfe) {
					logger.exception("Error parsing startupPos: ", nfe);					
				}
			}
			//System.out.println("Map startup lat/lon: " + startupPos.radlat*MoreMath.FAC_RADTODEC + "/" + startupPos.radlon*MoreMath.FAC_RADTODEC);
			setProjTypeDefault((byte) readInt(database,  RECORD_ID_MAP_PROJECTION));
			smsRecipient = readString(database, RECORD_ID_SMS_RECIPIENT);
			speedTolerance = readInt(database, RECORD_ID_SPEED_TOLERANCE);
			osm_username = readString(database, RECORD_ID_OSM_USERNAME);
			osm_pwd = readString(database, RECORD_ID_OSM_PWD);
			osm_url = readString(database, RECORD_ID_OSM_URL);
			if (osm_url == null) {
				osm_url = "http://api.openstreetmap.org/api/0.6/";
			}

			opencellid_apikey = readString(database, RECORD_ID_OPENCELLID_APIKEY);

			minRouteLineWidth = readInt(database, RECORD_ID_MIN_ROUTELINE_WIDTH);
			mainStreetDistanceKm = readInt(database, RECORD_ID_MAINSTREET_NET_DISTANCE_KM);
			autoRecenterToGpsMilliSecs = readInt(database, RECORD_ID_AUTO_RECENTER_TO_GPS_MILLISECS);
			currentTravelModeNr = readInt(database, RECORD_ID_ROUTE_TRAVEL_MODE);
			currentTravelMask = 1 << currentTravelModeNr;
			phoneAllTimeMaxMemory = readLong(database, RECORD_ID_PHONE_ALL_TIME_MAX_MEMORY);
			trafficSignalCalcDelay = readInt(database, RECORD_ID_TRAFFIC_SIGNAL_CALC_DELAY);
			wayptSortMode = readInt(database, RECORD_ID_WAYPT_SORT_MODE);
			backLightLevel = readInt(database, RECORD_ID_BACKLIGHTLEVEL);
			
			int configVersionStored = readInt(database, RECORD_ID_CONFIG_VERSION);
			//#debug info
			logger.info("Config version stored: " + configVersionStored);

			/* close the record store before accessing it nested for writing
			 * might otherwise cause problems on some devices
			 * see [ gpsmid-Bugs-2983148 ] Recordstore error on startup, settings are not persistent 
			 */
			database.closeRecordStore();
			
			applyDefaultValues(configVersionStored);
			// remember for which version the default values were stored
			write(VERSION, RECORD_ID_CONFIG_VERSION);
			
		} catch (Exception e) {
			logger.exception("Problems with reading our configuration: ", e);
		}
	}
	
	/** If in the recordstore (configVersionStored) there is a lower version than VERSION
	 *  of the Configuration, the default values for the features added between configVersionStored
	 *  and VERSION will be set, before the version in the recordstore is increased to VERSION.
	 */
	private static void applyDefaultValues(int configVersionStored) {
		if (configVersionStored < 1) {
			cfgBits_0_to_63 =	1L << CFGBIT_STREETRENDERMODE |
			   			1L << CFGBIT_POITEXTS |
			   			1L << CFGBIT_AREATEXTS |
			   			1L << CFGBIT_WPTTEXTS |
			   			// 1L << CFGBIT_WAYTEXTS | // way texts are still experimental
			   			1L << CFGBIT_ONEWAY_ARROWS |
			   			1L << CFGBIT_POIS |
			   			1L << CFGBIT_AUTOSAVE_MAPPOS |
			   			getDefaultDeviceBacklightMethodMask();
			// Record Rule Default
			setGpxRecordRuleMode(GPX_RECORD_MINIMUM_SECS_DIST);
			setGpxRecordMinMilliseconds(1000);
			setGpxRecordMinDistanceCentimeters(300);
			setGpxRecordAlwaysDistanceCentimeters(500);
			// Routing defaults
			setContinueMapWhileRouteing(continueMap_At_Route_Line_Creation);
			setRouteEstimationFac(7);
			// set default location provider to JSR-179 if available
			//#if polish.api.locationapi
			if (getDeviceSupportsJSR179()) {
				setLocationProvider(LOCATIONPROVIDER_JSR179);
			}
			//#endif
			//#debug info
			logger.info("Default config for version 0.4.0+ set.");
		}
		if (configVersionStored < 3) {
			cfgBits_0_to_63 |=	1L << CFGBIT_SND_CONNECT |
				   		1L << CFGBIT_SND_DISCONNECT |
				   		1L << CFGBIT_SND_ROUTINGINSTRUCTIONS |
				   		1L << CFGBIT_SND_DESTREACHED |
				   		1L << CFGBIT_SHOW_POINT_OF_COMPASS |
				   		1L << CFGBIT_AREAS |
				   		1L << CFGBIT_ROUTE_AUTO_RECALC;

			// Auto-reconnect GPS
			setBtAutoRecon(true);
			// make MOVE_UP map the default
			setProjTypeDefault(ProjFactory.MOVE_UP);
			//#debug info
			logger.info("Default config for version 3+ set.");
		}
		if (configVersionStored < 5) {
			cfgBits_0_to_63 |=	1L << CFGBIT_PLACETEXTS |
						1L << CFGBIT_SPEEDALERT_SND |
						1L << CFGBIT_ROUTE_HIDE_QUIET_ARROWS |
						1L << CFGBIT_SHOW_SCALE_BAR |
						1L << CFGBIT_SPEEDALERT_VISUAL;
			setMinRouteLineWidth(3);
			// Speed alert tolerance
			setSpeedTolerance(5);
			//#debug info
			logger.info("Default config for version 5+ set.");
		}
		if (configVersionStored < 6) {
			setAutoRecenterToGpsMilliSecs(30000);
			cfgBits_0_to_63 |=	1L << CFGBIT_BACKLIGHT_ONLY_WHILE_GPS_STARTED;
			logger.info("Default config for version 6+ set.");
//			if (getPhoneModel().startsWith("MicroEmulator")) {
//				cfgBits |= 	1L<<CFGBIT_ICONMENUS |
//							1L<<CFGBIT_ICONMENUS_FULLSCREEN;
//			}
		}
		if (configVersionStored < 7) {
			cfgBits_0_to_63 |=	1L << CFGBIT_SHOW_SPEED_IN_MAP |
						1L << CFGBIT_AUTO_START_GPS;
		}
		
		if (configVersionStored < 9) {
			cfgBits_0_to_63 |=	1L << CFGBIT_METRIC |
								1L << CFGBIT_SHOW_ROUTE_DURATION_IN_MAP |
								1L << CFGBIT_SHOW_OFF_ROUTE_DISTANCE_IN_MAP |
								1L << CFGBIT_SHOW_AIR_DISTANCE_IN_MAP |
								1L << CFGBIT_ICONMENUS |
								1L << CFGBIT_ICONMENUS_ROUTING_OPTIMIZED |
								1L << CFGBIT_ICONMENUS_FULLSCREEN;

			cfgBits_64_to_127 |=	1L << CFGBIT_SHOW_ALTITUDE_IN_MAP |
									1L << CFGBIT_SHOW_ETA_IN_MAP |
									1L << CFGBIT_BUILDINGS |
									1L << CFGBIT_BUILDING_LABELS |
									1L << CFGBIT_ICONMENUS_MAPPED_ICONS;
									
		}

		if (configVersionStored < 10) {
			setMainStreetDistanceKm(3);
		}
		
		if (configVersionStored < 11) {
			if (getDefaultIconMenuBackCmdSupport()) {
				cfgBits_64_to_127 |=	1L << CFGBIT_ICONMENUS_BACK_CMD;
			}
			
		}

		if (configVersionStored < 13) {
			// migrate boolean stopAllWhileRouteing to int continueMapWhileRouteing
			if (continueMapWhileRouteing == 0) {
				continueMapWhileRouteing = continueMap_Always;
			} else {
				continueMapWhileRouteing = continueMap_At_Route_Line_Creation;
			}
		}

		if (configVersionStored < 14) {
			cfgBits_64_to_127 |=	1L << CFGBIT_AUTOZOOM |
									1L << CFGBIT_ROUTE_TRY_FIND_MOTORWAY |
									1L << CFGBIT_ROUTE_BOOST_MOTORWAYS |
									1L << CFGBIT_ROUTE_BOOST_TRUNKS_PRIMARYS;
		}

		if (configVersionStored < 15) {
			// This bit was recycled as "backlight keep-alive only",
			// so it should be off by default.
			if (getCfgBitState(CFGBIT_BACKLIGHT_ONLY_KEEPALIVE))
			{
				cfgBits_0_to_63 ^= 1L << CFGBIT_BACKLIGHT_ONLY_KEEPALIVE;
			}
			cfgBits_64_to_127 |= 1L << CFGBIT_WAYPT_OFFER_PREDEF;
			wayptSortMode = WAYPT_SORT_MODE_NEW_FIRST;
			setWaypointSortMode(wayptSortMode);
		}

		if (configVersionStored < 16) {
			cfgBits_0_to_63 |= 1L << CFGBIT_USE_TURN_RESTRICTIONS_FOR_ROUTE_CALCULATION;
			setTrafficSignalCalcDelay(5);
		}

		if (configVersionStored < 17) {
			backLightLevel = 50;
			setBackLightLevel(backLightLevel);
		}

		
		setCfgBits(cfgBits_0_to_63, cfgBits_64_to_127);
	}

	private final static String sanitizeString(String s) {
		if (s == null) {
			return "!null!";
		}
		return s;
	}
	
	private final static String desanitizeString(String s) {
		if (s.equalsIgnoreCase("!null!")) {
			return null;
		}
		return s;
	}
	
	private static void write(String s, int idx) {
		writeBinary(sanitizeString(s).getBytes(), idx);
		//#debug info
		logger.info("wrote " + s + " to " + idx);
	}
	
	private static void writeBinary(byte [] data, int idx) {
		RecordStore	database;
		try {
			database = RecordStore.openRecordStore("Receiver", true);
			while (database.getNumRecords() < idx) {
				database.addRecord(empty, 0, empty.length);
			}
			database.setRecord(idx, data, 0, data.length);
			database.closeRecordStore();
			//#debug info
			logger.info("wrote binary data to " + idx);
		} catch (Exception e) {
			logger.exception("Could not write data (idx = " + idx + ") to recordstore", e);
		}
	}
	
	private static void write(int i, int idx) {
		write("" + i, idx);
	}

	private static void write(long i, int idx) {
		write("" + i, idx);
	}

	private static byte [] readBinary(RecordStore database, int idx) {
		try {
			byte[] data;
			try {
				data = database.getRecord(idx);
			}
			catch (InvalidRecordIDException irie) {
				logger.silentexception("Failed to read recordstore entry " + idx, irie);
				//Use defaults
				return null;
			}
			
			return data;
		} catch (Exception e) {
			logger.exception("Failed to read binary from config database", e);
			return null;
		}
	}

	private static String readString(RecordStore database, int idx) {
		byte [] data = readBinary(database, idx);
		if (data == null) {
			return null;
		}
		String ret = desanitizeString(new String(data));
		//#debug info
		logger.info("Read from config database " + idx + ": " + ret);
		return ret;
	}

	private static int readInt(RecordStore database, int idx) {
		try {
			String tmp = readString(database, idx);
			//#debug info
			logger.info("Read from config database " + idx + ": " + tmp);
			if (tmp == null) {
				return 0;
			} else {
				return Integer.parseInt(tmp);
			}
		} catch (Exception e) {
			logger.exception("Failed to read int from config database", e);
			return 0;
		}
	}
	
	private static long readLong(RecordStore database, int idx) {
		try {
			String tmp = readString(database, idx);
			//#debug info
			logger.info("Read from config database " + idx + ": " + tmp);
			if (tmp == null) {
				return 0;
			} else {
				return Long.parseLong(tmp);
			}
		} catch (Exception e) {
			logger.exception("Failed to read Long from config database", e);
			return 0;
		}
	}
	
	public static void serialise(OutputStream os) throws IOException {
		DataOutputStream dos = new DataOutputStream(os);
		dos.writeInt(VERSION);
		dos.writeLong(cfgBits_0_to_63);
		dos.writeLong(cfgBits_64_to_127);
		dos.writeUTF(sanitizeString(btUrl));
		dos.writeInt(locationProvider);
		dos.writeUTF(sanitizeString(gpxUrl));
		dos.writeUTF(sanitizeString(photoUrl));
		dos.writeUTF(sanitizeString(photoEncoding));
		dos.writeBoolean(mapFromJar);
		dos.writeUTF(sanitizeString(mapFileUrl));
		dos.writeUTF(sanitizeString(rawGpsLogUrl));
		dos.writeBoolean(rawGpsLogEnable);
		dos.writeInt(detailBoostDefault);
		dos.writeInt(detailBoostDefaultPOI);
		dos.writeInt(gpxRecordRuleMode);
		dos.writeInt(gpxRecordMinMilliseconds);
		dos.writeInt(gpxRecordMinDistanceCentimeters);
		dos.writeInt(gpxRecordAlwaysDistanceCentimeters);
		dos.writeUTF(sanitizeString(rawDebugLogUrl));
		dos.writeBoolean(rawDebugLogEnable);
		dos.writeInt(debugSeverity);
		dos.writeInt(routeEstimationFac);
		dos.writeInt(continueMapWhileRouteing);
		dos.writeBoolean(btKeepAlive);
		dos.writeBoolean(btAutoRecon);
		dos.writeUTF(sanitizeString(smsRecipient));
		dos.writeInt(speedTolerance);
		dos.writeUTF(sanitizeString(osm_username));
		dos.writeUTF(sanitizeString(osm_pwd));
		dos.writeUTF(sanitizeString(osm_url));
		dos.writeUTF(sanitizeString(opencellid_apikey));
		dos.flush();
	}
	
	public static void deserialise(InputStream is) throws IOException {
		DataInputStream dis = new DataInputStream(is);
		int version = dis.readInt();
		if (version != VERSION) {
			throw new IOException("Version of the stored config does not match with current GpsMid");
		}
		setCfgBits(dis.readLong(), dis.readLong());
		setBtUrl(desanitizeString(dis.readUTF()));
		setLocationProvider(dis.readInt());
		setGpxUrl(desanitizeString(dis.readUTF()));
		setPhotoUrl(desanitizeString(dis.readUTF()));
		setPhotoEncoding(desanitizeString(dis.readUTF()));
		setBuiltinMap(dis.readBoolean());
		setMapUrl(desanitizeString(dis.readUTF()));
		setGpsRawLoggerUrl(desanitizeString(dis.readUTF()));
		setGpsRawLoggerEnable(dis.readBoolean());
		setDetailBoost(dis.readInt(), true);
		setDetailBoostPOI(dis.readInt(), true);
		setGpxRecordRuleMode(dis.readInt());
		setGpxRecordMinMilliseconds(dis.readInt());
		setGpxRecordMinDistanceCentimeters(dis.readInt());
		setGpxRecordAlwaysDistanceCentimeters(dis.readInt());
		setDebugRawLoggerUrl(desanitizeString(dis.readUTF()));
		setDebugRawLoggerEnable(dis.readBoolean());
		debugSeverity = dis.readInt();
		write(debugSeverity, RECORD_ID_LOG_DEBUG_SEVERITY);
		setRouteEstimationFac(dis.readInt());
		setContinueMapWhileRouteing(dis.readInt());
		setBtKeepAlive(dis.readBoolean());
		setBtAutoRecon(dis.readBoolean());
		setSmsRecipient(desanitizeString(dis.readUTF()));
		setSpeedTolerance(dis.readInt());
		setOsmUsername(desanitizeString(dis.readUTF()));
		setOsmPwd(desanitizeString(dis.readUTF()));
		setOsmUrl(desanitizeString(dis.readUTF()));
		setOpencellidApikey(desanitizeString(dis.readUTF()));
	}
	
	public static String getGpsRawLoggerUrl() {
		return rawGpsLogUrl;
	}
	
	public static void setGpsRawLoggerUrl(String url) {
		rawGpsLogUrl = url;
		write(rawGpsLogUrl, RECORD_ID_LOG_RAW_GPS_URL);
	}
	
	public static void setGpsRawLoggerEnable(boolean enabled) {
		rawGpsLogEnable = enabled;
		if (rawGpsLogEnable) {
			write(1, RECORD_ID_LOG_RAW_GPS_ENABLE);
		} else {
			write(0, RECORD_ID_LOG_RAW_GPS_ENABLE);
		}
	}
	
	public static boolean getDebugRawLoggerEnable() {
		return rawDebugLogEnable;
	}
	
	public static String getDebugRawLoggerUrl() {
		return rawDebugLogUrl;
	}
	
	public static void setDebugRawLoggerUrl(String url) {
		rawDebugLogUrl = url;
		write(rawDebugLogUrl, RECORD_ID_LOG_DEBUG_URL);
	}
	
	public static void setDebugSeverityInfo(boolean enabled) {
		if (enabled) {
			debugSeverity |= 0x01;
		} else {
			debugSeverity &= ~0x01;
		}
		write(debugSeverity, RECORD_ID_LOG_DEBUG_SEVERITY);
	}
	
	public static boolean getDebugSeverityInfo() {
		return ((debugSeverity & 0x01) > 0);
	}
	
	public static void setDebugSeverityDebug(boolean enabled) {
		if (enabled) {
			debugSeverity |= 0x02;
		} else {
			debugSeverity &= ~0x02;
		}
		write(debugSeverity, RECORD_ID_LOG_DEBUG_SEVERITY);
	}
	
	public static boolean getDebugSeverityDebug() {
		return ((debugSeverity & 0x02) > 0);
	}
	
	public static void setDebugSeverityTrace(boolean enabled) {
		if (enabled) {
			debugSeverity |= 0x04;
		} else {
			debugSeverity &= ~0x04;
		}
		write(debugSeverity, RECORD_ID_LOG_DEBUG_SEVERITY);
	}
	
	public static boolean getDebugSeverityTrace() {
		return ((debugSeverity & 0x04) > 0);
	}
	
	public static void setDebugRawLoggerEnable(boolean enabled) {
		rawDebugLogEnable = enabled;
		if (rawDebugLogEnable) {
			write(1, RECORD_ID_LOG_DEBUG_ENABLE);
		} else {
			write(0, RECORD_ID_LOG_DEBUG_ENABLE);
		}
	}
	
	public static boolean getGpsRawLoggerEnable() {
		return rawGpsLogEnable;
	}

	public static String getBtUrl() {
		return btUrl;
	}

	public static void setBtUrl(String btUrl) {
		Configuration.btUrl = btUrl;
		write(btUrl, RECORD_ID_BT_URL);
	}

	public static int getLocationProvider() {
		return locationProvider;
	}

	public static void setLocationProvider(int locationProvider) {
		Configuration.locationProvider = locationProvider;
		write(locationProvider, RECORD_ID_LOCATION_PROVIDER);
	}

	public static int getGpxRecordRuleMode() {
		return gpxRecordRuleMode;
	}

	public static void setGpxRecordRuleMode(int gpxRecordRuleMode) {
		Configuration.gpxRecordRuleMode = gpxRecordRuleMode;
			write(gpxRecordRuleMode, RECORD_ID_GPX_FILTER_MODE);
	}

	public static int getGpxRecordMinMilliseconds() {
		return gpxRecordMinMilliseconds;
	}

	public static void setGpxRecordMinMilliseconds(int gpxRecordMinMilliseconds) {
		Configuration.gpxRecordMinMilliseconds = gpxRecordMinMilliseconds;
			write(gpxRecordMinMilliseconds, RECORD_ID_GPX_FILTER_TIME);
	}

	public static int getGpxRecordMinDistanceCentimeters() {
		return gpxRecordMinDistanceCentimeters;
	}

	public static void setGpxRecordMinDistanceCentimeters(int gpxRecordMinDistanceCentimeters) {
		Configuration.gpxRecordMinDistanceCentimeters = gpxRecordMinDistanceCentimeters;
			write(gpxRecordMinDistanceCentimeters, RECORD_ID_GPX_FILTER_DIST);
	}

	public static int getGpxRecordAlwaysDistanceCentimeters() {
		return gpxRecordAlwaysDistanceCentimeters;
	}

	public static void setGpxRecordAlwaysDistanceCentimeters(int gpxRecordAlwaysDistanceCentimeters) {
		Configuration.gpxRecordAlwaysDistanceCentimeters = gpxRecordAlwaysDistanceCentimeters;
			write(gpxRecordAlwaysDistanceCentimeters, RECORD_ID_GPX_FILTER_ALWAYS_DIST);
	}
	
	public static void setPhoneAllTimeMaxMemory(long i) {
		phoneAllTimeMaxMemory = i;
		write(i, RECORD_ID_PHONE_ALL_TIME_MAX_MEMORY);
	}
	

	public static boolean getCfgBitState(byte bit, boolean getDefault) {
		if (bit < 64) {
			if (getDefault) {
				return ((cfgBitsDefault_0_to_63 & (1L << bit)) != 0);
			} else {
				return ((cfgBits_0_to_63 & (1L << bit)) != 0);
			}
		} else {
			if (getDefault) {
				return ((cfgBitsDefault_64_to_127 & (1L << (bit - 64) )) != 0);
			} else {
				return ((cfgBits_64_to_127 & (1L << (bit - 64) )) != 0);
			}
		}
	}

	public static boolean getCfgBitState(byte bit) {
		return getCfgBitState(bit, false);
	}
	
	public static boolean getCfgBitSavedState(byte bit) {
		return getCfgBitState(bit, true);
	}

	public static void toggleCfgBitState(byte bit, boolean savePermanent) {
		setCfgBitState(bit, !getCfgBitState(bit), savePermanent);
	}
	
	public static void setCfgBitState(byte bit, boolean state, boolean savePermanent) {
		if (bit < 64) {
			// set bit
			Configuration.cfgBits_0_to_63 |= (1L << bit);
			if (!state) {
				// clear bit
				Configuration.cfgBits_0_to_63 ^= (1L << bit);
			}
			if (savePermanent) {
				Configuration.cfgBitsDefault_0_to_63 |= (1L << bit);
				if (!state) {
					// clear bit
					Configuration.cfgBitsDefault_0_to_63 ^= (1L << bit);
				}
				write(cfgBitsDefault_0_to_63, RECORD_ID_CFGBITS_0_TO_63);
			}
		} else {
			bit -= 64;
			// set bit
			Configuration.cfgBits_64_to_127 |= (1L << bit);
			if (!state) {
				// clear bit
				Configuration.cfgBits_64_to_127 ^= (1L << bit);
			}
			if (savePermanent) {
				Configuration.cfgBitsDefault_64_to_127 |= (1L << bit);
				if (!state) {
					// clear bit
					Configuration.cfgBitsDefault_64_to_127 ^= (1L << bit);
				}
				write(cfgBitsDefault_64_to_127, RECORD_ID_CFGBITS_64_TO_127);
			}
		}
	}

	public static void setCfgBitSavedState(byte bit, boolean state) {
		setCfgBitState(bit, state, true);
	}
	
	private static void setCfgBits(long cfgBits_0_to_63, long cfgBits_64_to_127) {
		Configuration.cfgBits_0_to_63 = cfgBits_0_to_63;
		Configuration.cfgBitsDefault_0_to_63 = cfgBits_0_to_63;
		write(cfgBitsDefault_0_to_63, RECORD_ID_CFGBITS_0_TO_63);
		
		Configuration.cfgBits_64_to_127 = cfgBits_64_to_127;
		Configuration.cfgBitsDefault_64_to_127 = cfgBits_64_to_127;
		write(cfgBitsDefault_64_to_127, RECORD_ID_CFGBITS_64_TO_127);
	}
	
	public static int getDetailBoost() {
		return detailBoost;
	}
	
	public static int getDetailBoostPOI() {
		return detailBoostPOI;
	}

	public static void setDetailBoost(int detailBoost, boolean savePermanent) {
		Configuration.detailBoost = detailBoost;
		calculateDetailBoostMultipliers();
		if (savePermanent) {
			Configuration.detailBoostDefault = detailBoost;
			write(detailBoost, RECORD_ID_DETAIL_BOOST);
		}
	}

	public static void setDetailBoostPOI(int detailBoost, boolean savePermanent) {
		Configuration.detailBoostPOI = detailBoost;
		calculateDetailBoostMultipliers();
		if (savePermanent) {
			Configuration.detailBoostDefaultPOI = detailBoost;
			write(detailBoost, RECORD_ID_DETAIL_BOOST_POI);
		}
	}

	
	public static float getDetailBoostMultiplier() {
		return detailBoostMultiplier;
	}

	public static float getMaxDetailBoostMultiplier() {
		if (detailBoost >= detailBoostPOI) {
			return detailBoostMultiplier;
		}
		return detailBoostMultiplierPOI;
	}

	public static float getDetailBoostMultiplierPOI() {
		return detailBoostMultiplierPOI;
	}
	
	public static int getDetailBoostDefault() {
		return detailBoostDefault;
	}

	public static int getDetailBoostDefaultPOI() {
		return detailBoostDefaultPOI;
	}

    /**	There's no pow()-function in J2ME so this manually calculates
     * 1.5 ^ detailBoost to get factor to multiply with zoom level limits
    **/
	private static void calculateDetailBoostMultipliers() {
		detailBoostMultiplier = 1;
		for (int i = 1; i <= detailBoost; i++) {
			detailBoostMultiplier *= 1.5;
		}
		detailBoostMultiplierPOI = 1;
		for (int i = 1; i <= detailBoostPOI; i++) {
			detailBoostMultiplierPOI *= 1.5;
		}
	}
	
	public static void setGpxUrl(String url) {
		Configuration.gpxUrl = url;
		write(url, RECORD_ID_GPX_URL);
	}

	public static String getGpxUrl() {
		return gpxUrl;
	}
	
	public static void setPhotoUrl(String url) {
		Configuration.photoUrl = url;
		write(url, RECORD_ID_PHOTO_URL);
	}

	public static String getPhotoUrl() {
		return photoUrl;
	}
	
	public static void setPhotoEncoding(String encoding) {
		photoEncoding = encoding;
		write(encoding, RECORD_ID_PHOTO_ENCODING);
	}

	public static String getPhotoEncoding() {
		return photoEncoding;
	}
	
	public static boolean usingBuiltinMap() {
		return mapFromJar;
	}
	
	public static void setBuiltinMap(boolean mapFromJar) {
		write(mapFromJar ? 0 : 1, RECORD_ID_MAP_FROM_JAR);
		Configuration.mapFromJar = mapFromJar;
	}
	
	public static String getMapUrl() {
		return mapFileUrl;
	}
	
	public static void setMapUrl(String url) {
		write(url, RECORD_ID_MAP_FILE_URL);
		mapFileUrl = url;
	}

	public static String getSmsRecipient() {
		return smsRecipient;
	}
	
	public static void setSmsRecipient(String s) {
		write(s, RECORD_ID_SMS_RECIPIENT);
		smsRecipient = s;
	}
	
	public static int getSpeedTolerance() {
		return speedTolerance;
	}
	
	public static void setSpeedTolerance(int s) {
		write(s, RECORD_ID_SPEED_TOLERANCE);
		speedTolerance = s;
	}

	public static int getMinRouteLineWidth() {
		return minRouteLineWidth;
	}

	public static int getMainStreetDistanceKm() {
		return mainStreetDistanceKm;
	}
	
	
	public static void setMinRouteLineWidth(int w) {
		minRouteLineWidth = Math.max(w, 1);
		write(minRouteLineWidth, RECORD_ID_MIN_ROUTELINE_WIDTH);
	}

	public static void setMainStreetDistanceKm(int km) {
		mainStreetDistanceKm = km;
		write(mainStreetDistanceKm, RECORD_ID_MAINSTREET_NET_DISTANCE_KM);
	}

	
	public static int getAutoRecenterToGpsMilliSecs() {
		return autoRecenterToGpsMilliSecs;
	}

	public static void setAutoRecenterToGpsMilliSecs(int ms) {
		autoRecenterToGpsMilliSecs = ms;
		write(autoRecenterToGpsMilliSecs, RECORD_ID_AUTO_RECENTER_TO_GPS_MILLISECS);
	}
	
	/**
	 * Opens a resource, either from the JAR, the file system or a ZIP archive,
	 * depending on the configuration, see mapFromJar and mapFileUrl.
	 * @param name Full path of the resource
	 * @return Stream which reads from the resource
	 * @throws IOException if file could not be found
	 */
	public static InputStream getMapResource(String name) throws IOException {
		InputStream is = null;
		if (mapFromJar
			||
			(
				Configuration.getCfgBitSavedState(Configuration.CFGBIT_PREFER_INTERNAL_PNGS)
					&&
				name.toLowerCase().endsWith(".png")
			)
		) {
			//#debug debug
			logger.debug("Opening file from JAR: " + name);
			is = QueueReader.class.getResourceAsStream(name);
			if (is != null) {
				return is;
			} else if (!Configuration.getCfgBitSavedState(Configuration.CFGBIT_PREFER_INTERNAL_PNGS)) {
				throw new IOException("Could not find file "/*i:ExFNF1*/ + name +
						" in JAR"/*i:ExFNF2*/);
			}
		}
		//#if polish.api.fileconnection
		try {
			if (mapFileUrl.endsWith("/")) {
				// directory mode
				name = mapFileUrl + name.substring(1);
				//#debug debug
				logger.debug("Opening file from filesystem: " + name);
				FileConnection fc = (FileConnection) Connector.open(name, Connector.READ);
				is = fc.openInputStream();
			}
			else {
				// zipfile mode
				if (mapZipFile == null) {
					mapZipFile = new ZipFile(mapFileUrl, -1);
				}
				//#debug debug
				logger.debug("Opening file from zip-file: " + name);
				is = mapZipFile.getInputStream(mapZipFile.getEntry(name.substring(1)));
			}
		} catch (Exception e) {
			//#debug info
			logger.info("Failed to open: " + name);
			throw new IOException(e.getMessage());
		}
		//#else
		//This should never happen.
		is = null;
		logger.fatal("Error, we don't have access to the filesystem, but our map data is supposed to be there!");
		//#endif

		return is;
	}

	public static int getRouteEstimationFac() {
		return routeEstimationFac;
	}

	public static void setRouteEstimationFac(int routeEstimationFac) {
		write(routeEstimationFac, RECORD_ID_ROUTE_ESTIMATION_FAC);
		Configuration.routeEstimationFac = routeEstimationFac;
	}

	public static int getBackLightLevel() {
		return backLightLevel;
	}

	public static void setBackLightLevel(int backLightLevel) {
		write(backLightLevel, RECORD_ID_BACKLIGHTLEVEL);
		Configuration.backLightLevel = backLightLevel;
	}
	
	public static void addToBackLightLevel(int diffBacklight) {
		backLightLevel += diffBacklight;
		if (backLightLevel > 100
			|| !Configuration.getCfgBitState(Configuration.CFGBIT_BACKLIGHT_NOKIA))
		{
			backLightLevel = 100;
		}
		if (backLightLevel <= 1) {
			backLightLevel = 1;
		}
		if (backLightLevel == 26) {
			backLightLevel = 25;
		}
		setBackLightLevel(backLightLevel);
	}

	
	public static TravelMode getTravelMode() {
		return Legend.getTravelModes()[currentTravelModeNr];
	}
	
	public static int getTravelModeNr() {
		return currentTravelModeNr;
	}

	public static int getTravelMask() {
		return currentTravelMask;
	}
	
	public static void setTravelMode(int travelModeNr) {
		write(travelModeNr, RECORD_ID_ROUTE_TRAVEL_MODE);
		Configuration.currentTravelModeNr = travelModeNr;
		Configuration.currentTravelMask = 1 << travelModeNr;
	}

	
	
	public static int getContinueMapWhileRouteing() {
		return continueMapWhileRouteing;
	}

	public static void setContinueMapWhileRouteing(int continueMapWhileRouteing) {
		write(continueMapWhileRouteing, RECORD_ID_CONTINUE_MAP_WHILE_ROUTING);
		Configuration.continueMapWhileRouteing = continueMapWhileRouteing;
	}
	
	public static boolean getBtKeepAlive() {
		return btKeepAlive;
	}
	
	public static void setBtKeepAlive(boolean keepAlive) {
		write(keepAlive ? 1 : 0, RECORD_ID_BT_KEEPALIVE);
		Configuration.btKeepAlive = keepAlive;
	}
	
	public static boolean getBtAutoRecon() {
		return btAutoRecon;
	}
	
	public static void setBtAutoRecon(boolean autoRecon) {
		write(autoRecon ? 1 : 0, RECORD_ID_GPS_RECONNECT);
		Configuration.btAutoRecon = autoRecon;
	}

	public static void getStartupPos(Node pos) {
		pos.setLatLonRad(startupPos.radlat, startupPos.radlon);
	}

	public static void setStartupPos(Node pos) {
		//System.out.println("Save Map startup lat/lon: " + startupPos.radlat*MoreMath.FAC_RADTODEC + "/" + startupPos.radlon*MoreMath.FAC_RADTODEC);
		write(Float.toString(pos.radlat), RECORD_ID_STARTUP_RADLAT);
		write(Float.toString(pos.radlon), RECORD_ID_STARTUP_RADLON);
	}
	
	public static String getOsmUsername() {
		return osm_username;
	}

	public static void setOsmUsername(String name) {
		osm_username = name;
		write(name, RECORD_ID_OSM_USERNAME);
	}
	
	public static String getOsmPwd() {
		return osm_pwd;
	}

	public static void setOsmPwd(String pwd) {
		osm_pwd = pwd;
		write(pwd, RECORD_ID_OSM_PWD);
	}
	
	public static String getOsmUrl() {
		return osm_url;
	}
	
	public static void setOsmUrl(String url) {
		osm_url = url;
		write(url, RECORD_ID_OSM_URL);
	}

	public static String getOpencellidApikey() {
		return opencellid_apikey;
	}

	public static void setOpencellidApikey(String name) {
		opencellid_apikey = name;
		write(name, RECORD_ID_OPENCELLID_APIKEY);
	}

	public static void setProjTypeDefault(byte t) {
		ProjFactory.setProj(t);
		projTypeDefault = t;
		write(t, RECORD_ID_MAP_PROJECTION);
	}

	public static byte getProjDefault() {
		return projTypeDefault;
	}
	
	public static boolean getDeviceSupportsJSR135() {
		//#if polish.api.mmapi
		String jsr135Version = null;
		try {
			jsr135Version = System.getProperty("video.snapshot.encodings");
		} catch (RuntimeException re) {
			/**
			 * Some phones throw exceptions if trying to access properties that don't
			 * exist, so we have to catch these and just ignore them.
			 */
		} catch (Exception e) {
			/**
			 * See above
			 */
		}
		if (jsr135Version != null && jsr135Version.length() > 0) {
			return true;
		}
		//#endif
		return false;
	}
	public static boolean getDeviceSupportsJSR179() {
		//#if polish.api.locationapi
		String jsr179Version = null;
		try {
			jsr179Version = System.getProperty("microedition.location.version");
		} catch (RuntimeException re) {
			/**
			 * Some phones throw exceptions if trying to access properties that don't
			 * exist, so we have to catch these and just ignore them.
			 */
		} catch (Exception e) {
			/**
			 * See above
			 */
		}
		if (jsr179Version != null && jsr179Version.length() > 0) {
			return true;
		}
		//#endif
		return false;
	}
	
	public static boolean hasDeviceJSR120() {
		try {
			Class.forName("javax.wireless.messaging.MessageConnection" );
			return true;
		}
		catch (Exception e) {
			return false;
		}
	}
	
	public static String getPhoneModel() {
		try {
			return System.getProperty("microedition.platform");
		} catch (RuntimeException re) {
			/**
			 * Some phones throw exceptions if trying to access properties that don't
			 * exist, so we have to catch these and just ignore them.
			 */
		} catch (Exception e) {
			/**
			 * See above
			 */
		}
		return "";
	}
	
	public static long getDefaultDeviceBacklightMethodMask() {
		// a list of return codes for microedition.platform can be found at:
		// http://www.club-java.com/TastePhone/J2ME/MIDP_Benchmark.jsp

		//#if polish.api.nokia-ui || polish.api.min-siemapi
		String phoneModel = getPhoneModel();
		// determine default backlight method for devices from the wiki
		if (phoneModel.startsWith("Nokia") ||
			phoneModel.startsWith("SonyEricssonC") ||
			phoneModel.startsWith("SonyEricssonK550")
		) {
			return 1L << CFGBIT_BACKLIGHT_NOKIA;
		} else if (phoneModel.startsWith("SonyEricssonK750") ||
			phoneModel.startsWith("SonyEricssonW800")
		) {
			return 1L << CFGBIT_BACKLIGHT_NOKIAFLASH;
		} else if (phoneModel.endsWith("(NSG)") ||
		    phoneModel.startsWith("SIE")
		) {
			return 1 << CFGBIT_BACKLIGHT_SIEMENS;
        }
		//#endif
		return 0;
	}
	
	private static boolean getDefaultIconMenuBackCmdSupport() {
		String phoneModel = getPhoneModel();
		// Nokia phones don't handle the fire button correctly
		// when there is a command specified and they are in
		// fullscreen mode
		if (phoneModel.startsWith("Nokia")) {
			return false;
		} else {
			return true;
		}
	}
	
	public static String getValidFileName(String fileName) {
		return fileName.replace('\\', '_')
		               .replace('/', '_')
		               .replace('>', '_')
		               .replace('<', '_')
		               .replace(':', '_')
		               .replace('?', '_')
		               .replace('*', '_');
	}
	
	public static String getCompassDirection(int course) {
		return compassDirections[(int)(((course % 360 + 11.25f) / 22.5f)) ];
	}

	public static long getPhoneAllTimeMaxMemory() {
		return phoneAllTimeMaxMemory;
	}

	public static String getUtf8Encoding() {
		final String[] encodings  = { "UTF-8", "UTF8", "utf-8", "utf8", "" };
		
		if (utf8encodingstring != null) {
			return utf8encodingstring;
		}
		StringBuffer sb = new StringBuffer();
		sb.append("Testing String");
		for (int i = 0; i < encodings.length; i++) {
			try {
    			logger.info("Testing encoding " + encodings[i] + ": " + sb.toString().getBytes(encodings[i]));
    			utf8encodingstring = encodings[i];
    			return utf8encodingstring;
    		} catch (UnsupportedEncodingException e) {
    			continue;
    		}
		}
		return "";
	}
	
	public static int getWaypointSortMode() {
		return wayptSortMode;
	}
	
	public static void setWaypointSortMode(int mode) {
		if (mode >= WAYPT_SORT_MODE_NONE && mode <= WAYPT_SORT_MODE_DISTANCE) {
			wayptSortMode = mode;
			write(wayptSortMode, RECORD_ID_WAYPT_SORT_MODE);
		} else {
			throw new IllegalArgumentException("Waypoint sort mode out of range!");
		}
	}
	
	public static int getTrafficSignalCalcDelay() {
		return trafficSignalCalcDelay ;
	}

	public static void setTrafficSignalCalcDelay(int i) {
		trafficSignalCalcDelay = i;
		write(i, RECORD_ID_TRAFFIC_SIGNAL_CALC_DELAY);
	}

	public static void loadKeyShortcuts(intTree gameKeys, intTree singleKeys,
			intTree repeatableKeys, intTree doubleKeys, intTree longKeys,
			intTree specialKeys, Command [] cmds) {
		logger.info("Loading key shortcuts");
		if (!loadKeyShortcutsDB(gameKeys, singleKeys, repeatableKeys, doubleKeys,
				longKeys, specialKeys, cmds)) {
			loadDefaultKeyShortcuts(gameKeys, singleKeys, repeatableKeys, doubleKeys,
					longKeys, specialKeys, cmds);
		}
	}
	
	private static void loadDefaultKeyShortcuts(intTree gameKeys, intTree singleKeys,
			intTree repeatableKeys, intTree doubleKeys, intTree longKeys,
			intTree specialKeys, Command [] cmds) {
		int keyType = 0;
		//#debug info
		logger.info("Initialising default key shortcuts");
		try {
			InputStream is = getMapResource("/keyMap.txt");
			if (is == null) {
				throw new IOException("keyMap.txt not found");
			}
			InputStreamReader isr = new InputStreamReader(is, getUtf8Encoding());
			BufferedReader br = new BufferedReader(isr);
			String line;
			line = br.readLine();
			while (line != null) {
				line.trim();
				if (line.length() == 0) {
					line = br.readLine();
					continue;
				}
				if ((line.length() > 2) && line.charAt(0) == '[') {
					String sectionName = line.substring(1, line.length() - 1);
					if (sectionName.equalsIgnoreCase("repeatable")) {
						logger.debug("Starting repeatable section");
						keyType = 1;
					} else if (sectionName.equalsIgnoreCase("game")) {
						logger.debug("Starting game section");
						keyType = 4;
					} else  if (sectionName.equalsIgnoreCase("single")) {
						logger.debug("Starting single section");
						keyType = 0;
					} else  if (sectionName.equalsIgnoreCase("double")) {
						logger.debug("Starting double section");
						keyType = 2;
					} else  if (sectionName.equalsIgnoreCase("long")) {
						logger.debug("Starting long section");
						keyType = 3 ;
					} else  if (sectionName.equalsIgnoreCase("special")) {
						logger.debug("Starting special section");
						keyType = 5;
					} else {
						logger.info("unknown section: " + sectionName + " falling back to single");
						keyType = 0;
					}
				}
				Vector shortCut = StringTokenizer.getVector(line, "\t");
				if (shortCut.size() == 2) {
					try {
						int keyCode = Integer.parseInt(((String)shortCut.elementAt(0)));
						Command c = cmds[Integer.parseInt(((String)shortCut.elementAt(1)))];
						switch (keyType) {
						case 0: {
							logger.debug("Adding single key shortcut for key: " + keyCode + " and command " + c);
							singleKeys.put(keyCode, c);
							break;
						}
						case 1: {
							logger.debug("Adding repeatable key shortcut for key: " + keyCode + " and command " + c);
							repeatableKeys.put(keyCode, c);
							break;
						}
						case 2: {
							logger.debug("Adding double press key shortcut for key: " + keyCode + " and command " + c);
							doubleKeys.put(keyCode, c);
							break;
						}
						case 3: {
							logger.debug("Adding longpress key shortcut for key: " + keyCode + " and command " + c);
							longKeys.put(keyCode, c);
							break;
						}
						case 4: {
							logger.debug("Adding game action shortcut for key: " + keyCode + " and command " + c);
							gameKeys.put(keyCode, c);
							break;
						}
						case 5: {
							logger.debug("Adding special key shortcut for key: " + keyCode + " and command " + c);
							specialKeys.put(keyCode, c);
							break;
						}
						}
					} catch (NumberFormatException nfe) {
						logger.info("Invalid line in keyMap.txt: " + line + " Error: " +nfe.getMessage());
					} catch (ArrayIndexOutOfBoundsException aioobe) {
						logger.info("Invalid command number in keyMap.txt: " + line + " Error: " + aioobe.getMessage());
					}
				}
				line = br.readLine();
			};
		} catch (IOException ioe) {
			logger.exception("Could not load key shortcuts", ioe);
		}
		
	}
	
	private static boolean loadKeyShortcutsDB(intTree gameKeys, intTree singleKeys,
			intTree repeatableKeys, intTree doubleKeys, intTree longKeys,
			intTree specialKeys, Command [] cmds) {
		try {
			//#debug info
			logger.info("Attempting to load keyboard shortcuts from record store");
			RecordStore database = RecordStore.openRecordStore("Receiver", true);
			if (database == null) {
				//#debug info
				logger.info("No database loaded at the moment");
				return false;
			}
			byte [] data = readBinary(database, RECORD_ID_KEY_SHORTCUT);
			if (data == null) {
				logger.info("Record store did not contain key shortcut entry");
				database.closeRecordStore();
				return false;
			}
			ByteArrayInputStream bais = new ByteArrayInputStream(data);
			DataInputStream dis = new DataInputStream(bais);
			
			intTree keyTree;
			for (int k = 0; k < 6; k++) {
				keyTree = null;
				switch (k) {
				case 0 :
					keyTree = gameKeys;
					break;
				case 1:
					keyTree = singleKeys;
					break;
				case 2:
					keyTree = repeatableKeys;
					break;
				case 3:
					keyTree = doubleKeys;
					break;
				case 4:
					keyTree = longKeys;
					break;
				case 5:
					keyTree = specialKeys;
					break;
				}
				int treeLength = dis.readShort();
				for (int i = 0; i < treeLength; i++) {
					int keyCode = dis.readInt();
					int cmdCode = dis.readInt();
					keyTree.put(keyCode, cmds[cmdCode]);
				}

				dis.readUTF();
			}
		database.closeRecordStore();
		return true;
		} catch (Exception e) {
			logger.exception("Failed to load keyshortcuts", e);
			return false;
		}
	}
	
	public static void saveKeyShortcuts(intTree gameKeys, intTree singleKeys,
			intTree repeatableKeys, intTree doubleKeys, intTree longKeys,
			intTree specialKeys, Command [] cmds) {
		//#debug info
		logger.info("Saving key shortcuts");
		
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream(baos);
		
		try {
			intTree keyTree;
			for (int k = 0; k < 6; k++) {
				keyTree = null;
				switch (k) {
				case 0 :
					keyTree = gameKeys;
					break;
				case 1:
					keyTree = singleKeys;
					break;
				case 2:
					keyTree = repeatableKeys;
					break;
				case 3:
					keyTree = doubleKeys;
					break;
				case 4:
					keyTree = longKeys;
					break;
				case 5:
					keyTree = specialKeys;
					break;
				}
				dos.writeShort(keyTree.size());
				for (int i = 0; i < keyTree.size(); i++) {
					int keyCode = keyTree.getKeyIdx(i);
					int cmdCode = -1;
					Command c = (Command)keyTree.getValueIdx(i);
					for (int j = 0; j < cmds.length; j++) {
						if (cmds[j] == c) {
							cmdCode = j;
							break;
						}
					}
					if (cmdCode < 0) {
						logger.error("Could not associate cmd number. Failed to save key shortcuts");
						return;
					}
					dos.writeInt(keyCode);
					dos.writeInt(cmdCode);
				}

				dos.writeUTF("Next key Type");
			}
			dos.flush();
			baos.flush();
			writeBinary(baos.toByteArray(), RECORD_ID_KEY_SHORTCUT);
		
		} catch (IOException ioe) {
			logger.exception("Failed to save keyshortcuts", ioe);
		}
	}
	
}
