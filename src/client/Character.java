package client;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.concurrent.ConcurrentHashMap;

import client.Map;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import client.Point;

public class Character implements Serializable{
	
	private static final long serialVersionUID = 1L;
	
	public final static int DUREE_ANIMATION = 4;
	public final static int DUREE_DEPLACEMENT = 15;
	
	private int x;
	private int y;
	private Direction direction;
	private Image image;
	private double width;
	private double height;
	private int animationState;
	private Image bubble;
	private boolean sayHello;
	
	public Character(String url, int x, int y, Direction direction){
		
		InputStream is;
		try {
			System.out.println(url);
			is = getClass().getResourceAsStream(url);
			this.image = new Image(is);
			is.close();

			is = getClass().getResourceAsStream("/frames/bulle.png");
			this.bubble = new Image(is);
			is.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		
		this.x = x;
		this.y = y;
		this.direction = direction;

		this.width = this.image.getWidth() / 4;
		this.height = this.image.getHeight() / 4;
		
		this.sayHello = false;
		this.animationState = -1;
	}
	
	public void drawCharacter(GraphicsContext gc){
		int frame = 0;
		double paddingX = 0;
		double paddingY = 0;
		
		if(this.animationState >= DUREE_DEPLACEMENT){
			this.animationState = -1;
		}else if(this.animationState >= 0){
			frame = (int) Math.floor((double)(this.animationState/DUREE_ANIMATION));
			if(frame > 3){
				frame %= 4;
			}
			
			float pixelsToExplore =  (float) (32.0 - (32.0 * ((float)this.animationState / (float)DUREE_DEPLACEMENT)));
			
			switch(this.direction){
				case UP :
					paddingY = pixelsToExplore;
					break;
				case DOWN:
					paddingY = -pixelsToExplore;
					break;
				case LEFT:
					paddingX = pixelsToExplore;
					break;
				case RIGHT:
					paddingX = -pixelsToExplore;
					break;
			}
			
			this.animationState++;
		}
		
		gc.drawImage(
			this.image, 
			this.width * frame,
			this.direction.ordinal() * this.height, 
			this.width, 
			this.height, 
			(this.x * 32) - (this.width / 2) + 16 + paddingX, 
			(this.y * 32) - this.height + 24 + paddingY,
			this.width,
			this.height
		);
		
	}
	
	public void drawBubble(GraphicsContext gc){
		double widthBulle  = this.bubble.getWidth();
		double heightBulle = this.bubble.getHeight();
		
		double xBubble = this.x * 32 - widthBulle + (this.width / 2);
		double yBubble = this.y * 32 - heightBulle; 
		
		gc.drawImage(
			this.bubble,
			xBubble, 
			yBubble
		);	
		
		String message = "coucou";
		
		int messageSize = message.length() * 4;
		
		gc.fillText(
			"coucou",
			(xBubble + (xBubble + widthBulle)) / 2 - messageSize,
			(yBubble + (yBubble + heightBulle)) / 2 - 5
        );

	}
	
	public Point getAdjacentCoordiante(Direction direction){
		
		Point p = new Point(this.x, this.y);
		
		switch(this.direction){
			case UP :
				p.y--;
				break;
			case DOWN:
				p.y++;
				break;
			case LEFT:
				p.x--;
				break;
			case RIGHT:
				p.x++;
				break;
		}
		
		return p;
	}
	
	public boolean move(Direction direction, Map map, ConcurrentHashMap<String, Character> listCharacters){
		if(this.animationState >= 0){
			return false;
		}

		this.direction = direction;

		Point nextCase = this.getAdjacentCoordiante(direction);

		if(nextCase.x < 0 || nextCase.y < 0 || nextCase.x >= map.getWidth() || nextCase.y >= map.getHeight()){
			return false;
		}

		for (ConcurrentHashMap.Entry<String, Character> e : listCharacters.entrySet()){
    	    Point p = e.getValue().getPos();
    	    if(nextCase.x == p.x && nextCase.y == p.y){
    	    	return false;
    	    }
    	}

		this.animationState = 1;

		this.x = nextCase.x;
		this.y = nextCase.y;

		return true;
	}

	public boolean moveWithPoint(Point newPosition, Map map, ConcurrentHashMap<String, Character> listCharacters){
		if(this.animationState >= 0){
			return false;
		}

		Point nextCase = newPosition;
		if(nextCase.getX()>this.x){
		    this.direction = Direction.RIGHT;
        }else if(nextCase.getX()<this.x){
		    this.direction = Direction.LEFT;
        }else if(nextCase.getY()<this.y){
		    this.direction = Direction.UP;
        }else if(nextCase.getY()>this.y){
		    this.direction = Direction.DOWN;
        }


		if(nextCase.x < 0 || nextCase.y < 0 || nextCase.x >= map.getWidth() || nextCase.y >= map.getHeight()){
			return false;
		}

		for (ConcurrentHashMap.Entry<String, Character> e : listCharacters.entrySet()){
			Point p = e.getValue().getPos();
			if(nextCase.x == p.x && nextCase.y == p.y){
				return false;
			}
		}

		this.animationState = 1;

		this.x = nextCase.x;
		this.y = nextCase.y;

		return true;
	}

	@Override
	public String toString() {
		return "Character [x=" + x + ", y=" + y + ", direction=" + direction + ", width=" + width + ", height=" + height
				+ "]";
	}

	public int getX() {
		return x;
	}

	public int getY() {
		return y;
	}
	
	public Point getPos(){
		return new Point(this.x, this.y);
	}

	public String positionSerializable() {
		return x + " " + y;
	}
	
	public boolean getHello(){
		return this.sayHello;
	}
	
	public void setHello(boolean say){
		this.sayHello = say;
	}
		
}
