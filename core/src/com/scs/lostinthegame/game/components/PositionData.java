package com.scs.lostinthegame.game.components;

import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.Vector3;
import com.scs.lostinthegame.game.Game;

public class PositionData {

	public Vector3 position;//, rotation;
	public Vector3 originalPosition = new Vector3();

	public PositionData() {
		this.position = new Vector3();
	}
	
	
	public PositionData(float x, float z) {
		this.position = new Vector3(x, 0, z);
	}
	
	
	public GridPoint2 getMapPos() {
		float x = (position.x/Game.UNIT) + 0.5f;
		float y = position.z/Game.UNIT + 0.5f;
		
		return new GridPoint2((int)x, (int)y) ;

	}
	
}
