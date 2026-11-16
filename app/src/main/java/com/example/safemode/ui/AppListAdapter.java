package com.example.safemode.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.safemode.R;
import com.example.safemode.data.AppInfo;

import java.util.List;

/**
 * Adapter para exibir a lista de aplicativos em um RecyclerView.
 * Gerencia a exibição dos aplicativos instalados e permite marcar/desmarcar apps para bloqueio.
 */
public class AppListAdapter extends RecyclerView.Adapter<AppListAdapter.AppViewHolder> {

    private final List<AppInfo> appList;
    private final OnAppToggleListener listener;

    // Construtor que inicializa o adapter com a lista de apps e o listener
    public AppListAdapter(List<AppInfo> appList, OnAppToggleListener listener) {
        this.appList = appList;
        this.listener = listener;
    }

    // Cria uma nova instância de ViewHolder para um item da lista
    @NonNull
    @Override
    public AppViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_app, parent, false);
        return new AppViewHolder(view);
    }

    // Vincula os dados do aplicativo ao ViewHolder na posição especificada
    @Override
    public void onBindViewHolder(@NonNull AppViewHolder holder, int position) {
        AppInfo app = appList.get(position);
        holder.bind(app);
    }

    // Retorna o número total de itens na lista
    @Override
    public int getItemCount() {
        return appList.size();
    }

    // Interface de callback para notificar quando um app é marcado/desmarcado para bloqueio
    public interface OnAppToggleListener {
        void onAppToggled(AppInfo appInfo, boolean isBlocked);
    }

    // ViewHolder que mantém as referências das views de cada item da lista
    public class AppViewHolder extends RecyclerView.ViewHolder {

        private final ImageView iconImageView;
        private final TextView nameTextView;
        private final TextView packageTextView;
        private final CheckBox blockCheckBox;

        // Construtor que inicializa as views do item
        public AppViewHolder(@NonNull View itemView) {
            super(itemView);

            iconImageView = itemView.findViewById(R.id.app_icon);
            nameTextView = itemView.findViewById(R.id.app_name);
            packageTextView = itemView.findViewById(R.id.app_package);
            blockCheckBox = itemView.findViewById(R.id.checkbox_block);
        }

        // Vincula os dados do aplicativo às views e configura os listeners
        public void bind(AppInfo app) {

            iconImageView.setImageDrawable(app.icon);
            nameTextView.setText(app.appName);
            packageTextView.setText(app.packageName);
            packageTextView.setVisibility(View.GONE);
            blockCheckBox.setOnCheckedChangeListener(null);
            blockCheckBox.setChecked(app.isBlocked);
            blockCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                app.isBlocked = isChecked;
                if (listener != null) {
                    listener.onAppToggled(app, isChecked);
                }
            });
            itemView.setOnClickListener(v -> {
                blockCheckBox.setChecked(!blockCheckBox.isChecked());
            });
        }
    }
}
