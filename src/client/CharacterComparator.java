package client;

import java.util.Comparator;

public class CharacterComparator implements Comparator<Character>{

	@Override
	public int compare(Character o1, Character o2) {
		// TODO Auto-generated method stub
		if(o1.getY() < o2.getY()){
			return -1;
	    }else if(o1.getY() > o2.getY()){
	    	return 1;
	    }else{
	    	return 0;
	    }
	}
	
}
