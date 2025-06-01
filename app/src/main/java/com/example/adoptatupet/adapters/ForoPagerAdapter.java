package com.example.adoptatupet.adapters;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.example.adoptatupet.views.fragments.MensajesTabFragment;
import com.example.adoptatupet.views.fragments.UserProfileTabFragment;

public class ForoPagerAdapter extends FragmentStateAdapter {

    public ForoPagerAdapter(@NonNull FragmentActivity fa) {
        super(fa);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        if (position == 0) {
            // Pestaña "Mensajes"
            return new MensajesTabFragment();
        }
        // Pestaña "Perfil"
        return new UserProfileTabFragment();
    }

    @Override
    public int getItemCount() {
        return 2; // Mensajes + Perfil
    }
}
