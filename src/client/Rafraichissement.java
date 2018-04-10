package client;

import client.Map;
import javafx.animation.AnimationTimer;
import javafx.scene.canvas.GraphicsContext;

public class Rafraichissement extends AnimationTimer{

	private Map map;
	private GraphicsContext gc;

    Rafraichissement(Map map, GraphicsContext gc) {
        this.map = map;
        this.gc = gc;
    }

    @Override
    public void handle(long now) {
    	this.map.drawMap(this.gc);
    }
	
}
