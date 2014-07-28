package com.dsatab.activity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.LightingColorFilter;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.DrawerLayout;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.dsatab.DsaTabApplication;
import com.dsatab.R;
import com.dsatab.TabInfo;
import com.dsatab.data.Hero;
import com.dsatab.data.HeroConfiguration;
import com.dsatab.data.HeroFileInfo;
import com.dsatab.data.HeroLoaderTask;
import com.dsatab.data.Probe;
import com.dsatab.data.adapter.TabDrawerAdapter;
import com.dsatab.data.adapter.TabDrawerAdapter.DrawerItem;
import com.dsatab.data.adapter.TabDrawerAdapter.DrawerItemType;
import com.dsatab.fragment.AttributeListFragment;
import com.dsatab.fragment.BaseFragment;
import com.dsatab.fragment.DiceSliderFragment;
import com.dsatab.util.Debug;
import com.dsatab.util.Util;
import com.dsatab.view.ChangeLogDialog;
import com.dsatab.view.listener.ShakeListener;
import com.gandulf.guilib.util.ResUtil;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

public class DsaTabActivity extends BaseFragmentActivity implements LoaderManager.LoaderCallbacks<Hero>,
		OnSharedPreferenceChangeListener, ListView.OnItemClickListener {

	public static boolean newsShown = false;

	private static final int DRAWER_ID_NONE = -1;
	private static final int DRAWER_ID_HEROES = -2;
	private static final int DRAWER_ID_ITEMS = -3;
	private static final int DRAWER_ID_SETTINGS = -4;
	private static final int DRAWER_ID_TABS = -5;

	protected static final String INTENT_TAB_INFO = "tabInfo";

	public static final String PREF_LAST_HERO = "LAST_HERO_JSON";

	private static final String KEY_HERO_PATH = "HERO_PATH";

	public static final int ACTION_PREFERENCES = 1000;
	public static final int ACTION_ADD_MODIFICATOR = 1003;
	protected static final int ACTION_CHOOSE_HERO = 1004;
	public static final int ACTION_EDIT_MODIFICATOR = 1005;
	public static final int ACTION_EDIT_TABS = 1006;
	public static final int ACTION_EDIT_NOTES = 1007;
	public static final int ACTION_EDIT_CUSTOM_PROBES = 1008;

	private static final String KEY_TAB_INFO = "tabInfo";

	protected SharedPreferences preferences;

	private AttributeListFragment attributeFragment;
	private DiceSliderFragment diceSliderFragment;

	private ShakeListener mShaker;

	private ViewGroup fragmentContainerLeft;
	private ViewGroup fragmentContainerRight;

	private TabInfo tabInfo;

	private View loadingView;

	private DrawerLayout mDrawerLayout;
	private ListView mDrawerList;
	private ActionBarDrawerToggle mDrawerToggle;
	private TabDrawerAdapter tabDrawerAdapter;

	public Hero getHero() {
		return DsaTabApplication.getInstance().getHero();
	}

	public final void loadHero(HeroFileInfo heroPath) {

		if (getHero() != null && preferences.getBoolean(DsaTabPreferenceActivity.KEY_AUTO_SAVE, true)) {
			DsaTabApplication.getInstance().saveHero(this);
		}

		loadingView.setVisibility(View.VISIBLE);
		loadingView.startAnimation(AnimationUtils.loadAnimation(this, android.R.anim.fade_in));

		Bundle args = new Bundle();
		args.putSerializable(KEY_HERO_PATH, heroPath);
		getSupportLoaderManager().restartLoader(0, args, this);
	}

	@Override
	public boolean onSearchRequested() {
		Bundle appData = new Bundle();
		appData.putParcelable(SearchableActivity.INTENT_TAB_INFO, tabInfo);
		startSearch(null, false, appData, false);
		return true;
	}

	private void updatePage(TabInfo tabInfo, FragmentTransaction ft) {

		if (diceSliderFragment != null && diceSliderFragment.isAdded() && diceSliderFragment.getView() != null) {
			if (tabInfo != null && tabInfo.isDiceSlider()) {
				ft.show(diceSliderFragment);

				// if (diceSliderFragment.getView().getVisibility() == View.GONE) {
				// diceSliderFragment.getView().setVisibility(View.VISIBLE);
				//
				// Animation animation = AnimationUtils.loadAnimation(this, R.anim.slide_in_bottom);
				// diceSliderFragment.getView().startAnimation(animation);
				// }

			} else {
				ft.hide(diceSliderFragment);

				// if (diceSliderFragment.getView().getVisibility() == View.VISIBLE) {
				// Animation animation = AnimationUtils.loadAnimation(this, R.anim.slide_out_bottom);
				// diceSliderFragment.getView().startAnimation(animation);
				// diceSliderFragment.getView().setVisibility(View.GONE);
				// }
			}
		}

		if (attributeFragment != null && attributeFragment.isAdded() && attributeFragment.getView() != null) {
			if (tabInfo != null && tabInfo.isAttributeList()) {

				ft.show(attributeFragment);
				// if (attributeFragment.getView().getVisibility() == View.GONE) {
				// attributeFragment.getView().setVisibility(View.VISIBLE);
				//
				// Animation animation = AnimationUtils.loadAnimation(this, R.anim.slide_in_top);
				// attributeFragment.getView().startAnimation(animation);
				// }

			} else {
				ft.hide(attributeFragment);
				// if (attributeFragment.getView().getVisibility() == View.VISIBLE) {
				// Animation animation = AnimationUtils.loadAnimation(this, R.anim.slide_out_top);
				// attributeFragment.getView().startAnimation(animation);
				// attributeFragment.getView().setVisibility(View.GONE);
				// }
			}
		}

		if (tabInfo != null) {
			getSupportActionBar().setTitle(tabInfo.getTitle());
			if (tabInfo.getIconUri() != null) {
				Drawable d = ResUtil.getDrawableByUri(this, tabInfo.getIconUri());
				if (d != null) {
					d = d.mutate();
					d.setBounds(0, 0, 200, 200);
					if (tabInfo.getColor() != Color.TRANSPARENT) {
						d.setColorFilter(new LightingColorFilter(tabInfo.getColor(), 1));
					}
					getSupportActionBar().setIcon(d);
				} else {
					getSupportActionBar().setIcon(R.drawable.icon);
				}
			} else {
				getSupportActionBar().setIcon(R.drawable.icon);
			}
		} else {
			getSupportActionBar().setIcon(R.drawable.icon);
		}

	}

	private void initHero() {
		// in case of orientation change the hero is already loaded, just recreate the menu etc...
		if (DsaTabApplication.getInstance().getHero() != null) {
			onHeroLoaded(DsaTabApplication.getInstance().getHero());
		} else {
			String heroFileInfoJson = preferences.getString(PREF_LAST_HERO, null);
			if (heroFileInfoJson != null) {
				try {
					HeroFileInfo fileInfo = new HeroFileInfo(new JSONObject(heroFileInfoJson));
					loadHero(fileInfo);
				} catch (IllegalArgumentException e) {
					Debug.error(e);
					showHeroChooser();
				} catch (JSONException e) {
					Debug.error(e);
					showHeroChooser();
				}
			} else {
				showHeroChooser();
			}
		}
	}

	@Override
	public Loader<Hero> onCreateLoader(int id, Bundle args) {
		// Debug.verbose("Creating loader for " + args.getString(KEY_HERO_PATH));
		return new HeroLoaderTask(this, (HeroFileInfo) args.getSerializable(KEY_HERO_PATH));
	}

	@Override
	public void onLoadFinished(Loader<Hero> loader, Hero hero) {
		loadingView.startAnimation(AnimationUtils.loadAnimation(this, android.R.anim.fade_out));
		loadingView.setVisibility(View.GONE);

		// Swap the new cursor in. (The framework will take care of closing the
		// old cursor once we return.)
		if (loader instanceof HeroLoaderTask) {
			HeroLoaderTask heroLoader = (HeroLoaderTask) loader;

			if (heroLoader.getException() != null) {
				Toast.makeText(this, heroLoader.getException().getLocalizedMessage(), Toast.LENGTH_SHORT).show();
			}
		}
		DsaTabApplication.getInstance().setHero(hero);

		if (hero != null) {
			checkHsVersion(hero);
		}
		onHeroLoaded(hero);
	}

	protected boolean checkHsVersion(Hero hero) {

		if (TextUtils.isEmpty(hero.getFileInfo().getVersion())) {
			Toast.makeText(this, getString(R.string.hero_loaded, hero.getName()), Toast.LENGTH_SHORT).show();
			return false;
		}

		int version = hero.getFileInfo().getVersionInt();

		if (version < 0) {
			Toast.makeText(
					this,
					"Warnung: Unbekannte Helden-Software Version. Es kann keine vollständige Kompatibilität gewährleistet werden.",
					Toast.LENGTH_LONG).show();
			return false;
		} else if (version < DsaTabApplication.HS_VERSION_INT) {
			Toast.makeText(this,
					"Warnung: Die Helden Xml Datei wurde nicht mit der aktuellen Helden-Software erstellt.",
					Toast.LENGTH_LONG).show();
			return false;
		} else if (version > DsaTabApplication.HS_VERSION_INT) {
			Toast.makeText(this, "Hinweis: DsaTab wurde noch nicht an die aktuellste Helden-Software angepasst.",
					Toast.LENGTH_LONG).show();
			return false;
		} else {
			Toast.makeText(this, getString(R.string.hero_loaded, hero.getName()), Toast.LENGTH_SHORT).show();
			return true;
		}
	}

	@Override
	public void onLoaderReset(Loader<Hero> loader) {
		// This is called when the last Cursor provided to onLoadFinished()
		// above is about to be closed. We need to make sure we are no
		// longer using it.
		// mAdapter.swapCursor(null);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onActivityResult(int, int, android.content.Intent)
	 */
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {

		if (requestCode == ACTION_CHOOSE_HERO) {

			if (resultCode == RESULT_OK) {
				HeroFileInfo herofileInfo = (HeroFileInfo) data
						.getSerializableExtra(HeroChooserActivity.INTENT_NAME_HERO_FILE_INFO);
				Debug.verbose("HeroChooserActivity returned with path:" + herofileInfo);
				loadHero(herofileInfo);
			} else if (resultCode == RESULT_CANCELED) {
				if (getHero() == null) {
					finish();
				}
			}
		} else if (requestCode == ACTION_PREFERENCES) {

			SharedPreferences preferences = DsaTabApplication.getPreferences();

			if (preferences.getBoolean(DsaTabPreferenceActivity.KEY_PROBE_SHAKE_ROLL_DICE, false)) {
				registerShakeDice();
			} else {
				unregisterShakeDice();
			}
		} else if (requestCode == ACTION_EDIT_TABS) {
			if (resultCode == Activity.RESULT_OK) {
				setupNavigationDrawer();
			}
		}

		// notify other listeners (fragments, heroes)
		for (int i = 0; i < TabInfo.MAX_TABS_PER_PAGE; i++) {
			if (getCurrentFragment(i) != null) {
				getCurrentFragment(i).onActivityResult(requestCode, resultCode, data);
			}
		}

		super.onActivityResult(requestCode, resultCode, data);
	}

	protected Fragment getCurrentFragment(int index) {
		if (tabInfo != null) {
			return getSupportFragmentManager().findFragmentByTag(tabInfo.getId().toString() + index);
		} else {
			return null;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		setTheme(DsaTabApplication.getInstance().getCustomTheme());
		applyPreferencesToTheme();
		super.onCreate(savedInstanceState);

		preferences = DsaTabApplication.getPreferences();

		getSupportActionBar().setDisplayShowTitleEnabled(true);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		getSupportActionBar().setHomeButtonEnabled(true);

		DsaTabApplication.getPreferences().registerOnSharedPreferenceChangeListener(this);

		// start tracing to "/sdcard/calc.trace"
		// android.os.Debug.startMethodTracing("dsatab");

		setContentView(R.layout.main_tab_view);

		fragmentContainerLeft = (ViewGroup) findViewById(R.id.pane_left);
		fragmentContainerRight = (ViewGroup) findViewById(R.id.pane_right);
		loadingView = findViewById(R.id.loading);

		SlidingUpPanelLayout slidingUpPanelLayout = (SlidingUpPanelLayout) findViewById(R.id.slidepanel);

		String orientation = preferences.getString(DsaTabPreferenceActivity.KEY_SCREEN_ORIENTATION,
				DsaTabPreferenceActivity.DEFAULT_SCREEN_ORIENTATION);

		Configuration configuration = getResources().getConfiguration();

		if (savedInstanceState != null) {
			tabInfo = savedInstanceState.getParcelable(KEY_TAB_INFO);
		}

		if (tabInfo == null && getIntent() != null) {
			tabInfo = getIntent().getParcelableExtra(INTENT_TAB_INFO);
		}

		attributeFragment = (AttributeListFragment) getSupportFragmentManager().findFragmentByTag(
				AttributeListFragment.TAG);

		diceSliderFragment = (DiceSliderFragment) getSupportFragmentManager().findFragmentByTag(DiceSliderFragment.TAG);
		diceSliderFragment.setSlidingUpPanelLayout(slidingUpPanelLayout);

		// Debug.verbose("onCreate Orientation =" + configuration.orientation);
		if (DsaTabPreferenceActivity.SCREEN_ORIENTATION_LANDSCAPE.equals(orientation)
				&& configuration.orientation != Configuration.ORIENTATION_LANDSCAPE) {
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
			return;
		} else if (DsaTabPreferenceActivity.SCREEN_ORIENTATION_PORTRAIT.equals(orientation)
				&& configuration.orientation != Configuration.ORIENTATION_PORTRAIT) {
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
			return;
		} else if (DsaTabPreferenceActivity.SCREEN_ORIENTATION_AUTO.equals(orientation)
				&& getRequestedOrientation() != ActivityInfo.SCREEN_ORIENTATION_SENSOR
				&& getRequestedOrientation() != ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED) {
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
			return;
		}
		initDrawer();
		initHero();
		setupNavigationDrawer();
		showNewsInfoPopup();
	}

	private void initDrawer() {
		mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
		mDrawerList = (ListView) findViewById(R.id.left_drawer);

		tabDrawerAdapter = new TabDrawerAdapter(this, new ArrayList<DrawerItem>());
		// Set the adapter for the list view
		mDrawerList.setAdapter(tabDrawerAdapter);

		// Set the list's click listener
		mDrawerList.setOnItemClickListener(this);
		mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);

		mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, R.drawable.ic_drawer, R.string.drawer_open,
				R.string.drawer_close) {

			/** Called when a drawer has settled in a completely closed state. */
			public void onDrawerClosed(View view) {
				super.onDrawerClosed(view);
				// getActionBar().setTitle(getS);
			}

			/** Called when a drawer has settled in a completely open state. */
			public void onDrawerOpened(View drawerView) {
				super.onDrawerOpened(drawerView);
				// getActionBar().setTitle(mDrawerTitle);
			}
		};

		// Set the drawer toggle as the DrawerListener
		mDrawerLayout.setDrawerListener(mDrawerToggle);

	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		// Sync the toggle state after onRestoreInstanceState has occurred.
		if (mDrawerToggle != null) {
			mDrawerToggle.syncState();
		}
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		if (mDrawerToggle != null) {
			mDrawerToggle.onConfigurationChanged(newConfig);
		}
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

		DrawerItem item = (DrawerItem) parent.getItemAtPosition(position);

		switch (item.getId()) {
		case DRAWER_ID_HEROES:
			showHeroChooser();
			break;
		case DRAWER_ID_ITEMS:
			ItemsActivity.view(this);
			break;
		case DRAWER_ID_SETTINGS:
			DsaTabPreferenceActivity.startPreferenceActivity(this);
			break;
		case DRAWER_ID_TABS:
			if (getHero() != null) {
				TabEditActivity.edit(this, getHeroConfiguration().getTabs().indexOf(tabInfo), ACTION_EDIT_TABS);
			}
			break;
		default:
			showTab(item.getId());
			break;
		}

		mDrawerLayout.closeDrawer(mDrawerList);
	}

	/**
	 * 
	 */
	private void setupNavigationDrawer() {
		getSupportActionBar();
		List<TabInfo> tabs;
		if (getHeroConfiguration() != null)
			tabs = getHeroConfiguration().getTabs();
		else
			tabs = Collections.emptyList();

		tabDrawerAdapter.clear();

		if (getHero() != null) {
			tabDrawerAdapter.add(new DrawerItem(DRAWER_ID_NONE, getHero().getName(), getHero().getPortraitUri(),
					Color.RED, DrawerItemType.Profile));
		}
		if (!tabs.isEmpty()) {
			int tabIndex = 0;
			for (TabInfo tabInfo : tabs) {
				tabDrawerAdapter.add(new DrawerItem(tabIndex++, tabInfo));
			}
		}

		tabDrawerAdapter.add(new DrawerItem("System"));
		if (getHero() != null) {
			tabDrawerAdapter.add(new DrawerItem(DRAWER_ID_TABS, getString(R.string.option_edit_tabs),
					R.drawable.ic_menu_moreoverflow, Color.TRANSPARENT, DrawerItemType.System));
		}
		tabDrawerAdapter.add(new DrawerItem(DRAWER_ID_HEROES, "HELDEN", R.drawable.dsa_group, Color.TRANSPARENT,
				DrawerItemType.System));
		tabDrawerAdapter.add(new DrawerItem(DRAWER_ID_ITEMS, "GEGENSTÄNDE", R.drawable.dsa_items, Color.TRANSPARENT,
				DrawerItemType.System));
		tabDrawerAdapter.add(new DrawerItem(DRAWER_ID_SETTINGS, "EINSTELLUNGEN", R.drawable.ic_menu_preferences,
				Color.TRANSPARENT, DrawerItemType.System));
	}

	private void showNewsInfoPopup() {
		if (newsShown)
			return;

		ChangeLogDialog logDialog = new ChangeLogDialog(this);
		logDialog.show();
		newsShown = true;
	}

	private HeroConfiguration getHeroConfiguration() {
		HeroConfiguration tabConfig = null;
		if (getHero() != null) {
			tabConfig = getHero().getHeroConfiguration();
		}
		return tabConfig;
	}

	public void notifyTabsChanged(TabInfo tabInfo) {
		this.tabInfo = refreshTabInfo();
		setupNavigationDrawer();
		showTab(tabInfo, true);
	}

	/**
	 * 
	 */
	private TabInfo refreshTabInfo() {

		if (getHeroConfiguration() == null || getHeroConfiguration().getTabs().isEmpty()) {
			tabInfo = null;
			return null;
		}

		List<TabInfo> tabs = getHeroConfiguration().getTabs();

		// if we have an existing tabinfo check if it's upto date
		if (tabInfo != null) {
			// check wether tabinfo is uptodate (within current tabconfig
			if (tabs.contains(tabInfo)) {
				return tabInfo;
			}

			// look for tabinfo with same activities
			for (TabInfo tab : tabs) {

				if (Util.equalsOrNull(tab.getActivityClazz(0), tabInfo.getActivityClazz(0))
						&& Util.equalsOrNull(tab.getActivityClazz(1), tabInfo.getActivityClazz(1))) {
					return tab;
				}
			}

			// if we have portraitmode and a secondary clazz this means we
			// switched from landscape here, in this case we look for a tabinfo
			// with the primary activityclass and use this one
			if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT
					&& tabInfo.getActivityClazz(1) != null) {

				Class<? extends BaseFragment> activityClazz = tabInfo.getActivityClazz(0);
				if (activityClazz != null) {
					for (TabInfo tab : tabs) {
						if (activityClazz.equals(tab.getActivityClazz(0))) {
							return tab;
						}
					}
				}
			}

			// if we have landscape mode and a empty secondary activity look for
			// one with one
			if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE
					&& tabInfo.getActivityClazz(1) == null) {

				Class<? extends BaseFragment> activityClazz = tabInfo.getActivityClazz(0);
				if (activityClazz != null) {
					for (TabInfo tab : tabs) {
						if (activityClazz.equals(tab.getActivityClazz(0))
								|| activityClazz.equals(tab.getActivityClazz(1))) {
							return tab;

						}
					}
				}
			}
		}

		// last resort set tabinfo to first one if no matching one is found
		return tabs.get(0);
	}

	public boolean showTab(int index) {
		if (getHeroConfiguration() != null && index >= 0 && index < getHeroConfiguration().getTabs().size()) {
			return showTab(getHeroConfiguration().getTab(index), false);
		} else {
			return false;
		}
	}

	protected boolean showTab(TabInfo newTabInfo, boolean forceRefresh) {

		if (newTabInfo != null && getHeroConfiguration() != null && (forceRefresh || !newTabInfo.equals(tabInfo))) {

			this.tabInfo = newTabInfo;
			String tag = newTabInfo.getId().toString();

			FragmentTransaction ft = getSupportFragmentManager().beginTransaction();

			for (int i = 0; i < TabInfo.MAX_TABS_PER_PAGE; i++) {
				int containerId;
				ViewGroup fragmentContainer;
				if (i == 0) {
					containerId = R.id.pane_left;
					fragmentContainer = fragmentContainerLeft;
				} else {
					containerId = R.id.pane_right;
					fragmentContainer = fragmentContainerRight;
				}

				// detach old fragment
				Fragment oldFragment = getSupportFragmentManager().findFragmentById(containerId);
				if (oldFragment != null) {
					ft.detach(oldFragment);
				}

				Fragment fragment = getSupportFragmentManager().findFragmentByTag(tag + i);
				if (fragment == null) {
					fragment = newTabInfo.getFragment(i);
					if (fragment != null) {
						Debug.verbose("Creating new fragment and adding it" + tag + " " + fragment);

						fragmentContainer.setVisibility(View.VISIBLE);
						ft.add(containerId, fragment, tag + i);
					} else {
						fragmentContainer.setVisibility(View.GONE);
					}
				} else {
					Debug.verbose("Reusing fragment " + tag + " " + fragment);
					fragmentContainer.setVisibility(View.VISIBLE);
					ft.attach(fragment);
				}

			}
			// ft.addToBackStack(tag);

			updatePage(tabInfo, ft);
			ft.commitAllowingStateLoss();

			return true;
		} else {
			return false;
		}
	}

	public void replaceFragment(String tag, Fragment oldFragment, Fragment newFragment) {
		FragmentTransaction ft = getSupportFragmentManager().beginTransaction();

		ViewGroup fragmentContainer = null;
		int containerId = 0;
		for (int i = 0; i < TabInfo.MAX_TABS_PER_PAGE; i++) {
			if (i == 0) {
				containerId = R.id.pane_left;
				fragmentContainer = fragmentContainerLeft;
			} else {
				containerId = R.id.pane_right;
				fragmentContainer = fragmentContainerRight;
			}

			// detach old fragment
			Fragment fragment = getSupportFragmentManager().findFragmentById(containerId);
			if (fragment != null && oldFragment == fragment) {
				ft.detach(oldFragment);
				break;
			}
		}

		if (newFragment != null) {
			fragmentContainer.setVisibility(View.VISIBLE);
			ft.add(containerId, newFragment, tag);
		} else {
			fragmentContainer.setVisibility(View.GONE);
		}

		ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
		ft.addToBackStack(tag);

		updatePage(null, ft);
		ft.commitAllowingStateLoss();

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onBackPressed()
	 */
	@Override
	public void onBackPressed() {
		if (diceSliderFragment != null && diceSliderFragment.isVisible() && diceSliderFragment.isSliderOpened()) {
			diceSliderFragment.collapsePanel();
		} else {
			super.onBackPressed();
		}
	}

	protected void onHeroLoaded(Hero hero) {

		if (hero == null) {
			Toast.makeText(this, R.string.message_load_hero_failed, Toast.LENGTH_LONG).show();
			return;
		}

		setupNavigationDrawer();
		showTab(refreshTabInfo(), true);

		if (attributeFragment != null && attributeFragment.isAdded()) {
			attributeFragment.loadHero(hero);
		}

		if (diceSliderFragment != null && diceSliderFragment.isAdded()) {
			diceSliderFragment.loadHero(hero);
		}
	}

	public boolean checkProbe(Probe probe) {
		return checkProbe(probe, true);
	}

	public boolean checkProbe(Probe probe, boolean autoRoll) {
		if (diceSliderFragment != null) {
			if (probe != null && getHero() != null) {
				diceSliderFragment.checkProbe(getHero(), probe, autoRoll);
				return true;
			}
		}
		return false;
	}

	private void unregisterShakeDice() {
		try {
			if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_SENSOR_ACCELEROMETER)) {
				if (mShaker != null) {
					mShaker.setOnShakeListener(null);
					mShaker = null;
				}
			}
		} catch (UnsupportedOperationException e) {
			mShaker = null;
			Debug.warning(e);
		}
	}

	private void registerShakeDice() {
		try {
			if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_SENSOR_ACCELEROMETER)) {
				if (mShaker == null) {
					final Vibrator vibe = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
					mShaker = new ShakeListener(this);
					mShaker.setOnShakeListener(new ShakeListener.OnShakeListener() {
						@Override
						public void onShake() {
							vibe.vibrate(100);
							if (diceSliderFragment != null) {
								diceSliderFragment.expandPanel();
								diceSliderFragment.rollDice20();
							}
						}
					});

				}
			}
		} catch (UnsupportedOperationException e) {
			mShaker = null;
			Debug.warning(e);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onDestroy()
	 */
	@Override
	protected void onDestroy() {
		unregisterShakeDice();

		DsaTabApplication.getPreferences().unregisterOnSharedPreferenceChangeListener(this);

		if (getHero() != null && preferences.getBoolean(DsaTabPreferenceActivity.KEY_AUTO_SAVE, true)) {
			DsaTabApplication.getInstance().saveHero(this);
		}

		super.onDestroy();
	}

	@Override
	protected void onPause() {
		try {
			if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_SENSOR_ACCELEROMETER)) {
				if (mShaker != null)
					mShaker.pause();
			}
		} catch (UnsupportedOperationException e) {
			mShaker = null;
			Debug.warning(e);
		}

		super.onPause();
	}

	@Override
	protected void onResume() {
		try {
			if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_SENSOR_ACCELEROMETER)) {
				if (preferences.getBoolean(DsaTabPreferenceActivity.KEY_PROBE_SHAKE_ROLL_DICE, false)) {
					if (mShaker == null)
						registerShakeDice();
					else
						mShaker.resume();
				}
			}
		} catch (UnsupportedOperationException e) {
			mShaker = null;
			Debug.warning(e);
		}
		super.onResume();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		MenuItem item;

		item = menu.add(Menu.NONE, R.id.option_save_hero, Menu.NONE, R.string.option_save_hero);
		MenuItemCompat.setShowAsAction(item, MenuItemCompat.SHOW_AS_ACTION_NEVER);
		item.setIcon(R.drawable.ic_menu_save);

		return super.onCreateOptionsMenu(menu);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.actionbarsherlock.app.SherlockFragmentActivity#onPrepareOptionsMenu (com.actionbarsherlock.view.Menu)
	 */
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		MenuItem item = menu.findItem(R.id.option_set);
		if (item != null) {
			item.setEnabled(getHero() != null);
			if (getHero() != null) {
				switch (getHero().getActiveSet()) {
				case 0:
					item.setIcon(Util.getThemeResourceId(this, R.attr.imgBarSet1));
					break;
				case 1:
					item.setIcon(Util.getThemeResourceId(this, R.attr.imgBarSet2));
					break;
				case 2:
					item.setIcon(Util.getThemeResourceId(this, R.attr.imgBarSet3));
					break;
				}
			}
		}

		item = menu.findItem(R.id.option_save_hero);
		if (item != null) {
			item.setVisible(!preferences.getBoolean(DsaTabPreferenceActivity.KEY_AUTO_SAVE, true));
			item.setEnabled(getHero() != null);
		}

		item = menu.findItem(R.id.option_tabs);
		if (item != null) {
			item.setEnabled(getHero() != null);
		}

		item = menu.findItem(R.id.option_set);
		if (getHero() != null) {

			if (item != null) {
				item.setEnabled(true);
			}
			switch (getHero().getActiveSet()) {
			case 0:
				if (menu.findItem(R.id.option_set1) != null) {
					menu.findItem(R.id.option_set1).setChecked(true);
				}
				break;
			case 1:
				if (menu.findItem(R.id.option_set2) != null) {
					menu.findItem(R.id.option_set2).setChecked(true);
				}
				break;
			case 2:
				if (menu.findItem(R.id.option_set3) != null) {
					menu.findItem(R.id.option_set3).setChecked(true);
				}
				break;
			default:
				break;
			}
		} else {
			if (item != null) {
				item.setEnabled(false);
			}
		}

		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		// Pass the event to ActionBarDrawerToggle, if it returns
		// true, then it has handled the app icon touch event
		if ((item != null) && (item.getItemId() == 16908332)) {
			if (this.mDrawerLayout.isDrawerVisible(8388611))
				this.mDrawerLayout.closeDrawer(8388611);
			else {
				this.mDrawerLayout.openDrawer(8388611);
			}
			return true;
		}

		switch (item.getItemId()) {
		case R.id.option_load_hero:
			showHeroChooser();
			return true;
		case R.id.option_save_hero:
			if (getHero() != null) {
				DsaTabApplication.getInstance().saveHero(this);
			}
			return true;
		case R.id.option_set1:
			if (getHero() != null) {
				getHero().setActiveSet(0);
				item.setChecked(true);
			}
			return true;
		case R.id.option_set2:
			if (getHero() != null) {
				getHero().setActiveSet(1);
				item.setChecked(true);
			}
			return true;
		case R.id.option_set3:
			if (getHero() != null) {
				getHero().setActiveSet(2);
				item.setChecked(true);
			}
			return true;
		}

		return super.onOptionsItemSelected(item);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.support.v4.app.FragmentActivity#onSaveInstanceState(android.os .Bundle)
	 */
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		// super.onSaveInstanceState(outState);
		outState.putParcelable(KEY_TAB_INFO, tabInfo);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onRestoreInstanceState(android.os.Bundle)
	 */
	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		// super.onRestoreInstanceState(savedInstanceState);
		if (savedInstanceState.containsKey(KEY_TAB_INFO))
			tabInfo = savedInstanceState.getParcelable(KEY_TAB_INFO);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.content.SharedPreferences.OnSharedPreferenceChangeListener#
	 * onSharedPreferenceChanged(android.content.SharedPreferences, java.lang.String)
	 */
	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		// Debug.verbose(key + " changed");

		if (DsaTabPreferenceActivity.KEY_STYLE_BG_PATH.equals(key)) {
			applyPreferencesToTheme();
		}

		if (DsaTabPreferenceActivity.KEY_SCREEN_ORIENTATION.equals(key)) {
			String orientation = sharedPreferences.getString(DsaTabPreferenceActivity.KEY_SCREEN_ORIENTATION,
					DsaTabPreferenceActivity.DEFAULT_SCREEN_ORIENTATION);

			if (DsaTabPreferenceActivity.SCREEN_ORIENTATION_LANDSCAPE.equals(orientation)) {
				if (getRequestedOrientation() != ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
					// You need to check if your desired orientation isn't
					// already set because setting orientation restarts your
					// Activity which takes long
					setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
				}
			} else if (DsaTabPreferenceActivity.SCREEN_ORIENTATION_PORTRAIT.equals(orientation)) {
				if (getRequestedOrientation() != ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
					setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
				}
			} else if (DsaTabPreferenceActivity.SCREEN_ORIENTATION_AUTO.equals(orientation)) {
				if (getRequestedOrientation() != ActivityInfo.SCREEN_ORIENTATION_SENSOR) {
					setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
				}
			}
		}

		if (DsaTabPreferenceActivity.KEY_FULLSCREEN.equals(key)) {
			updateFullscreenStatus(preferences.getBoolean(DsaTabPreferenceActivity.KEY_FULLSCREEN, false));
		}

		// notify other listeners (fragments, heroes)
		for (int i = 0; i < TabInfo.MAX_TABS_PER_PAGE; i++) {
			if (getCurrentFragment(i) instanceof OnSharedPreferenceChangeListener) {
				((OnSharedPreferenceChangeListener) getCurrentFragment(i)).onSharedPreferenceChanged(sharedPreferences,
						key);
			}
		}

		if (attributeFragment != null && attributeFragment.isAdded())
			attributeFragment.onSharedPreferenceChanged(sharedPreferences, key);

		Hero hero = DsaTabApplication.getInstance().getHero();
		if (hero != null) {
			hero.onSharedPreferenceChanged(sharedPreferences, key);
		}
	}

	protected void showHeroChooser() {
		startActivityForResult(new Intent(DsaTabActivity.this, HeroChooserActivity.class), ACTION_CHOOSE_HERO);
	}
}
