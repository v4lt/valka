package client;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;

public class Tileset {

	private Image image;
	private double width;
	
	Tileset(String url){
		InputStream is;
		try {
			is = getClass().getResourceAsStream(url);
			this.image = new Image(is);
			this.width = this.image.getWidth() / 32;
			is.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
	}
	
	public void drawTile(int num, GraphicsContext gc, int xDestination, int yDestination){
		double xSourceInTiles = num % this.width;
		if(xSourceInTiles == 0){
			xSourceInTiles = this.width;
		}
		double ySourceInTiles = Math.ceil(num / this.width);
		double xSource = (xSourceInTiles - 1) * 32;
		double ySource = (ySourceInTiles - 1) * 32;
		gc.drawImage(this.image, xSource, ySource, 32, 32, xDestination, yDestination, 32, 32);
	}
	
}
