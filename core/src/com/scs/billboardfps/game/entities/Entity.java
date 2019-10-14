package com.scs.billboardfps.game.entities;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.scs.basicecs.AbstractEntity;
import com.scs.billboardfps.game.Game;
import com.scs.billboardfps.game.World;
import com.scs.billboardfps.game.decals.DecalEntity;

public class Entity extends AbstractEntity {

	protected float sizeAsFracOfMapsquare = 0.75f; // For wall collisions

	public Vector3 position;
	public boolean remove = false;
	protected DecalEntity decalEntity;
	protected TextureRegion textureRegion[][];
	protected int world_x, world_y;
	
	public Entity(String _name) {
		super(_name);
	}

	
	public Entity(String name, int map_x, int map_y) {
		this(name);
		
		position = new Vector3(Game.UNIT*map_x, 0,Game.UNIT*map_y);
	}
	

	public Entity(String name, TextureRegion tex[][], int map_x, int map_y) {
		this(name, tex, map_x, map_y, 0, 0);
	}
	

	public Entity(String name, TextureRegion tex[][], int map_x, int map_y, int tx, int ty) {
		this(name, map_x, map_y);
		
		textureRegion = tex;

		decalEntity = new DecalEntity(tex[tx][ty]);

	}

	public void bindWorldTile(World wrld, int tx, int ty) {
		world_x = tx;
		world_y = ty;
		wrld.world[tx][ty].type = World.BLOCKED;
	}


	public Vector3 getPosition() {
		return position;
	}

	public void update(World world) {
	}

	
	// todo - remove this method and use vector3 for all
	protected boolean tryMove(World world, Vector2 moveVec, boolean doFine) {
		return this.tryMove(world, new Vector3(moveVec.x, 0, moveVec.y), doFine);
	}


	/**
	 * Returns false if entity fails to move on any axis.
	 */
	protected boolean tryMove(World world, Vector3 moveVec, boolean doFine) {
		if (moveVec.len() <= 0) {
			return true;
		}
		
		boolean resultX = false;
		if(world.rectangleFree(position.x+moveVec.x, position.z, sizeAsFracOfMapsquare, sizeAsFracOfMapsquare)) {
			position.x += moveVec.x;
			resultX = true;
		} else if (doFine) {
			for (int i = 0; i < 10; i++) {
				if (world.rectangleFree(position.x+moveVec.x/10f, position.z, sizeAsFracOfMapsquare, sizeAsFracOfMapsquare)) {
					position.x += moveVec.x/10f;
					resultX = true;
				} else {
					break;
				}
			}
		}

		boolean resultZ = false;
		if(world.rectangleFree(position.x, position.z+moveVec.z, sizeAsFracOfMapsquare, sizeAsFracOfMapsquare)) {
			position.z += moveVec.z;
			resultZ = true;
		} else if (doFine){
			for (int i = 0; i < 10; i++) {
				if(world.rectangleFree(position.x, position.z+moveVec.z/10f, sizeAsFracOfMapsquare, sizeAsFracOfMapsquare)) {
					position.z += moveVec.z/10f;
					resultZ = true;
				} else {
					break;
				}
			}
		}
		
		if (moveVec.y != 0) {
			position.y += moveVec.y;
		}
		
		return resultX && resultZ;
	}

	
	public String toString() {
		return name;
	}
}