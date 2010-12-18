/*
 * GpsMid - Copyright (c) 2008 Kai Krueger apmonkey at users dot sourceforge dot net 
 * See COPYING
 */

package de.ueller.midlet.gps;

public interface UploadListener {

	public void startProgress(String title);
	public void setProgress(String message);
	public void updateProgress(String message);
	public void updateProgressValue(int increment);
	public void completedUpload(boolean success, String message);
	public void uploadAborted();

}