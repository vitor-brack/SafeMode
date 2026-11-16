package com.example.safemode.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.safemode.R;
import com.example.safemode.data.LauncherAppInfo;

import java.util.List;

/**
 * Adapter para exibir aplicativos no launcher customizado.
 * Gerencia a exibição de apps e permite clicar para abri-los.
 */
public class LauncherAppAdapter extends RecyclerView.Adapter<LauncherAppAdapter.AppViewHolder> {

    private final Context context;
    private final List<LauncherAppInfo> apps;
    private final OnAppClickListener listener;

    // Construtor que inicializa o adapter com contexto, lista de apps e listener
    public LauncherAppAdapter(Context context, List<LauncherAppInfo> apps, OnAppClickListener listener) {
        this.context = context;
        this.apps = apps;
        this.listener = listener;
    }

    // Cria uma nova instância de ViewHolder para um item da lista
    @NonNull
    @Override
    public AppViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_launcher_app, parent, false);
        return new AppViewHolder(view);
    }

    // Vincula os dados do aplicativo ao ViewHolder na posição especificada
    @Override
    public void onBindViewHolder(@NonNull AppViewHolder holder, int position) {
        LauncherAppInfo app = apps.get(position);
        holder.bind(app);
    }

    // Retorna o número total de itens na lista
    @Override
    public int getItemCount() {
        return apps.size();
    }

    // Atualiza a lista de aplicativos e notifica o adapter
    public void updateApps(List<LauncherAppInfo> newApps) {
        this.apps.clear();
        this.apps.addAll(newApps);
        notifyDataSetChanged();
    }

    // Interface de callback para notificar quando um app é clicado
    public interface OnAppClickListener {
        void onAppClick(LauncherAppInfo app);
    }

    // ViewHolder que mantém as referências das views de cada item da lista
    class AppViewHolder extends RecyclerView.ViewHolder {

        private final ImageView iconView;
        private final TextView nameView;

        // Construtor que inicializa as views do item
        public AppViewHolder(@NonNull View itemView) {
            super(itemView);
            iconView = itemView.findViewById(R.id.app_icon);
            nameView = itemView.findViewById(R.id.app_name);
        }

        // Vincula os dados do aplicativo às views e configura o listener de clique
        public void bind(LauncherAppInfo app) {
            iconView.setImageDrawable(app.icon);
            nameView.setText(app.appName);

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onAppClick(app);
                }
            });
        }
    }
}
