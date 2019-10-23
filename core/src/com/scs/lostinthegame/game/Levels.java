package com.scs.lostinthegame.game;

import com.scs.lostinthegame.game.decals.DecalManager;
import com.scs.lostinthegame.game.entities.EntityManager;
import com.scs.lostinthegame.game.levels.AbstractLevel;
import com.scs.lostinthegame.game.levels.AndroidsLevel;
import com.scs.lostinthegame.game.levels.EricAndTheFloatersLevel;
import com.scs.lostinthegame.game.levels.GulpmanLevel;
import com.scs.lostinthegame.game.levels.MaziacsLevel;
import com.scs.lostinthegame.game.levels.MinedOutLevel;
import com.scs.lostinthegame.game.levels.OhMummyLevel;

public class Levels {
	
	private int currentLevelNum = 0; 

	public Levels() {
	}
	
	public AbstractLevel getNextLevel(EntityManager entityManager, DecalManager decalManager) {
		switch (currentLevelNum) {
		case 1:
			return new OhMummyLevel(entityManager, decalManager);
		case 2:
			return new GulpmanLevel(entityManager, decalManager);
		case 3:
			return new AndroidsLevel(entityManager, decalManager);
		case 4:
			return new EricAndTheFloatersLevel(entityManager, decalManager);
		case 5:
			return new MinedOutLevel(entityManager, decalManager);
		case 6:
			return new MaziacsLevel(entityManager, decalManager);
			//gameLevel = new LaserSquadLevel(this.entityManager, this.decalManager);

		default:
			throw new RuntimeException("Todo");
		}
	}
	
	
	public void nextLevel() {
		this.currentLevelNum++;
	}

}