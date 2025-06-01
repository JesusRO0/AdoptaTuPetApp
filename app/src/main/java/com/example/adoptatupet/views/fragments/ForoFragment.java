package com.example.adoptatupet.views.fragments;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.example.adoptatupet.views.fragments.ForoPagerAdapter;

import com.example.adoptatupet.R;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

/**
 * ForoFragment ahora funciona solo como contenedor de dos pestañas:
 *  - índice 0 → MensajesTabFragment
 *  - índice 1 → UserProfileTabFragment
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
        View view = inflater.inflate(R.layout.fragment_foro, container, false);

        tabLayoutForo = view.findViewById(R.id.tabLayoutForo);
        viewPagerForo = view.findViewById(R.id.viewPagerForo);

        // 1) Configurar el adapter para el ViewPager2
        pagerAdapter = new ForoPagerAdapter(this);
        viewPagerForo.setAdapter(pagerAdapter);

        // 2) Vincular TabLayout con ViewPager2
        new TabLayoutMediator(tabLayoutForo, viewPagerForo,
                (tab, position) -> {
                    if (position == 0) {
                        tab.setText("Mensajes");
                    } else {
                        tab.setText("Perfil");
                    }
                }
        ).attach();

        // 3) Inyectar el listener en MensajesTabFragment para clicks en nombre
        //    Esperamos un poco a que ViewPager2 haya creado el fragmento en la posición 0
        viewPagerForo.post(() -> {
            Fragment f0 = getChildFragmentManager().findFragmentByTag("f" + 0);
            if (f0 instanceof MensajesTabFragment) {
                ((MensajesTabFragment) f0).setOnUserNameClickListener(userId -> {
                    mostrarPerfilUsuario(userId);
                });
            }
        });

        return view;
    }

    /**
     * Cuando el usuario pulsa el nombre en la pestaña de Mensajes, esto:
     * - Cambia a la pestaña “Perfil” (índice 1)
     * - Llama a actualizarUsuario(userId) en el fragmento UserProfileTabFragment
     */
    private void mostrarPerfilUsuario(int userId) {
        // 1) Cambiar a pestaña “Perfil” (índice 1)
        viewPagerForo.setCurrentItem(1, true);

        // 2) Recuperar el fragment en la posición 1 (tag "f1")
        Fragment f1 = getChildFragmentManager().findFragmentByTag("f" + 1);
        if (f1 instanceof UserProfileTabFragment) {
            ((UserProfileTabFragment) f1).actualizarUsuario(userId);
        }
    }
}
