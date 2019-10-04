package com.scs.billboardfps.game.player;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.scs.billboardfps.Settings;
import com.scs.billboardfps.game.Game;
import com.scs.billboardfps.game.World;
import com.scs.billboardfps.game.components.IAttackable;
import com.scs.billboardfps.game.components.IDamagable;
import com.scs.billboardfps.game.components.IHarmsPlayer;
import com.scs.billboardfps.game.components.IInteractable;
import com.scs.billboardfps.game.entity.Entity;
import com.scs.billboardfps.game.entity.chaos.ChaosBolt;

public class Player implements IDamagable {

	private static final float moveSpeed = 2f * Game.UNIT;
	private static final float gravityScale = 25 * Game.UNIT;
	public static final float playerHeight = Game.UNIT * 0.4f;
	private static final float colliderSize = .2f * Game.UNIT;
	private static final float jumpScale = 4f * Game.UNIT;
	private static final float hurtDistanceSquared = Game.UNIT * .5f * Game.UNIT * .5f;

	private static final float defaultWeaponRotation = 30f;
	private static final float chargeWeaponRotation = -20f;
	private static final float attackWeaponRotation = 120f;

	private Camera camera;
	private World world;
	public Inventory inventory;
	public CameraController cameraController;
	private Vector3 position;
	private Vector3 moveVector;
	private Vector3 tmpVector;
	private boolean onGround = false;
	private float gravity = 0f;
	private Sprite weaponSprite;
	private float weaponRotation;
	private Vector2 weaponPosition;
	private float attackAnimation;
	private float weaponScaleY = 1f;
	private boolean didAttack = true;
	private boolean didPlayAudio = false;
	private boolean mouseReleased = false;
	private float footstepTimer;
	private int health, max_health;
	private float hurtTimer = 0f;
	private Texture hurtTexture; // Screen goes red when hit
	private Texture heart;
	public IInteractable interactTarget;

	public Player(Camera cam, World wrld, Inventory inv, int lookSens, int maxHealth) {
		inventory = inv;
		camera = cam;
		world = wrld;
		this.max_health = maxHealth;
		this.health = this.max_health;

		cameraController = new CameraController(camera, lookSens);

		position = new Vector3();//world.playersStartMapX * Game.UNIT, 0f, world.playerStartMapY * Game.UNIT);
		moveVector = new Vector3();
		tmpVector = new Vector3();

		if (Settings.USE_WAND) {
			Texture weaponTex = new Texture(Gdx.files.internal("chaos/wand2.png"));
			weaponSprite = new Sprite(weaponTex);
			weaponSprite.setOrigin(32, 20);
			weaponSprite.setScale(7.5f, 5f);
			weaponPosition = new Vector2(0,0);
		} else {
			Texture weaponTex = new Texture(Gdx.files.internal("sword.png"));
			weaponSprite = new Sprite(weaponTex);
			weaponSprite.setOrigin(32, 0);
			weaponSprite.setScale(7.5f, 5f);
			weaponPosition = new Vector2(0,0);
		}

		weaponRotation = defaultWeaponRotation;
		weaponSprite.setRotation(defaultWeaponRotation);

		hurtTexture = new Texture(Gdx.files.internal("red.png"));
		heart = new Texture(Gdx.files.internal("heart.png"));
	}


	public Vector3 getPosition() {
		return position;
	}


	public void update() {
		move();
		gravity();
		checkForAttack();
		interact();

		cameraController.update();

		float weaponBob = (float)Math.cos(cameraController.bobbing * 15f + .15f) * 20f;
		weaponSprite.setPosition(Gdx.graphics.getWidth()-300 + weaponPosition.x, -20 + weaponBob+weaponPosition.y);
		weaponSprite.setScale(6f, Math.min(5f*weaponScaleY,8f));
		weaponSprite.setRotation(weaponRotation + (float)Math.cos(cameraController.bobbing*7.5f)*5f - 2.5f);


		if (hurtTimer>0) {
			hurtTimer -= Gdx.graphics.getDeltaTime();
		} else {
			// Check if any enemies are harming us
			//float hurtDistance = Game.UNIT * .5f;
			for (Entity ent : Game.entityManager.getEntities()) {
				if (ent instanceof IHarmsPlayer) {
					IHarmsPlayer hp = (IHarmsPlayer)ent;
					if (hp.harmsPlayer()) {
						// For efficiency, we use a simple dist2 and check against hurtDistance2
						if (ent.getPosition().dst2(position) < hurtDistanceSquared) {
							this.damaged(1, new Vector3()); // todo - dir
						}
					}
				}
			}
		}
	}


	private void interact() {
		interactTarget = null;

		float dist = 0f;
		float d = 0;

		Vector3 hitPos = new Vector3().set(position).mulAdd(camera.direction, Game.UNIT/2f);

		for(Entity ent : Game.entityManager.getEntities()) {
			if (ent instanceof IInteractable) {
				IInteractable ii = (IInteractable)ent;
				if (ii.isInteractable()) {
					d = ent.getPosition().dst2(position);
					if(Game.collision.hitCircle(hitPos, ent.getPosition(), Game.UNIT/2f) && (dist==0 || d<dist)) {
						interactTarget = (IInteractable)ent;
						dist = d;
					}
				}
			}
		}

		if(Gdx.input.isKeyJustPressed(Input.Keys.E) && interactTarget!=null) {
			interactTarget.interact(this);
		}

	}


	private void checkForAttack() {
		if (Gdx.input.isKeyPressed(Input.Keys.SPACE) && attackAnimation<=0){
			attackAnimation = 1.0f;
			didAttack = false;
			didPlayAudio = false;
		}
		if (Gdx.input.isButtonPressed(Input.Buttons.LEFT)){
			if (mouseReleased && Gdx.input.isCursorCatched() && attackAnimation<=0) {
				attackAnimation = 1.0f;
				didAttack = false;
				didPlayAudio = false;
			}
			mouseReleased = false;
		} else {
			mouseReleased = true;
		}

		if (attackAnimation > 0f) {
			attackAnimation -= Gdx.graphics.getDeltaTime()*4;
			if(attackAnimation>.3f) {
				weaponRotation = MathUtils.lerp(weaponRotation, chargeWeaponRotation, Gdx.graphics.getDeltaTime() * 8f);
				weaponPosition.set(
						MathUtils.lerp(weaponPosition.x, 30, Gdx.graphics.getDeltaTime()*20f),
						MathUtils.lerp(weaponPosition.y, -80, Gdx.graphics.getDeltaTime()*20f)
						);

				if(!didPlayAudio && attackAnimation<.8f){
					didPlayAudio = true;
					Game.audio.play("weapon");
				}

			} else {
				weaponRotation = MathUtils.lerp(weaponRotation, attackWeaponRotation, Gdx.graphics.getDeltaTime() * 15f);
				weaponPosition.set(
						MathUtils.lerp(weaponPosition.x, -150, Gdx.graphics.getDeltaTime()*20f),
						MathUtils.lerp(weaponPosition.y, 150, Gdx.graphics.getDeltaTime()*20f)
						);
				weaponScaleY = MathUtils.lerp(weaponScaleY, 2f, Gdx.graphics.getDeltaTime()*3);

				//In case of low framerate skip, unlikely
				if(!didPlayAudio){
					didPlayAudio = true;
					Game.audio.play("weapon");
				}
			}
		} else {
			weaponRotation = MathUtils.lerp(weaponRotation, defaultWeaponRotation, Gdx.graphics.getDeltaTime()*5f);
			weaponPosition.set(
					MathUtils.lerp(weaponPosition.x, 0, Gdx.graphics.getDeltaTime()*10f),
					MathUtils.lerp(weaponPosition.y, 0, Gdx.graphics.getDeltaTime()*10f)
					);
			weaponScaleY = MathUtils.lerp(weaponScaleY, 1f, Gdx.graphics.getDeltaTime()*10);
		}

		if (attackAnimation < 0.3f && !didAttack) {
			didAttack = true;
			checkAttackHit();

			if (Settings.PLAYER_SHOOTING) {
				Entity b = new ChaosBolt(this, this.position, camera.direction);
				Game.entityManager.add(b);
			}
		}
	}


	private void checkAttackHit() {
		IDamagable closest = null;
		float dist = 0f;

		Vector3 tmp = new Vector3();

		for (Entity ent : Game.entityManager.getEntities()) {
			if(ent instanceof IAttackable == false) {
				continue;
			}
			if(ent instanceof IDamagable == false) {
				continue;
			}

			tmp.set(position).mulAdd(camera.direction, Game.UNIT*.75f);

			if(Game.collision.hitCircle(ent.getPosition(), tmp, Game.UNIT*.75f)){
				float d = position.dst2(ent.getPosition());
				if(closest == null || d<dist){
					dist = d;
					closest = (IDamagable)ent;
				}
			}

		}

		if (closest != null) {
			closest.damaged(1, this.camera.direction);
		}

	}

	private void gravity() {
		gravity -= gravityScale*Gdx.graphics.getDeltaTime();
		position.y += gravity*Gdx.graphics.getDeltaTime();

		position.y = Math.max(0, position.y);
		position.y = Math.min(0.8f*Game.UNIT-playerHeight, position.y);

		onGround = (position.y == 0);

		if (onGround) {
			if (Gdx.input.isKeyPressed(Input.Keys.SPACE)) {
				gravity = jumpScale;
			} else {
				gravity = 0f;
			}
		}
	}


	private void move() {
		//showPosition();
		float dt = Gdx.graphics.getDeltaTime();

		moveVector.setZero();

		//Movement
		if (Gdx.input.isKeyPressed(Input.Keys.W)) {
			tmpVector.set(camera.direction);
			tmpVector.y = 0;
			moveVector.add(tmpVector.nor().scl(dt * moveSpeed));
		} else if (Gdx.input.isKeyPressed(Input.Keys.S)) {
			tmpVector.set(camera.direction);
			tmpVector.y = 0;
			moveVector.add(tmpVector.nor().scl(dt * -moveSpeed));
		}
		if (Gdx.input.isKeyPressed(Input.Keys.A)){
			tmpVector.set(camera.direction).crs(camera.up);
			tmpVector.y = 0;
			moveVector.add(tmpVector.nor().scl(dt * -moveSpeed));
		} else if (Gdx.input.isKeyPressed(Input.Keys.D)){
			tmpVector.set(camera.direction).crs(camera.up);
			tmpVector.y = 0;
			moveVector.add(tmpVector.nor().scl(dt * moveSpeed));
		}

		if (moveVector.len2() > 0) {
			float colX = moveVector.x==0 ? 0 : (moveVector.x>0 ? 1 : -1);
			float colZ = moveVector.z==0 ? 0 : (moveVector.z>0 ? 1 : -1);

			if (world.getMapSquareAt(position.x + moveVector.x + colX * colliderSize, position.z) == World.NOTHING) {
				position.add(moveVector.x, 0, 0);
			}
			if (world.getMapSquareAt(position.x, position.z + moveVector.z + colZ * colliderSize) == World.NOTHING) {
				position.add(0, 0, moveVector.z);
			}
		}

		camera.position.set(position.x, position.y + playerHeight, position.z);

		if (moveVector.len2() > 0) {
			footstepTimer += Gdx.graphics.getDeltaTime();

			if (footstepTimer > 0.45f) {
				footstepTimer -= 0.45f;
				Game.audio.play("step");
			}
		}
		//showPosition();
	}


	private void showPosition() {
		Settings.p("Player pos: " + this.position);
	}


	public void render(SpriteBatch batch){
		weaponSprite.draw(batch);
	}


	public void renderUI(SpriteBatch batch, BitmapFont font) {
		//font.draw(batch, "Test", Gdx.graphics.getWidth() / 2 - 32, Gdx.graphics.getHeight() / 2 + 50/8);

		if (interactTarget != null) {
			String str = interactTarget.getInteractText(this);
			int w2 = str.length() * 8;
			font.setColor(1,1,1,1);
			font.draw(batch, str, Gdx.graphics.getWidth() / 2 - w2, Gdx.graphics.getHeight() / 2 + 50/8);
		}

		int sx = Gdx.graphics.getWidth()/2 - health*18;
		for (int i = 0; i < inventory.keys; i++) {
			batch.draw(Game.art.items[0][0], 10 + i*50, Gdx.graphics.getHeight()-40, 48, 48);
		}


		for (int i = 0; i < health; i++) {
			batch.draw(heart, sx + i*36, Gdx.graphics.getHeight()-40, 32, 32);
		}

		if (hurtTimer > 0 && (int)(hurtTimer*5)%2 == 0) {
			batch.setColor(1,1,1,.25f);
			batch.draw(hurtTexture, 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
			batch.setColor(1,1,1,1);
		}

	}


	@Override
	public int getHealth() {
		return health;
	}


	@Override
	public void damaged(int amt, Vector3 dir) {
		health -= amt;

		hurtTimer = 1.5f;
		Game.audio.play("player_hurt");
	}


	public void resetHealth() {
		this.health = this.max_health;
	}

}

