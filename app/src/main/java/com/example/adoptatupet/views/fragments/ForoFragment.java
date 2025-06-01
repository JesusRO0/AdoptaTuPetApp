package com.example.adoptatupet.views.fragments;

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
 */
public class ForoFragment extends Fragment {

    private TabLayout tabLayoutForo;
    private ViewPager2 viewPagerForo;
    private ForoPagerAdapter pagerAdapter;

    public ForoFragment() {
        // Constructor vacío
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup   container,
                             @Nullable Bundle      savedInstanceState) {
        // Inflamos el layout que contiene TabLayout y ViewPager2
        View view = inflater.inflate(R.layout.fragment_foro, container, false);

        // 1) Referencias a TabLayout y ViewPager2
        tabLayoutForo = view.findViewById(R.id.tabLayoutForo);
        viewPagerForo = view.findViewById(R.id.viewPagerForo);

        // 2) Instanciar el adapter PARA ViewPager2 pasando requireActivity()
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
                // Cuando se pulsa un nombre en MensajesTabFragment, mostrar perfil en la segunda pestaña
                ((MensajesTabFragment) f0).setOnUserNameClickListener(userId -> {
                    mostrarPerfilUsuario(userId);
                });
            }
        });

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
