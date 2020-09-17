package com.badlogic.gdx.graphics.g2d;

import java.nio.IntBuffer;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.Mesh.VertexDataType;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Affine2;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.IntArray;
import com.badlogic.gdx.utils.BufferUtils;

/** Draws batched quads using indices. Maintains an LRU texture-cache to combine draw calls with different textures effectively.
 * @see Batch
 * 
 * @author mzechner
 * @author Nathan Sweet
 * @author VaTTeRGeR */

public class TextureArraySpriteBatch implements Batch {

	private int idx = 0;
	
	private final Mesh mesh;

	private final float[] vertices;
	
	private final int spriteVertexSize	= Sprite.VERTEX_SIZE;
	private final int spriteFloatSize	= Sprite.SPRITE_SIZE;
	
	// The maximum number of available texture units for the fragment shader
	private int maxTextureUnits;
	// Textures in use (index: Texture Unit, value: Texture)
	private final Array<Texture> usedTextures;
	// LRU Array (first item = LRU) (index: Position in LRU order, value: Texture Unit)
	private final IntArray usedTexturesLRU;
	
	// Gets sent to the fragment shader as an uniform "uniform sampler2d[X] u_textures"
	private final IntBuffer textureUnitIndicesBuffer;

	float invTexWidth = 0, invTexHeight = 0;
	
	private boolean drawing = false;

	private final Matrix4 transformMatrix = new Matrix4();
	private final Matrix4 projectionMatrix = new Matrix4();
	private final Matrix4 combinedMatrix = new Matrix4();

	private boolean blendingDisabled = false;
	private int blendSrcFunc = GL20.GL_SRC_ALPHA;
	private int blendDstFunc = GL20.GL_ONE_MINUS_SRC_ALPHA;
	private int blendSrcFuncAlpha = GL20.GL_SRC_ALPHA;
	private int blendDstFuncAlpha = GL20.GL_ONE_MINUS_SRC_ALPHA;

	private ShaderProgram shader = null;
	private ShaderProgram customShader = null;

	private boolean ownsShader;

	private final Color color = new Color(1, 1, 1, 1);
	private float colorPacked = Color.WHITE_FLOAT_BITS;

	/** Number of render calls since the last {@link #begin()}. **/
	public int renderCalls = 0;

	/** Number of rendering calls, ever. Will not be reset unless set manually. **/
	public int totalRenderCalls = 0;

	/** The maximum number of sprites rendered in one batch so far. **/
	public int maxSpritesInBatch = 0;

	/** Constructs a new SpriteBatch with a size of 1000, one buffer, and the default shader.
	 * @see SpriteBatch#SpriteBatch(int, ShaderProgram) */
	public TextureArraySpriteBatch() {
		this(1000);
	}
	
	/** Constructs a SpriteBatch with one buffer and the default shader.
	 * @see SpriteBatch#SpriteBatch(int, ShaderProgram) */
	public TextureArraySpriteBatch(int size) {
		this(size, null);
	}
	
	/** Constructs a new SpriteBatch. Sets the projection matrix to an orthographic projection with y-axis point upwards, x-axis
	 * point to the right and the origin being in the bottom left corner of the screen. The projection will be pixel perfect with
	 * respect to the current screen resolution.
	 * <p>
	 * The defaultShader specifies the shader to use. Note that the names for uniforms for this default shader are different than
	 * the ones expect for shaders set with {@link #setShader(ShaderProgram)}. See {@link #createDefaultShader()}.
	 * @param size The max number of sprites in a single batch. Max of 8191.
	 * @param defaultShader The default shader to use. This is not owned by the SpriteBatch and must be disposed separately. */
	public TextureArraySpriteBatch(int size, ShaderProgram defaultShader) {
		
		// 32767 is max vertex index, so 32767 / 4 vertices per sprite = 8191 sprites max.
		if (size > 8191) throw new IllegalArgumentException("Can't have more than 8191 sprites per batch: " + size);

		// Query the number of available texture units and decide on a safe number of texture units to use
		IntBuffer texUnitsQueryBuffer = BufferUtils.newIntBuffer(32);

		Gdx.gl.glGetIntegerv(GL20.GL_MAX_TEXTURE_IMAGE_UNITS, (IntBuffer)texUnitsQueryBuffer.position(0));
		//Gdx.gl.glGetIntegerv(GL20.GL_MAX_TEXTURE_UNITS, texUnitsQueryBuffer.position(1));
		
		
		final int maxFragment	= texUnitsQueryBuffer.get(0);
		//final int maxLegacy	= texUnitsQueryBuffer.get(1);
		
		// Some obscure GPUs report wrong numbers, for example Intel HD 3000,
		// where GL_MAX_TEXTURE_UNITS is the actual number of supported fragment
		// texture units while newer GPUs like Nvidias models will respond with
		// low numbers for GL_MAX_TEXTURE_UNITS (like 4) but put out correct
		// values for GL_MAX_TEXTURE_IMAGE_UNITS.
		/*if(maxLegacy > 4) {
			maxTextureUnits = Math.min(maxLegacy, maxFragment);
		} else {
			maxTextureUnits = maxFragment;
		}*/

		maxTextureUnits = maxFragment;
		
		// If the user doesn't supply a shader we use the default shader
		if (defaultShader == null) {
			
			// Will try to create a shader with a lower amount of texture units if creation fails.
			while(maxTextureUnits > 0) {
				
				try {
					
					shader = createDefaultShader(maxTextureUnits);
					
					break;
					
				} catch (Exception e) {
					maxTextureUnits /= 2;
				}
			}
			
			if(maxTextureUnits == 0) {
				throw new IllegalStateException("Texture Arrays are not supported on this device.");
			}
			
			ownsShader = true;
			
		} else {
			shader = defaultShader;
			ownsShader = false;
		}
		
		System.out.println("Using " + maxTextureUnits + " texture units.");
		
		usedTextures = new Array<Texture>(true, maxTextureUnits, Texture.class);
		usedTexturesLRU = new IntArray(true, maxTextureUnits);
		
		// This contains the numbers 0 ... maxTextureUnits-1. We send these to the shader as an uniform.
		textureUnitIndicesBuffer = BufferUtils.newIntBuffer(maxTextureUnits);
		for (int i = 0; i < maxTextureUnits; i++) {
			textureUnitIndicesBuffer.put(i);
		}
		textureUnitIndicesBuffer.flip();
		
		VertexDataType vertexDataType = (Gdx.gl30 != null) ? VertexDataType.VertexBufferObjectWithVAO : VertexDataType.VertexArray;

		// The vertex data is extended with one float for the texture index.
		mesh = new Mesh(vertexDataType, false, size * 4, size * 6,
			new VertexAttribute(Usage.Position, 2, ShaderProgram.POSITION_ATTRIBUTE),
			new VertexAttribute(Usage.ColorPacked, 4, ShaderProgram.COLOR_ATTRIBUTE),
			new VertexAttribute(Usage.TextureCoordinates, 2, ShaderProgram.TEXCOORD_ATTRIBUTE + "0"),
			new VertexAttribute(Usage.Generic, 1, "texture_index"));

		projectionMatrix.setToOrtho2D(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

		vertices = new float[size * (Sprite.SPRITE_SIZE + 4)];

		int len = size * 6;
		short[] indices = new short[len];
		short j = 0;
		for (int i = 0; i < len; i += 6, j += 4) {
			indices[i] = j;
			indices[i + 1] = (short)(j + 1);
			indices[i + 2] = (short)(j + 2);
			indices[i + 3] = (short)(j + 2);
			indices[i + 4] = (short)(j + 3);
			indices[i + 5] = j;
		}
		
		mesh.setIndices(indices);
	}
	
	/** Returns a new instance of the default shader used by SpriteBatch for GL2 when no shader is specified. */
	static public ShaderProgram createDefaultShader (int maxTextureUnits) {
		
		// The texture index is just passed to the fragment shader, maybe there's an more elegant way.
		String vertexShader = "attribute vec4 " + ShaderProgram.POSITION_ATTRIBUTE + ";\n" //
			+ "attribute vec4 " + ShaderProgram.COLOR_ATTRIBUTE + ";\n" //
			+ "attribute vec2 " + ShaderProgram.TEXCOORD_ATTRIBUTE + "0;\n" //
			+ "attribute float texture_index;\n" //
			+ "uniform mat4 u_projTrans;\n" //
			+ "varying vec4 v_color;\n" //
			+ "varying vec2 v_texCoords;\n" //
			+ "varying float v_texture_index;\n" //
			+ "\n" //
			+ "void main()\n" //
			+ "{\n" //
			+ "   v_color = " + ShaderProgram.COLOR_ATTRIBUTE + ";\n" //
			+ "   v_color.a = v_color.a * (255.0/254.0);\n" //
			+ "   v_texCoords = " + ShaderProgram.TEXCOORD_ATTRIBUTE + "0;\n" //
			+ "   v_texture_index = texture_index;\n" //
			+ "   gl_Position =  u_projTrans * " + ShaderProgram.POSITION_ATTRIBUTE + ";\n" //
			+ "}\n";
		
		// The texture is simply selected from an array of textures
		String fragmentShader = "#ifdef GL_ES\n" //
			+ "#define LOWP lowp\n" //
			+ "precision mediump float;\n" //
			+ "#else\n" //
			+ "#define LOWP \n" //
			+ "#endif\n" //
			+ "varying LOWP vec4 v_color;\n" //
			+ "varying vec2 v_texCoords;\n" //
			+ "varying float v_texture_index;\n" //
			+ "uniform sampler2D u_textures[" + maxTextureUnits + "];\n" //
			+ "void main()\n"//
			+ "{\n" //
			+ "  int index = int(v_texture_index);" //
			+ "  gl_FragColor = v_color * texture2D(u_textures[index], v_texCoords);\n" //
			+ "}";

		ShaderProgram shader = new ShaderProgram(vertexShader, fragmentShader);
		
		if (!shader.isCompiled()) {
			throw new IllegalArgumentException("Error compiling shader: " + shader.getLog());
		}
		
		return shader;
	}
	
	@Override
	public void begin() {
		
		if (drawing) throw new IllegalStateException("SpriteBatch.end must be called before begin.");
		renderCalls = 0;

		Gdx.gl.glDepthMask(false);
		
		if(customShader != null) {
			customShader.begin();
		} else {
			shader.begin();
		}
		
		setupMatrices();

		drawing = true;
	}

	@Override
	public void end() {

		if (!drawing) throw new IllegalStateException("SpriteBatch.begin must be called before end.");
		
		if (idx > 0) flush();
		
		usedTextures.clear();
		usedTexturesLRU.clear();
		
		drawing = false;

		GL20 gl = Gdx.gl;
		
		gl.glDepthMask(true);
		
		if (isBlendingEnabled()) {
			gl.glDisable(GL20.GL_BLEND);
		}

		if(customShader != null) {
			customShader.end();
		} else {
			shader.end();
		}
	}

	@Override
	public void dispose() {
		
		mesh.dispose();
		
		if(ownsShader && shader != null) {
			shader.dispose();
		}
	}

	@Override
	public void setColor(Color tint) {
		color.set(tint);
		colorPacked = tint.toFloatBits();
	}

	@Override
	public void setColor(float r, float g, float b, float a) {
		color.set(r, g, b, a);
		colorPacked = color.toFloatBits();
	}

	@Override
	public Color getColor() {
		return color;
	}

	@Override
	public void setPackedColor(float packedColor) {
		Color.abgr8888ToColor(color, packedColor);
		this.colorPacked = packedColor;
	}

	@Override
	public float getPackedColor() {
		return colorPacked;
	}

	@Override
	public void draw(Texture texture, float x, float y, float originX, float originY, float width, float height,
			float scaleX, float scaleY, float rotation, int srcX, int srcY, int srcWidth, int srcHeight, boolean flipX, boolean flipY) {
		if (!drawing) throw new IllegalStateException("SpriteBatch.begin must be called before draw.");

		float[] vertices = this.vertices;

		flushIfFull();

		final float ti = activateTexture(texture);
		
		// bottom left and top right corner points relative to origin
		final float worldOriginX = x + originX;
		final float worldOriginY = y + originY;
		float fx = -originX;
		float fy = -originY;
		float fx2 = width - originX;
		float fy2 = height - originY;

		// scale
		if (scaleX != 1 || scaleY != 1) {
			fx *= scaleX;
			fy *= scaleY;
			fx2 *= scaleX;
			fy2 *= scaleY;
		}

		// construct corner points, start from top left and go counter clockwise
		final float p1x = fx;
		final float p1y = fy;
		final float p2x = fx;
		final float p2y = fy2;
		final float p3x = fx2;
		final float p3y = fy2;
		final float p4x = fx2;
		final float p4y = fy;

		float x1;
		float y1;
		float x2;
		float y2;
		float x3;
		float y3;
		float x4;
		float y4;

		// rotate
		if (rotation != 0) {
			final float cos = MathUtils.cosDeg(rotation);
			final float sin = MathUtils.sinDeg(rotation);

			x1 = cos * p1x - sin * p1y;
			y1 = sin * p1x + cos * p1y;

			x2 = cos * p2x - sin * p2y;
			y2 = sin * p2x + cos * p2y;

			x3 = cos * p3x - sin * p3y;
			y3 = sin * p3x + cos * p3y;

			x4 = x1 + (x3 - x2);
			y4 = y3 - (y2 - y1);
		} else {
			x1 = p1x;
			y1 = p1y;

			x2 = p2x;
			y2 = p2y;

			x3 = p3x;
			y3 = p3y;

			x4 = p4x;
			y4 = p4y;
		}

		x1 += worldOriginX;
		y1 += worldOriginY;
		x2 += worldOriginX;
		y2 += worldOriginY;
		x3 += worldOriginX;
		y3 += worldOriginY;
		x4 += worldOriginX;
		y4 += worldOriginY;

		float u = srcX * invTexWidth;
		float v = (srcY + srcHeight) * invTexHeight;
		float u2 = (srcX + srcWidth) * invTexWidth;
		float v2 = srcY * invTexHeight;

		if (flipX) {
			float tmp = u;
			u = u2;
			u2 = tmp;
		}

		if (flipY) {
			float tmp = v;
			v = v2;
			v2 = tmp;
		}

		final float color = this.colorPacked;
		
		vertices[idx++] = x1;
		vertices[idx++] = y1;
		vertices[idx++] = color;
		vertices[idx++] = u;
		vertices[idx++] = v;
		vertices[idx++] = ti;

		vertices[idx++] = x2;
		vertices[idx++] = y2;
		vertices[idx++] = color;
		vertices[idx++] = u;
		vertices[idx++] = v2;
		vertices[idx++] = ti;

		vertices[idx++] = x3;
		vertices[idx++] = y3;
		vertices[idx++] = color;
		vertices[idx++] = u2;
		vertices[idx++] = v2;
		vertices[idx++] = ti;

		vertices[idx++] = x4;
		vertices[idx++] = y4;
		vertices[idx++] = color;
		vertices[idx++] = u2;
		vertices[idx++] = v;
		vertices[idx++] = ti;
	}

	@Override
	public void draw(Texture texture, float x, float y, float width, float height, int srcX, int srcY, int srcWidth,
			int srcHeight, boolean flipX, boolean flipY) {
		
		if (!drawing) throw new IllegalStateException("SpriteBatch.begin must be called before draw.");

		float[] vertices = this.vertices;

		flushIfFull();
		
		final float ti = activateTexture(texture);

		float u = srcX * invTexWidth;
		float v = (srcY + srcHeight) * invTexHeight;
		float u2 = (srcX + srcWidth) * invTexWidth;
		float v2 = srcY * invTexHeight;
		final float fx2 = x + width;
		final float fy2 = y + height;

		if (flipX) {
			float tmp = u;
			u = u2;
			u2 = tmp;
		}

		if (flipY) {
			float tmp = v;
			v = v2;
			v2 = tmp;
		}

		float color = this.colorPacked;
		
		vertices[idx++] = x;
		vertices[idx++] = y;
		vertices[idx++] = color;
		vertices[idx++] = u;
		vertices[idx++] = v;
		vertices[idx++] = ti;

		vertices[idx++] = x;
		vertices[idx++] = fy2;
		vertices[idx++] = color;
		vertices[idx++] = u;
		vertices[idx++] = v2;
		vertices[idx++] = ti;

		vertices[idx++] = fx2;
		vertices[idx++] = fy2;
		vertices[idx++] = color;
		vertices[idx++] = u2;
		vertices[idx++] = v2;
		vertices[idx++] = ti;

		vertices[idx++] = fx2;
		vertices[idx++] = y;
		vertices[idx++] = color;
		vertices[idx++] = u2;
		vertices[idx++] = v;
		vertices[idx++] = ti;
	}

	@Override
	public void draw(Texture texture, float x, float y, int srcX, int srcY, int srcWidth, int srcHeight) {
		if (!drawing) throw new IllegalStateException("SpriteBatch.begin must be called before draw.");

		float[] vertices = this.vertices;

		flushIfFull();
		
		final float ti = activateTexture(texture);

		final float u = srcX * invTexWidth;
		final float v = (srcY + srcHeight) * invTexHeight;
		final float u2 = (srcX + srcWidth) * invTexWidth;
		final float v2 = srcY * invTexHeight;
		final float fx2 = x + srcWidth;
		final float fy2 = y + srcHeight;

		float color = this.colorPacked;
		
		vertices[idx++] = x;
		vertices[idx++] = y;
		vertices[idx++] = color;
		vertices[idx++] = u;
		vertices[idx++] = v;
		vertices[idx++] = ti;

		vertices[idx++] = x;
		vertices[idx++] = fy2;
		vertices[idx++] = color;
		vertices[idx++] = u;
		vertices[idx++] = v2;
		vertices[idx++] = ti;

		vertices[idx++] = fx2;
		vertices[idx++] = fy2;
		vertices[idx++] = color;
		vertices[idx++] = u2;
		vertices[idx++] = v2;
		vertices[idx++] = ti;

		vertices[idx++] = fx2;
		vertices[idx++] = y;
		vertices[idx++] = color;
		vertices[idx++] = u2;
		vertices[idx++] = v;
		vertices[idx++] = ti;
	}

	@Override
	public void draw(Texture texture, float x, float y, float width, float height, float u, float v, float u2, float v2) {
		
		if (!drawing) throw new IllegalStateException("SpriteBatch.begin must be called before draw.");

		float[] vertices = this.vertices;

		flushIfFull();
		
		final float ti = activateTexture(texture);

		final float fx2 = x + width;
		final float fy2 = y + height;

		float color = this.colorPacked;
		
		vertices[idx++] = x;
		vertices[idx++] = y;
		vertices[idx++] = color;
		vertices[idx++] = u;
		vertices[idx++] = v;
		vertices[idx++] = ti;

		vertices[idx++] = x;
		vertices[idx++] = fy2;
		vertices[idx++] = color;
		vertices[idx++] = u;
		vertices[idx++] = v2;
		vertices[idx++] = ti;

		vertices[idx++] = fx2;
		vertices[idx++] = fy2;
		vertices[idx++] = color;
		vertices[idx++] = u2;
		vertices[idx++] = v2;
		vertices[idx++] = ti;

		vertices[idx++] = fx2;
		vertices[idx++] = y;
		vertices[idx++] = color;
		vertices[idx++] = u2;
		vertices[idx++] = v;
		vertices[idx++] = ti;
	}

	@Override
	public void draw(Texture texture, float x, float y) {
		draw(texture, x, y, texture.getWidth(), texture.getHeight());
	}

	@Override
	public void draw(Texture texture, float x, float y, float width, float height) {
		
		if (!drawing) throw new IllegalStateException("SpriteBatch.begin must be called before draw.");

		float[] vertices = this.vertices;

		flushIfFull();
		
		final float ti = activateTexture(texture);

		final float fx2 = x + width;
		final float fy2 = y + height;
		final float u = 0;
		final float v = 1;
		final float u2 = 1;
		final float v2 = 0;

		float color = this.colorPacked;

		vertices[idx++] = x;
		vertices[idx++] = y;
		vertices[idx++] = color;
		vertices[idx++] = u;
		vertices[idx++] = v;
		vertices[idx++] = ti;

		vertices[idx++] = x;
		vertices[idx++] = fy2;
		vertices[idx++] = color;
		vertices[idx++] = u;
		vertices[idx++] = v2;
		vertices[idx++] = ti;

		vertices[idx++] = fx2;
		vertices[idx++] = fy2;
		vertices[idx++] = color;
		vertices[idx++] = u2;
		vertices[idx++] = v2;
		vertices[idx++] = ti;

		vertices[idx++] = fx2;
		vertices[idx++] = y;
		vertices[idx++] = color;
		vertices[idx++] = u2;
		vertices[idx++] = v;
		vertices[idx++] = ti;
	}

	@Override
	public void draw(Texture texture, float[] spriteVertices, int offset, int count) {
		
		if (!drawing) {
			throw new IllegalStateException("SpriteBatch.begin must be called before draw.");
		}
		
		flushIfFull();
		
		// Assigns a texture unit to this texture, flushing if none is available
		final float ti = (float)activateTexture(texture);
		
		copyVerticesAndInjectTextureUnit(spriteVertices, count, ti);
	}
	
	private void flushIfFull() {
		// original Sprite attribute size plus one extra float per sprite vertex
		if(vertices.length - idx < spriteFloatSize + spriteFloatSize / spriteVertexSize) {
			flush();
		}
	}
	
	private void copyVerticesAndInjectTextureUnit(float[] spriteVertices, int count, float textureUnit) {
		
		// spriteVertexSize is the number of floats an unmodified input vertex consists of.
		for (int srcPos = 0; srcPos < count; srcPos += spriteVertexSize) {
			
			// Copy the vertices
			System.arraycopy(spriteVertices, srcPos, vertices, idx, spriteVertexSize);

			// Advance idx by vertex float count
			idx += spriteVertexSize;
			
			// Inject texture unit index and advance idx
			vertices[idx++] = textureUnit;
		}
	}

	@Override
	public void draw(TextureRegion region, float x, float y) {
		draw(region, x, y, region.getRegionWidth(), region.getRegionHeight());
	}

	@Override
	public void draw(TextureRegion region, float x, float y, float width, float height) {

		if (!drawing) throw new IllegalStateException("SpriteBatch.begin must be called before draw.");

		float[] vertices = this.vertices;

		flushIfFull();
		
		final float ti = activateTexture(region.texture);

		final float fx2 = x + width;
		final float fy2 = y + height;
		final float u = region.u;
		final float v = region.v2;
		final float u2 = region.u2;
		final float v2 = region.v;

		float color = this.colorPacked;
		
		vertices[idx++] = x;
		vertices[idx++] = y;
		vertices[idx++] = color;
		vertices[idx++] = u;
		vertices[idx++] = v;
		vertices[idx++] = ti;

		vertices[idx++] = x;
		vertices[idx++] = fy2;
		vertices[idx++] = color;
		vertices[idx++] = u;
		vertices[idx++] = v2;
		vertices[idx++] = ti;

		vertices[idx++] = fx2;
		vertices[idx++] = fy2;
		vertices[idx++] = color;
		vertices[idx++] = u2;
		vertices[idx++] = v2;
		vertices[idx++] = ti;

		vertices[idx++] = fx2;
		vertices[idx++] = y;
		vertices[idx++] = color;
		vertices[idx++] = u2;
		vertices[idx++] = v;
		vertices[idx++] = ti;
	}

	@Override
	public void draw(TextureRegion region, float x, float y, float originX, float originY, float width, float height,
			float scaleX, float scaleY, float rotation) {
		
		if (!drawing) throw new IllegalStateException("SpriteBatch.begin must be called before draw.");

		float[] vertices = this.vertices;

		flushIfFull();
		
		final float ti = activateTexture(region.texture);

		// bottom left and top right corner points relative to origin
		final float worldOriginX = x + originX;
		final float worldOriginY = y + originY;
		float fx = -originX;
		float fy = -originY;
		float fx2 = width - originX;
		float fy2 = height - originY;

		// scale
		if (scaleX != 1 || scaleY != 1) {
			fx *= scaleX;
			fy *= scaleY;
			fx2 *= scaleX;
			fy2 *= scaleY;
		}

		// construct corner points, start from top left and go counter clockwise
		final float p1x = fx;
		final float p1y = fy;
		final float p2x = fx;
		final float p2y = fy2;
		final float p3x = fx2;
		final float p3y = fy2;
		final float p4x = fx2;
		final float p4y = fy;

		float x1;
		float y1;
		float x2;
		float y2;
		float x3;
		float y3;
		float x4;
		float y4;

		// rotate
		if (rotation != 0) {
			final float cos = MathUtils.cosDeg(rotation);
			final float sin = MathUtils.sinDeg(rotation);

			x1 = cos * p1x - sin * p1y;
			y1 = sin * p1x + cos * p1y;

			x2 = cos * p2x - sin * p2y;
			y2 = sin * p2x + cos * p2y;

			x3 = cos * p3x - sin * p3y;
			y3 = sin * p3x + cos * p3y;

			x4 = x1 + (x3 - x2);
			y4 = y3 - (y2 - y1);
		} else {
			x1 = p1x;
			y1 = p1y;

			x2 = p2x;
			y2 = p2y;

			x3 = p3x;
			y3 = p3y;

			x4 = p4x;
			y4 = p4y;
		}

		x1 += worldOriginX;
		y1 += worldOriginY;
		x2 += worldOriginX;
		y2 += worldOriginY;
		x3 += worldOriginX;
		y3 += worldOriginY;
		x4 += worldOriginX;
		y4 += worldOriginY;

		final float u = region.u;
		final float v = region.v2;
		final float u2 = region.u2;
		final float v2 = region.v;

		float color = this.colorPacked;
		
		vertices[idx++] = x1;
		vertices[idx++] = y1;
		vertices[idx++] = color;
		vertices[idx++] = u;
		vertices[idx++] = v;
		vertices[idx++] = ti;

		vertices[idx++] = x2;
		vertices[idx++] = y2;
		vertices[idx++] = color;
		vertices[idx++] = u;
		vertices[idx++] = v2;
		vertices[idx++] = ti;

		vertices[idx++] = x3;
		vertices[idx++] = y3;
		vertices[idx++] = color;
		vertices[idx++] = u2;
		vertices[idx++] = v2;
		vertices[idx++] = ti;

		vertices[idx++] = x4;
		vertices[idx++] = y4;
		vertices[idx++] = color;
		vertices[idx++] = u2;
		vertices[idx++] = v;
		vertices[idx++] = ti;
	}

	@Override
	public void draw(TextureRegion region, float x, float y, float originX, float originY, float width, float height,
			float scaleX, float scaleY, float rotation, boolean clockwise) {

		if (!drawing) throw new IllegalStateException("SpriteBatch.begin must be called before draw.");

		float[] vertices = this.vertices;

		flushIfFull();
		
		final float ti = activateTexture(region.texture);
		
		// bottom left and top right corner points relative to origin
		final float worldOriginX = x + originX;
		final float worldOriginY = y + originY;
		float fx = -originX;
		float fy = -originY;
		float fx2 = width - originX;
		float fy2 = height - originY;

		// scale
		if (scaleX != 1 || scaleY != 1) {
			fx *= scaleX;
			fy *= scaleY;
			fx2 *= scaleX;
			fy2 *= scaleY;
		}

		// construct corner points, start from top left and go counter clockwise
		final float p1x = fx;
		final float p1y = fy;
		final float p2x = fx;
		final float p2y = fy2;
		final float p3x = fx2;
		final float p3y = fy2;
		final float p4x = fx2;
		final float p4y = fy;

		float x1;
		float y1;
		float x2;
		float y2;
		float x3;
		float y3;
		float x4;
		float y4;

		// rotate
		if (rotation != 0) {
			final float cos = MathUtils.cosDeg(rotation);
			final float sin = MathUtils.sinDeg(rotation);

			x1 = cos * p1x - sin * p1y;
			y1 = sin * p1x + cos * p1y;

			x2 = cos * p2x - sin * p2y;
			y2 = sin * p2x + cos * p2y;

			x3 = cos * p3x - sin * p3y;
			y3 = sin * p3x + cos * p3y;

			x4 = x1 + (x3 - x2);
			y4 = y3 - (y2 - y1);
		} else {
			x1 = p1x;
			y1 = p1y;

			x2 = p2x;
			y2 = p2y;

			x3 = p3x;
			y3 = p3y;

			x4 = p4x;
			y4 = p4y;
		}

		x1 += worldOriginX;
		y1 += worldOriginY;
		x2 += worldOriginX;
		y2 += worldOriginY;
		x3 += worldOriginX;
		y3 += worldOriginY;
		x4 += worldOriginX;
		y4 += worldOriginY;

		float u1, v1, u2, v2, u3, v3, u4, v4;
		if (clockwise) {
			u1 = region.u2;
			v1 = region.v2;
			u2 = region.u;
			v2 = region.v2;
			u3 = region.u;
			v3 = region.v;
			u4 = region.u2;
			v4 = region.v;
		} else {
			u1 = region.u;
			v1 = region.v;
			u2 = region.u2;
			v2 = region.v;
			u3 = region.u2;
			v3 = region.v2;
			u4 = region.u;
			v4 = region.v2;
		}

		float color = this.colorPacked;

		vertices[idx++] = x1;
		vertices[idx++] = y1;
		vertices[idx++] = color;
		vertices[idx++] = u1;
		vertices[idx++] = v1;
		vertices[idx++] = ti;

		vertices[idx++] = x2;
		vertices[idx++] = y2;
		vertices[idx++] = color;
		vertices[idx++] = u2;
		vertices[idx++] = v2;
		vertices[idx++] = ti;

		vertices[idx++] = x3;
		vertices[idx++] = y3;
		vertices[idx++] = color;
		vertices[idx++] = u3;
		vertices[idx++] = v3;
		vertices[idx++] = ti;

		vertices[idx++] = x4;
		vertices[idx++] = y4;
		vertices[idx++] = color;
		vertices[idx++] = u4;
		vertices[idx++] = v4;
		vertices[idx++] = ti;
	}

	@Override
	public void draw(TextureRegion region, float width, float height, Affine2 transform) {
		if (!drawing) throw new IllegalStateException("SpriteBatch.begin must be called before draw.");

		float[] vertices = this.vertices;

		flushIfFull();
		
		final float ti = activateTexture(region.texture);

		// construct corner points
		float x1 = transform.m02;
		float y1 = transform.m12;
		float x2 = transform.m01 * height + transform.m02;
		float y2 = transform.m11 * height + transform.m12;
		float x3 = transform.m00 * width + transform.m01 * height + transform.m02;
		float y3 = transform.m10 * width + transform.m11 * height + transform.m12;
		float x4 = transform.m00 * width + transform.m02;
		float y4 = transform.m10 * width + transform.m12;

		float u = region.u;
		float v = region.v2;
		float u2 = region.u2;
		float v2 = region.v;

		float color = this.colorPacked;
		
		vertices[idx++] = x1;
		vertices[idx++] = y1;
		vertices[idx++] = color;
		vertices[idx++] = u;
		vertices[idx++] = v;
		vertices[idx++] = ti;

		vertices[idx++] = x2;
		vertices[idx++] = y2;
		vertices[idx++] = color;
		vertices[idx++] = u;
		vertices[idx++] = v2;
		vertices[idx++] = ti;

		vertices[idx++] = x3;
		vertices[idx++] = y3;
		vertices[idx++] = color;
		vertices[idx++] = u2;
		vertices[idx++] = v2;
		vertices[idx++] = ti;

		vertices[idx++] = x4;
		vertices[idx++] = y4;
		vertices[idx++] = color;
		vertices[idx++] = u2;
		vertices[idx++] = v;
		vertices[idx++] = ti;
	}

	@Override
	public void flush() {
		
		if (idx == 0) return;

		renderCalls++;
		totalRenderCalls++;
		int spritesInBatch = idx / (Sprite.SPRITE_SIZE + 4);
		if (spritesInBatch > maxSpritesInBatch) maxSpritesInBatch = spritesInBatch;
		int count = spritesInBatch * 6;

		// Bind the textures
		int textureUnit = 0;
		for (Texture texture : usedTextures) {
			texture.bind(textureUnit++);
		}
		// Set Texture unit one as active again before drawing.
		Gdx.gl.glActiveTexture(GL20.GL_TEXTURE0);
		
		Mesh mesh = this.mesh;
		mesh.setVertices(vertices, 0, idx);
		mesh.getIndicesBuffer().position(0);
		mesh.getIndicesBuffer().limit(count);

		if (blendingDisabled) {
			Gdx.gl.glDisable(GL20.GL_BLEND);
		} else {
			Gdx.gl.glEnable(GL20.GL_BLEND);
			if (blendSrcFunc != -1) Gdx.gl.glBlendFuncSeparate(blendSrcFunc, blendDstFunc, blendSrcFuncAlpha, blendDstFuncAlpha);
		}

		if(customShader != null) {
			mesh.render(customShader, GL20.GL_TRIANGLES, 0, count);
		} else {
			mesh.render(shader, GL20.GL_TRIANGLES, 0, count);
		}

		idx = 0;
	}

	// Assigns Texture units and manages the LRU cache.
	private int activateTexture(Texture texture) {
		
		invTexWidth = 1.0f / texture.getWidth();
		invTexHeight = 1.0f / texture.getHeight();
		
		// This is our identifier for the textures, you could also use something else
		final int textureHandle = texture.getTextureObjectHandle();
		
		// First try to see if the texture is already in use
		for (int i = 0; i < usedTextures.size; i++) {
			
			if(textureHandle == usedTextures.get(i).getTextureObjectHandle()) {
				
				// Position this texture as the most recently used one.
				// Optimization: Skip the copying if slot i is already the most recently used slot
				if(usedTexturesLRU.get(usedTexturesLRU.size - 1) != i) {
					usedTexturesLRU.removeValue(i);
					usedTexturesLRU.add(i);
				}
				
				return i;
			}
		}
		
		// If a free texture unit is available we just use it
		// If not we have to flush and then throw out the oldest one.
		if(usedTextures.size < usedTextures.items.length) {
			
			final int slot = usedTextures.size;
			
			//System.out.println("Adding new Texture " + textureHandle + " to slot " + usedTextures.size);
			
			usedTextures.add(texture);
			
			// Position this texture as the most recently used one.
			usedTexturesLRU.add(slot);
			
			return slot;
			
		} else {
			
			// We have to flush if there is something in the pipeline already,
			// otherwise the texture indices of previous sprites may be affected
			if(idx > 0) {
				flush();
			}
			
			// The least recently used texture gets kicked out.
			final int slot = usedTexturesLRU.first();
			
			//System.out.println("Kicking out Texture from slot " + slot + " for texture " + textureHandle);

			usedTextures.set(slot, texture);
			
			// Position this texture as the most recently used one.
			usedTexturesLRU.removeIndex(0);
			usedTexturesLRU.add(slot);
			
			return slot;
		}
	}
	
	@Override
	public void disableBlending() {
		
		if (blendingDisabled) return;
		
		flush();
		
		blendingDisabled = true;
	}

	@Override
	public void enableBlending() {
		
		if (!blendingDisabled) {
			return;
		}
		
		flush();
		
		blendingDisabled = false;
	}

	@Override
	public void setBlendFunction(int srcFunc, int dstFunc) {
		setBlendFunctionSeparate(srcFunc, dstFunc, srcFunc, dstFunc);
	}

	@Override
	public void setBlendFunctionSeparate(int srcFuncColor, int dstFuncColor, int srcFuncAlpha, int dstFuncAlpha) {
		
		if (blendSrcFunc == srcFuncColor && blendDstFunc == dstFuncColor && blendSrcFuncAlpha == srcFuncAlpha && blendDstFuncAlpha == dstFuncAlpha) {
			return;
		}
		
		flush();
		
		blendSrcFunc = srcFuncColor;
		blendDstFunc = dstFuncColor;
		blendSrcFuncAlpha = srcFuncAlpha;
		blendDstFuncAlpha = dstFuncAlpha;
	}

	@Override
	public int getBlendSrcFunc() {
		return blendSrcFunc;
	}

	@Override
	public int getBlendDstFunc() {
		return blendDstFunc;
	}

	@Override
	public int getBlendSrcFuncAlpha() {
		return blendSrcFuncAlpha;
	}

	@Override
	public int getBlendDstFuncAlpha() {
		return blendDstFuncAlpha;
	}

	@Override
	public boolean isBlendingEnabled() {
		return !blendingDisabled;
	}

	@Override
	public boolean isDrawing() {
		return drawing;
	}

	@Override
	public Matrix4 getProjectionMatrix() {
		return projectionMatrix;
	}

	@Override
	public Matrix4 getTransformMatrix() {
		return transformMatrix;
	}

	@Override
	public void setProjectionMatrix(Matrix4 projection) {
		
		if (drawing) {
			flush();
		}

		projectionMatrix.set(projection);
		
		if (drawing) {
			setupMatrices();
		}
	}

	@Override
	public void setTransformMatrix(Matrix4 transform) {
		
		if (drawing) {
			flush();
		}

		transformMatrix.set(transform);
		
		if (drawing) {
			setupMatrices();
		}
	}

	private void setupMatrices () {
		
		combinedMatrix.set(projectionMatrix).mul(transformMatrix);
		
		if(customShader != null) {
			customShader.setUniformMatrix("u_projTrans", combinedMatrix);
			Gdx.gl20.glUniform1iv(customShader.fetchUniformLocation("u_textures", true), maxTextureUnits, textureUnitIndicesBuffer);
			
		} else {
			shader.setUniformMatrix("u_projTrans", combinedMatrix);
			Gdx.gl20.glUniform1iv(shader.fetchUniformLocation("u_textures", true), maxTextureUnits, textureUnitIndicesBuffer);
		}
	}

	/** Sets the shader to be used in a GLES 2.0 environment. Vertex position attribute is called "a_position", the texture
	 * coordinates attribute is called "a_texCoord0", the color attribute is called "a_color", texture unit index is called
	 * "texture_index", this needs to be converted to int with int(...) in the fragment shader. See {@link ShaderProgram#POSITION_ATTRIBUTE},
	 * {@link ShaderProgram#COLOR_ATTRIBUTE} and {@link ShaderProgram#TEXCOORD_ATTRIBUTE} which gets "0" appended to indicate
	 * the use of the first texture unit. The combined transform and projection matrix is uploaded via a mat4 uniform called
	 * "u_projTrans". The texture sampler array is passed via a uniform called "u_textures", see {@link TextureArraySpriteBatch#createDefaultShader(int)}
	 * for reference.
	 * <p>
	 * Call this method with a null argument to use the default shader.
	 * <p>
	 * This method will flush the batch before setting the new shader, you can call it in between {@link #begin()} and
	 * {@link #end()}.
	 * @param shader the {@link ShaderProgram} or null to use the default shader. */
	@Override
	public void setShader(ShaderProgram shader) {

		if (drawing) {
			
			flush();
			
			if (customShader != null) {
				customShader.end();
			} else {
				this.shader.end();
			}
		}
		
		customShader = shader;
		
		if (drawing) {
			
			if (customShader != null) {
				customShader.begin();
			} else {
				this.shader.begin();
			}
			
			setupMatrices();
		}
	}

	@Override
	public ShaderProgram getShader() {		
		if(customShader != null) {
			return customShader;
		} else {
			return shader;
		}
	}
}