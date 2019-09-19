package net.evgiz.ld40.game.entity;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g3d.decals.Decal;
import net.evgiz.ld40.game.Game;
import net.evgiz.ld40.game.player.Player;
import net.evgiz.ld40.game.world.World;

public final class Door extends Entity {

    private Decal openDecal;

    private boolean locked = true;

    public Door(TextureRegion[][] tex, int x, int y) {
        super(tex, x, y, 0,1);

        interactable = true;
        openDecal = Decal.newDecal(tex[1][1], true);

        decalEntity.faceCamera = false;
        decalEntity.setRotation(-90f);
    }

    public void interact(Player player){
        if (locked && player.inventory.keys>0) {
            world.world[world_x + world_y * world.width] = 0;
            decalEntity.decal = openDecal;
            interactable = false;
            player.inventory.keys--;
            Game.audio.play("door");
        }
    }

    @Override
    public String getInteractText(Player player) {
        return (locked && player.inventory.keys == 0) ? "Locked" : "Open with Key (E)";
    }

}
