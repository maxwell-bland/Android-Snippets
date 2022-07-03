package edu.sysnet.skimmer.bluetoothscanner.layout.devicelist;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import java.util.List;

import edu.sysnet.skimmer.bluetoothscanner.data.DeviceDatapoint;

public abstract class DeviceListAdapter extends ArrayAdapter<DeviceDatapoint> {
    protected Context context;
    private List<DeviceDatapoint> items;

    DeviceListAdapter(Context context, int resource, List<DeviceDatapoint> objects,
                      HitlistUtil hitlist) {
        super(context, resource, objects);
        this.context = context;
        this.items = objects;
        this.hitlist = hitlist;
    }

    /**
     * Updates a device in the list if it is new, otherwise adds a device to the list of items
     * we are concerned with
     *
     * @param point the new datapoint to add
     */
    public void updateDatapoint(DeviceDatapoint point) {
        /* NOTE: removed for privacy */
            items.add(0, point);

        this.notifyDataSetChanged();
    }

    /**
     * Creates the view for a single item in the device list.
     *
     * @param position    the position in the list of the item
     * @param convertView unused
     * @param parent      the parent of the item being displayed
     * @return the resulting xml view
     */
    @NonNull
    abstract public View getView(int position, View convertView, @NonNull ViewGroup parent);
}
