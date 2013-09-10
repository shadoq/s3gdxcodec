/**
 * *****************************************************************************
 * Copyright 2013 See AUTHORS file.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 * ****************************************************************************
 */
package net.shad.s3gdxcodec;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import org.jcodec.codecs.h264.H264Encoder;
import org.jcodec.codecs.h264.H264Utils;
import org.jcodec.common.NIOUtils;
import org.jcodec.common.SeekableByteChannel;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Picture;
import org.jcodec.containers.mp4.Brand;
import org.jcodec.containers.mp4.MP4Packet;
import org.jcodec.containers.mp4.TrackType;
import org.jcodec.containers.mp4.muxer.FramesMP4MuxerTrack;
import org.jcodec.containers.mp4.muxer.MP4Muxer;
import org.jcodec.scale.RgbToYuv420;

/**
 * Simple class to encode libGdx pixmap to mpeg4 video file. Used jcodec.org
 * pure java codec library and libGdx pixamp. Based on jcodec example
 *
 * @author Jaroslaw Czub (http://shad.net.pl)
 */
public class PixmapEncoder
{

	// Enable loging
	public static boolean log=false;
	private SeekableByteChannel ch;
	private Picture encodePicture=null;
	private Picture rgbPicture;
	private RgbToYuv420 transform;
	private H264Encoder encoder;
	private ArrayList<ByteBuffer> spsList;
	private ArrayList<ByteBuffer> ppsList;
	private ByteBuffer outBuffer;
	private int frameNo;
	private MP4Muxer muxer;
	private FramesMP4MuxerTrack outTrack;
	private int frameRate=25;
	private double deltaTime=1.0d / frameRate;
	private int width=1900;
	private int height=1080;

	/**
	 * Return video framerate
	 *
	 * @return
	 */
	public int getFrameRate(){
		return frameRate;
	}

	/**
	 * Set video framerate
	 *
	 * @param frameRate
	 */
	public void setFrameRate(int frameRate){
		this.frameRate=frameRate;
		deltaTime=1.0d / frameRate;
	}

	/**
	 * Return a delaTime, used to calculate different time between frames
	 *
	 * @return
	 */
	public double getDeltaTime(){
		return deltaTime;
	}

	/**
	 *
	 * @return
	 */
	public int getWidth(){
		return width;
	}

	/**
	 *
	 * @return
	 */
	public int getHeight(){
		return height;
	}

	/**
	 * Initalize a compress class
	 *
	 * @param fileHandle - resource to mpeg4 file
	 * @param width - video width
	 * @param height - video height
	 * @throws IOException
	 */
	public void initalize(File fileHandle, int width, int height) throws IOException{

		this.width=width;
		this.height=height;

		ch=NIOUtils.writableFileChannel(fileHandle);
		muxer=new MP4Muxer(ch, Brand.MP4);
		outTrack=muxer.addTrackForCompressed(TrackType.VIDEO, frameRate);
		outBuffer=ByteBuffer.allocate(width * height * 6);
		transform=new RgbToYuv420(0, 0);
		encoder=new H264Encoder();
		spsList=new ArrayList<ByteBuffer>();
		ppsList=new ArrayList<ByteBuffer>();
		frameNo=0;
	}

	/**
	 * Convert a libGdx pixmap to jcodec Picture and convert color space model
	 * RGB to YUV.
	 * 
	 * @param pixmap to convert
	 * @return picture
	 */
	public Picture pixmapToPicture(Pixmap pixmap){

		if (encodePicture == null){
			encodePicture=Picture.create(width, height, ColorSpace.YUV420);
			rgbPicture=Picture.create(width, height, ColorSpace.RGB);
			if (log){
				Gdx.app.log("PixmapEncoder::pixmapToPicture()", "Create encode picture w:" + encodePicture.getWidth() + " h:" + encodePicture.getHeight());
			}
		}

		int dstOff=0;
		int[] rgbData=rgbPicture.getPlaneData(0);

		switch (pixmap.getFormat()){
			case RGBA8888:
				for (int i=0; i < height; i++){
					for (int j=0; j < width; j++){

						int rgb=pixmap.getPixel(j, i);
						int r=(rgb & 0xff000000) >>> 24;
						int g=(rgb & 0x00ff0000) >>> 16;
						int b=(rgb & 0x0000ff00) >>> 8;

						rgbData[dstOff]=r;
						rgbData[dstOff + 1]=g;
						rgbData[dstOff + 2]=b;
						dstOff+=3;
					}
				}
				break;
			case RGB888:
				for (int i=0; i < height; i++){
					for (int j=0; j < width; j++){

						int rgb=pixmap.getPixel(j, i);
						int r=(rgb & 0x00ff0000) >>> 16;
						int g=(rgb & 0x0000ff00) >>> 8;
						int b=(rgb & 0x000000ff);

						rgbData[dstOff]=r;
						rgbData[dstOff + 1]=g;
						rgbData[dstOff + 2]=b;
						dstOff+=3;
					}
				}
				break;
			default:
				throw new IllegalArgumentException("Not support Pixmap Format ...");
		}

		transform.transform(rgbPicture, encodePicture);
		return encodePicture;
	}

	/**
	 * Add picture to video stream
	 * @param picture
	 * @throws IOException
	 */
	public void encodeFrame(Picture picture) throws IOException{
		if (log){
			Gdx.app.log("PixmapEncoder::encodeFrame()", "Compress frame " + frameNo + " w:" + encodePicture.getWidth() + " h:" + encodePicture.getHeight());
		}
		outBuffer.clear();
		ByteBuffer result=encoder.encodeFrame(outBuffer, picture);
		spsList.clear();
		ppsList.clear();
		H264Utils.encodeMOVPacket(result, spsList, ppsList);
		outTrack.addFrame(new MP4Packet(result, frameNo, frameRate, 1, frameNo, true, null, frameNo, 0));
		frameNo++;
	}

	/**
	 * Close the file and clear resource
	 * @throws IOException
	 */
	public void close() throws IOException{
		outTrack.addSampleEntry(H264Utils.createMOVSampleEntry(spsList, ppsList));
		muxer.writeHeader();
		NIOUtils.closeQuietly(ch);

		outBuffer.clear();
		spsList.clear();
		ppsList.clear();
		
		outBuffer=null;
		transform=null;
		muxer=null;
		encoder=null;
		spsList=null;
		ppsList=null;		
	}
}
