package com.scs.lostinthegame;

import java.util.LinkedList;
import java.util.Random;

import com.badlogic.gdx.math.GridPoint2;

public class Maze {

	private static final char PASSAGE_CHAR = ' ';
	private static final char WALL_CHAR = 'W';
	public static final boolean WALL    = false;
	public static final boolean PASSAGE = !WALL;

	public final boolean map[][]; // wall == false!
	private final int width;
	private final int height;
	public GridPoint2 start_pos; 
	public GridPoint2 middle_pos; 
	public GridPoint2 end_pos; 

	public static void main(String args[]) {
		System.out.println(new Maze(20, 20));
	}


	public Maze(final int width, final int height) {
		this.width = width;
		this.height = height;
		this.map = new boolean[width][height];

		final LinkedList<int[]> frontiers = new LinkedList<int[]>();
		final Random random = new Random();
		int x = 1+random.nextInt(width-2);
		int y = 1+random.nextInt(height-2);
		frontiers.add(new int[]{x,y,x,y});

		while ( !frontiers.isEmpty() ){
			final int[] f = frontiers.remove( random.nextInt( frontiers.size() ) );
			x = f[2];
			y = f[3];
			if ( map[x][y] == WALL ) {
				map[f[0]][f[1]] = map[x][y] = PASSAGE;
				if ( x >= 2 && map[x-2][y] == WALL )
					frontiers.add( new int[]{x-1,y,x-2,y} );
				if ( y >= 2 && map[x][y-2] == WALL )
					frontiers.add( new int[]{x,y-1,x,y-2} );
				if ( x < width-2 && map[x+2][y] == WALL )
					frontiers.add( new int[]{x+1,y,x+2,y} );
				if ( y < height-2 && map[x][y+2] == WALL )
					frontiers.add( new int[]{x,y+1,x,y+2} );
			}
		}

		// Make outer walls actual walls
		for (int z=0 ; z<height ; z++) {
			for (x=0 ; x<width ; x++) {
				if (x == 0 || z == 0 || x >= width-1 || z >= height-1) {
					map[x][z] = WALL;
				} else {
					if (this.start_pos == null && map[x][z] != WALL) {
						start_pos = new GridPoint2(x, z);
					}
				}
			}
		}

		// Get middle pos
		for (int z=height/2 ; z<height ; z++) {
			for (x=width/2 ; x<width ; x++) {
				if (this.middle_pos == null && map[x][z] != WALL) {
					middle_pos = new GridPoint2(x, z);
					break;
				}
			}
		}

		// Get end pos
		for (int z=height-1 ; z>=0 ; z--) {
			for (x=width-1 ; x>=0; x--) {
				if (this.end_pos == null && map[x][z] != WALL) {
					end_pos = new GridPoint2(x, z);
					break;
				}
			}
		}

		System.out.println(this.toString());
	}

	@Override
	public String toString(){
		final StringBuffer b = new StringBuffer();
		for ( int y = 0; y < height; y++ ){
			for ( int x = 0; x < width; x++ ) {
				b.append( map[x][y] == WALL ? WALL_CHAR : PASSAGE_CHAR );
			}
			b.append( '\n' );
		}
		b.append( '\n' );
		return b.toString();
	}
}