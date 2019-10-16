package com.scs.lostinthegame.game.entities.burdenlair;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.scs.lostinthegame.game.Game;
import com.scs.lostinthegame.game.World;
import com.scs.lostinthegame.game.entities.Entity;
import com.scs.lostinthegame.game.interfaces.IInteractable;
import com.scs.lostinthegame.game.player.Player;

public final class Statue extends Entity implements IInteractable {

	private boolean reading = false;
	private int readProgress = 0;
	private float readTimer = 0f;
	private float soundTimer = 0f;

	private String readText[] = new String[]{
			"Stars Above Us All"," ",
			"Sun Shining In The East"," ",
			"And A Silent Moon To The West"," "
	};

	public Statue(TextureRegion[][] tex, int x, int y) {
		super(Statue.class.getSimpleName(), tex, x, y, 3, 4);
	}

	
	@Override
	public String getInteractText(Player player) {
		String txt = " ";
		if(readProgress < readText.length) {
			txt = readText[readProgress];
		}
		return reading ? txt.substring(0, Math.min(txt.length(), (int)(readTimer*txt.length()))) : "Read (E)";
	}

	
	@Override
	public void interact(Player player) {
		reading = true;
		readProgress = 0;
		readTimer = 0;

	}
	

	@Override
	public void update(World world) {
		if (reading) {
			Game.player.interactTarget = this;

			if(readTimer<1f && (readText[readProgress].equals(" ") == false))
				soundTimer += Gdx.graphics.getDeltaTime();

			if(soundTimer > 0.1f){
				soundTimer-=0.1f;
				Game.audio.play("read");
			}

			readTimer += Gdx.graphics.getDeltaTime();

			if(readTimer>2f || (readText[readProgress].equals(" ") && readTimer>.5f)){
				readTimer=0f;
				readProgress++;
				if(readProgress>readText.length-1) {
					reading = false;
				}
			}
		}
	}


    @Override
	public boolean isInteractable() {
		return true;
	}


}