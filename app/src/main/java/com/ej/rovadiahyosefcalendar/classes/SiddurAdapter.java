package com.ej.rovadiahyosefcalendar.classes;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.ej.rovadiahyosefcalendar.R;

import java.util.ArrayList;

public class SiddurAdapter extends ArrayAdapter<String> {

    private final Context context;
    private final ArrayList<HighlightString> siddur;
    private int textSize;

    public SiddurAdapter(Context context, ArrayList<HighlightString> siddur, int textSize) {
        super(context, 0);
        this.context = context;
        this.siddur = siddur;
        this.textSize = textSize;
    }

    public void setTextSize(int size) {
        textSize = size;
    }

    @Override
    public int getCount() {
        return siddur.size();
    }

    @Override
    public String getItem(int position) {
        return siddur.get(position).toString();
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder;

        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.text_view, parent, false);
            viewHolder = new ViewHolder();
            viewHolder.textView = convertView.findViewById(R.id.textView);
            convertView.setTag(viewHolder);
            viewHolder.defaultTextColor = viewHolder.textView.getCurrentTextColor();
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        String itemText = siddur.get(position).toString();
        viewHolder.textView.setText(itemText);
        viewHolder.textView.setTypeface(null, Typeface.BOLD);
        viewHolder.textView.setTextSize(textSize);

        if (siddur.get(position).shouldBeHighlighted()) {
            convertView.setBackgroundColor(Color.YELLOW);
            viewHolder.textView.setTextColor(context.getColor(R.color.black));
        } else {
            convertView.setBackgroundColor(Color.TRANSPARENT);
            viewHolder.textView.setTextColor(viewHolder.defaultTextColor);
        }

        return convertView;
    }

    static class ViewHolder {
        TextView textView;
        int defaultTextColor; // Store the default color
    }
}
