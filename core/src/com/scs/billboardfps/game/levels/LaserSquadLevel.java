package com.scs.billboardfps.game.levels;

import java.util.ArrayList;

import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.scs.basicecs.AbstractEntity;
import com.scs.billboardfps.game.Game;
import com.scs.billboardfps.game.World;
import com.scs.billboardfps.game.data.WorldSquare;
import com.scs.billboardfps.game.decals.DecalManager;
import com.scs.billboardfps.game.entities.EntityManager;
import com.scs.billboardfps.game.entities.Floor;
import com.scs.billboardfps.game.entities.GenericScenery;
import com.scs.billboardfps.game.entities.Wall;
import com.scs.billboardfps.game.entities.androids.PlayersLaserGun;
import com.scs.billboardfps.game.player.weapons.IPlayersWeapon;

public class LaserSquadLevel extends AbstractLevel {

	public LaserSquadLevel(EntityManager _entityManager, DecalManager _decalManager) {
		super(_entityManager, _decalManager);
	}


	@Override
	public void load(Game game) {
		entityManager.getEntities().clear();
		decalManager.clear();
		game.modelInstances = new ArrayList<ModelInstance>();

		//loadMapFromImage(game);
		loadTestMap(game);

		createWalls(game);
	}


	private void loadTestMap(Game game) {
		this.map_width = 5;
		this.map_height = 5;

		Game.world.world = new WorldSquare[map_width][map_height];

		for (int z=0 ; z<map_height ; z++) {
			for (int x=0 ; x<map_width ; x++) {
				int type = World.NOTHING;
				if (x == 0 || z == 0 || x >= map_width-1 || z >= map_height-1) {
					type = World.WALL;
				} else if (x == 1 && z == 1) {
					this.playerStartMapX = x;
					this.playerStartMapY = z;
				} else if (x == 3 && z == 3) {
					GenericScenery s = new GenericScenery("GasCan", "lasersquad/gas_cannisters.png", x, z);
					game.ecs.addEntity(s);
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
						game.ecs.addEntity(new Wall("lasersquad/moonbase_wall.png", x, y));
					}
				} catch (NullPointerException ex) {
					ex.printStackTrace();
				}
			}
		}

		game.ecs.addEntity(new Floor("lasersquad/moonbase_interior_floor.png", map_width, map_height));
		//game.ecs.addEntity(new Ceiling("colours/cyan.png", map_width, map_height));

	}


	@Override
	public void update(Game game, World world) {
	}


	@Override
	public void levelComplete() {

	}


	@Override
	public IPlayersWeapon getWeapon() {
		return new PlayersLaserGun();
	}


	@Override
	public void entityCollected(AbstractEntity collector, AbstractEntity collectable) {
		
	}

}
