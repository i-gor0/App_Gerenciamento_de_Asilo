
package com.example.projetoasilo;

import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.List;

import BancoDeDados.BancoControllerPaciente;
import BancoDeDados.BancoControllerQuarto;

public class CadastrarPacienteFragment extends Fragment {

    private EditText edtNome;
    private EditText edtNascimento;
    private EditText edtCpf;
    private EditText edtTelefone;
    private EditText edtResponsavel;
    private EditText edtTelefoneResponsavel;
    private EditText edtEndereco;
    private EditText edtObservacoes;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {

        return inflater.inflate(
                R.layout.fragment_cadastrar_paciente,
                container,
                false
        );
    }

    @Override
    public void onViewCreated(
            @NonNull View v,
            @Nullable Bundle savedInstanceState) {

        edtNome = v.findViewById(R.id.edtNomePaciente);
        edtNascimento = v.findViewById(R.id.edtDataNascimento);
        edtCpf = v.findViewById(R.id.edtCpf);
        edtTelefone = v.findViewById(R.id.edtTelefone);
        edtResponsavel = v.findViewById(R.id.edtResponsavel);
        edtTelefoneResponsavel = v.findViewById(R.id.edtTelefoneResponsavel);
        edtEndereco = v.findViewById(R.id.edtEndereco);
        edtObservacoes = v.findViewById(R.id.edtObservacoes);

        v.findViewById(R.id.btnCadastrarPaciente)
                .setOnClickListener(x -> salvar());
    }

    private String t(EditText campo) {
        return campo.getText().toString().trim();
    }

    private void salvar() {

        if (!Util.pode(requireContext(), "podecadastrar")) {
            return;
        }

        if (t(edtNome).isEmpty()) {
            edtNome.setError("Informe o nome do paciente");
            edtNome.requestFocus();
            return;
        }

        if (t(edtNascimento).isEmpty()) {
            edtNascimento.setError("Informe a data de nascimento");
            edtNascimento.requestFocus();
            return;
        }

        escolherQuarto(this::gravar);
    }

    /**
     * Exibe os quartos cadastrados para selecionar
     * onde o paciente ficará hospedado.
     */
    private void escolherQuarto(Util.OnNumber aoEscolher) {

        List<Integer> ids = new ArrayList<>();
        List<String> rotulos = new ArrayList<>();

        Cursor cursor = null;

        try {

            BancoControllerQuarto ctrl =
                    new BancoControllerQuarto(requireContext());

            cursor = ctrl.listarQuartos();

            while (cursor.moveToNext()) {

                ids.add(Util.num(cursor, "Id", "_id", "id"));

                rotulos.add(
                        "Quarto " + Util.str(cursor, "numero")
                                + " (capacidade "
                                + Util.num(cursor, "capacidade")
                                + ")"
                );
            }

        } finally {

            if (cursor != null) {
                cursor.close();
            }
        }

        if (ids.isEmpty()) {
            Util.toast(
                    requireContext(),
                    "Cadastre um quarto antes de cadastrar pacientes."
            );
            return;
        }

        Util.escolher(
                requireContext(),
                "Quarto do paciente",
                rotulos.toArray(new String[0]),
                i -> aoEscolher.on(ids.get(i))
        );
    }

    /**
     * Cadastra o paciente utilizando os nove parâmetros
     * exigidos pelo BancoControllerPaciente.
     */
    private void gravar(int quartoId) {

        try {

            BancoControllerPaciente ctrl =
                    new BancoControllerPaciente(requireContext());

            String resultado = ctrl.insereDados(
                    quartoId,
                    t(edtNome),
                    t(edtNascimento),
                    t(edtCpf),
                    t(edtTelefone),
                    t(edtTelefoneResponsavel),
                    t(edtResponsavel),
                    t(edtEndereco),
                    t(edtObservacoes)
            );

            Util.toast(requireContext(), resultado);

            if ("Paciente cadastrado com sucesso".equals(resultado)) {
                limpar();
            }

        } catch (Exception e) {

            Util.toast(
                    requireContext(),
                    "Erro ao cadastrar paciente: " + e.getMessage()
            );
        }
    }

    /**
     * Limpa os campos depois de um cadastro realizado.
     */
    private void limpar() {

        EditText[] campos = {
                edtNome,
                edtNascimento,
                edtCpf,
                edtTelefone,
                edtResponsavel,
                edtTelefoneResponsavel,
                edtEndereco,
                edtObservacoes
        };

        for (EditText campo : campos) {
            campo.setText("");
            campo.setError(null);
        }

        edtNome.requestFocus();
    }
}
