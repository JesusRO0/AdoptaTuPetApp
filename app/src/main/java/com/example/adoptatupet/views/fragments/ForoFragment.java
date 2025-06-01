package com.example.adoptatupet.views.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.example.adoptatupet.R;
import com.example.adoptatupet.adapters.ForoPagerAdapter;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

/**
 * ForoFragment actúa como contenedor de dos pestañas:
 *   - índice 0 → MensajesTabFragment
 *   - índice 1 → UserProfileTabFragment
 *
 * Ahora, se añade un callback para que cuando el usuario cambie a “Perfil” (posición 1),
 * automáticamente invoquemos actualizarUsuario(...) y así recarguemos el historial con los likes
 * actualizados.
 */
public class ForoFragment extends Fragment {

    private TabLayout   tabLayoutForo;
    private ViewPager2  viewPagerForo;
    private ForoPagerAdapter pagerAdapter;

    public ForoFragment() {
        // Constructor vacío
    }

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup   container,
                             @Nullable Bundle      savedInstanceState) {
        // Inflamos el layout que contiene TabLayout y ViewPager2
        View view = inflater.inflate(R.layout.fragment_foro, container, false);

        // 1) Referencias a TabLayout y ViewPager2
        tabLayoutForo = view.findViewById(R.id.tabLayoutForo);
        viewPagerForo = view.findViewById(R.id.viewPagerForo);

        // 2) Instanciar el adapter para ViewPager2, pasando requireActivity()
        pagerAdapter = new ForoPagerAdapter(requireActivity());
        viewPagerForo.setAdapter(pagerAdapter);

        // 3) Vincular el TabLayout con el ViewPager2 mediante TabLayoutMediator
        new TabLayoutMediator(tabLayoutForo, viewPagerForo,
                (tab, position) -> {
                    if (position == 0) {
                        tab.setText("Mensajes");
                    } else {
                        tab.setText("Perfil");
                    }
                }
        ).attach();

        // 4) Esperar a que la pestaña “Mensajes” (posición 0) esté creada para inyectar listener
        viewPagerForo.post(() -> {
            Fragment f0 = getChildFragmentManager().findFragmentByTag("f" + 0);
            if (f0 instanceof MensajesTabFragment) {
                // Cuando se pulsa un nombre en MensajesTabFragment, mostramos perfil en la pestaña 1
                ((MensajesTabFragment) f0).setOnUserNameClickListener(userId -> {
                    mostrarPerfilUsuario(userId);
                });
            }
        });

        // ---------- NUEVO: cada vez que cambie de pestaña, comprobamos si es “Perfil” (posición 1)
        viewPagerForo.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);

                if (position == 1) {
                    // Recuperamos el ID del usuario actual desde SharedPreferences
                    SharedPreferences prefs = requireActivity()
                            .getSharedPreferences("user", Context.MODE_PRIVATE);
                    int myUserId = prefs.getInt("idUsuario", -1);

                    // Buscamos el fragment de índice 1 por su tag “f1”
                    Fragment f1 = getChildFragmentManager().findFragmentByTag("f" + 1);
                    if (f1 instanceof UserProfileTabFragment && myUserId != -1) {
                        // Llamamos a actualizarUsuario(userId) para recargar historial (likes incluidos)
                        ((UserProfileTabFragment) f1).actualizarUsuario(myUserId);
                    }
                }
            }
        });
        // ---------- FIN DE CAMBIO

        return view;
    }

    /**
     * Cambia a la pestaña “Perfil” (índice 1) e invoca actualizarUsuario en UserProfileTabFragment.
     */
    private void mostrarPerfilUsuario(int userId) {
        // 1) Pasar a la pestaña “Perfil”
        viewPagerForo.setCurrentItem(1, true);

        // 2) Recuperar el fragment en la posición 1
        Fragment f1 = getChildFragmentManager().findFragmentByTag("f" + 1);
        if (f1 instanceof UserProfileTabFragment) {
            // Llamar a actualizarUsuario(userId) para recargar datos e historial
            ((UserProfileTabFragment) f1).actualizarUsuario(userId);
        }
    }
}
