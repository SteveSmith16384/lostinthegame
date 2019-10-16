package com.scs.lostinthegame.game.entities.maziacs;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g3d.decals.Decal;
import com.badlogic.gdx.math.Vector3;
import com.scs.basicecs.AbstractEntity;
import com.scs.lostinthegame.game.Art;
import com.scs.lostinthegame.game.Game;
import com.scs.lostinthegame.game.components.HasAI;
import com.scs.lostinthegame.game.components.HasDecal;
import com.scs.lostinthegame.game.components.HasDecalCycle;
import com.scs.lostinthegame.game.components.IsDamagable;
import com.scs.lostinthegame.game.components.MovementData;
import com.scs.lostinthegame.game.components.PositionData;
import com.scs.lostinthegame.game.entities.ericandthefloaters.Floater;
import com.scs.lostinthegame.game.systems.MobAISystem.Mode;

public class Maziac extends AbstractEntity {

    public Maziac(int x, int y) {
        super(Maziac.class.getSimpleName());

        PositionData pos = new PositionData();
        pos.position = new Vector3(x*Game.UNIT, 0, y*Game.UNIT);
        this.addComponent(pos);
        
		HasDecal hasDecal = new HasDecal();
		Texture tex = new Texture(Gdx.files.internal("maziacs/enemy1.png"));
		TextureRegion tr = new TextureRegion(tex, 0, 0, tex.getWidth(), tex.getHeight());
        hasDecal.decal = Decal.newDecal(tr, true);
        hasDecal.decal.setScale(Game.UNIT / tr.getRegionWidth()); // Scale to sq size by default
        hasDecal.faceCamera = true;
        hasDecal.faceCameraTilted = true;        
        this.addComponent(hasDecal);
        
        HasDecalCycle cycle = new HasDecalCycle(.5f, 2);
        cycle.decals[0] = hasDecal.decal;
        cycle.decals[1] = Art.DecalHelper("maziacs/enemy2.png", 1f);
        this.addComponent(cycle);
        
        IsDamagable damagable = new IsDamagable(1);
        this.addComponent(damagable);
        
        HasAI ai = new HasAI(Mode.MoveLikeRook, .005f);
        this.addComponent(ai);
        
        this.addComponent(new MovementData(.85f));
        
    }
    
}