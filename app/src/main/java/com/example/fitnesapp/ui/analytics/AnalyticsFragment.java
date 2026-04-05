package com.example.fitnesapp.ui.analytics;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.fitnesapp.Adapters.InsightsAdapter;
import com.example.fitnesapp.R;
import com.example.fitnesapp.databinding.FragmentAnalyticsBinding;
import com.example.fitnesapp.models.InsightItem;

import java.util.ArrayList;
import java.util.List;

public class AnalyticsFragment extends Fragment {

    private FragmentAnalyticsBinding binding;
    private AnalyticsViewModel analyticsViewModel;
    private InsightsAdapter adapter;

    private List<InsightItem> allInsights = new ArrayList<>();

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        analyticsViewModel = new ViewModelProvider(this).get(AnalyticsViewModel.class);
        binding = FragmentAnalyticsBinding.inflate(inflater, container, false);
        return binding.getRoot();

    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 1. Настраиваем список (RecyclerView)
        // ВАЖНО: Убедитесь, что в fragment_analytics.xml есть <androidx.recyclerview.widget.RecyclerView android:id="@+id/recyclerViewInsights" ... />
        adapter = new InsightsAdapter(new ArrayList<>());
        binding.recyclerViewInsights.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerViewInsights.setAdapter(adapter);

        // 2. Слушаем изменения из нейросети
        analyticsViewModel.getInsights().observe(getViewLifecycleOwner(), insights -> {
            if (insights != null) {
                // Отправляем новые карточки в адаптер
                adapter.updateData(insights);
            }
        });

        // 2. Слушаем изменения данных из ViewModel
        analyticsViewModel.getInsights().observe(getViewLifecycleOwner(), insights -> {
            if (insights != null) {
                allInsights = insights; // Сохраняем полный список
                // Сразу фильтруем по текущему выбранному чипу (по умолчанию "Все")
                filterAndDisplayInsights(binding.chipGroupFilters.getCheckedChipId());
            }
        });

        // 3. Слушаем нажатия на кнопки-фильтры
        binding.chipGroupFilters.setOnCheckedChangeListener((group, checkedId) -> {
            filterAndDisplayInsights(checkedId);
        });
    }

    // 4. Метод фильтрации
    private void filterAndDisplayInsights(int checkedId) {
        if (allInsights.isEmpty()) return;

        List<InsightItem> filteredList = new ArrayList<>();
        InsightItem.Category targetCategory = null;

        // Определяем, какая категория выбрана
        if (checkedId == R.id.chipRecovery) targetCategory = InsightItem.Category.RECOVERY;
        else if (checkedId == R.id.chipNutrition) targetCategory = InsightItem.Category.NUTRITION;
        else if (checkedId == R.id.chipActivity) targetCategory = InsightItem.Category.ACTIVITY;
        else if (checkedId == R.id.chipSleep) targetCategory = InsightItem.Category.SLEEP;
        // Если выбран чип "Все" (chipAll), targetCategory остается null

        // Фильтруем
        if (targetCategory == null) {
            filteredList.addAll(allInsights); // Показываем все
        } else {
            for (InsightItem item : allInsights) {
                if (item.category == targetCategory) {
                    filteredList.add(item);
                }
            }
        }

        // Если после фильтрации список пуст, можно показать сообщение-заглушку
        if (filteredList.isEmpty()) {
            filteredList.add(new InsightItem(InsightItem.Type.TIP, InsightItem.Category.GENERAL,
                    getString(R.string.analytics_nodata) , getString(R.string.analytics_nodata_dec) ));
        }

        // Передаем отфильтрованный список в ваш адаптер!
        // (Например: insightsAdapter.setItems(filteredList); insightsAdapter.notifyDataSetChanged(); )
        adapter.updateData(filteredList);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}