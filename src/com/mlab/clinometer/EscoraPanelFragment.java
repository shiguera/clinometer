package com.mlab.clinometer;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

public class EscoraPanelFragment extends Fragment {

	TextView labelEscora, labelCabeceo;
	ImageView imageEscoraArco, imageCabeceoArco;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View hierarchy = inflater.inflate(R.layout.escora_panel, container, false);
		labelEscora = (TextView) hierarchy.findViewById(R.id.labelEscoraContent);
		labelCabeceo = (TextView) hierarchy.findViewById(R.id.labelCabeceoContent);
		imageEscoraArco = (ImageView) hierarchy.findViewById(R.id.imageEscoraArco);
		imageCabeceoArco = (ImageView) hierarchy.findViewById(R.id.imageCabeceoArco);
		
		return hierarchy;
	}
	public void setEscora(double escora) {
		labelEscora.setText(String.format("%3.1f",Math.round(escora*10)/10.0));
		float giro = (float)escora;
		if (giro>22f) {
			// Para que no tape la etiqueta título
			giro = 22f;
		}
		if (giro<-22f) {
			// Para que no tape la etiqueta título
			giro = -22f;
		}
		imageEscoraArco.setRotation(giro);
	}
	public void setCabeceo(double cabeceo) {
		labelCabeceo.setText(String.format("%3.1f",Math.round(cabeceo*10)/10.0));
		float giro = (float)cabeceo;
		if (giro>19f) {
			// Para que no tape la etiqueta título
			giro = 19f;
		}
		if (giro<-19f) {
			// Para que no tape la etiqueta título
			giro = -19f;
		}
		imageCabeceoArco.setRotation(giro);

	}

}
