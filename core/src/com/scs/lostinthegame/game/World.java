package com.scs.lostinthegame.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector3;
import com.scs.lostinthegame.game.data.WorldSquare;

public class World {

	public static final int NOTHING = 0;
	public static final int WALL = 1;
	public static final int BLOCKED = 2;

	public WorldSquare world[][];
	public TextureRegion detailTexture[];

	public World() {
		Texture detailTex = new Texture(Gdx.files.internal("detail.png"));

		int w = detailTex.getWidth()/16;
		int h = detailTex.getHeight()/16;

		detailTexture = new TextureRegion[w*h];

		for (int x = 0; x < w; x++) {
			for (int y = 0; y < h; y++) {
				detailTexture[x + y*w] = new TextureRegion(detailTex, x*16, y*16, 16, 16);
			}
		}
	}


	public WorldSquare getMapSquareAt(int x, int y) {
		if (x < 0 || y < 0) {
			//Settings.p("OOB!");
			return new WorldSquare(true); // todo - return static
		}

		try {
			return world[x][y];//.type;
		} catch (ArrayIndexOutOfBoundsException ex) {
			return new WorldSquare(true); // todo - return static
		}
	}


	public boolean rectangleFree(float center_x, float center_z, float width, float depth) {
		//Upper left
		float x = (center_x/Game.UNIT)-(width/2) + 0.5f;
		float y = center_z/Game.UNIT-depth/2 + 0.5f;

		if (getMapSquareAt((int)(x), (int)(y)).blocked) {
			return false;
		}

		//Down left
		x = center_x/Game.UNIT-width/2 + 0.5f;
		y = center_z/Game.UNIT+depth/2 + 0.5f;

		if (getMapSquareAt((int)(x), (int)(y)).blocked) {
			return false;
		}

		//Upper right
		x = center_x/Game.UNIT+width/2 + 0.5f;
		y = center_z/Game.UNIT-depth/2 + 0.5f;

		if (getMapSquareAt((int)(x), (int)(y)).blocked) {
			return false;
		}

		//Down right
		x = center_x/Game.UNIT+width/2 + 0.5f;
		y = center_z/Game.UNIT+depth/2 + 0.5f;

		if (getMapSquareAt((int)(x), (int)(y)).blocked) {
			return false;
		}

		return true;
	}


	public boolean canSee(Vector3 pos1, Vector3 pos2) {
		Vector3 tmp = new Vector3();
		tmp.set(pos1);

		Vector3 dir = new Vector3();
		dir.set(tmp).sub(pos2);
		dir.y = 0;
		dir.nor();

		while(tmp.dst2(pos2) > (Game.UNIT/2f) * (Game.UNIT/2f)){
			tmp.mulAdd(dir, -Game.UNIT/4f);

			if (getMapSquareAt(tmp.x, tmp.z).blocked) {
				return false;
			}
		}
		return true;
	}


	public WorldSquare getMapSquareAt(float x, float y) {
		return getMapSquareAt((int)(x/Game.UNIT+0.5f), (int)(y/Game.UNIT+0.5f));
	}


	public WorldSquare getMapSquareAt(Vector3 vec) {
		return getMapSquareAt((int)((vec.x/Game.UNIT)+0.5f), (int)((vec.z/Game.UNIT)+0.5f));
	}


}
