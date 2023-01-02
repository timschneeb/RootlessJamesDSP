/*
 * MIT License
 *
 * Copyright (c) 2020 AmrDeveloper (Amr Hesham)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.

 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.amrdeveloper.codeview;

import android.content.Context;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Custom base adapter that to use it in CodeView auto complete and snippets feature
 *
 * CodeViewAdapter supports to take a list of code which can include Keywords and snippets
 *
 * @since 1.1.0
 */
public class CodeViewAdapter extends BaseAdapter implements Filterable {

    private List<Code> codeList;
    private List<Code> originalCodes;
    private final LayoutInflater layoutInflater;
    private final int codeViewLayoutId;
    private final int codeViewTextViewId;

    public CodeViewAdapter(@NonNull Context context, int resource, int textViewResourceId, @NonNull List<Code> codes) {
        this.codeList = codes;
        this.layoutInflater = LayoutInflater.from(context);
        this.codeViewLayoutId = resource;
        this.codeViewTextViewId = textViewResourceId;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = layoutInflater.inflate(codeViewLayoutId, parent, false);
        }

        TextView textViewName = convertView.findViewById(codeViewTextViewId);

        Code currentCode = codeList.get(position);
        if (currentCode != null) {
            textViewName.setText(currentCode.getCodeTitle());
        }

        return convertView;
    }

    @Override
    public int getCount() {
        return codeList.size();
    }

    @Override
    public Object getItem(int position) {
        return codeList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    /**
     * Update the current code list with new list
     * @param newCodeList The new code list
     */
    public void updateCodes(List<Code> newCodeList) {
        codeList.clear();
        codeList.addAll(newCodeList);
        notifyDataSetChanged();
    }

    /**
     * Clear the current code list and notify data set changed
     */
    public void clearCodes() {
        codeList.clear();
        notifyDataSetChanged();
    }

    @Override
    public Filter getFilter() {
        return codeFilter;
    }

    private final Filter codeFilter = new Filter() {
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            FilterResults results = new FilterResults();
            List<Code> suggestions = new ArrayList<>();

            if (originalCodes == null) {
                originalCodes = new ArrayList<>(codeList);
            }


            if (constraint == null || constraint.length() == 0) {
                results.values = originalCodes;
                results.count = originalCodes.size();
            } else {
                String filterPattern = constraint.toString().toLowerCase().trim();

                for (Code item : originalCodes) {
                    if (item.getCodePrefix().toLowerCase().contains(filterPattern)) {
                        suggestions.add(item);
                    }
                }
                results.values = suggestions;
                results.count = suggestions.size();
            }

            return results;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            codeList = (List<Code>) results.values;
            notifyDataSetChanged();
        }

        @Override
        public CharSequence convertResultToString(Object resultValue) {
            return ((Code) resultValue).getCodeBody();
        }
    };

}
