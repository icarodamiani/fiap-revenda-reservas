package io.fiap.revenda.veiculos.driven.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

@JsonSerialize(as = ImmutablePessoa.class)
@JsonDeserialize(as = ImmutablePessoa.class)
@Value.Immutable
@Value.Style(privateNoargConstructor = true, jdkOnly = true)
public abstract class Pessoa {
    public abstract String getId();
    public abstract String getNome();
    public abstract String getSobrenome();
    public abstract DocumentoPessoa getDocumento();
}
