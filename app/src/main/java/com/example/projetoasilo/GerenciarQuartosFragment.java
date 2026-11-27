package com.example.projetoasilo;

import android.database.Cursor;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import BancoDeDados.BancoControllerQuarto;

/** Lógica de fragment_gerenciar_quartos.xml + item_quarto.xml */
public class GerenciarQuartosFragment extends Fragment {

    private LinearLayout containerQuartos;
    private TextView tvVazio;
    private LayoutInflater inflater;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        this.inflater = inflater;
        return inflater.inflate(R.layout.fragment_gerenciar_quartos, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        containerQuartos = v.findViewById(R.id.containerQuartos);
        tvVazio = v.findViewById(R.id.tvQuartosVazios);

        v.findViewById(R.id.btnAdicionarQuarto).setOnClickListener(x -> {
            if (Util.pode(requireContext(), "podecadastrar")) {
                dialogoQuarto(-1, "", 1);
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        carregar();
    }

    // ------------------------------------------------------------------
    // Listagem
    // ------------------------------------------------------------------
    private void carregar() {
        containerQuartos.removeAllViews();

        Cursor c = new BancoControllerQuarto(requireContext()).listarQuartos();
        tvVazio.setVisibility(c.getCount() == 0 ? View.VISIBLE : View.GONE);

        while (c.moveToNext()) {
            final int id = Util.num(c, "id", "_id");
            final String numero = Util.str(c, "numero");
            final int capacidade = Util.num(c, "capacidade");

            View item = inflater.inflate(R.layout.item_quarto, containerQuartos, false);
            ((TextView) item.findViewById(R.id.tvNumeroQuarto)).setText("Quarto " + numero);
            ((TextView) item.findViewById(R.id.tvCapacidadeQuarto))
                    .setText("Capacidade: " + capacidade);

            item.findViewById(R.id.btnEditarQuarto).setOnClickListener(x -> {
                if (Util.pode(requireContext(), "podecadastrar")) {
                    dialogoQuarto(id, numero, capacidade);
                }
            });
            item.findViewById(R.id.btnExcluirQuarto).setOnClickListener(x -> {
                if (Util.pode(requireContext(), "podecadastrar")) {
                    confirmarExclusao(id, numero);
                }
            });

            containerQuartos.addView(item);
        }
        c.close();
    }

    // ------------------------------------------------------------------
    // Adicionar / editar (idQuarto = -1 significa novo)
    // ------------------------------------------------------------------
    private void dialogoQuarto(int idQuarto, String numeroAtual, int capacidadeAtual) {
        EditText edtNumero = new EditText(requireContext());
        edtNumero.setHint("Número do quarto");
        edtNumero.setText(numeroAtual);

        EditText edtCapacidade = new EditText(requireContext());
        edtCapacidade.setHint("Capacidade (leitos)");
        edtCapacidade.setInputType(InputType.TYPE_CLASS_NUMBER);
        edtCapacidade.setText(String.valueOf(capacidadeAtual));

        LinearLayout box = new LinearLayout(requireContext());
        box.setOrientation(LinearLayout.VERTICAL);
        int p = Util.dp(requireContext(), 20);
        box.setPadding(p, p / 2, p, 0);
        box.addView(edtNumero);
        box.addView(edtCapacidade);

        new AlertDialog.Builder(requireContext())
                .setTitle(idQuarto < 0 ? "Novo quarto" : "Editar quarto")
                .setView(box)
                .setPositiveButton("Salvar", (d, w) -> {
                    String numero = edtNumero.getText().toString().trim();
                    int capacidade;
                    try {
                        capacidade = Integer.parseInt(edtCapacidade.getText().toString().trim());
                    } catch (NumberFormatException e) {
                        capacidade = 0;
                    }
                    if (numero.isEmpty() || capacidade <= 0) {
                        Util.toast(requireContext(), "Informe o número e uma capacidade maior que zero.");
                        return;
                    }
                    salvarQuarto(idQuarto, numero, capacidade);
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void salvarQuarto(int idQuarto, String numero, int capacidade) {
        BancoControllerQuarto ctrl = new BancoControllerQuarto(requireContext());
        try {
            if (idQuarto < 0) {
                int antes = Util.contar(ctrl.listarQuartos());
                ctrl.insereDados(numero, capacidade);
                int depois = Util.contar(ctrl.listarQuartos());
                if (depois > antes) {
                    Util.toast(requireContext(), "Quarto cadastrado.");
                } else {
                    Util.toast(requireContext(), "Não foi possível cadastrar (número já existe?).");
                }
            } else {
                ctrl.atualizarQuarto(idQuarto, numero, capacidade);
                Util.toast(requireContext(), "Quarto atualizado.");
            }
        } catch (Exception e) {
            Util.toast(requireContext(), "Erro ao salvar: " + e.getMessage());
        }
        carregar();
    }

    // ------------------------------------------------------------------
    // Exclusão
    // ------------------------------------------------------------------
    private void confirmarExclusao(int id, String numero) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Excluir quarto")
                .setMessage("Deseja excluir o quarto " + numero + "?")
                .setPositiveButton("Excluir", (d, w) -> excluir(id))
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void excluir(int id) {
        BancoControllerQuarto ctrl = new BancoControllerQuarto(requireContext());
        try {
            ctrl.excluirQuarto(id);
        } catch (Exception ignorado) {
            // chave estrangeira: quarto com pacientes
        }

        // confere no banco se o quarto realmente saiu
        Cursor c = ctrl.buscarQuarto(id);
        boolean aindaExiste = c.getCount() > 0;
        c.close();

        if (aindaExiste) {
            Util.toast(requireContext(), "Não foi possível excluir: há pacientes neste quarto.");
        } else {
            Util.toast(requireContext(), "Quarto excluído.");
        }
        carregar();
    }
}
