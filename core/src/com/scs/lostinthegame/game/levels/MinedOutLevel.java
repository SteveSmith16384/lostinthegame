package com.scs.lostinthegame.game.levels;

import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.scs.basicecs.AbstractEntity;
import com.scs.lostinthegame.game.Game;
import com.scs.lostinthegame.game.World;
import com.scs.lostinthegame.game.data.WorldSquare;
import com.scs.lostinthegame.game.decals.DecalManager;
import com.scs.lostinthegame.game.entities.EntityManager;
import com.scs.lostinthegame.game.entities.Floor;
import com.scs.lostinthegame.game.entities.Wall;
import com.scs.lostinthegame.game.entities.minedout.Damsel;
import com.scs.lostinthegame.game.entities.minedout.Mine;
import com.scs.lostinthegame.game.entities.minedout.MinedOutExit;
import com.scs.lostinthegame.game.systems.CountMinesSystem;

public class MinedOutLevel extends AbstractLevel {

	private int num_damsels = 0;
	private CountMinesSystem countMinesSystem;
	
	public MinedOutLevel(EntityManager _entityManager, DecalManager _decalManager) {
		super(_entityManager, _decalManager);
	}

	@Override
	public void load(Game game) {
		loadTestMap(game);

		createWalls(game);
	
		this.countMinesSystem = new CountMinesSystem(Game.ecs);
		Game.ecs.addSystem(this.countMinesSystem);
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
				if (x == 0 || z == 0 || x >= map_width-1 || z >= map_height-1) {
					type = World.WALL;
				} else if (x == 3 && z == 3) {
					Mine m = new Mine(x, z);
					game.ecs.addEntity(m);
				} else if (x == 4 && z == 4) {
					Damsel d = new Damsel(x, z);
					game.ecs.addEntity(d);
					num_damsels++;
				}

				Game.world.world[x][z] = new WorldSquare();
				Game.world.world[x][z].type = type;
			}
		}
	}


	private void createWalls(Game game) {
		for (int y = 0; y < map_height; y++) {
			for (int x = 0; x < map_width; x++) {
				try {
					int block = Game.world.world[x][y].type;
					if (block == World.WALL) {
						game.ecs.addEntity(new Wall("minedout/wall.png", x, y));
					}
				} catch (NullPointerException ex) {
					ex.printStackTrace();
				}
			}
		}

		game.ecs.addEntity(new Floor("colours/cyan.png", map_width, map_height));

	}


	@Override
	public void entityCollected(AbstractEntity collector, AbstractEntity collectable) {
		if (collectable instanceof Damsel) {
			this.num_damsels--;
			if (this.num_damsels <= 0) {
				Game.world.world[map_width][(int)map_height/2].wall.remove();
				MinedOutExit exit = new MinedOutExit(map_width, (int)map_height/2);
				Game.ecs.addEntity(exit);
			}
		}
	}


	@Override
	public void renderUI(SpriteBatch batch, BitmapFont font) {
		font.draw(batch, "Adjacent Mines: " + this.countMinesSystem, 10, 30);
	}
	
	
	@Override
	public void update(Game game, World world) {
	}


	@Override
	public String GetName() {
		return "MINEDOUT";
	}

}
