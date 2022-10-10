package com.example.game15;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class GridAdapter extends BaseAdapter {

    private final Context mContext;

    public void setGridImages(Integer[] gridImages) {
        this.gridImages = gridImages;
    }

    private Integer[] gridImages;
    private Integer[] gridItems;
    private int cellSize;

    public void setGridItems(Integer[] gridItems) {
        this.gridItems = gridItems;
    }

    public GridAdapter(Context context, int cellSize) {
        this.mContext = context;
        gridImages = new Integer[16];
        this.cellSize = cellSize;
    }

    @Override
    public int getCount() {
        return gridItems.length;
    }

    @Override
    public Object getItem(int position) {
        return gridItems[position];
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(mContext).inflate(R.layout.grid_layout, parent, false);
        }
        int im = gridItems[position];
        int imageNum = 15;
        if (im != 0) {
            imageNum = im - 1;
        }

        ViewGroup.LayoutParams layoutParams = convertView.getLayoutParams();
        layoutParams.height = cellSize;
        layoutParams.width = cellSize;
        convertView.setLayoutParams(layoutParams);

        ((TextView) convertView.findViewById(R.id.item_text)).setText(gridItems[position] == 0 ? " " : gridItems[position].toString());
        ((ImageView) convertView.findViewById(R.id.item_image)).setImageResource(gridImages[imageNum]);

        return convertView;
    }
}
