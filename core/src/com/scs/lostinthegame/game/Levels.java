package com.scs.lostinthegame.game;

import com.scs.lostinthegame.Settings;
import com.scs.lostinthegame.game.levels.AbstractLevel;
import com.scs.lostinthegame.game.levels.GulpmanLevel;
import com.scs.lostinthegame.game.levels.IntroLevel;
import com.scs.lostinthegame.game.levels.MinedOutLevel;
import com.scs.lostinthegame.game.levels.MonsterMazeLevel;
import com.scs.lostinthegame.game.levels.OhMummyLevel;
import com.scs.lostinthegame.game.levels.StartLevel;

public class Levels {
	
	private int currentLevelNum = Settings.RELEASE_MODE ? 0 : Settings.START_LEVEL-1; 
	public int numberTimesLoopAround = 0;

	public Levels() {
	}
	
	public AbstractLevel getLevel() {
		switch (currentLevelNum) {
		case 0:
			return new IntroLevel();
		case 1:
			return new StartLevel();
		case 2:
			return new OhMummyLevel(numberTimesLoopAround);
		case 3:
			return new GulpmanLevel(numberTimesLoopAround);
		case 4:
			return new MonsterMazeLevel(numberTimesLoopAround);
		case 5:
			return new MinedOutLevel(numberTimesLoopAround);
		//case 6:
			//return new EricAndTheFloatersLevel(entityManager, decalManager);
			//return new MaziacsLevel(entityManager, decalManager);
			//gameLevel = new LaserSquadLevel(this.entityManager, this.decalManager);
			//return new AndroidsLevel(entityManager, decalManager, numberTimesLoopAround);

		default:
			//throw new RuntimeException("Unknown level: " + currentLevelNum);
			// Loop around
			currentLevelNum -= 4;
			numberTimesLoopAround++;
			return getLevel();
		}
	}
	
	
	public void nextLevel() {
		this.currentLevelNum++;
	}
	
	
	public void restart() {
		this.currentLevelNum = 0;
	}

}
