package com.scs.lostinthegame.game.systems;

import java.util.Iterator;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Vector3;
import com.scs.basicecs.AbstractEntity;
import com.scs.basicecs.AbstractSystem;
import com.scs.basicecs.BasicECS;
import com.scs.lostinthegame.Settings;
import com.scs.lostinthegame.game.Game;
import com.scs.lostinthegame.game.World;
import com.scs.lostinthegame.game.components.AutoMove;
import com.scs.lostinthegame.game.components.HarmsNasties;
import com.scs.lostinthegame.game.components.HarmsPlayer;
import com.scs.lostinthegame.game.components.IsDamagableNasty;
import com.scs.lostinthegame.game.components.MovementData;
import com.scs.lostinthegame.game.components.PositionData;
import com.scs.lostinthegame.game.player.Player;

public class MovementSystem extends AbstractSystem {

	private Player player;

	public MovementSystem(BasicECS ecs, Player _player) {
		super(ecs);

		player = _player;
	}


	@Override
	public Class<?> getComponentClass() {
		return MovementData.class;
	}


	@Override
	public void processEntity(AbstractEntity entity) {
		if (entity.isMarkedForRemoval()) {
			return;
		}

		MovementData movementData = (MovementData)entity.getComponent(MovementData.class);
		movementData.hitWall = false;

		AutoMove auto = (AutoMove)entity.getComponent(AutoMove.class);
		if (auto != null) {
			movementData.offset = auto.dir.cpy().scl(Gdx.graphics.getDeltaTime());
		}
		if (movementData.offset.x != 0 || movementData.offset.y != 0 || movementData.offset.z != 0) {
			boolean res = this.tryMove(entity, Game.world, movementData.offset, movementData.sizeAsFracOfMapsquare);
			if (!res) {
				movementData.hitWall = true;
				if (movementData.removeIfHitWall) {
					entity.remove();
					Settings.p(entity + " removed");
				}
			}
		}
	}


	/**
	 * Returns false if entity fails to move on any axis.
	 */
	private boolean tryMove(AbstractEntity entity, World world, Vector3 moveVec, float sizeAsFracOfMapsquare) {
		if (moveVec.len() <= 0) {
			return true;
		}

		PositionData pos = (PositionData)entity.getComponent(PositionData.class);
		pos.originalPosition.set(pos.position);
		Vector3 position = pos.position;

		boolean resultX = false;
		if (world.rectangleFree(position.x+moveVec.x, position.z, sizeAsFracOfMapsquare, sizeAsFracOfMapsquare)) {
			if (this.isMoverBlocked(entity, position) == false) {
				position.x += moveVec.x;
				resultX = true;
			}
		}

		boolean resultZ = false;
		if (world.rectangleFree(position.x, position.z+moveVec.z, sizeAsFracOfMapsquare, sizeAsFracOfMapsquare)) {
			if (this.isMoverBlocked(entity, position) == false) {
				position.z += moveVec.z;
				resultZ = true;
			}
		}

		if (moveVec.y != 0) {
			position.y += moveVec.y;
		}

		if (entity != player) {
			if (checkForPlayerCollision(entity, position) || checkForNastiesCollision(entity, position)) {
				pos.position.set(pos.originalPosition); // Move back
				return false;
			}
		}

		return resultX && resultZ;
	}


	private boolean checkForPlayerCollision(AbstractEntity entity, Vector3 pos) {
		HarmsPlayer hp = (HarmsPlayer)entity.getComponent(HarmsPlayer.class);
		if (hp != null) {
			float dist = pos.dst(player.getPosition());
			if (dist < Game.UNIT * .5f) {
				player.damaged(hp.damageCaused, new Vector3()); // todo - direction
				entity.remove(); // Prevent further collisions
				return true;
			}
		}
		return false;
	}


	private boolean checkForNastiesCollision(AbstractEntity bullet, Vector3 pos) {
		HarmsNasties hp = (HarmsNasties)bullet.getComponent(HarmsNasties.class);
		if (hp != null) {
			Iterator<AbstractEntity> it = ecs.getIterator();
			while (it.hasNext()) {
				AbstractEntity nasty = it.next();
				IsDamagableNasty dam = (IsDamagableNasty)nasty.getComponent(IsDamagableNasty.class);
				if (dam != null) {
					PositionData posData = (PositionData)nasty.getComponent(PositionData.class);
					float dist = pos.dst(posData.position);
					if (dist < Game.UNIT*.5f) {
						if (hp.remove_on_collision) {
							bullet.remove();
						}
						return true;
					}
				}
			}
		}
		return false;
	}


	private boolean isMoverBlocked(AbstractEntity mover, Vector3 moverPos) {
		Iterator<AbstractEntity> it = ecs.getIterator();
		while (it.hasNext()) {
			AbstractEntity blocker = it.next();
			if (blocker != mover) {
				MovementData md = (MovementData)blocker.getComponent(MovementData.class);
				if (md.blocksMovement) {
					PositionData posData = (PositionData)blocker.getComponent(PositionData.class);
					float dist = moverPos.dst(posData.position);
					if (dist < Game.UNIT*.5f) {
						return true;
					}
				}
			}
		}
		return false;
	}

}
