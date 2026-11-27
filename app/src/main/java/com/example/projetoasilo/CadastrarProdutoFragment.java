package com.example.projetoasilo;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import BancoDeDados.BancoControllerProduto;

/** Lógica de fragment_cadastrar_produto.xml */
public class CadastrarProdutoFragment extends Fragment {

    private EditText etNome, etTipo, etCodigoQR, etUnidade;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_cadastrar_produto, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        etNome = v.findViewById(R.id.etNomeProduto);
        etTipo = v.findViewById(R.id.etTipoProduto);
        etCodigoQR = v.findViewById(R.id.etCodigoQR);
        etUnidade = v.findViewById(R.id.etUnidadeMedida);

        v.findViewById(R.id.btnSalvarProduto).setOnClickListener(x -> salvar());
    }

    private String t(EditText e) {
        return e.getText().toString().trim();
    }

    private void salvar() {
        if (!Util.pode(requireContext(), "podecadastrar")) return;

        if (t(etNome).isEmpty()) {
            etNome.setError("Informe o nome do produto");
            return;
        }
        if (t(etUnidade).isEmpty()) {
            etUnidade.setError("Informe a unidade de medida");
            return;
        }

        // codigoqr é UNIQUE: se o campo vier vazio, gera um código interno
        String codigo = t(etCodigoQR);
        if (codigo.isEmpty()) codigo = "PROD-" + System.currentTimeMillis();

        long id = new BancoControllerProduto(requireContext())
                .inserirProduto(t(etNome), t(etTipo), codigo, t(etUnidade));

        if (id > 0) {
            Util.toast(requireContext(), "Produto cadastrado com sucesso.");
            // volta para a lista de estoque, que recarrega sozinha
            ((MainActivity) requireActivity()).voltar();
        } else {
            Util.toast(requireContext(), "Não foi possível salvar (código QR já cadastrado?).");
        }
    }
}
