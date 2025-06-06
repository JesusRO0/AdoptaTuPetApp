package com.example.adoptatupet.views.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.adoptatupet.R;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

/**
 * ContactoFragment muestra:
 *  - Logo + Descripción de la empresa
 *  - “Lo que nos mueve” con tabla informativa
 *  - Un mapa de Google Maps (SupportMapFragment) centrado en Ceuta
 *  - Dirección + Teléfono
 */
public class ContactoFragment extends Fragment implements OnMapReadyCallback {

    private GoogleMap mMap;

    public ContactoFragment() {
        // Constructor vacío
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup   container,
                             @Nullable Bundle      savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_contacto, container, false);

        // 1) Obtener instancia de SupportMapFragment y asignar el callback
        SupportMapFragment mapFragment = (SupportMapFragment)
                getChildFragmentManager().findFragmentById(R.id.mapContacto);

        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        return view;
    }

    /**
     * Se invoca cuando el mapa está listo. Aquí centramos el mapa en Ceuta
     * (latitud 35.8880, longitud -5.3179) y colocamos un marcador.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Coordenadas de Ceuta
        LatLng ubicacionCeuta = new LatLng(35.8880, -5.3179);

        // 1) Añadir marcador en Ceuta
        mMap.addMarker(new MarkerOptions()
                .position(ubicacionCeuta)
                .title("Oficinas AdoptaTuPet en Ceuta"));

        // 2) Mover cámara / zoom
        float zoomNivel = 12.0f; // 0 = lejísimo, 20 = súper acercado
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(ubicacionCeuta, zoomNivel));
    }
}
