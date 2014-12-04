package com.mlab.clinometer;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class LabelPuntosFragment extends Fragment {

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
			View hierarchy = inflater.inflate(R.layout.points_label, container, false);
			return hierarchy;
		}
}
