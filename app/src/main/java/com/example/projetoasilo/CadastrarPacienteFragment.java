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

/** Lógica de fragment_cadastrar_paciente.xml */
public class CadastrarPacienteFragment extends Fragment {

    private EditText edtNome, edtNascimento, edtCpf, edtTelefone,
            edtResponsavel, edtTelefoneResponsavel, edtEndereco, edtObservacoes;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_cadastrar_paciente, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        edtNome = v.findViewById(R.id.edtNomePaciente);
        edtNascimento = v.findViewById(R.id.edtDataNascimento);
        edtCpf = v.findViewById(R.id.edtCpf);
        edtTelefone = v.findViewById(R.id.edtTelefone);
        edtResponsavel = v.findViewById(R.id.edtResponsavel);
        edtTelefoneResponsavel = v.findViewById(R.id.edtTelefoneResponsavel);
        edtEndereco = v.findViewById(R.id.edtEndereco);
        edtObservacoes = v.findViewById(R.id.edtObservacoes);

        v.findViewById(R.id.btnCadastrarPaciente).setOnClickListener(x -> salvar());
    }

    private String t(EditText e) {
        return e.getText().toString().trim();
    }

    private void salvar() {
        if (!Util.pode(requireContext(), "podecadastrar")) return;

        if (t(edtNome).isEmpty()) {
            edtNome.setError("Informe o nome");
            return;
        }
        if (t(edtNascimento).isEmpty()) {
            edtNascimento.setError("Informe a data de nascimento");
            return;
        }

        escolherQuarto(this::gravar);
    }

    /** O layout não tem campo de quarto, então o quarto é escolhido num diálogo. */
    private void escolherQuarto(Util.OnNumber aoEscolher) {
        List<Integer> ids = new ArrayList<>();
        List<String> rotulos = new ArrayList<>();

        Cursor c = new BancoControllerQuarto(requireContext()).listarQuartos();
        while (c.moveToNext()) {
            ids.add(Util.num(c, "id", "_id"));
            rotulos.add("Quarto " + Util.str(c, "numero")
                    + " (capacidade " + Util.num(c, "capacidade") + ")");
        }
        c.close();

        if (ids.isEmpty()) {
            Util.toast(requireContext(), "Cadastre um quarto antes de cadastrar pacientes.");
            return;
        }

        Util.escolher(requireContext(), "Quarto do paciente",
                rotulos.toArray(new String[0]),
                i -> aoEscolher.on(ids.get(i)));
    }

    private void gravar(int quartoId) {
        try {
            BancoControllerPaciente ctrl = new BancoControllerPaciente(requireContext());
            int antes = Util.contar(ctrl.listarPacientes());

            // Assinatura documentada: insereDados(quartoId, nome, dataNascimento, observacoes)
            // Os demais campos do formulário são guardados dentro das observações.
            // Se o seu insereDados já aceita cpf/telefones/responsável/endereço,
            // troque esta linha pela chamada completa.
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

            if (resultado.equals("Paciente cadastrado com sucesso")) {
                Util.toast(requireContext(), resultado);
                limpar();
            } else {
                Util.toast(requireContext(), resultado);
            }

            int depois = Util.contar(ctrl.listarPacientes());
            if (depois > antes) {
                Util.toast(requireContext(), "Paciente cadastrado com sucesso.");
                limpar();
            } else {
                Util.toast(requireContext(), "Não foi possível cadastrar o paciente.");
            }
        } catch (Exception e) {
            Util.toast(requireContext(), "Erro ao cadastrar: " + e.getMessage());
        }
    }

    private String montarObservacoes() {
        StringBuilder sb = new StringBuilder();
        adicionar(sb, "CPF", t(edtCpf));
        adicionar(sb, "Telefone", t(edtTelefone));
        adicionar(sb, "Responsável", t(edtResponsavel));
        adicionar(sb, "Tel. responsável", t(edtTelefoneResponsavel));
        adicionar(sb, "Endereço do responsável", t(edtEndereco));
        adicionar(sb, "Observações", t(edtObservacoes));
        return sb.toString();
    }

    private void adicionar(StringBuilder sb, String rotulo, String valor) {
        if (valor.isEmpty()) return;
        if (sb.length() > 0) sb.append("\n");
        sb.append(rotulo).append(": ").append(valor);
    }

    private void limpar() {
        for (EditText e : new EditText[]{edtNome, edtNascimento, edtCpf, edtTelefone,
                edtResponsavel, edtTelefoneResponsavel, edtEndereco, edtObservacoes}) {
            e.setText("");
        }
        edtNome.requestFocus();
    }
}
