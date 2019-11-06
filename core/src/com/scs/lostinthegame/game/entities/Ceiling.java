package com.scs.lostinthegame.game.entities;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.Texture.TextureWrap;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Matrix3;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.scs.basicecs.AbstractEntity;
import com.scs.lostinthegame.game.Game;
import com.scs.lostinthegame.game.components.HasModel;

public class Ceiling extends AbstractEntity {

	public Ceiling(String tex_filename, int mapOffX, int mapOffZ, int map_width, int map_height, boolean tile, float height) {
		super(Ceiling.class.getSimpleName());
		
		Texture tex = new Texture(tex_filename);
		tex.setWrap(TextureWrap.Repeat, TextureWrap.ClampToEdge);
		Material white_material = new Material(TextureAttribute.createDiffuse(tex));		

		ModelBuilder modelBuilder = new ModelBuilder();
		Model floor = modelBuilder.createRect(
				0f, 0f, (float) map_height*Game.UNIT,
				(float)map_width*Game.UNIT, 0f, (float)map_height*Game.UNIT,
				(float)map_width*Game.UNIT, 0f, 0f,
				0f,0f,0f,
				1f,1f,1f,
				white_material,
				VertexAttributes.Usage.Position | VertexAttributes.Usage.TextureCoordinates);
		Matrix3 mat = new Matrix3();
		if (tile) {
			mat.scl(new Vector2(map_width, map_height));
		}
		floor.meshes.get(0).transformUV(mat);

		ModelInstance instance = new ModelInstance(floor);
		instance.transform.translate(mapOffX * Game.UNIT, height, mapOffZ * Game.UNIT);
		instance.transform.rotate(Vector3.X, 180);
		instance.transform.translate(0, 0, -(float)map_width* Game.UNIT);

		HasModel model = new HasModel(instance);
		this.addComponent(model);
	}

}
