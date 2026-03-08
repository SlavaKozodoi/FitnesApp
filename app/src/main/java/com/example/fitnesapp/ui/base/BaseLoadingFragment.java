package com.example.fitnesapp.ui.base;

import android.os.Handler;
import android.os.Looper;
import android.view.View;
import androidx.fragment.app.Fragment;
import com.example.fitnesapp.R; // Убедитесь, что тут ваш R класс

public abstract class BaseLoadingFragment extends Fragment {

    /**
     * Вызовите этот метод в onViewCreated() вашего фрагмента
     * @param root Корневая View вашего фрагмента
     * @param delayMs Задержка в миллисекундах (например, 1000)
     */
    protected void startFakeLoading(View root, int delayMs) {
        // Ищем слои по стандартизированным ID
        View contentLayout = root.findViewById(R.id.contentLayout);
        View loadingOverlay = root.findViewById(R.id.loadingOverlay);

        // Если вы забыли добавить эти ID в XML, метод просто ничего не сделает, чтобы не было краша
        if (contentLayout == null || loadingOverlay == null) return;

        // Запускаем задержку
        new Handler(Looper.getMainLooper()).postDelayed(() -> {

            // Защита от краша, если пользователь ушел с экрана до конца загрузки
            if (!isAdded() || getView() == null) return;

            // 1. Прячем загрузочный экран
            loadingOverlay.animate()
                    .alpha(0f)
                    .setDuration(300)
                    .withEndAction(() -> loadingOverlay.setVisibility(View.GONE))
                    .start();

            // 2. Показываем контент
            contentLayout.animate()
                    .alpha(1f)
                    .setDuration(300)
                    .start();

        }, delayMs);
    }
}