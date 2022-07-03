package edu.sysnet.skimmer.bluetoothscanner.layout.devicelist;

import android.graphics.Bitmap;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListView;


import edu.sysnet.skimmer.bluetoothscanner.R;

import static java.lang.Math.max;

/**
 * Holds the view for a device list
 */
public class DeviceListFragment extends Fragment {
    private Bitmap bgImgBM = null;
    private ImageView bgImage = null;
    private DeviceListAdapter devList;

    /**
     * Instantiates a new fragment with a given background image and device list
     *
     * @param backgroundImage the background image
     * @param devList         the device list
     * @return the new fragment
     */
    public static DeviceListFragment newInstance(Bitmap backgroundImage,
                                                 DeviceListAdapter devList) {
        DeviceListFragment res = new DeviceListFragment();
        res.devList = devList;
        res.bgImgBM = backgroundImage;
        return res;
    }

    /**
     * Automatically calculates and sets a new background transparency for the fragment
     */
    public void setBgAlpha() {
        if (bgImage != null) {
            bgImage.setAlpha((float)
                    max(0.5 - (devList.getCount() * 0.1), 0.1)
            );
        }
    }

    /**
     * Instantiates the view for the fragment
     *
     * @param inflater           to generate the view
     * @param container          the container the view is being put into
     * @param savedInstanceState the previous instance state of the fragment
     * @return the new view
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.device_list, container, false);
        // Set the background image
        bgImage = (ImageView) view.findViewById(R.id.listBackgroundImage);
        bgImage.setImageBitmap(bgImgBM);

        // Associate the device list
        // Instantiate the device lists
        final ListView btDeviceList = (ListView) view.findViewById(R.id.bluetoothDeviceList);
        btDeviceList.setAdapter(devList);
        // Under certain circumstances, the devList will not be initialized at this point
        try {
            setBgAlpha();
        } catch (Exception ignored) {
        }

        return view;
    }

    /**
     * Clears the list stored by this adapter
     */
    public void clear() {
        devList.clear();
    }
}
