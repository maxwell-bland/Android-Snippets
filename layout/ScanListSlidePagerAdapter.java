package edu.sysnet.skimmer.bluetoothscanner.layout;

import android.content.Context;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import edu.sysnet.skimmer.bluetoothscanner.layout.devicelist.BtDeviceListAdapter;
import edu.sysnet.skimmer.bluetoothscanner.layout.devicelist.DeviceListFragment;
import edu.sysnet.skimmer.bluetoothscanner.layout.devicelist.WifiDeviceListAdapter;
import edu.sysnet.skimmer.bluetoothscanner.scan.ScanManager;
import edu.sysnet.skimmer.bluetoothscanner.scan.ScanManager.ScanType;
import edu.sysnet.skimmer.toggle.LabeledSwitch;

import static java.lang.Math.max;

/**
 * Handles sliding between list fragments given two list adapters.
 * <p>
 * NOTE: Please understand the memory differences between PagerAdapter, StatePagerAdapter, and
 * StatepagerAdapter
 */
public class ScanListSlidePagerAdapter extends FragmentStatePagerAdapter {
    private final DeviceScannerFragment view;
    // The current item viewed
    private ScanType currentItem;
    private final LabeledSwitch scanToggle;
    private ScanManager scanManager;
    private final DeviceListFragment btListFrag;
    private final DeviceListFragment wifiListFrag;
    private final Button clearListButton;

    /**
     * Creates the pager
     *
     * @param fm                    the fragment manager to associate to
     * @param btDeviceListAdapter
     * @param wifiDeviceListAdapter
     */
    ScanListSlidePagerAdapter(FragmentManager fm,
                              DeviceScannerFragment view,
                              LabeledSwitch scanToggle,
                              ScanManager scanManager,
                              DeviceListFragment btListFrag,
                              DeviceListFragment wifiListFrag,
                              Button clearListButton) {
        super(fm);
        this.view = view;
        this.scanToggle = scanToggle;
        this.scanManager = scanManager;
        this.btListFrag = btListFrag;
        this.wifiListFrag = wifiListFrag;
        this.clearListButton = clearListButton;
    }

    /**
     * Sets the primary item for the pager to view, fired on swipe event and changes the toggler
     * color
     *
     * @param container the viewgroup for the pager
     * @param position  the new position to show
     * @param object    the new object to set the primary item for
     */
    @Override
    public void setPrimaryItem(ViewGroup container, int position, Object object) {
        if (position == 0) {
            currentItem = ScanType.bluetooth;
            clearListButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    btListFrag.clear();
                    btListFrag.setBgAlpha();
                }
            });
        } else {
            currentItem = ScanType.wifi;
            clearListButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    wifiListFrag.clear();
                    wifiListFrag.setBgAlpha();
                }
            });
        }

        super.setPrimaryItem(container, position, object);
    }

    /**
     * Gets the item in a position in the pager
     *
     * @param position the position to grab
     * @return the fragment for the item
     */
    @Override
    public Fragment getItem(int position) {
        if (position == 0) {
            return btListFrag;
        } else {
            return wifiListFrag;
        }
    }

    /**
     * Gets the count of the total number of pages
     *
     * @return the count
     */
    @Override
    public int getCount() {
        return 2;
    }


}
