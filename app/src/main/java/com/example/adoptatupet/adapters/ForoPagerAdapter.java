package com.example.adoptatupet.views.fragments;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

/**
 * Adapter para el ViewPager2 que alberga dos pestañas:
 *  - índice 0 → MensajesTabFragment
 *  - índice 1 → UserProfileTabFragment
 */
public class ForoPagerAdapter extends FragmentStateAdapter {

    public ForoPagerAdapter(@NonNull Fragment fragment) {
        super(fragment);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        if (position == 0) {
            return new MensajesTabFragment();
        } else {
            // Posición 1 = pestaña Perfil (UserProfileTabFragment)
            return new UserProfileTabFragment();
        }
    }

    @Override
    public int getItemCount() {
        return 2;
    }
}
