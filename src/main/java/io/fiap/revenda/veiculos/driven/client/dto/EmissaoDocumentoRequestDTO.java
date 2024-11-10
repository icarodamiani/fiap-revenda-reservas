package io.fiap.revenda.veiculos.driven.client.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.fiap.revenda.veiculos.driven.domain.Pessoa;
import io.fiap.revenda.veiculos.driven.domain.Veiculo;
import org.immutables.value.Value;

@JsonSerialize(as = ImmutableEmissaoDocumentoRequestDTO.class)
@JsonDeserialize(as = ImmutableEmissaoDocumentoRequestDTO.class)
@Value.Immutable
@Value.Style(privateNoargConstructor = true, jdkOnly = true)
public abstract class EmissaoDocumentoRequestDTO {
    public abstract String getId();
    public abstract String getTipo();
    public abstract String getOrgao();
    public abstract Veiculo getVeiculo();
    public abstract Pessoa getPessoa();
}