package com.example.projetoasilo;

import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import BancoDeDados.BancoControllerEstoque;
import BancoDeDados.BancoControllerPaciente;
import BancoDeDados.BancoControllerProduto;

/**
 * Lógica de fragment_visualizar_estoque.xml
 *
 * A lista usa BancoControllerProduto.listarProdutosComEstoque() porque
 * listarEstoqueGeral() não devolve o ID do produto, que é necessário
 * para abastecer e transferir. As escritas continuam nos seus métodos:
 * adicionarEstoqueGeral() e transferirParaPaciente().
 */
public class VisualizarEstoqueFragment extends Fragment {

    private static class Item {
        int produtoId;
        String nome;
        String unidade;
        int quantidade;
        int minimo;
    }

    private final List<Item> itens = new ArrayList<>();
    private EditText etBuscar;
    private LinearLayout containerProdutos;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_visualizar_estoque, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        etBuscar = v.findViewById(R.id.etBuscarProduto);
        containerProdutos = v.findViewById(R.id.containerProdutosEstoque);

        v.findViewById(R.id.btnCadastrarProduto).setOnClickListener(x ->
                ((MainActivity) requireActivity()).abrir(new CadastrarProdutoFragment(), true));

        v.findViewById(R.id.btnAbastecerEstoque).setOnClickListener(x -> abastecer(null));

        etBuscar.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { renderizar(); }
            @Override public void afterTextChanged(Editable s) { }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        carregar(); // recarrega ao voltar do cadastro de produto
    }

    // ------------------------------------------------------------------
    // Dados
    // ------------------------------------------------------------------
    private void carregar() {
        itens.clear();
        Cursor c = new BancoControllerProduto(requireContext()).listarProdutosComEstoque();
        while (c.moveToNext()) {
            Item it = new Item();
            it.produtoId = Util.num(c, "produtoId");
            it.nome = Util.str(c, "nome");
            it.unidade = Util.str(c, "unidade");
            it.quantidade = Util.num(c, "quantidade");
            it.minimo = Util.num(c, "minimo");
            itens.add(it);
        }
        c.close();
        renderizar();
    }

    // ------------------------------------------------------------------
    // Lista (busca em tempo real)
    // ------------------------------------------------------------------
    private void renderizar() {
        containerProdutos.removeAllViews();
        String filtro = etBuscar.getText().toString().trim().toLowerCase(Locale.getDefault());

        int exibidos = 0;
        for (Item it : itens) {
            if (!filtro.isEmpty() && !it.nome.toLowerCase(Locale.getDefault()).contains(filtro)) {
                continue;
            }
            containerProdutos.addView(criarCard(it));
            exibidos++;
        }

        if (exibidos == 0) {
            TextView vazio = new TextView(requireContext());
            vazio.setText(itens.isEmpty()
                    ? "Nenhum produto cadastrado."
                    : "Nenhum produto encontrado.");
            vazio.setPadding(0, Util.dp(requireContext(), 16), 0, 0);
            containerProdutos.addView(vazio);
        }
    }

    private View criarCard(Item it) {
        int p = Util.dp(requireContext(), 12);
        boolean baixo = it.quantidade <= it.minimo;

        LinearLayout card = new LinearLayout(requireContext());
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(p, p, p, p);

        GradientDrawable fundo = new GradientDrawable();
        fundo.setCornerRadius(Util.dp(requireContext(), 8));
        fundo.setStroke(Util.dp(requireContext(), 1), baixo ? Color.parseColor("#C62828") : Color.GRAY);
        card.setBackground(fundo);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, Util.dp(requireContext(), 8));
        card.setLayoutParams(lp);

        TextView titulo = new TextView(requireContext());
        titulo.setText(it.nome);
        titulo.setTextSize(16);
        titulo.setTypeface(null, Typeface.BOLD);

        TextView detalhe = new TextView(requireContext());
        String texto = "Quantidade: " + it.quantidade + " " + it.unidade
                + "   (mínimo: " + it.minimo + ")";
        if (baixo) texto += "\nEstoque baixo";
        detalhe.setText(texto);
        if (baixo) detalhe.setTextColor(Color.parseColor("#C62828"));

        card.addView(titulo);
        card.addView(detalhe);

        card.setOnClickListener(v -> Util.escolher(requireContext(), it.nome,
                new String[]{"Abastecer estoque", "Transferir para paciente"},
                i -> {
                    if (i == 0) abastecer(it);
                    else transferir(it);
                }));
        return card;
    }

    // ------------------------------------------------------------------
    // Abastecer estoque geral
    // ------------------------------------------------------------------
    private void abastecer(@Nullable Item produto) {
        if (!Util.pode(requireContext(), "podecadastrar")) return;

        if (produto != null) {
            pedirQuantidadeAbastecimento(produto);
            return;
        }

        if (itens.isEmpty()) {
            Util.toast(requireContext(), "Cadastre um produto primeiro.");
            return;
        }
        String[] nomes = new String[itens.size()];
        for (int i = 0; i < itens.size(); i++) {
            nomes[i] = itens.get(i).nome + " (" + itens.get(i).unidade + ")";
        }
        Util.escolher(requireContext(), "Produto a abastecer", nomes,
                i -> pedirQuantidadeAbastecimento(itens.get(i)));
    }

    private void pedirQuantidadeAbastecimento(Item produto) {
        Util.pedirNumero(requireContext(), "Abastecer " + produto.nome,
                "Quantidade a adicionar", qtd -> {
                    try {
                        new BancoControllerEstoque(requireContext())
                                .adicionarEstoqueGeral(produto.produtoId, qtd);
                        Util.toast(requireContext(), "Estoque atualizado.");
                    } catch (Exception e) {
                        Util.toast(requireContext(), "Erro ao abastecer: " + e.getMessage());
                    }
                    carregar();
                });
    }

    // ------------------------------------------------------------------
    // Transferir do estoque geral para o estoque individual do paciente
    // ------------------------------------------------------------------
    private void transferir(Item produto) {
        if (!Util.pode(requireContext(), "podebaixarestoque")) return;

        if (produto.quantidade <= 0) {
            Util.toast(requireContext(), "Sem saldo no estoque geral.");
            return;
        }

        List<Integer> ids = new ArrayList<>();
        List<String> nomes = new ArrayList<>();
        Cursor c = new BancoControllerPaciente(requireContext()).listarPacientes();
        while (c.moveToNext()) {
            ids.add(Util.num(c, "id", "_id"));
            String quarto = Util.str(c, "numero", "numeroquarto", "quarto");
            nomes.add(Util.str(c, "nome") + (quarto.isEmpty() ? "" : " - Quarto " + quarto));
        }
        c.close();

        if (ids.isEmpty()) {
            Util.toast(requireContext(), "Nenhum paciente cadastrado.");
            return;
        }

        Util.escolher(requireContext(), "Transferir " + produto.nome + " para:",
                nomes.toArray(new String[0]),
                i -> Util.pedirNumero(requireContext(),
                        "Quantidade (disponível: " + produto.quantidade + ")",
                        "Quantidade a transferir",
                        qtd -> {
                            try {
                                String mensagem = new BancoControllerEstoque(requireContext())
                                        .transferirParaPaciente(produto.produtoId, ids.get(i), qtd);
                                Util.toastLongo(requireContext(), mensagem);
                            } catch (Exception e) {
                                Util.toast(requireContext(), "Erro na transferência: " + e.getMessage());
                            }
                            carregar();
                        }));
    }
}
