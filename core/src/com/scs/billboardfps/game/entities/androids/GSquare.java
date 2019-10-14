package com.scs.billboardfps.game.entities.androids;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.scs.basicecs.AbstractEntity;
import com.scs.billboardfps.game.Game;
import com.scs.billboardfps.game.components.HasModel;

public class GSquare extends AbstractEntity {

	private static Model floor;
	
	static {
		Material material = new Material(TextureAttribute.createDiffuse(new Texture(Gdx.files.internal("androids/g_square.png"))));		
		ModelBuilder modelBuilder = new ModelBuilder();
		floor = modelBuilder.createRect(
				0f, 0f, Game.UNIT,
				Game.UNIT, 0f, Game.UNIT,
				Game.UNIT, 0f, 0f,
				0f, 0f,0f,
				1f, 1f,1f,
				material,
				VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal | VertexAttributes.Usage.TextureCoordinates);

	}
	
	public GSquare(int map_x, int map_y) {
		super(GSquare.class.getSimpleName());
		
		ModelInstance instance = new ModelInstance(floor);
		instance.transform.translate((map_x*Game.UNIT)-(Game.UNIT/2), 0.1f, (map_y*Game.UNIT)-(Game.UNIT/2));
		//instance.transform.translate((map_x*Game.UNIT), 0.1f, (map_y*Game.UNIT));
		this.addComponent(new HasModel(instance));

	}

}
