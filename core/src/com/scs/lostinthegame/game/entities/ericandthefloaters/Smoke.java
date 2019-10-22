package com.scs.lostinthegame.game.entities.ericandthefloaters;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g3d.decals.Decal;
import com.badlogic.gdx.math.Vector3;
import com.scs.basicecs.AbstractEntity;
import com.scs.lostinthegame.game.Game;
import com.scs.lostinthegame.game.components.HasDecal;
import com.scs.lostinthegame.game.components.RemoveAfterTimeData;

public class Smoke extends AbstractEntity {

	public Smoke(int x, int y) {
		super(Smoke.class.getSimpleName());
		
		HasDecal hasDecal = new HasDecal();
		Texture tex = new Texture(Gdx.files.internal("ericandthefloaters/smoke.png"));
		TextureRegion tr = new TextureRegion(tex, 0, 0, tex.getWidth(), tex.getHeight());
        hasDecal.decal = Decal.newDecal(tr, true);
        hasDecal.decal.setScale(Game.UNIT / tr.getRegionWidth() / 2);
        hasDecal.decal.setPosition(new Vector3(x*Game.UNIT, -Game.UNIT/5, y*Game.UNIT));
        hasDecal.faceCamera = true;
        hasDecal.faceCameraTilted = true;
        this.addComponent(hasDecal);

        this.addComponent(new RemoveAfterTimeData(2));
		
	}

}
