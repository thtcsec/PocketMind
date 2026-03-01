package com.tuhoang.pocketmind;

import android.os.Bundle;
import android.view.MenuItem;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;
import com.tuhoang.pocketmind.databinding.ActivityMainBinding;
import com.tuhoang.pocketmind.ui.MainPagerAdapter;
import com.tuhoang.pocketmind.utils.AppLogger;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        AppLogger.d("MainActivity started");

        ViewPager2 viewPager = binding.viewPager;
        BottomNavigationView navView = binding.navView;

        MainPagerAdapter pagerAdapter = new MainPagerAdapter(this);
        viewPager.setAdapter(pagerAdapter);

        // Remove forced login interception as requested
        // viewPager handles swiping directly

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                switch (position) {
                    case 0:
                        navView.setSelectedItemId(R.id.navigation_home);
                        break;
                    case 1:
                        navView.setSelectedItemId(R.id.navigation_add);
                        break;
                    case 2:
                        navView.setSelectedItemId(R.id.navigation_report);
                        break;
                    case 3:
                        navView.setSelectedItemId(R.id.navigation_profile);
                        break;
                }
            }
        });

        navView.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int id = item.getItemId();
                if (id == R.id.navigation_home) {
                    viewPager.setCurrentItem(0);
                    return true;
                } else if (id == R.id.navigation_add) {
                    viewPager.setCurrentItem(1);
                    return true;
                } else if (id == R.id.navigation_report) {
                    viewPager.setCurrentItem(2);
                    return true;
                } else if (id == R.id.navigation_profile) {
                    viewPager.setCurrentItem(3);
                    return true;
                }
                return false;
            }
        });
    }
}