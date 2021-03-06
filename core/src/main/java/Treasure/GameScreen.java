package Treasure;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.Animation.PlayMode;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.MapObjects;
import com.badlogic.gdx.maps.objects.RectangleMapObject;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;

import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.graphics.OrthographicCamera;

import java.util.ArrayList;

/** First screen of the application. Displayed after the application is created. */
public class GameScreen extends BaseScreen {

	private PhysicsActor player;
	private BaseActor door;
	private BaseActor key;
	private boolean hasKey;

	private BaseActor baseCoin;
	private ArrayList<BaseActor> coinList;

	private ArrayList<BaseActor> wallList;
	private ArrayList<BaseActor> removeList;

	private int tileSize = 64;
	private int tileCountWidth = 20;
	private int tileCountHeight = 20;

	//calculate game world dimensions
	final int mapWidth = tileSize * tileCountWidth;
	final int mapHeight = tileSize * tileCountHeight;

	private TiledMap tiledMap;

	private OrthographicCamera tiledCamera;
	private OrthogonalTiledMapRenderer tiledMapRenderer;
	private int[] backgroundLayers = {0, 1};
	private int[] foregroundLayers = {2};

	public GameScreen(BaseGame g) {
		super(g);
	}

	@Override
	public void create() {

		player = new PhysicsActor();
		Texture playerTexture = new Texture(Gdx.files.internal("general-single.png"));
		player.storeAnimation("default", playerTexture);
		player.setEllipseBoundary();
		mainStage.addActor(player);

		key = new BaseActor();
		key.setTexture(new Texture(Gdx.files.internal("key.png")));
		key.setSize(36,24);
		key.setEllipseBoundary();
		mainStage.addActor(key);

		door = new BaseActor();
		door.setTexture(new Texture(Gdx.files.internal("door.png")));
		door.setRectangleBoundary();
		mainStage.addActor(door);

		baseCoin = new BaseActor();
		baseCoin.setTexture(new Texture(Gdx.files.internal("coin.png")));
		baseCoin.setEllipseBoundary();

		coinList = new ArrayList<BaseActor>();
		wallList = new ArrayList<BaseActor>();
		removeList = new ArrayList<BaseActor>();

		//set up tile map, renderer, and camera
		tiledMap = new TmxMapLoader().load("game_map.tmx");
		tiledMapRenderer = new OrthogonalTiledMapRenderer(tiledMap);
		tiledCamera = new OrthographicCamera();
		tiledCamera.setToOrtho(false, viewWidth, viewHeight);
		tiledCamera.update();

		MapObjects mapObjects = tiledMap.getLayers().get("ObjectData").getObjects();
		for (MapObject object : mapObjects) {
			String name = object.getName();
			RectangleMapObject rectangleMapObject = (RectangleMapObject) object;
			Rectangle r = rectangleMapObject.getRectangle();
			if (name != null) {
				switch (name) {
					case "player":
						player.setPosition(r.x, r.y);
						break;
					case "coin":
						BaseActor coin = baseCoin.clone();
						coin.setPosition(r.x, r.y);
						mainStage.addActor(coin);
						coinList.add(coin);
						break;
					case "door":
						door.setPosition(r.x, r.y);
						break;
					default:
						System.err.println("Unknown tilemap object: " + name);
				}
			}
		}

		mapObjects = tiledMap.getLayers().get("PhysicsData").getObjects();
		for (MapObject object : mapObjects){
			RectangleMapObject rectangleMapObject = (RectangleMapObject) object;
			Rectangle r = rectangleMapObject.getRectangle();

			BaseActor solid = new BaseActor();
			solid.setPosition(r.x, r.y);
			solid.setSize(r.width, r.height);
			solid.setRectangleBoundary();
			wallList.add(solid);
		}

	}

	@Override
	public void update(float dt) {

		float playerSpeed = 100;
		player.setVelocityXY(0, 0);

		if (Gdx.input.isKeyPressed(Keys.LEFT))
			player.setVelocityXY(- playerSpeed, 0);
		if (Gdx.input.isKeyPressed(Keys.RIGHT))
			player.setVelocityXY( playerSpeed, 0);
		if (Gdx.input.isKeyPressed(Keys.UP))
			player.setVelocityXY( 0, playerSpeed);
		if (Gdx.input.isKeyPressed(Keys.DOWN))
			player.setVelocityXY( 0, - playerSpeed);

		for (BaseActor wall :  wallList){

			player.overlaps(wall, true);
		}

		if ( key.getStage() != null && player.overlaps(key, false)){
			hasKey = true;
			removeList.add(key);
		}

		if (door.getStage() != null && player.overlaps(door, true)){
			if (hasKey)
				removeList.add(door);
		}

		for (BaseActor coin : coinList){
			if (player.overlaps(coin, false))
				removeList.add(coin);
		}

		for (BaseActor ba: removeList){
			ba.destroy();
		}

		// camera adjustment
		Camera mainCamera = mainStage.getCamera();

		// center camera on player
		mainCamera.position.x = player.getX() + player.getOriginX();
		mainCamera.position.y = player.getY() + player.getOriginY();

		// bound camera to layout
		mainCamera.position.x = MathUtils.clamp(mainCamera.position.x, viewWidth/2, mapWidth - viewWidth/2);
		mainCamera.position.y = MathUtils.clamp(mainCamera.position.y, viewHeight/2, mapHeight - viewHeight/2);
		mainCamera.update();

		// adjust tilemap camera to stay in sync with main camera
		tiledCamera.position.x = mainCamera.position.x;
		tiledCamera.position.y = mainCamera.position.y;
		tiledCamera.update();
		tiledMapRenderer.setView(tiledCamera);

	}

	//override the render method to interleave tilemap rendering
	@Override
	public void render(float dt) {
		uiStage.act(dt);

		//pause only gameplay events, not UI events
		if ( !isPaused()){
			mainStage.act(dt);
			update(dt);
		}

		//render
		Gdx.gl.glClearColor(0, 0, 0, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
		tiledMapRenderer.render(backgroundLayers);
		mainStage.draw();
		tiledMapRenderer.render(foregroundLayers);
		uiStage.draw();
	}

	@Override
	public boolean keyDown(int keycode) {
		if (keycode == Keys.P)
			togglePaused();
		if (keycode == Keys.R)
			game.setScreen(new GameScreen(game));
		return false;
	}
}