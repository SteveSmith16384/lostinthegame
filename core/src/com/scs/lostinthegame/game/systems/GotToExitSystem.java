package com.scs.lostinthegame.game.systems;

import com.scs.basicecs.AbstractEntity;
import com.scs.basicecs.AbstractSystem;
import com.scs.basicecs.BasicECS;
import com.scs.lostinthegame.Settings;
import com.scs.lostinthegame.game.Game;
import com.scs.lostinthegame.game.components.CompletesLevelData;
import com.scs.lostinthegame.game.components.PositionData;

public class GotToExitSystem extends AbstractSystem {

	public GotToExitSystem(BasicECS ecs) {
		super(ecs);
	}


	@Override
	public Class<?> getComponentClass() {
		return CompletesLevelData.class;
	}


	@Override
	public void processEntity(AbstractEntity entity) {
		PositionData ourPos = (PositionData)entity.getComponent(PositionData.class);
		PositionData playerPos = (PositionData)Game.player.getComponent(PositionData.class);
		float dist = ourPos.getMapPos().dst(playerPos.getMapPos());
		/*if (!Settings.RELEASE_MODE) {
			Settings.p("Dist to exit: " + dist);
		}*/
		if (dist < 1) {//Game.UNIT / 2) {
			Game.levelComplete = true;
			if (Settings.DEBUG_LEVEL_JUMP) {
				Settings.p("Got to exit!");
			}
		}
	}

}
