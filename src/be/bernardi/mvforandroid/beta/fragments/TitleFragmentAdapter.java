package be.bernardi.mvforandroid.beta.fragments;

import java.util.Map;

import com.viewpagerindicator.TitleProvider;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

public class TitleFragmentAdapter extends FragmentPagerAdapter implements TitleProvider {

	private Map<String, Fragment>	mScreens;

	public TitleFragmentAdapter(Map<String, Fragment> screenMap, FragmentManager fm) {
		super(fm);
		this.mScreens = screenMap;
	}

	@Override
	public String getTitle(int position) {
		return mScreens.keySet().toArray(new String[mScreens.size()])[position];
	}

	@Override
	public Fragment getItem(int position) {
		return mScreens.values().toArray(new Fragment[mScreens.size()])[position];
	}

	@Override
	public int getCount() {
		return mScreens.size();
	}

}