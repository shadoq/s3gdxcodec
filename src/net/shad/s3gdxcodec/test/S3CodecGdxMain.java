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
package net.shad.s3gdxcodec.test;

import net.shad.s3gdxcodec.PixmapEncoder;
import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.GL10;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jcodec.common.model.Picture;

/**
 * Main comperss video test
 * Create a exit from mandelbrot fractal
 * @author Jaroslaw Czub (http://shad.net.pl)
 */
public class S3CodecGdxMain implements ApplicationListener
{

	private Pixmap pixmap;
	private PixmapEncoder encoder;
	private FileHandle fileHandle;
	private Picture pixmapToPicture;

	/**
	 * Create video file
	 */
	@Override
	public void create(){

		pixmap=new Pixmap(512, 512, Pixmap.Format.RGBA8888);
		encoder=new PixmapEncoder();
		try {
			fileHandle=Gdx.files.internal("test.mp4");
			Gdx.app.log("create ", "Save file to: " + fileHandle.path());
			encoder.initalize(fileHandle.file(), pixmap.getWidth(), pixmap.getHeight());
			for (int i=0; i < 1024; i++){
				fractalMandelbrot(pixmap, 0.0, 0.0, 0.2 + (0.01 * i), 0.2 + (0.01 * i), 192);
				pixmapToPicture=encoder.pixmapToPicture(pixmap);
				encoder.encodeFrame(pixmapToPicture);
			}
			encoder.close();

		} catch (IOException ex) {
			Logger.getLogger(S3CodecGdxMain.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	@Override
	public void render(){
		Gdx.gl.glClearColor(0.2f, 0.2f, 0.2f, 1);
		Gdx.gl.glClear(GL10.GL_COLOR_BUFFER_BIT);
	}

	@Override
	public void resize(int width, int height){
	}

	@Override
	public void pause(){
	}

	@Override
	public void resume(){
	}

	@Override
	public void dispose(){
	}

	/**
	 * Generate Mandelbrot fractal
	 * @param pixmap
	 * @param xCenter
	 * @param yCenter
	 * @param xSize
	 * @param ySize
	 * @param maxIterations 
	 */
	public static void fractalMandelbrot(Pixmap pixmap, double xCenter, double yCenter, double xSize, double ySize, int maxIterations){

		int width=pixmap.getWidth();
		int height=pixmap.getHeight();
		double xStart=xCenter - xSize;
		double yStart=yCenter - ySize;

		double xStep=(xSize * 2f) / width;
		double yStep=(ySize * 2f) / height;

		double px, py, zx, zy, zx2, zy2;
		float grey;
		int value;

		for (int y=0; y < width; y++){
			for (int x=0; x < height; x++){

				px=xStart + x * xStep;
				py=yStart + y * yStep;

				value=0;
				zx=0.0;
				zy=0.0;
				zx2=0.0;
				zy2=0.0;

				while (value < maxIterations && zx2 + zy2 < 4.0){
					zy=2.0 * zx * zy + py;
					zx=zx2 - zy2 + px;

					zx2=zx * zx;
					zy2=zy * zy;
					value++;
				}
				grey=((maxIterations - value) * (255 / maxIterations));
				pixmap.drawPixel(x, y, ((int) grey << 24) | ((int) grey << 16) | ((int) grey << 8) | 255);
			}
		}
	}
}
