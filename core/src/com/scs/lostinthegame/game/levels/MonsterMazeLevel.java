package com.scs.lostinthegame.game.levels;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.scs.basicecs.AbstractEntity;
import com.scs.lostinthegame.Settings;
import com.scs.lostinthegame.game.Game;
import com.scs.lostinthegame.game.World;
import com.scs.lostinthegame.game.components.HasAI;
import com.scs.lostinthegame.game.components.PositionData;
import com.scs.lostinthegame.game.data.WorldSquare;
import com.scs.lostinthegame.game.decals.DecalManager;
import com.scs.lostinthegame.game.entities.EntityManager;
import com.scs.lostinthegame.game.entities.Wall;
import com.scs.lostinthegame.game.entities.monstermaze.MonsterMazeExit;
import com.scs.lostinthegame.game.entities.monstermaze.TRex;

public class MonsterMazeLevel extends AbstractLevel {

	private TRex trex;
	private String trex_msg = "REX LIES IN WAIT";
	private boolean has_seen = false;
	private float next_check = 0;
	
	public MonsterMazeLevel(EntityManager _entityManager, DecalManager _decalManager) {
		super(_entityManager, _decalManager);
	}


	@Override
	public void load(Game game) {
		//loadMapFromImage(game);
		loadTestMap(game);
	}


	public void setBackgroundColour() {
		Gdx.gl.glClearColor(1, 1, 1, 1);
	}


	private void loadTestMap(Game game) {
		this.map_width = 5;
		this.map_height = 5;

		Game.world.world = new WorldSquare[map_width][map_height];

		this.playerStartMapX = 1;
		this.playerStartMapY = 1;

		for (int z=0 ; z<map_height ; z++) {
			for (int x=0 ; x<map_width ; x++) {
				int type = World.NOTHING;
				Game.world.world[x][z] = new WorldSquare();
				if (x == 0 || z == 0 || x >= map_width-1 || z >= map_height-1) {
					type = World.WALL;
					Wall wall = new Wall("monstermaze/wall.png", x, z);
					game.ecs.addEntity(wall);
				} else if (x == 2 && z == 2) {
					type = World.WALL;
					Wall wall = new Wall("monstermaze/wall.png", x, z);
					game.ecs.addEntity(wall);
				} else if (x == 3 && z == 1) {
					trex = new TRex(x, z);
					game.ecs.addEntity(trex);
				} else if (x == 3 && z == 3) {
					MonsterMazeExit exit = new MonsterMazeExit(x, z);
					game.ecs.addEntity(exit);
				}

				Game.world.world[x][z].blocked = type == World.WALL;
			}
		}
	}


	@Override
	public void update(Game game, World world) {
		if (next_check > 0) {
			next_check -= Gdx.graphics.getDeltaTime();
			return;
		}
		next_check = 3;
		
		// todo - ", or RUN HE IS BEHIND YOU"
		PositionData trexPos = (PositionData)trex.getComponent(PositionData.class);
		PositionData playerPos = (PositionData)Game.player.getComponent(PositionData.class);
		float dist = trexPos.position.dst(playerPos.position);
		if (dist < Game.UNIT*2) {
			trex_msg = "RUN HE IS BESIDE YOU";
			return;
		}
		HasAI ai = (HasAI)trex.getComponent(HasAI.class);
		if (ai.can_see_player) {
			trex_msg = "REX HAS SEEN YOU";
			has_seen = true;
			return;
		}

		if (dist < Game.UNIT*5) {
			trex_msg = "FOOTSTEPS APPROACHING";
			return;
		}

		if (has_seen) {
			trex_msg = "HE IS HUNTING FOR YOU";
			return;
		}
	}


	@Override
	public void entityCollected(AbstractEntity collector, AbstractEntity collectable) {
	}


	@Override
	public void renderUI(SpriteBatch batch, BitmapFont font) {		
		font.draw(batch, trex_msg, 10, Settings.WINDOW_HEIGHT_PIXELS-40);
	}


	@Override
	public String GetName() {
		return "3D MONSTER MAZE";
	}

}
