package com.example.fitnesapp.ui.analytics;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
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

import me.toptas.fancyshowcase.FancyShowCaseQueue;
import me.toptas.fancyshowcase.FancyShowCaseView;
import me.toptas.fancyshowcase.FocusShape;

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
        adapter = new InsightsAdapter(new ArrayList<>());
        binding.recyclerViewInsights.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerViewInsights.setAdapter(adapter);

        // 2. Слушаем изменения данных из ViewModel (Объединили два вызова в один)
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

        // --- ЗАПУСК ОБУЧЕНИЯ ---
        SharedPreferences prefs = requireActivity().getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
        int tutorialStep = prefs.getInt("tutorial_step_anality", 0);

        if (tutorialStep == 0) {
            // Самый первый запуск
            view.postDelayed(() -> showTutorial(view), 500);
        }
    }

    // Вспомогательный метод для склейки заголовка и текста из ресурсов
    private String getTutorialText(int titleResId, int descResId) {
        return getString(titleResId) + "\n\n" + getString(descResId);
    }

    private void showTutorial(View rootFragmentView) {
        if (binding == null) return;

        FancyShowCaseView welcomeStep = new FancyShowCaseView.Builder(requireActivity())
                .title(getTutorialText(R.string.tutorial_analytics_welcome_title, R.string.tutorial_analytics_welcome_text))
                .titleStyle(0, Gravity.CENTER)
                .fitSystemWindows(true)
                .backgroundColor(Color.parseColor("#CC000000"))
                .build();

        FancyShowCaseView healthStep = new FancyShowCaseView.Builder(requireActivity())
                .title(getTutorialText(R.string.tutorial_analytics_list_title, R.string.tutorial_analytics_list_text))
                .focusOn(binding.recyclerViewInsights)
                .titleStyle(0, Gravity.BOTTOM)
                .focusShape(FocusShape.ROUNDED_RECTANGLE)
                .fitSystemWindows(true)
                .backgroundColor(Color.parseColor("#CC000000"))
                .build();

        FancyShowCaseView healthStep2 = new FancyShowCaseView.Builder(requireActivity())
                .title(getTutorialText(R.string.tutorial_analytics_filters_title, R.string.tutorial_analytics_filters_text))
                .focusOn(binding.chipGroupFilters)
                .focusShape(FocusShape.ROUNDED_RECTANGLE)
                .fitSystemWindows(true)
                .backgroundColor(Color.parseColor("#CC000000"))
                .build();

        FancyShowCaseQueue queue = new FancyShowCaseQueue()
                .add(welcomeStep)
                .add(healthStep)
                .add(healthStep2);

        queue.setCompleteListener(() -> {
            requireActivity().getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
                    .edit()
                    .putInt("tutorial_step_anality", 1)
                    .apply();
        });

        queue.show();
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

        // Если после фильтрации список пуст, показываем заглушку
        if (filteredList.isEmpty()) {
            filteredList.add(new InsightItem(InsightItem.Type.TIP, InsightItem.Category.GENERAL,
                    getString(R.string.analytics_nodata), getString(R.string.analytics_nodata_dec)));
        }

        // Передаем отфильтрованный список в адаптер
        adapter.updateData(filteredList);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}