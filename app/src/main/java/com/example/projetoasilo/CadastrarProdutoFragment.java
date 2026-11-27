
package com.example.projetoasilo;

import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

import BancoDeDados.BancoControllerProduto;


public class CadastrarProdutoFragment extends Fragment {

    private static final String ARG_CODIGO = "codigo";

    private EditText etNome;
    private EditText etTipo;
    private EditText etCodigoQR;
    private EditText etUnidade;

    private Button btnSalvar;

    /**
     * Permite abrir o cadastro com um código QR já preenchido.
     */
    public static CadastrarProdutoFragment novo(@Nullable String codigo) {

        CadastrarProdutoFragment fragment =
                new CadastrarProdutoFragment();

        Bundle args = new Bundle();
        args.putString(ARG_CODIGO, codigo);

        fragment.setArguments(args);

        return fragment;
    }

    /**
     * Configuração do leitor de QR Code.
     */
    private final ActivityResultLauncher<ScanOptions> leitor =
            registerForActivityResult(
                    new ScanContract(),
                    resultado -> {

                        if (!isAdded()) {
                            return;
                        }

                        String codigo = QrCodeHelper.lido(
                                requireContext(),
                                resultado
                        );

                        if (codigo != null) {
                            preencherCodigo(codigo);
                        }
                    }
            );

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {

        return inflater.inflate(
                R.layout.fragment_cadastrar_produto,
                container,
                false
        );
    }

    @Override
    public void onViewCreated(
            @NonNull View v,
            @Nullable Bundle savedInstanceState) {

        etNome = v.findViewById(R.id.etNomeProduto);
        etTipo = v.findViewById(R.id.etTipoProduto);
        etCodigoQR = v.findViewById(R.id.etCodigoQR);
        etUnidade = v.findViewById(R.id.etUnidadeMedida);

        btnSalvar = v.findViewById(R.id.btnSalvarProduto);

        // Salvar produto
        btnSalvar.setOnClickListener(x -> salvar());

        // Escanear QR Code
        v.findViewById(R.id.btnEscanearCodigo)
                .setOnClickListener(x -> {

                    leitor.launch(
                            QrCodeHelper.opcoesLeitor(
                                    "Aponte a câmera para o QR code do produto"
                            )
                    );
                });

        // Gerar código automaticamente
        v.findViewById(R.id.btnGerarCodigo)
                .setOnClickListener(x -> {

                    BancoControllerProduto ctrl =
                            new BancoControllerProduto(requireContext());

                    etCodigoQR.setText(ctrl.gerarCodigoUnico());
                    etCodigoQR.setError(null);
                });

        // Recuperar código recebido por argumento
        Bundle args = getArguments();

        if (savedInstanceState == null
                && args != null
                && args.getString(ARG_CODIGO) != null) {

            etCodigoQR.setText(args.getString(ARG_CODIGO));
        }
    }

    private String t(EditText campo) {
        return campo.getText().toString().trim();
    }

    /**
     * Preenche o campo com o código lido,
     * verificando se ele já pertence a outro produto.
     */
    private void preencherCodigo(String codigo) {

        BancoControllerProduto ctrl =
                new BancoControllerProduto(requireContext());

        Cursor cursor = null;

        try {

            cursor = ctrl.buscarPorCodigoQR(codigo);

            if (cursor.moveToFirst()) {

                String nome = Util.str(cursor, "nome");

                etCodigoQR.setError("Código já cadastrado");

                Util.toastLongo(
                        requireContext(),
                        "Este código já pertence ao produto \""
                                + nome
                                + "\". Escaneie outro ou gere um novo."
                );

                return;
            }

            etCodigoQR.setError(null);
            etCodigoQR.setText(codigo);

            Util.toast(requireContext(), "Código lido com sucesso.");

        } finally {

            if (cursor != null) {
                cursor.close();
            }
        }
    }

    /**
     * Valida e cadastra o produto.
     */
    private void salvar() {

        if (!Util.pode(requireContext(), "podecadastrar")) {
            return;
        }

        if (t(etNome).isEmpty()) {
            etNome.setError("Informe o nome do produto");
            etNome.requestFocus();
            return;
        }

        if (t(etUnidade).isEmpty()) {
            etUnidade.setError("Informe a unidade de medida");
            etUnidade.requestFocus();
            return;
        }

        BancoControllerProduto ctrl =
                new BancoControllerProduto(requireContext());

        String codigo = t(etCodigoQR);

        // Gera código caso o usuário não tenha informado nenhum.
        if (codigo.isEmpty()) {

            codigo = ctrl.gerarCodigoUnico();

        } else if (ctrl.codigoExiste(codigo)) {

            etCodigoQR.setError("Código já cadastrado em outro produto");
            etCodigoQR.requestFocus();

            return;
        }

        String nome = t(etNome);
        String tipo = t(etTipo);
        String unidade = t(etUnidade);

        // Evita múltiplos cadastros por toque duplo.
        btnSalvar.setEnabled(false);

        try {

            long id = ctrl.inserirProduto(
                    nome,
                    tipo,
                    codigo,
                    unidade
            );

            if (id <= 0) {

                btnSalvar.setEnabled(true);

                Util.toast(
                        requireContext(),
                        "Não foi possível salvar o produto."
                );

                return;
            }

            Util.toast(
                    requireContext(),
                    "Produto cadastrado com sucesso."
            );

            // Exibe o QR Code gerado para o produto.
            QrCodeHelper.mostrar(
                    requireContext(),
                    nome,
                    codigo,
                    () -> {

                        if (isAdded()) {
                            ((MainActivity) requireActivity()).voltar();
                        }
                    }
            );

        } catch (Exception e) {

            btnSalvar.setEnabled(true);

            Util.toast(
                    requireContext(),
                    "Erro ao cadastrar produto: " + e.getMessage()
            );
        }
    }
}
