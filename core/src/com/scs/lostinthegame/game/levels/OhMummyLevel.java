package com.scs.lostinthegame.game.levels;

import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.GridPoint2;
import com.scs.basicecs.AbstractEntity;
import com.scs.lostinthegame.game.Game;
import com.scs.lostinthegame.game.World;
import com.scs.lostinthegame.game.components.PositionData;
import com.scs.lostinthegame.game.data.WorldSquare;
import com.scs.lostinthegame.game.decals.DecalManager;
import com.scs.lostinthegame.game.entities.EntityManager;
import com.scs.lostinthegame.game.entities.Wall;
import com.scs.lostinthegame.game.entities.ohmummy.Pill;

public class OhMummyLevel extends AbstractLevel {

	private static final int RECT_SIZE_EXCLUDING_EDGES = 4;

	private boolean[][] pill_map;

	public OhMummyLevel(EntityManager _entityManager, DecalManager _decalManager) {
		super(_entityManager, _decalManager);
	}


	@Override
	public void load(Game game) {
		loadMap(game);

		createWalls(game);
	}


	private void loadMap(Game game) {
		int NUM_RECTS = 4;
		this.map_width = 3 + ((RECT_SIZE_EXCLUDING_EDGES+1)*NUM_RECTS);
		this.map_height = 3 + ((RECT_SIZE_EXCLUDING_EDGES+1)*NUM_RECTS);

		Game.world.world = new WorldSquare[map_width][map_height];
		pill_map = new boolean[map_width][map_height];

		this.playerStartMapX = 1;
		this.playerStartMapY = 1;

		for (int z=0 ; z<map_height ; z++) {
			for (int x=0 ; x<map_width ; x++) {
				int type = World.NOTHING;
				if (x == 0 || z == 0 || x >= map_width-1 || z >= map_height-1) {
					type = World.WALL;
				}

				Game.world.world[x][z] = new WorldSquare();
				Game.world.world[x][z].blocked = type == World.WALL;
			}
		}

		for (int z=2 ; z<map_height-2 ; z+=RECT_SIZE_EXCLUDING_EDGES+1) {
			for (int x=2 ; x<map_width-2 ; x+=RECT_SIZE_EXCLUDING_EDGES+1) {
				createRect(x, z);
			}
		}
	}


	private void createRect(int sx, int sz) {
		for (int z=sz ; z<sz+RECT_SIZE_EXCLUDING_EDGES ; z++) {
			for (int x=sx ; x<sx+RECT_SIZE_EXCLUDING_EDGES ; x++) {
				try {
					Game.world.world[x][z].blocked = true;
				} catch (ArrayIndexOutOfBoundsException ex) {
					ex.printStackTrace();
				}
			}
		}
	}


	private void createWalls(Game game) {
		for (int y = 0; y < map_height; y++) {
			for (int x = 0; x < map_width; x++) {
				try {
					boolean block = Game.world.world[x][y].blocked;
					if (block) {
						Wall wall = null;
						if (x == 0 || y == 0 || x >= map_width-1 || y >= map_height-1) {
							wall = new Wall("colours/white.png", x, y);
						} else {
							wall = new Wall("colours/magenta.png", x, y);
						}
						game.ecs.addEntity(wall);
						Game.world.world[x][y].wall = wall;
					}
				} catch (NullPointerException ex) {
					ex.printStackTrace();
				}
			}
		}
	}


	@Override
	public void update(Game game, World world) {
		// Drop pill?
		boolean checkForCircled = false;
		PositionData posData = (PositionData)Game.player.getComponent(PositionData.class);
		GridPoint2 map_pos = posData.getMapPos();
		if (this.pill_map[map_pos.x][map_pos.y] == false) {
			Pill ch = new Pill(map_pos.x, map_pos.y);
			game.ecs.addEntity(ch);
			this.pill_map[map_pos.x][map_pos.y] = true;
			//Settings.p("Adding pill to " + map_pos.x + "," + map_pos.y);
			checkForCircled = true;
		}


		if (checkForCircled) {
			for (int z=1 ; z<map_height-2 ; z+=RECT_SIZE_EXCLUDING_EDGES+1) {
				for (int x=1 ; x<map_width-2 ; x+=RECT_SIZE_EXCLUDING_EDGES+1) {
					checkRect(x, z); 
				}
			}
		}
	}


	private void checkRect(int sx, int sz) {
		boolean covered = true;
		for (int z=sz ; z<=sz+RECT_SIZE_EXCLUDING_EDGES+1 ; z++) {
			for (int x=sx ; x<=sx+RECT_SIZE_EXCLUDING_EDGES+1; x++) {
				if (x == sx || z == sz || x >= sx+RECT_SIZE_EXCLUDING_EDGES+1 || z >= sz+RECT_SIZE_EXCLUDING_EDGES+1) {
					if (this.pill_map[x][z] == false) {
						covered = false;
						break;
					}
				}
			}
		}
		if (covered) {
			for (int z=sz+1 ; z<=sz+RECT_SIZE_EXCLUDING_EDGES ; z++) {
				for (int x=sx+1 ; x<=sx+RECT_SIZE_EXCLUDING_EDGES; x++) {
					AbstractEntity wall = Game.world.world[x][z].wall;
					wall.remove();
					wall = new Wall("ohmummy/wall1.png", x, z);
					Game.ecs.addEntity(wall);
					Game.world.world[x][z].wall = wall;
				}
			}

			checkForCompletion();
		}
	}


	private void checkForCompletion() {
		/*if (this.completed) {
			return;
		}*/

		boolean c = true;
		for (int z=0 ; z<map_height ; z++) {
			for (int x=0 ; x<map_width ; x++) {
				if (Game.world.world[x][z].blocked == false) {
					if (this.pill_map[x][z] == false) {
						c = false;
						break;
					}
				}
			}
		}
		if (c) {
			Game.levelComplete = true;
		}
	}


	@Override
	public void entityCollected(AbstractEntity collector, AbstractEntity collectable) {

	}


	@Override
	public void renderUI(SpriteBatch batch, BitmapFont font) {
		//font.draw(batch, "Oh Mummy!", 10, 30);
	}


	@Override
	public String GetName() {
		return "OH MUMMY!";
	}

}
