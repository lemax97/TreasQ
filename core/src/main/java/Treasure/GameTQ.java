package Treasure;

import com.badlogic.gdx.Game;

/** {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms. */
public class GameTQ extends Game {
	@Override
	public void create() {
		setScreen(new GameScreen());
	}
}