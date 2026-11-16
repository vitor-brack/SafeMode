package com.example.safemode.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ScrollView;

/**
 * ScrollView customizado que permite interação com mapas dentro de uma área scrollável.
 * Detecta toques na área do mapa e desabilita o scroll do container para permitir
 * que o usuário interaja diretamente com o mapa (zoom, pan, etc).
 */
public class MapFriendlyScrollView extends ScrollView {

    private View mapContainer;

    // Construtor básico
    public MapFriendlyScrollView(Context context) {
        super(context);
    }

    // Construtor com atributos XML
    public MapFriendlyScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    // Construtor com atributos XML e estilo
    public MapFriendlyScrollView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    // Define a view do container do mapa para detectar toques
    public void setMapContainer(View mapContainer) {
        this.mapContainer = mapContainer;
    }

    // Intercepta eventos de toque para permitir interação com o mapa
    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        try {
            if (mapContainer == null) {
                return super.onInterceptTouchEvent(event);
            }

            if (isTouchInsideMapArea(event)) {
                return false;
            }

            return super.onInterceptTouchEvent(event);

        } catch (Exception e) {
            return super.onInterceptTouchEvent(event);
        }
    }

    // Verifica se o toque ocorreu dentro da área do mapa
    private boolean isTouchInsideMapArea(MotionEvent event) {
        try {
            if (mapContainer == null) {
                return false;
            }

            float touchX = event.getRawX();
            float touchY = event.getRawY();

            int[] mapLocation = new int[2];
            mapContainer.getLocationOnScreen(mapLocation);

            int mapLeft = mapLocation[0];
            int mapTop = mapLocation[1];
            int mapRight = mapLeft + mapContainer.getWidth();
            int mapBottom = mapTop + mapContainer.getHeight();

            boolean isInsideMap = touchX >= mapLeft && touchX <= mapRight &&
                    touchY >= mapTop && touchY <= mapBottom;

            return isInsideMap;

        } catch (Exception e) {
            return false;
        }
    }
}