package de.ueller.gps.tools;
/*
 * GpsMid - Copyright (c) 2009 sk750 at users dot sourceforge dot net 
 * See Copying
 */

import javax.microedition.lcdui.Image;

public class ImageTools  {

	public static Image getGreyImage(Image original) {
        try {
			int[] rawInput = new int[original.getHeight() * original.getWidth()];
	        original.getRGB(rawInput, 0, original.getWidth(), 0, 0, original.getWidth(), original.getHeight());
	        
            for (int i = 0; i < rawInput.length; i++) {
                int rgb = rawInput[i];
                int red   = (rgb >> 16) & 0xff;
                int green = (rgb >>  8) & 0xff;
                int blue  =  rgb        & 0xff;
                int grey = (((red * 30) / 100) + ((green * 59) / 100) + ((blue * 11) / 100)) & 0xff;
                rawInput[i] = (grey << 16) | (grey << 8) | grey;
            } 
	        return Image.createRGBImage(rawInput, original.getWidth(), original.getWidth(), false);
        } catch (Exception e) {
        	return original;
        }
	}
	
	
	// based on Public Domain code (confirmed by E-Mail)
	// from http://willperone.net/Code/codescaling.php 
	public static Image scaleImage(Image original, int newWidth, int newHeight)
    {        
        try {
			int[] rawInput = new int[original.getHeight() * original.getWidth()];
	        original.getRGB(rawInput, 0, original.getWidth(), 0, 0, original.getWidth(), original.getHeight());	        	        
	        int[] rawOutput = new int[newWidth*newHeight];        
	
	        // YD compensates for the x loop by subtracting the width back out
	        int YD = (original.getHeight() / newHeight) * original.getWidth() - original.getWidth(); 
	        int YR = original.getHeight() % newHeight;
	        int XD = original.getWidth() / newWidth;
	        int XR = original.getWidth() % newWidth;        
	        int outOffset= 0;
	        int inOffset=  0;
	        
	        for (int y= newHeight, YE= 0; y > 0; y--) {            
	            for (int x= newWidth, XE= 0; x > 0; x--) {
	                rawOutput[outOffset++]= rawInput[inOffset];
	                inOffset+=XD;
	                XE+=XR;
	                if (XE >= newWidth) {
	                    XE-= newWidth;
	                    inOffset++;
	                }
	            }            
	            inOffset+= YD;
	            YE+= YR;
	            if (YE >= newHeight) {
	                YE -= newHeight;     
	                inOffset+=original.getWidth();
	            }
	        }               
	        return Image.createRGBImage(rawOutput, newWidth, newHeight, false);
        } catch (Exception e) {
        	return original;
        }
    }

	public static boolean isScaleMemAvailable(Image original, int newWidth, int newHeight) {
		return (Runtime.getRuntime().freeMemory() > 5*(original.getHeight() * original.getWidth() + newWidth * newHeight));
	}
}
