package client;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import javafx.scene.canvas.GraphicsContext;

public class Map {

	private Tileset tileset;
	private int [][] map;
	private int height;
	private int width;
	private ArrayList<Character> characters;
	
	Map(String url){
        try {
        	
            //Object obj = parser.parse(new FileReader(url));
			InputStream inputStream = getClass().getResourceAsStream("/map/"+url);
            JSONParser jsonParser = new JSONParser();
			JSONObject jsonObject = (JSONObject)jsonParser.parse(
					new InputStreamReader(inputStream, "UTF-8"));

            //JSONObject jsonObject =  (JSONObject) obj;

            String tilesetJson = (String) jsonObject.get("tileset");
            
            JSONArray mapJson = (JSONArray) jsonObject.get("terrain");
            
            int h = mapJson.toArray().length;
            int w = ((JSONArray) mapJson.toArray()[0]).toArray().length;
            
            this.characters = new ArrayList<>();
            
            this.tileset = new Tileset(tilesetJson);
            
            this.height = h;
            this.width = w;
            this.map = new int[h][w];
            
            JSONArray tmp;
            for(int i=0; i<h; i++){
            	tmp = (JSONArray) mapJson.toArray()[i];
            	for(int j=0; j<w; j++){
            		this.map[i][j] = ((Long) tmp.toArray()[j]).intValue();
            	}
            }
            
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }
	}

	public int getHeight(){
		return this.height;
	}
	
	public int getWidth(){
		return this.width;
	}
	
	public void addPlayer(Character character){
		this.characters.add(character);
	}

	public void removePlayer(Character character){
		this.characters.remove(character);
	}
	
	public void drawMap(GraphicsContext gc){
		for(int i=0; i < this.height; i++){
			int [] line = this.map[i];
			int y = i * 32;
			for(int j=0; j < this.width; j++){
				this.tileset.drawTile(line[j], gc, j * 32, y);
			}
		}
		
		Collections.sort(characters, new CharacterComparator());
		
		for(int i=0; i < characters.size(); i++){
			this.characters.get(i).drawCharacter(gc);
			if(this.characters.get(i).getHello()){
				this.characters.get(i).drawBubble(gc);
			}
		}
		
	}
	
}
